# CacheCache — Plugin Paper 1.21

Plugin de mini-jeu "Cache-Cache" (type Prop Hunt) pour serveur Paper 1.21.

## ⚠️ Dépendance requise : LibsDisguises

Paper (API vanilla) ne permet pas nativement de faire apparaître un joueur comme un mob
pour les autres clients. Ce plugin utilise donc **LibsDisguises** (soft-dépendance) pour
déguiser les joueurs cachés en mobs. Sans LibsDisguises installé sur le serveur, le jeu
reste jouable mais les joueurs resteront visuellement des joueurs (le plugin log un
avertissement au démarrage dans ce cas).

Téléchargement : https://www.spigotmc.org/resources/libsdisguises.81/

## Compilation

Ce projet est un projet Maven standard.

```bash
mvn clean package
```

Le jar final sera dans `target/CacheCache.jar`.

> Je n'ai pas pu compiler ce projet dans mon environnement sandbox (accès réseau
> restreint, impossible de télécharger paper-api et libsdisguises depuis leurs dépôts
> Maven). Le code a été relu attentivement (accolades, imports, signatures) mais
> **je te recommande de faire un premier `mvn clean package` et de me remonter les
> éventuelles erreurs de compilation** si tu en rencontres, pour que je les corrige.

## Installation

1. Compiler le plugin (ou récupérer le jar).
2. Installer LibsDisguises sur le serveur.
3. Placer `CacheCache.jar` dans le dossier `plugins/`.
4. Redémarrer le serveur.

## Utilisation rapide

```
/cc create <nom>              -> créer une map
/cc <nom> pos1 / pos2          -> définir la zone
/cc <nom> posconfirm           -> confirmer la zone
/cc <nom> spawnseek             -> spawn du Seeker
/cc <nom> lobby                 -> lobby d'attente
/cc <nom> time <ticks>          -> durée de la partie
/cc <nom> killmax <n>           -> coups max du Seeker
/cc <nom> maxplayers <n>        -> joueurs max
/cc <nom> mob <type> [%]        -> ajouter un mob
/cc <nom> listmob               -> lister les mobs
/cc <nom> seeker <n>            -> nombre de Seekers
/cc <nom> seeker virus          -> activer/désactiver le mode virus
/cc <nom> hunt <tick> [nb]      -> ajouter un déclenchement de hunt
/cc <nom> scenario              -> ouvrir le GUI des scénarios
/cc <nom> save                  -> valider la map
/cc <nom> config                -> repasser en édition
/cc delete <nom>                -> supprimer (confirmation requise)
/cc <nom> rename <nouveau nom>  -> renommer la map (confirmation par message)
/cc list                        -> lister les maps
/cc gui                         -> ouvrir la liste des parties
/cc join <nom>                  -> rejoindre une arène
/cc leave                       -> quitter l'arène actuelle
/cc hub                         -> définir le hub principal
/cc help                        -> aide
```

## Permission admin

`cachecache.admin` (par défaut : op) permet de casser/poser des blocs sur une map même
pendant qu'une partie y est en cours. Sans cette permission, la map est incassable
uniquement pour les joueurs qui participent à une partie en cours (Seeker, caché ou
spectateur d'une partie RUNNING) — en dehors d'une partie (construction, édition), la map
reste modifiable normalement par tout le monde.

## Nouveautés de cette mise à jour

- **Zone éditable en mode config** : après `/cc <map> config`, toutes les commandes de
  configuration (pos1, pos2, posconfirm, spawnseek, lobby, time, killmax, etc.) sont de
  nouveau utilisables, même si la map était déjà sauvegardée.
- **Vrais mobs sur la map** : au lancement d'une partie, de vrais mobs (selon les
  pourcentages configurés) apparaissent aléatoirement un peu partout sur la map, pour que
  les joueurs déguisés se fondent réellement dans la masse. Ces mobs sont rendus
  non-collidables (`setCollidable(false)`) pour ne jamais pousser les joueurs.
- **Plateforme du lobby** : `/cc <map> lobby` génère désormais une plateforme de 8x8 blocs
  invisibles (BARRIER) centrée sous le point de lobby, et les joueurs en attente sont
  confinés à cette zone (impossible de sortir de la plateforme).
- **Spectateurs libres** : les spectateurs (morts en jeu ou observateurs via le GUI) sont
  désormais libres de voler dans toute la zone de jeu (avec une marge confortable), sans
  être renvoyés en boucle vers le lobby.
- **Renommage de map** : `/cc <map> rename <nouveau nom>` avec message de confirmation.
- **`/cc join <map>` / `/cc leave`** : rejoindre ou quitter une arène par commande (en plus
  du GUI). `/cc leave` fonctionne à tout moment (lobby, en jeu, spectateur) et ramène
  proprement au hub.
