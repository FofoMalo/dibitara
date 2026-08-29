# Cadrage — Objectifs d'épargne connectés au flux d'argent

> Statut : **CADRAGE** (rien d'implémenté). En attente d'arbitrage priorité + go.
> Branche cible : `florent/prive` (les objectifs vivent ici, Room v25).
> Date : 2026-08-29
> Room : **une seule** migration v25 → v26 pour tout le sprint (pas une par feature).

---

## 1. Contexte

Refonte de l'écran Épargne, Phases 1 et 2 livrées. Les **objectifs d'épargne**
(§3) sont un `SavingsGoal(name, targetAmountCents, currentAmountCents, targetDate,
monthlyContributionCents, currency, colorKey, iconKey)`.

Deux limites constatées à l'usage :

1. **`currentAmountCents` est saisi à la main.** Pour faire avancer un objectif il
   faut ⋮ → Modifier → changer le montant → Enregistrer, chaque mois. La
   `monthlyContributionCents` n'est qu'une intention affichée, rien ne l'applique.
   → traité par le **lot « capture du versement »** (voir §6), Approche A :
   `CompteType.OBJECTIF` sur `monthly_versements`, zéro migration.

2. **Objectif et argent réel restent totalement découplés.** Verser 400 € vers
   l'objectif « Voiture » ne bouge rien sur les comptes ; enregistrer un versement
   sur un compte ne bouge rien sur les objectifs. Symptôme concret chez Florent :
   le compte épargne **« Epargne Cesar »** (solde 0 €, +17 €/mois) **et** l'objectif
   **« Cesar »** coexistent — deux entités maintenues à la main pour une seule
   réalité.

Ce sprint traite la limite 2.

## 2. Objectif & critères de succès

**Objectif :** relier un `SavingsGoal` à sa source de financement réelle, pour
qu'un seul geste — ou aucun — fasse avancer la progression, sans double saisie ni
dérive des montants.

**Critères de succès vérifiables :**

1. Un objectif peut être lié à un compte épargne existant ; sa progression suit
   alors ce compte (solde ou versements, selon le mode) → test unitaire du calcul
   de progression par mode.
2. Enregistrer le « Versement du mois » d'un compte lié crédite l'objectif
   (automatiquement ou sur proposition) → test ViewModel.
3. Objectif en XOF alimenté par un compte en EUR (cas réel « Projet Burkina ») :
   la progression et le % s'affichent convertis et cohérents → test avec
   `CurrencyConverter`.
4. Délier une source ne perd pas le montant déjà acquis → test.
5. Les objectifs **non liés** continuent de fonctionner en saisie manuelle,
   aucune régression → tests Phase 2 toujours verts.
6. Une seule migration Room v25 → v26 pour l'ensemble des colonnes du sprint.

## 3. Features candidates (cluster)

Socle = **F1 + F2**. Les autres sont indépendantes et activables séparément.

| #  | Feature | Dépend de | Room v26 | Maille |
|----|---------|-----------|----------|--------|
| F1 | Objectif ↔ compte source (1:1) : `SavingsGoal.sourceAccountId: Long?` + `fundingMode` | — | 2 colonnes | ~1,5 j |
| F2 | Progression dérivée selon `fundingMode` : `MANUEL` / `VERSEMENTS` / `SOLDE_COMPTE` | F1 | (inclus F1) | (inclus) |
| F3 | « Versement du mois » d'un compte lié → crédite l'objectif (auto ou proposé) | F1 | — | ~0,5 j |
| F4 | Répartir un versement compte entre N objectifs (« 400 € = 250 Voiture + 150 Vacances ») | F3 | à étudier (réutiliser `monthly_versements` ou table de répartition) | ~1 j |
| F5 | Mensualité cible calculée (inverse de `ProjeterObjectifUseCase` : « verse +520 €/mois pour tenir juin 2027 ») | — | — | ~0,5 j |
| F6 | Objectif rattaché à un enfant (`SavingsGoal.childId`, comme `SavingsAccount`) | — | 1 colonne | ~0,5 j |
| F7 | Alerte « objectif en retard » : puce Dashboard + notification mensuelle (WorkManager déjà en place) | — | garde-fou fréquence (`derniereAlerteObjectifEpochDay`, cf. bug alertes répétées) | ~1 j |
| F8 | Clôture d'un objectif atteint : archiver / transférer le montant vers un compte / relancer | — | `statut` ou `archivedAtEpochDay` | ~1 j |
| F9 | Objectif comme réserve budgétaire mensuelle (lien `CategoryEnvelope` / reco 50/30/20 du Sprint 41) | — | à étudier | ~1 j |

## 4. Décisions d'architecture à trancher

- **`fundingMode` sur `SavingsGoal`** (enum stockée en TEXT, `safeValueOf`) :
  - `MANUEL` — comportement actuel, `currentAmountCents` stocké et saisi.
  - `VERSEMENTS` — `currentAmountCents` = seed initial + Σ des versements
    `CompteType.OBJECTIF` (c'est le lot « capture du versement », §6).
  - `SOLDE_COMPTE` — `currentAmountCents` = solde du compte lié (converti si
    devise différente), en lecture seule.
- **Migration groupée** : `sourceAccountId`, `childId`, `fundingMode`, `statut` —
  une seule `MIGRATION_25_26`, un seul `26.json`, un seul test de migration.
- **Devise** : objectif et compte source peuvent différer. Montants stockés dans
  leur devise d'origine (convention projet), conversion à l'affichage via
  `CurrencyConverter` (pivot EUR, XOF/XAF à parité).
- **Doublon compte/objectif** : proposer une action explicite (« lier ce compte à
  un objectif » / « convertir en objectif ») plutôt qu'une fusion automatique.
- **Sauvegarde JSON** : les nouvelles colonnes de `savings_goals` suivent
  automatiquement (`SavingsGoalEntity` est déjà dans l'export/restore + dans
  `TABLES_A_VIDER`). F4, si elle crée une table, refait la danse à 6 points.

## 5. Questions ouvertes

- **Multi-source** par objectif (F4 le suppose) ou une source maximum ?
- **Déliaison d'une source** : on fige `currentAmountCents` à la valeur courante et
  on repasse en `MANUEL` ? ou on garde le dernier montant connu ?
- En mode `SOLDE_COMPTE`, le bouton « Verser ce mois » de l'objectif **disparaît**
  (redondant avec le versement du compte) ou **reste** pour un apport exceptionnel ?
- **F7** : quel retard déclenche l'alerte (dès 1 mois ? seulement si le retard
  s'aggrave ?) et à quelle fréquence (mensuelle, comme fonds/budget/dettes) ?
- **Rattrapage** des mois manqués sur le versement d'objectif : 1 tap = 1 mois
  (comme `appliquerVersement` pour les comptes) ou option « rattraper N mois » ?
  *(question déjà posée pour le lot §6, réponse commune)*

## 6. Lot « capture du versement » — prérequis, à faire avant ce sprint

Petit lot (~0,5 j), **Approche A** retenue :

- `CompteType` gagne `OBJECTIF`. **Aucune migration** (`account_type` est un TEXT
  libre lu en `safeValueOf`, même principe que `Category`).
- Bouton « Verser ce mois (+X) » sur l'`ObjectifCard` → `existeVersementMois(goal.id,
  OBJECTIF, y, m)` → `saveVersement(...)` + `upsertSavingsGoal(currentAmountCents += …)`.
- Rappel in-app doré sur l'`ObjectifCard` (jumeau de `GetVersementsEnAttenteUseCase`
  livré pour les comptes).
- Historique gratuit via `MonthlyVersementDao.getForAccount(goalId, OBJECTIF)`.
- `versements_mensuels` déjà dans la sauvegarde JSON → rien à ajouter.

Ce lot **n'est pas un cul-de-sac** : il devient le mode `fundingMode = VERSEMENTS`
du sprint. Le piège double-comptage sur restauration (backup sans
`versements_mensuels` → table vide → rappels tous allumés) s'applique aussi ici,
même parade à cadrer.

## 7. Hors scope

- Objectifs partagés multi-utilisateurs (app mono-utilisateur, cf. chemin 1).
- Objectif indexé sur un actif (immo, SCPI) au lieu du cash.

## 8. Estimation grosse maille

Socle F1 + F2 ≈ 2 j. Chaque feature F3–F9 ≈ 0,5–1 j, indépendante. Sprint réaliste
= socle + 3–4 features = **~5–6 j**. Lot §6 (capture) ≈ 0,5 j, à part et avant.
