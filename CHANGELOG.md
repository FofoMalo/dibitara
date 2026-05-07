# Changelog — Dibitara

Format : [Keep a Changelog](https://keepachangelog.com/fr/1.0.0/)
Versioning : [Semantic Versioning](https://semver.org/lang/fr/)

---

## [Unreleased] — Sprint 2 : Patrimoine & Gestion financière complète

### Ajouté
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

## [Sprint 1] — Auth & Navigation

### Ajouté
- `BiometricAuthManager` : authentification biométrique via `BiometricPrompt` (empreinte, face, PIN)
- `LockScreen` : écran de verrouillage affiché au lancement, déverrouillage automatique
- `AuthViewModel` + `AuthUiState` : gestion de l'état d'authentification
- `BottomNavBar` : barre de navigation inférieure (Dashboard, Budget, Dépenses, Investissements)
- Navigation complète avec gestion de la pile (`popUpTo`, `launchSingleTop`)
- `DashboardScreen` : affichage du budget du mois avec `BudgetCard` et barre de progression
- Écrans placeholder : `BudgetScreen`, `ExpensesScreen`, `InvestmentsScreen`
- `SecurityModule` Hilt pour `BiometricAuthManager`
- Tests unitaires `AuthViewModelTest` (4 cas : idle, succès, annulation, erreur)

## [Init]

### Ajouté
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
