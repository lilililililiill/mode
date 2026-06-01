package com.agrieconomy.farmland;

import com.agrieconomy.core.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraft.world.item.HoeItem;

/**
 * 농지 보호 규칙을 처리한다.
 *
 * <ul>
 *   <li>농지 외부: 괭이 사용 불가</li>
 *   <li>농지 내부: 소유자만 괭이 사용 가능</li>
 *   <li>농지 등록 아이템 우클릭: 현재 청크를 농지로 등록</li>
 * </ul>
 */
public class FarmlandProtectionHandler {

    // ── 블록 파괴 이벤트 ──────────────────────────────────────────────

    public void onBlockBreak(BlockEvent.BreakEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        if (!(event.getPlayer() instanceof ServerPlayer player)) return;

        ItemStack held = player.getMainHandItem();
        if (!(held.getItem() instanceof HoeItem)) return;

        BlockPos pos = event.getPos();
        int cx = pos.getX() >> 4;
        int cz = pos.getZ() >> 4;

        FarmlandManager manager = FarmlandManager.get(level);

        if (!manager.isFarmland(cx, cz)) {
            // 농지 외부 → 괭이 사용 차단
            event.setCanceled(true);
            player.sendSystemMessage(Component.literal("§c이 곳은 등록된 농지가 아닙니다."));
            return;
        }

        if (!manager.isOwner(cx, cz, player.getName().getString())) {
            // 소유자가 아님
            event.setCanceled(true);
            player.sendSystemMessage(Component.literal("§c이 농지의 소유자가 아닙니다."));
        }
    }


    // ── 우클릭 이벤트 ─────────────────────────────────────────────────

    public void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getLevel().isClientSide()) return;
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        ItemStack held = player.getItemInHand(event.getHand());

        // ── 괭이 차단 (기존 코드 앞에 추가) ──────────────────────────
        if (held.getItem() instanceof HoeItem) {
            BlockPos pos = event.getPos();
            int cx = pos.getX() >> 4;
            int cz = pos.getZ() >> 4;
            FarmlandManager manager = FarmlandManager.get(level);

            if (!manager.isFarmland(cx, cz)) {
                event.setCanceled(true);
                player.sendSystemMessage(Component.literal("§c이 곳은 등록된 농지가 아닙니다."));
                return;
            }

            if (!manager.isOwner(cx, cz, player.getName().getString())) {
                event.setCanceled(true);
                player.sendSystemMessage(Component.literal("§c이 농지의 소유자가 아닙니다."));
                return;
            }
            return; // 농지 + 소유자 맞으면 그냥 통과
        }
        if (!(held.getItem() instanceof FarmlandRegisterItem)) return;

        BlockPos pos = event.getPos();
        int cx = pos.getX() >> 4;
        int cz = pos.getZ() >> 4;

        FarmlandManager manager = FarmlandManager.get(level);

        if (manager.isFarmland(cx, cz)) {
            player.sendSystemMessage(Component.literal("§e이 청크는 이미 농지로 등록되어 있습니다."));
        } else {
            manager.register(cx, cz, player.getName().getString());
            player.sendSystemMessage(
                    Component.literal("§a청크 [" + cx + ", " + cz + "] 를 농지로 등록했습니다."));
        }

        event.setCanceled(true);
    }
}