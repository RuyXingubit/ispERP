package br.dev.xb.isperp.client;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SuppressWarnings("null")
class GeoCepClientTest {

    private final GeoCepClient geoCepClient = new GeoCepClient("https://api.geocep.api.br", "");

    @Test
    @DisplayName("Deve buscar dados de endereço e coordenadas a partir do CEP")
    void shouldLookupCepWithCoordinates() {
        GeoCepClient.CepLookupResult result = geoCepClient.lookupCep("68370000");

        assertNotNull(result);
        assertEquals("68370000", result.getCep());
        assertNotNull(result.getLatitude());
        assertNotNull(result.getLongitude());
        assertNotNull(result.getLocalidade());
    }

    @Test
    @DisplayName("Deve otimizar rota ordenando 5 paradas pelo menor trajeto")
    void shouldOptimizeRouteStopsInSequentialOrder() {
        GeoCepClient.RouteWaypoint origin = GeoCepClient.RouteWaypoint.builder()
                .id("ORIGIN")
                .label("Depósito Central")
                .latitude(new BigDecimal("-3.2033"))
                .longitude(new BigDecimal("-52.2064"))
                .build();

        GeoCepClient.RouteWaypoint stop1 = GeoCepClient.RouteWaypoint.builder()
                .id("OS-1")
                .label("Cliente Longe")
                .latitude(new BigDecimal("-3.2500"))
                .longitude(new BigDecimal("-52.2500"))
                .build();

        GeoCepClient.RouteWaypoint stop2 = GeoCepClient.RouteWaypoint.builder()
                .id("OS-2")
                .label("Cliente Perto")
                .latitude(new BigDecimal("-3.2050"))
                .longitude(new BigDecimal("-52.2070"))
                .build();

        GeoCepClient.OptimizedRouteResult result = geoCepClient.optimizeRoute(origin, List.of(stop1, stop2));

        assertNotNull(result);
        assertEquals(2, result.getOrderedWaypoints().size());
        // O ponto mais perto deve ser visitado primeiro
        assertEquals("OS-2", result.getOrderedWaypoints().get(0).getId());
        assertEquals("OS-1", result.getOrderedWaypoints().get(1).getId());
        assertTrue(result.getTotalDistanceKm().compareTo(BigDecimal.ZERO) > 0);
        assertTrue(result.getEstimatedDurationMinutes() > 0);
    }

    @Test
    @DisplayName("Deve enviar contribuição de coordenada predial ao GeoCEP com sucesso")
    void shouldContributeAddressCoordinateToGeoCep() {
        GeoCepClient.ContributeCoordinateRequest request = GeoCepClient.ContributeCoordinateRequest.builder()
                .cep("68372-005")
                .numero("3554")
                .latitude(new BigDecimal("-3.211890"))
                .longitude(new BigDecimal("-52.214100"))
                .precisaoGpsMetros(new BigDecimal("4.5"))
                .build();

        GeoCepClient.ContributeCoordinateResponse response = geoCepClient.contributeCoordinate(request);

        assertNotNull(response);
        assertEquals("success", response.getStatus());
        assertNotNull(response.getData());
    }
}
