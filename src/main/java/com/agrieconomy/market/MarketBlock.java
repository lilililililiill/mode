package com.agrieconomy.market;

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
 * 거래소 블록.
 * 관리자 도구 + 권한 보유자만 UI를 열 수 있다.
 */
public class MarketBlock extends Block {

    public MarketBlock() {
        super(BlockBehaviour.Properties.of()
                .mapColor(MapColor.STONE)
                .strength(3.0f)
                .sound(SoundType.STONE)
                .requiresCorrectToolForDrops());
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos,
                                  Player player, InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide) return InteractionResult.SUCCESS;

        boolean isAdminTool = player.getItemInHand(hand).getItem() instanceof AdminToolItem;
        if (!(player instanceof ServerPlayer serverPlayer)) return InteractionResult.PASS;
        if (!isAdminTool || !player.hasPermissions(2)) {
            return InteractionResult.CONSUME;
        }

        NetworkHooks.openScreen(serverPlayer, new MarketMenuProvider(pos), pos);
        return InteractionResult.CONSUME;
    }
}
