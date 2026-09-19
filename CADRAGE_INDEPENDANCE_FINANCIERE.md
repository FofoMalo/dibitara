# Cadrage — Indépendance financière (Cap FI + Signal / Bruit / Concentration)

> Statut : **cadrage initial, rien développé dans l'app Kotlin**. Origine : maquette
> React (`dibitara-mockup/src/app/screens/IndependanceFinanciereScreen.tsx`, 3ᵉ carte
> du hub Scénarios) + session de vérification avant/après sur device réel (Fairphone 6,
> 2026-09-19) ayant révélé deux limites de données réelles qui doivent façonner la
> conception avant tout code.
> Branche cible : `florent/prive`.
> Date : 2026-09-19.

---

## 1. Contexte

Demande initiale : une fonctionnalité pour aider à l'indépendance financière, et pour
distinguer les signaux des bruits dans la gestion financière. Avant de coder quoi que
ce soit, une vérification avant/après sur le device réel de Florent a servi de terrain
d'essai et a mis au jour deux limites concrètes des données/algorithmes existants :

1. **Concentration non détectée.** La catégorie Transport affichait ~3220 €/mois dans
   les Recommandations, quasi identique sur 3 mois glissants (moyenne 3214,57 €) — donc
   lu comme une dérive durable. En réalité, une seule transaction récurrente mal
   catégorisée (« Total des frais de gestion débités du 01.06 au 30.06 », un frais
   bancaire) portait la quasi-totalité du montant. Corrigé en session (recatégorisation
   + règle apprise). **Un bug de catégorisation produit exactement le même profil
   temporel qu'un vrai signal structurel** — rien dans l'app actuelle ne les distingue.
2. **Taux d'épargne peu fiable si le revenu est irrégulier.** `tauxEpargneActuelPct`
   (`GetSpendingRecommendationsUseCase.kt:89-95`) = `(revenuMoyen - depensesMoyennes) *
   100 / revenuMoyen`, coercé à `[0, 100]`. Exclut déjà les transactions `INVESTMENT`
   (donc pas de confusion investissement/consommation), mais un revenu mensuel très
   irrégulier (2213,19 € un mois donné contre 13544,04 € de moyenne 3 mois observée en
   session) peut faire tomber le ratio à 0 sans indiquer si c'est réel ou un symptôme de
   revenu sous-capté. Diagnostic de Florent en session : revenu probablement sous-capté.
3. **Profil patrimonial réel** (Patrimoine, device, 2026-09-19) : très
   investissement-heavy (~95 % Investissements, ~4 % Épargne liquide). Un indicateur
   basé sur le cash-flow mensuel sous-estime structurellement l'effort d'épargne de ce
   genre de profil ; la trajectoire réelle du patrimoine net (`patrimoine_snapshots`,
   déjà alimentée mensuellement par le WorkManager existant) est la mesure la plus
   fidèle, et insensible aux deux limites ci-dessus.

## 2. Objectif & critères de succès

**Objectif :** donner un cap (capital cible + échéance estimée) vers l'indépendance
financière, et faire remonter dans les catégories de budget ce qui mérite une action
(signal) plutôt que ce qui ne le mérite pas (bruit) ou ce qui doit d'abord être
vérifié (concentration/couverture) — sans reproduire les biais du §1.

**Critères vérifiables :**

1. Le cap FI se base sur `patrimoine_snapshots` (trajectoire réelle), jamais sur le
   ratio cash-flow mensuel → un mois de revenu bas ne fait pas reculer le cap.
2. Un mois avec revenu anormalement bas par rapport à la moyenne déclenche une alerte
   « revenu potentiellement incomplet — vérifier les imports » plutôt que d'afficher
   silencieusement un taux dégradé.
3. Une catégorie dont une part significative du total est portée par 1-2 transactions
   récurrentes est signalée « à vérifier » **avant** toute classification signal/bruit
   → test unitaire avec le cas réel Transport/frais de gestion comme fixture.
4. Un écart « signal » (persiste ≥ N mois consécutifs) est visuellement distinct d'un
   écart « bruit » (ponctuel, déjà revenu à la normale) → test de classification.
5. Aucune régression sur `GetSpendingRecommendationsUseCase` (réutilisé tel quel pour
   le revenu/dépense moyens, pas remplacé).

## 3. Features candidates (cluster)

