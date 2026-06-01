package com.agrieconomy.client.screen;

import com.agrieconomy.delivery.DeliveryAdminMenu;
import com.agrieconomy.delivery.DeliveryQuest;
import com.agrieconomy.network.PacketHandler;
import com.agrieconomy.network.C2SAdminSavePacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/**
 * 납품 블록 관리자 설정 UI.
 *
 * <p>설정 가능 항목:
 * <ul>
 *   <li>임무 제목</li>
 *   <li>설명</li>
 *   <li>요구 아이템 ID</li>
 *   <li>요구 수량</li>
 *   <li>보상 아이템 ID</li>
 *   <li>보상 수량</li>
 *   <li>기한 (분 단위, 0 = 무제한)</li>
 *   <li>자동 활성화 여부 (토글 버튼)</li>
 *   <li>자동 활성화 확률 (%)</li>
 * </ul>
 * </p>
 *
 * <p>레이아웃:
 * <pre>
 * ┌──────────────────────────────────┐
 * │  납품 설정                       │
 * ├──────────────────────────────────┤
 * │  임무 제목  [________________]   │
 * │  설명       [________________]   │
 * │  요구 아이템[________________]   │
 * │  요구 수량  [____]               │
 * │  보상 아이템[________________]   │
 * │  보상 수량  [____]               │
 * │  기한(분)   [____]  0=무제한     │
 * │  자동활성화 [ON/OFF]  확률[___]% │
 * ├──────────────────────────────────┤
 * │         [저장]   [즉시 활성화]   │
 * └──────────────────────────────────┘
 * </pre>
 * </p>
 */
public class DeliveryAdminScreen extends AbstractContainerScreen<com.agrieconomy.delivery.DeliveryAdminMenu> {

    private static final int GUI_WIDTH  = 220;
    private static final int GUI_HEIGHT = 240;

    // 입력 필드
    private EditBox titleField;
    private EditBox descField;
    private EditBox reqItemField;
    private EditBox reqAmountField;
    private EditBox rewardItemField;
    private EditBox rewardAmountField;
    private EditBox deadlineField;
    private EditBox chanceField;

    // 자동 활성화 토글
    private boolean autoEnabled = false;
    private Button autoToggleButton;

    // 버튼
    private Button saveButton;
    private Button activateButton;

    public DeliveryAdminScreen(DeliveryAdminMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth  = GUI_WIDTH;
        this.imageHeight = GUI_HEIGHT;
    }

    // ── 초기화 ────────────────────────────────────────────────────────

    @Override
    protected void init() {
        super.init();

        int x = leftPos + 6;
        int y = topPos + 22;
        int fieldW = 120;
        int rowH   = 20;

        // 임무 제목
        titleField = newEditBox(x + 80, y, fieldW);
        addRenderableWidget(titleField);
        y += rowH;

        // 설명
        descField = newEditBox(x + 80, y, fieldW);
        addRenderableWidget(descField);
        y += rowH;

        // 요구 아이템
        reqItemField = newEditBox(x + 80, y, fieldW);
        addRenderableWidget(reqItemField);
        y += rowH;

        // 요구 수량
        reqAmountField = newEditBox(x + 80, y, 40);
        reqAmountField.setFilter(s -> s.matches("\\d*"));
        addRenderableWidget(reqAmountField);
        y += rowH;

        // 보상 아이템
        rewardItemField = newEditBox(x + 80, y, fieldW);
        addRenderableWidget(rewardItemField);
        y += rowH;

        // 보상 수량
        rewardAmountField = newEditBox(x + 80, y, 40);
        rewardAmountField.setFilter(s -> s.matches("\\d*"));
        addRenderableWidget(rewardAmountField);
        y += rowH;

        // 기한 (분)
        deadlineField = newEditBox(x + 80, y, 40);
        deadlineField.setFilter(s -> s.matches("\\d*"));
        deadlineField.setValue("0");
        addRenderableWidget(deadlineField);
        y += rowH;

        // 자동 활성화 토글
        autoToggleButton = addRenderableWidget(
                Button.builder(Component.literal("OFF"), btn -> {
                            autoEnabled = !autoEnabled;
                            btn.setMessage(Component.literal(autoEnabled ? "§aON" : "§cOFF"));
                        })
                        .pos(x + 80, y)
                        .size(36, 14)
                        .build()
        );

        // 자동 활성화 확률
        chanceField = newEditBox(x + 130, y, 30);
        chanceField.setFilter(s -> s.matches("\\d*") && (s.isEmpty() || Integer.parseInt(s) <= 100));
        chanceField.setValue("5");
        addRenderableWidget(chanceField);
        y += rowH + 6;

        // 저장 버튼
        saveButton = addRenderableWidget(
                Button.builder(Component.literal("§f저장"), btn -> onSaveClicked())
                        .pos(leftPos + 30, topPos + GUI_HEIGHT - 28)
                        .size(60, 18)
                        .build()
        );

        // 즉시 활성화 버튼
        activateButton = addRenderableWidget(
                Button.builder(Component.literal("§e즉시 활성화"), btn -> onActivateClicked())
                        .pos(leftPos + 110, topPos + GUI_HEIGHT - 28)
                        .size(82, 18)
                        .build()
        );

        // 서버에서 현재 설정 불러오기
        PacketHandler.sendToServer(new C2SAdminSavePacket(menu.getBlockPos(), null, true));
    }

