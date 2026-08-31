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
public class FtthClosureDiagramResponse {
    private FtthClosureResponse closure;
    private List<FtthCableResponse> cables;
    private List<FtthSplitterResponse> splitters;
    private List<FtthFusionResponse> fusions;
    private List<FtthCtoResponse> connectedCtos;
}
