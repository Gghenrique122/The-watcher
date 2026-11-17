package com.stalkerhorror;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;

public final class ModItems {
    public static Item WARDING_CHARM;

    private ModItems() {}

    public static void register() {
        WARDING_CHARM = Registry.register(BuiltInRegistries.ITEM,
                new ResourceLocation(StalkerHorror.MODID, "warding_charm"),
                new WardingCharmItem(new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON)));
    }
}
