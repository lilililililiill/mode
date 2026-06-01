package com.agrieconomy.disaster;

import com.agrieconomy.defense.DefenseManager;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * 청크 단위 재해 상태를 관리한다.
 *
 * <p>재해 발생 시:
 * <ol>
 *   <li>방어 시설({@link DefenseManager}) 확인</li>
 *   <li>대응 시설이 있으면 재해 무효화 + 시설 소모</li>
 *   <li>없으면 청크에 재해 상태 저장</li>
 *   <li>수확 시 {@link CropGradeHandler}가 상태를 읽어 확률 보정</li>
 * </ol>
 * </p>
 */
public class DisasterManager extends SavedData {

    private static final String DATA_NAME = "agrieconomy_disaster";

    /** key: "chunkX,chunkZ" → DisasterType */
    private final Map<String, DisasterType> activeDisasters = new HashMap<>();

    private boolean autoMode = false;
    private final Random random = new Random();

    private static DisasterManager instance;

    // ── 싱글턴 접근 ──────────────────────────────────────────────────

    public static DisasterManager get(ServerLevel level) {
        instance = level.getServer()
                .overworld()
                .getDataStorage()
                .computeIfAbsent(DisasterManager::load, DisasterManager::new, DATA_NAME);
        return instance;
    }

    /** CropGradeHandler 등 이벤트 핸들러에서 접근하는 정적 진입점. */
    public static DisasterManager getInstance() {
        return instance;
    }

    // ── 재해 조회 ─────────────────────────────────────────────────────

    /**
     * 해당 BlockPos의 청크에 활성 재해가 있으면 반환한다.
     */
    public DisasterType getDisaster(ServerLevel level, BlockPos pos) {
        int cx = pos.getX() >> 4;
        int cz = pos.getZ() >> 4;
        return activeDisasters.get(chunkKey(cx, cz));
    }

    public DisasterType getDisaster(int chunkX, int chunkZ) {
        return activeDisasters.get(chunkKey(chunkX, chunkZ));
    }

    // ── 재해 발생 ─────────────────────────────────────────────────────

    /**
     * 특정 청크에 재해를 발생시킨다.
     * 방어 시설이 있으면 무효화 후 시설 소모.
     */
    public void trigger(ServerLevel level, int chunkX, int chunkZ, DisasterType type) {
        DefenseManager defenseManager = DefenseManager.get(level);

        if (defenseManager.hasDefense(chunkX, chunkZ, type)) {
            // 방어 성공 → 시설 소모
            defenseManager.consumeDefense(chunkX, chunkZ, type, level);
            broadcastMessage(level, "§a[재해 방어] 청크 [" + chunkX + ", " + chunkZ + "] 의 "
                    + type.name() + " 재해가 방어 시설에 의해 무효화되었습니다.");
        } else {
            // 재해 적용
            activeDisasters.put(chunkKey(chunkX, chunkZ), type);
            setDirty();
            broadcastMessage(level, "§c[재해 발생] 청크 [" + chunkX + ", " + chunkZ + "] 에 "
                    + type.name() + " 재해가 발생했습니다!");
        }
    }

    /** 재해 제거 (수확 이후 또는 관리자 명령) */
    public void clear(int chunkX, int chunkZ) {
        activeDisasters.remove(chunkKey(chunkX, chunkZ));
        setDirty();
    }

    // ── 자동 모드 ─────────────────────────────────────────────────────

    public void setAutoMode(boolean enabled) { this.autoMode = enabled; }
    public boolean isAutoMode() { return autoMode; }

    // ── 유틸 ─────────────────────────────────────────────────────────

    private static String chunkKey(int cx, int cz) { return cx + "," + cz; }

    private void broadcastMessage(ServerLevel level, String message) {
        MinecraftServer server = level.getServer();
        if (server != null) {
            server.getPlayerList().broadcastSystemMessage(
                    Component.literal(message), false);
        }
    }

    // ── NBT 직렬화 ────────────────────────────────────────────────────

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag list = new ListTag();
        for (Map.Entry<String, DisasterType> entry : activeDisasters.entrySet()) {
            CompoundTag entry_tag = new CompoundTag();
            String[] parts = entry.getKey().split(",");
            entry_tag.putInt("chunkX", Integer.parseInt(parts[0]));
            entry_tag.putInt("chunkZ", Integer.parseInt(parts[1]));
            entry_tag.putString("disaster", entry.getValue().name());
            list.add(entry_tag);
        }
        tag.put("disasters", list);
        tag.putBoolean("autoMode", autoMode);
        return tag;
    }

    public static DisasterManager load(CompoundTag tag) {
        DisasterManager manager = new DisasterManager();
        ListTag list = tag.getList("disasters", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag entry_tag = list.getCompound(i);
            int cx = entry_tag.getInt("chunkX");
            int cz = entry_tag.getInt("chunkZ");
            DisasterType type = DisasterType.fromString(entry_tag.getString("disaster"));
            if (type != null) manager.activeDisasters.put(chunkKey(cx, cz), type);
        }
        manager.autoMode = tag.getBoolean("autoMode");
        return manager;
    }
}