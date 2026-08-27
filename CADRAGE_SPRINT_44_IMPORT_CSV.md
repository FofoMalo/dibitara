# Cadrage Sprint 44 — Import CSV bancaire générique

> Statut : **CADRAGE — en attente du go de Florent**
> Branche cible : `develop-catchup` (branche générique)
> Date : 2026-08-27
> Room : **aucune migration** (colonnes `importSource` / `externalId` / `bankAccountId` déjà présentes, v11 / v22)

---

## 1. Contexte

La couche d'import bancaire historique (BRED CSV/PDF/notification, TradeRepublic)
était **calibrée sur les relevés réels de Florent** : `BredCsvParser` connaissait
les 3 formats d'export de bred.fr, `BredCategoriseur` ses libellés, le
`BredNotificationListenerService` le texte des notifications push de l'app BRED.
Rien de tout ça n'est utilisable par un autre utilisateur.

Cette couche a été retirée de `develop-catchup` le 2026-08-27 (commit `01d468d`)
pour ne garder que le générique.

**Conséquence : un utilisateur lambda n'a aujourd'hui aucun moyen de faire
entrer son historique bancaire dans Dibitara.** Deux seules portes d'entrée :
- saisie manuelle transaction par transaction ;
- restauration d'une sauvegarde JSON Dibitara — qui n'existe que s'il a déjà
  utilisé l'app (Sprint 42).

Ce sprint comble ce trou avec un **import CSV générique**, indépendant de toute
banque.

## 2. Objectif & critères de succès

**Objectif :** permettre à n'importe quel utilisateur d'importer l'historique
de transactions exporté depuis sa banque (ou son tableur) au format CSV, en
mappant lui-même les colonnes si l'auto-détection échoue.

**Critères de succès vérifiables :**

1. Depuis *Paramètres → Données*, un bouton « Importer un relevé CSV » ouvre le
   sélecteur de fichiers (SAF).
2. Un CSV « propre » (en-têtes reconnus, 1 colonne montant signée, date
   `dd/MM/yyyy` ou `yyyy-MM-dd`) est importé **sans passer par l'écran de
   mapping** → test instrumenté + test unitaire du parseur.
3. Un CSV « exotique » (délimiteur `,`, colonnes Débit/Crédit séparées, date
   `MM/dd/yyyy`, séparateur décimal `.`) est importable **après mapping manuel**
   → test unitaire du parseur avec `ColumnMapping` explicite.
4. L'aperçu affiche, avant toute écriture : nombre de nouvelles / doublons /
   lignes ignorées, catégorie suggérée par ligne, compte bancaire cible.
5. Réimporter deux fois le même fichier n'insère **rien** la 2ᵉ fois
   (dédup par `externalId`) → test unitaire de l'use case.
6. `./gradlew test koverVerify` vert ; couverture `domain/` ≥ 80 %.
7. Validé sur téléphone physique avec **au moins 2 exports de banques
   différentes** (Florent fournit les fichiers, anonymisés si besoin).

## 3. Décisions actées (Florent, 2026-08-27)

| Décision | Choix retenu | Raison |
|---|---|---|
| Formats de fichier | **CSV uniquement**, pas de `.xlsx` natif | Parser xlsx = Apache POI (~10-15 Mo, method count). Tout tableur exporte en CSV. Guidage in-app pour les utilisateurs Excel. |
| Dédup sans identifiant bancaire | **Hash `externalId` accepté tel quel**, tradeoff assumé | Deux transactions vraiment jumelles le même jour (2 cafés à 2 €) fusionnent en une. Filet de sécurité : écran « Nettoyer les doublons » existant. BRED Sprint 30 avait la même limite. Pas d'index intra-fichier. |
| Écran de mapping | **Optionnel** — auto-détection d'abord, mapping en secours / sur demande | Chemin en un tap pour un fichier bien formé, fallback pour les formats tordus. |
| Cadrage | Doc écrit **avant** implémentation (ce fichier) | — |

