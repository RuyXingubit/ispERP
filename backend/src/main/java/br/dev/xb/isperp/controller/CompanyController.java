package br.dev.xb.isperp.controller;

import br.dev.xb.isperp.api.contract.CompaniesApi;
import br.dev.xb.isperp.api.dto.CompanyCreateRequest;
import br.dev.xb.isperp.api.dto.CompanyResponse;
import br.dev.xb.isperp.api.dto.CompanyUpdateRequest;
import br.dev.xb.isperp.entity.Company;
import br.dev.xb.isperp.mapper.CompanyMapper;
import br.dev.xb.isperp.service.CompanyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/companies")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
@SuppressWarnings("null")
public class CompanyController implements CompaniesApi {

    private final CompanyService companyService;
    private final CompanyMapper companyMapper;

    @Override
    @GetMapping
    public ResponseEntity<List<CompanyResponse>> getAllCompanies() {
        return ResponseEntity.ok(companyMapper.toResponseList(companyService.getAllCompanies()));
    }

    @Override
    @GetMapping("/primary")
    public ResponseEntity<CompanyResponse> getPrimaryCompany() {
        return companyService.getPrimaryCompany()
                .map(companyMapper::toResponse)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Override
    @GetMapping("/{id}")
    public ResponseEntity<CompanyResponse> getCompanyById(@PathVariable UUID id) {
        return companyService.getCompanyById(id)
                .map(companyMapper::toResponse)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Override
    @PostMapping
    public ResponseEntity<CompanyResponse> createCompany(@Valid @RequestBody CompanyCreateRequest request) {
        try {
            Company entity = companyMapper.toEntity(request);
            Company created = companyService.createCompany(entity);
            return ResponseEntity.status(HttpStatus.CREATED).body(companyMapper.toResponse(created));
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @Override
    @PutMapping("/{id}")
    public ResponseEntity<CompanyResponse> updateCompany(@PathVariable UUID id, @Valid @RequestBody CompanyUpdateRequest request) {
        try {
            Company companyDetails = new Company();
            companyMapper.updateEntityFromRequest(request, companyDetails);
            Company updated = companyService.updateCompany(id, companyDetails);
            return ResponseEntity.ok(companyMapper.toResponse(updated));
        } catch (RuntimeException e) {
            if ("Empresa não encontrada".equals(e.getMessage())) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @Override
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCompany(@PathVariable UUID id) {
        try {
            companyService.deleteCompany(id);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }
}
