package com.prizowo.headshotrespawn.event;

import com.prizowo.headshotrespawn.Headshotrespawn;
import com.prizowo.headshotrespawn.Config;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import com.prizowo.headshotrespawn.network.PlayerHeadshotData;

@Mod.EventBusSubscriber(modid = Headshotrespawn.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class HeadshotEffectEvent {

    @SubscribeEvent
    public static void onHeadshot(LivingDamageEvent event) {
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

            createHeadshotEffect(target);

            if (attacker instanceof Player player) {
                playHeadshotSound(player);
                
                if (player.getRandom().nextFloat() < 0.2f) {
                    applySpecialEffect(player, target);
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

    private static void createHeadshotEffect(LivingEntity target) {
        if (!target.level().isClientSide() && target.level() instanceof ServerLevel serverLevel) {
            for (int i = 0; i < 20; i++) {
                double angle = Math.PI * 2 * target.getRandom().nextDouble();
                double radius = 0.5 + target.getRandom().nextDouble() * 0.5;
                
                double offsetX = Math.cos(angle) * radius;
                double offsetY = target.getRandom().nextDouble() * 0.5;
                double offsetZ = Math.sin(angle) * radius;

                serverLevel.sendParticles(
                    ParticleTypes.DAMAGE_INDICATOR,
                    target.getX(),
                    target.getY() + target.getBbHeight() * 0.8,
                    target.getZ(),
                    3,
                    offsetX, offsetY, offsetZ,
                    0.1
                );

                serverLevel.sendParticles(
                    ParticleTypes.CRIT,
                    target.getX(),
                    target.getY() + target.getBbHeight() * 0.8,
                    target.getZ(),
                    2,
                    offsetX, offsetY, offsetZ,
                    0.1
                );
            }
        }
    }

    private static void playHeadshotSound(Player player) {
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
            
            player.level().playSound(
                null,
                player.getX(),
                player.getY(),
                player.getZ(),
                SoundEvents.ARROW_HIT_PLAYER,
                SoundSource.PLAYERS,
                0.3F,
                0.8F
            );
        }
    }

    private static void applySpecialEffect(Player player, LivingEntity target) {
        switch (player.getRandom().nextInt(5)) {
            case 0 -> applyExplosiveEffect(target);
            case 1 -> applyFreezeEffect(target);
            case 2 -> applyLevitationEffect(target);
            case 3 -> applyLootExplosionEffect(target);
            case 4 -> applyThunderEffect(target);
        }
    }

    private static void applyExplosiveEffect(LivingEntity target) {
        target.level().explode(null, target.getX(), target.getY(), target.getZ(),
            1.0F, false, Level.ExplosionInteraction.NONE);
    }

    private static void applyFreezeEffect(LivingEntity target) {
        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 10, false, false));
        if (target.level() instanceof ServerLevel serverLevel) {
            for (int i = 0; i < 50; i++) {
                serverLevel.sendParticles(
                    ParticleTypes.SNOWFLAKE,
                    target.getX(), target.getY() + 1, target.getZ(),
                    1, 0.5, 0.5, 0.5, 0
                );
            }
        }
    }

    private static void applyLevitationEffect(LivingEntity target) {
        target.addEffect(new MobEffectInstance(MobEffects.LEVITATION, 40, 0, false, false));
        target.addEffect(new MobEffectInstance(MobEffects.GLOWING, 60, 0, false, false));
    }

    private static void applyLootExplosionEffect(LivingEntity target) {
        ItemStack[] loot = {
            new ItemStack(Items.GOLD_NUGGET, 1 + target.getRandom().nextInt(3)),
            new ItemStack(Items.ARROW, 1 + target.getRandom().nextInt(5)),
            new ItemStack(Items.EXPERIENCE_BOTTLE, 1),
            new ItemStack(Items.EMERALD, 1)
        };

        for (ItemStack item : loot) {
            if (target.getRandom().nextFloat() < 0.3f) {
                ItemEntity itemEntity = new ItemEntity(
                    target.level(),
                    target.getX(),
                    target.getY() + 1,
                    target.getZ(),
                    item
                );
                itemEntity.setDeltaMovement(
                    (target.getRandom().nextDouble() - 0.5) * 0.3,
                    target.getRandom().nextDouble() * 0.2 + 0.1,
                    (target.getRandom().nextDouble() - 0.5) * 0.3
                );
                target.level().addFreshEntity(itemEntity);
            }
        }
    }

    private static void applyThunderEffect(LivingEntity target) {
        if (target.level() instanceof ServerLevel serverLevel) {
            for (int i = 0; i < 50; i++) {
                serverLevel.sendParticles(
                    ParticleTypes.ELECTRIC_SPARK,
                    target.getX(),
                    target.getY() + target.getBbHeight() * 0.5,
                    target.getZ(),
                    1,
                    0.5, 1.0, 0.5,
                    0.1
                );
            }
            target.level().playSound(
                null,
                target.getX(),
                target.getY(),
                target.getZ(),
                SoundEvents.LIGHTNING_BOLT_THUNDER,
                SoundSource.WEATHER,
                1.0F,
                1.0F
            );
        }
    }
} 