# Cadrage — Mouvements de capital sur SCPI et Épargne salariale

> Statut : **LIVRÉ** (2026-08-30). G1 + G2 + G3 implémentés tels que cadrés
> ci-dessous (voies `SCPI_MOUVEMENT`/`EMPLOYEE_SAVINGS_MOUVEMENT`, delta de
> parts pour SCPI §3, pattern live-sync repris tel quel §3). G4 (composable
> partagé) non fait, reste un nettoyage optionnel.
> Branche cible : `florent/prive`. Room : **aucune migration** (voir §4).
> Date : 2026-08-30
> Fait suite au Sprint « Mouvements de capital sur les actifs libres et
> l'immobilier » (v4.4.1, livré) : `CompteType.CUSTOM_ASSET`/`REAL_ESTATE`,
> `EnregistrerMouvementCapitalUseCase`, case à cocher + champ signé dans
> `EditCustomAssetSheet`/`EditRealEstateSheet`.

---

## 1. Contexte

Le taux « Acquisition → Aujourd'hui » (`AcquisitionEvolutionBlock`,
`CalculerPerformanceActifUseCase`) compte tout écart entre la valeur
d'acquisition et la valeur actuelle d'un actif comme de la performance de
marché. Or une édition manuelle de la valeur peut aussi représenter un
mouvement de capital (apport ou retrait) qui n'a rien à voir avec le marché.
Le Sprint précédent a corrigé ça pour les actifs libres (ex. CTO, retrait
constaté par Florent) et l'immobilier (ex. travaux qui font remonter la
valeur affichée).

**Le même angle mort existe sur SCPI et Épargne salariale** :

- `EditScpiSheet` — `sharesCount` et `shareValueCents` sont des champs libres,
  éditables indépendamment du bouton « Appliquer versement ».
- `EditEmployeeSavingsSheet` — `currentBalanceCents` est un champ libre, même
  chose.

Si Florent corrige manuellement l'un de ces champs (relevé du gestionnaire,
apport ponctuel hors mensualité), c'est compté à 100 % comme performance.

**Ce n'est pas hypothétique pour l'épargne salariale** : le KDoc de
`appliquerVersementEmployeeSavings` (InvestmentsViewModel.kt:506-513)
documente déjà un risque de double-comptage si Florent re-saisit le solde à
la main après avoir aussi appliqué l'abondement du mois — confirmé sur
device (mémoire de session, bug solde versement épargne salariale/SCPI,
2026-08-21). Ce sprint ne corrige pas ce double-comptage-là (bug distinct,
déjà tracé), mais le champ mouvement de capital est un outil que Florent
peut réutiliser pour le documenter correctement au lieu de laisser un écart
muet dans le taux.

## 2. La différence avec le sprint précédent : collision avec le versement mensuel

CTO et Immobilier n'ont **aucun** versement mensuel récurrent existant.
SCPI et Épargne salariale, si : `appliquerVersementScpi` /
`appliquerVersementEmployeeSavings` gardent avec `ucExisteVersementMois` et
insèrent une ligne `monthly_versements` clé (account_id, account_type,
year, month) — contrainte UNIQUE, un seul enregistrement par mois.

Réutiliser tel quel `CompteType.SCPI` / `CompteType.EMPLOYEE_SAVINGS` pour
un mouvement de capital ad hoc écrirait dans **la même ligne** que ce
bouton : soit ça la remplace (perte du montant réellement versé), soit
`EnregistrerMouvementCapitalUseCase` la cumule (comportement voulu pour le
CTO) et alors le prochain clic sur « Appliquer versement » ce mois-ci
échoue avec « Versement déjà enregistré », alors qu'aucun versement mensuel
n'a encore été fait. Dans les deux cas, c'est cassé.

**Décision proposée : une deuxième "voie" (`CompteType`) dédiée au
mouvement ad hoc, distincte de la voie du versement mensuel régulier**,
pour le même compte :

- `CompteType.SCPI_MOUVEMENT`
- `CompteType.EMPLOYEE_SAVINGS_MOUVEMENT`

Aucune migration (même principe que les valeurs existantes — `account_type`
est un `TEXT` libre lu en `safeValueOf`). `SommeVersementsDepuisUseCase` est
appelé deux fois dans `performanceDepuisAcquisition` — une fois par voie —
et les deux totaux s'additionnent :

```kotlin
private val mouvementLane = mapOf(
    CompteType.SCPI to CompteType.SCPI_MOUVEMENT,
    CompteType.EMPLOYEE_SAVINGS to CompteType.EMPLOYEE_SAVINGS_MOUVEMENT
)

suspend fun performanceDepuisAcquisition(...): PerformanceActif? {
    val versementsCumules = versementCompteType?.let { type ->
        val versementRecurrent = ucSommeVersementsDepuis(accountId, type, acquisitionDate)
        val mouvementAdHoc = mouvementLane[type]?.let { ucSommeVersementsDepuis(accountId, it, acquisitionDate) } ?: 0L
        versementRecurrent + mouvementAdHoc
    } ?: 0L
    return ucCalculerPerformanceActif(acquisitionValueCents, currentValueCents, versementsCumules)
}
```

Alternative écartée : ajouter une colonne `kind` (VERSEMENT_MENSUEL /
MOUVEMENT_PONCTUEL) à `monthly_versements` pour distinguer les deux sans
dupliquer `CompteType`. Rejetée pour ce sprint : migration Room pour un
problème que la voie séparée résout à coût zéro ; à reconsidérer seulement
si le nombre de voies dédoublées grossit au point de devenir illisible.

## 3. Nuance SCPI : parts vs prix de la part

