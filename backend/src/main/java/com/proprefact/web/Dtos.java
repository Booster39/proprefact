package com.proprefact.web;

import com.proprefact.domain.Client;
import com.proprefact.domain.Contrat;
import com.proprefact.domain.Facture;
import com.proprefact.domain.Site;
import com.proprefact.domain.StatutFacture;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Objets échangés avec le front-end. */
public final class Dtos {
    private Dtos() {}

    static final String SIREN = "\\d{9}";

    // Messages affichés tels quels à l'utilisateur, à côté du nom du champ.
    static final String OBLIGATOIRE = "obligatoire";
    static final String SIREN_INVALIDE = "9 chiffres attendus";
    static final String EMAIL_INVALIDE = "adresse e-mail invalide";
    static final String MAX_10 = "10 caractères au plus";
    static final String MAX_11 = "11 caractères au plus";
    static final String MAX_20 = "20 caractères au plus";
    static final String MAX_34 = "34 caractères au plus";
    static final String MAX_100 = "100 caractères au plus";
    static final String MAX_200 = "200 caractères au plus";
    static final String MAX_300 = "300 caractères au plus";
    static final String MAX_500 = "500 caractères au plus";
    static final String DEUX_DECIMALES = "2 décimales au plus";
    static final String TROIS_DECIMALES = "3 décimales au plus";
    static final String POSITIF = "doit être supérieur à zéro";
    static final String NON_NEGATIF = "ne peut pas être négatif";

    // Les tailles maximales sont celles des colonnes de la base (migration V1).

    public record InscriptionReq(
            @NotBlank(message = OBLIGATOIRE) @Email(message = EMAIL_INVALIDE) @Size(max = 200, message = MAX_200)
            String email,

            @NotBlank(message = OBLIGATOIRE) @Size(min = 10, max = 72, message = "entre 10 et 72 caractères")
            String motDePasse,

            @NotBlank(message = OBLIGATOIRE) @Size(max = 200, message = MAX_200)
            String raisonSociale,

            @NotBlank(message = OBLIGATOIRE) @Pattern(regexp = SIREN, message = SIREN_INVALIDE)
            String siren,

            @Size(max = 20, message = MAX_20) String tvaIntra,

            @NotBlank(message = OBLIGATOIRE) @Size(max = 200, message = MAX_200)
            String adresse,

            @NotBlank(message = OBLIGATOIRE) @Size(max = 10, message = MAX_10)
            String codePostal,

            @NotBlank(message = OBLIGATOIRE) @Size(max = 100, message = MAX_100)
            String ville,

            @Size(max = 34, message = MAX_34) String iban,
            @Size(max = 11, message = MAX_11) String bic,
            Boolean optionTvaDebits) {}

    public record ConnexionReq(
            @NotBlank(message = OBLIGATOIRE) @Size(max = 200, message = MAX_200)
            String email,

            @NotBlank(message = OBLIGATOIRE) String motDePasse) {}

    public record ConfirmationReq(
            @NotBlank(message = OBLIGATOIRE) String jeton) {}

    public record EmailReq(
            @NotBlank(message = OBLIGATOIRE) @Email(message = EMAIL_INVALIDE) @Size(max = 200, message = MAX_200)
            String email) {}

    public record JetonDto(String jeton, String raisonSociale) {}

    public record ClientReq(
            @NotBlank(message = OBLIGATOIRE) @Size(max = 200, message = MAX_200)
            String raisonSociale,

            @NotBlank(message = OBLIGATOIRE) @Pattern(regexp = SIREN, message = SIREN_INVALIDE)
            String siren,

            @Size(max = 20, message = MAX_20) String tvaIntra,

            @NotBlank(message = OBLIGATOIRE) @Size(max = 200, message = MAX_200)
            String adresse,

            @NotBlank(message = OBLIGATOIRE) @Size(max = 10, message = MAX_10)
            String codePostal,

            @NotBlank(message = OBLIGATOIRE) @Size(max = 100, message = MAX_100)
            String ville,

            @Email(message = EMAIL_INVALIDE) @Size(max = 200, message = MAX_200)
            String email) {}

    public record ClientDto(
            Long id,
            String raisonSociale,
            String siren,
            String tvaIntra,
            String adresse,
            String codePostal,
            String ville,
            String email) {
        static ClientDto de(Client c) {
            return new ClientDto(
                    c.getId(),
                    c.getRaisonSociale(),
                    c.getSiren(),
                    c.getTvaIntra(),
                    c.getAdresse(),
                    c.getCodePostal(),
                    c.getVille(),
                    c.getEmail());
        }
    }

    public record SiteReq(
            @NotBlank(message = OBLIGATOIRE) @Size(max = 200, message = MAX_200)
            String nom,

            @NotBlank(message = OBLIGATOIRE) @Size(max = 200, message = MAX_200)
            String adresse,

            @NotBlank(message = OBLIGATOIRE) @Size(max = 10, message = MAX_10)
            String codePostal,

            @NotBlank(message = OBLIGATOIRE) @Size(max = 100, message = MAX_100)
            String ville) {}

