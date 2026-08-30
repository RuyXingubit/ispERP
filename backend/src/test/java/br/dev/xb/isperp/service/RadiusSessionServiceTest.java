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
}
