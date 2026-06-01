package com.agrieconomy;

import com.agrieconomy.config.AgriConfig;
import com.agrieconomy.core.*;
import com.agrieconomy.network.PacketHandler;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(AgriEconomy.MOD_ID)
public class AgriEconomy {

    public static final String MOD_ID = "agrieconomy";
    public static final Logger LOGGER = LogManager.getLogger(MOD_ID);

    public AgriEconomy() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();

        // 레지스트리 등록
        ModBlocks.BLOCKS.register(modBus);
        ModItems.ITEMS.register(modBus);
        ModMenuTypes.MENUS.register(modBus);
        ModBlockEntities.BLOCK_ENTITIES.register(modBus);
        ModCreativeTab.TABS.register(modBus);

        // 설정 파일 등록
        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, AgriConfig.SERVER_SPEC, "agrieconomy-server.toml");
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, AgriConfig.COMMON_SPEC, "agrieconomy-common.toml");

        modBus.addListener(this::commonSetup);

        // 게임 이벤트 버스 등록
        MinecraftForge.EVENT_BUS.register(new EventBusSubscriber());
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(PacketHandler::register);
        LOGGER.info("AgriEconomy 초기화 완료");
    }
}
