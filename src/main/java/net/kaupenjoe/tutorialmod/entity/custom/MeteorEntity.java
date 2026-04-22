package net.kaupenjoe.tutorialmod.entity.custom;

import net.kaupenjoe.tutorialmod.entity.ModEntityTypes;
import net.kaupenjoe.tutorialmod.util.MathUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.entity.IEntityAdditionalSpawnData;

import java.util.ArrayList;
import java.util.List;

public class MeteorEntity extends Entity implements IEntityAdditionalSpawnData {
    private static final int AFTERBURN_LIFETIME_TICKS = 160; // ~8 секунд
    private static final int MAX_AFTERBURN_POINTS = 90;

    private Vec3 strikeTarget = Vec3.ZERO;
    private final List<AfterburnPoint> afterburnPoints = new ArrayList<>();

    public MeteorEntity(EntityType<? extends MeteorEntity> entityType, Level level) {
        super(entityType, level);
        noPhysics = true;
        noCulling = true;
    }

    public MeteorEntity(Level level, Vec3 spawnPosition, Vec3 velocity, Vec3 strikeTarget) {
        this(ModEntityTypes.METEOR.get(), level);
        this.strikeTarget = strikeTarget;
        moveTo(spawnPosition.x, spawnPosition.y, spawnPosition.z, 0.0F, 0.0F);
        setDeltaMovement(velocity);
        refreshRotationFromMotion();
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        if (tag.contains("StrikeTargetX")) {
            strikeTarget = new Vec3(
                    tag.getDouble("StrikeTargetX"),
                    tag.getDouble("StrikeTargetY"),
                    tag.getDouble("StrikeTargetZ")
            );
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putDouble("StrikeTargetX", strikeTarget.x);
        tag.putDouble("StrikeTargetY", strikeTarget.y);
        tag.putDouble("StrikeTargetZ", strikeTarget.z);
    }

    @Override
    public void tick() {
        super.tick();

        Vec3 previousPosition = position();
        move(MoverType.SELF, getDeltaMovement());
        refreshRotationFromMotion();

        if (level().isClientSide) {
            spawnClientTrail();
        } else {
            ServerLevel serverLevel = (ServerLevel) level();
            destroyBlocksAlongPath(serverLevel, previousPosition, position());
            spawnServerTrail(serverLevel);
            playFlightSound(serverLevel);
            tickAfterburn(serverLevel);

            if (position().distanceToSqr(strikeTarget) < 4.0D) {
                discard();
            }
        }
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public void writeSpawnData(FriendlyByteBuf buffer) {
        buffer.writeDouble(strikeTarget.x);
        buffer.writeDouble(strikeTarget.y);
        buffer.writeDouble(strikeTarget.z);
    }

    @Override
    public void readSpawnData(FriendlyByteBuf additionalData) {
        strikeTarget = new Vec3(
                additionalData.readDouble(),
                additionalData.readDouble(),
                additionalData.readDouble()
        );
    }

    public Vec3 getStrikeTarget() {
        return strikeTarget;
    }

    private void refreshRotationFromMotion() {
        Vec3 motion = getDeltaMovement();
        if (motion.lengthSqr() < 1.0E-5D) {
            return;
        }

        float[] rotation = MathUtil.getYawPitch(position(), position().add(motion));
        setYRot(rotation[0]);
        setXRot(rotation[1]);
        setYHeadRot(rotation[0]);
        setYBodyRot(rotation[0]);
    }

    private void spawnClientTrail() {
        Vec3 velocity = getDeltaMovement();
        if (velocity.lengthSqr() < 1.0E-5D) {
            return;
        }

        Vec3 backward = velocity.normalize().scale(-1.55D);

        for (int segment = 0; segment < 12; segment++) {
            double stretch = 1.15D + segment * 0.28D;
            double width = 1.15D + segment * 0.24D;
            Vec3 trailPos = position().add(backward.scale(stretch));

            for (int i = 0; i < 38; i++) {
                level().addParticle(ParticleTypes.FLAME,
                        trailPos.x + (random.nextDouble() - 0.5D) * width * 2.6D,
                        trailPos.y + (random.nextDouble() - 0.25D) * width * 1.0D,
                        trailPos.z + (random.nextDouble() - 0.5D) * width * 2.6D,
                        backward.x * 0.32D, backward.y * 0.18D, backward.z * 0.32D);
            }

            for (int i = 0; i < 20; i++) {
                level().addParticle(ParticleTypes.SOUL_FIRE_FLAME,
                        trailPos.x + (random.nextDouble() - 0.5D) * width * 2.1D,
                        trailPos.y + (random.nextDouble() - 0.25D) * width * 0.9D,
                        trailPos.z + (random.nextDouble() - 0.5D) * width * 2.1D,
                        backward.x * 0.18D, backward.y * 0.11D, backward.z * 0.18D);
            }

            for (int i = 0; i < 28; i++) {
                level().addParticle(ParticleTypes.SMOKE,
                        trailPos.x + (random.nextDouble() - 0.5D) * width * 2.9D,
                        trailPos.y + random.nextDouble() * width * 0.95D,
                        trailPos.z + (random.nextDouble() - 0.5D) * width * 2.9D,
                        backward.x * 0.10D, 0.012D, backward.z * 0.10D);
            }

            for (int i = 0; i < 18; i++) {
                level().addParticle(ParticleTypes.ASH,
                        trailPos.x + (random.nextDouble() - 0.5D) * width * 3.0D,
                        trailPos.y + random.nextDouble() * width * 1.0D,
                        trailPos.z + (random.nextDouble() - 0.5D) * width * 3.0D,
                        backward.x * 0.05D, 0.012D, backward.z * 0.05D);
            }

            for (int i = 0; i < 14; i++) {
                level().addParticle(ParticleTypes.LARGE_SMOKE,
                        trailPos.x + (random.nextDouble() - 0.5D) * width * 2.2D,
                        trailPos.y + random.nextDouble() * width * 0.65D,
                        trailPos.z + (random.nextDouble() - 0.5D) * width * 2.2D,
                        backward.x * 0.08D, 0.02D, backward.z * 0.08D);
            }

            for (int i = 0; i < 10; i++) {
                level().addParticle(ParticleTypes.CAMPFIRE_SIGNAL_SMOKE,
                        trailPos.x + (random.nextDouble() - 0.5D) * width * 2.0D,
                        trailPos.y + random.nextDouble() * width * 0.60D,
                        trailPos.z + (random.nextDouble() - 0.5D) * width * 2.0D,
                        backward.x * 0.05D, 0.03D, backward.z * 0.05D);
            }

            if (segment < 5) {
                for (int i = 0; i < 12; i++) {
                    level().addParticle(ParticleTypes.END_ROD,
                            trailPos.x + (random.nextDouble() - 0.5D) * width * 1.15D,
                            trailPos.y + (random.nextDouble() - 0.5D) * width * 0.48D,
                            trailPos.z + (random.nextDouble() - 0.5D) * width * 1.15D,
                            backward.x * 0.04D, backward.y * 0.02D, backward.z * 0.04D);
                }

                for (int i = 0; i < 7; i++) {
                    level().addParticle(ParticleTypes.ELECTRIC_SPARK,
                            trailPos.x + (random.nextDouble() - 0.5D) * width * 0.95D,
                            trailPos.y + (random.nextDouble() - 0.5D) * width * 0.38D,
                            trailPos.z + (random.nextDouble() - 0.5D) * width * 0.95D,
                            backward.x * 0.03D, 0.01D, backward.z * 0.03D);
                }
            }
        }

        for (int i = 0; i < 10; i++) {
            level().addParticle(ParticleTypes.LAVA,
                    getX() + (random.nextDouble() - 0.5D) * 1.9D,
                    getY() + (random.nextDouble() - 0.5D) * 0.8D,
                    getZ() + (random.nextDouble() - 0.5D) * 1.9D,
                    backward.x * 0.15D, backward.y * 0.08D, backward.z * 0.15D);
        }

        if (tickCount % 2 == 0) {
            level().addParticle(ParticleTypes.FLASH, getX(), getY(), getZ(), 0.0D, 0.0D, 0.0D);
        }
    }

    private void spawnServerTrail(ServerLevel level) {
        Vec3 velocity = getDeltaMovement();
        if (velocity.lengthSqr() < 1.0E-5D) {
            return;
        }

        Vec3 backward = velocity.normalize().scale(-1.12D);

        for (int segment = 0; segment < 10; segment++) {
            double stretch = 1.15D + segment * 0.32D;
            double width = 1.20D + segment * 0.26D;
            Vec3 trailPos = position().add(backward.scale(stretch));

            level.sendParticles(ParticleTypes.FLAME,
                    trailPos.x, trailPos.y, trailPos.z,
                    72, width * 1.0D, width * 0.38D, width * 1.0D, 0.11D);

            level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
                    trailPos.x, trailPos.y, trailPos.z,
                    34, width * 0.72D, width * 0.25D, width * 0.72D, 0.05D);

            level.sendParticles(ParticleTypes.SMOKE,
                    trailPos.x, trailPos.y + 0.16D, trailPos.z,
                    42, width * 1.08D, width * 0.38D, width * 1.08D, 0.03D);

            level.sendParticles(ParticleTypes.ASH,
                    trailPos.x, trailPos.y + 0.18D, trailPos.z,
                    28, width * 1.0D, width * 0.35D, width * 1.0D, 0.03D);

            level.sendParticles(ParticleTypes.LARGE_SMOKE,
                    trailPos.x, trailPos.y + 0.25D, trailPos.z,
                    30, width * 0.90D, width * 0.30D, width * 0.90D, 0.025D);

            level.sendParticles(ParticleTypes.CAMPFIRE_SIGNAL_SMOKE,
                    trailPos.x, trailPos.y + 0.40D, trailPos.z,
                    22, width * 0.75D, width * 0.22D, width * 0.75D, 0.018D);

            if (segment < 3) {
                level.sendParticles(ParticleTypes.END_ROD,
                        trailPos.x, trailPos.y, trailPos.z,
                        14, width * 0.35D, width * 0.12D, width * 0.35D, 0.05D);

                level.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                        trailPos.x, trailPos.y, trailPos.z,
                        8, width * 0.22D, width * 0.08D, width * 0.22D, 0.03D);
            }
        }

        if (tickCount % 3 == 0) {
            level.sendParticles(ParticleTypes.FLASH, getX(), getY(), getZ(),
                    2, 0.18D, 0.18D, 0.18D, 0.0D);
        }
    }

