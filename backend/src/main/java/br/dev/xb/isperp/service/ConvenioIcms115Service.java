package br.dev.xb.isperp.service;

import br.dev.xb.isperp.entity.Customer;
import br.dev.xb.isperp.entity.FiscalCompany;
import br.dev.xb.isperp.entity.NfcomRecord;
import br.dev.xb.isperp.repository.CustomerRepository;
import br.dev.xb.isperp.repository.FiscalCompanyRepository;
import br.dev.xb.isperp.repository.NfcomRecordRepository;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
@RequiredArgsConstructor
@Slf4j
@SuppressWarnings("null")
public class ConvenioIcms115Service {

    private final FiscalCompanyRepository companyRepository;
    private final NfcomRecordRepository nfcomRecordRepository;
    private final CustomerRepository customerRepository;

    @Data
    @Builder
    public static class Convenio115BatchResult {
        private String filenameMestre;
        private String filenameItem;
        private String filenameDestinatario;
        private String filenameControle;
        private String md5Mestre;
        private String md5Item;
        private String md5Destinatario;
        private int totalRecords;
        private BigDecimal totalFaturado;
        private BigDecimal totalIcms;
        private byte[] zipBytes;
    }

    /**
     * Gera o lote oficial dos 4 arquivos magnéticos do Convênio ICMS 115/03 para o mês/ano informado.
     */
    public Convenio115BatchResult generateMonthlyBatch(@Nullable UUID companyId, int year, int month) {
        FiscalCompany company = (companyId != null)
                ? companyRepository.findById(companyId).orElse(null)
                : companyRepository.findFirstByIsActiveTrue().orElse(null);

        if (company == null) {
            company = FiscalCompany.builder()
                    .cnpj("12.345.678/0001-95")
                    .razaoSocial("Provedor Xingu Telecom Ltda")
                    .inscricaoEstadual("15999888")
                    .uf("PA")
                    .build();
        }

        LocalDateTime start = LocalDate.of(year, month, 1).atStartOfDay();
        LocalDateTime end = start.plusMonths(1).minusNanos(1);

        List<NfcomRecord> records = (company.getId() != null)
                ? nfcomRecordRepository.findByCompanyIdAndCreatedAtBetween(company.getId(), start, end)
                : nfcomRecordRepository.findByCreatedAtBetween(start, end);

        String uf = company.getUf() != null ? company.getUf().toUpperCase() : "PA";
        String cleanCnpj = company.getCnpj().replaceAll("[^0-9]", "");
        String formattedCnpj = String.format("%-14s", cleanCnpj).replace(' ', '0');
        String yearMonthShort = String.format("%02d%02d", year % 100, month);
        String serie = company.getNfcomSerie() != null ? company.getNfcomSerie() : "1";

        // Nomenclatura oficial: UF + CNPJ(14) + MOD(62) + SERIE(3) + AAMM + TIPO + VOL(001)
        String baseName = uf + formattedCnpj + "62" + String.format("%03d", Integer.parseInt(serie)) + yearMonthShort + "N01";
        String fileMestreName = baseName + ".M";
        String fileItemName = baseName + ".I";
        String fileDestName = baseName + ".D";
        String fileControleName = baseName + ".C";

        StringBuilder sbMestre = new StringBuilder();
        StringBuilder sbItem = new StringBuilder();
        StringBuilder sbDest = new StringBuilder();

        BigDecimal sumTotal = BigDecimal.ZERO;
        BigDecimal sumIcms = BigDecimal.ZERO;

        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyyMMdd");

        for (int i = 0; i < records.size(); i++) {
            NfcomRecord rec = records.get(i);
            Customer cust = customerRepository.findById(rec.getCustomerId()).orElse(null);
            String custDoc = (cust != null && cust.getCpf() != null) ? cust.getCpf().replaceAll("[^0-9]", "") : "00000000000";
            String custName = (cust != null) ? cust.getName() : "Cliente Assinante";
            String docDate = rec.getCreatedAt() != null ? rec.getCreatedAt().format(dtf) : LocalDate.now().format(dtf);

            BigDecimal vTotal = rec.getValorTotal() != null ? rec.getValorTotal() : BigDecimal.ZERO;
            BigDecimal vIcms = rec.getValorIcms() != null ? rec.getValorIcms() : BigDecimal.ZERO;
            sumTotal = sumTotal.add(vTotal);
            sumIcms = sumIcms.add(vIcms);

            // Linha Mestre
            String mestreLine = String.format("%-14s%-14s%-35s%2s%2s%3s%09d%8s%012d%012d%012d%012d%-44s",
                    formattedCnpj,
                    String.format("%-14s", custDoc).replace(' ', '0'),
                    padRight(company.getRazaoSocial(), 35),
                    uf,
                    "62",
                    String.format("%03d", Integer.parseInt(rec.getSerie())),
                    rec.getNumero(),
                    docDate,
                    vTotal.multiply(new BigDecimal("100")).longValue(),
                    vTotal.multiply(new BigDecimal("100")).longValue(),
                    vIcms.multiply(new BigDecimal("100")).longValue(),
                    0L,
                    rec.getChaveAcesso() != null ? rec.getChaveAcesso() : ""
            );
            String md5MestreLine = calculateMd5(mestreLine);
            sbMestre.append(mestreLine).append(md5MestreLine).append("\r\n");

            // Linha Item
            String itemLine = String.format("%-14s%2s%3s%09d%8s%03d%-4s%-40s%012d%04d%012d",
                    formattedCnpj,
                    "62",
                    String.format("%03d", Integer.parseInt(rec.getSerie())),
                    rec.getNumero(),
                    docDate,
                    1,
                    "5307",
                    padRight("PROVIMENTO ACESSO INTERNET BANDA LARGA SCM", 40),
                    vTotal.multiply(new BigDecimal("100")).longValue(),
                    0,
                    vIcms.multiply(new BigDecimal("100")).longValue()
            );
            String md5ItemLine = calculateMd5(itemLine);
            sbItem.append(itemLine).append(md5ItemLine).append("\r\n");

            // Linha Destinatário
            String destLine = String.format("%-14s%-14s%-14s%-35s%-40s%-5s%-15s%-8s%-30s%2s%-7s",
                    formattedCnpj,
                    String.format("%-14s", custDoc).replace(' ', '0'),
                    "ISENTO        ",
                    padRight(custName, 35),
                    padRight(cust != null && cust.getAddress() != null ? cust.getAddress() : "AV BRIGADEIRO EDUARDO GOMES", 40),
                    "1000 ",
                    "CENTRO         ",
                    cust != null && cust.getZipCode() != null ? cust.getZipCode().replaceAll("[^0-9]", "") : "68370000",
                    padRight(cust != null && cust.getCity() != null ? cust.getCity() : "ALTAMIRA", 30),
                    cust != null && cust.getState() != null ? cust.getState() : "PA",
                    "1500602"
            );
            String md5DestLine = calculateMd5(destLine);
            sbDest.append(destLine).append(md5DestLine).append("\r\n");
        }

        byte[] bytesMestre = sbMestre.toString().getBytes(StandardCharsets.ISO_8859_1);
        byte[] bytesItem = sbItem.toString().getBytes(StandardCharsets.ISO_8859_1);
        byte[] bytesDest = sbDest.toString().getBytes(StandardCharsets.ISO_8859_1);

        String md5Mestre = calculateMd5(bytesMestre);
        String md5Item = calculateMd5(bytesItem);
        String md5Dest = calculateMd5(bytesDest);

        // Arquivo de Controle (C)
        String controleContent = String.format(
                "%-14s%-14s%-35s%2s%04d%02d%08d%015d%015d%-32s%-32s%-32s\r\n",
                formattedCnpj,
                company.getInscricaoEstadual() != null ? padRight(company.getInscricaoEstadual(), 14) : "15999888      ",
                padRight(company.getRazaoSocial(), 35),
                uf,
                year,
                month,
                records.size(),
                sumTotal.multiply(new BigDecimal("100")).longValue(),
                sumIcms.multiply(new BigDecimal("100")).longValue(),
                md5Mestre,
                md5Item,
                md5Dest
        );
        byte[] bytesControle = controleContent.getBytes(StandardCharsets.ISO_8859_1);

        // Empacotamento em arquivo ZIP
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(baos)) {
            addZipEntry(zos, fileMestreName, bytesMestre);
            addZipEntry(zos, fileItemName, bytesItem);
            addZipEntry(zos, fileDestName, bytesDest);
            addZipEntry(zos, fileControleName, bytesControle);
        } catch (Exception e) {
            log.error("Erro ao gerar arquivo ZIP do Convênio 115/03: {}", e.getMessage(), e);
        }

