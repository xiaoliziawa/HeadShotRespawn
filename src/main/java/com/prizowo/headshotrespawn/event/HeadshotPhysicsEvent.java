package com.prizowo.headshotrespawn.event;

import com.prizowo.headshotrespawn.Headshotrespawn;
import com.prizowo.headshotrespawn.Config;
import com.prizowo.headshotrespawn.network.PlayerHeadshotData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Headshotrespawn.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class HeadshotPhysicsEvent {

    @SubscribeEvent
    public static void onHeadshot(LivingDamageEvent event) {
        if (!(event.getSource().getEntity() instanceof Player player)) {
            return;
        }
        
        if (PlayerHeadshotData.getPlayerMode(player.getUUID()) != 2) {
            return;
        }

        if (!event.getSource().is(DamageTypeTags.IS_PROJECTILE)) {
            return;
        }

        Entity source = event.getSource().getDirectEntity();
        LivingEntity target = event.getEntity();
        
        if (!(source instanceof Projectile) || event.getSource().getSourcePosition() == null) {
            return;
        }

        double headStart = target.position().add(0.0, target.getDimensions(target.getPose()).height * 0.85, 0.0).y - 0.17;

        if (event.getSource().getSourcePosition().y > headStart && !target.isDamageSourceBlocked(event.getSource())) {
            event.setAmount(event.getAmount() * 2.0F);
            
            createSlowMotionEffect(target);
            
            applyPhysicsKnockback(player, target, source);
            
            createDynamicParticles(target);
            
            playDynamicSounds(target);
        }
    }

    private static void createSlowMotionEffect(LivingEntity target) {
        if (target.level() instanceof ServerLevel serverLevel) {
            for (int i = 0; i < 30; i++) {
                double angle = Math.PI * 2 * target.getRandom().nextDouble();
                double radius = 1.0 + target.getRandom().nextDouble();
                double height = target.getRandom().nextDouble() * 2;
                
                serverLevel.sendParticles(
                    ParticleTypes.REVERSE_PORTAL,
                    target.getX() + Math.cos(angle) * radius,
                    target.getY() + height,
                    target.getZ() + Math.sin(angle) * radius,
                    1, 0, 0, 0, 0.05
                );
            }
        }
    }

    private static void applyPhysicsKnockback(Player player, LivingEntity target, Entity projectile) {
        Vec3 knockbackDir = target.position().subtract(player.position()).normalize();
        double force = projectile.getDeltaMovement().length();
        
        double upwardForce = force * 0.5;
        double horizontalForce = force * 0.8;
        
        target.setDeltaMovement(
            knockbackDir.x * horizontalForce,
            upwardForce,
            knockbackDir.z * horizontalForce
        );
        
        target.setYRot(target.getYRot() + 180.0F);
    }

    private static void createDynamicParticles(LivingEntity target) {
        if (target.level() instanceof ServerLevel serverLevel) {
            double baseRadius = 0.5;
            for (int y = 0; y < 20; y++) {
                double angle = y * Math.PI / 10;
                double radius = baseRadius + (y / 20.0) * 0.5;
                
                serverLevel.sendParticles(
                    ParticleTypes.END_ROD,
                    target.getX() + Math.cos(angle) * radius,
                    target.getY() + y * 0.1,
                    target.getZ() + Math.sin(angle) * radius,
                    1, 0, 0, 0, 0
                );
                
                serverLevel.sendParticles(
                    ParticleTypes.DRAGON_BREATH,
                    target.getX() + Math.cos(angle + Math.PI) * radius,
                    target.getY() + y * 0.1,
                    target.getZ() + Math.sin(angle + Math.PI) * radius,
                    1, 0, 0, 0, 0
                );
            }

            for (int i = 0; i < 360; i += 10) {
                double angle = Math.toRadians(i);
                double radius = 2.0;
                serverLevel.sendParticles(
                    ParticleTypes.SONIC_BOOM,
                    target.getX() + Math.cos(angle) * radius,
                    target.getY() + 0.5,
                    target.getZ() + Math.sin(angle) * radius,
                    1, 0, 0, 0, 0
                );
            }
        }
    }

    private static void playDynamicSounds(LivingEntity target) {
        target.level().playSound(null,
            target.getX(), target.getY(), target.getZ(),
            SoundEvents.DRAGON_FIREBALL_EXPLODE,
            SoundSource.PLAYERS,
            0.5F, 2.0F
        );
        
        target.level().playSound(null,
            target.getX(), target.getY(), target.getZ(),
            SoundEvents.END_PORTAL_FRAME_FILL,
            SoundSource.PLAYERS,
            0.8F, 0.5F
        );
    }
} 