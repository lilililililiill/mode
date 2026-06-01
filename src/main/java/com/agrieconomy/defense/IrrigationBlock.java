package com.agrieconomy.defense;

import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

/** 관개 장치 블록 — DROUGHT 재해 방어 */
public class IrrigationBlock extends AbstractDefenseBlock {
    public IrrigationBlock() {
        super(BlockBehaviour.Properties.of()
                .mapColor(MapColor.WATER)
                .strength(1.5f)
                .sound(SoundType.STONE)
                .noOcclusion());
    }
    @Override public DefenseType getDefenseType()  { return DefenseType.IRRIGATION; }
    @Override protected String getFacilityName()    { return "관개 장치"; }
}
