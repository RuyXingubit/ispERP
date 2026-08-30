package br.dev.xb.isperp.controller;

import br.dev.xb.isperp.dto.NasRequest;
import br.dev.xb.isperp.dto.NasResponse;
import br.dev.xb.isperp.service.NasService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/radius/nas")
@RequiredArgsConstructor
@Tag(name = "RADIUS - NAS / BNG", description = "Gestão de Servidores NAS e Roteadores BNG do FreeRADIUS")
public class NasController {

    private final NasService nasService;

    @GetMapping
    @Operation(summary = "Lista todos os NAS/BNGs cadastrados")
    public ResponseEntity<List<NasResponse>> getAllNas() {
        return ResponseEntity.ok(nasService.getAllNas());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Busca detalhes de um NAS por ID")
    public ResponseEntity<NasResponse> getNasById(@PathVariable UUID id) {
        return ResponseEntity.ok(nasService.getNasById(id));
    }

    @PostMapping
    @Operation(summary = "Cadastra um novo NAS/BNG")
    public ResponseEntity<NasResponse> createNas(@Valid @RequestBody NasRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(nasService.createNas(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualiza dados de um NAS/BNG")
    public ResponseEntity<NasResponse> updateNas(@PathVariable UUID id, @Valid @RequestBody NasRequest request) {
        return ResponseEntity.ok(nasService.updateNas(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Remove um NAS/BNG")
    public ResponseEntity<Void> deleteNas(@PathVariable UUID id) {
        nasService.deleteNas(id);
        return ResponseEntity.noContent().build();
    }
}
