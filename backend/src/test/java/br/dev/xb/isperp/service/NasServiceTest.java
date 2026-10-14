package br.dev.xb.isperp.service;

import br.dev.xb.isperp.dto.NasRequest;
import br.dev.xb.isperp.dto.NasResponse;
import br.dev.xb.isperp.entity.Nas;
import br.dev.xb.isperp.mapper.RadiusMapper;
import br.dev.xb.isperp.radius.NasVendorType;
import br.dev.xb.isperp.repository.NasRepository;
import br.dev.xb.isperp.util.UuidCreatorUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("null")
class NasServiceTest {

    @Mock
    private NasRepository nasRepository;

    @Mock
    private RadiusMapper radiusMapper;

    @InjectMocks
    private NasService nasService;

    @Test
    @DisplayName("Segurança: getAllNas e getNasById devem mascarar secret e community mantendo apenas os últimos 4 caracteres")
    void shouldMaskRadiusSecretAndCommunityOnRead() {
        UUID id = UuidCreatorUtils.generateUuidV7();
        Nas entity = Nas.builder()
                .id(id)
                .nasname("10.0.0.1")
                .shortname("BNG-Core")
                .secret("radiusSuperSecret2026")
                .community("snmpPrivateComm99")
                .vendorType(NasVendorType.MIKROTIK)
                .build();

        when(nasRepository.findAll()).thenReturn(List.of(entity));
        when(radiusMapper.toResponse(entity)).thenReturn(NasResponse.builder()
                .id(id)
                .nasname("10.0.0.1")
                .shortname("BNG-Core")
                .secret("radiusSuperSecret2026")
                .community("snmpPrivateComm99")
                .vendorType(NasVendorType.MIKROTIK)
                .build());

        List<NasResponse> result = nasService.getAllNas();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getSecret()).isEqualTo("••••••••2026");
        assertThat(result.get(0).getCommunity()).isEqualTo("••••••••mm99");
    }

    @Test
    @DisplayName("Regressão + Segurança: updateNas deve preservar secret original quando a UI reenviar valor mascarado")
    void shouldPreserveOriginalSecretWhenMaskedValueIsSentOnUpdate() {
        UUID id = UuidCreatorUtils.generateUuidV7();
        Nas existing = Nas.builder()
                .id(id)
                .nasname("10.0.0.1")
                .shortname("BNG-Antigo")
                .secret("radiusSuperSecret2026")
                .community("snmpPrivateComm99")
                .vendorType(NasVendorType.MIKROTIK)
                .build();

        NasRequest updateReq = NasRequest.builder()
                .nasname("10.0.0.1")
                .shortname("BNG-Atualizado")
                .secret("••••••••2026")
                .community("••••••••mm99")
                .vendorType(NasVendorType.MIKROTIK)
                .build();

        when(nasRepository.findById(id)).thenReturn(Optional.of(existing));
        doAnswer(invocation -> {
            NasRequest req = invocation.getArgument(0);
            Nas target = invocation.getArgument(1);
            target.setShortname(req.getShortname());
            target.setSecret(req.getSecret());
            target.setCommunity(req.getCommunity());
            return null;
        }).when(radiusMapper).updateEntityFromRequest(updateReq, existing);

        when(nasRepository.save(any(Nas.class))).thenAnswer(inv -> inv.getArgument(0));
        when(radiusMapper.toResponse(any(Nas.class))).thenAnswer(inv -> {
            Nas n = inv.getArgument(0);
            return NasResponse.builder()
                    .id(n.getId())
                    .shortname(n.getShortname())
                    .secret(n.getSecret())
                    .community(n.getCommunity())
                    .build();
        });

        NasResponse updated = nasService.updateNas(id, updateReq);

        // O banco deve ter preservado os segredos reais
        assertThat(existing.getShortname()).isEqualTo("BNG-Atualizado");
        assertThat(existing.getSecret()).isEqualTo("radiusSuperSecret2026");
        assertThat(existing.getCommunity()).isEqualTo("snmpPrivateComm99");
        // E o retorno HTTP continua mascarado
        assertThat(updated.getSecret()).isEqualTo("••••••••2026");
        assertThat(updated.getCommunity()).isEqualTo("••••••••mm99");
    }
}
