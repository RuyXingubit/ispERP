package br.dev.xb.isperp.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CgnatScriptImportResponse {
    private int totalParsed;
    private int totalSaved;
    private List<CgnatMappingResponse> importedMappings;
    private List<String> warnings;
}
