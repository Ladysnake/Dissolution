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
package ladysnake.pandemonium;

import dev.onyxstudios.cca.api.v3.entity.EntityComponentFactoryRegistry;
import dev.onyxstudios.cca.api.v3.entity.EntityComponentInitializer;
import dev.onyxstudios.cca.api.v3.entity.RespawnCopyStrategy;
import dev.onyxstudios.cca.api.v3.scoreboard.ScoreboardComponentFactoryRegistry;
import dev.onyxstudios.cca.api.v3.scoreboard.ScoreboardComponentInitializer;
import io.github.ladysnake.impersonate.Impersonate;
import ladysnake.pandemonium.client.ClientRecordKeeper;
import ladysnake.pandemonium.common.PandemoniumCommand;
import ladysnake.pandemonium.common.PandemoniumConfig;
import ladysnake.pandemonium.common.block.PandemoniumBlocks;
import ladysnake.pandemonium.common.entity.PandemoniumEntities;
import ladysnake.pandemonium.common.entity.WololoComponent;
import ladysnake.pandemonium.common.entity.effect.PandemoniumStatusEffects;
import ladysnake.pandemonium.common.network.ServerMessageHandling;
import ladysnake.pandemonium.common.remnant.PlayerBodyTracker;
import ladysnake.pandemonium.compat.PandemoniumCompatibilityManager;
import ladysnake.requiem.api.v1.RequiemApi;
import ladysnake.requiem.api.v1.annotation.CalledThroughReflection;
import ladysnake.requiem.api.v1.event.minecraft.PlayerRespawnCallback;
import ladysnake.requiem.api.v1.event.requiem.RemnantStateChangeCallback;
import ladysnake.requiem.api.v1.record.GlobalRecordKeeper;
import ladysnake.requiem.core.RequiemCore;
import ladysnake.requiem.core.record.EntityPositionClerk;
import ladysnake.requiem.core.record.ServerRecordKeeper;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v1.CommandRegistrationCallback;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.EndermanEntity;
import net.minecraft.util.Identifier;

@CalledThroughReflection
public final class Pandemonium implements ModInitializer, EntityComponentInitializer, ScoreboardComponentInitializer {
    public static final String MOD_ID = "pandemonium";
    public static final Identifier BODY_IMPERSONATION = RequiemCore.id("body_impersonation");
    public static final Pandemonium INSTANCE = new Pandemonium();

    public static Identifier id(String path) {
        return new Identifier(MOD_ID, path);
    }

    @Override
    public void onInitialize() {
        PandemoniumConfig.load();
        PandemoniumBlocks.init();
        PandemoniumEntities.init();
        PandemoniumStatusEffects.init();
        ServerMessageHandling.init();
        RequiemApi.registerPlugin(new PandemoniumRequiemPlugin());
        PlayerRespawnCallback.EVENT.register((player, returnFromEnd) -> {
            if (!returnFromEnd) Impersonate.IMPERSONATION.get(player).stopImpersonation(BODY_IMPERSONATION);
        });
        RemnantStateChangeCallback.EVENT.register((player, state) -> {
            if (state.isVagrant()) Impersonate.IMPERSONATION.get(player).stopImpersonation(BODY_IMPERSONATION);
        });
        CommandRegistrationCallback.EVENT.register((dispatcher, dedicated) -> PandemoniumCommand.register(dispatcher));
        PandemoniumCompatibilityManager.init();
    }

    @Override
    public void registerEntityComponentFactories(EntityComponentFactoryRegistry registry) {
        registry.registerForPlayers(PlayerBodyTracker.KEY, PlayerBodyTracker::new, RespawnCopyStrategy.ALWAYS_COPY);
        registry.registerFor(EndermanEntity.class, WololoComponent.KEY, WololoComponent::create);
        registry.registerFor(LivingEntity.class, EntityPositionClerk.KEY, EntityPositionClerk::new);
    }

    @Override
    public void registerScoreboardComponentFactories(ScoreboardComponentFactoryRegistry registry) {
        registry.registerScoreboardComponent(GlobalRecordKeeper.KEY, (scoreboard, server) -> server == null
            ? new ClientRecordKeeper(scoreboard)
            : new ServerRecordKeeper(scoreboard, server)
        );
    }

    private Pandemonium() {
        super();
    }
}
