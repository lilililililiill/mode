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
 *
 * <p>{@code syncOnly=true}: 현재 퀘스트 상태만 요청</p>
 * <p>{@code syncOnly=false}: 납품 처리 후 상태 응답</p>
 */
public class C2SDeliverPacket {

    private final BlockPos blockPos;
    private final String itemId;
    private final int amount;
    private final boolean syncOnly;

    public C2SDeliverPacket(BlockPos blockPos, String itemId, int amount, boolean syncOnly) {
        this.blockPos = blockPos;
        this.itemId   = itemId;
        this.amount   = amount;
        this.syncOnly = syncOnly;
    }

    // ── 직렬화 ───────────────────────────────────────────────────────

    public static void encode(C2SDeliverPacket pkt, FriendlyByteBuf buf) {
        buf.writeBlockPos(pkt.blockPos);
        buf.writeUtf(pkt.itemId);
        buf.writeInt(pkt.amount);
        buf.writeBoolean(pkt.syncOnly);
    }

    public static C2SDeliverPacket decode(FriendlyByteBuf buf) {
        return new C2SDeliverPacket(
                buf.readBlockPos(),
                buf.readUtf(),
                buf.readInt(),
                buf.readBoolean()
        );
    }

    // ── 서버 처리 ─────────────────────────────────────────────────────

    public static void handle(C2SDeliverPacket pkt, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player == null) return;

            var level   = player.serverLevel();
            var manager = DeliveryManager.get(level);

            if (!pkt.syncOnly && !pkt.itemId.isEmpty() && pkt.amount > 0) {
                // 납품 슬롯 아이템 검증
                var menu = player.containerMenu;
                if (!(menu instanceof com.agrieconomy.delivery.DeliveryMenu)) return;

                var slot = menu.slots.get(0);
                if (!slot.hasItem()) return;

                var stack = slot.getItem();
                String stackId = net.minecraft.core.registries.BuiltInRegistries.ITEM
                        .getKey(stack.getItem()).toString();

                DeliveryResult result = manager.processDelivery(level, pkt.blockPos, stackId, stack.getCount());

                switch (result.getStatus()) {
                    case WRONG_ITEM ->
                            player.sendSystemMessage(Component.literal("§c[납품] 요구 품목이 아닙니다."));

                    case INACTIVE ->
                            player.sendSystemMessage(Component.literal("§c[납품] 현재 활성화된 임무가 없습니다."));

                    case IN_PROGRESS -> {
                        slot.set(net.minecraft.world.item.ItemStack.EMPTY);
                        player.sendSystemMessage(Component.literal(
                                "§a[납품] " + result.getDelivered() + " / " + result.getRequired() + " 납품 완료."));
                    }

                    case COMPLETED -> {
                        slot.set(net.minecraft.world.item.ItemStack.EMPTY);
                        giveReward(player, result.getRewardItem(), result.getRewardAmount());
                        player.sendSystemMessage(Component.literal(
                                "§6[납품 완료!] §f보상을 획득했습니다: §a" +
                                result.getRewardItem().split(":")[1] + " x" + result.getRewardAmount()));

                        // 전체 공지
                        level.getServer().getPlayerList().broadcastSystemMessage(
                                Component.literal("§6[납품 완료] §f" + player.getName().getString() +
                                        " 님이 임무를 완료했습니다!"), false);
                    }
                }
            }

            // 최신 퀘스트 상태를 클라이언트로 전송
            var quest = manager.getQuest(pkt.blockPos);
            PacketHandler.sendToPlayer(
                    new S2CDeliverySyncPacket(quest, level.getGameTime()), player);
        });
        ctx.setPacketHandled(true);
    }

    private static void giveReward(ServerPlayer player, String itemId, int amount) {
        try {
            var item = net.minecraft.core.registries.BuiltInRegistries.ITEM
                    .get(new net.minecraft.resources.ResourceLocation(itemId));
            if (item != null) {
                var stack = new net.minecraft.world.item.ItemStack(item, Math.min(amount, 64));
                player.addItem(stack);
            }
        } catch (Exception e) {
            AgriEconomy.LOGGER.warn("보상 아이템 지급 실패: {}", itemId);
        }
    }

    // inner class 참조용 import 우회
    private static final class AgriEconomy {
        static final org.apache.logging.log4j.Logger LOGGER =
                org.apache.logging.log4j.LogManager.getLogger("agrieconomy");
    }
}
