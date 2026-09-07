package br.dev.xb.isperp.controller;

import br.dev.xb.isperp.api.contract.IpamApi;
import br.dev.xb.isperp.api.dto.*;
import br.dev.xb.isperp.service.IpamService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@SuppressWarnings("null")
public class IpamController implements IpamApi {

    private final IpamService ipamService;

    // =========================================================================
    // ASNs
    // =========================================================================

    @Override
    @GetMapping({"/ipam/asns", "/api/ipam/asns"})
    public ResponseEntity<List<IpamAsnResponse>> listIpamAsns() {
        return ResponseEntity.ok(ipamService.getAllAsns().stream()
                .map(this::toApiResponse)
                .collect(Collectors.toList()));
    }

    @Override
    @GetMapping({"/ipam/asns/{id}", "/api/ipam/asns/{id}"})
    public ResponseEntity<IpamAsnResponse> getIpamAsnById(@PathVariable UUID id) {
        return ResponseEntity.ok(toApiResponse(ipamService.getAsnById(id)));
    }

    @Override
    @PostMapping({"/ipam/asns", "/api/ipam/asns"})
    public ResponseEntity<IpamAsnResponse> createIpamAsn(@RequestBody IpamAsnRequest request) {
        br.dev.xb.isperp.dto.IpamAsnRequest internalReq = br.dev.xb.isperp.dto.IpamAsnRequest.builder()
                .companyId(request.getCompanyId())
                .asn(request.getAsn())
                .name(request.getName())
                .rir(request.getRir() != null ? br.dev.xb.isperp.ipam.IpamRir.valueOf(request.getRir().name()) : br.dev.xb.isperp.ipam.IpamRir.REGISTRO_BR)
                .description(request.getDescription())
                .build();
        return ResponseEntity.status(HttpStatus.CREATED).body(toApiResponse(ipamService.createAsn(internalReq)));
    }

    @Override
    @PutMapping({"/ipam/asns/{id}", "/api/ipam/asns/{id}"})
    public ResponseEntity<IpamAsnResponse> updateIpamAsn(@PathVariable UUID id, @RequestBody IpamAsnRequest request) {
        br.dev.xb.isperp.dto.IpamAsnRequest internalReq = br.dev.xb.isperp.dto.IpamAsnRequest.builder()
                .companyId(request.getCompanyId())
                .asn(request.getAsn())
                .name(request.getName())
                .rir(request.getRir() != null ? br.dev.xb.isperp.ipam.IpamRir.valueOf(request.getRir().name()) : br.dev.xb.isperp.ipam.IpamRir.REGISTRO_BR)
                .description(request.getDescription())
                .build();
        return ResponseEntity.ok(toApiResponse(ipamService.updateAsn(id, internalReq)));
    }

    @Override
    @DeleteMapping({"/ipam/asns/{id}", "/api/ipam/asns/{id}"})
    public ResponseEntity<Void> deleteIpamAsn(@PathVariable UUID id) {
        ipamService.deleteAsn(id);
        return ResponseEntity.noContent().build();
    }

    // =========================================================================
    // VRFs
    // =========================================================================

    @Override
    @GetMapping({"/ipam/vrfs", "/api/ipam/vrfs"})
    public ResponseEntity<List<IpamVrfResponse>> listIpamVrfs() {
        return ResponseEntity.ok(ipamService.getAllVrfs().stream()
                .map(this::toApiResponse)
                .collect(Collectors.toList()));
    }

    @Override
    @GetMapping({"/ipam/vrfs/{id}", "/api/ipam/vrfs/{id}"})
    public ResponseEntity<IpamVrfResponse> getIpamVrfById(@PathVariable UUID id) {
        return ResponseEntity.ok(toApiResponse(ipamService.getVrfById(id)));
    }

