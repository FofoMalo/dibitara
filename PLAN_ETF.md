# Suivi d’un ETF — 4.14.0

Implémentation : mode ETF sur un actif libre existant, capital investi distinct de la valeur, achats validés depuis les imports Trade Republic ou saisis manuellement, association des achats manuels, historique Valeur / Investi et plan hebdomadaire indicatif.

## Utilisation

1. Placements → menu ⋮ du CTO → **Suivre un ETF**.
2. Renseigner le nom de l’ETF, le capital total investi à une date de référence (jour inclus), le montant hebdomadaire, la prochaine date et le compte Trade Republic.
3. **Valider les achats** : confirmer l’ETF pour chaque import, ajouter uniquement les frais non déjà compris dans le montant, ou associer l’import à un achat manuel existant.
4. **Actualiser la valeur** : saisir la valeur totale des ETF constatée et sa date. Cette action ne change jamais le capital investi.

Le capital investi de départ inclut l’historique antérieur ; les anciens mouvements sont conservés mais ne sont pas ajoutés de nouveau. Un achat à la date de référence est donc déjà inclus dans le capital de départ. Aucune donnée personnelle n’est préremplie par déduction.

## Garanties

- Room 29 → 30 : deux tables ajoutées, clé étrangère vers l’actif avec cascade, lien unique vers une transaction bancaire (sans effacement à sa mise en corbeille).
- Écritures atomiques et contrôle des données relues au moment de confirmer. Confirmation explicite nécessaire en cas d’achat manuel similaire.
- Aucune création de dépense, aucun débit supplémentaire du compte source, aucune valorisation extrapolée depuis le calendrier.
- Sauvegarde complète format 4, anciens formats compatibles, validation des références et des doublons avant remplacement.

## Validation

- 657 tests JVM réussis, contrôle de couverture réussi et APK debug 4.14.0 / code 28 généré.
- Migration SQL vérifiée sur une ancienne copie Room28 (3129 transactions) et sur la sauvegarde récente du téléphone Room29 (3150 transactions) : anciennes données inchangées, schéma conforme aux 23 tables Room30, intégrité et clés étrangères correctes.
- Installation Fairphone vérifiée : 3150 transactions et toutes les tables préexistantes identiques ; réglages utilisateur conservés (seul l’horodatage du cache des taux a été actualisé). Sauvegardes avant/après et rapport dans `archive/installation-4.14.0-2026-09-16/`.
- Contrôles sans émulateur. Tests d’interface laissés à Florent ; aucun test instrumenté sur le téléphone personnel.

## Limites prévues

Un seul ETF par actif ; accumulation uniquement. Pas de ventes, dividendes distribués, cours automatiques ni rendement annualisé dans ce suivi. Les ventes des nouveaux imports CSV restent des revenus et ne sont pas proposées comme achats. Pour les anciens imports ambigus, l’utilisateur confirme explicitement qu’il s’agit d’un achat de l’ETF choisi.
