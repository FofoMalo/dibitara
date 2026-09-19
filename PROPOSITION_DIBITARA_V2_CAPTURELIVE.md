# Dibitara V2 — proposition globale et extension CaptureLive

Statut : proposition UX/UI globale ; CaptureLive Roundup/plan et réconciliation CSV implémentés (4.9.0). Virements en attente de messages réels.

## Objectif

Réduire la saisie et les imports répétitifs grâce aux notifications Trade Republic, puis rendre l’ensemble de Dibitara plus cohérent et plus facile à parcourir. La V2 conserve les fonctionnalités actuelles et reste un chantier distinct de CaptureLive.

## 1. CaptureLive Trade Republic

### Périmètre demandé

| Événement confirmé | Résultat attendu |
| --- | --- |
| Virement entrant | Enregistrer un crédit sur le compte Trade Republic, identifiable comme virement. |
| Virement sortant | Enregistrer un débit sur le compte Trade Republic, identifiable comme virement. |
| Roundup exécuté | Enregistrer le montant effectivement investi, avec un libellé Roundup et le support lorsqu’il est présent. |
| Plan d’épargne exécuté | Enregistrer le montant effectivement investi, avec un libellé Plan d’épargne et le support MSCI World lorsqu’il est présent. |

La capture concerne les opérations exécutées. Une annonce d’exécution future, un échec ou une demande de confirmation ne crée pas de transaction. Le montant investi ne permet pas, à lui seul, de calculer une valeur de marché ou un nombre de parts : ces valeurs ne seront pas inventées.

### Constat dans le code

- `BredNotificationListenerService` filtre le package BRED. `TradeRepublicNotificationListenerService` existe déjà depuis 4.7.0 pour les paiements carte : le premier constat fondé sur l’index ancien était incomplet.
- `CapturerTransactionLiveUseCase` associe le compte à partir de la source et vérifie l’existence de l’identifiant avant insertion.
- Depuis 4.9.0, `ImportTransactionsUseCase` réconcilie aussi les captures TradeRepublic ; le parcours inverse est géré par `CapturerTransactionLiveUseCase`.
- La recherche historique BRED utilise montant/date à ±1 jour. TradeRepublic ajoute devise, sens et clé de nature/support/marchand, avec une correspondance unique et des écritures atomiques.
- Le CSV Trade Republic reconnaît déjà les achats `TRADING`, dont le plan MSCI World, mais ne conserve pas leur origine Roundup/plan dans le libellé.

### Mise en œuvre proposée

1. Confirmer les titres et textes réels des quatre notifications, ainsi que le package émetteur sur l’appareil. Vérifier où Android expose leur texte complet.
2. Ajouter un parseur Trade Republic distinct, en Kotlin pur, et conserver le parseur BRED existant. Les notifications ambiguës ne doivent pas produire une opération supposée.
3. Acheminer la notification vers le bon parseur puis réutiliser le parcours métier de capture, avec la source Trade Republic et son compte bancaire.
4. Définir une identité de capture à partir des informations réellement disponibles sur Android. Tester les rediffusions, les mises à jour et deux événements légitimes identiques : le couple montant/date seul est insuffisant.
5. Réconcilier les imports CSV ultérieurs avec les captures Trade Republic en comparant source, devise, sens, montant, date et nature quand elle est disponible. Une correspondance ambiguë doit être présentée à vérifier ; elle ne doit pas effacer arbitrairement une opération.
6. Rendre la capture visible dans Activité et dans les réglages : compte, provenance, date, accès Android actif ou désactivé. Garder l’import comme moyen de rattrapage.

### Vérification avant livraison

- Un test issu de chaque notification réelle produit le bon montant en centimes, le bon sens et le bon compte.
- Les annonces, échecs et notifications étrangères au périmètre ne créent aucune opération.
- Une notification rejouée ne crée pas de doublon ; deux opérations distinctes de même montant restent distinctes.
- Un import après capture ne double pas les opérations ; tester aussi une notification retardée après import.
- Les tests BRED existants passent et un essai sur appareil confirme la capture Android réelle.
- Les virements internes ne gonflent ni les revenus ni les dépenses courantes. Un virement vers un tiers ne devient pas interne sur la seule présence du mot « virement ».
- Les investissements capturés ne sont pas ajoutés une seconde fois par une éventuelle récurrence locale.
- Une fonctionnalité livrée entraîne un bump de version et une entrée de changelog. Une éventuelle modification Room implique sa migration et son schéma exporté.

### Information attendue

Roundup et plan confirmés par `trade.jpg` : « Vous avez économisé et investi 48,66 € dans le Round up ! » et « Votre plan d’épargne sur Core MSCI World USD (Acc) de 35,00 € a été exécuté ». Le parseur existant est étendu à ces deux formulations. Le support du Roundup n’est pas indiqué. Les notifications de virements restent à fournir. La réconciliation CSV est implémentée dans les deux ordres avec des identifiants persistés (Room v27), un appariement unique et un refus sans écriture si les données sont ambiguës. Le Roundup doit être identifiable dans le CSV ; les doublons déjà présents ne sont pas supprimés automatiquement.

## 2. Refonte UX/UI de toute l’application

### Direction visuelle

Conserver l’identité noir/or avec des surfaces neutres, des accents dorés ponctuels, des montants lisibles et un espacement homogène. Réutiliser `HeroCard`, `CategoryVisuals`, `TrendChip`, `AcquisitionEvolutionBlock` et `MouvementCapitalField`. Le rouge sert aux alertes réelles ; une dépense ordinaire n’est pas une erreur.

Chaque écran suit la même lecture : titre et période, synthèse utile, action principale, détails. Les filtres actifs sont visibles. Les actions Modifier/Supprimer restent dans un menu contextuel. Les états vides expliquent la prochaine action, les erreurs proposent une reprise, les états de chargement préservent la structure.

