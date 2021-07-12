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

import com.mojang.serialization.Codec;
import net.fabricmc.fabric.api.event.registry.FabricRegistryBuilder;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.registry.SimpleRegistry;

import java.util.Objects;

public final class RecordType<T> {
    @SuppressWarnings("unchecked")
    public static final SimpleRegistry<RecordType<?>> REGISTRY =
        FabricRegistryBuilder.createSimple((Class<RecordType<?>>) (Class<?>) RecordType.class, new Identifier("requiem", "record_types")).buildAndRegister();

    public static final RecordType<EntityPointer> ENTITY_POINTER = register(new Identifier("requiem", "entity_ref"), EntityPointer.CODEC);

    public static Identifier getId(RecordType<?> type) {
        return Objects.requireNonNull(REGISTRY.getId(type));
    }

    public Codec<T> getCodec() {
        return codec;
    }

    private final Codec<T> codec;

    private RecordType(Codec<T> codec) {
        this.codec = codec;
    }

    public static <T> RecordType<T> register(Identifier id, Codec<T> codec) {
        return Registry.register(REGISTRY, id, new RecordType<>(codec));
    }
}
