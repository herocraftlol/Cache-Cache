# 🦊 CacheCache — Plugin Paper 1.21

Plugin de mini-jeu **Cache-Cache** (style Prop Hunt / Hide & Seek) pour serveur Minecraft
Paper 1.21 : les joueurs cachés se camouflent en mobs, et le Seeker doit les retrouver
avant la fin du temps !

---

## ✨ Nouveautés de la version 1.8.0

### ⏱️ Fin de partie instantanée quand les Seekers n'ont plus de coups

- Auparavant, une fois que **tous les Seekers** avaient épuisé leur quota de coups,
  la partie continuait jusqu'à la fin du chrono alors que plus personne ne pouvait
  éliminer quiconque — un temps mort frustrant pour tout le monde.
- Désormais, une nouvelle vérification `allSeekersOutOfKills()` détecte cette situation
  et **termine immédiatement la partie sur une victoire des cachés**, avec un message
  clair : *« Le(s) Seeker(s) n'ont plus aucun coup disponible ! »*
- Le contrôle est déclenché dans **toutes les situations possibles** : attaque sur un
  joueur camouflé (système natif), attaque sur un mob fantôme du backend ProtocolLib,
  et incidents de mort pendant la partie.
- Le scénario **Coups illimités** est évidemment exempté : il ne peut jamais se
  déclencher prématurément.

---

## 🗃️ Détails des versions précédentes

### v1.7.0

### 🧊 Le Seeker est totalement figé pendant le compte à rebours

- `setWalkSpeed(0)` empêchait de marcher... mais **pas de sauter** (ni de dériver en
  nageant) pendant les 15 secondes d'attente. Sa position exacte est maintenant
  **verrouillée à chaque tick** : téléportation de correction si elle change, vélocité
  remise à zéro, et seule l'orientation de la caméra reste libre pour observer les
  alentours. Le Seeker est désormais vraiment immobile jusqu'au signal de départ —
  fini les sauts pour grappiller quelques blocs d'avance !

### 🛡️ Les mobs hostiles ne peuvent plus jamais accrocher les joueurs

- **Filtre élargi à tous les mobs pilotés par IA** : l'ancienne protection ne couvrait
  que l'interface `Monster`, ce qui laissait passer pas mal de mobs hostiles (Phantom,
  Slime, MagmaCube, Guardian, Vex, Shulker...). Elle couvre maintenant
  `org.bukkit.entity.Mob`, l'interface la plus large : **aucun mob ne peut plus cibler
  un joueur de l'arène**, quel que soit son type.
- **Protection active à tout moment** : elle ne s'appliquait auparavant qu'en partie
  RUNNING. Elle protège maintenant les joueurs dès qu'ils font partie d'une arène
  (lobby, compte à rebours, en jeu, fin de partie) — seule exception inchangée : le
  scénario **Mobs Hostiles** laisse volontairement les mobs cibler le Seeker en jeu.

### 🔧 Note technique

- Le backend ProtocolLib reste sur l'approche **`PacketContainer` brut** (API stable de
  ProtocolLib 5.x) : la compilation est garantie quelle que soit la distribution exacte
  de ProtocolLib, avec repli automatique sur le système natif en cas de souci.

### v1.6.0

- **Durée de partie dynamique** : base + bonus par joueur (`/cc <map> time <base> [par_joueur]`,
  défaut 5 min + 1 min/joueur), calculée à chaque lancement de partie.
- **Configuration d'arène plus rapide** : la durée n'est plus un paramètre obligatoire.
- **Mobs de décor proportionnels au killmax** : plus le Seeker a de coups, plus la map
  regorge de faux positifs pour compenser.