    /** 서버에서 받아온 기존 설정으로 필드를 채운다 (C2SAdminSavePacket 응답 시 호출). */
    public void populateFields(DeliveryQuest quest) {
        if (quest == null) return;
        titleField.setValue(quest.getTitle() != null ? quest.getTitle() : "");
        descField.setValue(quest.getDescription() != null ? quest.getDescription() : "");
        reqItemField.setValue(quest.getRequiredItem() != null ? quest.getRequiredItem() : "");
        reqAmountField.setValue(String.valueOf(quest.getRequiredAmount()));
        rewardItemField.setValue(quest.getRewardItem() != null ? quest.getRewardItem() : "");
        rewardAmountField.setValue(String.valueOf(quest.getRewardAmount()));
        deadlineField.setValue(String.valueOf(quest.getDeadlineTicks() / 1200)); // 틱→분
        autoEnabled = quest.isAutoEnabled();
        autoToggleButton.setMessage(Component.literal(autoEnabled ? "§aON" : "§cOFF"));
        chanceField.setValue(String.valueOf(quest.getActivationChance()));
    }

    // ── 렌더링 ────────────────────────────────────────────────────────

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.fill(leftPos, topPos, leftPos + GUI_WIDTH, topPos + GUI_HEIGHT, 0xFF2D2D2D);
        graphics.fill(leftPos + 1, topPos + 1, leftPos + GUI_WIDTH - 1, topPos + GUI_HEIGHT - 1, 0xFF3B3B3B);
        graphics.fill(leftPos + 4, topPos + 18, leftPos + GUI_WIDTH - 4, topPos + 19, 0xFF5A5A5A);
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

        // 필드 라벨 렌더링
        String[] labels = {"임무 제목", "설명", "요구 아이템", "요구 수량", "보상 아이템", "보상 수량", "기한(분)"};
        for (String label : labels) {
            graphics.drawString(font, "§7" + label, x, y + 3, 0xAAAAAA, false);
            y += rowH;
        }

        // 자동 활성화 라벨
        graphics.drawString(font, "§7자동 활성화", x, y + 3, 0xAAAAAA, false);
        graphics.drawString(font, "§7확률", x + 120, y + 3, 0xAAAAAA, false);
        graphics.drawString(font, "§7%", leftPos + 164, y + 3, 0xAAAAAA, false);

        // 기한 0=무제한 힌트
        graphics.drawString(font, "§80=무제한", leftPos + 126, topPos + 22 + rowH * 6 + 3, 0x888888, false);
    }

    // ── 버튼 처리 ─────────────────────────────────────────────────────

    private void onSaveClicked() {
        DeliveryQuest quest = buildQuestFromFields();
        if (quest == null) return;
        PacketHandler.sendToServer(new C2SAdminSavePacket(menu.getBlockPos(), quest, false));
    }

    private void onActivateClicked() {
        // 저장 후 즉시 활성화
        onSaveClicked();
        PacketHandler.sendToServer(new C2SAdminSavePacket(menu.getBlockPos(), null, false, true));
    }

    /** 입력 필드에서 DeliveryQuest 객체를 생성한다. 유효성 검사 포함. */
    private DeliveryQuest buildQuestFromFields() {
        String title      = titleField.getValue().trim();
        String desc       = descField.getValue().trim();
        String reqItem    = reqItemField.getValue().trim();
        String rewardItem = rewardItemField.getValue().trim();

        if (title.isEmpty() || reqItem.isEmpty() || rewardItem.isEmpty()) return null;

        int reqAmount    = parseIntSafe(reqAmountField.getValue(), 1);
        int rewardAmount = parseIntSafe(rewardAmountField.getValue(), 1);
        int deadlineMin  = parseIntSafe(deadlineField.getValue(), 0);
        int chance       = parseIntSafe(chanceField.getValue(), 5);

        DeliveryQuest quest = new DeliveryQuest();
        quest.setTitle(title);
        quest.setDescription(desc);
        quest.setRequiredItem(reqItem);
        quest.setRequiredAmount(reqAmount);
        quest.setRewardItem(rewardItem);
        quest.setRewardAmount(rewardAmount);
        quest.setDeadlineTicks(deadlineMin * 1200L); // 분 → 틱
        quest.setAutoEnabled(autoEnabled);
        quest.setActivationChance(Math.min(100, Math.max(0, chance)));
        return quest;
    }

    // ── 유틸 ─────────────────────────────────────────────────────────

    private EditBox newEditBox(int x, int y, int width) {
        EditBox box = new EditBox(font, x, y, width, 14, Component.empty());
        box.setMaxLength(64);
        return box;
    }

    private int parseIntSafe(String str, int fallback) {
        try { return Integer.parseInt(str); }
        catch (NumberFormatException e) { return fallback; }
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        // render()에서 직접 처리
    }
}