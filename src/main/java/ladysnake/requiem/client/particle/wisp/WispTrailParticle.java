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
package ladysnake.requiem.client.particle.wisp;

import ladysnake.requiem.common.particle.WispTrailParticleEffect;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleFactory;
import net.minecraft.client.particle.ParticleTextureSheet;
import net.minecraft.client.particle.SpriteBillboardParticle;
import net.minecraft.client.particle.SpriteProvider;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Quaternion;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3f;

import java.util.Random;

public final class WispTrailParticle extends SpriteBillboardParticle {
    private final float redEvolution;
    private final float greenEvolution;
    private final float blueEvolution;

    private WispTrailParticle(ClientWorld world, double x, double y, double z, double velocityX, double velocityY, double velocityZ, WispTrailParticleEffect wispTrailParticleEffect, SpriteProvider spriteProvider) {
        super(world, x, y, z, velocityX, velocityY, velocityZ);
        this.colorRed = wispTrailParticleEffect.red();
        this.colorGreen = wispTrailParticleEffect.green();
        this.colorBlue = wispTrailParticleEffect.blue();
        this.redEvolution = wispTrailParticleEffect.redEvolution();
        this.greenEvolution = wispTrailParticleEffect.greenEvolution();
        this.blueEvolution = wispTrailParticleEffect.blueEvolution();
        this.maxAge = 10 + this.random.nextInt(10);
        this.scale *= 0.25f + new Random().nextFloat() * 0.50f;
        this.setSpriteForAge(spriteProvider);
        this.velocityY = 0.1;
    }

    @Override
    public ParticleTextureSheet getType() {
        return ParticleTextureSheet.PARTICLE_SHEET_TRANSLUCENT;
    }

    @Override
    public void tick() {
        this.prevPosX = this.x;
        this.prevPosY = this.y;
        this.prevPosZ = this.z;

        // fade and die
        if (this.age++ >= this.maxAge) {
            colorAlpha -= 0.05f;
        }
        if (colorAlpha < 0f || this.scale <= 0f) {
            this.markDead();
        }

//        float redEv = -0.03f;
//        float greenEv = 0.0f;
//        float blueEv = -0.01f;
//        colorRed = MathHelper.clamp(colorRed+redEv, 0, 1);
//        colorGreen = MathHelper.clamp(colorGreen+greenEv, 0, 1);
//        colorBlue = MathHelper.clamp(colorBlue+blueEv, 0, 1);

        colorRed = MathHelper.clamp(colorRed + redEvolution, 0, 1);
        colorGreen = MathHelper.clamp(colorGreen + greenEvolution, 0, 1);
        colorBlue = MathHelper.clamp(colorBlue + blueEvolution, 0, 1);

        this.velocityY -= 0.001;
        this.velocityX = 0;
        this.velocityZ = 0;
        this.scale = Math.max(0, this.scale - 0.005f);
        this.move(velocityX, velocityY, velocityZ);
    }

    @Override
    public void buildGeometry(VertexConsumer vertexConsumer, Camera camera, float tickDelta) {
        Vec3d vec3d = camera.getPos();
        float f = (float) (MathHelper.lerp(tickDelta, this.prevPosX, this.x) - vec3d.getX());
        float g = (float) (MathHelper.lerp(tickDelta, this.prevPosY, this.y) - vec3d.getY());
        float h = (float) (MathHelper.lerp(tickDelta, this.prevPosZ, this.z) - vec3d.getZ());
        Quaternion quaternion2;
        if (this.angle == 0.0F) {
            quaternion2 = camera.getRotation();
        } else {
            quaternion2 = new Quaternion(camera.getRotation());
            float i = MathHelper.lerp(tickDelta, this.prevAngle, this.angle);
            quaternion2.hamiltonProduct(Vec3f.POSITIVE_Z.getRadialQuaternion(i));
        }

        Vec3f Vec3f = new Vec3f(-1.0F, -1.0F, 0.0F);
        Vec3f.rotate(quaternion2);
        Vec3f[] Vec3fs = new Vec3f[]{new Vec3f(-1.0F, -1.0F, 0.0F), new Vec3f(-1.0F, 1.0F, 0.0F), new Vec3f(1.0F, 1.0F, 0.0F), new Vec3f(1.0F, -1.0F, 0.0F)};
        float j = this.getSize(tickDelta);

        for (int k = 0; k < 4; ++k) {
            Vec3f Vec3f2 = Vec3fs[k];
            Vec3f2.rotate(quaternion2);
            Vec3f2.scale(j);
            Vec3f2.add(f, g, h);
        }

        float minU = this.getMinU();
        float maxU = this.getMaxU();
        float minV = this.getMinV();
        float maxV = this.getMaxV();
        int l = 15728880;

        vertexConsumer.vertex(Vec3fs[0].getX(), Vec3fs[0].getY(), Vec3fs[0].getZ()).texture(maxU, maxV).color(colorRed, colorGreen, colorBlue, colorAlpha).light(l).next();
        vertexConsumer.vertex(Vec3fs[1].getX(), Vec3fs[1].getY(), Vec3fs[1].getZ()).texture(maxU, minV).color(colorRed, colorGreen, colorBlue, colorAlpha).light(l).next();
        vertexConsumer.vertex(Vec3fs[2].getX(), Vec3fs[2].getY(), Vec3fs[2].getZ()).texture(minU, minV).color(colorRed, colorGreen, colorBlue, colorAlpha).light(l).next();
        vertexConsumer.vertex(Vec3fs[3].getX(), Vec3fs[3].getY(), Vec3fs[3].getZ()).texture(minU, maxV).color(colorRed, colorGreen, colorBlue, colorAlpha).light(l).next();
    }

    @Environment(EnvType.CLIENT)
    public static class Factory implements ParticleFactory<WispTrailParticleEffect> {
        private final SpriteProvider spriteProvider;

        public Factory(SpriteProvider spriteProvider) {
            this.spriteProvider = spriteProvider;
        }

        @Override
        public Particle createParticle(WispTrailParticleEffect wispTrailParticleEffect, ClientWorld clientWorld, double x, double y, double z, double velocityX, double velocityY, double velocityZ) {
            return new WispTrailParticle(clientWorld, x, y, z, velocityX, velocityY, velocityZ, wispTrailParticleEffect, this.spriteProvider);
        }
    }
}
