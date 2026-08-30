package br.dev.xb.isperp.service;

import br.dev.xb.isperp.dto.CgnatMappingRequest;
import br.dev.xb.isperp.dto.CgnatMappingResponse;
import br.dev.xb.isperp.dto.CgnatScriptImportRequest;
import br.dev.xb.isperp.dto.CgnatScriptImportResponse;
import br.dev.xb.isperp.entity.CgnatMapping;
import br.dev.xb.isperp.entity.Nas;
import br.dev.xb.isperp.mapper.CgnatMapper;
import br.dev.xb.isperp.radius.NasVendorType;
import br.dev.xb.isperp.repository.CgnatMappingRepository;
import br.dev.xb.isperp.repository.NasRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class CgnatParserService {

    private final CgnatMappingRepository cgnatMappingRepository;
    private final NasRepository nasRepository;
    private final CgnatMapper cgnatMapper;

    // Regex Patterns para parsers
    private static final Pattern MIKROTIK_NAT_PATTERN = Pattern.compile(
            "src-address[= ]+([0-9.]+)(?:-([0-9.]+))?.*?to-addresses[= ]+([0-9.]+).*?to-ports[= ]+([0-9]+)-([0-9]+)",
            Pattern.CASE_INSENSITIVE
    );

    private static final Pattern HUAWEI_NAT_PATTERN = Pattern.compile(
            "(?:source-ip|inside)[= ]+([0-9.]+).*?(?:address-group|global)[= ]+([0-9.]+).*?port-range[= ]+([0-9]+)[ -]+([0-9]+)",
            Pattern.CASE_INSENSITIVE
    );

    private static final Pattern A10_NAT_PATTERN = Pattern.compile(
            "static[= ]+([0-9.]+)[= ]+([0-9.]+).*?port-range[= ]+([0-9]+)[ -]+([0-9]+)",
            Pattern.CASE_INSENSITIVE
    );

    private static final Pattern CISCO_NAT_PATTERN = Pattern.compile(
            "pool[= ]+\\w+[= ]+([0-9.]+).*?port-block[= ]+([0-9]+)[ -]+([0-9]+)",
            Pattern.CASE_INSENSITIVE
    );

    @Transactional(readOnly = true)
    public List<CgnatMappingResponse> getAllMappings() {
        return cgnatMappingRepository.findAll().stream()
                .map(cgnatMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CgnatMappingResponse> getMappingsByNas(UUID nasId) {
        return cgnatMappingRepository.findByNasId(nasId).stream()
                .map(cgnatMapper::toResponse)
                .toList();
    }

    @Transactional
    public CgnatMappingResponse createMapping(CgnatMappingRequest request) {
        CgnatMapping entity = cgnatMapper.toEntity(request);
        if (request.getNasId() != null) {
            nasRepository.findById(request.getNasId()).ifPresent(entity::setNas);
        }
        CgnatMapping saved = cgnatMappingRepository.save(entity);
        return cgnatMapper.toResponse(saved);
    }

    @Transactional
    public void deleteMapping(UUID id) {
        cgnatMappingRepository.deleteById(id);
    }

    /**
     * Importa e faz o parse de scripts de firewall (MikroTik, Huawei, A10, Cisco, CSV)
     */
    @Transactional
    public CgnatScriptImportResponse importScript(CgnatScriptImportRequest request) {
        log.info("Importando regras CGNAT para vendor: {} (NAS: {})", request.getVendorType(), request.getNasId());

        Nas nas = null;
        if (request.getNasId() != null) {
            nas = nasRepository.findById(request.getNasId()).orElse(null);
        }

        if (request.isReplaceExisting() && request.getNasId() != null) {
            List<CgnatMapping> existing = cgnatMappingRepository.findByNasId(request.getNasId());
            cgnatMappingRepository.deleteAll(existing);
        }

        String[] lines = request.getScriptContent().split("\\r?\\n");
        List<CgnatMapping> parsedEntities = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();
            if (line.isEmpty() || line.startsWith("#") || line.startsWith("//")) {
                continue;
            }

            try {
                CgnatMapping parsed = parseLine(line, request.getVendorType());
                if (parsed != null) {
                    parsed.setNas(nas);
                    parsed.setVendorType(request.getVendorType());
                    parsedEntities.add(parsed);
                }
            } catch (Exception e) {
                warnings.add(String.format("Linha %d ignorada por formato inválido: %s (%s)", i + 1, line, e.getMessage()));
            }
        }

        List<CgnatMapping> saved = cgnatMappingRepository.saveAll(parsedEntities);
        log.info("Processamento concluído: {} regras CGNAT salvas no banco de dados.", saved.size());

        return CgnatScriptImportResponse.builder()
                .totalParsed(parsedEntities.size())
                .totalSaved(saved.size())
                .importedMappings(saved.stream().map(cgnatMapper::toResponse).toList())
                .warnings(warnings)
                .build();
    }

    /**
     * Identifica o padrão e converte a linha em CgnatMapping
     */
    public @Nullable CgnatMapping parseLine(String line, NasVendorType vendorType) {
        // 1. Tenta formato CSV delimitado por vírgula, ponto-e-vírgula ou tab
        if (line.contains(",") || line.contains(";") || line.contains("\t")) {
            String[] parts = line.split("[,;\\t]");
            if (parts.length >= 5) {
                String publicIp = parts[0].trim();
                int portStart = Integer.parseInt(parts[1].trim());
                int portEnd = Integer.parseInt(parts[2].trim());
                String privStart = parts[3].trim();
                String privEnd = parts[4].trim();
                String protocol = parts.length > 5 ? parts[5].trim().toUpperCase() : "BOTH";

                return CgnatMapping.builder()
                        .publicIp(publicIp)
                        .portStart(portStart)
                        .portEnd(portEnd)
                        .privateIpStart(privStart)
                        .privateIpEnd(privEnd)
                        .protocol(protocol)
                        .build();
            }
        }

        // 2. Parser MikroTik RouterOS
        if (vendorType == NasVendorType.MIKROTIK || line.toLowerCase().contains("mikrotik") || line.toLowerCase().contains("srcnat")) {
            Matcher m = MIKROTIK_NAT_PATTERN.matcher(line);
            if (m.find()) {
                String privStart = m.group(1);
                String privEnd = m.group(2) != null ? m.group(2) : privStart;
                String publicIp = m.group(3);
                int portStart = Integer.parseInt(m.group(4));
                int portEnd = Integer.parseInt(m.group(5));

                String protocol = "BOTH";
                if (line.toLowerCase().contains("protocol=tcp")) protocol = "TCP";
                if (line.toLowerCase().contains("protocol=udp")) protocol = "UDP";

                return CgnatMapping.builder()
                        .publicIp(publicIp)
                        .portStart(portStart)
                        .portEnd(portEnd)
                        .privateIpStart(privStart)
                        .privateIpEnd(privEnd)
                        .protocol(protocol)
                        .notes("Importado de script MikroTik RouterOS")
                        .build();
            }
        }

        // 3. Parser Huawei
        if (vendorType == NasVendorType.HUAWEI || line.toLowerCase().contains("huawei") || line.toLowerCase().contains("address-group")) {
            Matcher m = HUAWEI_NAT_PATTERN.matcher(line);
            if (m.find()) {
                String privStart = m.group(1);
                String publicIp = m.group(2);
                int portStart = Integer.parseInt(m.group(3));
                int portEnd = Integer.parseInt(m.group(4));

                return CgnatMapping.builder()
                        .publicIp(publicIp)
                        .portStart(portStart)
                        .portEnd(portEnd)
                        .privateIpStart(privStart)
                        .privateIpEnd(privStart)
                        .protocol("BOTH")
                        .notes("Importado de script Huawei VRP")
                        .build();
            }
        }

        // 4. Parser A10 Networks
        if (vendorType == NasVendorType.A10 || line.toLowerCase().contains("cgnv6") || line.toLowerCase().contains("a10")) {
            Matcher m = A10_NAT_PATTERN.matcher(line);
            if (m.find()) {
                String privStart = m.group(1);
                String publicIp = m.group(2);
                int portStart = Integer.parseInt(m.group(3));
                int portEnd = Integer.parseInt(m.group(4));

                return CgnatMapping.builder()
                        .publicIp(publicIp)
                        .portStart(portStart)
                        .portEnd(portEnd)
                        .privateIpStart(privStart)
                        .privateIpEnd(privStart)
                        .protocol("BOTH")
                        .notes("Importado de script A10 Networks")
                        .build();
            }
        }

        return null;
    }

    /**
     * Localiza o IP privado correspondente a um IP público e porta consultada
     */
    @Transactional(readOnly = true)
    public Optional<String> findPrivateIpForPublicPort(String publicIp, int port) {
        List<CgnatMapping> matches = cgnatMappingRepository.findMatchingMappings(publicIp, port);
        if (matches.isEmpty()) {
            return Optional.empty();
        }

        // Retorna o IP privado mapeado na regra (se for range 1:1, privateIpStart)
        CgnatMapping match = matches.get(0);
        return Optional.of(match.getPrivateIpStart());
    }
}
