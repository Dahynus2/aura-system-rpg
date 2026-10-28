package com.aurasystem.rpg.level;

import com.aurasystem.rpg.config.AuraConfig;

public final class LevelCurve {
    private LevelCurve() {}

    /** Experiencia (= aura ganada) necesaria para pasar de 'level' a 'level + 1'. */
    public static long xpToNext(int level) {
        return 100L + 40L * level + 8L * level * level;
    }

    /** Puntos de estadistica que se reciben al alcanzar 'newLevel'. */
    public static int statPointsFor(int newLevel) {
        AuraConfig c = AuraConfig.get();
        int pts = c.statPointsPerLevel;
        if (c.bonusStatPointEveryLevels > 0 && newLevel % c.bonusStatPointEveryLevels == 0) {
            pts += 1;
        }
        return pts;
    }
}
