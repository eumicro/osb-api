package io.osb;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

/**
 * Minimal dependency rules from arc42 (R-03 / Anhang B): domain and ports stay free of
 * delivery/infrastructure adapters.
 */
@AnalyzeClasses(packages = "io.osb", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

    private static final String[] ADAPTER_PACKAGES = {
        "io.osb.api..",
        "io.osb.infrastructure..",
        "io.osb.workflow.n8n..",
        "jakarta.ws.rs..",
        "io.quarkus.."
    };

    @ArchTest
    static final ArchRule domainDoesNotDependOnAdapters = noClasses()
            .that()
            .resideInAPackage("io.osb.domain..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage(ADAPTER_PACKAGES)
            .because("domain must stay free of API/infra/n8n adapters (arc42 R-03)");

    @ArchTest
    static final ArchRule applicationDoesNotDependOnAdapters = noClasses()
            .that()
            .resideInAPackage("io.osb.application..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage(ADAPTER_PACKAGES)
            .because("application use cases depend on ports, not adapters");

    @ArchTest
    static final ArchRule portsDoNotDependOnAdapters = noClasses()
            .that()
            .resideInAPackage("io.osb.port..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage(ADAPTER_PACKAGES)
            .because("ports define contracts only");

    @ArchTest
    static final ArchRule workflowPortsDoNotDependOnN8n = noClasses()
            .that()
            .resideInAPackage("io.osb.workflow..")
            .and()
            .resideOutsideOfPackage("io.osb.workflow.n8n..")
            .should()
            .dependOnClassesThat()
            .resideInAPackage("io.osb.workflow.n8n..")
            .because("workflow ports must not depend on the n8n adapter");
}
