# Changelog — Dibitara

Format : [Keep a Changelog](https://keepachangelog.com/fr/1.0.0/)
Versioning : [Semantic Versioning](https://semver.org/lang/fr/)

---

## [Unreleased]

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
