package com.aurasystem.rpg.aura;

import com.aurasystem.rpg.config.AuraConfig;
import com.aurasystem.rpg.data.AuraData;
import com.aurasystem.rpg.data.ModAttachments;
import com.aurasystem.rpg.level.LevelCurve;
import com.aurasystem.rpg.rank.RankService;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/** Unico punto de entrada para dar aura/experiencia. Todo ocurre en el servidor. */
public final class AuraService {
    private AuraService() {}

    /** Aura con rendimientos decrecientes y bonus por variedad. */
    public static long grant(ServerPlayerEntity player, Activity activity, double raw) {
        return grantInternal(player, activity, raw, true);
    }

    /** Aura de un solo uso (p. ej. bioma nuevo): sin fatiga, pero cuenta para la variedad. */
    public static long grantFlat(ServerPlayerEntity player, Activity activity, double raw) {
        return grantInternal(player, activity, raw, false);
    }

    private static long grantInternal(ServerPlayerEntity player, Activity activity, double raw, boolean fatigue) {
        if (raw <= 0 || player.isSpectator()) {
            return 0;
        }
        long now = player.getEntityWorld().getServer().getTicks();
        double amount = raw
                * ActivityTracker.evaluate(player.getUuid(), activity, raw, now, fatigue)
                * AuraConfig.get().globalAuraMultiplier;
        long whole = ActivityTracker.takeWhole(player.getUuid(), amount);
        if (whole <= 0) {
            return 0;
        }
        addAura(player, ModAttachments.get(player), whole);
        return whole;
    }

    /** Suma aura sin pasar por el antiexploit (comandos de administracion, recompensas fijas). */
    public static void addAura(ServerPlayerEntity player, AuraData d, long amount) {
        if (amount <= 0) {
            return;
        }
        d.aura += amount;
        d.lifetime += amount;
        addXp(player, d, amount);
        RankService.refresh(player, d);
        ModAttachments.save(player, d);
    }

    private static void addXp(ServerPlayerEntity player, AuraData d, long amount) {
        int max = AuraConfig.get().maxLevel;
        if (d.level >= max) {
            d.xp = 0;
            return;
        }
        d.xp += amount;
        int gainedPoints = 0;
        int startLevel = d.level;
        while (d.level < max && d.xp >= LevelCurve.xpToNext(d.level)) {
            d.xp -= LevelCurve.xpToNext(d.level);
            d.level++;
            int pts = LevelCurve.statPointsFor(d.level);
            d.statPoints += pts;
            gainedPoints += pts;
        }
        if (d.level >= max) {
            d.xp = 0;
        }
        if (d.level > startLevel) {
            player.sendMessage(Text.literal("★ ¡Nivel " + d.level + "! +" + gainedPoints
                    + " punto(s) de estadística (abre el menú con K)").formatted(Formatting.GOLD), false);
            player.getEntityWorld().playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.ENTITY_PLAYER_LEVELUP, SoundCategory.PLAYERS, 0.8f, 1.0f);
        }
    }
}
