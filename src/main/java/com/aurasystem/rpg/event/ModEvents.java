package com.aurasystem.rpg.event;

import com.aurasystem.rpg.aura.Activity;
import com.aurasystem.rpg.aura.ActivityTracker;
import com.aurasystem.rpg.aura.AuraService;
import com.aurasystem.rpg.data.AuraData;
import com.aurasystem.rpg.data.ModAttachments;
import com.aurasystem.rpg.network.ModNetworking;
import com.aurasystem.rpg.power.AuraBoltManager;
import com.aurasystem.rpg.power.PowerService;
import com.aurasystem.rpg.stats.StatEffects;
import com.aurasystem.rpg.stats.StatService;
import java.util.Optional;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.boss.WitherEntity;
import net.minecraft.entity.boss.dragon.EnderDragonEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.ElderGuardianEntity;
import net.minecraft.entity.mob.Monster;
import net.minecraft.entity.mob.WardenEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Util;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;

public final class ModEvents {
    private ModEvents() {}

    public static void register() {
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayerEntity player = handler.player;
            AuraData d = ModAttachments.get(player);
            d.normalize();
            ModAttachments.save(player, d);
            StatService.applyAttributes(player, d);
            ModNetworking.sync(player, true);
        });

        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            java.util.UUID id = handler.player.getUuid();
            ModNetworking.forget(id);
            TrainingTracker.forget(id);
            PowerService.forget(id);
        });

        // Tambien se dispara al volver del End: los atributos del jugador nuevo estan vacios.
        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> {
            AuraData d = ModAttachments.get(newPlayer);
            StatService.applyAttributes(newPlayer, d);
            if (!alive) {
                newPlayer.setHealth(newPlayer.getMaxHealth());
            }
            ModNetworking.sync(newPlayer, true);
        });

        ServerTickEvents.END_SERVER_TICK.register(ModEvents::onServerTick);
        ServerLivingEntityEvents.AFTER_DEATH.register(ModEvents::onEntityDeath);
        PlayerBlockBreakEvents.AFTER.register(ModEvents::onBlockBreak);

        // El estado estatico no debe sobrevivir entre mundos de un jugador (servidor integrado).
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
            AuraBoltManager.clearAll();
            PowerService.clearAll();
            ModNetworking.clearAll();
            TrainingTracker.clearAll();
            ActivityTracker.clearAll();
        });
    }

    private static void onServerTick(MinecraftServer server) {
        int tick = server.getTicks();
        AuraBoltManager.tick(server);

        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            AuraData d = ModAttachments.get(player);

            double max = StatEffects.maxEnergy(d);
            if (d.energy < 0) {
                d.energy = max;
            } else if (d.energy > max) {
                d.energy = max;
            } else if (d.energy < max && player.isAlive()) {
                d.energy = Math.min(max, d.energy + StatEffects.energyRegenPerSecond(d) / 20.0);
            }

            if (player.isAlive() && !player.isSpectator()) {
                TrainingTracker.tick(player);
                if (tick % 40 == 0) {
                    checkExploration(player, d);
                }
            }
            if (tick % 200 == 0) {
                StatService.applyAttributes(player, d);
            }
            if (tick % 5 == 0) {
                ModNetworking.sync(player, false);
            }
        }
    }

    private static void checkExploration(ServerPlayerEntity player, AuraData d) {
        RegistryEntry<Biome> entry = player.getEntityWorld().getBiome(player.getBlockPos());
        Optional<RegistryKey<Biome>> key = entry.getKey();
        if (key.isEmpty()) {
            return;
        }
        net.minecraft.util.Identifier biomeId = key.get().getValue();
        String id = biomeId.toString();
        if (d.visitedBiomes.contains(id)) {
            return;
        }
        d.visitedBiomes.add(id);
        ModAttachments.save(player, d);
        long gained = AuraService.grantFlat(player, Activity.EXPLORATION, 30.0);
        player.sendMessage(Text.literal("Bioma descubierto: ").formatted(Formatting.GREEN)
                .append(Text.translatable(Util.createTranslationKey("biome", biomeId)).formatted(Formatting.WHITE))
                .append(Text.literal(gained > 0 ? "  (+" + gained + " aura)" : "").formatted(Formatting.GRAY)), true);
    }

    private static void onEntityDeath(LivingEntity entity, DamageSource source) {
        if (!(source.getAttacker() instanceof ServerPlayerEntity player)) {
            return;
        }
        if (entity == player || entity instanceof PlayerEntity) {
            return;
        }
        double bossValue = bossValue(entity);
        if (bossValue > 0) {
            AuraService.grant(player, Activity.BOSS, bossValue);
        } else if (entity instanceof Monster) {
            double raw = Math.max(1.0, Math.min(60.0, entity.getMaxHealth() * 0.4));
            AuraService.grant(player, Activity.COMBAT, raw);
        }
    }

    private static double bossValue(LivingEntity e) {
        if (e instanceof EnderDragonEntity) {
            return 600.0;
        }
        if (e instanceof WitherEntity) {
            return 450.0;
        }
        if (e instanceof WardenEntity) {
            return 350.0;
        }
        if (e instanceof ElderGuardianEntity) {
            return 120.0;
        }
        return 0.0;
    }

    private static void onBlockBreak(World world, PlayerEntity player, BlockPos pos, BlockState state, BlockEntity blockEntity) {
        if (!(player instanceof ServerPlayerEntity sp) || sp.isCreative() || sp.isSpectator()) {
            return;
        }
        double raw = MiningRewards.valueOf(state);
        if (raw > 0) {
            AuraService.grant(sp, Activity.MINING, raw);
        }
    }
}
