package com.agrieconomy.command;

import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import com.agrieconomy.AgriEconomy;

/**
 * Forge 커맨드 등록 이벤트를 수신하여 모든 커맨드를 등록한다.
 */
@Mod.EventBusSubscriber(modid = AgriEconomy.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class CommandRegistry {

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        MarketCommand.register(event.getDispatcher());
        DisasterCommand.register(event.getDispatcher());
        FarmlandCommand.register(event.getDispatcher());
    }
}
