package br.dev.xb.isperp.architecture;

import br.dev.xb.isperp.entity.ProcessedEvent;
import br.dev.xb.isperp.entity.RadAcct;
import br.dev.xb.isperp.entity.RadPostAuth;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.library.GeneralCodingRules;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.fields;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Testes automatizados de arquitetura, segurança e qualidade de código (ArchUnit).
 * Garante permanentemente as regras de engenharia do ispERP mesmo sem ferramentas externas.
 */
@SuppressWarnings("null")
class ArchitectureAndQualityRulesTest {

    private static JavaClasses productionClasses;

    @BeforeAll
    static void importClasses() {
        productionClasses = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages("br.dev.xb.isperp");
    }

    @Test
    @DisplayName("Regra UUIDv7: Nenhuma classe de produção deve chamar UUID.randomUUID() (UUIDv4)")
    void noProductionClassShouldCallRandomUuid() {
        noClasses()
                .that().resideInAPackage("br.dev.xb.isperp..")
                .should().callMethod(UUID.class, "randomUUID")
                .because("O projeto exige exclusivamente UUIDv7 (RFC 9562) via UuidCreatorUtils.v7() ou UuidCreator")
                .check(productionClasses);
    }

    @Test
    @DisplayName("Regra de Logs: Nenhuma classe de produção deve usar System.out, System.err ou printStackTrace()")
    void noProductionClassShouldUseStandardStreamsOrPrintStackTrace() {
        GeneralCodingRules.NO_CLASSES_SHOULD_ACCESS_STANDARD_STREAMS
                .because("Logs devem utilizar SLF4J (@Slf4j) com sanitização adequada")
                .check(productionClasses);
    }

    @Test
    @DisplayName("Regra de Camadas: Entidades, Repositories e Services nunca devem depender de Controllers")
    void domainAndServicesShouldNotDependOnControllers() {
        noClasses()
                .that().resideInAnyPackage(
                        "br.dev.xb.isperp.entity..",
                        "br.dev.xb.isperp.repository..",
                        "br.dev.xb.isperp.service..")
                .should().dependOnClassesThat().resideInAPackage("br.dev.xb.isperp.controller..")
                .because("Camadas de domínio, persistência e negócio não podem acoplar-se à camada HTTP")
                .check(productionClasses);
    }

    @Test
    @DisplayName("Regra de Banco de Dados: Todos os campos @Id de entidades JPA de domínio devem ser do tipo UUID")
    void allEntityIdsShouldBeUuid() {
        fields()
                .that().areAnnotatedWith(Id.class)
                .and().areNotDeclaredIn(ProcessedEvent.class)
                .and().areNotDeclaredIn(RadAcct.class)
                .and().areNotDeclaredIn(RadPostAuth.class)
                .should().haveRawType(UUID.class)
                .because("Todas as chaves primárias de domínio do ispERP são padronizadas em UUIDv7 (exceto tabelas nativas do protocolo FreeRADIUS e chave composta @IdClass)")
                .check(productionClasses);
    }

    @Test
    @DisplayName("Regra API-First (Zero Trust DTOs): Controllers não devem receber ou retornar @Entity diretamente")
    void controllersShouldNotReturnEntitiesDirectly() {
        methods()
                .that().areDeclaredInClassesThat().areAnnotatedWith(RestController.class)
                .and().arePublic()
                .should().notHaveRawReturnType(com.tngtech.archunit.base.DescribedPredicate.describe(
                        "uma classe anotada com @Entity",
                        javaClass -> javaClass.isAnnotatedWith(Entity.class)))
                .because("Rotas HTTP devem expor exclusivamente DTOs de contrato para prevenir Mass Assignment e vazamento de dados")
                .check(productionClasses);
    }

    @Test
    @DisplayName("Regra JSpecify: Pacotes principais do Backend devem possuir package-info.java com @NullMarked")
    void corePackagesShouldBeNullMarked() throws ClassNotFoundException {
        List<String> requiredPackages = List.of(
                "br.dev.xb.isperp.controller",
                "br.dev.xb.isperp.service",
                "br.dev.xb.isperp.repository",
                "br.dev.xb.isperp.entity",
                "br.dev.xb.isperp.security",
                "br.dev.xb.isperp.config",
                "br.dev.xb.isperp.exception",
                "br.dev.xb.isperp.mapper"
        );

        for (String pkgName : requiredPackages) {
            Class<?> packageInfoClass = Class.forName(pkgName + ".package-info");
            Package pkg = packageInfoClass.getPackage();
            assertNotNull(pkg, "Pacote carregado não encontrado: " + pkgName);
            assertTrue(
                    pkg.isAnnotationPresent(NullMarked.class),
                    "O pacote " + pkgName + " deve possuir package-info.java anotado com @NullMarked"
            );
        }
    }
}
