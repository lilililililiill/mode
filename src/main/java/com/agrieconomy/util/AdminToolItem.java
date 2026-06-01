package com.agrieconomy.util;

import com.agrieconomy.farmland.FarmlandManager;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

/**
 * 관리자 전용 도구 아이템.
 *
 * <p>농지 대상:
 * <ul>
 *   <li>우클릭: 농지 등록</li>
 *   <li>Shift+우클릭: 소유자 변경 (채팅 입력 방식은 향후 구현, 현재는 관리자 본인으로 변경)</li>
 *   <li>좌클릭(블록 파괴 이벤트): FarmlandProtectionHandler에서 처리</li>
 * </ul>
 * </p>
 * <p>납품 블록 대상: DeliveryBlock.use()에서 분기 처리.</p>
 */
public class AdminToolItem extends Item {

    public AdminToolItem() {
        super(new Item.Properties().stacksTo(1));
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        Level level = ctx.getLevel();
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        if (!(level instanceof ServerLevel serverLevel)) return InteractionResult.PASS;
        if (!(ctx.getPlayer() instanceof ServerPlayer player)) return InteractionResult.PASS;

        // 권한 검사
        if (!player.hasPermissions(2)) {
            player.sendSystemMessage(Component.literal("§c관리자 권한이 필요합니다."));
            return InteractionResult.FAIL;
        }

        BlockPos pos  = ctx.getClickedPos();
        int cx = pos.getX() >> 4;
        int cz = pos.getZ() >> 4;
        FarmlandManager mgr = FarmlandManager.get(serverLevel);

        // 납품 블록이면 DeliveryBlock.use()로 위임 (여기선 처리 안 함)
        var block = serverLevel.getBlockState(pos).getBlock();
        if (block instanceof com.agrieconomy.delivery.DeliveryBlock) {
            return InteractionResult.PASS;
        }

        if (ctx.getPlayer().isShiftKeyDown()) {
            // Shift+우클릭: 소유자를 관리자 본인으로 변경
            if (!mgr.isFarmland(cx, cz)) {
                player.sendSystemMessage(Component.literal("§c등록된 농지가 아닙니다."));
                return InteractionResult.FAIL;
            }
            String adminName = player.getName().getString();
            mgr.changeOwner(cx, cz, adminName);
            player.sendSystemMessage(
                    Component.literal("§a[농지] 소유자를 §f" + adminName + "§a 로 변경했습니다."));
        } else {
            // 우클릭: 농지 등록
            if (mgr.isFarmland(cx, cz)) {
                player.sendSystemMessage(
                        Component.literal("§e[농지] 이미 등록된 농지입니다. (소유자: "
                                + mgr.get(cx, cz).getOwnerName() + ")"));
            } else {
                mgr.register(cx, cz, player.getName().getString());
                player.sendSystemMessage(
                        Component.literal("§a[농지] 청크 [" + cx + ", " + cz + "] 등록 완료."));
            }
        }
        return InteractionResult.CONSUME;
    }
}
