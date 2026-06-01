package com.agrieconomy.network;

import com.agrieconomy.delivery.DeliveryManager;
import com.agrieconomy.delivery.DeliveryQuest;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * 클라이언트 → 서버: 관리자 설정 저장 및 즉시 활성화.
 *
 * <ul>
 *   <li>{@code syncOnly=true, activate=false}: 현재 설정 불러오기 요청</li>
 *   <li>{@code syncOnly=false, activate=false}: 설정 저장</li>
 *   <li>{@code syncOnly=false, activate=true}: 저장 후 즉시 활성화</li>
 * </ul>
 */
public class C2SAdminSavePacket {

    private final BlockPos blockPos;
    private final DeliveryQuest quest; // null 가능 (syncOnly 시)
    private final boolean syncOnly;
    private final boolean activate;

    public C2SAdminSavePacket(BlockPos pos, DeliveryQuest quest, boolean syncOnly) {
        this(pos, quest, syncOnly, false);
    }

    public C2SAdminSavePacket(BlockPos pos, DeliveryQuest quest, boolean syncOnly, boolean activate) {
        this.blockPos = pos;
        this.quest    = quest;
        this.syncOnly = syncOnly;
        this.activate = activate;
    }

    // ── 직렬화 ───────────────────────────────────────────────────────

    public static void encode(C2SAdminSavePacket pkt, FriendlyByteBuf buf) {
        buf.writeBlockPos(pkt.blockPos);
        buf.writeBoolean(pkt.syncOnly);
        buf.writeBoolean(pkt.activate);
        buf.writeBoolean(pkt.quest != null);
        if (pkt.quest != null) {
            buf.writeNbt(pkt.quest.toNBT());
        }
    }

    public static C2SAdminSavePacket decode(FriendlyByteBuf buf) {
        BlockPos pos      = buf.readBlockPos();
        boolean syncOnly  = buf.readBoolean();
        boolean activate  = buf.readBoolean();
        boolean hasQuest  = buf.readBoolean();
        DeliveryQuest q   = hasQuest ? DeliveryQuest.fromNBT(buf.readNbt()) : null;
        return new C2SAdminSavePacket(pos, q, syncOnly, activate);
    }

    // ── 서버 처리 ─────────────────────────────────────────────────────

    public static void handle(C2SAdminSavePacket pkt, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player == null) return;

            // 권한 검사
            if (!player.hasPermissions(2)) {
                player.sendSystemMessage(Component.literal("§c권한이 없습니다."));
                return;
            }

            var level   = player.serverLevel();
            var manager = DeliveryManager.get(level);

            if (pkt.syncOnly) {
                // 현재 설정 불러와서 클라이언트로 전송
                var quest = manager.getQuest(pkt.blockPos);
                PacketHandler.sendToPlayer(
                        new S2CDeliverySyncPacket(quest, level.getGameTime(), true), player);
                return;
            }

            // 설정 저장
            if (pkt.quest != null) {
                manager.setQuest(pkt.blockPos, pkt.quest);
                player.sendSystemMessage(Component.literal("§a[납품 관리] 설정이 저장되었습니다."));
            }

            // 즉시 활성화
            if (pkt.activate) {
                boolean ok = manager.manualActivate(level, pkt.blockPos);
                if (!ok) {
                    player.sendSystemMessage(Component.literal("§c[납품 관리] 활성화 실패: 설정이 없습니다."));
                }
            }

            // 갱신된 상태 전송
            var quest = manager.getQuest(pkt.blockPos);
            PacketHandler.sendToPlayer(
                    new S2CDeliverySyncPacket(quest, level.getGameTime(), true), player);
        });
        ctx.setPacketHandled(true);
    }
}
