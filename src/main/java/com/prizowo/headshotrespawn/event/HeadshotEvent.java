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
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Headshotrespawn.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class HeadshotEvent {

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onHeadshot(LivingDamageEvent event) {
        String eventKey = "headshot_processed_" + event.getSource().getMsgId();
        
        if (event.getEntity().getPersistentData().contains(eventKey)) {
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
            event.getEntity().getPersistentData().putBoolean(eventKey, true);
            
            boolean hasHelmet = !target.getItemBySlot(EquipmentSlot.HEAD).isEmpty();
            
            float baseMultiplier = Config.HEADSHOT_DAMAGE_MULTIPLIER_WITHOUT_HELMET.get().floatValue();
            float helmetMultiplier = Math.min(
                Config.HEADSHOT_DAMAGE_MULTIPLIER_WITH_HELMET.get().floatValue(),
                baseMultiplier * 0.75f
            );
            
            float damageMultiplier = hasHelmet ? helmetMultiplier : baseMultiplier;

            float newDamage = Math.min(
                event.getAmount() * damageMultiplier, 
                Config.MAX_HEADSHOT_DAMAGE.get().floatValue()
            );

            event.setAmount(newDamage);

            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 20, 3, false, false));
            target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 40, 0, false, false));

            if (!target.level().isClientSide() && target.level() instanceof ServerLevel serverLevel) {
                for (int i = 0; i < 20; i++) {
                    double angle = Math.PI * 2 * target.getRandom().nextDouble();
                    double radius = 0.5 + target.getRandom().nextDouble() * 0.5;
                    
                    double offsetX = Math.cos(angle) * radius;
                    double offsetY = target.getRandom().nextDouble() * 0.5;
                    double offsetZ = Math.sin(angle) * radius;

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

            if (attacker instanceof Player player) {
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
            }

            if (hasHelmet) {
                target.getItemBySlot(EquipmentSlot.HEAD).hurtAndBreak(
                    (int)(event.getAmount() / 2),
                    target,
                    (p) -> p.broadcastBreakEvent(EquipmentSlot.HEAD)
                );
            }

            if (target.level() instanceof ServerLevel serverLevel) {
                serverLevel.getServer().tell(new net.minecraft.server.TickTask(
                    serverLevel.getServer().getTickCount() + 1,
                    () -> event.getEntity().getPersistentData().remove(eventKey)
                ));
            }
        }
    }

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        if (!event.getSource().is(DamageTypeTags.IS_PROJECTILE)) {
            event.getEntity().getPersistentData().getAllKeys().stream()
                .filter(key -> key.startsWith("headshot_processed_"))
                .forEach(key -> event.getEntity().getPersistentData().remove(key));
        }
    }
} 