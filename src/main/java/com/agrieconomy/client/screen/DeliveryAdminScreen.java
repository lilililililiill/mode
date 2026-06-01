package com.agrieconomy.client.screen;

import com.agrieconomy.delivery.DeliveryAdminMenu;
import com.agrieconomy.delivery.DeliveryQuest;
import com.agrieconomy.network.C2SAdminSavePacket;
import com.agrieconomy.network.PacketHandler;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/**
 * 납품 블록 관리자 설정 UI.
 */
public class DeliveryAdminScreen extends AbstractContainerScreen<DeliveryAdminMenu> {

    private static final int GUI_WIDTH = 180;
    private static final int GUI_HEIGHT = 296;

    private EditBox titleField;
    private EditBox descField;
    private EditBox deadlineField;
    private EditBox chanceField;

    private boolean autoEnabled = false;
    private Button autoToggleButton;
    private Button requirementModeButton;
    private Button rewardModeButton;
    private Button requiredRegButton;
    private Button rewardRegButton;
    private boolean requiredTab = true;

    private DeliveryQuest.RequirementMode requirementMode = DeliveryQuest.RequirementMode.AND;
    private DeliveryQuest.RewardMode rewardMode = DeliveryQuest.RewardMode.ALL;

    private Button saveButton;
    private Button activateButton;

    public DeliveryAdminScreen(DeliveryAdminMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = GUI_WIDTH;
        this.imageHeight = GUI_HEIGHT;
    }

    @Override
    protected void init() {
        super.init();

        int x = leftPos + 6;
        int y = topPos + 22;
        int fieldW = 88;
        int rowH = 20;

        titleField = newEditBox(x + 64, y, fieldW);
        addRenderableWidget(titleField);
        y += rowH;

        descField = newEditBox(x + 64, y, fieldW);
        addRenderableWidget(descField);
        y += rowH;

        deadlineField = newEditBox(x + 64, y, 40);
        deadlineField.setFilter(s -> s.matches("\\d*"));
        deadlineField.setValue("0");
        addRenderableWidget(deadlineField);

        autoToggleButton = addRenderableWidget(
                Button.builder(Component.literal("OFF"), btn -> {
                            autoEnabled = !autoEnabled;
                            btn.setMessage(Component.literal(autoEnabled ? "§aON" : "§cOFF"));
                        })
                        .pos(x + 110, y)
                        .size(36, 14)
                        .build()
        );
        y += rowH;

        chanceField = newEditBox(x + 64, y, 40);
        chanceField.setFilter(s -> s.matches("\\d*") && (s.isEmpty() || Integer.parseInt(s) <= 100));
        chanceField.setValue("5");
        addRenderableWidget(chanceField);
        y += rowH;

        requirementModeButton = addRenderableWidget(
                Button.builder(Component.literal("AND"), btn -> {
                            requirementMode = requirementMode == DeliveryQuest.RequirementMode.AND
                                    ? DeliveryQuest.RequirementMode.OR
                                    : DeliveryQuest.RequirementMode.AND;
                            btn.setMessage(Component.literal(requirementMode.name()));
                        })
                        .pos(x + 64, y)
                        .size(50, 16)
                        .build()
        );

        rewardModeButton = addRenderableWidget(
                Button.builder(Component.literal("ALL"), btn -> {
                            rewardMode = rewardMode == DeliveryQuest.RewardMode.ALL
                                    ? DeliveryQuest.RewardMode.CHOICE
                                    : DeliveryQuest.RewardMode.ALL;
                            btn.setMessage(Component.literal(rewardMode.name()));
                        })
                        .pos(x + 116, y)
                        .size(50, 16)
                        .build()
        );
        y += rowH;

        requiredRegButton = addRenderableWidget(
                Button.builder(Component.literal("요구 등록"), btn -> requiredTab = true)
                        .pos(x, y).size(72, 16).build()
        );
        rewardRegButton = addRenderableWidget(
                Button.builder(Component.literal("보상 등록"), btn -> requiredTab = false)
                        .pos(x + 76, y).size(72, 16).build()
        );

        saveButton = addRenderableWidget(
                Button.builder(Component.literal("§f저장"), btn -> onSaveClicked())
                        .pos(leftPos + 20, topPos + GUI_HEIGHT - 26)
                        .size(60, 18)
                        .build()
        );

        activateButton = addRenderableWidget(
                Button.builder(Component.literal("§e즉시 활성화"), btn -> onActivateClicked())
                        .pos(leftPos + 90, topPos + GUI_HEIGHT - 26)
                        .size(82, 18)
                        .build()
        );

        PacketHandler.sendToServer(new C2SAdminSavePacket(menu.getBlockPos(), null, true));
    }

