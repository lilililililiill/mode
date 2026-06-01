package com.agrieconomy.client.screen;

import com.agrieconomy.AgriEconomy;
import com.agrieconomy.delivery.DeliveryMenu;
import com.agrieconomy.delivery.DeliveryQuest;
import com.agrieconomy.network.PacketHandler;
import com.agrieconomy.network.C2SDeliverPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/**
 * 납품 플레이어 UI Screen.
 *
 * <p>레이아웃:
 * <pre>
 * ┌───────────────────────────────┐
 * │  📦 긴급 식량 조달            │  ← 임무명
 * │  감자 64개를 납품하시오       │  ← 설명
 * ├───────────────────────────────┤
 * │  요구: minecraft:potato x64   │
 * │  진행: ████████░░  40/64      │  ← 진행 바
 * │  남은 시간: 03:42             │  ← 카운트다운
 * ├───────────────────────────────┤
 * │  납품 슬롯: [  ]  [납품하기]  │
 * ├───────────────────────────────┤
 * │  플레이어 인벤토리            │
 * └───────────────────────────────┘
 * </pre>
 * 임무 비활성 상태면 "현재 진행 중인 임무가 없습니다" 표시.
 * </p>
 */
public class DeliveryScreen extends AbstractContainerScreen<DeliveryMenu> {

    private static final int GUI_WIDTH  = 195;
    private static final int GUI_HEIGHT = 230;

    // 서버로부터 동기화된 퀘스트 데이터
    private DeliveryQuest currentQuest = null;
    private long remainingTicks = 0;

    private Button deliverButton;

    public DeliveryScreen(DeliveryMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth  = GUI_WIDTH;
        this.imageHeight = GUI_HEIGHT;
    }

    // ── 초기화 ────────────────────────────────────────────────────────

    @Override
    protected void init() {
        super.init();

        int x = leftPos;
        int y = topPos;

        // 납품 버튼
        deliverButton = addRenderableWidget(
                Button.builder(Component.literal("납품하기"), btn -> onDeliverClicked())
                        .pos(x + 110, y + 115)
                        .size(70, 16)
                        .build()
        );

        // 서버에 퀘스트 상태 동기화 요청
        PacketHandler.sendToServer(new C2SDeliverPacket(menu.getBlockPos(), "", 0, true));
    }

    // ── 동기화 ────────────────────────────────────────────────────────

    /** S2CDeliveryQuestSyncPacket에서 호출된다. */
    public void updateQuest(DeliveryQuest quest, long serverGameTime) {
        this.currentQuest = quest;
        if (quest != null && quest.isActive() && quest.getDeadlineTicks() > 0) {
            long elapsed = serverGameTime - quest.getStartTick();
            this.remainingTicks = Math.max(0, quest.getDeadlineTicks() - elapsed);
        } else {
            this.remainingTicks = 0;
        }
        if (deliverButton != null) {
            deliverButton.active = quest != null && quest.isActive();
        }
    }

