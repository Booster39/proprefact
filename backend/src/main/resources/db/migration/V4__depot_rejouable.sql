-- Dépôt sur la plateforme agréée en deux temps : l'intention est enregistrée, puis l'envoi est
-- tenté et rejoué jusqu'au succès, toujours avec la même clé pour ne jamais déposer en double.
alter table facture add column version bigint default 0 not null;
alter table facture add column cle_depot varchar(36);
alter table facture add column depot_tentatives int default 0 not null;
alter table facture add column depot_prochaine_tentative timestamp with time zone;
alter table facture add column depot_erreur varchar(500);
create index facture_depots_en_attente on facture (statut, depot_prochaine_tentative);
