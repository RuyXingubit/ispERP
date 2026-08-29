package com.xingubit.isperp.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class UuidCreatorUtilsTest {

    @Test
    @DisplayName("Deve gerar um UUID válido da versão 7")
    void shouldGenerateValidUuidV7() {
        UUID uuid = UuidCreatorUtils.generateUuidV7();
        
        assertNotNull(uuid);
        assertEquals(7, uuid.version(), "O UUID gerado deve ser estritamente da versão 7 (RFC 9562)");
        assertEquals(2, uuid.variant(), "A variante do UUID deve ser RFC 4122 / RFC 9562");
    }

    @Test
    @DisplayName("Deve gerar UUIDs ordenados cronologicamente")
    void shouldGenerateChronologicallyOrderedUuids() throws InterruptedException {
        UUID first = UuidCreatorUtils.generateUuidV7();
        Thread.sleep(5);
        UUID second = UuidCreatorUtils.generateUuidV7();

        assertNotNull(first);
        assertNotNull(second);
        assertNotEquals(first, second);
        assertTrue(first.compareTo(second) < 0, "O primeiro UUIDv7 deve ser menor cronologicamente que o segundo");
    }
}
