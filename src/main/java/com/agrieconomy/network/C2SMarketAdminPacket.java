package com.agrieconomy.network;

import com.agrieconomy.market.MarketManager;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.function.Supplier;

/**
 * 클라이언트 → 서버: 거래소 관리자 등록/삭제 요청.
 */
public class C2SMarketAdminPacket {

    public enum Action { SAVE, REMOVE }

    private final BlockPos blockPos;
    private final Action action;
    private final int basePrice;
    private final int minPrice;
    private final int maxPrice;

    public C2SMarketAdminPacket(BlockPos blockPos, Action action, int basePrice, int minPrice, int maxPrice) {
        this.blockPos = blockPos;
        this.action = action;
        this.basePrice = basePrice;
        this.minPrice = minPrice;
        this.maxPrice = maxPrice;
    }

    public static void encode(C2SMarketAdminPacket pkt, FriendlyByteBuf buf) {
        buf.writeBlockPos(pkt.blockPos);
        buf.writeEnum(pkt.action);
        buf.writeInt(pkt.basePrice);
        buf.writeInt(pkt.minPrice);
        buf.writeInt(pkt.maxPrice);
    }

    public static C2SMarketAdminPacket decode(FriendlyByteBuf buf) {
        return new C2SMarketAdminPacket(
                buf.readBlockPos(),
                buf.readEnum(Action.class),
                buf.readInt(),
                buf.readInt(),
                buf.readInt()
        );
    }

    public static void handle(C2SMarketAdminPacket pkt, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player == null) return;
            if (!player.hasPermissions(2)) {
                player.sendSystemMessage(Component.literal("§c권한이 없습니다."));
                return;
            }

            if (!(player.containerMenu instanceof com.agrieconomy.market.MarketMenu menu)) return;
            var slot = menu.slots.get(com.agrieconomy.market.MarketMenu.SELL_SLOT_INDEX);
            if (!slot.hasItem()) {
                player.sendSystemMessage(Component.literal("§c[거래소] 등록할 아이템을 판매 슬롯에 넣어주세요."));
                return;
            }

            var stack = slot.getItem().copy();
            var manager = MarketManager.get(player.serverLevel());

            if (pkt.action == Action.SAVE) {
                int base = Math.max(1, pkt.basePrice);
                int min = Math.max(1, pkt.minPrice);
                int max = Math.max(base, pkt.maxPrice);
                manager.addEntry(stack, base, Math.min(min, max), max);
                player.sendSystemMessage(Component.literal("§a[거래소] 품목이 등록/갱신되었습니다."));
            } else {
                boolean removed = manager.removeEntry(stack);
                player.sendSystemMessage(Component.literal(removed
                        ? "§a[거래소] 품목이 삭제되었습니다."
                        : "§c[거래소] 등록된 품목을 찾지 못했습니다."));
            }

            PacketHandler.sendToPlayer(new S2CMarketSyncPacket(new ArrayList<>(manager.getAllEntries())), player);
        });
        ctx.setPacketHandled(true);
    }
}
