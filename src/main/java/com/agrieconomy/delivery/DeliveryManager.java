package com.agrieconomy.delivery;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * 납품 블록 위치별 퀘스트 데이터를 관리한다.
 */
public class DeliveryManager extends SavedData {

    private static final String DATA_NAME = "agrieconomy_delivery";

    /** BlockPos → DeliveryQuest */
    private final Map<BlockPos, DeliveryQuest> quests = new HashMap<>();

    private final Random random = new Random();

    public static DeliveryManager get(ServerLevel level) {
        return level.getServer()
                .overworld()
                .getDataStorage()
                .computeIfAbsent(DeliveryManager::load, DeliveryManager::new, DATA_NAME);
    }

    public void setQuest(BlockPos pos, DeliveryQuest quest) {
        quests.put(pos.immutable(), quest);
        setDirty();
    }

    public DeliveryQuest getQuest(BlockPos pos) {
        return quests.get(pos);
    }

    public void removeQuest(BlockPos pos) {
        quests.remove(pos);
        setDirty();
    }

    /**
     * 플레이어 인벤토리에서 요구품을 소모해 납품을 처리한다.
     */
    public DeliveryResult processDelivery(ServerLevel level, BlockPos pos, ServerPlayer player,
                                          int requiredSelection, int rewardSelection) {
        DeliveryQuest quest = quests.get(pos);
        if (quest == null || !quest.isActive()) return DeliveryResult.INACTIVE;
        if (quest.getRequiredStacks().isEmpty()) return DeliveryResult.INACTIVE;

        boolean consumedAny = false;

        if (quest.getRequirementMode() == DeliveryQuest.RequirementMode.OR) {
            if (requiredSelection < 0 || requiredSelection >= quest.getRequiredStacks().size()) {
                return DeliveryResult.INVALID_SELECTION;
            }
            quest.setSelectedRequirementIndex(requiredSelection);
            ItemStack req = quest.getRequiredStacks().get(requiredSelection);
            int need = req.getCount() - quest.getDeliveredCountForIndex(requiredSelection);
            if (need > 0) {
                int consumed = consumeFromInventory(player.getInventory(), req, need);
                if (consumed > 0) {
                    consumedAny = true;
                    quest.addDeliveredForIndex(requiredSelection, consumed);
                }
            }
        } else {
            for (int i = 0; i < quest.getRequiredStacks().size(); i++) {
                ItemStack req = quest.getRequiredStacks().get(i);
                int need = req.getCount() - quest.getDeliveredCountForIndex(i);
                if (need <= 0) continue;
                int consumed = consumeFromInventory(player.getInventory(), req, need);
                if (consumed > 0) {
                    consumedAny = true;
                    quest.addDeliveredForIndex(i, consumed);
                }
            }
        }

        if (!consumedAny) {
            return DeliveryResult.INSUFFICIENT_ITEMS;
        }

        setDirty();

        if (quest.isCompleted()) {
            List<ItemStack> rewardsToGive = resolveRewards(quest, rewardSelection);
            if (rewardsToGive == null) return DeliveryResult.INVALID_SELECTION;

            for (ItemStack reward : rewardsToGive) {
                if (reward.isEmpty()) continue;
                ItemStack remaining = reward.copy();
                player.addItem(remaining);
                if (!remaining.isEmpty()) {
                    player.drop(remaining, false);
                }
            }

            quest.reset();
            setDirty();
            return DeliveryResult.completed(rewardsToGive);
        }

        return DeliveryResult.progress(quest.getTotalDeliveredAmount(), quest.getTotalRequiredAmount());
    }

    private List<ItemStack> resolveRewards(DeliveryQuest quest, int rewardSelection) {
        List<ItemStack> rewards = quest.getRewardStacks();
        if (rewards.isEmpty()) return List.of();

        if (quest.getRewardMode() == DeliveryQuest.RewardMode.CHOICE) {
            if (rewardSelection < 0 || rewardSelection >= rewards.size()) return null;
            return List.of(rewards.get(rewardSelection).copy());
        }

        List<ItemStack> all = new ArrayList<>();
        for (ItemStack stack : rewards) all.add(stack.copy());
        return all;
    }

    private int consumeFromInventory(Inventory inv, ItemStack required, int maxConsume) {
        if (maxConsume <= 0 || required.isEmpty()) return 0;

        int remaining = maxConsume;

        for (int i = 0; i < inv.items.size() && remaining > 0; i++) {
            ItemStack stack = inv.items.get(i);
            if (!ItemStack.isSameItemSameTags(stack, required)) continue;

            int take = Math.min(stack.getCount(), remaining);
            stack.shrink(take);
            remaining -= take;
        }

        for (int i = 0; i < inv.offhand.size() && remaining > 0; i++) {
            ItemStack stack = inv.offhand.get(i);
            if (!ItemStack.isSameItemSameTags(stack, required)) continue;

            int take = Math.min(stack.getCount(), remaining);
            stack.shrink(take);
            remaining -= take;
        }

        int consumed = maxConsume - remaining;
        if (consumed > 0) inv.setChanged();
        return consumed;
    }

    public boolean tryAutoActivate(ServerLevel level, BlockPos pos) {
        DeliveryQuest quest = quests.get(pos);
        if (quest == null || quest.isActive() || !quest.isAutoEnabled()) return false;

        int roll = random.nextInt(100);
        if (roll >= quest.getActivationChance()) return false;

        quest.setActive(true);
        quest.setStartTick(level.getGameTime());
        setDirty();

        MinecraftServer server = level.getServer();
        if (server != null) {
            server.getPlayerList().broadcastSystemMessage(
                    Component.literal("§6[납품 알림] §f" + quest.getTitle() + " 임무가 활성화되었습니다!"),
                    false
            );
        }
        return true;
    }

    public boolean manualActivate(ServerLevel level, BlockPos pos) {
        DeliveryQuest quest = quests.get(pos);
        if (quest == null) return false;

        quest.setActive(true);
        quest.setStartTick(level.getGameTime());
        setDirty();

        MinecraftServer server = level.getServer();
        if (server != null) {
            server.getPlayerList().broadcastSystemMessage(
                    Component.literal("§6[납품 알림] §f" + quest.getTitle() + " 임무가 시작되었습니다!"),
                    false
            );
        }
        return true;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag list = new ListTag();
        for (Map.Entry<BlockPos, DeliveryQuest> entry : quests.entrySet()) {
            CompoundTag entryTag = new CompoundTag();
            entryTag.put("pos", NbtUtils.writeBlockPos(entry.getKey()));
            entryTag.put("quest", entry.getValue().toNBT());
            list.add(entryTag);
        }
        tag.put("quests", list);
        return tag;
    }

    public static DeliveryManager load(CompoundTag tag) {
        DeliveryManager manager = new DeliveryManager();
        ListTag list = tag.getList("quests", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag entryTag = list.getCompound(i);
            BlockPos pos = NbtUtils.readBlockPos(entryTag.getCompound("pos"));
            DeliveryQuest q = DeliveryQuest.fromNBT(entryTag.getCompound("quest"));
            manager.quests.put(pos, q);
        }
        return manager;
    }
}
