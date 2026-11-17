package com.stalkerhorror.system;

import com.stalkerhorror.Config;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.Vec3;

/** Sustos "sem corpo": o que voce ouve e nao ve. Sao enviados apenas ao jogador assombrado. */
public final class Haunts {
    private Haunts() {}

    public static SoundEvent sound(String id) {
        return BuiltInRegistries.SOUND_EVENT.get(new ResourceLocation(id));
    }

    public static void playTo(ServerPlayer p, SoundEvent s, SoundSource src,
                              double x, double y, double z, float vol, float pitch) {
        if (s == null) return;
        p.connection.send(new ClientboundSoundPacket(BuiltInRegistries.SOUND_EVENT.wrapAsHolder(s),
                src, x, y, z, vol, pitch, p.getRandom().nextLong()));
    }

    public static void playTo(ServerPlayer p, String id, double x, double y, double z, float vol, float pitch) {
        playTo(p, sound(id), SoundSource.AMBIENT, x, y, z, vol, pitch);
    }

    private static ServerPlayer find(ServerPlayer p, UUID id) {
        if (p.getServer() == null) return null;
        return p.getServer().getPlayerList().getPlayer(id);
    }

    /** Escolhe um susto de acordo com a sanidade e o que o jogador ja fez. */
    public static void trigger(ServerPlayer p, FearProfile f) {
        RandomSource r = p.getRandom();
        float s = f.sanity();
        int roll = r.nextInt(100);
        boolean done = false;

        if (s < 35.0F && roll < 18) {
            heartbeat(p, 0.8F);
            done = true;
        } else if (roll < 42) {
            footsteps(p);
            done = true;
        } else if (roll < 60 && !f.mimic().isEmpty()) {
            done = mimic(p, f);
        } else if (roll < 70 && Config.OPEN_DOORS.get()) {
            done = door(p);
        } else if (roll < 80 && Config.SNUFF_TORCHES.get() && s < 65.0F) {
            done = snuffTorch(p);
        } else if (roll < 90 && s < 60.0F) {
            breath(p);
            done = true;
        }
        if (!done) ambient(p);
        f.addSanity(-0.6F);
    }

    /** Passos atras de voce que se aproximam... e param de repente. */
    public static void footsteps(ServerPlayer p) {
        RandomSource r = p.getRandom();
        double bearing = Math.toRadians(p.getYRot() + 180.0D + (r.nextInt(61) - 30));
        int steps = 4 + r.nextInt(4);
        double start = 9.0D + r.nextInt(4);
        double end = 4.5D;
        UUID id = p.getUUID();
        int delay = 0;
        for (int i = 0; i < steps; i++) {
            final double dist = start - i * (start - end) / steps;
            delay += 8 + r.nextInt(5);
            StalkerDirector.schedule(delay, () -> {
                ServerPlayer pl = find(p, id);
                if (pl == null || !pl.isAlive()) return;
                double x = pl.getX() - Math.sin(bearing) * dist;
                double z = pl.getZ() + Math.cos(bearing) * dist;
                BlockState below = pl.level().getBlockState(BlockPos.containing(x, pl.getY() - 0.5D, z));
                if (below.isAir()) below = pl.level().getBlockState(pl.blockPosition().below());
                SoundEvent step = below.getSoundType().getStepSound();
                playTo(pl, step, SoundSource.HOSTILE, x, pl.getY(), z,
                        0.55F, 0.75F + pl.getRandom().nextFloat() * 0.2F);
            });
        }
    }

