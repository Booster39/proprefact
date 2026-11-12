package com.proprefact.service;

import java.awt.color.ColorSpace;
import java.awt.color.ICC_Profile;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import org.apache.pdfbox.pdfwriter.compress.CompressParameters;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDMetadata;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.pdmodel.graphics.color.PDOutputIntent;
import org.apache.xmpbox.XMPMetadata;
import org.apache.xmpbox.schema.DublinCoreSchema;
import org.apache.xmpbox.schema.PDFAIdentificationSchema;
import org.apache.xmpbox.xml.XmpSerializer;
import org.mustangproject.BankDetails;
import org.mustangproject.IncludedNote;
import org.mustangproject.Invoice;
import org.mustangproject.Item;
import org.mustangproject.LegalOrganisation;
import org.mustangproject.Product;
import org.mustangproject.SchemedID;
import org.mustangproject.SubjectCode;
import org.mustangproject.TradeParty;
import org.mustangproject.ZUGFeRD.Profiles;
import org.mustangproject.ZUGFeRD.ZUGFeRDExporterFromA1;
import org.springframework.stereotype.Service;

import com.proprefact.domain.Client;
import com.proprefact.domain.Entreprise;
import com.proprefact.domain.Facture;
import com.proprefact.domain.LigneFacture;

/**
 * Produit la facture au format Factur-X : un PDF lisible (PDF/A-3) qui embarque les mêmes
 * données en XML structuré (profil EN 16931).
 */
@Service
public class FacturXService {
    /** Identifiant de schéma ISO 6523 du SIREN. */
    static final String SCHEMA_SIREN = "0002";
    /** Schéma des adresses de facturation électronique de l'annuaire français. */
    static final String SCHEMA_ADRESSE_ELECTRONIQUE = "0225";
    /** Code UN/ECE « unité » : un forfait mensuel ou une prestation. */
    static final String UNITE = "C62";

    static final String MENTION_PENALITES = "En cas de retard de paiement, pénalités au taux de trois fois le taux "
            + "d'intérêt légal, exigibles sans rappel.";
    static final String MENTION_INDEMNITE = "Indemnité forfaitaire pour frais de recouvrement en cas de retard de "
            + "paiement : 40 €.";
    static final String MENTION_ESCOMPTE = "Pas d'escompte pour paiement anticipé.";
    static final String MENTION_FRANCHISE = "TVA non applicable, art. 293 B du CGI";

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final float MARGE = 50;

    public byte[] generer(Facture f, Entreprise e) {
        try {
            byte[] pdf = pdfLisible(f, e);
            ByteArrayOutputStream sortie = new ByteArrayOutputStream();
            ZUGFeRDExporterFromA1 exporteur = new ZUGFeRDExporterFromA1()
                    .setProducer("ProPreFact")
                    .setCreator(e.getRaisonSociale())
                    .setProfile(Profiles.getByName("EN16931"))
                    .load(pdf);
            exporteur.setTransaction(donneesStructurees(f, e));
            exporteur.export(sortie);
            return sortie.toByteArray();
        } catch (IOException ex) {
            throw new UncheckedIOException("Génération Factur-X impossible pour la facture " + f.getNumero(), ex);
        }
    }

    // ------------------------------------------------------------------ XML

    Invoice donneesStructurees(Facture f, Entreprise e) {
        Client c = f.getClient();
        TradeParty vendeur = new TradeParty(e.getRaisonSociale(), e.getAdresse(), e.getCodePostal(), e.getVille(), "FR")
                .setLegalOrganisation(new LegalOrganisation(e.getSiren(), SCHEMA_SIREN))
                .addUriUniversalCommunicationID(new SchemedID(SCHEMA_ADRESSE_ELECTRONIQUE, e.getSiren()));
        if (e.getTvaIntra() != null) {
            vendeur.addVATID(e.getTvaIntra());
        }
        if (e.getIban() != null) {
            vendeur.addBankDetails(new BankDetails(e.getIban(), e.getBic()));
        }
        TradeParty acheteur = new TradeParty(c.getRaisonSociale(), c.getAdresse(), c.getCodePostal(), c.getVille(), "FR")
                .setLegalOrganisation(new LegalOrganisation(c.getSiren(), SCHEMA_SIREN))
                .addUriUniversalCommunicationID(new SchemedID(SCHEMA_ADRESSE_ELECTRONIQUE, c.getSiren()));
        if (c.getTvaIntra() != null) {
            acheteur.addVATID(c.getTvaIntra());
        }

        Invoice facture = new Invoice()
                .setNumber(f.getNumero())
                .setCurrency("EUR")
                .setIssueDate(date(f.getDateEmission()))
                .setDueDate(date(f.getDateEcheance()))
                .setDeliveryDate(date(finDePrestation(f)))
                .setSender(vendeur)
                .setRecipient(acheteur)
                .setPaymentTermDescription("Paiement à %d jours, au plus tard le %s".formatted(
                        FacturationService.DELAI_PAIEMENT_JOURS, DATE.format(f.getDateEcheance())));
        if (f.getPeriode() != null) {
            YearMonth mois = YearMonth.parse(f.getPeriode());
            facture.setDetailedDeliveryPeriod(date(mois.atDay(1)), date(mois.atEndOfMonth()));
        }
        // Exigibilité de la TVA : 5 = à la facturation (option débits), 72 = à l'encaissement (régime des services).
        facture.setVATDueDateTypeCode(e.isOptionTvaDebits() ? "5" : "72");
        facture.addNotes(List.of(
                new IncludedNote(MENTION_PENALITES, SubjectCode.PMD),
                new IncludedNote(MENTION_INDEMNITE, SubjectCode.PMT),
                new IncludedNote(MENTION_ESCOMPTE, SubjectCode.AAB)));

        for (LigneFacture l : f.getLignes()) {
            Product produit = new Product(l.getDesignation(), "", UNITE, l.getTauxTva());
            if (l.getTauxTva().signum() == 0) {
                produit.setTaxCategoryCode("E").setTaxExemptionReason(MENTION_FRANCHISE);
            }
            facture.addItem(new Item(produit, l.getPrixUnitaireHt(), l.getQuantite()));
        }
        return facture;
    }

