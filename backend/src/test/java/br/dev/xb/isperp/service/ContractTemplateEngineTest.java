package br.dev.xb.isperp.service;

import br.dev.xb.isperp.dto.ContractTemplateVariableInfo;
import br.dev.xb.isperp.entity.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ContractTemplateEngineTest {

    private ContractTemplateEngine engine;

    @BeforeEach
    void setUp() {
        engine = new ContractTemplateEngine();
    }

    @Test
    @DisplayName("Deve substituir corretamente tags dinâmicas de cliente, empresa, plano e contrato")
    void testRenderTemplateWithAllVariables() {
        Customer customer = Customer.builder()
                .id(UUID.randomUUID())
                .name("Maria de Souza")
                .cpf("12345678901")
                .phone("(31) 98888-7777")
                .email("maria@exemplo.com.br")
                .address("Rua das Flores, 120")
                .city("Belo Horizonte")
                .state("MG")
                .zipCode("30100-000")
                .build();

        Company company = Company.builder()
                .id(UUID.randomUUID())
                .name("Xingubit Telecom Ltda")
                .document("12345678000190")
                .address("Av. Afonso Pena, 500")
                .phone("(31) 3333-0000")
                .email("contato@xingubit.com.br")
                .build();

        Plan plan = Plan.builder()
                .id(UUID.randomUUID())
                .name("Fibra 500 Mega Turbo")
                .downloadSpeed(500)
                .uploadSpeed(250)
                .price(BigDecimal.valueOf(99.90))
                .svaIncluded("Paramount+, Deezer")
                .build();

        Contract contract = Contract.builder()
                .id(UUID.randomUUID())
                .contractNumber("CTR-2026-0099")
                .monthlyFee(BigDecimal.valueOf(99.90))
                .dueDay(15)
                .installationAddress("Rua das Flores, 120, Apto 302")
                .city("Belo Horizonte")
                .state("MG")
                .zipCode("30100-000")
                .createdAt(LocalDateTime.of(2026, 8, 31, 10, 0))
                .build();

        String template = """
                CONTRATANTE: {{customer.name}}, CPF: {{customer.cpf_cnpj}}
                PRESTADORA: {{company.name}}, CNPJ: {{company.cnpj}}
                PLANO: {{plan.name}} ({{plan.download_speed}} / {{plan.upload_speed}})
                VALOR: {{contract.monthly_fee}} com vencimento todo dia {{contract.due_day}}
                LOCAL DE INSTALAÇÃO: {{contract.full_installation_address}}
                """;

        String rendered = engine.render(template, customer, company, contract, plan, null);

        assertThat(rendered).contains("CONTRATANTE: Maria de Souza, CPF: 123.456.789-01");
        assertThat(rendered).contains("PRESTADORA: Xingubit Telecom Ltda, CNPJ: 12.345.678/0001-90");
        assertThat(rendered).contains("PLANO: Fibra 500 Mega Turbo (500 Mbps / 250 Mbps)");
        assertThat(rendered).contains("com vencimento todo dia 15");
        assertThat(rendered).contains("LOCAL DE INSTALAÇÃO: Rua das Flores, 120, Apto 302 - Belo Horizonte/MG - CEP: 30100-000");
    }

    @Test
    @DisplayName("Deve calcular hash SHA-256 determinístico de texto contratual")
    void testCalculateSha256() {
        String text = "Contrato de Prestação de Serviços Fibra Óptica";
        String hash1 = engine.calculateSha256(text);
        String hash2 = engine.calculateSha256(text);

        assertThat(hash1).isNotBlank();
        assertThat(hash1).hasSize(64);
        assertThat(hash1).isEqualTo(hash2);
    }

    @Test
    @DisplayName("Deve listar catálogo com todas as variáveis documentadas")
    void testGetAvailableVariables() {
        List<ContractTemplateVariableInfo> variables = engine.getAvailableVariables();

        assertThat(variables).isNotEmpty();
        assertThat(variables).anyMatch(v -> v.getTag().equals("{{customer.name}}"));
        assertThat(variables).anyMatch(v -> v.getTag().equals("{{company.cnpj}}"));
        assertThat(variables).anyMatch(v -> v.getTag().equals("{{plan.download_speed}}"));
        assertThat(variables).anyMatch(v -> v.getTag().equals("{{signature.pix_id}}"));
    }
}
