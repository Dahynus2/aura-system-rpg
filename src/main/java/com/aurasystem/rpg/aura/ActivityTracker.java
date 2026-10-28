package com.aurasystem.rpg.aura;

import com.aurasystem.rpg.config.AuraConfig;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Sistema antiexploit (solo servidor, en memoria):
 *  - Rendimientos decrecientes: cada actividad acumula "fatiga" al ganar aura y la pierde con el tiempo.
 *    multiplicador = 1 / (1 + fatiga / softCap)  -> repetir lo mismo rinde cada vez menos.
 *  - Bonificacion por variedad: ganar aura de varias actividades distintas en una ventana de tiempo da un extra.
 */
public final class ActivityTracker {
    private ActivityTracker() {}

    private static final Map<UUID, State> STATES = new HashMap<>();

    private static final class State {
        final double[] fatigue = new double[Activity.values().length];
        final long[] lastDecay = new long[Activity.values().length];
        final long[] lastAward = new long[Activity.values().length];
        double carry;

        State() {
            Arrays.fill(lastAward, Long.MIN_VALUE / 2);
        }
    }

    /** Devuelve el multiplicador total (fatiga x variedad) para un premio bruto 'raw'. */
    public static double evaluate(UUID id, Activity activity, double raw, long nowTick, boolean applyFatigue) {
        AuraConfig c = AuraConfig.get();
        State s = STATES.computeIfAbsent(id, k -> new State());
        int i = activity.ordinal();
        double mult = 1.0;

        if (applyFatigue) {
            double cap = Math.max(1.0, activity.softCap(c));
            double decayPerTick = cap / (Math.max(0.1, c.fatigueRecoveryMinutes) * 1200.0);
            long dt = Math.max(0L, nowTick - s.lastDecay[i]);
            s.fatigue[i] = Math.max(0.0, s.fatigue[i] - dt * decayPerTick);
            s.lastDecay[i] = nowTick;
            mult = 1.0 / (1.0 + s.fatigue[i] / cap);
            s.fatigue[i] += raw;
        }

        long window = (long) c.varietyWindowMinutes * 1200L;
        int distinct = 1;
        for (Activity other : Activity.values()) {
            if (other != activity && nowTick - s.lastAward[other.ordinal()] <= window) {
                distinct++;
            }
        }
        double bonus = Math.min(c.varietyBonusMax, c.varietyBonusPerActivity * (distinct - 1));
        s.lastAward[i] = nowTick;
        return mult * (1.0 + bonus);
    }

    /** Acumula fracciones de aura y devuelve la parte entera lista para entregar. */
    public static long takeWhole(UUID id, double amount) {
        State s = STATES.computeIfAbsent(id, k -> new State());
        s.carry += amount;
        long whole = (long) Math.floor(s.carry);
        s.carry -= whole;
        return whole;
    }

    public static void clearAll() {
        STATES.clear();
    }
}
