package com.agrieconomy.delivery;

import com.agrieconomy.core.ModMenuTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.SimpleContainer;

/**
 * 납품 플레이어 UI 컨테이너.
 * Shift+클릭으로 인벤토리 아이템을 납품 슬롯으로 이동할 수 있다.
 */
public class DeliveryMenu extends AbstractContainerMenu {

    private static final int DELIVER_SLOT  = 0;
    private static final int INV_START     = 1;
    private static final int INV_END       = 28;   // 27 + 1
    private static final int HOTBAR_START  = 28;
    private static final int HOTBAR_END    = 37;   // 9 + 28

    private final BlockPos blockPos;
    private final SimpleContainer deliveryContainer = new SimpleContainer(1);

    public DeliveryMenu(int containerId, Inventory playerInventory, BlockPos pos) {
        super(ModMenuTypes.DELIVERY_MENU.get(), containerId);
        this.blockPos = pos;

        // 납품 슬롯 (슬롯 0)
        addSlot(new Slot(deliveryContainer, 0, 80, 35));

        // 플레이어 인벤토리 (슬롯 1~27)
        for (int row = 0; row < 3; row++)
            for (int col = 0; col < 9; col++)
                addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));

        // 핫바 (슬롯 28~36)
        for (int col = 0; col < 9; col++)
            addSlot(new Slot(playerInventory, col, 8 + col * 18, 142));
    }

    public DeliveryMenu(int id, Inventory inv, net.minecraft.network.FriendlyByteBuf buf) {
        this(id, inv, buf.readBlockPos());
    }

    /**
     * Shift+클릭 처리.
     * 인벤토리 슬롯 → 납품 슬롯으로 이동 (스택 전체).
     * 납품 슬롯 → 인벤토리로 반환.
     */
    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = slots.get(index);

        if (slot == null || !slot.hasItem()) return ItemStack.EMPTY;

        ItemStack stack = slot.getItem();
        result = stack.copy();

        if (index == DELIVER_SLOT) {
            // 납품 슬롯 → 인벤토리
            if (!moveItemStackTo(stack, INV_START, HOTBAR_END, true))
                return ItemStack.EMPTY;
        } else {
            // 인벤토리/핫바 → 납품 슬롯 (납품 슬롯이 비어있을 때만)
            if (!moveItemStackTo(stack, DELIVER_SLOT, 1, false))
                return ItemStack.EMPTY;
        }

        if (stack.isEmpty())       slot.set(ItemStack.EMPTY);
        else                       slot.setChanged();

        if (result.getCount() == stack.getCount()) return ItemStack.EMPTY;

        slot.onTake(player, stack);
        return result;
    }

    @Override
    public boolean stillValid(Player player) {
        return player.distanceToSqr(
                blockPos.getX() + 0.5, blockPos.getY() + 0.5, blockPos.getZ() + 0.5) < 64.0;
    }

    public BlockPos getBlockPos() { return blockPos; }
}
