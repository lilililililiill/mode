package com.agrieconomy.client.screen;

import com.agrieconomy.delivery.DeliveryMenu;
import com.agrieconomy.delivery.DeliveryQuest;
import com.agrieconomy.network.C2SDeliverPacket;
import com.agrieconomy.network.PacketHandler;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * 납품 플레이어 UI Screen.
 */
public class DeliveryScreen extends AbstractContainerScreen<DeliveryMenu> {

    private static final int GUI_WIDTH = 210;
    private static final int GUI_HEIGHT = 230;

    private DeliveryQuest currentQuest = null;
    private long remainingTicks = 0;

    private Button deliverButton;
    private Button requiredChoiceButton;
    private Button rewardChoiceButton;

    private int requiredSelection = 0;
    private int rewardSelection = 0;

    public DeliveryScreen(DeliveryMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = GUI_WIDTH;
        this.imageHeight = GUI_HEIGHT;
    }

    @Override
    protected void init() {
        super.init();

        int x = leftPos;
        int y = topPos;

        deliverButton = addRenderableWidget(
                Button.builder(Component.literal("납품하기"), btn -> onDeliverClicked())
                        .pos(x + 130, y + 112)
                        .size(70, 16)
                        .build()
        );

        requiredChoiceButton = addRenderableWidget(
                Button.builder(Component.literal("요구 선택"), btn -> cycleRequiredChoice())
                        .pos(x + 130, y + 50)
                        .size(70, 16)
                        .build()
        );

        rewardChoiceButton = addRenderableWidget(
                Button.builder(Component.literal("보상 선택"), btn -> cycleRewardChoice())
                        .pos(x + 130, y + 92)
                        .size(70, 16)
                        .build()
        );

        PacketHandler.sendToServer(new C2SDeliverPacket(menu.getBlockPos(), requiredSelection, rewardSelection, true));
    }

    public void updateQuest(DeliveryQuest quest, long serverGameTime) {
        this.currentQuest = quest;

        if (quest != null) {
            requiredSelection = Math.min(requiredSelection, Math.max(0, quest.getRequiredStacks().size() - 1));
            rewardSelection = Math.min(rewardSelection, Math.max(0, quest.getRewardStacks().size() - 1));
            if (quest.getRequirementMode() == DeliveryQuest.RequirementMode.OR) {
                requiredSelection = Math.min(quest.getSelectedRequirementIndex(), Math.max(0, quest.getRequiredStacks().size() - 1));
            }
        } else {
            requiredSelection = 0;
            rewardSelection = 0;
        }

        if (quest != null && quest.isActive() && quest.getDeadlineTicks() > 0) {
            long elapsed = serverGameTime - quest.getStartTick();
            this.remainingTicks = Math.max(0, quest.getDeadlineTicks() - elapsed);
        } else {
            this.remainingTicks = 0;
        }

        if (deliverButton != null) {
            deliverButton.active = quest != null && quest.isActive();
        }
        refreshChoiceButtons();
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.fill(leftPos, topPos, leftPos + GUI_WIDTH, topPos + GUI_HEIGHT, 0xFF2D2D2D);
        graphics.fill(leftPos + 1, topPos + 1, leftPos + GUI_WIDTH - 1, topPos + GUI_HEIGHT - 1, 0xFF3B3B3B);
        graphics.fill(leftPos + 4, topPos + 18, leftPos + GUI_WIDTH - 4, topPos + 19, 0xFF5A5A5A);
        graphics.fill(leftPos + 4, topPos + 134, leftPos + GUI_WIDTH - 4, topPos + 135, 0xFF5A5A5A);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);

        int x = leftPos;
        int y = topPos;

        graphics.drawString(font, "§e납품 현황", x + 6, y + 6, 0xFFFFFF, false);