    @Override
    @PostMapping({"/ipam/vrfs", "/api/ipam/vrfs"})
    public ResponseEntity<IpamVrfResponse> createIpamVrf(@RequestBody IpamVrfRequest request) {
        br.dev.xb.isperp.dto.IpamVrfRequest internalReq = br.dev.xb.isperp.dto.IpamVrfRequest.builder()
                .companyId(request.getCompanyId())
                .name(request.getName())
                .rd(request.getRd())
                .description(request.getDescription())
                .isDefault(Boolean.TRUE.equals(request.getIsDefault()))
                .build();
        return ResponseEntity.status(HttpStatus.CREATED).body(toApiResponse(ipamService.createVrf(internalReq)));
    }

    @Override
    @PutMapping({"/ipam/vrfs/{id}", "/api/ipam/vrfs/{id}"})
    public ResponseEntity<IpamVrfResponse> updateIpamVrf(@PathVariable UUID id, @RequestBody IpamVrfRequest request) {
        br.dev.xb.isperp.dto.IpamVrfRequest internalReq = br.dev.xb.isperp.dto.IpamVrfRequest.builder()
                .companyId(request.getCompanyId())
                .name(request.getName())
                .rd(request.getRd())
                .description(request.getDescription())
                .isDefault(Boolean.TRUE.equals(request.getIsDefault()))
                .build();
        return ResponseEntity.ok(toApiResponse(ipamService.updateVrf(id, internalReq)));
    }

    @Override
    @DeleteMapping({"/ipam/vrfs/{id}", "/api/ipam/vrfs/{id}"})
    public ResponseEntity<Void> deleteIpamVrf(@PathVariable UUID id) {
        ipamService.deleteVrf(id);
        return ResponseEntity.noContent().build();
    }

    // =========================================================================
    // Subnets
    // =========================================================================

    @Override
    @GetMapping({"/ipam/subnets", "/api/ipam/subnets"})
    public ResponseEntity<List<IpamSubnetResponse>> listIpamSubnets() {
        return ResponseEntity.ok(ipamService.getAllSubnets().stream()
                .map(this::toApiResponse)
                .collect(Collectors.toList()));
    }

    @Override
    @GetMapping({"/ipam/subnets/{id}", "/api/ipam/subnets/{id}"})
    public ResponseEntity<IpamSubnetResponse> getIpamSubnetById(@PathVariable UUID id) {
        return ResponseEntity.ok(toApiResponse(ipamService.getSubnetById(id)));
    }

    @Override
    @PostMapping({"/ipam/subnets", "/api/ipam/subnets"})
    public ResponseEntity<IpamSubnetResponse> createIpamSubnet(@RequestBody IpamSubnetRequest request) {
        br.dev.xb.isperp.dto.IpamSubnetRequest internalReq = br.dev.xb.isperp.dto.IpamSubnetRequest.builder()
                .parentId(request.getParentId())
                .vrfId(request.getVrfId())
                .asnId(request.getAsnId())
                .companyId(request.getCompanyId())
                .cidr(request.getCidr())
                .isPool(Boolean.TRUE.equals(request.getIsPool()))
                .poolName(request.getPoolName())
                .status(request.getStatus() != null ? br.dev.xb.isperp.ipam.IpamSubnetStatus.valueOf(request.getStatus().name()) : br.dev.xb.isperp.ipam.IpamSubnetStatus.ACTIVE)
                .category(request.getCategory() != null ? br.dev.xb.isperp.ipam.IpamSubnetCategory.valueOf(request.getCategory().name()) : br.dev.xb.isperp.ipam.IpamSubnetCategory.CUSTOMER_ACCESS)
                .description(request.getDescription())
                .build();
        return ResponseEntity.status(HttpStatus.CREATED).body(toApiResponse(ipamService.createSubnet(internalReq)));
    }

