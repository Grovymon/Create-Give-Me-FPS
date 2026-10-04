package dev.creategmf.integration;
import static org.junit.jupiter.api.Assertions.*;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.*;

/** Verifies source-capture injection against the actually resolved Create JAR, not just Java compilation. */
class RenderHookBytecodeTest {
    @Test void pinnedEntityCullingHasCompatibleApiAndWorkerCallsite() throws Exception {
        var methods = new java.util.HashSet<String>();
        try (var stream = getClass().getClassLoader().getResourceAsStream("dev/tr7zw/entityculling/access/Cullable.class")) {
            assertNotNull(stream);
            new ClassReader(stream).accept(new ClassVisitor(Opcodes.ASM9) {
                @Override public MethodVisitor visitMethod(int access, String name, String desc,
                        String signature, String[] exceptions) {
                    methods.add(name + desc); return null;
                }
            }, ClassReader.SKIP_CODE);
        }
        assertTrue(methods.contains("isCulled()Z"));
        assertTrue(methods.contains("isForcedVisible()Z"));
        var found = new AtomicBoolean();
        try (var stream = getClass().getClassLoader().getResourceAsStream("dev/tr7zw/entityculling/EntityCullingModBase.class")) {
            assertNotNull(stream);
            new ClassReader(stream).accept(new ClassVisitor(Opcodes.ASM9) {
                @Override public MethodVisitor visitMethod(int access, String name, String desc,
                        String signature, String[] exceptions) {
                    if (!name.equals("prefetchBlockEntityData")) return null;
                    return new MethodVisitor(Opcodes.ASM9) {
                        @Override public void visitMethodInsn(int opcode, String owner, String method, String descriptor, boolean face) {
                            if (owner.equals("net/minecraft/client/renderer/blockentity/BlockEntityRenderDispatcher")
                                    && method.equals("getRenderer")) found.set(true);
                        }
                    };
                }
            }, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
        }
        assertTrue(found.get(), "Pinned EC must expose the renderer-probe hook; no copied EC code");
    }
    @Test void contraptionOwnerShadowUsesErasedEntityDescriptor() throws Exception {
        var descriptor = new java.util.concurrent.atomic.AtomicReference<String>();
        try (var stream = getClass().getClassLoader().getResourceAsStream(
                "dev/engine_room/flywheel/lib/visual/AbstractEntityVisual.class")) {
            assertNotNull(stream);
            new ClassReader(stream).accept(new ClassVisitor(Opcodes.ASM9) {
                @Override public FieldVisitor visitField(int access, String name, String desc,
                        String signature, Object value) {
                    if (name.equals("entity")) descriptor.set(desc);
                    return null;
                }
            }, ClassReader.SKIP_CODE);
        }
        assertEquals("Lnet/minecraft/world/entity/Entity;", descriptor.get());
        var source = java.nio.file.Files.readString(java.nio.file.Path.of(
                "src/main/java/dev/creategmf/mixin/client/ContraptionVisualMixin.java"));
        assertTrue(source.contains("extends dev.engine_room.flywheel.lib.visual.AbstractEntityVisual<AbstractContraptionEntity>"));
        assertFalse(source.contains("@Shadow @Final protected AbstractContraptionEntity entity"));
    }
    @Test void beltSetupHasTheExactCaptureInvocation() throws Exception {
        var found = new AtomicBoolean();
        try (var stream = getClass().getClassLoader().getResourceAsStream(
                "com/simibubi/create/content/kinetics/belt/BeltVisual.class")) {
            assertNotNull(stream);
            new ClassReader(stream).accept(new ClassVisitor(Opcodes.ASM9) {
                @Override public MethodVisitor visitMethod(int access, String name, String descriptor,
                        String signature, String[] exceptions) {
                    if (!name.equals("setup")) return null;
                    return new MethodVisitor(Opcodes.ASM9) {
                        @Override public void visitMethodInsn(int opcode, String owner, String method, String desc, boolean face) {
                            if (owner.equals("dev/engine_room/flywheel/lib/instance/ColoredLitInstance")
                                    && method.equals("setChanged") && desc.equals("()V")) found.set(true);
                        }
                    };
                }
            }, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
        }
        assertTrue(found.get(), "Belt shader source must be captured BEFORE ColoredLitInstance.setChanged");
    }
}
