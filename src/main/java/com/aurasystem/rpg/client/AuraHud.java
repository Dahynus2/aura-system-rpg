package com.aurasystem.rpg.client;

import com.aurasystem.rpg.network.Payloads;
import com.aurasystem.rpg.power.Powers;
import com.aurasystem.rpg.rank.Rank;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;

/** HUD compacto en la esquina superior izquierda: nivel/rango, XP, aura, energia y enfriamiento. */
public final class AuraHud {
    private AuraHud() {}

    private static final int BAR_W = 110;

    public static void render(DrawContext ctx, RenderTickCounter tickCounter) {
        MinecraftClient mc = MinecraftClient.getInstance();
        Payloads.Sync s = ClientAuraState.get();
        if (s == null || mc.player == null || mc.options.hudHidden || !ClientSettings.hudEnabled) {
            return;
        }
        if (mc.currentScreen instanceof AuraScreen) {
            return;
        }
        TextRenderer tr = mc.textRenderer;
        int x = 6;
        int y = 6;

        ctx.drawTextWithShadow(tr, "Nv." + s.level() + " · " + Rank.byIndex(s.rank()).label, x, y, 0xFFFFFFFF);

        // Barra de experiencia
        double xpRatio = s.xpNext() > 0 ? Math.min(1.0, (double) s.xp() / s.xpNext()) : 1.0;
        ctx.fill(x, y + 11, x + BAR_W, y + 14, 0xAA000000);
        ctx.fill(x, y + 11, x + (int) (BAR_W * xpRatio), y + 14, 0xFF55FFFF);

        ctx.drawTextWithShadow(tr, "Aura: " + s.aura(), x, y + 17, 0xFFFFD84A);

        // Barra de energia
        double eRatio = s.maxEnergy() > 0 ? Math.min(1.0, (double) s.energy() / s.maxEnergy()) : 0.0;
        ctx.fill(x, y + 29, x + BAR_W, y + 37, 0xAA000000);
        ctx.fill(x, y + 29, x + (int) (BAR_W * eRatio), y + 37, 0xFF35A7FF);
        ctx.drawTextWithShadow(tr, s.energy() + "/" + s.maxEnergy(), x + 3, y + 29, 0xFFFFFFFF);

        // Poder equipado y enfriamiento
        long cd = ClientAuraState.cooldownTicksLeft(Powers.AURA_BOLT.id());
        String key = ClientKeys.CAST_BOLT == null ? "R" : ClientKeys.CAST_BOLT.getBoundKeyLocalizedText().getString();
        String state = cd > 0 ? String.format("%.1fs", cd / 20.0) : "listo";
        int color = cd > 0 ? 0xFFFF7777 : 0xFF77FF77;
        ctx.drawTextWithShadow(tr, Powers.AURA_BOLT.name() + " [" + key + "]: " + state, x, y + 41, color);

        if (s.statPoints() > 0) {
            ctx.drawTextWithShadow(tr, "¡" + s.statPoints() + " punto(s) por asignar!", x, y + 53, 0xFFFFAA33);
        }

        // Aviso flotante de aura ganada
        long age = ClientAuraState.ticks - ClientAuraState.lastGainTick;
        if (age >= 0 && age < 50) {
            int alpha = Math.max(16, (int) (255 * (1.0 - age / 50.0)));
            ctx.drawTextWithShadow(tr, "+" + ClientAuraState.lastGain + " aura", x + BAR_W + 8, y + 17, (alpha << 24) | 0xFFD84A);
        }
    }
}