    @Override
    @PutMapping({"/ipam/subnets/{id}", "/api/ipam/subnets/{id}"})
    public ResponseEntity<IpamSubnetResponse> updateIpamSubnet(@PathVariable UUID id, @RequestBody IpamSubnetRequest request) {
        br.dev.xb.isperp.dto.IpamSubnetRequest internalReq = br.dev.xb.isperp.dto.IpamSubnetRequest.builder()
                .parentId(request.getParentId())
                .vrfId(request.getVrfId())
                .asnId(request.getAsnId())
                .companyId(request.getCompanyId())
                .cidr(request.getCidr())
                .isPool(Boolean.TRUE.equals(request.getIsPool()))
                .poolName(request.getPoolName())
                .status(request.getStatus() != null ? br.dev.xb.isperp.ipam.IpamSubnetStatus.valueOf(request.getStatus().name()) : br.dev.xb.isperp.ipam.IpamSubnetStatus.ACTIVE)
                .category(request.getCategory() != null ? br.dev.xb.isperp.ipam.IpamSubnetCategory.valueOf(request.getCategory().name()) : br.dev.xb.isperp.ipam.IpamSubnetCategory.CUSTOMER_ACCESS)
                .description(request.getDescription())
                .build();
        return ResponseEntity.ok(toApiResponse(ipamService.updateSubnet(id, internalReq)));
    }

    @Override
    @DeleteMapping({"/ipam/subnets/{id}", "/api/ipam/subnets/{id}"})
    public ResponseEntity<Void> deleteIpamSubnet(@PathVariable UUID id) {
        ipamService.deleteSubnet(id);
        return ResponseEntity.noContent().build();
    }

    @Override
    @PostMapping({"/ipam/subnets/split", "/api/ipam/subnets/split"})
    public ResponseEntity<IpamSplitResponse> splitIpamSubnet(@RequestBody IpamSplitRequest request) {
        br.dev.xb.isperp.dto.IpamSplitRequest internalReq = br.dev.xb.isperp.dto.IpamSplitRequest.builder()
                .subnetId(request.getSubnetId())
                .targetPrefixLength(request.getTargetPrefixLength())
                .createSubnets(Boolean.TRUE.equals(request.getCreateSubnets()))
                .build();
        return ResponseEntity.ok(toApiResponse(ipamService.splitSubnet(internalReq)));
    }

    // =========================================================================
    // IP Addresses
    // =========================================================================

    @Override
    @GetMapping({"/ipam/subnets/{subnetId}/ips", "/api/ipam/subnets/{subnetId}/ips"})
    public ResponseEntity<List<IpamIpAddressResponse>> listIpamIpsBySubnet(@PathVariable UUID subnetId) {
        return ResponseEntity.ok(ipamService.getIpsBySubnet(subnetId).stream()
                .map(this::toApiResponse)
                .collect(Collectors.toList()));
    }

    @Override
    @PostMapping({"/ipam/ips", "/api/ipam/ips"})
    public ResponseEntity<IpamIpAddressResponse> createIpamIp(@RequestBody IpamIpAddressRequest request) {
        br.dev.xb.isperp.dto.IpamIpAddressRequest internalReq = br.dev.xb.isperp.dto.IpamIpAddressRequest.builder()
                .subnetId(request.getSubnetId())
                .ipAddress(request.getIpAddress())
                .status(request.getStatus() != null ? br.dev.xb.isperp.ipam.IpamAddressStatus.valueOf(request.getStatus().name()) : br.dev.xb.isperp.ipam.IpamAddressStatus.AVAILABLE)
                .assignedToType(request.getAssignedToType() != null ? br.dev.xb.isperp.ipam.IpamAssignedToType.valueOf(request.getAssignedToType().name()) : null)
                .assignedToId(request.getAssignedToId())
                .dnsName(request.getDnsName())
                .description(request.getDescription())
                .build();
        return ResponseEntity.status(HttpStatus.CREATED).body(toApiResponse(ipamService.createIpAddress(internalReq)));
    }

