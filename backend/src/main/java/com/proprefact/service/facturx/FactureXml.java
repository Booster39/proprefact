package com.proprefact.service.facturx;

import com.proprefact.domain.Facture;
import com.proprefact.domain.LigneFacture;
import com.proprefact.domain.PartieFacture;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import org.mustangproject.BankDetails;
import org.mustangproject.IncludedNote;
import org.mustangproject.Invoice;
import org.mustangproject.Item;
import org.mustangproject.LegalOrganisation;
import org.mustangproject.Product;
import org.mustangproject.SchemedID;
import org.mustangproject.SubjectCode;
import org.mustangproject.TradeParty;
import org.springframework.stereotype.Component;

/** Données structurées de la facture (XML EN 16931), construites avec la bibliothèque Mustang. */
@Component
class FactureXml {
    /** Identifiant de schéma ISO 6523 du SIREN. */
    static final String SCHEMA_SIREN = "0002";
    /** Schéma des adresses de facturation électronique de l'annuaire français. */
    static final String SCHEMA_ADRESSE_ELECTRONIQUE = "0225";
    /** Code UN/ECE « unité » : un forfait mensuel ou une prestation. */
    static final String UNITE = "C62";

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    Invoice donneesStructurees(Facture facture) {
        PartieFacture vendeur = facture.getVendeur();
        PartieFacture acheteur = facture.getAcheteur();
        TradeParty partieVendeur = new TradeParty(
                        vendeur.getRaisonSociale(),
                        vendeur.getAdresse(),
                        vendeur.getCodePostal(),
                        vendeur.getVille(),
                        "FR")
                .setLegalOrganisation(new LegalOrganisation(vendeur.getSiren(), SCHEMA_SIREN))
                .addUriUniversalCommunicationID(new SchemedID(SCHEMA_ADRESSE_ELECTRONIQUE, vendeur.getSiren()));
        if (vendeur.getTvaIntra() != null) {
            partieVendeur.addVATID(vendeur.getTvaIntra());
        }
        if (facture.getVendeurIban() != null) {
            partieVendeur.addBankDetails(new BankDetails(facture.getVendeurIban(), facture.getVendeurBic()));
        }
        TradeParty partieAcheteur = new TradeParty(
                        acheteur.getRaisonSociale(),
                        acheteur.getAdresse(),
                        acheteur.getCodePostal(),
                        acheteur.getVille(),
                        "FR")
                .setLegalOrganisation(new LegalOrganisation(acheteur.getSiren(), SCHEMA_SIREN))
                .addUriUniversalCommunicationID(new SchemedID(SCHEMA_ADRESSE_ELECTRONIQUE, acheteur.getSiren()));
        if (acheteur.getTvaIntra() != null) {
            partieAcheteur.addVATID(acheteur.getTvaIntra());
        }

        Invoice donnees = new Invoice()
                .setNumber(facture.getNumero())
                .setCurrency("EUR")
                .setIssueDate(date(facture.getDateEmission()))
                .setDueDate(date(facture.getDateEcheance()))
                .setDeliveryDate(date(finDePrestation(facture)))
                .setSender(partieVendeur)
                .setRecipient(partieAcheteur)
                .setPaymentTermDescription("Paiement à %d jours, au plus tard le %s"
                        .formatted(
                                ChronoUnit.DAYS.between(facture.getDateEmission(), facture.getDateEcheance()),
                                DATE.format(facture.getDateEcheance())));
        YearMonth mois = facture.getPeriode();
        if (mois != null) {
            donnees.setDetailedDeliveryPeriod(date(mois.atDay(1)), date(mois.atEndOfMonth()));
        }
        // Exigibilité de la TVA : 5 = à la facturation (option débits), 72 = à l'encaissement (régime des
        // services).
        donnees.setVATDueDateTypeCode(Boolean.TRUE.equals(facture.getVendeurOptionTvaDebits()) ? "5" : "72");
        donnees.addNotes(List.of(
                new IncludedNote(Mentions.PENALITES, SubjectCode.PMD),
                new IncludedNote(Mentions.INDEMNITE, SubjectCode.PMT),
                new IncludedNote(Mentions.ESCOMPTE, SubjectCode.AAB)));

        for (LigneFacture ligne : facture.getLignes()) {
            Product produit = new Product(ligne.getDesignation(), "", UNITE, ligne.getTauxTva());
            if (ligne.getTauxTva().signum() == 0) {
                produit.setTaxCategoryCode("E").setTaxExemptionReason(Mentions.FRANCHISE);
            }
            donnees.addItem(new Item(produit, ligne.getPrixUnitaireHt(), ligne.getQuantite()));
        }
        return donnees;
    }

    private static LocalDate finDePrestation(Facture facture) {
        return facture.getPeriode() != null ? facture.getPeriode().atEndOfMonth() : facture.getDateEmission();
    }

    private static Date date(LocalDate jour) {
        return Date.from(jour.atStartOfDay(ZoneId.systemDefault()).toInstant());
    }
}
