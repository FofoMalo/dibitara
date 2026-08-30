# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Contexte du projet

Application bancaire Android à usage personnel, inspirée de **Finary**. L'objectif est de centraliser budget mensuel, suivi des dépenses, investissements et projections financières.

**Stack cible :** Android natif (Kotlin), architecture MVVM + Clean Architecture.
**Version courante :** v4.4.2 (versionCode 17) — Room v25 (branche `florent/prive`).

## Fonctionnalités principales

- Centralisation du budget mensuel et suivi des dépenses
- Suivi des investissements (immo, SCPI, Airbnb, métaux précieux, actifs libres, épargne salariale)
- Export des données en CSV et JSON (partage via Intent Android)
- Rappels et conseils sur les fonds disponibles
- Devises supportées : Euro (€), Dollar ($), Franc CFA (XOF/XAF)
- Projections graphiques (courbes, camemberts, histogrammes)
- Authentification PIN + biométrie

## Build & Run Commands

```bash
# Build debug
./gradlew assembleDebug

# Build release (APK)
./gradlew assembleRelease

# Build release (AAB — pour le Play Store)
./gradlew bundleRelease

# Lancer les tests unitaires
./gradlew test

# Lancer un test spécifique
./gradlew test --tests "com.dibitara.app.NomDuTest"

# Vérifier la couverture de code (seuil 80% domain/)
./gradlew koverVerify

# Rapport de couverture HTML
./gradlew koverHtmlReport

# Lancer les tests instrumentés (émulateur requis)
./gradlew connectedAndroidTest

# Lint
./gradlew lint
```

## Architecture

Clean Architecture en 3 couches :

- **`presentation/`** — Fragments/Activities + ViewModels (MVVM). Aucune logique métier ici.
- **`domain/`** — UseCases, entités métier, interfaces Repository. Couche pure Kotlin, sans dépendance Android.
- **`data/`** — Implémentations Repository, Room (base locale), éventuellement API distante.

Le flux de données va toujours dans un seul sens : `UI → ViewModel → UseCase → Repository → DataSource`.

- **Piège `by viewModels()` (Activity) :** ce délégué est paresseux — déclarer
  `private val vm: AppViewModel by viewModels()` ne l'instancie pas tant que
  la propriété n'est pas lue au moins une fois. Bug rencontré en 2026-08 sur
  `MainActivity` : le seul point de lecture de `appViewModel` était le
  callback de la demande de permission notifications, jamais déclenché si la
  permission est déjà accordée — toute la logique de démarrage (récurrences,
  notifications, migrations) placée dans `AppViewModel.init` ne s'exécutait
  donc jamais sur un appareil réel. Fix : lire explicitement la propriété
  dans `onCreate` (ex. `appViewModel` en instruction seule) pour forcer
  l'instanciation.
- **Piège `Category.AUTRE` seul ≠ « non catégorisé » :** `AUTRE` est un
  bucket parent — la vraie catégorisation d'une transaction `AUTRE` se lit
  dans `subCategory` (enum fixe) OU `customSubCategoryId` (sous-catégorie
  perso), jamais dans `category` seul. Tout filtre/comptage qui veut dire
  « à catégoriser » doit vérifier `category == AUTRE && subCategory == null
  && customSubCategoryId == null` ensemble. Bug rencontré deux fois le même
  jour (2026-08-27) : le chip « À catégoriser » d'`ExpensesScreen`
  (`ExpensesFilter`) filtrait sur `category == AUTRE` seul et affichait
  aussi les dépenses déjà classées (ex. `BAR_ET_RESTAURANT`) ; et
  `GetRecategorizationSuggestionsUseCase` (suggestions du Dashboard)
  vérifiait `subCategory == null` mais oubliait `customSubCategoryId`.

## Schéma Room — Version actuelle : v25 (branche `florent/prive`)

