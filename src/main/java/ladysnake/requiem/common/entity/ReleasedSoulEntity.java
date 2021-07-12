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

import com.mojang.datafixers.util.Unit;
import ladysnake.requiem.api.v1.record.EntityPointer;
import ladysnake.requiem.api.v1.record.GlobalRecord;
import ladysnake.requiem.api.v1.record.GlobalRecordKeeper;
import ladysnake.requiem.api.v1.record.RecordType;
import ladysnake.requiem.common.RequiemRecordTypes;
import ladysnake.requiem.common.item.RequiemItems;
import ladysnake.requiem.common.particle.WispTrailParticleEffect;
import ladysnake.requiem.common.sound.RequiemSoundEvents;
import ladysnake.requiem.core.entity.EntityAiToggle;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.UUID;

public class ReleasedSoulEntity extends SoulEntity {
    private static final byte TELEPORT_AWAY_STATUS = 2;
    private static final byte MERGE_WITH_BODY_STATUS = 3;

    public static final byte BODY_MISSING = -1;
    public static final byte BODY_FOUND = 0;
    public static final byte BODY_ISEKAI = 1;

    public static final TrackedData<Byte> BODY_STATUS = DataTracker.registerData(ReleasedSoulEntity.class, TrackedDataHandlerRegistry.BYTE);
    private int targetChanges = 0;
    private @Nullable UUID ownerRecord;
    private int ticksUntilDespawn = 60;

    public ReleasedSoulEntity(EntityType<? extends ReleasedSoulEntity> type, World world) {
        this(type, world, null);
    }

    public ReleasedSoulEntity(EntityType<? extends SoulEntity> type, World world, @Nullable UUID ownerRecord) {
        super(type, world);
        this.ownerRecord = ownerRecord;
        this.noClip = true;
        this.targetChangeCooldown = 40;
    }

    @Override
    protected void initDataTracker() {
        super.initDataTracker();
        this.getDataTracker().startTracking(BODY_STATUS, BODY_FOUND);
    }

    @Override
    public void tick() {
        if (!this.world.isClient()) {
            this.setBodyStatus(
                this.getTarget()
                    .map(ptr -> ptr.resolve(((ServerWorld) this.world).getServer())
                        .filter(e -> e.world == this.world)
                        .filter(e -> e.distanceTo(this) < 100)
                        .isPresent() ? BODY_FOUND : BODY_ISEKAI)
                    .orElse(BODY_MISSING)
            );
        }
        if (this.getBodyStatus() != BODY_FOUND) {
            this.ticksUntilDespawn--;
            if (this.ticksUntilDespawn <= 0) {
                if (this.getBodyStatus() == BODY_ISEKAI) {
                    // Will be retrieved later
                    this.getRecord().ifPresent(data -> data.put(RequiemRecordTypes.RELEASED_SOUL, Unit.INSTANCE));
                    this.world.sendEntityStatus(this, TELEPORT_AWAY_STATUS);
                } else {
                    // RIP
                    this.getRecord().ifPresent(GlobalRecord::invalidate);
                    this.world.sendEntityStatus(this, SOUL_EXPIRED_STATUS);
                }
                this.discard();
            }
        } else if (!this.world.isClient()) {
            this.getCollidingBody().ifPresent(body -> {
                this.world.sendEntityStatus(this, SOUL_EXPIRED_STATUS);
                EntityAiToggle.KEY.maybeGet(body).ifPresent(t -> t.toggleAi(Registry.ITEM.getId(RequiemItems.EMPTY_SOUL_VESSEL), false, false));
                this.discard();
            });
        }
        super.tick();
    }

    private Optional<Entity> getCollidingBody() {
        return this.getTarget()
            .flatMap(ptr -> ptr.resolve(((ServerWorld)this.world).getServer()))
            .filter(e -> e.getBoundingBox().intersects(this.getBoundingBox()));
    }

    private void setBodyStatus(byte value) {
        this.getDataTracker().set(BODY_STATUS, value);
    }

    private byte getBodyStatus() {
        return this.getDataTracker().get(BODY_STATUS);
    }

