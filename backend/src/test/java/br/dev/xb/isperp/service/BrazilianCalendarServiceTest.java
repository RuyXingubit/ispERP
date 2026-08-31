package br.dev.xb.isperp.service;

import br.dev.xb.isperp.entity.RadiusPolicyConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;

class BrazilianCalendarServiceTest {

    private BrazilianCalendarService calendarService;
    private RadiusPolicyConfig config;

    @BeforeEach
    void setUp() {
        calendarService = new BrazilianCalendarService();
        config = RadiusPolicyConfig.builder()
                .blockStartHour(9)
                .blockEndHour(11)
                .allowBlockOnFriday(false)
                .protectEveOfHolidays(true)
                .build();
    }

    @Test
    @DisplayName("Deve permitir auto-corte em Terça-feira útil às 10:00 da manhã")
    void testAllowedOnTuesdayMorning() {
        LocalDate tuesday = LocalDate.of(2026, 8, 25); // Terça-feira comum
        LocalTime time = LocalTime.of(10, 0);

        boolean allowed = calendarService.isAllowedForAutoBlock(tuesday, time, config);
        assertThat(allowed).isTrue();
    }

    @Test
    @DisplayName("Deve proibir auto-corte fora da janela de horário comercial (às 08:00 ou 14:00)")
    void testProhibitedOutsideBusinessHours() {
        LocalDate tuesday = LocalDate.of(2026, 8, 25);

        assertThat(calendarService.isAllowedForAutoBlock(tuesday, LocalTime.of(8, 30), config)).isFalse();
        assertThat(calendarService.isAllowedForAutoBlock(tuesday, LocalTime.of(12, 0), config)).isFalse();
        assertThat(calendarService.isAllowedForAutoBlock(tuesday, LocalTime.of(18, 0), config)).isFalse();
    }

    @Test
    @DisplayName("Deve proibir auto-corte na Sexta-feira (véspera de final de semana)")
    void testProhibitedOnFriday() {
        LocalDate friday = LocalDate.of(2026, 8, 28); // Sexta-feira
        LocalTime time = LocalTime.of(10, 0);

        boolean allowed = calendarService.isAllowedForAutoBlock(friday, time, config);
        assertThat(allowed).isFalse();
    }

    @Test
    @DisplayName("Deve proibir auto-corte no Sábado e Domingo")
    void testProhibitedOnWeekend() {
        LocalDate saturday = LocalDate.of(2026, 8, 29);
        LocalDate sunday = LocalDate.of(2026, 8, 30);
        LocalTime time = LocalTime.of(10, 0);

        assertThat(calendarService.isAllowedForAutoBlock(saturday, time, config)).isFalse();
        assertThat(calendarService.isAllowedForAutoBlock(sunday, time, config)).isFalse();
    }

    @Test
    @DisplayName("Deve proibir auto-corte em Feriados Nacionais (ex: 07/09 e 25/12)")
    void testProhibitedOnNationalHolidays() {
        LocalDate independenceDay = LocalDate.of(2026, 9, 7); // Segunda-feira (Independência)
        LocalTime time = LocalTime.of(10, 0);

        boolean allowed = calendarService.isAllowedForAutoBlock(independenceDay, time, config);
        assertThat(allowed).isFalse();
    }

    @Test
    @DisplayName("Deve proibir auto-corte na véspera de feriado prolongado (Quarta-feira antes de Corpus Christi)")
    void testProhibitedOnEveOfHoliday() {
        // Corpus Christi em 2026 é na Quinta-feira 04/06/2026. A Quarta-feira 03/06/2026 é véspera.
        LocalDate eveOfCorpusChristi = LocalDate.of(2026, 6, 3);
        LocalTime time = LocalTime.of(10, 0);

        boolean allowed = calendarService.isAllowedForAutoBlock(eveOfCorpusChristi, time, config);
        assertThat(allowed).isFalse();
    }

    @Test
    @DisplayName("Deve calcular corretamente a Páscoa e Corpus Christi móveis")
    void testCalculateEasterAndMovableHolidays() {
        LocalDate easter2026 = calendarService.calculateEasterSunday(2026);
        assertThat(easter2026).isEqualTo(LocalDate.of(2026, 4, 5));

        assertThat(calendarService.isHoliday(LocalDate.of(2026, 4, 3))).isTrue(); // Sexta-feira Santa
        assertThat(calendarService.isHoliday(LocalDate.of(2026, 6, 4))).isTrue(); // Corpus Christi
        assertThat(calendarService.isHoliday(LocalDate.of(2026, 11, 20))).isTrue(); // Consciência Negra
    }
}
