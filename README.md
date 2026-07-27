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
/cc list                        -> lister les maps
/cc gui                         -> ouvrir la liste des parties
/cc hub                         -> définir le hub principal
/cc help                        -> aide
```

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
