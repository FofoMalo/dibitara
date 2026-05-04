# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Contexte du projet

Application bancaire Android à usage personnel, inspirée de **Finary**. L'objectif est de centraliser budget mensuel, suivi des dépenses, investissements et projections financières.

**Stack cible :** Android natif (Kotlin), architecture MVVM + Clean Architecture.

## Fonctionnalités principales

- Centralisation du budget mensuel et suivi des dépenses
- Suivi des investissements
- Rappels et conseils sur les fonds disponibles
- Devises supportées : Euro (€), Dollar ($), Franc CFA (XOF/XAF)
- Projections graphiques (courbes, camemberts, histogrammes)

## Build & Run Commands

```bash
# Build debug
./gradlew assembleDebug

# Build release
./gradlew assembleRelease

# Lancer les tests unitaires
./gradlew test

# Lancer un test spécifique
./gradlew test --tests "com.dibitara.app.NomDuTest"

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

## Conventions de développement

- **Langue du code :** Kotlin uniquement.
- **Commentaires :** en français, clairs et pédagogiques — le code est lu par un développeur junior.
- **Chaque UseCase** ne fait qu'une seule chose (principe de responsabilité unique).
- **Les ViewModels** exposent des `StateFlow` ou `LiveData`, jamais de logique métier directe.
- **Devises :** toujours stocker les montants en centimes (Long) avec la devise associée ; la conversion se fait dans la couche `domain`.

## Posture de travail

Ce projet est développé en binôme senior/junior. Le code doit être un support pédagogique :
- Expliquer les choix d'architecture dans les commentaires quand ce n'est pas évident.
- Préférer la clarté à la concision quand les deux sont en tension.
- Valider les approches avec le junior avant d'implémenter des patterns avancés.
