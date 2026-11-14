package com.proprefact.service;

import com.proprefact.domain.Client;
import com.proprefact.domain.CompteurFacture;
import com.proprefact.domain.Contrat;
import com.proprefact.domain.Entreprise;
import com.proprefact.domain.Facture;
import com.proprefact.domain.FactureDocument;
import com.proprefact.domain.IntrouvableException;
import com.proprefact.domain.LigneFacture;
import com.proprefact.domain.StatutFacture;
import com.proprefact.domain.TransitionInterditeException;
import com.proprefact.repo.ClientRepository;
import com.proprefact.repo.CompteurFactureRepository;
import com.proprefact.repo.ContratRepository;
import com.proprefact.repo.EntrepriseRepository;
import com.proprefact.repo.FactureDocumentRepository;
import com.proprefact.repo.FactureRepository;
import com.proprefact.repo.LigneFactureRepository;
import com.proprefact.service.commande.FactureCommande;
import com.proprefact.service.facturx.FacturXService;
import com.proprefact.tenant.TenantResolver;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class FacturationService {
    static final int DELAI_PAIEMENT_JOURS = 30;

    private final FactureRepository factures;
    private final LigneFactureRepository lignes;
    private final ContratRepository contrats;
    private final ClientRepository clients;
    private final CompteurFactureRepository compteurs;
    private final EntrepriseRepository entreprises;
    private final TenantResolver tenant;
    private final FacturXService facturX;
    private final FactureDocumentRepository documents;
    private final Clock horloge;

    FacturationService(
            FactureRepository factures,
            LigneFactureRepository lignes,
            ContratRepository contrats,
            ClientRepository clients,
            CompteurFactureRepository compteurs,
            EntrepriseRepository entreprises,
            TenantResolver tenant,
            FacturXService facturX,
            FactureDocumentRepository documents,
            Clock horloge) {
        this.factures = factures;
        this.lignes = lignes;
        this.contrats = contrats;
        this.clients = clients;
        this.compteurs = compteurs;
        this.entreprises = entreprises;
        this.tenant = tenant;
        this.facturX = facturX;
        this.documents = documents;
        this.horloge = horloge;
    }

    /**
     * Crée un brouillon de facture par client pour tous les contrats actifs pendant le mois et pas
     * encore facturés pour ce mois. Peut être relancée sans risque de doublon.
     * Le mois est facturé en entier, sans prorata pour les contrats commencés ou terminés en cours de mois.
     */
    public List<Facture> genererMensuelles(YearMonth mois) {
        String periode = mois.toString();
        Set<Long> dejaFactures = lignes.findContratsDejaFactures(periode);
        Map<Client, Facture> parClient = new LinkedHashMap<>();
        for (Contrat c : contrats.findAllAvecSiteEtClient()) {
            if (!c.actifPendant(mois) || dejaFactures.contains(c.getId())) {
                continue;
            }
            Facture f =
                    parClient.computeIfAbsent(c.getSite().getClient(), client -> Facture.brouillon(client, periode));
            LigneFacture l = new LigneFacture();
            l.setContrat(c);
            l.setPeriode(periode);
            l.setDesignation("%s – %s – %s %d"
                    .formatted(
                            c.getLibelle(),
                            c.getSite().getNom(),
                            mois.getMonth().getDisplayName(TextStyle.FULL, Locale.FRENCH),
                            mois.getYear()));
            l.setQuantite(BigDecimal.ONE);
            l.setPrixUnitaireHt(c.getMontantMensuelHt());
            l.setTauxTva(c.getTauxTva());
            f.ajouter(l);
        }
        return factures.saveAll(new ArrayList<>(parClient.values()));
    }

    /** Facture ponctuelle (remise en état, vitrerie, intervention exceptionnelle...). */
    public Facture creer(FactureCommande req) {
        Client client =
                clients.findById(req.clientId()).orElseThrow(() -> new IntrouvableException("Client introuvable"));
        Facture f = Facture.brouillon(client, null);
        req.lignes().forEach(r -> {
            LigneFacture l = new LigneFacture();
            l.setDesignation(r.designation());
            l.setQuantite(r.quantite());
            l.setPrixUnitaireHt(r.prixUnitaireHt());
            l.setTauxTva(r.tauxTva());
            f.ajouter(l);
        });
        return factures.save(f);
    }

    /** Toutes les factures, de la plus récente à la plus ancienne, avec leur client mais sans leurs lignes. */
    @Transactional(readOnly = true)
    public List<Facture> lister() {
        return factures.findAllAvecClient();
    }

    @Transactional(readOnly = true)
    public Facture charger(Long id) {
        return factures.findComplete(id).orElseThrow(() -> new IntrouvableException("Facture introuvable"));
    }

    /** Attribue le numéro définitif et fige la facture. */
    public Facture valider(Long id) {
        Facture f = charger(id);
        f.exiger(StatutFacture.BROUILLON); // avant de consommer un numéro
        LocalDate aujourdhui = LocalDate.now(horloge);
        CompteurFacture compteur = compteurs.findByAnnee(aujourdhui.getYear()).orElseGet(() -> {
            CompteurFacture c = new CompteurFacture();
            c.setAnnee(aujourdhui.getYear());
            return c;
        });
        compteur.setDernierNumero(compteur.getDernierNumero() + 1);
        compteurs.save(compteur);

        String numero = "F-%d-%05d".formatted(compteur.getAnnee(), compteur.getDernierNumero());
        f.valider(numero, aujourdhui, DELAI_PAIEMENT_JOURS, entrepriseCourante());
        document(f);
        return f;
    }

    public Facture marquerPayee(Long id) {
        Facture f = charger(id);
        f.marquerPayee();
        return f;
    }

    /** Seul un brouillon peut être supprimé : une facture numérotée s'annule par un avoir. */
    public void supprimerBrouillon(Long id) {
        Facture f = charger(id);
        f.exiger(StatutFacture.BROUILLON);
        factures.delete(f);
    }

    /** Le fichier émis à la validation, toujours le même octet pour octet. */
    public FichierFacture facturX(Long id) {
        Facture f = charger(id);
        if (f.getStatut() == StatutFacture.BROUILLON) {
            throw new TransitionInterditeException("La facture doit d'abord être validée");
        }
        return new FichierFacture(f.getNumero(), document(f));
    }

    /**
     * Renvoie le Factur-X conservé pour la facture. Il est produit une seule fois : à la validation,
     * ou au premier accès pour une facture validée avant que les fichiers ne soient conservés.
     */
    private byte[] document(Facture f) {
        return documents
                .findByFactureId(f.getId())
                .map(FactureDocument::getContenu)
                .orElseGet(() -> {
                    byte[] contenu = facturX.generer(f);
                    documents.save(new FactureDocument(f, contenu, Empreinte.sha256(contenu), horloge.instant()));
                    return contenu;
                });
    }

    private Entreprise entrepriseCourante() {
        return entreprises.findById(tenant.resolveCurrentTenantIdentifier()).orElseThrow();
    }
}
