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
package ladysnake.requiem.common.entity;

import ladysnake.requiem.common.particle.WispTrailParticleEffect;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.MovementType;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.network.Packet;
import net.minecraft.network.packet.s2c.play.EntitySpawnS2CPacket;
import net.minecraft.particle.BlockStateParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.tag.BlockTags;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.Objects;
import java.util.Optional;

public class SoulEntity extends Entity {
    public static final byte SOUL_EXPIRED_STATUS = 1;
    public static final double RADIANS_TO_DEGREES = 180 / Math.PI;

    public static final TrackedData<Optional<Vec3d>> NEXT_TARGET = DataTracker.registerData(SoulEntity.class, RequiemTrackedDataHandlers.OPTIONAL_VEC_3D);
    public static final TrackedData<Float> SPEED_MODIFIER = DataTracker.registerData(SoulEntity.class, TrackedDataHandlerRegistry.FLOAT);

    protected int maxAge;
    protected int targetChangeCooldown = 0;
    protected int timeInSolid = -1;

    public SoulEntity(EntityType<? extends SoulEntity> type, World world) {
        super(type, world);
        this.maxAge = 600 + random.nextInt(600);
    }

    public void setMaxAge(int maxAge) {
        this.maxAge = maxAge;
    }

    @Override
    protected void initDataTracker() {
        this.getDataTracker().startTracking(NEXT_TARGET, Optional.empty());
        this.getDataTracker().startTracking(SPEED_MODIFIER, 0f);
    }

    @Override
    public void tick() {
        super.tick();

        if (!this.world.isClient()) {
            if (this.maxAge >= 0 && this.age >= this.maxAge) {
                this.world.sendEntityStatus(this, SOUL_EXPIRED_STATUS);
                this.discard();
            }

            this.targetChangeCooldown -= this.getPos().squaredDistanceTo(this.prevX, this.prevY, this.prevZ) < 0.0125 ? 10 : 1;

            if ((this.world.getTime() % 20 == 0) && (this.getTarget().map(this.getPos()::squaredDistanceTo).orElse(Double.NaN) < 9 || targetChangeCooldown <= 0)) {
                this.updateTarget();
            }

            if (random.nextInt(20) == 0) {
                this.world.playSound(this.getX(), this.getY(), this.getZ(), SoundEvents.PARTICLE_SOUL_ESCAPE, SoundCategory.AMBIENT, 1.0f, 1.5f, true);
            }

            if (!this.getBlockStateAtPos().isAir()) {
                if (timeInSolid > -1) {
                    timeInSolid += 1;
                }
            } else {
                timeInSolid = 0;
            }

            if (timeInSolid > 25) {
                this.kill();
            }
        }

        this.getTarget().ifPresent(target -> {
            Vec3d targetVector = target.subtract(this.getPos());
            double length = targetVector.length();
            targetVector = targetVector.multiply(getSpeedModifier() / length);

            // newVelocity = 0.9 * velocity + 0.1 * targetVector
            Vec3d newVelocity = this.getVelocity().multiply(0.9).add(targetVector.multiply(0.1));
            this.setVelocity(newVelocity);

            float f = (float) Math.sqrt(newVelocity.x * newVelocity.x + newVelocity.z * newVelocity.z);
            this.setRotation(
                (float) (MathHelper.atan2(newVelocity.x, newVelocity.z) * RADIANS_TO_DEGREES),
                (float) (MathHelper.atan2(newVelocity.y, f) * RADIANS_TO_DEGREES)
            );

            if (!Objects.equals(new BlockPos(target), this.getBlockPos())) {
                this.move(MovementType.SELF, this.getVelocity());
            }
        });

        if (this.world.isClient()) {
            for (int i = 0; i < 10 * this.getSpeedModifier(); i++) {
                this.spawnTrailParticle();
            }
        }
    }

