package br.dev.xb.isperp.service;

import br.dev.xb.isperp.dto.*;
import br.dev.xb.isperp.entity.*;
import br.dev.xb.isperp.ftth.FiberColorInfo;
import br.dev.xb.isperp.mapper.FtthMapper;
import br.dev.xb.isperp.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@SuppressWarnings("null")
public class FtthFusionService {

    private final FtthFusionRepository fusionRepository;
    private final FtthCableRepository cableRepository;
    private final FtthSplitterRepository splitterRepository;
    private final FtthCtoRepository ctoRepository;
    private final FtthTopologyService topologyService;
    private final FtthColorService colorService;
    private final FtthMapper mapper;

    @Transactional(readOnly = true)
    public FtthClosureDiagramResponse getClosureDiagram(UUID closureId) {
        FtthClosureResponse closure = topologyService.getClosureById(closureId);

        // Cabos vinculados ao poste ou à caixa
        List<FtthCableResponse> cables = topologyService.getAllCables().stream()
                .filter(c -> (closure.getPoleId() != null && (closure.getPoleId().equals(c.getSourcePoleId()) || closure.getPoleId().equals(c.getTargetPoleId())))
                        || c.getName().toUpperCase().contains(closure.getName().toUpperCase()))
                .toList();

        // Se não houver cabos diretamente vinculados pelo poste, retorna todos os cabos para seleção
        if (cables.isEmpty()) {
            cables = topologyService.getAllCables();
        }

        List<FtthSplitterResponse> splitters = topologyService.getSplittersByClosure(closureId);
        List<FtthFusionResponse> fusions = fusionRepository.findByClosureId(closureId).stream()
                .map(this::enrichFusionResponse)
                .toList();

        List<FtthCtoResponse> connectedCtos = ctoRepository.findByClosureId(closureId).stream()
                .map(c -> topologyService.getCtoById(c.getId()))
                .toList();

        return FtthClosureDiagramResponse.builder()
                .closure(closure)
                .cables(cables)
                .splitters(splitters)
                .fusions(fusions)
                .connectedCtos(connectedCtos)
                .build();
    }

    @Transactional
    public FtthFusionResponse createFusion(FtthFusionRequest request) {
        // Valida se a fibra de origem já está fundida nesta ou em outra caixa
        Optional<FtthFusion> existing = fusionRepository.findBySourceCableIdAndSourceFiberNumber(
                request.getSourceCableId(), request.getSourceFiberNumber());
        if (existing.isPresent()) {
            throw new IllegalArgumentException("A fibra " + request.getSourceFiberNumber() + " do cabo de origem já possui uma fusão cadastrada.");
        }

        FtthFusion fusion = mapper.toFusionEntity(request);
        fusion = fusionRepository.save(fusion);
        return enrichFusionResponse(fusion);
    }

    @Transactional
    public void deleteFusion(UUID fusionId) {
        if (!fusionRepository.existsById(fusionId)) {
            throw new IllegalArgumentException("Fusão não encontrada: " + fusionId);
        }
        fusionRepository.deleteById(fusionId);
    }

    private FtthFusionResponse enrichFusionResponse(FtthFusion fusion) {
        FtthFusionResponse resp = mapper.toFusionResponse(fusion);

        // Origem
        cableRepository.findById(fusion.getSourceCableId()).ifPresent(cable -> {
            resp.setSourceCableName(cable.getName());
            int fibersPerTube = Math.max(1, cable.getFiberCount() / Math.max(1, cable.getTubeCount()));
            FiberColorInfo color = colorService.getFiberColor(fusion.getSourceFiberNumber(), fibersPerTube, cable.getColorStandard());
            resp.setSourceFiberColor(color);
        });

        // Destino A: Outro Cabo
        if (fusion.getTargetCableId() != null && fusion.getTargetFiberNumber() != null) {
            cableRepository.findById(fusion.getTargetCableId()).ifPresent(cable -> {
                resp.setTargetCableName(cable.getName());
                int fibersPerTube = Math.max(1, cable.getFiberCount() / Math.max(1, cable.getTubeCount()));
                FiberColorInfo color = colorService.getFiberColor(fusion.getTargetFiberNumber(), fibersPerTube, cable.getColorStandard());
                resp.setTargetFiberColor(color);
            });
        }

        // Destino B: Splitter
        if (fusion.getTargetSplitterId() != null) {
            splitterRepository.findById(fusion.getTargetSplitterId()).ifPresent(sp -> resp.setTargetSplitterName(sp.getName()));
        }

        // Destino C: CTO
        if (fusion.getTargetCtoId() != null) {
            ctoRepository.findById(fusion.getTargetCtoId()).ifPresent(cto -> resp.setTargetCtoName(cto.getName()));
        }

        return resp;
    }
}
