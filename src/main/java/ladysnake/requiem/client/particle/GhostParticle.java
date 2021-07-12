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

import ladysnake.requiem.client.render.RequiemRenderPhases;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.particle.AbstractSlowingParticle;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleFactory;
import net.minecraft.client.particle.ParticleTextureSheet;
import net.minecraft.client.particle.SpriteProvider;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particle.DefaultParticleType;

public final class GhostParticle extends AbstractSlowingParticle {
    private static boolean renderedGhostParticle;
    private static final VertexConsumerProvider.Immediate ghostVertexConsumers = VertexConsumerProvider.immediate(
        new BufferBuilder(RequiemRenderPhases.GHOST_PARTICLE_LAYER.getExpectedBufferSize())
    );

    public static void draw(float tickDelta) {
        if (renderedGhostParticle) {
            // Somehow, the GL state can be really broken after another shader render
            RequiemRenderPhases.GHOST_PARTICLE_SHADER.render(tickDelta);
            RequiemRenderPhases.GHOST_PARTICLE_FRAMEBUFFER.clear();
            // Somehow, the GL state can also be really broken after the shader render
            MinecraftClient.getInstance().gameRenderer.getLightmapTextureManager().enable();
            renderedGhostParticle = false;
        }
    }

    private final SpriteProvider spriteProvider;

    private GhostParticle(ClientWorld world, double x, double y, double z, double velocityX, double velocityY, double velocityZ, SpriteProvider spriteProvider) {
        super(world, x, y, z, velocityX, velocityY, velocityZ);
        this.spriteProvider = spriteProvider;
        this.scale(1.5F);
        // if the particle spawns within a block, ghost mode goes brr
        this.collidesWithWorld = !this.isColliding();
        this.setSpriteForAge(spriteProvider);
    }

    @Override
    public ParticleTextureSheet getType() {
        return ParticleTextureSheet.CUSTOM;
    }

    @Override
    public void buildGeometry(VertexConsumer vertexConsumer, Camera camera, float tickDelta) {
        VertexConsumer actualConsumer = ghostVertexConsumers.getBuffer(RequiemRenderPhases.GHOST_PARTICLE_LAYER);
        super.buildGeometry(actualConsumer, camera, tickDelta);
        ghostVertexConsumers.draw();
        renderedGhostParticle = true;
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.dead) {
            this.setSpriteForAge(this.spriteProvider);
        }
    }

    public boolean isColliding() {
        return this.world.hasBlockCollision(null, this.getBoundingBox(),
            (state, pos) -> state.shouldSuffocate(this.world, pos));
    }

    @Override
    public void setSpriteForAge(SpriteProvider spriteProvider) {
        if (this.collidesWithWorld || this.isColliding()) {
            this.setSprite(spriteProvider.getSprite(this.age / 2, this.maxAge));
        } else {
            this.setSprite(spriteProvider.getSprite(this.age / 2 + this.maxAge / 2, this.maxAge));
        }
    }

    @Environment(EnvType.CLIENT)
    public static class Factory implements ParticleFactory<DefaultParticleType> {
        private final SpriteProvider spriteProvider;

        public Factory(SpriteProvider spriteProvider) {
            this.spriteProvider = spriteProvider;
        }

        @Override
        public Particle createParticle(DefaultParticleType particleType, ClientWorld world, double x, double y, double z, double vx, double vy, double vz) {
            GhostParticle soulParticle = new GhostParticle(world, x, y, z, vx, vy, vz, this.spriteProvider);
            soulParticle.setColorAlpha(1.0F);
            return soulParticle;
        }
    }
}
