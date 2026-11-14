package com.proprefact.service.facturx;

import com.proprefact.domain.Facture;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import org.mustangproject.ZUGFeRD.Profiles;
import org.mustangproject.ZUGFeRD.ZUGFeRDExporterFromA1;
import org.springframework.stereotype.Service;

/**
 * Produit la facture au format Factur-X : un PDF lisible qui embarque les mêmes données en XML structuré
 * (profil EN 16931). Le PDF lisible est produit en PDF/A-1b, puis la bibliothèque Mustang le convertit en
 * PDF/A-3 en y joignant le XML. Ne lit que les données figées dans la facture.
 */
@Service
public class FacturXService {
    private final FacturePdf pdf;
    private final FactureXml xml;

    FacturXService(FacturePdf pdf, FactureXml xml) {
        this.pdf = pdf;
        this.xml = xml;
    }

    public byte[] generer(Facture facture) {
        try {
            byte[] pdfLisible = pdf.pdfLisible(facture);
            ByteArrayOutputStream sortie = new ByteArrayOutputStream();
            ZUGFeRDExporterFromA1 exporteur = new ZUGFeRDExporterFromA1()
                    .setProducer("ProPreFact")
                    .setCreator(facture.getVendeur().getRaisonSociale())
                    .setProfile(Profiles.getByName("EN16931"))
                    .load(pdfLisible);
            exporteur.setTransaction(xml.donneesStructurees(facture));
            exporteur.export(sortie);
            return sortie.toByteArray();
        } catch (IOException ex) {
            throw new UncheckedIOException("Génération Factur-X impossible pour la facture " + facture.getNumero(), ex);
        }
    }
}
