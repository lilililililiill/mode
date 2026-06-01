package com.agrieconomy.client.screen;

import com.agrieconomy.AgriEconomy;
import com.agrieconomy.market.MarketEntry;
import com.agrieconomy.market.MarketMenu;
import com.agrieconomy.network.PacketHandler;
import com.agrieconomy.network.C2SSellPacket;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

import java.util.ArrayList;
import java.util.List;

/**
 * 거래소 GUI Screen.
 *
 * <p>레이아웃:
 * <pre>
 * ┌─────────────────────────────────────┐
 * │  농업 거래소          [◀] 1/3 [▶]  │
 * ├─────────────────────────────────────┤
 * │  🌾 minecraft:wheat                 │
 * │     기준: 100  현재: 85  변동: -15% │  ← 품목 행 (최대 7개)
 * │                          [판매]     │
 * ├─────────────────────────────────────┤
 * │  판매 슬롯: [  ]                    │
 * ├─────────────────────────────────────┤
 * │  플레이어 인벤토리                  │
 * └─────────────────────────────────────┘
 * </pre>
 * </p>
 */
public class MarketScreen extends AbstractContainerScreen<MarketMenu> {

    // 텍스처
    private static final ResourceLocation TEXTURE =
            new ResourceLocation(AgriEconomy.MOD_ID, "textures/gui/market.png");

    // GUI 크기
    private static final int GUI_WIDTH  = 210;
    private static final int GUI_HEIGHT = 220;

    // 품목 행 레이아웃
    private static final int ENTRY_START_Y  = 22;
    private static final int ENTRY_HEIGHT   = 22;
    private static final int PAGE_SIZE      = MarketMenu.PAGE_SIZE; // 7

    // 상태
    private int currentPage = 0;
    private List<MarketEntry> entries = new ArrayList<>();

    // 버튼
    private Button prevButton;
    private Button nextButton;

    public MarketScreen(MarketMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth  = GUI_WIDTH;
        this.imageHeight = GUI_HEIGHT;
    }

    // ── 초기화 ────────────────────────────────────────────────────────

    @Override
    protected void init() {
        super.init();

        int x = this.leftPos;
        int y = this.topPos;

        // 이전 페이지 버튼
        prevButton = addRenderableWidget(
                Button.builder(Component.literal("◀"), btn -> {
                            if (currentPage > 0) {
                                currentPage--;
                                rebuildSellButtons();
                            }
                        })
                        .pos(x + GUI_WIDTH - 62, y + 4)
                        .size(18, 12)
                        .build()
        );

        // 다음 페이지 버튼
        nextButton = addRenderableWidget(
                Button.builder(Component.literal("▶"), btn -> {
                            if ((currentPage + 1) * PAGE_SIZE < entries.size()) {
                                currentPage++;
                                rebuildSellButtons();
                            }
                        })
                        .pos(x + GUI_WIDTH - 22, y + 4)
                        .size(18, 12)
                        .build()
        );

        // 서버로부터 품목 목록 동기화 요청
        PacketHandler.sendToServer(new C2SSellPacket(menu.getBlockPos(), "", 0, true));
    }

    /** 현재 페이지의 판매 버튼 목록을 재구성한다. */
    private void rebuildSellButtons() {
        // 기존 판매 버튼 제거 후 재생성은 renderables에서 관리하기 복잡하므로
        // render()에서 동적으로 그리는 방식 사용 (버튼은 init에서 고정 위치로 처리)
    }

    // ── 동기화 ────────────────────────────────────────────────────────

    /** 서버로부터 품목 목록을 수신하면 호출된다 (S2CMarketSyncPacket에서 호출). */
    public void updateEntries(List<MarketEntry> newEntries) {
        this.entries = newEntries;
        this.currentPage = 0;
        updatePageButtons();
    }

    private void updatePageButtons() {
        if (prevButton != null) prevButton.active = currentPage > 0;
        if (nextButton != null) nextButton.active = (currentPage + 1) * PAGE_SIZE < entries.size();
    }

    // ── 렌더링 ────────────────────────────────────────────────────────

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        // 배경 패널 (텍스처 없으면 단색 fallback)
        graphics.fill(leftPos, topPos, leftPos + GUI_WIDTH, topPos + GUI_HEIGHT, 0xFF2D2D2D);
        graphics.fill(leftPos + 1, topPos + 1, leftPos + GUI_WIDTH - 1, topPos + GUI_HEIGHT - 1, 0xFF3B3B3B);

        // 헤더 구분선
        graphics.fill(leftPos + 4, topPos + 18, leftPos + GUI_WIDTH - 4, topPos + 19, 0xFF5A5A5A);

