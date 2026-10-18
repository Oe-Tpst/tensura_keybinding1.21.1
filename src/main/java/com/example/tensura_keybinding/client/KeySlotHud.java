package com.example.tensura_keybinding.client;

import io.github.manasmods.manascore.skill.api.ManasSkill;
import io.github.manasmods.manascore.skill.api.SkillAPI;
import io.github.manasmods.tensura.handler.client.OverlayHandler;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;

/**
 * Always-on HUD row above Tensura's ability slots. Empty slots use Tensura's
 * own ability-slot frame; filled slots show the skill icon.
 */
public final class KeySlotHud {
    private static final int BASE_ICON_SIZE = 16;

    private static final ResourceLocation ABILITY_OVERLAY =
            ResourceLocation.fromNamespaceAndPath("tensura", "textures/player_hud/abilities/ability_overlay.png");

    private KeySlotHud() {
    }

    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || minecraft.options.hideGui || minecraft.screen != null) {
            return;
        }

        float[] positions = OverlayHandler.getPositionsById(OverlayHandler.ABILITIES);
        if (positions == null) {
            return;
        }

        float scale = Math.max(0.5F, Math.min(2.0F,
                OverlayHandler.getScaleById(OverlayHandler.ABILITIES)));
        boolean leftSide = OverlayHandler.isLeftSide(OverlayHandler.ABILITIES);

        int iconSize = Math.max(10, Math.min(24, Math.round(BASE_ICON_SIZE * scale)));
        int gap = Math.max(2, Math.round(2.0F * scale));
        int rowWidth = iconSize * KeySlotData.SLOT_COUNT + gap * (KeySlotData.SLOT_COUNT - 1);

        int rowX = Math.round(positions[0]);
        if (!leftSide) {
            rowX = rowX + Math.round(41.0F * scale) - rowWidth;
        }
        int rowY = Math.max(2, Math.round(positions[1] - iconSize - 4.0F * scale));

        GuiGraphics graphics = event.getGuiGraphics();
        for (int slot = 0; slot < KeySlotData.SLOT_COUNT; slot++) {
            drawSlot(graphics, slot, rowX + slot * (iconSize + gap), rowY, iconSize);
        }
    }

    private static void drawSlot(GuiGraphics graphics, int slot, int iconX, int iconY, int iconSize) {
        // Empty-slot frame from Tensura's ability overlay: (8, 1) is the 32x32
        // icon frame inside the 41x104 overlay.
        graphics.blit(ABILITY_OVERLAY, iconX, iconY, iconSize, iconSize,
                8.0F, 1.0F, 32, 32, 41, 104);

        KeySlotData.SlotEntry entry = KeySlotData.get(slot);
        if (entry == null || entry.skillId == null) {
            return;
        }

        ResourceLocation skillId = ResourceLocation.tryParse(entry.skillId);
        if (skillId == null) {
            return;
        }

        ManasSkill skill = SkillAPI.getSkillRegistry().get(skillId);
        if (skill == null) {
            return;
        }

        ResourceLocation icon = skill.getSkillIcon();
        if (icon != null) {
            graphics.blit(icon, iconX, iconY, iconSize, iconSize,
                    0.0F, 0.0F, 32, 32, 32, 32);
        }
    }
}
