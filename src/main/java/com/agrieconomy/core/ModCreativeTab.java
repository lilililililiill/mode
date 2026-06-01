package com.agrieconomy.core;

import com.agrieconomy.AgriEconomy;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public class ModCreativeTab {

    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, AgriEconomy.MOD_ID);

    public static final RegistryObject<CreativeModeTab> MAIN_TAB = TABS.register("main",
            () -> CreativeModeTab.builder()
                    .icon(() -> new ItemStack(ModItems.FARMLAND_REGISTER.get()))
                    .title(Component.translatable("itemGroup.agrieconomy.main"))
                    .displayItems((params, output) -> {
                        // 블록
                        output.accept(ModItems.MARKET_BLOCK_ITEM.get());
                        output.accept(ModItems.DELIVERY_BLOCK_ITEM.get());
                        output.accept(ModItems.PESTICIDE_BLOCK_ITEM.get());
                        output.accept(ModItems.IRRIGATION_BLOCK_ITEM.get());
                        output.accept(ModItems.GREENHOUSE_BLOCK_ITEM.get());
                        // 아이템
                        output.accept(ModItems.FARMLAND_REGISTER.get());
                        output.accept(ModItems.ADMIN_TOOL.get());
                    })
                    .build());
}
