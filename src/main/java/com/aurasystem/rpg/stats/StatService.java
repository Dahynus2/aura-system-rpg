package com.aurasystem.rpg.stats;

import com.aurasystem.rpg.AuraSystemMod;
import com.aurasystem.rpg.config.AuraConfig;
import com.aurasystem.rpg.data.AuraData;
import com.aurasystem.rpg.data.ModAttachments;
import com.aurasystem.rpg.network.ModNetworking;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

/** Gasto/reinicio de puntos (validado en servidor) y aplicacion de las estadisticas como atributos. */
public final class StatService {
    private StatService() {}

    public static void spendPoint(ServerPlayerEntity player, int statIndex) {
        if (statIndex < 0 || statIndex >= AuraData.STAT_COUNT) {
            return;
        }
        AuraData d = ModAttachments.get(player);
        if (d.statPoints <= 0) {
            player.sendMessage(Text.literal("No tienes puntos de estadística.").formatted(Formatting.RED), true);
            return;
        }
        int current = d.stats.get(statIndex);
        if (current >= AuraConfig.get().statCap) {
            player.sendMessage(Text.literal("Esa estadística ya está al máximo.").formatted(Formatting.RED), true);
            return;
        }
        d.stats.set(statIndex, current + 1);
        d.statPoints--;
        ModAttachments.save(player, d);
        applyAttributes(player, d);
        ModNetworking.sync(player, true);
    }

    public static long resetCost(AuraData d) {
        AuraConfig c = AuraConfig.get();
        return c.resetStatsBaseCost + c.resetStatsCostStep * d.resets;
    }

    /** Reinicia las estadisticas pagando aura. El coste sube con cada reinicio. */
    public static void resetStats(ServerPlayerEntity player) {
        AuraData d = ModAttachments.get(player);
        int spent = d.spentPoints();
        if (spent <= 0) {
            player.sendMessage(Text.literal("No tienes puntos invertidos.").formatted(Formatting.RED), true);
            return;
        }
        long cost = resetCost(d);
        if (d.aura < cost) {
            player.sendMessage(Text.literal("Necesitas " + cost + " de aura para reiniciar (tienes " + d.aura + ").")
                    .formatted(Formatting.RED), true);
            return;
        }
        d.aura -= cost;
        d.resets++;
        for (int i = 0; i < d.stats.size(); i++) {
            d.stats.set(i, 0);
        }
        d.statPoints += spent;
        ModAttachments.save(player, d);
        applyAttributes(player, d);
        ModNetworking.sync(player, true);
        player.sendMessage(Text.literal("Estadísticas reiniciadas (-" + cost + " aura). Recuperas " + spent + " punto(s).")
                .formatted(Formatting.YELLOW), false);
    }

    /** Aplica las estadisticas como modificadores TEMPORALES (no se guardan; se reaplican al entrar/reaparecer). */
    public static void applyAttributes(ServerPlayerEntity player, AuraData d) {
        double str = StatEffects.eff(d, StatType.STRENGTH);
        double vit = StatEffects.eff(d, StatType.VITALITY);
        double res = StatEffects.eff(d, StatType.RESISTANCE);
        double agi = StatEffects.eff(d, StatType.AGILITY);

        apply(player, EntityAttributes.ATTACK_DAMAGE, "strength",
                StatEffects.MELEE_PER_POINT * str, EntityAttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        apply(player, EntityAttributes.MAX_HEALTH, "vitality",
                StatEffects.HEALTH_PER_POINT * vit, EntityAttributeModifier.Operation.ADD_VALUE);
        apply(player, EntityAttributes.ARMOR, "resistance_armor",
                StatEffects.ARMOR_PER_POINT * res, EntityAttributeModifier.Operation.ADD_VALUE);
        apply(player, EntityAttributes.KNOCKBACK_RESISTANCE, "resistance_knockback",
                StatEffects.KNOCKBACK_PER_POINT * res, EntityAttributeModifier.Operation.ADD_VALUE);
        apply(player, EntityAttributes.MOVEMENT_SPEED, "agility_move",
                StatEffects.MOVE_PER_POINT * agi, EntityAttributeModifier.Operation.ADD_MULTIPLIED_BASE);
        apply(player, EntityAttributes.ATTACK_SPEED, "agility_attack",
                StatEffects.ATTACK_SPEED_PER_POINT * agi, EntityAttributeModifier.Operation.ADD_MULTIPLIED_BASE);

        if (player.getHealth() > player.getMaxHealth()) {
            player.setHealth(player.getMaxHealth());
        }
    }

    private static void apply(ServerPlayerEntity player, RegistryEntry<EntityAttribute> attribute, String name,
                              double value, EntityAttributeModifier.Operation operation) {
        EntityAttributeInstance instance = player.getAttributeInstance(attribute);
        if (instance == null) {
            return;
        }
        Identifier id = AuraSystemMod.id("stat_" + name);
        instance.removeModifier(id);
        if (value != 0.0) {
            instance.addTemporaryModifier(new EntityAttributeModifier(id, value, operation));
        }
    }
}
