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
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleType;
import net.minecraft.util.registry.Registry;

public record WispTrailParticleEffect(float red, float green, float blue, float redEvolution, float greenEvolution,
                                      float blueEvolution) implements ParticleEffect {
    public static final Codec<WispTrailParticleEffect> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        Codec.FLOAT.fieldOf("r").forGetter(WispTrailParticleEffect::red),
        Codec.FLOAT.fieldOf("g").forGetter(WispTrailParticleEffect::green),
        Codec.FLOAT.fieldOf("b").forGetter(WispTrailParticleEffect::blue),
        Codec.FLOAT.fieldOf("re").forGetter(WispTrailParticleEffect::redEvolution),
        Codec.FLOAT.fieldOf("ge").forGetter(WispTrailParticleEffect::greenEvolution),
        Codec.FLOAT.fieldOf("be").forGetter(WispTrailParticleEffect::blueEvolution)
    ).apply(instance, WispTrailParticleEffect::new));
    public static final ParticleEffect.Factory<WispTrailParticleEffect> PARAMETERS_FACTORY = new ParticleEffect.Factory<>() {
        @Override
        public WispTrailParticleEffect read(ParticleType<WispTrailParticleEffect> particleType, StringReader stringReader) throws CommandSyntaxException {
            stringReader.expect(' ');
            float r = (float) stringReader.readDouble();
            stringReader.expect(' ');
            float g = (float) stringReader.readDouble();
            stringReader.expect(' ');
            float b = (float) stringReader.readDouble();
            stringReader.expect(' ');
            float re = (float) stringReader.readDouble();
            stringReader.expect(' ');
            float ge = (float) stringReader.readDouble();
            stringReader.expect(' ');
            float be = (float) stringReader.readDouble();
            return new WispTrailParticleEffect(r, g, b, re, ge, be);
        }

        @Override
        public WispTrailParticleEffect read(ParticleType<WispTrailParticleEffect> particleType, PacketByteBuf packetByteBuf) {
            return new WispTrailParticleEffect(packetByteBuf.readFloat(), packetByteBuf.readFloat(), packetByteBuf.readFloat(), packetByteBuf.readFloat(), packetByteBuf.readFloat(), packetByteBuf.readFloat());
        }
    };

    @Override
    public void write(PacketByteBuf buf) {
        buf.writeFloat(this.red);
        buf.writeFloat(this.green);
        buf.writeFloat(this.blue);
        buf.writeFloat(this.redEvolution);
        buf.writeFloat(this.greenEvolution);
        buf.writeFloat(this.blueEvolution);
    }

    @Override
    public String asString() {
        return "%s %.2f %.2f %.2f %.2f %.2f %.2f".formatted(Registry.PARTICLE_TYPE.getId(this.getType()), this.red, this.green, this.blue, this.redEvolution, this.greenEvolution, this.blueEvolution);
    }

    @Override
    public ParticleType<WispTrailParticleEffect> getType() {
        return RequiemParticleTypes.SOUL_TRAIL;
    }

    @Override
    @Environment(EnvType.CLIENT)
    public float red() {
        return this.red;
    }

    @Override
    @Environment(EnvType.CLIENT)
    public float green() {
        return this.green;
    }

    @Override
    @Environment(EnvType.CLIENT)
    public float blue() {
        return this.blue;
    }

    @Override
    @Environment(EnvType.CLIENT)
    public float redEvolution() {
        return redEvolution;
    }

    @Override
    @Environment(EnvType.CLIENT)
    public float greenEvolution() {
        return greenEvolution;
    }

    @Override
    @Environment(EnvType.CLIENT)
    public float blueEvolution() {
        return blueEvolution;
    }
}
