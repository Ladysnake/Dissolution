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
package ladysnake.requiem.common.particle;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleType;
import net.minecraft.util.registry.Registry;

public class RequiemEntityParticleEffect implements ParticleEffect {
    public static final Factory<RequiemEntityParticleEffect> PARAMETERS_FACTORY = new Factory<>() {
        @Override
        public RequiemEntityParticleEffect read(ParticleType<RequiemEntityParticleEffect> particleType, StringReader stringReader) throws CommandSyntaxException {
            stringReader.expect(' ');
            int sourceEntityId = stringReader.readInt();
            stringReader.expect(' ');
            int targetEntityId = stringReader.readInt();
            return new RequiemEntityParticleEffect(particleType, sourceEntityId, targetEntityId);
        }

        @Override
        public RequiemEntityParticleEffect read(ParticleType<RequiemEntityParticleEffect> particleType, PacketByteBuf buf) {
            return new RequiemEntityParticleEffect(particleType, buf.readVarInt(), buf.readVarInt());
        }
    };
    private final ParticleType<RequiemEntityParticleEffect> type;
    private final int targetEntityId;
    private final int sourceEntityId;

    public static Codec<RequiemEntityParticleEffect> codec(ParticleType<RequiemEntityParticleEffect> particleType) {
        return RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.fieldOf("srcId").forGetter(RequiemEntityParticleEffect::getSourceEntityId),
            Codec.INT.fieldOf("destId").forGetter(RequiemEntityParticleEffect::getTargetEntityId)
        ).apply(instance, (sourceEntityId, id) -> new RequiemEntityParticleEffect(particleType, sourceEntityId, id)));
    }

    public RequiemEntityParticleEffect(ParticleType<RequiemEntityParticleEffect> type, int sourceEntityId, int targetEntityId) {
        this.type = type;
        this.sourceEntityId = sourceEntityId;
        this.targetEntityId = targetEntityId;
    }

    @Override
    public void write(PacketByteBuf buf) {
        buf.writeVarInt(sourceEntityId);
        buf.writeVarInt(targetEntityId);
    }

    @Override
    public String asString() {
        return "%s %d %d".formatted(Registry.PARTICLE_TYPE.getId(this.getType()), this.sourceEntityId, this.targetEntityId);
    }

    @Override
    public ParticleType<RequiemEntityParticleEffect> getType() {
        return this.type;
    }

    public int getTargetEntityId() {
        return targetEntityId;
    }

    public int getSourceEntityId() {
        return sourceEntityId;
    }
}
