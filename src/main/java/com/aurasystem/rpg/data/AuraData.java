package com.aurasystem.rpg.data;

import com.aurasystem.rpg.config.AuraConfig;
import com.aurasystem.rpg.stats.StatType;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.List;

/**
 * Progreso de un jugador. Se guarda como Data Attachment persistente (Fabric) en la entidad jugador,
 * por lo que sobrevive a morir, cerrar el juego, cambiar de dimension y reconectar.
 * Todos los campos son opcionales en el codec para poder anadir campos en futuras etapas sin romper partidas.
 */
public final class AuraData {
    public static final int STAT_COUNT = StatType.values().length;

    public static final Codec<AuraData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.optionalFieldOf("level", 1).forGetter(d -> d.level),
            Codec.LONG.optionalFieldOf("xp", 0L).forGetter(d -> d.xp),
            Codec.LONG.optionalFieldOf("aura", 0L).forGetter(d -> d.aura),
            Codec.LONG.optionalFieldOf("lifetime_aura", 0L).forGetter(d -> d.lifetime),
            Codec.INT.optionalFieldOf("stat_points", 0).forGetter(d -> d.statPoints),
            Codec.INT.listOf().optionalFieldOf("stats", List.of()).forGetter(d -> d.stats),
            Codec.DOUBLE.optionalFieldOf("energy", -1.0).forGetter(d -> d.energy),
            Codec.INT.optionalFieldOf("rank", 0).forGetter(d -> d.rank),
            Codec.INT.optionalFieldOf("trials", 0).forGetter(d -> d.trials),
            Codec.INT.optionalFieldOf("resets", 0).forGetter(d -> d.resets),
            Codec.STRING.listOf().optionalFieldOf("visited_biomes", List.of()).forGetter(d -> d.visitedBiomes)
    ).apply(instance, AuraData::new));

    public int level;
    /** Experiencia acumulada dentro del nivel actual. */
    public long xp;
    /** Aura gastable (reinicio de estadisticas, y habilidades en la etapa 2). */
    public long aura;
    /** Aura total ganada en la vida del personaje; se usa para los rangos. */
    public long lifetime;
    public int statPoints;
    public List<Integer> stats;
    /** Energia actual para poderes. Negativo = "llenar al maximo en el proximo tick". */
    public double energy;
    public int rank;
    public int trials;
    public int resets;
    public List<String> visitedBiomes;

    public AuraData() {
        this(1, 0L, 0L, 0L, 0, List.of(), -1.0, 0, 0, 0, List.of());
    }

    public AuraData(int level, long xp, long aura, long lifetime, int statPoints, List<Integer> stats,
                    double energy, int rank, int trials, int resets, List<String> visitedBiomes) {
        this.level = level;
        this.xp = xp;
        this.aura = aura;
        this.lifetime = lifetime;
        this.statPoints = statPoints;
        this.stats = new ArrayList<>(stats);
        this.energy = energy;
        this.rank = rank;
        this.trials = trials;
        this.resets = resets;
        this.visitedBiomes = new ArrayList<>(visitedBiomes);
        normalizeStructure();
    }

    private void normalizeStructure() {
        while (stats.size() < STAT_COUNT) {
            stats.add(0);
        }
        while (stats.size() > STAT_COUNT) {
            stats.remove(stats.size() - 1);
        }
    }

    public int stat(StatType type) {
        return stats.get(type.ordinal());
    }

    public int spentPoints() {
        int sum = 0;
        for (int v : stats) {
            sum += v;
        }
        return sum;
    }

    /** Corrige valores fuera de rango (por ejemplo si se cambio la config). Llamar al unirse. */
    public void normalize() {
        AuraConfig c = AuraConfig.get();
        normalizeStructure();
        level = Math.max(1, Math.min(level, c.maxLevel));
        xp = Math.max(0, xp);
        aura = Math.max(0, aura);
        lifetime = Math.max(0, lifetime);
        statPoints = Math.max(0, statPoints);
        rank = Math.max(0, Math.min(rank, com.aurasystem.rpg.rank.Rank.values().length - 1));
        trials = Math.max(0, trials);
        resets = Math.max(0, resets);
        for (int i = 0; i < stats.size(); i++) {
            int v = Math.max(0, stats.get(i));
            if (v > c.statCap) {
                statPoints += v - c.statCap; // se reembolsa el exceso
                v = c.statCap;
            }
            stats.set(i, v);
        }
    }
}