    protected void spawnTrailParticle() {
        if (this.getBlockStateAtPos().isIn(BlockTags.SOUL_FIRE_BASE_BLOCKS)) {
            this.world.addParticle(ParticleTypes.SOUL, this.getX() + random.nextGaussian() / 10, this.getY() + random.nextGaussian() / 10, this.getZ() + random.nextGaussian() / 10, random.nextGaussian() / 20, random.nextGaussian() / 20, random.nextGaussian() / 20);
        } else {
            this.world.addParticle(new WispTrailParticleEffect(1.0f, 1.0f, 1.0f, -0.1f, -0.01f, 0.0f), this.getX() + random.nextGaussian() / 15, this.getY() + random.nextGaussian() / 15, this.getZ() + random.nextGaussian() / 15, 0, 0.2d, 0);
        }
    }

    private Optional<Vec3d> getTarget() {
        return this.getDataTracker().get(NEXT_TARGET);
    }

    private void setTarget(Vec3d target) {
        this.getDataTracker().set(NEXT_TARGET, Optional.of(target));
    }

    private float getSpeedModifier() {
        return this.getDataTracker().get(SPEED_MODIFIER);
    }

    private void setSpeedModifier(float speedModifier) {
        this.getDataTracker().set(SPEED_MODIFIER, speedModifier);
    }

    @Override
    public void handleStatus(byte status) {
        switch (status) {
            case SOUL_EXPIRED_STATUS -> {
                for (int i = 0; i < 25; i++) {
                    this.world.addParticle(new WispTrailParticleEffect(1.0f, 1.0f, 1.0f, -0.1f, -0.01f, 0.0f), this.getX() + random.nextGaussian() / 15, this.getY() + random.nextGaussian() / 15, this.getZ() + random.nextGaussian() / 15, 0, 0.2d, 0);
                    this.world.addParticle(new BlockStateParticleEffect(ParticleTypes.BLOCK, Blocks.SOUL_SAND.getDefaultState()), this.getX() + random.nextGaussian() / 10, this.getY() + random.nextGaussian() / 10, this.getZ() + random.nextGaussian() / 10, random.nextGaussian() / 20, random.nextGaussian() / 20, random.nextGaussian() / 20);
                }
                this.world.playSound(this.getX(), this.getY(), this.getZ(), SoundEvents.PARTICLE_SOUL_ESCAPE, SoundCategory.AMBIENT, 1.0f, 1.5f, true);
                this.world.playSound(this.getX(), this.getY(), this.getZ(), SoundEvents.BLOCK_SOUL_SAND_BREAK, SoundCategory.AMBIENT, 1.0f, 1.0f, true);
            }
            default -> super.handleStatus(status);
        }
    }

    protected void updateTarget() {
        Vec3d newTarget = this.selectNextTarget();
        this.setTarget(newTarget);

        BlockPos targetPos = new BlockPos(newTarget);
        if (this.world.getBlockState(targetPos).isFullCube(world, targetPos) && !this.world.getBlockState(targetPos).isIn(BlockTags.SOUL_FIRE_BASE_BLOCKS)) {
            this.targetChangeCooldown = 0;
            return;
        }

        this.setSpeedModifier(0.1f + Math.max(0, random.nextFloat() - 0.1f));
        this.targetChangeCooldown = random.nextInt() % (int) (100 / getSpeedModifier());
    }

    protected Vec3d selectNextTarget() {
        return this.getPos().add(
            random.nextGaussian() * 10,
            random.nextGaussian() * 10,
            random.nextGaussian() * 10
        );
    }

    @Override
    protected void readCustomDataFromNbt(NbtCompound nbt) {
        if (nbt.contains("max_age", NbtElement.INT_TYPE)) {
            this.maxAge = nbt.getInt("max_age");
        }
    }

    @Override
    protected void writeCustomDataToNbt(NbtCompound nbt) {
        nbt.putInt("max_age", this.maxAge);
    }

    @Override
    public Packet<?> createSpawnPacket() {
        return new EntitySpawnS2CPacket(this);
    }
}