    private Optional<EntityPointer> getTarget() {
        return getRecord()
            .flatMap(record -> record.get(RecordType.ENTITY_POINTER));
    }

    @NotNull
    private Optional<GlobalRecord> getRecord() {
        return Optional.ofNullable(ownerRecord)
            .flatMap(GlobalRecordKeeper.get(this.world)::getRecord);
    }

    @Override
    protected void spawnTrailParticle() {
        switch (this.getBodyStatus()) {
            case BODY_ISEKAI -> this.world.addParticle(ParticleTypes.PORTAL, this.getParticleX(0.5D), this.getRandomBodyY() - 0.25D, this.getParticleZ(0.5D), (this.random.nextDouble() - 0.5D) * 2.0D, -this.random.nextDouble(), (this.random.nextDouble() - 0.5D) * 2.0D);
            case BODY_MISSING -> world.addParticle(ParticleTypes.SMOKE, this.getX() + random.nextDouble() / 5.0D, this.getY() + random.nextDouble(), this.getZ() + random.nextDouble() / 5.0D, 0.0D, 0.0D, 0.0D);
            default -> super.spawnTrailParticle();
        }
    }

    @Override
    protected Vec3d selectNextTarget() {
        this.targetChanges++;

        return this.getTarget().filter(ptr -> ptr.world() == this.world.getRegistryKey()).map(target -> {
            double distanceToTarget = this.getPos().distanceTo(target.pos());
            final double maxStepDistance = 15.0;
            double scuffedDistanceToTarget = Math.min(maxStepDistance, distanceToTarget);
            Vec3d towardsTarget = target.pos().subtract(this.getPos()).normalize().multiply(scuffedDistanceToTarget);
            assert targetChanges > 0;
            double fuzzyFactor = (scuffedDistanceToTarget * 1.5) / this.targetChanges;
            Vec3d fuzzyTarget = towardsTarget.multiply(
                Math.abs(1 + random.nextGaussian() * fuzzyFactor),
                Math.abs(1 + random.nextGaussian() * fuzzyFactor),
                Math.abs(1 + random.nextGaussian() * fuzzyFactor)
            );
            return this.getPos().add(fuzzyTarget);
        }).orElseGet(this::getPos);
    }

    @Override
    public void handleStatus(byte status) {
        switch (status) {
            case TELEPORT_AWAY_STATUS -> {
                this.playSound(RequiemSoundEvents.ENTITY_SOUL_TELEPORT, 1, 1);
                for(double angle = 0.0D; angle < Math.PI * 2; angle += Math.PI / 20.0) {
                    this.world.addParticle(ParticleTypes.PORTAL, this.getX() + Math.cos(angle) * 5.0D, this.getY() - 0.4D, this.getZ() + Math.sin(angle) * 5.0D, Math.cos(angle) * -5.0D, 0.0D, Math.sin(angle) * -5.0D);
                    this.world.addParticle(ParticleTypes.PORTAL, this.getX() + Math.cos(angle) * 5.0D, this.getY() - 0.4D, this.getZ() + Math.sin(angle) * 5.0D, Math.cos(angle) * -7.0D, 0.0D, Math.sin(angle) * -7.0D);
                }
            }
            case MERGE_WITH_BODY_STATUS -> {
                this.playSound(SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME, 1, 1);
                for (int i = 0; i < 25; i++) {
                    this.world.addParticle(new WispTrailParticleEffect(1.0f, 1.0f, 1.0f, -0.1f, -0.01f, 0.0f), this.getX() + random.nextGaussian() / 15, this.getY() + random.nextGaussian() / 15, this.getZ() + random.nextGaussian() / 15, 0, 0.2d, 0);
                }
            }
            default -> super.handleStatus(status);
        }
    }

    @Override
    protected void readCustomDataFromNbt(NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);
        if (nbt.containsUuid("owner_record")) {
            this.ownerRecord = nbt.getUuid("owner_record");
        }
    }

    @Override
    protected void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);
        if (this.ownerRecord != null) {
            nbt.putUuid("owner_record", this.ownerRecord);
        }
    }
}
