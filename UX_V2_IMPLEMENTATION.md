# UX/UI V2 — intégration native

Version 4.11.0 (24), 11 septembre 2026. Schéma Room inchangé : 27.

## Intégré

- Navigation principale : Accueil, Budget, Activité, Patrimoine, Plus ; libellés visibles et section Patrimoine sélectionnée dans Épargne, Investissements et Dettes.
- Plus : accès aux comptes, épargne/enfants, placements, dettes, rapport, tendances, projections, scénarios et paramètres/sauvegardes.
- Thème partagé : anthracite, surfaces neutres, or doux, formes et hiérarchie typographique communes. Le mode clair est conservé.
- Accueil : budget restant en premier, projection à 30 jours identifiée comme estimation ; cartes personnalisables conservées.
- Budget et Activité : titres explicites. Patrimoine : synthèse nette en premier, accès aux actifs et dettes conservés.
- Épargne : sections Comptes, Enfants, Objectifs ; total familial et part des enfants ; actions sur les comptes et objectifs liés accessibles dans la section de chaque enfant.

## Périmètre de cette étape

Le socle et les parcours principaux sont intégrés. Les formulaires métier existants restent utilisés et bénéficient du thème commun ; ils ne reproduisent pas encore chaque détail de la maquette HTML. Les objectifs d'un enfant sont identifiés via leur compte source, sans créer de nouvelle relation en base. Le budget restant et la projection restent deux valeurs distinctes ; aucun nouvel indicateur financier n'est déduit dans la présentation.

## Contrôles

Compilation debug et suite JVM sur ordinateur. Aucun test instrumenté sur téléphone personnel. Une vérification visuelle sur appareil reste nécessaire : petite largeur et texte agrandi, navigation retour, filtres et formulaires existants, gestion des comptes et objectifs des enfants.

L'archive locale avant V2 est dans `archive/avant-ux-v2-2026-09-10`, exclue de Git. Elle contient les sources 4.10.0, l'APK installé 4.9.0, l'APK 4.10.0, les données privées et leur contrôle d'intégrité. La mise à jour 4.9.0 vers 4.10.0 a conservé les fichiers métier et préférences à l'identique.
