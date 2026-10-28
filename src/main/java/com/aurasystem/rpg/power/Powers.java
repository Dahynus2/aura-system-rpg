package com.aurasystem.rpg.power;

import java.util.LinkedHashMap;
import java.util.Map;

public final class Powers {
    private Powers() {}

    private static final Map<String, AuraPower> REGISTRY = new LinkedHashMap<>();

    /** Etapa 1: unico poder. */
    public static final AuraPower AURA_BOLT = register(
            new AuraPower("aura_bolt", "Proyectil de aura", 1, 20.0, 30, 6.0, 28.0));

    private static AuraPower register(AuraPower power) {
        REGISTRY.put(power.id(), power);
        return power;
    }

    public static AuraPower get(String id) {
        return REGISTRY.get(id);
    }
}
