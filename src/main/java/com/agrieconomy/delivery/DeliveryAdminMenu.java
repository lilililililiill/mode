package com.agrieconomy.delivery;

import com.agrieconomy.core.ModMenuTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

/**
 * 납품 관리자 UI.
 *
 * <p>설정 가능 항목:
 * 임무 제목, 설명, 요구 아이템/수량, 보상 아이템/수량,
 * 기한, 자동 활성화 여부, 자동 활성화 확률
 * </p>
 *
 * GUI 렌더링은 클라이언트 전용 DeliveryAdminScreen에서 담당한다.
 * 설정값 변경은 네트워크 패킷으로 서버에 전달된다.
 */
public class DeliveryAdminMenu extends AbstractContainerMenu {

    private final BlockPos blockPos;

    public DeliveryAdminMenu(int containerId, Inventory playerInventory, BlockPos pos) {
        super(ModMenuTypes.DELIVERY_ADMIN_MENU.get(), containerId);
        this.blockPos = pos;
    }

    public DeliveryAdminMenu(int id, Inventory inv, net.minecraft.network.FriendlyByteBuf buf) {
        this(id, inv, buf.readBlockPos());
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) { return ItemStack.EMPTY; }

    @Override
    public boolean stillValid(Player player) {
        return player.hasPermissions(2) &&
                player.distanceToSqr(blockPos.getX() + 0.5, blockPos.getY() + 0.5, blockPos.getZ() + 0.5) < 64.0;
    }

    public BlockPos getBlockPos() { return blockPos; }
}
