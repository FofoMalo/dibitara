# Sauvegardes Dibitara — Synology Drive, puis Proton Drive

## État de la livraison 4.10.0

Le code ajoute une sauvegarde JSON manuelle et une sauvegarde quotidienne optionnelle. Il n’a pas été installé sur le téléphone personnel et aucune destination distante n’a été configurée par l’agent.

Les tests de ce changement sont exécutés sur ordinateur uniquement. Un verrou Gradle bloque par défaut les tâches connected/install/uninstall. Ne pas lever ce verrou sans accord explicite et sauvegarde externe vérifiée ; privilégier un émulateur jetable.

## Configuration proposée sur Android

1. Créer un dossier personnel **Documents/Dibitara-Sauvegardes**, en dehors d’Android/data et du stockage privé de l’application.
2. Dans Dibitara → Paramètres → Sauvegardes externes, choisir ce dossier via le sélecteur Android.
3. Appuyer sur **Sauvegarder maintenant**. Vérifier le nom du fichier et la date de dernière écriture vérifiée. Conserver aussi la sauvegarde JSON antérieure restaurée après l’incident.
4. Dans Synology Drive → Plus → Tâches de sauvegarde et synchronisation → Tâches de synchronisation, associer ce dossier local à un dossier du NAS. Privilégier l’envoi vers le NAS et vérifier les options de propagation des suppressions avant activation.
5. Vérifier depuis Synology Drive ou le NAS que le fichier distant existe, a fini de se synchroniser et peut être téléchargé. **La relecture locale par Dibitara ne prouve pas la réception sur le NAS.**
6. Activer **Sauvegarde quotidienne** dans Dibitara. Android peut décaler l’exécution ; ce n’est pas un rendez-vous à une heure précise. Les erreurs restent visibles dans les paramètres.

La documentation Synology confirme les tâches de synchronisation entre dossier local Android et serveur, avec modes unidirectionnel et bidirectionnel :
- https://kb.synology.com/tr-tr/DSM/help/Drive/Android?version=7
- https://www.synology.com/en-eu/dsm/7.4/software_spec/synology_drive

## Conservation et vérification

- Chaque copie porte une date, une heure et un identifiant unique. Aucun remplacement du fichier précédent.
- Contrôle de la présence des 17 collections JSON exportées avant écriture.
- Relecture et comparaison intégrale des octets du fichier écrit, puis finalisation du nom .json.
- Si une écriture échoue, seule sa propre copie partielle est nettoyée ; les sauvegardes antérieures sont conservées.
- Pas de suppression automatique ni de rotation pouvant propager une suppression sur le NAS.
- Après désinstallation/réinstallation, les autorisations du dossier et la planification doivent être rétablies. Un dossier partagé local subsiste indépendamment de Dibitara ; cela ne protège pas d’une perte du téléphone sans copie distante.

## Copie secondaire Proton Drive

Le bouton **Partager une copie JSON · Proton Drive** ouvre le partage Android. Sélectionner Proton Drive s’il est proposé. Sinon, utiliser **Importer un fichier** dans Proton Drive et choisir un JSON finalisé du dossier de sauvegardes. Vérifier ensuite sa présence distante.

Cette livraison ne prétend pas fournir une synchronisation Proton en arrière-plan. L’import manuel de fichiers est documenté par Proton : https://proton.me/support/proton-drive-mobile-upload-download

## Contenu et limites

Le format reste compatible avec la restauration JSON existante : enfants, transactions et identifiants de réconciliation, budgets, comptes d’épargne, objectifs, investissements, dettes, comptes bancaires, sous-catégories, enveloppes, règles et versements.

Les historiques de patrimoine/valorisation et les préférences de l’application ne sont pas inclus par l’export existant. Il ne s’agit donc pas d’une image complète de l’application. Le code PIN, les secrets de sécurité et les autorisations Android ne sont pas sauvegardés. Le JSON n’est pas chiffré par Dibitara.

La validation de restauration doit se faire sur une installation de test isolée, jamais en remplaçant la base du téléphone personnel. Les tests unitaires ne démontrent pas à eux seuls la bonne synchronisation des applications Drive installées sur le téléphone.
