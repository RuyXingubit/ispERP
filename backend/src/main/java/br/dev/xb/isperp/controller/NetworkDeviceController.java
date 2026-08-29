package br.dev.xb.isperp.controller;

import br.dev.xb.isperp.entity.NetworkDevice;
import br.dev.xb.isperp.repository.NetworkDeviceRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/network-devices")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class NetworkDeviceController {

    private final NetworkDeviceRepository deviceRepository;

    @GetMapping
    public ResponseEntity<List<NetworkDevice>> getAllDevices() {
        return ResponseEntity.ok(deviceRepository.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<NetworkDevice> getDeviceById(@PathVariable @NonNull UUID id) {
        Optional<NetworkDevice> device = deviceRepository.findById(id);
        return device.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<NetworkDevice> saveDevice(@Valid @NonNull @RequestBody NetworkDevice device) {
        NetworkDevice saved = deviceRepository.save(device);
        return ResponseEntity.ok(saved);
    }
}
