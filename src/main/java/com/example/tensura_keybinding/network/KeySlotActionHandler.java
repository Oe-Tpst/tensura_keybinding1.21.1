package com.example.tensura_keybinding.network;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import com.example.tensura_keybinding.TensuraKeybinding;

import io.github.manasmods.manascore.skill.api.ManasSkill;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import io.github.manasmods.manascore.skill.api.SkillAPI;
import io.github.manasmods.manascore.skill.impl.SkillStorage;
import io.github.manasmods.tensura.storage.TensuraStorages;
import io.github.manasmods.tensura.storage.ability.AbilitySlot;
import io.github.manasmods.tensura.storage.ability.IAbility;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Server side of the key-slot activation.
 *
 * <p>Tensura's ACTIVATE_SKILL listener rejects any skill that is not in the
 * active ability preset at the supplied key number. We therefore temporarily
 * place the requested skill (and mode) into a free preset slot, activate it
 * through Tensura's normal {@code startHoldSkill} path, and immediately restore
 * the original slot contents.</p>
 */
public final class KeySlotActionHandler {
    private static final int SLOT_COUNT = 3;
    private static final Map<UUID, PressState[]> PRESSED = new HashMap<>();

    private KeySlotActionHandler() {
    }

    public static void handle(KeySlotActionPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                if (payload.action() == KeySlotActionPayload.PRESS) {
                    press(player, payload);
                } else if (payload.action() == KeySlotActionPayload.RELEASE) {
                    release(player, payload);
                }
            }
        });
    }

    private static void press(ServerPlayer player, KeySlotActionPayload payload) {
        int ourSlot = payload.slot();
        if (ourSlot < 0 || ourSlot >= SLOT_COUNT) {
            return;
        }

        ResourceLocation skillId = payload.skillId();
        SkillStorage skills = SkillAPI.getSkillsFrom(player);
        Optional<ManasSkillInstance> learned = skills.getSkill(skillId);
        if (learned.isEmpty()) {
            TensuraKeybinding.LOGGER.debug("[KeySlot] server press: {} not learned", skillId);
            return;
        }

        ManasSkillInstance instance = learned.get();
        ManasSkill skill = instance.getSkill();
        int mode = payload.mode();
        if (mode < 0 || mode >= Math.max(1, instance.getModes())) {
            TensuraKeybinding.LOGGER.debug("[KeySlot] server press: invalid mode {} for {}", mode, skillId);
            return;
        }

        IAbility ability = TensuraStorages.getAbilityFrom(player);
        List<AbilitySlot> slots = ability.getAbilitySlots();
        if (slots.isEmpty()) {
            return;
        }

        // Prefer an existing slot that already holds this exact skill + mode.
        int abilitySlot = -1;
        for (int i = 0; i < slots.size(); i++) {
            AbilitySlot slot = slots.get(i);
            if (!slot.isEmpty() && skill.equals(slot.getSkill()) && slot.getMode() == mode) {
                abilitySlot = i;
                break;
            }
        }

        boolean temporary = false;
        AbilitySlot original = null;
        if (abilitySlot < 0) {
            // Otherwise use an empty slot; fall back to the last slot, which is
            // restored immediately after activation.
            for (int i = 0; i < slots.size(); i++) {
                if (slots.get(i).isEmpty()) {
                    abilitySlot = i;
                    break;
                }
            }
            if (abilitySlot < 0) {
                abilitySlot = slots.size() - 1;
            }

            original = ability.getAbilitySlot(abilitySlot);
            ability.setAbilitySlot(abilitySlot, skill, mode);
            ability.markDirty();
            temporary = true;
        }

        boolean activated;
        try {
            activated = skills.startHoldSkill(skillId, abilitySlot, mode);
        } finally {
            if (temporary) {
                ability.setAbilitySlot(ability.getActivePreset(), abilitySlot, original);
                ability.markDirty();
            }
        }

        TensuraKeybinding.LOGGER.debug(
                "[KeySlot] server press: ourSlot={} skill={} abilitySlot={} mode={} temp={} activated={}",
                ourSlot, skillId, abilitySlot, mode, temporary, activated);

        PressState[] states = PRESSED.computeIfAbsent(player.getUUID(),
                id -> new PressState[SLOT_COUNT]);
        states[ourSlot] = activated ? new PressState(skillId, abilitySlot, mode) : null;
    }

    private static void release(ServerPlayer player, KeySlotActionPayload payload) {
        int ourSlot = payload.slot();
        if (ourSlot < 0 || ourSlot >= SLOT_COUNT) {
            return;
        }

        PressState[] states = PRESSED.get(player.getUUID());
        if (states == null) {
            return;
        }
        PressState state = states[ourSlot];
        if (state == null) {
            return;
        }
        states[ourSlot] = null;

        SkillStorage skills = SkillAPI.getSkillsFrom(player);
        skills.getSkill(state.skillId).ifPresent(instance ->
                skills.handleSkillRelease(instance, state.abilitySlot, state.mode, false));

        TensuraKeybinding.LOGGER.debug(
                "[KeySlot] server release: ourSlot={} skill={} abilitySlot={} mode={}",
                ourSlot, state.skillId, state.abilitySlot, state.mode);
    }

    private record PressState(ResourceLocation skillId, int abilitySlot, int mode) {
    }
}
