package br.dev.xb.isperp.util;

import java.text.Normalizer;
import java.util.Locale;

public class UsernameGeneratorUtils {

    /**
     * Gera login baseado no primeiro e último nome do cliente.
     * Exemplo: "Ruy Barbosa Borges França" -> "ruyfranca"
     *
     * @param fullName Nome completo do cliente
     * @return Username normalizado em minúsculas
     */
    public static String generateUsername(String fullName) {
        if (fullName == null || fullName.trim().isEmpty()) {
            return "cliente" + System.currentTimeMillis() % 10000;
        }

        String normalized = removeAccents(fullName.trim().toLowerCase(Locale.ROOT));
        String[] parts = normalized.split("\\s+");

        if (parts.length == 1) {
            return parts[0].replaceAll("[^a-z0-9]", "");
        }

        String firstName = parts[0].replaceAll("[^a-z0-9]", "");
        String lastName = parts[parts.length - 1].replaceAll("[^a-z0-9]", "");

        return firstName + lastName;
    }

    /**
     * Gera senha inicial padrão baseada no último nome do cliente.
     * Exemplo: "Ruy Barbosa Borges França" -> "franca"
     *
     * @param fullName Nome completo do cliente
     * @return Senha inicial em minúsculas
     */
    public static String generateInitialPassword(String fullName) {
        if (fullName == null || fullName.trim().isEmpty()) {
            return "mudar123";
        }

        String normalized = removeAccents(fullName.trim().toLowerCase(Locale.ROOT));
        String[] parts = normalized.split("\\s+");

        String lastName = parts[parts.length - 1].replaceAll("[^a-z0-9]", "");
        return lastName.isEmpty() ? "mudar123" : lastName;
    }

    private static String removeAccents(String text) {
        String nfd = Normalizer.normalize(text, Normalizer.Form.NFD);
        return nfd.replaceAll("[\\p{InCombiningDiacriticalMarks}]", "");
    }
}
