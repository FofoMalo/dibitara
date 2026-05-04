# Changelog — Dibitara

Format : [Keep a Changelog](https://keepachangelog.com/fr/1.0.0/)
Versioning : [Semantic Versioning](https://semver.org/lang/fr/)

---

## [Unreleased] — Sprint 2 : Budget & Dépenses

### Ajouté
- `SetBudgetUseCase` : création/mise à jour du budget mensuel avec validation
- `BudgetViewModel` + `BudgetUiState` : gestion du budget mensuel
- `BudgetScreen` : affichage du budget, barre de progression, répartition par catégorie, dialogue de saisie
- `ExpensesViewModel` + `ExpensesUiState` + `ExpensesEvent` : gestion des dépenses
- `ExpensesScreen` : liste des dépenses du mois, bottom sheet d'ajout rapide, feedback snackbar
- Tests unitaires : `BudgetViewModelTest` (3 cas), `ExpensesViewModelTest` (4 cas), `SetBudgetUseCaseTest` (2 cas)

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
