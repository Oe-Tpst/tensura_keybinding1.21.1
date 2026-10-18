package com.oetpst.tensura_keybinding.network;

import com.oetpst.tensura_keybinding.client.KeySlotData;

import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Client side of {@link SkillLostPayload}: clears every slot that still
 * references the removed skill. Runs on the client only.
 */
public final class SkillLostHandler {
    private SkillLostHandler() {
    }

    public static void handle(SkillLostPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> KeySlotData.clearSkill(payload.skillId()));
    }
}
