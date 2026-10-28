package com.aurasystem.rpg.client;

import com.aurasystem.rpg.AuraSystemMod;
import com.aurasystem.rpg.network.Payloads;
import com.aurasystem.rpg.power.Powers;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

public class AuraSystemClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ClientSettings.load();
        ClientKeys.register();

        ClientPlayNetworking.registerGlobalReceiver(Payloads.Sync.ID,
                (payload, context) -> ClientAuraState.update(payload));
        ClientPlayNetworking.registerGlobalReceiver(Payloads.CastFeedback.ID,
                (payload, context) -> ClientAuraState.setCooldown(payload.power(), payload.cooldownTicks()));
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> ClientAuraState.clear());

        ClientTickEvents.END_CLIENT_TICK.register(AuraSystemClient::onClientTick);
        HudElementRegistry.addLast(AuraSystemMod.id("hud"), AuraHud::render);
        ScreenEvents.AFTER_INIT.register(AuraSystemClient::onScreenInit);
    }

    private static void onClientTick(MinecraftClient client) {
        if (client.world != null) {
            ClientAuraState.ticks++;
        }
        while (ClientKeys.OPEN_MENU.wasPressed()) {
            if (client.player != null && client.currentScreen == null) {
                client.setScreen(new AuraScreen());
            }
        }
        while (ClientKeys.CAST_BOLT.wasPressed()) {
            if (client.player != null && client.currentScreen == null) {
                ClientPlayNetworking.send(new Payloads.CastPower(Powers.AURA_BOLT.id()));
            }
        }
    }

    /** Boton "Aura" sobre el inventario. */
    private static void onScreenInit(MinecraftClient client, Screen screen, int scaledWidth, int scaledHeight) {
        if (screen instanceof InventoryScreen) {
            int left = (scaledWidth - 176) / 2;
            int top = (scaledHeight - 166) / 2;
            ButtonWidget button = ButtonWidget.builder(Text.literal("Aura"), b -> client.setScreen(new AuraScreen()))
                    .dimensions(left + 176 - 44, Math.max(2, top - 22), 44, 18)
                    .build();
            Screens.getButtons(screen).add(button);
        }
    }
}
