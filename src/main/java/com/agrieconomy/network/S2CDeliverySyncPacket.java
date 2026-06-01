package com.agrieconomy.network;

import com.agrieconomy.delivery.DeliveryQuest;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * 서버 → 클라이언트: 납품 퀘스트 상태 동기화.
 *
 * <p>{@code isAdminSync=true}: DeliveryAdminScreen 갱신</p>
 * <p>{@code isAdminSync=false}: DeliveryScreen 갱신</p>
 */
public class S2CDeliverySyncPacket {

    private final DeliveryQuest quest;     // null 가능 (퀘스트 없음)
    private final long serverGameTime;
    private final boolean isAdminSync;

    public S2CDeliverySyncPacket(DeliveryQuest quest, long serverGameTime) {
        this(quest, serverGameTime, false);
    }

    public S2CDeliverySyncPacket(DeliveryQuest quest, long serverGameTime, boolean isAdminSync) {
        this.quest          = quest;
        this.serverGameTime = serverGameTime;
        this.isAdminSync    = isAdminSync;
    }

    // ── 직렬화 ───────────────────────────────────────────────────────

    public static void encode(S2CDeliverySyncPacket pkt, FriendlyByteBuf buf) {
        buf.writeLong(pkt.serverGameTime);
        buf.writeBoolean(pkt.isAdminSync);
        buf.writeBoolean(pkt.quest != null);
        if (pkt.quest != null) {
            buf.writeNbt(pkt.quest.toNBT());
        }
    }

    public static S2CDeliverySyncPacket decode(FriendlyByteBuf buf) {
        long gameTime      = buf.readLong();
        boolean adminSync  = buf.readBoolean();
        boolean hasQuest   = buf.readBoolean();
        DeliveryQuest quest = hasQuest ? DeliveryQuest.fromNBT(buf.readNbt()) : null;
        return new S2CDeliverySyncPacket(quest, gameTime, adminSync);
    }

    // ── 클라이언트 처리 ───────────────────────────────────────────────

    public static void handle(S2CDeliverySyncPacket pkt, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> handleClient(pkt));
        ctx.setPacketHandled(true);
    }

    @OnlyIn(Dist.CLIENT)
    private static void handleClient(S2CDeliverySyncPacket pkt) {
        var screen = Minecraft.getInstance().screen;
        if (pkt.isAdminSync) {
            if (screen instanceof com.agrieconomy.client.screen.DeliveryAdminScreen adminScreen) {
                adminScreen.populateFields(pkt.quest);
            }
        } else {
            if (screen instanceof com.agrieconomy.client.screen.DeliveryScreen deliveryScreen) {
                deliveryScreen.updateQuest(pkt.quest, pkt.serverGameTime);
            }
        }
    }
}
