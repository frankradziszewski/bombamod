package com.bombamod;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public class Networking {
    public static final Identifier BOMB_PACKET_ID = Identifier.of("bombamod", "bomb_packet");
    public static final CustomPayload.Id<BombPayload> BOMB_PACKET_TYPE = new CustomPayload.Id<>(BOMB_PACKET_ID);

    public record BombPayload(String targetPlayerName) implements CustomPayload {
        public static final PacketCodec<RegistryByteBuf, BombPayload> CODEC = PacketCodec.tuple(
                PacketCodecs.STRING, BombPayload::targetPlayerName,
                BombPayload::new
        );

        @Override
        public Id<? extends CustomPayload> getId() {
            return BOMB_PACKET_TYPE;
        }
    }
}