# Dibitara — Plan de Projet

> Application bancaire Android personnelle | Inspirée de Finary  
> Version du document : 3.8 — 2026-05-20  
> Statut : **En production** — v3.1.0 publiée (CI vert, Play Store en attente validation)

---

## Table des matières

1. [Vision & Périmètre](#1-vision--périmètre)
2. [Besoins techniques](#2-besoins-techniques)
3. [Besoins non-techniques](#3-besoins-non-techniques)
4. [Environnement de test & gestion des régressions](#4-environnement-de-test--gestion-des-régressions)
5. [Estimation des coûts](#5-estimation-des-coûts)
6. [Avancement des sprints](#6-avancement-des-sprints)
7. [Backlog post-v2.5.0](#7-backlog-post-v250)
8. [Risques & Mitigations](#8-risques--mitigations)
9. [Suivi de version du document](#9-suivi-de-version-du-document)

---

## 1. Vision & Périmètre

### Objectif
Centraliser toutes les informations financières personnelles (budget, dépenses, investissements) dans une application Android native, sécurisée, et pédagogiquement construite.

### Fonctionnalités V1 — Statut actuel
| # | Fonctionnalité | Priorité | Statut |
|---|---------------|----------|--------|
| F1 | Saisie et suivi du budget mensuel | MUST | ✅ Fait |
| F2 | Suivi des dépenses par catégorie | MUST | ✅ Fait (+ recherche, filtres, sous-catégories) |
| F3 | Suivi des investissements | MUST | ✅ Fait (SCPI, immobilier, Airbnb, épargne) |
| F4 | Multi-devises : EUR, USD, XOF/XAF | MUST | ✅ Fait — stockage en centimes + taux de change Frankfurter en temps réel |
| F5 | Projections graphiques | MUST | ✅ Fait (donut, courbe 6 mois, barres investissements) |
| F6 | Rappels et conseils sur fonds disponibles | SHOULD | ✅ Fait (3 canaux de notifications, Sprint 6) |
| F7 | Authentification sécurisée | SHOULD | ✅ Fait (PIN 4 chiffres + biométrie — email/password retiré UI Sprint 12) |
| F8 | Export des données (CSV/PDF) | COULD | ❌ Non implémenté — backlog V4 |
| F9 | Sauvegarde cloud chiffrée | COULD | ❌ Non implémenté — backlog V4 |

### Fonctionnalités réalisées au-delà du périmètre initial
- **Rapport mensuel** — Module complet avec bilan, top catégories, variation M/M-1 (Sprint 8)
- **Transactions récurrentes** — Génération automatique par `AppViewModel` au démarrage (Sprint 4)
- **Préférences utilisateur** — DataStore : devise par défaut, seuil liquidités, toggle rapport (Sprint 7)
- **Rapport mensuel dashboard** — Carte synthèse compacte activable depuis les paramètres
- **Politique de confidentialité** — Publiée sur GitHub pour le Play Store

### Hors périmètre V1 (inchangé)
- Connexion aux APIs bancaires réelles (Open Banking)
- Version iOS
- Application web

---

## 2. Besoins techniques

### 2.1 Environnement de développement
| Outil | Version utilisée | Rôle |
|-------|-----------------|------|
| Android Studio | Hedgehog (2023.1+) | IDE principal |
| JDK | 17 | Compilation |
| Kotlin | 1.9+ | Langage |
| Gradle | 8.x | Build system |
| Git | 2.x | Contrôle de version |
| GitHub | — | Hébergement + CI (build debug) |

### 2.2 Stack technique Android — État réel
```
app/src/main/java/com/dibitara/app/
├── data/
│   ├── local/        — Room v7, migrations 1→2→3→4→5→6→7
│   └── repository/   — *RepositoryImpl.kt + UserPreferencesRepositoryImpl (DataStore)
├── di/               — DatabaseModule, DataStoreModule, SecurityModule
├── domain/
│   ├── model/        — Transaction, Budget, Debt, SavingsAccount, RealEstateAsset,
│   │                   ScpiInvestment, AirbnbRental, PatrimonyOverview, Currency,
│   │                   Category, SubCategory, CustomSubCategory, DebtType, SavingsType,
│   │                   Child, UserPreferences, MonthlyReport, CategoryExpense,
│   │                   MonthlyVersement
│   ├── repository/   — 8 interfaces + UserPreferencesRepository + VersementRepository
│   └── usecase/      — 40+ UseCases (1 responsabilité = 1 UseCase)
└── presentation/
    ├── auth/         — LockScreen, AuthViewModel (PIN + email/password + biométrie)
    ├── dashboard/    — graphique 6 mois OU carte rapport selon toggle
    ├── budget/       — donut + bilan revenus/dépenses réels
    ├── expenses/     — liste + recherche + filtres + sous-catégories
    ├── investments/  — barres + SRP (SCPI, immobilier, Airbnb)
    ├── savings/      — SavingsScreen, SavingsViewModel (CRUD complet)
    ├── debts/        — DebtsScreen, DebtsViewModel
    ├── report/       — MonthlyReportScreen, MonthlyReportViewModel
    ├── settings/     — SettingsScreen (seuil, devise, toggle rapport, toggle nav)
    ├── common/       — CurrencyExt.kt, NotificationHelper.kt, BottomNavBar.kt
    ├── AppViewModel  — récurrentes + notifications au démarrage
    └── navigation/   — DibitaraNavGraph (8 routes), BottomNavBar (6 onglets)
```

| Composant | Bibliothèque | Statut |
|-----------|-------------|--------|
| UI | Jetpack Compose | ✅ En production |
| Navigation | Navigation Component | ✅ En production |
| Architecture | ViewModel + StateFlow | ✅ En production |
| Base de données locale | Room v7 | ✅ En production |
| Injection de dépendances | Hilt | ✅ En production |
| Async | Coroutines + Flow | ✅ En production |
| Graphiques | Vico (ou MPAndroidChart) | ✅ En production |
| Préférences | DataStore | ✅ En production |
| Sécurité auth | EncryptedSharedPreferences + PBKDF2 | ✅ En production |
| Biométrie | BiometricPrompt (récupération accès) | ✅ En production |
| Taux de change API | Frankfurter | ✅ En production |
| Tests UI | Espresso / Compose Test | ❌ Non implémenté |
| Tests unitaires | JUnit 5 + MockK | ✅ 123 tests |
| Couverture | Kover | ✅ Configuré — seuil 80% domain/, CI actif |
| Firebase Crashlytics | — | ✅ En production |

### 2.3 Cibles Android
| Paramètre | Valeur |
|-----------|--------|
| minSdkVersion | 26 (Android 8.0) |
| targetSdkVersion | 35 (Android 15) |
| compileSdkVersion | 35 |

### 2.4 Sécurité — État réel
- Authentification : PIN 4 chiffres + email/password PBKDF2 (EncryptedSharedPreferences) ✅
- Récupération par biométrie sans perte de données ✅
- Aucune donnée financière en clair dans les logs ✅
- Obfuscation R8/ProGuard en mode release ✅
- SQLCipher (chiffrement Room) — **non implémenté**, Room non chiffrée

> **Recommandation v3 :** Ajouter SQLCipher pour chiffrer la base Room. C'est la principale lacune sécurité restante.

---

## 3. Besoins non-techniques

### 3.1 Conception & UX — État réel
| Livrable | Outil | Statut |
|---------|-------|--------|
| Wireframes & maquettes UI | Figma (par Florent) | ✅ Réalisé — thème lanterne, conforme au cahier des charges |
| Charte graphique | Figma | ✅ Définie |
| Icône app 512×512 | Design Figma livré | ✅ Livré |
| Feature graphic 1024×500 | Design Figma livré | ✅ Livré |
| Screenshots Play Store | 7 sélectionnés | ✅ Prêts |

### 3.2 Organisation & Méthode — Ajustements constatés

Le plan initial prévoyait une organisation Agile stricte qui a été adaptée en pratique :

| Pratique | Plan initial | Réalité |
|----------|-------------|---------|
| Branches git | main / develop / feature/xxx / fix/xxx | ✅ Mis en place le 2026-05-18 : `develop` + `feature/xxx` → PR → `main` |
| Convention commits | Conventional Commits (feat:, fix:) | Style FR : sujet verbe complément |
| Code review | PR obligatoire | 0 approbation requise (solo dev) — CI doit passer |
| Versioning | Semantic Versioning strict | Semantic Versioning suivi (v1.0.0 → v3.0.1) |
| Suivi des tâches | GitHub Projects | AMELIORATIONS.md local + mémoire sessions |

**Workflow actuel (depuis Sprint 13) :**
```
feature/xxx  →  PR  →  develop  →  PR  →  main
```
- `main` protégé : PR obligatoire + CI `Lint + Tests + Build` requis + `enforce_admins=false`
- `develop` : push direct autorisé, point de départ de chaque feature branch

### 3.3 Compétences junior — Bilan
| Compétence | Sprint prévu | Réalisé |
|-----------|-------------|---------|
| Architecture MVVM | Sprint 1 | ✅ Sprint 1 |
| Room & migrations | Sprint 2 | ✅ Sprint 2–9 (4 migrations) |
| Coroutines & Flow | Sprint 2 | ✅ Sprint 2 |
| Jetpack Compose | Sprint 3 | ✅ Sprint 3 |
| Tests unitaires | Sprint 3 | ✅ 110 tests en place |
| Sécurité Android | Sprint 4 | ✅ Sprint 9 (PBKDF2, EncryptedSharedPreferences) |
| DataStore | Non prévu | ✅ Sprint 7 |
| Notifications Android | Non prévu | ✅ Sprint 6 |

---

## 4. Environnement de test & Gestion des régressions

### 4.1 Niveaux de test — État réel
```
Pyramide de tests (situation actuelle) :
        [E2E]          ← Non implémenté
      [Intégration]    ← Non implémenté
    [Tests Unitaires]  ← 110 tests (JUnit + MockK)
```

| Niveau | Outil | Cible | Réel |
|--------|-------|-------|------|
| Unitaires | JUnit + MockK | ≥ 80% sur `domain/` | 25 fichiers (123+ tests), Kover actif |
| Intégration | Room In-Memory | Repositories | ❌ Non implémenté |
| UI / E2E | Espresso + Compose Test | Parcours critiques | ❌ Non implémenté |

> **Note :** Kover configuré — seuil 80% domain/ actif en CI (Sprint 13). Priorité aux tests d'intégration Room avant la prochaine migration.

### 4.2 Appareils de test
| Type | Détail | Statut |
|------|--------|--------|
| Appareil physique | Android 16 | ✅ Tests réalisés (session 2026-05-10) |
| Émulateur | Non systématique | ⚠️ À formaliser |
| Firebase Test Lab | Cloud | ❌ Non utilisé |

### 4.3 CI/CD — État réel
- Build debug automatisé sur GitHub ✅
- Signing config conditionnelle (ne bloque pas le CI) ✅
- Tests unitaires automatisés en CI : ✅ 123 tests lancés à chaque push
- Rapport de couverture domain/ (seuil 80%) : ✅ Step CI actif
- Kover — mesure détaillée couverture : ✅ Configuré, seuil 80% domain/ (TECH-01 livré Sprint 13)
- Protection branche `main` : ✅ PR + CI requis depuis 2026-05-18

### 4.4 Parcours critiques (smoke tests manuels)
1. Lancement app → écran PIN → tableau de bord ✅ Validé v2.5.0
2. Ajout d'une dépense → apparition dans le budget mensuel ✅ Validé v2.5.0
3. Modification d'un budget → valeur mise à jour ✅ Validé (BUG-02 corrigé)
4. Consultation graphiques dashboard + rapport mensuel ✅ Validé v2.5.0
5. CRUD Épargne et Investissements (édition + suppression) ✅ Validé (BUG-03/04 corrigés)
6. Changement de devise EUR → USD → XOF ✅ Stockage + taux de change Frankfurter en temps réel
7. Sauvegarde et restauration des données ❌ Non couvert (pas de backup cloud)

---

## 5. Estimation des coûts

### 5.1 Coûts directs — Bilan réel
| Poste | Coût | Fréquence | Statut |
|-------|------|-----------|--------|
| Google Play Store | 25 € | Une fois | ✅ Payé |
| Figma | 0 € | — | Non utilisé |
| GitHub | 0 € | — | ✅ Actif |
| Firebase Crashlytics | 0 € | — | ✅ Actif en production |
| API Frankfurter | 0 € | — | ✅ En production |

> **Total coûts directs V1 : 25 € (réalisé)**

### 5.2 Coûts en temps — Bilan réel
| Phase | Durée estimée | Durée réelle |
|-------|--------------|--------------|
| Préparation & Setup | 1 semaine | ~1 semaine |
| Sprint 1 — Architecture & Auth | 2 semaines | ~2 semaines |
| Sprint 2 — Budget & Dépenses | 2 semaines | ~2 semaines |
| Sprint 3 — Investissements & Graphiques | 2 semaines | ~2 semaines |
| Sprint 4 — Récurrentes & Notifications prép. | 2 semaines | ~1 semaine |
| Sprint 5 — Recherche & Filtres | 2 semaines | ~1 semaine |
| Sprint 6 — Notifications | Non prévu | ~1 semaine |
| Sprint 7 — Préférences DataStore | Non prévu | ~1 semaine |
| Sprint 8 — Rapport mensuel | Non prévu | ~1 semaine |
| Sprint 9 — Auth avancée + CRUD + Play Store | Non prévu | ~1 semaine |
| **Total réel** | **~11 semaines** | **~13 semaines** |

---

## 6. Avancement des sprints

| Sprint | Titre | Statut | Version |
|--------|-------|--------|---------|
| Sprint 1 | Architecture + Navigation + Auth biométrique basique | ✅ Terminé | — |
| Sprint 2 | Budget, dépenses, patrimoine | ✅ Terminé | — |
| Sprint 3 | Graphiques, investissements, CI | ✅ Terminé | — |
| Sprint 4 | Transactions récurrentes, dettes | ✅ Terminé | — |
| Sprint 5 | Recherche & filtres | ✅ Terminé | — |
| Sprint 6 | Notifications (3 canaux) | ✅ Terminé | — |
| Sprint 7 | Préférences DataStore + SettingsScreen | ✅ Terminé | — |
| Sprint 8 | Rapport mensuel complet | ✅ Terminé | v1.0.0 |
| Sprint 9 | Auth PIN/password, CRUD Épargne/Invest, Dashboard liens, Play Store | ✅ Terminé | v2.5.0 |
| Sprint 10 | Corrections UX + CRUD Budget + Patrimoine détail + Responsive | ✅ Terminé | v3.0.0 |
| Sprint 11 | Versement mensuel Épargne & SCPI (migration v5→v6) | ✅ Terminé | v3.0.0 |
| Sprint 12 | Améliorations pré-déploiement v3.0.0 | ✅ Terminé | v3.0.0 |
| Sprint 12b | Correctifs CI — tests ViewModel + UseCase | ✅ Terminé | v3.0.1 |
| Sprint 13 | Qualité technique (Kover, Crashlytics, taux de change) | ✅ Terminé | v3.1.0 |
| Sprint 14 | IME complet, analyse RecurringExpenseTracker, budget interactif | ✅ Terminé | v3.1.0 |
| Sprint 15 | Suggestions de saisie rapide basées sur l'historique récent | ✅ Terminé | v3.2.0 |

---

## 7. Backlog post-v2.5.0

### 7.0 Items déjà livrés (post-Sprint 9)
| ID | Fonctionnalité | Statut | Version |
|----|---------------|--------|---------|
| FEAT-02 | Toggle Épargne/Investissements dans la navigation | ✅ Livré | v2.0.0 |
| AUTH-03 | 2FA TOTP complet RFC 6238, QR code | ✅ Livré | v2.1.0 |
| FEAT-01B | Sous-catégories personnalisées CRUD + migration v4→v5 | ✅ Livré | v2.2.0 |
| FEAT-05 | Logo Dibitara lanterne or/noir (mipmaps) | ✅ Livré | v2.3.0 |

---

### 7.1 Sprint 10 — Corrections & Améliorations UX ✅ v3.0.0

#### P0 — Quick wins
| ID | Fonctionnalité | Statut |
|----|---------------|--------|
| FIX-01 | Libellé "Salaire Florent" → "Salaire" | ✅ Livré |
| FIX-02 | Seuil notifications : valeur par défaut 200 € | ✅ Livré |

#### P1 — CRUD & Navigation
| ID | Fonctionnalité | Statut |
|----|---------------|--------|
| FEAT-07 | Budget : suppression (CRUD complet) | ✅ Livré |
| FEAT-08 | Dashboard → Patrimoine net & Brut → détails CRUD | ✅ Livré |
| FEAT-09 | Épargne : créer un type personnalisé depuis "Autres" (UI) | ✅ Livré |

#### P2 — UX & Libellés
| ID | Fonctionnalité | Statut |
|----|---------------|--------|
| FEAT-10 | Responsive : ModalBottomSheet + clavier IME | ✅ Livré |
| FEAT-11 | Revenus locatifs : libellé + champ source personnalisé | ✅ Livré |

---

### 7.2 Sprint 11 — Modèle Versement Mensuel ✅ v3.0.0

| ID | Fonctionnalité | Migration Room | Statut |
|----|---------------|----------------|--------|
| FEAT-12 | Versement mensuel non-rétroactif — Épargne & SCPI | ✅ v5→v6 | ✅ Livré |

**Règles métier FEAT-12 :**
- Solde Mois N = Solde Mois N-1 + Versement Mois N
- Modification du versement mensuel → **non rétroactive** : applicable à partir de Mois N+1 uniquement
- Dialogue de confirmation obligatoire avec la date du jour
- Un seul versement par mois par compte (contrainte UNIQUE en base)
- Applicable à : **Épargne ET Placements** (même modèle, deux écrans)

---

### 7.3 Sprint 12 — Améliorations pré-déploiement ✅ v3.0.0 / v3.0.1

| ID | Fonctionnalité | Statut |
|----|---------------|--------|
| SPRINT-12A | Auth simplifiée — email/password retiré de l'UI (PIN + biométrie only) | ✅ Livré |
| SPRINT-12B | Édition dépenses pré-remplie — tous champs + bouton Enregistrer actif dès ouverture | ✅ Livré |
| SPRINT-12C | Catégories restructurées : ABONNEMENTS promu, VACANCES→LOISIRS, AUTRE éclaté — migration Room v6→v7 | ✅ Livré |
| LOCALE-FIX | Parsing montants locale FR (virgule → point) sur tous les écrans | ✅ Livré |
| BIO-FIX | Biométrie — fallback PIN corrigé sur émulateur (ERROR_NO_BIOMETRICS → Cancelled) | ✅ Livré |
| DEVISE-DEF | Devise par défaut propagée depuis Paramètres vers 4 formulaires d'ajout | ✅ Livré |
| RENAME-TX | "Dépenses" → "Transactions" dans nav bar et dashboard | ✅ Livré |
| FIX-NAV | Navigation Accueil : retour Dashboard fiable depuis n'importe quelle profondeur | ✅ Livré |
| FIX-NOTIF | Notification "Fonds insuffisants" → deep link vers Paramètres (seuil d'alerte) | ✅ Livré |
| FIX-DATE | Confirmation versement : affiche la date du jour ("le 15 mai 2026") | ✅ Livré |
| FIX-ANNEE | Revenus locatifs : titre affiche l'année courante ("Revenus locatifs (2026)") | ✅ Livré |
| FEAT-ENFANT | Section enfant : associer/désassocier des comptes épargne via dialog multi-sélection | ✅ Livré |
| CI-FIX | Tests ViewModel (ucGetPreferences) + UseCase (getCustomSubCategories) — 123 tests CI verts | ✅ Livré v3.0.1 |

---

### 7.4 Sprint 13 — Qualité technique ✅ v3.1.0
| ID | Fonctionnalité | Effort | Priorité | Statut |
|----|---------------|--------|----------|--------|
| TECH-01 | Kover — mesure couverture détaillée + seuil CI | 4-6h | Moyen | ✅ Livré |
| TECH-02 | Firebase Crashlytics (free tier) | 2-3h | Moyen | ✅ Livré |
| FEAT-04 | Taux de change live (API Frankfurter EUR/USD/XOF) | 6-8h | Moyen | ✅ Livré |

**Workflow Sprint 13 :** `feature/sprint-13-tech` → PR → `develop` → PR → `main` — ✅ Exécuté.

### 7.5 Sprint 14 — Idées & Analyses (backlog identifié 2026-05-19)
| ID | Fonctionnalité | Effort | Priorité | Statut |
|----|---------------|--------|----------|--------|
| UX-02 | Clavier de saisie — couverture IME complète sur tous les écrans | 2-3h | Moyen | ✅ Livré |
| ANALYSE-01 | Étude RecurringExpenseTracker (DennisBauer) — go/no-go intégration | 1-2h | Analyse | ✅ NO-GO — voir ci-dessous |
| FEAT-BUDGET-INT | Budget — donut interactif : Autres→sous-catégories + onclick → transactions filtrées + revenus cliquables | 8-12h | Moyen | ✅ Livré |
| BUG-AUTH | Restauration du PIN développeur sur nouveau device (transfert D2D / backup) | 1h | Critique | ✅ Livré |

#### ANALYSE-01 — Résultat de l'étude (2026-05-19)

**Verdict : NO-GO pour l'intégration directe.**

Deux blocages rédhibitoires :
1. **Licence GPL-3.0** — toute réutilisation de code obligerait Dibitara à passer sous GPL, incompatible avec une app bancaire personnelle.
2. **Stack incompatible** — KMP + Koin + kotlinx-datetime vs Android-natif + Hilt + java.time ; migrer l'architecture entière serait disproportionné.

Fonctionnalités notables absentes de Dibitara (inspiration pour backlog v4) :
- Récurrences enrichies : Daily / Weekly / Yearly + `everyXRecurrence` (actuellement : mensuel uniquement)
- `firstPayment` + `endDate` sur les dépenses récurrentes
- Vue "Prochains paiements" — timeline des échéances à venir (**haute valeur, à intégrer au futur FEAT-BUDGET-INT**)
- Calcul d'équivalent mensuel (yearly/weekly → mensuel)

**Note versioning :** v3.0.1 est la référence stable. Patch = correctif mineur, Mineur = sprint fonctionnel, **Majeur (v4) = rupture schéma/architecture → validation requise avant incrément.**

### 7.7 Sprint 15 — Suggestions de saisie rapide ✅ v3.2.0
| ID | Fonctionnalité | Effort | Priorité | Statut |
|----|---------------|--------|----------|--------|
| FEAT-SUGGEST | Suggestions de saisie rapide basées sur l'historique récent | 5-8h | Moyen | 🔵 À faire |

**Contexte :** Quand l'utilisateur tape dans le champ libellé du formulaire d'ajout de transaction, l'app suggère les transactions fréquentes des 30 derniers jours qui correspondent. Un tap pré-remplit libellé + montant + catégorie d'un coup — sans setup manuel, l'app apprend de l'historique existant.

**Périmètre technique :**
- `TransactionSuggestion` — data class domain `(label, montantCents, devise, categorie, sousCategorie?)`
- `GetTransactionSuggestionsUseCase` — requête Room sur les 30 derniers jours, groupé par `(label normalisé, montant, catégorie)`, seuil ≥ 2 occurrences, trié par fréquence décroissante
- `ExpensesViewModel` — expose `StateFlow<List<TransactionSuggestion>>` réactif à la saisie du libellé
- UI — chips horizontaux sous le champ libellé dans `AddTransactionSheet` ; tap → pré-remplit tous les champs ; limite : top 5 suggestions

**Tests :**
- `GetTransactionSuggestionsUseCaseTest` — cas : 0 occurrence, 1 occurrence (sous le seuil → absent), ≥ 2 (présent), tri par fréquence

**Pas de migration Room — version cible : v3.2.0**

**Workflow Sprint 15 :** `feature/sprint-15-suggest` → PR → `develop` → PR → `main`.

### 7.6 Backlog V4 (long terme)
| ID | Fonctionnalité | Effort |
|----|---------------|--------|
| F8 | Export CSV des transactions | 8-12h |
| F9 | Sauvegarde cloud chiffrée | 15-20h |
| SEC-01 | SQLCipher — chiffrement Room | 8-12h |
| PERF-01 | Tests d'intégration Room | 10-15h |
| FEAT-RECUR | Récurrences enrichies : weekly/yearly + firstPayment + endDate + vue "Prochains paiements" | 10-15h |

---

## 8. Risques & Mitigations

| Risque | Probabilité | Impact | Mitigation | Statut |
|--------|------------|--------|------------|--------|
| Complexité des migrations Room | Moyen | Élevé | Tests de migration avant chaque sprint | ⚠️ 4 migrations faites, tests absents |
| Régression lors d'une 8e migration | Moyen | Élevé | Configurer tests Room In-Memory avant v7→v8 | 🟡 Room v7 stable, 7 migrations sans tests d'intégration |
| Taux de change indisponible | Faible | Moyen | Cache local des derniers taux | 🟡 API pas encore intégrée |
| Fuite de données sensibles | Faible | Très élevé | Room non chiffrée (SQLCipher absent) | 🟡 Acceptable V1, à traiter V3 |
| Rejet Play Store | Moyen | Élevé | Politique de confidentialité publiée, assets conformes | ✅ Mitigé |
| Dérive des délais | Moyen | Moyen | Backlog priorisé, scope V1 tenu | ✅ 2 semaines de dépassement acceptable |
| Drift du junior sur l'architecture | Faible | Moyen | Sessions pair-programming régulières | ✅ Architecture respectée |

---

## 9. Suivi de version du document

| Version | Date | Auteur | Changements |
|---------|------|--------|-------------|
| 1.0 | 2026-05-04 | Florent | Création initiale |
| 2.0 | 2026-05-11 | Florent | Mise à jour complète — état réel v2.5.0, sprints 1-9 terminés, backlog post-deploy, ajustements méthode de travail |
| 3.0 | 2026-05-15 | Florent | Backlog réorganisé — items post-Sprint 9 marqués livrés, Sprint 10 (8 items P0/P1/P2), Sprint 11 FEAT-12 versement mensuel |
| 3.1 | 2026-05-15 | Florent | Sprints 10-11-12 marqués terminés, Sprint 12 améliorations pré-déploiement (navigation, notifications deep link, versement date, année locatifs, enfant associer comptes), Sprint 13 qualité technique |
| 3.2 | 2026-05-18 | Florent | v3.0.1 — CI-FIX tests (123 tests), Sprint 12 chantiers A/B/C documentés, Room v7, workflow git develop+feature branches, protection main GitHub |
| 3.3 | 2026-05-19 | Florent | Sprint 14 backlog : UX-02 (IME), ANALYSE-01 (RecurringExpenseTracker), FEAT-BUDGET-INT (donut interactif) ; note convention versioning v3→v4 |
| 3.4 | 2026-05-19 | Florent | Sprint 14 : UX-02 livré, ANALYSE-01 terminé (NO-GO GPL-3.0 + KMP incompatible), backlog v4 enrichi FEAT-RECUR |
| 3.5 | 2026-05-19 | Florent | BUG-AUTH livré (preuve d'installation noBackupFilesDir), FEAT-BUDGET-INT marqué livré, Sprint 13 marqué terminé |
| 3.6 | 2026-05-20 | Florent | Mise à jour état réel — Sprints 13 + 14 marqués terminés, stack technique corrigée (Kover/Crashlytics/Frankfurter ✅), version 3.1.0, F8/F9 backlog V4 |
| 3.7 | 2026-05-20 | Florent | Sprint 15 défini — FEAT-SUGGEST suggestions de saisie rapide (5-8h, v3.2.0, sans migration Room) |
| 3.8 | 2026-05-20 | Florent | Sprint 15 marqué terminé — PR #7 mergée, v3.2.0 |

---

*Ce document est vivant. Il doit être mis à jour à chaque fin de sprint.*
