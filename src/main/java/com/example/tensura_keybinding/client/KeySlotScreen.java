package com.example.tensura_keybinding.client;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import io.github.manasmods.manascore.skill.api.ManasSkill;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import io.github.manasmods.manascore.skill.api.SkillAPI;
import io.github.manasmods.tensura.ability.skill.Skill;
import io.github.manasmods.tensura.ability.skill.resist.ResistSkill;
import io.github.manasmods.tensura.client.screen.widgets.SimpleButton;
import io.github.manasmods.tensura.util.client.ScreenHelper;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/**
 * Selection screen for the three configurable skill slots.
 *
 * <p>The learned-skill list is on the left; the three slots are stacked
 * vertically on the right, each with its icon on the left and its bound key on
 * the right. Buttons use Tensura's own {@link SimpleButton} widget.</p>
 */
public class KeySlotScreen extends Screen {
    private static final int ROW_HEIGHT = 22;
    private static final int SCROLL_BAR_WIDTH = 5;
    private static final int ICON_SIZE = 24;
    private static final int MARGIN = 16;
    private static final int PANEL_GAP = 12;
    private static final int SLOT_ROW_HEIGHT = 44;
    private static final int SLOT_BUTTON_HEIGHT = 20;
    private static final int KEY_WIDTH = 44;

    private List<ManasSkillInstance> learnedSkills = List.of();

    private int selectedSlot;
    private ResourceLocation selectedSkillId;
    private double scrollAmount;
    private int listX;
    private int listY;
    private int listWidth;
    private int listHeight;
    private int hoveredSkill = -1;

    private int slotsX;
    private int slotsTop;
    private int rightWidth;
    private int slotButtonX;
    private int slotButtonWidth;
    private int keyX;

    public KeySlotScreen() {
        super(Component.translatable("screen.tensura_keybinding.title"));
    }

    @Override
    protected void init() {
        super.init();

        loadLearnedSkills();

        rightWidth = Math.max(150, Math.min(220, (int) (this.width * 0.42)));
        slotsX = this.width - MARGIN - rightWidth;
        slotsTop = 38;

        listX = MARGIN;
        listWidth = Math.max(100, slotsX - MARGIN - PANEL_GAP);
        listY = 30;
        listHeight = Math.max(ROW_HEIGHT, this.height - listY - MARGIN);
        scrollAmount = clampScroll(scrollAmount);

        slotButtonX = slotsX + ICON_SIZE + 6;
        keyX = slotsX + rightWidth - KEY_WIDTH;
        slotButtonWidth = Math.max(40, keyX - 4 - slotButtonX);

        for (int slot = 0; slot < KeySlotData.SLOT_COUNT; slot++) {
            final int index = slot;
            int rowY = slotsTop + slot * SLOT_ROW_HEIGHT;
            int buttonY = rowY + (SLOT_ROW_HEIGHT - SLOT_BUTTON_HEIGHT) / 2;
            Component label = trim(slotLabel(slot), slotButtonWidth - 8);
            this.addRenderableWidget(new SimpleButton(
                    slotButtonX, buttonY, slotButtonWidth, SLOT_BUTTON_HEIGHT,
                    label, Component.empty(),
                    pressed -> {
                        selectedSlot = index;
                        if (selectedSkillId != null) {
                            KeySlotData.set(index, selectedSkillId.toString(), 0);
                            selectedSkillId = null;
                        }
                        this.rebuildWidgets();
                    }));
        }

        int bottomY = this.height - 28;
        int doneWidth = 60;
        int clearWidth = Math.max(70, rightWidth - doneWidth - 6);
        this.addRenderableWidget(new SimpleButton(
                slotsX, bottomY, clearWidth, SLOT_BUTTON_HEIGHT,
                Component.translatable("screen.tensura_keybinding.clear"), Component.empty(),
                pressed -> {
                    KeySlotData.clear(selectedSlot);
                    this.rebuildWidgets();
                }));
        this.addRenderableWidget(new SimpleButton(
                slotsX + rightWidth - doneWidth, bottomY, doneWidth, SLOT_BUTTON_HEIGHT,
                Component.translatable("gui.done"), Component.empty(),
                // 4 = ScreenHelper.STATUS_MENU, i.e. the screen opened by B.
                pressed -> ScreenHelper.clicked(() -> ScreenHelper.openScreen(4))));
    }

    private void loadLearnedSkills() {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            learnedSkills = List.of();
            return;
        }

