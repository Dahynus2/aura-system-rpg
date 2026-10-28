package com.aurasystem.rpg.client;

import com.aurasystem.rpg.AuraSystemMod;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import net.fabricmc.loader.api.FabricLoader;

/** Preferencias visuales del cliente (config/aurasystemrpg-client.json). Las teclas se cambian en Controles. */
public final class ClientSettings {
    private ClientSettings() {}

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static boolean hudEnabled = true;

    private static final class Data {
        boolean hudEnabled = true;
    }

    private static Path path() {
        return FabricLoader.getInstance().getConfigDir().resolve(AuraSystemMod.MOD_ID + "-client.json");
    }

    public static void load() {
        Path p = path();
        if (!Files.exists(p)) {
            return;
        }
        try (Reader r = Files.newBufferedReader(p)) {
            Data d = GSON.fromJson(r, Data.class);
            if (d != null) {
                hudEnabled = d.hudEnabled;
            }
        } catch (IOException | RuntimeException e) {
            AuraSystemMod.LOGGER.warn("No se pudo leer la configuracion del cliente.", e);
        }
    }

    public static void save() {
        Data d = new Data();
        d.hudEnabled = hudEnabled;
        try (Writer w = Files.newBufferedWriter(path())) {
            GSON.toJson(d, w);
        } catch (IOException e) {
            AuraSystemMod.LOGGER.warn("No se pudo guardar la configuracion del cliente.", e);
        }
    }
}
