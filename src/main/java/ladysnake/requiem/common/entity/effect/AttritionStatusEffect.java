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
package ladysnake.requiem.common.entity.effect;

import com.google.common.base.Preconditions;
import ladysnake.requiem.api.v1.internal.StatusEffectReapplicator;
import ladysnake.requiem.api.v1.possession.Possessable;
import ladysnake.requiem.api.v1.remnant.RemnantComponent;
import ladysnake.requiem.api.v1.remnant.StickyStatusEffect;
import ladysnake.requiem.common.remnant.RemnantTypes;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffectType;
import net.minecraft.entity.player.PlayerEntity;

import javax.annotation.Nonnegative;

public class AttritionStatusEffect extends StatusEffect implements StickyStatusEffect {
    public static final DamageSource ATTRITION_HARDCORE_DEATH = new DamageSource("requiem.attrition.hardcore") {{
        // We need this dirty anonymous initializer because everything is protected
        this.setBypassesArmor();
        this.setOutOfWorld();
    }};
    public static final int MAX_LEVEL = 3;
    public static final int DEFAULT_DURATION = 300;

    public static void apply(PlayerEntity target) {
        apply(target, target.world.getLevelProperties().isHardcore() ? 2 : 1);
    }

    public static void apply(LivingEntity target, @Nonnegative int amount) {
        apply(target, amount, DEFAULT_DURATION);
    }

    public static void apply(LivingEntity target, @Nonnegative int amount, int minDuration) {
        Preconditions.checkArgument(amount > 0);

        StatusEffectInstance attrition = target.getStatusEffect(RequiemStatusEffects.ATTRITION);
        int expectedAmplifier = attrition == null ? amount - 1 : attrition.getAmplifier() + amount;
        int amplifier = Math.min(MAX_LEVEL, expectedAmplifier);
        int duration = Math.max(attrition == null ? 0 : attrition.getDuration(), minDuration);
        addAttrition(target, amplifier, duration);

        if (expectedAmplifier > MAX_LEVEL && (!(target instanceof PlayerEntity) || target.world.getLevelProperties().isHardcore())) {
            if (target instanceof PlayerEntity) {
                RemnantComponent.get((PlayerEntity) target).become(RemnantTypes.MORTAL);
            }
            target.damage(ATTRITION_HARDCORE_DEATH, Float.MAX_VALUE);
        }
    }

    public static void addAttrition(LivingEntity target, int amplifier, int duration) {
        target.addStatusEffect(new StatusEffectInstance(
            RequiemStatusEffects.ATTRITION,
            duration,
            amplifier,
            false,
            false,
            true
        ));
    }

    public static void reduce(LivingEntity target, @Nonnegative int amount) {
        Preconditions.checkArgument(amount > 0);

        StatusEffectInstance attrition = target.getStatusEffect(RequiemStatusEffects.ATTRITION);
        if (attrition == null) return;

        int amplifier = attrition.getAmplifier() - amount;
        target.removeStatusEffect(RequiemStatusEffects.ATTRITION);
        StatusEffectReapplicator.KEY.maybeGet(target)
            .or(() -> StatusEffectReapplicator.KEY.maybeGet(((Possessable)target).getPossessor()))
            .ifPresent(r -> r.definitivelyClear(RequiemStatusEffects.ATTRITION));

        if (amplifier >= 0) {
            addAttrition(target, amplifier, DEFAULT_DURATION);
        }
    }

    public AttritionStatusEffect(StatusEffectType type, int color) {
        super(type, color);
    }

    @Override
    public double adjustModifierAmount(int amplifier, EntityAttributeModifier entityAttributeModifier) {
        return super.adjustModifierAmount(Math.min(amplifier, MAX_LEVEL), entityAttributeModifier);
    }

    @Override
    public boolean shouldStick(LivingEntity entity) {
        if (RemnantComponent.isVagrant(entity)) return true;
        PlayerEntity possessor = ((Possessable)entity).getPossessor();
        if (possessor == null) return false;
        RemnantComponent remnantComponent = RemnantComponent.get(possessor);
        return remnantComponent.isVagrant() || remnantComponent.getRemnantType() == RemnantTypes.WANDERING_SPIRIT;
    }
}
