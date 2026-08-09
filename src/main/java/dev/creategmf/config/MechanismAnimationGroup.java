package dev.creategmf.config;

public enum MechanismAnimationGroup {
    BELTS("belts", "config.create_gmf.mechanism.belts"),
    SHAFTS_AND_COGS("shaftsAndCogs", "config.create_gmf.mechanism.shafts_and_cogs"),
    FLYWHEELS("flywheels", "config.create_gmf.mechanism.flywheels"),
    STEAM_ENGINES("steamEngines", "config.create_gmf.mechanism.steam_engines"),
    WATER_WHEELS("waterWheels", "config.create_gmf.mechanism.water_wheels"),
    FANS("fans", "config.create_gmf.mechanism.fans"),
    PRESSES("presses", "config.create_gmf.mechanism.presses"),
    MIXERS("mixers", "config.create_gmf.mechanism.mixers"),
    DEPLOYERS("deployers", "config.create_gmf.mechanism.deployers"),
    MECHANICAL_ARMS("mechanicalArms", "config.create_gmf.mechanism.mechanical_arms"),
    HARVESTERS("harvesters", "config.create_gmf.mechanism.harvesters"),
    SAWS("saws", "config.create_gmf.mechanism.saws"),
    CRUSHERS_AND_MILLSTONES("crushersAndMillstones", "config.create_gmf.mechanism.crushers_and_millstones"),
    DRILLS("drills", "config.create_gmf.mechanism.drills"),
    MECHANICAL_PISTONS("mechanicalPistons", "config.create_gmf.mechanism.mechanical_pistons"),
    GANTRIES_AND_PULLEYS("gantriesAndPulleys", "config.create_gmf.mechanism.gantries_and_pulleys"),
    BEARINGS_AND_CONTRAPTIONS("bearingsAndContraptions", "config.create_gmf.mechanism.bearings_and_contraptions"),
    PUMPS_AND_PIPES("pumpsAndPipes", "config.create_gmf.mechanism.pumps_and_pipes"),
    CHAINS_AND_CONVEYORS("chainsAndConveyors", "config.create_gmf.mechanism.chains_and_conveyors"),
    OTHER("other", "config.create_gmf.mechanism.other");

    private final String configKey;
    private final String translationKey;

    MechanismAnimationGroup(String configKey, String translationKey) {
        this.configKey = configKey;
        this.translationKey = translationKey;
    }

    public String configKey() {
        return configKey;
    }

    public String translationKey() {
        return translationKey;
    }

    public static MechanismAnimationGroup fromClassName(String className) {
        if (className == null) {
            return OTHER;
        }
        if (className.contains(".kinetics.belt.")) return BELTS;
        if (className.contains(".kinetics.flywheel.")) return FLYWHEELS;
        if (className.contains(".kinetics.steamEngine.")) return STEAM_ENGINES;
        if (className.contains(".kinetics.waterwheel.")) return WATER_WHEELS;
        if (className.contains(".kinetics.fan.")) return FANS;
        if (className.contains(".kinetics.press.")) return PRESSES;
        if (className.contains(".kinetics.mixer.")) return MIXERS;
        if (className.contains(".kinetics.deployer.")) return DEPLOYERS;
        if (className.contains(".kinetics.mechanicalArm.")) return MECHANICAL_ARMS;
        if (className.contains(".contraptions.actors.harvester.")
                || className.contains(".contraptions.actors.roller.")) return HARVESTERS;
        if (className.contains(".kinetics.saw.")) return SAWS;
        if (className.contains(".kinetics.crusher.") || className.contains(".kinetics.millstone.")) {
            return CRUSHERS_AND_MILLSTONES;
        }
        if (className.contains(".kinetics.drill.")) return DRILLS;
        if (className.contains(".contraptions.piston.")) return MECHANICAL_PISTONS;
        if (className.contains(".contraptions.gantry.") || className.contains(".contraptions.pulley.")
                || className.contains(".contraptions.elevator.") || className.contains(".kinetics.gantry.")) {
            return GANTRIES_AND_PULLEYS;
        }
        if (className.contains(".contraptions.bearing.") || className.contains(".contraptions.render.")) {
            return BEARINGS_AND_CONTRAPTIONS;
        }
        if (className.contains(".fluids.")) return PUMPS_AND_PIPES;
        if (className.contains(".kinetics.chain")) return CHAINS_AND_CONVEYORS;
        if (className.contains(".kinetics.simpleRelays.") || className.contains(".kinetics.transmission.")
                || className.contains(".kinetics.gearbox.") || className.contains(".kinetics.base.")
                || className.contains(".kinetics.motor.") || className.contains(".kinetics.speedController.")
                || className.contains(".kinetics.turntable.") || className.contains(".kinetics.crank.")
                || className.contains(".kinetics.gauge.") || className.contains(".kinetics.crafter.")) {
            return SHAFTS_AND_COGS;
        }
        return OTHER;
    }
}