- **Scoreboard unique par arène** : au lieu de recréer un scoreboard par joueur à chaque
  tick, chaque arène possède désormais deux gabarits de scoreboard persistants (un pour
  les cachés, un pour le/les Seeker(s)), mis à jour en place (juste le texte des lignes
  change, jamais les objets). Le scoreboard est assigné une seule fois à l'entrée en jeu,
  et retiré (retour au scoreboard principal du serveur) dès que le joueur quitte l'arène
  — à la fin d'une partie ou via `/cc leave`. À noter : si plusieurs Seekers sont actifs
  en même temps sur une même arène, la ligne "vos coups" étant partagée par le même
  scoreboard, elle affichera la valeur du dernier Seeker mis à jour plutôt qu'une valeur
  strictement individuelle — dis-moi si tu veux que j'aille plus loin là-dessus (ça
  demanderait un système par paquets pour un vrai affichage 100% individuel).
- **Aucune collision pour les cachés** : en plus des vrais mobs de camouflage (déjà
  non-collidables), le joueur caché lui-même n'a plus aucune collision pendant qu'il est
  déguisé (`setCollidable(false)`) : plus aucun mob ni joueur ne peut le pousser. La
  collision normale est restaurée dès qu'il est éliminé, infecté (virus), ou que la partie
  se termine.
- **Confinement des spectateurs à l'arène** : les cachés éliminés restent spectateurs
  exactement là où ils sont morts (dans la zone de jeu), et tout spectateur — caché
  éliminé, observateur via le GUI, ou joueur passé spectateur à la fin de la partie,
  Seeker inclus — est désormais strictement bloqué dans la zone de l'arène : s'il atteint
  le bord, il est renvoyé au centre de l'arène (avec un message, limité à une fois toutes
  les 3s pour ne pas spammer).
- **Correctif "je bouge tout seul" (renforcé)** : en plus de `setModifyBoundingBox(false)`
  et `setVelocitySent(false)`, le plugin réaffirme maintenant `setCollidable(false)` sur
  chaque joueur caché toutes les secondes pendant la partie. Certains plugins de
  déguisement resynchronisent périodiquement les métadonnées d'entité (dont l'état de
  collision) pour coller au mob imité, ce qui pouvait annuler notre réglage initial et
  provoquer ce mouvement parasite. Si le souci persiste après cette mise à jour, dis-moi
  avec quel(s) type(s) de mob précis ça arrive : ça m'aiderait à cibler si c'est un type de
  déguisement particulier qui pose problème côté LibsDisguises.
- **Le Seeker one-shot aussi les mobs** : dès qu'il frappe n'importe quel mob (un décor de
  camouflage ou un mob naturel de la map), celui-ci meurt instantanément — exactement
  comme s'il touchait un joueur cousin caché, puisqu'il ne peut pas savoir lequel est réel.
  Ça consomme également un coup de son quota (killmax) : s'acharner sans discernement sur
  les décors peut donc lui faire perdre la partie en épuisant ses coups avant d'avoir
  trouvé tout le monde.


## Simplifications assumées / points à tester en jeu

Le cahier des charges est très riche ; certains points ont nécessité une interprétation
ou une simplification que je documente ici :

- **Distinction visuelle "cachés entre eux" vs Seeker** : le cahier des charges demande
  que les joueurs cachés se reconnaissent entre eux mais que le Seeker les confonde avec
  les vrais mobs. Une distinction *par joueur* (le même mob visible différemment selon qui
  regarde) nécessite un système par paquets (type ProtocolLib) que je n'ai pas ajouté pour
  garder le plugin autonome. Actuellement, tout le monde (y compris le Seeker) voit le
  même déguisement LibsDisguises. À voir si tu veux que j'ajoute ProtocolLib pour ce point
  précis.
- **Killmax** : interprété comme un nombre de coups "one-shot" disponibles pour le(s)
  Seeker(s). Une fois ce quota atteint, le Seeker ne peut plus achever personne (son épée
  ne fait plus rien) — la partie continue jusqu'à la fin du temps.
- **Hunt** : le feu d'artifice donné aux joueurs cachés déclenche un effet sonore/visuel
  à la position de chaque Seeker (pour indiquer sa position), plutôt qu'un vrai
  feu d'artifice lancé physiquement.
- **Scénario Enclume** : l'enclume tombe sur tous les cachés mais ne leur inflige aucun
  vrai dégât (effet troll uniquement), pour éviter qu'il ne devienne une élimination
  détournée.
- **Lancement automatique** : la partie démarre automatiquement (compte à rebours de 15s)
  dès que 2 joueurs sont dans le lobby, et le compte à rebours est raccourci si le lobby
  atteint `maxplayers`. Aucune commande `/cc <map> forcestart` n'était demandée, donc je
  ne l'ai pas ajoutée — dis-moi si tu en veux une.
- **Protection de la map** : toute casse de bloc, pose de bloc, et dégât d'explosion est
  bloquée globalement pendant que le plugin est actif (pas seulement dans la zone de jeu),
  pour garantir qu'aucune map ne puisse être dégradée. Si ton serveur héberge autre chose
  qu'un hub Cache-Cache, il faudra restreindre cette protection à la zone de jeu.

Tous les scénarios (1 à 10), les commandes listées dans le cahier des charges, le GUI des
parties (verre gris/vert/orange/rouge), le GUI des scénarios (objets enchantés si actifs),
le mode virus, le mode multi-seekers, et la protection anti-chute pendant `posconfirm`
au sol sont implémentés.
