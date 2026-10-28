package com.aurasystem.rpg.network;

import com.aurasystem.rpg.AuraSystemMod;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;

/** Todos los paquetes del mod. Los C2S solo expresan intenciones; el servidor valida todo. */
public final class Payloads {
    private Payloads() {}

    // ---- S2C: estado completo del jugador (solo se envia si cambia) ----
    public record Sync(int level, long xp, long xpNext, long aura, long lifetime, int statPoints,
                       List<Integer> stats, int energy, int maxEnergy, int rank, int trials,
                       long resetCost, int maxLevel, int statCap) implements CustomPayload {

        public static final CustomPayload.Id<Sync> ID = new CustomPayload.Id<>(AuraSystemMod.id("sync"));

        public static final PacketCodec<RegistryByteBuf, Sync> CODEC = new PacketCodec<>() {
            @Override
            public Sync decode(RegistryByteBuf buf) {
                int level = buf.readVarInt();
                long xp = buf.readVarLong();
                long xpNext = buf.readVarLong();
                long aura = buf.readVarLong();
                long lifetime = buf.readVarLong();
                int statPoints = buf.readVarInt();
                int n = buf.readVarInt();
                if (n < 0 || n > 64) {
                    throw new IllegalArgumentException("Numero de estadisticas invalido: " + n);
                }
                List<Integer> stats = new ArrayList<>(n);
                for (int i = 0; i < n; i++) {
                    stats.add(buf.readVarInt());
                }
                int energy = buf.readVarInt();
                int maxEnergy = buf.readVarInt();
                int rank = buf.readVarInt();
                int trials = buf.readVarInt();
                long resetCost = buf.readVarLong();
                int maxLevel = buf.readVarInt();
                int statCap = buf.readVarInt();
                return new Sync(level, xp, xpNext, aura, lifetime, statPoints, List.copyOf(stats),
                        energy, maxEnergy, rank, trials, resetCost, maxLevel, statCap);
            }

            @Override
            public void encode(RegistryByteBuf buf, Sync v) {
                buf.writeVarInt(v.level());
                buf.writeVarLong(v.xp());
                buf.writeVarLong(v.xpNext());
                buf.writeVarLong(v.aura());
                buf.writeVarLong(v.lifetime());
                buf.writeVarInt(v.statPoints());
                buf.writeVarInt(v.stats().size());
                for (int s : v.stats()) {
                    buf.writeVarInt(s);
                }
                buf.writeVarInt(v.energy());
                buf.writeVarInt(v.maxEnergy());
                buf.writeVarInt(v.rank());
                buf.writeVarInt(v.trials());
                buf.writeVarLong(v.resetCost());
                buf.writeVarInt(v.maxLevel());
                buf.writeVarInt(v.statCap());
            }
        };

        @Override
        public CustomPayload.Id<? extends CustomPayload> getId() {
            return ID;
        }
    }

    // ---- S2C: confirmacion de lanzamiento (para mostrar el enfriamiento en el HUD) ----
    public record CastFeedback(String power, int cooldownTicks) implements CustomPayload {
        public static final CustomPayload.Id<CastFeedback> ID = new CustomPayload.Id<>(AuraSystemMod.id("cast_feedback"));
        public static final PacketCodec<RegistryByteBuf, CastFeedback> CODEC = PacketCodec.tuple(
                PacketCodecs.STRING, CastFeedback::power,
                PacketCodecs.VAR_INT, CastFeedback::cooldownTicks,
                CastFeedback::new);

        @Override
        public CustomPayload.Id<? extends CustomPayload> getId() {
            return ID;
        }
    }

    // ---- C2S: gastar un punto en una estadistica ----
    public record SpendStat(int stat) implements CustomPayload {
        public static final CustomPayload.Id<SpendStat> ID = new CustomPayload.Id<>(AuraSystemMod.id("spend_stat"));
        public static final PacketCodec<RegistryByteBuf, SpendStat> CODEC = PacketCodec.tuple(
                PacketCodecs.VAR_INT, SpendStat::stat,
                SpendStat::new);

        @Override
        public CustomPayload.Id<? extends CustomPayload> getId() {
            return ID;
        }
    }

    // ---- C2S: lanzar un poder ----
    public record CastPower(String power) implements CustomPayload {
        public static final CustomPayload.Id<CastPower> ID = new CustomPayload.Id<>(AuraSystemMod.id("cast_power"));
        public static final PacketCodec<RegistryByteBuf, CastPower> CODEC = PacketCodec.tuple(
                PacketCodecs.STRING, CastPower::power,
                CastPower::new);

        @Override
        public CustomPayload.Id<? extends CustomPayload> getId() {
            return ID;
        }
    }

    // ---- C2S: reiniciar estadisticas (cuesta aura) ----
    public record ResetStats() implements CustomPayload {
        public static final CustomPayload.Id<ResetStats> ID = new CustomPayload.Id<>(AuraSystemMod.id("reset_stats"));
        public static final PacketCodec<RegistryByteBuf, ResetStats> CODEC = new PacketCodec<>() {
            @Override
            public ResetStats decode(RegistryByteBuf buf) {
                return new ResetStats();
            }

            @Override
            public void encode(RegistryByteBuf buf, ResetStats value) {
            }
        };

        @Override
        public CustomPayload.Id<? extends CustomPayload> getId() {
            return ID;
        }
    }
}
