package br.dev.xb.isperp.service;

import br.dev.xb.isperp.api.dto.AnatelDiciAccessGroupDto;
import br.dev.xb.isperp.api.dto.AnatelDiciReportResponse;
import br.dev.xb.isperp.client.GeoCepClient;
import br.dev.xb.isperp.entity.Contract;
import br.dev.xb.isperp.entity.Customer;
import br.dev.xb.isperp.entity.FiscalCompany;
import br.dev.xb.isperp.entity.Plan;
import br.dev.xb.isperp.repository.ContractRepository;
import br.dev.xb.isperp.repository.CustomerRepository;
import br.dev.xb.isperp.repository.FiscalCompanyRepository;
import br.dev.xb.isperp.repository.PlanRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.YearMonth;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@SuppressWarnings({"null", "java:S2583"})
public class AnatelDiciService {

    private final ContractRepository contractRepository;
    private final CustomerRepository customerRepository;
    private final PlanRepository planRepository;
    private final FiscalCompanyRepository fiscalCompanyRepository;
    private final GeoCepClient geoCepClient;

    private record GroupKey(
            String ibgeCode,
            String municipalityName,
            String uf,
            String customerType,
            String planName,
            String technology,
            String transmissionMedium,
            int downloadMbps,
            int uploadMbps
    ) {}

    private static class GroupAccumulator {
        int activeCount = 0;
        int suspendedCount = 0;
        String resolutionSource = "GEOCEP_API";
    }

    @Transactional
    public AnatelDiciReportResponse generateReport(String requestedYearMonth) {
        String yearMonth = normalizeYearMonth(requestedYearMonth);
        Optional<FiscalCompany> activeCompanyOpt = fiscalCompanyRepository.findFirstByIsActiveTrue();

        String companyCnpj = activeCompanyOpt.map(FiscalCompany::getCnpj).orElse("00.000.000/0001-00");
        String companyName = activeCompanyOpt.map(FiscalCompany::getRazaoSocial).orElse("ISP Telecom SCM Ltda");
        String companyIbge = activeCompanyOpt.map(FiscalCompany::getCodigoIbge).orElse("1500602");
        String companyCity = activeCompanyOpt.map(FiscalCompany::getCidade).orElse("Altamira");
        String companyUf = activeCompanyOpt.map(FiscalCompany::getUf).orElse("PA");

        List<Contract> contractsInService = contractRepository.findByStatusIn(
                List.of(Contract.ContractStatus.ACTIVE, Contract.ContractStatus.SUSPENDED)
        );

        Map<UUID, Customer> customerMap = customerRepository.findAllById(
                contractsInService.stream().map(Contract::getCustomerId).filter(Objects::nonNull).collect(Collectors.toSet())
        ).stream().collect(Collectors.toMap(Customer::getId, Function.identity(), (a, b) -> a));

        Map<UUID, Plan> planMap = planRepository.findAllById(
                contractsInService.stream().map(Contract::getPlanId).filter(Objects::nonNull).collect(Collectors.toSet())
        ).stream().collect(Collectors.toMap(Plan::getId, Function.identity(), (a, b) -> a));

        Map<GroupKey, GroupAccumulator> grouped = new LinkedHashMap<>();
        int totalActive = 0;
        int totalSuspended = 0;
        int pfCount = 0;
        int pjCount = 0;
        int govCount = 0;
        Set<String> distinctIbges = new LinkedHashSet<>();

        List<Contract> contractsToUpdateIbge = new ArrayList<>();

        for (Contract contract : contractsInService) {
            Customer customer = customerMap.get(contract.getCustomerId());
            Plan plan = planMap.get(contract.getPlanId());

            String cep = firstNonBlank(contract.getZipCode(), customer != null ? customer.getZipCode() : null);
            String city = firstNonBlank(contract.getCity(), customer != null ? customer.getCity() : null);
            String state = firstNonBlank(contract.getState(), customer != null ? customer.getState() : null);

            GeoCepClient.IbgeResolutionResult resolution = geoCepClient.resolveIbgeWithFallback(
                    contract.getIbgeCode(),
                    cep,
                    city,
                    state,
                    companyIbge,
                    companyCity,
                    companyUf
            );

            if ((contract.getIbgeCode() == null || contract.getIbgeCode().isBlank())
                    && resolution.getIbgeCode() != null
                    && resolution.getIbgeCode().length() == 7) {
                contract.setIbgeCode(resolution.getIbgeCode());
                contractsToUpdateIbge.add(contract);
            }

            distinctIbges.add(resolution.getIbgeCode());

            String customerType = classifyCustomerType(customer);
            if ("GOVERNO".equals(customerType)) {
                govCount++;
            } else if ("PJ".equals(customerType)) {
                pjCount++;
            } else {
                pfCount++;
            }

            int downMbps = plan != null && plan.getDownloadSpeed() != null ? plan.getDownloadSpeed() : 100;
            int upMbps = plan != null && plan.getUploadSpeed() != null ? plan.getUploadSpeed() : Math.max(50, downMbps / 2);
            String planName = plan != null && plan.getName() != null ? plan.getName() : ("Fibra " + downMbps + " Mega");

            GroupKey key = new GroupKey(
                    resolution.getIbgeCode(),
                    resolution.getMunicipalityName(),
                    resolution.getUf(),
                    customerType,
                    planName,
                    "FTTH",
                    "FIBRA",
                    downMbps,
                    upMbps
            );

            GroupAccumulator acc = grouped.computeIfAbsent(key, k -> new GroupAccumulator());
            if (contract.getStatus() == Contract.ContractStatus.ACTIVE) {
                acc.activeCount++;
                totalActive++;
            } else if (contract.getStatus() == Contract.ContractStatus.SUSPENDED) {
                acc.suspendedCount++;
                totalSuspended++;
            }
            acc.resolutionSource = resolution.getResolutionSource() != null ? resolution.getResolutionSource() : "GEOCEP_API";
        }

        if (!contractsToUpdateIbge.isEmpty()) {
            contractRepository.saveAll(contractsToUpdateIbge);
        }

        List<AnatelDiciAccessGroupDto> rows = grouped.entrySet().stream()
                .sorted(Comparator
                        .comparing((Map.Entry<GroupKey, GroupAccumulator> e) -> e.getKey().ibgeCode())
                        .thenComparing(e -> e.getKey().customerType())
                        .thenComparingInt(e -> e.getKey().downloadMbps()))
                .map(entry -> {
                    GroupKey k = entry.getKey();
                    GroupAccumulator acc = entry.getValue();
                    int totalGroup = acc.activeCount + acc.suspendedCount;
                    return new AnatelDiciAccessGroupDto(
                            k.ibgeCode(),
                            k.municipalityName(),
                            k.uf(),
                            k.customerType(),
                            k.planName(),
                            k.downloadMbps(),
                            k.uploadMbps(),
                            k.technology(),
                            k.transmissionMedium(),
                            acc.activeCount,
                            acc.suspendedCount,
                            totalGroup,
                            acc.resolutionSource
                    );
                })
                .toList();

        String csvContent = buildCsvContent(companyCnpj, yearMonth, rows);

        return new AnatelDiciReportResponse(
                yearMonth,
                companyCnpj,
                companyName,
                totalActive + totalSuspended,
                totalActive,
                totalSuspended,
                pfCount,
                pjCount,
                govCount,
                distinctIbges.size(),
                rows,
                csvContent
        );
    }

