# 🦊 CacheCache — Plugin Paper 1.21

Plugin de mini-jeu **Cache-Cache** (style Prop Hunt / Hide & Seek) pour serveur Minecraft Paper 1.21.

> *Un Seeker doit retrouver et éliminer tous les joueurs cachés avant la fin du temps !*

---

## 🎮 Fonctionnalités principales

### Mode Cache-Cache classique
- Les **joueurs cachés** se camouflent en mobs (Zombie, Squelette, Enderman, Vache, etc.)
- Le **Seeker** doit les retrouver et les éliminer avec un nombre limité de coups
- Comptez sur votre sens de l'observation et votre adresse pour survivre !

### Camouflage intelligent
- **100% natif (sans dépendances)** : Le vrai joueur est **invisible** pour tous les autres joueurs
- Un **mob fantôme** le remplace visuellement et suit ses mouvements en temps réel
- Les autres joueurs ne peuvent voir et interagir qu'avec ce mob fantôme
- **Optionnel ProtocolLib** : Si installé, le camouflage utilise des **paquets purs** (aucune entité réelle côté serveur) — donc **zéro collision, zéro physique**, garanti à 100% quel que soit le mob choisi. Le combat est intercepté directement au niveau des paquets client.

### Leaderboards en hologramme
- Classements **Top 10 Seekers** et **Top 10 Hiders** affichés sous forme d'hologrammes
- Persistants après redémarrage du serveur
- Mis à jour automatiquement à chaque fin de partie

### Modes de jeu
- **Mode Virus** : Le Seeker infecte les cachés au lieu de les tuer (ils deviennent Seekers à leur tour)
- **Multi-Seekers** : Plusieurs Seekers peuvent jouer simultanément
- **Hunt** : Des événements périodiques révèlent brièvement la position des Seekers

### 10 Scénarios jouables
- **Blind** : Le Seeker a un bandeau sur les yeux
- **Flash** : Lumière aveuglante périodique
- **Speed** : Les cachés sont plus rapides
- **Invisible** : Les cachés sont invisibles (même le mob)
- **Enclume** : Des enclumes tombent périodiquement (troll, pas de dégâts)
- **Double Jump** : Les cachés ont un double saut
- **Compass** : Le Seeker a une boussole pointant vers le cachés le plus proche
- Et plus encore !

### Interface graphique (GUI)
- Liste des arènes avec indication du statut (en attente, en cours, pleine)
- Sélection des scénarios pour chaque map via GUI
- Menu de configuration intuitif
- Bouton "Partie rapide" pour rejoindre la partie avec le plus de joueurs

### Interface spectator
- `/cc spectate <map>` pour observer n'importe quelle arène
- `/cc unspectate` ou item Barrière pour quitter
- Scoreboard complet pour les spectateurs (temps restant, cachés restants, etc.)

---

## ✨ Nouveautés de la version 1.3.0

### 🆕 Camouflage 100% par paquets (ProtocolLib) — enfin fonctionnel !
La grande nouveauté de cette version : le backend de camouflage par paquets ProtocolLib
est **désormais pleinement opérationnel et compilé**. Dans les versions précédentes, le
support ProtocolLib était annoncé mais restait inactif (classes wrapper inexistantes).

- **Détection automatique** : si ProtocolLib est installé, le plugin bascule sur le mode
  paquets (`PacketDisguiseBackend`). Sinon, il utilise le système natif (vrai mob fantôme).
- **Zéro collision, zéro physique** : le mob que voient les joueurs n'est **plus une vraie
  entité** côté serveur — c'est uniquement une apparence envoyée par paquets. Il devient
  donc **strictement impossible** que ce mob soit poussé, bloqué ou corrige dans un passage
  étroit, quel que soit le type de mob choisi.
- **Combat intercepté au niveau du paquet** : comme aucune vraie entité n'existe, l'attaque
  du Seeker est interceptée directement sur le paquet `USE_ENTITY` envoyé par son client, et
  redirigée vers la logique d'élimination.
- **Repli automatique** : en cas d'échec d'un paquet (version de ProtocolLib différente,
  etc.), le plugin retombe automatiquement sur le système natif pour ce joueur — il ne
  plante jamais.

