package com.proprefact.web;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import com.proprefact.domain.Client;
import com.proprefact.domain.Contrat;
import com.proprefact.domain.Facture;
import com.proprefact.domain.Site;
import com.proprefact.domain.StatutFacture;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Objets échangés avec le front-end. */
public final class Dtos {
    private Dtos() {}

    static final String SIREN = "\\d{9}";

    public record InscriptionReq(
            @NotBlank @Email String email,
            @NotBlank @Size(min = 10, max = 72) String motDePasse,
            @NotBlank String raisonSociale,
            @NotBlank @Pattern(regexp = SIREN, message = "9 chiffres attendus") String siren,
            String tvaIntra,
            @NotBlank String adresse,
            @NotBlank String codePostal,
            @NotBlank String ville,
            String iban,
            String bic,
            Boolean optionTvaDebits) {}

    public record ConnexionReq(@NotBlank String email, @NotBlank String motDePasse) {}

    public record ConfirmationReq(@NotBlank String jeton) {}

    public record EmailReq(@NotBlank @Email String email) {}

    public record JetonDto(String jeton, String raisonSociale) {}

    public record ClientReq(
            @NotBlank String raisonSociale,
            @NotBlank @Pattern(regexp = SIREN, message = "9 chiffres attendus") String siren,
            String tvaIntra,
            @NotBlank String adresse,
            @NotBlank String codePostal,
            @NotBlank String ville,
            @Email String email) {}

    public record ClientDto(Long id, String raisonSociale, String siren, String tvaIntra, String adresse,
            String codePostal, String ville, String email) {
        static ClientDto de(Client c) {
            return new ClientDto(c.getId(), c.getRaisonSociale(), c.getSiren(), c.getTvaIntra(), c.getAdresse(), c.getCodePostal(), c.getVille(),
                    c.getEmail());
        }
    }

    public record SiteReq(@NotBlank String nom, @NotBlank String adresse, @NotBlank String codePostal,
            @NotBlank String ville) {}

    public record SiteDto(Long id, Long clientId, String nom, String adresse, String codePostal, String ville) {
        static SiteDto de(Site s) {
            return new SiteDto(s.getId(), s.getClient().getId(), s.getNom(), s.getAdresse(), s.getCodePostal(), s.getVille());
        }
    }

    public record ContratReq(
            @NotNull Long siteId,
            @NotBlank String libelle,
            @NotNull @DecimalMin("0.01") BigDecimal montantMensuelHt,
            @NotNull @DecimalMin("0") BigDecimal tauxTva,
            @NotNull LocalDate dateDebut,
            LocalDate dateFin) {}

    public record ContratDto(Long id, Long siteId, String site, String client, String libelle,
            BigDecimal montantMensuelHt, BigDecimal tauxTva, LocalDate dateDebut, LocalDate dateFin) {
        static ContratDto de(Contrat c) {
            return new ContratDto(c.getId(), c.getSite().getId(), c.getSite().getNom(), c.getSite().getClient().getRaisonSociale(), c.getLibelle(),
                    c.getMontantMensuelHt(), c.getTauxTva(), c.getDateDebut(), c.getDateFin());
        }
    }

    public record LigneReq(
            @NotBlank String designation,
            @NotNull @DecimalMin("0.001") BigDecimal quantite,
            @NotNull @DecimalMin("0") BigDecimal prixUnitaireHt,
            @NotNull @DecimalMin("0") BigDecimal tauxTva) {}

    public record FactureReq(@NotNull Long clientId, @NotEmpty @Valid List<LigneReq> lignes) {}

    public record LigneDto(String designation, BigDecimal quantite, BigDecimal prixUnitaireHt, BigDecimal tauxTva,
            BigDecimal montantHt) {}

    public record FactureDto(Long id, String numero, StatutFacture statut, Long clientId, String client,
            LocalDate dateEmission, LocalDate dateEcheance, String periode, BigDecimal totalHt, BigDecimal totalTva,
            BigDecimal totalTtc, String idDepot, String depotErreur, List<LigneDto> lignes) {

        /** Sans les lignes, pour les listes. */
        static FactureDto resume(Facture f) {
            return de(f, null);
        }

        static FactureDto complete(Facture f) {
            return de(f, f.getLignes().stream()
                    .map(l -> new LigneDto(l.getDesignation(), l.getQuantite(), l.getPrixUnitaireHt(), l.getTauxTva(), l.montantHt()))
                    .toList());
        }

        private static FactureDto de(Facture f, List<LigneDto> lignes) {
            return new FactureDto(f.getId(), f.getNumero(), f.getStatut(), f.getClient().getId(),
                    f.getAcheteur() != null ? f.getAcheteur().getRaisonSociale() : f.getClient().getRaisonSociale(),
                    f.getDateEmission(), f.getDateEcheance(), f.getPeriode(), f.getTotalHt(), f.getTotalTva(), f.getTotalTtc(), f.getIdDepot(),
                    f.getDepotErreur(), lignes);
        }
    }
}
