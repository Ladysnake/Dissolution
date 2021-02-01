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
package ladysnake.requiem.mixin.common.possession.possessor;

import com.mojang.authlib.GameProfile;
import dev.onyxstudios.cca.api.v3.component.ComponentProvider;
import ladysnake.requiem.api.v1.entity.MovementAlterer;
import ladysnake.requiem.api.v1.possession.Possessable;
import ladysnake.requiem.api.v1.possession.PossessionComponent;
import ladysnake.requiem.api.v1.remnant.RemnantComponent;
import ladysnake.requiem.common.entity.attribute.NonDeterministicAttribute;
import ladysnake.requiem.common.entity.attribute.PossessionDelegatingModifier;
import ladysnake.requiem.common.entity.internal.VariableMobilityEntity;
import ladysnake.requiem.common.tag.RequiemItemTags;
import ladysnake.requiem.mixin.common.access.LivingEntityAccessor;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.mob.DrownedEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.ZombieEntity;
import net.minecraft.entity.player.HungerManager;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.fluid.Fluid;
import net.minecraft.item.FoodComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Slice;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.EnumSet;

@Mixin(PlayerEntity.class)
public abstract class PossessorPlayerEntityMixin extends PossessorLivingEntityMixin {

    @Shadow
    public abstract HungerManager getHungerManager();

    @Inject(method = "<init>", at = @At("RETURN"))
    private void initAttributes(World world, BlockPos pos, float yaw, GameProfile profile, CallbackInfo ci) {
        // Replace every registered attribute
        for (EntityAttribute attribute : Registry.ATTRIBUTE) {
            // Note: this fills the attribute map for the player, whether this is an issue is to be determined
            EntityAttributeInstance current = this.requiem$getAttributeInstance(attribute);
            if (current != null) {
                ((NonDeterministicAttribute) current).addFinalModifier(new PossessionDelegatingModifier(current.getAttribute(), PossessionComponent.KEY.get(this)));
            }
        }
    }

    @Inject(method = "travel", at = @At("HEAD"), cancellable = true)
    private void travel(CallbackInfo info) {
        @SuppressWarnings("ConstantConditions") Entity possessed = PossessionComponent.getPossessedEntity((Entity) (Object) this);
        if (possessed != null && ((VariableMobilityEntity) possessed).requiem_isImmovable()) {
            if (!this.requiem$getWorld().isClient && (this.requiem$getX() != possessed.getX() || this.requiem$getY() != possessed.getY() || this.requiem$getZ() != possessed.getZ())) {
                ServerPlayNetworkHandler networkHandler = ((ServerPlayerEntity) (Object) this).networkHandler;
                networkHandler.teleportRequest(possessed.getX(), possessed.getY(), possessed.getZ(), this.requiem$getYaw(), this.requiem$getPitch(), EnumSet.allOf(PlayerPositionLookS2CPacket.Flag.class));
                networkHandler.syncWithPlayerPosition();
            }
            info.cancel();
        }
    }

    /**
     * Players' sizes are hardcoded in an immutable enum map.
     * This injection delegates the call to the possessed entity, if any.
     */
    @Inject(method = "getDimensions", at = @At("HEAD"), cancellable = true)
    private void adjustSize(EntityPose pose, CallbackInfoReturnable<EntityDimensions> cir) {
        Entity possessedEntity = PossessionComponent.KEY.get(this).getPossessedEntity();
        if (possessedEntity != null) {
            cir.setReturnValue(possessedEntity.getDimensions(pose));
        }
    }

    @Inject(method = "canConsume", at = @At("RETURN"), cancellable = true)
    private void canSoulConsume(boolean ignoreHunger, CallbackInfoReturnable<Boolean> cir) {
        Possessable possessed = (Possessable) PossessionComponent.KEY.get(this).getPossessedEntity();
        if (possessed != null) {
            cir.setReturnValue(ignoreHunger || possessed.isRegularEater() && this.getHungerManager().isNotFull());
        }
    }

    @Inject(method = "canFoodHeal", at = @At("RETURN"), cancellable = true)
    private void canFoodHealPossessed(CallbackInfoReturnable<Boolean> cir) {
        LivingEntity possessed = PossessionComponent.KEY.get(this).getPossessedEntity();
        if (possessed != null) {
            cir.setReturnValue(((Possessable) possessed).isRegularEater() && possessed.getHealth() > 0 && possessed.getHealth() < possessed.getMaxHealth());
        }
    }

    @Inject(method = "addExhaustion", slice = @Slice(to = @At("INVOKE:FIRST")), at = @At(value = "RETURN"))
    private void addExhaustion(float exhaustion, CallbackInfo ci) {
        Possessable possessed = (Possessable) PossessionComponent.KEY.get(this).getPossessedEntity();
        if (possessed != null && possessed.isRegularEater()) {
            if (!this.requiem$getWorld().isClient) {
                this.getHungerManager().addExhaustion(exhaustion);
            }
        }
    }

