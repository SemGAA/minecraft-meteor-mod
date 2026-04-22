package net.kaupenjoe.tutorialmod.util;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public final class MathUtil {
    private MathUtil() {
    }

    public static float[] getYawPitch(Vec3 from, Vec3 to) {
        Vec3 direction = to.subtract(from);
        double horizontal = Math.sqrt(direction.x * direction.x + direction.z * direction.z);
        float yaw = (float) (Mth.atan2(direction.z, direction.x) * (180.0F / Math.PI)) - 90.0F;
        float pitch = (float) (-(Mth.atan2(direction.y, horizontal) * (180.0F / Math.PI)));
        return new float[]{yaw, pitch};
    }

    public static double easeInOutCubic(double value) {
        double clamped = Mth.clamp(value, 0.0D, 1.0D);
        return clamped < 0.5D
                ? 4.0D * clamped * clamped * clamped
                : 1.0D - Math.pow(-2.0D * clamped + 2.0D, 3.0D) / 2.0D;
    }
}
