package br.dev.xb.isperp.service;

import br.dev.xb.isperp.dto.ContractTemplateRequest;
import br.dev.xb.isperp.dto.ContractTemplateResponse;
import br.dev.xb.isperp.entity.ContractTemplate;
import br.dev.xb.isperp.mapper.ContractTemplateMapper;
import br.dev.xb.isperp.repository.ContractTemplateRepository;
import br.dev.xb.isperp.signature.DocumentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ContractTemplateServiceTest {

    @Mock
    private ContractTemplateRepository templateRepository;

    private final ContractTemplateMapper templateMapper = Mappers.getMapper(ContractTemplateMapper.class);
    private final ContractTemplateEngine templateEngine = new ContractTemplateEngine();

    private ContractTemplateService templateService;

    @BeforeEach
    void setUp() {
        templateService = new ContractTemplateService(templateRepository, templateMapper, templateEngine);
    }

    @Test
    @DisplayName("Deve criar novo template de contrato")
    void testCreateTemplate() {
        UUID companyId = UUID.randomUUID();
        ContractTemplateRequest request = ContractTemplateRequest.builder()
                .companyId(companyId)
                .name("Contrato SCM Fibra")
                .documentType(DocumentType.SERVICE_AGREEMENT)
                .version(1)
                .isActive(true)
                .contentMarkdown("# Contrato SCM\n{{customer.name}}")
                .consentClause("Aceito os termos.")
                .build();

        ContractTemplate saved = ContractTemplate.builder()
                .id(UUID.randomUUID())
                .companyId(companyId)
                .name("Contrato SCM Fibra")
                .documentType(DocumentType.SERVICE_AGREEMENT)
                .version(1)
                .isActive(true)
                .contentMarkdown("# Contrato SCM\n{{customer.name}}")
                .consentClause("Aceito os termos.")
                .build();

        when(templateRepository.save(any(ContractTemplate.class))).thenReturn(saved);

        ContractTemplateResponse response = templateService.createTemplate(request);

        assertThat(response).isNotNull();
        assertThat(response.getName()).isEqualTo("Contrato SCM Fibra");
        assertThat(response.getDocumentType()).isEqualTo(DocumentType.SERVICE_AGREEMENT);
    }

    @Test
    @DisplayName("Deve clonar um template existente com versionamento incrementado")
    void testCloneTemplate() {
        UUID id = UUID.randomUUID();
        ContractTemplate original = ContractTemplate.builder()
                .id(id)
                .name("Contrato Base")
                .documentType(DocumentType.SERVICE_AGREEMENT)
                .version(1)
                .isActive(true)
                .contentMarkdown("Cláusulas originais")
                .consentClause("Aceite original")
                .build();

        when(templateRepository.findById(id)).thenReturn(Optional.of(original));
        when(templateRepository.save(any(ContractTemplate.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ContractTemplateResponse response = templateService.cloneTemplate(id);

        assertThat(response).isNotNull();
        assertThat(response.getName()).isEqualTo("Contrato Base (Cópia)");
        assertThat(response.getVersion()).isEqualTo(2);
    }
}
