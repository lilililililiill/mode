package com.agrieconomy.delivery;

import net.minecraft.nbt.CompoundTag;

/**
 * 납품 임무 데이터.
 *
 * <p>저장 구조 (NBT):
 * <pre>
 * {
 *   "title": "긴급 식량 조달",
 *   "description": "...",
 *   "requiredItem": "minecraft:potato",
 *   "requiredAmount": 64,
 *   "rewardItem": "minecraft:emerald",
 *   "rewardAmount": 10,
 *   "deadlineTicks": 72000,
 *   "active": false,
 *   "autoEnabled": true,
 *   "activationChance": 5,
 *   "deliveredAmount": 0,
 *   "startTick": 0
 * }
 * </pre>
 * </p>
 */
public class DeliveryQuest {

    private String title;
    private String description;
    private String requiredItem;
    private int requiredAmount;
    private String rewardItem;
    private int rewardAmount;

    /** 임무 제한 시간 (틱 단위, 20틱 = 1초). 0 이면 무제한. */
    private long deadlineTicks;

    private boolean active;
    private boolean autoEnabled;

    /** 자동 활성화 확률 (정수 %, 0~100) */
    private int activationChance;

    /** 현재까지 납품된 수량 */
    private int deliveredAmount;

    /** 활성화된 게임 월드 틱 (만료 계산용) */
    private long startTick;

    // ── 생성 ─────────────────────────────────────────────────────────

    public DeliveryQuest() {}

    // ── 납품 처리 ─────────────────────────────────────────────────────

    /**
     * 납품 시도. 성공(완료) 여부를 반환한다.
     *
     * @param amount 납품 수량
     * @return true: 임무 완료 | false: 진행 중
     */
    public boolean deliver(int amount) {
        if (!active) return false;
        deliveredAmount = Math.min(deliveredAmount + amount, requiredAmount);
        return deliveredAmount >= requiredAmount;
    }

    public boolean isCompleted() {
        return active && deliveredAmount >= requiredAmount;
    }

    /** 임무를 초기화하고 비활성화한다. */
    public void reset() {
        active           = false;
        deliveredAmount  = 0;
        startTick        = 0;
    }

    // ── NBT 직렬화 ────────────────────────────────────────────────────

    public CompoundTag toNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putString("title",            title);
        tag.putString("description",      description == null ? "" : description);
        tag.putString("requiredItem",     requiredItem);
        tag.putInt("requiredAmount",      requiredAmount);
        tag.putString("rewardItem",       rewardItem);
        tag.putInt("rewardAmount",        rewardAmount);
        tag.putLong("deadlineTicks",      deadlineTicks);
        tag.putBoolean("active",          active);
        tag.putBoolean("autoEnabled",     autoEnabled);
        tag.putInt("activationChance",    activationChance);
        tag.putInt("deliveredAmount",     deliveredAmount);
        tag.putLong("startTick",          startTick);
        return tag;
    }

    public static DeliveryQuest fromNBT(CompoundTag tag) {
        DeliveryQuest q = new DeliveryQuest();
        q.title             = tag.getString("title");
        q.description       = tag.getString("description");
        q.requiredItem      = tag.getString("requiredItem");
        q.requiredAmount    = tag.getInt("requiredAmount");
        q.rewardItem        = tag.getString("rewardItem");
        q.rewardAmount      = tag.getInt("rewardAmount");
        q.deadlineTicks     = tag.getLong("deadlineTicks");
        q.active            = tag.getBoolean("active");
        q.autoEnabled       = tag.getBoolean("autoEnabled");
        q.activationChance  = tag.getInt("activationChance");
        q.deliveredAmount   = tag.getInt("deliveredAmount");
        q.startTick         = tag.getLong("startTick");
        return q;
    }

    // ── Getters / Setters ─────────────────────────────────────────────

    public String getTitle()              { return title; }
    public void setTitle(String v)        { this.title = v; }

    public String getDescription()        { return description; }
    public void setDescription(String v)  { this.description = v; }

    public String getRequiredItem()       { return requiredItem; }
    public void setRequiredItem(String v) { this.requiredItem = v; }

    public int getRequiredAmount()        { return requiredAmount; }
    public void setRequiredAmount(int v)  { this.requiredAmount = v; }

    public String getRewardItem()         { return rewardItem; }
    public void setRewardItem(String v)   { this.rewardItem = v; }

    public int getRewardAmount()          { return rewardAmount; }
    public void setRewardAmount(int v)    { this.rewardAmount = v; }

    public long getDeadlineTicks()        { return deadlineTicks; }
    public void setDeadlineTicks(long v)  { this.deadlineTicks = v; }

    public boolean isActive()             { return active; }
    public void setActive(boolean v)      { this.active = v; }

    public boolean isAutoEnabled()        { return autoEnabled; }
    public void setAutoEnabled(boolean v) { this.autoEnabled = v; }

    public int getActivationChance()      { return activationChance; }
    public void setActivationChance(int v){ this.activationChance = v; }

    public int getDeliveredAmount()       { return deliveredAmount; }

    public long getStartTick()            { return startTick; }
    public void setStartTick(long v)      { this.startTick = v; }
}