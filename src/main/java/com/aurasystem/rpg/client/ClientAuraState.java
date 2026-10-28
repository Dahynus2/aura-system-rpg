package com.aurasystem.rpg.client;

import com.aurasystem.rpg.network.Payloads;
import java.util.HashMap;
import java.util.Map;

/** Copia local (solo lectura) del estado enviado por el servidor. Nunca es la fuente de verdad. */
public final class ClientAuraState {
    private ClientAuraState() {}

    private static Payloads.Sync current;
    private static final Map<String, Long> COOLDOWN_END = new HashMap<>();

    /** Reloj del cliente en ticks (solo avanza dentro de un mundo). */
    public static long ticks;
    public static long lastGain;
    public static long lastGainTick = Long.MIN_VALUE / 2;

    public static Payloads.Sync get() {
        return current;
    }

    public static void update(Payloads.Sync next) {
        if (current != null && next.lifetime() > current.lifetime()) {
            long diff = next.lifetime() - current.lifetime();
            lastGain = (ticks - lastGainTick < 40) ? lastGain + diff : diff;
            lastGainTick = ticks;
        }
        current = next;
    }

    public static void setCooldown(String power, int cooldownTicks) {
        COOLDOWN_END.put(power, ticks + cooldownTicks);
    }

    public static long cooldownTicksLeft(String power) {
        return Math.max(0L, COOLDOWN_END.getOrDefault(power, 0L) - ticks);
    }

    public static void clear() {
        current = null;
        COOLDOWN_END.clear();
        lastGain = 0;
        lastGainTick = Long.MIN_VALUE / 2;
    }
}
