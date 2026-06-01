package com.agrieconomy.core;

import com.agrieconomy.AgriEconomy;
import com.agrieconomy.delivery.DeliveryAdminMenu;
import com.agrieconomy.delivery.DeliveryMenu;
import com.agrieconomy.market.MarketMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModMenuTypes {

    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(ForgeRegistries.MENU_TYPES, AgriEconomy.MOD_ID);

    public static final RegistryObject<MenuType<MarketMenu>> MARKET_MENU =
            MENUS.register("market_menu",
                    () -> IForgeMenuType.create(MarketMenu::new));

    public static final RegistryObject<MenuType<DeliveryMenu>> DELIVERY_MENU =
            MENUS.register("delivery_menu",
                    () -> IForgeMenuType.create(DeliveryMenu::new));

    public static final RegistryObject<MenuType<DeliveryAdminMenu>> DELIVERY_ADMIN_MENU =
            MENUS.register("delivery_admin_menu",
                    () -> IForgeMenuType.create(DeliveryAdminMenu::new));
}
