package com.agrieconomy.network;

import com.agrieconomy.market.MarketEntry;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * 서버 → 클라이언트: 거래소 전체 품목 목록 동기화.
 * MarketScreen이 열려 있으면 {@code updateEntries()}를 호출한다.
 */
public class S2CMarketSyncPacket {

    private final List<MarketEntry> entries;

    public S2CMarketSyncPacket(List<MarketEntry> entries) {
        this.entries = entries;
    }

    // ── 직렬화 ───────────────────────────────────────────────────────

    public static void encode(S2CMarketSyncPacket pkt, FriendlyByteBuf buf) {
        ListTag list = new ListTag();
        for (MarketEntry e : pkt.entries) list.add(e.toNBT());
        CompoundTag root = new CompoundTag();
        root.put("entries", list);
        buf.writeNbt(root);
    }

    public static S2CMarketSyncPacket decode(FriendlyByteBuf buf) {
        CompoundTag root = buf.readNbt();
        List<MarketEntry> entries = new ArrayList<>();
        if (root != null) {
            ListTag list = root.getList("entries", net.minecraft.nbt.Tag.TAG_COMPOUND);
            for (int i = 0; i < list.size(); i++) {
                entries.add(MarketEntry.fromNBT(list.getCompound(i)));
            }
        }
        return new S2CMarketSyncPacket(entries);
    }

    // ── 클라이언트 처리 ───────────────────────────────────────────────

    public static void handle(S2CMarketSyncPacket pkt, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> handleClient(pkt));
        ctx.setPacketHandled(true);
    }

    @OnlyIn(Dist.CLIENT)
    private static void handleClient(S2CMarketSyncPacket pkt) {
        var screen = Minecraft.getInstance().screen;
        if (screen instanceof com.agrieconomy.client.screen.MarketScreen marketScreen) {
            marketScreen.updateEntries(pkt.entries);
        }
    }
}
