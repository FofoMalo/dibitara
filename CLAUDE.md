# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Contexte du projet

Application bancaire Android à usage personnel, inspirée de **Finary**. L'objectif est de centraliser budget mensuel, suivi des dépenses, investissements et projections financières.

**Stack cible :** Android natif (Kotlin), architecture MVVM + Clean Architecture.
**Version courante :** v4.5.0 (versionCode 16) — Room v13.

## Fonctionnalités principales

- Centralisation du budget mensuel et suivi des dépenses
- Suivi des investissements (immo, SCPI, Airbnb, métaux précieux, actifs libres, épargne salariale)
- Réconciliation dettes/biens immobiliers (équité nette par actif)
- Export des données en CSV et JSON (partage via Intent Android)
- Rappels et conseils sur les fonds disponibles
- Devises supportées : Euro (€), Dollar ($), Franc CFA (XOF/XAF) avec conversion multi-devises
- Projections trésorerie 30 jours et graphiques (courbes, camemberts, histogrammes)
- Historique mensuel du patrimoine net avec sparkline
- Détection et nettoyage des doublons de transactions
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

## Schéma Room — Version actuelle : v13

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
| v10 → v11 | Colonnes `importSource TEXT`, `externalId TEXT` sur `transactions` (déduplication import) |
| v11 → v12 | Colonne `debtId INTEGER` sur `real_estate_assets` (réconciliation dettes/immo) |
| v12 → v13 | Table `patrimoine_snapshots` (historique mensuel du patrimoine net) |

## Modèles métier clés (domain/model/)

`Transaction`, `Budget`, `Debt`, `SavingsAccount`, `RealEstateAsset`, `ScpiInvestment`, `AirbnbRental`, `PatrimonyOverview`, `Currency`, `Category`, `SubCategory`, `CustomSubCategory`, `DebtType`, `SavingsType`, `Child`, `UserPreferences`, `MonthlyReport`, `CategoryExpense`, `MonthlyVersement`, `RecurrenceFrequency`, `UpcomingPayment`, `TransactionSuggestion`, `ExportData`, `ExportFormat`, `PreciousMetalAsset`, `CustomAsset`, `EmployeeSavings`, `MetalType`, `EmployeeSavingsType`, `ImportedTransaction`, `ImportResult`, `ExchangeRates`, `CurrencyConverter`, `DuplicateGroup`, `PatrimoineSnapshot`

## Conventions de développement

- **Langue du code :** Kotlin uniquement.
- **Commentaires :** en français, clairs et pédagogiques — le code est lu par un développeur junior.
- **Chaque UseCase** ne fait qu'une seule chose (principe de responsabilité unique). ~80 UseCases au total.
- **Les ViewModels** exposent des `StateFlow` ou `LiveData`, jamais de logique métier directe.
- **Devises :** toujours stocker les montants en centimes (Long) avec la devise associée ; la conversion se fait dans la couche `domain`.
- **Migrations Room :** chaque modification de schéma incrémente `version` d'exactement 1 et requiert une migration + le fichier `N.json` exporté. Ne jamais utiliser `fallbackToDestructiveMigration` en production.
- **Messages de commit :** sujet verbe complément, en français, sans Co-Authored-By.

## Posture de travail

Ce projet est développé en binôme senior/junior. Le code doit être un support pédagogique :
- Expliquer les choix d'architecture dans les commentaires quand ce n'est pas évident.
- Préférer la clarté à la concision quand les deux sont en tension.
- Valider les approches avec le junior avant d'implémenter des patterns avancés.
