package br.dev.xb.isperp.controller;

import br.dev.xb.isperp.dto.CreateSaleRequest;
import br.dev.xb.isperp.entity.Sale;
import br.dev.xb.isperp.service.SaleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/sales")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class SaleController {

    private final SaleService saleService;

    @GetMapping
    public ResponseEntity<List<Sale>> getAllSales() {
        return ResponseEntity.ok(saleService.getAllSales());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Sale> getSaleById(@PathVariable @NonNull UUID id) {
        Optional<Sale> sale = saleService.getSaleById(id);
        return sale.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<?> submitSale(@Valid @NonNull @RequestBody CreateSaleRequest request) {
        try {
            Sale created = saleService.submitSale(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erro ao processar venda");
        }
    }
}