        List<ManasSkillInstance> skills = new ArrayList<>();
        for (ManasSkillInstance instance : SkillAPI.getSkillsFrom(player).getLearnedSkills()) {
            ManasSkill skill = instance.getSkill();
            // Match Tensura's ability selection: skip entries without a name and
            // keep only actual skills (no magic or battlewill). Resistance-type
            // skills are excluded as well.
            if (instance.getDisplayName() == null
                    || !(skill instanceof Skill)
                    || skill instanceof ResistSkill) {
                continue;
            }
            skills.add(instance);
        }
        skills.sort(Comparator.comparing(
                instance -> instance.getDisplayName().getString(),
                String.CASE_INSENSITIVE_ORDER));
        learnedSkills = skills;
    }

    private Component slotLabel(int index) {
        KeySlotData.SlotEntry entry = KeySlotData.get(index);
        if (entry == null || entry.skillId == null) {
            return Component.literal((index + 1) + ": "
                    + Component.translatable("screen.tensura_keybinding.empty").getString());
        }

        ResourceLocation skillId = ResourceLocation.tryParse(entry.skillId);
        ManasSkill skill = skillId == null ? null : SkillAPI.getSkillRegistry().get(skillId);
        String name = skill == null ? entry.skillId : skill.getName().getString();
        int modes = skill == null ? 1 : Math.max(1, skill.createDefaultInstance().getModes());
        String mode = modes > 1 ? " [" + (entry.mode + 1) + "/" + modes + "]" : "";
        return Component.literal((index + 1) + ": " + name + mode);
    }

    private double maxScroll() {
        return Math.max(0, learnedSkills.size() * ROW_HEIGHT - listHeight);
    }

    private double clampScroll(double amount) {
        return Math.max(0, Math.min(amount, maxScroll()));
    }

    private int skillIndexAt(double mouseX, double mouseY) {
        if (mouseX < listX || mouseX >= listX + listWidth
                || mouseY < listY || mouseY >= listY + listHeight) {
            return -1;
        }

        int index = (int) ((mouseY - listY + scrollAmount) / ROW_HEIGHT);
        return index >= 0 && index < learnedSkills.size() ? index : -1;
    }

    private void selectSkill(int index) {
        if (index < 0 || index >= learnedSkills.size()) {
            return;
        }
        selectedSkillId = learnedSkills.get(index).getSkillId();
    }

    private ResourceLocation skillIcon(int slot) {
        KeySlotData.SlotEntry entry = KeySlotData.get(slot);
        if (entry == null || entry.skillId == null) {
            return null;
        }

        ResourceLocation skillId = ResourceLocation.tryParse(entry.skillId);
        if (skillId == null) {
            return null;
        }

        ManasSkill skill = SkillAPI.getSkillRegistry().get(skillId);
        return skill == null ? null : skill.getSkillIcon();
    }

    private String slotMarkers(ResourceLocation skillId) {
        StringBuilder markers = new StringBuilder();
        for (int slot = 0; slot < KeySlotData.SLOT_COUNT; slot++) {
            KeySlotData.SlotEntry entry = KeySlotData.get(slot);
            if (entry != null && skillId.toString().equals(entry.skillId)) {
                markers.append(" [").append(slot + 1).append(']');
            }
        }
        return markers.toString();
    }

    private Component trim(Component text, int width) {
        return Component.literal(this.font.plainSubstrByWidth(text.getString(), width));
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);

        graphics.drawCenteredString(this.font, this.title, this.width / 2, 6, 0xFFFFFF);

        renderSkillList(graphics, mouseX, mouseY);
        renderSlots(graphics);
    }

    private void renderSkillList(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.fill(listX - 1, listY - 1, listX + listWidth + 1, listY + listHeight + 1, 0xFF707070);
        graphics.fill(listX, listY, listX + listWidth, listY + listHeight, 0xC0101010);

        if (learnedSkills.isEmpty()) {
            graphics.drawCenteredString(this.font,
                    Component.translatable("screen.tensura_keybinding.no_skills"),
                    listX + listWidth / 2, listY + listHeight / 2 - 4, 0xFF5555);
            return;
        }

        hoveredSkill = skillIndexAt(mouseX, mouseY);
        int textWidth = listWidth - 20 - (maxScroll() > 0 ? SCROLL_BAR_WIDTH + 4 : 0);

        graphics.enableScissor(listX, listY, listX + listWidth, listY + listHeight);
        int firstRow = (int) (scrollAmount / ROW_HEIGHT);
        int lastRow = Math.min(learnedSkills.size(), (int) ((scrollAmount + listHeight) / ROW_HEIGHT) + 1);
        for (int index = firstRow; index < lastRow; index++) {
            ManasSkillInstance instance = learnedSkills.get(index);
            int rowY = listY + (int) (index * ROW_HEIGHT - scrollAmount);

            if (index == hoveredSkill) {
                graphics.fill(listX, rowY, listX + listWidth, rowY + ROW_HEIGHT, 0x40FFFFFF);
            }

            boolean selected = selectedSkillId != null
                    && selectedSkillId.equals(instance.getSkillId());
            if (selected) {
                graphics.fill(listX, rowY, listX + listWidth, rowY + ROW_HEIGHT, 0x6033AA33);
                graphics.fill(listX, rowY, listX + 2, rowY + ROW_HEIGHT, 0xFF55FF55);
            }

            ManasSkill skill = instance.getSkill();
            int nameColor = 0xFFFFFF;
            if (skill instanceof Skill tensuraSkill) {
                ChatFormatting formatting = tensuraSkill.getType().getChatFormatting();
                Integer color = formatting.getColor();
                if (color != null) {
                    nameColor = color;
                }
            }

            String markers = slotMarkers(instance.getSkillId());
            int markerWidth = this.font.width(markers);
            String name = this.font.plainSubstrByWidth(
                    instance.getDisplayName().getString(), Math.max(0, textWidth - markerWidth));
            int textX = listX + 7;
            graphics.drawString(this.font, name, textX, rowY + 7, nameColor, false);
            graphics.drawString(this.font, markers, textX + this.font.width(name),
                    rowY + 7, 0xAAAAAA, false);
        }
        graphics.disableScissor();

        if (maxScroll() > 0) {
            int barX = listX + listWidth - SCROLL_BAR_WIDTH - 4;
            int thumbHeight = Math.max(16,
                    (int) (listHeight * (listHeight / (double) (learnedSkills.size() * ROW_HEIGHT))));
            int thumbY = listY + (int) ((listHeight - thumbHeight) * (scrollAmount / maxScroll()));
            graphics.fill(barX, listY, barX + SCROLL_BAR_WIDTH, listY + listHeight, 0x60000000);
            graphics.fill(barX, thumbY, barX + SCROLL_BAR_WIDTH, thumbY + thumbHeight, 0xFFAAAAAA);
        }
    }

    private void renderSlots(GuiGraphics graphics) {
        for (int slot = 0; slot < KeySlotData.SLOT_COUNT; slot++) {
            int rowY = slotsTop + slot * SLOT_ROW_HEIGHT;
            int buttonY = rowY + (SLOT_ROW_HEIGHT - SLOT_BUTTON_HEIGHT) / 2;
            int iconY = rowY + (SLOT_ROW_HEIGHT - ICON_SIZE) / 2;

            graphics.fill(slotsX - 1, iconY - 1,
                    slotsX + ICON_SIZE + 1, iconY + ICON_SIZE + 1, 0xFF707070);
            graphics.fill(slotsX, iconY, slotsX + ICON_SIZE, iconY + ICON_SIZE, 0xC0101010);
            ResourceLocation icon = skillIcon(slot);
            if (icon != null) {
                graphics.blit(icon, slotsX, iconY, ICON_SIZE, ICON_SIZE,
                        0.0F, 0.0F, 32, 32, 32, 32);
            }

            graphics.fill(keyX, buttonY, keyX + KEY_WIDTH, buttonY + SLOT_BUTTON_HEIGHT, 0xC0101010);
            graphics.fill(keyX, buttonY, keyX + KEY_WIDTH, buttonY + 1, 0xFF707070);
            graphics.fill(keyX, buttonY + SLOT_BUTTON_HEIGHT - 1,
                    keyX + KEY_WIDTH, buttonY + SLOT_BUTTON_HEIGHT, 0xFF707070);
            graphics.fill(keyX, buttonY, keyX + 1, buttonY + SLOT_BUTTON_HEIGHT, 0xFF707070);
            graphics.fill(keyX + KEY_WIDTH - 1, buttonY,
                    keyX + KEY_WIDTH, buttonY + SLOT_BUTTON_HEIGHT, 0xFF707070);

            Component keyName = KeySlotHandler.getBoundKey(slot);
            String keyText = this.font.plainSubstrByWidth(keyName.getString(), KEY_WIDTH - 6);
            graphics.drawCenteredString(this.font, keyText,
                    keyX + KEY_WIDTH / 2, buttonY + 6, 0xFFFFFF);
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (mouseX >= listX && mouseX < listX + listWidth
                && mouseY >= listY && mouseY < listY + listHeight) {
            scrollAmount = clampScroll(scrollAmount - scrollY * ROW_HEIGHT * 2);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            int index = skillIndexAt(mouseX, mouseY);
            if (index >= 0) {
                selectSkill(index);
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
