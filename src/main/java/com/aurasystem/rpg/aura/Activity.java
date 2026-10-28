package com.aurasystem.rpg.aura;

import com.aurasystem.rpg.config.AuraConfig;

public enum Activity {
    COMBAT("Combate"),
    BOSS("Jefes"),
    TRAINING("Entrenamiento"),
    EXPLORATION("Exploración"),
    MINING("Minería"),
    MASTERY("Dominio de habilidades");

    public final String label;

    Activity(String label) {
        this.label = label;
    }

    public double softCap(AuraConfig c) {
        return switch (this) {
            case COMBAT -> c.combatSoftCap;
            case BOSS -> c.bossSoftCap;
            case TRAINING -> c.trainingSoftCap;
            case EXPLORATION -> c.explorationSoftCap;
            case MINING -> c.miningSoftCap;
            case MASTERY -> c.masterySoftCap;
        };
    }
}
