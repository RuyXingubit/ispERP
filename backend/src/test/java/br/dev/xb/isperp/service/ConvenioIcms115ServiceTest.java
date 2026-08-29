package br.dev.xb.isperp.service;

import br.dev.xb.isperp.entity.Customer;
import br.dev.xb.isperp.entity.FiscalCompany;
import br.dev.xb.isperp.entity.NfcomRecord;
import br.dev.xb.isperp.repository.CustomerRepository;
import br.dev.xb.isperp.repository.FiscalCompanyRepository;
import br.dev.xb.isperp.repository.NfcomRecordRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class ConvenioIcms115ServiceTest {

    private FiscalCompanyRepository companyRepository;
    private NfcomRecordRepository nfcomRecordRepository;
    private CustomerRepository customerRepository;
    private ConvenioIcms115Service service;

    @BeforeEach
    void setUp() {
        companyRepository = Mockito.mock(FiscalCompanyRepository.class);
        nfcomRecordRepository = Mockito.mock(NfcomRecordRepository.class);
        customerRepository = Mockito.mock(CustomerRepository.class);

        service = new ConvenioIcms115Service(companyRepository, nfcomRecordRepository, customerRepository);
    }

    @Test
    @DisplayName("Deve gerar lote oficial do Convênio 115/03 com 4 arquivos e ZIP íntegro")
    void testGenerateMonthlyBatch() {
        UUID companyId = UUID.randomUUID();
        FiscalCompany company = FiscalCompany.builder()
                .id(companyId)
                .cnpj("12.345.678/0001-95")
                .razaoSocial("Provedor Xingu Telecom Ltda")
                .inscricaoEstadual("15999888")
                .uf("PA")
                .nfcomSerie("1")
                .build();

        UUID customerId = UUID.randomUUID();
        Customer customer = Customer.builder()
                .id(customerId)
                .name("Maria Silva")
                .cpf("529.982.247-25")
                .address("Av. Brigadeiro Eduardo Gomes, 1500")
                .city("Altamira")
                .state("PA")
                .zipCode("68370-000")
                .build();

        NfcomRecord record = NfcomRecord.builder()
                .id(UUID.randomUUID())
                .companyId(companyId)
                .customerId(customerId)
                .chaveAcesso("15260812345678000195620010000000011123456789")
                .numero(1)
                .serie("1")
                .valorTotal(new BigDecimal("99.90"))
                .valorIcms(BigDecimal.ZERO)
                .createdAt(LocalDateTime.of(2026, 8, 15, 10, 0))
                .build();

        when(companyRepository.findById(companyId)).thenReturn(Optional.of(company));
        when(companyRepository.findFirstByIsActiveTrue()).thenReturn(Optional.of(company));
        when(nfcomRecordRepository.findByCompanyIdAndCreatedAtBetween(any(), any(), any())).thenReturn(List.of(record));
        when(customerRepository.findById(customerId)).thenReturn(Optional.of(customer));

        ConvenioIcms115Service.Convenio115BatchResult result = service.generateMonthlyBatch(companyId, 2026, 8);

        assertNotNull(result);
        assertEquals(1, result.getTotalRecords());
        assertEquals(new BigDecimal("99.90"), result.getTotalFaturado());
        assertTrue(result.getFilenameMestre().endsWith(".M"));
        assertTrue(result.getFilenameItem().endsWith(".I"));
        assertTrue(result.getFilenameDestinatario().endsWith(".D"));
        assertTrue(result.getFilenameControle().endsWith(".C"));
        assertNotNull(result.getMd5Mestre());
        assertEquals(32, result.getMd5Mestre().length(), "Hash MD5 deve ter 32 caracteres hexadecimais");
        assertNotNull(result.getZipBytes());
        assertTrue(result.getZipBytes().length > 0, "O arquivo ZIP deve conter dados empacotados");
    }
}
