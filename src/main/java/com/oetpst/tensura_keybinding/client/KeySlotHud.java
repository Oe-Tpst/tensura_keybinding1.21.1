package com.oetpst.tensura_keybinding.client;

import java.util.Optional;

import com.mojang.blaze3d.systems.RenderSystem;

import io.github.manasmods.manascore.skill.api.ManasSkill;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import io.github.manasmods.manascore.skill.api.SkillAPI;
import io.github.manasmods.tensura.ability.TensuraSkill;
import io.github.manasmods.tensura.handler.client.OverlayHandler;
import io.github.manasmods.tensura.util.client.RenderHelper;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;

/**
 * Always-on HUD row below Tensura's ability slots. Empty slots use Tensura's
 * own ability-slot frame; filled slots show the skill icon and Tensura's
 * cooldown overlay.
 */
public final class KeySlotHud {
    private static final int BASE_ICON_SIZE = 16;
    // Tensura's abilities overlay texture is 41x104: three 32x32 skill frames
    // stacked vertically with 4px gaps.
    private static final float ABILITY_HUD_HEIGHT = 104.0F;
    private static final float ROW_GAP = 4.0F;

    // Same texture and text colour Tensura uses for its native ability slots.
    private static final int COOLDOWN_TEXT_COLOR = 0xFF5555;

    private static final ResourceLocation ABILITY_OVERLAY =
            ResourceLocation.fromNamespaceAndPath("tensura", "textures/player_hud/abilities/ability_overlay.png");
    private static final ResourceLocation COOLDOWN_ICON =
            ResourceLocation.fromNamespaceAndPath("tensura", "textures/player_hud/abilities/cooldown_icon.png");

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
        // Place the row directly below the whole abilities column.
        int guiHeight = minecraft.getWindow().getGuiScaledHeight();
        int rowY = Math.round(positions[1] + (ABILITY_HUD_HEIGHT + ROW_GAP) * scale);
        rowY = Math.max(2, Math.min(rowY, guiHeight - iconSize - 2));

        GuiGraphics graphics = event.getGuiGraphics();
        for (int slot = 0; slot < KeySlotData.SLOT_COUNT; slot++) {
            drawSlot(graphics, minecraft.font, player, slot,
                    rowX + slot * (iconSize + gap), rowY, iconSize);
        }
    }

    private static void drawSlot(GuiGraphics graphics, Font font, LocalPlayer player, int slot,
                                 int iconX, int iconY, int iconSize) {
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

        drawCooldown(graphics, font, player, skillId, skill, entry.mode,
                iconX, iconY, iconSize);
    }

    /**
     * Mirrors Tensura's native ability cooldown rendering: a 28x28 dark overlay
     * centred on the 32x32 skill icon plus the remaining seconds in red.
     */
    private static void drawCooldown(GuiGraphics graphics, Font font, LocalPlayer player,
                                     ResourceLocation skillId, ManasSkill skill, int mode,
                                     int iconX, int iconY, int iconSize) {
        Optional<ManasSkillInstance> learned = SkillAPI.getSkillsFrom(player).getSkill(skillId);
        if (learned.isEmpty()) {
            return;
        }
        ManasSkillInstance instance = learned.get();

        boolean showCooldown;
        if (skill instanceof TensuraSkill tensuraSkill) {
            showCooldown = tensuraSkill.shouldShowCoolDown(instance, player, mode);
        } else {
            showCooldown = instance.onCoolDown(mode) && !instance.canIgnoreCoolDown(player, mode);
        }
        if (!showCooldown) {
            return;
        }

        int cooldownSize = Math.max(6, Math.round(iconSize * 28.0F / 32.0F));
        int cooldownX = iconX + (iconSize - cooldownSize) / 2;
        int cooldownY = iconY + (iconSize - cooldownSize) / 2;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        graphics.blit(COOLDOWN_ICON, cooldownX, cooldownY, cooldownSize, cooldownSize,
                0.0F, 0.0F, 28, 28, 28, 28);
        RenderSystem.disableBlend();

        String seconds = String.valueOf(instance.getCoolDown(mode));
        int textY = cooldownY + Math.max(0, (cooldownSize - font.lineHeight) / 2);
        RenderHelper.drawCenteredText(graphics, font, seconds,
                cooldownX, textY, cooldownSize, COOLDOWN_TEXT_COLOR);
    }
}
