package br.dev.xb.isperp.util;

import com.github.f4b6a3.uuid.UuidCreator;
import org.jspecify.annotations.Nullable;

import java.util.Objects;
import java.util.UUID;

@SuppressWarnings("java:S2583")
public final class UuidCreatorUtils {

    private UuidCreatorUtils() {
        // Utility class
    }

    /**
     * Generates an RFC 9562 compliant UUID version 7 (time-ordered).
     *
     * @return Time-ordered UUIDv7
     */
    public static UUID generateUuidV7() {
        return Objects.requireNonNull(UuidCreator.getTimeOrderedEpoch());
    }

    public static UUID orGenerate(@Nullable UUID id) {
        return id != null ? id : generateUuidV7();
    }
}
