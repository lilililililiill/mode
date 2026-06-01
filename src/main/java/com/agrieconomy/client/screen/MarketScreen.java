package com.agrieconomy.client.screen;

import com.agrieconomy.market.MarketEntry;
import com.agrieconomy.market.MarketMenu;
import com.agrieconomy.network.C2SMarketAdminPacket;
import com.agrieconomy.network.C2SSellPacket;
import com.agrieconomy.network.PacketHandler;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import java.util.ArrayList;
import java.util.List;

/**
 * 거래소 GUI Screen.
 */
public class MarketScreen extends AbstractContainerScreen<MarketMenu> {

    private static final int GUI_WIDTH = 210;
    private static final int GUI_HEIGHT = 250;

    private static final int ENTRY_START_Y = 22;
    private static final int ENTRY_HEIGHT = 22;
    private static final int PAGE_SIZE = MarketMenu.PAGE_SIZE;

    private int currentPage = 0;
    private List<MarketEntry> entries = new ArrayList<>();

    private Button prevButton;
    private Button nextButton;
    private EditBox basePriceField;
    private EditBox minPriceField;
    private EditBox maxPriceField;

    public MarketScreen(MarketMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = GUI_WIDTH;
        this.imageHeight = GUI_HEIGHT;
    }

    @Override
    protected void init() {
        super.init();

        int x = this.leftPos;
        int y = this.topPos;

        prevButton = addRenderableWidget(
                Button.builder(Component.literal("◀"), btn -> {
                            if (currentPage > 0) {
                                currentPage--;
                            }
                        })
                        .pos(x + GUI_WIDTH - 62, y + 4)
                        .size(18, 12)
                        .build()
        );

        nextButton = addRenderableWidget(
                Button.builder(Component.literal("▶"), btn -> {
                            if ((currentPage + 1) * PAGE_SIZE < entries.size()) {
                                currentPage++;
                            }
                        })
                        .pos(x + GUI_WIDTH - 22, y + 4)
                        .size(18, 12)
                        .build()
        );

        basePriceField = new EditBox(font, x + 12, y + 196, 42, 14, Component.empty());
        basePriceField.setFilter(s -> s.matches("\\d*"));
        basePriceField.setValue("100");
        addRenderableWidget(basePriceField);

        minPriceField = new EditBox(font, x + 58, y + 196, 42, 14, Component.empty());
        minPriceField.setFilter(s -> s.matches("\\d*"));
        minPriceField.setValue("50");
        addRenderableWidget(minPriceField);

        maxPriceField = new EditBox(font, x + 104, y + 196, 42, 14, Component.empty());
        maxPriceField.setFilter(s -> s.matches("\\d*"));
        maxPriceField.setValue("200");
        addRenderableWidget(maxPriceField);

        addRenderableWidget(Button.builder(Component.literal("등록"), btn -> onRegisterClicked())
                .pos(x + 150, y + 195).size(48, 16).build());
        addRenderableWidget(Button.builder(Component.literal("삭제"), btn -> onRemoveClicked())
                .pos(x + 150, y + 214).size(48, 16).build());

        PacketHandler.sendToServer(new C2SSellPacket(menu.getBlockPos(), -1, 0, true));
    }

    public void updateEntries(List<MarketEntry> newEntries) {
        this.entries = newEntries;
        this.currentPage = 0;
        updatePageButtons();
    }

