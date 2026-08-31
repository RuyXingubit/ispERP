package br.dev.xb.isperp.service;

import br.dev.xb.isperp.dto.ContractTemplateRequest;
import br.dev.xb.isperp.dto.ContractTemplateResponse;
import br.dev.xb.isperp.dto.ContractTemplateVariableInfo;
import br.dev.xb.isperp.entity.ContractTemplate;
import br.dev.xb.isperp.mapper.ContractTemplateMapper;
import br.dev.xb.isperp.repository.ContractTemplateRepository;
import br.dev.xb.isperp.signature.DocumentType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@SuppressWarnings("null")
public class ContractTemplateService {

    private final ContractTemplateRepository templateRepository;
    private final ContractTemplateMapper templateMapper;
    private final ContractTemplateEngine templateEngine;

    @Transactional(readOnly = true)
    public List<ContractTemplateResponse> listTemplates(UUID companyId) {
        List<ContractTemplate> list;
        if (companyId != null) {
            list = templateRepository.findByCompanyId(companyId);
            if (list.isEmpty()) {
                list = templateRepository.findAll();
            }
        } else {
            list = templateRepository.findAll();
        }
        return templateMapper.toResponseList(list);
    }

    @Transactional(readOnly = true)
    public ContractTemplateResponse getTemplateById(UUID id) {
        ContractTemplate template = templateRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Template de contrato não encontrado com o ID: " + id));
        return templateMapper.toResponse(template);
    }

    @Transactional
    public ContractTemplateResponse createTemplate(ContractTemplateRequest request) {
        ContractTemplate entity = templateMapper.toEntity(request);
        if (entity.getCompanyId() == null) {
            entity.setCompanyId(request.getCompanyId());
        }
        ContractTemplate saved = templateRepository.save(entity);
        log.info("Template de contrato criado com sucesso: '{}' ({})", saved.getName(), saved.getDocumentType());
        return templateMapper.toResponse(saved);
    }

    @Transactional
    public ContractTemplateResponse updateTemplate(UUID id, ContractTemplateRequest request) {
        ContractTemplate existing = templateRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Template de contrato não encontrado com o ID: " + id));

        existing.setName(request.getName());
        existing.setDocumentType(request.getDocumentType());
        existing.setContentMarkdown(request.getContentMarkdown());
        existing.setConsentClause(request.getConsentClause());
        if (request.getIsActive() != null) {
            existing.setIsActive(request.getIsActive());
        }
        if (request.getVersion() != null) {
            existing.setVersion(request.getVersion());
        }

        ContractTemplate updated = templateRepository.save(existing);
        log.info("Template de contrato atualizado: '{}' ({})", updated.getName(), updated.getId());
        return templateMapper.toResponse(updated);
    }

    @Transactional
    public void deleteTemplate(UUID id) {
        ContractTemplate existing = templateRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Template de contrato não encontrado com o ID: " + id));
        templateRepository.delete(existing);
        log.info("Template de contrato removido: {}", id);
    }

    @Transactional
    public ContractTemplateResponse cloneTemplate(UUID id) {
        ContractTemplate original = templateRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Template não encontrado para clonagem: " + id));

        ContractTemplate clone = ContractTemplate.builder()
                .companyId(original.getCompanyId())
                .name(original.getName() + " (Cópia)")
                .documentType(original.getDocumentType())
                .version(original.getVersion() + 1)
                .isActive(true)
                .contentMarkdown(original.getContentMarkdown())
                .consentClause(original.getConsentClause())
                .build();

        ContractTemplate saved = templateRepository.save(clone);
        log.info("Template clonado com sucesso: {} a partir de {}", saved.getId(), original.getId());
        return templateMapper.toResponse(saved);
    }

    public List<ContractTemplateVariableInfo> getAvailableVariables() {
        return templateEngine.getAvailableVariables();
    }

