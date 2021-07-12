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
package ladysnake.requiem.common.item;

import ladysnake.requiem.api.v1.event.requiem.EntityRecordUpdateCallback;
import ladysnake.requiem.common.RequiemRecordTypes;
import ladysnake.requiem.common.entity.ReleasedSoulEntity;
import ladysnake.requiem.common.entity.RequiemEntities;
import ladysnake.requiem.core.entity.EntityAiToggle;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsage;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

public class FilledSoulVesselItem extends Item {
    public static final String SOUL_FRAGMENT_NBT = "requiem:soul_fragment";

    private final EmptySoulVesselItem emptySoulVessel;

    public FilledSoulVesselItem(Settings settings, EmptySoulVesselItem emptySoulVessel) {
        super(settings);
        this.emptySoulVessel = emptySoulVessel;
    }

    public void registerCallbacks() {
        EntityRecordUpdateCallback.EVENT.register((entity, linkedRecord) ->
            linkedRecord.get(RequiemRecordTypes.RELEASED_SOUL).flatMap(u -> EntityAiToggle.KEY.maybeGet(entity)).ifPresent(aiSwitch -> {
                aiSwitch.toggleAi(Registry.ITEM.getId(this.emptySoulVessel), false, false);
                linkedRecord.invalidate();
            }));
    }

    @Override
    public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
        Optional.ofNullable(stack.getSubNbt(SOUL_FRAGMENT_NBT))
            .flatMap(fragmentData -> EntityType.get(fragmentData.getString("type")))
            .ifPresent(contained -> tooltip.add(new TranslatableText("requiem:tooltip.filled_vessel", contained.getName()).formatted(Formatting.GRAY)));
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);
        if (!world.isClient()) {
            ReleasedSoulEntity releasedSoul = new ReleasedSoulEntity(RequiemEntities.RELEASED_SOUL, world, Optional.ofNullable(stack.getSubNbt(FilledSoulVesselItem.SOUL_FRAGMENT_NBT))
                .filter(data -> data.containsUuid("uuid"))
                .map(data -> data.getUuid("uuid"))
                .orElse(null));
            releasedSoul.setPosition(user.getX(), user.getBodyY(0.5D), user.getZ());
            releasedSoul.setVelocity(user.getRotationVector());
            world.spawnEntity(releasedSoul);
            ItemStack result = new ItemStack(this.emptySoulVessel);
            return TypedActionResult.success(ItemUsage.exchangeStack(stack, user, result));
        }
        return TypedActionResult.success(stack);
    }
}
