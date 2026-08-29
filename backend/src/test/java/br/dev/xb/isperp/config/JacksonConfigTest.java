package br.dev.xb.isperp.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SuppressWarnings("null")
class JacksonConfigTest {

    private final JacksonConfig jacksonConfig = new JacksonConfig();

    @Test
    @DisplayName("Deve instanciar ObjectMapper com suporte a JavaTimeModule e sem timestamps numéricos")
    void shouldConfigureObjectMapperProperly() throws Exception {
        ObjectMapper mapper = jacksonConfig.objectMapper();
        assertThat(mapper).isNotNull();

        TestPayload payload = new TestPayload(
                UUID.fromString("018f9d6c-66a7-7a54-b4a1-0242ac120002"),
                "ispERP",
                LocalDateTime.of(2026, 8, 29, 14, 30, 0),
                Instant.parse("2026-08-29T14:30:00Z")
        );

        String json = mapper.writeValueAsString(payload);

        assertThat(json).contains("\"name\":\"ispERP\"");
        assertThat(json).contains("\"id\":\"018f9d6c-66a7-7a54-b4a1-0242ac120002\"");
        assertThat(json).contains("2026-08-29T14:30:00");

        TestPayload deserialized = mapper.readValue(json, TestPayload.class);
        assertThat(deserialized.id()).isEqualTo(payload.id());
        assertThat(deserialized.name()).isEqualTo(payload.name());
        assertThat(deserialized.localDateTime()).isEqualTo(payload.localDateTime());
    }

    private record TestPayload(UUID id, String name, LocalDateTime localDateTime, Instant instant) {}
}
