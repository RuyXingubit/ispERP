package br.dev.xb.isperp.seeder;

import br.dev.xb.isperp.entity.*;
import br.dev.xb.isperp.entity.financial.*;
import br.dev.xb.isperp.repository.*;
import br.dev.xb.isperp.repository.financial.*;
import br.dev.xb.isperp.util.UuidCreatorUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.*;

/**
 * Seeder de Homologação e Simulação Operacional de 1 Ano.
 * Executado EXCLUSIVAMENTE quando o profile 'dev' estiver ativo.
 * Cria toda a massa de dados rica e correlacionada para a "Nexus Fibra Telecom".
 */
@Component
@Profile("dev")
@RequiredArgsConstructor
@Slf4j
public class DevDataSeederService implements ApplicationRunner {

    private final UserRepository userRepository;
    private final CompanyRepository companyRepository;
    private final SiteSettingsRepository siteSettingsRepository;
    private final PlanRepository planRepository;
    private final WarehouseRepository warehouseRepository;
    private final InventoryItemRepository inventoryItemRepository;
    private final UserMaterialCustodyRepository userMaterialCustodyRepository;
    private final UserCashCustodyRepository userCashCustodyRepository;
    private final CustomerRepository customerRepository;
    private final ContractRepository contractRepository;
    private final InvoiceRepository invoiceRepository;
    private final PayableInvoiceRepository payableInvoiceRepository;
    private final ExpenseInstallmentRepository expenseInstallmentRepository;
    private final ChartOfAccountRepository chartOfAccountRepository;
    private final NetworkProjectRepository networkProjectRepository;
    private final FtthCtoRepository ftthCtoRepository;
    private final WorkOrderRepository workOrderRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (userRepository.count() > 0) {
            log.info("⏩ [DevDataSeeder] Banco de dados já possui usuários cadastrados. Pulando carga de dados de teste.");
            return;
        }

        log.info("🚀 [DevDataSeeder] Iniciando povoamento operacional simulando 1 ano de histórico para 'Nexus Fibra Telecom'...");

        Company company = seedCompany();
        seedSiteSettings();
        Map<String, User> users = seedUsers();
        Map<String, Plan> plans = seedPlans();
        Map<String, Warehouse> warehouses = seedWarehouses(users);
        seedInventoryAndMaterialCustody(warehouses, users);
        Map<String, NetworkProject> projects = seedNetworkProjects();
        Map<String, FtthCto> ctos = seedFtthCtos(company, projects);
        seedCashCustody(users);

        ChartOfAccount caLink = chartOfAccountRepository.findByCode("03.01.01").orElse(null);
        ChartOfAccount caPostes = chartOfAccountRepository.findByCode("04.01.01").orElse(null);
        ChartOfAccount caEquip = chartOfAccountRepository.findByCode("05.01.01").orElse(null);

        seedPayablesAndCapex(caLink, caPostes, caEquip);
        seedCustomersContractsInvoicesAndWorkOrders(plans, users, projects, ctos);