    /** Repete, de longe, o ultimo som que VOCE fez (quebrar, construir, abrir bau/porta). */
    public static boolean mimic(ServerPlayer p, FearProfile f) {
        SoundEvent s = sound(f.mimic());
        if (s == null) return false;
        RandomSource r = p.getRandom();
        double ang = r.nextDouble() * Math.PI * 2.0D;
        double dist = 10.0D + r.nextInt(9);
        double x = p.getX() + Math.cos(ang) * dist;
        double z = p.getZ() + Math.sin(ang) * dist;
        int times = 1 + r.nextInt(2);
        UUID id = p.getUUID();
        for (int i = 0; i < times; i++) {
            StalkerDirector.schedule(i * (25 + r.nextInt(30)), () -> {
                ServerPlayer pl = find(p, id);
                if (pl != null) playTo(pl, s, SoundSource.BLOCKS, x, pl.getY(), z, 1.0F, 0.85F);
            });
        }
        return true;
    }

    public static void heartbeat(ServerPlayer p, float vol) {
        UUID id = p.getUUID();
        playTo(p, "entity.warden.heartbeat", p.getX(), p.getY(), p.getZ(), vol, 1.0F);
        StalkerDirector.schedule(8, () -> {
            ServerPlayer pl = find(p, id);
            if (pl != null) playTo(pl, "entity.warden.heartbeat", pl.getX(), pl.getY(), pl.getZ(), vol * 0.8F, 0.95F);
        });
    }

    public static void breath(ServerPlayer p) {
        double ang = Math.toRadians(p.getYRot() + 180.0D + (p.getRandom().nextInt(41) - 20));
        playTo(p, "entity.player.breath",
                p.getX() - Math.sin(ang) * 1.6D, p.getEyeY(), p.getZ() + Math.cos(ang) * 1.6D, 0.45F, 0.55F);
    }

    public static void ambient(ServerPlayer p) {
        RandomSource r = p.getRandom();
        double ang = r.nextDouble() * Math.PI * 2.0D;
        playTo(p, "ambient.cave", p.getX() + Math.cos(ang) * 8.0D, p.getY(), p.getZ() + Math.sin(ang) * 8.0D,
                0.8F, 0.5F + r.nextFloat() * 0.3F);
    }

    private static boolean looking(ServerPlayer p, BlockPos pos) {
        Vec3 to = Vec3.atCenterOf(pos).subtract(p.getEyePosition()).normalize();
        return p.getViewVector(1.0F).dot(to) > 0.6D;
    }

    /** Uma porta de madeira distante abre/fecha sozinha, fora do seu campo de visao. */
    public static boolean door(ServerPlayer p) {
        ServerLevel lvl = p.serverLevel();
        RandomSource r = lvl.random;
        BlockPos c = p.blockPosition();
        for (int i = 0; i < 160; i++) {
            BlockPos pos = c.offset(r.nextInt(33) - 16, r.nextInt(9) - 4, r.nextInt(33) - 16);
            if (!lvl.hasChunkAt(pos)) continue;
            BlockState st = lvl.getBlockState(pos);
            if (st.getBlock() instanceof DoorBlock door && st.is(BlockTags.WOODEN_DOORS)
                    && st.getValue(DoorBlock.HALF) == DoubleBlockHalf.LOWER) {
                if (pos.distSqr(c) < 36 || looking(p, pos)) continue;
                door.setOpen(null, lvl, st, pos, !st.getValue(DoorBlock.OPEN));
                return true;
            }
        }
        return false;
    }

    /** Uma tocha longe do seu olhar apaga (o item cai no chao, nada se perde). */
    public static boolean snuffTorch(ServerPlayer p) {
        ServerLevel lvl = p.serverLevel();
        RandomSource r = lvl.random;
        BlockPos c = p.blockPosition();
        for (int i = 0; i < 140; i++) {
            BlockPos pos = c.offset(r.nextInt(25) - 12, r.nextInt(11) - 5, r.nextInt(25) - 12);
            if (!lvl.hasChunkAt(pos)) continue;
            BlockState st = lvl.getBlockState(pos);
            if (st.is(Blocks.TORCH) || st.is(Blocks.WALL_TORCH)) {
                if (looking(p, pos)) continue;
                lvl.destroyBlock(pos, true);
                playTo(p, "block.fire.extinguish", pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D, 0.7F, 1.0F);
                return true;
            }
        }
        return false;
    }
}
