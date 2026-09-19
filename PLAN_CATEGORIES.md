# Gestion des catégories — 14 septembre 2026

Proposition acceptée : gestion dédiée, sélecteur commun avec recherche/récents/suggestion, deux niveaux facultatifs, personnalisation, archive, fusion/déplacement avec aperçu, classement multiple précis, règles proposées explicitement.

## Critères
- Identifiants existants inchangés ; migration additive Room 28 → 29, sauvegarde couvrant le catalogue.
- Catégories personnelles réellement distinctes dans les regroupements et enveloppes.
- Renommer/archiver conserve l’historique. Fusion/déplacement atomiques : opérations, règles, enveloppes et corbeille ; conflits refusés avant écriture.
- Même sélecteur en saisie, modification, filtres et sélection multiple.
- Contrôles JVM et compilation ; pas d’émulateur. Florent valide l’interface sur téléphone.

## Validation terminée
Fonctions implémentées : catalogue, sélecteur, règles explicites, fusions/déplacements atomiques, sauvegarde format3 et marque de classement volontaire. Vérifications finales terminées.

Validation acquise : compilation ; 628 tests JVM, dont les 17 tests du catalogue et de son repository correctement exécutés sous JUnit 5 ; seuil de couverture validé. Migration sur copie locale Room28 avec 3129 transactions et toutes anciennes colonnes identiques. La version 4.14.0 conserve ce catalogue et ajoute le suivi ETF.

Limite volontaire : les catégories personnelles participent aux dépenses, rapports et enveloppes, mais ne sont pas attribuées automatiquement à un groupe « besoin/envie » dans les recommandations 50/30/20. Aucun déplacement des classements existants à la migration.
Archive préalable : archive/avant-categories-2026-09-14/sources.tar.gz.
