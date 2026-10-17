package dev.creategmf.optimization;

import static org.junit.jupiter.api.Assertions.*;
import com.google.gson.JsonParser;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.*;

/** Recovery release contracts: baseline coverage plus the actual resolved dependency ABI. */
class RecoveryContractTest {
    @Test void baselineFreezeAndEffectHooksRemainRegistered() throws Exception {
        var json = JsonParser.parseString(Files.readString(Path.of("src/main/resources/create_gmf.mixins.json"))).getAsJsonObject();
        var actual = new java.util.HashSet<String>();
        json.getAsJsonArray("client").forEach(value -> actual.add(value.getAsString()));
        for (String hook : Set.of("AbstractInstanceMixin", "RotatingInstanceMixin", "BeltVisualMixin",
                "BlazeBurnerVisualMixin", "CreateBlockEntityDynamicVisualMixin", "ContraptionVisualMixin",
                "SpriteContentsMixin", "SpriteContentsTickerMixin", "FluidManipulationBehaviourMixin",
                "FluidFXMixin", "FluidSplashPacketMixin", "ParticleEngineMixin")) {
            assertTrue(actual.contains("client." + hook), hook);
            assertTrue(Files.exists(Path.of("src/main/java/dev/creategmf/mixin/client/" + hook + ".java")), hook);
        }
    }

    @Test void unknownScrollFallbackStillReachesTheFreezePolicy() throws Exception {
        String source = source("mixin/client/AbstractInstanceMixin.java");
        assertTrue(source.contains("shouldFreezeUnclassifiedScrolling("));
        assertTrue(source.contains("scroll.speedU = 0;"));
        assertTrue(source.contains("scroll.speedV = 0;"));
        assertTrue(source.contains("scroll.speedU = createGmf$savedSpeedU;"));
    }

    @Test void animationControllerDoesNotReintroduceOwnerBasedFullAnimationBypass() throws Exception {
        String controller = source("optimization/animations/DistantAnimationController.java");
        assertFalse(controller.contains("knownWorldOwner"));
        assertFalse(controller.contains("CreateVisualClassifier"));
        assertTrue(controller.contains("AnimationPolicyRules.evaluate("));
        assertTrue(controller.contains("refreshAnimationBoundary();"));
    }

    @Test void wholeContraptionPlanKeepsTheBaselineStaticGuard() throws Exception {
        String contraption = source("mixin/client/ContraptionVisualMixin.java");
        assertTrue(contraption.contains("context -> createGmf$shouldUpdateFrame(context), original"));
        assertFalse(contraption.contains("@Shadow @Final protected AbstractContraptionEntity entity"));
        try (var input = getClass().getClassLoader().getResourceAsStream(
                "dev/engine_room/flywheel/lib/visual/AbstractEntityVisual.class")) {
            assertNotNull(input);
            var descriptor = new java.util.concurrent.atomic.AtomicReference<String>();
            new ClassReader(input).accept(new ClassVisitor(Opcodes.ASM9) {
                @Override public FieldVisitor visitField(int access, String name, String desc, String signature, Object value) {
                    if (name.equals("entity")) descriptor.set(desc);
                    return null;
                }
            }, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
            assertEquals("Lnet/minecraft/world/entity/Entity;", descriptor.get());
        }
    }

    @Test void modernEntityCullingApiMatchesTheBridgeAndPrefetchAdapter() throws Exception {
        try (var input = getClass().getClassLoader().getResourceAsStream("dev/tr7zw/entityculling/access/Cullable.class")) {
            assertNotNull(input);
            Set<String> methods = new java.util.HashSet<>();
            new ClassReader(input).accept(new ClassVisitor(Opcodes.ASM9) {
                @Override public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
                    methods.add(name + desc);
                    return null;
                }
            }, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
            assertTrue(methods.contains("isCulled()Z"));
            assertTrue(methods.contains("isForcedVisible()Z"));
        }
        assertTrue(source("optimization/occlusion/EntityCullingBridge.java")
                .contains("dev.tr7zw.entityculling.versionless.access.Cullable"));
        assertTrue(source("mixin/GmfMixinPlugin.java").contains("EntityCullingPrefetchMixin"));
    }

    @Test void particleAndFluidPoliciesHaveNotBeenNarrowedToCreateIdsOnly() throws Exception {
        String particles = source("optimization/particles/CreateParticleOptimizer.java");
        assertTrue(particles.contains("HEAVY_VANILLA_FLUID_PARTICLES"));
        assertTrue(particles.contains("shouldBeStatic(position, ownerClassName)"));
        assertTrue(source("optimization/animations/DistantAnimationController.java").contains("shouldFreezeSharedFluidTextures()"));
    }

    private static String source(String name) throws Exception {
        return Files.readString(Path.of("src/main/java/dev/creategmf/" + name));
    }
}
