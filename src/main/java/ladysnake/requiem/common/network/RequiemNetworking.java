/*
 * Requiem
 * Copyright (C) 2019 Ladysnake
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
 */
package ladysnake.requiem.common.network;

import ladysnake.requiem.Requiem;
import ladysnake.requiem.api.v1.RequiemPlayer;
import ladysnake.requiem.api.v1.remnant.RemnantType;
import ladysnake.requiem.api.v1.util.SubDataManager;
import ladysnake.requiem.api.v1.util.SubDataManagerHelper;
import ladysnake.requiem.common.remnant.RemnantStates;
import net.fabricmc.fabric.api.server.PlayerStream;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.packet.CustomPayloadS2CPacket;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.network.packet.CustomPayloadC2SPacket;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.PacketByteBuf;
import org.jetbrains.annotations.Contract;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static io.netty.buffer.Unpooled.buffer;

public class RequiemNetworking {
    // Server -> Client
    public static final Identifier POSSESSION_SYNC = Requiem.id("possession_sync");
    public static final Identifier REMNANT_SYNC = Requiem.id("remnant_sync");
    public static final Identifier POSSESSION_ACK = Requiem.id("possession_ack");
    public static final Identifier OPUS_USE = Requiem.id("opus_use");
    public static final Identifier DATA_SYNC = Requiem.id("data_sync");

    // Client -> Server
    public static final Identifier LEFT_CLICK_AIR = Requiem.id("attack_air");
    public static final Identifier RIGHT_CLICK_AIR = Requiem.id("interact_air");
    public static final Identifier POSSESSION_REQUEST = Requiem.id("possession_request");
    public static final Identifier ETHEREAL_FRACTURE = Requiem.id("ethereal_fracture");
    public static final Identifier OPUS_UPDATE = Requiem.id("opus_update");
    public static final Identifier DIALOGUE_ACTION = Requiem.id("dialogue_action");

    public static void sendToServer(Identifier identifier, PacketByteBuf data) {
        sendToServer(new CustomPayloadC2SPacket(identifier, data));
    }

    public static void sendToServer(CustomPayloadC2SPacket message) {
        MinecraftClient.getInstance().player.networkHandler.sendPacket(message);
    }

    public static void sendTo(ServerPlayerEntity player, CustomPayloadS2CPacket message) {
        sendToPlayer(player, message);
        message.getData().release();
    }

    public static void sendToAllTrackingIncluding(Entity tracked, CustomPayloadS2CPacket message) {
        if (tracked.world instanceof ServerWorld) {
            PlayerStream.watching(tracked).forEach(p -> sendToPlayer((ServerPlayerEntity) p, message));
            if (tracked instanceof ServerPlayerEntity) {
                sendToPlayer((ServerPlayerEntity) tracked, message);
            }
        }
        message.getData().release();
    }

    private static void sendToPlayer(ServerPlayerEntity player, CustomPayloadS2CPacket message) {
        if (player.networkHandler != null) {
            player.networkHandler.sendPacket(message);
        }
    }

    @Contract(pure = true)
    public static CustomPayloadS2CPacket createCorporealityMessage(PlayerEntity synchronizedPlayer) {
        RemnantType remnantType = ((RequiemPlayer) synchronizedPlayer).getRemnantState().getType();
        boolean incorporeal = ((RequiemPlayer)synchronizedPlayer).getRemnantState().isSoul();
        UUID playerUuid = synchronizedPlayer.getUuid();
        boolean lifeTransient = ((RequiemPlayer) synchronizedPlayer).getDeathSuspender().isLifeTransient();
        return createCorporealityMessage(playerUuid, remnantType, incorporeal, lifeTransient);
    }

    @Contract(pure = true)
    private static CustomPayloadS2CPacket createCorporealityMessage(UUID playerUuid, RemnantType remnantType, boolean incorporeal, boolean lifeTransient) {
        PacketByteBuf buf = createEmptyBuffer();
        buf.writeUuid(playerUuid);
        buf.writeVarInt(RemnantStates.getRawId(remnantType));
        buf.writeBoolean(incorporeal);
        buf.writeBoolean(lifeTransient);
        return new CustomPayloadS2CPacket(REMNANT_SYNC, buf);
    }

    @Contract(pure = true)
    public static CustomPayloadS2CPacket createPossessionMessage(UUID playerUuid, int possessedId) {
        PacketByteBuf buf = createEmptyBuffer();
        buf.writeUuid(playerUuid);
        buf.writeInt(possessedId);
        return new CustomPayloadS2CPacket(POSSESSION_SYNC, buf);
    }

    public static CustomPayloadS2CPacket createOpusUsePacket(boolean cure, boolean showBook) {
        PacketByteBuf buf = createEmptyBuffer();
        buf.writeBoolean(cure);
        buf.writeBoolean(showBook);
        return new CustomPayloadS2CPacket(OPUS_USE, buf);
    }

    @Contract(pure = true)
    public static CustomPayloadS2CPacket createEmptyMessage(Identifier id) {
        return new CustomPayloadS2CPacket(id, createEmptyBuffer());
    }

    public static CustomPayloadS2CPacket createDataSyncMessage(SubDataManagerHelper helper) {
        PacketByteBuf buf = createEmptyBuffer();
        List<SubDataManager<?>> managers = helper.streamDataManagers().collect(Collectors.toList());
        buf.writeVarInt(managers.size());
        for (SubDataManager<?> manager : managers) {
            buf.writeIdentifier(manager.getFabricId());
            manager.toPacket(buf);
        }
        return new CustomPayloadS2CPacket(DATA_SYNC, buf);
    }

    @Contract(pure = true)
    public static PacketByteBuf createEmptyBuffer() {
        return new PacketByteBuf(buffer());
    }

    @Contract(pure = true)
    public static PacketByteBuf createPossessionRequestBuffer(Entity entity) {
        PacketByteBuf buf = new PacketByteBuf(buffer());
        buf.writeInt(entity.getEntityId());
        return buf;
    }

    public static CustomPayloadC2SPacket createOpusUpdateBuffer(String content, boolean sign, RemnantType resultingBook, Hand hand) {
        PacketByteBuf buf = new PacketByteBuf(buffer());
        buf.writeString(content);
        buf.writeBoolean(sign);
        if (sign) {
            buf.writeString(RemnantStates.getId(resultingBook).toString());
        }
        buf.writeEnumConstant(hand);
        return new CustomPayloadC2SPacket(OPUS_UPDATE, buf);
    }

    public static CustomPayloadC2SPacket createDialogueActionMessage(Identifier action) {
        PacketByteBuf buf = new PacketByteBuf(buffer());
        buf.writeIdentifier(action);
        return new CustomPayloadC2SPacket(DIALOGUE_ACTION, buf);
    }

}