### Navigation proposée

Cinq destinations permanentes : **Accueil · Budget · Activité · Patrimoine · Plus**.

| Destination | Contenu et parcours |
| --- | --- |
| Accueil | Disponible du mois, engagements à venir, actions réellement nécessaires, activité récente ; accès au détail du patrimoine. |
| Budget | Revenus, dépenses, enveloppes, récurrences et reste disponible ; un appui sur une catégorie ouvre Activité avec période et filtre conservés. |
| Activité | Historique unifié, compte et période, distinction dépenses/revenus/virements/investissements ; accès direct aux opérations à catégoriser. |
| Patrimoine | Synthèse actifs/dettes, puis Épargne, Investissements et Dettes ; chaque total ouvre les lignes qui l’expliquent. |
| Plus | Analyses, rapport mensuel, projections, scénarios, comptes, imports, exports et réglages ; pas de fonction supprimée. |

Les préférences existantes de visibilité de l’épargne et des investissements restent respectées. Les notifications et liens directs doivent retrouver le bon écran et ses filtres.

### Couverture écran par écran

| Écran ou famille actuelle | Proposition V2 |
| --- | --- |
| Configuration PIN et verrouillage | Parcours bref, clavier lisible, biométrie identifiable, erreurs contextualisées ; mécanismes de sécurité conservés. |
| Dashboard | Une synthèse prioritaire, puis échéances et actions ; éviter que toutes les cartes aient la même importance visuelle. |
| Budget | Période persistante, synthèse revenus/dépenses/engagements, enveloppes comparables ; accès aux opérations justificatives. |
| Dépenses / transactions | Devient Activité ; titre explicite pour tous les flux, provenance CaptureLive/CSV visible dans le détail, filtres conservés au retour. |
| Ajout / modification de transaction | Champs essentiels d’abord, détails secondaires dépliables ; type, compte, montant et devise visibles ; confirmation de suppression contextualisée. |
| Épargne | Synthèse puis comptes et objectifs ; progression, compte source et mode de financement lisibles sans ouvrir chaque fiche. |
| Investissements | Synthèse puis actifs ; séparer apports et performance ; distinguer les exécutions Roundup/plan dans l’historique associé. |
| Fiches et édition des actifs | Structure commune immobilier, SCPI, actifs libres et épargne salariale ; conserver les particularités de parts, versements et mouvements de capital. |
| Activités locatives | Airbnb et location de véhicule conservent leurs opérations et bilans ; présenter recettes, charges et résultat avec une période commune. |
| Dettes | Solde restant, prochaine échéance et lien éventuel avec le bien ; détails du crédit accessibles depuis la carte. |
| Détail du patrimoine | Total net explicable par actifs moins dettes, répartition et évolution ; accès aux familles de patrimoine. |
| Tendances | Période et comparaison lisibles, axes sans troncature ; accès au détail des catégories. |
| Rapport mensuel | Bilan du mois, écarts utiles, opérations justificatives et export dans une progression de lecture unique. |
| Recommandations | Prioriser les conseils actionnables avec leur justification et l’accès à l’action correspondante. |
| Projections | Hypothèses visibles et modifiables avant les résultats ; distinguer clairement données constatées et simulation. |
| Scénarios / logement / conseiller patrimoine | Même structure : situation, hypothèses, résultats, comparaison ; saisie conservée au retour. |
| Comptes bancaires | Compte, provenance des données, accès à son activité et réglages de capture regroupés. |
| Imports TR / BRED CSV / BRED PDF | Parcours commun choisir → vérifier → confirmer → résultat ; distinguer nouveautés, doublons et rapprochements à vérifier. |
| Nettoyage des doublons | Comparaison compréhensible avant toute suppression, provenance et critères visibles. |
| Réglages | Sections Préférences, Capture et données, Sécurité ; accès Android expliqué et statut réel affiché. |
| Exports CSV / JSON | Type de données, format et action de partage explicites ; conserver les capacités actuelles. |

### Parcours prioritaires à maqueter

1. Notification reçue → opération visible dans Activité → détail et correction éventuelle.
2. Accueil → enveloppe budgétaire → opérations du mois → retour sans perte de filtre.
3. Patrimoine → investissement → apports Roundup/plan et performance distincts.
4. Épargne → objectif → compte source et progression.
5. Import de rattrapage → vérification des rapprochements → résultat sans double comptage.
6. Plus → scénario logement → hypothèses → comparaison.

### Accessibilité et cohérence

Tester les petits écrans, l’agrandissement des caractères, TalkBack, les libellés longs et toutes les devises déjà prises en charge. Prévoir des zones tactiles d’au moins 48 dp. Un signe, un libellé ou une icône complète toujours une couleur porteuse de sens. Les graphiques donnent aussi un résumé textuel exploitable.

## 3. Préserver l’existant

- CaptureLive : changement fonctionnel autonome, validé sur les notifications réelles et les imports de rattrapage.
- V2 : proposition globale puis maquettes des parcours ci-dessus, avant intégration aux écrans de production.
- Lors de l’intégration, utiliser une branche dédiée et un point de comparaison explicite avec la version stable ; ne pas coupler les calculs métier à la refonte visuelle.
- Livrer progressivement navigation et composants, quotidien, patrimoine, puis analyses/réglages. Vérifier pour chaque lot les parcours existants, le retour Android, les liens directs et la persistance des filtres.
- Aucun déploiement V2 ni modification des données n’est effectué par ce document.

La première maquette présentée dans la conversation illustre seulement la direction de trois vues. Elle ne constitue pas une refonte complète : la couverture globale à produire est celle de ce document.
