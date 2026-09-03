package br.dev.xb.isperp.service.backup;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jspecify.annotations.Nullable;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StorageTestResult {
    private boolean success;
    private String message;
    @Nullable
    private String detailedError;
    private long latencyMs;
}
