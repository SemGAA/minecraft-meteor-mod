package net.kaupenjoe.tutorialmod.entity;

import net.kaupenjoe.tutorialmod.TutorialMod;
import net.kaupenjoe.tutorialmod.entity.custom.MeteorEntity;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModEntityTypes {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, TutorialMod.MOD_ID);

    public static final RegistryObject<EntityType<MeteorEntity>> METEOR = ENTITY_TYPES.register("meteor",
            () -> EntityType.Builder.<MeteorEntity>of(MeteorEntity::new, MobCategory.MISC)
                    .sized(2.25F, 2.25F)
                    .clientTrackingRange(16)
                    .updateInterval(1)
                    .fireImmune()
                    .build(ResourceLocation.parse(TutorialMod.MOD_ID + ":meteor").toString()));

    private ModEntityTypes() {
    }

    public static void register(IEventBus eventBus) {
        ENTITY_TYPES.register(eventBus);
    }
}