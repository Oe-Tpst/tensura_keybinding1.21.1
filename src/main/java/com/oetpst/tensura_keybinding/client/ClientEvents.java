package com.oetpst.tensura_keybinding.client;

import io.github.manasmods.tensura.client.screen.MainScreen;
import io.github.manasmods.tensura.client.screen.widgets.SimpleButton;
import io.github.manasmods.tensura.util.client.ScreenHelper;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;

public final class ClientEvents {
    private static final int TAB_WIDTH = 21;
    private static final int TAB_HEIGHT = 24;

    private static final ResourceLocation WARP_CORE_ICON =
            ResourceLocation.fromNamespaceAndPath("tensura", "textures/item/warp_core.png");

    private ClientEvents() {
    }

    @SubscribeEvent
    public static void onScreenInit(ScreenEvent.Init.Post event) {
        if (!(event.getScreen() instanceof MainScreen screen)) {
            return;
        }

        // Tensura 2.0.1.3's MainScreen is a 256 x 163 panel centred on the screen.
        // Attach our tab to the panel's left edge, below the three top-left tabs.
        int guiLeft = (screen.width - 256) / 2;
        int guiTop = (screen.height - 163) / 2;
        int x = guiLeft - TAB_WIDTH;
        int y = guiTop + 30;

        event.addListener(new KeyTabButton(
                x, y, TAB_WIDTH, TAB_HEIGHT,
                ScreenHelper.RIGHT_TAB,
                WARP_CORE_ICON,
                Component.translatable("screen.tensura_keybinding.open"),
                button -> ScreenHelper.clicked(() ->
                        Minecraft.getInstance().setScreen(new KeySlotScreen()))));
    }

    /**
     * A left-side Tensura tab. The right-side tab texture is mirrored
     * horizontally so its attached edge faces the main panel, then the warp
     * core icon is drawn on top.
     */
    private static final class KeyTabButton extends SimpleButton {
        private final ResourceLocation icon;

        private KeyTabButton(int x, int y, int width, int height, ResourceLocation texture,
                             ResourceLocation icon, Component tooltip, Button.OnPress onPress) {
            super(x, y, width, height, texture, null, tooltip, onPress);
            this.icon = icon;
        }

        @Override
        protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            boolean hovered = isHoveredOrFocused();
            float vOffset = hovered ? this.height : 0.0F;

            // Draw the right-tab texture mirrored, one source column at a time,
            // so the attached edge faces the main panel.
            for (int column = 0; column < this.width; column++) {
                graphics.blit(this.texture,
                        getX() + (this.width - 1 - column), getY(),
                        column, vOffset, 1, this.height,
                        this.textureWidth, this.textureHeight);
            }

            graphics.blit(icon, getX() + 3, getY() + 4, 0.0F, 0.0F, 16, 16, 16, 16);

            if (hovered) {
                Screen screen = Minecraft.getInstance().screen;
                if (screen != null) {
                    screen.setTooltipForNextRenderPass(this.tooltip);
                }
            }
        }
    }
}
