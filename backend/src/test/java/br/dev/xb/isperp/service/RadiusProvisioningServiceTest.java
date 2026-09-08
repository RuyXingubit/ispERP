package br.dev.xb.isperp.service;

import br.dev.xb.isperp.entity.RadCheck;
import br.dev.xb.isperp.entity.RadReply;
import br.dev.xb.isperp.radius.NasVendorType;
import br.dev.xb.isperp.repository.RadCheckRepository;
import br.dev.xb.isperp.repository.RadReplyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RadiusProvisioningServiceTest {

    @Mock
    private RadCheckRepository radCheckRepository;

    @Mock
    private RadReplyRepository radReplyRepository;

    @Captor
    private ArgumentCaptor<List<RadReply>> replyCaptor;

    private RadiusProvisioningService provisioningService;

    @BeforeEach
    void setUp() {
        provisioningService = new RadiusProvisioningService(radCheckRepository, radReplyRepository);
    }

    @Test
    @DisplayName("Deve provisionar assinante MikroTik com rate-limit e IPv6 delegado")
    void testProvisionMikrotikSubscriber() {
        provisioningService.provisionSubscriber(
                "ruyfranca",
                "senha123",
                500, // 500 Mbps download
                250, // 250 Mbps upload
                NasVendorType.MIKROTIK,
                "200.150.10.15",
                "2804:192c:100::/56",
                false
        );

        verify(radCheckRepository).deleteByUsername("ruyfranca");
        verify(radReplyRepository).deleteByUsername("ruyfranca");
        verify(radCheckRepository).save(any(RadCheck.class));

        verify(radReplyRepository).saveAll(replyCaptor.capture());
        List<RadReply> replies = replyCaptor.getValue();

        assertThat(replies).extracting(RadReply::getAttribute)
                .contains("Mikrotik-Rate-Limit", "Framed-IP-Address", "Delegated-IPv6-Prefix", "Framed-Protocol");

        RadReply rateLimit = replies.stream().filter(r -> r.getAttribute().equals("Mikrotik-Rate-Limit")).findFirst().orElseThrow();
        assertThat(rateLimit.getValue()).isEqualTo("250M/500M");
    }

    @Test
    @DisplayName("Deve provisionar assinante Huawei com atributos de average-rate")
    void testProvisionHuaweiSubscriber() {
        provisioningService.provisionSubscriber(
                "cliente_huawei",
                "secret",
                100, // 100 Mbps
                50,  // 50 Mbps
                NasVendorType.HUAWEI,
                null,
                null,
                false
        );

        verify(radReplyRepository).saveAll(replyCaptor.capture());
        List<RadReply> replies = replyCaptor.getValue();

        assertThat(replies).extracting(RadReply::getAttribute)
                .contains("Huawei-Input-Average-Rate", "Huawei-Output-Average-Rate");
    }

    @Test
    @DisplayName("Deve provisionar assinante em bloqueio por inadimplência")
    void testProvisionBlockedSubscriber() {
        provisioningService.provisionSubscriber(
                "inadimplente",
                "senha",
                100,
                50,
                NasVendorType.MIKROTIK,
                null,
                null,
                true // Bloqueado
        );

        verify(radReplyRepository).saveAll(replyCaptor.capture());
        List<RadReply> replies = replyCaptor.getValue();

        assertThat(replies).extracting(RadReply::getAttribute)
                .contains("Mikrotik-Address-List", "Mikrotik-Rate-Limit");

        RadReply addrList = replies.stream().filter(r -> r.getAttribute().equals("Mikrotik-Address-List")).findFirst().orElseThrow();
        assertThat(addrList.getValue()).isEqualTo("pg_bloqueados");
    }

    @Test
    @DisplayName("Deve provisionar assinante com apenas rota adicional IPv4 (Framed-Route) sem IPv6")
    void testProvisionSubscriberWithOnlyFramedRouteIpv4() {
        provisioningService.provisionUser(
                "corp_ipv4_only",
                "senhaCorp1",
                300,
                300,
                NasVendorType.MIKROTIK,
                "200.10.10.2",
                null,
                "200.10.10.0/29",
                null // Sem IPv6
        );

        verify(radReplyRepository).saveAll(replyCaptor.capture());
        List<RadReply> replies = replyCaptor.getValue();

        assertThat(replies).extracting(RadReply::getAttribute)
                .contains("Framed-Route")
                .doesNotContain("Framed-IPv6-Route", "Delegated-IPv6-Prefix");

        RadReply route = replies.stream().filter(r -> r.getAttribute().equals("Framed-Route")).findFirst().orElseThrow();
        assertThat(route.getValue()).isEqualTo("200.10.10.0/29 0.0.0.0 1");
    }

    @Test
    @DisplayName("Deve provisionar assinante com apenas rota adicional IPv6 (Framed-IPv6-Route) sem IPv4 adicional")
    void testProvisionSubscriberWithOnlyFramedIpv6Route() {
        provisioningService.provisionUser(
                "corp_ipv6_only",
                "senhaCorp2",
                600,
                600,
                NasVendorType.HUAWEI,
                null,
                "2804:100:1::/56",
                null, // Sem IPv4 adicional
                "2804:100:2::/48"
        );

        verify(radReplyRepository).saveAll(replyCaptor.capture());
        List<RadReply> replies = replyCaptor.getValue();

        assertThat(replies).extracting(RadReply::getAttribute)
                .contains("Framed-IPv6-Route", "Delegated-IPv6-Prefix")
                .doesNotContain("Framed-Route");

        RadReply ipv6Route = replies.stream().filter(r -> r.getAttribute().equals("Framed-IPv6-Route")).findFirst().orElseThrow();
        assertThat(ipv6Route.getValue()).isEqualTo("2804:100:2::/48 :: 1");
    }

    @Test
    @DisplayName("Deve provisionar assinante dual-stack com ambas as rotas (Framed-Route e Framed-IPv6-Route)")
    void testProvisionSubscriberWithBothFramedRoutes() {
        provisioningService.provisionUser(
                "corp_dual_stack",
                "senhaDual",
                1000,
                1000,
                NasVendorType.MIKROTIK,
                "192.168.200.1",
                "2804:aaa:bbb::/56",
                "192.168.200.0/24 0.0.0.0 1",
                "2804:aaa:ccc::/48 :: 1"
        );

        verify(radReplyRepository).saveAll(replyCaptor.capture());
        List<RadReply> replies = replyCaptor.getValue();

        assertThat(replies).extracting(RadReply::getAttribute)
                .contains("Framed-Route", "Framed-IPv6-Route", "Framed-IP-Address", "Delegated-IPv6-Prefix");

        RadReply v4Route = replies.stream().filter(r -> r.getAttribute().equals("Framed-Route")).findFirst().orElseThrow();
        assertThat(v4Route.getValue()).isEqualTo("192.168.200.0/24 0.0.0.0 1");

        RadReply v6Route = replies.stream().filter(r -> r.getAttribute().equals("Framed-IPv6-Route")).findFirst().orElseThrow();
        assertThat(v6Route.getValue()).isEqualTo("2804:aaa:ccc::/48 :: 1");
    }

    @Test
    @DisplayName("Deve formatar rotas preservando gateways informados e adicionando defaults quando omitidos")
    void testFormatFramedRoutes() {
        assertThat(provisioningService.formatFramedRoute("200.50.50.0/29"))
                .isEqualTo("200.50.50.0/29 0.0.0.0 1");
        assertThat(provisioningService.formatFramedRoute("200.50.50.0/29 10.0.0.1 5"))
                .isEqualTo("200.50.50.0/29 10.0.0.1 5");

        assertThat(provisioningService.formatFramedIpv6Route("2804:10::/48"))
                .isEqualTo("2804:10::/48 :: 1");
        assertThat(provisioningService.formatFramedIpv6Route("2804:10::/48 fe80::1 10"))
                .isEqualTo("2804:10::/48 fe80::1 10");
    }
}
