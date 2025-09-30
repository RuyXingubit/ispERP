package com.xingubit.isperp.util;

/**
 * Utilitário para validação e formatação de CPF
 */
public class CpfValidator {

    /**
     * Valida se um CPF é válido
     * @param cpf CPF a ser validado (pode conter pontos e traços)
     * @return true se o CPF for válido, false caso contrário
     */
    public static boolean isValid(String cpf) {
        if (cpf == null || cpf.trim().isEmpty()) {
            return false;
        }

        // Remove caracteres não numéricos
        String cleanCpf = cpf.replaceAll("[^\\d]", "");

        // Verifica se tem 11 dígitos
        if (cleanCpf.length() != 11) {
            return false;
        }

        // Verifica se todos os dígitos são iguais (CPF inválido)
        if (cleanCpf.matches("(\\d)\\1{10}")) {
            return false;
        }

        // Calcula o primeiro dígito verificador
        int sum = 0;
        for (int i = 0; i < 9; i++) {
            sum += Character.getNumericValue(cleanCpf.charAt(i)) * (10 - i);
        }
        int remainder = sum % 11;
        int firstDigit = remainder < 2 ? 0 : 11 - remainder;

        // Verifica o primeiro dígito
        if (Character.getNumericValue(cleanCpf.charAt(9)) != firstDigit) {
            return false;
        }

        // Calcula o segundo dígito verificador
        sum = 0;
        for (int i = 0; i < 10; i++) {
            sum += Character.getNumericValue(cleanCpf.charAt(i)) * (11 - i);
        }
        remainder = sum % 11;
        int secondDigit = remainder < 2 ? 0 : 11 - remainder;

        // Verifica o segundo dígito
        return Character.getNumericValue(cleanCpf.charAt(10)) == secondDigit;
    }

    /**
     * Formata um CPF adicionando pontos e traço
     * @param cpf CPF a ser formatado
     * @return CPF formatado (xxx.xxx.xxx-xx)
     */
    public static String format(String cpf) {
        if (cpf == null || cpf.trim().isEmpty()) {
            return cpf;
        }

        // Remove caracteres não numéricos
        String cleanCpf = cpf.replaceAll("[^\\d]", "");

        // Aplica a máscara se tiver 11 dígitos
        if (cleanCpf.length() == 11) {
            return cleanCpf.replaceAll("(\\d{3})(\\d{3})(\\d{3})(\\d{2})", "$1.$2.$3-$4");
        }

        return cpf; // Retorna o original se não tiver 11 dígitos
    }

    /**
     * Remove a formatação do CPF, deixando apenas números
     * @param cpf CPF formatado
     * @return CPF apenas com números
     */
    public static String clean(String cpf) {
        if (cpf == null) {
            return null;
        }
        return cpf.replaceAll("[^\\d]", "");
    }
}