package com.proprefact;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Règles de dépendances entre paquets (voir « Conventions » dans le README). Un échec ici signifie qu'une classe
 * a été placée dans le mauvais paquet ou qu'une couche en appelle une autre à l'envers.
 */
@AnalyzeClasses(packages = "com.proprefact", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

    @ArchTest
    static final ArchRule le_domaine_ne_depend_d_aucune_autre_couche = noClasses()
            .that()
            .resideInAPackage("..domain..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage("..service..", "..web..", "..repo..", "..config..", "..tenant..", "..proprietes..");

    @ArchTest
    static final ArchRule les_services_ignorent_la_couche_web = noClasses()
            .that()
            .resideInAnyPackage("..service..", "..repo..", "..tenant..", "..proprietes..")
            .should()
            .dependOnClassesThat()
            .resideInAPackage("com.proprefact.web..");

    @ArchTest
    static final ArchRule seule_la_couche_web_parle_http = noClasses()
            .that()
            .resideOutsideOfPackage("com.proprefact.web..")
            .should()
            .dependOnClassesThat()
            .areAssignableTo(ResponseStatusException.class);

    @ArchTest
    static final ArchRule les_controleurs_passent_par_les_services = noClasses()
            .that()
            .resideInAPackage("com.proprefact.web..")
            .should()
            .dependOnClassesThat()
            .resideInAPackage("..repo..")
            .orShould()
            .beAnnotatedWith(Transactional.class);

    @ArchTest
    static final ArchRule aucun_cycle_entre_paquets =
            slices().matching("com.proprefact.(*)..").should().beFreeOfCycles();
}
