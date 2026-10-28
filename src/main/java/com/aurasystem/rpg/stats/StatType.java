package com.aurasystem.rpg.stats;

import java.util.Locale;

/** Las 8 estadisticas. El orden (ordinal) es el indice usado en guardado y red. */
public enum StatType {
    STRENGTH("Fuerza", "Aumenta el daño físico cuerpo a cuerpo."),
    VITALITY("Vitalidad", "Aumenta la vida máxima."),
    RESISTANCE("Resistencia", "Reduce el daño recibido (armadura) y el retroceso."),
    AGILITY("Agilidad", "Mejora la velocidad de movimiento y de ataque."),
    ENERGY("Energía", "Aumenta la energía de aura máxima."),
    REGENERATION("Regeneración", "Acelera la recuperación de energía de aura."),
    CONTROL("Control", "Reduce el coste y el enfriamiento de los poderes."),
    POTENCY("Potencia", "Aumenta el daño de las técnicas de aura.");

    public final String label;
    public final String description;

    StatType(String label, String description) {
        this.label = label;
        this.description = description;
    }

    /** Rendimientos decrecientes: los primeros 10 puntos valen 1, los siguientes valen 0.5. */
    public static double effective(int points) {
        if (points <= 10) {
            return Math.max(0, points);
        }
        return 10 + (points - 10) * 0.5;
    }

    /** Texto cuantificado del efecto actual con 'points' puntos invertidos. */
    public String effectText(int points) {
        double e = effective(points);
        Locale l = Locale.ROOT;
        return switch (this) {
            case STRENGTH -> String.format(l, "+%.0f%% daño físico", StatEffects.MELEE_PER_POINT * e * 100);
            case VITALITY -> String.format(l, "+%.1f vida (%.1f ♥)", StatEffects.HEALTH_PER_POINT * e, StatEffects.HEALTH_PER_POINT * e / 2);
            case RESISTANCE -> String.format(l, "+%.1f armadura, +%.0f%% antiretroceso", StatEffects.ARMOR_PER_POINT * e, StatEffects.KNOCKBACK_PER_POINT * e * 100);
            case AGILITY -> String.format(l, "+%.0f%% velocidad, +%.1f%% vel. ataque", StatEffects.MOVE_PER_POINT * e * 100, StatEffects.ATTACK_SPEED_PER_POINT * e * 100);
            case ENERGY -> String.format(l, "+%.0f energía máxima", StatEffects.ENERGY_PER_POINT * e);
            case REGENERATION -> String.format(l, "+%.1f energía/s", StatEffects.REGEN_PER_POINT * e);
            case CONTROL -> String.format(l, "-%.0f%% coste, -%.0f%% enfriamiento",
                    Math.min(StatEffects.COST_REDUCTION_CAP, StatEffects.COST_PER_POINT * e) * 100,
                    Math.min(StatEffects.COOLDOWN_REDUCTION_CAP, StatEffects.COOLDOWN_PER_POINT * e) * 100);
            case POTENCY -> String.format(l, "+%.0f%% daño de técnicas", StatEffects.POWER_PER_POINT * e * 100);
        };
    }
}
