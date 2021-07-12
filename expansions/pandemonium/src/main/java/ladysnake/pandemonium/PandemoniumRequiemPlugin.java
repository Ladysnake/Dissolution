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

import baritone.api.fakeplayer.AutomatoneFakePlayer;
import ladysnake.pandemonium.common.PandemoniumConfig;
import ladysnake.pandemonium.common.PlayerSplitter;
import ladysnake.pandemonium.common.entity.PlayerShellEntity;
import ladysnake.pandemonium.common.entity.ability.BlazeFireballAbility;
import ladysnake.pandemonium.common.entity.ability.BlinkAbility;
import ladysnake.pandemonium.common.entity.ability.CreeperPrimingAbility;
import ladysnake.pandemonium.common.entity.ability.EvokerFangAbility;
import ladysnake.pandemonium.common.entity.ability.EvokerVexAbility;
import ladysnake.pandemonium.common.entity.ability.EvokerWololoAbility;
import ladysnake.pandemonium.common.entity.ability.GhastFireballAbility;
import ladysnake.pandemonium.common.entity.ability.GuardianBeamAbility;
import ladysnake.pandemonium.common.entity.ability.WitherSkullAbility;
import ladysnake.pandemonium.common.entity.effect.PandemoniumStatusEffects;
import ladysnake.pandemonium.common.entity.effect.PenanceStatusEffect;
import ladysnake.pandemonium.common.remnant.PlayerBodyTracker;
import ladysnake.pandemonium.common.util.RayHelper;
import ladysnake.requiem.Requiem;
import ladysnake.requiem.api.v1.RequiemPlugin;
import ladysnake.requiem.api.v1.entity.ability.MobAbilityConfig;
import ladysnake.requiem.api.v1.entity.ability.MobAbilityRegistry;
import ladysnake.requiem.api.v1.event.requiem.CanCurePossessedCallback;
import ladysnake.requiem.api.v1.event.requiem.InitiateFractureCallback;
import ladysnake.requiem.api.v1.event.requiem.PlayerShellEvents;
import ladysnake.requiem.api.v1.event.requiem.PossessionStartCallback;
import ladysnake.requiem.api.v1.possession.PossessionComponent;
import ladysnake.requiem.api.v1.remnant.SoulbindingRegistry;
import ladysnake.requiem.api.v1.remnant.VagrantInteractionRegistry;
import ladysnake.requiem.common.network.RequiemNetworking;
import ladysnake.requiem.core.entity.ability.RangedAttackAbility;
import net.fabricmc.fabric.api.util.TriState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.boss.WitherEntity;
import net.minecraft.entity.mob.CreeperEntity;
import net.minecraft.entity.mob.EvokerEntity;
import net.minecraft.entity.mob.GuardianEntity;
import net.minecraft.entity.passive.LlamaEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Identifier;

import java.util.Objects;

public class PandemoniumRequiemPlugin implements RequiemPlugin {

