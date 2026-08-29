package br.dev.xb.isperp.fiscal;

import br.dev.xb.isperp.entity.FiscalCompany;
import br.dev.xb.isperp.entity.FiscalGatewayConfig;
import br.dev.xb.isperp.repository.FiscalCompanyRepository;
import br.dev.xb.isperp.repository.FiscalGatewayConfigRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
@SuppressWarnings("null")
public class FiscalGatewayResolver {

    private final List<FiscalGateway> fiscalGateways;
    private final FiscalGatewayConfigRepository configRepository;
    private final FiscalCompanyRepository companyRepository;

    /**
     * Resolve o FiscalGateway e suas configurações ativas para a empresa informada.
     */
    public ResolvedFiscalGateway resolve(@Nullable UUID companyId) {
        FiscalCompany resolvedCompany = null;
        if (companyId != null) {
            resolvedCompany = companyRepository.findById(companyId).orElse(null);
        }
        if (resolvedCompany == null) {
            resolvedCompany = companyRepository.findFirstByIsActiveTrue()
                    .orElseGet(() -> FiscalCompany.builder()
                            .cnpj("12.345.678/0001-95")
                            .razaoSocial("Provedor Xingu Telecom Ltda")
                            .inscricaoEstadual("15999888")
                            .logradouro("Av. Brigadeiro Eduardo Gomes")
                            .numero("1000")
                            .bairro("Centro")
                            .cidade("Altamira")
                            .uf("PA")
                            .cep("68370-000")
                            .codigoIbge("1500602")
                            .nfcomAmbiente("HOMOLOGACAO")
                            .nfcomSerie("1")
                            .build());
        }

        final FiscalCompany company = resolvedCompany;
        final UUID finalCompanyId = company.getId() != null ? company.getId() : UUID.randomUUID();

        Optional<FiscalGatewayConfig> configOpt = (company.getId() != null)
                ? configRepository.findByCompanyIdAndIsActiveTrue(company.getId())
                : configRepository.findFirstByIsActiveTrue();

        FiscalGatewayConfig config = configOpt.orElseGet(() -> FiscalGatewayConfig.builder()
                .companyId(finalCompanyId)
                .gatewayType(FiscalGatewayType.XINGUBIT_PAY)
                .environment("HOMOLOGACAO")
                .baseUrl("https://pay.xingubit.com.br")
                .isActive(true)
                .build());

        FiscalGatewayType targetType = config.getGatewayType() != null ? config.getGatewayType() : FiscalGatewayType.XINGUBIT_PAY;

        FiscalGateway gateway = fiscalGateways.stream()
                .filter(g -> g.getGatewayType() == targetType)
                .findFirst()
                .orElseGet(() -> fiscalGateways.stream()
                        .filter(g -> g.getGatewayType() == FiscalGatewayType.XINGUBIT_PAY)
                        .findFirst()
                        .orElseThrow(() -> new RuntimeException("Nenhum driver de emissão fiscal disponível")));

        return new ResolvedFiscalGateway(gateway, config, company);
    }

    public record ResolvedFiscalGateway(FiscalGateway gateway, FiscalGatewayConfig config, FiscalCompany company) {}
}
