package br.dev.xb.isperp.service;

import br.dev.xb.isperp.entity.Company;
import br.dev.xb.isperp.repository.CompanyRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@SuppressWarnings("null")
public class CompanyService {

    @Autowired
    private CompanyRepository companyRepository;

    public List<Company> getAllCompanies() {
        return companyRepository.findAll();
    }

    public Optional<Company> getCompanyById(UUID id) {
        return companyRepository.findById(id);
    }

    public Optional<Company> getPrimaryCompany() {
        return companyRepository.findFirstByOrderByCreatedAtAsc();
    }

    public Company createCompany(Company company) {
        return companyRepository.save(company);
    }

    public Company updateCompany(UUID id, Company companyDetails) {
        Company company = companyRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Empresa não encontrada"));

        company.setName(companyDetails.getName());
        company.setDocument(companyDetails.getDocument());
        company.setEmail(companyDetails.getEmail());
        company.setPhone(companyDetails.getPhone());
        company.setAddress(companyDetails.getAddress());
        company.setWebsite(companyDetails.getWebsite());
        company.setActive(companyDetails.getActive());

        return companyRepository.save(company);
    }

    public void deleteCompany(UUID id) {
        if (!companyRepository.existsById(id)) {
            throw new RuntimeException("Empresa não encontrada");
        }
        companyRepository.deleteById(id);
    }
}
