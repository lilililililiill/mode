package com.agrieconomy.farmland;

import net.minecraft.nbt.CompoundTag;

/**
 * 청크 단위 농지 데이터.
 *
 * <p>저장 구조 (NBT):
 * <pre>
 * {
 *   "chunkX": 12,
 *   "chunkZ": -5,
 *   "owner": "Steve"
 * }
 * </pre>
 * </p>
 */
public class FarmlandData {

    private final int chunkX;
    private final int chunkZ;
    private String ownerName;

    public FarmlandData(int chunkX, int chunkZ, String ownerName) {
        this.chunkX     = chunkX;
        this.chunkZ     = chunkZ;
        this.ownerName  = ownerName;
    }

    // ── NBT ──────────────────────────────────────────────────────────

    public CompoundTag toNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("chunkX",    chunkX);
        tag.putInt("chunkZ",    chunkZ);
        tag.putString("owner",  ownerName);
        return tag;
    }

    public static FarmlandData fromNBT(CompoundTag tag) {
        return new FarmlandData(
                tag.getInt("chunkX"),
                tag.getInt("chunkZ"),
                tag.getString("owner")
        );
    }

    // ── Getters / Setters ─────────────────────────────────────────────

    public int getChunkX()              { return chunkX; }
    public int getChunkZ()              { return chunkZ; }
    public String getOwnerName()        { return ownerName; }
    public void setOwnerName(String v)  { this.ownerName = v; }

    public static String key(int cx, int cz) { return cx + "," + cz; }
    public String key() { return key(chunkX, chunkZ); }
}