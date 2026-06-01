package com.agrieconomy.market;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 거래소 전체 품목을 관리한다.
 */
public class MarketManager extends SavedData {

    private static final String DATA_NAME = "agrieconomy_market";

    /** stack key → MarketEntry */
    private final Map<String, MarketEntry> entries = new LinkedHashMap<>();

    public static MarketManager get(ServerLevel level) {
        return level.getServer()
                .overworld()
                .getDataStorage()
                .computeIfAbsent(MarketManager::load, MarketManager::new, DATA_NAME);
    }

    public void addEntry(ItemStack tradedStack, int basePrice, int minPrice, int maxPrice) {
        ItemStack keyStack = tradedStack.copy();
        if (keyStack.isEmpty()) return;
        entries.put(keyForStack(keyStack), new MarketEntry(keyStack, basePrice, minPrice, maxPrice));
        setDirty();
    }

    public boolean removeEntry(ItemStack tradedStack) {
        boolean removed = entries.remove(keyForStack(tradedStack)) != null;
        if (removed) setDirty();
        return removed;
    }

    public MarketEntry getEntry(ItemStack tradedStack) {
        return entries.get(keyForStack(tradedStack));
    }

    public Collection<MarketEntry> getAllEntries() {
        return Collections.unmodifiableCollection(entries.values());
    }

    public int sell(ItemStack soldStack, int amount, float gradeMultiplier) {
        MarketEntry entry = entries.get(keyForStack(soldStack));
        if (entry == null) return -1;

        int revenue = (int) (entry.getCurrentPrice() * amount * gradeMultiplier);
        entry.recordSale(amount);
        setDirty();
        return revenue;
    }

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
            manager.entries.put(keyForStack(entry.getTradedStack()), entry);
        }
        return manager;
    }

    public static String keyForStack(ItemStack stack) {
        if (stack.isEmpty()) return "";
        return BuiltInRegistries.ITEM.getKey(stack.getItem()) + "|" + (stack.hasTag() ? stack.getTag().toString() : "");
    }
}