    // ── 렌더링 ────────────────────────────────────────────────────────

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        // 배경
        graphics.fill(leftPos, topPos, leftPos + GUI_WIDTH, topPos + GUI_HEIGHT, 0xFF2D2D2D);
        graphics.fill(leftPos + 1, topPos + 1, leftPos + GUI_WIDTH - 1, topPos + GUI_HEIGHT - 1, 0xFF3B3B3B);
        // 구분선
        graphics.fill(leftPos + 4, topPos + 18, leftPos + GUI_WIDTH - 4, topPos + 19, 0xFF5A5A5A);
        graphics.fill(leftPos + 4, topPos + 108, leftPos + GUI_WIDTH - 4, topPos + 109, 0xFF5A5A5A);
        graphics.fill(leftPos + 4, topPos + 135, leftPos + GUI_WIDTH - 4, topPos + 136, 0xFF5A5A5A);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);

        int x = leftPos;
        int y = topPos;

        // 타이틀
        graphics.drawString(font, "§e납품 현황", x + 6, y + 6, 0xFFFFFF, false);

        if (currentQuest == null || !currentQuest.isActive()) {
            // 비활성 상태
            graphics.drawString(font, "§7현재 진행 중인 임무가 없습니다.", x + 6, y + 35, 0xAAAAAA, false);
            graphics.drawString(font, "§7잠시 후 새로운 임무가 등록됩니다.", x + 6, y + 48, 0xAAAAAA, false);
        } else {
            renderQuestInfo(graphics, x, y);
        }
    }

    private void renderQuestInfo(GuiGraphics graphics, int x, int y) {
        if (currentQuest == null) return;

        // 임무명
        graphics.drawString(font, "§f" + currentQuest.getTitle(), x + 6, y + 22, 0xFFFFFF, false);

        // 설명 (최대 2줄, 줄바꿈 처리)
        String desc = currentQuest.getDescription();
        if (desc != null && !desc.isEmpty()) {
            graphics.drawString(font, "§7" + truncate(desc, 28), x + 6, y + 33, 0xAAAAAA, false);
        }

        // 요구 품목
        String itemDisplay = currentQuest.getRequiredItem().contains(":")
                ? currentQuest.getRequiredItem().split(":")[1]
                : currentQuest.getRequiredItem();
        graphics.drawString(font,
                "§7요구: §f" + itemDisplay + " §7x" + currentQuest.getRequiredAmount(),
                x + 6, y + 50, 0xFFFFFF, false);

        // 진행 바
        int barX = x + 6;
        int barY = y + 63;
        int barW = 130;
        int barH = 8;
        float ratio = currentQuest.getRequiredAmount() > 0
                ? (float) currentQuest.getDeliveredAmount() / currentQuest.getRequiredAmount()
                : 0f;
        ratio = Math.min(ratio, 1f);

        graphics.fill(barX, barY, barX + barW, barY + barH, 0xFF1A1A1A);           // 배경
        graphics.fill(barX, barY, barX + (int)(barW * ratio), barY + barH, 0xFF5AAF5A); // 진행

        // 진행 수치
        String progressStr = currentQuest.getDeliveredAmount() + " / " + currentQuest.getRequiredAmount();
        graphics.drawString(font, "§f" + progressStr, barX + barW + 4, barY, 0xFFFFFF, false);

        // 남은 시간
        if (currentQuest.getDeadlineTicks() > 0) {
            String timeStr = formatTicks(remainingTicks);
            int timeColor = remainingTicks < 1200 ? 0xFFFF5555 : 0xFFFFAA00; // 1분 미만이면 빨간색
            graphics.drawString(font, "§7남은 시간: " + timeStr, x + 6, y + 78, timeColor, false);
        } else {
            graphics.drawString(font, "§7제한 시간: §f없음", x + 6, y + 78, 0xFFFFFF, false);
        }

        // 납품 슬롯 라벨
        graphics.drawString(font, "§7납품 슬롯", x + 6, y + 118, 0xAAAAAA, false);

        // 보상 미리보기
        String rewardItem = currentQuest.getRewardItem().contains(":")
                ? currentQuest.getRewardItem().split(":")[1]
                : currentQuest.getRewardItem();
        graphics.drawString(font,
                "§7보상: §a" + rewardItem + " x" + currentQuest.getRewardAmount(),
                x + 6, y + 95, 0xFFFFFF, false);
    }

    // ── 입력 처리 ─────────────────────────────────────────────────────

    private void onDeliverClicked() {
        if (currentQuest == null || !currentQuest.isActive()) return;

        var slot = menu.slots.get(0);
        if (!slot.hasItem()) return;

        var stack = slot.getItem();
        PacketHandler.sendToServer(
                new C2SDeliverPacket(menu.getBlockPos(),
                        currentQuest.getRequiredItem(),
                        stack.getCount(),
                        false));
    }

    // ── 유틸 ─────────────────────────────────────────────────────────

    /** 틱을 MM:SS 형식으로 변환한다. */
    private String formatTicks(long ticks) {
        long seconds = ticks / 20;
        long minutes = seconds / 60;
        long secs    = seconds % 60;
        return String.format("%02d:%02d", minutes, secs);
    }

    private String truncate(String str, int maxLen) {
        return str.length() > maxLen ? str.substring(0, maxLen - 1) + "…" : str;
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        // render()에서 직접 처리
    }
}