| # | Feature | Dépend de | Room | Maille |
|---|---|---|---|---|
| F1 | Cap FI : capital cible = dépense annuelle lissée (hors `INVESTMENT`) × multiple réglable (défaut 25×, règle des 4 %) | `GetSpendingRecommendationsUseCase` (revenu/dépense moyens) | `UserPreferences` (2 champs) | ~1 j |
| F2 | Progression + échéance estimée, via trajectoire `patrimoine_snapshots` réelle + curseur de rendement hypothétique pour la projection | F1, `patrimoine_snapshots` (déjà en base) | — | ~1 j |
| F3 | Détection de concentration par catégorie (part du total portée par 1-2 transactions récurrentes) → badge « à vérifier » | Dépenses par catégorie (déjà calculées dans `GetSpendingRecommendationsUseCase`) | — | ~0,5 j |
| F4 | Classification persistance signal/bruit par catégorie (fenêtre glissante) | F3 s'exécute **avant** — ne pas classer un artefact de catégorisation comme signal structurel | — | ~1 j |
| F5 | Alerte couverture revenu (mois anormalement bas vs moyenne 3 mois) | `GetSpendingRecommendationsUseCase` | garde-fou fréquence (réutiliser le mécanisme `derniereAlerte*EpochDay` existant) | ~0,5 j |
| F6 | Écran dédié (3ᵉ carte du hub Scénarios, déjà maquettée) — réutilise `HeroCard`/`TrendChip`/enveloppes existants | F1-F5 | — | ~0,5 j |

## 4. Décisions d'architecture à trancher

- **Stockage des paramètres FI** (multiple, rendement espéré) : `UserPreferences`
  (aucune migration), cohérent avec `tauxEpargneCiblePct` déjà présent pour le 50/30/20
  — pas de nouvelle table pour 2 nombres.
- **Fenêtre de persistance « signal »** (F4) : 3 mois par défaut, comme le reste de
  `GetSpendingRecommendationsUseCase` — à confirmer, rien ne garantit que 3 mois soit
  le bon réglage pour toutes les catégories (une charge annuelle ne « persistera »
  jamais sur 3 mois par nature).
- **Seuil de concentration** (F3) : à calibrer sur des cas réels plutôt qu'inventer un
  chiffre — le cas Transport était ~100 % porté par une seule transaction. Proposer un
  défaut prudent (ex. 70 %) et l'ajuster après un premier passage sur l'historique réel.
- **Le cap FI (F1/F2) doit-il exclure les catégories « à vérifier » (F3) du calcul de
  dépense annuelle tant qu'elles ne sont pas confirmées ?** Risque de sous-estimer le
  cap si on exclut trop largement — probablement non : le signaler suffit, ne pas
  fausser le cap sur une hypothèse non confirmée par l'utilisateur.
- **F5** doit réutiliser le garde-fou de fréquence existant (`derniereAlerte*EpochDay`,
  cf. `bug_notification_seuil_fonds_repetee_2026_08_21`) pour ne pas reproduire le bug
  d'alertes répétées déjà corrigé une fois sur fonds/budget/dettes.

## 5. Hors scope

- Séparation automatique pro/perso ou détection de compte commun non enregistré — le
  cas réel du §1 était une catégorisation, pas un virement interne réel ; si ce
  symptôme se reproduit sous forme de virement, cadrer séparément (cf.
  `IdentifierVirementsInternesUseCase` existant).
- Recommandation d'allocation d'actifs (quoi acheter pour atteindre le cap) — ce
  cadrage reste sur la mesure et l'alerte, pas le conseil d'investissement.

## 6. Hiérarchie typographique — H1 / H2 / Montants / Explicatif

Demande de Florent (2026-09-19) : sur les écrans de cette fonctionnalité, les titres
(H1/H2), les montants et le texte explicatif doivent être **distinguables d'un coup
d'œil**, en restant dans la charte graphique Dibitara (palette noir/or, `HeroCard`,
`CategoryVisuals`, cf. CLAUDE.md § Conventions UI/Design) — l'idée étant que la
hiérarchie visuelle elle-même *apporte de l'éclairage* sur la situation financière,
pas seulement le contenu. Constat en lisant le code existant (Android `Theme.kt` +
maquette React) : la charte actuelle **documente des composants** (HeroCard, TrendChip,
CategoryVisuals) mais **pas une échelle typographique formelle** — `Theme.kt` ne
personnalise que 3 styles Material3 par défaut (poids/letter-spacing), chaque écran
choisit ensuite sa taille au cas par cas (constaté dans la maquette : montants tantôt
11px, tantôt 13px, tantôt 18px, tantôt 28px, sans règle explicite). Proposition à 4
niveaux, qui formalise ce qui est déjà *presque* respecté plutôt que d'inventer un
nouveau vocabulaire :