    @Override
    public void onRequiemInitialize() {
        CanCurePossessedCallback.EVENT.register((body) -> {
            if (body.hasStatusEffect(PandemoniumStatusEffects.PENANCE)) {
                return TriState.FALSE;
            }
            return TriState.DEFAULT;
        });

        if (PandemoniumConfig.possession.allowPossessingAllMobs) {
            // Enderman specific behaviour is unneeded now that players can possess them
            PossessionStartCallback.EVENT.unregister(new Identifier(Requiem.MOD_ID, "enderman"));
            PossessionStartCallback.EVENT.register(Pandemonium.id("allow_everything"), (target, possessor, simulate) -> PossessionStartCallback.Result.ALLOW);
        }
        PlayerShellEvents.PRE_MERGE.register((player, playerShell, shellProfile) ->
            PenanceStatusEffect.getLevel(player) < 1);
        PossessionStartCallback.EVENT.register(Pandemonium.id("deny_penance_three"), ((target, possessor, simulate) ->
            PenanceStatusEffect.getLevel(possessor) >= 2 ? PossessionStartCallback.Result.DENY : PossessionStartCallback.Result.PASS));
        InitiateFractureCallback.EVENT.register(player -> {
            PossessionComponent possessionComponent = PossessionComponent.get(player);

            boolean success;

            if (PlayerSplitter.split(player)) {
                success = true;
            } else if (possessionComponent.isPossessing()) {
                Entity targetedEntity = RayHelper.getTargetedEntity(player);
                if (targetedEntity instanceof PlayerShellEntity && Objects.equals(player.getUuid(), ((PlayerShellEntity) targetedEntity).getOwnerUuid())) {
                    possessionComponent.stopPossessing();
                    PlayerSplitter.merge((PlayerShellEntity) targetedEntity, player);
                    RequiemNetworking.sendBodyCureMessage(player);
                    success = true;
                } else if (PlayerBodyTracker.get(player).getAnchor().isPresent()) {
                    possessionComponent.stopPossessing();
                    success = true;
                } else {
                    success = false;
                }
            } else {
                success = false;
            }

            if (success) {
                RequiemNetworking.sendEtherealAnimationMessage(player);
            }

            return success;
        });
    }

    @Override
    public void registerMobAbilities(MobAbilityRegistry abilityRegistry) {
        abilityRegistry.register(EntityType.BLAZE, MobAbilityConfig.builder().indirectAttack(BlazeFireballAbility::new).build());
        abilityRegistry.register(EntityType.CREEPER, MobAbilityConfig.<CreeperEntity>builder().indirectAttack(CreeperPrimingAbility::new).build());
        abilityRegistry.register(EntityType.ENDERMAN, MobAbilityConfig.builder().indirectInteract(BlinkAbility::new).build());
        abilityRegistry.register(EntityType.EVOKER, MobAbilityConfig.<EvokerEntity>builder()
            .directAttack(EvokerFangAbility::new)
            .directInteract(EvokerWololoAbility::new)
            .indirectInteract(EvokerVexAbility::new)
            .build());
        abilityRegistry.register(EntityType.GHAST, MobAbilityConfig.builder().indirectAttack(GhastFireballAbility::new).build());
        abilityRegistry.register(EntityType.GUARDIAN, MobAbilityConfig.<GuardianEntity>builder().directAttack(GuardianBeamAbility::new).build());
        abilityRegistry.register(EntityType.ELDER_GUARDIAN, MobAbilityConfig.<GuardianEntity>builder().directAttack(GuardianBeamAbility::new).build());
        abilityRegistry.register(EntityType.LLAMA, MobAbilityConfig.<LlamaEntity>builder().directAttack(RangedAttackAbility::new).build());
        abilityRegistry.register(EntityType.TRADER_LLAMA, MobAbilityConfig.<LlamaEntity>builder().directAttack(RangedAttackAbility::new).build());
        abilityRegistry.register(EntityType.WITHER, MobAbilityConfig.<WitherEntity>builder().indirectAttack(WitherSkullAbility.BlueWitherSkullAbility::new).directAttack(WitherSkullAbility.BlackWitherSkullAbility::new).build());
    }

    @Override
    public void registerSoulBindings(SoulbindingRegistry registry) {
        registry.registerSoulbound(PandemoniumStatusEffects.PENANCE);
    }

    @Override
    public void registerVagrantInteractions(VagrantInteractionRegistry registry) {
        registry.registerPossessionInteraction(PlayerEntity.class,
            (target, possessor) -> target instanceof AutomatoneFakePlayer,
            (target, possessor) -> {
                if (target instanceof PlayerShellEntity && !PlayerSplitter.merge((PlayerShellEntity) target, (ServerPlayerEntity) possessor)) {
                    possessor.sendMessage(new TranslatableText("requiem:possess.incompatible_body"), true);
                }
            }
        );
    }
}
