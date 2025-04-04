package org.oryxel.viabedrockutility.payload.impl.entity;

import lombok.Getter;
import org.oryxel.viabedrockutility.enums.bedrock.ActorFlags;
import org.oryxel.viabedrockutility.payload.BasePayload;

import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;
import java.util.function.ToIntFunction;

@Getter
public class ModelRequestPayload extends BasePayload {
    private final String identifier;
    private final UUID uuid;
    private final EntityData entityData;

    public ModelRequestPayload(String identifier, long bitmask, Integer variant, Integer mark_variant, UUID uuid) {
        this.identifier = identifier;
        this.uuid = uuid;

        this.entityData = new EntityData(getEnumSetFromBitmask(ActorFlags.class, bitmask, ActorFlags::getValue), variant, mark_variant);
    }

    public record EntityData(Set<ActorFlags> flags, Integer mark_variant, Integer variant) {
    }

    public static <T extends Enum<T>> Set<T> getEnumSetFromBitmask(final Class<T> enumClass, final long bitmask, final ToIntFunction<T> bitGetter) {
        final EnumSet<T> set = EnumSet.noneOf(enumClass);
        for (T constant : enumClass.getEnumConstants()) {
            final int bit = bitGetter.applyAsInt(constant);
            if (bit >= 0 && bit < Long.SIZE && (bitmask & (1L << bit)) != 0) {
                set.add(constant);
            }
        }
        return set;
    }
}
