package br.dev.xb.isperp.controller;

import br.dev.xb.isperp.api.contract.NetworkDevicesApi;
import br.dev.xb.isperp.api.dto.NetworkDeviceCreateRequest;
import br.dev.xb.isperp.api.dto.NetworkDeviceResponse;
import br.dev.xb.isperp.entity.NetworkDevice;
import br.dev.xb.isperp.mapper.NetworkDeviceMapper;
import br.dev.xb.isperp.repository.NetworkDeviceRepository;
import br.dev.xb.isperp.util.UuidCreatorUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'SUPPORT_N2', 'TECHNICIAN')")
@SuppressWarnings({"null", "java:S2583"})
public class NetworkDeviceController implements NetworkDevicesApi {

    private final NetworkDeviceRepository deviceRepository;
    private final NetworkDeviceMapper deviceMapper;

    @Override
    public ResponseEntity<List<NetworkDeviceResponse>> getAllDevices() {
        List<NetworkDevice> devices = deviceRepository.findAll();
        return ResponseEntity.ok(deviceMapper.toResponseList(devices));
    }

    @Override
    public ResponseEntity<NetworkDeviceResponse> getDeviceById(UUID id) {
        Optional<NetworkDevice> device = deviceRepository.findById(id);
        return device.map(deviceMapper::toResponse)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Override
    public ResponseEntity<NetworkDeviceResponse> saveDevice(@Valid NetworkDeviceCreateRequest request) {
        NetworkDevice entity = deviceMapper.toEntity(request);
        if (entity.getId() == null) {
            entity.setId(UuidCreatorUtils.generateUuidV7());
        }
        NetworkDevice saved = deviceRepository.save(entity);
        return ResponseEntity.status(HttpStatus.CREATED).body(deviceMapper.toResponse(saved));
    }
}