    private void updatePageButtons() {
        if (prevButton != null) prevButton.active = currentPage > 0;
        if (nextButton != null) nextButton.active = (currentPage + 1) * PAGE_SIZE < entries.size();
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        graphics.fill(leftPos, topPos, leftPos + GUI_WIDTH, topPos + GUI_HEIGHT, 0xFF2D2D2D);
        graphics.fill(leftPos + 1, topPos + 1, leftPos + GUI_WIDTH - 1, topPos + GUI_HEIGHT - 1, 0xFF3B3B3B);

        graphics.fill(leftPos + 4, topPos + 18, leftPos + GUI_WIDTH - 4, topPos + 19, 0xFF5A5A5A);

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

        graphics.drawString(font, "§6농업 거래소", x + 6, y + 6, 0xFFFFFF, false);

        int totalPages = Math.max(1, (int) Math.ceil((double) entries.size() / PAGE_SIZE));
        String pageStr = (currentPage + 1) + "/" + totalPages;
        graphics.drawString(font, pageStr, x + GUI_WIDTH - 46, y + 6, 0xAAAAAA, false);

        int startIdx = currentPage * PAGE_SIZE;
        for (int i = 0; i < PAGE_SIZE; i++) {
            int idx = startIdx + i;
            int rowY = y + ENTRY_START_Y + i * ENTRY_HEIGHT;

            if (idx < entries.size()) {
                renderEntryRow(graphics, entries.get(idx), x + 4, rowY, mouseX, mouseY);
            } else {
                graphics.fill(x + 4, rowY, x + GUI_WIDTH - 4, rowY + ENTRY_HEIGHT - 1, 0xFF2A2A2A);
            }
        }

        int slotY = y + ENTRY_START_Y + PAGE_SIZE * ENTRY_HEIGHT + 6;
        graphics.drawString(font, "§7판매/등록 슬롯", x + 6, slotY, 0xAAAAAA, false);

        graphics.drawString(font, "§7거래소 등록 (실물 아이템 + 가격)", x + 8, y + 184, 0xAAAAAA, false);
        graphics.drawString(font, "§8기준", x + 18, y + 187, 0x888888, false);
        graphics.drawString(font, "§8최소", x + 64, y + 187, 0x888888, false);
        graphics.drawString(font, "§8최대", x + 110, y + 187, 0x888888, false);

        updatePageButtons();
    }

    private void renderEntryRow(GuiGraphics graphics, MarketEntry entry,
                                int x, int y, int mouseX, int mouseY) {
        boolean hovered = mouseX >= x && mouseX < x + GUI_WIDTH - 8
                && mouseY >= y && mouseY < y + ENTRY_HEIGHT - 1;
        int bgColor = hovered ? 0xFF454545 : 0xFF383838;
        graphics.fill(x, y, x + GUI_WIDTH - 8, y + ENTRY_HEIGHT - 1, bgColor);

        String displayName = entry.getTradedStack().getHoverName().getString();
        graphics.drawString(font, "§f" + displayName, x + 3, y + 3, 0xFFFFFF, false);

        String priceInfo = String.format("§7기준:§f%d  §7현재:§f%d", entry.getBasePrice(), entry.getCurrentPrice());
        graphics.drawString(font, priceInfo, x + 3, y + 12, 0xFFFFFF, false);

        int change = entry.getChangePercent();
        String changeStr = String.format("%+d%%", change);
        int changeColor = change > 0 ? 0xFF55FF55 : change < 0 ? 0xFFFF5555 : 0xFFAAAAAA;
        graphics.drawString(font, changeStr, x + GUI_WIDTH - 60, y + 7, changeColor, false);

        int btnX = x + GUI_WIDTH - 36;
        int btnY = y + 4;
        boolean btnHovered = mouseX >= btnX && mouseX < btnX + 28
                && mouseY >= btnY && mouseY < btnY + 13;
        graphics.fill(btnX, btnY, btnX + 28, btnY + 13, btnHovered ? 0xFF5AAF5A : 0xFF3A8A3A);
        graphics.drawString(font, "§f판매", btnX + 5, btnY + 3, 0xFFFFFF, false);
    }

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
                    onSellButtonClicked(idx, entries.get(idx));
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void onSellButtonClicked(int entryIndex, MarketEntry entry) {
        var sellSlot = menu.slots.get(MarketMenu.SELL_SLOT_INDEX);
        if (sellSlot == null || !sellSlot.hasItem()) return;

        var stack = sellSlot.getItem();
        int amount = stack.getCount();

        PacketHandler.sendToServer(new C2SSellPacket(menu.getBlockPos(), entryIndex, amount, false));
    }

    private void onRegisterClicked() {
        int base = parseIntSafe(basePriceField.getValue(), 100);
        int min = parseIntSafe(minPriceField.getValue(), 50);
        int max = parseIntSafe(maxPriceField.getValue(), 200);
        PacketHandler.sendToServer(new C2SMarketAdminPacket(menu.getBlockPos(), C2SMarketAdminPacket.Action.SAVE, base, min, max));
    }

    private void onRemoveClicked() {
        PacketHandler.sendToServer(new C2SMarketAdminPacket(menu.getBlockPos(), C2SMarketAdminPacket.Action.REMOVE, 0, 0, 0));
    }

    private int parseIntSafe(String value, int fallback) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ignored) {
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