        if (currentQuest == null || !currentQuest.isActive()) {
            graphics.drawString(font, "§7현재 진행 중인 임무가 없습니다.", x + 6, y + 35, 0xAAAAAA, false);
            graphics.drawString(font, "§7잠시 후 새로운 임무가 등록됩니다.", x + 6, y + 48, 0xAAAAAA, false);
        } else {
            renderQuestInfo(graphics, x, y);
        }
    }

    private void renderQuestInfo(GuiGraphics graphics, int x, int y) {
        if (currentQuest == null) return;

        graphics.drawString(font, "§f" + currentQuest.getTitle(), x + 6, y + 22, 0xFFFFFF, false);

        String desc = currentQuest.getDescription();
        if (desc != null && !desc.isEmpty()) {
            graphics.drawString(font, "§7" + truncate(desc, 30), x + 6, y + 34, 0xAAAAAA, false);
        }

        List<ItemStack> reqs = currentQuest.getRequiredStacks();
        String reqMode = currentQuest.getRequirementMode() == DeliveryQuest.RequirementMode.AND ? "AND" : "OR";
        graphics.drawString(font, "§7요구모드: §f" + reqMode, x + 6, y + 48, 0xFFFFFF, false);
        if (!reqs.isEmpty()) {
            ItemStack shownReq = currentQuest.getRequirementMode() == DeliveryQuest.RequirementMode.OR
                    ? reqs.get(Math.min(requiredSelection, reqs.size() - 1))
                    : reqs.get(0);
            graphics.drawString(font, "§7요구: §f" + shownReq.getHoverName().getString() + " x" + shownReq.getCount(), x + 6, y + 60, 0xFFFFFF, false);
            if (currentQuest.getRequirementMode() == DeliveryQuest.RequirementMode.AND && reqs.size() > 1) {
                graphics.drawString(font, "§8+" + (reqs.size() - 1) + "개 추가 요구", x + 6, y + 71, 0x888888, false);
            }
        }

        int barX = x + 6;
        int barY = y + 82;
        int barW = 118;
        int barH = 8;
        float ratio = currentQuest.getTotalRequiredAmount() > 0
                ? (float) currentQuest.getTotalDeliveredAmount() / currentQuest.getTotalRequiredAmount()
                : 0f;
        ratio = Math.min(ratio, 1f);

        graphics.fill(barX, barY, barX + barW, barY + barH, 0xFF1A1A1A);
        graphics.fill(barX, barY, barX + (int) (barW * ratio), barY + barH, 0xFF5AAF5A);

        String progressStr = currentQuest.getTotalDeliveredAmount() + " / " + currentQuest.getTotalRequiredAmount();
        graphics.drawString(font, "§f" + progressStr, barX + barW + 4, barY, 0xFFFFFF, false);

        String rewardMode = currentQuest.getRewardMode() == DeliveryQuest.RewardMode.ALL ? "ALL" : "CHOICE";
        graphics.drawString(font, "§7보상모드: §f" + rewardMode, x + 6, y + 94, 0xFFFFFF, false);
        List<ItemStack> rewards = currentQuest.getRewardStacks();
        if (!rewards.isEmpty()) {
            ItemStack shownReward = rewards.get(Math.min(rewardSelection, rewards.size() - 1));
            graphics.drawString(font, "§7보상: §a" + shownReward.getHoverName().getString() + " x" + shownReward.getCount(), x + 6, y + 106, 0xFFFFFF, false);
            if (currentQuest.getRewardMode() == DeliveryQuest.RewardMode.ALL && rewards.size() > 1) {
                graphics.drawString(font, "§8+" + (rewards.size() - 1) + "개 추가 보상", x + 6, y + 117, 0x888888, false);
            }
        }

        if (currentQuest.getDeadlineTicks() > 0) {
            String timeStr = formatTicks(remainingTicks);
            int timeColor = remainingTicks < 1200 ? 0xFFFF5555 : 0xFFFFAA00;
            graphics.drawString(font, "§7남은 시간: " + timeStr, x + 6, y + 125, timeColor, false);
        } else {
            graphics.drawString(font, "§7제한 시간: §f없음", x + 6, y + 125, 0xFFFFFF, false);
        }
    }

    private void cycleRequiredChoice() {
        if (currentQuest == null || currentQuest.getRequiredStacks().isEmpty()) return;
        requiredSelection = (requiredSelection + 1) % currentQuest.getRequiredStacks().size();
    }

    private void cycleRewardChoice() {
        if (currentQuest == null || currentQuest.getRewardStacks().isEmpty()) return;
        rewardSelection = (rewardSelection + 1) % currentQuest.getRewardStacks().size();
    }

    private void refreshChoiceButtons() {
        if (requiredChoiceButton != null) {
            boolean showReq = currentQuest != null
                    && currentQuest.isActive()
                    && currentQuest.getRequirementMode() == DeliveryQuest.RequirementMode.OR
                    && currentQuest.getRequiredStacks().size() > 1;
            requiredChoiceButton.visible = showReq;
            requiredChoiceButton.active = showReq;
        }
        if (rewardChoiceButton != null) {
            boolean showReward = currentQuest != null
                    && currentQuest.isActive()
                    && currentQuest.getRewardMode() == DeliveryQuest.RewardMode.CHOICE
                    && currentQuest.getRewardStacks().size() > 1;
            rewardChoiceButton.visible = showReward;
            rewardChoiceButton.active = showReward;
        }
    }

    private void onDeliverClicked() {
        if (currentQuest == null || !currentQuest.isActive()) return;
        PacketHandler.sendToServer(new C2SDeliverPacket(menu.getBlockPos(), requiredSelection, rewardSelection, false));
    }

    private String formatTicks(long ticks) {
        long seconds = ticks / 20;
        long minutes = seconds / 60;
        long secs = seconds % 60;
        return String.format("%02d:%02d", minutes, secs);
    }

    private String truncate(String str, int maxLen) {
        return str.length() > maxLen ? str.substring(0, maxLen - 1) + "…" : str;
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
