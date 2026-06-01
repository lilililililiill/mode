package com.agrieconomy.defense;

import com.agrieconomy.disaster.DisasterType;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.Map;

/**
 * 청크 단위 방어 시설 데이터를 관리한다.
 * 블록 위치도 함께 저장하여 재해 소모 시 블록을 월드에서 제거할 수 있다.
 */
public class DefenseManager extends SavedData {

    private static final String DATA_NAME = "agrieconomy_defense";

    private static class Entry {
        final DefenseType type;
        final BlockPos blockPos;
        Entry(DefenseType type, BlockPos blockPos) {
            this.type     = type;
            this.blockPos = blockPos;
        }
    }

    /** key: "chunkX,chunkZ" → Entry */
    private final Map<String, Entry> installations = new HashMap<>();

    // ── 싱글턴 접근 ──────────────────────────────────────────────────

    public static DefenseManager get(ServerLevel level) {
        return level.getServer()
                .overworld()
                .getDataStorage()
                .computeIfAbsent(DefenseManager::load, DefenseManager::new, DATA_NAME);
    }

    // ── 설치 / 제거 ───────────────────────────────────────────────────

    /** 시설 설치. 블록 위치 포함. */
    public void install(int chunkX, int chunkZ, DefenseType type, BlockPos blockPos) {
        installations.put(chunkKey(chunkX, chunkZ), new Entry(type, blockPos.immutable()));
        setDirty();
    }

    /** 완전 제거 (플레이어 수동 파괴 등). */
    public boolean remove(int chunkX, int chunkZ) {
        boolean removed = installations.remove(chunkKey(chunkX, chunkZ)) != null;
        if (removed) setDirty();
        return removed;
    }

    /**
     * 블록이 제거될 때 해당 위치가 등록된 시설과 일치하면 제거한다.
     * onRemove()에서 중복 제거를 방지하기 위해 위치 검증 포함.
     */
    public void removeIfPos(int chunkX, int chunkZ, BlockPos pos) {
        String key = chunkKey(chunkX, chunkZ);
        Entry entry = installations.get(key);
        if (entry != null && entry.blockPos.equals(pos)) {
            installations.remove(key);
            setDirty();
        }
    }

    public DefenseType getInstallation(int chunkX, int chunkZ) {
        Entry e = installations.get(chunkKey(chunkX, chunkZ));
        return e != null ? e.type : null;
    }

    // ── 방어 판정 ─────────────────────────────────────────────────────

    public boolean hasDefense(int chunkX, int chunkZ, DisasterType disasterType) {
        Entry e = installations.get(chunkKey(chunkX, chunkZ));
        return e != null && e.type.counters(disasterType);
    }

    /**
     * 방어 성공 → 시설 소모.
     * 저장된 블록 위치에서 월드 블록을 직접 제거한다 (드롭 없음).
     */
    public void consumeDefense(int chunkX, int chunkZ, DisasterType disasterType, ServerLevel level) {
        String key = chunkKey(chunkX, chunkZ);
        Entry entry = installations.get(key);
        if (entry == null || !entry.type.counters(disasterType)) return;

        // 월드에서 블록 제거 (드롭 없음, 이미 소모된 것으로 처리)
        level.removeBlock(entry.blockPos, false);

        installations.remove(key);
        setDirty();
    }

    // ── 유틸 ─────────────────────────────────────────────────────────

    private static String chunkKey(int cx, int cz) { return cx + "," + cz; }

    // ── NBT 직렬화 ────────────────────────────────────────────────────

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag list = new ListTag();
        for (Map.Entry<String, Entry> mapEntry : installations.entrySet()) {
            CompoundTag e = new CompoundTag();
            String[] parts = mapEntry.getKey().split(",");
            e.putInt("chunkX",    Integer.parseInt(parts[0]));
            e.putInt("chunkZ",    Integer.parseInt(parts[1]));
            e.putString("defense", mapEntry.getValue().type.name());
            e.put("blockPos",    NbtUtils.writeBlockPos(mapEntry.getValue().blockPos));
            list.add(e);
        }
        tag.put("installations", list);
        return tag;
    }

    public static DefenseManager load(CompoundTag tag) {
        DefenseManager manager = new DefenseManager();
        ListTag list = tag.getList("installations", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag e  = list.getCompound(i);
            int cx         = e.getInt("chunkX");
            int cz         = e.getInt("chunkZ");
            DefenseType t  = DefenseType.fromString(e.getString("defense"));
            BlockPos pos   = NbtUtils.readBlockPos(e.getCompound("blockPos"));
            if (t != null) manager.installations.put(chunkKey(cx, cz), new Entry(t, pos));
        }
        return manager;
    }
}
