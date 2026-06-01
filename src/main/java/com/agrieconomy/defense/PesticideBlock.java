package com.agrieconomy.defense;

import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

/** 살충제 살포기 블록 — PEST 재해 방어 */
public class PesticideBlock extends AbstractDefenseBlock {
    public PesticideBlock() {
        super(BlockBehaviour.Properties.of()
                .mapColor(MapColor.COLOR_GREEN)
                .strength(1.5f)
                .sound(SoundType.METAL)
                .noOcclusion());
    }
    @Override public DefenseType getDefenseType()  { return DefenseType.PESTICIDE; }
    @Override protected String getFacilityName()    { return "살충제 살포기"; }
}
