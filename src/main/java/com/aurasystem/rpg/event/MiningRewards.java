package com.aurasystem.rpg.event;

import net.minecraft.block.Blocks;
import net.minecraft.block.BlockState;
import net.minecraft.registry.tag.BlockTags;

/** Aura bruta por romper menas. La fatiga (antiexploit) se aplica despues en AuraService. */
public final class MiningRewards {
    private MiningRewards() {}

    public static double valueOf(BlockState state) {
        if (state.isOf(Blocks.ANCIENT_DEBRIS)) {
            return 15.0;
        }
        if (state.isIn(BlockTags.DIAMOND_ORES) || state.isIn(BlockTags.EMERALD_ORES)) {
            return 8.0;
        }
        if (state.isIn(BlockTags.GOLD_ORES) || state.isIn(BlockTags.LAPIS_ORES)) {
            return 3.0;
        }
        if (state.isIn(BlockTags.IRON_ORES) || state.isIn(BlockTags.REDSTONE_ORES)) {
            return 2.0;
        }
        if (state.isIn(BlockTags.COPPER_ORES) || state.isIn(BlockTags.COAL_ORES)) {
            return 1.0;
        }
        return 0.0;
    }
}
