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
package ladysnake.requiem.core.record;

import ladysnake.requiem.api.v1.record.GlobalRecord;
import ladysnake.requiem.api.v1.record.GlobalRecordKeeper;
import ladysnake.requiem.api.v1.record.RecordType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtOps;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

public class GlobalRecordImpl implements GlobalRecord {
    public static final String ANCHOR_UUID_NBT = "uuid";

    protected final GlobalRecordKeeper manager;
    private final int id;
    private final UUID uuid;
    private final Map<RecordType<?>, Object> data;
    private final Map<Identifier, Consumer<GlobalRecord>> tickingActions;
    private boolean invalid;

    public GlobalRecordImpl(GlobalRecordKeeper manager, UUID uuid, int id) {
        this.manager = manager;
        this.id = id;
        this.uuid = uuid;
        this.data = new LinkedHashMap<>();
        this.tickingActions = new LinkedHashMap<>();
    }

    GlobalRecordImpl(GlobalRecordKeeper manager, UUID uuid, int id, Map<RecordType<?>, Object> data) {
        this(manager, uuid, id);
        this.data.putAll(data);
    }

    @Override
    public int getId() {
        return this.id;
    }

    @Override
    public UUID getUuid() {
        return this.uuid;
    }

    @Override
    public <T> void put(RecordType<T> type, @Nullable T data) {
        if (data == null) this.data.remove(type);
        else this.data.put(type, data);
    }

    @Override
    public <T> Optional<T> get(RecordType<T> type) {
        @SuppressWarnings("unchecked") T ret = (T) this.data.get(type);
        return Optional.ofNullable(ret);
    }

    @Override
    public void addTickingAction(Identifier actionId, Consumer<GlobalRecord> action) {
        this.tickingActions.put(actionId, action);
    }

    @Override
    public void removeTickingAction(Identifier actionId) {
        this.tickingActions.remove(actionId);
    }

    @Override
    public void update() {
        for (Consumer<GlobalRecord> runnable : this.tickingActions.values()) {
            runnable.accept(this);
        }
    }

    @Override
    public void invalidate() {
        this.invalid = true;
    }

    @Override
    public boolean isInvalid() {
        return this.invalid;
    }

    @Override
    public NbtCompound toTag(NbtCompound tag) {
        tag.putUuid(ANCHOR_UUID_NBT, this.getUuid());
        NbtCompound data = new NbtCompound();
        for (var entry : this.data.keySet()) writeToTag(data, entry);
        tag.put("data", data);
        return tag;
    }

    private <U> void writeToTag(NbtCompound tag, RecordType<U> type) {
        tag.put(RecordType.getId(type).toString(), this.get(type).flatMap(v -> type.getCodec().encodeStart(NbtOps.INSTANCE, v).result()).orElseThrow());
    }
}
