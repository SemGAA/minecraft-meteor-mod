package net.kaupenjoe.tutorialmod.network;

import net.kaupenjoe.tutorialmod.TutorialMod;
import net.kaupenjoe.tutorialmod.network.packet.MeteorFlashS2CPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.ChannelBuilder;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.SimpleChannel;

public final class ModMessages {
    private static final SimpleChannel CHANNEL = ChannelBuilder
            .named(ResourceLocation.parse(TutorialMod.MOD_ID + ":main"))
            .networkProtocolVersion(1)
            .simpleChannel();

    private static boolean registered;

    private ModMessages() {
    }

    public static void register() {
        if (registered) {
            return;
        }

        CHANNEL.messageBuilder(MeteorFlashS2CPacket.class, 0, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(MeteorFlashS2CPacket::encode)
                .decoder(MeteorFlashS2CPacket::decode)
                .consumerMainThread(MeteorFlashS2CPacket::handle)
                .add();

        CHANNEL.build();
        registered = true;
    }

    public static <MSG> void sendToPlayer(MSG message, ServerPlayer player) {
        CHANNEL.send(message, PacketDistributor.PLAYER.with(player));
    }
}