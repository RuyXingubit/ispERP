package br.dev.xb.isperp.service;

import br.dev.xb.isperp.entity.RadiusPolicyConfig;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashSet;
import java.util.Set;

@Service
public class BrazilianCalendarService {

    /**
     * Verifica se uma data e horário são permitidos para corte automático por inadimplência.
     * Regras:
     * 1. Apenas entre a janela matutina (ex: 09:00 às 11:00).
     * 2. Apenas em dias úteis (Segunda a Quinta-feira).
     * 3. Nunca em finais de semana (Sábado e Domingo).
     * 4. Nunca na Sexta-feira (véspera de final de semana).
     * 5. Nunca em feriados nacionais ou vésperas de feriados.
     */
    public boolean isAllowedForAutoBlock(LocalDate date, LocalTime time, RadiusPolicyConfig config) {
        // 1. Valida janela de horário comercial
        int hour = time.getHour();
        if (hour < config.getBlockStartHour() || hour >= config.getBlockEndHour()) {
            return false;
        }

        // 2. Valida final de semana
        DayOfWeek dayOfWeek = date.getDayOfWeek();
        if (dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY) {
            return false;
        }

        // 3. Valida Sexta-feira (proibido por padrão por ser véspera de fim de semana)
        if (dayOfWeek == DayOfWeek.FRIDAY && !config.isAllowBlockOnFriday()) {
            return false;
        }

        // 4. Valida se o dia atual é feriado nacional
        if (isHoliday(date)) {
            return false;
        }

        // 5. Valida véspera de feriado (se proteção estiver ativa)
        if (config.isProtectEveOfHolidays()) {
            LocalDate nextDay = date.plusDays(1);
            if (isHoliday(nextDay)) {
                return false;
            }
        }

        return true;
    }

    /**
     * Verifica se uma data específica é feriado nacional brasileiro (fixo ou móvel).
     */
    public boolean isHoliday(LocalDate date) {
        Set<LocalDate> holidays = getHolidaysForYear(date.getYear());
        return holidays.contains(date);
    }

    /**
     * Retorna o conjunto de todos os feriados nacionais brasileiros para o ano especificado.
     */
    public Set<LocalDate> getHolidaysForYear(int year) {
        Set<LocalDate> holidays = new HashSet<>();

        // Feriados Fixos Nacionais
        holidays.add(LocalDate.of(year, 1, 1));   // Confraternização Universal (Ano Novo)
        holidays.add(LocalDate.of(year, 4, 21));  // Tiradentes
        holidays.add(LocalDate.of(year, 5, 1));   // Dia do Trabalhador
        holidays.add(LocalDate.of(year, 9, 7));   // Independência do Brasil
        holidays.add(LocalDate.of(year, 10, 12)); // Nossa Senhora Aparecida
        holidays.add(LocalDate.of(year, 11, 2));  // Finados
        holidays.add(LocalDate.of(year, 11, 15)); // Proclamação da República
        holidays.add(LocalDate.of(year, 11, 20)); // Dia Nacional de Zumbi e Consciência Negra (Lei 14.759/2023)
        holidays.add(LocalDate.of(year, 12, 25)); // Natal

        // Feriados Móveis baseados no Domingo de Páscoa (Algoritmo de Meeus/Butcher)
        LocalDate easter = calculateEasterSunday(year);
        holidays.add(easter.minusDays(48)); // Segunda-feira de Carnaval
        holidays.add(easter.minusDays(47)); // Terça-feira de Carnaval
        holidays.add(easter.minusDays(2));  // Sexta-feira Santa / Paixão de Cristo
        holidays.add(easter);               // Domingo de Páscoa
        holidays.add(easter.plusDays(60));  // Corpus Christi

        return holidays;
    }

    /**
     * Algoritmo canônico de Jean Meeus para cálculo exato do Domingo de Páscoa no calendário Gregoriano.
     */
    public LocalDate calculateEasterSunday(int year) {
        int a = year % 19;
        int b = year / 100;
        int c = year % 100;
        int d = b / 4;
        int e = b % 4;
        int f = (b + 8) / 25;
        int g = (b - f + 1) / 3;
        int h = (19 * a + b - d - g + 15) % 30;
        int i = c / 4;
        int k = c % 4;
        int l = (32 + 2 * e + 2 * i - h - k) % 7;
        int m = (a + 11 * h + 22 * l) / 451;
        int month = (h + l - 7 * m + 114) / 31;
        int day = ((h + l - 7 * m + 114) % 31) + 1;
        return LocalDate.of(year, month, day);
    }
}
