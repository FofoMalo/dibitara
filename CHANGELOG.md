# Changelog — Dibitara

Format : [Keep a Changelog](https://keepachangelog.com/fr/1.0.0/)
Versioning : [Semantic Versioning](https://semver.org/lang/fr/)

Reconstruit le 2026-08-27 à partir de l'historique git (le suivi manuel avait
décroché après Sprint 2). Voir la convention « bump de version » dans
`CLAUDE.md` pour éviter que ça se reproduise.

---

## [4.6.0] - 2026-08-28 — Sprint 44

> Fusion de `develop-catchup` dans `develop`. La branche `develop-catchup` était
> repassée à `4.4.0 / versionCode 15` par accident lors du merge `a3e1fb5`
> (le `build.gradle.kts` périmé de `florent/prive` a écrasé celui de `develop`) ;
> la lignée publiée de `develop` étant `v4.5.0 / versionCode 16`, cette version
> reprend la numérotation : **4.6.0 / versionCode 17**.

### Retrait de l'import bancaire personnel
- Suppression de toute la couche d'import BRED (CSV, PDF, capture live par
  notification) et TradeRepublic — calibrée sur les relevés réels de Florent,
  inutilisable par un autre utilisateur. Dépendance `pdfbox` retirée.
- Conservé car générique : comptes bancaires (`bank_accounts`, Room v22),
  détection des virements internes.

### Import CSV bancaire générique (Sprint 44)
- Nouvel écran *Paramètres → Importer un relevé CSV* : sélection du fichier,
  détection automatique des colonnes (délimiteur, format de date, montant signé
  ou débit/crédit, séparateur décimal, synonymes d'en-tête FR/EN), écran de
  mapping manuel en secours, aperçu éditable (catégorie par ligne, inclusion,
  compte de rattachement) avant écriture.
- Déduplication par empreinte SHA-256 (date + montant + libellé). Catégorie
  **et sous-catégorie** suggérées via les règles apprises puis les dictionnaires
  génériques (cascade partagée avec la recatégorisation du Dashboard) : une
  sous-catégorie personnalisée enseignée sur un libellé est ré-appliquée aux
  imports suivants.
- La colonne « Référence » n'est plus fusionnée dans le libellé (identifiant
  technique sur les relevés BCEAO). ⚠️ Comme le libellé change, l'empreinte de
  déduplication change aussi : un ré-import après cette mise à jour peut recréer
  les transactions CSV importées avant.
- Aucune migration de schéma Room ajoutée (v23 inchangée).

### Plateforme et publication

- Cible l'API 36 (Android 16), requis par le Play Store pour toute mise à jour
  à partir du 31 août 2026 (`compileSdk`/`targetSdk` 35 → 36).
- **`MIGRATION_5_6` corrigée** : la table `monthly_versements` était créée avec
  une contrainte `UNIQUE(...)` en ligne au lieu de l'index unique séparé attendu
  par l'entité. Sans effet visible (même règle d'unicité) mais faisait échouer la
  validation de schéma pour une base créée en v2-v5. Chaîne de migration v6→v23
  (plus ancienne version publiée = v3.0.0) validée bout-en-bout sur appareil.
- Le module `androidTest` recompile de nouveau (noms de tests avec espaces qui
  cassaient le dexing pour `minSdk 26` ; schémas Room empaquetés en assets de
  test).

---

## [4.4.0] - 2026-08-27

> Le `versionCode` n'avait pas été incrémenté depuis Sprint 19 (4.3.0, 2026-05-22)
> malgré ~20 sprints et ~107 commits livrés entre-temps. Cette entrée couvre donc
> tout ce qui a été construit sur cette période, regroupé par thème plutôt que
> par sprint au-delà de Sprint 42 (les commits suivants ne sont plus taggés par
> numéro de sprint).

### Sprint 20-22 — Import bancaire et trésorerie
- Import CSV TradeRepublic (branche perso/florent)
- Projection de trésorerie à 30 jours + recatégorisation des transactions `AUTRE` (32 tests)
- Cartes du dashboard réordonnables (Reorderable) + notifications mensuelles via WorkManager
- Filtrage des transactions par période poussé en SQL, navigation mensuelle

### Sprint 23-25 — Multi-devises et patrimoine
- Conversion multi-devises du patrimoine et du rapport mensuel
- Affichage multi-devises dans l'UI : épargne, dettes, investissements
- Graphique de répartition du patrimoine dans le détail Patrimoine

### Sprint 26-29 — Réconciliation, doublons, historique
- Réconciliation dettes/immobilier — `debtId` sur les biens, Room v11 → v12
- Détection et nettoyage des doublons de transactions
- Historique mensuel du patrimoine net — table `patrimoine_snapshots`, Room v12 → v13, sparkline
- Nouveaux assets de launcher (icône v2)