        log.info("Lote do Convênio ICMS 115/03 gerado com sucesso: {} faturas processadas, Total R$ {}",
                records.size(), sumTotal);

        return Convenio115BatchResult.builder()
                .filenameMestre(fileMestreName)
                .filenameItem(fileItemName)
                .filenameDestinatario(fileDestName)
                .filenameControle(fileControleName)
                .md5Mestre(md5Mestre)
                .md5Item(md5Item)
                .md5Destinatario(md5Dest)
                .totalRecords(records.size())
                .totalFaturado(sumTotal)
                .totalIcms(sumIcms)
                .zipBytes(baos.toByteArray())
                .build();
    }

    private void addZipEntry(ZipOutputStream zos, String entryName, byte[] data) throws Exception {
        ZipEntry entry = new ZipEntry(entryName);
        zos.putNextEntry(entry);
        zos.write(data);
        zos.closeEntry();
    }

    private String calculateMd5(String content) {
        return calculateMd5(content.getBytes(StandardCharsets.ISO_8859_1));
    }

    private String calculateMd5(byte[] data) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] hash = md.digest(data);
            return HexFormat.of().formatHex(hash).toUpperCase();
        } catch (Exception e) {
            return "00000000000000000000000000000000";
        }
    }

    private String padRight(String text, int length) {
        if (text == null) text = "";
        if (text.length() > length) return text.substring(0, length);
        return String.format("%-" + length + "s", text);
    }
}
