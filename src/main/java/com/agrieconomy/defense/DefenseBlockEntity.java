package com.agrieconomy.defense;

import com.agrieconomy.core.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 방어 시설 블록 엔티티.
 * DefenseType을 저장하여 재시작 후에도 식별 가능하게 한다.
 * DefenseManager가 소모 처리 시 이 블록을 월드에서 제거한다.
 */
public class DefenseBlockEntity extends BlockEntity {

    private DefenseType defenseType;

    public DefenseBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.DEFENSE_BLOCK_ENTITY.get(), pos, state);
    }

    public DefenseType getDefenseType() { return defenseType; }
    public void setDefenseType(DefenseType type) {
        this.defenseType = type;
        setChanged();
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        if (defenseType != null) tag.putString("defenseType", defenseType.name());
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        if (tag.contains("defenseType")) {
            defenseType = DefenseType.fromString(tag.getString("defenseType"));
        }
    }
}
