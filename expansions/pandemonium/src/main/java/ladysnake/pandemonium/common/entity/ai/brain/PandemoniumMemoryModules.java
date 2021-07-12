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
package ladysnake.pandemonium.common.entity.ai.brain;

import com.mojang.serialization.Codec;
import ladysnake.pandemonium.Pandemonium;
import ladysnake.pandemonium.mixin.common.entity.ai.MemoryModuleTypeAccessor;
import ladysnake.pandemonium.mixin.common.entity.ai.SerializableMemoryModuleTypeAccessor;
import ladysnake.requiem.api.v1.record.EntityPointer;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.brain.MemoryModuleType;
import net.minecraft.util.dynamic.DynamicSerializableUuid;

import java.util.List;
import java.util.UUID;

public final class PandemoniumMemoryModules {
    public static final MemoryModuleType<EntityPointer> GLOBAL_ENTITY_POS = register("global_entity_pos", EntityPointer.CODEC);
    public static final MemoryModuleType<UUID> LINKED_ENTITY = register("linked_entity", DynamicSerializableUuid.CODEC);
    public static final MemoryModuleType<Integer> GO_HOME_ATTEMPTS = register("pathfinding_failures");
    public static final MemoryModuleType<List<LivingEntity>> VISIBLE_HOSTILES = register("visible_hostiles");

    private static <U> MemoryModuleType<U> register(String id) {
        return MemoryModuleTypeAccessor.pandemonium$register(Pandemonium.MOD_ID + ":" + id);
    }

    private static <U> MemoryModuleType<U> register(String id, Codec<U> codec) {
        return SerializableMemoryModuleTypeAccessor.pandemonium$register(Pandemonium.MOD_ID + ":" + id, codec);
    }
}
