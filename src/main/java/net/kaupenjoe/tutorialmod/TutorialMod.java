package net.kaupenjoe.tutorialmod;

import com.mojang.logging.LogUtils;
import net.kaupenjoe.tutorialmod.block.ModBlocks;
import net.kaupenjoe.tutorialmod.entity.ModEntityTypes;
import net.kaupenjoe.tutorialmod.item.ModItems;
import net.kaupenjoe.tutorialmod.network.ModMessages;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

@Mod(TutorialMod.MOD_ID)
public class TutorialMod {
    public static final String MOD_ID = "tutorialmod";
    public static final Logger LOGGER = LogUtils.getLogger();

    public TutorialMod() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        ModItems.register(modEventBus);
        ModBlocks.register(modEventBus);
        ModEntityTypes.register(modEventBus);

        modEventBus.addListener(this::addCreativeTabs);

        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, Config.SPEC);
        ModMessages.register();
        MinecraftForge.EVENT_BUS.register(this);
    }

    private void addCreativeTabs(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.INGREDIENTS) {
            event.accept(ModItems.ALEXANDRITE);
        }

        if (event.getTabKey() == CreativeModeTabs.BUILDING_BLOCKS) {
            event.accept(ModBlocks.ALEXANDRITE_BLOCK);
        }
        if (event.getTabKey() == CreativeModeTabs.INGREDIENTS) {
            event.accept(ModItems.ALEXANDRITE);
            event.accept(ModItems.METEOR_STAFF);
        }
    }
}
