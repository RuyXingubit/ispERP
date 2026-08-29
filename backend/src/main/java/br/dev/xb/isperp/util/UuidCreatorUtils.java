package br.dev.xb.isperp.util;

import com.github.f4b6a3.uuid.UuidCreator;

import java.util.UUID;
import org.springframework.lang.NonNull;

public final class UuidCreatorUtils {

    private UuidCreatorUtils() {
        // Utility class
    }

    /**
     * Generates an RFC 9562 compliant UUID version 7 (time-ordered).
     *
     * @return Time-ordered UUIDv7
     */
    @NonNull
    public static UUID generateUuidV7() {
        return UuidCreator.getTimeOrderedEpoch();
    }
}
