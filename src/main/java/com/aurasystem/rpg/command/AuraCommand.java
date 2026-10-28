package com.aurasystem.rpg.command;

import com.aurasystem.rpg.aura.AuraService;
import com.aurasystem.rpg.data.AuraData;
import com.aurasystem.rpg.data.ModAttachments;
import com.aurasystem.rpg.network.ModNetworking;
import com.aurasystem.rpg.rank.Rank;
import com.aurasystem.rpg.rank.RankService;
import com.aurasystem.rpg.stats.StatService;
import com.aurasystem.rpg.stats.StatType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

public final class AuraCommand {
    private AuraCommand() {}

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                dispatcher.register(CommandManager.literal("aura")
                        .executes(ctx -> info(ctx.getSource()))
                        .then(CommandManager.literal("admin")
                                .requires(source -> source.hasPermissionLevel(2))
                                .then(CommandManager.literal("give")
                                        .then(CommandManager.argument("target", EntityArgumentType.player())
                                                .then(CommandManager.argument("amount", IntegerArgumentType.integer(1, 10_000_000))
                                                        .executes(ctx -> {
                                                            ServerPlayerEntity t = EntityArgumentType.getPlayer(ctx, "target");
                                                            int amount = IntegerArgumentType.getInteger(ctx, "amount");
                                                            AuraService.addAura(t, ModAttachments.get(t), amount);
                                                            ModNetworking.sync(t, true);
                                                            ctx.getSource().sendFeedback(() -> Text.literal("+" + amount + " aura para " + t.getName().getString()), true);
                                                            return 1;
                                                        }))))
                                .then(CommandManager.literal("trial")
                                        .then(CommandManager.argument("target", EntityArgumentType.player())
                                                .executes(ctx -> {
                                                    ServerPlayerEntity t = EntityArgumentType.getPlayer(ctx, "target");
                                                    AuraData d = ModAttachments.get(t);
                                                    d.trials++;
                                                    RankService.refresh(t, d);
                                                    ModAttachments.save(t, d);
                                                    ModNetworking.sync(t, true);
                                                    ctx.getSource().sendFeedback(() -> Text.literal("Prueba de ascensión otorgada (" + d.trials + ") a " + t.getName().getString()), true);
                                                    return 1;
                                                })))
                                .then(CommandManager.literal("reset")
                                        .then(CommandManager.argument("target", EntityArgumentType.player())
                                                .executes(ctx -> {
                                                    ServerPlayerEntity t = EntityArgumentType.getPlayer(ctx, "target");
                                                    AuraData fresh = new AuraData();
                                                    ModAttachments.save(t, fresh);
                                                    StatService.applyAttributes(t, fresh);
                                                    ModNetworking.sync(t, true);
                                                    ctx.getSource().sendFeedback(() -> Text.literal("Progreso de " + t.getName().getString() + " reiniciado."), true);
                                                    return 1;
                                                }))))));
    }

    private static int info(ServerCommandSource source) throws CommandSyntaxException {
        ServerPlayerEntity player = source.getPlayerOrThrow();
        AuraData d = ModAttachments.get(player);
        source.sendFeedback(() -> Text.literal("— Aura System RPG —"), false);
        source.sendFeedback(() -> Text.literal("Nivel " + d.level + " · Rango " + Rank.byIndex(d.rank).label), false);
        source.sendFeedback(() -> Text.literal("Aura: " + d.aura + " (acumulada: " + d.lifetime + ") · Puntos libres: " + d.statPoints), false);
        StringBuilder sb = new StringBuilder();
        for (StatType t : StatType.values()) {
            sb.append(t.label).append(' ').append(d.stat(t)).append("  ");
        }
        source.sendFeedback(() -> Text.literal(sb.toString().trim()), false);
        source.sendFeedback(() -> Text.literal("Pulsa K para abrir el menú y R para lanzar el Proyectil de aura."), false);
        return 1;
    }
}