### Sprint 30-34 — Import BRED et recatégorisation
- Import CSV BRED (`BredCsvParser`, écran dédié, calibré sur le format réel du relevé, 15 tests)
- Plafond légal d'épargne — Room v13 → v14 — et nettoyage multi-sélection des doublons
- Import du relevé PDF BRED via `pdfbox-android`
- Recatégorisation en masse par note + contexte mois sur Budget → Dépenses
- Recatégorisation affinée : correspondance mot-entier, sous-catégories d'`AUTRE`, Mobile Money Afrique, catégories `EDUCATION` et `TRANSFERTS_FAMILIAUX`, taux XOF fixe, CFA sans décimales

### Projection de trésorerie et Budget
- Projection 30j recalculée depuis les transactions réelles (revenus récurrents inclus), ajout SCPI et crédits, jour mensuel et « aujourd'hui » figés pour les tests
- Écran détail de la projection 30j
- `Dettes & crédits` : hint quand le capital d'origine n'est pas renseigné — `paymentDay`/`originalAmountCents`, Room v14 → v15
- Budget : fusion bilan/objectif, ligne de revenus compacte

### Sprint 35-39 — Dettes, tests, apprentissage de catégorisation
- Taux d'intérêt et simulation de remboursement anticipé sur les dettes — Room v15 → v16
- Tests d'intégration Room (Transaction, Debt, migration v14→v15)
- Neutralisation du rouge sur Dettes/Dashboard, bouton de confirmation de mensualité
- Apprentissage des catégorisations manuelles — table `categorization_rules`, Room v16 → v17, correctifs sur « À catégoriser »

