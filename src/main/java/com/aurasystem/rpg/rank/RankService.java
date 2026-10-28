package com.aurasystem.rpg.rank;

import com.aurasystem.rpg.config.AuraConfig;
import com.aurasystem.rpg.data.AuraData;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public final class RankService {
    private RankService() {}

    /** Sube de rango si se cumplen los requisitos. Nunca baja de rango. */
    public static void refresh(ServerPlayerEntity player, AuraData d) {
        int best = Rank.highestEligible(d, AuraConfig.get().maxLevel);
        if (best > d.rank) {
            d.rank = best;
            Rank r = Rank.byIndex(best);
            player.sendMessage(Text.literal("✦ ¡Has ascendido al rango " + r.label + "!").formatted(Formatting.AQUA), false);
            player.getEntityWorld().playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundCategory.PLAYERS, 0.9f, 1.0f);
        }
    }
}