Pour le CTO/Immobilier/Épargne salariale, la valeur éditée est un scalaire
unique — le delta avant/après *est* le mouvement de capital.

Pour SCPI, `totalValueCents = sharesCount × shareValueCents`
(`ScpiInvestment.kt:34`) est le produit de deux champs éditables
indépendamment :

- `sharesCount` qui change = un vrai mouvement de capital (achat/vente de
  parts hors mensualité).
- `shareValueCents` qui change = une révision du prix de la part par le
  gestionnaire — c'est de la performance de marché, pas un mouvement.

**Un simple delta sur `totalValueCents` mélangerait les deux** : corriger
le prix de la part avec la case cochée neutraliserait à tort une vraie
plus-value. La case à cocher de `EditScpiSheet` doit donc calculer le
mouvement à partir du **delta de parts uniquement**, valorisé au prix
actuel :

```kotlin
val diffParts = nouveauSharesCount - scpi.sharesCount
val montantMouvementSuggere = (diffParts * nouveauShareValueCents).roundToLong()
```

Le champ reste éditable (comme pour CTO/Immobilier), donc Florent peut
corriger la suggestion si besoin.

**Piège UX déjà rencontré sur CTO/Immobilier, à ne pas reproduire** : la
première version calculait la suggestion une seule fois, au moment où la
case est cochée (`onCheckedChange`). Si Florent coche la case *avant* de
taper la nouvelle valeur (ordre naturel), le montant restait figé à 0 et
rien n'était enregistré — repéré sur le bien "Appart Rue Ecuyère", corrigé
en `LaunchedEffect(value, mouvementCapital)` qui resynchronise la
suggestion tant qu'un flag `mouvementAmountModifieManuellement` (mis à
`true` uniquement quand Florent tape lui-même dans le champ) reste à
`false`. **G1/G2 doivent reprendre ce pattern tel quel**, y compris pour
SCPI où c'est `diffParts` (§3) qui doit rester synchronisé, pas le delta de
valeur totale.

## 4. Critères de succès vérifiables

1. Un apport/retrait de parts SCPI hors versement mensuel (case cochée) est
   neutralisé du taux d'évolution → test `CalculerPerformanceActifUseCase`
   ou `InvestmentsViewModel`.
2. Une révision du prix de la part SCPI (case décochée, ou cochée mais
   `diffParts == 0`) reste comptée en performance → test dédié, c'est le
   garde-fou de la nuance §3.
3. Une correction manuelle du solde épargne salariale hors abondement (case
   cochée) est neutralisée → test.
4. Enregistrer un mouvement de capital SCPI/épargne salariale le même mois
   qu'un « Appliquer versement » n'écrase ni ne bloque ce dernier, et
   inversement → test d'intégration ViewModel vérifiant les deux lignes
   `monthly_versements` (voies différentes) coexistent.
5. Aucune régression sur `appliquerVersementScpi`/
   `appliquerVersementEmployeeSavings` ni sur le CTO/Immobilier déjà livrés
   → suite de tests existante toujours verte + `koverVerify`.
6. Aucune migration Room.

## 5. Features candidates (cluster)

| # | Feature | Dépend de | Maille |
|---|---------|-----------|--------|
| G1 | `CompteType.SCPI_MOUVEMENT` + case à cocher/champ signé `EditScpiSheet` (delta de parts, §3) | — | ~0,5 j |
| G2 | `CompteType.EMPLOYEE_SAVINGS_MOUVEMENT` + case à cocher/champ signé `EditEmployeeSavingsSheet` | — | ~0,5 j |
| G3 | `performanceDepuisAcquisition` : somme des deux voies par type (§2) | G1, G2 | ~0,5 j (inclus dans G1/G2) |
| G4 | Extraire un composable partagé `MouvementCapitalField` (case + champ signé) — dupliqué 2× aujourd'hui (CustomAsset, RealEstate), 4× après ce sprint | — | ~0,5 j, cosmétique, non bloquant |

G1 + G2 (+ G3 incluse) = le sprint. G4 est un nettoyage, à faire seulement
si le temps le permet — pas un prérequis fonctionnel.

## 6. Questions ouvertes

- **§3 confirmé ?** Le delta-de-parts-valorisé-au-prix-actuel est le calcul
  proposé par défaut pour SCPI — à valider avant implémentation, c'est le
  seul point du sprint qui n'est pas un simple copier-coller du mécanisme
  CTO/Immobilier.
- **Double-comptage épargne salariale (KDoc `appliquerVersementEmployeeSavings`)** :
  ce sprint ne le corrige pas, seulement le taux d'évolution. Faut-il un
  sprint dédié à ce garde-fou (bloquer/avertir si le solde semble déjà à
  jour), séparé de celui-ci ?
- **Nommage des voies** : `SCPI_MOUVEMENT`/`EMPLOYEE_SAVINGS_MOUVEMENT`
  retenu par défaut (cohérent avec les noms `CompteType` existants) — à
  confirmer, ou un préfixe différent si Florent préfère.

## 7. Hors scope

- Le double-comptage abondement/solde manuel de l'épargne salariale (bug
  distinct, déjà tracé en mémoire).
- Le badge de tendance à 12 mois (`CalculerTendanceActifUseCase`) — aucune
  neutralisation pour aucun type, mais ne s'affiche que quand il n'y a pas
  de valeur d'acquisition renseignée (mentionné pour mémoire dans l'échange
  qui a mené à ce cadrage, pas prioritaire).
- Toute automatisation qui déduirait seule si une édition est un mouvement
  de capital ou une performance — reste une saisie explicite de Florent.

## 8. Estimation grosse maille

G1 + G2 + G3 ≈ **1,5 j**. G4 (nettoyage) ≈ 0,5 j en option.