    public record SiteDto(Long id, Long clientId, String nom, String adresse, String codePostal, String ville) {
        static SiteDto de(Site s) {
            return new SiteDto(
                    s.getId(), s.getClient().getId(), s.getNom(), s.getAdresse(), s.getCodePostal(), s.getVille());
        }
    }

    public record ContratReq(
            @NotNull(message = OBLIGATOIRE) Long siteId,

            @NotBlank(message = OBLIGATOIRE) @Size(max = 300, message = MAX_300)
            String libelle,

            @NotNull(message = OBLIGATOIRE)
            @DecimalMin(value = "0.01", message = POSITIF)
            @Digits(integer = 10, fraction = 2, message = DEUX_DECIMALES)
            BigDecimal montantMensuelHt,

            @NotNull(message = OBLIGATOIRE)
            @DecimalMin(value = "0", message = NON_NEGATIF)
            @DecimalMax(value = "100", message = "100 au plus")
            @Digits(integer = 3, fraction = 2, message = DEUX_DECIMALES)
            BigDecimal tauxTva,

            @NotNull(message = OBLIGATOIRE) LocalDate dateDebut,
            LocalDate dateFin) {}

    public record ContratDto(
            Long id,
            Long siteId,
            String site,
            String client,
            String libelle,
            BigDecimal montantMensuelHt,
            BigDecimal tauxTva,
            LocalDate dateDebut,
            LocalDate dateFin) {
        static ContratDto de(Contrat c) {
            return new ContratDto(
                    c.getId(),
                    c.getSite().getId(),
                    c.getSite().getNom(),
                    c.getSite().getClient().getRaisonSociale(),
                    c.getLibelle(),
                    c.getMontantMensuelHt(),
                    c.getTauxTva(),
                    c.getDateDebut(),
                    c.getDateFin());
        }
    }

    public record LigneReq(
            @NotBlank(message = OBLIGATOIRE) @Size(max = 500, message = MAX_500)
            String designation,

            @NotNull(message = OBLIGATOIRE)
            @DecimalMin(value = "0.001", message = POSITIF)
            @Digits(integer = 9, fraction = 3, message = TROIS_DECIMALES)
            BigDecimal quantite,

            // La base garde 2 décimales : en accepter plus ferait changer le total à la validation.
            @NotNull(message = OBLIGATOIRE)
            @DecimalMin(value = "0", message = NON_NEGATIF)
            @Digits(integer = 10, fraction = 2, message = DEUX_DECIMALES)
            BigDecimal prixUnitaireHt,

            @NotNull(message = OBLIGATOIRE)
            @DecimalMin(value = "0", message = NON_NEGATIF)
            @DecimalMax(value = "100", message = "100 au plus")
            @Digits(integer = 3, fraction = 2, message = DEUX_DECIMALES)
            BigDecimal tauxTva) {}

    public record FactureReq(
            @NotNull(message = OBLIGATOIRE) Long clientId,

            @NotEmpty(message = "au moins une ligne") @Valid List<LigneReq> lignes) {}

    public record LigneDto(
            String designation,
            BigDecimal quantite,
            BigDecimal prixUnitaireHt,
            BigDecimal tauxTva,
            BigDecimal montantHt) {}

    public record FactureDto(
            Long id,
            String numero,
            StatutFacture statut,
            Long clientId,
            String client,
            LocalDate dateEmission,
            LocalDate dateEcheance,
            String periode,
            BigDecimal totalHt,
            BigDecimal totalTva,
            BigDecimal totalTtc,
            String idDepot,
            String depotErreur,
            List<LigneDto> lignes) {

        /** Sans les lignes, pour les listes. */
        static FactureDto resume(Facture f) {
            return de(f, null);
        }

        static FactureDto complete(Facture f) {
            return de(
                    f,
                    f.getLignes().stream()
                            .map(l -> new LigneDto(
                                    l.getDesignation(),
                                    l.getQuantite(),
                                    l.getPrixUnitaireHt(),
                                    l.getTauxTva(),
                                    l.montantHt()))
                            .toList());
        }

        private static FactureDto de(Facture f, List<LigneDto> lignes) {
            return new FactureDto(
                    f.getId(),
                    f.getNumero(),
                    f.getStatut(),
                    f.getClient().getId(),
                    f.getAcheteur() != null
                            ? f.getAcheteur().getRaisonSociale()
                            : f.getClient().getRaisonSociale(),
                    f.getDateEmission(),
                    f.getDateEcheance(),
                    f.getPeriode(),
                    f.getTotalHt(),
                    f.getTotalTva(),
                    f.getTotalTtc(),
                    f.getIdDepot(),
                    f.getDepotErreur(),
                    lignes);
        }
    }
}
