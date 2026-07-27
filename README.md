# CacheCache — Plugin Paper 1.21

Plugin de mini-jeu "Cache-Cache" (type Prop Hunt) pour serveur Paper 1.21.

## ✅ Aucune dépendance externe

Ce plugin ne dépend plus de LibsDisguises (ni d'aucun autre plugin). Le camouflage des
joueurs en mobs est désormais 100% natif, avec uniquement l'API Paper :

- Le vrai joueur est rendu invisible pour tout le monde (`Player#hidePlayer`).
- Un vrai mob "fantôme" (sans IA, increvable, sans collision, sans gravité) apparaît à sa
  place et est téléporté sur sa position à chaque tick.
- Tout le monde (Seeker compris) ne voit et ne peut cliquer que sur ce mob fantôme — le
  vrai joueur n'existe tout simplement plus sur le client des autres, donc personne ne
  peut accidentellement viser "la vraie personne" au lieu du mob.
- Frapper le mob fantôme d'un joueur cache déclenche une vraie élimination ; frapper un
  simple mob de décor le tue juste instantanément (et consomme un coup du Seeker dans les
  deux cas).

Limite assumée avec cette approche : comme un joueur normal (pas caché) ne peut pas voir
"à travers" `hidePlayer`, les joueurs cachés se voient forcément comme des mobs entre eux
aussi (pas seulement pour le Seeker) — l'ancienne idée de "les cachés se reconnaissent
entre eux" n'est donc plus possible sans un système par paquets (type ProtocolLib), ce qui
irait à l'encontre de la demande de ne plus dépendre d'aucun plugin externe.

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
2. Placer `CacheCache.jar` dans le dossier `plugins/`.
3. Redémarrer le serveur.

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
- **Correctif "je bouge tout seul"** : ce souci venait des ajustements de hitbox/vélocité
  faits par LibsDisguises pour imiter le mob. Depuis le passage au système natif (mob
  fantôme + joueur invisible), ce risque disparaît complètement : le vrai joueur garde sa
  hitbox de joueur normale et se déplace normalement ; seul le mob fantôme, qu'on
  téléporte sur sa position à chaque tick, est visible des autres. Le `setCollidable(false)`
  périodique reste en place par sécurité, pour éviter toute poussée par un mob de décor.
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
  les vrais mobs. Le système natif (sans dépendance) cache le vrai joueur pour TOUT LE
  MONDE, pas seulement pour le Seeker — une distinction *par viewer* (le même joueur visible
  différemment selon qui regarde) nécessiterait un système par paquets (type ProtocolLib),
  ce qui irait à l'encontre de la demande de ne plus dépendre d'aucun plugin externe. Donc
  actuellement, tout le monde (cachés entre eux inclus) voit uniquement le mob fantôme.
  Dis-moi si tu préfères qu'on réintroduise une dépendance (ProtocolLib) pour ce point
  précis, sinon ça reste ainsi.
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
