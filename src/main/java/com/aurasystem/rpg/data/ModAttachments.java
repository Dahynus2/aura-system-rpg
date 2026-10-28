package com.aurasystem.rpg.data;

import com.aurasystem.rpg.AuraSystemMod;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.entity.player.PlayerEntity;

public final class ModAttachments {
    private ModAttachments() {}

    /** Datos del jugador: persistente y copiado al morir. */
    public static final AttachmentType<AuraData> AURA = AttachmentRegistry.create(
            AuraSystemMod.id("aura_data"),
            builder -> builder
                    .initializer(AuraData::new)
                    .persistent(AuraData.CODEC)
                    .copyOnDeath());

    /** Fuerza la carga de la clase (y por tanto el registro) durante onInitialize. */
    public static void init() {
    }

    public static AuraData get(PlayerEntity player) {
        return player.getAttachedOrCreate(AURA);
    }

    /** Reasigna el adjunto para garantizar que se persista tras una mutacion. */
    public static void save(PlayerEntity player, AuraData data) {
        player.setAttached(AURA, data);
    }
}
