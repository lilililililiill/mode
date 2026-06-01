package com.agrieconomy.network;

import com.agrieconomy.market.MarketManager;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.function.Supplier;

/**
 * 클라이언트 → 서버: 아이템 판매 요청 또는 거래소 목록 동기화 요청.
 *
 * <p>{@code syncOnly=true}: 거래소 목록만 요청 (판매 없음)</p>
 * <p>{@code syncOnly=false}: 판매 처리 후 업데이트된 목록 응답</p>
 */
public class C2SSellPacket {

    private final BlockPos blockPos;
    private final String itemId;
    private final int amount;
    private final boolean syncOnly;

    public C2SSellPacket(BlockPos blockPos, String itemId, int amount, boolean syncOnly) {
        this.blockPos = blockPos;
        this.itemId   = itemId;
        this.amount   = amount;
        this.syncOnly = syncOnly;
    }

    // ── 직렬화 ───────────────────────────────────────────────────────

    public static void encode(C2SSellPacket pkt, FriendlyByteBuf buf) {
        buf.writeBlockPos(pkt.blockPos);
        buf.writeUtf(pkt.itemId);
        buf.writeInt(pkt.amount);
        buf.writeBoolean(pkt.syncOnly);
    }

    public static C2SSellPacket decode(FriendlyByteBuf buf) {
        return new C2SSellPacket(
                buf.readBlockPos(),
                buf.readUtf(),
                buf.readInt(),
                buf.readBoolean()
        );
    }

    // ── 서버 처리 ─────────────────────────────────────────────────────

    public static void handle(C2SSellPacket pkt, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player == null) return;

            var level   = player.serverLevel();
            var manager = MarketManager.get(level);

            if (!pkt.syncOnly && !pkt.itemId.isEmpty() && pkt.amount > 0) {
                // 판매 슬롯 아이템 확인
                var menu = player.containerMenu;
                if (!(menu instanceof com.agrieconomy.market.MarketMenu marketMenu)) return;

                var slot = marketMenu.slots.get(com.agrieconomy.market.MarketMenu.SELL_SLOT_INDEX);
                if (!slot.hasItem()) return;

                var stack = slot.getItem();
                // 아이템 ID 검증
                String stackId = net.minecraft.core.registries.BuiltInRegistries.ITEM
                        .getKey(stack.getItem()).toString();
                if (!stackId.equals(pkt.itemId)) return;

                // CropGrade NBT에서 배율 읽기
                String gradeStr = stack.hasTag() ? stack.getTag().getString("CropGrade") : "NORMAL";
                float multiplier = com.agrieconomy.crop.CropGrade.fromString(gradeStr).getPriceMultiplier();

                int revenue = manager.sell(pkt.itemId, pkt.amount, multiplier);
                if (revenue >= 0) {
                    // 아이템 소모
                    slot.set(net.minecraft.world.item.ItemStack.EMPTY);

                    // 경제 보상 지급 (Vault API 연동 예정, 임시로 에메랄드 지급)
                    giveReward(player, revenue);

                    player.sendSystemMessage(
                            net.minecraft.network.chat.Component.literal(
                                    "§a[거래소] §f" + pkt.itemId.split(":")[1] +
                                    " §7판매 완료. §6+" + revenue + " 코인"));
                }
            }

            // 최신 거래소 목록을 클라이언트로 전송
            var entries = new ArrayList<>(manager.getAllEntries());
            PacketHandler.sendToPlayer(new S2CMarketSyncPacket(entries), player);
        });
        ctx.setPacketHandled(true);
    }

    /**
     * 임시 보상 지급 메서드.
     * 추후 경제 플러그인 연동 시 이 메서드를 교체한다.
     */
    private static void giveReward(ServerPlayer player, int amount) {
        // 에메랄드 1개 = 100코인 기준으로 지급
        int emeralds = amount / 100;
        if (emeralds > 0) {
            var emeraldStack = new net.minecraft.world.item.ItemStack(
                    net.minecraft.world.item.Items.EMERALD, Math.min(emeralds, 64));
            player.addItem(emeraldStack);
        }
    }
}
