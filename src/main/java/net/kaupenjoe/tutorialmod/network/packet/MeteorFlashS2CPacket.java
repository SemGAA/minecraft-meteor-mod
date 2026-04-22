package net.kaupenjoe.tutorialmod.network.packet;

import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.event.network.CustomPayloadEvent;

public record MeteorFlashS2CPacket(int durationTicks) {
    public static void encode(MeteorFlashS2CPacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.durationTicks);
    }

    public static MeteorFlashS2CPacket decode(FriendlyByteBuf buffer) {
        return new MeteorFlashS2CPacket(buffer.readVarInt());
    }

    public static void handle(MeteorFlashS2CPacket packet, CustomPayloadEvent.Context context) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level != null) {
            minecraft.level.setSkyFlashTime(Math.max(minecraft.level.getSkyFlashTime(), packet.durationTicks));
        }
    }
}