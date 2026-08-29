package br.dev.xb.isperp.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * Validador customizado para CPF
 */
public class CpfValidator implements ConstraintValidator<ValidCpf, String> {

    @Override
    public void initialize(ValidCpf constraintAnnotation) {
        // Inicialização se necessária
    }

    @Override
    public boolean isValid(String cpf, ConstraintValidatorContext context) {
        if (cpf == null || cpf.trim().isEmpty()) {
            return true; // Deixa a validação @NotBlank cuidar de campos obrigatórios
        }

        return br.dev.xb.isperp.util.CpfValidator.isValid(cpf);
    }
}