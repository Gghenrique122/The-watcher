package com.stalkerhorror;

import com.stalkerhorror.system.LearningEvents;
import com.stalkerhorror.system.StalkerCommands;
import com.stalkerhorror.system.StalkerDirector;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.world.item.CreativeModeTabs;

public class StalkerHorror implements ModInitializer {
    public static final String MODID = "stalker_horror";

    @Override
    public void onInitialize() {
        Config.load();
        ModItems.register();
        ModEntities.register();
        StalkerDirector.init();
        LearningEvents.init();
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, env) ->
                StalkerCommands.register(dispatcher));
        ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.TOOLS_AND_UTILITIES)
                .register(entries -> entries.accept(ModItems.WARDING_CHARM));
    }
}