    private void playFlightSound(ServerLevel level) {
        if (tickCount % 5 != 0) {
            return;
        }

        float speed = (float) getDeltaMovement().length();
        float volume = Math.min(3.4F, 0.85F + speed * 2.4F);
        float pitch = Math.max(0.42F, 1.10F - speed * 0.16F);

        level.playSound(null, getX(), getY(), getZ(), SoundEvents.ELYTRA_FLYING, SoundSource.HOSTILE, volume * 0.65F, 0.52F);
        level.playSound(null, getX(), getY(), getZ(), SoundEvents.PHANTOM_FLAP, SoundSource.HOSTILE, volume * 0.40F, 0.55F);
        level.playSound(null, getX(), getY(), getZ(), SoundEvents.FIRECHARGE_USE, SoundSource.HOSTILE, volume, pitch);
    }

    private void destroyBlocksAlongPath(ServerLevel level, Vec3 from, Vec3 to) {
        Vec3 path = to.subtract(from);
        double pathLength = path.length();
        int steps = Math.max(4, (int) Math.ceil(pathLength * 4.5D));

        Vec3 motion = getDeltaMovement();
        Vec3 forward = motion.lengthSqr() < 1.0E-5D ? new Vec3(1.0D, 0.0D, 0.0D) : motion.normalize();
        Vec3 side = new Vec3(-forward.z, 0.0D, forward.x);
        if (side.lengthSqr() < 1.0E-5D) {
            side = new Vec3(1.0D, 0.0D, 0.0D);
        } else {
            side = side.normalize();
        }

        for (int step = 0; step <= steps; step++) {
            double progress = step / (double) steps;
            Vec3 sample = from.lerp(to, progress);
            BlockPos center = BlockPos.containing(sample);

            double distToTarget = Math.sqrt(sample.distanceToSqr(strikeTarget));

            double alongRadius = distToTarget < 18.0D ? 8.2D : 5.8D;
            double acrossRadius = distToTarget < 18.0D ? 5.4D : 3.6D;
            double verticalRadius = distToTarget < 18.0D ? 4.8D : 3.0D;

            int rx = Mth.ceil(alongRadius);
            int ry = Mth.ceil(verticalRadius);
            int rz = Mth.ceil(alongRadius);

            for (BlockPos target : BlockPos.betweenClosed(
                    center.offset(-rx, -ry, -rz),
                    center.offset(rx, ry, rz))) {

                BlockState state = level.getBlockState(target);
                if (state.isAir() || state.is(Blocks.BEDROCK)) {
                    continue;
                }

                if (state.getDestroySpeed(level, target) < 0.0F) {
                    continue;
                }

                Vec3 rel = Vec3.atCenterOf(target).subtract(sample);
                double along = rel.dot(forward);
                double across = rel.dot(side);
                double vertical = rel.y;

                double shape = (along * along) / (alongRadius * alongRadius)
                        + (across * across) / (acrossRadius * acrossRadius)
                        + (vertical * vertical) / (verticalRadius * verticalRadius);

                if (shape > 1.0D) {
                    continue;
                }

                level.removeBlock(target, false);

                level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, state),
                        target.getX() + 0.5D, target.getY() + 0.5D, target.getZ() + 0.5D,
                        22, 0.50D, 0.50D, 0.50D, 0.15D);

                level.sendParticles(ParticleTypes.FLAME,
                        target.getX() + 0.5D, target.getY() + 0.5D, target.getZ() + 0.5D,
                        10, 0.28D, 0.28D, 0.28D, 0.03D);

                level.sendParticles(ParticleTypes.SMOKE,
                        target.getX() + 0.5D, target.getY() + 0.5D, target.getZ() + 0.5D,
                        8, 0.24D, 0.24D, 0.24D, 0.02D);

                level.sendParticles(ParticleTypes.ASH,
                        target.getX() + 0.5D, target.getY() + 0.5D, target.getZ() + 0.5D,
                        9, 0.24D, 0.24D, 0.24D, 0.02D);

                BlockPos firePos = target.above();
                if (level.getBlockState(firePos).isAir() && random.nextInt(3) != 0) {
                    level.setBlockAndUpdate(firePos, BaseFireBlock.getState(level, firePos));
                    addAfterburnPoint(firePos);
                }

                addAfterburnPoint(target.above());
            }

