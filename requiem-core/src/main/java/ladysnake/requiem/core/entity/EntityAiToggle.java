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
package ladysnake.requiem.core.entity;

import dev.onyxstudios.cca.api.v3.component.Component;
import dev.onyxstudios.cca.api.v3.component.ComponentKey;
import dev.onyxstudios.cca.api.v3.component.ComponentRegistry;
import it.unimi.dsi.fastutil.objects.Object2BooleanMap;
import it.unimi.dsi.fastutil.objects.Object2BooleanOpenHashMap;
import ladysnake.requiem.core.RequiemCore;
import ladysnake.requiem.core.entity.ai.DisableableAiController;
import ladysnake.requiem.core.mixin.access.MobEntityAccessor;
import net.minecraft.entity.LivingEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.util.Identifier;

import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class EntityAiToggle implements Component {
    public static final ComponentKey<EntityAiToggle> KEY = ComponentRegistry.getOrCreate(RequiemCore.id("ai_toggle"), EntityAiToggle.class);

    public static boolean isAiDisabled(LivingEntity entity) {
        return KEY.get(entity).isAiDisabled();
    }

    private final LivingEntity owner;
    private final Object2BooleanMap<Identifier> aiInhibitors = new Object2BooleanOpenHashMap<>();

    public EntityAiToggle(LivingEntity owner) {
        this.owner = owner;
    }

    /**
     * Toggles an AI inhibitor on this entity.
     *
     * @param inhibitorId the unique identifier for the mechanic inhibiting this entity's AI
     * @param inhibit if {@code true}, the entity's AI will be disabled, otherwise the inhibitor will stop affecting the entity
     * @param persistent if {@code true}, the inhibition will be saved with the entity
     */
    public void toggleAi(Identifier inhibitorId, boolean inhibit, boolean persistent) {
        boolean wasDisabled = this.isAiDisabled();
        if (inhibit) {
            this.aiInhibitors.put(inhibitorId, persistent);
        } else {
            this.aiInhibitors.removeBoolean(inhibitorId);
        }
        boolean nowDisabled = this.isAiDisabled();

        if (wasDisabled != nowDisabled) {
            this.refresh(nowDisabled);
        }
    }

    private void refresh(boolean nowDisabled) {
        ((DisableableAiController) this.owner.getBrain()).requiem$setDisabled(nowDisabled);
        if (this.owner instanceof MobEntityAccessor mob) {
            ((DisableableAiController) mob.getGoalSelector()).requiem$setDisabled(nowDisabled);
            ((DisableableAiController) mob.getTargetSelector()).requiem$setDisabled(nowDisabled);
            ((DisableableAiController) mob.requiem$getNavigation()).requiem$setDisabled(nowDisabled);
        }
    }

    public boolean isAiDisabled() {
        return !this.aiInhibitors.isEmpty();
    }

    @Override
    public void readFromNbt(NbtCompound tag) {
        tag.getList("inhibitors", NbtElement.STRING_TYPE)
            .stream()
            .map(NbtElement::asString)
            .map(Identifier::tryParse)
            .filter(Objects::nonNull)
            .forEach(id -> this.aiInhibitors.put(id, true));
        this.refresh(this.isAiDisabled());
    }

    @Override
    public void writeToNbt(NbtCompound tag) {
        tag.put("inhibitors", this.aiInhibitors.object2BooleanEntrySet().stream()
            .filter(Object2BooleanMap.Entry::getBooleanValue)
            .map(Map.Entry::getKey)
            .map(Identifier::toString)
            .map(NbtString::of)
            .collect(Collectors.toCollection(NbtList::new)));
    }
}
