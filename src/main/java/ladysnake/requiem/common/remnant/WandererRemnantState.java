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
package ladysnake.requiem.common.remnant;

import ladysnake.requiem.api.v1.possession.PossessionComponent;
import ladysnake.requiem.api.v1.remnant.AttritionFocus;
import ladysnake.requiem.api.v1.remnant.RemnantState;
import ladysnake.requiem.common.advancement.criterion.RequiemCriteria;
import ladysnake.requiem.common.entity.effect.AttritionStatusEffect;
import ladysnake.requiem.common.entity.effect.RequiemStatusEffects;
import ladysnake.requiem.common.particle.RequiemParticleTypes;
import ladysnake.requiem.core.remnant.MutableRemnantState;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;

public class WandererRemnantState extends MutableRemnantState {
    public static final int ATTRITION_MEND_PROBABILITY = 4000;

    public WandererRemnantState(PlayerEntity player) {
        super(player);
    }

    @Override
    public void setup(RemnantState oldHandler) {
        this.setVagrant(true);
    }

    @Override
    public boolean canDissociateFrom(MobEntity possessed) {
        return true;
    }

    @Override
    public void curePossessed(LivingEntity body) {
        super.curePossessed(body);
        AttritionStatusEffect.reduce(this.player, Integer.MAX_VALUE);
    }

    @Override
    protected MobEntity cureMob(LivingEntity body) {
        MobEntity cured = super.cureMob(body);
        if (cured != null) {
            RequiemCriteria.TRANSFORMED_POSSESSED_ENTITY.handle((ServerPlayerEntity) this.player, body, cured, true);
        }
        return cured;
    }

    @Override
    public boolean canRegenerateBody() {
        return false;
    }

    @Override
    public void serverTick() {
        ServerPlayerEntity player = (ServerPlayerEntity) this.player;
        LivingEntity body = this.isVagrant() ? PossessionComponent.get(player).getPossessedEntity() : player;

        if (body != null && !player.getServerWorld().getServer().isHardcore() && player.hasStatusEffect(RequiemStatusEffects.ATTRITION) && player.getRandom().nextInt(ATTRITION_MEND_PROBABILITY) == 0) {
            AttritionFocus.KEY.get(body).addAttrition(this.player.getUuid(), 1);
            AttritionStatusEffect.reduce(player, 1);

            spawnAttritionParticles(player, body);
        }
    }

    public static void spawnAttritionParticles(ServerPlayerEntity player, LivingEntity body) {
        player.getServerWorld().spawnParticles(
            RequiemParticleTypes.ATTRITION,
            body.getX(),
            body.getBodyY(0.5),
            body.getZ(),
            60,
            body.getWidth() * 0.8,
            body.getHeight() * 0.6,
            body.getWidth() *0.8,
            1.0
        );
    }

    @Override
    protected void regenerateBody(LivingEntity body) {
        throw new UnsupportedOperationException("Wandering spirits cannot regenerate human bodies");
    }

    @Override
    public boolean canSplit() {
        return true;
    }

    @Override
    protected void onRespawnAfterDeath() {
        // NO-OP
    }
}
