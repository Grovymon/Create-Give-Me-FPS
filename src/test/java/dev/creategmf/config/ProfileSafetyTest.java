package dev.creategmf.config;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import java.util.*;
class ProfileSafetyTest {
    @Test void presetsStayInClientRangesAndCustomIsNotInvented() {
        for (PcProfile profile : PcProfile.values()) if (profile != PcProfile.CUSTOM) {
            var values = ProfileRenderingValues.forProfile(profile);
            assertTrue(values.radius() >= 0 && values.radius() <= 32);
            assertTrue(values.animationFps() >= 1 && values.animationFps() <= 30);
        }
        assertEquals(0, ProfileRenderingValues.forProfile(PcProfile.POTATO).radius());
        assertEquals(CreateParticleMode.OFF, ProfileRenderingValues.forProfile(PcProfile.POTATO).particles());
        assertThrows(IllegalArgumentException.class, () -> ProfileRenderingValues.forProfile(PcProfile.CUSTOM));
    }
    @Test void exactCustomSnapshotIsImmutableAndRestorable() {
        Map<String, Object> live = new HashMap<>();
        List<String> groups = new ArrayList<>(List.of("newage:motor"));
        live.put("profile", PcProfile.CUSTOM); live.put("radius", 17.0); live.put("disabled", groups);
        var snapshot = new SettingsSnapshot<>(live);
        groups.clear(); live.put("profile", PcProfile.POTATO); live.put("radius", 0.0);
        snapshot.restore(live::put);
        assertEquals(PcProfile.CUSTOM, live.get("profile"));
        assertEquals(17.0, live.get("radius"));
        assertEquals(List.of("newage:motor"), live.get("disabled"));
    }
    @Test void migrationKeepsValidRadiusAndRejectsNonFiniteOrOversized() {
        assertEquals(13, ConfigMigration.schema(12, 13));
        assertEquals(17.0, ConfigMigration.animationRadius(17.0));
        assertEquals(32.0, ConfigMigration.animationRadius(Double.NaN));
        assertEquals(32.0, ConfigMigration.animationRadius(128));
        assertThrows(IllegalArgumentException.class, () -> ConfigMigration.schema(14, 13));
    }
}
