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
    private final ChartOfAccountRepository chartOfAccountRepository;
    private final NetworkProjectRepository networkProjectRepository;
    private final FtthCtoRepository ftthCtoRepository;
    private final WorkOrderRepository workOrderRepository;
    private final ContractTemplateRepository contractTemplateRepository;
    private final SaleRepository saleRepository;
    private final StorageConfigRepository storageConfigRepository;
    private final NotificationConfigRepository notificationConfigRepository;
    private final NasRepository nasRepository;
    private final IpamSubnetRepository ipamSubnetRepository;
    private final HelpdeskTicketRepository helpdeskTicketRepository;
    private final OnuProvisioningRepository onuProvisioningRepository;
    private final FiscalCompanyRepository fiscalCompanyRepository;
    private final SerializedAssetRepository serializedAssetRepository;
    private final TrustUnblockRepository trustUnblockRepository;
    private final LegalCollectionRepository legalCollectionRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (userRepository.count() > 0) {
            log.info("⏩ [DevDataSeeder] Usuários já cadastrados. Verificando se faltam módulos a enriquecer...");
            seedMissingEntitiesIfAny();
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
        seedCustomersContractsInvoicesAndWorkOrders(plans, users, warehouses, projects, ctos);
        seedContractTemplates(company);
        seedSales(plans);
        seedStorageConfig(company);
        seedNotificationConfig(company);
        seedNas();
        seedIpam(company);
        seedFiscalCompany(company);

        log.info("✅ [DevDataSeeder] Simulação de 1 ano concluída com 100% de sucesso! Ambiente pronto para uso.");
    }

    private void seedMissingEntitiesIfAny() {
        Company company = companyRepository.findAll().stream().findFirst().orElse(null);
        if (company == null) return;

        Map<String, Plan> plans = new HashMap<>();
        for (Plan p : planRepository.findAll()) {
            if (p.getName().contains("300")) plans.put("300M", p);
            if (p.getName().contains("600")) plans.put("600M", p);
            if (p.getName().contains("1 Giga")) plans.put("1G", p);
        }

        if (contractTemplateRepository.count() == 0) {
            seedContractTemplates(company);
        }
        if (saleRepository.count() == 0 && !plans.isEmpty()) {
            seedSales(plans);
        }
        if (storageConfigRepository.count() == 0) {
            seedStorageConfig(company);
        }
        if (notificationConfigRepository.count() == 0) {
            seedNotificationConfig(company);
        }
        if (nasRepository.count() == 0) {
            seedNas();
        }
        if (ipamSubnetRepository.count() == 0) {
            seedIpam(company);
        }
        if (helpdeskTicketRepository.count() == 0) {
            Customer c1 = customerRepository.findAll().stream().findFirst().orElse(null);
            Contract ctr1 = contractRepository.findAll().stream().findFirst().orElse(null);
            User maria = userRepository.findByEmail("atendente.maria@nexusfibra.com.br").orElse(null);
            if (c1 != null && ctr1 != null && maria != null) {
                seedHelpdeskTickets(c1, ctr1, maria);
            }
        }
        if (onuProvisioningRepository.count() == 0) {
            Contract ctr1 = contractRepository.findAll().stream().findFirst().orElse(null);
            Customer c1 = customerRepository.findAll().stream().findFirst().orElse(null);
            FtthCto cto1 = ftthCtoRepository.findAll().stream().findFirst().orElse(null);
            if (ctr1 != null && c1 != null && cto1 != null) {
                seedOnus(cto1, ctr1, c1);
            }
        }
        if (fiscalCompanyRepository.count() == 0) {
            seedFiscalCompany(company);
        }
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

        // Ativos Serializados Rastreáveis (SerializedAsset) no Almoxarifado e com Técnicos
        Warehouse central = warehouses.get("central");
        Warehouse v1 = warehouses.get("v1");
        Warehouse v2 = warehouses.get("v2");

        for (int i = 1; i <= 3; i++) {
            serializedAssetRepository.save(SerializedAsset.builder()
                    .id(UuidCreatorUtils.generateUuidV7())
                    .serialNumber(String.format("HWTC-CENTRAL-%04d", i))
                    .macAddress(String.format("48:57:02:10:00:%02X", i))
                    .brandModel("Huawei EG8145V5")
                    .category(SerializedAsset.AssetCategory.ONU_ONT)
                    .replacementValue(new BigDecimal("420.00"))
                    .currentWarehouseId(central.getId())
                    .status(SerializedAsset.AssetStatus.DISPONIVEL_DEPOSITO)
                    .build());

            serializedAssetRepository.save(SerializedAsset.builder()
                    .id(UuidCreatorUtils.generateUuidV7())
                    .serialNumber(String.format("ZTEG-CENTRAL-%04d", i))
                    .macAddress(String.format("68:DB:54:20:00:%02X", i))
                    .brandModel("ZTE F670L")
                    .category(SerializedAsset.AssetCategory.ONU_ONT)
                    .replacementValue(new BigDecimal("420.00"))
                    .currentWarehouseId(central.getId())
                    .status(SerializedAsset.AssetStatus.DISPONIVEL_DEPOSITO)
                    .build());
        }

        serializedAssetRepository.save(SerializedAsset.builder()
                .id(UuidCreatorUtils.generateUuidV7())
                .serialNumber("HWTC12345678")
                .macAddress("48:57:02:11:22:33")
                .brandModel("Huawei EG8145V5")
                .category(SerializedAsset.AssetCategory.ONU_ONT)
                .replacementValue(new BigDecimal("420.00"))
                .currentWarehouseId(v1.getId())
                .currentHolderUserId(carlos.getId())
                .status(SerializedAsset.AssetStatus.CUSTODIA_COLABORADOR)
                .build());

        serializedAssetRepository.save(SerializedAsset.builder()
                .id(UuidCreatorUtils.generateUuidV7())
                .serialNumber("FJK-994820")
                .brandModel("Fujikura 70S")
                .category(SerializedAsset.AssetCategory.TOOL_FUSION_MACHINE)
                .replacementValue(new BigDecimal("18500.00"))
                .currentWarehouseId(v1.getId())
                .currentHolderUserId(carlos.getId())
                .status(SerializedAsset.AssetStatus.CUSTODIA_COLABORADOR)
                .build());

        serializedAssetRepository.save(SerializedAsset.builder()
                .id(UuidCreatorUtils.generateUuidV7())
                .serialNumber("INNO-771122")
                .brandModel("Inno View 5")
                .category(SerializedAsset.AssetCategory.TOOL_FUSION_MACHINE)
                .replacementValue(new BigDecimal("14200.00"))
                .currentWarehouseId(v2.getId())
                .currentHolderUserId(marcos.getId())
                .status(SerializedAsset.AssetStatus.CUSTODIA_COLABORADOR)
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
            Map<String, Warehouse> warehouses,
            Map<String, NetworkProject> projects,
            Map<String, FtthCto> ctos) {
        LocalDate today = LocalDate.now();
        FtthCto ctoJardins = ctos.get("jardins1");
        Warehouse centralWarehouse = warehouses.get("central");

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

            if (c == 0) {
                seedHelpdeskTickets(customer, contract, users.get("maria"));
                seedOnus(ctoJardins, contract, customer);
            }

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

        // =========================================================================
        // MATRIZ DOS 7 CLIENTES DO CICLO DE VIDA OPERACIONAL DE PONTA A PONTA
        // =========================================================================

        // -------------------------------------------------------------------------
        // CENÁRIO 1: Lucas Andrade (Nova Venda e Instalação Agendada para Hoje)
        // -------------------------------------------------------------------------
        Customer c1 = Customer.builder()
                .id(UuidCreatorUtils.generateUuidV7())
                .name("Lucas Andrade (Venda e Agendamento)")
                .cpf(generateValidCpf(41))
                .email("lucas.andrade@gmail.com")
                .phone("(93) 98141-1111")
                .address("Rua das Mangueiras, 305, Bairro Jardins")
                .city("Altamira")
                .state("PA")
                .zipCode("68370-000")
                .active(true)
                .createdAt(LocalDateTime.now().minusDays(1))
                .updatedAt(LocalDateTime.now().minusDays(1))
                .build();
        c1 = customerRepository.save(c1);

        Plan p600 = plans.get("600M");
        Contract ctr1 = Contract.builder()
                .id(UuidCreatorUtils.generateUuidV7())
                .customerId(c1.getId())
                .planId(p600.getId())
                .contractNumber("CTR-2026-CICLO-01")
                .status(Contract.ContractStatus.PENDING_INSTALLATION)
                .monthlyFee(p600.getPrice())
                .dueDay(10)
                .installationAddress(c1.getAddress())
                .city(c1.getCity())
                .state(c1.getState())
                .zipCode(c1.getZipCode())
                .ctoId(ctoJardins != null ? ctoJardins.getId() : null)
                .ctoPortNumber(7)
                .createdAt(LocalDateTime.now().minusDays(1))
                .updatedAt(LocalDateTime.now().minusDays(1))
                .build();
        ctr1 = contractRepository.save(ctr1);

        WorkOrder wo1 = WorkOrder.builder()
                .id(UuidCreatorUtils.generateUuidV7())
                .contractId(ctr1.getId())
                .customerId(c1.getId())
                .type(WorkOrder.WorkOrderType.INSTALACAO)
                .status(WorkOrder.WorkOrderStatus.SCHEDULED)
                .scheduledDate(today)
                .scheduledPeriod("MANHA")
                .technicianName(users.get("carlos").getName())
                .notes("Instalação de fibra óptica GPON 600 Mega com ONU Wi-Fi 6 AX3000. Venda balcão ontem.")
                .standardFeeAmount(new BigDecimal("150.00"))
                .feeStatus(FeeStatus.BILLABLE)
                .createdAt(LocalDateTime.now().minusDays(1))
                .updatedAt(LocalDateTime.now())
                .build();
        workOrderRepository.save(wo1);

        // -------------------------------------------------------------------------
        // CENÁRIO 2: Beatriz Santos (Instalação Concluída, Ativa e Adimplente)
        // -------------------------------------------------------------------------
        Customer c2 = Customer.builder()
                .id(UuidCreatorUtils.generateUuidV7())
                .name("Beatriz Santos (Ativa 600M)")
                .cpf(generateValidCpf(42))
                .email("beatriz.santos@gmail.com")
                .phone("(93) 98142-2222")
                .address("Av. Brigadeiro Eduardo Gomes, 1200")
                .city("Altamira")
                .state("PA")
                .zipCode("68370-000")
                .active(true)
                .createdAt(LocalDateTime.now().minusMonths(3))
                .updatedAt(LocalDateTime.now().minusMonths(3))
                .build();
        c2 = customerRepository.save(c2);

        Contract ctr2 = Contract.builder()
                .id(UuidCreatorUtils.generateUuidV7())
                .customerId(c2.getId())
                .planId(p600.getId())
                .contractNumber("CTR-2026-CICLO-02")
                .status(Contract.ContractStatus.ACTIVE)
                .monthlyFee(p600.getPrice())
                .dueDay(10)
                .installationAddress(c2.getAddress())
                .city(c2.getCity())
                .state(c2.getState())
                .zipCode(c2.getZipCode())
                .ctoId(ctoJardins != null ? ctoJardins.getId() : null)
                .ctoPortNumber(8)
                .createdAt(LocalDateTime.now().minusMonths(3))
                .updatedAt(LocalDateTime.now().minusMonths(3))
                .build();
        ctr2 = contractRepository.save(ctr2);

        WorkOrder wo2 = WorkOrder.builder()
                .id(UuidCreatorUtils.generateUuidV7())
                .contractId(ctr2.getId())
                .customerId(c2.getId())
                .type(WorkOrder.WorkOrderType.INSTALACAO)
                .status(WorkOrder.WorkOrderStatus.COMPLETED)
                .scheduledDate(today.minusMonths(3))
                .completedAt(LocalDateTime.now().minusMonths(3))
                .technicianName(users.get("carlos").getName())
                .notes("Instalação concluída com sucesso. Potência óptica -19.20 dBm na porta 8 da CTO Jardins 01.")
                .standardFeeAmount(new BigDecimal("150.00"))
                .feeStatus(FeeStatus.NOT_APPLICABLE)
                .build();
        workOrderRepository.save(wo2);

        serializedAssetRepository.save(SerializedAsset.builder()
                .id(UuidCreatorUtils.generateUuidV7())
                .serialNumber("HWTC-BEA-6001")
                .macAddress("48:57:02:AA:10:01")
                .brandModel("Huawei EG8145V5")
                .category(SerializedAsset.AssetCategory.ONU_ONT)
                .replacementValue(new BigDecimal("420.00"))
                .currentCustomerId(c2.getId())
                .currentContractId(ctr2.getId())
                .status(SerializedAsset.AssetStatus.INSTALADO_CLIENTE)
                .build());

        onuProvisioningRepository.save(OnuProvisioning.builder()
                .id(UuidCreatorUtils.generateUuidV7())
                .contractId(ctr2.getId())
                .customerId(c2.getId())
                .onuMac("48:57:02:AA:10:01")
                .onuSerial("HWTC-BEA-6001")
                .vlanId(100)
                .pppoeUser("beatriz.santos@nexusfibra")
                .pppoePassword("nexus2026")
                .downloadSpeed(600)
                .uploadSpeed(300)
                .rxPowerDbm(new BigDecimal("-19.20"))
                .status(OnuProvisioning.OnuStatus.PROVISIONED)
                .build());

        for (int m = 2; m >= 0; m--) {
            LocalDate dueDate = today.minusMonths(m).withDayOfMonth(10);
            Invoice inv = Invoice.builder()
                    .id(UuidCreatorUtils.generateUuidV7())
                    .contractId(ctr2.getId())
                    .customerId(c2.getId())
                    .gatewayType("XINGUBIT_PAY")
                    .amount(p600.getPrice())
                    .discountAmount(BigDecimal.ZERO)
                    .dueDate(dueDate)
                    .status(Invoice.InvoiceStatus.PAID)
                    .paidAt(dueDate.atTime(11, 0))
                    .paidAmount(p600.getPrice())
                    .paymentMethod("PIX")
                    .protectedAgainstSuspension(false)
                    .build();
            invoiceRepository.save(inv);
        }

        // -------------------------------------------------------------------------
        // CENÁRIO 3: Fernando Lima (Inadimplente 7 dias, Auto-Corte, Elegível Desbloqueio)
        // -------------------------------------------------------------------------
        Customer c3 = Customer.builder()
                .id(UuidCreatorUtils.generateUuidV7())
                .name("Fernando Lima (Auto-Corte 7d)")
                .cpf(generateValidCpf(43))
                .email("fernando.lima@gmail.com")
                .phone("(93) 98143-3333")
                .address("Rua Sete de Setembro, 880")
                .city("Altamira")
                .state("PA")
                .zipCode("68370-000")
                .active(true)
                .createdAt(LocalDateTime.now().minusMonths(4))
                .updatedAt(LocalDateTime.now().minusMonths(4))
                .build();
        c3 = customerRepository.save(c3);

        Plan p300 = plans.get("300M");
        Contract ctr3 = Contract.builder()
                .id(UuidCreatorUtils.generateUuidV7())
                .customerId(c3.getId())
                .planId(p300.getId())
                .contractNumber("CTR-2026-CICLO-03")
                .status(Contract.ContractStatus.SUSPENDED)
                .monthlyFee(p300.getPrice())
                .dueDay(10)
                .installationAddress(c3.getAddress())
                .city(c3.getCity())
                .state(c3.getState())
                .zipCode(c3.getZipCode())
                .ctoId(ctoJardins != null ? ctoJardins.getId() : null)
                .ctoPortNumber(9)
                .createdAt(LocalDateTime.now().minusMonths(4))
                .updatedAt(LocalDateTime.now().minusMonths(4))
                .build();
        ctr3 = contractRepository.save(ctr3);

        serializedAssetRepository.save(SerializedAsset.builder()
                .id(UuidCreatorUtils.generateUuidV7())
                .serialNumber("ZTEG-FER-7002")
                .macAddress("68:DB:54:BB:20:02")
                .brandModel("ZTE F670L")
                .category(SerializedAsset.AssetCategory.ONU_ONT)
                .replacementValue(new BigDecimal("420.00"))
                .currentCustomerId(c3.getId())
                .currentContractId(ctr3.getId())
                .status(SerializedAsset.AssetStatus.INSTALADO_CLIENTE)
                .build());

        Invoice inv3 = Invoice.builder()
                .id(UuidCreatorUtils.generateUuidV7())
                .contractId(ctr3.getId())
                .customerId(c3.getId())
                .gatewayType("XINGUBIT_PAY")
                .amount(p300.getPrice())
                .discountAmount(BigDecimal.ZERO)
                .dueDate(today.minusDays(7))
                .status(Invoice.InvoiceStatus.OVERDUE)
                .protectedAgainstSuspension(false)
                .build();
        invoiceRepository.save(inv3);

        // -------------------------------------------------------------------------
        // CENÁRIO 4: Juliana Rocha (Em Desbloqueio em Confiança Ativo de 48h)
        // -------------------------------------------------------------------------
        Customer c4 = Customer.builder()
                .id(UuidCreatorUtils.generateUuidV7())
                .name("Juliana Rocha (Desbloqueio Confiança)")
                .cpf(generateValidCpf(44))
                .email("juliana.rocha@gmail.com")
                .phone("(93) 98144-4444")
                .address("Rua das Orquídeas, 210")
                .city("Altamira")
                .state("PA")
                .zipCode("68370-000")
                .active(true)
                .createdAt(LocalDateTime.now().minusMonths(2))
                .updatedAt(LocalDateTime.now().minusMonths(2))
                .build();
        c4 = customerRepository.save(c4);

        Contract ctr4 = Contract.builder()
                .id(UuidCreatorUtils.generateUuidV7())
                .customerId(c4.getId())
                .planId(p600.getId())
                .contractNumber("CTR-2026-CICLO-04")
                .status(Contract.ContractStatus.ACTIVE) // Restabelecida temporariamente pela política de 48h
                .monthlyFee(p600.getPrice())
                .dueDay(10)
                .installationAddress(c4.getAddress())
                .city(c4.getCity())
                .state(c4.getState())
                .zipCode(c4.getZipCode())
                .ctoId(ctoJardins != null ? ctoJardins.getId() : null)
                .ctoPortNumber(10)
                .createdAt(LocalDateTime.now().minusMonths(2))
                .updatedAt(LocalDateTime.now().minusMonths(2))
                .build();
        ctr4 = contractRepository.save(ctr4);

        serializedAssetRepository.save(SerializedAsset.builder()
                .id(UuidCreatorUtils.generateUuidV7())
                .serialNumber("HWTC-JUL-4803")
                .macAddress("48:57:02:CC:30:03")
                .brandModel("Huawei EG8145V5")
                .category(SerializedAsset.AssetCategory.ONU_ONT)
                .replacementValue(new BigDecimal("420.00"))
                .currentCustomerId(c4.getId())
                .currentContractId(ctr4.getId())
                .status(SerializedAsset.AssetStatus.INSTALADO_CLIENTE)
                .build());

        Invoice inv4 = Invoice.builder()
                .id(UuidCreatorUtils.generateUuidV7())
                .contractId(ctr4.getId())
                .customerId(c4.getId())
                .gatewayType("XINGUBIT_PAY")
                .amount(p600.getPrice())
                .discountAmount(BigDecimal.ZERO)
                .dueDate(today.minusDays(9))
                .status(Invoice.InvoiceStatus.OVERDUE)
                .protectedAgainstSuspension(true)
                .build();
        invoiceRepository.save(inv4);

        trustUnblockRepository.save(TrustUnblock.builder()
                .id(UuidCreatorUtils.generateUuidV7())
                .contractId(ctr4.getId())
                .invoiceId(inv4.getId())
                .requestedAt(LocalDateTime.now().minusHours(20))
                .expiresAt(LocalDateTime.now().plusHours(28))
                .unblockType("BOT_AUTO")
                .status("ACTIVE")
                .build());

        // -------------------------------------------------------------------------
        // CENÁRIO 5: Roberto Silva (Inadimplente 32 dias, O.S. de Retirada Aberta)
        // -------------------------------------------------------------------------
        Customer c5 = Customer.builder()
                .id(UuidCreatorUtils.generateUuidV7())
                .name("Roberto Silva (Retirada 30d+)")
                .cpf(generateValidCpf(45))
                .email("roberto.silva@gmail.com")
                .phone("(93) 98145-5555")
                .address("Rua Coronel Mota, 540")
                .city("Altamira")
                .state("PA")
                .zipCode("68370-000")
                .active(true)
                .createdAt(LocalDateTime.now().minusMonths(5))
                .updatedAt(LocalDateTime.now().minusMonths(5))
                .build();
        c5 = customerRepository.save(c5);

        Contract ctr5 = Contract.builder()
                .id(UuidCreatorUtils.generateUuidV7())
                .customerId(c5.getId())
                .planId(p300.getId())
                .contractNumber("CTR-2026-CICLO-05")
                .status(Contract.ContractStatus.SUSPENDED)
                .monthlyFee(p300.getPrice())
                .dueDay(10)
                .installationAddress(c5.getAddress())
                .city(c5.getCity())
                .state(c5.getState())
                .zipCode(c5.getZipCode())
                .ctoId(ctoJardins != null ? ctoJardins.getId() : null)
                .ctoPortNumber(11)
                .createdAt(LocalDateTime.now().minusMonths(5))
                .updatedAt(LocalDateTime.now().minusMonths(5))
                .build();
        ctr5 = contractRepository.save(ctr5);

        serializedAssetRepository.save(SerializedAsset.builder()
                .id(UuidCreatorUtils.generateUuidV7())
                .serialNumber("ZTEG-ROB-3204")
                .macAddress("68:DB:54:DD:40:04")
                .brandModel("ZTE F670L")
                .category(SerializedAsset.AssetCategory.ONU_ONT)
                .replacementValue(new BigDecimal("420.00"))
                .currentCustomerId(c5.getId())
                .currentContractId(ctr5.getId())
                .status(SerializedAsset.AssetStatus.INSTALADO_CLIENTE)
                .build());

        Invoice inv5 = Invoice.builder()
                .id(UuidCreatorUtils.generateUuidV7())
                .contractId(ctr5.getId())
                .customerId(c5.getId())
                .gatewayType("XINGUBIT_PAY")
                .amount(p300.getPrice())
                .discountAmount(BigDecimal.ZERO)
                .dueDate(today.minusDays(32))
                .status(Invoice.InvoiceStatus.OVERDUE)
                .protectedAgainstSuspension(false)
                .build();
        invoiceRepository.save(inv5);

        WorkOrder wo5 = WorkOrder.builder()
                .id(UuidCreatorUtils.generateUuidV7())
                .contractId(ctr5.getId())
                .customerId(c5.getId())
                .type(WorkOrder.WorkOrderType.RETIRADA)
                .status(WorkOrder.WorkOrderStatus.SCHEDULED)
                .scheduledDate(today)
                .scheduledPeriod("TARDE")
                .technicianName(users.get("carlos").getName())
                .notes("Ordem de serviço de retirada gerada automaticamente por inadimplência superior a 30 dias (Fatura vencida há 32 dias).")
                .standardFeeAmount(BigDecimal.ZERO)
                .feeStatus(FeeStatus.NOT_APPLICABLE)
                .createdAt(LocalDateTime.now().minusDays(1))
                .updatedAt(LocalDateTime.now())
                .build();
        workOrderRepository.save(wo5);

        // -------------------------------------------------------------------------
        // CENÁRIO 6: Mariana Costa (Retirada Concluída com Sucesso, Equipamento Devolvido)
        // -------------------------------------------------------------------------
        Customer c6 = Customer.builder()
                .id(UuidCreatorUtils.generateUuidV7())
                .name("Mariana Costa (Retirada Concluída)")
                .cpf(generateValidCpf(46))
                .email("mariana.costa@gmail.com")
                .phone("(93) 98146-6666")
                .address("Rua da Paz, 99")
                .city("Altamira")
                .state("PA")
                .zipCode("68370-000")
                .active(true)
                .createdAt(LocalDateTime.now().minusMonths(6))
                .updatedAt(LocalDateTime.now().minusMonths(6))
                .build();
        c6 = customerRepository.save(c6);

        Contract ctr6 = Contract.builder()
                .id(UuidCreatorUtils.generateUuidV7())
                .customerId(c6.getId())
                .planId(p600.getId())
                .contractNumber("CTR-2026-CICLO-06")
                .status(Contract.ContractStatus.CANCELED)
                .monthlyFee(p600.getPrice())
                .dueDay(10)
                .installationAddress(c6.getAddress())
                .city(c6.getCity())
                .state(c6.getState())
                .zipCode(c6.getZipCode())
                .ctoId(ctoJardins != null ? ctoJardins.getId() : null)
                .ctoPortNumber(12)
                .createdAt(LocalDateTime.now().minusMonths(6))
                .updatedAt(LocalDateTime.now().minusDays(2))
                .build();
        ctr6 = contractRepository.save(ctr6);

        WorkOrder wo6 = WorkOrder.builder()
                .id(UuidCreatorUtils.generateUuidV7())
                .contractId(ctr6.getId())
                .customerId(c6.getId())
                .type(WorkOrder.WorkOrderType.RETIRADA)
                .status(WorkOrder.WorkOrderStatus.COMPLETED)
                .scheduledDate(today.minusDays(2))
                .completedAt(LocalDateTime.now().minusDays(2))
                .technicianName(users.get("andre").getName())
                .notes("ONT recolhida com sucesso em perfeitas condições. Acessórios e fonte conferidos. Contrato rescindido e logística reversa concluída.")
                .standardFeeAmount(BigDecimal.ZERO)
                .feeStatus(FeeStatus.NOT_APPLICABLE)
                .build();
        workOrderRepository.save(wo6);

        serializedAssetRepository.save(SerializedAsset.builder()
                .id(UuidCreatorUtils.generateUuidV7())
                .serialNumber("HWTC-MAR-9905")
                .macAddress("48:57:02:EE:50:05")
                .brandModel("Huawei EG8145V5")
                .category(SerializedAsset.AssetCategory.ONU_ONT)
                .replacementValue(new BigDecimal("420.00"))
                .currentWarehouseId(centralWarehouse != null ? centralWarehouse.getId() : null)
                .currentCustomerId(null)
                .currentContractId(null)
                .status(SerializedAsset.AssetStatus.DISPONIVEL_DEPOSITO) // Retornou ao depósito central!
                .build());

        // -------------------------------------------------------------------------
        // CENÁRIO 7: Rodrigo Mendes (Retirada Infrutífera -> Jurídico / SPC/Serasa)
        // -------------------------------------------------------------------------
        Customer c7 = Customer.builder()
                .id(UuidCreatorUtils.generateUuidV7())
                .name("Rodrigo Mendes (Cobrança Jurídica / SPC)")
                .cpf(generateValidCpf(47))
                .email("rodrigo.mendes@gmail.com")
                .phone("(93) 98147-7777")
                .address("Rua Travessa Treze, 700")
                .city("Altamira")
                .state("PA")
                .zipCode("68370-000")
                .active(true)
                .createdAt(LocalDateTime.now().minusMonths(6))
                .updatedAt(LocalDateTime.now().minusDays(3))
                .build();
        c7 = customerRepository.save(c7);

        Contract ctr7 = Contract.builder()
                .id(UuidCreatorUtils.generateUuidV7())
                .customerId(c7.getId())
                .planId(p600.getId())
                .contractNumber("CTR-2026-CICLO-07")
                .status(Contract.ContractStatus.CANCELED)
                .monthlyFee(p600.getPrice())
                .dueDay(10)
                .installationAddress(c7.getAddress())
                .city(c7.getCity())
                .state(c7.getState())
                .zipCode(c7.getZipCode())
                .ctoId(ctoJardins != null ? ctoJardins.getId() : null)
                .ctoPortNumber(13)
                .createdAt(LocalDateTime.now().minusMonths(6))
                .updatedAt(LocalDateTime.now().minusDays(3))
                .build();
        ctr7 = contractRepository.save(ctr7);

        Invoice inv7a = Invoice.builder()
                .id(UuidCreatorUtils.generateUuidV7())
                .contractId(ctr7.getId())
                .customerId(c7.getId())
                .gatewayType("XINGUBIT_PAY")
                .amount(p600.getPrice())
                .discountAmount(BigDecimal.ZERO)
                .dueDate(today.minusDays(45))
                .status(Invoice.InvoiceStatus.OVERDUE)
                .protectedAgainstSuspension(false)
                .build();
        invoiceRepository.save(inv7a);

        Invoice inv7b = Invoice.builder()
                .id(UuidCreatorUtils.generateUuidV7())
                .contractId(ctr7.getId())
                .customerId(c7.getId())
                .gatewayType("XINGUBIT_PAY")
                .amount(p600.getPrice())
                .discountAmount(BigDecimal.ZERO)
                .dueDate(today.minusDays(15))
                .status(Invoice.InvoiceStatus.OVERDUE)
                .protectedAgainstSuspension(false)
                .build();
        invoiceRepository.save(inv7b);

        WorkOrder wo7 = WorkOrder.builder()
                .id(UuidCreatorUtils.generateUuidV7())
                .contractId(ctr7.getId())
                .customerId(c7.getId())
                .type(WorkOrder.WorkOrderType.RETIRADA)
                .status(WorkOrder.WorkOrderStatus.INFRUTIFERA)
                .unsuccessReason("CLIENTE_MUDOU_SE")
                .scheduledDate(today.minusDays(3))
                .completedAt(LocalDateTime.now().minusDays(3))
                .technicianName(users.get("marcos").getName())
                .notes("Tentativa de recolhimento frustrada. Imóvel desocupado e placa de Aluga-se. Vizinhos confirmaram mudança sem aviso prévio.")
                .standardFeeAmount(BigDecimal.ZERO)
                .feeStatus(FeeStatus.NOT_APPLICABLE)
                .build();
        workOrderRepository.save(wo7);

        serializedAssetRepository.save(SerializedAsset.builder()
                .id(UuidCreatorUtils.generateUuidV7())
                .serialNumber("HWTC-ROD-8806")
                .macAddress("48:57:02:FF:60:06")
                .brandModel("Huawei EG8145V5")
                .category(SerializedAsset.AssetCategory.ONU_ONT)
                .replacementValue(new BigDecimal("420.00"))
                .currentCustomerId(c7.getId())
                .currentContractId(ctr7.getId())
                .status(SerializedAsset.AssetStatus.INSTALADO_CLIENTE) // Não recuperada
                .build());

        legalCollectionRepository.save(LegalCollectionRecord.builder()
                .id(UuidCreatorUtils.generateUuidV7())
                .customerId(c7.getId())
                .contractId(ctr7.getId())
                .workOrderId(wo7.getId())
                .totalDebtInvoices(new BigDecimal("199.80"))
                .equipmentIndemnityAmount(new BigDecimal("420.00"))
                .totalClaimAmount(new BigDecimal("619.80"))
                .status(LegalCollectionRecord.LegalCollectionStatus.PENDING_BUREAU_SUBMISSION)
                .evidenceNotes("Retirada de ONT frustrada por mudança sem notificação prévia. Débito consolidado: 2 faturas em aberto (R$ 199,80) + Indenização de comodato ONT Huawei SN HWTC-ROD-8806 (R$ 420,00). Encaminhado para negativação e cobrança extrajudicial.")
                .build());

        // -------------------------------------------------------------------------
        // CLIENTE DE TESTE DE ASSINATURA ELETRÔNICA PIX (/sign/:token)
        // -------------------------------------------------------------------------
        Customer pixSignCustomer = Customer.builder()
                .id(UuidCreatorUtils.generateUuidV7())
                .name("Thiago Alencar (Pendente Assinatura)")
                .cpf(generateValidCpf(48))
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

    private void seedContractTemplates(Company company) {
        ContractTemplate template1 = ContractTemplate.builder()
                .companyId(company.getId())
                .name("Contrato de Adesão SCM & SVA (Nexus Fibra)")
                .documentType(br.dev.xb.isperp.signature.DocumentType.SERVICE_AGREEMENT)
                .version(1)
                .isActive(true)
                .contentMarkdown("""
                        # TERMO DE ADESÃO AO CONTRATO DE PRESTAÇÃO DE SERVIÇOS SCM E SVA

                        **CONTRATADA:** {{company.name}}, CNPJ {{company.cnpj}}, com sede em {{company.address}}.
                        **CONTRATANTE:** {{customer.name}}, CPF/CNPJ {{customer.cpf_cnpj}}, residente em {{contract.installation_address}}.

                        ### 1. DO OBJETO
                        O presente instrumento tem por objeto a prestação contínua de Serviços de Comunicação Multimídia (SCM) e Conexão à Internet em Fibra Óptica no plano **{{plan.name}}**, com velocidade de download de **{{plan.download_speed}} Mbps** e upload de **{{plan.upload_speed}} Mbps**, pelo valor mensal contratado de **R$ {{plan.price}}**.

                        ### 2. DA COBRANÇA E DO PAGAMENTO
                        A mensalidade vencerá todo dia **{{contract.due_day}}** de cada mês, devendo ser quitada via Pix Instantâneo com Baixa Automática ou Boleto Bancário disponibilizado na Central do Assinante.

                        ### 3. DO COMODATO DE EQUIPAMENTOS
                        A CONTRATADA cede ao CONTRATANTE, a título de comodato gratuito, a Unidade de Rede Óptica (ONT Wi-Fi) com porta Gigabit e Wi-Fi integrado, a qual deverá ser devolvida em perfeito estado de conservação em caso de rescisão.

                        ### 4. DA ASSINATURA ELETRÔNICA AVANÇADA
                        As partes reconhecem expressamente a plena validade jurídica deste documento assinado digitalmente, nos termos da Medida Provisória nº 2.200-2/2001 e da Lei Federal nº 14.063/2020.
                        """)
                .consentClause("Declaro que li e concordo integralmente com as cláusulas deste Contrato de Prestação de Serviços SCM e Comodato de Equipamentos.")
                .build();
        contractTemplateRepository.save(template1);

        ContractTemplate template2 = ContractTemplate.builder()
                .companyId(company.getId())
                .name("Termo de Fidelidade Contratual 12 Meses")
                .documentType(br.dev.xb.isperp.signature.DocumentType.LOYALTY_TERM)
                .version(1)
                .isActive(true)
                .contentMarkdown("""
                        # TERMO DE FIDELIDADE CONTRATUAL E BENEFÍCIO DE INSTALAÇÃO

                        Em virtude do benefício concedido pela CONTRATADA na isenção de 100% da taxa de instalação e ativação da fibra óptica (no valor de R$ 350,00), o CONTRATANTE compromete-se a manter o plano ativo pelo período mínimo de 12 (doze) meses.
                        """)
                .consentClause("Aceito a cláusula de permanência mínima de 12 meses em contrapartida à isenção da taxa de instalação.")
                .build();
        contractTemplateRepository.save(template2);
    }

    private void seedSales(Map<String, Plan> plans) {
        Plan p300 = plans.get("300M");
        Plan p600 = plans.get("600M");

        Sale sale1 = Sale.builder()
                .planId(p300.getId())
                .customerName("Patrícia Ribeiro")
                .customerCpf("111.222.333-44")
                .customerEmail("patricia.ribeiro@gmail.com")
                .customerPhone("(93) 99123-4567")
                .installationAddress("Rua das Acácias, 102 - Bairro Jardins")
                .city("Altamira")
                .state("PA")
                .zipCode("68371-000")
                .preferredDueDate(10)
                .notificationChannel("WHATSAPP")
                .sellerName("Loja Central - Balcão")
                .status(Sale.SaleStatus.SUBMITTED)
                .build();
        saleRepository.save(sale1);

        Sale sale2 = Sale.builder()
                .planId(p600.getId())
                .customerName("Thiago Alencar")
                .customerCpf("222.333.444-55")
                .customerEmail("thiago.alencar@hotmail.com")
                .customerPhone("(93) 98456-7890")
                .installationAddress("Av. João Pessoa, 450 - Centro")
                .city("Altamira")
                .state("PA")
                .zipCode("68370-000")
                .preferredDueDate(15)
                .notificationChannel("WHATSAPP")
                .sellerName("Vendedor Externo - Porta a Porta")
                .status(Sale.SaleStatus.PROCESSED)
                .build();
        saleRepository.save(sale2);
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

    private void seedStorageConfig(Company company) {
        StorageConfig storage = StorageConfig.builder()
                .id(UuidCreatorUtils.generateUuidV7())
                .companyId(company.getId())
                .storageType(br.dev.xb.isperp.storage.StorageType.S3)
                .provider(br.dev.xb.isperp.storage.StorageProvider.SEAWEEDFS_LOCAL)
                .bucketName("isperp-documents")
                .endpointUrl("http://seaweedfs:8333")
                .region("us-east-1")
                .accessKey("seaweed_access_key")
                .secretKey("seaweed_secret_key")
                .isActive(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        storageConfigRepository.save(storage);
    }

    private void seedNotificationConfig(Company company) {
        NotificationConfig notif = NotificationConfig.builder()
                .id(UuidCreatorUtils.generateUuidV7())
                .companyId(company.getId())
                .name("WhatsApp Principal - Nexus Fibra")
                .providerType(br.dev.xb.isperp.notification.whatsapp.WhatsAppProviderType.EVOLUTION_API)
                .apiUrl("https://evolution.nexusfibra.com.br")
                .apiToken("nexus-evo-secret-token-2026")
                .fromPhoneNumber("+5593984012000")
                .active(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        notificationConfigRepository.save(notif);
    }

    private void seedNas() {
        Nas bng = Nas.builder()
                .id(UuidCreatorUtils.generateUuidV7())
                .nasname("10.0.0.1")
                .shortname("BNG-MIKROTIK-SEDE")
                .type("mikrotik")
                .ports(1812)
                .secret("NexusRadiusSecret2026")
                .description("Concentrador BNG CCR2004 - Pop Sede Altamira")
                .build();
        nasRepository.save(bng);
    }

    private void seedIpam(Company company) {
        IpamSubnet subnetCgnat = IpamSubnet.builder()
                .id(UuidCreatorUtils.generateUuidV7())
                .companyId(company.getId())
                .cidr("100.64.0.0/20")
                .ipVersion(br.dev.xb.isperp.ipam.IpamIpVersion.IPV4)
                .networkAddress("100.64.0.0")
                .broadcastAddress("100.64.15.255")
                .prefixLength(20)
                .category(br.dev.xb.isperp.ipam.IpamSubnetCategory.CGNAT)
                .status(br.dev.xb.isperp.ipam.IpamSubnetStatus.ACTIVE)
                .description("Bloco Carrier-Grade NAT para Assinantes Residenciais")
                .build();
        ipamSubnetRepository.save(subnetCgnat);

        IpamSubnet subnetIpv6 = IpamSubnet.builder()
                .id(UuidCreatorUtils.generateUuidV7())
                .companyId(company.getId())
                .cidr("2804:7f80::/32")
                .ipVersion(br.dev.xb.isperp.ipam.IpamIpVersion.IPV6)
                .networkAddress("2804:7f80::")
                .prefixLength(32)
                .category(br.dev.xb.isperp.ipam.IpamSubnetCategory.CUSTOMER_ACCESS)
                .status(br.dev.xb.isperp.ipam.IpamSubnetStatus.ACTIVE)
                .description("Bloco IPv6 Delegado pelo LACNIC / Registro.br (/32)")
                .build();
        ipamSubnetRepository.save(subnetIpv6);
    }

    private void seedHelpdeskTickets(Customer customer, Contract contract, User attendant) {
        HelpdeskTicket ticket1 = HelpdeskTicket.builder()
                .id(UuidCreatorUtils.generateUuidV7())
                .protocol(String.format("ANATEL-2026-%06d", 10452))
                .customerId(customer.getId())
                .contractId(contract.getId())
                .category(HelpdeskTicket.TicketCategory.ROUTER_CONFIG)
                .priority(HelpdeskTicket.TicketPriority.NORMAL)
                .status(HelpdeskTicket.TicketStatus.OPEN)
                .channel(HelpdeskTicket.TicketChannel.WHATSAPP_BOT)
                .subject("Troca de Senha do Wi-Fi 5GHz da ONT")
                .description("Cliente solicitou alteração do SSID e senha da rede 5GHz do roteador de casa.")
                .assignedToUserId(attendant.getId())
                .slaDeadline(LocalDateTime.now().plusHours(4))
                .build();
        helpdeskTicketRepository.save(ticket1);

        HelpdeskTicket ticket2 = HelpdeskTicket.builder()
                .id(UuidCreatorUtils.generateUuidV7())
                .protocol(String.format("ANATEL-2026-%06d", 10453))
                .customerId(customer.getId())
                .contractId(contract.getId())
                .category(HelpdeskTicket.TicketCategory.FINANCIAL)
                .priority(HelpdeskTicket.TicketPriority.LOW)
                .status(HelpdeskTicket.TicketStatus.RESOLVED)
                .channel(HelpdeskTicket.TicketChannel.PORTAL)
                .subject("Solicitação de 2ª via de fatura para Pix")
                .description("Fatura reenviada com código copia e cola diretamente pelo WhatsApp.")
                .assignedToUserId(attendant.getId())
                .slaDeadline(LocalDateTime.now().plusHours(2))
                .resolvedAt(LocalDateTime.now().minusMinutes(20))
                .resolutionNotes("Fatura gerada e baixada com sucesso.")
                .build();
        helpdeskTicketRepository.save(ticket2);
    }

    private void seedOnus(FtthCto cto, Contract contract, Customer customer) {
        OnuProvisioning onu = OnuProvisioning.builder()
                .id(UuidCreatorUtils.generateUuidV7())
                .contractId(contract.getId())
                .customerId(customer.getId())
                .onuMac("48:57:02:AA:BB:01")
                .onuSerial("HWTC00AABB01")
                .vlanId(100)
                .pppoeUser(customer.getEmail() != null ? customer.getEmail() : "user.pppoe@nexusfibra")
                .pppoePassword("nexus123")
                .downloadSpeed(300)
                .uploadSpeed(150)
                .rxPowerDbm(new BigDecimal("-19.45"))
                .status(OnuProvisioning.OnuStatus.PROVISIONED)
                .build();
        onuProvisioningRepository.save(onu);
    }

    private void seedFiscalCompany(Company company) {
        FiscalCompany fc = FiscalCompany.builder()
                .id(UuidCreatorUtils.generateUuidV7())
                .cnpj(company.getDocument())
                .razaoSocial(company.getName())
                .nomeFantasia("Nexus Fibra")
                .inscricaoEstadual("15888999")
                .inscricaoMunicipal("998811")
                .cnaePrincipal("6110-8/03")
                .regimeTributario("SIMPLES_NACIONAL")
                .aliquotaIcms(BigDecimal.ZERO)
                .aliquotaFust(new BigDecimal("0.65"))
                .aliquotaFunttel(new BigDecimal("0.50"))
                .aliquotaPis(BigDecimal.ZERO)
                .aliquotaCofins(BigDecimal.ZERO)
                .logradouro("Av. Brasil")
                .numero("1500")
                .complemento("Sala 01")
                .bairro("Centro")
                .cidade("Altamira")
                .uf("PA")
                .cep("68370-000")
                .codigoIbge("1500602")
                .telefone(company.getPhone())
                .emailFiscal(company.getEmail())
                .nfcomAmbiente("HOMOLOGACAO")
                .nfcomSerie("1")
                .nfcomProximoNumero(1)
                .isActive(true)
                .hasCertificate(false)
                .accountingName("Assessoria Contábil Silva & Associados")
                .accountingEmails("fiscal@contabilidade.com.br")
                .accountingSendDay(5)
                .accountingAutoSend(true)
                .build();
        fiscalCompanyRepository.save(fc);
    }
}