- **Commande `hunt`** : avertissement (au lieu d'un blocage) si le déclenchement dépasse
  la durée de base.

---

## 🎮 Fonctionnalités principales

### Mode Cache-Cache classique
- Les **joueurs cachés** se camouflent en mobs (Zombie, Squelette, Enderman, Vache, etc.)
- Le **Seeker** doit les retrouver et les éliminer avec un nombre limité de coups
- Comptez sur votre sens de l'observation et votre adresse pour survivre !

### Camouflage intelligent
- **100% natif (sans dépendances)** : le vrai joueur est **invisible** pour tous les autres joueurs
- Un **mob fantôme** le remplace visuellement et suit ses mouvements en temps réel
- Les autres joueurs ne peuvent voir et interagir qu'avec ce mob fantôme
- **Optionnel ProtocolLib** : s'il est installé, le camouflage utilise des **paquets purs**
  (aucune entité réelle côté serveur) — donc **zéro collision, zéro physique**, garanti
  quel que soit le mob choisi. Le combat est intercepté directement au niveau des paquets
  client. Le plugin choisit automatiquement le meilleur système disponible au démarrage.

### Leaderboards en hologramme
- Classements **Top 10 Seekers** et **Top 10 Hiders** affichés sous forme d'hologrammes
- Persistants après redémarrage du serveur
- Mis à jour automatiquement à chaque fin de partie

### Modes de jeu
- **Mode Virus** : le Seeker infecte les cachés au lieu de les tuer (ils deviennent Seekers à leur tour)
- **Multi-Seekers** : plusieurs Seekers peuvent jouer simultanément
- **Hunt** : des événements périodiques révèlent brièvement la position des Seekers

### 10 Scénarios jouables
- **Blind** : le Seeker a un bandeau sur les yeux
- **Flash** : lumière aveuglante périodique
- **Speed** : les cachés sont plus rapides
- **Invisible** : les cachés sont invisibles (même le mob)
- **Enclume** : des enclumes tombent périodiquement (troll, pas de dégâts)
- **Double Jump** : les cachés ont un double saut
- **Compass** : le Seeker a une boussole pointant vers le caché le plus proche
- Et plus encore !

### Interface graphique (GUI)
- Liste des arènes avec indication du statut (en attente, en cours, pleine)
- Sélection des scénarios pour chaque map via GUI
- Menu de configuration intuitif
- Bouton « Partie rapide » pour rejoindre la partie avec le plus de joueurs

### Interface spectateur
- `/cc spectate <map>` pour observer n'importe quelle arène
- `/cc unspectate` ou item Barrière pour quitter
- Scoreboard complet pour les spectateurs (temps restant, cachés restants, etc.)

---

## 📜 Historique des versions

### v1.8.0 (actuelle)
- Nouveauté : la partie se termine immédiatement (victoire des cachés) dès que tous les Seekers ont épuisé leurs coups, au lieu d'attendre la fin du chrono
- Vérification `allSeekersOutOfKills()` déclenchée dans tous les cas : attaque native, attaque sur mob fantôme ProtocolLib et morts en jeu
- Le scénario Coups illimités est exempté de cette fin anticipée

### v1.7.0
- Correctif : le Seeker est totalement figé pendant les 15 s d'attente (position verrouillée à chaque tick, saut et nage inclus)
- Correctif : les mobs hostiles ne peuvent plus accrocher les joueurs — filtre élargi de `Monster` à `Mob` (Phantom, Slime, MagmaCube, Guardian, Vex, Shulker...)
- Correctif : la protection anti-ciblage est active à tout moment (lobby, compte à rebours, en jeu, fin de partie)
- Backend ProtocolLib conservé sur `PacketContainer` brut (compilation garantie, repli natif automatique)

### v1.6.0
- Durée de partie dynamique : base + bonus par joueur (`/cc <map> time <base> [par_joueur]`, défaut 5 min + 1 min/joueur)
- La durée n'est plus un paramètre obligatoire : configuration d'arène plus rapide
- Mobs de décor désormais proportionnels au killmax du Seeker
- Commande `hunt` : avertissement (au lieu d'un blocage) si le déclenchement dépasse la durée de base

### v1.5.0
- Killmax dynamique : coups du Seeker proportionnels au nombre de cachés (`/cc <map> killmax <base> [par_caché]`)
- Nouvelle commande `/cc <map> decoys <base> [par_caché]` pour configurer les mobs de décor
- Correctif : les admins ne voient plus les joueurs cachés (corps ni pseudo)
- Correctif : un caché éliminé devient spectateur sur place au lieu de réapparaître en survie
- Backend ProtocolLib sur `PacketContainer` brut : compilation garantie, suppression robuste conservée

### v1.4.0
- Paquet `ENTITY_HEAD_ROTATION` : la tête du mob fantôme suit le regard du joueur
- Suppression robuste du mob fantôme (multi-replis `ENTITY_DESTROY`) : plus de résidus figés
- Robustesse réseau renforcée (try/catch sur chaque paquet, repli natif automatique)

### v1.3.0
- Backend de camouflage par paquets ProtocolLib entièrement fonctionnel
- Zéro collision / zéro physique quand ProtocolLib est installé
- Combat intercepté au niveau des paquets client
- Correctif de la collision joueur ↔ mob (équipe `NEVER`) pour la 1.21

### v1.2.0
- Leaderboards en hologramme (Top 10 Seekers/Hiders)
- Support optionnel de ProtocolLib pour un camouflage sans collision
- Message cliquable en fin de partie (Rejouer/Quitter)
- Bouton « Partie rapide » dans le GUI
- Multiples corrections de bugs

### v1.1.0
- Vrais mobs sur la map pour le camouflage
- Plateforme du lobby
- Scoreboards persistants par arène
- Correction des bugs de frappe des mobs

### v1.0.0
- Version initiale

---

## 📋 Commandes

| Commande | Description |
|----------|-------------|
| `/cc create <nom>` | Créer une nouvelle arène |
| `/cc <nom> pos1 / pos2` | Définir la zone de jeu |
| `/cc <nom> posconfirm` | Confirmer la zone |
| `/cc <nom> spawnseek` | Définir le point d'apparition du Seeker |
| `/cc <nom> lobby` | Définir le lobby d'attente |
| `/cc <nom> time <base> [par_joueur]` | Durée de la partie (défaut : 5 min, +1 min par joueur) |
| `/cc <nom> killmax <base> [par_caché]` | Coups du Seeker (défaut : 10, +5 par caché en plus) |
| `/cc <nom> decoys <base> [par_caché]` | Nombre de mobs de décor (défaut : 12, +4 par caché) |
| `/cc <nom> maxplayers <n>` | Nombre maximum de joueurs |
| `/cc <nom> mob <type> [%]` | Ajouter un type de mob |
| `/cc <nom> listmob` | Lister les mobs configurés |
| `/cc <nom> seeker <n>` | Nombre de Seekers |
| `/cc <nom> seeker virus` | Activer le mode virus |
| `/cc <nom> hunt <tick> [nb]` | Ajouter un déclenchement de hunt |
| `/cc <nom> scenario` | Ouvrir le GUI des scénarios |
| `/cc <nom> save` | Sauvegarder l'arène |
| `/cc <nom> config` | Repasser en mode édition |
| `/cc <nom> delete` | Supprimer l'arène |
| `/cc <nom> rename <nom>` | Renommer l'arène |
| `/cc list` | Lister toutes les arènes |
| `/cc gui` | Ouvrir la liste des parties |
| `/cc join <nom>` | Rejoindre une arène |
| `/cc leave` | Quitter l'arène actuelle |
| `/cc spectate <map>` | Observer une arène |
| `/cc unspectate` | Quitter le mode spectateur |
| `/cc hub` | Définir le hub principal |
| `/cc leaderboard seeker summon/remove` | Afficher/retirer le classement Seekers |
| `/cc leaderboard hider summon/remove` | Afficher/retirer le classement Hiders |
| `/cc help` | Afficher l'aide |

---

## 🔧 Installation

1. Téléchargez le fichier `CacheCache.jar` (v1.8.0) depuis la [page des releases](https://github.com/herocraftlol/Cache-Cache/releases)
2. Placez le fichier dans le dossier `plugins/` de votre serveur Paper 1.21
3. Redémarrez le serveur

### Optionnel : ProtocolLib
Pour une expérience optimale (zéro collision avec les mobs), installez [ProtocolLib](https://www.spigotmc.org/resources/protocollib.1997/) sur votre serveur. Le plugin le détectera automatiquement et basculera sur le camouflage par paquets.

### Compilation depuis les sources

```bash
mvn clean package
```

Le fichier JAR sera généré dans `target/CacheCache.jar`.

---

## 📝 Permissions

| Permission | Description | Par défaut |
|------------|-------------|------------|
| `cachecache.admin` | Permet de modifier les blocs pendant les parties | OP |

---

## ⚙️ Configuration

Le plugin crée automatiquement les fichiers nécessaires dans `plugins/CacheCache/` :
- `maps.yml` : configuration des arènes
- `stats.yml` : statistiques des joueurs (victoires)
- `holograms.yml` : position des leaderboards

---

## 🎯 Tips pour les joueurs

- **Cachés** : restez mobiles, confondez-vous parmi les mobs de décoration — chaque coup
  du Seeker dépensé sur un décor le rapproche de l'épuisement de son quota !
- **Seeker** : écoutez les bruits de pas, surveillez les mouvements suspects, et visez
  juste — chaque coup compte, surtout avec un quota proportionnel au nombre de cachés.

---

**Amusez-vous bien ! 🎮**
