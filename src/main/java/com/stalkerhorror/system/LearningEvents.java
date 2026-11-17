package com.stalkerhorror.system;

import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/** O Observador presta atencao em tudo o que voce faz. */
public final class LearningEvents {
    private LearningEvents() {}

    public static void init() {
        PlayerBlockBreakEvents.AFTER.register((level, player, pos, state, be) -> {
            if (!(player instanceof ServerPlayer p)) return;
            FearProfile f = FearProfile.of(p);
            f.add("breaks", 1);
            if (p.getY() < 50) f.add("mining", 1);
            SoundEvent s = state.getSoundType().getBreakSound();
            f.setMimic(s.getLocation().toString());
        });

        UseBlockCallback.EVENT.register((player, level, hand, hit) -> {
            if (level.isClientSide || hand != InteractionHand.MAIN_HAND || !(player instanceof ServerPlayer p)) {
                return InteractionResult.PASS;
            }
            BlockState st = level.getBlockState(hit.getBlockPos());
            FearProfile f = FearProfile.of(p);
            if (st.is(BlockTags.WOODEN_DOORS) || st.is(BlockTags.WOODEN_TRAPDOORS)) {
                f.add("doors", 1);
                f.setMimic("block.wooden_door.open");
            } else if (st.is(Blocks.CHEST) || st.is(Blocks.TRAPPED_CHEST) || st.is(Blocks.BARREL)) {
                f.add("chests", 1);
                f.setMimic(st.is(Blocks.BARREL) ? "block.barrel.open" : "block.chest.open");
            } else if (p.getMainHandItem().getItem() instanceof BlockItem bi) {
                f.add("builds", 1);
                f.setMimic(bi.getBlock().defaultBlockState().getSoundType().getPlaceSound().getLocation().toString());
            }
            return InteractionResult.PASS;
        });

        // a memoria fica no mundo (por UUID); ao morrer, a sanidade volta a pelo menos 70
        ServerPlayerEvents.COPY_FROM.register((oldPlayer, newPlayer, alive) -> {
            if (!alive) {
                FearProfile f = FearProfile.of(newPlayer);
                f.setSanity(Math.max(70.0F, f.sanity()));
                f.add("deaths", 1);
            }
        });
    }
}
