package br.dev.xb.isperp.service;

import br.dev.xb.isperp.dto.ContractTemplateVariableInfo;
import br.dev.xb.isperp.entity.*;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.NumberFormat;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Component
@Slf4j
@SuppressWarnings("null")
public class ContractTemplateEngine {

    private static final NumberFormat CURRENCY_FORMAT = NumberFormat.getCurrencyInstance(Locale.of("pt", "BR"));
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    /**
     * Interpola o template em Markdown substituindo todas as tags dinâmicas.
     */
    public String render(
            String template,
            @Nullable Customer customer,
            @Nullable Company company,
            @Nullable Contract contract,
            @Nullable Plan plan,
            @Nullable ContractSignature signature
    ) {
        if (template == null || template.isBlank()) {
            return "";
        }

        Map<String, String> values = buildVariableMap(customer, company, contract, plan, signature);

        String result = template;
        for (Map.Entry<String, String> entry : values.entrySet()) {
            result = result.replace("{{" + entry.getKey() + "}}", entry.getValue() != null ? entry.getValue() : "");
        }

        return result;
    }

    /**
     * Monta o mapa chave-valor de todas as tags dinâmicas suportadas.
     */
    public Map<String, String> buildVariableMap(
            @Nullable Customer customer,
            @Nullable Company company,
            @Nullable Contract contract,
            @Nullable Plan plan,
            @Nullable ContractSignature signature
    ) {
        Map<String, String> map = new HashMap<>();

        // Cliente
        if (customer != null) {
            map.put("customer.name", customer.getName());
            map.put("customer.cpf_cnpj", formatCpfCnpj(customer.getCpf()));
            map.put("customer.document", customer.getCpf() != null ? customer.getCpf() : "");
            map.put("customer.rg_ie", "Isento / Não Informado");
            map.put("customer.phone", customer.getPhone() != null ? customer.getPhone() : "");
            map.put("customer.email", customer.getEmail() != null ? customer.getEmail() : "");
            map.put("customer.address", customer.getAddress() != null ? customer.getAddress() : "");
            map.put("customer.city", customer.getCity() != null ? customer.getCity() : "");
            map.put("customer.state", customer.getState() != null ? customer.getState() : "");
            map.put("customer.zip_code", customer.getZipCode() != null ? customer.getZipCode() : "");
            map.put("customer.full_address", formatAddress(customer.getAddress(), customer.getCity(), customer.getState(), customer.getZipCode()));
        }

        // Empresa / ISP
        if (company != null) {
            map.put("company.name", company.getName());
            map.put("company.trade_name", company.getName());
            map.put("company.document", formatCpfCnpj(company.getDocument()));
            map.put("company.cnpj", formatCpfCnpj(company.getDocument()));
            map.put("company.address", company.getAddress() != null ? company.getAddress() : "");
            map.put("company.phone", company.getPhone() != null ? company.getPhone() : "");
            map.put("company.email", company.getEmail() != null ? company.getEmail() : "");
            map.put("company.website", company.getWebsite() != null ? company.getWebsite() : "");
            map.put("company.anatel_act", "Ato de Autorização SCM/Anatel nº 1.234/2020");
        }

        // Contrato
        if (contract != null) {
            map.put("contract.number", contract.getContractNumber());
            map.put("contract.date", contract.getCreatedAt() != null ? contract.getCreatedAt().format(DATE_FORMATTER) : OffsetDateTime.now().format(DATE_FORMATTER));
            map.put("contract.monthly_fee", contract.getMonthlyFee() != null ? formatCurrency(contract.getMonthlyFee()) : "");
            map.put("contract.due_day", contract.getDueDay() != null ? String.valueOf(contract.getDueDay()) : "10");
            map.put("contract.installation_address", contract.getInstallationAddress());
            map.put("contract.city", contract.getCity() != null ? contract.getCity() : "");
            map.put("contract.state", contract.getState() != null ? contract.getState() : "");
            map.put("contract.zip_code", contract.getZipCode() != null ? contract.getZipCode() : "");
            map.put("contract.full_installation_address", formatAddress(contract.getInstallationAddress(), contract.getCity(), contract.getState(), contract.getZipCode()));
        }

        // Plano
        if (plan != null) {
            map.put("plan.name", plan.getName());
            map.put("plan.download_speed", plan.getDownloadSpeed() + " Mbps");
            map.put("plan.upload_speed", plan.getUploadSpeed() + " Mbps");
            map.put("plan.price", plan.getPrice() != null ? formatCurrency(plan.getPrice()) : "");
            map.put("plan.sva_included", plan.getSvaIncluded() != null && !plan.getSvaIncluded().isBlank() ? plan.getSvaIncluded() : "Não incluso");
            map.put("plan.loyalty_months", "12");
            map.put("plan.loyalty_penalty", formatCurrency(BigDecimal.valueOf(360.00)));
        }

        // Assinatura & Transação Pix
        if (signature != null) {
            map.put("signature.signed_at", signature.getSignedAt() != null ? signature.getSignedAt().format(DATE_TIME_FORMATTER) : "Pendente");
            map.put("signature.pix_id", signature.getPixEndToEndId() != null ? signature.getPixEndToEndId() : (signature.getPixTxid() != null ? signature.getPixTxid() : ""));
            map.put("signature.payer_name", signature.getPayerName() != null ? signature.getPayerName() : "");
            map.put("signature.payer_document", formatCpfCnpj(signature.getPayerCpfCnpj()));
            map.put("signature.bank_name", signature.getPayerBankName() != null ? signature.getPayerBankName() : "");
            map.put("signature.ip_address", signature.getClientIp() != null ? signature.getClientIp() : "");
            map.put("signature.document_hash", signature.getDocumentSha256Hash() != null ? signature.getDocumentSha256Hash() : "");
        }

        return map;
    }

