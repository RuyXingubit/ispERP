package br.dev.xb.isperp.service;

import br.dev.xb.isperp.dto.*;
import br.dev.xb.isperp.entity.*;
import br.dev.xb.isperp.ftth.FtthPortStatus;
import br.dev.xb.isperp.mapper.FtthMapper;
import br.dev.xb.isperp.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
@SuppressWarnings("null")
public class FtthTopologyService {

    private final FtthPopRepository popRepository;
    private final FtthPoleRepository poleRepository;
    private final FtthCableRepository cableRepository;
    private final FtthClosureRepository closureRepository;
    private final FtthSplitterRepository splitterRepository;
    private final FtthCtoRepository ctoRepository;
    private final FtthCtoPortRepository ctoPortRepository;
    private final FtthFusionRepository fusionRepository;
    private final OnuProvisioningRepository onuProvisioningRepository;
    private final CustomerRepository customerRepository;
    private final FtthColorService colorService;
    private final FtthMapper mapper;

    // --- POPs ---
    @Transactional(readOnly = true)
    public List<FtthPopResponse> getAllPops() {
        return popRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(mapper::toPopResponse)
                .toList();
    }

    @Transactional
    public FtthPopResponse createPop(FtthPopRequest request) {
        FtthPop pop = mapper.toPopEntity(request);
        pop = popRepository.save(pop);
        return mapper.toPopResponse(pop);
    }

    // --- Postes ---
    @Transactional(readOnly = true)
    public List<FtthPoleResponse> getAllPoles() {
        return poleRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(mapper::toPoleResponse)
                .toList();
    }

    @Transactional
    public FtthPoleResponse createPole(FtthPoleRequest request) {
        FtthPole pole = mapper.toPoleEntity(request);
        pole = poleRepository.save(pole);
        return mapper.toPoleResponse(pole);
    }

