package com.aurasystem.rpg.network;

import com.aurasystem.rpg.config.AuraConfig;
import com.aurasystem.rpg.data.AuraData;
import com.aurasystem.rpg.data.ModAttachments;
import com.aurasystem.rpg.level.LevelCurve;
import com.aurasystem.rpg.power.PowerService;
import com.aurasystem.rpg.stats.StatEffects;
import com.aurasystem.rpg.stats.StatService;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.network.ServerPlayerEntity;

public final class ModNetworking {
    private ModNetworking() {}

    private static final Map<UUID, Payloads.Sync> LAST_SENT = new HashMap<>();

    public static void registerPayloads() {
        PayloadTypeRegistry.playS2C().register(Payloads.Sync.ID, Payloads.Sync.CODEC);
        PayloadTypeRegistry.playS2C().register(Payloads.CastFeedback.ID, Payloads.CastFeedback.CODEC);
        PayloadTypeRegistry.playC2S().register(Payloads.SpendStat.ID, Payloads.SpendStat.CODEC);
        PayloadTypeRegistry.playC2S().register(Payloads.CastPower.ID, Payloads.CastPower.CODEC);
        PayloadTypeRegistry.playC2S().register(Payloads.ResetStats.ID, Payloads.ResetStats.CODEC);
    }

    public static void registerServerReceivers() {
        ServerPlayNetworking.registerGlobalReceiver(Payloads.SpendStat.ID,
                (payload, context) -> StatService.spendPoint(context.player(), payload.stat()));
        ServerPlayNetworking.registerGlobalReceiver(Payloads.CastPower.ID,
                (payload, context) -> PowerService.tryCast(context.player(), payload.power()));
        ServerPlayNetworking.registerGlobalReceiver(Payloads.ResetStats.ID,
                (payload, context) -> StatService.resetStats(context.player()));
    }

    public static Payloads.Sync build(AuraData d) {
        AuraConfig c = AuraConfig.get();
        long xpNext = d.level >= c.maxLevel ? 0L : LevelCurve.xpToNext(d.level);
        return new Payloads.Sync(
                d.level, d.xp, xpNext, d.aura, d.lifetime, d.statPoints, List.copyOf(d.stats),
                (int) Math.floor(Math.max(0.0, d.energy)), (int) Math.floor(StatEffects.maxEnergy(d)),
                d.rank, d.trials, StatService.resetCost(d), c.maxLevel, c.statCap);
    }

    /** Envia el estado al jugador. Con force=false solo si algo cambio desde el ultimo envio. */
    public static void sync(ServerPlayerEntity player, boolean force) {
        if (!ServerPlayNetworking.canSend(player, Payloads.Sync.ID)) {
            return;
        }
        Payloads.Sync snapshot = build(ModAttachments.get(player));
        if (!force && snapshot.equals(LAST_SENT.get(player.getUuid()))) {
            return;
        }
        LAST_SENT.put(player.getUuid(), snapshot);
        ServerPlayNetworking.send(player, snapshot);
    }

    public static void forget(UUID id) {
        LAST_SENT.remove(id);
    }

    public static void clearAll() {
        LAST_SENT.clear();
    }
}
