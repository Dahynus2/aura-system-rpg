package com.aurasystem.rpg.client;

import com.aurasystem.rpg.network.Payloads;
import com.aurasystem.rpg.rank.Rank;
import com.aurasystem.rpg.stats.StatType;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;

/** Pantalla de estadisticas (etapa 1). El arbol de habilidades llega en la etapa 2. */
public class AuraScreen extends Screen {
    private static final int W = 340;
    private static final int H = 280;
    private static final int ROW_H = 16;

    private final List<ButtonWidget> plusButtons = new ArrayList<>();
    private ButtonWidget resetButton;
    private ButtonWidget hudButton;

    public AuraScreen() {
        super(Text.literal("Aura System RPG"));
    }

    private int left() {
        return (this.width - W) / 2;
    }

    private int top() {
        return (this.height - H) / 2;
    }

    @Override
    protected void init() {
        plusButtons.clear();
        int left = left();
        int top = top();

        for (StatType st : StatType.values()) {
            int y = top + 92 + st.ordinal() * ROW_H;
            ButtonWidget plus = ButtonWidget.builder(Text.literal("+"),
                            b -> ClientPlayNetworking.send(new Payloads.SpendStat(st.ordinal())))
                    .dimensions(left + W - 28, y - 2, 18, 14)
                    .build();
            plusButtons.add(this.addDrawableChild(plus));
        }

        resetButton = this.addDrawableChild(ButtonWidget.builder(Text.literal("Reiniciar estadísticas"),
                        b -> ClientPlayNetworking.send(new Payloads.ResetStats()))
                .dimensions(left + 10, top + H - 24, 130, 18).build());

        hudButton = this.addDrawableChild(ButtonWidget.builder(hudLabel(), b -> {
                    ClientSettings.hudEnabled = !ClientSettings.hudEnabled;
                    ClientSettings.save();
                    b.setMessage(hudLabel());
                })
                .dimensions(left + 148, top + H - 24, 100, 18).build());

        this.addDrawableChild(ButtonWidget.builder(Text.literal("Cerrar"), b -> this.close())
                .dimensions(left + W - 80, top + H - 24, 70, 18).build());
    }

    private static Text hudLabel() {
        return Text.literal("HUD: " + (ClientSettings.hudEnabled ? "SÍ" : "NO"));
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float deltaTicks) {
        Payloads.Sync s = ClientAuraState.get();
        for (int i = 0; i < plusButtons.size(); i++) {
            plusButtons.get(i).active = s != null && s.statPoints() > 0
                    && i < s.stats().size() && s.stats().get(i) < s.statCap();
        }
        resetButton.active = s != null && s.stats().stream().anyMatch(v -> v > 0);

        super.render(ctx, mouseX, mouseY, deltaTicks);

        TextRenderer tr = this.textRenderer;
        int left = left();
        int top = top();
        int cx = this.width / 2;

        ctx.drawCenteredTextWithShadow(tr, Text.literal("Aura System RPG"), cx, top + 4, 0xFFFFD84A);
        if (s == null) {
            ctx.drawCenteredTextWithShadow(tr, Text.literal("Sin datos del servidor todavía..."), cx, top + 30, 0xFFAAAAAA);
            return;
        }

        Rank rank = Rank.byIndex(s.rank());
        ctx.drawTextWithShadow(tr, "Nivel " + s.level() + " / " + s.maxLevel() + "  ·  Rango: " + rank.label,
                left + 10, top + 18, 0xFFFFFFFF);

        double xpRatio = s.xpNext() > 0 ? Math.min(1.0, (double) s.xp() / s.xpNext()) : 1.0;
        ctx.fill(left + 10, top + 30, left + W - 10, top + 36, 0xAA000000);
        ctx.fill(left + 10, top + 30, left + 10 + (int) ((W - 20) * xpRatio), top + 36, 0xFF55FFFF);
        String xpText = s.xpNext() > 0 ? s.xp() + " / " + s.xpNext() + " XP" : "Nivel máximo";
        ctx.drawTextWithShadow(tr, xpText, left + 10, top + 39, 0xFFAADDFF);

        ctx.drawTextWithShadow(tr, "Aura: " + s.aura() + "   (acumulada: " + s.lifetime() + ")",
                left + 10, top + 51, 0xFFFFD84A);
        ctx.drawTextWithShadow(tr, "Energía: " + s.energy() + " / " + s.maxEnergy()
                + "      Puntos libres: " + s.statPoints(), left + 10, top + 63, 0xFF8CC8FF);

        if (s.rank() + 1 < Rank.values().length) {
            Rank next = Rank.byIndex(s.rank() + 1);
            String req = "Siguiente rango (" + next.label + "): nivel " + next.requiredLevel(s.maxLevel())
                    + ", " + next.minLifetimeAura + " aura acumulada"
                    + (next.trialsRequired > 0 ? ", pruebas " + s.trials() + "/" + next.trialsRequired : "");
            ctx.drawTextWithShadow(tr, req, left + 10, top + 75, 0xFFCCCCCC);
        }

        // Estadisticas
        int hovered = -1;
        for (StatType st : StatType.values()) {
            int i = st.ordinal();
            int y = top + 92 + i * ROW_H;
            int value = i < s.stats().size() ? s.stats().get(i) : 0;

            if (mouseX >= left + 10 && mouseX <= left + W - 10 && mouseY >= y - 3 && mouseY < y + ROW_H - 3) {
                hovered = i;
                ctx.fill(left + 8, y - 3, left + W - 8, y + ROW_H - 3, 0x33FFFFFF);
            }

            ctx.drawTextWithShadow(tr, st.label, left + 12, y + 1, 0xFFFFFFFF);
            ctx.drawTextWithShadow(tr, value + "/" + s.statCap(), left + 92, y + 1, 0xFFFFD84A);

            int barX = left + 130;
            int barW = 70;
            ctx.fill(barX, y + 3, barX + barW, y + 8, 0xAA000000);
            ctx.fill(barX, y + 3, barX + (int) (barW * Math.min(1.0, (double) value / Math.max(1, s.statCap()))), y + 8, 0xFF66DD66);

            ctx.drawTextWithShadow(tr, st.effectText(value), left + 206, y + 1, 0xFFBBBBBB);
        }

        // Descripcion / ayuda
        int helpY = top + 92 + StatType.values().length * ROW_H + 6;
        String help;
        if (hovered >= 0) {
            StatType st = StatType.values()[hovered];
            help = st.description + " Los puntos pasados de 10 rinden la mitad.";
        } else {
            help = "Consigue aura: combatir monstruos y jefes, correr/nadar, descubrir biomas y minar menas raras. "
                    + "Repetir siempre lo mismo rinde cada vez menos; mezclar actividades da un extra. "
                    + "Gasta aura en: reiniciar estadísticas (" + s.resetCost() + " aura).";
        }
        List<OrderedText> lines = tr.wrapLines(Text.literal(help), W - 20);
        for (int i = 0; i < Math.min(lines.size(), 4); i++) {
            ctx.drawTextWithShadow(tr, lines.get(i), left + 10, helpY + i * 10, 0xFFDDDDDD);
        }
    }
}
