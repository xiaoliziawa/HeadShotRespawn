package com.prizowo.headshotrespawn.client;

import com.prizowo.headshotrespawn.Headshotrespawn;
import com.prizowo.headshotrespawn.network.HeadshotModePacket;
import com.prizowo.headshotrespawn.network.NetworkHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Headshotrespawn.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ClientEvents {
    private static int currentMode = 0;
    private static final String[] MODE_NAMES = {
        "message.headshotrespawn.mode.normal",
        "message.headshotrespawn.mode.effect",
        "message.headshotrespawn.mode.physics"
    };

    @SubscribeEvent
    public static void onKeyRegister(RegisterKeyMappingsEvent event) {
        event.register(KeyBindings.TOGGLE_HEADSHOT_MODE);
    }

    @Mod.EventBusSubscriber(modid = Headshotrespawn.MOD_ID, value = Dist.CLIENT)
    public static class ClientForgeEvents {
        @SubscribeEvent
        public static void onKeyInput(InputEvent.Key event) {
            if (KeyBindings.TOGGLE_HEADSHOT_MODE.consumeClick()) {
                currentMode = (currentMode + 1) % MODE_NAMES.length;
                NetworkHandler.INSTANCE.sendToServer(new HeadshotModePacket(currentMode));
                
                if (Minecraft.getInstance().player != null) {
                    Minecraft.getInstance().player.displayClientMessage(
                        Component.translatable("message.headshotrespawn.mode.switch", 
                            Component.translatable(MODE_NAMES[currentMode])),
                        true
                    );
                }
            }
        }
    }
} 