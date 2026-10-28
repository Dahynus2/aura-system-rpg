package com.aurasystem.rpg.rank;

import com.aurasystem.rpg.data.AuraData;

/**
 * Rangos. Del 5 en adelante exigen pruebas de ascension (trialsRequired).
 * Las pruebas jugables llegan en la etapa 3; mientras tanto se otorgan con /aura admin trial.
 */
public enum Rank {
    INICIADO("Iniciado", 1, 0L, 0),
    DESPERTADO("Despertado", 5, 1000L, 0),
    APRENDIZ("Aprendiz del aura", 10, 5000L, 0),
    COMBATIENTE("Combatiente", 15, 13500L, 0),
    EXPERTO("Experto", 22, 38000L, 1),
    MAESTRO("Maestro", 30, 89000L, 2),
    ASCENDIDO("Ascendido", 38, 172000L, 3),
    TRASCENDENTE("Trascendente", 44, 262000L, 4),
    LEGENDARIO("Legendario", 48, 336000L, 5),
    DIVINO("Divino", 50, 377000L, 6);

    public final String label;
    public final int minLevel;
    public final long minLifetimeAura;
    public final int trialsRequired;

    Rank(String label, int minLevel, long minLifetimeAura, int trialsRequired) {
        this.label = label;
        this.minLevel = minLevel;
        this.minLifetimeAura = minLifetimeAura;
        this.trialsRequired = trialsRequired;
    }

    public int requiredLevel(int maxLevel) {
        return Math.min(minLevel, maxLevel);
    }

    public boolean meets(AuraData d, int maxLevel) {
        return d.level >= requiredLevel(maxLevel)
                && d.lifetime >= minLifetimeAura
                && d.trials >= trialsRequired;
    }

    public static Rank byIndex(int i) {
        Rank[] all = values();
        return all[Math.max(0, Math.min(all.length - 1, i))];
    }

    /** Indice del mayor rango cuyos requisitos (y los de todos los anteriores) se cumplen. */
    public static int highestEligible(AuraData d, int maxLevel) {
        int best = 0;
        for (Rank r : values()) {
            if (r.meets(d, maxLevel)) {
                best = r.ordinal();
            } else {
                break;
            }
        }
        return best;
    }
}