| Migration | Contenu |
|-----------|---------|
| v1 → v2 | `childId` sur transactions, tables dettes/épargne/investissements |
| v2 → v3 | Transactions récurrentes (`isRecurring`, `recurrenceDay`, `sourceRecurringId`) |
| v3 → v4 | `subCategory` sur transactions |
| v4 → v5 | Table `custom_sub_categories` + `customSubCategoryId` |
| v5 → v6 | Table `monthly_versements` |
| v6 → v7 | Restructuration catégories (VACANCES→LOISIRS, ABONNEMENTS promu) |
| v7 → v8 | Récurrences enrichies (`recurrenceFrequency`, `firstPaymentDateEpochDay`, `endDateEpochDay`) |
| v8 → v9 | `sharesCount` Int→Real (SCPI parts fractionnées) |
| v9 → v10 | Tables `precious_metals`, `custom_assets`, `employee_savings` |
| v10 → v11 | Colonnes `importSource TEXT`, `externalId TEXT` sur `transactions` (import CSV TradeRepublic) |
| v11 → v12 | `debtId` sur `real_estate_assets` pour lier un crédit à un bien |
| v12 → v13 | Table `patrimoine_snapshots` pour l'historique mensuel du patrimoine |
| v13 → v14 | `plafondCents` sur `savings_accounts` pour le suivi du plafond légal |
| v14 → v15 | `paymentDay` et `originalAmountCents` sur `debts` |
| v15 → v16 | Taux d'intérêt annuel sur `debts` |
| v16 → v17 | Table `categorization_rules` pour l'apprentissage des catégorisations manuelles |
| v17 → v18 | Table `category_envelopes` pour les enveloppes budgétaires par catégorie |
| v18 → v19 | Table `vehicle_rental_entries` pour l'activité de location de véhicule |
| v19 → v20 | Suppression de `precious_metals` (métaux précieux, fonctionnalité retirée) |
| v20 → v21 | Table `asset_valuation_snapshots` pour les badges de tendance par actif (Immobilier/SCPI) |
| v21 → v22 | Table `bank_accounts` (BRED, TradeRepublic) + `bankAccountId` sur `transactions`, seed et backfill via `importSource` |
| v22 → v23 | `acquisitionValueCents`/`acquisitionDateEpochDay` sur les 4 types d'actifs investissement, pour le bloc "Acquisition → Aujourd'hui" |
| v23 → v24 | `tauxAnnuelPct REAL` sur `savings_accounts` (intérêts annuels estimés, hero Épargne) |
| v24 → v25 | Table `savings_goals` pour les objectifs d'épargne (écran Épargne §3) |

## Modèles métier clés (domain/model/)

`Transaction`, `Budget`, `Debt`, `SavingsAccount`, `RealEstateAsset`, `ScpiInvestment`, `AirbnbRental`, `PatrimonyOverview`, `Currency`, `Category`, `SubCategory`, `CustomSubCategory`, `DebtType`, `SavingsType`, `Child`, `UserPreferences`, `MonthlyReport`, `CategoryExpense`, `MonthlyVersement`, `RecurrenceFrequency`, `UpcomingPayment`, `TransactionSuggestion`, `ExportData`, `ExportFormat`, `PreciousMetalAsset`, `CustomAsset`, `EmployeeSavings`, `MetalType`, `EmployeeSavingsType`, `ImportedTransaction`, `ImportResult`

## Conventions de développement

- **Langue du code :** Kotlin uniquement.
- **Commentaires :** en français, clairs et pédagogiques — le code est lu par un développeur junior.
- **Chaque UseCase** ne fait qu'une seule chose (principe de responsabilité unique). ~69 UseCases au total.
- **Les ViewModels** exposent des `StateFlow` ou `LiveData`, jamais de logique métier directe.
- **Devises :** toujours stocker les montants en centimes (Long) avec la devise associée ; la conversion se fait dans la couche `domain`.
- **Migrations Room :** chaque modification de schéma incrémente `version` d'exactement 1 et requiert une migration + le fichier `N.json` exporté. Ne jamais utiliser `fallbackToDestructiveMigration` en production.
- **Promouvoir une sous-catégorie personnalisée en `Category` à part entière :** `Category` est stockée en base comme `String` (colonne `category` sur `transactions`, avec `safeValueOf` en fallback) — ajouter une valeur à l'enum ne nécessite **pas** de migration Room. Seule la donnée doit être réconciliée : un `UseCase` applicatif (pas une migration de schéma) réassigne les transactions liées à l'ancienne `CustomSubCategory` vers la nouvelle `Category` et supprime la sous-catégorie devenue orpheline. Cas réel : `MigrerTabacVersCategorieUseCase` (2026-08), nécessaire car `CategoryEnvelope` ne peut cibler qu'une `Category`, jamais une sous-catégorie personnalisée.
- **Messages de commit :** sujet verbe complément, en français, sans Co-Authored-By.
- **Bump de version :** à chaque saut de fonctionnalité ou d'amélioration
  notable (fin de sprint, feature livrée), incrémenter `versionCode`/
  `versionName` dans `app/build.gradle.kts` et ajouter une entrée
  `CHANGELOG.md`. Si je livre un tel saut sans que ce soit fait, je dois le
  signaler explicitement à Florent avant de considérer la tâche terminée.
  Constat du 2026-08-27 : ce réflexe avait été perdu entre Sprint 19 (4.3.0)
  et Sprint 42+ — 20 sprints et 107 commits livrés sans un seul bump,
  `CLAUDE.md` annonçait même une version qui n'existait pas dans le code.