    public void populateFields(DeliveryQuest quest) {
        if (quest == null) return;
        titleField.setValue(quest.getTitle() != null ? quest.getTitle() : "");
        descField.setValue(quest.getDescription() != null ? quest.getDescription() : "");
        deadlineField.setValue(String.valueOf(quest.getDeadlineTicks() / 1200));
        autoEnabled = quest.isAutoEnabled();
        autoToggleButton.setMessage(Component.literal(autoEnabled ? "§aON" : "§cOFF"));
        chanceField.setValue(String.valueOf(quest.getActivationChance()));

        requirementMode = quest.getRequirementMode();
        rewardMode = quest.getRewardMode();
        requirementModeButton.setMessage(Component.literal(requirementMode.name()));
        rewardModeButton.setMessage(Component.literal(rewardMode.name()));
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.fill(leftPos, topPos, leftPos + GUI_WIDTH, topPos + GUI_HEIGHT, 0xFF2D2D2D);
        graphics.fill(leftPos + 1, topPos + 1, leftPos + GUI_WIDTH - 1, topPos + GUI_HEIGHT - 1, 0xFF3B3B3B);
        graphics.fill(leftPos + 4, topPos + 18, leftPos + GUI_WIDTH - 4, topPos + 19, 0xFF5A5A5A);
        graphics.fill(leftPos + 4, topPos + 94, leftPos + GUI_WIDTH - 4, topPos + 95, 0xFF5A5A5A);
        graphics.fill(leftPos + 4, topPos + GUI_HEIGHT - 34, leftPos + GUI_WIDTH - 4, topPos + GUI_HEIGHT - 33, 0xFF5A5A5A);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);

        int x = leftPos + 6;
        int y = topPos + 22;
        int rowH = 20;

        graphics.drawString(font, "§c[관리자] §f납품 설정", leftPos + 6, topPos + 6, 0xFFFFFF, false);

        graphics.drawString(font, "§7임무 제목", x, y + 3, 0xAAAAAA, false); y += rowH;
        graphics.drawString(font, "§7설명", x, y + 3, 0xAAAAAA, false); y += rowH;
        graphics.drawString(font, "§7기한(분)", x, y + 3, 0xAAAAAA, false);
        graphics.drawString(font, "§8자동", x + 90, y + 3, 0x888888, false); y += rowH;
        graphics.drawString(font, "§7확률(%)", x, y + 3, 0xAAAAAA, false); y += rowH;
        graphics.drawString(font, "§7요구/보상 모드", x, y + 3, 0xAAAAAA, false); y += rowH;

        graphics.drawString(font, "§7요구 아이템 슬롯", x, topPos + 86, 0xAAAAAA, false);
        graphics.drawString(font, "§7보상 아이템 슬롯", x, topPos + 112, 0xAAAAAA, false);
        graphics.drawString(font, requiredTab ? "§a요구 등록 화면" : "§a보상 등록 화면", x, topPos + 138, 0xAAAAAA, false);
    }

    private void onSaveClicked() {
        DeliveryQuest quest = buildQuestFromFields();
        if (quest == null) return;
        PacketHandler.sendToServer(new C2SAdminSavePacket(menu.getBlockPos(), quest, false));
    }

    private void onActivateClicked() {
        onSaveClicked();
        PacketHandler.sendToServer(new C2SAdminSavePacket(menu.getBlockPos(), null, false, true));
    }

    private DeliveryQuest buildQuestFromFields() {
        String title = titleField.getValue().trim();
        if (title.isEmpty()) return null;

        var requiredStacks = menu.getRequiredStacksFromSlots();
        var rewardStacks = menu.getRewardStacksFromSlots();
        if (requiredStacks.isEmpty() || rewardStacks.isEmpty()) return null;

        int deadlineMin = parseIntSafe(deadlineField.getValue(), 0);
        int chance = parseIntSafe(chanceField.getValue(), 5);

        DeliveryQuest quest = new DeliveryQuest();
        quest.setTitle(title);
        quest.setDescription(descField.getValue().trim());
        quest.setRequiredStacks(requiredStacks);
        quest.setRewardStacks(rewardStacks);
        quest.setRequirementMode(requirementMode);
        quest.setRewardMode(rewardMode);
        quest.setDeadlineTicks(deadlineMin * 1200L);
        quest.setAutoEnabled(autoEnabled);
        quest.setActivationChance(Math.min(100, Math.max(0, chance)));
        return quest;
    }

    private EditBox newEditBox(int x, int y, int width) {
        EditBox box = new EditBox(font, x, y, width, 14, Component.empty());
        box.setMaxLength(64);
        return box;
    }

    private int parseIntSafe(String str, int fallback) {
        try {
            return Integer.parseInt(str);
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (minecraft != null && minecraft.options.keyInventory.matches(keyCode, scanCode)) {
            onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
    }
}
