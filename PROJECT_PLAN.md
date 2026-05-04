# Dibitara — Plan de Préparation du Projet

> Application bancaire Android personnelle | Inspirée de Finary  
> Version du document : 1.0 — 2026-05-04  
> Statut : **Préparation**

---

## Table des matières

1. [Vision & Périmètre](#1-vision--périmètre)
2. [Besoins techniques](#2-besoins-techniques)
3. [Besoins non-techniques](#3-besoins-non-techniques)
4. [Environnement de test & gestion des régressions](#4-environnement-de-test--gestion-des-régressions)
5. [Estimation des coûts](#5-estimation-des-coûts)
6. [Planning & Livrables](#6-planning--livrables)
7. [Risques & Mitigations](#7-risques--mitigations)
8. [Suivi de version du document](#8-suivi-de-version-du-document)

---

## 1. Vision & Périmètre

### Objectif
Centraliser toutes les informations financières personnelles (budget, dépenses, investissements) dans une application Android native, sécurisée, et pédagogiquement construite.

### Fonctionnalités V1 (MVP)
| # | Fonctionnalité | Priorité |
|---|---------------|----------|
| F1 | Saisie et suivi du budget mensuel | MUST |
| F2 | Suivi des dépenses par catégorie | MUST |
| F3 | Suivi des investissements | MUST |
| F4 | Multi-devises : EUR, USD, XOF/XAF | MUST |
| F5 | Projections graphiques | MUST |
| F6 | Rappels et conseils sur fonds disponibles | SHOULD |
| F7 | Authentification biométrique | SHOULD |
| F8 | Export des données (CSV/PDF) | COULD |
| F9 | Sauvegarde cloud chiffrée | COULD |

### Hors périmètre V1
- Connexion aux APIs bancaires réelles (Open Banking)
- Version iOS
- Application web

---

## 2. Besoins techniques

### 2.1 Environnement de développement
| Outil | Version minimale | Rôle |
|-------|-----------------|------|
| Android Studio | Hedgehog (2023.1+) | IDE principal |
| JDK | 17 | Compilation |
| Kotlin | 1.9+ | Langage |
| Gradle | 8.x | Build system |
| Git | 2.x | Contrôle de version |
| GitHub / GitLab | — | Hébergement + CI/CD |

### 2.2 Stack technique Android
```
app/
├── presentation/     # MVVM : Fragments, ViewModels, Compose UI
├── domain/           # UseCases, Entités, interfaces Repository
└── data/             # Room, DataStore, API clients
```

| Composant | Bibliothèque | Justification |
|-----------|-------------|---------------|
| UI | Jetpack Compose | Standard moderne Android |
| Navigation | Navigation Component | Gestion des écrans |
| Architecture | ViewModel + StateFlow | MVVM réactif |
| Base de données locale | Room | ORM officiel Android |
| Injection de dépendances | Hilt | Standard Google |
| Async | Coroutines + Flow | Programmation réactive |
| Graphiques | Vico (ou MPAndroidChart) | Projections visuelles |
| Préférences | DataStore | Remplacement SharedPreferences |
| Sécurité | AndroidKeyStore + BiometricPrompt | Protection des données |
| Taux de change | API Frankfurter (gratuite) | Conversion EUR/USD/XOF |
| Tests UI | Espresso / Compose Test | Tests d'interface |
| Tests unitaires | JUnit 5 + MockK | Tests métier |
| Couverture | Kover | Rapport de couverture |

### 2.3 Cibles Android
| Paramètre | Valeur |
|-----------|--------|
| minSdkVersion | 26 (Android 8.0) — couvre 95%+ des appareils |
| targetSdkVersion | 35 (Android 15) |
| compileSdkVersion | 35 |

### 2.4 Sécurité (non négociable)
- Toutes les données locales chiffrées via **EncryptedSharedPreferences** ou **SQLCipher + Room**.
- Authentification biométrique avant accès à l'app.
- Aucune donnée financière en clair dans les logs.
- Obfuscation du code release via **R8/ProGuard**.

---

## 3. Besoins non-techniques

### 3.1 Conception & UX
| Livrable | Outil recommandé | Statut |
|---------|-----------------|--------|
| Wireframes basse fidélité | Figma (gratuit) | À faire |
| Charte graphique & Design System | Figma | À faire |
| Maquettes haute fidélité | Figma | À faire |
| Prototype cliquable | Figma | Optionnel V1 |

> **Inspiration :** étudier l'UX de Finary, Linxo, et Wallet pour identifier les patterns UX de référence en gestion financière.

### 3.2 Organisation & Méthode
- **Méthode :** Agile allégé — sprints de 2 semaines avec revue de fin de sprint.
- **Suivi des tâches :** GitHub Projects (kanban : Backlog / En cours / Review / Done).
- **Branches Git :** `main` (stable), `develop` (intégration), `feature/xxx`, `fix/xxx`.
- **Convention de commits :** [Conventional Commits](https://www.conventionalcommits.org/) — ex. `feat(budget): ajouter saisie dépense`.
- **Code review :** toute PR doit être relue avant merge — moment pédagogique senior → junior.
- **Versioning :** Semantic Versioning `MAJOR.MINOR.PATCH`.

### 3.3 Compétences junior à développer
| Compétence | Sprint cible |
|-----------|-------------|
| Architecture MVVM | Sprint 1 |
| Room & migrations | Sprint 2 |
| Coroutines & Flow | Sprint 2 |
| Jetpack Compose | Sprint 3 |
| Tests unitaires | Sprint 3 |
| Sécurité Android | Sprint 4 |

---

## 4. Environnement de test & Gestion des régressions

### 4.1 Niveaux de test
```
Pyramide de tests :
        [E2E]          ← Peu nombreux, coûteux
      [Intégration]    ← Room, Repository
    [Tests Unitaires]  ← UseCases, ViewModels, Convertisseurs
```

| Niveau | Outil | Cible de couverture |
|--------|-------|-------------------|
| Unitaires | JUnit 5 + MockK | ≥ 80% sur `domain/` |
| Intégration | JUnit + Room In-Memory | Repositories |
| UI / E2E | Espresso + Compose Test | Parcours critiques |

### 4.2 Appareils de test
| Type | Détail | Obligatoire |
|------|--------|------------|
| Émulateur API 26 | Android 8 — minSdk | Oui |
| Émulateur API 35 | Android 15 — targetSdk | Oui |
| Appareil physique | Ex. Pixel ou Samsung réel | Recommandé |
| Firebase Test Lab | Cloud — multiples appareils | V2 |

### 4.3 CI/CD & Prévention des régressions
Pipeline GitHub Actions déclenché à chaque PR sur `develop` et `main` :

```
PR ouverte
  └─> [Lint] gradle lint
  └─> [Tests unitaires] gradle test
  └─> [Couverture] rapport Kover (seuil min 80%)
  └─> [Build debug] gradle assembleDebug
  └─> [Tests UI] gradle connectedAndroidTest (émulateur)
  └─> Merge autorisé seulement si tout est vert ✅
```

### 4.4 Suivi des régressions
- **CHANGELOG.md** tenu à jour à chaque sprint (format Keep a Changelog).
- **Numéro de build** incrémenté automatiquement par CI.
- Toute régression détectée crée automatiquement une issue GitHub avec le log.
- Avant chaque release : **test de fumée** manuel sur les 5 parcours critiques (voir liste ci-dessous).

### 4.5 Parcours critiques (smoke tests)
1. Lancement app → authentification biométrique → tableau de bord.
2. Ajout d'une dépense → apparition dans le budget mensuel.
3. Changement de devise EUR → USD → vérification de la conversion.
4. Consultation d'un graphique de projection.
5. Sauvegarde et restauration des données.

---

## 5. Estimation des coûts

### 5.1 Coûts directs
| Poste | Coût | Fréquence | Notes |
|-------|------|-----------|-------|
| Google Play Store | 25 € | Une fois | Compte développeur |
| Figma | 0 € | — | Plan gratuit suffisant pour 2 éditeurs |
| GitHub | 0 € | — | Plan gratuit (CI inclus) |
| Firebase (Crashlytics + Analytics) | 0 € | — | Free tier largement suffisant |
| API Frankfurter (taux de change) | 0 € | — | Open source, sans clé API |
| Firebase Test Lab | ~5–15 €/mois | Sprint 3+ | Optionnel V1, recommandé V2 |

> **Total coûts directs V1 : ~25 € (one-shot)**

### 5.2 Coûts en temps (estimation)
| Phase | Durée estimée | Charge senior | Charge junior |
|-------|--------------|--------------|--------------|
| Préparation & Setup | 1 semaine | 60% | 40% |
| Sprint 1 — Architecture & Auth | 2 semaines | 50% | 50% |
| Sprint 2 — Budget & Dépenses | 2 semaines | 40% | 60% |
| Sprint 3 — Investissements & Graphiques | 2 semaines | 40% | 60% |
| Sprint 4 — Devises & Rappels | 2 semaines | 30% | 70% |
| Sprint 5 — Tests, polish & release | 2 semaines | 50% | 50% |
| **Total** | **~11 semaines** | | |

---

## 6. Planning & Livrables

### 6.1 Jalons
```
Semaine 1   ── Setup complet (repo, CI, structure projet)
Semaine 3   ── Authentification + navigation principale
Semaine 5   ── Budget mensuel + dépenses fonctionnels
Semaine 7   ── Investissements + graphiques de base
Semaine 9   ── Multi-devises + rappels
Semaine 11  ── Release V1 sur Play Store (ou APK interne)
```

### 6.2 Livrables par phase
| Phase | Livrables |
|-------|-----------|
| Préparation | Ce document + CLAUDE.md + repo initialisé + maquettes |
| Sprint 1 | Squelette Clean Architecture + écran auth + navigation |
| Sprint 2 | Module Budget complet + tests unitaires associés |
| Sprint 3 | Module Investissements + graphiques Vico |
| Sprint 4 | Conversion devises + système de rappels |
| Sprint 5 | APK release signée + CHANGELOG + documentation utilisateur |

---

## 7. Risques & Mitigations

| Risque | Probabilité | Impact | Mitigation |
|--------|------------|--------|------------|
| Complexité des migrations Room | Moyen | Élevé | Écrire les migrations dès le sprint 1, tests de migration systématiques |
| Drift du junior sur l'architecture | Élevé | Moyen | Code review à chaque PR, session de pair-programming hebdomadaire |
| API taux de change indisponible | Faible | Moyen | Cache local des derniers taux connus |
| Fuite de données sensibles | Faible | Très élevé | Audit sécurité avant chaque release, aucun log de données financières |
| Dérive des délais | Moyen | Moyen | Backlog priorisé, scope V1 figé — nouvelles idées → V2 |

---

## 8. Suivi de version du document

| Version | Date | Auteur | Changements |
|---------|------|--------|-------------|
| 1.0 | 2026-05-04 | Florent | Création initiale |

---

*Ce document est vivant. Il doit être mis à jour à chaque fin de sprint.*
