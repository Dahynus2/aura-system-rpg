package com.aurasystem.rpg.stats;

import com.aurasystem.rpg.config.AuraConfig;
import com.aurasystem.rpg.data.AuraData;

/** Formulas puras de las estadisticas. Todas se evaluan en el servidor. */
public final class StatEffects {
    private StatEffects() {}

    public static final double MELEE_PER_POINT = 0.03;
    public static final double HEALTH_PER_POINT = 1.0;
    public static final double ARMOR_PER_POINT = 0.4;
    public static final double KNOCKBACK_PER_POINT = 0.01;
    public static final double MOVE_PER_POINT = 0.01;
    public static final double ATTACK_SPEED_PER_POINT = 0.015;
    public static final double ENERGY_PER_POINT = 10.0;
    public static final double REGEN_PER_POINT = 0.4;
    public static final double COST_PER_POINT = 0.03;
    public static final double COST_REDUCTION_CAP = 0.5;
    public static final double COOLDOWN_PER_POINT = 0.02;
    public static final double COOLDOWN_REDUCTION_CAP = 0.3;
    public static final double POWER_PER_POINT = 0.05;

    public static double eff(AuraData d, StatType t) {
        return StatType.effective(d.stat(t));
    }

    public static double maxEnergy(AuraData d) {
        return AuraConfig.get().baseEnergy + ENERGY_PER_POINT * eff(d, StatType.ENERGY);
    }

    public static double energyRegenPerSecond(AuraData d) {
        return AuraConfig.get().baseEnergyRegenPerSecond + REGEN_PER_POINT * eff(d, StatType.REGENERATION);
    }

    public static double costMultiplier(AuraData d) {
        return 1.0 - Math.min(COST_REDUCTION_CAP, COST_PER_POINT * eff(d, StatType.CONTROL));
    }

    public static double cooldownMultiplier(AuraData d) {
        return 1.0 - Math.min(COOLDOWN_REDUCTION_CAP, COOLDOWN_PER_POINT * eff(d, StatType.CONTROL));
    }

    public static double powerMultiplier(AuraData d) {
        return 1.0 + POWER_PER_POINT * eff(d, StatType.POTENCY);
    }
}
