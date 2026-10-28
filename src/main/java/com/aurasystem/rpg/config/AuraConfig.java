package com.aurasystem.rpg.config;

import com.aurasystem.rpg.AuraSystemMod;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import net.fabricmc.loader.api.FabricLoader;

/** Configuracion del servidor (config/aurasystemrpg.json). Solo el servidor la usa para la logica. */
public final class AuraConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static AuraConfig instance = new AuraConfig();

    // Niveles y estadisticas
    public int maxLevel = 50;
    public int statCap = 20;
    public int statPointsPerLevel = 1;
    public int bonusStatPointEveryLevels = 5;

    // Energia
    public double baseEnergy = 100.0;
    public double baseEnergyRegenPerSecond = 2.0;

    // Aura
    public double globalAuraMultiplier = 1.0;
    public long resetStatsBaseCost = 500;
    public long resetStatsCostStep = 250;

    // Antiexploit: "softCap" = unidades de aura bruta tras las cuales el rendimiento cae a la mitad
    public double combatSoftCap = 600.0;
    public double bossSoftCap = 2500.0;
    public double trainingSoftCap = 60.0;
    public double explorationSoftCap = 300.0;
    public double miningSoftCap = 120.0;
    public double masterySoftCap = 80.0;
    public double fatigueRecoveryMinutes = 12.0;

    // Bonificacion por variedad de actividades
    public double varietyBonusPerActivity = 0.10;
    public double varietyBonusMax = 0.30;
    public int varietyWindowMinutes = 10;

    // Poderes
    public int maxActiveBolts = 256;

    public static AuraConfig get() {
        return instance;
    }

    public static void load() {
        Path path = FabricLoader.getInstance().getConfigDir().resolve(AuraSystemMod.MOD_ID + ".json");
        AuraConfig loaded = null;
        if (Files.exists(path)) {
            try (Reader reader = Files.newBufferedReader(path)) {
                loaded = GSON.fromJson(reader, AuraConfig.class);
            } catch (IOException | RuntimeException e) {
                AuraSystemMod.LOGGER.error("No se pudo leer {}; se usan valores por defecto.", path, e);
            }
        }
        if (loaded == null) {
            loaded = new AuraConfig();
        }
        loaded.sanitize();
        instance = loaded;
        try (Writer writer = Files.newBufferedWriter(path)) {
            GSON.toJson(loaded, writer);
        } catch (IOException e) {
            AuraSystemMod.LOGGER.error("No se pudo escribir {}.", path, e);
        }
    }

    private void sanitize() {
        maxLevel = Math.max(1, Math.min(maxLevel, 1000));
        statCap = Math.max(1, Math.min(statCap, 1000));
        statPointsPerLevel = Math.max(0, statPointsPerLevel);
        bonusStatPointEveryLevels = Math.max(0, bonusStatPointEveryLevels);
        baseEnergy = Math.max(1.0, baseEnergy);
        baseEnergyRegenPerSecond = Math.max(0.0, baseEnergyRegenPerSecond);
        globalAuraMultiplier = Math.max(0.0, globalAuraMultiplier);
        resetStatsBaseCost = Math.max(0, resetStatsBaseCost);
        resetStatsCostStep = Math.max(0, resetStatsCostStep);
        combatSoftCap = Math.max(1.0, combatSoftCap);
        bossSoftCap = Math.max(1.0, bossSoftCap);
        trainingSoftCap = Math.max(1.0, trainingSoftCap);
        explorationSoftCap = Math.max(1.0, explorationSoftCap);
        miningSoftCap = Math.max(1.0, miningSoftCap);
        masterySoftCap = Math.max(1.0, masterySoftCap);
        fatigueRecoveryMinutes = Math.max(0.1, fatigueRecoveryMinutes);
        varietyBonusPerActivity = Math.max(0.0, varietyBonusPerActivity);
        varietyBonusMax = Math.max(0.0, varietyBonusMax);
        varietyWindowMinutes = Math.max(1, varietyWindowMinutes);
        maxActiveBolts = Math.max(1, maxActiveBolts);
    }
}