    /**
     * Calcula o hash criptográfico SHA-256 de um texto.
     */
    public String calculateSha256(String text) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(text.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }

    /**
     * Retorna a lista detalhada de variáveis suportadas para exibição no frontend.
     */
    public List<ContractTemplateVariableInfo> getAvailableVariables() {
        return List.of(
                // Cliente
                ContractTemplateVariableInfo.builder().tag("{{customer.name}}").label("Nome do Cliente").category("CUSTOMER").example("João da Silva").description("Nome completo ou Razão Social").build(),
                ContractTemplateVariableInfo.builder().tag("{{customer.cpf_cnpj}}").label("CPF/CNPJ Formatado").category("CUSTOMER").example("123.456.789-00").description("Documento com máscara").build(),
                ContractTemplateVariableInfo.builder().tag("{{customer.rg_ie}}").label("RG ou IE").category("CUSTOMER").example("12.345.678-9").description("Registro Geral ou Inscrição Estadual").build(),
                ContractTemplateVariableInfo.builder().tag("{{customer.phone}}").label("Telefone").category("CUSTOMER").example("(11) 98765-4321").description("Telefone / WhatsApp de contato").build(),
                ContractTemplateVariableInfo.builder().tag("{{customer.email}}").label("E-mail").category("CUSTOMER").example("cliente@email.com").description("E-mail do titular").build(),
                ContractTemplateVariableInfo.builder().tag("{{customer.full_address}}").label("Endereço Completo do Cliente").category("CUSTOMER").example("Rua das Palmeiras, 120, Apto 101 - Centro, São Paulo/SP - CEP: 01001-000").description("Endereço cadastral completo").build(),

                // Empresa
                ContractTemplateVariableInfo.builder().tag("{{company.name}}").label("Razão Social do Provedor").category("COMPANY").example("Xingubit Telecomunicações Ltda").description("Nome oficial do ISP").build(),
                ContractTemplateVariableInfo.builder().tag("{{company.cnpj}}").label("CNPJ do Provedor").category("COMPANY").example("12.345.678/0001-90").description("CNPJ formatado do ISP").build(),
                ContractTemplateVariableInfo.builder().tag("{{company.address}}").label("Endereço da Sede do ISP").category("COMPANY").example("Av. Paulista, 1000 - Bela Vista, São Paulo/SP").description("Endereço da sede").build(),
                ContractTemplateVariableInfo.builder().tag("{{company.phone}}").label("Telefone do ISP").category("COMPANY").example("(11) 3000-0000").description("Telefone de suporte / SAC").build(),
                ContractTemplateVariableInfo.builder().tag("{{company.anatel_act}}").label("Ato de Autorização Anatel").category("COMPANY").example("Ato SCM/Anatel nº 1.234/2020").description("Autorização regulatória").build(),

                // Contrato
                ContractTemplateVariableInfo.builder().tag("{{contract.number}}").label("Número do Contrato").category("CONTRACT").example("CTR-2026-0042").description("Identificador sequencial do contrato").build(),
                ContractTemplateVariableInfo.builder().tag("{{contract.date}}").label("Data do Contrato").category("CONTRACT").example("31/08/2026").description("Data de geração").build(),
                ContractTemplateVariableInfo.builder().tag("{{contract.monthly_fee}}").label("Valor da Mensalidade").category("CONTRACT").example("R$ 99,90").description("Mensalidade contratada").build(),
                ContractTemplateVariableInfo.builder().tag("{{contract.due_day}}").label("Dia de Vencimento").category("CONTRACT").example("10").description("Dia escolhido para pagamento").build(),
                ContractTemplateVariableInfo.builder().tag("{{contract.full_installation_address}}").label("Endereço de Instalação").category("CONTRACT").example("Rua das Acácias, 45 - Jardim Botânico").description("Ponto de ativação da fibra").build(),

                // Plano
                ContractTemplateVariableInfo.builder().tag("{{plan.name}}").label("Nome do Plano").category("PLAN").example("Fibra Ultra 500 Mega").description("Nome comercial do plano").build(),
                ContractTemplateVariableInfo.builder().tag("{{plan.download_speed}}").label("Velocidade de Download").category("PLAN").example("500 Mbps").description("Banda nominal contratada").build(),
                ContractTemplateVariableInfo.builder().tag("{{plan.upload_speed}}").label("Velocidade de Upload").category("PLAN").example("250 Mbps").description("Taxa de envio contratada").build(),
                ContractTemplateVariableInfo.builder().tag("{{plan.sva_included}}").label("SVAs Inclusos").category("PLAN").example("Paramount+, Deezer").description("Serviços de Valor Adicionado").build(),
                ContractTemplateVariableInfo.builder().tag("{{plan.loyalty_months}}").label("Prazo de Fidelidade").category("PLAN").example("12").description("Meses de permanência mínima").build(),
                ContractTemplateVariableInfo.builder().tag("{{plan.loyalty_penalty}}").label("Multa Rescisória Máxima").category("PLAN").example("R$ 360,00").description("Multa proporcional por quebra").build(),

                // Assinatura
                ContractTemplateVariableInfo.builder().tag("{{signature.signed_at}}").label("Data/Hora da Assinatura").category("SIGNATURE").example("31/08/2026 10:15:22").description("Carimbo de tempo oficial").build(),
                ContractTemplateVariableInfo.builder().tag("{{signature.pix_id}}").label("ID da Transação Pix (BACEN)").category("SIGNATURE").example("E12345678202608311015...").description("End-to-End ID do Banco Central").build(),
                ContractTemplateVariableInfo.builder().tag("{{signature.payer_name}}").label("Nome do Pagador no Banco").category("SIGNATURE").example("João da Silva").description("Nome retornado pela instituição bancária").build(),
                ContractTemplateVariableInfo.builder().tag("{{signature.bank_name}}").label("Banco do Pagador").category("SIGNATURE").example("Banco Nubank S.A.").description("Instituição de pagamento").build(),
                ContractTemplateVariableInfo.builder().tag("{{signature.ip_address}}").label("Endereço IP do Assinante").category("SIGNATURE").example("177.18.29.40").description("IP de conexão no momento do aceite").build(),
                ContractTemplateVariableInfo.builder().tag("{{signature.document_hash}}").label("Hash SHA-256 do Documento").category("SIGNATURE").example("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855").description("Criptografia para prova jurídica de integridade").build()
        );
    }

