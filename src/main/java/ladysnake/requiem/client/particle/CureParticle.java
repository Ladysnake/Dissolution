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
package ladysnake.requiem.client.particle;

import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleFactory;
import net.minecraft.client.particle.ParticleTextureSheet;
import net.minecraft.client.particle.SpriteBillboardParticle;
import net.minecraft.client.particle.SpriteProvider;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particle.DefaultParticleType;
import net.minecraft.util.math.MathHelper;

public class CureParticle extends SpriteBillboardParticle {
    private boolean reachedGround;

    private CureParticle(ClientWorld world, double x, double y, double z, double velocityX, double velocityY, double velocityZ) {
        super(world, x, y, z);
        this.velocityX = velocityX;
        this.velocityY = velocityY;
        this.velocityZ = velocityZ;
        this.maxAge = (int)(20.0D / ((double)this.random.nextFloat() * 0.8D + 0.2D));
        this.reachedGround = false;
        this.collidesWithWorld = false;
    }

    // Taken from DragonBreathParticle
    @Override
    public void tick() {
        this.prevPosX = this.x;
        this.prevPosY = this.y;
        this.prevPosZ = this.z;

        if (this.age++ >= this.maxAge) {
            this.markDead();
        } else {
            if (this.onGround) {
                this.velocityY = 0.0D;
                this.reachedGround = true;
            }

            if (this.reachedGround) {
                this.velocityY += 0.002;
            }

            this.move(this.velocityX, this.velocityY, this.velocityZ);

            if (this.y == this.prevPosY) {
                this.velocityX *= 1.1;
                this.velocityZ *= 1.1;
            }

            this.velocityX *= 0.95;
            this.velocityZ *= 0.95;

            if (this.reachedGround) {
                this.velocityY *= 0.95;
            }
        }
    }

    @Override
    public ParticleTextureSheet getType() {
        return ParticleTextureSheet.PARTICLE_SHEET_OPAQUE;
    }

    @Override
    public float getSize(float tickDelta) {
        return this.scale * MathHelper.clamp((this.age + tickDelta) / this.maxAge * 32.0F, 0.0F, 1.0F);
    }

    // taken from FlameParticle
    @Override
    public int getBrightness(float tickDelta) {
        float progress = ((float)this.age + tickDelta) / (float)this.maxAge;
        progress = MathHelper.clamp(progress, 0.0F, 1.0F);
        int lightCoords = super.getBrightness(tickDelta);
        int u = lightCoords & 255;
        int v = lightCoords >> 16 & 255;
        u += (int)(progress * 15.0F * 16.0F);
        if (u > 240) {
            u = 240;
        }

        return u | v << 16;
    }

    public static class Factory implements ParticleFactory<DefaultParticleType> {
        private final SpriteProvider spriteProvider;

        public Factory(SpriteProvider spriteProvider) {
            this.spriteProvider = spriteProvider;
        }

        @Override
        public Particle createParticle(DefaultParticleType parameters, ClientWorld world, double x, double y, double z, double velocityX, double velocityY, double velocityZ) {
            CureParticle particle = new CureParticle(world, x, y, z, velocityX, velocityY, velocityZ);
            particle.setSpriteForAge(spriteProvider);
            return particle;
        }
    }
}
