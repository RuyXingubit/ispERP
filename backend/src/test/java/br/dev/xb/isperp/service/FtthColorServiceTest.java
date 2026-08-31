package br.dev.xb.isperp.service;

import br.dev.xb.isperp.ftth.FiberColorInfo;
import br.dev.xb.isperp.ftth.FiberColorStandard;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class FtthColorServiceTest {

    private FtthColorService colorService;

    @BeforeEach
    void setUp() {
        colorService = new FtthColorService();
    }

    @Test
    @DisplayName("Deve calcular corretamente as cores no Padrão Nacional ABNT NBR 14106")
    void testAbntStandardColors() {
        // Fibra 1 ABNT: Verde (#10B981)
        FiberColorInfo f1 = colorService.getFiberColor(1, 12, FiberColorStandard.ABNT_NBR_14106);
        assertThat(f1.getFiberColorName()).isEqualTo("Verde");
        assertThat(f1.getFiberColorHex()).isEqualTo("#10B981");
        assertThat(f1.getTubeNumber()).isEqualTo(1);
        assertThat(f1.getTubeColorName()).isEqualTo("Verde");

        // Fibra 2 ABNT: Amarelo
        FiberColorInfo f2 = colorService.getFiberColor(2, 12, FiberColorStandard.ABNT_NBR_14106);
        assertThat(f2.getFiberColorName()).isEqualTo("Amarelo");
        assertThat(f2.getFiberColorHex()).isEqualTo("#FACC15");

        // Fibra 4 ABNT: Azul
        FiberColorInfo f4 = colorService.getFiberColor(4, 12, FiberColorStandard.ABNT_NBR_14106);
        assertThat(f4.getFiberColorName()).isEqualTo("Azul");
        assertThat(f4.getFiberColorHex()).isEqualTo("#2563EB");

        // Fibra 13 ABNT: Segundo Tubo (Tubo 2 = Amarelo, Fibra 1 dentro do tubo = Verde)
        FiberColorInfo f13 = colorService.getFiberColor(13, 12, FiberColorStandard.ABNT_NBR_14106);
        assertThat(f13.getTubeNumber()).isEqualTo(2);
        assertThat(f13.getTubeColorName()).isEqualTo("Amarelo");
        assertThat(f13.getFiberInTubeNumber()).isEqualTo(1);
        assertThat(f13.getFiberColorName()).isEqualTo("Verde");
    }

    @Test
    @DisplayName("Deve calcular corretamente as cores no Padrão Internacional TIA/EIA-598")
    void testTiaStandardColors() {
        // Fibra 1 TIA-598: Azul (#2563EB)
        FiberColorInfo f1 = colorService.getFiberColor(1, 12, FiberColorStandard.TIA_EIA_598);
        assertThat(f1.getFiberColorName()).isEqualTo("Azul");
        assertThat(f1.getFiberColorHex()).isEqualTo("#2563EB");

        // Fibra 2 TIA-598: Laranja (#F97316)
        FiberColorInfo f2 = colorService.getFiberColor(2, 12, FiberColorStandard.TIA_EIA_598);
        assertThat(f2.getFiberColorName()).isEqualTo("Laranja");
        assertThat(f2.getFiberColorHex()).isEqualTo("#F97316");

        // Fibra 3 TIA-598: Verde
        FiberColorInfo f3 = colorService.getFiberColor(3, 12, FiberColorStandard.TIA_EIA_598);
        assertThat(f3.getFiberColorName()).isEqualTo("Verde");

        // Fibra 4 TIA-598: Marrom
        FiberColorInfo f4 = colorService.getFiberColor(4, 12, FiberColorStandard.TIA_EIA_598);
        assertThat(f4.getFiberColorName()).isEqualTo("Marrom");
    }

    @Test
    @DisplayName("Deve gerar a lista completa de fibras de um cabo de 24FO")
    void testGetCableFibersList() {
        List<FiberColorInfo> fibers = colorService.getCableFibers(24, 12, FiberColorStandard.ABNT_NBR_14106);
        assertThat(fibers).hasSize(24);
        assertThat(fibers.get(0).getFiberColorName()).isEqualTo("Verde");
        assertThat(fibers.get(23).getFiberColorName()).isEqualTo("Aqua");
        assertThat(fibers.get(23).getTubeNumber()).isEqualTo(2);
    }
}
