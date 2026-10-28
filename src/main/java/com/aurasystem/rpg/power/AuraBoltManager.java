package com.aurasystem.rpg.power;

import com.aurasystem.rpg.aura.Activity;
import com.aurasystem.rpg.aura.AuraService;
import com.aurasystem.rpg.config.AuraConfig;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

/**
 * Proyectil de aura simulado 100% en el servidor (sin entidad personalizada en la etapa 1):
 * avanza cada tick, colisiona con bloques y entidades mediante raycast y dibuja su estela con particulas.
 * Al no ser una entidad no necesita renderer en el cliente y no puede desincronizarse.
 * (En la etapa 2 se puede migrar a una entidad propia con modelo si se desea.)
 */
public final class AuraBoltManager {
    private AuraBoltManager() {}

    private static final double SPEED = 1.8;     // bloques por tick
    private static final double RADIUS = 0.35;   // margen de colision con entidades
    private static final List<Bolt> BOLTS = new ArrayList<>();

    private static final class Bolt {
        final ServerWorld world;
        final UUID owner;
        Vec3d pos;
        final Vec3d dir;
        double remaining;
        final float damage;

        Bolt(ServerWorld world, UUID owner, Vec3d pos, Vec3d dir, double remaining, float damage) {
            this.world = world;
            this.owner = owner;
            this.pos = pos;
            this.dir = dir;
            this.remaining = remaining;
            this.damage = damage;
        }
    }

    public static void launch(ServerPlayerEntity owner, double range, float damage) {
        if (BOLTS.size() >= AuraConfig.get().maxActiveBolts) {
            return;
        }
        ServerWorld world = (ServerWorld) owner.getEntityWorld();
        Vec3d dir = owner.getRotationVec(1.0F).normalize();
        Vec3d start = owner.getEyePos().add(dir.multiply(0.8)).subtract(0.0, 0.2, 0.0);
        BOLTS.add(new Bolt(world, owner.getUuid(), start, dir, range, damage));
        world.playSound(null, start.x, start.y, start.z, SoundEvents.ENTITY_EVOKER_CAST_SPELL,
                SoundCategory.PLAYERS, 0.8f, 1.6f);
    }

    public static void tick(MinecraftServer server) {
        if (BOLTS.isEmpty()) {
            return;
        }
        Iterator<Bolt> it = BOLTS.iterator();
        while (it.hasNext()) {
            if (!step(it.next())) {
                it.remove();
            }
        }
    }

    public static void clearAll() {
        BOLTS.clear();
    }

    /** @return true si el proyectil sigue vivo. */
    private static boolean step(Bolt b) {
        PlayerEntity found = b.world.getPlayerByUuid(b.owner);
        if (!(found instanceof ServerPlayerEntity owner)) {
            return false;
        }

        double step = Math.min(SPEED, b.remaining);
        Vec3d end = b.pos.add(b.dir.multiply(step));

        BlockHitResult blockHit = b.world.raycast(new RaycastContext(
                b.pos, end, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, owner));
        boolean hitBlock = blockHit.getType() != HitResult.Type.MISS;
        Vec3d limit = hitBlock ? blockHit.getPos() : end;

        LivingEntity target = null;
        Vec3d targetPos = null;
        double best = Double.MAX_VALUE;
        Box sweep = new Box(b.pos, limit).expand(RADIUS + 0.5);
        List<Entity> candidates = b.world.getOtherEntities(owner, sweep,
                e -> e instanceof LivingEntity le && le.isAlive() && !le.isSpectator() && canHit(owner, e));
        for (Entity e : candidates) {
            Box box = e.getBoundingBox().expand(RADIUS);
            Vec3d point = null;
            if (box.contains(b.pos)) {
                point = b.pos;
            } else {
                Optional<Vec3d> r = box.raycast(b.pos, limit);
                if (r.isPresent()) {
                    point = r.get();
                }
            }
            if (point != null) {
                double dist = b.pos.squaredDistanceTo(point);
                if (dist < best) {
                    best = dist;
                    target = (LivingEntity) e;
                    targetPos = point;
                }
            }
        }

        if (target != null) {
            boolean damaged = target.damage(b.world, b.world.getDamageSources().playerAttack(owner), b.damage);
            target.takeKnockback(0.4, -b.dir.x, -b.dir.z);
            if (damaged) {
                AuraService.grant(owner, Activity.MASTERY, 1.0);
            }
            impact(b.world, targetPos);
            return false;
        }
        if (hitBlock) {
            impact(b.world, blockHit.getPos());
            return false;
        }

        for (double t = 0.0; t <= step; t += 0.6) {
            Vec3d p = b.pos.add(b.dir.multiply(t));
            b.world.spawnParticles(ParticleTypes.END_ROD, p.x, p.y, p.z, 1, 0.04, 0.04, 0.04, 0.0);
        }
        b.world.spawnParticles(ParticleTypes.SOUL_FIRE_FLAME, end.x, end.y, end.z, 2, 0.05, 0.05, 0.05, 0.0);

        b.pos = end;
        b.remaining -= step;
        return b.remaining > 0.01;
    }

    private static boolean canHit(ServerPlayerEntity owner, Entity e) {
        if (e == owner) {
            return false;
        }
        if (e instanceof PlayerEntity other && !owner.shouldDamagePlayer(other)) {
            return false;
        }
        return true;
    }

    private static void impact(ServerWorld world, Vec3d at) {
        world.spawnParticles(ParticleTypes.ELECTRIC_SPARK, at.x, at.y, at.z, 16, 0.25, 0.25, 0.25, 0.15);
        world.spawnParticles(ParticleTypes.END_ROD, at.x, at.y, at.z, 8, 0.2, 0.2, 0.2, 0.05);
        world.playSound(null, at.x, at.y, at.z, SoundEvents.ENTITY_FIREWORK_ROCKET_BLAST,
                SoundCategory.PLAYERS, 0.7f, 1.4f);
    }
}
