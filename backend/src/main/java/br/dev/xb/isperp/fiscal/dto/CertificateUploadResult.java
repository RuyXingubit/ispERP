package br.dev.xb.isperp.fiscal.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jspecify.annotations.Nullable;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CertificateUploadResult {
    private boolean success;
    @Nullable
    private String subjectCnpj;
    @Nullable
    private String subjectName;
    @Nullable
    private LocalDateTime validUntil;
    @Nullable
    private String message;
    @Nullable
    private String errorMessage;
}
