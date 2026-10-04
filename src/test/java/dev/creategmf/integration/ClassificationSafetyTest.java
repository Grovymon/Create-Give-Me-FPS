package dev.creategmf.integration;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import dev.creategmf.optimization.particles.ParticleRules;
class ClassificationSafetyTest {
    @Test void addonUsesInheritanceNotPackageName() {
        assertTrue(CreateVisualClassifier.supported("createaddition", true, false));
        assertTrue(CreateVisualClassifier.supported("new_unknown_addon", false, true));
        assertFalse(CreateVisualClassifier.supported("randommod", false, false));
        assertFalse(CreateVisualClassifier.supported("minecraft", false, false));
        assertTrue(CreateVisualClassifier.supported("create", false, false));
    }
    @Test void vanillaParticleAppearanceDoesNotProveCreateOrigin() {
        assertFalse(ParticleRules.registeredCreateParticle("minecraft"));
        assertFalse(ParticleRules.registeredCreateParticle("othermod"));
        assertTrue(ParticleRules.registeredCreateParticle("create"));
    }
}
