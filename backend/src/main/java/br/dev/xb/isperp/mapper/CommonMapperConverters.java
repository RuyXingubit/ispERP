package br.dev.xb.isperp.mapper;

import org.jspecify.annotations.Nullable;

import java.math.BigDecimal;
import java.net.URI;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;

@SuppressWarnings("java:S2583")
public interface CommonMapperConverters {

    @Nullable
    default BigDecimal doubleToBigDecimal(@Nullable Double value) {
        return value != null ? BigDecimal.valueOf(value) : null;
    }

    @Nullable
    default Double bigDecimalToDouble(@Nullable BigDecimal value) {
        return value != null ? value.doubleValue() : null;
    }

    @Nullable
    default URI stringToUri(@Nullable String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return URI.create(value);
        } catch (Exception e) {
            return null;
        }
    }

    @Nullable
    default String uriToString(@Nullable URI value) {
        return value != null ? value.toString() : null;
    }

    @Nullable
    default OffsetDateTime localDateTimeToOffsetDateTime(@Nullable LocalDateTime localDateTime) {
        if (localDateTime == null) {
            return null;
        }
        return localDateTime.atZone(ZoneId.systemDefault()).toOffsetDateTime();
    }

    @Nullable
    default LocalDateTime offsetDateTimeToLocalDateTime(@Nullable OffsetDateTime offsetDateTime) {
        if (offsetDateTime == null) {
            return null;
        }
        return offsetDateTime.toLocalDateTime();
    }
}
