package br.dev.xb.isperp.controller;

import br.dev.xb.isperp.dto.*;
import br.dev.xb.isperp.ipam.SubnetCalculationResult;
import br.dev.xb.isperp.service.IpamService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping({"/ipam", "/api/ipam"})
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Tag(name = "IPAM (IP Address Management)", description = "Gestão corporativa de recursos de numeração, ASNs, VRFs, Subnets IPv4/IPv6, Split e Inventário de IPs")
@SecurityRequirement(name = "bearerAuth")
public class IpamController {

    private final IpamService ipamService;

    // =========================================================================
    // ASNs
    // =========================================================================

    @GetMapping("/asns")
    @Operation(summary = "Lista todos os ASNs cadastrados")
    public ResponseEntity<List<IpamAsnResponse>> getAllAsns() {
        return ResponseEntity.ok(ipamService.getAllAsns());
    }

    @GetMapping("/asns/{id}")
    @Operation(summary = "Obtém detalhes de um ASN por ID")
    public ResponseEntity<IpamAsnResponse> getAsnById(@PathVariable UUID id) {
        return ResponseEntity.ok(ipamService.getAsnById(id));
    }

    @PostMapping("/asns")
    @Operation(summary = "Cadastra um novo ASN")
    public ResponseEntity<IpamAsnResponse> createAsn(@Valid @RequestBody IpamAsnRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ipamService.createAsn(request));
    }

    @PutMapping("/asns/{id}")
    @Operation(summary = "Atualiza dados de um ASN")
    public ResponseEntity<IpamAsnResponse> updateAsn(@PathVariable UUID id, @Valid @RequestBody IpamAsnRequest request) {
        return ResponseEntity.ok(ipamService.updateAsn(id, request));
    }

    @DeleteMapping("/asns/{id}")
    @Operation(summary = "Remove um ASN")
    public ResponseEntity<Void> deleteAsn(@PathVariable UUID id) {
        ipamService.deleteAsn(id);
        return ResponseEntity.noContent().build();
    }

    // =========================================================================
    // VRFs
    // =========================================================================

    @GetMapping("/vrfs")
    @Operation(summary = "Lista todas as VRFs")
    public ResponseEntity<List<IpamVrfResponse>> getAllVrfs() {
        return ResponseEntity.ok(ipamService.getAllVrfs());
    }

    @GetMapping("/vrfs/{id}")
    @Operation(summary = "Obtém detalhes de uma VRF por ID")
    public ResponseEntity<IpamVrfResponse> getVrfById(@PathVariable UUID id) {
        return ResponseEntity.ok(ipamService.getVrfById(id));
    }

    @PostMapping("/vrfs")
    @Operation(summary = "Cria uma nova VRF")
    public ResponseEntity<IpamVrfResponse> createVrf(@Valid @RequestBody IpamVrfRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ipamService.createVrf(request));
    }

    @PutMapping("/vrfs/{id}")
    @Operation(summary = "Atualiza dados de uma VRF")
    public ResponseEntity<IpamVrfResponse> updateVrf(@PathVariable UUID id, @Valid @RequestBody IpamVrfRequest request) {
        return ResponseEntity.ok(ipamService.updateVrf(id, request));
    }

    @DeleteMapping("/vrfs/{id}")
    @Operation(summary = "Remove uma VRF")
    public ResponseEntity<Void> deleteVrf(@PathVariable UUID id) {
        ipamService.deleteVrf(id);
        return ResponseEntity.noContent().build();
    }

    // =========================================================================
    // Subnets
    // =========================================================================

    @GetMapping("/subnets")
    @Operation(summary = "Lista todas as sub-redes com métricas de utilização")
    public ResponseEntity<List<IpamSubnetResponse>> getAllSubnets() {
        return ResponseEntity.ok(ipamService.getAllSubnets());
    }

    @GetMapping("/subnets/{id}")
    @Operation(summary = "Obtém detalhes de uma sub-rede por ID")
    public ResponseEntity<IpamSubnetResponse> getSubnetById(@PathVariable UUID id) {
        return ResponseEntity.ok(ipamService.getSubnetById(id));
    }

    @PostMapping("/subnets")
    @Operation(summary = "Cadastra uma nova sub-rede IPv4 ou IPv6")
    public ResponseEntity<IpamSubnetResponse> createSubnet(@Valid @RequestBody IpamSubnetRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ipamService.createSubnet(request));
    }

    @PutMapping("/subnets/{id}")
    @Operation(summary = "Atualiza dados e status de uma sub-rede")
    public ResponseEntity<IpamSubnetResponse> updateSubnet(@PathVariable UUID id, @Valid @RequestBody IpamSubnetRequest request) {
        return ResponseEntity.ok(ipamService.updateSubnet(id, request));
    }

    @DeleteMapping("/subnets/{id}")
    @Operation(summary = "Remove uma sub-rede")
    public ResponseEntity<Void> deleteSubnet(@PathVariable UUID id) {
        ipamService.deleteSubnet(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/subnets/split")
    @Operation(summary = "Divide uma sub-rede em blocos menores (Split / VLSM)")
    public ResponseEntity<IpamSplitResponse> splitSubnet(@Valid @RequestBody IpamSplitRequest request) {
        return ResponseEntity.ok(ipamService.splitSubnet(request));
    }

    // =========================================================================
    // IP Addresses
    // =========================================================================

    @GetMapping("/subnets/{subnetId}/ips")
    @Operation(summary = "Lista todos os IPs alocados/reservados de uma sub-rede")
    public ResponseEntity<List<IpamIpAddressResponse>> getIpsBySubnet(@PathVariable UUID subnetId) {
        return ResponseEntity.ok(ipamService.getIpsBySubnet(subnetId));
    }

    @PostMapping("/ips")
    @Operation(summary = "Registra um IP individual no inventário do IPAM")
    public ResponseEntity<IpamIpAddressResponse> createIpAddress(@Valid @RequestBody IpamIpAddressRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ipamService.createIpAddress(request));
    }

    @PutMapping("/ips/{id}")
    @Operation(summary = "Atualiza status e vinculação de um IP")
    public ResponseEntity<IpamIpAddressResponse> updateIpAddress(@PathVariable UUID id, @Valid @RequestBody IpamIpAddressRequest request) {
        return ResponseEntity.ok(ipamService.updateIpAddress(id, request));
    }

    @DeleteMapping("/ips/{id}")
    @Operation(summary = "Remove um registro de IP")
    public ResponseEntity<Void> deleteIpAddress(@PathVariable UUID id) {
        ipamService.deleteIpAddress(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/subnets/{subnetId}/next-available")
    @Operation(summary = "Localiza o próximo IP livre de uma sub-rede")
    public ResponseEntity<Map<String, String>> getNextAvailableIp(@PathVariable UUID subnetId) {
        String nextIp = ipamService.findNextAvailableIp(subnetId);
        return ResponseEntity.ok(Map.of("nextAvailableIp", nextIp != null ? nextIp : ""));
    }

    // =========================================================================
    // Calculator & Subnetting Engine
    // =========================================================================

    @GetMapping("/calculate")
    @Operation(summary = "Calcula métricas matemáticas de qualquer CIDR (IPv4/IPv6)")
    public ResponseEntity<SubnetCalculationResult> calculateCidr(@RequestParam String cidr) {
        return ResponseEntity.ok(ipamService.calculate(cidr));
    }
}
