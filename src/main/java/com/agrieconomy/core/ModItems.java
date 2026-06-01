package com.agrieconomy.core;

import com.agrieconomy.AgriEconomy;
import com.agrieconomy.farmland.FarmlandRegisterItem;
import com.agrieconomy.util.AdminToolItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModItems {

    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, AgriEconomy.MOD_ID);

    /** 거래소 블록 아이템 */
    public static final RegistryObject<Item> MARKET_BLOCK_ITEM =
            ITEMS.register("market_block",
                    () -> new BlockItem(ModBlocks.MARKET_BLOCK.get(), new Item.Properties()));

    /** 납품 블록 아이템 */
    public static final RegistryObject<Item> DELIVERY_BLOCK_ITEM =
            ITEMS.register("delivery_block",
                    () -> new BlockItem(ModBlocks.DELIVERY_BLOCK.get(), new Item.Properties()));

    /** 살충제 살포기 아이템 */
    public static final RegistryObject<Item> PESTICIDE_BLOCK_ITEM =
            ITEMS.register("pesticide_block",
                    () -> new BlockItem(ModBlocks.PESTICIDE_BLOCK.get(), new Item.Properties()));

    /** 관개 장치 아이템 */
    public static final RegistryObject<Item> IRRIGATION_BLOCK_ITEM =
            ITEMS.register("irrigation_block",
                    () -> new BlockItem(ModBlocks.IRRIGATION_BLOCK.get(), new Item.Properties()));

    /** 온실기 아이템 */
    public static final RegistryObject<Item> GREENHOUSE_BLOCK_ITEM =
            ITEMS.register("greenhouse_block",
                    () -> new BlockItem(ModBlocks.GREENHOUSE_BLOCK.get(), new Item.Properties()));

    /** 농지 등록 아이템 (현재 청크를 농지로 등록) */
    public static final RegistryObject<Item> FARMLAND_REGISTER =
            ITEMS.register("farmland_register", FarmlandRegisterItem::new);

    /** 관리자 도구 (납품 블록·농지 관리용) */
    public static final RegistryObject<Item> ADMIN_TOOL =
            ITEMS.register("admin_tool", AdminToolItem::new);
}
