package com.agrieconomy.delivery;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;

import javax.annotation.Nullable;

/** 납품 블록 관리자 UI MenuProvider */
public class DeliveryAdminMenuProvider implements MenuProvider {

    private final BlockPos pos;

    public DeliveryAdminMenuProvider(BlockPos pos) { this.pos = pos; }

    @Override
    public Component getDisplayName() {
        return Component.translatable("gui.agrieconomy.delivery_admin");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new DeliveryAdminMenu(id, inv, pos);
    }

    public void writeToBuffer(FriendlyByteBuf buf) { buf.writeBlockPos(pos); }
}