### Sprint 40-42 — Enveloppes, recommandations, restauration
- Enveloppes budgétaires par catégorie — table `category_envelopes`, Room v17 → v18 (+ 4 bugs post-test corrigés sur téléphone réel)
- Recommandations budgétaires mensuelles (règle 50/30/20)
- Suppression des données personnelles (conformité RGPD Art. 17)
- Restauration des données depuis une sauvegarde JSON (inverse de l'export Sprint 18)

### Suivi véhicule locatif, nettoyage, corrections diverses
- Suivi de l'activité de location de véhicule — table `vehicle_rental_entries`, Room v18 → v19
- Retrait de la fonctionnalité métaux précieux — Room v19 → v20
- Notifications cliquables via deep links, navigation vers une transaction depuis le dashboard
- Nettoyage du projet, mise à jour Hilt/Compose BOM, dette de couverture Kover comblée (73,57 % → 89,88 %)
- Correction de l'équité nette pour un bien lié à un crédit dans une autre devise

### Refonte UX/UI (2026-08)
- Fondations : palette noir/or, `HeroCard`, couleurs/icônes stables par catégorie (`CategoryVisuals`), graphique en barres horizontales custom
- Refonte du Dashboard, du Budget, des Placements et de l'Épargne (accent doré en filet, tendances stables)
- Regroupement des transactions par jour avec icône de catégorie ; menu unique « ⋮ » pour Modifier/Supprimer
- Corrections de troncature (dates, libellés de mois, arrondi IEEE754 des montants) sur les graphiques et l'import CSV
- Masquage des montants étendu aux graphiques du Dashboard/Tendances et à l'aperçu d'import
- Réglage d'apparence système / clair / sombre
- Snapshot mensuel du patrimoine garanti via WorkManager (indépendant de la visite de l'écran)
- Historique de valorisation par actif — Room v20 → v21 — base des badges de tendance, étendus à l'épargne et aux placements personnalisés
- Bloc « Acquisition → Aujourd'hui » sur les 4 cartes d'investissement — Room v22 → v23

### Comptes bancaires et capture live BRED
- Entité `Compte bancaire` (BRED, TradeRepublic), détection des virements internes — Room v21 → v22
- Capture live des retraits et paiements carte BRED via notification push (avec tolérance apostrophe/espace)
- Correction du parseur CSV BRED pour le nouveau format export à 13 colonnes
- Ajout de Qonto comme provider de compte bancaire (solde manuel uniquement, pas d'analyse pro)

### Module Scénarios
- Scénario logement — simulateur de plancher sans double-compte mensualité/besoins, projection 6 mois
- Conseiller patrimoine — 4 axes (précaution, plafonds, capacité, concentration) ; abondement PEE/PERCO inclus dans les versements programmés ; compte pro Qonto exclu des liquidités sûres

### Trésorerie basée sur le solde réel
- Projection de trésorerie et alerte de liquidités insuffisantes basées sur le solde bancaire réel plutôt que le flux du mois
- Retrait du plafond fixe au 28 du mois dans la génération des occurrences récurrentes et les prochaines échéances
- Notifications budget/dettes/liquidités limitées à une alerte par jour
- Séparation du seuil de reste à vivre (Scénario logement) du seuil de solde (alerte liquidités)

### Corrigé
- Instanciation paresseuse d'`AppViewModel` au démarrage (piège `by viewModels()` — logique de démarrage jamais exécutée sur device réel)
- Génération anticipée des occurrences mensuelles récurrentes
- Chevauchement du FAB avec le contenu ; généralisation de `HeroCard` aux cartes de synthèse
- Filtre « À catégoriser » des Transactions : affichait toutes les dépenses `AUTRE` (34 ce mois) au lieu des transactions réellement non catégorisées (5) — incluait celles déjà classées en sous-catégorie. Même défaut corrigé dans les suggestions de recatégorisation du Dashboard (`GetRecategorizationSuggestionsUseCase`)

## [4.5.0] - 2026-05-25 — Sprint 29

> Tag `v4.5.0` (branche `release/v4.5.0`). Retrait de l'import TradeRepublic sur
> `develop`, portage des Sprints 23-28. Le détail thématique de tout ce qui a été
> construit entre Sprint 19 et Sprint 43 est regroupé dans l'entrée `[4.4.0]`
> ci-dessus (CHANGELOG reconstruit depuis git sur la lignée `florent/prive`, où
> la numérotation s'était arrêtée à 4.4.0).

## [4.3.0] - 2026-05-22 — Sprint 19

### Ajouté
- Investissements personnalisés : métaux précieux, actifs libres, épargne salariale
- Room v9 → v10 : tables `precious_metals`, `custom_assets`, `employee_savings`

## [4.2.0] - 2026-05-22 — Sprint 18

### Ajouté
- Export des données en CSV et JSON, partage via Intent Android
- Section dédiée dans les Paramètres

## [4.1.0] - 2026-05-21 — Sprint 17

### Ajouté
- SCPI : parts fractionnées (ex. 2,2 parts) — `sharesCount` Int → Real, migration Room v8 → v9 (recrée la table en préservant les données)

## [4.0.0] - 2026-05-20 — Sprints 15-16

### Ajouté
- FEAT-SUGGEST : suggestions de saisie rapide de transactions
- FEAT-RECUR : récurrences enrichies (hebdomadaire/mensuelle/annuelle), vue « prochains paiements », Room v7 → v8
- Réglage Paramètres pour l'affichage des prochains paiements

### Corrigé
- Tests `UpdateAfficherProchainsPaiementsUseCase`, couverture Kover ≥ 80 %

## [3.1.0] - 2026-05-19 — Sprints 13-14

### Ajouté
- Kover (couverture de code), Crashlytics, taux de change Frankfurter
- UX-02 : gestion du clavier IME
- Donut budget interactif avec drill-down catégorie `AUTRE` et navigation revenus/dépenses
- ANALYSE-01 : étude `RecurringExpenseTracker` — NO-GO (licence GPL-3.0 incompatible + KMP)

### Corrigé
- BUG-AUTH : preuve d'installation via `noBackupFilesDir`

## [3.0.1] - 2026-05-17

### Corrigé
- Suppression du champ email/mot de passe de l'UI d'authentification (PIN + biométrie uniquement)
- Pré-remplissage et validation du formulaire de dépenses
- Parsing des montants en locale française (virgule → point)
- Fallback PIN de la biométrie sur émulateur

### Modifié
- Restructuration des catégories, éclatement d'`AUTRE` dans le rapport mensuel — Room v6 → v7
- Devise par défaut propagée dans toute l'app ; renommage de l'écran en « Transactions »

### Ajouté
- Intégration de `gradle-play-publisher` pour la publication Play Store

## [3.0.0] - 2026-05-15 — Sprints 10-12, déploiement Play Store

### Ajouté
- Politique de confidentialité (Play Store)
- Sprints 10 et 11
- Sprint 12 — améliorations pré-déploiement

### Modifié
- Signing config du build release conditionnelle (ne bloque plus le CI)

## [2.5.0] - 2026-05-10

### Ajouté
- Build release signé via `keystore.properties`, préparation de la soumission Play Store

## [2.4.0] - 2026-05-10

### Ajouté
- Palette et philosophie visuelle Dibitara

## [2.3.0] - 2026-05-10

### Ajouté
- Intégration du logo Dibitara (lanterne or/noir)

## [2.2.0] - 2026-05-10

### Ajouté
- FEAT-01B : sous-catégories personnalisées (CRUD), migration Room v4 → v5

## [2.1.0] - 2026-05-10

### Ajouté
- AUTH-03 : couche crypto et domaine TOTP complète (RFC 6238)

### Modifié
- Préparation du build release (ProGuard, permissions, tests)

## [2.0.0] - 2026-05-10 — Sprints 1-9, fondations

### Ajouté
- Sprint 3 : graphiques donut budget, courbe dépenses, barres investissements ; investissements éclatés en 10 UseCases distincts (SRP)
- Sprint 4 : transactions récurrentes avec génération mensuelle automatique
- Sprint 5 : recherche et filtres sur la liste des transactions (barre de recherche + icône filtre avec badge)
- Sprint 6 : notifications budget dépassé, rappels dettes, alerte fonds faibles
- Sprint 7 : préférences utilisateur via DataStore (seuil liquidités, devise par défaut)
- Sprint 8 : rapport mensuel (bilan, budget, top catégories, variation M-1), intégré au Dashboard via toggle Paramètres
- Sprint 9 : authentification PIN/password, sous-catégories, enrichissement des catégories, navigation ; saisie des revenus ; toggle Épargne/Placements dans Paramètres
- README v1.0 (présentation, stack, architecture)

### Corrigé
- BUG-01 : liquidités figées ; BUG-02 : budget non modifiable
- BUG-03/BUG-04 : édition CRUD sur Épargne et Investissements
- 2 bugs notifications détectés en test physique (Android 16)
- 3 bugs détectés en test physique avant Sprint 3

### Sprint 2 — Budget, patrimoine et gestion financière complète

- `SetBudgetUseCase` : création/mise à jour du budget mensuel avec validation
- `BudgetViewModel` + `BudgetUiState` : gestion du budget mensuel
- `BudgetScreen` : affichage du budget, barre de progression, répartition par catégorie, dialogue de saisie
- `ExpensesViewModel` + `ExpensesUiState` + `ExpensesEvent` : gestion des dépenses
- `ExpensesScreen` : liste des dépenses du mois, bottom sheet d'ajout rapide, feedback snackbar
- `UpdateTransactionUseCase` : mise à jour d'une transaction par @Update (préserve l'ID)
- `SavingsViewModel` + `SavingsScreen` : gestion des comptes épargne et enfants
- `InvestmentsViewModel` + `InvestmentsScreen` : gestion de l'immobilier, SCPI et revenus Airbnb
- `DebtsViewModel` + `DebtsScreen` : suivi des dettes et crédits avec navigation depuis le Dashboard
- `PatrimonyOverview` + `GetPatrimonyOverviewUseCase` : vue consolidée du patrimoine net
- `DashboardScreen` : tableau de bord avec patrimoine brut/net, métriques et accès aux dettes
- Navigation complète depuis le Dashboard vers `DebtsScreen`
- Migration Room v1 → v2 : `childId` sur transactions, tables dettes/épargne/investissements
- `EnumExt.kt` : `safeValueOf` — lecture défensive des enums depuis la base de données
- `CurrencyExt.kt` : `toCurrencyDisplay` partagé entre tous les écrans
- Tests unitaires : `BudgetViewModelTest` (3 cas), `ExpensesViewModelTest` (4 cas), `SetBudgetUseCaseTest` (2 cas), `DebtsViewModelTest` (4 cas), `SavingsViewModelTest` (3 cas), `InvestmentsViewModelTest` (4 cas)

### Sprint 1 — Auth & Navigation

- `BiometricAuthManager` : authentification biométrique via `BiometricPrompt` (empreinte, face, PIN)
- `LockScreen` : écran de verrouillage affiché au lancement, déverrouillage automatique
- `AuthViewModel` + `AuthUiState` : gestion de l'état d'authentification
- `BottomNavBar` : barre de navigation inférieure (Dashboard, Budget, Dépenses, Investissements)
- Navigation complète avec gestion de la pile (`popUpTo`, `launchSingleTop`)
- `DashboardScreen` : affichage du budget du mois avec `BudgetCard` et barre de progression
- Écrans placeholder : `BudgetScreen`, `ExpensesScreen`, `InvestmentsScreen`
- `SecurityModule` Hilt pour `BiometricAuthManager`
- Tests unitaires `AuthViewModelTest` (4 cas : idle, succès, annulation, erreur)

### Init

- Initialisation du projet Android (Clean Architecture, MVVM, Hilt, Room, Compose)
- Structure des couches : `presentation`, `domain`, `data`
- Modèles métier : `Transaction`, `Budget`, `Currency`, `Category`
- Interfaces Repository : `TransactionRepository`, `BudgetRepository`
- UseCases : `AddTransactionUseCase`, `GetMonthlyBudgetUseCase`, `GetMonthlyTransactionsUseCase`
- `DashboardViewModel` avec pattern UiState scellé
- Module Hilt pour la base de données Room
- Pipeline CI GitHub Actions (lint + tests + couverture + build)
- Premier test unitaire : `AddTransactionUseCaseTest`
- Thème Compose (dark mode par défaut, couleur principale verte)
