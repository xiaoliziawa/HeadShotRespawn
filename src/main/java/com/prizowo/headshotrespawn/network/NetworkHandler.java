package com.prizowo.headshotrespawn.network;

import com.prizowo.headshotrespawn.Headshotrespawn;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.Optional;

public class NetworkHandler {
    private static final String PROTOCOL_VERSION = "1";
    public static final SimpleChannel INSTANCE = NetworkRegistry.newSimpleChannel(
        new ResourceLocation(Headshotrespawn.MOD_ID, "main"),
        () -> PROTOCOL_VERSION,
        PROTOCOL_VERSION::equals,
        PROTOCOL_VERSION::equals
    );

    public static void register() {
        int id = 0;
        INSTANCE.registerMessage(
            id++,
            HeadshotModePacket.class,
            HeadshotModePacket::encode,
            HeadshotModePacket::new,
            HeadshotModePacket::handle,
            Optional.of(NetworkDirection.PLAY_TO_SERVER)
        );
    }
} 