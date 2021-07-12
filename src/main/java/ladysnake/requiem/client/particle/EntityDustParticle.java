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

import com.mojang.blaze3d.systems.RenderSystem;
import ladysnake.requiem.common.particle.RequiemEntityParticleEffect;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.particle.BillboardParticle;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleFactory;
import net.minecraft.client.particle.ParticleTextureSheet;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.texture.TextureManager;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

@Environment(EnvType.CLIENT)
public class EntityDustParticle extends BillboardParticle {
    private final ParticleTextureSheet sheet;
    private final Entity target;
    private final Set<BlockPos> exploredBlocks = new LinkedHashSet<>();
    private final float sampleU;
    private final float sampleV;
    private @Nullable Vec3d nextStep;

    public EntityDustParticle(ClientWorld world, double x, double y, double z, double velocityX, double velocityY, double velocityZ, Entity src, Entity target) {
        super(world, x, y, z, velocityX, velocityY, velocityZ);
        this.sheet = TextureSheet.get(getTexture(src));
        this.target = target;
        this.gravityStrength = 1.0F;
        this.colorRed = 0.6F;
        this.colorGreen = 0.6F;
        this.colorBlue = 0.6F;

        this.scale /= 2.0F;
        this.sampleU = this.random.nextFloat() * 31.0F;
        this.sampleV = this.random.nextFloat() * 31.0F;
    }

    private static <E extends Entity> Identifier getTexture(E src) {
        EntityRenderer<? super E> renderer = MinecraftClient.getInstance().getEntityRenderDispatcher().getRenderer(src);
        return renderer.getTexture(src);
    }

    @Override
    public void tick() {
        if (this.nextStep == null || this.reachedNextStep(this.nextStep)) {
            Vec3d targetPos = this.getTargetPos();
            double distanceToTarget = targetPos.squaredDistanceTo(this.x, this.y, this.z);
            if (distanceToTarget < 0.1) {
                this.markDead();
                return;
            }
            this.nextStep = this.findNextStep(targetPos, distanceToTarget);
        }
        this.moveTowardsGoal(this.nextStep);
        super.tick();
    }

    private boolean reachedNextStep(Vec3d nextStep) {
        if (nextStep.squaredDistanceTo(this.x, this.y, this.z) < 0.1) {
            this.exploredBlocks.add(new BlockPos(nextStep));
            return true;
        }
        return false;
    }

    private void moveTowardsGoal(Vec3d nextStep) {
        Vec3d desiredDir = nextStep.subtract(this.x, this.y, this.z).normalize();
        this.setVelocity(
            MathHelper.lerp(0.4, this.velocityX, desiredDir.x*0.7),
            MathHelper.lerp(0.4, this.velocityY, desiredDir.y*0.7),
            MathHelper.lerp(0.4, this.velocityZ, desiredDir.z*0.7)
        );
    }

    private Vec3d getTargetPos() {
        return this.target.getEyePos().subtract(0, 0.2, 0);
    }

    private Vec3d findNextStep(Vec3d targetPos, double distanceToTarget) {
        if (distanceToTarget < 1) {
            return targetPos;
        } else {
            BlockPos nextPos = findNextPos();
            return swizzle(nextPos);
        }
    }

    @NotNull
    private Vec3d swizzle(BlockPos nextPos) {
        return new Vec3d(
            nextPos.getX() + world.random.nextFloat() - 0.5,
            nextPos.getY() + world.random.nextFloat() - 0.5,
            nextPos.getZ() + world.random.nextFloat() - 0.5
        );
    }

    private BlockPos findNextPos() {
        Vec3d eyePos = this.target.getEyePos();
        BlockPos best = BlockPos.ORIGIN;
        double bestDistance = Integer.MAX_VALUE;
        for (BlockPos candidate : BlockPos.iterate(
            MathHelper.floor(this.x) - 1,
            MathHelper.floor(this.y) - 1,
            MathHelper.floor(this.z) - 1,
            MathHelper.floor(this.x) + 1,
            MathHelper.floor(this.y) + 1,
            MathHelper.floor(this.z) + 1
        )) {
            if (!this.exploredBlocks.contains(candidate) && !this.world.getBlockState(candidate).isFullCube(this.world, candidate)) {
                double candidateDistance = candidate.getSquaredDistance(eyePos.x, eyePos.y, eyePos.z, false);
                if (candidateDistance < bestDistance) {
                    bestDistance = candidateDistance;
                    best = candidate.toImmutable();
                }
            }
        }
        return best;
    }

    @Override
    public ParticleTextureSheet getType() {
        return this.sheet;
    }

    @Override
    protected float getMinU() {
        return (this.sampleU + 1) * 4.0f / 128.0f;
    }

    @Override
    protected float getMaxU() {
        return this.sampleU * 4.0f / 128.0f;
    }

    @Override
    protected float getMinV() {
        return (this.sampleV + 1) * 4.0f / 128.0f;
    }

    @Override
    protected float getMaxV() {
        return this.sampleV * 4.0f / 128.0f;
    }

    @Environment(EnvType.CLIENT)
    public static class Factory implements ParticleFactory<RequiemEntityParticleEffect> {
        @Override
        public Particle createParticle(RequiemEntityParticleEffect fx, ClientWorld world, double x, double y, double z, double vx, double vy, double vz) {
            int srcId = fx.getSourceEntityId();
            int entityId = fx.getTargetEntityId();
            Entity src = world.getEntityById(srcId);
            Entity entity = world.getEntityById(entityId);
            return src != null && entity != null ? new EntityDustParticle(world, x, y, z, vx, vy, vz, src, entity) : null;
        }
    }

    public record TextureSheet(Identifier texture) implements ParticleTextureSheet {
        private static final Map<Identifier, ParticleTextureSheet> CACHE = new HashMap<>();

        static ParticleTextureSheet get(Identifier texture) {
            return CACHE.computeIfAbsent(texture, TextureSheet::new);
        }

        @Override
        public void begin(BufferBuilder bufferBuilder, TextureManager textureManager) {
            RenderSystem.disableBlend();
            RenderSystem.depthMask(true);
            RenderSystem.setShader(GameRenderer::getParticleShader);
            RenderSystem.setShaderTexture(0, texture);
            bufferBuilder.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR_LIGHT);
        }

        @Override
        public void draw(Tessellator tessellator) {
            tessellator.draw();
        }

        @Override
        public String toString() {
            return "EntityDustParticleTextureSheet[%s]".formatted(texture);
        }
    }
}
