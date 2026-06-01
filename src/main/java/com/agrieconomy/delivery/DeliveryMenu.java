package com.agrieconomy.delivery;

import com.agrieconomy.core.ModMenuTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * 납품 플레이어 UI 컨테이너.
 * 블록 슬롯 없이 플레이어 인벤토리만 표시한다.
 */
public class DeliveryMenu extends AbstractContainerMenu {

    private final BlockPos blockPos;

    public DeliveryMenu(int containerId, Inventory playerInventory, BlockPos pos) {
        super(ModMenuTypes.DELIVERY_MENU.get(), containerId);
        this.blockPos = pos;

        // 플레이어 인벤토리
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }

        // 핫바
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(playerInventory, col, 8 + col * 18, 142));
        }
    }

    public DeliveryMenu(int id, Inventory inv, net.minecraft.network.FriendlyByteBuf buf) {
        this(id, inv, buf.readBlockPos());
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return player.distanceToSqr(
                blockPos.getX() + 0.5, blockPos.getY() + 0.5, blockPos.getZ() + 0.5) < 64.0;
    }

    public BlockPos getBlockPos() { return blockPos; }
}
