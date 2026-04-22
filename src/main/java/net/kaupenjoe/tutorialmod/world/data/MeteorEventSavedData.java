package net.kaupenjoe.tutorialmod.world.data;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

public class MeteorEventSavedData extends SavedData {
    private static final String DATA_NAME = "tutorialmod_meteor_event";
    private static final Factory<MeteorEventSavedData> FACTORY =
            new Factory<>(MeteorEventSavedData::new, MeteorEventSavedData::load, null);

    private boolean scheduled;
    private boolean completed;
    private long scheduledTick;
    private BlockPos lastImpactPos = BlockPos.ZERO;

    public static MeteorEventSavedData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(FACTORY, DATA_NAME);
    }

    private static MeteorEventSavedData load(CompoundTag tag, HolderLookup.Provider provider) {
        MeteorEventSavedData data = new MeteorEventSavedData();
        data.scheduled = tag.getBoolean("Scheduled");
        data.completed = tag.getBoolean("Completed");
        data.scheduledTick = tag.getLong("ScheduledTick");
        if (tag.contains("LastImpactPos")) {
            data.lastImpactPos = BlockPos.of(tag.getLong("LastImpactPos"));
        }
        return data;
    }

    public boolean isScheduled() {
        return scheduled;
    }

    public boolean isCompleted() {
        return completed;
    }

    public void schedule(long targetTick) {
        scheduled = true;
        scheduledTick = targetTick;
        setDirty();
    }

    public boolean shouldStart(long currentTick) {
        return scheduled && !completed && currentTick >= scheduledTick;
    }

    public void markCompleted(BlockPos impactPos) {
        scheduled = false;
        completed = true;
        lastImpactPos = impactPos;
        setDirty();
    }

    public BlockPos getLastImpactPos() {
        return lastImpactPos;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
        tag.putBoolean("Scheduled", scheduled);
        tag.putBoolean("Completed", completed);
        tag.putLong("ScheduledTick", scheduledTick);
        tag.putLong("LastImpactPos", lastImpactPos.asLong());
        return tag;
    }
}
