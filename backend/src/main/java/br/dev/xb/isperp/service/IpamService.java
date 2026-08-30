package br.dev.xb.isperp.service;

import br.dev.xb.isperp.dto.*;
import br.dev.xb.isperp.entity.*;
import br.dev.xb.isperp.ipam.*;
import br.dev.xb.isperp.mapper.IpamMapper;
import br.dev.xb.isperp.repository.*;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class IpamService {

    private final IpamAsnRepository asnRepository;
    private final IpamVrfRepository vrfRepository;
    private final IpamSubnetRepository subnetRepository;
    private final IpamIpAddressRepository ipAddressRepository;
    private final IpamMapper ipamMapper;
    private final IpCalculator ipCalculator;

    // =========================================================================
    // ASN Management
    // =========================================================================

    @Transactional(readOnly = true)
    public List<IpamAsnResponse> getAllAsns() {
        return asnRepository.findAll().stream()
                .map(ipamMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public IpamAsnResponse getAsnById(UUID id) {
        IpamAsn asn = asnRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("ASN não encontrado com ID: " + id));
        return ipamMapper.toResponse(asn);
    }

    @Transactional
    public IpamAsnResponse createAsn(IpamAsnRequest request) {
        asnRepository.findByAsn(request.getAsn()).ifPresent(existing -> {
            throw new IllegalArgumentException("Já existe um ASN cadastrado com o número: " + request.getAsn());
        });
        IpamAsn entity = ipamMapper.toEntity(request);
        IpamAsn saved = asnRepository.save(entity);
        log.info("ASN cadastrado com sucesso: AS{} - {}", saved.getAsn(), saved.getName());
        return ipamMapper.toResponse(saved);
    }

    @Transactional
    public IpamAsnResponse updateAsn(UUID id, IpamAsnRequest request) {
        IpamAsn entity = asnRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("ASN não encontrado com ID: " + id));
        ipamMapper.updateEntityFromRequest(request, entity);
        return ipamMapper.toResponse(asnRepository.save(entity));
    }

    @Transactional
    public void deleteAsn(UUID id) {
        if (!asnRepository.existsById(id)) {
            throw new EntityNotFoundException("ASN não encontrado com ID: " + id);
        }
        asnRepository.deleteById(id);
    }

    // =========================================================================
    // VRF Management
    // =========================================================================

    @Transactional(readOnly = true)
    public List<IpamVrfResponse> getAllVrfs() {
        return vrfRepository.findAll().stream()
                .map(ipamMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public IpamVrfResponse getVrfById(UUID id) {
        IpamVrf vrf = vrfRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("VRF não encontrada com ID: " + id));
        return ipamMapper.toResponse(vrf);
    }

    @Transactional
    public IpamVrfResponse createVrf(IpamVrfRequest request) {
        IpamVrf entity = ipamMapper.toEntity(request);
        IpamVrf saved = vrfRepository.save(entity);
        log.info("VRF criada com sucesso: {} (RD: {})", saved.getName(), saved.getRd());
        return ipamMapper.toResponse(saved);
    }

    @Transactional
    public IpamVrfResponse updateVrf(UUID id, IpamVrfRequest request) {
        IpamVrf entity = vrfRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("VRF não encontrada com ID: " + id));
        ipamMapper.updateEntityFromRequest(request, entity);
        return ipamMapper.toResponse(vrfRepository.save(entity));
    }

    @Transactional
    public void deleteVrf(UUID id) {
        if (!vrfRepository.existsById(id)) {
            throw new EntityNotFoundException("VRF não encontrada com ID: " + id);
        }
        vrfRepository.deleteById(id);
    }

    // =========================================================================
    // Subnet Management
    // =========================================================================

    @Transactional(readOnly = true)
    public List<IpamSubnetResponse> getAllSubnets() {
        List<IpamSubnet> subnets = subnetRepository.findAllWithRelations();
        return subnets.stream()
                .map(this::enrichSubnetResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public IpamSubnetResponse getSubnetById(UUID id) {
        IpamSubnet subnet = subnetRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Sub-rede não encontrada com ID: " + id));
        return enrichSubnetResponse(subnet);
    }

    @Transactional
    public IpamSubnetResponse createSubnet(IpamSubnetRequest request) {
        SubnetCalculationResult calc = ipCalculator.calculateSubnet(request.getCidr());

        // Overlap verification within same VRF
        if (request.getVrfId() != null) {
            List<IpamSubnet> vrfSubnets = subnetRepository.findByVrfId(request.getVrfId());
            for (IpamSubnet existing : vrfSubnets) {
                if (existing.getId().equals(request.getParentId())) {
                    continue; // Child can be inside parent
                }
                if (ipCalculator.isOverlap(existing.getCidr(), calc.getCidr()) && !existing.getCidr().equals(calc.getCidr())) {
                    // Check if it is a legit child
                    if (request.getParentId() == null) {
                        log.warn("Detectada sobreposição de sub-rede: {} com existente {}", calc.getCidr(), existing.getCidr());
                    }
                }
            }
        }

        IpamSubnet entity = ipamMapper.toEntity(request);
        entity.setCidr(calc.getCidr());
        entity.setIpVersion(calc.getIpVersion());
        entity.setNetworkAddress(calc.getNetworkAddress());
        entity.setBroadcastAddress(calc.getBroadcastAddress());
        entity.setPrefixLength(calc.getPrefixLength());
        entity.setTotalHosts(calc.getTotalHosts());

        if (request.getParentId() != null) {
            IpamSubnet parent = subnetRepository.findById(request.getParentId())
                    .orElseThrow(() -> new EntityNotFoundException("Sub-rede pai não encontrada"));
            entity.setParent(parent);
            if (entity.getVrf() == null) {
                entity.setVrf(parent.getVrf());
            }
            if (entity.getAsn() == null) {
                entity.setAsn(parent.getAsn());
            }
        }

        if (request.getVrfId() != null) {
            IpamVrf vrf = vrfRepository.findById(request.getVrfId())
                    .orElseThrow(() -> new EntityNotFoundException("VRF não encontrada"));
            entity.setVrf(vrf);
        }

        if (request.getAsnId() != null) {
            IpamAsn asn = asnRepository.findById(request.getAsnId())
                    .orElseThrow(() -> new EntityNotFoundException("ASN não encontrado"));
            entity.setAsn(asn);
        }

        IpamSubnet saved = subnetRepository.save(entity);
        log.info("Sub-rede criada com sucesso: {} ({}) - Hosts: {}", saved.getCidr(), saved.getIpVersion(), saved.getTotalHosts());
        return enrichSubnetResponse(saved);
    }

    @Transactional
    public IpamSubnetResponse updateSubnet(UUID id, IpamSubnetRequest request) {
        IpamSubnet entity = subnetRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Sub-rede não encontrada com ID: " + id));

        entity.setDescription(request.getDescription());
        entity.setStatus(request.getStatus());
        entity.setCategory(request.getCategory());
        entity.setPool(request.isPool());
        entity.setPoolName(request.getPoolName());

        return enrichSubnetResponse(subnetRepository.save(entity));
    }

    @Transactional
    public void deleteSubnet(UUID id) {
        if (!subnetRepository.existsById(id)) {
            throw new EntityNotFoundException("Sub-rede não encontrada com ID: " + id);
        }
        subnetRepository.deleteById(id);
    }

    // =========================================================================
    // Subnet Splitting & VLSM
    // =========================================================================

    @Transactional
    public IpamSplitResponse splitSubnet(IpamSplitRequest request) {
        IpamSubnet parent = subnetRepository.findById(request.getSubnetId())
                .orElseThrow(() -> new EntityNotFoundException("Sub-rede pai não encontrada com ID: " + request.getSubnetId()));

        List<SubnetCalculationResult> generated = ipCalculator.splitSubnet(parent.getCidr(), request.getTargetPrefixLength());
        List<IpamSubnetResponse> persisted = new ArrayList<>();

        if (request.isCreateSubnets()) {
            for (SubnetCalculationResult childCalc : generated) {
                IpamSubnet child = IpamSubnet.builder()
                        .parent(parent)
                        .vrf(parent.getVrf())
                        .asn(parent.getAsn())
                        .companyId(parent.getCompanyId())
                        .cidr(childCalc.getCidr())
                        .ipVersion(childCalc.getIpVersion())
                        .networkAddress(childCalc.getNetworkAddress())
                        .broadcastAddress(childCalc.getBroadcastAddress())
                        .prefixLength(childCalc.getPrefixLength())
                        .totalHosts(childCalc.getTotalHosts())
                        .status(IpamSubnetStatus.ACTIVE)
                        .category(parent.getCategory())
                        .description("Sub-rede filha gerada a partir de " + parent.getCidr())
                        .build();

                IpamSubnet savedChild = subnetRepository.save(child);
                persisted.add(enrichSubnetResponse(savedChild));
            }
            log.info("Split executado: {} dividido em {} sub-redes /{}", parent.getCidr(), persisted.size(), request.getTargetPrefixLength());
        }

        return IpamSplitResponse.builder()
                .parentSubnetId(parent.getId())
                .parentCidr(parent.getCidr())
                .targetPrefixLength(request.getTargetPrefixLength())
                .totalSubnetsGenerated(generated.size())
                .generatedSubnets(generated)
                .persistedSubnets(persisted)
                .build();
    }

    // =========================================================================
    // IP Addresses Management
    // =========================================================================

    @Transactional(readOnly = true)
    public List<IpamIpAddressResponse> getIpsBySubnet(UUID subnetId) {
        return ipAddressRepository.findBySubnetId(subnetId).stream()
                .map(this::enrichIpResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public IpamIpAddressResponse createIpAddress(IpamIpAddressRequest request) {
        IpamSubnet subnet = subnetRepository.findById(request.getSubnetId())
                .orElseThrow(() -> new EntityNotFoundException("Sub-rede não encontrada"));

        if (!ipCalculator.contains(subnet.getCidr(), request.getIpAddress())) {
            throw new IllegalArgumentException("O endereço IP " + request.getIpAddress() + " não pertence à sub-rede " + subnet.getCidr());
        }

        ipAddressRepository.findBySubnetIdAndIpAddress(subnet.getId(), request.getIpAddress())
                .ifPresent(existing -> {
                    throw new IllegalArgumentException("O endereço IP " + request.getIpAddress() + " já está cadastrado nesta sub-rede.");
                });

        IpamIpAddress entity = ipamMapper.toEntity(request);
        entity.setSubnet(subnet);
        IpamIpAddress saved = ipAddressRepository.save(entity);
        log.info("IP registrado no IPAM: {} na sub-rede {}", saved.getIpAddress(), subnet.getCidr());
        return enrichIpResponse(saved);
    }

    @Transactional
    public IpamIpAddressResponse updateIpAddress(UUID id, IpamIpAddressRequest request) {
        IpamIpAddress entity = ipAddressRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Endereço IP não encontrado com ID: " + id));

        entity.setStatus(request.getStatus());
        entity.setAssignedToType(request.getAssignedToType());
        entity.setAssignedToId(request.getAssignedToId());
        entity.setDnsName(request.getDnsName());
        entity.setDescription(request.getDescription());

        return enrichIpResponse(ipAddressRepository.save(entity));
    }

    @Transactional
    public void deleteIpAddress(UUID id) {
        if (!ipAddressRepository.existsById(id)) {
            throw new EntityNotFoundException("Endereço IP não encontrado com ID: " + id);
        }
        ipAddressRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public @Nullable String findNextAvailableIp(UUID subnetId) {
        IpamSubnet subnet = subnetRepository.findById(subnetId)
                .orElseThrow(() -> new EntityNotFoundException("Sub-rede não encontrada"));

        SubnetCalculationResult calc = ipCalculator.calculateSubnet(subnet.getCidr());
        List<String> usedIps = ipAddressRepository.findUsedIpsBySubnetId(subnetId);
        Set<String> usedSet = new HashSet<>(usedIps);

        if (subnet.getIpVersion() == IpamIpVersion.IPV4 && subnet.getPrefixLength() <= 30) {
            // Include network and broadcast as unavailable
            usedSet.add(calc.getNetworkAddress());
            if (calc.getBroadcastAddress() != null) {
                usedSet.add(calc.getBroadcastAddress());
            }
        }

        // Fast scan for small subnets
        if (subnet.getIpVersion() == IpamIpVersion.IPV4 && subnet.getPrefixLength() >= 20) {
            long total = calc.getTotalHosts();
            try {
                inet.ipaddr.IPAddress block = new inet.ipaddr.IPAddressString(subnet.getCidr()).getAddress().toPrefixBlock();
                Iterator<? extends inet.ipaddr.IPAddress> it = block.iterator();
                while (it.hasNext()) {
                    String candidate = it.next().toCanonicalString();
                    if (!usedSet.contains(candidate)) {
                        return candidate;
                    }
                }
            } catch (Exception e) {
                log.warn("Erro ao buscar próximo IP livre: {}", e.getMessage());
            }
        }

        return calc.getFirstUsableIp();
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    public SubnetCalculationResult calculate(String cidr) {
        return ipCalculator.calculateSubnet(cidr);
    }

    private IpamSubnetResponse enrichSubnetResponse(IpamSubnet subnet) {
        IpamSubnetResponse response = ipamMapper.toResponse(subnet);
        long allocated = ipAddressRepository.countAllocatedBySubnetId(subnet.getId());
        response.setAllocatedHosts(allocated);
        if (subnet.getTotalHosts() > 0) {
            double pct = ((double) allocated / subnet.getTotalHosts()) * 100.0;
            response.setUtilizationPercentage(Math.min(100.0, Math.round(pct * 10.0) / 10.0));
        } else {
            response.setUtilizationPercentage(0.0);
        }
        return response;
    }

    private IpamIpAddressResponse enrichIpResponse(IpamIpAddress ip) {
        IpamIpAddressResponse response = ipamMapper.toResponse(ip);
        if (ip.getAssignedToType() != null && ip.getAssignedToId() != null) {
            response.setAssignedToLabel(ip.getAssignedToType().name() + " #" + ip.getAssignedToId().toString().substring(0, 8));
        }
        return response;
    }
}
