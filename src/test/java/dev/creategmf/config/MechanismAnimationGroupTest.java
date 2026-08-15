package dev.creategmf.config;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class MechanismAnimationGroupTest {
    @Test
    void classifiesCreateMechanismPackages() {
        assertEquals(MechanismAnimationGroup.BELTS, group("kinetics.belt.BeltVisual"));
        assertEquals(MechanismAnimationGroup.STEAM_ENGINES, group("kinetics.steamEngine.SteamEngineVisual"));
        assertEquals(MechanismAnimationGroup.PRESSES, group("kinetics.press.PressVisual"));
        assertEquals(MechanismAnimationGroup.MIXERS, group("kinetics.mixer.MixerVisual"));
        assertEquals(MechanismAnimationGroup.MECHANICAL_ARMS, group("kinetics.mechanicalArm.ArmVisual"));
        assertEquals(MechanismAnimationGroup.HARVESTERS,
                group("contraptions.actors.harvester.HarvesterActorVisual"));
        assertEquals(MechanismAnimationGroup.HARVESTERS,
                group("contraptions.actors.roller.RollerActorVisual"));
        assertEquals(MechanismAnimationGroup.MECHANICAL_PISTONS,
                group("contraptions.piston.MechanicalPistonBlockEntity"));
        assertEquals(MechanismAnimationGroup.GANTRIES_AND_PULLEYS,
                group("contraptions.pulley.RopePulleyVisual"));
        assertEquals(MechanismAnimationGroup.PUMPS_AND_PIPES, group("fluids.pipes.GlassPipeVisual"));
        assertEquals(MechanismAnimationGroup.SHAFTS_AND_COGS,
                group("kinetics.simpleRelays.BracketedKineticBlockEntityVisual"));
        assertEquals(MechanismAnimationGroup.BLAZE_BURNERS,
                group("processing.burner.BlazeBurnerVisual"));
        assertEquals(MechanismAnimationGroup.TRAINS_AND_RAILS,
                group("trains.display.FlapDisplaySection"));
        assertEquals(MechanismAnimationGroup.TUNNELS_AND_FUNNELS,
                group("logistics.tunnel.BeltTunnelVisual"));
        assertEquals(MechanismAnimationGroup.EJECTORS,
                group("logistics.depot.EjectorVisual"));
        assertEquals(MechanismAnimationGroup.PACKAGE_PORTS,
                group("logistics.packagePort.frogport.FrogportVisual"));
    }

    @Test
    void unknownCreateClassUsesOtherGroup() {
        assertEquals(MechanismAnimationGroup.OTHER,
                MechanismAnimationGroup.fromClassName("com.simibubi.create.content.logistics.box.PackageVisual"));
    }

    private static MechanismAnimationGroup group(String suffix) {
        return MechanismAnimationGroup.fromClassName("com.simibubi.create.content." + suffix);
    }
}