| Niveau | Rôle | Police | Poids/couleur | Existant à conserver | Existant à corriger |
|---|---|---|---|---|---|
| **H1** — titre d'écran | Identifie l'écran ou l'action, un seul par écran | Sans-serif (corps, Outfit côté maquette) | Bold, `--db-fg` plein (jamais réduit en opacité) | `ScreenHeader.title`, titre Dashboard | RAS — déjà cohérent |
| **H2** — titre de section | Découpe l'écran en blocs thématiques | **Mono** (JetBrains Mono), MAJUSCULES, `tracking-widest` | Semi-bold, `--db-fg`/50, petite taille (10px) | `SectionHeader` — le pattern fonctionne bien : le mono+petit crée un vrai contraste avec H1 sans concurrencer les montants | Quelques titres de carte (ex. axes du Conseiller patrimoine) utilisent un `<p>` brut 13px au lieu de passer par un composant partagé — écart mineur, à homogénéiser à l'implémentation |
| **Montant** — la donnée chiffrée | Ce qu'on doit voir en premier — c'est un outil financier, le chiffre EST le contenu | **Mono** (JetBrains Mono, chiffres tabulaires à chasse fixe) | 3 crans selon importance : Héros (24-28px, bold) / Secondaire (13-20px, semi-bold) / Inline (11-13px, medium) — couleur GOLD/GREEN/RED selon le sens économique, jamais neutre si un signe existe | — | **Inversion à faire** : aujourd'hui le mono est réservé aux petits labels au-dessus du montant, et le montant lui-même hérite du corps de texte (Outfit) — proposition d'inverser la priorité : c'est le chiffre qui doit porter le mono, pas son étiquette |
| **Explicatif** — verbatim de contexte | La phrase qui explique le chiffre (jamais le chiffre lui-même) | Sans-serif (Outfit), jamais mono | Regular, 10-12px, `--db-fg`/25-40 | Déjà globalement respecté (`text-[10-12px] text-[var(--db-fg)]/25..40`) | Quelques labels courts type "PATRIMOINE NET" utilisent le mono alors que ce sont des étiquettes, pas des montants — zone grise à trancher : étiquette d'un montant héros = H2 (mono, OK) ou explicatif (pas mono) ? Proposition : **H2** si elle est directement collée au chiffre qu'elle nomme (fait partie du même bloc visuel), explicatif sinon |

**Principe transversal** : à l'œil, seuls deux registres de police doivent apparaître
sur un écran — Outfit pour tout ce qui se *lit* (titres, phrases), JetBrains Mono pour
tout ce qui se *compte* (H2/labels de section, montants). Le contraste police devient
lui-même un signal ("ceci est un chiffre à retenir" vs "ceci est une explication à
lire si besoin"), sans dépendre uniquement de la taille ou de l'opacité.

**Décision à trancher avant implémentation Android** : `Theme.kt` n'embarque aucune
police custom aujourd'hui (Material3 par défaut, probablement Roboto) — la paire
Outfit/JetBrains Mono n'existe que côté maquette React. Porter cette hiérarchie sur
Android suppose soit (a) intégrer ces deux polices (Google Fonts, via `Downloadable
Fonts` ou fichiers bundlés dans `res/font/`), coût ponctuel mais aligne enfin l'app
sur l'identité déjà établie par la maquette, soit (b) obtenir le même contraste
seulement par poids/taille/couleur avec la police système, sans levier "police mono
vs sans-serif". Recommandation : (a), parce que (b) a déjà été tenté à l'usage (le
constat du tableau ci-dessus) et n'a pas suffi à empêcher la dérive taille par écran.

## 7. Estimation grosse maille

F1+F2 (cap + projection) ≈ 2 j. F3+F4 (concentration + signal/bruit) ≈ 1,5 j — la
partie la plus utile mais la plus incertaine (seuils à calibrer sur données réelles,
pas de valeur "correcte" a priori). F5 ≈ 0,5 j. F6 (écran) ≈ 0,5 j. Total réaliste ≈
4,5-5 j. Suggestion d'ordre : commencer par **F3** (déjà validé sur un cas réel cette
session, valeur immédiate même seul) avant F1/F2/F4 qui demandent plus d'arbitrages.