    @Inject(method = "eatFood", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/HungerManager;eat(Lnet/minecraft/item/Item;Lnet/minecraft/item/ItemStack;)V"))
    private void eatZombieFood(World world, ItemStack stack, CallbackInfoReturnable<ItemStack> cir) {
        PossessionComponent possessionComponent = PossessionComponent.KEY.get(this);
        MobEntity possessedEntity = possessionComponent.getPossessedEntity();
        if (possessedEntity instanceof ZombieEntity && stack.getItem().isFood()) {
            if (RequiemItemTags.RAW_MEATS.contains(stack.getItem()) || RequiemItemTags.RAW_FISHES.contains(stack.getItem()) && possessedEntity instanceof DrownedEntity) {
                FoodComponent food = stack.getItem().getFoodComponent();
                assert food != null;
                player.addStatusEffect(new StatusEffectInstance(
                    StatusEffects.HUNGER,
                    600,
                    0,
                    false,
                    false,
                    true
                ));
                possessedEntity.heal(food.getHunger());
            }
        }
        if (possessedEntity != null && possessionComponent.canBeCured(stack)) {
            possessionComponent.startCuring();
        }
    }

    @Override
    protected void requiem$delegateBreath(CallbackInfoReturnable<Integer> cir) {
        @SuppressWarnings("ConstantConditions") Entity self = (Entity) (Object) this;
        // This method can be called in the constructor
        if (ComponentProvider.fromEntity(self).getComponentContainer() != null) {
            Entity possessedEntity = PossessionComponent.getPossessedEntity(self);
            if (possessedEntity != null) {
                cir.setReturnValue(possessedEntity.getAir());
            }
        }
    }

    @Override
    protected void requiem$delegateMaxBreath(CallbackInfoReturnable<Integer> cir) {
        // This method can be called in the constructor, before CCA is initialized
        if (((ComponentProvider) this).getComponentContainer() != null) {
            @SuppressWarnings("ConstantConditions") Entity possessedEntity = PossessionComponent.getPossessedEntity((Entity) (Object) this);
            if (possessedEntity != null) {
                cir.setReturnValue(possessedEntity.getMaxAir());
            }
        }
    }

    @Override
    protected void requiem$setSprinting(boolean sprinting, CallbackInfo ci) {
        MobEntity possessedEntity = PossessionComponent.KEY.get(this).getPossessedEntity();
        if (possessedEntity != null) {
            possessedEntity.setSprinting(sprinting);
        }
    }

    @Override
    protected void requiem$canClimb(CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValueZ() && this.requiem$isCollidingHorizontally()) {
            cir.setReturnValue(MovementAlterer.KEY.get(this).canClimbWalls());
        }
    }

    @Override
    protected void requiem$soulsAvoidTraps(CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValueZ()) {
            if (RemnantComponent.KEY.get(this).isIncorporeal()) {
                cir.setReturnValue(true);
            } else {
                MobEntity possessedEntity = PossessionComponent.KEY.get(this).getPossessedEntity();

                if (possessedEntity != null && possessedEntity.canAvoidTraps()) {
                    cir.setReturnValue(true);
                }
            }
        }
    }

    @Override
    protected void requiem$isOnFire(CallbackInfoReturnable<Boolean> cir) {
        MobEntity possessedEntity = PossessionComponent.KEY.get(this).getPossessedEntity();
        if (possessedEntity != null) {
            cir.setReturnValue(possessedEntity.isOnFire());
        } else if (RemnantComponent.KEY.get(this).isIncorporeal()) {
            // Also prevent incorporeal players from burning
            cir.setReturnValue(false);
        }
    }

    @Override
    protected void requiem$canWalkOnFluid(Fluid fluid, CallbackInfoReturnable<Boolean> cir) {
        MobEntity possessedEntity = PossessionComponent.KEY.get(this).getPossessedEntity();
        if (possessedEntity != null) {
            cir.setReturnValue(possessedEntity.canWalkOnFluid(fluid));
        }
    }

    @Inject(method = "getActiveEyeHeight", at = @At("HEAD"), cancellable = true)
    private void adjustEyeHeight(EntityPose pose, EntityDimensions size, CallbackInfoReturnable<Float> cir) {
        // This method can be called in the Entity constructor, before CCA is initialized
        if (((ComponentProvider)this).getComponentContainer() != null) {
            @SuppressWarnings("ConstantConditions") LivingEntity possessed = PossessionComponent.getPossessedEntity((Entity) (Object) this);
            if (possessed != null) {
                cir.setReturnValue(((LivingEntityAccessor) possessed).invokeGetEyeHeight(pose, possessed.getDimensions(pose)));
            }
        }
    }
}
