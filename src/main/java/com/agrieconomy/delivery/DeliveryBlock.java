package com.agrieconomy.delivery;

import com.agrieconomy.delivery.DeliveryAdminMenuProvider;
import com.agrieconomy.delivery.DeliveryMenuProvider;
import com.agrieconomy.util.AdminToolItem;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.network.NetworkHooks;

/**
 * 납품 블록.
 *
 * <ul>
 *   <li>일반 플레이어 우클릭 → 납품 UI</li>
 *   <li>관리자 도구({@link AdminToolItem}) 우클릭 → 관리 UI</li>
 * </ul>
 */
public class DeliveryBlock extends Block {

    public DeliveryBlock() {
        super(BlockBehaviour.Properties.of()
                .mapColor(MapColor.WOOD)
                .strength(2.0f)
                .sound(SoundType.WOOD));
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos,
                                  Player player, InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (!(player instanceof ServerPlayer serverPlayer)) return InteractionResult.PASS;

        boolean isAdminTool = player.getItemInHand(hand).getItem() instanceof AdminToolItem;

        if (isAdminTool && player.hasPermissions(2)) {
            // 관리자 UI
            NetworkHooks.openScreen(serverPlayer, new DeliveryAdminMenuProvider(pos), pos);
        } else {
            // 플레이어 UI
            NetworkHooks.openScreen(serverPlayer, new DeliveryMenuProvider(pos), pos);
        }

        return InteractionResult.CONSUME;
    }
}