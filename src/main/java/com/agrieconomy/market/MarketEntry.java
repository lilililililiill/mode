package com.agrieconomy.market;

import net.minecraft.nbt.CompoundTag;

/**
 * 거래소에 등록된 단일 품목 데이터.
 *
 * <p>저장 구조 (NBT):
 * <pre>
 * {
 *   "item": "minecraft:wheat",
 *   "basePrice": 100,
 *   "minPrice": 50,
 *   "maxPrice": 200,
 *   "currentPrice": 120,
 *   "soldAmount": 340
 * }
 * </pre>
 * </p>
 */
public class MarketEntry {

    private final String itemId;
    private final int basePrice;
    private final int minPrice;
    private final int maxPrice;

    private int currentPrice;
    private long soldAmount;

    public MarketEntry(String itemId, int basePrice, int minPrice, int maxPrice) {
        this.itemId       = itemId;
        this.basePrice    = basePrice;
        this.minPrice     = minPrice;
        this.maxPrice     = maxPrice;
        this.currentPrice = basePrice;
        this.soldAmount   = 0;
    }

    // ── 가격 계산 ─────────────────────────────────────────────────────

    /**
     * 누적 판매량을 기반으로 현재 가격을 갱신한다.
     *
     * <p>공급이 많을수록 가격이 하락하는 단순 선형 모델:
     * <pre>
     *   ratio      = soldAmount / 1000.0  (1000개 = 기준점)
     *   newPrice   = basePrice * (1 - ratio * 0.5)
     * </pre>
     * 결과는 minPrice ~ maxPrice 범위로 클램핑된다.
     * </p>
     */
    public void recalculatePrice() {
        double ratio = Math.min(soldAmount / 1000.0, 2.0);
        double newPrice = basePrice * (1.0 - ratio * 0.5);
        currentPrice = (int) Math.max(minPrice, Math.min(maxPrice, newPrice));
    }

    /** 판매 완료 시 누적량 추가 및 가격 재계산. */
    public void recordSale(int amount) {
        soldAmount += amount;
        recalculatePrice();
    }

    /** 변동률 (현재가 / 기준가 - 1.0), 백분율 정수로 반환. */
    public int getChangePercent() {
        return (int) (((double) currentPrice / basePrice - 1.0) * 100);
    }

    // ── NBT 직렬화 ────────────────────────────────────────────────────

    public CompoundTag toNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putString("item", itemId);
        tag.putInt("basePrice", basePrice);
        tag.putInt("minPrice", minPrice);
        tag.putInt("maxPrice", maxPrice);
        tag.putInt("currentPrice", currentPrice);
        tag.putLong("soldAmount", soldAmount);
        return tag;
    }

    public static MarketEntry fromNBT(CompoundTag tag) {
        MarketEntry entry = new MarketEntry(
                tag.getString("item"),
                tag.getInt("basePrice"),
                tag.getInt("minPrice"),
                tag.getInt("maxPrice")
        );
        entry.currentPrice = tag.getInt("currentPrice");
        entry.soldAmount   = tag.getLong("soldAmount");
        return entry;
    }

    // ── Getters ───────────────────────────────────────────────────────

    public String getItemId()      { return itemId; }
    public int getBasePrice()      { return basePrice; }
    public int getMinPrice()       { return minPrice; }
    public int getMaxPrice()       { return maxPrice; }
    public int getCurrentPrice()   { return currentPrice; }
    public long getSoldAmount()    { return soldAmount; }
}