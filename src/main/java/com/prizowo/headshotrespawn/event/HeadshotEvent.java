package com.prizowo.headshotrespawn.event;

import com.prizowo.headshotrespawn.Headshotrespawn;
import com.prizowo.headshotrespawn.Config;
import com.prizowo.headshotrespawn.network.PlayerHeadshotData;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import java.util.HashMap;
import java.util.UUID;
import net.minecraft.sounds.SoundEvent;

@Mod.EventBusSubscriber(modid = Headshotrespawn.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class HeadshotEvent {

    // 存储玩家的连续爆头次数
    private static final HashMap<UUID, HeadshotStreak> playerStreaks = new HashMap<>();
    
    private static class HeadshotStreak {
        int count;
        long lastHeadshotTime;
        
        HeadshotStreak() {
            this.count = 0;
            this.lastHeadshotTime = 0;
        }
    }

    @SubscribeEvent
    public static void onHeadshot(LivingDamageEvent event) {
        if (!(event.getSource().getEntity() instanceof Player player)) {
            return;
        }
        
        // 检查玩家的爆头模式
        if (PlayerHeadshotData.getPlayerMode(player.getUUID()) != 0) {
            return;
        }

        if (!event.getSource().is(DamageTypeTags.IS_PROJECTILE)) {
            return;
        }

        Entity source = event.getSource().getDirectEntity();
        Entity attacker = event.getSource().getEntity();
        LivingEntity target = event.getEntity();
        
        if (!(source instanceof Projectile) || attacker == null || event.getSource().getSourcePosition() == null) {
            return;
        }

        double headStart = target.position().add(0.0, target.getDimensions(target.getPose()).height * 0.85, 0.0).y - 0.17;

        if (event.getSource().getSourcePosition().y > headStart && !target.isDamageSourceBlocked(event.getSource())) {
            boolean hasHelmet = !target.getItemBySlot(EquipmentSlot.HEAD).isEmpty();
            
            float damageMultiplier = hasHelmet ?
                Config.HEADSHOT_DAMAGE_MULTIPLIER_WITH_HELMET.get().floatValue() : 
                Config.HEADSHOT_DAMAGE_MULTIPLIER_WITHOUT_HELMET.get().floatValue();

            event.setAmount(event.getAmount() * damageMultiplier);

            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 20, 3, false, false));
            target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 40, 0, false, false));

            if (!target.level().isClientSide() && target.level() instanceof ServerLevel serverLevel) {
                for (int i = 0; i < 20; i++) {
                    double angle = Math.PI * 2 * target.getRandom().nextDouble();
                    double radius = 0.5 + target.getRandom().nextDouble() * 0.5;
                    
                    double offsetX = Math.cos(angle) * radius;
                    double offsetY = target.getRandom().nextDouble() * 0.5;
                    double offsetZ = Math.sin(angle) * radius;
                    
                    double speedX = offsetX * 0.2;
                    double speedY = 0.1 + target.getRandom().nextDouble() * 0.1;
                    double speedZ = offsetZ * 0.2;

                    serverLevel.sendParticles(
                        ParticleTypes.FIREWORK,
                        target.getX(),
                        target.getY() + target.getBbHeight() * 0.8,
                        target.getZ(),
                        1,
                        offsetX, offsetY, offsetZ,
                        0.1
                    );

                    serverLevel.sendParticles(
                        ParticleTypes.CRIT,
                        target.getX(),
                        target.getY() + target.getBbHeight() * 0.8,
                        target.getZ(),
                        1,
                        offsetX, offsetY, offsetZ,
                        0.1
                    );
                }
            }

            if (attacker instanceof Player ) {
                if (!player.level().isClientSide()) {
                    player.level().playSound(
                        null,
                        player.getX(),
                        player.getY(),
                        player.getZ(),
                        SoundEvents.EXPERIENCE_ORB_PICKUP,
                        SoundSource.PLAYERS,
                        0.5F,
                        1.0F
                    );
                }

                // 处理连续爆头
                handleHeadshotStreak(player, target);

                // 处理随机特殊效果
                if (player.getRandom().nextFloat() < Config.SPECIAL_EFFECT_CHANCE.get()) {
                    applyRandomSpecialEffect(player, target);
                }
            }

            if (hasHelmet) {
                target.getItemBySlot(EquipmentSlot.HEAD).hurtAndBreak(
                    (int)(event.getAmount() / 2),
                    target,
                    (p) -> p.broadcastBreakEvent(EquipmentSlot.HEAD)
                );
            }
        }
    }

    private static void handleHeadshotStreak(Player player, LivingEntity target) {
        UUID playerId = player.getUUID();
        long currentTime = System.currentTimeMillis();
        HeadshotStreak streak = playerStreaks.computeIfAbsent(playerId, k -> new HeadshotStreak());

        // 如果距离上次爆头超过10秒，重置计数
        if (currentTime - streak.lastHeadshotTime > Config.STREAK_TIMEOUT_SECONDS.get() * 1000) {
            streak.count = 0;
        }

        streak.count++;
        // 如果达到30次，重置计数并给予特殊奖励
        if (streak.count >= 30) {
            giveMaxStreakReward(player);
            streak.count = 0;
        }

        streak.lastHeadshotTime = currentTime;

        // 根据连续爆头次数给予奖励
        handleStreakRewards(player, streak.count);
    }

    private static void handleStreakRewards(Player player, int streakCount) {
        switch (streakCount) {
            case 5 -> giveSpeedBoost(player);
            case 10 -> giveJumpBoost(player);
            case 15 -> giveStrengthBoost(player);
            case 20 -> giveRegenerationBoost(player);
            case 25 -> giveResistanceBoost(player);
            case 30 -> giveMaxStreakReward(player);
        }
    }

    // 新的奖励方法
    private static void giveSpeedBoost(Player player) {
        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 200, 1, false, false));
        createColoredParticles(player, ParticleTypes.CLOUD);
        playRewardSound(player, SoundEvents.EXPERIENCE_ORB_PICKUP, 1.0F);
    }

    private static void giveJumpBoost(Player player) {
        player.addEffect(new MobEffectInstance(MobEffects.JUMP, 200, 2, false, false));
        createColoredParticles(player, ParticleTypes.SOUL_FIRE_FLAME);
        playRewardSound(player, SoundEvents.ENDER_DRAGON_FLAP, 0.5F);
    }

    private static void giveStrengthBoost(Player player) {
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 200, 1, false, false));
        createColoredParticles(player, ParticleTypes.FLAME);
        playRewardSound(player, SoundEvents.BLAZE_SHOOT, 0.8F);
    }

    private static void giveRegenerationBoost(Player player) {
        player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 200, 1, false, false));
        player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 200, 1, false, false));
        createColoredParticles(player, ParticleTypes.HEART);
        playRewardSound(player, SoundEvents.TOTEM_USE, 0.5F);
    }

    private static void giveResistanceBoost(Player player) {
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 200, 1, false, false));
        createColoredParticles(player, ParticleTypes.END_ROD);
        playRewardSound(player, SoundEvents.ELDER_GUARDIAN_CURSE, 0.5F);
    }

    private static void giveMaxStreakReward(Player player) {
        // 给予多个效果
        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 400, 2, false, false));
        player.addEffect(new MobEffectInstance(MobEffects.JUMP, 400, 2, false, false));
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 400, 2, false, false));
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 400, 1, false, false));
        player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 400, 1, false, false));
        
        // 生成特效
        createMaxStreakEffect(player);
        
        // 播放音效组合
        playMaxStreakSounds(player);
        
        // 给予物品奖励
        giveMaxStreakItems(player);
    }

    private static void createMaxStreakEffect(Player player) {
        if (player.level() instanceof ServerLevel serverLevel) {
            // 创建螺旋上升的粒子效果
            double radius = 1.0;
            for (int y = 0; y < 20; y++) {
                for (int i = 0; i < 360; i += 20) {
                    double angle = Math.toRadians(i + y * 18); // 螺旋效果
                    double x = player.getX() + Math.cos(angle) * radius;
                    double z = player.getZ() + Math.sin(angle) * radius;
                    
                    serverLevel.sendParticles(
                        ParticleTypes.END_ROD,
                        x, player.getY() + y * 0.1, z,
                        1, 0, 0, 0, 0.02
                    );
                }
            }
        }
    }

    private static void playMaxStreakSounds(Player player) {
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
            SoundEvents.ENDER_DRAGON_GROWL, SoundSource.PLAYERS, 0.5F, 1.0F);
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
            SoundEvents.TOTEM_USE, SoundSource.PLAYERS, 1.0F, 1.0F);
    }

    private static void giveMaxStreakItems(Player player) {
        // 给予一些有用的物品
        ItemStack[] rewards = {
            new ItemStack(Items.ENCHANTED_GOLDEN_APPLE),
            new ItemStack(Items.ARROW, 64),
            new ItemStack(Items.EXPERIENCE_BOTTLE, 16),
            new ItemStack(Items.TOTEM_OF_UNDYING)
        };
        
        for (ItemStack reward : rewards) {
            player.getInventory().add(reward);
        }
    }

    private static void createColoredParticles(Player player, ParticleOptions particleType) {
        if (player.level() instanceof ServerLevel serverLevel) {
            for (int i = 0; i < 20; i++) {
                double angle = Math.PI * 2 * player.getRandom().nextDouble();
                double radius = 1.0 + player.getRandom().nextDouble() * 0.5;
                double x = player.getX() + Math.cos(angle) * radius;
                double z = player.getZ() + Math.sin(angle) * radius;
                
                serverLevel.sendParticles(
                    particleType,
                    x, player.getY() + 1, z,
                    1, 0, 0.1, 0, 0.02
                );
            }
        }
    }

    private static void playRewardSound(Player player, SoundEvent sound, float volume) {
        if (!player.level().isClientSide()) {
            player.level().playSound(
                null,  // null 表示所有玩家都能听到
                player.getX(),
                player.getY(),
                player.getZ(),
                sound,
                SoundSource.PLAYERS,
                volume,
                1.0F
            );
        }
    }

    private static void createHeadshotAura(Player player) {
        if (!player.level().isClientSide() && player.level() instanceof ServerLevel serverLevel) {
            double radius = 1.5;
            for (int i = 0; i < 360; i += 10) {
                double angle = Math.toRadians(i);
                double x = player.getX() + Math.cos(angle) * radius;
                double z = player.getZ() + Math.sin(angle) * radius;
                serverLevel.sendParticles(
                    ParticleTypes.SOUL_FIRE_FLAME,
                    x, player.getY(), z,
                    1, 0, 0.1, 0, 0.02
                );
            }
        }
    }

    private static void applyRandomSpecialEffect(Player player, LivingEntity target) {
        switch (player.getRandom().nextInt(8)) { // 增加更多效果
            case 0 -> { // 目标变小
                if (target instanceof Player targetPlayer) {
                    targetPlayer.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 200, 0, false, false));
                }
                // 缩小粒子效果
                spawnShrinkParticles(target);
            }
            case 1 -> { // 冰冻效果
                target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 5, false, false));
                spawnFrostParticles(target);
            }
            case 2 -> { // 彩虹效果
                createRainbowTrail(player);
            }
            case 3 -> { // 生命恢复
                player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 100, 2, false, false));
                player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 200, 1, false, false));
            }
            case 4 -> { // 跳跃提升
                player.addEffect(new MobEffectInstance(MobEffects.JUMP, 200, 2, false, false));
            }
            case 5 -> { // 击退效果
                Vec3 knockback = target.position().subtract(player.position()).normalize().scale(2);
                target.setDeltaMovement(knockback.x, 0.5, knockback.z);
            }
            case 6 -> { // 生成金苹果
                ItemStack goldenApple = new ItemStack(Items.GOLDEN_APPLE);
                target.spawnAtLocation(goldenApple);
            }
            case 7 -> { // 生成烟花
                spawnFireworks(target);
            }
        }
    }

    private static void spawnShrinkParticles(LivingEntity target) {
        if (target.level() instanceof ServerLevel serverLevel) {
            for (int i = 0; i < 20; i++) {
                double x = target.getX() + (target.getRandom().nextDouble() - 0.5) * 2;
                double y = target.getY() + target.getRandom().nextDouble() * 2;
                double z = target.getZ() + (target.getRandom().nextDouble() - 0.5) * 2;
                serverLevel.sendParticles(
                    ParticleTypes.PORTAL,
                    x, y, z,
                    1, 0, 0, 0, 0.1
                );
            }
        }
    }

    private static void spawnFrostParticles(LivingEntity target) {
        if (target.level() instanceof ServerLevel serverLevel) {
            for (int i = 0; i < 30; i++) {
                double x = target.getX() + (target.getRandom().nextDouble() - 0.5) * 2;
                double y = target.getY() + target.getRandom().nextDouble() * 2;
                double z = target.getZ() + (target.getRandom().nextDouble() - 0.5) * 2;
                serverLevel.sendParticles(
                    ParticleTypes.CLOUD,
                    x, y, z,
                    1, 0, 0, 0, 0.1
                );
            }
        }
    }

    private static void createRainbowTrail(Player player) {
        if (player.level() instanceof ServerLevel serverLevel) {
            double radius = 1.0;
            for (int i = 0; i < 360; i += 20) {
                double angle = Math.toRadians(i);
                double x = player.getX() + Math.cos(angle) * radius;
                double z = player.getZ() + Math.sin(angle) * radius;
                
                // 使用不同的粒子
                switch (i / 20 % 5) {
                    case 0 -> serverLevel.sendParticles(
                        ParticleTypes.FLAME,
                        x, player.getY() + 0.5, z,
                        1, 0, 0.1, 0, 0.02
                    );
                    case 1 -> serverLevel.sendParticles(
                        ParticleTypes.SOUL_FIRE_FLAME,
                        x, player.getY() + 0.5, z,
                        1, 0, 0.1, 0, 0.02
                    );
                    case 2 -> serverLevel.sendParticles(
                        ParticleTypes.DRIPPING_WATER,
                        x, player.getY() + 0.5, z,
                        1, 0, 0.1, 0, 0.02
                    );
                    case 3 -> serverLevel.sendParticles(
                        ParticleTypes.ENCHANTED_HIT,
                        x, player.getY() + 0.5, z,
                        1, 0, 0.1, 0, 0.02
                    );
                    case 4 -> serverLevel.sendParticles(
                        ParticleTypes.END_ROD,
                        x, player.getY() + 0.5, z,
                        1, 0, 0.1, 0, 0.02
                    );
                }
            }
        }
    }

    private static void spawnFireworks(LivingEntity target) {
        if (!target.level().isClientSide()) {
            ItemStack fireworkRocket = new ItemStack(Items.FIREWORK_ROCKET);
            target.spawnAtLocation(fireworkRocket);
            // 播放烟花音效
            target.level().playSound(
                null,
                target.getX(),
                target.getY(),
                target.getZ(),
                SoundEvents.FIREWORK_ROCKET_LAUNCH,
                SoundSource.NEUTRAL,
                1.0F,
                1.0F
            );
        }
    }

    private static void spawnRandomReward(LivingEntity target) {
        ItemStack reward = switch (target.getRandom().nextInt(5)) {
            case 0 -> new ItemStack(Items.GOLDEN_APPLE);
            case 1 -> new ItemStack(Items.ARROW, 16);
            case 2 -> new ItemStack(Items.EXPERIENCE_BOTTLE);
            case 3 -> new ItemStack(Items.EMERALD);
            default -> new ItemStack(Items.COOKIE);
        };
        target.spawnAtLocation(reward);
    }
} 