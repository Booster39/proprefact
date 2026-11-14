package com.proprefact.service.facturx;

import com.proprefact.domain.Facture;
import com.proprefact.domain.LigneFacture;
import com.proprefact.domain.PartieFacture;
import java.awt.color.ColorSpace;
import java.awt.color.ICC_Profile;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
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
import org.springframework.stereotype.Component;

/** Mise en page du PDF lisible de la facture, sur une seule page A4. */
@Component
class FacturePdf {
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final float MARGE = 50;
    /** Ordonnée de la première ligne, en points depuis le bas de la page. */
    private static final float HAUT_DE_PAGE = 790;
    /** Abscisse de la colonne de droite de l'en-tête (dates, destinataire). */
    private static final float COLONNE_DROITE = 350;
    /** Abscisse où se terminent les libellés du bloc des totaux. */
    private static final float COLONNE_TOTAUX = 470;

    /** PDF/A-1b : polices incorporées, métadonnées XMP et profil de couleur, sans compression d'objets. */
    byte[] pdfLisible(Facture facture) throws IOException {
        try (PDDocument doc = new PDDocument()) {
            doc.setVersion(1.4f);
            PDFont normal = police(doc, "DejaVuSans.ttf");
            PDFont gras = police(doc, "DejaVuSans-Bold.ttf");
            PDPage page = new PDPage(PDRectangle.A4);
            doc.addPage(page);
            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                dessiner(new Plume(cs, normal, gras), facture);
            }
            conformitePdfA(doc, "Facture " + facture.getNumero());
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            doc.save(out, CompressParameters.NO_COMPRESSION);
            return out.toByteArray();
        }
    }

    private void dessiner(Plume plume, Facture facture) throws IOException {
        PartieFacture vendeur = facture.getVendeur();
        PartieFacture acheteur = facture.getAcheteur();
        float droite = PDRectangle.A4.getWidth() - MARGE;
        float y = HAUT_DE_PAGE;

        plume.texte(vendeur.getRaisonSociale(), MARGE, y, 14, true);
        plume.texteDroite("FACTURE " + facture.getNumero(), droite, y, 14, true);
        y -= 18;
        List<String> emetteur = new ArrayList<>(List.of(
                vendeur.getAdresse(),
                vendeur.getCodePostal() + " " + vendeur.getVille(),
                "SIREN " + vendeur.getSiren()));
        emetteur.add(vendeur.getTvaIntra() != null ? "TVA " + vendeur.getTvaIntra() : Mentions.FRANCHISE);
        float yGauche = plume.lignes(emetteur, MARGE, y, 9);
        plume.lignes(
                List.of(
                        "Date d'émission : " + DATE.format(facture.getDateEmission()),
                        "Échéance : " + DATE.format(facture.getDateEcheance()),
                        "Catégorie de l'opération : prestation de services"),
                COLONNE_DROITE,
                y,
                9);

        y = yGauche - 25;
        plume.texte("Facturé à", COLONNE_DROITE, y, 9, true);
        y -= 13;
        List<String> destinataire = new ArrayList<>(List.of(
                acheteur.getRaisonSociale(),
                acheteur.getAdresse(),
                acheteur.getCodePostal() + " " + acheteur.getVille(),
                "SIREN " + acheteur.getSiren()));
        if (acheteur.getTvaIntra() != null) {
            destinataire.add("TVA " + acheteur.getTvaIntra());
        }
        y = plume.lignes(destinataire, COLONNE_DROITE, y, 9) - 30;

        float xQte = 360, xPu = 430, xTva = 475;
        plume.texte("Désignation", MARGE, y, 9, true);
        plume.texteDroite("Qté", xQte, y, 9, true);
        plume.texteDroite("PU HT", xPu, y, 9, true);
        plume.texteDroite("TVA", xTva, y, 9, true);
        plume.texteDroite("Total HT", droite, y, 9, true);
        plume.trait(MARGE, droite, y - 4);
        y -= 18;
        for (LigneFacture ligne : facture.getLignes()) {
            List<String> morceaux = plume.couper(ligne.getDesignation(), 8, xQte - 40 - MARGE);
            float yLigne = y;
            y = plume.lignes(morceaux, MARGE, y, 8);
            plume.texteDroite(ligne.getQuantite().stripTrailingZeros().toPlainString(), xQte, yLigne, 8, false);
            plume.texteDroite(montant(ligne.getPrixUnitaireHt()), xPu, yLigne, 8, false);
            plume.texteDroite(ligne.getTauxTva().stripTrailingZeros().toPlainString() + " %", xTva, yLigne, 8, false);
            plume.texteDroite(montant(ligne.montantHt()), droite, yLigne, 8, false);
            y -= 4;
        }
        plume.trait(MARGE, droite, y + 6);

        y -= 12;
        plume.texteDroite("Total HT", COLONNE_TOTAUX, y, 9, false);
        plume.texteDroite(montant(facture.getTotalHt()), droite, y, 9, false);
        for (var tva : facture.tvaParTaux().entrySet()) {
            y -= 13;
            plume.texteDroite("TVA " + tva.getKey().toPlainString() + " %", COLONNE_TOTAUX, y, 9, false);
            plume.texteDroite(montant(tva.getValue()), droite, y, 9, false);
        }
        y -= 16;
        plume.texteDroite("Total TTC", COLONNE_TOTAUX, y, 11, true);
        plume.texteDroite(montant(facture.getTotalTtc()), droite, y, 11, true);

        y -= 40;
        List<String> mentions = new ArrayList<>();
        if (facture.getVendeurIban() != null) {
            mentions.add("Règlement par virement – IBAN " + facture.getVendeurIban()
                    + (facture.getVendeurBic() != null ? " – BIC " + facture.getVendeurBic() : ""));
        }
        if (Boolean.TRUE.equals(facture.getVendeurOptionTvaDebits())) {
            mentions.add("Option pour le paiement de la taxe d'après les débits.");
        }
        mentions.addAll(List.of(Mentions.PENALITES, Mentions.INDEMNITE, Mentions.ESCOMPTE));
        plume.lignes(mentions, MARGE, y, 7.5f);
    }

    private static String montant(BigDecimal valeur) {
        // Espace simple comme séparateur de milliers : l'espace fine insécable n'est pas dans toutes les polices.
        return String.format(Locale.FRANCE, "%,.2f €", valeur)
                .replace('\u202F', ' ')
                .replace('\u00A0', ' ');
    }

    private static PDFont police(PDDocument doc, String fichier) throws IOException {
        try (InputStream flux = FacturePdf.class.getResourceAsStream("/fonts/" + fichier)) {
            if (flux == null) {
                throw new IllegalStateException("Police introuvable dans l'application : " + fichier);
            }
            return PDType0Font.load(doc, flux);
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
