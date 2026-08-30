package br.dev.xb.isperp.service;

import br.dev.xb.isperp.dto.CgnatScriptImportRequest;
import br.dev.xb.isperp.dto.CgnatScriptImportResponse;
import br.dev.xb.isperp.entity.CgnatMapping;
import br.dev.xb.isperp.mapper.CgnatMapper;
import br.dev.xb.isperp.radius.NasVendorType;
import br.dev.xb.isperp.repository.CgnatMappingRepository;
import br.dev.xb.isperp.repository.NasRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CgnatParserServiceTest {

    @Mock
    private CgnatMappingRepository cgnatMappingRepository;

    @Mock
    private NasRepository nasRepository;

    private CgnatMapper cgnatMapper = Mappers.getMapper(CgnatMapper.class);

    private CgnatParserService cgnatParserService;

    @BeforeEach
    void setUp() {
        cgnatParserService = new CgnatParserService(cgnatMappingRepository, nasRepository, cgnatMapper);
    }

    @Test
    @DisplayName("Deve fazer parse correto de regra CGNAT MikroTik RouterOS")
    void testParseMikrotikRule() {
        String line = "/ip firewall nat add chain=srcnat action=src-nat src-address=100.64.1.10 to-addresses=200.150.10.5 to-ports=2000-2999 protocol=tcp";
        CgnatMapping mapping = cgnatParserService.parseLine(line, NasVendorType.MIKROTIK);

        assertThat(mapping).isNotNull();
        assertThat(mapping.getPublicIp()).isEqualTo("200.150.10.5");
        assertThat(mapping.getPortStart()).isEqualTo(2000);
        assertThat(mapping.getPortEnd()).isEqualTo(2999);
        assertThat(mapping.getPrivateIpStart()).isEqualTo("100.64.1.10");
        assertThat(mapping.getProtocol()).isEqualTo("TCP");
    }

    @Test
    @DisplayName("Deve fazer parse correto de regra Huawei VRP")
    void testParseHuaweiRule() {
        String line = "rule name cgnat1 source-ip 100.64.2.20 address-group 200.150.10.8 port-range 5000 5999";
        CgnatMapping mapping = cgnatParserService.parseLine(line, NasVendorType.HUAWEI);

        assertThat(mapping).isNotNull();
        assertThat(mapping.getPublicIp()).isEqualTo("200.150.10.8");
        assertThat(mapping.getPortStart()).isEqualTo(5000);
        assertThat(mapping.getPortEnd()).isEqualTo(5999);
        assertThat(mapping.getPrivateIpStart()).isEqualTo("100.64.2.20");
    }

    @Test
    @DisplayName("Deve fazer parse de linha CSV ou planilha")
    void testParseCsvLine() {
        String line = "200.150.10.2, 1024, 2047, 100.64.1.2, 100.64.1.2, UDP";
        CgnatMapping mapping = cgnatParserService.parseLine(line, NasVendorType.GENERIC);

        assertThat(mapping).isNotNull();
        assertThat(mapping.getPublicIp()).isEqualTo("200.150.10.2");
        assertThat(mapping.getPortStart()).isEqualTo(1024);
        assertThat(mapping.getPortEnd()).isEqualTo(2047);
        assertThat(mapping.getPrivateIpStart()).isEqualTo("100.64.1.2");
        assertThat(mapping.getProtocol()).isEqualTo("UDP");
    }

    @Test
    @DisplayName("Deve importar script com múltiplas regras e persistir no repositório")
    void testImportScript() {
        String script = """
                # Regras de CGNAT BNG Centro
                /ip firewall nat add chain=srcnat action=src-nat src-address=100.64.1.2 to-addresses=200.150.10.2 to-ports=1000-1999 protocol=tcp
                /ip firewall nat add chain=srcnat action=src-nat src-address=100.64.1.3 to-addresses=200.150.10.2 to-ports=2000-2999 protocol=tcp
                """;

        when(cgnatMappingRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        CgnatScriptImportResponse response = cgnatParserService.importScript(CgnatScriptImportRequest.builder()
                .vendorType(NasVendorType.MIKROTIK)
                .scriptContent(script)
                .build());

        assertThat(response.getTotalParsed()).isEqualTo(2);
        assertThat(response.getTotalSaved()).isEqualTo(2);
        assertThat(response.getWarnings()).isEmpty();
    }

    @Test
    @DisplayName("Deve encontrar IP privado a partir de IP público e porta consultada")
    void testFindPrivateIpForPublicPort() {
        CgnatMapping mapping = CgnatMapping.builder()
                .publicIp("200.150.10.2")
                .portStart(1000)
                .portEnd(1999)
                .privateIpStart("100.64.1.2")
                .privateIpEnd("100.64.1.2")
                .build();

        when(cgnatMappingRepository.findMatchingMappings("200.150.10.2", 1500)).thenReturn(List.of(mapping));

        Optional<String> privateIp = cgnatParserService.findPrivateIpForPublicPort("200.150.10.2", 1500);

        assertThat(privateIp).isPresent();
        assertThat(privateIp.get()).isEqualTo("100.64.1.2");
    }
}
