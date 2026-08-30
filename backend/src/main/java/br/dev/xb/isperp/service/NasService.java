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

    @Transactional(readOnly = true)
    public List<NasResponse> getAllNas() {
        return nasRepository.findAll().stream()
                .map(radiusMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public NasResponse getNasById(UUID id) {
        return nasRepository.findById(id)
                .map(radiusMapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("NAS / BNG não encontrado com ID: " + id));
    }

    @Transactional
    public NasResponse createNas(NasRequest request) {
        log.info("Cadastrando novo NAS / BNG: {} (Vendor: {})", request.getNasname(), request.getVendorType());
        Nas entity = radiusMapper.toEntity(request);
        Nas saved = nasRepository.save(entity);
        return radiusMapper.toResponse(saved);
    }

    @Transactional
    public NasResponse updateNas(UUID id, NasRequest request) {
        Nas entity = nasRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("NAS / BNG não encontrado com ID: " + id));

        radiusMapper.updateEntityFromRequest(request, entity);
        Nas saved = nasRepository.save(entity);
        return radiusMapper.toResponse(saved);
    }

    @Transactional
    public void deleteNas(UUID id) {
        if (!nasRepository.existsById(id)) {
            throw new ResourceNotFoundException("NAS / BNG não encontrado com ID: " + id);
        }
        nasRepository.deleteById(id);
    }
}
