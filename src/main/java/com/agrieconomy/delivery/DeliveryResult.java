package com.agrieconomy.delivery;

/**
 * 납품 처리 결과를 담는 값 객체.
 */
public class DeliveryResult {

    public enum Status { INACTIVE, WRONG_ITEM, IN_PROGRESS, COMPLETED }

    public static final DeliveryResult INACTIVE    = new DeliveryResult(Status.INACTIVE, null, 0, 0, 0);
    public static final DeliveryResult WRONG_ITEM  = new DeliveryResult(Status.WRONG_ITEM, null, 0, 0, 0);

    private final Status status;
    private final String rewardItem;
    private final int rewardAmount;
    private final int delivered;
    private final int required;

    private DeliveryResult(Status status, String rewardItem, int rewardAmount,
                            int delivered, int required) {
        this.status       = status;
        this.rewardItem   = rewardItem;
        this.rewardAmount = rewardAmount;
        this.delivered    = delivered;
        this.required     = required;
    }

    public static DeliveryResult completed(String rewardItem, int rewardAmount) {
        return new DeliveryResult(Status.COMPLETED, rewardItem, rewardAmount, 0, 0);
    }

    public static DeliveryResult progress(int delivered, int required) {
        return new DeliveryResult(Status.IN_PROGRESS, null, 0, delivered, required);
    }

    public Status getStatus()      { return status; }
    public String getRewardItem()  { return rewardItem; }
    public int getRewardAmount()   { return rewardAmount; }
    public int getDelivered()      { return delivered; }
    public int getRequired()       { return required; }
    public boolean isCompleted()   { return status == Status.COMPLETED; }
}