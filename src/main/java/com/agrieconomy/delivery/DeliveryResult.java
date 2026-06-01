package com.agrieconomy.delivery;

import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 납품 처리 결과를 담는 값 객체.
 */
public class DeliveryResult {

    public enum Status { INACTIVE, INVALID_SELECTION, INSUFFICIENT_ITEMS, IN_PROGRESS, COMPLETED }

    public static final DeliveryResult INACTIVE = new DeliveryResult(Status.INACTIVE, List.of(), 0, 0);
    public static final DeliveryResult INVALID_SELECTION = new DeliveryResult(Status.INVALID_SELECTION, List.of(), 0, 0);
    public static final DeliveryResult INSUFFICIENT_ITEMS = new DeliveryResult(Status.INSUFFICIENT_ITEMS, List.of(), 0, 0);

    private final Status status;
    private final List<ItemStack> rewards;
    private final int delivered;
    private final int required;

    private DeliveryResult(Status status, List<ItemStack> rewards, int delivered, int required) {
        this.status = status;
        this.rewards = rewards;
        this.delivered = delivered;
        this.required = required;
    }

    public static DeliveryResult completed(List<ItemStack> rewards) {
        List<ItemStack> copied = new ArrayList<>();
        for (ItemStack stack : rewards) copied.add(stack.copy());
        return new DeliveryResult(Status.COMPLETED, copied, 0, 0);
    }

    public static DeliveryResult progress(int delivered, int required) {
        return new DeliveryResult(Status.IN_PROGRESS, List.of(), delivered, required);
    }

    public Status getStatus() { return status; }
    public List<ItemStack> getRewards() { return Collections.unmodifiableList(rewards); }
    public int getDelivered() { return delivered; }
    public int getRequired() { return required; }
    public boolean isCompleted() { return status == Status.COMPLETED; }
}