### 🆕 Correctif définitif de la collision joueur ↔ mob (1.21)
Sur les versions récentes de Minecraft, le flag `Entity#setCollidable(false)` ne suffisait
plus à empêcher la poussée entre un joueur et son mob fantôme. Le plugin crée maintenant
automatiquement une **équipe de scoreboard dédiée avec la règle de collision `NEVER`** sur
chacun de ses scoreboards (caché / Seeker / spectateur) et y ajoute le joueur dès qu'il
rejoint la partie. C'est le mécanisme officiel Mojang/Bukkit pour désactiver la collision
d'un joueur — il couvre les cas que les flags bruts ne couvraient plus en 1.21.

### 🐛 Corrections & robustesse
- Le code du backend ProtocolLib a été réécrit avec l'API `PacketContainer` officielle
  (fini les classes wrapper générées inexistantes en ProtocolLib 5.x) : le projet **compile
  désormais sans erreur** avec `mvn clean package`.
- Chaque opération réseau est enveloppée d'un `try/catch` pour garantir qu'aucune erreur
  ponctuelle ne fasse planter le plugin ou la partie en cours.

---

## 📋 Commandes

| Commande | Description |
|----------|-------------|
| `/cc create <nom>` | Créer une nouvelle arène |
| `/cc <nom> pos1 / pos2` | Définir la zone de jeu |
| `/cc <nom> posconfirm` | Confirmer la zone |
| `/cc <nom> spawnseek` | Définir le point d'apparition du Seeker |
| `/cc <nom> lobby` | Définir le lobby d'attente |
| `/cc <nom> time <ticks>` | Définir la durée de la partie |
| `/cc <nom> killmax <n>` | Nombre de coups du Seeker |
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
| `/cc unspectate` | Quitter le mode spectator |
| `/cc hub` | Définir le hub principal |
| `/cc leaderboard seeker summon/remove` | Afficher/retirer le classement Seekers |
| `/cc leaderboard hider summon/remove` | Afficher/retirer le classement Hiders |
| `/cc help` | Afficher l'aide |

---

## 🔧 Installation

1. Téléchargez le fichier `CacheCache-1.3.0.jar` depuis la page des releases
2. Placez le fichier dans le dossier `plugins/` de votre serveur Paper 1.21
3. Redémarrez le serveur

### Optionnel : ProtocolLib
Pour une expérience optimale (zéro collision avec les mobs), installez [ProtocolLib](https://www.spigotmc.org/resources/protocollib.1997/) sur votre serveur. Le plugin le détectera automatiquement.

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

Le plugin crée automatiquement les fichiers de configuration nécessaires dans `plugins/CacheCache/` :
- `maps.yml` : Configuration des arènes
- `stats.yml` : Statistiques des joueurs (victoires)
- `holograms.yml` : Position des leaderboards

---

## 🎯 Tips pour les joueurs

- **Cachés** : Restez mobiles, confondez-vous parmi les mobs de décoration, et attention à ne pas épuiser les coups du Seeker en massacrant les mobs autour de vous !
- **Seeker** : Écoutez les bruits de pas, surveillez les mouvements suspects, et visez juste — chaque coup compte !

---

## 📜 Historique des versions

### v1.3.0 (actuelle)
- Backend de camouflage par paquets ProtocolLib **entièrement fonctionnel et compilé**
- Zéro collision / zéro physique quand ProtocolLib est installé
- Combat intercepté au niveau des paquets client
- Correctif définitif de la collision joueur ↔ mob (équipe `NEVER`) pour la 1.21
- Repli automatique natif en cas d'échec — le plugin ne plante jamais

### v1.2.0
- Leaderboards en hologramme (Top 10 Seekers/Hiders)
- Support optionnel de ProtocolLib pour un camouflage sans collision
- Message cliquable en fin de partie (Rejouer/Quitter)
- Bouton "Partie rapide" dans le GUI
- Multiples corrections de bugs

### v1.1.0
- Vrais mobs sur la map pour le camouflage
- Plateforme du lobby
- Scoreboards persistants par arène
- Correction des bugs de frappe des mobs

### v1.0.0
- Version initiale

---

**Amusez-vous bien ! 🎮**