        log.info("✅ [DevDataSeeder] Simulação de 1 ano concluída com 100% de sucesso! Ambiente pronto para uso.");
    }

    private Company seedCompany() {
        Company company = Company.builder()
                .id(UuidCreatorUtils.generateUuidV7())
                .name("Nexus Fibra Telecomunicações Ltda.")
                .document("28.451.983/0001-44")
                .address("Av. Brasil, 1500, Centro, Altamira - PA")
                .phone("(93) 3515-2000")
                .email("contato@nexusfibra.com.br")
                .website("https://nexusfibra.com.br")
                .active(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        return companyRepository.save(company);
    }

    private void seedSiteSettings() {
        SiteSettings settings = SiteSettings.builder()
                .id(UuidCreatorUtils.generateUuidV7())
                .siteTitle("Nexus Fibra - Ultra Velocidade & Conectividade")
                .siteDescription("Provedor de Internet 100% Fibra Óptica de Alta Disponibilidade")
                .primaryColor("#1976d2")
                .secondaryColor("#dc004e")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        siteSettingsRepository.save(settings);
    }

    private Map<String, User> seedUsers() {
        Map<String, User> map = new HashMap<>();
        String passHash = passwordEncoder.encode("password123");

        // 1. Admin Master
        User admin = User.builder()
                .id(UuidCreatorUtils.generateUuidV7())
                .name("Administrador Master")
                .email("admin@nexusfibra.com.br")
                .password(passHash)
                .role(UserRole.ADMIN)
                .cpf(generateValidCpf(1))
                .active(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        map.put("admin", userRepository.save(admin));

        // 2. CFO / Diretor Financeiro
        User cfo = User.builder()
                .id(UuidCreatorUtils.generateUuidV7())
                .name("Roberto Silveira (CFO)")
                .email("cfo@nexusfibra.com.br")
                .password(passHash)
                .role(UserRole.FINANCIAL)
                .cpf(generateValidCpf(2))
                .active(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        map.put("cfo", userRepository.save(cfo));

        // 3. Atendentes
        User maria = User.builder()
                .id(UuidCreatorUtils.generateUuidV7())
                .name("Maria Clara (Comercial)")
                .email("atendente.maria@nexusfibra.com.br")
                .password(passHash)
                .role(UserRole.SUPPORT_ANALYST)
                .cpf(generateValidCpf(3))
                .active(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        map.put("maria", userRepository.save(maria));

        User joao = User.builder()
                .id(UuidCreatorUtils.generateUuidV7())
                .name("João Paulo (Suporte N1)")
                .email("atendente.joao@nexusfibra.com.br")
                .password(passHash)
                .role(UserRole.ADMINISTRATIVE_ASSISTANT)
                .cpf(generateValidCpf(4))
                .active(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        map.put("joao", userRepository.save(joao));

        User camila = User.builder()
                .id(UuidCreatorUtils.generateUuidV7())
                .name("Camila Santos (Cobrança)")
                .email("atendente.camila@nexusfibra.com.br")
                .password(passHash)
                .role(UserRole.ATTENDANT)
                .cpf(generateValidCpf(5))
                .active(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        map.put("camila", userRepository.save(camila));

        // 4. Técnicos de Campo
        User carlos = User.builder()
                .id(UuidCreatorUtils.generateUuidV7())
                .name("Carlos Alberto (Técnico Alfa)")
                .email("tecnico.carlos@nexusfibra.com.br")
                .password(passHash)
                .role(UserRole.TECHNICIAN)
                .cpf(generateValidCpf(6))
                .active(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        map.put("carlos", userRepository.save(carlos));

        User lucas = User.builder()
                .id(UuidCreatorUtils.generateUuidV7())
                .name("Lucas Mendes (Técnico Alfa)")
                .email("tecnico.lucas@nexusfibra.com.br")
                .password(passHash)
                .role(UserRole.TECHNICIAN)
                .cpf(generateValidCpf(7))
                .active(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        map.put("lucas", userRepository.save(lucas));

        User marcos = User.builder()
                .id(UuidCreatorUtils.generateUuidV7())
                .name("Marcos Rocha (Técnico Bravo)")
                .email("tecnico.marcos@nexusfibra.com.br")
                .password(passHash)
                .role(UserRole.TECHNICIAN)
                .cpf(generateValidCpf(8))
                .active(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        map.put("marcos", userRepository.save(marcos));

        User andre = User.builder()
                .id(UuidCreatorUtils.generateUuidV7())
                .name("André Luis (Técnico Bravo)")
                .email("tecnico.andre@nexusfibra.com.br")
                .password(passHash)
                .role(UserRole.TECHNICIAN)
                .cpf(generateValidCpf(9))
                .active(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        map.put("andre", userRepository.save(andre));

        return map;
    }

    private Map<String, Plan> seedPlans() {
        Map<String, Plan> map = new HashMap<>();

        Plan p1 = Plan.builder()
                .id(UuidCreatorUtils.generateUuidV7())
                .name("Fibra 300 Mega Residencial")
                .downloadSpeed(300)
                .uploadSpeed(150)
                .price(new BigDecimal("79.90"))
                .description("Internet ultra-estável para streaming e home office")
                .svaIncluded("Clube de Vantagens Nexus")
                .suspensionDays(5)
                .alwaysIssueNfcom(false)
                .active(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        map.put("300M", planRepository.save(p1));

        Plan p2 = Plan.builder()
                .id(UuidCreatorUtils.generateUuidV7())
                .name("Fibra 600 Mega Ultra Gamer")
                .downloadSpeed(600)
                .uploadSpeed(300)
                .price(new BigDecimal("99.90"))
                .description("Baixa latência para jogos online e downloads pesados")
                .svaIncluded("Paramount+, Deezer")
                .suspensionDays(5)
                .alwaysIssueNfcom(false)
                .active(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        map.put("600M", planRepository.save(p2));

        Plan p3 = Plan.builder()
                .id(UuidCreatorUtils.generateUuidV7())
                .name("Fibra 1 Giga Corporativo")
                .downloadSpeed(1000)
                .uploadSpeed(500)
                .price(new BigDecimal("199.90"))
                .description("Link com IP Fixo Dedicado e SLA de 4 horas")
                .svaIncluded("Backup Cloud, IP Fixo Dedicado")
                .suspensionDays(5)
                .alwaysIssueNfcom(true)
                .active(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        map.put("1G", planRepository.save(p3));

        return map;
    }

    private Map<String, Warehouse> seedWarehouses(Map<String, User> users) {
        Map<String, Warehouse> map = new HashMap<>();

        Warehouse central = Warehouse.builder()
                .id(UuidCreatorUtils.generateUuidV7())
                .code("WH-SEDE-01")
                .name("Almoxarifado Central (Sede)")
                .city("Altamira")
                .state("PA")
                .address("Av. Brasil, 1500")
                .responsibleUserId(users.get("admin").getId())
                .active(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        map.put("central", warehouseRepository.save(central));

        Warehouse v1 = Warehouse.builder()
                .id(UuidCreatorUtils.generateUuidV7())
                .code("VEH-ALFA-01")
                .name("Veículo 01 - Fiat Strada (Equipe Alfa)")
                .city("Altamira")
                .state("PA")
                .address("Móvel em Campo")
                .responsibleUserId(users.get("carlos").getId())
                .active(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        map.put("v1", warehouseRepository.save(v1));

        Warehouse v2 = Warehouse.builder()
                .id(UuidCreatorUtils.generateUuidV7())
                .code("VEH-BRAVO-02")
                .name("Veículo 02 - Renault Oroch (Equipe Bravo)")
                .city("Altamira")
                .state("PA")
                .address("Móvel em Campo")
                .responsibleUserId(users.get("marcos").getId())
                .active(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        map.put("v2", warehouseRepository.save(v2));

        return map;
    }

    private void seedInventoryAndMaterialCustody(Map<String, Warehouse> warehouses, Map<String, User> users) {
        InventoryItem ontHw = InventoryItem.builder()
                .id(UuidCreatorUtils.generateUuidV7())
                .code("ONT-HW-EG8145V5")
                .name("ONT Huawei Dual-Band GPON Wi-Fi 5")
                .category("ONU_ONT")
                .quantityInStock(150)
                .minQuantity(20)
                .unit("UN")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        inventoryItemRepository.save(ontHw);

        InventoryItem ontZte = InventoryItem.builder()
                .id(UuidCreatorUtils.generateUuidV7())
                .code("ONT-ZTE-F670L")
                .name("ONT ZTE Dual-Band AX3000 Wi-Fi 6")
                .category("ONU_ONT")
                .quantityInStock(80)
                .minQuantity(15)
                .unit("UN")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        inventoryItemRepository.save(ontZte);

        InventoryItem dropCable = InventoryItem.builder()
                .id(UuidCreatorUtils.generateUuidV7())
                .code("DROP-1FO-1000M")
                .name("Bobina de Cabo Drop Óptico 1FO 1000m")
                .category("CABO_DROP")
                .quantityInStock(25)
                .minQuantity(5)
                .unit("BOB")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        inventoryItemRepository.save(dropCable);

        InventoryItem connector = InventoryItem.builder()
                .id(UuidCreatorUtils.generateUuidV7())
                .code("FAST-SC-APC")
                .name("Conector de Campo Fast SC/APC Verde")
                .category("CONECTOR")
                .quantityInStock(800)
                .minQuantity(100)
                .unit("UN")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        inventoryItemRepository.save(connector);

        // Carga de Materiais na Custódia do Técnico Carlos (Veículo 01)
        User carlos = users.get("carlos");
        userMaterialCustodyRepository.save(UserMaterialCustody.builder()
                .id(UuidCreatorUtils.generateUuidV7())
                .user(carlos)
                .itemName("ONT Huawei Dual-Band GPON (Kit de Instalação)")
                .itemType(MaterialType.ONT)
                .serialNumber("HWTC12345678")
                .macAddress("48:57:02:11:22:33")
                .quantity(BigDecimal.ONE)
                .allocatedAt(OffsetDateTime.now().minusDays(10))
                .notes("Carregado para rota de instalações do bairro Jardins")
                .build());

        userMaterialCustodyRepository.save(UserMaterialCustody.builder()
                .id(UuidCreatorUtils.generateUuidV7())
                .user(carlos)
                .itemName("Máquina de Fusão Óptica Fujikura 70S")
                .itemType(MaterialType.FUSION_MACHINE)
                .serialNumber("FJK-994820")
                .quantity(BigDecimal.ONE)
                .allocatedAt(OffsetDateTime.now().minusDays(60))
                .notes("Equipamento patrimonial sob responsabilidade civil")
                .build());

        // Carga de Materiais na Custódia do Técnico Marcos (Veículo 02)
        User marcos = users.get("marcos");
        userMaterialCustodyRepository.save(UserMaterialCustody.builder()
                .id(UuidCreatorUtils.generateUuidV7())
                .user(marcos)
                .itemName("Máquina de Fusão Óptica Inno View 5")
                .itemType(MaterialType.FUSION_MACHINE)
                .serialNumber("INNO-771122")
                .quantity(BigDecimal.ONE)
                .allocatedAt(OffsetDateTime.now().minusDays(45))
                .notes("Equipamento patrimonial sob responsabilidade civil")
                .build());
    }

    private Map<String, NetworkProject> seedNetworkProjects() {
        Map<String, NetworkProject> map = new HashMap<>();

        NetworkProject p1 = NetworkProject.builder()
                .id(UuidCreatorUtils.generateUuidV7())
                .name("Expansão FTTH - Bairro Jardins")
                .neighborhood("Jardins")
                .city("Altamira")
                .budgetAmount(new BigDecimal("48000.00"))
                .targetSubscribers(120)
                .startDate(LocalDate.now().minusMonths(12))
                .status(ProjectStatus.ACTIVE)
                .notes("Rede implantada com alta densidade populacional e rápida taxa de adesão.")
                .createdAt(OffsetDateTime.now().minusMonths(12))
                .updatedAt(OffsetDateTime.now().minusMonths(12))
                .build();
        map.put("jardins", networkProjectRepository.save(p1));

        NetworkProject p2 = NetworkProject.builder()
                .id(UuidCreatorUtils.generateUuidV7())
                .name("Expansão FTTH - Bairro Alvorada")
                .neighborhood("Alvorada")
                .city("Altamira")
                .budgetAmount(new BigDecimal("65000.00"))
                .targetSubscribers(140)
                .startDate(LocalDate.now().minusMonths(6))
                .status(ProjectStatus.ACTIVE)
                .notes("Área nova de expansão com potencial comercial aguardando panfletagem.")
                .createdAt(OffsetDateTime.now().minusMonths(6))
                .updatedAt(OffsetDateTime.now().minusMonths(6))
                .build();
        map.put("alvorada", networkProjectRepository.save(p2));

        return map;
    }

    private Map<String, FtthCto> seedFtthCtos(Company company, Map<String, NetworkProject> projects) {
        Map<String, FtthCto> map = new HashMap<>();

        // CTO 01 - Jardins (Alta ocupação)
        FtthCto ctoJardins1 = FtthCto.builder()
                .id(UuidCreatorUtils.generateUuidV7())
                .companyId(company.getId())
                .name("CTO-JARDINS-01")
                .latitude(new BigDecimal("-3.20450000"))
                .longitude(new BigDecimal("-52.20810000"))
                .totalPorts(16)
                .splitterType("BALANCED_1_16")
                .status("ATIVA")
                .description("Poste 14, esquina com Rua das Flores")
                .projectId(projects.get("jardins").getId())
                .build();
        map.put("jardins1", ftthCtoRepository.save(ctoJardins1));

        // CTO 02 - Jardins
        FtthCto ctoJardins2 = FtthCto.builder()
                .id(UuidCreatorUtils.generateUuidV7())
                .companyId(company.getId())
                .name("CTO-JARDINS-02")
                .latitude(new BigDecimal("-3.20520000"))
                .longitude(new BigDecimal("-52.20950000"))
                .totalPorts(16)
                .splitterType("BALANCED_1_16")
                .status("ATIVA")
                .description("Poste 22, em frente à Praça")
                .projectId(projects.get("jardins").getId())
                .build();
        map.put("jardins2", ftthCtoRepository.save(ctoJardins2));

        // CTO 03 - Alvorada (Baixa ocupação para disparar Direcionador Comercial)
        FtthCto ctoAlvorada1 = FtthCto.builder()
                .id(UuidCreatorUtils.generateUuidV7())
                .companyId(company.getId())
                .name("CTO-ALVORADA-01")
                .latitude(new BigDecimal("-3.21850000"))
                .longitude(new BigDecimal("-52.22150000"))
                .totalPorts(16)
                .splitterType("BALANCED_1_16")
                .status("ATIVA")
                .description("Poste 05, Avenida Alvorada")
                .projectId(projects.get("alvorada").getId())
                .build();
        map.put("alvorada1", ftthCtoRepository.save(ctoAlvorada1));

        return map;
    }

    private void seedCashCustody(Map<String, User> users) {
        User carlos = users.get("carlos");
        // Técnico Carlos tem R$ 150,00 recebidos em dinheiro vivo em campo pendentes de entrega
        userCashCustodyRepository.save(UserCashCustody.builder()
                .id(UuidCreatorUtils.generateUuidV7())
                .user(carlos)
                .cpf(carlos.getCpf())
                .currentBalance(new BigDecimal("150.00"))
                .createdAt(OffsetDateTime.now().minusDays(1))
                .updatedAt(OffsetDateTime.now().minusHours(2))
                .build());
    }

    private void seedPayablesAndCapex(ChartOfAccount caLink, ChartOfAccount caPostes, ChartOfAccount caEquip) {
        LocalDate today = LocalDate.now();

        // 1. CAPEX - Compra da OLT MA5800 em 24x de R$ 2.000,00
        if (caEquip != null) {
            PayableInvoice oltPurchase = PayableInvoice.builder()
                    .id(UuidCreatorUtils.generateUuidV7())
                    .supplierName("Huawei do Brasil Telecomunicações")
                    .supplierDocument("02.469.340/0001-08")
                    .chartOfAccount(caEquip)
                    .description("Aquisição OLT GPON SmartAX MA5800-X7 com 8 portas PON")
                    .invoiceNumber("NF-88492")
                    .totalAmount(new BigDecimal("48000.00"))
                    .issueDate(today.minusMonths(12))
                    .status(PayableStatus.PARTIALLY_PAID)
                    .build();

            List<ExpenseInstallment> installments = new ArrayList<>();
            for (int i = 1; i <= 24; i++) {
                LocalDate due = today.minusMonths(12).plusMonths(i);
                boolean isPast = due.isBefore(today);

                ExpenseInstallment inst = ExpenseInstallment.builder()
                        .id(UuidCreatorUtils.generateUuidV7())
                        .payableInvoice(oltPurchase)
                        .installmentNumber(i)
                        .totalInstallments(24)
                        .dueDate(due)
                        .amount(new BigDecimal("2000.00"))
                        .interestAmount(BigDecimal.ZERO)
                        .status(isPast ? PayableStatus.PAID : PayableStatus.PENDING)
                        .paidAt(isPast ? due.atTime(14, 0).atOffset(OffsetDateTime.now().getOffset()) : null)
                        .paidAmount(isPast ? new BigDecimal("2000.00") : null)
                        .paymentMethod(isPast ? "TED_BANCARIA" : null)
                        .build();
                installments.add(inst);
            }
            oltPurchase.setInstallments(installments);
            payableInvoiceRepository.save(oltPurchase);
        }

        // 2. OPEX Contínuo - Link Trânsito IP (Últimos 12 meses pagos)
        if (caLink != null) {
            for (int i = 11; i >= 0; i--) {
                LocalDate monthDate = today.minusMonths(i);
                PayableInvoice ipTransit = PayableInvoice.builder()
                        .id(UuidCreatorUtils.generateUuidV7())
                        .supplierName("V.tal Rede Neutra & Trânsito IP")
                        .supplierDocument("43.518.256/0001-08")
                        .chartOfAccount(caLink)
                        .description(String.format("Link Trânsito IP Dedicado 10 Gbps PTT - Mês %02d/%d", monthDate.getMonthValue(), monthDate.getYear()))
                        .invoiceNumber(String.format("FT-%d%02d", monthDate.getYear(), monthDate.getMonthValue()))
                        .totalAmount(new BigDecimal("3500.00"))
                        .issueDate(monthDate.withDayOfMonth(1))
                        .status(PayableStatus.PAID)
                        .build();

                ExpenseInstallment inst = ExpenseInstallment.builder()
                        .id(UuidCreatorUtils.generateUuidV7())
                        .payableInvoice(ipTransit)
                        .installmentNumber(1)
                        .totalInstallments(1)
                        .dueDate(monthDate.withDayOfMonth(10))
                        .amount(new BigDecimal("3500.00"))
                        .interestAmount(BigDecimal.ZERO)
                        .status(PayableStatus.PAID)
                        .paidAt(monthDate.withDayOfMonth(10).atTime(11, 30).atOffset(OffsetDateTime.now().getOffset()))
                        .paidAmount(new BigDecimal("3500.00"))
                        .paymentMethod("PIX_EMPRESARIAL")
                        .build();

                ipTransit.getInstallments().add(inst);
                payableInvoiceRepository.save(ipTransit);
            }
        }

        // 3. OPEX Contínuo - Compartilhamento de Postes (Últimos 12 meses pagos)
        if (caPostes != null) {
            for (int i = 11; i >= 0; i--) {
                LocalDate monthDate = today.minusMonths(i);
                PayableInvoice poles = PayableInvoice.builder()
                        .id(UuidCreatorUtils.generateUuidV7())
                        .supplierName("Equatorial Energia Pará")
                        .supplierDocument("04.895.728/0001-80")
                        .chartOfAccount(caPostes)
                        .description(String.format("Aluguel de 280 Postes Concessionária - Mês %02d/%d", monthDate.getMonthValue(), monthDate.getYear()))
                        .invoiceNumber(String.format("EQ-%d%02d", monthDate.getYear(), monthDate.getMonthValue()))
                        .totalAmount(new BigDecimal("1200.00"))
                        .issueDate(monthDate.withDayOfMonth(1))
                        .status(PayableStatus.PAID)
                        .build();

                ExpenseInstallment inst = ExpenseInstallment.builder()
                        .id(UuidCreatorUtils.generateUuidV7())
                        .payableInvoice(poles)
                        .installmentNumber(1)
                        .totalInstallments(1)
                        .dueDate(monthDate.withDayOfMonth(15))
                        .amount(new BigDecimal("1200.00"))
                        .interestAmount(BigDecimal.ZERO)
                        .status(PayableStatus.PAID)
                        .paidAt(monthDate.withDayOfMonth(15).atTime(16, 0).atOffset(OffsetDateTime.now().getOffset()))
                        .paidAmount(new BigDecimal("1200.00"))
                        .paymentMethod("DEBITO_AUTOMATICO")
                        .build();

                poles.getInstallments().add(inst);
                payableInvoiceRepository.save(poles);
            }
        }
    }

    private void seedCustomersContractsInvoicesAndWorkOrders(
            Map<String, Plan> plans,
            Map<String, User> users,
            Map<String, NetworkProject> projects,
            Map<String, FtthCto> ctos) {
        LocalDate today = LocalDate.now();
        FtthCto ctoJardins = ctos.get("jardins1");
        FtthCto ctoAlvorada = ctos.get("alvorada1");

        // 1. Clientes Antigos com Histórico de 12 Meses de Faturas Pagas (MRR contínuo para o DRE)
        String[] antigosNomes = {
                "Ana Carolina Ferreira", "Bruno Henrique Castro", "Clara Beatriz Souza",
                "Daniel Oliveira Santos", "Eduarda Lima Carvalho", "Fábio Augusto Ramos"
        };

        for (int c = 0; c < antigosNomes.length; c++) {
            String nome = antigosNomes[c];
            String email = "cliente." + nome.toLowerCase().split(" ")[0] + "@gmail.com";
            String cpf = generateValidCpf(10 + c);

            Customer customer = Customer.builder()
                    .id(UuidCreatorUtils.generateUuidV7())
                    .name(nome)
                    .cpf(cpf)
                    .email(email)
                    .phone("(93) 98111-" + (1000 + c))
                    .address("Rua das Acácias, " + (100 + c * 20))
                    .city("Altamira")
                    .state("PA")
                    .zipCode("68370-000")
                    .active(true)
                    .createdAt(LocalDateTime.now().minusMonths(12))
                    .updatedAt(LocalDateTime.now().minusMonths(12))
                    .build();
            customer = customerRepository.save(customer);

            Plan plan = (c % 2 == 0) ? plans.get("300M") : plans.get("600M");

            Contract contract = Contract.builder()
                    .id(UuidCreatorUtils.generateUuidV7())
                    .customerId(customer.getId())
                    .planId(plan.getId())
                    .contractNumber(String.format("CTR-2025-%04d", c + 1))
                    .status(Contract.ContractStatus.ACTIVE)
                    .monthlyFee(plan.getPrice())
                    .dueDay(10)
                    .installationAddress(customer.getAddress())
                    .city(customer.getCity())
                    .state(customer.getState())
                    .zipCode(customer.getZipCode())
                    .ctoId(ctoJardins != null ? ctoJardins.getId() : null)
                    .ctoPortNumber(c + 1)
                    .createdAt(LocalDateTime.now().minusMonths(12))
                    .updatedAt(LocalDateTime.now().minusMonths(12))
                    .build();
            contract = contractRepository.save(contract);

            // Gerar 12 faturas pagas mês a mês retroativas
            for (int m = 11; m >= 0; m--) {
                LocalDate dueDate = today.minusMonths(m).withDayOfMonth(10);
                Invoice inv = Invoice.builder()
                        .id(UuidCreatorUtils.generateUuidV7())
                        .contractId(contract.getId())
                        .customerId(customer.getId())
                        .gatewayType("XINGUBIT_PAY")
                        .amount(plan.getPrice())
                        .discountAmount(BigDecimal.ZERO)
                        .dueDate(dueDate)
                        .status(Invoice.InvoiceStatus.PAID)
                        .paidAt(dueDate.atTime(15, 30))
                        .paidAmount(plan.getPrice())
                        .paymentMethod("PIX")
                        .protectedAgainstSuspension(false)
                        .build();
                invoiceRepository.save(inv);
            }
        }

        // 2. Clientes Intermediários (6 a 3 meses)
        String[] interNomes = { "Gabriel Santana Lima", "Helena Duarte Pires", "Igor Cavalcante", "Juliana Paes Costa" };
        for (int c = 0; c < interNomes.length; c++) {
            String nome = interNomes[c];
            String email = "cliente." + nome.toLowerCase().split(" ")[0] + "@gmail.com";
            Customer customer = Customer.builder()
                    .id(UuidCreatorUtils.generateUuidV7())
                    .name(nome)
                    .cpf(generateValidCpf(20 + c))
                    .email(email)
                    .phone("(93) 98122-" + (2000 + c))
                    .address("Avenida Alvorada, " + (200 + c * 15))
                    .city("Altamira")
                    .state("PA")
                    .zipCode("68370-000")
                    .active(true)
                    .createdAt(LocalDateTime.now().minusMonths(5))
                    .updatedAt(LocalDateTime.now().minusMonths(5))
                    .build();
            customer = customerRepository.save(customer);

            Plan plan = plans.get("600M");
            Contract contract = Contract.builder()
                    .id(UuidCreatorUtils.generateUuidV7())
                    .customerId(customer.getId())
                    .planId(plan.getId())
                    .contractNumber(String.format("CTR-2026-%04d", c + 10))
                    .status(Contract.ContractStatus.ACTIVE)
                    .monthlyFee(plan.getPrice())
                    .dueDay(15)
                    .installationAddress(customer.getAddress())
                    .city(customer.getCity())
                    .state(customer.getState())
                    .zipCode(customer.getZipCode())
                    .ctoId(ctoAlvorada != null ? ctoAlvorada.getId() : null)
                    .ctoPortNumber(c + 1)
                    .createdAt(LocalDateTime.now().minusMonths(5))
                    .updatedAt(LocalDateTime.now().minusMonths(5))
                    .build();
            contract = contractRepository.save(contract);

            for (int m = 4; m >= 0; m--) {
                LocalDate dueDate = today.minusMonths(m).withDayOfMonth(15);
                Invoice inv = Invoice.builder()
                        .id(UuidCreatorUtils.generateUuidV7())
                        .contractId(contract.getId())
                        .customerId(customer.getId())
                        .gatewayType("XINGUBIT_PAY")
                        .amount(plan.getPrice())
                        .discountAmount(BigDecimal.ZERO)
                        .dueDate(dueDate)
                        .status(Invoice.InvoiceStatus.PAID)
                        .paidAt(dueDate.atTime(10, 0))
                        .paidAmount(plan.getPrice())
                        .paymentMethod("PIX")
                        .protectedAgainstSuspension(false)
                        .build();
                invoiceRepository.save(inv);
            }
        }

        // 3. Cliente Inadimplente (Fatura vencida há 12 dias para testar auto-corte RADIUS)
        Customer inadiCustomer = Customer.builder()
                .id(UuidCreatorUtils.generateUuidV7())
                .name("Marcos Vinicius Inadimplente")
                .cpf(generateValidCpf(30))
                .email("marcos.inadimplente@gmail.com")
                .phone("(93) 98133-9999")
                .address("Rua dos Cravos, 404")
                .city("Altamira")
                .state("PA")
                .zipCode("68370-000")
                .active(true)
                .createdAt(LocalDateTime.now().minusMonths(3))
                .updatedAt(LocalDateTime.now().minusMonths(3))
                .build();
        inadiCustomer = customerRepository.save(inadiCustomer);

        Plan inadiPlan = plans.get("300M");
        Contract inadiContract = Contract.builder()
                .id(UuidCreatorUtils.generateUuidV7())
                .customerId(inadiCustomer.getId())
                .planId(inadiPlan.getId())
                .contractNumber("CTR-2026-INAD-01")
                .status(Contract.ContractStatus.SUSPENDED)
                .monthlyFee(inadiPlan.getPrice())
                .dueDay(10)
                .installationAddress(inadiCustomer.getAddress())
                .city(inadiCustomer.getCity())
                .state(inadiCustomer.getState())
                .zipCode(inadiCustomer.getZipCode())
                .ctoId(ctoJardins != null ? ctoJardins.getId() : null)
                .ctoPortNumber(7)
                .createdAt(LocalDateTime.now().minusMonths(3))
                .updatedAt(LocalDateTime.now().minusMonths(3))
                .build();
        inadiContract = contractRepository.save(inadiContract);

        Invoice inadiInvoice = Invoice.builder()
                .id(UuidCreatorUtils.generateUuidV7())
                .contractId(inadiContract.getId())
                .customerId(inadiCustomer.getId())
                .gatewayType("XINGUBIT_PAY")
                .amount(inadiPlan.getPrice())
                .discountAmount(BigDecimal.ZERO)
                .dueDate(today.minusDays(12))
                .status(Invoice.InvoiceStatus.OVERDUE)
                .protectedAgainstSuspension(false)
                .build();
        invoiceRepository.save(inadiInvoice);

        // 4. Cliente Novo com Instalação Agendada para Hoje no Despacho
        Customer agendadoCustomer = Customer.builder()
                .id(UuidCreatorUtils.generateUuidV7())
                .name("Patrícia Ribeiro (Nova Instalação)")
                .cpf(generateValidCpf(31))
                .email("patricia.ribeiro@gmail.com")
                .phone("(93) 98144-8888")
                .address("Rua das Palmeiras, 750, Bairro Jardins")
                .city("Altamira")
                .state("PA")
                .zipCode("68370-000")
                .active(true)
                .createdAt(LocalDateTime.now().minusDays(1))
                .updatedAt(LocalDateTime.now().minusDays(1))
                .build();
        agendadoCustomer = customerRepository.save(agendadoCustomer);

        Plan novoPlan = plans.get("600M");
        Contract agendadoContract = Contract.builder()
                .id(UuidCreatorUtils.generateUuidV7())
                .customerId(agendadoCustomer.getId())
                .planId(novoPlan.getId())
                .contractNumber("CTR-2026-NEW-01")
                .status(Contract.ContractStatus.PENDING_INSTALLATION)
                .monthlyFee(novoPlan.getPrice())
                .dueDay(10)
                .installationAddress(agendadoCustomer.getAddress())
                .city(agendadoCustomer.getCity())
                .state(agendadoCustomer.getState())
                .zipCode(agendadoCustomer.getZipCode())
                .ctoId(ctoJardins != null ? ctoJardins.getId() : null)
                .ctoPortNumber(8)
                .createdAt(LocalDateTime.now().minusDays(1))
                .updatedAt(LocalDateTime.now().minusDays(1))
                .build();
        agendadoContract = contractRepository.save(agendadoContract);

        WorkOrder wo = WorkOrder.builder()
                .id(UuidCreatorUtils.generateUuidV7())
                .contractId(agendadoContract.getId())
                .customerId(agendadoCustomer.getId())
                .type(WorkOrder.WorkOrderType.INSTALACAO)
                .status(WorkOrder.WorkOrderStatus.SCHEDULED)
                .scheduledDate(today)
                .scheduledPeriod("MANHA")
                .technicianName(users.get("carlos").getName())
                .notes("Instalação de fibra óptica GPON 600 Mega com ONU Wi-Fi 6")
                .standardFeeAmount(new BigDecimal("150.00"))
                .feeStatus(FeeStatus.BILLABLE)
                .createdAt(LocalDateTime.now().minusDays(1))
                .updatedAt(LocalDateTime.now())
                .build();
        workOrderRepository.save(wo);

        // 5. Cliente com Contrato Aguardando Assinatura Eletrônica Pix (/sign/:token)
        Customer pixSignCustomer = Customer.builder()
                .id(UuidCreatorUtils.generateUuidV7())
                .name("Thiago Alencar (Pendente Assinatura)")
                .cpf(generateValidCpf(32))
                .email("thiago.alencar@gmail.com")
                .phone("(93) 98155-7777")
                .address("Rua Tapajós, 320")
                .city("Altamira")
                .state("PA")
                .zipCode("68370-000")
                .active(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        pixSignCustomer = customerRepository.save(pixSignCustomer);

        Contract pixContract = Contract.builder()
                .id(UuidCreatorUtils.generateUuidV7())
                .customerId(pixSignCustomer.getId())
                .planId(plans.get("1G").getId())
                .contractNumber("CTR-2026-PIX-SIGN")
                .status(Contract.ContractStatus.DRAFT)
                .monthlyFee(plans.get("1G").getPrice())
                .dueDay(10)
                .installationAddress(pixSignCustomer.getAddress())
                .city(pixSignCustomer.getCity())
                .state(pixSignCustomer.getState())
                .zipCode(pixSignCustomer.getZipCode())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        contractRepository.save(pixContract);
    }

    /**
     * Gera CPFs matematicamente válidos e únicos para passar nas validações @ValidCpf.
     */
    private String generateValidCpf(int seed) {
        int base = 100000000 + seed * 12347;
        String s = String.format("%09d", Math.abs(base) % 800000000 + 100000000);
        int[] d = new int[9];
        for (int i = 0; i < 9; i++) {
            d[i] = s.charAt(i) - '0';
        }

        int v1 = 0;
        for (int i = 0; i < 9; i++) {
            v1 += d[i] * (10 - i);
        }
        int d1 = 11 - (v1 % 11);
        if (d1 >= 10) d1 = 0;

        int v2 = 0;
        for (int i = 0; i < 9; i++) {
            v2 += d[i] * (11 - i);
        }
        v2 += d1 * 2;
        int d2 = 11 - (v2 % 11);
        if (d2 >= 10) d2 = 0;

        return String.format("%d%d%d.%d%d%d.%d%d%d-%d%d",
                d[0], d[1], d[2], d[3], d[4], d[5], d[6], d[7], d[8], d1, d2);
    }
}
