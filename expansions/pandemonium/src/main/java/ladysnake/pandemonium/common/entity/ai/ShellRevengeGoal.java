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
package ladysnake.pandemonium.common.entity.ai;

import ladysnake.pandemonium.common.entity.PlayerShellEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.RevengeGoal;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.util.math.Box;

import java.util.Objects;
import java.util.UUID;

public class ShellRevengeGoal extends RevengeGoal {
    private final PlayerShellEntity shell;

    public ShellRevengeGoal(PlayerShellEntity shell, Class<?>... noRevengeTypes) {
        super(shell.getGuide(), noRevengeTypes);
        this.shell = shell;
    }

    @Override
    public void start() {
        super.start();
        this.callForHelp();
    }

    protected void callForHelp() {
        double d = this.getFollowRange();
        Box box = Box.method_29968(this.mob.getPos()).expand(d, 10.0D, d);
        UUID ownerUuid = this.shell.getOwnerUuid();

        for (LivingEntity e : this.mob.world.getEntitiesIncludingUngeneratedChunks(LivingEntity.class, box)) {
            if (isFriendlyShell(ownerUuid, e)) {
                this.setMobEntityTarget(((PlayerShellEntity) e).getGuide(), target);
            } else if (isPet(ownerUuid, e)) {
                this.setMobEntityTarget((MobEntity) e, target);
            }
        }
    }

    private boolean isPet(UUID ownerUuid, LivingEntity e) {
        return e instanceof TameableEntity && Objects.equals(((TameableEntity) e).getOwnerUuid(), ownerUuid);
    }

    private boolean isFriendlyShell(UUID ownerUuid, LivingEntity e) {
        return e instanceof PlayerShellEntity && Objects.equals(((PlayerShellEntity) e).getOwnerUuid(), ownerUuid);
    }
}
