package com.aurasystem.rpg.power;

/** Definicion de un poder. Los valores finales se ajustan con las estadisticas (Control, Potencia). */
public record AuraPower(String id, String name, int minLevel, double energyCost, int cooldownTicks,
                        double baseDamage, double range) {
}