## Conventions UI / Design

Vocabulaire visuel établi pendant la refonte UX/UI 2026-08 (palette noir/or,
Material 3) — à réutiliser plutôt qu'à réinventer sur tout nouvel écran ou
toute nouvelle carte de synthèse.

- **Palette :** l'or (`primary`) est réservé aux accents (bordures, CTA,
  icônes actives, valeurs clés) — jamais en `containerColor` plein d'une
  `Card`. Le rouge (`error`) est réservé aux vraies alertes, jamais en fond
  de carte. Le vert (`tertiary`) marque les montants positifs/tendances à la
  hausse.
- **`HeroCard`** (`presentation/common/HeroCard.kt`) — carte de synthèse mise
  en avant (fond neutre `surface`, bordure dorée légère, filet dégradé en
  haut). À utiliser pour toute carte "résumé" en tête d'écran (Patrimoine,
  Budget, Épargne), plutôt qu'un `containerColor` plein.
- **`CategoryVisuals`** — couleur et icône stables par `Category`, jamais
  positionnelles (une palette indexée par position change de couleur d'un
  mois à l'autre selon l'ordre de tri).
- **`TrendChip`** (`presentation/common/TrendChip.kt`) — badge générique
  `+X,X%`/`-X,X%`, réutilisé sur le patrimoine global, les placements et les
  actifs individuels. Générique et sans dépendance de domaine : à réutiliser
  pour tout nouveau badge de tendance plutôt qu'en recréer un.
- **`AcquisitionEvolutionBlock`** (`presentation/common/AcquisitionEvolutionBlock.kt`) —
  bloc "Acquisition → Aujourd'hui" affiché sur les 4 cartes d'actif investissement
  (Immobilier, SCPI, Actif libre, Épargne salariale) quand `acquisitionValueCents`
  et `acquisitionDate` sont renseignés (saisie rétroactive, jamais déduite des
  `AssetValuationSnapshot` - ceux-ci ne remontent pas avant la refonte du 2026-08).
  Réutilise `TrendChip` pour le badge de pourcentage. Le delta est net des
  versements/abondements/mouvements de capital enregistrés depuis l'acquisition
  (`SommeVersementsDepuisUseCase`) pour SCPI, Épargne salariale, Actif libre et Immobilier via
  `CompteType.SCPI`/`CompteType.EMPLOYEE_SAVINGS`/`CompteType.CUSTOM_ASSET`/`CompteType.REAL_ESTATE`
  - un versement est de l'argent apporté (ou retiré), pas de la performance. Pour Actif libre
  et Immobilier (ex. travaux qui font remonter la valeur affichée), ce mouvement est signé
  (retrait = négatif) et cumulé dans le mois via `EnregistrerMouvementCapitalUseCase` plutôt
  que rejeté par la contrainte UNIQUE (compte, type, mois) de `monthly_versements` -
  contrairement au versement mensuel fixe de SCPI/Épargne salariale, ces deux types peuvent
  recevoir plusieurs mouvements le même mois. SCPI et Épargne salariale ONT un versement
  mensuel récurrent (`appliquerVersementScpi`/`appliquerVersementEmployeeSavings`,
  contrainte UNIQUE) - leur mouvement ad hoc passe donc par une **voie séparée**
  (`CompteType.SCPI_MOUVEMENT`/`EMPLOYEE_SAVINGS_MOUVEMENT`) pour ne pas collisionner avec
  cette ligne mensuelle ; `InvestmentsViewModel.performanceDepuisAcquisition` additionne les
  deux voies (`mouvementLane`). Nuance SCPI (`EditScpiSheet`) : le mouvement suggéré porte sur
  le **delta de parts** valorisé au prix actuel, pas sur le delta de valeur totale - sinon une
  simple révision du prix de la part par le gestionnaire serait neutralisée à tort (voir
  `CADRAGE_MOUVEMENTS_CAPITAL_SCPI_EPARGNE.md`).
