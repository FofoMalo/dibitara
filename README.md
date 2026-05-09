# Dibitara

Application Android de gestion financière personnelle, inspirée de [Finary](https://finary.com).  
Centralise budget, dépenses, investissements et patrimoine en un seul endroit.

---

## Fonctionnalités v1.0

### Tableau de bord
- Vue patrimoine brut / net, liquidités, épargne, investissements
- Graphique des dépenses sur les 6 derniers mois
- Carte rapport mensuel synthèse (activable dans les Paramètres)
- Accès rapide aux dettes & crédits

### Budget
- Définir un budget mensuel
- Suivi en temps réel avec graphique donut (alloué / dépensé / restant)

### Dépenses
- Ajout, modification, suppression de transactions
- Catégories : alimentation, logement, transport, santé, loisirs, enfant, autre
- Transactions récurrentes avec génération automatique mensuelle
- Recherche par note + filtres (période, type, catégorie, tri) via icône badge

### Épargne
- Suivi des comptes d'épargne (livrets, assurance-vie…)

### Investissements
- Immobilier, SCPI, Airbnb
- Graphiques de répartition
- Vue patrimoine global

### Rapport mensuel
- Bilan revenus / dépenses / solde
- Barre de progression budget
- Top 3 catégories de dépenses avec pourcentage
- Variation vs mois précédent (↑↓)

### Paramètres
- Seuil d'alerte liquidités insuffisantes (configurable)
- Devise par défaut (EUR / USD / FCFA)
- Activation du rapport mensuel dans le tableau de bord

### Notifications
- Alerte budget dépassé
- Rappel d'échéance de dette
- Alerte liquidités insuffisantes (seuil configurable)

### Sécurité
- Verrouillage biométrique au démarrage

---

## Stack technique

| Couche | Technologies |
|--------|-------------|
| Langage | Kotlin |
| UI | Jetpack Compose + Material 3 |
| Architecture | MVVM + Clean Architecture (3 couches) |
| Injection | Hilt |
| Base de données | Room 2.7.2 (migrations 1→2→3) |
| Préférences | DataStore |
| Graphiques | Vico |
| Tests | JUnit 5 + MockK + Coroutines Test |

---

## Build

```bash
# Build debug
./gradlew assembleDebug

# Tests unitaires
./gradlew test

# Lint
./gradlew lint
```

---

## Architecture

```
app/src/main/java/com/dibitara/app/
├── data/          — Room, DataStore, implémentations Repository
├── di/            — Modules Hilt (Database, DataStore, Security)
├── domain/        — Modèles, interfaces Repository, 37 UseCases
└── presentation/  — ViewModels, écrans Compose, navigation
```

Le flux de données est unidirectionnel : `UI → ViewModel → UseCase → Repository → DataSource`.

---

## Tests

110 tests unitaires couvrant les UseCases et ViewModels.

```bash
./gradlew test --tests "com.dibitara.app.*"
```

---

*Projet personnel — v1.0.0*
