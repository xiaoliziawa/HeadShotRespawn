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
        
        // 检查玩家的爆头模式
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
            // 增加基础伤害
            event.setAmount(event.getAmount() * 2.0F);
            
            // 创建慢动作效果
            createSlowMotionEffect(target);
            
            // 应用物理击退效果
            applyPhysicsKnockback(player, target, source);
            
            // 创建动态粒子效果
            createDynamicParticles(target);
            
            // 播放动态音效
            playDynamicSounds(target);
        }
    }

    private static void createSlowMotionEffect(LivingEntity target) {
        if (target.level() instanceof ServerLevel serverLevel) {
            // 创建时间扭曲粒子效果
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
        // 计算击退方向
        Vec3 knockbackDir = target.position().subtract(player.position()).normalize();
        // 获取弹射物的速度作为力量因子
        double force = projectile.getDeltaMovement().length();
        
        // 应用向上的力
        double upwardForce = force * 0.5;
        // 应用水平击退
        double horizontalForce = force * 0.8;
        
        // 设置目标的运动
        target.setDeltaMovement(
            knockbackDir.x * horizontalForce,
            upwardForce,
            knockbackDir.z * horizontalForce
        );
        
        // 添加旋转效果
        target.setYRot(target.getYRot() + 180.0F);
    }

    private static void createDynamicParticles(LivingEntity target) {
        if (target.level() instanceof ServerLevel serverLevel) {
            // 创建螺旋上升的粒子链
            double baseRadius = 0.5;
            for (int y = 0; y < 20; y++) {
                double angle = y * Math.PI / 10;
                double radius = baseRadius + (y / 20.0) * 0.5;
                
                // 主螺旋
                serverLevel.sendParticles(
                    ParticleTypes.END_ROD,
                    target.getX() + Math.cos(angle) * radius,
                    target.getY() + y * 0.1,
                    target.getZ() + Math.sin(angle) * radius,
                    1, 0, 0, 0, 0
                );
                
                // 副螺旋
                serverLevel.sendParticles(
                    ParticleTypes.DRAGON_BREATH,
                    target.getX() + Math.cos(angle + Math.PI) * radius,
                    target.getY() + y * 0.1,
                    target.getZ() + Math.sin(angle + Math.PI) * radius,
                    1, 0, 0, 0, 0
                );
            }

            // 冲击波效果
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
        // 创建声音序列
        target.level().playSound(null,
            target.getX(), target.getY(), target.getZ(),
            SoundEvents.DRAGON_FIREBALL_EXPLODE,
            SoundSource.PLAYERS,
            0.5F, 2.0F
        );
        
        // 延迟音效（通过调整音高来模拟）
        target.level().playSound(null,
            target.getX(), target.getY(), target.getZ(),
            SoundEvents.END_PORTAL_FRAME_FILL,
            SoundSource.PLAYERS,
            0.8F, 0.5F
        );
    }
} 