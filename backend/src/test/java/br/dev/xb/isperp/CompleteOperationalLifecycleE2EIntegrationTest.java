package br.dev.xb.isperp;

import br.dev.xb.isperp.dto.*;
import br.dev.xb.isperp.entity.*;
import br.dev.xb.isperp.repository.*;
import br.dev.xb.isperp.service.*;
import br.dev.xb.isperp.util.UuidCreatorUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Teste de Integração End-to-End do Ciclo Operacional Completo do Provedor de Internet (ispERP):
 * 1. Multi-Almoxarifado & Termos de Custódia de Ferramental de Alto Valor (Nota Promissória)
 * 2. Transferência Inter-Bases com Duplo Handshake de Portador
 * 3. Contratação ➔ Faturamento Recorrente ➔ Pagamento Fora de Ordem (Rebalanceamento Contábil / Dona Maria)
 * 4. Régua de Cobrança (Dunning) & Desbloqueio em Confiança (48h) via Central do Assinante
 * 5. Abertura de Chamado Helpdesk com Protocolo Regulatório ANATEL & SLA
 * 6. Cancelamento de Contrato & Logística Reversa de Ativos
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@SuppressWarnings("null")
public class CompleteOperationalLifecycleE2EIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private WarehouseRepository warehouseRepository;

    @Autowired
    private SerializedAssetRepository serializedAssetRepository;

    @Autowired
    private ToolCustodyAgreementRepository toolCustodyAgreementRepository;

    @Autowired
    private StockTransferRepository stockTransferRepository;

    @Autowired
    private AssetCustodyService assetCustodyService;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private PlanRepository planRepository;

    @Autowired
    private ContractRepository contractRepository;

    @Autowired
    private InvoiceRepository invoiceRepository;

    @Autowired
    private ContractService contractService;

    @Autowired
    private InvoiceService invoiceService;

    @Autowired
    private InvoiceRebalanceService invoiceRebalanceService;

    @Autowired
    private HierarchicalBillingService hierarchicalBillingService;

    @Autowired
    private TrustUnblockPolicyService trustUnblockPolicyService;

    @Autowired
    private ClientPortalService clientPortalService;

    @Autowired
    private HelpdeskTicketRepository helpdeskTicketRepository;

    @Autowired
    private HelpdeskService helpdeskService;

    @Test
    @Order(1)
    @DisplayName("E2E - 1. Multi-Almoxarifado, Custódia de Ferramental & Transferência com Handshake")
    void testInventoryCustodyAndTransfers() {
        // Usuário Operador / Atendente
        User operator = userRepository.save(User.builder()
                .name("Operador Logística E2E")
                .email("operador.logistica@isperp.local")
                .password("password123")
                .role(UserRole.ADMIN)
                .active(true)
                .build());

        // A. Criação de Depósitos
        Warehouse central = warehouseRepository.save(Warehouse.builder()
                .code("DEP-CENTRAL-E2E")
                .name("Almoxarifado Central Matriz")
                .city("Altamira")
                .state("PA")
                .responsibleUserId(operator.getId())
                .active(true)
                .build());

        Warehouse filial = warehouseRepository.save(Warehouse.builder()
                .code("DEP-FILIAL-E2E")
                .name("Base Operacional Vitória do Xingu")
                .city("Vitória do Xingu")
                .state("PA")
                .responsibleUserId(operator.getId())
                .active(true)
                .build());

        assertNotNull(central.getId());
        assertNotNull(filial.getId());

        // B. Cadastro de Máquina de Fusão (Ferramenta de Alto Valor) e ONT
        SerializedAsset fusionMachine = serializedAssetRepository.save(SerializedAsset.builder()
                .serialNumber("FUSION-FSM-90S-001")
                .brandModel("Fujikura 90S Core Alignment")
                .category(SerializedAsset.AssetCategory.TOOL_FUSION_MACHINE)
                .replacementValue(new BigDecimal("18500.00"))
                .currentWarehouseId(central.getId())
                .status(SerializedAsset.AssetStatus.DISPONIVEL_DEPOSITO)
                .build());

        SerializedAsset ont = serializedAssetRepository.save(SerializedAsset.builder()
                .serialNumber("ZTE-F670L-E2E-001")
                .macAddress("CC:2D:E0:11:22:33")
                .brandModel("ZTE F670L Wi-Fi 6")
                .category(SerializedAsset.AssetCategory.ONU_ONT)
                .replacementValue(new BigDecimal("350.00"))
                .currentWarehouseId(central.getId())
                .status(SerializedAsset.AssetStatus.DISPONIVEL_DEPOSITO)
                .build());

        // C. Emissão de Termo de Cautela / Nota Promissória Executiva para o Técnico
        CheckoutToolRequest checkoutRequest = CheckoutToolRequest.builder()
                .holderUserId(operator.getId())
                .holderName("João Técnico de Fibra")
                .holderCpf("529.982.247-25")
                .isThirdParty(false)
                .assetIds(List.of(fusionMachine.getId()))
                .totalPromissoryValue(new BigDecimal("18500.00"))
                .notes("Ferramenta entregue limpa e calibrada para manutenções em campo.")
                .build();

        ToolCustodyAgreement agreement = assetCustodyService.checkoutToolAgreement(checkoutRequest);
        assertNotNull(agreement);
        assertEquals(ToolCustodyAgreement.AgreementStatus.ACTIVE, agreement.getStatus());
        assertTrue(agreement.getCode().startsWith("NOT-PROM-"));

        // Verifica que o status do ativo mudou para CUSTODIA_COLABORADOR
        SerializedAsset updatedTool = serializedAssetRepository.findById(fusionMachine.getId()).orElseThrow();
        assertEquals(SerializedAsset.AssetStatus.CUSTODIA_COLABORADOR, updatedTool.getStatus());

        // D. Criação e Duplo Aceite de Transferência Inter-Bases para a ONT
        CreateTransferRequest transferRequest = CreateTransferRequest.builder()
                .originWarehouseId(central.getId())
                .destinationWarehouseId(filial.getId())
                .carrierUserId(operator.getId())
                .carrierName("Carlos Motorista")
                .carrierDocument("529.982.247-25")
                .carrierType(StockTransfer.CarrierType.COLABORADOR)
                .assetIds(List.of(ont.getId()))
                .notes("Remessa de ONTs para ativações da semana.")
                .build();

        StockTransfer transfer = assetCustodyService.createTransfer(transferRequest);
        assertEquals(StockTransfer.TransferStatus.PENDING, transfer.getStatus());

        // 1º Handshake: Despacho na Origem
        StockTransfer dispatched = assetCustodyService.dispatchTransfer(transfer.getId(), operator.getId(), "https://cdn.isperp.local/despacho.jpg");
        assertEquals(StockTransfer.TransferStatus.IN_TRANSIT, dispatched.getStatus());
        SerializedAsset inTransitOnt = serializedAssetRepository.findById(ont.getId()).orElseThrow();
        assertEquals(SerializedAsset.AssetStatus.EM_TRANSITO, inTransitOnt.getStatus());

        // 2º Handshake: Recebimento no Destino
        StockTransfer received = assetCustodyService.confirmReceiptTransfer(transfer.getId(), operator.getId(), "https://cdn.isperp.local/recebimento.jpg");
        assertEquals(StockTransfer.TransferStatus.RECEIVED, received.getStatus());
        SerializedAsset deliveredOnt = serializedAssetRepository.findById(ont.getId()).orElseThrow();
        assertEquals(SerializedAsset.AssetStatus.DISPONIVEL_DEPOSITO, deliveredOnt.getStatus());
        assertEquals(filial.getId(), deliveredOnt.getCurrentWarehouseId());
    }

    @Test
    @Order(2)
    @DisplayName("E2E - 2. Contratação ➔ Faturamento ➔ Rebalanceamento Contábil (Dona Maria)")
    void testContractBillingAndOutOfOrderRebalance() {
        // A. Cadastro de Cliente e Plano
        Customer customer = customerRepository.save(Customer.builder()
                .name("Maria Silva (Dona Maria)")
                .cpf("529.982.247-25")
                .email("dona.maria.e2e@email.com")
                .phone("93988887777")
                .address("Av. Brigadeiro Eduardo Gomes, 1500")
                .city("Altamira")
                .state("PA")
                .zipCode("68370-000")
                .active(true)
                .build());

        Plan plan = planRepository.save(Plan.builder()
                .name("Fibra Turbo 500 Mega E2E")
                .downloadSpeed(500)
                .uploadSpeed(250)
                .price(new BigDecimal("99.90"))
                .active(true)
                .build());

        Contract contract = contractRepository.save(Contract.builder()
                .customerId(customer.getId())
                .planId(plan.getId())
                .contractNumber("CTR-E2E-MARIA-01")
                .status(Contract.ContractStatus.ACTIVE)
                .monthlyFee(new BigDecimal("99.90"))
                .dueDay(10)
                .installationAddress("Av. Brigadeiro Eduardo Gomes, 1500")
                .city("Altamira")
                .state("PA")
                .zipCode("68370-000")
                .build());

        // B. Geração de Fatura Anterior (Mês 1 - Vencida) e Fatura Futura (Mês 2 - Paga por engano)
        Invoice invoiceOlderOverdue = invoiceRepository.save(Invoice.builder()
                .customerId(customer.getId())
                .contractId(contract.getId())
                .amount(new BigDecimal("99.90"))
                .dueDate(LocalDate.now().minusDays(15))
                .status(Invoice.InvoiceStatus.OVERDUE)
                .build());

        Invoice invoiceFuturePaid = invoiceRepository.save(Invoice.builder()
                .customerId(customer.getId())
                .contractId(contract.getId())
                .amount(new BigDecimal("99.90"))
                .dueDate(LocalDate.now().plusDays(15))
                .status(Invoice.InvoiceStatus.PAID)
                .paidAt(LocalDateTime.now())
                .paymentMethod("PIX")
                .build());

        // C. Detecção autônoma de pagamento fora de ordem
        boolean detectedInversion = invoiceRebalanceService.checkAndHandleOutOfOrderPayment(invoiceFuturePaid);
        assertTrue(detectedInversion, "Deveria ter detectado pagamento invertido/fora de ordem");

        Invoice protectedInvoice = invoiceRepository.findById(invoiceOlderOverdue.getId()).orElseThrow();
        assertTrue(protectedInvoice.getProtectedAgainstSuspension(), "A fatura anterior deve estar protegida contra bloqueio");

        // D. Execução do Rebalanceamento Contábil Cruzado
        invoiceRebalanceService.executeCrossCreditRebalance(invoiceFuturePaid.getId(), invoiceOlderOverdue.getId());

        Invoice rebalancedOverdue = invoiceRepository.findById(invoiceOlderOverdue.getId()).orElseThrow();
        Invoice rebalancedFuture = invoiceRepository.findById(invoiceFuturePaid.getId()).orElseThrow();

        assertEquals(Invoice.InvoiceStatus.PAID, rebalancedOverdue.getStatus(), "Fatura anterior agora deve estar PAGA");
        assertNotNull(rebalancedOverdue.getPaidByCrossCreditId());
        assertTrue(rebalancedOverdue.getRebalanceNotice().contains("quitada automaticamente"));
        assertEquals(Invoice.InvoiceStatus.PENDING, rebalancedFuture.getStatus(), "Fatura futura deve ter sido reaberta");
        assertTrue(rebalancedFuture.getRebalanceNotice().contains("quitar a fatura pendente"));
    }

    @Test
    @Order(3)
    @DisplayName("E2E - 3. Central do Assinante, Desbloqueio em Confiança (48h) & Helpdesk ANATEL")
    void testClientPortalAndHelpdeskAnatelFlow() {
        // A. Setup de Cliente com Fatura Vencida e Contrato Suspenso
        Customer customer = customerRepository.save(Customer.builder()
                .name("Roberto Carlos Assinante")
                .cpf("123.456.789-09")
                .email("roberto.e2e@email.com")
                .phone("93999990000")
                .address("Rua Sete de Setembro, 200")
                .city("Altamira")
                .state("PA")
                .zipCode("68370-000")
                .active(true)
                .build());

        Plan plan = planRepository.save(Plan.builder()
                .name("Fibra Gamer 1000 Mega E2E")
                .downloadSpeed(1000)
                .uploadSpeed(500)
                .price(new BigDecimal("149.90"))
                .active(true)
                .build());

        Contract contract = contractRepository.save(Contract.builder()
                .customerId(customer.getId())
                .planId(plan.getId())
                .contractNumber("CTR-E2E-ROBERTO-02")
                .status(Contract.ContractStatus.SUSPENDED)
                .monthlyFee(new BigDecimal("149.90"))
                .dueDay(5)
                .installationAddress("Rua Sete de Setembro, 200")
                .city("Altamira")
                .state("PA")
                .zipCode("68370-000")
                .build());

        Invoice overdueInvoice = invoiceRepository.save(Invoice.builder()
                .customerId(customer.getId())
                .contractId(contract.getId())
                .amount(new BigDecimal("149.90"))
                .dueDate(LocalDate.now().minusDays(20))
                .status(Invoice.InvoiceStatus.OVERDUE)
                .pixCopiaECola("00020126580014br.gov.bcb.pix2536pay.xingubit.com.br/qr/e2e")
                .build());

        // B. Consulta ao Dashboard da Central do Assinante
        ClientPortalDashboardDTO dashboard = clientPortalService.getClientDashboard(customer.getId());
        assertNotNull(dashboard);
        assertTrue(dashboard.isConnectionBlocked());
        assertTrue(dashboard.isCanRequestTrustUnblock());

        // C. Solicitação de Desbloqueio em Confiança (48h)
        TrustUnblock trustUnblock = clientPortalService.requestTrustUnblock(customer.getId(), contract.getId());
        assertNotNull(trustUnblock);
        assertEquals("ACTIVE", trustUnblock.getStatus());

        // Verifica que o contrato foi reativado
        Contract reactivated = contractRepository.findById(contract.getId()).orElseThrow();
        assertEquals(Contract.ContractStatus.ACTIVE, reactivated.getStatus());

        // D. Abertura de Chamado via Portal (Protocolo ANATEL)
        HelpdeskService.CreateTicketRequest ticketRequest = HelpdeskService.CreateTicketRequest.builder()
                .customerId(customer.getId())
                .contractId(contract.getId())
                .category(HelpdeskTicket.TicketCategory.SLOW_SPEED)
                .channel(HelpdeskTicket.TicketChannel.PORTAL)
                .subject("Lentidão ao acessar servidores de jogos à noite")
                .description("Ping alto após às 19h no jogo Valorant.")
                .priority(HelpdeskTicket.TicketPriority.NORMAL)
                .build();

        HelpdeskTicket ticket = helpdeskService.createTicket(ticketRequest);
        assertNotNull(ticket);
        assertNotNull(ticket.getProtocol());
        assertTrue(ticket.getProtocol().length() >= 12, "Protocolo ANATEL deve conter data e sequencial");
        assertNotNull(ticket.getSlaDeadline());
        assertEquals(HelpdeskTicket.TicketStatus.OPEN, ticket.getStatus());

        // E. Atendente resolve o chamado no Nível 2
        HelpdeskTicket resolved = helpdeskService.resolveByN2(
                ticket.getId(),
                null,
                "Suporte N2",
                "Realizado ajuste de rotas BGP e liberação de porta FastPath."
        );
        assertEquals(HelpdeskTicket.TicketStatus.RESOLVED, resolved.getStatus());

        // F. Cliente avalia satisfação ANATEL e encerra chamado (Nota 5)
        HelpdeskTicket closed = helpdeskService.closeTicket(ticket.getId(), 5, "Problema resolvido muito rápido, excelente!");
        assertEquals(5, closed.getAnatelSatisfactionRating());
        assertEquals(HelpdeskTicket.TicketStatus.CLOSED, closed.getStatus());
    }

    @Test
    @Order(4)
    @DisplayName("E2E - 4. Cancelamento de Contrato & Logística Reversa de Equipamento")
    void testCancellationAndReverseLogistics() {
        Warehouse warehouse = warehouseRepository.findAll().stream().findFirst().orElseThrow();

        // Criação de Ativo em posse de cliente
        SerializedAsset customerOnt = serializedAssetRepository.save(SerializedAsset.builder()
                .serialNumber("HUAWEI-HG8145V5-REV-01")
                .macAddress("A4:B2:C3:D4:E5:F6")
                .brandModel("Huawei HG8145V5 Wi-Fi Dual Band")
                .category(SerializedAsset.AssetCategory.ONU_ONT)
                .replacementValue(new BigDecimal("420.00"))
                .status(SerializedAsset.AssetStatus.INSTALADO_CLIENTE)
                .build());

        // Técnico recolhe ONT em visita de retirada
        customerOnt.setStatus(SerializedAsset.AssetStatus.RETIRADO_PENDENTE_DEVOLUCAO);
        customerOnt.setCurrentWarehouseId(null);
        serializedAssetRepository.save(customerOnt);

        // Entrada e Triagem no Almoxarifado via Logística Reversa
        SerializedAsset returnedAsset = assetCustodyService.returnAssetFromWorkOrder(
                customerOnt.getId(),
                warehouse.getId(),
                false, // sem avarias
                "https://cdn.isperp.local/triagem-ont.jpg",
                "Equipamento testado em bancada de fibra, higienizado e pronto para reuso."
        );

        assertEquals(SerializedAsset.AssetStatus.DISPONIVEL_DEPOSITO, returnedAsset.getStatus());
        assertEquals(warehouse.getId(), returnedAsset.getCurrentWarehouseId());
    }
}
