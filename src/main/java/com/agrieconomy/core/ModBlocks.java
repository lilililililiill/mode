package com.agrieconomy.core;

import com.agrieconomy.AgriEconomy;
import com.agrieconomy.market.MarketBlock;
import com.agrieconomy.delivery.DeliveryBlock;
import com.agrieconomy.defense.PesticideBlock;
import com.agrieconomy.defense.IrrigationBlock;
import com.agrieconomy.defense.GreenhouseBlock;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModBlocks {

    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, AgriEconomy.MOD_ID);

    /** 거래소 블록 */
    public static final RegistryObject<Block> MARKET_BLOCK =
            BLOCKS.register("market_block", MarketBlock::new);

    /** 납품 블록 */
    public static final RegistryObject<Block> DELIVERY_BLOCK =
            BLOCKS.register("delivery_block", DeliveryBlock::new);

    /** 살충제 살포기 (PEST 방어) */
    public static final RegistryObject<Block> PESTICIDE_BLOCK =
            BLOCKS.register("pesticide_block", PesticideBlock::new);

    /** 관개 장치 (DROUGHT 방어) */
    public static final RegistryObject<Block> IRRIGATION_BLOCK =
            BLOCKS.register("irrigation_block", IrrigationBlock::new);

    /** 온실기 (COLD 방어) */
    public static final RegistryObject<Block> GREENHOUSE_BLOCK =
            BLOCKS.register("greenhouse_block", GreenhouseBlock::new);
}
