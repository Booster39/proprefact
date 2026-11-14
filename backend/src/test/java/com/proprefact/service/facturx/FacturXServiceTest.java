package com.proprefact.service.facturx;

import static org.assertj.core.api.Assertions.assertThat;

import com.proprefact.domain.Client;
import com.proprefact.domain.Entreprise;
import com.proprefact.domain.Facture;
import com.proprefact.domain.LigneFacture;
import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mustangproject.ZUGFeRD.ZUGFeRDImporter;

/** Vérifie le fichier produit pour une facture à trois taux de TVA, sans base de données ni Spring. */
class FacturXServiceTest {
    private static String xml;
    private static String textePdf;

    @BeforeAll
    static void genererUneFacture() throws Exception {
        Facture facture = Facture.brouillon(client(), "2026-11");
        facture.ajouter(ligne("Entretien des bureaux – Bureaux Bellecour – novembre 2026", "1", "1480.00", "20"));
        facture.ajouter(ligne("Vitrerie", "2.5", "333.33", "10"));
        facture.ajouter(ligne("Prestation exonérée", "1", "50", "0"));
        facture.valider("F-2026-00042", LocalDate.of(2026, 11, 14), 30, vendeur());

        byte[] fichier = new FacturXService(new FacturePdf(), new FactureXml()).generer(facture);

        xml = new String(new ZUGFeRDImporter(new ByteArrayInputStream(fichier)).getRawXML(), StandardCharsets.UTF_8);
        try (PDDocument document = Loader.loadPDF(fichier)) {
            PDFTextStripper lecteur = new PDFTextStripper();
            lecteur.setSortByPosition(true);
            textePdf = lecteur.getText(document);
        }
    }

    @Test
    void leXmlPorteLesTotauxEtLesIdentifiantsDesDeuxParties() {
        assertThat(xml)
                .contains("urn:cen.eu:en16931:2017")
                .contains("<ram:ID>F-2026-00042</ram:ID>")
                .contains("552100554", "732829320")
                .contains("<ram:TaxBasisTotalAmount>2363.33</ram:TaxBasisTotalAmount>")
                .contains("<ram:GrandTotalAmount>2742.66</ram:GrandTotalAmount>")
                .contains("Paiement à 30 jours, au plus tard le 14/12/2026");
    }

    @Test
    void lePdfAfficheLesMemesMontantsQueLeXml() {
        assertThat(textePdf)
                .contains("FACTURE F-2026-00042")
                .contains("Date d'émission : 14/11/2026")
                .contains("Échéance : 14/12/2026")
                .contains("Total HT 2 363,33 €")
                .contains("TVA 0 % 0,00 €")
                .contains("TVA 10 % 83,33 €")
                .contains("TVA 20 % 296,00 €")
                .contains("Total TTC 2 742,66 €");
    }

    @Test
    void lePdfPorteLesMentionsObligatoires() {
        assertThat(textePdf)
                .contains("SIREN 552100554", "SIREN 732829320")
                .contains("IBAN FR7630006000011234567890189 – BIC AGRIFRPP")
                .contains("Option pour le paiement de la taxe d'après les débits.")
                .contains(Mentions.PENALITES, Mentions.INDEMNITE, Mentions.ESCOMPTE);
    }

    private static Client client() {
        Client client = new Client();
        client.setRaisonSociale("Cabinet Durand");
        client.setSiren("732829320");
        client.setTvaIntra("FR44732829320");
        client.setAdresse("12 rue de la Paix");
        client.setCodePostal("69002");
        client.setVille("Lyon");
        return client;
    }

    private static Entreprise vendeur() {
        Entreprise entreprise = new Entreprise();
        entreprise.setRaisonSociale("Net Plus SARL");
        entreprise.setSiren("552100554");
        entreprise.setTvaIntra("FR00552100554");
        entreprise.setAdresse("5 rue des Lilas");
        entreprise.setCodePostal("69003");
        entreprise.setVille("Lyon");
        entreprise.setIban("FR7630006000011234567890189");
        entreprise.setBic("AGRIFRPP");
        entreprise.setOptionTvaDebits(true);
        return entreprise;
    }

    private static LigneFacture ligne(String designation, String quantite, String prixUnitaireHt, String tauxTva) {
        LigneFacture ligne = new LigneFacture();
        ligne.setDesignation(designation);
        ligne.setQuantite(new BigDecimal(quantite));
        ligne.setPrixUnitaireHt(new BigDecimal(prixUnitaireHt));
        ligne.setTauxTva(new BigDecimal(tauxTva));
        return ligne;
    }
}
