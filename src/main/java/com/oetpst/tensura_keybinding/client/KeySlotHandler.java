package com.oetpst.tensura_keybinding.client;

import java.util.Optional;

import com.oetpst.tensura_keybinding.TensuraKeybinding;
import com.oetpst.tensura_keybinding.network.KeySlotActionPayload;

import io.github.manasmods.manascore.skill.api.ManasSkill;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import io.github.manasmods.manascore.skill.api.SkillAPI;
import io.github.manasmods.manascore.skill.api.Skills;
import io.github.manasmods.tensura.ability.TensuraSkill;
import io.github.manasmods.tensura.client.TensuraKeybinds;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import org.lwjgl.glfw.GLFW;

/**
 * Owns the three extra key mappings and drives them through Tensura's normal
 * skill activation path (the same packets used by Tensura's own ability slots).
 */
public final class KeySlotHandler {
    private static final String CATEGORY = "key.categories.tensura_keybinding";

    private static final KeyMapping[] KEYS = new KeyMapping[KeySlotData.SLOT_COUNT];
    private static final boolean[] WAS_DOWN = new boolean[KeySlotData.SLOT_COUNT];
    private static final boolean[] ACTIVATED = new boolean[KeySlotData.SLOT_COUNT];
    private static final int[] ACTIVE_MODE = new int[KeySlotData.SLOT_COUNT];

    private KeySlotHandler() {
    }

    public static void registerKeys(RegisterKeyMappingsEvent event) {
        int[] defaults = {GLFW.GLFW_KEY_J, GLFW.GLFW_KEY_K, GLFW.GLFW_KEY_L};
        for (int i = 0; i < KEYS.length; i++) {
            KEYS[i] = new KeyMapping("key.tensura_keybinding.slot_" + (i + 1), defaults[i], CATEGORY);
            event.register(KEYS[i]);
        }
        TensuraKeybinding.LOGGER.debug("Registered {} Tensura key slot mappings", KEYS.length);
    }

    public static Component getBoundKey(int slot) {
        if (slot < 0 || slot >= KEYS.length || KEYS[slot] == null) {
            return Component.literal("-");
        }

        KeyMapping key = KEYS[slot];
        return key.isUnbound() ? Component.literal("-") : key.getTranslatedKeyMessage();
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null) {
            return;
        }

        // Release everything if a screen opens while a slot is held.
        if (minecraft.screen != null) {
            for (int slot = 0; slot < KEYS.length; slot++) {
                if (WAS_DOWN[slot]) {
                    release(slot);
                    WAS_DOWN[slot] = false;
                }
            }
            return;
        }

        for (int slot = 0; slot < KEYS.length; slot++) {
            boolean down = KEYS[slot].isDown();
            if (down && !WAS_DOWN[slot]) {
                press(slot, player);
            } else if (!down && WAS_DOWN[slot]) {
                release(slot);
            }
            WAS_DOWN[slot] = down;
        }
    }

    private static void press(int slot, LocalPlayer player) {
        KeySlotData.SlotEntry entry = KeySlotData.get(slot);
        if (entry == null || entry.skillId == null) {
            return;
        }

        ResourceLocation skillId = ResourceLocation.tryParse(entry.skillId);
        if (skillId == null) {
            return;
        }

        boolean nextMode = TensuraKeybinds.NEXT_ABILITY_MODE.isDown();
        boolean previousMode = TensuraKeybinds.PREVIOUS_ABILITY_MODE.isDown();
        if (nextMode || previousMode) {
            ACTIVATED[slot] = false;
            cycleMode(slot, skillId, nextMode ? 1 : -1, player);
            return;
        }

        ACTIVE_MODE[slot] = entry.mode;
        ACTIVATED[slot] = true;
        TensuraKeybinding.LOGGER.debug("[KeySlot] press slot={} skill={} mode={}", slot, skillId, entry.mode);
        PacketDistributor.sendToServer(
                new KeySlotActionPayload(KeySlotActionPayload.PRESS, slot, entry.mode, skillId));
    }

    private static void release(int slot) {
        if (!ACTIVATED[slot]) {
            return;
        }
        ACTIVATED[slot] = false;

        KeySlotData.SlotEntry entry = KeySlotData.get(slot);
        if (entry == null || entry.skillId == null) {
            return;
        }

        ResourceLocation skillId = ResourceLocation.tryParse(entry.skillId);
        if (skillId != null) {
            TensuraKeybinding.LOGGER.debug("[KeySlot] release slot={} skill={} mode={}",
                    slot, skillId, ACTIVE_MODE[slot]);
            PacketDistributor.sendToServer(
                    new KeySlotActionPayload(KeySlotActionPayload.RELEASE, slot, ACTIVE_MODE[slot], skillId));
        }
    }

    private static void cycleMode(int slot, ResourceLocation skillId, int delta, LocalPlayer player) {
        ManasSkill skill = SkillAPI.getSkillRegistry().get(skillId);
        if (skill == null) {
            return;
        }

        Skills skills = SkillAPI.getSkillsFrom(player);
        Optional<ManasSkillInstance> learned = skills.getSkill(skillId);
        ManasSkillInstance instance = learned.orElseGet(skill::createDefaultInstance);

        int modeCount = Math.max(1, instance.getModes());
        KeySlotData.SlotEntry entry = KeySlotData.get(slot);
        if (entry == null) {
            return;
        }

        int newMode = Math.floorMod(entry.mode + delta, modeCount);
        KeySlotData.setMode(slot, newMode);

        Component modeName = skill instanceof TensuraSkill tensuraSkill
                ? tensuraSkill.getModeName(instance, newMode)
                : Component.literal(String.valueOf(newMode + 1));
        player.displayClientMessage(
                Component.translatable("message.tensura_keybinding.mode_changed", skill.getName(), modeName),
                true);
    }
}
