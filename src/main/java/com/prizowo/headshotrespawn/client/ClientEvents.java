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
        "普通爆头模式",
        "特效爆头模式",
        "物理爆头模式"
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
                // 切换模式
                currentMode = (currentMode + 1) % MODE_NAMES.length;
                
                // 发送到服务器
                NetworkHandler.INSTANCE.sendToServer(new HeadshotModePacket(currentMode));
                
                // 显示当前模式
                if (Minecraft.getInstance().player != null) {
                    Minecraft.getInstance().player.displayClientMessage(
                        Component.literal("§6切换到：§e" + MODE_NAMES[currentMode]),
                        true
                    );
                }
            }
        }
    }
} 