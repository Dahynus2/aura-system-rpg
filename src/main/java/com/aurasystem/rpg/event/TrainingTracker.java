package com.aurasystem.rpg.event;

import com.aurasystem.rpg.aura.Activity;
import com.aurasystem.rpg.aura.AuraService;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.network.ServerPlayerEntity;

/**
 * Entrenamiento: correr o nadar de verdad. Cada ventana de 40 ticks se evalua que el jugador haya
 * corrido/nadado casi todo el tiempo Y se haya desplazado al menos 6 bloques (evita correr contra un muro).
 */
public final class TrainingTracker {
    private TrainingTracker() {}

    private static final int WINDOW = 40;
    private static final Map<UUID, State> STATES = new HashMap<>();

    private static final class State {
        double startX;
        double startZ;
        int ticks;
        int active;

        State(double x, double z) {
            this.startX = x;
            this.startZ = z;
        }
    }

    public static void tick(ServerPlayerEntity player) {
        State s = STATES.computeIfAbsent(player.getUuid(), k -> new State(player.getX(), player.getZ()));
        boolean moving = !player.hasVehicle() && (player.isSprinting() || player.isSwimming());
        if (moving) {
            s.active++;
        }
        if (++s.ticks >= WINDOW) {
            double dx = player.getX() - s.startX;
            double dz = player.getZ() - s.startZ;
            double dist = Math.sqrt(dx * dx + dz * dz);
            if (s.active >= 30 && dist >= 6.0) {
                AuraService.grant(player, Activity.TRAINING, 0.6);
            }
            s.startX = player.getX();
            s.startZ = player.getZ();
            s.ticks = 0;
            s.active = 0;
        }
    }

    public static void forget(UUID id) {
        STATES.remove(id);
    }

    public static void clearAll() {
        STATES.clear();
    }
}
