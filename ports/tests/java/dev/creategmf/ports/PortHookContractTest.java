package dev.creategmf.ports;

import static org.junit.jupiter.api.Assertions.*;
import com.google.gson.JsonParser;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.net.JarURLConnection;
import java.util.zip.ZipFile;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;

/** Binary hook checks, not a substitute for in-world behavioural testing. */
class PortHookContractTest {
    private static ClassNode read(String name) throws Exception {
        var resource = PortHookContractTest.class.getClassLoader().getResource(name.replace('.', '/') + ".class");
        if (resource == null) return null;
        // Development remapping changes signed Forge classes. Inspect raw ZIP
        // bytes (never execute them), rather than invoking JarVerifier on this
        // transformed inspection artifact.
        if (resource.openConnection() instanceof JarURLConnection connection) {
            try (ZipFile zip = new ZipFile(new java.io.File(connection.getJarFileURL().toURI()));
                    InputStream in = zip.getInputStream(zip.getEntry(connection.getEntryName()))) {
                ClassNode node = new ClassNode();
                new ClassReader(in).accept(node, ClassReader.SKIP_DEBUG);
                return node;
            }
        }
        try (InputStream in = resource.openStream()) {
            if (in == null) return null;
            ClassNode node = new ClassNode();
            new ClassReader(in).accept(node, ClassReader.SKIP_DEBUG);
            return node;
        }
    }

    private static Object value(AnnotationNode node, String key) {
        if (node.values != null) for (int i = 0; i < node.values.size(); i += 2)
            if (key.equals(node.values.get(i))) return node.values.get(i + 1);
        return null;
    }

    private static List<AnnotationNode> annotations(ClassNode node) {
        List<AnnotationNode> out = new ArrayList<>();
        if (node.visibleAnnotations != null) out.addAll(node.visibleAnnotations);
        if (node.invisibleAnnotations != null) out.addAll(node.invisibleAnnotations);
        return out;
    }

    private static Set<String> methodNames(ClassNode target) throws Exception {
        Set<String> names = new HashSet<>();
        for (var method : target.methods) names.add(method.name);
        if (target.superName != null) {
            ClassNode parent = read(target.superName);
            if (parent != null) names.addAll(methodNames(parent));
        }
        for (String iface : target.interfaces) {
            ClassNode parent = read(iface);
            if (parent != null) names.addAll(methodNames(parent));
        }
        return names;
    }

    private static void checkInvocations(ClassNode target, String selector, AnnotationNode injector, String label) {
        Object at = value(injector, "at");
        List<?> sites = at instanceof List<?> list ? list : at == null ? List.of() : List.of(at);
        for (Object site : sites) {
            if (!(site instanceof AnnotationNode annotation)) continue;
            if (!"INVOKE".equals(value(annotation, "value"))) continue;
            Object raw = value(annotation, "target");
            if (!(raw instanceof String expected) || !expected.startsWith("L")) continue;
            // Minecraft members are remapped by the loader's refmap. This
            // contract compares only stable, Create/Flywheel-owned bytecode.
            if (!expected.startsWith("Lcom/simibubi/") && !expected.startsWith("Lnet/createmod/")
                    && !expected.startsWith("Ldev/engine_room/")) continue;
            String name = selector.split("\\(", 2)[0];
            boolean found = false;
            for (var method : target.methods) {
                if (!method.name.equals(name)) continue;
                if (selector.contains("(") && !selector.equals(method.name + method.desc)) continue;
                for (var instruction : method.instructions) {
                    if (instruction instanceof org.objectweb.asm.tree.MethodInsnNode call
                            && expected.equals("L" + call.owner + ";" + call.name + call.desc)) found = true;
                }
            }
            assertTrue(found, label + " missing call " + expected);
        }
    }

    @Test void configuredHooksTargetRealClassesAndMethods() throws Exception {
        try (InputStream in = getClass().getResourceAsStream("/create_gmf.mixins.json")) {
            assertNotNull(in);
            var config = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
            String prefix = config.get("package").getAsString() + ".";
            int checked = 0;
            for (var element : config.getAsJsonArray("client")) {
                String mixinName = prefix + element.getAsString();
                ClassNode mixin = read(mixinName);
                assertNotNull(mixin, mixinName);
                List<AnnotationNode> annotations = annotations(mixin);
                if (annotations.stream().anyMatch(a -> a.desc.endsWith("/Pseudo;"))) continue;
                AnnotationNode declaration = annotations.stream().filter(a -> a.desc.endsWith("/Mixin;"))
                        .findFirst().orElseThrow();
                List<String> targets = new ArrayList<>();
                Object types = value(declaration, "value");
                if (types instanceof List<?> list) for (Object type : list) targets.add(((Type) type).getClassName());
                Object strings = value(declaration, "targets");
                if (strings instanceof List<?> list) for (Object name : list) targets.add(name.toString());
                assertFalse(targets.isEmpty(), mixinName);
                for (String targetName : targets) {
                    ClassNode target = read(targetName);
                    assertNotNull(target, mixinName + " -> " + targetName);
                    Set<String> available = methodNames(target);
                    for (var handler : mixin.methods) {
                        if (handler.visibleAnnotations == null) continue;
                        for (AnnotationNode injector : handler.visibleAnnotations) {
                            Object selectors = value(injector, "method");
                            if (!(selectors instanceof List<?> list)) continue;
                            for (Object selector : list) {
                                String method = selector.toString().split("\\(", 2)[0];
                                // This method is added by Entity Culling's own
                                // BlockEntity mixin after the original class is read.
                                if (targetName.equals("net.minecraft.world.level.block.entity.BlockEntity")
                                        && method.equals("setCulled")) continue;
                                assertTrue(available.contains(method), mixinName + " -> " + targetName + "#" + selector);
                                checkInvocations(target, selector.toString(), injector, mixinName + "#" + selector);
                                checked++;
                            }
                        }
                    }
                }
            }
            // Create 0.5 has fewer renderer classes than Create 6; still inspect
            // every configured injector above, including its invocation sites.
            int minimum = read("dev.creategmf.mixin.client.LegacyInstanceManagerMixin") == null ? 50 : 35;
            assertTrue(checked >= minimum, "Too few hooks checked: " + checked);
        }
    }

    @Test void flywheelSimpleDispatchAdapterIsPresent() throws Exception {
        ClassNode storage = read("dev.engine_room.flywheel.impl.visualization.storage.Storage");
        if (storage == null) {
            ClassNode manager = read("com.jozufozu.flywheel.backend.instancing.InstanceManager");
            assertNotNull(manager);
            assertTrue(methodNames(manager).containsAll(List.of("updateInstance", "tickInstance")));
            assertNotNull(read("dev.creategmf.mixin.client.LegacyInstanceManagerMixin"));
            assertNotNull(read("dev.creategmf.mixin.client.LegacyKineticDataMixin"));
            return;
        }
        assertNotNull(storage);
        assertTrue(methodNames(storage).containsAll(List.of("framePlan", "tickPlan", "setup")));
        assertNotNull(read("dev.creategmf.mixin.client.StorageAnimationMixin"));
        assertNull(read("dev.creategmf.mixin.client.SimpleDynamicVisualMixin"));
    }
}
