package net.kaupenjoe.tutorialmod.item.custom;

import net.kaupenjoe.tutorialmod.world.event.MeteorWorldEventHandler;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;

public class MeteorStaffItem extends Item {
    public MeteorStaffItem(Properties properties) {
        super(properties);
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.BOW;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand usedHand) {
        ItemStack stack = player.getItemInHand(usedHand);

        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer && level instanceof ServerLevel serverLevel) {
            boolean started = MeteorWorldEventHandler.summonMeteorForPlayer(serverLevel, serverPlayer);
            if (started) {
                level.playSound(null, player.blockPosition(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.2F, 0.65F);
                level.playSound(null, player.blockPosition(), SoundEvents.TRIDENT_THUNDER.value(), SoundSource.PLAYERS, 0.9F, 1.35F);

                if (!player.getAbilities().instabuild) {
                    stack.hurtAndBreak(1, serverPlayer, player.getEquipmentSlotForItem(stack));
                }
            }
        }

        player.getCooldowns().addCooldown(this, 20 * 8);
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }
}