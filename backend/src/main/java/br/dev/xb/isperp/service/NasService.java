package br.dev.xb.isperp.service;

import br.dev.xb.isperp.dto.NasRequest;
import br.dev.xb.isperp.dto.NasResponse;
import br.dev.xb.isperp.entity.Nas;
import br.dev.xb.isperp.exception.ResourceNotFoundException;
import br.dev.xb.isperp.mapper.RadiusMapper;
import br.dev.xb.isperp.repository.NasRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class NasService {

    private final NasRepository nasRepository;
    private final RadiusMapper radiusMapper;

    private static final String MASK_PREFIX = "••••••••";

    @Transactional(readOnly = true)
    public List<NasResponse> getAllNas() {
        return nasRepository.findAll().stream()
                .map(this::toMaskedResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public NasResponse getNasById(UUID id) {
        return nasRepository.findById(id)
                .map(this::toMaskedResponse)
                .orElseThrow(() -> new ResourceNotFoundException("NAS / BNG não encontrado com ID: " + id));
    }

    @Transactional
    public NasResponse createNas(NasRequest request) {
        log.info("Cadastrando novo NAS / BNG: {} (Vendor: {})", request.getNasname(), request.getVendorType());
        Nas entity = radiusMapper.toEntity(request);
        Nas saved = nasRepository.save(entity);
        return toMaskedResponse(saved);
    }

    @Transactional
    public NasResponse updateNas(UUID id, NasRequest request) {
        Nas entity = nasRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("NAS / BNG não encontrado com ID: " + id));

        String currentSecret = entity.getSecret();
        String currentCommunity = entity.getCommunity();

        radiusMapper.updateEntityFromRequest(request, entity);

        if (isMaskedOrBlank(request.getSecret())) {
            entity.setSecret(currentSecret);
        }
        if (isMaskedOrBlank(request.getCommunity())) {
            entity.setCommunity(currentCommunity);
        }

        Nas saved = nasRepository.save(entity);
        return toMaskedResponse(saved);
    }

    @Transactional
    public void deleteNas(UUID id) {
        if (!nasRepository.existsById(id)) {
            throw new ResourceNotFoundException("NAS / BNG não encontrado com ID: " + id);
        }
        nasRepository.deleteById(id);
    }

    private NasResponse toMaskedResponse(Nas entity) {
        NasResponse response = radiusMapper.toResponse(entity);
        if (response != null) {
            response.setSecret(maskSecret(response.getSecret()));
            response.setCommunity(maskSecret(response.getCommunity()));
        }
        return response;
    }

    private boolean isMaskedOrBlank(String value) {
        return value == null || value.isBlank() || value.startsWith(MASK_PREFIX);
    }

    private String maskSecret(String value) {
        if (value == null || value.isBlank()) {
            return value;
        }
        if (value.startsWith(MASK_PREFIX)) {
            return value;
        }
        if (value.length() <= 4) {
            return MASK_PREFIX;
        }
        return MASK_PREFIX + value.substring(value.length() - 4);
    }
}
