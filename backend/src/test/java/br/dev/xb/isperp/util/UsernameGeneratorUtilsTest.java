package br.dev.xb.isperp.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@SuppressWarnings("null")
class UsernameGeneratorUtilsTest {

    @Test
    @DisplayName("Deve gerar username e senha inicial a partir de nome com múltiplos sobrenomes e acentuação")
    void shouldGenerateUsernameFromFullNameWithAccents() {
        String fullName = "Ruy Barbosa Borges França";

        String username = UsernameGeneratorUtils.generateUsername(fullName);
        String password = UsernameGeneratorUtils.generateInitialPassword(fullName);

        assertEquals("ruyfranca", username);
        assertEquals("franca", password);
    }

    @Test
    @DisplayName("Deve gerar username e senha para nome simples")
    void shouldGenerateUsernameForSimpleName() {
        String fullName = "João Silva";

        String username = UsernameGeneratorUtils.generateUsername(fullName);
        String password = UsernameGeneratorUtils.generateInitialPassword(fullName);

        assertEquals("joaosilva", username);
        assertEquals("silva", password);
    }

    @Test
    @DisplayName("Deve lidar com nome de palavra única")
    void shouldHandleSingleWordName() {
        String fullName = "Admin";

        String username = UsernameGeneratorUtils.generateUsername(fullName);
        String password = UsernameGeneratorUtils.generateInitialPassword(fullName);

        assertEquals("admin", username);
        assertEquals("admin", password);
    }
}
