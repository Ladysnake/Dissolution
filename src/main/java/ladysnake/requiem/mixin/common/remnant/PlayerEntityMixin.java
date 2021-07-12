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
package ladysnake.requiem.mixin.common.remnant;

import ladysnake.requiem.api.v1.RequiemPlayer;
import ladysnake.requiem.api.v1.entity.MovementAlterer;
import ladysnake.requiem.api.v1.remnant.RemnantComponent;
import ladysnake.requiem.common.entity.RequiemEntityAttributes;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.player.PlayerAbilities;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Slice;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerEntity.class)
public abstract class PlayerEntityMixin extends LivingEntity implements RequiemPlayer {

    /* Implementation of RequiemPlayer */

    @Shadow @Final private PlayerAbilities abilities;
    private static final EntityDimensions REQUIEM$SOUL_SNEAKING_SIZE = EntityDimensions.changing(0.6f, 0.6f);

    @Inject(method = "createPlayerAttributes", at = @At("RETURN"))
    private static void addSoulOffenseAttribute(CallbackInfoReturnable<DefaultAttributeContainer.Builder> cir) {
        cir.getReturnValue().add(RequiemEntityAttributes.SOUL_OFFENSE);
    }

    /* Actual modifications of vanilla behaviour */

    @Inject(method = "isSwimming", at = @At("HEAD"), cancellable = true)
    private void flyLikeSuperman(CallbackInfoReturnable<Boolean> cir) {
        if (this.abilities.flying && this.isSprinting() && RemnantComponent.KEY.get(this).isIncorporeal()) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "travel",
        slice = @Slice(
            from = @At(value = "FIELD", target = "Lnet/minecraft/entity/player/PlayerEntity;flyingSpeed:F", ordinal = 0),
            to = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/PlayerEntity;setFlag(IZ)V")
        ),
        at = @At(
            value = "FIELD",
            opcode = Opcodes.PUTFIELD,
            target = "Lnet/minecraft/entity/player/PlayerEntity;flyingSpeed:F",
            ordinal = 0,
            shift = At.Shift.AFTER
        )
    )
    private void slowGhosts(Vec3d movementInput, CallbackInfo ci) {
        if (MovementAlterer.KEY.get(this).isNoClipping()) {
            this.flyingSpeed *= 0.1;
        }
    }

    @Inject(method = "travel", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/PlayerEntity;getRotationVector()Lnet/minecraft/util/math/Vec3d;"))
    private void flySwimVertically(Vec3d motion, CallbackInfo ci) {
        double yMotion = this.getRotationVector().y;
        double modifier = yMotion < -0.2D ? 0.085D : 0.06D;
        // If the motion change would not be applied, apply it ourselves
        if (yMotion > 0.0D && !this.jumping && this.world.getBlockState(new BlockPos(this.getX(), this.getY() + 1.0D - 0.1D, this.getZ())).getFluidState().isEmpty() && RemnantComponent.KEY.get(this).isIncorporeal()) {
            Vec3d velocity = this.getVelocity();
            this.setVelocity(velocity.add(0.0D, (yMotion - velocity.y) * modifier, 0.0D));
        }
    }

    /**
     * Players' sizes are hardcoded in an immutable enum map.
     * This injection makes souls smaller when sneaking.
     */
    @Inject(method = "getDimensions", at = @At("HEAD"), cancellable = true)
    private void adjustSize(EntityPose pose, CallbackInfoReturnable<EntityDimensions> cir) {
        if (RemnantComponent.KEY.get(this).isIncorporeal() && pose == EntityPose.CROUCHING) {
            cir.setReturnValue(REQUIEM$SOUL_SNEAKING_SIZE);
        }
    }

    // 1.27 is the sneaking eye height
    @Inject(method = "getActiveEyeHeight", at = @At(value = "CONSTANT", args = "floatValue=1.27"), cancellable = true)
    private void adjustSoulSneakingEyeHeight(EntityPose pose, EntityDimensions size, CallbackInfoReturnable<Float> cir) {
        if (RemnantComponent.KEY.get(this).isIncorporeal()) {
            cir.setReturnValue(0.4f);
        }
    }

    protected PlayerEntityMixin(EntityType<? extends PlayerEntity> type, World world) {
        super(type, world);
    }

}
