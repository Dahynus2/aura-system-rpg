package com.aurasystem.rpg.power;

import com.aurasystem.rpg.data.AuraData;
import com.aurasystem.rpg.data.ModAttachments;
import com.aurasystem.rpg.network.Payloads;
import com.aurasystem.rpg.stats.StatEffects;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/** Validacion y consumo de energia/enfriamiento de los poderes. Todo en el servidor. */
public final class PowerService {
    private PowerService() {}

    private static final Map<UUID, Map<String, Integer>> COOLDOWN_END = new HashMap<>();

    public static void tryCast(ServerPlayerEntity player, String powerId) {
        if (!player.isAlive() || player.isSpectator()) {
            return;
        }
        AuraPower power = Powers.get(powerId);
        if (power == null) {
            return;
        }
        AuraData d = ModAttachments.get(player);
        if (d.level < power.minLevel()) {
            player.sendMessage(Text.literal("Necesitas nivel " + power.minLevel() + " para usar este poder.")
                    .formatted(Formatting.RED), true);
            return;
        }

        int now = player.getEntityWorld().getServer().getTicks();
        Map<String, Integer> cds = COOLDOWN_END.computeIfAbsent(player.getUuid(), k -> new HashMap<>());
        int end = cds.getOrDefault(power.id(), 0);
        if (now < end) {
            return; // en enfriamiento: se ignora en silencio (evita spam de mensajes)
        }

        double cost = power.energyCost() * StatEffects.costMultiplier(d);
        if (d.energy < cost) {
            player.sendMessage(Text.literal("Energía insuficiente (" + (int) Math.ceil(cost) + ").")
                    .formatted(Formatting.RED), true);
            return;
        }

        d.energy -= cost;
        int cooldown = Math.max(1, (int) Math.ceil(power.cooldownTicks() * StatEffects.cooldownMultiplier(d)));
        cds.put(power.id(), now + cooldown);

        float damage = (float) (power.baseDamage() * StatEffects.powerMultiplier(d));
        if (power == Powers.AURA_BOLT) {
            AuraBoltManager.launch(player, power.range(), damage);
        }

        if (ServerPlayNetworking.canSend(player, Payloads.CastFeedback.ID)) {
            ServerPlayNetworking.send(player, new Payloads.CastFeedback(power.id(), cooldown));
        }
    }

    public static void forget(UUID id) {
        COOLDOWN_END.remove(id);
    }

    public static void clearAll() {
        COOLDOWN_END.clear();
    }
}
