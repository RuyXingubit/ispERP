package br.dev.xb.isperp.util;

import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ThreadLocalRandom;

@Component
public class AnatelProtocolGenerator {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    public String generateProtocol() {
        String datePrefix = LocalDate.now().format(DATE_FORMATTER);
        int randomSeq = ThreadLocalRandom.current().nextInt(10000, 99999);
        return datePrefix + "-" + randomSeq;
    }
}