    private String formatCurrency(BigDecimal value) {
        if (value == null) return "";
        return CURRENCY_FORMAT.format(value);
    }

    private String formatCpfCnpj(@Nullable String doc) {
        if (doc == null) return "";
        String cleaned = doc.replaceAll("[^0-9]", "");
        if (cleaned.length() == 11) {
            return cleaned.substring(0, 3) + "." + cleaned.substring(3, 6) + "." + cleaned.substring(6, 9) + "-" + cleaned.substring(9, 11);
        } else if (cleaned.length() == 14) {
            return cleaned.substring(0, 2) + "." + cleaned.substring(2, 5) + "." + cleaned.substring(5, 8) + "/" + cleaned.substring(8, 12) + "-" + cleaned.substring(12, 14);
        }
        return doc;
    }

    private String formatAddress(@Nullable String address, @Nullable String city, @Nullable String state, @Nullable String zipCode) {
        StringBuilder sb = new StringBuilder();
        if (address != null && !address.isBlank()) sb.append(address);
        if (city != null && !city.isBlank()) {
            if (!sb.isEmpty()) sb.append(" - ");
            sb.append(city);
        }
        if (state != null && !state.isBlank()) {
            sb.append("/").append(state);
        }
        if (zipCode != null && !zipCode.isBlank()) {
            sb.append(" - CEP: ").append(zipCode);
        }
        return sb.toString();
    }
}
