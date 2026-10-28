package com.aurasystem.rpg;

import com.aurasystem.rpg.command.AuraCommand;
import com.aurasystem.rpg.config.AuraConfig;
import com.aurasystem.rpg.data.ModAttachments;
import com.aurasystem.rpg.event.ModEvents;
import com.aurasystem.rpg.network.ModNetworking;
import net.fabricmc.api.ModInitializer;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AuraSystemMod implements ModInitializer {
    public static final String MOD_ID = "aurasystemrpg";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static Identifier id(String path) {
        return Identifier.of(MOD_ID, path);
    }

    @Override
    public void onInitialize() {
        AuraConfig.load();
        ModAttachments.init();
        ModNetworking.registerPayloads();
        ModNetworking.registerServerReceivers();
        ModEvents.register();
        AuraCommand.register();
        LOGGER.info("Aura System RPG (etapa 1) cargado.");
    }
}
