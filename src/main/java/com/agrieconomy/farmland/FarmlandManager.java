package com.agrieconomy.farmland;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.Map;

/**
 * 서버 전체 농지 데이터를 관리한다.
 * key: "chunkX,chunkZ"
 */
public class FarmlandManager extends SavedData {

    private static final String DATA_NAME = "agrieconomy_farmland";

    private final Map<String, FarmlandData> farmlands = new HashMap<>();

    // ── 싱글턴 접근 ──────────────────────────────────────────────────

    public static FarmlandManager get(ServerLevel level) {
        return level.getServer()
                .overworld()
                .getDataStorage()
                .computeIfAbsent(FarmlandManager::load, FarmlandManager::new, DATA_NAME);
    }

    // ── CRUD ─────────────────────────────────────────────────────────

    public void register(int chunkX, int chunkZ, String ownerName) {
        farmlands.put(FarmlandData.key(chunkX, chunkZ),
                new FarmlandData(chunkX, chunkZ, ownerName));
        setDirty();
    }

    public boolean remove(int chunkX, int chunkZ) {
        boolean removed = farmlands.remove(FarmlandData.key(chunkX, chunkZ)) != null;
        if (removed) setDirty();
        return removed;
    }

    public boolean isFarmland(int chunkX, int chunkZ) {
        return farmlands.containsKey(FarmlandData.key(chunkX, chunkZ));
    }

    public FarmlandData get(int chunkX, int chunkZ) {
        return farmlands.get(FarmlandData.key(chunkX, chunkZ));
    }

    public boolean isOwner(int chunkX, int chunkZ, String playerName) {
        FarmlandData data = get(chunkX, chunkZ);
        return data != null && data.getOwnerName().equals(playerName);
    }

    public boolean changeOwner(int chunkX, int chunkZ, String newOwner) {
        FarmlandData data = get(chunkX, chunkZ);
        if (data == null) return false;
        data.setOwnerName(newOwner);
        setDirty();
        return true;
    }

    // ── NBT 직렬화 ────────────────────────────────────────────────────

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag list = new ListTag();
        for (FarmlandData data : farmlands.values()) {
            list.add(data.toNBT());
        }
        tag.put("farmlands", list);
        return tag;
    }

    public static FarmlandManager load(CompoundTag tag) {
        FarmlandManager manager = new FarmlandManager();
        ListTag list = tag.getList("farmlands", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            FarmlandData data = FarmlandData.fromNBT(list.getCompound(i));
            manager.farmlands.put(data.key(), data);
        }
        return manager;
    }
}