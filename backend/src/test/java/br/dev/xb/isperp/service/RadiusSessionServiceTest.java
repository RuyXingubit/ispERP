package br.dev.xb.isperp.service;

import br.dev.xb.isperp.dto.RadiusDisconnectRequest;
import br.dev.xb.isperp.dto.RadiusDisconnectResponse;
import br.dev.xb.isperp.dto.RadiusSessionResponse;
import br.dev.xb.isperp.entity.Customer;
import br.dev.xb.isperp.entity.Nas;
import br.dev.xb.isperp.entity.OnuProvisioning;
import br.dev.xb.isperp.entity.RadAcct;
import br.dev.xb.isperp.mapper.RadiusMapper;
import br.dev.xb.isperp.repository.CustomerRepository;
import br.dev.xb.isperp.repository.NasRepository;
import br.dev.xb.isperp.repository.OnuProvisioningRepository;
import br.dev.xb.isperp.repository.RadAcctRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RadiusSessionServiceTest {

    @Mock
    private RadAcctRepository radAcctRepository;

    @Mock
    private NasRepository nasRepository;

    @Mock
    private OnuProvisioningRepository onuProvisioningRepository;

    @Mock
    private CustomerRepository customerRepository;

    private RadiusMapper radiusMapper = Mappers.getMapper(RadiusMapper.class);

    private RadiusSessionService sessionService;

    @BeforeEach
    void setUp() {
        sessionService = new RadiusSessionService(
                radAcctRepository,
                nasRepository,
                onuProvisioningRepository,
                customerRepository,
                radiusMapper
        );
    }

    @Test
    @DisplayName("Deve listar sessões ativas com enriquecimento de dados do NAS e Cliente")
    void testGetActiveSessions() {
        UUID customerId = UUID.randomUUID();

        RadAcct session = RadAcct.builder()
                .radacctId(10L)
                .acctSessionId("sess-001")
                .username("cliente_fibra")
                .nasIpAddress("10.0.0.1")
                .acctStartTime(OffsetDateTime.now().minusMinutes(30))
                .acctStopTime(null)
                .framedIpAddress("100.64.1.20")
                .callingStationId("00:11:22:33:44:55")
                .build();

        when(radAcctRepository.findByAcctStopTimeIsNullOrderByAcctStartTimeDesc())
                .thenReturn(List.of(session));

        Nas nas = Nas.builder()
                .nasname("10.0.0.1")
                .shortname("BNG-Principal")
                .build();
        when(nasRepository.findByNasname("10.0.0.1")).thenReturn(Optional.of(nas));

        OnuProvisioning onu = OnuProvisioning.builder()
                .pppoeUser("cliente_fibra")
                .customerId(customerId)
                .build();
        when(onuProvisioningRepository.findByPppoeUser("cliente_fibra")).thenReturn(Optional.of(onu));

        Customer customer = Customer.builder()
                .id(customerId)
                .name("Mariana Oliveira")
                .cpf("999.888.777-66")
                .build();
        when(customerRepository.findById(customerId)).thenReturn(Optional.of(customer));

        List<RadiusSessionResponse> active = sessionService.getActiveSessions();

        assertThat(active).hasSize(1);
        RadiusSessionResponse res = active.get(0);
        assertThat(res.getUsername()).isEqualTo("cliente_fibra");
        assertThat(res.getNasShortname()).isEqualTo("BNG-Principal");
        assertThat(res.getCustomerName()).isEqualTo("Mariana Oliveira");
        assertThat(res.isOnline()).isTrue();
    }

    @Test
    @DisplayName("Deve processar requisição de desconexão PoD")
    void testDisconnectUser() {
        RadAcct session = RadAcct.builder()
                .username("cliente_teste")
                .nasIpAddress("127.0.0.1")
                .acctStopTime(null)
                .build();

        when(radAcctRepository.findFirstByUsernameAndAcctStopTimeIsNullOrderByAcctStartTimeDesc("cliente_teste"))
                .thenReturn(Optional.of(session));

        RadiusDisconnectResponse res = sessionService.disconnectUser(RadiusDisconnectRequest.builder()
                .username("cliente_teste")
                .build());

        assertThat(res.isSuccess()).isTrue();
        assertThat(res.getMessage()).contains("127.0.0.1");
    }

    @Test
    @DisplayName("Deve suportar atributos complementares do FreeRADIUS (IPv6, InterfaceId, Class) na entidade RadAcct")
    void testRadAcctFullAttributes() {
        RadAcct session = RadAcct.builder()
                .radacctId(100L)
                .acctSessionId("session-xyz")
                .acctUniqueId("unique-xyz")
                .username("cliente_ipv6")
                .nasIpAddress("10.0.0.1")
                .framedIpAddress("100.64.0.2")
                .framedIpv6Address("2804:100::2")
                .framedIpv6Prefix("2804:100::/64")
                .framedInterfaceId("0000:0000:0000:0001")
                .delegatedIpv6Prefix("2804:100:1::/56")
                .acctClass("plano-corporativo-1g")
                .build();

        assertThat(session.getFramedIpv6Address()).isEqualTo("2804:100::2");
        assertThat(session.getFramedInterfaceId()).isEqualTo("0000:0000:0000:0001");
        assertThat(session.getAcctClass()).isEqualTo("plano-corporativo-1g");
    }

    @Test
    @DisplayName("Deve instanciar entidade RadPostAuth com todos os atributos de auditoria FreeRADIUS")
    void testRadPostAuthEntity() {
        br.dev.xb.isperp.entity.RadPostAuth postAuth = br.dev.xb.isperp.entity.RadPostAuth.builder()
                .id(1L)
                .username("cliente_teste")
                .pass("senha123")
                .reply("Access-Accept")
                .calledStationId("BNG-01")
                .callingStationId("00:11:22:33:44:55")
                .acctClass("vip")
                .build();

        assertThat(postAuth.getUsername()).isEqualTo("cliente_teste");
        assertThat(postAuth.getReply()).isEqualTo("Access-Accept");
        assertThat(postAuth.getCallingStationId()).isEqualTo("00:11:22:33:44:55");
        assertThat(postAuth.getAcctClass()).isEqualTo("vip");
        assertThat(postAuth.getAuthDate()).isNotNull();
    }
}
