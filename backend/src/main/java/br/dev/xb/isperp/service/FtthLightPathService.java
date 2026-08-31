package br.dev.xb.isperp.service;

import br.dev.xb.isperp.entity.*;
import br.dev.xb.isperp.repository.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@SuppressWarnings("null")
public class FtthLightPathService {

    private final FtthCtoRepository ctoRepository;
    private final FtthCtoPortRepository ctoPortRepository;
    private final FtthFusionRepository fusionRepository;
    private final FtthCableRepository cableRepository;
    private final FtthSplitterRepository splitterRepository;
    private final FtthClosureRepository closureRepository;
    private final FtthPopRepository popRepository;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LightPathNode {
        private String elementType; // CTO_PORT, CTO, SPLITTER, FUSION, CABLE, CLOSURE, DIO, POP
        private String name;
        private String details;
        private double addedAttenuationDb;
        private double cumulativeAttenuationDb;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LightPathTraceResult {
        private boolean reachedSource;
        private String sourcePopName;
        private double totalAttenuationDb;
        private double estimatedRxPowerDbm; // Baseado em +4 dBm de emissão SFP GPON B+ / C+
        private List<LightPathNode> nodes;
    }

    /**
     * Rastreia a rota óptica da porta da CTO até a central/POP.
     */
    @Transactional(readOnly = true)
    public LightPathTraceResult traceLightPathFromCtoPort(UUID ctoPortId) {
        FtthCtoPort port = ctoPortRepository.findById(ctoPortId)
                .orElseThrow(() -> new IllegalArgumentException("Porta da CTO não encontrada: " + ctoPortId));

        FtthCto cto = ctoRepository.findById(port.getCtoId())
                .orElseThrow(() -> new IllegalArgumentException("CTO não encontrada: " + port.getCtoId()));

        List<LightPathNode> path = new ArrayList<>();
        double cumulativeLoss = 0.0;

        // 1. Nó Inicial: Porta de Atendimento
        double dropLoss = 0.30; // Conector SC-APC
        cumulativeLoss += dropLoss;
        path.add(LightPathNode.builder()
                .elementType("CTO_PORT")
                .name(cto.getName() + " - Porta " + port.getPortNumber())
                .details("Conector SC-APC Drop Assinante (Status: " + port.getStatus() + ")")
                .addedAttenuationDb(dropLoss)
                .cumulativeAttenuationDb(round(cumulativeLoss))
                .build());

        // 2. Splitter Interno da CTO
        double ctoSplitterLoss = cto.getTotalPorts() == 16 ? 13.80 : 10.50;
        cumulativeLoss += ctoSplitterLoss;
        path.add(LightPathNode.builder()
                .elementType("CTO")
                .name(cto.getName())
                .details("Splitter Interno " + cto.getSplitterType() + " (" + cto.getTotalPorts() + " Portas)")
                .addedAttenuationDb(ctoSplitterLoss)
                .cumulativeAttenuationDb(round(cumulativeLoss))
                .build());

        // 3. Rastreamento reverso pelas fusões
        UUID currentClosureId = cto.getClosureId();
        UUID currentCableId = null;
        Integer currentFiberNumber = null;
        String popName = "Central Indeterminada";
        boolean reachedPop = false;

        // Procura se há uma fusão que alimenta diretamente esta CTO
        Optional<FtthFusion> ctoFusion = fusionRepository.findAll().stream()
                .filter(f -> cto.getId().equals(f.getTargetCtoId()))
                .findFirst();

        if (ctoFusion.isPresent()) {
            FtthFusion f = ctoFusion.get();
            currentCableId = f.getSourceCableId();
            currentFiberNumber = f.getSourceFiberNumber();
            currentClosureId = f.getClosureId();

            cumulativeLoss += f.getLossDb().doubleValue();
            path.add(LightPathNode.builder()
                    .elementType("FUSION")
                    .name("Fusão de Alimentação CTO")
                    .details("Cabo Origem Fibra " + currentFiberNumber + " ➔ Entrada CTO")
                    .addedAttenuationDb(f.getLossDb().doubleValue())
                    .cumulativeAttenuationDb(round(cumulativeLoss))
                    .build());
        }

        // Loop de rastreamento reverso de cabos e fusões até chegar ao POP
        int maxHops = 10;
        while (currentCableId != null && maxHops-- > 0) {
            Optional<FtthCable> optCable = cableRepository.findById(currentCableId);
            if (optCable.isEmpty()) break;

            FtthCable cable = optCable.get();
            double cableKm = cable.getLengthMeters().doubleValue() / 1000.0;
            double cableLoss = cableKm * cable.getAttenuationDbPerKm().doubleValue();
            cumulativeLoss += cableLoss;

            path.add(LightPathNode.builder()
                    .elementType("CABLE")
                    .name(cable.getName())
                    .details(cable.getCableType() + " - " + cable.getFiberCount() + "FO (" + cable.getLengthMeters() + "m, Fibra " + currentFiberNumber + ")")
                    .addedAttenuationDb(round(cableLoss))
                    .cumulativeAttenuationDb(round(cumulativeLoss))
                    .build());

            if (cable.getSourcePopId() != null) {
                Optional<FtthPop> pop = popRepository.findById(cable.getSourcePopId());
                if (pop.isPresent()) {
                    popName = pop.get().getName();
                    reachedPop = true;
                    path.add(LightPathNode.builder()
                            .elementType("POP")
                            .name(popName)
                            .details("DIO / OLT Central Headend")
                            .addedAttenuationDb(0.25)
                            .cumulativeAttenuationDb(round(cumulativeLoss + 0.25))
                            .build());
                    break;
                }
            }

            // Procura a próxima fusão anterior
            final UUID srcCableId = currentCableId;
            final Integer srcFiber = currentFiberNumber;
            Optional<FtthFusion> prevFusion = fusionRepository.findAll().stream()
                    .filter(f -> srcCableId.equals(f.getTargetCableId()) && srcFiber != null && srcFiber.equals(f.getTargetFiberNumber()))
                    .findFirst();

            if (prevFusion.isPresent()) {
                FtthFusion pf = prevFusion.get();
                currentCableId = pf.getSourceCableId();
                currentFiberNumber = pf.getSourceFiberNumber();
                currentClosureId = pf.getClosureId();

                cumulativeLoss += pf.getLossDb().doubleValue();
                path.add(LightPathNode.builder()
                        .elementType("FUSION")
                        .name("Fusão em CEO")
                        .details("Fibra " + pf.getSourceFiberNumber() + " ➔ Fibra " + pf.getTargetFiberNumber())
                        .addedAttenuationDb(pf.getLossDb().doubleValue())
                        .cumulativeAttenuationDb(round(cumulativeLoss))
                        .build());
            } else {
                break;
            }
        }

        double laserEmitPowerDbm = 3.50; // Média SFP GPON Class B+/C+ (+3.5 dBm)
        double estimatedRx = laserEmitPowerDbm - cumulativeLoss;

        return LightPathTraceResult.builder()
                .reachedSource(reachedPop)
                .sourcePopName(popName)
                .totalAttenuationDb(round(cumulativeLoss))
                .estimatedRxPowerDbm(round(estimatedRx))
                .nodes(path)
                .build();
    }

    private double round(double val) {
        return BigDecimal.valueOf(val).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }
}
