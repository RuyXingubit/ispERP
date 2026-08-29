package br.dev.xb.isperp.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

@SuppressWarnings("null")
class CpfValidatorTest {

    @Test
    @DisplayName("Deve validar CPF válido com formatação e sem formatação")
    void shouldValidateValidCpf() {
        // CPF válido matemático
        assertTrue(CpfValidator.isValid("52998224725"));
        assertTrue(CpfValidator.isValid("529.982.247-25"));
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "11111111111", "22222222222", "00000000000",
        "12345678900", "123", "", "   "
    })
    @DisplayName("Deve rejeitar CPFs inválidos ou repetidos")
    void shouldRejectInvalidCpfs(String invalidCpf) {
        assertFalse(CpfValidator.isValid(invalidCpf));
    }

    @Test
    @DisplayName("Deve formatar e limpar CPF corretamente")
    void shouldFormatAndCleanCpf() {
        String raw = "52998224725";
        String formatted = CpfValidator.format(raw);
        assertEquals("529.982.247-25", formatted);

        String cleaned = CpfValidator.clean(formatted);
        assertEquals("52998224725", cleaned);
    }
}
