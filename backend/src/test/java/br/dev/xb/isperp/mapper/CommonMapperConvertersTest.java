package br.dev.xb.isperp.mapper;

import br.dev.xb.isperp.controller.ElectronicSignatureController;
import br.dev.xb.isperp.entity.CompositionItemView;
import br.dev.xb.isperp.entity.Invoice;
import br.dev.xb.isperp.entity.InvoiceCompositionItem;
import br.dev.xb.isperp.entity.Plan;
import br.dev.xb.isperp.entity.PlanCompositionCategory;
import br.dev.xb.isperp.entity.PlanCompositionItem;
import br.dev.xb.isperp.entity.RadCheck;
import br.dev.xb.isperp.entity.RadReply;
import br.dev.xb.isperp.network.NetworkProvisioner;
import br.dev.xb.isperp.network.dto.OnuStatusResponse;
import br.dev.xb.isperp.signature.FallbackMethod;
import br.dev.xb.isperp.signature.SignatureStatus;
import br.dev.xb.isperp.util.UuidCreatorUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.net.URI;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class CommonMapperConvertersTest {

    private final CommonMapperConverters converters = new CommonMapperConverters() {};

    @Test
    @DisplayName("Deve converter tipos básicos no CommonMapperConverters com segurança para null")
    void shouldConvertTypesSafely() {
        assertThat(converters.doubleToBigDecimal(null)).isNull();
        assertThat(converters.doubleToBigDecimal(12.5)).isEqualByComparingTo("12.5");

        assertThat(converters.bigDecimalToDouble(null)).isNull();
        assertThat(converters.bigDecimalToDouble(new BigDecimal("19.99"))).isEqualTo(19.99);

        assertThat(converters.stringToUri(null)).isNull();
        assertThat(converters.stringToUri("   ")).isNull();
        assertThat(converters.stringToUri("://invalid uri")).isNull();
        assertThat(converters.stringToUri("https://isperp.xb.dev.br")).isEqualTo(URI.create("https://isperp.xb.dev.br"));

        assertThat(converters.uriToString(null)).isNull();
        assertThat(converters.uriToString(URI.create("https://isperp.xb.dev.br"))).isEqualTo("https://isperp.xb.dev.br");

        LocalDateTime now = LocalDateTime.of(2026, 10, 22, 12, 0);
        assertThat(converters.localDateTimeToOffsetDateTime(null)).isNull();
        OffsetDateTime odt = converters.localDateTimeToOffsetDateTime(now);
        assertThat(odt).isNotNull();

        assertThat(converters.offsetDateTimeToLocalDateTime(null)).isNull();
        assertThat(converters.offsetDateTimeToLocalDateTime(odt)).isEqualTo(now);
    }

    @Test
    @DisplayName("Deve calcular totais SCM e SVA em CompositionItemView, Invoice e Plan")
    void shouldCalculateCompositionTotals() {
        UUID existingId = UuidCreatorUtils.generateUuidV7();
        assertThat(UuidCreatorUtils.orGenerate(existingId)).isEqualTo(existingId);
        assertThat(UuidCreatorUtils.orGenerate(null)).isNotNull();

        InvoiceCompositionItem invItem1 = InvoiceCompositionItem.builder()
                .name("SCM Fibra")
                .category(null)
                .amount(new BigDecimal("70.00"))
                .build();
        InvoiceCompositionItem invItem2 = InvoiceCompositionItem.builder()
                .name("SVA Streaming")
                .category(PlanCompositionCategory.SVA_STREAMING_APP)
                .amount(new BigDecimal("29.90"))
                .build();

        Invoice invoice = Invoice.builder()
                .amount(new BigDecimal("99.90"))
                .compositionItems(List.of(invItem1, invItem2))
                .build();
        invoice.recalculateCompositionTotals();

        assertThat(invoice.getScmAmount()).isEqualByComparingTo("70.00");
        assertThat(invoice.getSvaAmount()).isEqualByComparingTo("29.90");
        assertThat(invItem1.getId()).isNotNull();
        assertThat(invItem1.getCreatedAt()).isNotNull();
        assertThat(invItem1.getCategory()).isEqualTo(PlanCompositionCategory.SCM);

        PlanCompositionItem planItem = PlanCompositionItem.builder()
                .name("SCM Plano")
                .category(PlanCompositionCategory.SCM)
                .amount(new BigDecimal("100.00"))
                .build();
        Plan plan = Plan.builder()
                .price(new BigDecimal("100.00"))
                .compositionItems(List.of(planItem))
                .build();
        plan.recalculateCompositionTotals();

        assertThat(plan.getScmAmount()).isEqualByComparingTo("100.00");
        assertThat(plan.getSvaAmount()).isEqualByComparingTo("0.00");

        RadCheck rc = RadCheck.builder().username("u").attribute("a").value("v").build();
        rc.prePersist();
        assertThat(rc.getId()).isNotNull();

        RadReply rr = RadReply.builder().username("u").attribute("a").value("v").build();
        rr.prePersist();
        assertThat(rr.getId()).isNotNull();

        OnuStatusResponse status = NetworkProvisioner.buildOnlineStatus("AA:BB:CC", "-19.00", "2.10", "OLT-1", "OK");
        assertThat(status.getStatus()).isEqualTo("ONLINE");
        assertThat(status.getRxPowerDbm()).isEqualByComparingTo("-19.00");
    }

    @Test
    @DisplayName("Deve mapear SignatureSessionResponse para DTO da API")
    void shouldMapSignatureSessionResponse() {
        assertThat(ElectronicSignatureController.toApiResponse((br.dev.xb.isperp.dto.SignatureSessionResponse) null)).isNull();

        br.dev.xb.isperp.dto.SignatureSessionResponse internal = br.dev.xb.isperp.dto.SignatureSessionResponse.builder()
                .id(UuidCreatorUtils.generateUuidV7())
                .contractId(UuidCreatorUtils.generateUuidV7())
                .token("tok-123")
                .status(SignatureStatus.SIGNED)
                .symbolicAmount(BigDecimal.ONE)
                .fallbackMethod(FallbackMethod.PIX)
                .onboardingCreditAmount(BigDecimal.ONE)
                .build();

        var apiRes = ElectronicSignatureController.toApiResponse(internal);
        assertThat(apiRes).isNotNull();
        assertThat(apiRes.getToken()).isEqualTo("tok-123");
        assertThat(apiRes.getSymbolicAmount()).isEqualTo(1.0);
    }
}
