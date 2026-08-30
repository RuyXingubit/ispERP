package br.dev.xb.isperp.service;

import br.dev.xb.isperp.dto.*;
import br.dev.xb.isperp.entity.*;
import br.dev.xb.isperp.ipam.*;
import br.dev.xb.isperp.mapper.IpamMapper;
import br.dev.xb.isperp.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IpamServiceTest {

    @Mock
    private IpamAsnRepository asnRepository;

    @Mock
    private IpamVrfRepository vrfRepository;

    @Mock
    private IpamSubnetRepository subnetRepository;

    @Mock
    private IpamIpAddressRepository ipAddressRepository;

    private IpamMapper ipamMapper = Mappers.getMapper(IpamMapper.class);
    private IpCalculator ipCalculator = new IpCalculator();

    private IpamService ipamService;

    @BeforeEach
    void setUp() {
        ipamService = new IpamService(asnRepository, vrfRepository, subnetRepository, ipAddressRepository, ipamMapper, ipCalculator);
    }

    @Test
    @DisplayName("Deve cadastrar um ASN com sucesso")
    void testCreateAsn() {
        IpamAsnRequest request = IpamAsnRequest.builder()
                .asn(265123L)
                .name("ISP Telecom Brasil")
                .rir(IpamRir.REGISTRO_BR)
                .build();

        when(asnRepository.findByAsn(265123L)).thenReturn(Optional.empty());
        when(asnRepository.save(any(IpamAsn.class))).thenAnswer(inv -> {
            IpamAsn a = inv.getArgument(0);
            a.setId(UUID.randomUUID());
            return a;
        });

        IpamAsnResponse response = ipamService.createAsn(request);

        assertThat(response).isNotNull();
        assertThat(response.getAsn()).isEqualTo(265123L);
        assertThat(response.getName()).isEqualTo("ISP Telecom Brasil");
    }

    @Test
    @DisplayName("Deve criar uma Sub-rede calculando métricas e hosts automaticamente")
    void testCreateSubnet() {
        IpamSubnetRequest request = IpamSubnetRequest.builder()
                .cidr("200.150.10.0/24")
                .category(IpamSubnetCategory.CUSTOMER_ACCESS)
                .status(IpamSubnetStatus.ACTIVE)
                .build();

        when(subnetRepository.save(any(IpamSubnet.class))).thenAnswer(inv -> {
            IpamSubnet s = inv.getArgument(0);
            s.setId(UUID.randomUUID());
            return s;
        });
        when(ipAddressRepository.countAllocatedBySubnetId(any())).thenReturn(0L);

        IpamSubnetResponse response = ipamService.createSubnet(request);

        assertThat(response).isNotNull();
        assertThat(response.getCidr()).isEqualTo("200.150.10.0/24");
        assertThat(response.getNetworkAddress()).isEqualTo("200.150.10.0");
        assertThat(response.getBroadcastAddress()).isEqualTo("200.150.10.255");
        assertThat(response.getTotalHosts()).isEqualTo(256);
        assertThat(response.getUtilizationPercentage()).isEqualTo(0.0);
    }

    @Test
    @DisplayName("Deve executar split de sub-rede e persistir filhos quando solicitado")
    void testSplitSubnetWithPersistence() {
        UUID parentId = UUID.randomUUID();
        IpamSubnet parent = IpamSubnet.builder()
                .id(parentId)
                .cidr("10.0.0.0/24")
                .ipVersion(IpamIpVersion.IPV4)
                .category(IpamSubnetCategory.MANAGEMENT)
                .build();

        when(subnetRepository.findById(parentId)).thenReturn(Optional.of(parent));
        when(subnetRepository.save(any(IpamSubnet.class))).thenAnswer(inv -> {
            IpamSubnet s = inv.getArgument(0);
            s.setId(UUID.randomUUID());
            return s;
        });

        IpamSplitRequest request = IpamSplitRequest.builder()
                .subnetId(parentId)
                .targetPrefixLength(26) // 4x /26
                .createSubnets(true)
                .build();

        IpamSplitResponse response = ipamService.splitSubnet(request);

        assertThat(response.getTotalSubnetsGenerated()).isEqualTo(4);
        assertThat(response.getPersistedSubnets()).hasSize(4);
        verify(subnetRepository, times(4)).save(any(IpamSubnet.class));
    }

    @Test
    @DisplayName("Deve registrar um endereço IP se pertencer à sub-rede")
    void testCreateIpAddressSuccess() {
        UUID subnetId = UUID.randomUUID();
        IpamSubnet subnet = IpamSubnet.builder()
                .id(subnetId)
                .cidr("200.150.10.0/24")
                .ipVersion(IpamIpVersion.IPV4)
                .build();

        when(subnetRepository.findById(subnetId)).thenReturn(Optional.of(subnet));
        when(ipAddressRepository.findBySubnetIdAndIpAddress(subnetId, "200.150.10.50")).thenReturn(Optional.empty());
        when(ipAddressRepository.save(any(IpamIpAddress.class))).thenAnswer(inv -> {
            IpamIpAddress ip = inv.getArgument(0);
            ip.setId(UUID.randomUUID());
            return ip;
        });

        IpamIpAddressRequest request = IpamIpAddressRequest.builder()
                .subnetId(subnetId)
                .ipAddress("200.150.10.50")
                .status(IpamAddressStatus.ALLOCATED)
                .assignedToType(IpamAssignedToType.CONTRACT)
                .assignedToId(UUID.randomUUID())
                .build();

        IpamIpAddressResponse response = ipamService.createIpAddress(request);

        assertThat(response).isNotNull();
        assertThat(response.getIpAddress()).isEqualTo("200.150.10.50");
        assertThat(response.getStatus()).isEqualTo(IpamAddressStatus.ALLOCATED);
    }

    @Test
    @DisplayName("Deve rejeitar IP que não pertença à sub-rede")
    void testCreateIpAddressOutOfRange() {
        UUID subnetId = UUID.randomUUID();
        IpamSubnet subnet = IpamSubnet.builder()
                .id(subnetId)
                .cidr("200.150.10.0/24")
                .ipVersion(IpamIpVersion.IPV4)
                .build();

        when(subnetRepository.findById(subnetId)).thenReturn(Optional.of(subnet));

        IpamIpAddressRequest request = IpamIpAddressRequest.builder()
                .subnetId(subnetId)
                .ipAddress("192.168.1.1") // Out of range
                .build();

        assertThrows(IllegalArgumentException.class, () -> ipamService.createIpAddress(request));
    }
}
