package com.agrieconomy.network;

import com.agrieconomy.AgriEconomy;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

/**
 * 모드 네트워크 채널 등록 및 패킷 전송 유틸.
 *
 * <p>패킷 목록:
 * <ul>
 *   <li>0: C2SSellPacket       — 판매 요청 / 거래소 목록 동기화 요청</li>
 *   <li>1: C2SDeliverPacket    — 납품 처리 요청 / 퀘스트 상태 요청</li>
 *   <li>2: C2SAdminSavePacket  — 관리자 설정 저장 / 즉시 활성화</li>
 *   <li>3: S2CMarketSyncPacket — 거래소 품목 목록 동기화 (서버→클라이언트)</li>
 *   <li>4: S2CDeliverySyncPacket — 퀘스트 상태 동기화 (서버→클라이언트)</li>
 * </ul>
 * </p>
 */
public class PacketHandler {

    private static final String PROTOCOL = "1";

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(AgriEconomy.MOD_ID, "main"),
            () -> PROTOCOL,
            PROTOCOL::equals,
            PROTOCOL::equals
    );

    private static int id = 0;

    public static void register() {
        // C2S
        CHANNEL.registerMessage(id++, C2SSellPacket.class,
                C2SSellPacket::encode, C2SSellPacket::decode, C2SSellPacket::handle,
                java.util.Optional.of(NetworkDirection.PLAY_TO_SERVER));

        CHANNEL.registerMessage(id++, C2SDeliverPacket.class,
                C2SDeliverPacket::encode, C2SDeliverPacket::decode, C2SDeliverPacket::handle,
                java.util.Optional.of(NetworkDirection.PLAY_TO_SERVER));

        CHANNEL.registerMessage(id++, C2SAdminSavePacket.class,
                C2SAdminSavePacket::encode, C2SAdminSavePacket::decode, C2SAdminSavePacket::handle,
                java.util.Optional.of(NetworkDirection.PLAY_TO_SERVER));

        CHANNEL.registerMessage(id++, C2SMarketAdminPacket.class,
                C2SMarketAdminPacket::encode, C2SMarketAdminPacket::decode, C2SMarketAdminPacket::handle,
                java.util.Optional.of(NetworkDirection.PLAY_TO_SERVER));

        // S2C
        CHANNEL.registerMessage(id++, S2CMarketSyncPacket.class,
                S2CMarketSyncPacket::encode, S2CMarketSyncPacket::decode, S2CMarketSyncPacket::handle,
                java.util.Optional.of(NetworkDirection.PLAY_TO_CLIENT));

        CHANNEL.registerMessage(id++, S2CDeliverySyncPacket.class,
                S2CDeliverySyncPacket::encode, S2CDeliverySyncPacket::decode, S2CDeliverySyncPacket::handle,
                java.util.Optional.of(NetworkDirection.PLAY_TO_CLIENT));
    }

    /** 클라이언트 → 서버 전송 */
    public static <T> void sendToServer(T packet) {
        CHANNEL.sendToServer(packet);
    }

    /** 서버 → 특정 플레이어 전송 */
    public static <T> void sendToPlayer(T packet, ServerPlayer player) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
    }
}