            if (step % 2 == 0) {
                scorchTunnelShell(level, center, forward, side,
                        alongRadius + 1.2D, acrossRadius + 1.1D, verticalRadius + 1.0D);
            }
        }
    }

    private void scorchTunnelShell(ServerLevel level, BlockPos center, Vec3 forward, Vec3 side,
                                   double alongRadius, double acrossRadius, double verticalRadius) {
        int rx = Mth.ceil(alongRadius);
        int ry = Mth.ceil(verticalRadius);
        int rz = Mth.ceil(alongRadius);

        for (BlockPos target : BlockPos.betweenClosed(
                center.offset(-rx, -ry, -rz),
                center.offset(rx, ry, rz))) {

            BlockState state = level.getBlockState(target);
            if (state.isAir() || state.is(Blocks.BEDROCK) || state.getDestroySpeed(level, target) < 0.0F) {
                continue;
            }

            Vec3 rel = Vec3.atCenterOf(target).subtract(Vec3.atCenterOf(center));
            double along = rel.dot(forward);
            double across = rel.dot(side);
            double vertical = rel.y;

            double shell = (along * along) / (alongRadius * alongRadius)
                    + (across * across) / (acrossRadius * acrossRadius)
                    + (vertical * vertical) / (verticalRadius * verticalRadius);

            if (shell < 0.80D || shell > 1.32D) {
                continue;
            }

            if (random.nextInt(8) == 0) {
                Block replacement = switch (random.nextInt(8)) {
                    case 0 -> Blocks.MAGMA_BLOCK;
                    case 1, 2, 3 -> Blocks.BLACKSTONE;
                    case 4, 5 -> Blocks.BASALT;
                    case 6 -> Blocks.NETHERRACK;
                    default -> Blocks.COARSE_DIRT;
                };
                level.setBlockAndUpdate(target, replacement.defaultBlockState());
            }

            if (random.nextInt(12) == 0) {
                BlockPos firePos = target.above();
                if (level.getBlockState(firePos).isAir()) {
                    level.setBlockAndUpdate(firePos, BaseFireBlock.getState(level, firePos));
                    addAfterburnPoint(firePos);
                }
            }

            if (random.nextInt(4) == 0) {
                level.sendParticles(ParticleTypes.ASH,
                        target.getX() + 0.5D, target.getY() + 0.6D, target.getZ() + 0.5D,
                        4, 0.18D, 0.12D, 0.18D, 0.01D);
            }

            if (random.nextInt(5) == 0) {
                level.sendParticles(ParticleTypes.SMOKE,
                        target.getX() + 0.5D, target.getY() + 0.6D, target.getZ() + 0.5D,
                        3, 0.16D, 0.08D, 0.16D, 0.01D);
            }

            if (random.nextInt(10) == 0) {
                addAfterburnPoint(target.above());
            }
        }
    }

    private void tickAfterburn(ServerLevel level) {
        if (afterburnPoints.isEmpty()) {
            return;
        }

        for (int i = afterburnPoints.size() - 1; i >= 0; i--) {
            AfterburnPoint point = afterburnPoints.get(i);
            point.age++;

            if (point.age > AFTERBURN_LIFETIME_TICKS) {
                afterburnPoints.remove(i);
                continue;
            }

            if (point.age % 4 != 0) {
                continue;
            }

            BlockPos pos = point.pos;
            if (level.getBlockState(pos).isAir() && level.getBlockState(pos.below()).isSolid()) {
                if (point.age < AFTERBURN_LIFETIME_TICKS * 0.65D && level.random.nextInt(3) != 0) {
                    level.setBlockAndUpdate(pos, BaseFireBlock.getState(level, pos));
                }
            }

            level.sendParticles(ParticleTypes.SMOKE,
                    pos.getX() + 0.5D, pos.getY() + 0.3D, pos.getZ() + 0.5D,
                    2, 0.18D, 0.12D, 0.18D, 0.01D);

            level.sendParticles(ParticleTypes.ASH,
                    pos.getX() + 0.5D, pos.getY() + 0.35D, pos.getZ() + 0.5D,
                    2, 0.16D, 0.10D, 0.16D, 0.01D);

            if (point.age < AFTERBURN_LIFETIME_TICKS * 0.45D) {
                level.sendParticles(ParticleTypes.FLAME,
                        pos.getX() + 0.5D, pos.getY() + 0.12D, pos.getZ() + 0.5D,
                        1, 0.08D, 0.02D, 0.08D, 0.01D);
            }
        }
    }

    private void addAfterburnPoint(BlockPos pos) {
        if (afterburnPoints.size() >= MAX_AFTERBURN_POINTS) {
            return;
        }

        for (AfterburnPoint existing : afterburnPoints) {
            if (existing.pos.closerThan(pos, 2.0D)) {
                return;
            }
        }

        afterburnPoints.add(new AfterburnPoint(pos.immutable()));
    }

    private static final class AfterburnPoint {
        private final BlockPos pos;
        private int age;

        private AfterburnPoint(BlockPos pos) {
            this.pos = pos;
        }
    }
}