# Plan de simplification et de fiabilité — 13 septembre 2026

## Objectif
Rendre les montants compréhensibles, protéger les données et raccourcir les parcours quotidiens. Conserver les fonctions spécialisées et les changements déjà présents. Archive des sources initiales : `archive/avant-simplification-2026-09-13/sources.tar.gz`.

## Lots et critères de validation
1. **Chiffres explicables** : préciser budget restant, bilan mensuel, projection et périmètre du patrimoine ; ne pas additionner des comptes potentiellement déjà présents en épargne. Vérifier les calculs et les exclusions de virements internes.
2. **Sauvegardes et restauration** : inclure les historiques et les préférences fonctionnelles ; conserver les anciens exports lisibles ; aperçu du contenu, validation avant écriture, copie de secours avant remplacement. Les secrets de connexion et autorisations Android restent propres à l’appareil. Tests de format et de restauration.
3. **Transactions** : lignes compactes, libellé principal, accès direct, provenance ; sélection multiple pour catégoriser ; corbeille persistante avec restauration sans écraser une opération existante. Migration non destructive et tests.
4. **Rapprochement** : parcours par compte et période, soldes d’ouverture/clôture saisis, solde calculé et écart ; accès aux opérations pour corriger. Calculs testés, aucune correction automatique de données.
5. **Navigation et paramètres** : pages courtes avec retour ; imports/capture accessibles depuis Activité ; analyses regroupées entre passé et avenir ; réglage logement accessible depuis son scénario.
6. **Accueil et présentation** : échéances et actions avant les analyses ; détails secondaires repliables ; cartes réservées aux synthèses ; origine/date disponibles explicitement et absence de date signalée ; dispositions adaptatives pour texte agrandi.
7. **Livraison** : compilation, tests JVM, migration/restauration sur environnement de test si disponible, revue du diff, version et changelog ; aucune restauration de test sur les données personnelles.

## Suivi
- Lots 1 à 6 implémentés ; version préparée : 4.12.0 (26), Room 28.
- Validation acquise : 611 tests JVM ; 9 tests instrumentés ciblés de migration, corbeille et restauration sur émulateur ; contrôle de couverture debug.
- APK final compilé et installé sur le Fairphone à la demande de Florent. Analyse statique passée sans erreur (84 avertissements) avant le dernier ajustement du bouton budget ; nouvelle analyse interrompue à sa demande. Tests d’interface arrêtés : transaction longue validée, captures devise/bilan revues, bouton budget corrigé ; vérifications manuelles confiées à Florent sur téléphone.
- Aucun test automatisé ni restauration exécuté sur le téléphone. Mise à jour non destructive 4.12.0 : sauvegarde avant/après dans archive/installation-4.12.0-2026-09-13 ; intégrité SQLite ok, 3129 transactions et contenu des 19 tables préexistantes strictement identiques, préférences identiques ; Room 27 → 28 ajoute seulement la corbeille vide. Démarrage réussi.

## Choix de livraison
- Patrimoine : périmètre explicitement affiché, sans addition automatique des comptes bancaires ; les doubles comptes entre banque et épargne nécessiteraient une relation explicite avant un futur changement de formule.
- Corbeille : conservation persistante, sans purge automatique ; les conflits de restauration sont refusés et expliqués. La suppression globale des données efface aussi les copies de secours privées.
- Rapprochement : contrôle mensuel sans écriture, avec opérations du compte accessibles et rattachement du compte modifiable dans la saisie. Les devises étrangères et les investissements à vérifier sont signalés.
- Sauvegarde : les 20 tables sont couvertes, ainsi que les préférences fonctionnelles. Les secrets, droits Android et autorisations de dossier ne sont pas transférés. La destination de sauvegarde externe reste configurée sur l’appareil.
- Accueil : priorité aux alertes et échéances, analyses et patrimoine détaillé accessibles à la demande ; réorganisation conservée.
- Imports et capture : accessibles depuis Activité ; sécurité, devises, apparence, notifications et sauvegardes ont leur page ; seuil logement accessible depuis le scénario.

