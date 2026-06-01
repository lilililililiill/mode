package com.agrieconomy.market;

import com.agrieconomy.core.ModMenuTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * 거래소 컨테이너 메뉴.
 *
 * <p>GUI 구성:
 * <ul>
 *   <li>페이지당 6~8개 품목 표시</li>
 *   <li>품목별: 아이템 / 기준가 / 현재가 / 변동률</li>
 *   <li>이전·다음 페이지 버튼</li>
 *   <li>판매 슬롯 (플레이어 인벤토리에서 드래그)</li>
 * </ul>
 * </p>
 *
 * 실제 GUI 렌더링은 클라이언트 전용 MarketScreen에서 담당한다.
 */
public class MarketMenu extends AbstractContainerMenu {

    public static final int PAGE_SIZE = 7; // 페이지당 품목 수
    public static final int SELL_SLOT_INDEX = 0;

    private final BlockPos blockPos;

    /** 판매할 아이템을 넣는 단일 슬롯 */
    private final net.minecraft.world.SimpleContainer sellContainer = new net.minecraft.world.SimpleContainer(1);

    // 서버 측 생성자
    public MarketMenu(int containerId, Inventory playerInventory, BlockPos pos) {
        super(ModMenuTypes.MARKET_MENU.get(), containerId);
        this.blockPos = pos;

        // 판매 슬롯
        addSlot(new Slot(sellContainer, 0, 80, 35));

        // 플레이어 인벤토리 슬롯 (9×3 + 핫바)
        addPlayerInventorySlots(playerInventory);
    }

    // 클라이언트 측 생성자 (FriendlyByteBuf 사용)
    public MarketMenu(int containerId, Inventory playerInventory,
                      net.minecraft.network.FriendlyByteBuf buf) {
        this(containerId, playerInventory, buf.readBlockPos());
    }

    // ── 슬롯 배치 ─────────────────────────────────────────────────────

    private void addPlayerInventorySlots(Inventory inv) {
        // 메인 인벤토리 (3행)
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(inv, col + row * 9 + 9,
                        8 + col * 18, 84 + row * 18));
            }
        }
        // 핫바
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(inv, col, 8 + col * 18, 142));
        }
    }

    // ── 공통 로직 ─────────────────────────────────────────────────────

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        // Shift+클릭: 판매 슬롯 ↔ 인벤토리 이동
        ItemStack result = ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack stack = slot.getItem();
            result = stack.copy();
            if (index == SELL_SLOT_INDEX) {
                if (!moveItemStackTo(stack, 1, slots.size(), true)) return ItemStack.EMPTY;
            } else {
                if (!moveItemStackTo(stack, SELL_SLOT_INDEX, 1, false)) return ItemStack.EMPTY;
            }
            if (stack.isEmpty()) slot.set(ItemStack.EMPTY);
            else slot.setChanged();
        }
        return result;
    }

    @Override
    public boolean stillValid(Player player) {
        return player.distanceToSqr(
                blockPos.getX() + 0.5,
                blockPos.getY() + 0.5,
                blockPos.getZ() + 0.5) < 64.0;
    }

    public BlockPos getBlockPos() { return blockPos; }
}