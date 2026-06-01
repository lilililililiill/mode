package com.agrieconomy.core;

import com.agrieconomy.crop.CropGradeHandler;
import com.agrieconomy.farmland.FarmlandManager;
import com.agrieconomy.farmland.FarmlandProtectionHandler;
import com.agrieconomy.util.AdminToolItem;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * 모든 Forge 게임 이벤트 수신 → 각 시스템 핸들러로 위임.
 */
public class EventBusSubscriber {

    private final CropGradeHandler cropGradeHandler        = new CropGradeHandler();
    private final FarmlandProtectionHandler farmlandHandler = new FarmlandProtectionHandler();

    // ── 블록 파괴 ─────────────────────────────────────────────────────

    @SubscribeEvent
    public void onBlockBreak(BlockEvent.BreakEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        if (!(event.getPlayer() instanceof ServerPlayer player)) return;

        var held = player.getMainHandItem();

        // 관리자 도구 좌클릭 → 농지 제거
        if (held.getItem() instanceof AdminToolItem && player.hasPermissions(2)) {
            BlockPos pos = event.getPos();
            int cx = pos.getX() >> 4;
            int cz = pos.getZ() >> 4;
            FarmlandManager mgr = FarmlandManager.get(level);
            if (mgr.isFarmland(cx, cz)) {
                mgr.remove(cx, cz);
                player.sendSystemMessage(
                        Component.literal("§c[농지] 청크 [" + cx + ", " + cz + "] 제거됨."));
                event.setCanceled(true);
                return;
            }
        }

        // 농지 보호
        farmlandHandler.onBlockBreak(event);

        // 작물 등급 추첨 (pendingGrades에 저장)
        cropGradeHandler.onBlockBreak(event);
    }

    // ── 아이템 스폰 → NBT 부여 ────────────────────────────────────────

    @SubscribeEvent
    public void onItemEntityJoinLevel(EntityJoinLevelEvent event) {
        cropGradeHandler.onItemEntityJoinLevel(event);
    }

    // ── 우클릭 ────────────────────────────────────────────────────────

    @SubscribeEvent
    public void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        farmlandHandler.onRightClickBlock(event);
    }
}
