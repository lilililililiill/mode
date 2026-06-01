package com.agrieconomy.delivery;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * 납품 블록 위치별 퀘스트 데이터를 관리한다.
 *
 * <p>자동 활성화는 서버 레벨 틱이 아닌
 * {@link #tryAutoActivate(ServerLevel, BlockPos)} 를 외부에서 호출하는 방식으로 동작한다.
 * (틱 기반 전체 월드 검사 금지 원칙 준수)</p>
 */
public class DeliveryManager extends SavedData {

    private static final String DATA_NAME = "agrieconomy_delivery";

    /** BlockPos → DeliveryQuest */
    private final Map<BlockPos, DeliveryQuest> quests = new HashMap<>();

    private final Random random = new Random();

    // ── 싱글턴 접근 ──────────────────────────────────────────────────

    public static DeliveryManager get(ServerLevel level) {
        return level.getServer()
                .overworld()
                .getDataStorage()
                .computeIfAbsent(DeliveryManager::load, DeliveryManager::new, DATA_NAME);
    }

    // ── 퀘스트 관리 ───────────────────────────────────────────────────

    public void setQuest(BlockPos pos, DeliveryQuest quest) {
        quests.put(pos.immutable(), quest);
        setDirty();
    }

    public DeliveryQuest getQuest(BlockPos pos) {
        return quests.get(pos);
    }

    public void removeQuest(BlockPos pos) {
        quests.remove(pos);
        setDirty();
    }

    // ── 납품 처리 ─────────────────────────────────────────────────────

    /**
     * 플레이어의 납품을 처리한다.
     *
     * @return DeliveryResult: 결과 상태와 보상 정보
     */
    public DeliveryResult processDelivery(ServerLevel level, BlockPos pos,
                                           String itemId, int amount) {
        DeliveryQuest quest = quests.get(pos);
        if (quest == null || !quest.isActive()) return DeliveryResult.INACTIVE;
        if (!quest.getRequiredItem().equals(itemId)) return DeliveryResult.WRONG_ITEM;

        boolean completed = quest.deliver(amount);
        setDirty();

        if (completed) {
            DeliveryResult result = DeliveryResult.completed(
                    quest.getRewardItem(), quest.getRewardAmount());
            quest.reset();
            setDirty();
            return result;
        }
        return DeliveryResult.progress(quest.getDeliveredAmount(), quest.getRequiredAmount());
    }

    // ── 자동 활성화 ───────────────────────────────────────────────────

    /**
     * 해당 블록의 퀘스트 자동 활성화를 시도한다.
     * 호출 시점은 외부(예: 플레이어 접근 이벤트 또는 게임 이벤트)에서 결정한다.
     */
    public boolean tryAutoActivate(ServerLevel level, BlockPos pos) {
        DeliveryQuest quest = quests.get(pos);
        if (quest == null || quest.isActive() || !quest.isAutoEnabled()) return false;

        int roll = random.nextInt(100);
        if (roll >= quest.getActivationChance()) return false;

        quest.setActive(true);
        quest.setStartTick(level.getGameTime());
        setDirty();

        // 전체 채팅 공지
        MinecraftServer server = level.getServer();
        if (server != null) {
            server.getPlayerList().broadcastSystemMessage(
                    Component.literal("§6[납품 알림] §f" + quest.getTitle() + " 임무가 활성화되었습니다!"),
                    false
            );
        }
        return true;
    }

    // ── 수동 활성화 (관리자) ──────────────────────────────────────────

    public boolean manualActivate(ServerLevel level, BlockPos pos) {
        DeliveryQuest quest = quests.get(pos);
        if (quest == null) return false;

        quest.setActive(true);
        quest.setStartTick(level.getGameTime());
        setDirty();

        MinecraftServer server = level.getServer();
        if (server != null) {
            server.getPlayerList().broadcastSystemMessage(
                    Component.literal("§6[납품 알림] §f" + quest.getTitle() + " 임무가 시작되었습니다!"),
                    false
            );
        }
        return true;
    }

    // ── NBT 직렬화 ────────────────────────────────────────────────────

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag list = new ListTag();
        for (Map.Entry<BlockPos, DeliveryQuest> entry : quests.entrySet()) {
            CompoundTag entry_tag = new CompoundTag();
            entry_tag.put("pos", NbtUtils.writeBlockPos(entry.getKey()));
            entry_tag.put("quest", entry.getValue().toNBT());
            list.add(entry_tag);
        }
        tag.put("quests", list);
        return tag;
    }

    public static DeliveryManager load(CompoundTag tag) {
        DeliveryManager manager = new DeliveryManager();
        ListTag list = tag.getList("quests", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag entry_tag = list.getCompound(i);
            BlockPos pos   = NbtUtils.readBlockPos(entry_tag.getCompound("pos"));
            DeliveryQuest q = DeliveryQuest.fromNBT(entry_tag.getCompound("quest"));
            manager.quests.put(pos, q);
        }
        return manager;
    }
}