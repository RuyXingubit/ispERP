package br.dev.xb.isperp.controller;

import br.dev.xb.isperp.api.contract.SalesApi;
import br.dev.xb.isperp.api.dto.CreateSaleRequest;
import br.dev.xb.isperp.api.dto.SaleResponse;
import br.dev.xb.isperp.entity.Sale;
import br.dev.xb.isperp.mapper.SaleMapper;
import br.dev.xb.isperp.service.SaleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/sales")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
@SuppressWarnings("null")
public class SaleController implements SalesApi {

    private final SaleService saleService;
    private final SaleMapper saleMapper;

    @Override
    @GetMapping
    public ResponseEntity<List<SaleResponse>> getAllSales() {
        return ResponseEntity.ok(saleMapper.toResponseList(saleService.getAllSales()));
    }

    @Override
    @GetMapping("/{id}")
    public ResponseEntity<SaleResponse> getSaleById(@PathVariable UUID id) {
        return saleService.getSaleById(id)
                .map(saleMapper::toResponse)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Override
    @PostMapping
    public ResponseEntity<SaleResponse> submitSale(@Valid @RequestBody CreateSaleRequest request) {
        try {
            br.dev.xb.isperp.dto.CreateSaleRequest legacyRequest = saleMapper.toLegacyRequest(request);
            Sale created = saleService.submitSale(legacyRequest);
            return ResponseEntity.status(HttpStatus.CREATED).body(saleMapper.toResponse(created));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
