package com.prizowo.headshotrespawn.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class HeadshotModePacket {
    private final int mode; // 0: HeadshotEvent, 1: HeadshotEffectEvent

    public HeadshotModePacket(int mode) {
        this.mode = mode;
    }

    public HeadshotModePacket(FriendlyByteBuf buf) {
        this.mode = buf.readInt();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeInt(mode);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player != null) {
                // 更新玩家的爆头模式
                PlayerHeadshotData.setPlayerMode(player.getUUID(), mode);
            }
        });
        ctx.get().setPacketHandled(true);
    }
} 