## 4. Périmètre

### Dans le périmètre

- Parseur CSV générique (Kotlin pur, `data/importcsv/`).
- Auto-détection : délimiteur, encodage, présence d'en-tête, format de date,
  séparateur décimal, colonnes par nom d'en-tête connu.
- Modèle `ColumnMapping` (quelle colonne = quel champ).
- Écran de mapping Compose (affiché seulement si auto-détection incomplète ou
  si l'utilisateur clique « Ajuster les colonnes »).
- Écran d'aperçu (réutilise le pattern preview/confirm en 2 phases).
- Catégorie suggérée : règles apprises puis dictionnaire générique
  (`CategorizationRuleRepository.getRuleForNote` → `CategoriseurLibelle.suggererCategorie`).
- Dédup par `externalId` (hash).
- Affectation à un compte bancaire existant (dropdown, ou « aucun »).
- Mise à jour de `UserPreferences.derniereImportEpochMilli` (réactive la ligne
  « Dernier import » aujourd'hui morte).
- Guidage : texte « Comment exporter un CSV depuis votre banque / Excel ».

### Hors périmètre (à ne pas construire)

- `.xlsx`, `.ods`, PDF, OFX/QIF.
- Profils de banque sauvegardés (« se souvenir du format de la Société
  Générale ») — spéculatif, à revoir si le besoin émerge.
- Conversion de devise à l'import (on stocke le montant tel quel avec sa
  devise ; la conversion reste dans la couche `domain` à l'affichage).
- Création automatique de compte bancaire depuis l'import.
- Import de catégories / budgets / autre chose que des transactions.
- Réconciliation avec une capture live (spécifique BRED, supprimée).

## 5. Architecture

Flux Clean Architecture inchangé : `UI → ViewModel → UseCase → Repository → DAO`.

### Fichiers à créer

| Couche | Fichier | Rôle |
|---|---|---|
| domain/model | `ImportedTransaction.kt` | **Réintroduit** en version générique (sans `rawType` bank-specific, `importSource = "csv"`). Méthode `toTransaction(bankAccountId)`. |
| domain/model | `CsvColumnMapping.kt` | Index de colonne pour chaque champ + options (format date, séparateur décimal, mode montant, ligne d'en-tête). Data class pure. |
| domain/model | `CsvImportPreview.kt` | Résultat d'analyse : colonnes détectées, échantillon de lignes, mapping deviné, liste `ImportedTransaction`, compteurs. |
| domain/repository | `ImportRepository.kt` | **Réintroduit**, allégé : `externalIdsExistants()`, `importerTransactions(List<Transaction>): Int`. (On retire `trouverCaptureLiveProche` / `mettreAJour` — spécifiques BRED.) |
| domain/usecase | `AnalyserCsvUseCase.kt` | Lit l'`InputStream`, auto-détecte, applique le mapping (deviné ou fourni), retourne `CsvImportPreview`. Ne touche pas la base. |
| domain/usecase | `ImporterTransactionsCsvUseCase.kt` | 2 phases : `verifierDoublons()` (enrichit `alreadyImported`) puis `confirmer()` (insère les nouvelles, met à jour `derniereImport`). |
| data/importcsv | `CsvParser.kt` | Parsing bas niveau : `parseRow` (RFC 4180), détection délimiteur/encodage. **Récupère ~120 lignes de l'ex-`BredCsvParser`** (`git show 01d468d~1:app/src/main/java/com/dibitara/app/data/importcsv/BredCsvParser.kt`). |
| data/importcsv | `CsvColumnDetector.kt` | Devine le mapping à partir des en-têtes (`Date`, `Montant`, `Libellé`, `Amount`, `Description`, `Debit`, `Credit`, `Débit`, `Crédit`…) et du contenu des 1res lignes (format date, séparateur décimal). Réutilise `BredCategoriseur.normaliser` (majuscules + suppression accents NFD) — **à extraire** dans un util partagé, ex. `data/importcsv/TexteNormalisation.kt`. |
| data/importcsv | `MontantParser.kt` | Parse un montant FR/EN (`1 234,56` / `1,234.56` / `-120.50` / tiret unicode). Récupéré de `parseMontantFr`. |
| data/repository | `ImportRepositoryImpl.kt` | **Réintroduit**, allégé : s'appuie sur `TransactionDao` existant. |

**Pas de nouveau `SuggererCategorieImportUseCase`.** La cascade « règle apprise
→ dictionnaire » existe déjà dans `GetRecategorizationSuggestionsUseCase.trouverSuggestion`
(enveloppée dans un `Flow` sur 90 jours de transactions). On **extrait la cascade
interne** dans un helper partagé (`internal object SuggestionCategorie` ou fonction
`internal`) que les deux appelants utilisent — plus petit qu'un UseCase, et évite
deux copies de la logique de priorité qui divergeraient.
| presentation/importcsv | `ImportCsvScreen.kt` | Point d'entrée : choix fichier → route vers mapping ou aperçu. |
| presentation/importcsv | `ImportCsvMappingScreen.kt` | Tableau d'aperçu + dropdowns de mapping. Affiché conditionnellement. |
| presentation/importcsv | `ImportCsvViewModel.kt` | État : `Analyse`, `Mapping`, `Apercu`, `Confirmation`, `Termine`, `Erreur`. |

### Fichiers à modifier

| Fichier | Modification |
|---|---|
| `di/DatabaseModule.kt` | `@Binds bindImportRepository(ImportRepositoryImpl): ImportRepository` (re-ajout). |
| `presentation/navigation/DibitaraNavGraph.kt` | `Screen.ImportCsv` + `composable` + câblage `onNavigateToImportCsv` depuis `SettingsScreen`. |
| `presentation/settings/SettingsScreen.kt` | Bouton « Importer un relevé CSV » dans la section Données + param `onNavigateToImportCsv`. |

### Nettoyage à faire au passage (code orphelin depuis `01d468d`)

- `TransactionDao.findByAmountDateRangeAndSource` — n'était utilisé que par
  l'ex-`ImportRepositoryImpl` (réconciliation capture live BRED). À supprimer.
- `UserPreferencesRepository.updateDerniereImport` — n'avait plus d'appelant ;
  ce sprint lui en redonne un, donc **à conserver**.

## 6. Le parseur générique

### 6.0 Lecture en deux passes

Le fichier est lu **deux fois** (l'`InputStream` SAF n'étant pas rewindable, on
rouvre l'`Uri` via `contentResolver.openInputStream` pour la 2ᵉ passe) :

1. **Passe de détection** — `bufferedReader().useLines { it.take(15).toList() }` :
   un échantillon borné pour §6.1 (encodage, délimiteur, en-tête, format date,
   séparateur décimal, mapping des colonnes).
2. **Passe de données** — `bufferedReader().useLines { seq -> seq.drop(if header 1 else 0).mapNotNull(::parseLigne) }` :
   traitement ligne à ligne, **jamais** de `readText()` global sur le corps du
   fichier.

L'aperçu (`CsvImportPreview.echantillon`) réutilise les 5 premières lignes de
données de la passe 1 — pas de 3ᵉ lecture.

### 6.1 Auto-détection (dans l'ordre)

1. **Encodage** : lecture UTF-8, fallback ISO-8859-1 si `MalformedInput`
   (logique existante de l'ex-`BredCsvParser`). Le fallback impose de relire
   l'`Uri` avec l'autre charset — d'où l'intérêt de garder la passe 1 courte.
2. **Délimiteur** : sur les 10 premières lignes non vides, compte `;`, `,`,
   `\t` ; retient celui dont le nombre d'occurrences par ligne est le plus
   stable et > 0.
3. **Ligne d'en-tête** : présente si la 1re ligne ne contient **aucune** valeur
   parseable en date ou en montant.
4. **Format de date** : essaie dans l'ordre `dd/MM/yyyy`, `yyyy-MM-dd`,
   `dd.MM.yyyy`, `dd-MM-yyyy`, `MM/dd/yyyy` sur la 1re valeur de la colonne
   date détectée ; retient le premier qui parse toutes les 5 premières valeurs.
5. **Séparateur décimal** : si la colonne montant contient `,` suivi d'exactement
   2 chiffres en fin → décimale FR ; sinon `.`.
6. **Colonnes** : correspondance insensible casse/accents entre en-têtes et
   dictionnaire de synonymes :
   - date → `date`, `date opération`, `date de l'opération`, `date valeur`, `booking date`
   - montant signé → `montant`, `amount`, `valeur`
   - débit → `débit`, `debit`, `retrait`, `sortie`
   - crédit → `crédit`, `credit`, `versement`, `entrée`
   - libellé → `libellé`, `libelle`, `description`, `nom de l'opération`, `détail`, `motif`, `nature`
   - devise → `devise`, `currency`, `monnaie`

   Si `débit` **et** `crédit` détectés → mode « deux colonnes ». Sinon mode
   « colonne signée ».

Si l'étape 6 ne trouve pas au minimum {date, (montant signé | débit/crédit),
libellé} → l'écran de mapping s'ouvre, pré-rempli avec ce qui a été deviné.

### 6.2 `CsvColumnMapping`

```kotlin
data class CsvColumnMapping(
    val hasHeaderRow: Boolean,
    val delimiter: Char,
    val dateColumn: Int,
    val dateFormat: String,             // pattern DateTimeFormatter
    val amountMode: AmountMode,          // SIGNED_SINGLE | DEBIT_CREDIT
    val amountColumn: Int? = null,       // si SIGNED_SINGLE
    val debitColumn: Int? = null,        // si DEBIT_CREDIT
    val creditColumn: Int? = null,       // si DEBIT_CREDIT
    val decimalSeparator: Char,          // ',' ou '.'
    val labelColumns: List<Int>,         // concaténées avec ' ' si plusieurs
    val currencyColumn: Int? = null,     // sinon devise par défaut utilisateur
)
```

### 6.3 Règles de conversion d'une ligne → `ImportedTransaction`

| Champ | Règle |
|---|---|
| `date` | parse via `dateFormat` ; ligne ignorée si échec |
| `amountCents` | `abs((montant * 100).roundToLong())` — **`roundToLong` obligatoire** (piège IEEE754, cf. `f86316f`) |
| `type` | mode signé : `montant >= 0 → INCOME` sinon `EXPENSE`. Mode débit/crédit : crédit non vide → `INCOME`, débit non vide → `EXPENSE` |
| `currency` | colonne devise si mappée (`EUR`/`USD`/`XOF`/`XAF`), sinon `UserPreferences` devise par défaut |
| `note` | colonnes libellé concaténées, trim, espaces multiples réduits |
| `category` | helper de suggestion partagé (cf. §5) : règle apprise puis `CategoriseurLibelle.suggererCategorie(note)`, sinon `AUTRE` |
| `externalId` | `"csv_" + sha256Hex("$dateIso\|$amountCents\|${noteNormalisée}").take(32)` — normalisée via `TexteNormalisation`. **Changement délibéré vs BRED** : l'ancien `BredCategoriseur.genererExternalId` utilisait `String.hashCode()` (32 bits, `libelle.take(30)`) ; pour un import CSV potentiellement volumineux (historique pluriannuel), on passe à un vrai digest tronqué — coût négligeable, collisions écartées. |
| `importSource` | `"csv"` |
| `alreadyImported` | positionné plus tard par `verifierDoublons()` |

Ligne ignorée si : date non parseable, montant non parseable, montant nul
(`abs < 0.001`), ou toutes les colonnes libellé vides **et** montant nul.

## 7. Flux UI

```
Paramètres → Données → « Importer un relevé CSV »
        │
        ▼
[1] Sélecteur SAF (GetContent, "text/*" + extension .csv/.txt)
        │
        ▼
[2] AnalyserCsvUseCase  ──► CsvImportPreview
        │
        ├─ mapping complet deviné ─────────────► [4] Aperçu
        │
        └─ mapping incomplet OU « Ajuster » ──► [3] Écran mapping
                                                     │ (dropdowns pré-remplis,
                                                     │  aperçu 5 lignes live)
                                                     ▼
                                                 [4] Aperçu
        ▼
[4] Aperçu  (verifierDoublons)
     • résumé : N nouvelles · M doublons · K ignorées
     • liste : date | libellé | montant | catégorie (éditable) | ☑ inclure
     • dropdown « Compte bancaire cible » (BankAccountRepository.getAll(), ou « aucun »)
     • bouton « Importer N transactions »
        │
        ▼
[5] confirmer()  →  insertion  →  écran succès (« N importées, M ignorées »)
```

`ImportCsvViewModel` — `sealed interface ImportCsvUiState` :
`Analyse` / `MappingRequis(preview)` / `Apercu(transactions, comptes)` /
`ImportEnCours` / `Termine(result)` / `Erreur(message)`.

## 8. Room

**Aucune migration.** Colonnes utilisées :
- `transactions.importSource TEXT` — v11
- `transactions.externalId TEXT` — v11
- `transactions.bankAccountId INTEGER` — v22

`@Database(version = 23)` inchangé, pas de fichier `24.json`.

## 9. Tests prévus

### Unitaires — `CsvParser` / `CsvColumnDetector` / `MontantParser`

- délimiteur `;` / `,` / `\t` détecté correctement
- guillemets doublés RFC 4180
- encodage ISO-8859-1 (accents) en fallback
- `parseMontantFr` : `1 234,56` → `123456` ; `-120,50` ; tiret unicode ; `1,234.56` (EN)
- détection format date sur les 5 variantes
- détection en-tête absent (fichier sans header)
- détection mode débit/crédit vs signé
- synonymes d'en-tête FR/EN/accents

### Unitaires — `AnalyserCsvUseCase`

- CSV propre → mapping complet deviné, 0 ligne ignorée
- CSV colonnes Débit/Crédit → types corrects
- ligne avec date pourrie → ignorée, les autres passent
- montant nul → ignoré
- devise absente → devise par défaut utilisateur
- mapping fourni explicitement (format exotique) → parse OK

### Unitaires — `ImporterTransactionsCsvUseCase`

- `verifierDoublons` positionne `alreadyImported` selon les `externalId` en base
- `confirmer` n'insère que les nouvelles
- réimport du même fichier → 0 insertion
- `confirmer` met à jour `derniereImportEpochMilli`
- affectation `bankAccountId` quand un compte cible est choisi
- `Result.failure` remonté proprement si le DAO jette

### Unitaires — suggestion de catégorie

- règle apprise prioritaire sur le dictionnaire
- fallback dictionnaire (`CategoriseurLibelle`) si pas de règle
- `AUTRE` si rien ne matche

### Instrumenté (émulateur)

- `ImportCsvViewModel` : fichier propre → saute le mapping → aperçu → import
- fichier exotique → écran mapping → import

### Manuel (device physique) — bloquant pour clôture

- 2 exports de banques réelles différentes fournis par Florent

## 10. Risques & limites connues

| Risque / limite | Traitement |
|---|---|
| Transactions jumelles fusionnées par le hash | **Accepté** (décision §3). Filet : écran doublons. Message d'info dans l'aperçu : « les transactions identiques du même jour sont comptées une seule fois ». |
| Vico : rien (pas de graphique dans ce sprint) | — |
| CSV géant (plusieurs années) chargé en mémoire | Lecture en deux passes (§6.0) : détection sur 15 lignes, données en `useLines` streaming. La liste d'`ImportedTransaction` résultante reste en mémoire pour l'aperçu — limite douce : avertir / paginer au-delà de ~5000 lignes. |
| Format de date ambigu `01/02/2026` (FR jj/mm vs US mm/jj) | L'auto-détection teste FR en premier ; l'écran de mapping laisse choisir. Documenté dans le guidage. |
| Utilisateur importe un CSV qui n'est pas un relevé | Aperçu vide ou incohérent → message « aucune transaction reconnue, vérifiez le mapping ». Rien n'est écrit tant que « Importer » n'est pas pressé. |
| Séparateur décimal `.` avec virgule comme séparateur de milliers en mode EN | `MontantParser` retire les `,` de milliers si `.` est décimal, et inversement. Testé. |

## 11. Estimation

| Poste | Charge |
|---|---|
| `CsvParser` + `CsvColumnDetector` + `MontantParser` + tests | 0,5 j |
| `ImportedTransaction` / `ImportRepository` / use cases + tests | 0,5 j |
| `ImportCsvScreen` + `ImportCsvMappingScreen` + ViewModel | 0,75 j |
| Câblage nav / Settings / nettoyage DAO orphelin | 0,25 j |
| Tests instrumentés + passe device + ajustements | 0,5 j |
| **Total** | **~2,5 j (1 sprint)** |

## 12. Bump de version

Feature livrée notable → **bump requis** en fin de sprint
(`versionCode` / `versionName` dans `app/build.gradle.kts` + entrée `CHANGELOG.md`).

⚠️ **À clarifier avec Florent avant le bump** : `develop-catchup` est
actuellement à **4.4.0 / versionCode 15**, alors que `develop` est à
**4.5.0 / versionCode 16**. La numérotation est incohérente entre les deux
branches. Décider au moment du merge : soit `develop-catchup` reprend la
lignée de `develop` (prochain = 4.6.0 / 17), soit on assume une renumérotation.
**Ne pas bumper en aveugle.**

## 13. Plan d'exécution (objectifs vérifiables)

1. Récupérer les helpers de l'ex-`BredCsvParser`, créer `CsvParser` +
   `MontantParser` → **vérif :** tests unitaires parsing verts.
2. `CsvColumnDetector` + dictionnaire de synonymes → **vérif :** tests
   détection (en-tête, date, délimiteur, débit/crédit) verts.
3. `ImportedTransaction`, `CsvColumnMapping`, `ImportRepository`(+Impl),
   `@Binds` → **vérif :** compile + `ImportRepositoryImpl` testé.
4. Extraire le helper de suggestion partagé depuis
   `GetRecategorizationSuggestionsUseCase`, puis `AnalyserCsvUseCase` →
   **vérif :** tests use case (CSV propre / exotique / lignes pourries) verts +
   `GetRecategorizationSuggestionsUseCaseTest` toujours vert (pas de régression).
5. `ImporterTransactionsCsvUseCase` (2 phases) → **vérif :** tests dédup +
   `derniereImport` + `bankAccountId` verts.
6. `ImportCsvViewModel` + états → **vérif :** test instrumenté flux propre.
7. `ImportCsvScreen` + `ImportCsvMappingScreen` → **vérif :** test instrumenté
   flux mapping.
8. Câblage nav + Settings + suppression `findByAmountDateRangeAndSource` →
   **vérif :** `./gradlew compileDebugKotlin compileDebugAndroidTestKotlin`.
9. `./gradlew test koverVerify` → **vérif :** vert, `domain/` ≥ 80 %.
10. Passe device avec 2 relevés réels → **vérif :** import correct, aucun
    doublon au réimport.
11. Bump version (après clarification §12) + `CHANGELOG.md` +
    `CLAUDE.md` (schéma inchangé, mais mentionner l'import CSV générique dans
    « Fonctionnalités principales ») + `PROJECT_PLAN.md` (ligne Sprint 44).
