package com.agrieconomy.market;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;

import javax.annotation.Nullable;

/**
 * 거래소 GUI를 위한 MenuProvider.
 * NetworkHooks.openScreen()에 전달된다.
 */
public class MarketMenuProvider implements MenuProvider {

    private final BlockPos pos;

    public MarketMenuProvider(BlockPos pos) {
        this.pos = pos;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("gui.agrieconomy.market");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new MarketMenu(containerId, inventory, pos);
    }

    /** 블록 위치를 클라이언트로 전송한다. */
    public void writeToBuffer(FriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
    }
}