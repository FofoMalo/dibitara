# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Contexte du projet

Application bancaire Android à usage personnel, inspirée de **Finary**. L'objectif est de centraliser budget mensuel, suivi des dépenses, investissements et projections financières.

**Stack cible :** Android natif (Kotlin), architecture MVVM + Clean Architecture.
**Version courante :** v4.4.0 (versionCode 15) — Room v11.

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

## Schéma Room — Version actuelle : v11

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

## Modèles métier clés (domain/model/)

`Transaction`, `Budget`, `Debt`, `SavingsAccount`, `RealEstateAsset`, `ScpiInvestment`, `AirbnbRental`, `PatrimonyOverview`, `Currency`, `Category`, `SubCategory`, `CustomSubCategory`, `DebtType`, `SavingsType`, `Child`, `UserPreferences`, `MonthlyReport`, `CategoryExpense`, `MonthlyVersement`, `RecurrenceFrequency`, `UpcomingPayment`, `TransactionSuggestion`, `ExportData`, `ExportFormat`, `PreciousMetalAsset`, `CustomAsset`, `EmployeeSavings`, `MetalType`, `EmployeeSavingsType`, `ImportedTransaction`, `ImportResult`

## Conventions de développement

- **Langue du code :** Kotlin uniquement.
- **Commentaires :** en français, clairs et pédagogiques — le code est lu par un développeur junior.
- **Chaque UseCase** ne fait qu'une seule chose (principe de responsabilité unique). ~69 UseCases au total.
- **Les ViewModels** exposent des `StateFlow` ou `LiveData`, jamais de logique métier directe.
- **Devises :** toujours stocker les montants en centimes (Long) avec la devise associée ; la conversion se fait dans la couche `domain`.
- **Migrations Room :** chaque modification de schéma incrémente `version` d'exactement 1 et requiert une migration + le fichier `N.json` exporté. Ne jamais utiliser `fallbackToDestructiveMigration` en production.
- **Messages de commit :** sujet verbe complément, en français, sans Co-Authored-By.

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
