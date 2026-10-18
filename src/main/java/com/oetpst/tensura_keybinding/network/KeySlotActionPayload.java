package com.oetpst.tensura_keybinding.network;

import com.oetpst.tensura_keybinding.TensuraKeybinding;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Client -> server request to press or release one of our key slots. The
 * server temporarily equips the skill in Tensura's active ability preset so
 * the normal activation checks accept it, then restores the preset.
 */
public record KeySlotActionPayload(int action, int slot, int mode, ResourceLocation skillId)
        implements CustomPacketPayload {
    public static final int PRESS = 0;
    public static final int RELEASE = 1;

    public static final Type<KeySlotActionPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(TensuraKeybinding.MODID, "key_slot_action"));

    public static final StreamCodec<ByteBuf, KeySlotActionPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, KeySlotActionPayload::action,
                    ByteBufCodecs.VAR_INT, KeySlotActionPayload::slot,
                    ByteBufCodecs.VAR_INT, KeySlotActionPayload::mode,
                    ResourceLocation.STREAM_CODEC, KeySlotActionPayload::skillId,
                    KeySlotActionPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
