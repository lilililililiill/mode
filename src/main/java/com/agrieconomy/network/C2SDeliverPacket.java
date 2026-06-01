package com.agrieconomy.network;

import com.agrieconomy.delivery.DeliveryManager;
import com.agrieconomy.delivery.DeliveryResult;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * 클라이언트 → 서버: 납품 처리 요청 또는 퀘스트 상태 동기화 요청.
 */
public class C2SDeliverPacket {

    private final BlockPos blockPos;
    private final int requiredSelection;
    private final int rewardSelection;
    private final boolean syncOnly;

    public C2SDeliverPacket(BlockPos blockPos, int requiredSelection, int rewardSelection, boolean syncOnly) {
        this.blockPos = blockPos;
        this.requiredSelection = requiredSelection;
        this.rewardSelection = rewardSelection;
        this.syncOnly = syncOnly;
    }

    public static void encode(C2SDeliverPacket pkt, FriendlyByteBuf buf) {
        buf.writeBlockPos(pkt.blockPos);
        buf.writeInt(pkt.requiredSelection);
        buf.writeInt(pkt.rewardSelection);
        buf.writeBoolean(pkt.syncOnly);
    }

    public static C2SDeliverPacket decode(FriendlyByteBuf buf) {
        return new C2SDeliverPacket(
                buf.readBlockPos(),
                buf.readInt(),
                buf.readInt(),
                buf.readBoolean()
        );
    }

    public static void handle(C2SDeliverPacket pkt, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player == null) return;

            var level = player.serverLevel();
            var manager = DeliveryManager.get(level);

            if (!pkt.syncOnly) {
                DeliveryResult result = manager.processDelivery(level, pkt.blockPos, player, pkt.requiredSelection, pkt.rewardSelection);

                switch (result.getStatus()) {
                    case INSUFFICIENT_ITEMS ->
                            player.sendSystemMessage(Component.literal("§c[납품] 요구 아이템이 부족합니다."));

                    case INACTIVE ->
                            player.sendSystemMessage(Component.literal("§c[납품] 현재 활성화된 임무가 없습니다."));

                    case INVALID_SELECTION ->
                            player.sendSystemMessage(Component.literal("§c[납품] 선택이 올바르지 않습니다."));

                    case IN_PROGRESS ->
                            player.sendSystemMessage(Component.literal(
                                    "§a[납품] " + result.getDelivered() + " / " + result.getRequired() + " 진행 중"));

                    case COMPLETED -> {
                        player.sendSystemMessage(Component.literal("§6[납품 완료!] §f보상을 획득했습니다."));
                        level.getServer().getPlayerList().broadcastSystemMessage(
                                Component.literal("§6[납품 완료] §f" + player.getName().getString() + " 님이 임무를 완료했습니다!"), false);
                    }
                }
            }

            var quest = manager.getQuest(pkt.blockPos);
            PacketHandler.sendToPlayer(
                    new S2CDeliverySyncPacket(quest, level.getGameTime()), player);
        });
        ctx.setPacketHandled(true);
    }
}
