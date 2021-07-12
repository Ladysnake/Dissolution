/*
 * Requiem
 * Copyright (C) 2017-2021 Ladysnake
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; If not, see <https://www.gnu.org/licenses>.
 */
package ladysnake.requiem.api.v1.record;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * A {@link GlobalRecord} represents a serializable collection of data accessible from anywhere in the world.
 * Global records are tracked regardless of distance and loaded chunks.
 */
public interface GlobalRecord {
    /**
     * Returns the constant shorter ID that uniquely identifies the record
     * within its {@link GlobalRecordKeeper}. This ID may change whenever the record is
     * loaded from disk and may be reused.
     *
     * @return the constant short ID for this record
     */
    int getId();

    /**
     * Returns the constant longer UUID that uniquely identifies the record
     * within its {@link GlobalRecordKeeper}. This ID will not change whenever the record is
     * loaded from disk and may not be reused.
     *
     * @return the constant UUID of the anchor
     */
    UUID getUuid();

    <T> void put(RecordType<T> type, @Nullable T data);

    <T> Optional<T> get(RecordType<T> type);

    void addTickingAction(Identifier actionId, Consumer<GlobalRecord> action);

    void removeTickingAction(Identifier actionId);

    void update();

    boolean isInvalid();

    void invalidate();

    NbtCompound toTag(NbtCompound tag);

}
