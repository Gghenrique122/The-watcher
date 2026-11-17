package com.stalkerhorror;

import com.stalkerhorror.entity.StalkerEntity;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

public final class ModEntities {
    public static EntityType<StalkerEntity> STALKER;

    private ModEntities() {}

    public static void register() {
        STALKER = Registry.register(BuiltInRegistries.ENTITY_TYPE,
                new ResourceLocation(StalkerHorror.MODID, "stalker"),
                EntityType.Builder.of(StalkerEntity::new, MobCategory.MISC)
                        .sized(0.6F, 2.4F)
                        .clientTrackingRange(10)
                        .updateInterval(2)
                        .fireImmune()
                        .build("stalker"));
        FabricDefaultAttributeRegistry.register(STALKER, StalkerEntity.createAttributes());
    }
}
