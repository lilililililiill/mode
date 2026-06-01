package com.agrieconomy.delivery;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 납품 임무 데이터.
 */
public class DeliveryQuest {

    public enum RequirementMode { AND, OR }
    public enum RewardMode { ALL, CHOICE }

    private String title;
    private String description;

    private final List<ItemStack> requiredStacks = new ArrayList<>();
    private final List<ItemStack> rewardStacks = new ArrayList<>();
    private RequirementMode requirementMode = RequirementMode.AND;
    private RewardMode rewardMode = RewardMode.ALL;

    /** 각 요구 스택별 누적 납품량 */
    private final List<Integer> deliveredAmounts = new ArrayList<>();

    /** OR 모드에서 진행 중인 선택 인덱스 */
    private int selectedRequirementIndex;

    /** 임무 제한 시간 (틱 단위, 20틱 = 1초). 0 이면 무제한. */
    private long deadlineTicks;

    private boolean active;
    private boolean autoEnabled;

    /** 자동 활성화 확률 (정수 %, 0~100) */
    private int activationChance;

    /** 활성화된 게임 월드 틱 (만료 계산용) */
    private long startTick;

    public DeliveryQuest() {}

    public void setRequiredStacks(List<ItemStack> stacks) {
        requiredStacks.clear();
        for (ItemStack stack : stacks) {
            if (!stack.isEmpty()) requiredStacks.add(stack.copy());
        }
        normalizeProgress();
    }

    public void setRewardStacks(List<ItemStack> stacks) {
        rewardStacks.clear();
        for (ItemStack stack : stacks) {
            if (!stack.isEmpty()) rewardStacks.add(stack.copy());
        }
    }

    public List<ItemStack> getRequiredStacks() {
        return Collections.unmodifiableList(requiredStacks);
    }

    public List<ItemStack> getRewardStacks() {
        return Collections.unmodifiableList(rewardStacks);
    }

    private void normalizeProgress() {
        while (deliveredAmounts.size() < requiredStacks.size()) deliveredAmounts.add(0);
        while (deliveredAmounts.size() > requiredStacks.size()) deliveredAmounts.remove(deliveredAmounts.size() - 1);
        for (int i = 0; i < deliveredAmounts.size(); i++) {
            int max = requiredStacks.get(i).getCount();
            deliveredAmounts.set(i, Math.max(0, Math.min(max, deliveredAmounts.get(i))));
        }
        if (requiredStacks.isEmpty()) {
            selectedRequirementIndex = 0;
        } else {
            selectedRequirementIndex = Math.max(0, Math.min(selectedRequirementIndex, requiredStacks.size() - 1));
        }
    }

    public int getSelectedRequirementIndex() {
        return selectedRequirementIndex;
    }

    public void setSelectedRequirementIndex(int selectedRequirementIndex) {
        this.selectedRequirementIndex = selectedRequirementIndex;
        normalizeProgress();
    }

    public int getRequiredCountForIndex(int index) {
        if (index < 0 || index >= requiredStacks.size()) return 0;
        return requiredStacks.get(index).getCount();
    }

    public int getDeliveredCountForIndex(int index) {
        if (index < 0 || index >= deliveredAmounts.size()) return 0;
        return deliveredAmounts.get(index);
    }

    public int addDeliveredForIndex(int index, int amount) {
        if (index < 0 || index >= requiredStacks.size()) return 0;
        normalizeProgress();
        int current = deliveredAmounts.get(index);
        int max = requiredStacks.get(index).getCount();
        int next = Math.min(max, current + Math.max(0, amount));
        deliveredAmounts.set(index, next);
        return next;
    }

    public int getTotalRequiredAmount() {
        if (requirementMode == RequirementMode.OR) {
            return requiredStacks.isEmpty() ? 0 : requiredStacks.get(selectedRequirementIndex).getCount();
        }
        int total = 0;
        for (ItemStack stack : requiredStacks) total += stack.getCount();
        return total;
    }

    public int getTotalDeliveredAmount() {
        normalizeProgress();
        if (requirementMode == RequirementMode.OR) {
            return requiredStacks.isEmpty() ? 0 : deliveredAmounts.get(selectedRequirementIndex);
        }
        int total = 0;
        for (Integer v : deliveredAmounts) total += v;
        return total;
    }

    public boolean isCompleted() {
        if (!active || requiredStacks.isEmpty()) return false;
        normalizeProgress();
        if (requirementMode == RequirementMode.OR) {
            return deliveredAmounts.get(selectedRequirementIndex) >= requiredStacks.get(selectedRequirementIndex).getCount();
        }
        for (int i = 0; i < requiredStacks.size(); i++) {
            if (deliveredAmounts.get(i) < requiredStacks.get(i).getCount()) return false;
        }
        return true;
    }

    /** 임무를 초기화하고 비활성화한다. */
    public void reset() {
        active = false;
        startTick = 0;
        for (int i = 0; i < deliveredAmounts.size(); i++) deliveredAmounts.set(i, 0);
    }

    public CompoundTag toNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putString("title", title == null ? "" : title);
        tag.putString("description", description == null ? "" : description);

        ListTag requiredList = new ListTag();
        for (ItemStack stack : requiredStacks) requiredList.add(stack.save(new CompoundTag()));
        tag.put("requiredStacks", requiredList);

        ListTag rewardList = new ListTag();
        for (ItemStack stack : rewardStacks) rewardList.add(stack.save(new CompoundTag()));
        tag.put("rewardStacks", rewardList);

