package br.dev.xb.isperp.service;

import br.dev.xb.isperp.ftth.FiberColorInfo;
import br.dev.xb.isperp.ftth.FiberColorStandard;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class FtthColorService {

    // Paleta ABNT NBR 14106 / Telebrás (Nacional)
    private static final String[] ABNT_NAMES = {
            "Verde", "Amarelo", "Branco", "Azul", "Vermelho", "Violeta",
            "Marrom", "Rosa", "Preto", "Cinza", "Laranja", "Aqua"
    };

    private static final String[] ABNT_HEX = {
            "#10B981", "#FACC15", "#F8FAFC", "#2563EB", "#EF4444", "#8B5CF6",
            "#78350F", "#EC4899", "#1E293B", "#64748B", "#F97316", "#06B6D4"
    };

    // Paleta Internacional TIA/EIA-598 (Americano / Importado)
    private static final String[] TIA_NAMES = {
            "Azul", "Laranja", "Verde", "Marrom", "Cinza", "Branco",
            "Vermelho", "Preto", "Amarelo", "Violeta", "Rosa", "Aqua"
    };

    private static final String[] TIA_HEX = {
            "#2563EB", "#F97316", "#10B981", "#78350F", "#64748B", "#F8FAFC",
            "#EF4444", "#1E293B", "#FACC15", "#8B5CF6", "#EC4899", "#06B6D4"
    };

    /**
     * Obtém as informações completas de cor de uma fibra em um cabo.
     *
     * @param fiberNumber Número da fibra (1 a N)
     * @param fibersPerTube Número de fibras por tubo loose (padrão: 6 ou 12)
     * @param standard Padrão de cores (ABNT ou TIA-598)
     */
    public FiberColorInfo getFiberColor(int fiberNumber, int fibersPerTube, FiberColorStandard standard) {
        if (fiberNumber <= 0) {
            throw new IllegalArgumentException("O número da fibra deve ser maior que zero");
        }
        if (fibersPerTube <= 0) {
            fibersPerTube = 12;
        }

        int tubeIndex = (fiberNumber - 1) / fibersPerTube; // 0-based
        int fiberInTubeIndex = (fiberNumber - 1) % fibersPerTube; // 0-based

        int colorIndex = fiberInTubeIndex % 12;
        int tubeColorIndex = tubeIndex % 12;

        String[] names = standard == FiberColorStandard.TIA_EIA_598 ? TIA_NAMES : ABNT_NAMES;
        String[] hex = standard == FiberColorStandard.TIA_EIA_598 ? TIA_HEX : ABNT_HEX;

        return FiberColorInfo.builder()
                .fiberNumber(fiberNumber)
                .tubeNumber(tubeIndex + 1)
                .fiberInTubeNumber(fiberInTubeIndex + 1)
                .fiberColorName(names[colorIndex])
                .fiberColorHex(hex[colorIndex])
                .tubeColorName(names[tubeColorIndex])
                .tubeColorHex(hex[tubeColorIndex])
                .standard(standard)
                .build();
    }

    /**
     * Retorna a lista de todas as fibras de um cabo com suas respectivas cores.
     */
    public List<FiberColorInfo> getCableFibers(int totalFibers, int fibersPerTube, FiberColorStandard standard) {
        List<FiberColorInfo> list = new ArrayList<>(totalFibers);
        for (int i = 1; i <= totalFibers; i++) {
            list.add(getFiberColor(i, fibersPerTube, standard));
        }
        return list;
    }
}