    private static LocalDate finDePrestation(Facture f) {
        return f.getPeriode() != null ? YearMonth.parse(f.getPeriode()).atEndOfMonth() : f.getDateEmission();
    }

    private static Date date(LocalDate d) {
        return Date.from(d.atStartOfDay(ZoneId.systemDefault()).toInstant());
    }

    // ------------------------------------------------------------------ PDF

    /** PDF/A-1b : polices incorporées, métadonnées XMP et profil de couleur, sans compression d'objets. */
    private byte[] pdfLisible(Facture f, Entreprise e) throws IOException {
        try (PDDocument doc = new PDDocument()) {
            doc.setVersion(1.4f);
            PDFont normal = police(doc, "DejaVuSans.ttf");
            PDFont gras = police(doc, "DejaVuSans-Bold.ttf");
            PDPage page = new PDPage(PDRectangle.A4);
            doc.addPage(page);
            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                dessiner(new Plume(cs, normal, gras), f, e);
            }
            conformitePdfA(doc, "Facture " + f.getNumero());
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            doc.save(out, CompressParameters.NO_COMPRESSION);
            return out.toByteArray();
        }
    }

    private void dessiner(Plume p, Facture f, Entreprise e) throws IOException {
        Client c = f.getClient();
        float droite = PDRectangle.A4.getWidth() - MARGE;
        float y = 790;

        p.texte(e.getRaisonSociale(), MARGE, y, 14, true);
        p.texteDroite("FACTURE " + f.getNumero(), droite, y, 14, true);
        y -= 18;
        List<String> emetteur = new ArrayList<>(List.of(e.getAdresse(), e.getCodePostal() + " " + e.getVille(), "SIREN " + e.getSiren()));
        emetteur.add(e.getTvaIntra() != null ? "TVA " + e.getTvaIntra() : MENTION_FRANCHISE);
        float yGauche = p.lignes(emetteur, MARGE, y, 9);
        p.lignes(List.of("Date d'émission : " + DATE.format(f.getDateEmission()),
                "Échéance : " + DATE.format(f.getDateEcheance()),
                "Catégorie de l'opération : prestation de services"), 350, y, 9);

        y = yGauche - 25;
        p.texte("Facturé à", 350, y, 9, true);
        y -= 13;
        List<String> destinataire = new ArrayList<>(
                List.of(c.getRaisonSociale(), c.getAdresse(), c.getCodePostal() + " " + c.getVille(), "SIREN " + c.getSiren()));
        if (c.getTvaIntra() != null) {
            destinataire.add("TVA " + c.getTvaIntra());
        }
        y = p.lignes(destinataire, 350, y, 9) - 30;

        float xQte = 360, xPu = 430, xTva = 475;
        p.texte("Désignation", MARGE, y, 9, true);
        p.texteDroite("Qté", xQte, y, 9, true);
        p.texteDroite("PU HT", xPu, y, 9, true);
        p.texteDroite("TVA", xTva, y, 9, true);
        p.texteDroite("Total HT", droite, y, 9, true);
        p.trait(MARGE, droite, y - 4);
        y -= 18;
        for (LigneFacture l : f.getLignes()) {
            List<String> morceaux = p.couper(l.getDesignation(), 8, xQte - 40 - MARGE);
            float yLigne = y;
            y = p.lignes(morceaux, MARGE, y, 8);
            p.texteDroite(l.getQuantite().stripTrailingZeros().toPlainString(), xQte, yLigne, 8, false);
            p.texteDroite(montant(l.getPrixUnitaireHt()), xPu, yLigne, 8, false);
            p.texteDroite(l.getTauxTva().stripTrailingZeros().toPlainString() + " %", xTva, yLigne, 8, false);
            p.texteDroite(montant(l.montantHt()), droite, yLigne, 8, false);
            y -= 4;
        }
        p.trait(MARGE, droite, y + 6);

        y -= 12;
        p.texteDroite("Total HT", 470, y, 9, false);
        p.texteDroite(montant(f.getTotalHt()), droite, y, 9, false);
        for (var base : f.basesParTaux().entrySet()) {
            y -= 13;
            BigDecimal tva = base.getValue().multiply(base.getKey()).divide(BigDecimal.valueOf(100), 2,
                    RoundingMode.HALF_UP);
            p.texteDroite("TVA " + base.getKey().toPlainString() + " %", 470, y, 9, false);
            p.texteDroite(montant(tva), droite, y, 9, false);
        }
        y -= 16;
        p.texteDroite("Total TTC", 470, y, 11, true);
        p.texteDroite(montant(f.getTotalTtc()), droite, y, 11, true);

        y -= 40;
        List<String> mentions = new ArrayList<>();
        if (e.getIban() != null) {
            mentions.add("Règlement par virement – IBAN " + e.getIban() + (e.getBic() != null ? " – BIC " + e.getBic() : ""));
        }
        if (e.isOptionTvaDebits()) {
            mentions.add("Option pour le paiement de la taxe d'après les débits.");
        }
        mentions.addAll(List.of(MENTION_PENALITES, MENTION_INDEMNITE, MENTION_ESCOMPTE));
        p.lignes(mentions, MARGE, y, 7.5f);
    }

    private static String montant(BigDecimal m) {
        // Espace simple comme séparateur de milliers : l'espace fine insécable n'est pas dans toutes les polices.
        return String.format(Locale.FRANCE, "%,.2f €", m).replace(' ', ' ').replace(' ', ' ');
    }

    private static PDFont police(PDDocument doc, String fichier) throws IOException {
        try (InputStream in = FacturXService.class.getResourceAsStream("/fonts/" + fichier)) {
            return PDType0Font.load(doc, in);
        }
    }

    private static void conformitePdfA(PDDocument doc, String titre) throws IOException {
        try {
            XMPMetadata xmp = XMPMetadata.createXMPMetadata();
            DublinCoreSchema dc = xmp.createAndAddDublinCoreSchema();
            dc.setTitle(titre);
            PDFAIdentificationSchema id = xmp.createAndAddPDFAIdentificationSchema();
            id.setPart(1);
            id.setConformance("B");
            ByteArrayOutputStream octets = new ByteArrayOutputStream();
            new XmpSerializer().serialize(xmp, octets, true);
            PDMetadata metadonnees = new PDMetadata(doc);
            metadonnees.importXMPMetadata(octets.toByteArray());
            doc.getDocumentCatalog().setMetadata(metadonnees);
        } catch (Exception ex) {
            throw new IOException("Métadonnées PDF/A invalides", ex);
        }
        doc.getDocumentInformation().setTitle(titre);
        byte[] srgb = ICC_Profile.getInstance(ColorSpace.CS_sRGB).getData();
        PDOutputIntent intention = new PDOutputIntent(doc, new ByteArrayInputStream(srgb));
        intention.setInfo("sRGB IEC61966-2.1");
        intention.setOutputCondition("sRGB IEC61966-2.1");
        intention.setOutputConditionIdentifier("sRGB IEC61966-2.1");
        intention.setRegistryName("http://www.color.org");
        doc.getDocumentCatalog().addOutputIntent(intention);
    }

    /** Petites aides de dessin au-dessus de PDFBox. */
    private record Plume(PDPageContentStream cs, PDFont normal, PDFont gras) {

        void texte(String s, float x, float y, float taille, boolean enGras) throws IOException {
            cs.beginText();
            cs.setFont(enGras ? gras : normal, taille);
            cs.newLineAtOffset(x, y);
            cs.showText(s);
            cs.endText();
        }

        void texteDroite(String s, float xDroite, float y, float taille, boolean enGras) throws IOException {
            texte(s, xDroite - largeur(s, enGras ? gras : normal, taille), y, taille, enGras);
        }

        /** Écrit les lignes l'une sous l'autre et renvoie l'ordonnée de la ligne suivante. */
        float lignes(List<String> lignes, float x, float y, float taille) throws IOException {
            for (String s : lignes) {
                texte(s, x, y, taille, false);
                y -= taille + 3.5f;
            }
            return y;
        }

        void trait(float x1, float x2, float y) throws IOException {
            cs.setLineWidth(0.5f);
            cs.moveTo(x1, y);
            cs.lineTo(x2, y);
            cs.stroke();
        }

        List<String> couper(String s, float taille, float largeurMax) throws IOException {
            List<String> resultat = new ArrayList<>();
            StringBuilder ligne = new StringBuilder();
            for (String mot : s.split(" ")) {
                String essai = ligne.isEmpty() ? mot : ligne + " " + mot;
                if (!ligne.isEmpty() && largeur(essai, normal, taille) > largeurMax) {
                    resultat.add(ligne.toString());
                    ligne = new StringBuilder(mot);
                } else {
                    ligne = new StringBuilder(essai);
                }
            }
            resultat.add(ligne.toString());
            return resultat;
        }

        private static float largeur(String s, PDFont police, float taille) throws IOException {
            return police.getStringWidth(s) / 1000 * taille;
        }
    }
}
