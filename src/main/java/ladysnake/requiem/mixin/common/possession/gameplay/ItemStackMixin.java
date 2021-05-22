/*
 * Requiem
 * Copyright (C) 2017-2021 Ladysnake
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses>.
 *
 * Linking this mod statically or dynamically with other
 * modules is making a combined work based on this mod.
 * Thus, the terms and conditions of the GNU General Public License cover the whole combination.
 *
 * In addition, as a special exception, the copyright holders of
 * this mod give you permission to combine this mod
 * with free software programs or libraries that are released under the GNU LGPL
 * and with code included in the standard release of Minecraft under All Rights Reserved (or
 * modified versions of such code, with unchanged license).
 * You may copy and distribute such a system following the terms of the GNU GPL for this mod
 * and the licenses of the other code concerned.
 *
 * Note that people who make modified versions of this mod are not obligated to grant
 * this special exception for their modified versions; it is their choice whether to do so.
 * The GNU General Public License gives permission to release a modified version without this exception;
 * this exception also makes it possible to release a modified version which carries forward this exception.
 */
package ladysnake.requiem.mixin.common.possession.gameplay;

import ladysnake.requiem.api.v1.possession.Possessable;
import ladysnake.requiem.api.v1.possession.PossessionComponent;
import ladysnake.requiem.common.impl.possession.item.PossessionItemOverride;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemStack.class)
public abstract class ItemStackMixin {
    @Shadow
    public abstract void decrement(int amount);

    private @Nullable PossessionItemOverride requiem$currentOverride = null;

    @Inject(method = "use", at = @At("HEAD"), cancellable = true)
    private void use(World world, PlayerEntity player, Hand hand, CallbackInfoReturnable<TypedActionResult<ItemStack>> cir) {
        PossessionComponent possessionComponent = PossessionComponent.get(player);
        MobEntity possessedEntity = possessionComponent.getPossessedEntity();
        this.requiem$currentOverride = null;

        if (possessedEntity != null) {
            ItemStack heldStack = (ItemStack) (Object) this;

            PossessionItemOverride.findOverride(world, player, possessedEntity, heldStack)
                .ifPresent(override -> {
                    if (override.getUseTime() <= 0) {
                        TypedActionResult<ItemStack> res = override.runAction(player, possessedEntity, heldStack, world, hand);
                        if (res.getResult() != ActionResult.PASS) cir.setReturnValue(res);
                    } else {
                        this.requiem$currentOverride = override;
                        player.setCurrentHand(hand);
                        cir.setReturnValue(TypedActionResult.success(heldStack));
                    }
                });
        }
    }

    @Inject(method = "getMaxUseTime", at = @At("HEAD"), cancellable = true)
    private void overrideMaxUseTime(CallbackInfoReturnable<Integer> cir) {
        if (this.requiem$currentOverride != null) cir.setReturnValue(this.requiem$currentOverride.getUseTime());
    }

    @Inject(method = "finishUsing", at = @At("HEAD"), cancellable = true)
    private void overridePossessedUseEnd(World world, LivingEntity user, CallbackInfoReturnable<ItemStack> cir) {
        if (!(user instanceof PlayerEntity)) return;

        MobEntity possessedEntity = PossessionComponent.getPossessedEntity(user);

        if (possessedEntity != null) {
            ItemStack heldStack = (ItemStack) (Object) this;

            PossessionItemOverride.findOverride(world, (PlayerEntity) user, possessedEntity, heldStack)
                .ifPresent(override -> {
                    TypedActionResult<ItemStack> res = override.runAction((PlayerEntity) user, possessedEntity, heldStack, world, user.getActiveHand());
                    if (res.getResult() != ActionResult.PASS) cir.setReturnValue(res.getValue());
                });
        }
    }

    @Inject(method = "useOnEntity", at = @At("RETURN"), cancellable = true)
    private void useOnEntity(PlayerEntity user, LivingEntity entity, Hand hand, CallbackInfoReturnable<ActionResult> cir) {
        PlayerEntity possessor = ((Possessable) entity).getPossessor();
        if (possessor != null && PossessionComponent.get(possessor).canBeCured(((ItemStack) (Object) this))) {
            if (!user.abilities.creativeMode) {
                this.decrement(1);
            }

            PossessionComponent.KEY.get(possessor).startCuring();
            cir.setReturnValue(ActionResult.SUCCESS);
        }
    }
}
