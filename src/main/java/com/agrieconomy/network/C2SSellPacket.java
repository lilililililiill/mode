package com.agrieconomy.network;

import com.agrieconomy.market.MarketEntry;
import com.agrieconomy.market.MarketManager;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * 클라이언트 → 서버: 아이템 판매 요청 또는 거래소 목록 동기화 요청.
 */
public class C2SSellPacket {

    private final BlockPos blockPos;
    private final int entryIndex;
    private final int amount;
    private final boolean syncOnly;

    public C2SSellPacket(BlockPos blockPos, int entryIndex, int amount, boolean syncOnly) {
        this.blockPos = blockPos;
        this.entryIndex = entryIndex;
        this.amount = amount;
        this.syncOnly = syncOnly;
    }

    public static void encode(C2SSellPacket pkt, FriendlyByteBuf buf) {
        buf.writeBlockPos(pkt.blockPos);
        buf.writeInt(pkt.entryIndex);
        buf.writeInt(pkt.amount);
        buf.writeBoolean(pkt.syncOnly);
    }

    public static C2SSellPacket decode(FriendlyByteBuf buf) {
        return new C2SSellPacket(
                buf.readBlockPos(),
                buf.readInt(),
                buf.readInt(),
                buf.readBoolean()
        );
    }

    public static void handle(C2SSellPacket pkt, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player == null) return;

            var level = player.serverLevel();
            var manager = MarketManager.get(level);

            if (!pkt.syncOnly && pkt.amount > 0) {
                var menu = player.containerMenu;
                if (!(menu instanceof com.agrieconomy.market.MarketMenu marketMenu)) return;

                var slot = marketMenu.slots.get(com.agrieconomy.market.MarketMenu.SELL_SLOT_INDEX);
                if (!slot.hasItem()) return;

                List<MarketEntry> entries = new ArrayList<>(manager.getAllEntries());
                if (pkt.entryIndex < 0 || pkt.entryIndex >= entries.size()) return;
                MarketEntry selected = entries.get(pkt.entryIndex);

                var stack = slot.getItem();
                var selectedStack = selected.getTradedStack();
                if (!net.minecraft.world.item.ItemStack.isSameItemSameTags(stack, selectedStack)) return;

                String gradeStr = stack.hasTag() ? stack.getTag().getString("CropGrade") : "NORMAL";
                float multiplier = com.agrieconomy.crop.CropGrade.fromString(gradeStr).getPriceMultiplier();

                int revenue = manager.sell(stack, pkt.amount, multiplier);
                if (revenue >= 0) {
                    slot.set(net.minecraft.world.item.ItemStack.EMPTY);
                    giveRewardByCommand(player, revenue);

                    player.sendSystemMessage(Component.literal(
                            "§a[거래소] §f" + selected.getItemId().split(":")[1] +
                                    " §7판매 완료. §6+" + revenue + " 코인"));
                }
            }

            var entries = new ArrayList<>(manager.getAllEntries());
            PacketHandler.sendToPlayer(new S2CMarketSyncPacket(entries), player);
        });
        ctx.setPacketHandled(true);
    }

    private static void giveRewardByCommand(ServerPlayer player, int amount) {
        var server = player.server;
        if (server == null) return;
        String cmd = "giftmoney " + player.getGameProfile().getName() + " " + amount;
        server.getCommands().performPrefixedCommand(server.createCommandSourceStack().withSuppressedOutput(), cmd);
    }
}