        // 판매 슬롯 구역 구분선
        int slotAreaY = topPos + ENTRY_START_Y + PAGE_SIZE * ENTRY_HEIGHT + 4;
        graphics.fill(leftPos + 4, slotAreaY, leftPos + GUI_WIDTH - 4, slotAreaY + 1, 0xFF5A5A5A);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);

        int x = leftPos;
        int y = topPos;

        // 타이틀
        graphics.drawString(font, "§6농업 거래소", x + 6, y + 6, 0xFFFFFF, false);

        // 페이지 표시
        int totalPages = Math.max(1, (int) Math.ceil((double) entries.size() / PAGE_SIZE));
        String pageStr = (currentPage + 1) + "/" + totalPages;
        graphics.drawString(font, pageStr, x + GUI_WIDTH - 46, y + 6, 0xAAAAAA, false);

        // 품목 행 렌더링
        int startIdx = currentPage * PAGE_SIZE;
        for (int i = 0; i < PAGE_SIZE; i++) {
            int idx = startIdx + i;
            int rowY = y + ENTRY_START_Y + i * ENTRY_HEIGHT;

            if (idx < entries.size()) {
                renderEntryRow(graphics, entries.get(idx), x + 4, rowY, mouseX, mouseY);
            } else {
                // 빈 행
                graphics.fill(x + 4, rowY, x + GUI_WIDTH - 4, rowY + ENTRY_HEIGHT - 1, 0xFF2A2A2A);
            }
        }

        // 판매 슬롯 라벨
        int slotY = y + ENTRY_START_Y + PAGE_SIZE * ENTRY_HEIGHT + 6;
        graphics.drawString(font, "§7판매 슬롯", x + 6, slotY, 0xAAAAAA, false);

        updatePageButtons();
    }

    /**
     * 단일 품목 행을 렌더링한다.
     *
     * <pre>
     * [아이템명]  기준:XXX  현재:YYY  [+/-ZZ%]  [판매]
     * </pre>
     */
    private void renderEntryRow(GuiGraphics graphics, MarketEntry entry,
                                int x, int y, int mouseX, int mouseY) {
        // 행 배경 (hover 강조)
        boolean hovered = mouseX >= x && mouseX < x + GUI_WIDTH - 8
                && mouseY >= y && mouseY < y + ENTRY_HEIGHT - 1;
        int bgColor = hovered ? 0xFF454545 : 0xFF383838;
        graphics.fill(x, y, x + GUI_WIDTH - 8, y + ENTRY_HEIGHT - 1, bgColor);

        // 아이템 ID (minecraft: 접두사 제거해서 표시)
        String displayName = entry.getItemId().contains(":")
                ? entry.getItemId().split(":")[1]
                : entry.getItemId();
        graphics.drawString(font, "§f" + displayName, x + 3, y + 3, 0xFFFFFF, false);

        // 가격 정보
        String priceInfo = String.format("§7기준:§f%d  §7현재:§f%d", entry.getBasePrice(), entry.getCurrentPrice());
        graphics.drawString(font, priceInfo, x + 3, y + 12, 0xFFFFFF, false);

        // 변동률 (색상 분기)
        int change = entry.getChangePercent();
        String changeStr = String.format("%+d%%", change);
        int changeColor = change > 0 ? 0xFF55FF55 : change < 0 ? 0xFFFF5555 : 0xFFAAAAAA;
        graphics.drawString(font, changeStr, x + GUI_WIDTH - 60, y + 7, changeColor, false);

        // 판매 버튼 영역 (클릭 감지는 mouseClicked에서)
        int btnX = x + GUI_WIDTH - 36;
        int btnY = y + 4;
        boolean btnHovered = mouseX >= btnX && mouseX < btnX + 28
                && mouseY >= btnY && mouseY < btnY + 13;
        graphics.fill(btnX, btnY, btnX + 28, btnY + 13, btnHovered ? 0xFF5AAF5A : 0xFF3A8A3A);
        graphics.drawString(font, "§f판매", btnX + 5, btnY + 3, 0xFFFFFF, false);
    }

    // ── 입력 처리 ─────────────────────────────────────────────────────

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            int startIdx = currentPage * PAGE_SIZE;
            for (int i = 0; i < PAGE_SIZE; i++) {
                int idx = startIdx + i;
                if (idx >= entries.size()) break;

                int rowY = topPos + ENTRY_START_Y + i * ENTRY_HEIGHT;
                int btnX = leftPos + GUI_WIDTH - 40;
                int btnY = rowY + 4;

                if (mouseX >= btnX && mouseX < btnX + 28
                        && mouseY >= btnY && mouseY < btnY + 13) {
                    onSellButtonClicked(entries.get(idx));
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    /**
     * 판매 버튼 클릭 처리.
     * 판매 슬롯에 있는 아이템을 서버로 전송한다.
     */
    private void onSellButtonClicked(MarketEntry entry) {
        var sellSlot = menu.slots.get(MarketMenu.SELL_SLOT_INDEX);
        if (sellSlot == null || !sellSlot.hasItem()) return;

        var stack = sellSlot.getItem();
        int amount = stack.getCount();
        // CropGrade NBT에서 배율 읽기
        String gradeStr = stack.hasTag() ? stack.getTag().getString("CropGrade") : "NORMAL";

        PacketHandler.sendToServer(
                new C2SSellPacket(menu.getBlockPos(), entry.getItemId(), amount, false));
    }

    // ── 제목 렌더링 오버라이드 (super에서 기본 title 렌더를 막기 위해) ──

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        // render()에서 직접 처리하므로 super 호출 생략
    }
}