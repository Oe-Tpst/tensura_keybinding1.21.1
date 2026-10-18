package com.oetpst.tensura_keybinding.network;

import com.oetpst.tensura_keybinding.TensuraKeybinding;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Server -> client notification that a skill has been removed from the
 * player, so any key slot bound to it can be cleared.
 */
public record SkillLostPayload(ResourceLocation skillId) implements CustomPacketPayload {
    public static final Type<SkillLostPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(TensuraKeybinding.MODID, "skill_lost"));

    public static final StreamCodec<ByteBuf, SkillLostPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ResourceLocation.STREAM_CODEC, SkillLostPayload::skillId,
                    SkillLostPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
