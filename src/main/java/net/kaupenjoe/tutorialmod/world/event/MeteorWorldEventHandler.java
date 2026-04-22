package net.kaupenjoe.tutorialmod.world.event;

import com.mojang.brigadier.context.CommandContext;
import net.kaupenjoe.tutorialmod.Config;
import net.kaupenjoe.tutorialmod.TutorialMod;
import net.kaupenjoe.tutorialmod.block.ModBlocks;
import net.kaupenjoe.tutorialmod.entity.custom.MeteorEntity;
import net.kaupenjoe.tutorialmod.network.ModMessages;
import net.kaupenjoe.tutorialmod.network.packet.MeteorFlashS2CPacket;
import net.kaupenjoe.tutorialmod.world.data.MeteorEventSavedData;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.util.List;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = TutorialMod.MOD_ID)
public final class MeteorWorldEventHandler {
    private static final int METEOR_SPAWN_TICK = 1;
    private static final int EVENT_END_TICK = 240;
    private static final double IMPACT_SOUND_RADIUS = 1000.0D;
    private static final double ECHO_SOUND_RADIUS = 300.0D;

    private static ActiveMeteorEvent activeEvent;

    private MeteorWorldEventHandler() {
    }

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.serverLevel().dimension() != Level.OVERWORLD) {
            return;
        }

        MeteorEventSavedData data = MeteorEventSavedData.get(player.serverLevel());
        if (data.isCompleted() || data.isScheduled()) {
            return;
        }

        int delay = Mth.nextInt(player.getRandom(), Config.meteorMinDelayTicks, Config.meteorMaxDelayTicks);
        data.schedule(player.serverLevel().getGameTime() + delay);
        TutorialMod.LOGGER.info("Scheduled meteor event in {} ticks.", delay);
    }

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) {
            return;
        }

        ServerLevel level = server.overworld();
        if (level == null) {
            return;
        }

        MeteorEventSavedData data = MeteorEventSavedData.get(level);
        if (activeEvent == null) {
            if (data.shouldStart(level.getGameTime())) {
                startEvent(level, null);
            }
            return;
        }

        tickActiveEvent(level, data);
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        activeEvent = null;
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("meteor")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("start")
                        .executes(MeteorWorldEventHandler::runMeteorStartCommand)));
    }

    private static int runMeteorStartCommand(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        ServerLevel level = source.getServer().overworld();
        ServerPlayer focalPlayer = source.getEntity() instanceof ServerPlayer player ? player : null;

        if (level == null) {
            source.sendFailure(Component.literal("Overworld not available for meteor event."));
            return 0;
        }

        if (activeEvent != null) {
            source.sendFailure(Component.literal("Meteor event is already running."));
            return 0;
        }

        if (!startEvent(level, focalPlayer)) {
            source.sendFailure(Component.literal("No active players found in the overworld."));
            return 0;
        }

        source.sendSuccess(() -> Component.literal("Meteor event started."), true);
        return 1;
    }

    public static boolean summonMeteorForPlayer(ServerLevel level, ServerPlayer player) {
        if (activeEvent != null) {
            return false;
        }

        return startEvent(level, player);
    }

    private static boolean startEvent(ServerLevel level, ServerPlayer requestedFocalPlayer) {
        List<ServerPlayer> players = level.players().stream()
                .filter(player -> !player.isSpectator())
                .toList();
        if (players.isEmpty()) {
            return false;
        }

        ServerPlayer focalPlayer = requestedFocalPlayer != null
                && requestedFocalPlayer.serverLevel() == level
                && !requestedFocalPlayer.isSpectator()
                ? requestedFocalPlayer
                : players.get(0);

        StrikePlan plan = createStrikePlan(level, focalPlayer, players);
        activeEvent = new ActiveMeteorEvent(
                plan.strikePos(),
                plan.viewPos(),
                plan.meteorSpawnPos(),
                plan.meteorVelocity(),
                Vec3.atCenterOf(plan.strikePos()),
                plan.sideVector(),
                plan.approachVector()
        );

        level.playSound(null, focalPlayer.blockPosition(),
                SoundEvents.BEACON_POWER_SELECT, SoundSource.AMBIENT, 1.1F, 0.7F);

        focalPlayer.sendSystemMessage(Component.literal(
                "Метеорит летит в точку: "
                        + plan.strikePos().getX() + " "
                        + plan.strikePos().getY() + " "
                        + plan.strikePos().getZ()
        ).withStyle(ChatFormatting.GOLD));

        TutorialMod.LOGGER.info("Meteor started at {}", activeEvent.strikePos);
        return true;
    }

    private static void tickActiveEvent(ServerLevel level, MeteorEventSavedData data) {
        activeEvent.tick++;

        if (activeEvent.tick == METEOR_SPAWN_TICK) {
            triggerAtmosphereEntry(level, activeEvent);
            spawnMeteor(level, activeEvent);
        }

        if (!activeEvent.impactDone && meteorReachedTarget(level, activeEvent)) {
            doImpact(level, activeEvent);
        }

        if (!activeEvent.impactDone && activeEvent.tick > 200) {
            doImpact(level, activeEvent);
        }

        if (activeEvent.tick >= EVENT_END_TICK) {
            finishEvent(level, data);
        }
    }

    private static void triggerAtmosphereEntry(ServerLevel level, ActiveMeteorEvent event) {
        sendFlashToPlayers(level, 30);

        level.playSound(null, BlockPos.containing(event.viewPos), SoundEvents.WITHER_SPAWN, SoundSource.HOSTILE, 1.5F, 1.8F);
        level.playSound(null, BlockPos.containing(event.meteorSpawnPos), SoundEvents.FIRECHARGE_USE, SoundSource.HOSTILE, 2.2F, 0.5F);
        level.playSound(null, BlockPos.containing(event.meteorSpawnPos), SoundEvents.BLAZE_SHOOT, SoundSource.HOSTILE, 2.4F, 0.45F);
        level.playSound(null, BlockPos.containing(event.viewPos), SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.HOSTILE, 1.7F, 1.8F);

        level.sendParticles(ParticleTypes.FLASH, event.meteorSpawnPos.x, event.meteorSpawnPos.y, event.meteorSpawnPos.z,
                10, 2.8D, 2.0D, 2.8D, 0.0D);
        level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, event.meteorSpawnPos.x, event.meteorSpawnPos.y, event.meteorSpawnPos.z,
                150, 1.9D, 1.2D, 1.9D, 0.20D);
        level.sendParticles(ParticleTypes.LARGE_SMOKE, event.meteorSpawnPos.x, event.meteorSpawnPos.y, event.meteorSpawnPos.z,
                120, 2.4D, 1.4D, 2.4D, 0.06D);
        level.sendParticles(ParticleTypes.ASH, event.meteorSpawnPos.x, event.meteorSpawnPos.y, event.meteorSpawnPos.z,
                90, 2.2D, 1.2D, 2.2D, 0.04D);
        level.sendParticles(ParticleTypes.END_ROD, event.meteorSpawnPos.x, event.meteorSpawnPos.y, event.meteorSpawnPos.z,
                90, 2.5D, 1.5D, 2.5D, 0.16D);
        level.sendParticles(ParticleTypes.FLAME, event.meteorSpawnPos.x, event.meteorSpawnPos.y, event.meteorSpawnPos.z,
                140, 2.1D, 1.1D, 2.1D, 0.18D);
        level.sendParticles(ParticleTypes.CAMPFIRE_SIGNAL_SMOKE, event.meteorSpawnPos.x, event.meteorSpawnPos.y, event.meteorSpawnPos.z,
                80, 1.9D, 1.0D, 1.9D, 0.04D);
    }

    private static void spawnMeteor(ServerLevel level, ActiveMeteorEvent event) {
        MeteorEntity meteor = new MeteorEntity(level, event.meteorSpawnPos, event.meteorVelocity, event.impactCenter);
        level.addFreshEntity(meteor);
        event.meteorId = meteor.getUUID();
    }

    private static boolean meteorReachedTarget(ServerLevel level, ActiveMeteorEvent event) {
        if (event.meteorId == null) {
            return false;
        }

        MeteorEntity meteor = getMeteor(level, event.meteorId);
        return meteor == null || meteor.position().distanceToSqr(event.impactCenter) < 6.0D;
    }

    private static void doImpact(ServerLevel level, ActiveMeteorEvent event) {
        event.impactDone = true;
        boolean waterImpact = isWaterImpact(level, event.strikePos);

        MeteorEntity meteor = getMeteor(level, event.meteorId);
        if (meteor != null) {
            meteor.discard();
        }

        sendFlashToPlayers(level, 60);
        playImpactSounds(level, event.impactCenter);
        applyEarthquake(level, event.impactCenter);

        level.explode(null, event.impactCenter.x, event.impactCenter.y, event.impactCenter.z,
                17.0F, true, Level.ExplosionInteraction.TNT);

        if (waterImpact) {
            handleWaterImpact(level, event.impactCenter);
        }

        makeCrater(level, event.strikePos, event.meteorVelocity, 11, waterImpact);
        placeMeteorRemnant(level, event.strikePos, event.meteorVelocity, waterImpact);
        scorchImpactRing(level, event.strikePos, 7, 15, waterImpact);
        igniteCrater(level, event.strikePos, waterImpact ? 4 : 11);
        spawnImpactDebris(level, event.strikePos, waterImpact);
        applyShockwave(level, event.impactCenter, 30.0D);
        spawnGroundShockwave(level, event.strikePos, waterImpact);
        scorchFlightPath(level, event.impactCenter, event.meteorVelocity);

        level.sendParticles(ParticleTypes.FLASH,
                event.impactCenter.x, event.impactCenter.y + 1.0D, event.impactCenter.z,
                18, 2.0D, 1.0D, 2.0D, 0.0D);

        level.sendParticles(ParticleTypes.EXPLOSION_EMITTER,
                event.impactCenter.x, event.impactCenter.y, event.impactCenter.z,
                24, 2.0D, 1.2D, 2.0D, 0.0D);

        level.sendParticles(ParticleTypes.EXPLOSION,
                event.impactCenter.x, event.impactCenter.y + 0.7D, event.impactCenter.z,
                96, 5.1D, 2.6D, 5.1D, 0.05D);

        level.sendParticles(ParticleTypes.FLAME,
                event.impactCenter.x, event.impactCenter.y + 0.4D, event.impactCenter.z,
                950, 6.1D, 2.9D, 6.1D, 0.33D);

        level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
                event.impactCenter.x, event.impactCenter.y + 0.6D, event.impactCenter.z,
                210, 3.7D, 2.0D, 3.7D, 0.11D);

        level.sendParticles(ParticleTypes.ASH,
                event.impactCenter.x, event.impactCenter.y + 1.0D, event.impactCenter.z,
                320, 4.8D, 2.2D, 4.8D, 0.05D);

        level.sendParticles(ParticleTypes.LAVA,
                event.impactCenter.x, event.impactCenter.y + 0.2D, event.impactCenter.z,
                320, 3.5D, 1.8D, 3.5D, 0.25D);

        level.sendParticles(ParticleTypes.CAMPFIRE_SIGNAL_SMOKE,
                event.impactCenter.x, event.impactCenter.y + 1.3D, event.impactCenter.z,
                520, 5.2D, 4.1D, 5.2D, 0.11D);

        level.sendParticles(ParticleTypes.LARGE_SMOKE,
                event.impactCenter.x, event.impactCenter.y + 1.0D, event.impactCenter.z,
                470, 5.2D, 2.9D, 5.2D, 0.10D);

        level.sendParticles(ParticleTypes.SMOKE,
                event.impactCenter.x, event.impactCenter.y + 0.8D, event.impactCenter.z,
                260, 4.6D, 2.2D, 4.6D, 0.05D);

        level.sendParticles(ParticleTypes.CLOUD,
                event.impactCenter.x, event.impactCenter.y + 0.4D, event.impactCenter.z,
                360, 6.0D, 1.3D, 6.0D, 0.14D);

        level.sendParticles(ParticleTypes.END_ROD,
                event.impactCenter.x, event.impactCenter.y + 1.5D, event.impactCenter.z,
                260, 3.0D, 1.9D, 3.0D, 0.18D);
    }

    private static void playImpactSounds(ServerLevel level, Vec3 center) {
        for (ServerPlayer player : level.players()) {
            if (player.isSpectator()) {
                continue;
            }

            double distance = player.position().distanceTo(center);
            if (distance > IMPACT_SOUND_RADIUS) {
                continue;
            }

            double factor = 1.0D - (distance / IMPACT_SOUND_RADIUS);
            float bassPitch = Math.max(0.30F, 0.82F - (float) factor * 0.38F);
            float crackPitch = Math.max(0.42F, 0.88F - (float) factor * 0.22F);
            float volume = 0.8F + (float) factor * 3.4F;

            player.playNotifySound(SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, volume, bassPitch);
            player.playNotifySound(SoundEvents.DRAGON_FIREBALL_EXPLODE, SoundSource.HOSTILE, volume * 0.90F, crackPitch);
            player.playNotifySound(SoundEvents.WITHER_BREAK_BLOCK, SoundSource.HOSTILE, volume * 0.75F, 0.52F);
            player.playNotifySound(SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.HOSTILE, volume * 0.85F, Math.max(0.42F, 0.78F - (float) factor * 0.18F));

            if (distance <= ECHO_SOUND_RADIUS) {
                float echoVol = 0.25F + (float) (1.0D - distance / ECHO_SOUND_RADIUS) * 1.35F;
                player.playNotifySound(SoundEvents.AMBIENT_BASALT_DELTAS_MOOD.value(), SoundSource.HOSTILE, echoVol, 0.55F);
                player.playNotifySound(SoundEvents.WIND_CHARGE_BURST.value(), SoundSource.HOSTILE, echoVol * 0.75F, 0.45F);
            }
        }
    }

    private static void applyEarthquake(ServerLevel level, Vec3 center) {
        for (ServerPlayer player : level.players()) {
            if (player.isSpectator()) {
                continue;
            }

            double distance = player.position().distanceTo(center);
            if (distance > 96.0D) {
                continue;
            }

            double factor = 1.0D - (distance / 96.0D);
            Vec3 away = player.position().subtract(center);
            if (away.lengthSqr() < 1.0E-5D) {
                away = new Vec3(1.0D, 0.0D, 0.0D);
            } else {
                away = away.normalize();
            }

            double shake = 0.22D + factor * 0.55D;
            player.push(
                    away.x * (0.12D + factor * 0.22D),
                    0.10D + factor * 0.25D,
                    away.z * (0.12D + factor * 0.22D)
            );

            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 20 + (int) (factor * 20), 1, false, false, false));
            player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 20 + (int) (factor * 25), 0, false, false, false));

            level.sendParticles(ParticleTypes.CLOUD,
                    player.getX(), player.getY(), player.getZ(),
                    8, shake, 0.08D, shake, 0.01D);
        }
    }

    private static void finishEvent(ServerLevel level, MeteorEventSavedData data) {
        for (ServerPlayer player : level.players()) {
            if (!player.isSpectator()) {
                player.sendSystemMessage(Component.translatable("message.tutorialmod.meteor_impact",
                                activeEvent.strikePos.getX(), activeEvent.strikePos.getY(), activeEvent.strikePos.getZ())
                        .withStyle(ChatFormatting.GOLD));
                player.sendSystemMessage(Component.translatable("message.tutorialmod.meteor_core_hint",
                                activeEvent.strikePos.getX(), activeEvent.strikePos.getY(), activeEvent.strikePos.getZ())
                        .withStyle(ChatFormatting.AQUA));
            }
        }

        data.markCompleted(activeEvent.strikePos);
        activeEvent = null;
    }

    private static void makeCrater(ServerLevel level, BlockPos center, Vec3 meteorVelocity, int radius, boolean waterImpact) {
        Vec3 forward = new Vec3(meteorVelocity.x, 0.0D, meteorVelocity.z);
        if (forward.lengthSqr() < 1.0E-4D) {
            forward = new Vec3(1.0D, 0.0D, 0.0D);
        } else {
            forward = forward.normalize();
        }

        Vec3 side = new Vec3(-forward.z, 0.0D, forward.x).normalize();

        int max = radius + 5;

        for (int x = -max; x <= max; x++) {
            for (int z = -max; z <= max; z++) {
                double along = x * forward.x + z * forward.z;
                double across = x * side.x + z * side.z;

                double forwardRadius = radius * 1.45D;
                double sideRadius = radius * 0.92D;

                if (along < 0.0D) {
                    forwardRadius *= 0.78D;
                }

                double ellipse = (along * along) / (forwardRadius * forwardRadius)
                        + (across * across) / (sideRadius * sideRadius);

                if (ellipse > 1.45D) {
                    continue;
                }

                if (ellipse <= 1.0D) {
                    double normalized = Math.min(1.0D, ellipse);
                    int depth = 3 + Mth.floor(Math.pow(1.0D - normalized, 1.35D) * (radius * 1.15D));

                    if (along > 0.0D) {
                        depth += Mth.floor(along / Math.max(1.0D, radius * 0.35D));
                    }

                    int ceiling = along > radius * 0.25D ? 1 : 2;

                    for (int y = ceiling; y >= -depth; y--) {
                        BlockPos target = center.offset(x, y, z);
                        if (target.getY() <= level.getMinBuildHeight() + 2) {
                            continue;
                        }

                        BlockState state = level.getBlockState(target);
                        if (!state.isAir() && !state.is(Blocks.BEDROCK)) {
                            level.removeBlock(target, false);
                        }
                    }
                }

                if (ellipse >= 0.82D && ellipse <= 1.32D) {
                    BlockPos top = level.getHeightmapPos(Heightmap.Types.WORLD_SURFACE, center.offset(x, 0, z));
                    if (top.getY() <= level.getMinBuildHeight() + 2) {
                        continue;
                    }

                    int rimHeight = along > 0.0D ? 1 : 2;
                    Block rimBlock = waterImpact ? pickWaterScorchBlock(x, z) : pickScorchBlock(x, z);

                    for (int i = 0; i < rimHeight; i++) {
                        BlockPos rimPos = top.above(i);
                        if (!level.getBlockState(rimPos).is(Blocks.BEDROCK)) {
                            level.setBlockAndUpdate(rimPos, rimBlock.defaultBlockState());
                        }
                    }
                }
            }
        }
    }

    private static void placeMeteorRemnant(ServerLevel level, BlockPos impactPos, Vec3 meteorVelocity, boolean waterImpact) {
        Vec3 forward = new Vec3(meteorVelocity.x, 0.0D, meteorVelocity.z);
        if (forward.lengthSqr() < 1.0E-4D) {
            forward = new Vec3(1.0D, 0.0D, 0.0D);
        }
        forward = forward.normalize();
        Vec3 side = new Vec3(-forward.z, 0.0D, forward.x).normalize();

        BlockPos corePos = impactPos.below(4)
                .offset(-Mth.floor(forward.x * 4.0D), 0, -Mth.floor(forward.z * 4.0D));

        for (int x = -3; x <= 3; x++) {
            for (int y = -3; y <= 1; y++) {
                for (int z = -3; z <= 3; z++) {
                    double along = x * forward.x + z * forward.z;
                    double across = x * side.x + z * side.z;
                    double shape = (along * along) / 6.2D + (across * across) / 3.2D
                            + Math.pow(y + 1.35D, 2.0D) / 4.8D;
                    if (shape > 1.55D) {
                        continue;
                    }

                    if (along > 1.4D && y > 0) {
                        continue;
                    }

                    BlockPos target = corePos.offset(x, y, z);
                    if (x == 0 && y == -1 && z == 0) {
                        level.setBlockAndUpdate(target, ModBlocks.ALEXANDRITE_BLOCK.get().defaultBlockState());
                    } else {
                        level.setBlockAndUpdate(target, pickMeteorShellBlock(x, y, z).defaultBlockState());
                    }

                    BlockPos firePos = target.above();
                    if (!waterImpact && y >= 0 && level.getBlockState(firePos).isAir() && level.random.nextInt(2) == 0) {
                        level.setBlockAndUpdate(firePos, BaseFireBlock.getState(level, firePos));
                    }
                }
            }
        }
    }

    private static Block pickMeteorShellBlock(int x, int y, int z) {
        int selector = Math.floorMod(x * 31 + y * 17 + z * 13, 10);
        if (selector <= 1) {
            return Blocks.CRYING_OBSIDIAN;
        }
        if (selector <= 4) {
            return Blocks.MAGMA_BLOCK;
        }
        if (selector <= 6) {
            return Blocks.BASALT;
        }
        return Blocks.BLACKSTONE;
    }

    private static Block pickScorchBlock(int x, int z) {
        int selector = Math.floorMod(x * 19 + z * 23, 12);
        if (selector <= 1) {
            return Blocks.MAGMA_BLOCK;
        }
        if (selector <= 4) {
            return Blocks.BLACKSTONE;
        }
        if (selector <= 7) {
            return Blocks.BASALT;
        }
        if (selector <= 9) {
            return Blocks.NETHERRACK;
        }
        return Blocks.COARSE_DIRT;
    }

    private static Block pickWaterScorchBlock(int x, int z) {
        int selector = Math.floorMod(x * 13 + z * 17, 8);
        if (selector <= 1) {
            return Blocks.MAGMA_BLOCK;
        }
        if (selector <= 4) {
            return Blocks.BASALT;
        }
        return Blocks.BLACKSTONE;
    }

    private static void scorchImpactRing(ServerLevel level, BlockPos center, int innerRadius, int outerRadius, boolean waterImpact) {
        for (int x = -outerRadius; x <= outerRadius; x++) {
            for (int z = -outerRadius; z <= outerRadius; z++) {
                double distance = Math.sqrt(x * x + z * z);
                if (distance < innerRadius || distance > outerRadius) {
                    continue;
                }

                BlockPos top = level.getHeightmapPos(Heightmap.Types.WORLD_SURFACE, center.offset(x, 0, z));
                BlockPos ground = level.getBlockState(top).isAir() ? top.below() : top;
                if (ground.getY() <= level.getMinBuildHeight() + 2) {
                    continue;
                }

                if (level.getFluidState(ground).is(FluidTags.WATER)) {
                    continue;
                }

                Block scorchBlock = waterImpact ? pickWaterScorchBlock(x, z) : pickScorchBlock(x, z);
                level.setBlockAndUpdate(ground, scorchBlock.defaultBlockState());

                BlockPos firePos = ground.above();
                if (!waterImpact && distance < innerRadius + 2.5D && level.getBlockState(firePos).isAir()
                        && level.random.nextInt(4) != 0) {
                    level.setBlockAndUpdate(firePos, BaseFireBlock.getState(level, firePos));
                }
            }
        }
    }

    private static void igniteCrater(ServerLevel level, BlockPos center, int radius) {
        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                BlockPos top = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, center.offset(x, 0, z));
                BlockPos firePos = top.above();
                if (center.distSqr(top) <= (radius + 1) * (radius + 1)
                        && !level.getFluidState(top).is(FluidTags.WATER)
                        && level.getBlockState(firePos).isAir() && level.random.nextInt(4) != 0) {
                    level.setBlockAndUpdate(firePos, BaseFireBlock.getState(level, firePos));
                }
            }
        }
    }

    private static boolean isWaterImpact(ServerLevel level, BlockPos strikePos) {
        for (BlockPos target : BlockPos.betweenClosed(strikePos.offset(-2, -2, -2), strikePos.offset(2, 2, 2))) {
            if (level.getFluidState(target).is(FluidTags.WATER)) {
                return true;
            }
        }
        return false;
    }

    private static void handleWaterImpact(ServerLevel level, Vec3 impactCenter) {
        BlockPos impactPos = BlockPos.containing(impactCenter);
        for (BlockPos target : BlockPos.betweenClosed(impactPos.offset(-8, -4, -8), impactPos.offset(8, 4, 8))) {
            if (level.getFluidState(target).is(FluidTags.WATER)) {
                level.removeBlock(target, false);
            }
        }

        level.playSound(null, impactPos, SoundEvents.GENERIC_SPLASH, SoundSource.HOSTILE, 4.0F, 0.55F);
        level.playSound(null, impactPos, SoundEvents.LAVA_EXTINGUISH, SoundSource.HOSTILE, 3.2F, 0.5F);
        level.sendParticles(ParticleTypes.SPLASH, impactCenter.x, impactCenter.y + 0.5D, impactCenter.z,
                320, 5.5D, 1.6D, 5.5D, 0.18D);
        level.sendParticles(ParticleTypes.BUBBLE, impactCenter.x, impactCenter.y + 0.2D, impactCenter.z,
                220, 4.0D, 1.2D, 4.0D, 0.08D);
        level.sendParticles(ParticleTypes.CLOUD, impactCenter.x, impactCenter.y + 1.2D, impactCenter.z,
                180, 4.2D, 1.4D, 4.2D, 0.09D);
        level.sendParticles(ParticleTypes.LARGE_SMOKE, impactCenter.x, impactCenter.y + 1.5D, impactCenter.z,
                120, 3.0D, 1.2D, 3.0D, 0.06D);
    }

    private static void spawnImpactDebris(ServerLevel level, BlockPos center, boolean waterImpact) {
        int bursts = waterImpact ? 26 : 40;
        for (int i = 0; i < bursts; i++) {
            double angle = level.random.nextDouble() * Math.PI * 2.0D;
            double radius = 2.0D + level.random.nextDouble() * 9.0D;
            int x = center.getX() + Mth.floor(Math.cos(angle) * radius);
            int z = center.getZ() + Mth.floor(Math.sin(angle) * radius);
            BlockPos sourcePos = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, new BlockPos(x, center.getY(), z)).below();
            BlockState sourceState = level.getBlockState(sourcePos);
            if (sourceState.isAir() || sourceState.is(Blocks.BEDROCK)) {
                sourceState = (waterImpact ? Blocks.BASALT : Blocks.BLACKSTONE).defaultBlockState();
            }

            level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, sourceState),
                    sourcePos.getX() + 0.5D, sourcePos.getY() + 0.5D, sourcePos.getZ() + 0.5D,
                    18, 1.6D, 1.4D, 1.6D, 0.12D);
        }
    }

    private static void applyShockwave(ServerLevel level, Vec3 center, double radius) {
        AABB bounds = new AABB(center, center).inflate(radius);
        for (Entity entity : level.getEntitiesOfClass(Entity.class, bounds, Entity::isAlive)) {
            Vec3 offset = entity.position().subtract(center);
            double distance = Math.sqrt(offset.lengthSqr());
            if (distance < 0.1D || distance > radius) {
                continue;
            }

            double strength = 1.0D - distance / radius;
            Vec3 knockback = offset.normalize().scale(1.4D + strength * 1.8D)
                    .add(0.0D, 0.45D + strength * 0.75D, 0.0D);

            entity.push(knockback.x, knockback.y, knockback.z);
        }
    }

    private static StrikePlan createStrikePlan(ServerLevel level, ServerPlayer focalPlayer, List<ServerPlayer> players) {
        BlockPos strikePos = getCrosshairStrikePos(level, focalPlayer);
        BlockPos origin = focalPlayer.blockPosition();

        if (origin.distSqr(strikePos) < 16.0D) {
            Vec3 look = focalPlayer.getLookAngle();
            Vec3 horizontalLook = new Vec3(look.x, 0.0D, look.z);

            if (horizontalLook.lengthSqr() < 1.0E-4D) {
                horizontalLook = new Vec3(0.0D, 0.0D, 1.0D);
            } else {
                horizontalLook = horizontalLook.normalize();
            }

            int fallbackX = Mth.floor(origin.getX() + horizontalLook.x * 12.0D);
            int fallbackZ = Mth.floor(origin.getZ() + horizontalLook.z * 12.0D);
            int fallbackY = level.getHeight(Heightmap.Types.WORLD_SURFACE, fallbackX, fallbackZ);
            strikePos = new BlockPos(fallbackX, fallbackY, fallbackZ);
        }

        return buildStrikePlan(level, origin, strikePos);
    }

    private static BlockPos getCrosshairStrikePos(ServerLevel level, ServerPlayer player) {
        Vec3 eyePos = player.getEyePosition();
        Vec3 look = player.getLookAngle();

        double maxDistance = 256.0D;
        Vec3 end = eyePos.add(look.scale(maxDistance));

        ClipContext clipContext = new ClipContext(
                eyePos,
                end,
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE,
                player
        );

        BlockHitResult hitResult = level.clip(clipContext);

        if (hitResult.getType() == HitResult.Type.BLOCK) {
            BlockPos hitPos = hitResult.getBlockPos();
            int surfaceY = level.getHeight(Heightmap.Types.WORLD_SURFACE, hitPos.getX(), hitPos.getZ());
            return new BlockPos(hitPos.getX(), surfaceY, hitPos.getZ());
        }

        Vec3 horizontalLook = new Vec3(look.x, 0.0D, look.z);
        if (horizontalLook.lengthSqr() < 1.0E-4D) {
            horizontalLook = new Vec3(0.0D, 0.0D, 1.0D);
        } else {
            horizontalLook = horizontalLook.normalize();
        }

        int fallbackDistance = 48;
        int strikeX = Mth.floor(player.getX() + horizontalLook.x * fallbackDistance);
        int strikeZ = Mth.floor(player.getZ() + horizontalLook.z * fallbackDistance);
        int strikeY = level.getHeight(Heightmap.Types.WORLD_SURFACE, strikeX, strikeZ);

        return new BlockPos(strikeX, strikeY, strikeZ);
    }

    private static StrikePlan buildStrikePlan(ServerLevel level, BlockPos origin, BlockPos strikePos) {
        Vec3 impactCenter = Vec3.atCenterOf(strikePos);

        Vec3 approach = impactCenter.subtract(Vec3.atCenterOf(origin)).multiply(1.0D, 0.0D, 1.0D);
        if (approach.lengthSqr() < 1.0E-4D) {
            approach = new Vec3(1.0D, 0.0D, 0.0D);
        } else {
            approach = approach.normalize();
        }

        Vec3 side = new Vec3(-approach.z, 0.0D, approach.x).normalize();

        double angleDegrees = 30.0D;
        double spawnY = Math.min(level.getMaxBuildHeight() - 10.0D, impactCenter.y + 70.0D);
        double verticalDrop = spawnY - impactCenter.y;
        double horizontalDistance = verticalDrop / Math.tan(Math.toRadians(angleDegrees));

        Vec3 meteorSpawnPos = impactCenter.add(approach.scale(horizontalDistance)).subtract(side.scale(8.0D));
        meteorSpawnPos = new Vec3(meteorSpawnPos.x, spawnY, meteorSpawnPos.z);

        Vec3 toImpact = impactCenter.add(0.0D, 1.0D, 0.0D).subtract(meteorSpawnPos);
        int flightTicks = 34;
        Vec3 meteorVelocity = toImpact.scale(1.0D / flightTicks);

        Vec3 flightHorizontal = impactCenter.subtract(meteorSpawnPos).multiply(1.0D, 0.0D, 1.0D);
        if (flightHorizontal.lengthSqr() < 1.0E-4D) {
            flightHorizontal = approach.scale(-1.0D);
        } else {
            flightHorizontal = flightHorizontal.normalize();
        }

        Vec3 cameraSide = new Vec3(-flightHorizontal.z, 0.0D, flightHorizontal.x).normalize();
        Vec3 viewPos = impactCenter.subtract(flightHorizontal.scale(70.0D))
                .add(cameraSide.scale(28.0D))
                .add(0.0D, 24.0D, 0.0D);

        return new StrikePlan(strikePos, viewPos, meteorSpawnPos, meteorVelocity, cameraSide, flightHorizontal);
    }

    private static void scorchFlightPath(ServerLevel level, Vec3 impactCenter, Vec3 meteorVelocity) {
        Vec3 forward = new Vec3(meteorVelocity.x, meteorVelocity.y, meteorVelocity.z).normalize().scale(-1.0D);

        for (int i = 6; i <= 26; i += 2) {
            Vec3 sample = impactCenter.add(forward.scale(i));
            BlockPos pos = level.getHeightmapPos(Heightmap.Types.WORLD_SURFACE,
                    BlockPos.containing(sample.x, sample.y, sample.z));

            for (BlockPos target : BlockPos.betweenClosed(pos.offset(-1, 0, -1), pos.offset(1, 1, 1))) {
                BlockState state = level.getBlockState(target);

                if (state.isAir() || state.is(Blocks.BEDROCK) || level.getFluidState(target).is(FluidTags.WATER)) {
                    continue;
                }

                if (level.random.nextInt(4) == 0) {
                    level.setBlockAndUpdate(target, Blocks.BLACKSTONE.defaultBlockState());
                } else if (level.random.nextInt(6) == 0) {
                    level.setBlockAndUpdate(target, Blocks.MAGMA_BLOCK.defaultBlockState());
                }

                BlockPos firePos = target.above();
                if (level.getBlockState(firePos).isAir() && level.random.nextInt(3) == 0) {
                    level.setBlockAndUpdate(firePos, BaseFireBlock.getState(level, firePos));
                }
            }

            level.sendParticles(ParticleTypes.SMOKE, sample.x, sample.y, sample.z,
                    12, 0.65D, 0.25D, 0.65D, 0.02D);
            level.sendParticles(ParticleTypes.ASH, sample.x, sample.y, sample.z,
                    10, 0.48D, 0.18D, 0.48D, 0.015D);
            level.sendParticles(ParticleTypes.FLAME, sample.x, sample.y, sample.z,
                    8, 0.38D, 0.18D, 0.38D, 0.01D);
        }
    }

    private static MeteorEntity getMeteor(ServerLevel level, UUID meteorId) {
        if (meteorId == null) {
            return null;
        }

        return level.getEntity(meteorId) instanceof MeteorEntity meteor ? meteor : null;
    }

    private static void sendFlashToPlayers(ServerLevel level, int durationTicks) {
        for (ServerPlayer player : level.players()) {
            if (!player.isSpectator()) {
                ModMessages.sendToPlayer(new MeteorFlashS2CPacket(durationTicks), player);
            }
        }
    }

    private static void spawnGroundShockwave(ServerLevel level, BlockPos center, boolean waterImpact) {
        for (int ring = 2; ring <= 14; ring += 2) {
            int points = ring * 14;
            for (int i = 0; i < points; i++) {
                double angle = (Math.PI * 2.0D * i) / points;
                double dx = Math.cos(angle) * ring;
                double dz = Math.sin(angle) * ring;

                BlockPos wavePos = level.getHeightmapPos(Heightmap.Types.WORLD_SURFACE,
                        center.offset(Mth.floor(dx), 0, Mth.floor(dz)));

                double px = wavePos.getX() + 0.5D;
                double py = wavePos.getY() + 0.15D;
                double pz = wavePos.getZ() + 0.5D;

                level.sendParticles(ParticleTypes.CLOUD, px, py, pz,
                        3, 0.18D, 0.05D, 0.18D, 0.02D);

                level.sendParticles(ParticleTypes.ASH, px, py, pz,
                        2, 0.08D, 0.03D, 0.08D, 0.01D);

                if (!waterImpact) {
                    level.sendParticles(ParticleTypes.FLAME, px, py, pz,
                            1, 0.05D, 0.02D, 0.05D, 0.01D);
                }
            }
        }
    }

    private record StrikePlan(BlockPos strikePos, Vec3 viewPos, Vec3 meteorSpawnPos, Vec3 meteorVelocity,
                              Vec3 sideVector, Vec3 approachVector) {
    }

    private static final class ActiveMeteorEvent {
        private final BlockPos strikePos;
        private final Vec3 viewPos;
        private final Vec3 meteorSpawnPos;
        private final Vec3 meteorVelocity;
        private final Vec3 impactCenter;
        private final Vec3 sideVector;
        private final Vec3 approachVector;
        private UUID meteorId;
        private int tick;
        private boolean impactDone;

        private ActiveMeteorEvent(BlockPos strikePos, Vec3 viewPos, Vec3 meteorSpawnPos, Vec3 meteorVelocity,
                                  Vec3 impactCenter, Vec3 sideVector, Vec3 approachVector) {
            this.strikePos = strikePos;
            this.viewPos = viewPos;
            this.meteorSpawnPos = meteorSpawnPos;
            this.meteorVelocity = meteorVelocity;
            this.impactCenter = impactCenter;
            this.sideVector = sideVector;
            this.approachVector = approachVector;
        }
    }
}