package com.agrieconomy.crop;

import com.agrieconomy.config.AgriConfig;
import com.agrieconomy.disaster.DisasterManager;
import com.agrieconomy.disaster.DisasterType;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.level.BlockEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class CropGradeHandler {

    private final Random random = new Random();

    /**
     * key: 블록 위치(long)
     * value: [등급, 저장된 게임틱] — 일정 틱 이후 만료되어 오염 방지
     */
    private final Map<Long, GradeEntry> pendingGrades = new HashMap<>();

    /** 등급 유효 시간 (틱). 드롭 아이템은 보통 1~2틱 내에 스폰된다. */
    private static final long EXPIRE_TICKS = 20L;

    private record GradeEntry(CropGrade grade, long createdTick) {}

    // ── 블록 파괴 ─────────────────────────────────────────────────────

    public void onBlockBreak(BlockEvent.BreakEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;

        BlockPos pos = event.getPos();
        BlockState state = level.getBlockState(pos);

        if (!(state.getBlock() instanceof CropBlock crop)) return;
        if (!crop.isMaxAge(state)) return;

        CropGrade grade = rollGrade(level, pos);
        long now = level.getGameTime();

        // 현재 위치 + 한 칸 위 모두 등록 (아이템 스폰 위치 대비)
        pendingGrades.put(pos.asLong(), new GradeEntry(grade, now));
        pendingGrades.put(pos.above().asLong(), new GradeEntry(grade, now));

        // 만료된 항목 정리
        pendingGrades.entrySet().removeIf(e -> now - e.getValue().createdTick > EXPIRE_TICKS);
    }

    // ── 아이템 스폰 → NBT 부여 ────────────────────────────────────────

    public void onItemEntityJoinLevel(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide()) return;
        if (!(event.getEntity() instanceof ItemEntity itemEntity)) return;
        if (pendingGrades.isEmpty()) return;

        BlockPos itemPos = BlockPos.containing(
                itemEntity.getX(), itemEntity.getY(), itemEntity.getZ());

        // remove 대신 get으로 읽어서 복수 드롭 모두 적용
        GradeEntry entry = pendingGrades.get(itemPos.asLong());
        if (entry == null) entry = pendingGrades.get(itemPos.below().asLong());
        if (entry == null) return;

        ItemStack stack = itemEntity.getItem();
        stack.getOrCreateTag().putString(CropGrade.NBT_KEY, entry.grade().name());
    }

    // ── 등급 추첨 ─────────────────────────────────────────────────────

    private CropGrade rollGrade(ServerLevel level, BlockPos pos) {
        int[] weights = getBaseWeights();
        applyDisasterModifier(level, pos, weights);

        int total = 0;
        for (int w : weights) total += Math.max(0, w);

        int roll = random.nextInt(Math.max(total, 1));
        int cumulative = 0;
        CropGrade[] grades = CropGrade.values();

        for (int i = 0; i < grades.length; i++) {
            cumulative += Math.max(0, weights[i]);
            if (roll < cumulative) return grades[i];
        }
        return CropGrade.NORMAL;
    }

    private int[] getBaseWeights() {
        return new int[]{
                AgriConfig.SERVER.gradeWeightSmall.get(),
                AgriConfig.SERVER.gradeWeightNormal.get(),
                AgriConfig.SERVER.gradeWeightLarge.get(),
                AgriConfig.SERVER.gradeWeightPremium.get()
        };
    }

    private void applyDisasterModifier(ServerLevel level, BlockPos pos, int[] weights) {
        if (DisasterManager.getInstance() == null) return;
        DisasterType disaster = DisasterManager.getInstance().getDisaster(level, pos);
        if (disaster == null) return;

        switch (disaster) {
            case PEST     -> { weights[0] += 15; weights[3] -= 2; }
            case DROUGHT  -> { weights[0] += 15; weights[2] -= 5; }
            case BLESSING -> { weights[2] += 10; weights[3] += 5; }
            default       -> { }
        }
    }
}