    @Transactional
    public byte[] exportCsvBytes(String requestedYearMonth) {
        AnatelDiciReportResponse report = generateReport(requestedYearMonth);
        return report.getCsvPreviewContent().getBytes(StandardCharsets.UTF_8);
    }

    private String buildCsvContent(String companyCnpj, String yearMonth, List<AnatelDiciAccessGroupDto> rows) {
        String[] ymParts = yearMonth.split("-");
        String year = ymParts.length > 0 ? ymParts[0] : String.valueOf(YearMonth.now().getYear());
        String month = ymParts.length > 1 ? ymParts[1] : String.format("%02d", YearMonth.now().getMonthValue());
        String cleanCnpj = companyCnpj != null ? companyCnpj.replaceAll("\\D", "") : "00000000000100";

        StringBuilder sb = new StringBuilder();
        sb.append("CNPJ;ANO;MES;CODIGO_IBGE;MUNICIPIO;UF;TIPO_CLIENTE;TECNOLOGIA;MEIO_TRANSMISSAO;VELOCIDADE_DOWN_MBPS;VELOCIDADE_UP_MBPS;ACESSOS_ATIVOS;ACESSOS_SUSPENSOS;ACESSOS_TOTAL\n");

        for (AnatelDiciAccessGroupDto g : rows) {
            sb.append(cleanCnpj).append(';')
                    .append(year).append(';')
                    .append(month).append(';')
                    .append(g.getIbgeCode()).append(';')
                    .append(sanitizeCsvField(g.getMunicipalityName())).append(';')
                    .append(g.getUf()).append(';')
                    .append(g.getCustomerType()).append(';')
                    .append(g.getTechnology()).append(';')
                    .append(g.getTransmissionMedium()).append(';')
                    .append(g.getDownloadSpeedMbps()).append(';')
                    .append(g.getUploadSpeedMbps()).append(';')
                    .append(g.getActiveCount()).append(';')
                    .append(g.getSuspendedCount()).append(';')
                    .append(g.getTotalInServiceCount()).append('\n');
        }

        return sb.toString();
    }

    public String normalizeYearMonth(String requestedYearMonth) {
        if (requestedYearMonth != null && requestedYearMonth.trim().matches("^\\d{4}-\\d{2}$")) {
            return requestedYearMonth.trim();
        }
        return YearMonth.now().toString();
    }

    private String classifyCustomerType(Customer customer) {
        if (customer == null) {
            return "PF";
        }
        if (Boolean.TRUE.equals(customer.getIsGovernment())) {
            return "GOVERNO";
        }
        String cleanDoc = customer.getCpf() != null ? customer.getCpf().replaceAll("\\D", "") : "";
        if (cleanDoc.length() > 11) {
            return "PJ";
        }
        return "PF";
    }

    private static String firstNonBlank(String primary, String secondary) {
        if (primary != null && !primary.isBlank()) {
            return primary.trim();
        }
        if (secondary != null && !secondary.isBlank()) {
            return secondary.trim();
        }
        return null;
    }

    private static String sanitizeCsvField(String input) {
        if (input == null) {
            return "";
        }
        return input.replace(";", ",").replace("\n", " ").replace("\r", " ").trim();
    }
}
