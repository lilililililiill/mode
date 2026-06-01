package com.agrieconomy.market;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

/**
 * 거래소에 등록된 단일 품목 데이터.
 */
public class MarketEntry {

    private final ItemStack tradedStack;
    private final int basePrice;
    private final int minPrice;
    private final int maxPrice;

    private int currentPrice;
    private long soldAmount;

    public MarketEntry(ItemStack tradedStack, int basePrice, int minPrice, int maxPrice) {
        this.tradedStack = tradedStack.copy();
        this.basePrice = basePrice;
        this.minPrice = minPrice;
        this.maxPrice = maxPrice;
        this.currentPrice = basePrice;
        this.soldAmount = 0;
    }

    public void recalculatePrice() {
        double ratio = Math.min(soldAmount / 1000.0, 2.0);
        double newPrice = basePrice * (1.0 - ratio * 0.5);
        currentPrice = (int) Math.max(minPrice, Math.min(maxPrice, newPrice));
    }

    public void recordSale(int amount) {
        soldAmount += amount;
        recalculatePrice();
    }

    public int getChangePercent() {
        return (int) (((double) currentPrice / basePrice - 1.0) * 100);
    }

    public CompoundTag toNBT() {
        CompoundTag tag = new CompoundTag();
        tag.put("stack", tradedStack.save(new CompoundTag()));
        tag.putInt("basePrice", basePrice);
        tag.putInt("minPrice", minPrice);
        tag.putInt("maxPrice", maxPrice);
        tag.putInt("currentPrice", currentPrice);
        tag.putLong("soldAmount", soldAmount);
        return tag;
    }

    public static MarketEntry fromNBT(CompoundTag tag) {
        MarketEntry entry = new MarketEntry(
                ItemStack.of(tag.getCompound("stack")),
                tag.getInt("basePrice"),
                tag.getInt("minPrice"),
                tag.getInt("maxPrice")
        );
        entry.currentPrice = tag.getInt("currentPrice");
        entry.soldAmount = tag.getLong("soldAmount");
        return entry;
    }

    public ItemStack getTradedStack() { return tradedStack.copy(); }
    public int getBasePrice() { return basePrice; }
    public int getMinPrice() { return minPrice; }
    public int getMaxPrice() { return maxPrice; }
    public int getCurrentPrice() { return currentPrice; }
    public long getSoldAmount() { return soldAmount; }

    public String getDisplayKey() {
        return BuiltInRegistries.ITEM.getKey(tradedStack.getItem()).toString() + (tradedStack.hasTag() ? tradedStack.getTag().toString() : "");
    }

    public String getItemId() {
        return BuiltInRegistries.ITEM.getKey(tradedStack.getItem()).toString();
    }
}