    /**
     * Garante a existência de templates padrão de sistema para novos ISPs.
     */
    @Transactional
    public void seedDefaultTemplatesIfEmpty(UUID companyId) {
        List<ContractTemplate> existing = templateRepository.findByCompanyId(companyId);
        if (!existing.isEmpty()) {
            return;
        }

        ContractTemplate scmTemplate = ContractTemplate.builder()
                .companyId(companyId)
                .name("Contrato Padrão de Prestação de Serviços SCM e SVA")
                .documentType(DocumentType.SERVICE_AGREEMENT)
                .version(1)
                .isActive(true)
                .contentMarkdown("""
# CONTRATO DE PRESTAÇÃO DE SERVIÇOS DE COMUNICAÇÃO MULTIMÍDIA (SCM) E SVA

**CONTRATADA:** **{{company.name}}**, inscrita no CNPJ sob o nº **{{company.cnpj}}**, com sede em **{{company.address}}**, doravante denominada simplesmente **PRESTADORA**, devidamente autorizada pela ANATEL nos termos do {{company.anatel_act}}.

**CONTRATANTE / ASSINANTE:** **{{customer.name}}**, inscrito(a) no CPF/CNPJ sob o nº **{{customer.cpf_cnpj}}**, portador(a) do RG/IE nº **{{customer.rg_ie}}**, residente/domiciliado(a) em **{{customer.full_address}}**, doravante denominado(a) **ASSINANTE**.

---

### CLÁUSULA 1ª - DO OBJETO
1.1. O presente instrumento tem por objeto a prestação contínua de Serviços de Comunicação Multimídia (SCM - Acesso à Internet em Banda Larga via Fibra Óptica) e Serviços de Valor Adicionado (SVA) conforme o plano **{{plan.name}}**, com velocidade nominal de **{{plan.download_speed}}** de download e **{{plan.upload_speed}}** de upload.
1.2. O ponto de instalação e entrega dos serviços situa-se no endereço: **{{contract.full_installation_address}}**.

---

### CLÁUSULA 2ª - DOS VALORES E FORMA DE PAGAMENTO
2.1. Pela prestação dos serviços objeto deste contrato, o(a) **ASSINANTE** pagará à **PRESTADORA** o valor mensal de **{{contract.monthly_fee}}**, com vencimento todo dia **{{contract.due_day}}** de cada mês.
2.2. O não pagamento no vencimento ensejará aplicação de multa moratória de 2% (dois por cento) e juros de mora de 1% (um por cento) ao mês pro-rata die, além da suspensão gradual dos serviços nos termos do Regulamento Geral de Direitos do Consumidor de Serviços de Telecomunicações (RGC/ANATEL).

---

### CLÁUSULA 3ª - DA VIGÊNCIA E TERMO DE FIDELIDADE
3.1. Este contrato entra em vigor na data de sua assinatura eletrônica e vige por prazo indeterminado, aplicando-se o Termo de Permanência de **{{plan.loyalty_months}} meses** em virtude dos benefícios concedidos na taxa de instalação e comodato de equipamentos.

---

### CLÁUSULA 4ª - DA ASSINATURA ELETRÔNICA AVANÇADA (MP 2.200-2/01 E LEI 14.063/2020)
4.1. As partes reconhecem expressamente a validade jurídica, integridade e eficácia da assinatura deste contrato por meio eletrônico com autenticação bancária instantânea via Pix, nos termos do Art. 10, § 2º da Medida Provisória nº 2.200-2/2001 e Art. 4º, II da Lei nº 14.063/2020.
""")
                .consentClause("Ao realizar o pagamento do Pix abaixo de R$ 1,00 pela conta bancária do titular, declaro que li, compreendi e concordo integralmente com todas as cláusulas do presente Contrato de Prestação de Serviços, servindo esta transação bancária certificada pelo Banco Central do Brasil como minha ASSINATURA ELETRÔNICA DEFINITIVA e inequívoca manifestação de vontade.")
                .build();

        templateRepository.save(scmTemplate);

        ContractTemplate loyaltyTemplate = ContractTemplate.builder()
                .companyId(companyId)
                .name("Termo de Permanência e Fidelidade Contratual (12 Meses)")
                .documentType(DocumentType.LOYALTY_TERM)
                .version(1)
                .isActive(true)
                .contentMarkdown("""
# TERMO DE PERMANÊNCIA E FIDELIDADE CONTRATUAL - 12 MESES

**CONTRATANTE:** **{{customer.name}}** - CPF/CNPJ: **{{customer.cpf_cnpj}}**
**CONTRATO Nº:** **{{contract.number}}** | **PLANO:** **{{plan.name}}**

### CLÁUSULA 1ª - DOS BENEFÍCIOS CONCEDIDOS
1.1. Em contrapartida à permanência mínima de **12 (doze) meses**, a PRESTADORA concedeu ao ASSINANTE os seguintes benefícios:
- Isenção integral da Taxa de Instalação e Ativação de Fibra Óptica (Valor venal: R$ 350,00);
- Comodato gratuito do Roteador Wi-Fi Gigabit e ONT Óptica;
- Desconto comercial na mensalidade do plano **{{plan.name}}**.

### CLÁUSULA 2ª - DA RESCISÃO ANTECIPADA
2.1. Em caso de cancelamento imotivado por iniciativa do ASSINANTE antes do término do prazo de 12 meses, incidirá multa rescisória proporcional ao tempo restante de contrato, calculada sobre o valor base de **{{plan.loyalty_penalty}}**.
""")
                .consentClause("Declaro estar ciente e de acordo com o prazo de permanência mínima de 12 meses e as condições de rescisão proporcional estipuladas neste Termo de Fidelidade, autenticando meu consentimento por meio da transação bancária via Pix do titular.")
                .build();

        templateRepository.save(loyaltyTemplate);
        log.info("Templates padrão de contrato e fidelidade criados para a empresa {}", companyId);
    }
}
