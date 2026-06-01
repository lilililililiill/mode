package com.agrieconomy.core;

import com.agrieconomy.AgriEconomy;
import com.agrieconomy.defense.DefenseBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModBlockEntities {

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, AgriEconomy.MOD_ID);

    public static final RegistryObject<BlockEntityType<DefenseBlockEntity>> DEFENSE_BLOCK_ENTITY =
            BLOCK_ENTITIES.register("defense_block_entity", () ->
                    BlockEntityType.Builder.of(DefenseBlockEntity::new,
                            ModBlocks.PESTICIDE_BLOCK.get(),
                            ModBlocks.IRRIGATION_BLOCK.get(),
                            ModBlocks.GREENHOUSE_BLOCK.get()
                    ).build(null));
}
