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
package ladysnake.pandemonium.client;

import baritone.api.fakeplayer.AutomatoneFakePlayer;
import ladysnake.pandemonium.client.render.entity.MorticianEntityRenderer;
import ladysnake.pandemonium.common.entity.PandemoniumEntities;
import ladysnake.requiem.api.v1.annotation.CalledThroughReflection;
import ladysnake.requiem.api.v1.event.minecraft.client.CrosshairRenderCallback;
import ladysnake.requiem.api.v1.possession.PossessionComponent;
import ladysnake.requiem.client.FractureKeyBinding;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendereregistry.v1.EntityRendererRegistry;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.entity.EntityType;
import net.minecraft.text.LiteralText;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Identifier;
import org.apache.commons.lang3.mutable.MutableBoolean;

@CalledThroughReflection
public class PandemoniumClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ClientMessageHandling.init();
        // shh, it's fine
        @SuppressWarnings({"unchecked", "RedundantCast"}) EntityType<? extends AbstractClientPlayerEntity> playerShellType = (EntityType<? extends AbstractClientPlayerEntity>) (EntityType<?>) PandemoniumEntities.PLAYER_SHELL;
        EntityRendererRegistry.INSTANCE.register(playerShellType, ctx -> new PlayerEntityRenderer(ctx, false));
        EntityRendererRegistry.INSTANCE.register(PandemoniumEntities.MORTICIAN, MorticianEntityRenderer::new);
        MutableBoolean wasLookingAtShell = new MutableBoolean();
        ClientTickEvents.START_CLIENT_TICK.register(client -> {
            if (client.targetedEntity instanceof AutomatoneFakePlayer && client.player != null) {
                if (PossessionComponent.getPossessedEntity(client.player) != null && client.player.getUuid().equals(((AutomatoneFakePlayer) client.targetedEntity).getOwnerUuid())) {
                    client.inGameHud.setOverlayMessage(new TranslatableText("requiem:merge_hint", FractureKeyBinding.etherealFractureKey.getBoundKeyLocalizedText()), false);
                    wasLookingAtShell.setTrue();
                }
            } else if (wasLookingAtShell.booleanValue()) {
                client.inGameHud.setOverlayMessage(LiteralText.EMPTY, false);
                wasLookingAtShell.setFalse();
            }
        });
        this.registerCallbacks();
        this.registerSprites();
    }

    private void registerSprites() {

    }

    private void registerCallbacks() {
        CrosshairRenderCallback.EVENT.unregister(new Identifier("requiem:enderman_color"));
    }
}
