-- Chaque requête est filtrée par entreprise : ces colonnes doivent être indexées.
create index site_entreprise on site (entreprise_id);
create index ligne_facture_entreprise on ligne_facture (entreprise_id);
create index contrat_site on contrat (site_id);
create index facture_client on facture (client_id);
create index utilisateur_entreprise on utilisateur (entreprise_id);

-- Le statut ne peut prendre que les valeurs connues de l'application (énumération StatutFacture).
alter table facture add constraint facture_statut_connu
    check (statut in ('BROUILLON', 'VALIDEE', 'DEPOT_EN_COURS', 'DEPOSEE', 'PAYEE'));

-- Une adresse e-mail est unique sans tenir compte de la casse, comme la recherche à la connexion.
create unique index utilisateur_email_minuscule on utilisateur (lower(email));
