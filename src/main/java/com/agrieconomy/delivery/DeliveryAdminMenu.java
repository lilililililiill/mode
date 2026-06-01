package com.agrieconomy.delivery;

import com.agrieconomy.core.ModMenuTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * 납품 블록 관리자 UI 메뉴.
 */
public class DeliveryAdminMenu extends AbstractContainerMenu {

    public static final int REQUIRED_SLOT_START = 0;
    public static final int REQUIRED_SLOT_COUNT = 9;
    public static final int REWARD_SLOT_START = REQUIRED_SLOT_START + REQUIRED_SLOT_COUNT;
    public static final int REWARD_SLOT_COUNT = 9;

    private final BlockPos blockPos;
    private final SimpleContainer requiredContainer = new SimpleContainer(REQUIRED_SLOT_COUNT);
    private final SimpleContainer rewardContainer = new SimpleContainer(REWARD_SLOT_COUNT);

    public DeliveryAdminMenu(int containerId, Inventory playerInventory, BlockPos pos) {
        super(ModMenuTypes.DELIVERY_ADMIN_MENU.get(), containerId);
        this.blockPos = pos;

        if (playerInventory.player.level() instanceof ServerLevel level) {
            DeliveryQuest quest = DeliveryManager.get(level).getQuest(pos);
            if (quest != null) {
                for (int i = 0; i < Math.min(REQUIRED_SLOT_COUNT, quest.getRequiredStacks().size()); i++) {
                    requiredContainer.setItem(i, quest.getRequiredStacks().get(i).copy());
                }
                for (int i = 0; i < Math.min(REWARD_SLOT_COUNT, quest.getRewardStacks().size()); i++) {
                    rewardContainer.setItem(i, quest.getRewardStacks().get(i).copy());
                }
            }
        }

        for (int i = 0; i < REQUIRED_SLOT_COUNT; i++) {
            addSlot(new Slot(requiredContainer, i, 8 + i * 18, 96));
        }
        for (int i = 0; i < REWARD_SLOT_COUNT; i++) {
            addSlot(new Slot(rewardContainer, i, 8 + i * 18, 122));
        }

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 154 + row * 18));
            }
        }

        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(playerInventory, col, 8 + col * 18, 212));
        }
    }

    public DeliveryAdminMenu(int id, Inventory inv, net.minecraft.network.FriendlyByteBuf buf) {
        this(id, inv, buf.readBlockPos());
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (slot == null || !slot.hasItem()) return ItemStack.EMPTY;

        ItemStack stack = slot.getItem();
        result = stack.copy();

        int playerStart = REWARD_SLOT_START + REWARD_SLOT_COUNT;
        int playerEnd = slots.size();

        if (index < playerStart) {
            if (!moveItemStackTo(stack, playerStart, playerEnd, true)) return ItemStack.EMPTY;
        } else {
            if (!moveItemStackTo(stack, REQUIRED_SLOT_START, playerStart, false)) return ItemStack.EMPTY;
        }

        if (stack.isEmpty()) slot.set(ItemStack.EMPTY);
        else slot.setChanged();

        return result;
    }

    @Override
    public boolean stillValid(Player player) {
        return player.hasPermissions(2) &&
                player.distanceToSqr(blockPos.getX() + 0.5, blockPos.getY() + 0.5, blockPos.getZ() + 0.5) < 64.0;
    }

    public List<ItemStack> getRequiredStacksFromSlots() {
        List<ItemStack> stacks = new ArrayList<>();
        for (int i = 0; i < REQUIRED_SLOT_COUNT; i++) {
            ItemStack stack = slots.get(REQUIRED_SLOT_START + i).getItem();
            if (!stack.isEmpty()) stacks.add(stack.copy());
        }
        return stacks;
    }

    public List<ItemStack> getRewardStacksFromSlots() {
        List<ItemStack> stacks = new ArrayList<>();
        for (int i = 0; i < REWARD_SLOT_COUNT; i++) {
            ItemStack stack = slots.get(REWARD_SLOT_START + i).getItem();
            if (!stack.isEmpty()) stacks.add(stack.copy());
        }
        return stacks;
    }

    public BlockPos getBlockPos() { return blockPos; }
}
