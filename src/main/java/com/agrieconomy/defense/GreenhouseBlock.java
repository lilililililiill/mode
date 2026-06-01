package com.agrieconomy.defense;

import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

/** 온실기 블록 — COLD 재해 방어 */
public class GreenhouseBlock extends AbstractDefenseBlock {
    public GreenhouseBlock() {
        super(BlockBehaviour.Properties.of()
                .mapColor(MapColor.COLOR_LIGHT_BLUE)
                .strength(1.5f)
                .sound(SoundType.GLASS)
                .noOcclusion());
    }
    @Override public DefenseType getDefenseType()  { return DefenseType.GREENHOUSE; }
    @Override protected String getFacilityName()    { return "온실기"; }
}
