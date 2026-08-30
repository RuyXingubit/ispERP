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
}
