package net.kaupenjoe.tutorialmod.block.custom;

import net.kaupenjoe.tutorialmod.Config;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public class AlexandriteSpreadingBlock extends Block {
    public AlexandriteSpreadingBlock(Properties properties) {
        super(properties);
    }

    @Override
    public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
        if (!level.isClientSide() && entity instanceof LivingEntity livingEntity && entity.tickCount % 100 == 0) {
            livingEntity.hurt(level.damageSources().magic(), 1.0F);
        }

        super.stepOn(level, pos, state, entity);
    }

    @Override
    public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        int attempts = Math.max(1, Config.alexandriteSpreadAttempts);

        for (int i = 0; i < attempts; i++) {
            BlockPos targetPos = pos.offset(random.nextInt(3) - 1, random.nextInt(3) - 1, random.nextInt(3) - 1);
            if (targetPos.equals(pos)) {
                continue;
            }

            BlockState targetState = level.getBlockState(targetPos);
            if (!isValidTarget(targetState)) {
                continue;
            }

            level.setBlockAndUpdate(targetPos, defaultBlockState());
            level.sendParticles(ParticleTypes.PORTAL,
                    targetPos.getX() + 0.5, targetPos.getY() + 0.5, targetPos.getZ() + 0.5,
                    2, 0.2, 0.2, 0.2, 0.05);
        }
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(20) == 0) {
            level.addParticle(ParticleTypes.WITCH,
                    pos.getX() + random.nextDouble(),
                    pos.getY() + 1.1,
                    pos.getZ() + random.nextDouble(),
                    0.0, 0.0, 0.0);
        }
    }

    private boolean isValidTarget(BlockState state) {
        if (state.isAir() || state.is(this)) {
            return false;
        }

        return state.is(Blocks.STONE)
                || state.is(Blocks.DIRT)
                || state.is(Blocks.GRASS_BLOCK)
                || state.is(Blocks.DEEPSLATE)
                || state.is(Blocks.SAND)
                || state.is(Blocks.GRAVEL)
                || state.is(Blocks.CLAY)
                || state.is(Blocks.MUD)
                || state.is(BlockTags.LOGS)
                || state.is(BlockTags.LEAVES);
    }
}