        ListTag deliveredList = new ListTag();
        for (Integer amount : deliveredAmounts) {
            CompoundTag progress = new CompoundTag();
            progress.putInt("amount", amount);
            deliveredList.add(progress);
        }
        tag.put("deliveredAmounts", deliveredList);

        tag.putString("requirementMode", requirementMode.name());
        tag.putString("rewardMode", rewardMode.name());
        tag.putInt("selectedRequirementIndex", selectedRequirementIndex);

        tag.putLong("deadlineTicks", deadlineTicks);
        tag.putBoolean("active", active);
        tag.putBoolean("autoEnabled", autoEnabled);
        tag.putInt("activationChance", activationChance);
        tag.putLong("startTick", startTick);
        return tag;
    }

    public static DeliveryQuest fromNBT(CompoundTag tag) {
        DeliveryQuest q = new DeliveryQuest();
        q.title = tag.getString("title");
        q.description = tag.getString("description");

        if (tag.contains("requiredStacks", net.minecraft.nbt.Tag.TAG_LIST)) {
            ListTag requiredList = tag.getList("requiredStacks", net.minecraft.nbt.Tag.TAG_COMPOUND);
            for (int i = 0; i < requiredList.size(); i++) {
                ItemStack stack = ItemStack.of(requiredList.getCompound(i));
                if (!stack.isEmpty()) q.requiredStacks.add(stack);
            }
        } else if (tag.contains("requiredItem")) {
            String legacyItemId = tag.getString("requiredItem");
            int amount = Math.max(1, tag.getInt("requiredAmount"));
            ResourceLocation key = ResourceLocation.tryParse(legacyItemId);
            Item item = key == null ? Items.AIR : BuiltInRegistries.ITEM.get(key);
            if (item == null || item == Items.AIR) item = Items.BARRIER;
            q.requiredStacks.add(new ItemStack(item, amount));
        }

        if (tag.contains("rewardStacks", net.minecraft.nbt.Tag.TAG_LIST)) {
            ListTag rewardList = tag.getList("rewardStacks", net.minecraft.nbt.Tag.TAG_COMPOUND);
            for (int i = 0; i < rewardList.size(); i++) {
                ItemStack stack = ItemStack.of(rewardList.getCompound(i));
                if (!stack.isEmpty()) q.rewardStacks.add(stack);
            }
        } else if (tag.contains("rewardItem")) {
            String legacyItemId = tag.getString("rewardItem");
            int amount = Math.max(1, tag.getInt("rewardAmount"));
            ResourceLocation key = ResourceLocation.tryParse(legacyItemId);
            Item item = key == null ? Items.AIR : BuiltInRegistries.ITEM.get(key);
            if (item == null || item == Items.AIR) item = Items.BARRIER;
            q.rewardStacks.add(new ItemStack(item, amount));
        }

        if (tag.contains("deliveredAmounts", net.minecraft.nbt.Tag.TAG_LIST)) {
            ListTag deliveredList = tag.getList("deliveredAmounts", net.minecraft.nbt.Tag.TAG_COMPOUND);
            for (int i = 0; i < deliveredList.size(); i++) {
                q.deliveredAmounts.add(deliveredList.getCompound(i).getInt("amount"));
            }
        } else if (tag.contains("deliveredAmount")) {
            q.deliveredAmounts.add(tag.getInt("deliveredAmount"));
        }

        if (tag.contains("requirementMode")) {
            try { q.requirementMode = RequirementMode.valueOf(tag.getString("requirementMode")); }
            catch (IllegalArgumentException ignored) {}
        }
        if (tag.contains("rewardMode")) {
            try { q.rewardMode = RewardMode.valueOf(tag.getString("rewardMode")); }
            catch (IllegalArgumentException ignored) {}
        }

        q.selectedRequirementIndex = tag.getInt("selectedRequirementIndex");
        q.deadlineTicks = tag.getLong("deadlineTicks");
        q.active = tag.getBoolean("active");
        q.autoEnabled = tag.getBoolean("autoEnabled");
        q.activationChance = tag.getInt("activationChance");
        q.startTick = tag.getLong("startTick");
        q.normalizeProgress();
        return q;
    }

    public String getTitle()              { return title; }
    public void setTitle(String v)        { this.title = v; }

    public String getDescription()        { return description; }
    public void setDescription(String v)  { this.description = v; }

    public RequirementMode getRequirementMode() { return requirementMode; }
    public void setRequirementMode(RequirementMode requirementMode) { this.requirementMode = requirementMode == null ? RequirementMode.AND : requirementMode; }

    public RewardMode getRewardMode() { return rewardMode; }
    public void setRewardMode(RewardMode rewardMode) { this.rewardMode = rewardMode == null ? RewardMode.ALL : rewardMode; }

    public long getDeadlineTicks()        { return deadlineTicks; }
    public void setDeadlineTicks(long v)  { this.deadlineTicks = v; }

    public boolean isActive()             { return active; }
    public void setActive(boolean v)      { this.active = v; }

    public boolean isAutoEnabled()        { return autoEnabled; }
    public void setAutoEnabled(boolean v) { this.autoEnabled = v; }

    public int getActivationChance()      { return activationChance; }
    public void setActivationChance(int v){ this.activationChance = v; }

    public long getStartTick()            { return startTick; }
    public void setStartTick(long v)      { this.startTick = v; }
}
