package com.agrieconomy.client;

import com.agrieconomy.client.screen.DeliveryAdminScreen;
import com.agrieconomy.client.screen.DeliveryScreen;
import com.agrieconomy.client.screen.MarketScreen;
import com.agrieconomy.core.ModMenuTypes;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

import com.agrieconomy.AgriEconomy;

/**
 * 클라이언트 전용 초기화.
 * MenuType ↔ Screen 클래스를 연결한다.
 */
@Mod.EventBusSubscriber(modid = AgriEconomy.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientSetup {

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            MenuScreens.register(ModMenuTypes.MARKET_MENU.get(),        MarketScreen::new);
            MenuScreens.register(ModMenuTypes.DELIVERY_MENU.get(),      DeliveryScreen::new);
            MenuScreens.register(ModMenuTypes.DELIVERY_ADMIN_MENU.get(),DeliveryAdminScreen::new);
        });
    }
}