    @Override
    @PutMapping({"/ipam/ips/{id}", "/api/ipam/ips/{id}"})
    public ResponseEntity<IpamIpAddressResponse> updateIpamIp(@PathVariable UUID id, @RequestBody IpamIpAddressRequest request) {
        br.dev.xb.isperp.dto.IpamIpAddressRequest internalReq = br.dev.xb.isperp.dto.IpamIpAddressRequest.builder()
                .subnetId(request.getSubnetId())
                .ipAddress(request.getIpAddress())
                .status(request.getStatus() != null ? br.dev.xb.isperp.ipam.IpamAddressStatus.valueOf(request.getStatus().name()) : br.dev.xb.isperp.ipam.IpamAddressStatus.AVAILABLE)
                .assignedToType(request.getAssignedToType() != null ? br.dev.xb.isperp.ipam.IpamAssignedToType.valueOf(request.getAssignedToType().name()) : null)
                .assignedToId(request.getAssignedToId())
                .dnsName(request.getDnsName())
                .description(request.getDescription())
                .build();
        return ResponseEntity.ok(toApiResponse(ipamService.updateIpAddress(id, internalReq)));
    }

    @Override
    @DeleteMapping({"/ipam/ips/{id}", "/api/ipam/ips/{id}"})
    public ResponseEntity<Void> deleteIpamIp(@PathVariable UUID id) {
        ipamService.deleteIpAddress(id);
        return ResponseEntity.noContent().build();
    }

    @Override
    @GetMapping({"/ipam/subnets/{subnetId}/next-available", "/api/ipam/subnets/{subnetId}/next-available"})
    public ResponseEntity<IpamNextAvailableResponse> getNextAvailableIpamIp(@PathVariable UUID subnetId) {
        String nextIp = ipamService.findNextAvailableIp(subnetId);
        IpamNextAvailableResponse response = new IpamNextAvailableResponse();
        response.setNextAvailableIp(nextIp != null ? nextIp : "");
        return ResponseEntity.ok(response);
    }

    // =========================================================================
    // Calculator & Subnetting Engine
    // =========================================================================

    @Override
    @GetMapping({"/ipam/calculate", "/api/ipam/calculate"})
    public ResponseEntity<SubnetCalculationResult> calculateIpamCidr(@RequestParam String cidr) {
        return ResponseEntity.ok(toApiResponse(ipamService.calculate(cidr)));
    }

    // =========================================================================
    // Private Converters
    // =========================================================================

    private IpamAsnResponse toApiResponse(br.dev.xb.isperp.dto.IpamAsnResponse internal) {
        if (internal == null) return null;
        IpamAsnResponse res = new IpamAsnResponse();
        res.setId(internal.getId());
        res.setCompanyId(internal.getCompanyId());
        res.setAsn(internal.getAsn());
        res.setName(internal.getName());
        if (internal.getRir() != null) {
            res.setRir(IpamRir.fromValue(internal.getRir().name()));
        }
        res.setDescription(internal.getDescription());
        res.setCreatedAt(internal.getCreatedAt());
        res.setUpdatedAt(internal.getUpdatedAt());
        return res;
    }

    private IpamVrfResponse toApiResponse(br.dev.xb.isperp.dto.IpamVrfResponse internal) {
        if (internal == null) return null;
        IpamVrfResponse res = new IpamVrfResponse();
        res.setId(internal.getId());
        res.setCompanyId(internal.getCompanyId());
        res.setName(internal.getName());
        res.setRd(internal.getRd());
        res.setDescription(internal.getDescription());
        res.setIsDefault(internal.isDefault());
        res.setCreatedAt(internal.getCreatedAt());
        res.setUpdatedAt(internal.getUpdatedAt());
        return res;
    }

