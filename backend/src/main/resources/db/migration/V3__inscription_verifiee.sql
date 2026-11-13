-- Un compte n'est utilisable qu'après confirmation de l'adresse e-mail.
alter table utilisateur add column email_verifie boolean default false not null;
update utilisateur set email_verifie = true; -- comptes créés avant la vérification
alter table utilisateur add column jeton_confirmation_hash varchar(64);
alter table utilisateur add column jeton_expire_le timestamp with time zone;
create index utilisateur_jeton on utilisateur (jeton_confirmation_hash);

-- Le SIREN n'est plus unique : s'inscrire avec le SIREN d'un tiers ne doit pas pouvoir
-- empêcher son vrai titulaire d'ouvrir un compte. La colonne est recréée pour retirer la contrainte.
alter table entreprise rename column siren to siren_ancien;
alter table entreprise add column siren varchar(9);
update entreprise set siren = siren_ancien;
alter table entreprise alter column siren set not null;
alter table entreprise drop column siren_ancien;
