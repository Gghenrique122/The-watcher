package com.stalkerhorror.entity;

import com.stalkerhorror.Config;
import com.stalkerhorror.system.FearProfile;
import com.stalkerhorror.system.Haunts;
import com.stalkerhorror.system.StalkerDirector;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * O Observador. Nunca ataca, nunca chega perto. Observa, some quando voce encara,
 * e cada reacao sua (olhar, fugir, avancar) e registrada no FearProfile.
 */
public class StalkerEntity extends PathfinderMob {
    private UUID targetId;
    private StalkerStyle style = StalkerStyle.WATCHER;
    private int life;
    private int maxLife = 1500;
    private int maxStare = 40;
    private int stareTicks;
    private double preferDist = 26;
    private boolean seenOnce, focusedOnce;
    private double towardAcc, awayAcc;
    private Vec3 lastPlayerPos;

    public StalkerEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        this.setPersistenceRequired();
        this.setInvulnerable(true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.30D)
                .add(Attributes.FOLLOW_RANGE, 64.0D);
    }

    public void setup(UUID target, StalkerStyle style, int maxStare, int maxLife) {
        this.targetId = target;
        this.style = style;
        this.maxStare = Math.max(10, maxStare);
        this.maxLife = maxLife;
        this.preferDist = (style.minR + style.maxR) / 2.0;
    }

    public UUID getTargetId() {
        return targetId;
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) return;
        if (targetId == null) {
            discard();
            return;
        }

        ServerPlayer p = ((ServerLevel) level()).getServer().getPlayerList().getPlayer(targetId);
        if (p == null || p.level() != level() || !p.isAlive() || p.isSpectator()) {
            discard();
            return;
        }

        StalkerDirector.markActive(targetId, level().getGameTime());
        life++;
        double dist = distanceTo(p);
        face(p);

        boolean focus = inView(p, 0.955D);
        boolean seen = focus || inView(p, 0.62D);
        FearProfile f = FearProfile.of(p);

        // --- aprendizado: para onde o jogador se move depois de ter visto a criatura ---
        Vec3 pp = p.position();
        if (lastPlayerPos != null && seenOnce) {
            Vec3 dir = new Vec3(getX() - pp.x, 0, getZ() - pp.z).normalize();
            double proj = (pp.x - lastPlayerPos.x) * dir.x + (pp.z - lastPlayerPos.z) * dir.z;
            if (proj > 0) towardAcc += proj;
            else awayAcc -= proj;
        }
        lastPlayerPos = pp;

        if (seen) seenOnce = true;

        if (focus) {
            if (!focusedOnce) {
                focusedOnce = true;
                f.addSanity(-4.0F);
            }
            stareTicks++;
            getNavigation().stop();
            if (stareTicks % 20 == 0) f.addSanity(-1.5F);
            if (stareTicks % 22 == 1) {
                Haunts.playTo(p, Haunts.sound("entity.warden.heartbeat"), SoundSource.AMBIENT,
                        p.getX(), p.getY(), p.getZ(), 0.9F, 1.0F);
            }
            if (stareTicks >= maxStare) {
                f.add("stares", 1);   // aprendeu: voce encara
                vanish(p);
                return;
            }
        } else if (stareTicks > 0 && life % 3 == 0) {
            stareTicks--;
        }

        if (dist < Config.MIN_DISTANCE.get() || dist > 75.0D || life > maxLife) {
            vanish(p);
            return;
        }

        // PURSUIT: so se mexe quando voce NAO esta olhando direto
        if (style.walker && !focus && life % 10 == 0) {
            if (dist > preferDist + 3.0D) getNavigation().moveTo(p, 1.0D);
            else getNavigation().stop();
        }
    }

    private void vanish(ServerPlayer p) {
        FearProfile f = FearProfile.of(p);
        if (seenOnce) {
            if (towardAcc > 6.0D) f.add("approaches", 1);   // aprendeu: voce avanca
            if (awayAcc > 12.0D) f.add("flees", 1);         // aprendeu: voce foge
        }
        StalkerDirector.afterSighting(p, seenOnce);
        discard();
    }

    private void face(Player p) {
        double dx = p.getX() - getX();
        double dz = p.getZ() - getZ();
        float yaw = (float) (Mth.atan2(dz, dx) * (180.0D / Math.PI)) - 90.0F;
        setYRot(yaw);
        this.yBodyRot = yaw;
        this.yHeadRot = yaw;
        setXRot(0.0F);
    }

    private boolean inView(Player p, double cos) {
        Vec3 look = p.getViewVector(1.0F);
        Vec3 to = getEyePosition().subtract(p.getEyePosition());
        double len = to.length();
        if (len < 1.0E-4D) return true;
        return look.dot(to.scale(1.0D / len)) > cos && p.hasLineOfSight(this);
    }

    // --- sem interacao fisica / sem som proprio / nao salva no mundo ---
    @Override public boolean isPickable() { return false; }
    @Override public boolean isPushable() { return false; }
    @Override protected void pushEntities() { }
    @Override public boolean shouldBeSaved() { return false; }
    @Override public boolean removeWhenFarAway(double d) { return false; }
    @Override protected void playStepSound(BlockPos pos, BlockState state) { }
    @Override public boolean hurt(DamageSource source, float amount) { return false; }
}