    private IpamSubnetResponse toApiResponse(br.dev.xb.isperp.dto.IpamSubnetResponse internal) {
        if (internal == null) return null;
        IpamSubnetResponse res = new IpamSubnetResponse();
        res.setId(internal.getId());
        res.setParentId(internal.getParentId());
        res.setVrfId(internal.getVrfId());
        res.setVrfName(internal.getVrfName());
        res.setAsnId(internal.getAsnId());
        res.setAsnNumber(internal.getAsnNumber());
        res.setCompanyId(internal.getCompanyId());
        res.setCidr(internal.getCidr());
        if (internal.getIpVersion() != null) {
            res.setIpVersion(IpamIpVersion.fromValue(internal.getIpVersion().name()));
        }
        res.setNetworkAddress(internal.getNetworkAddress());
        res.setBroadcastAddress(internal.getBroadcastAddress());
        res.setPrefixLength(internal.getPrefixLength());
        res.setTotalHosts(internal.getTotalHosts());
        res.setAllocatedHosts(internal.getAllocatedHosts());
        res.setUtilizationPercentage(internal.getUtilizationPercentage());
        res.setIsPool(internal.isPool());
        res.setPoolName(internal.getPoolName());
        if (internal.getStatus() != null) {
            res.setStatus(IpamSubnetStatus.fromValue(internal.getStatus().name()));
        }
        if (internal.getCategory() != null) {
            res.setCategory(IpamSubnetCategory.fromValue(internal.getCategory().name()));
        }
        res.setDescription(internal.getDescription());
        res.setCreatedAt(internal.getCreatedAt());
        res.setUpdatedAt(internal.getUpdatedAt());
        return res;
    }

    private SubnetCalculationResult toApiResponse(br.dev.xb.isperp.ipam.SubnetCalculationResult internal) {
        if (internal == null) return null;
        SubnetCalculationResult res = new SubnetCalculationResult();
        res.setCidr(internal.getCidr());
        if (internal.getIpVersion() != null) {
            res.setIpVersion(IpamIpVersion.fromValue(internal.getIpVersion().name()));
        }
        res.setNetworkAddress(internal.getNetworkAddress());
        res.setBroadcastAddress(internal.getBroadcastAddress());
        res.setNetmask(internal.getNetmask());
        res.setWildcardMask(internal.getWildcardMask());
        res.setFirstUsableIp(internal.getFirstUsableIp());
        res.setLastUsableIp(internal.getLastUsableIp());
        res.setPrefixLength(internal.getPrefixLength());
        res.setTotalHosts(internal.getTotalHosts());
        res.setUsableHosts(internal.getUsableHosts());
        return res;
    }

    private IpamSplitResponse toApiResponse(br.dev.xb.isperp.dto.IpamSplitResponse internal) {
        if (internal == null) return null;
        IpamSplitResponse res = new IpamSplitResponse();
        res.setParentSubnetId(internal.getParentSubnetId());
        res.setParentCidr(internal.getParentCidr());
        res.setTargetPrefixLength(internal.getTargetPrefixLength());
        res.setTotalSubnetsGenerated(internal.getTotalSubnetsGenerated());
        if (internal.getGeneratedSubnets() != null) {
            res.setGeneratedSubnets(internal.getGeneratedSubnets().stream()
                    .map(this::toApiResponse)
                    .collect(Collectors.toList()));
        }
        if (internal.getPersistedSubnets() != null) {
            res.setPersistedSubnets(internal.getPersistedSubnets().stream()
                    .map(this::toApiResponse)
                    .collect(Collectors.toList()));
        }
        return res;
    }

    private IpamIpAddressResponse toApiResponse(br.dev.xb.isperp.dto.IpamIpAddressResponse internal) {
        if (internal == null) return null;
        IpamIpAddressResponse res = new IpamIpAddressResponse();
        res.setId(internal.getId());
        res.setSubnetId(internal.getSubnetId());
        res.setSubnetCidr(internal.getSubnetCidr());
        res.setIpAddress(internal.getIpAddress());
        if (internal.getStatus() != null) {
            res.setStatus(IpamAddressStatus.fromValue(internal.getStatus().name()));
        }
        if (internal.getAssignedToType() != null) {
            res.setAssignedToType(IpamAssignedToType.fromValue(internal.getAssignedToType().name()));
        }
        res.setAssignedToId(internal.getAssignedToId());
        res.setAssignedToLabel(internal.getAssignedToLabel());
        res.setDnsName(internal.getDnsName());
        res.setDescription(internal.getDescription());
        res.setCreatedAt(internal.getCreatedAt());
        res.setUpdatedAt(internal.getUpdatedAt());
        return res;
    }
}