- **Piège Vico (bibliothèque de graphiques) :** Vico tronque les libellés
  d'axe indépendamment de l'espace réellement disponible - il alloue la
  largeur de chaque graduation selon le nombre total de points de données,
  pas selon les libellés effectivement affichés. Un `AxisValueFormatter` qui
  n'affiche qu'un label sur N ne suffit pas toujours à corriger ça. Pour tout
  graphique avec beaucoup de points ou des libellés de longueur variable,
  préférer un `Canvas` custom (voir `HorizontalBarChart.kt`,
  `ProjectionSparkline.kt`) ou masquer l'axe Vico et dessiner ses propres
  libellés (voir `TrendsScreen.kt`) plutôt que de chercher à configurer
  l'axe existant.
- **Menu "⋮" pour Modifier/Supprimer :** sur un élément de liste, préférer
  `Box { IconButton(MoreVert) { DropdownMenu { DropdownMenuItem(Modifier) ; DropdownMenuItem(Supprimer) } } }`
  à deux `IconButton` côte à côte — ces derniers ne laissent plus assez de
  place dès qu'une icône ou un badge supplémentaire s'ajoute à la ligne.

## Posture de travail

Ce projet est développé en binôme senior/junior. Le code doit être un support pédagogique :
- Expliquer les choix d'architecture dans les commentaires quand ce n'est pas évident.
- Préférer la clarté à la concision quand les deux sont en tension.
- Valider les approches avec le junior avant d'implémenter des patterns avancés.

## Lignes directrice comportementales :

### **Les Quatre Principes en Détail**

---

#### 1. **Réfléchir avant de coder**

Ne fais pas d’hypothèses. Ne cache pas ta confusion. Mets en lumière les compromis.

- **Exprime clairement tes hypothèses** — Si tu n’es pas sûr, demande plutôt que de deviner.
- **Présente plusieurs interprétations** — Ne choisis pas en silence s’il y a une ambiguïté.
- **Propose une alternative si nécessaire** — Si une approche plus simple existe, dis-le.
- **Arrête-toi si tu es perdu** — Identifie ce qui n’est pas clair et demande des éclaircissements.

---

#### 2. **La simplicité d’abord**

Écris le code minimal qui résout le problème. Rien de spéculatif.

- Pas de fonctionnalités au-delà de ce qui a été demandé.
- Pas d’abstractions pour du code utilisé une seule fois.
- Pas de « flexibilité » ou de « configurabilité » non demandée.
- Pas de gestion d’erreurs pour des scénarios impossibles.
- Si 200 lignes peuvent être réduites à 50, réécris-les.

**Le test** : Un ingénieur senior trouverait-il cela trop compliqué ? Si oui, simplifie.

---
#### 3. **Des modifications chirurgicales**

Ne touche qu’à ce qui est nécessaire. Nettoie uniquement ton propre désordre.

Lorsque tu modifies du code existant :

- Ne « améliore » pas le code, les commentaires ou la mise en forme adjacente.
- Ne refactorise pas ce qui fonctionne.
- Respecte le style existant, même si tu ferais autrement.
- Si tu repères du code mort non lié, signale-le — ne le supprime pas.

Si tes modifications créent du code orphelin :

- Supprime les imports/variables/fonctions **que TES modifications** ont rendus inutilisés.
- Ne supprime pas le code mort préexistant, sauf si on te le demande.

**Le test** : Chaque ligne modifiée doit pouvoir être reliée directement à la demande de l’utilisateur.

#### 4. **Exécution orientée objectifs**

Définis des critères de succès. Boucle jusqu’à vérification.

Transforme les tâches impératives en objectifs vérifiables :

Au lieu de… | Transforme en…
---|---
« Ajoute une validation » | « Écris des tests pour les entrées invalides, puis fais-les passer »
« Corrige le bug » | « Écris un test qui le reproduit, puis fais-le passer »
« Refactorise X » | « Vérifie que les tests passent avant et après »

Pour les tâches en plusieurs étapes, expose un plan concis :

1. [Étape] → vérification : [contrôle]
2. [Étape] → vérification : [contrôle]
3. [Étape] → vérification : [contrôle]