    // --- Cabos ---
    @Transactional(readOnly = true)
    public List<FtthCableResponse> getAllCables() {
        return cableRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(this::enrichCableResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public FtthCableResponse getCableById(UUID id) {
        FtthCable cable = cableRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Cabo óptico não encontrado: " + id));
        return enrichCableResponse(cable);
    }

    @Transactional
    public FtthCableResponse createCable(FtthCableRequest request) {
        FtthCable cable = mapper.toCableEntity(request);
        cable = cableRepository.save(cable);
        return enrichCableResponse(cable);
    }

    private FtthCableResponse enrichCableResponse(FtthCable cable) {
        FtthCableResponse resp = mapper.toCableResponse(cable);
        int fibersPerTube = Math.max(1, cable.getFiberCount() / Math.max(1, cable.getTubeCount()));
        resp.setFibers(colorService.getCableFibers(cable.getFiberCount(), fibersPerTube, cable.getColorStandard()));
        return resp;
    }

    // --- Caixas de Emenda (CEO) ---
    @Transactional(readOnly = true)
    public List<FtthClosureResponse> getAllClosures() {
        return closureRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(this::enrichClosureResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public FtthClosureResponse getClosureById(UUID id) {
        FtthClosure closure = closureRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Caixa de emenda não encontrada: " + id));
        return enrichClosureResponse(closure);
    }

    @Transactional
    public FtthClosureResponse createClosure(FtthClosureRequest request) {
        FtthClosure closure = mapper.toClosureEntity(request);
        closure = closureRepository.save(closure);
        return enrichClosureResponse(closure);
    }

    private FtthClosureResponse enrichClosureResponse(FtthClosure closure) {
        FtthClosureResponse resp = mapper.toClosureResponse(closure);
        if (closure.getPoleId() != null) {
            poleRepository.findById(closure.getPoleId()).ifPresent(p -> resp.setPoleCode(p.getCode()));
        }
        int usedFusions = fusionRepository.findByClosureId(closure.getId()).size();
        resp.setUsedFusionsCount(usedFusions);
        return resp;
    }

    // --- Splitters ---
    @Transactional(readOnly = true)
    public List<FtthSplitterResponse> getSplittersByClosure(UUID closureId) {
        return splitterRepository.findByClosureId(closureId).stream()
                .map(mapper::toSplitterResponse)
                .toList();
    }

    @Transactional
    public FtthSplitterResponse createSplitter(FtthSplitterRequest request) {
        FtthSplitter splitter = mapper.toSplitterEntity(request);
        splitter = splitterRepository.save(splitter);
        return mapper.toSplitterResponse(splitter);
    }

    // --- Caixas de Atendimento (CTO) ---
    @Transactional(readOnly = true)
    public List<FtthCtoResponse> getAllCtos() {
        return ctoRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(this::enrichCtoResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public FtthCtoResponse getCtoById(UUID id) {
        FtthCto cto = ctoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("CTO não encontrada: " + id));
        return enrichCtoResponse(cto);
    }

    @Transactional
    public FtthCtoResponse createCto(FtthCtoRequest request) {
        FtthCto cto = mapper.toCtoEntity(request);
        cto = ctoRepository.save(cto);

        // Cria automaticamente as portas da CTO (1 a N)
        List<FtthCtoPort> ports = new ArrayList<>();
        for (int i = 1; i <= cto.getTotalPorts(); i++) {
            ports.add(FtthCtoPort.builder()
                    .ctoId(cto.getId())
                    .portNumber(i)
                    .status(FtthPortStatus.LIVRE)
                    .build());
        }
        ctoPortRepository.saveAll(ports);

        return enrichCtoResponse(cto);
    }

    private FtthCtoResponse enrichCtoResponse(FtthCto cto) {
        FtthCtoResponse resp = mapper.toCtoResponse(cto);
        if (cto.getPoleId() != null) {
            poleRepository.findById(cto.getPoleId()).ifPresent(p -> resp.setPoleCode(p.getCode()));
        }

        List<FtthCtoPort> ports = ctoPortRepository.findByCtoIdOrderByPortNumberAsc(cto.getId());
        long freeCount = ports.stream().filter(p -> p.getStatus() == FtthPortStatus.LIVRE).count();
        long occupiedCount = ports.stream().filter(p -> p.getStatus() == FtthPortStatus.OCUPADA).count();

        resp.setFreePortsCount((int) freeCount);
        resp.setOccupiedPortsCount((int) occupiedCount);
        double percentage = cto.getTotalPorts() > 0 ? ((double) occupiedCount / cto.getTotalPorts()) * 100.0 : 0.0;
        resp.setOccupancyPercentage(Math.round(percentage * 10.0) / 10.0);

        List<FtthCtoPortResponse> portResponses = ports.stream().map(port -> {
            FtthCtoPortResponse pr = mapper.toCtoPortResponse(port);
            if (port.getOnuProvisioningId() != null) {
                onuProvisioningRepository.findById(port.getOnuProvisioningId()).ifPresent(onu -> {
                    pr.setOnuSerial(onu.getOnuSerial());
                    pr.setOnuMac(onu.getOnuMac());
                    pr.setPppoeUser(onu.getPppoeUser());
                });
            }
            if (port.getCustomerId() != null) {
                customerRepository.findById(port.getCustomerId()).ifPresent(c -> pr.setCustomerName(c.getName()));
            }
            return pr;
        }).toList();

        resp.setPorts(portResponses);
        return resp;
    }

    // --- Viabilidade de Vendas por Raio ---
    @Transactional(readOnly = true)
    public FtthFeasibilityResponse calculateFeasibility(FtthFeasibilityRequest request) {
        List<FtthCto> allCtos = ctoRepository.findAll();
        List<FtthFeasibilityResponse.FeasibleCtoItem> nearby = new ArrayList<>();

        double userLat = request.getLatitude().doubleValue();
        double userLng = request.getLongitude().doubleValue();

        for (FtthCto cto : allCtos) {
            double ctoLat = cto.getLatitude().doubleValue();
            double ctoLng = cto.getLongitude().doubleValue();

            double distance = calculateHaversineDistanceMeters(userLat, userLng, ctoLat, ctoLng);
            if (distance <= request.getMaxDistanceMeters()) {
                long freePorts = ctoPortRepository.countByCtoIdAndStatus(cto.getId(), FtthPortStatus.LIVRE);
                FtthCtoResponse ctoResp = enrichCtoResponse(cto);

                nearby.add(FtthFeasibilityResponse.FeasibleCtoItem.builder()
                        .cto(ctoResp)
                        .distanceMeters(Math.round(distance * 10.0) / 10.0)
                        .freePorts((int) freePorts)
                        .hasCapacity(freePorts > 0)
                        .build());
            }
        }

        nearby.sort(Comparator.comparingDouble(FtthFeasibilityResponse.FeasibleCtoItem::getDistanceMeters));

        return FtthFeasibilityResponse.builder()
                .viable(!nearby.isEmpty() && nearby.stream().anyMatch(FtthFeasibilityResponse.FeasibleCtoItem::isHasCapacity))
                .viableCtosCount((int) nearby.stream().filter(FtthFeasibilityResponse.FeasibleCtoItem::isHasCapacity).count())
                .nearbyCtos(nearby)
                .build();
    }

    private double calculateHaversineDistanceMeters(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371000; // Raio da Terra em metros
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }
}
