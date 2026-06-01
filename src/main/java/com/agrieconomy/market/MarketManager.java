package com.agrieconomy.market;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 거래소 전체 품목을 관리한다.
 * WorldSavedData를 이용해 서버 재시작 후에도 데이터가 유지된다.
 */
public class MarketManager extends SavedData {

    private static final String DATA_NAME = "agrieconomy_market";

    /** itemId → MarketEntry */
    private final Map<String, MarketEntry> entries = new LinkedHashMap<>();

    // ── 싱글턴 접근 ──────────────────────────────────────────────────

    public static MarketManager get(ServerLevel level) {
        return level.getServer()
                .overworld()
                .getDataStorage()
                .computeIfAbsent(MarketManager::load, MarketManager::new, DATA_NAME);
    }

    // ── CRUD ─────────────────────────────────────────────────────────

    public void addEntry(String itemId, int basePrice, int minPrice, int maxPrice) {
        entries.put(itemId, new MarketEntry(itemId, basePrice, minPrice, maxPrice));
        setDirty();
    }

    public boolean removeEntry(String itemId) {
        boolean removed = entries.remove(itemId) != null;
        if (removed) setDirty();
        return removed;
    }

    public MarketEntry getEntry(String itemId) {
        return entries.get(itemId);
    }

    public Collection<MarketEntry> getAllEntries() {
        return Collections.unmodifiableCollection(entries.values());
    }

    public boolean hasEntry(String itemId) {
        return entries.containsKey(itemId);
    }

    // ── 판매 처리 ─────────────────────────────────────────────────────

    /**
     * 아이템 판매. 등급 배율을 적용한 실제 획득 금액을 반환한다.
     * 등록되지 않은 아이템이면 -1 반환.
     */
    public int sell(String itemId, int amount, float gradeMultiplier) {
        MarketEntry entry = entries.get(itemId);
        if (entry == null) return -1;

        int revenue = (int) (entry.getCurrentPrice() * amount * gradeMultiplier);
        entry.recordSale(amount);
        setDirty();
        return revenue;
    }

    // ── WorldSavedData 직렬화 ─────────────────────────────────────────

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag list = new ListTag();
        for (MarketEntry entry : entries.values()) {
            list.add(entry.toNBT());
        }
        tag.put("entries", list);
        return tag;
    }

    public static MarketManager load(CompoundTag tag) {
        MarketManager manager = new MarketManager();
        ListTag list = tag.getList("entries", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            MarketEntry entry = MarketEntry.fromNBT(list.getCompound(i));
            manager.entries.put(entry.getItemId(), entry);
        }
        return manager;
    }
}