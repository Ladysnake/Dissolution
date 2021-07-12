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
package ladysnake.requiem.common.sound;

import ladysnake.requiem.Requiem;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;

public final class RequiemSoundEvents {
    public static final SoundEvent EFFECT_BECOME_MORTAL = register("effect.become.mortal");
    public static final SoundEvent EFFECT_BECOME_REMNANT = register("effect.become.remnant");
    public static final SoundEvent EFFECT_PHASE = register("effect.phase");
    public static final SoundEvent EFFECT_POSSESSION_ATTEMPT = register("effect.possession.attempt");
    public static final SoundEvent EFFECT_RECLAMATION_CLEAR = register("effect.reclamation.clear");
    public static final SoundEvent EFFECT_TIME_STOP = register("effect.time.stop");
    public static final SoundEvent ENTITY_SOUL_TELEPORT = register("entity.soul.teleport");
    public static final SoundEvent ITEM_OPUS_USE = register("item.opus.use");

    private static SoundEvent register(String name) {
        Identifier id = Requiem.id(name);
        return Registry.register(Registry.SOUND_EVENT, id, new SoundEvent(id));
    }

    public static void init() {
        // NO-OP
    }
}
