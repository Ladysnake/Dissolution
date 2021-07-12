/*
 * Requiem
 * Copyright (C) 2017-2021 Ladysnake
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; If not, see <https://www.gnu.org/licenses>.
 */
package ladysnake.requiem.api.v1.event.requiem;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;

public final class SoulCaptureEvents {
    public static final Event<BeforeAttempt> BEFORE_ATTEMPT = EventFactory.createArrayBacked(BeforeAttempt.class,
        (callbacks) -> (player, target) -> {
            for (BeforeAttempt callback : callbacks) {
                if (!callback.canAttemptCapturing(player, target)) {
                    return false;
                }
            }
            return true;
        });

    @FunctionalInterface
    public interface BeforeAttempt {
        boolean canAttemptCapturing(PlayerEntity player, LivingEntity target);
    }
}
