# 🦊 CacheCache — Plugin Paper 1.21

Plugin de mini-jeu **Cache-Cache** (style Prop Hunt / Hide & Seek) pour serveur Minecraft Paper 1.21.

> *Un Seeker doit retrouver et éliminer tous les joueurs cachés avant la fin du temps !*

---

## 🎮 Fonctionnalités principales

### Mode Cache-Cache classique
- Les **joueurs cachés** se camouflent en mobs (Zombie, Squelette, Enderman, Vache, etc.)
- Le **Seeker** doit les retrouver et les éliminer avec un nombre limité de coups
- Comptez sur votre sens de l'observation et votre adresse pour survivre !

### Camouflage 100% natif (sans dépendances)
- Le vrai joueur est **invisible** pour tous les autres joueurs
- Un **mob fantôme** le remplace visuellement et suit ses mouvements en temps réel
- Les autres joueurs ne peuvent voir et interagir qu'avec ce mob fantôme
- Les Seekers peuvent frapper les mobs de décoration ET les joueurs cachés (un coup = une élimination)

### Leaderboards en hologramme
- Classements **Top 10 Seekers** et **Top 10 Hiders** affichés sous forme d'hologrammes
- Persistants après redémarrage du serveur
- Mis à jour automatiquement à chaque fin de partie

### Modes de jeu
- **Mode Virus** : Le Seeker infecte les cachés au lieu de les tuer (ils deviennent Seekers à leur tour)
- **Multi-Seekers** : Plusieurs Seekers peuvent jouer simultanément
- **Hunt** : Des événements périodiques révèlent brièvement la position des Seekers

### 10 Scénarios可选
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

## ✨ Nouveautés de la version 1.1.0

### Améliorations du camouflage
- **Vrais mobs sur la map** : Au lancement d'une partie, de vrais mobs apparaissent aléatoirement pour que les joueurs déguisés se fondent dans la masse
- **Le Seeker one-shot aussi les mobs de décoration** : Frapper un mob consomme un coup du quota — s'acharner sur les décors peut faire perdre la partie !
- **Pas de "double vision"** : Le joueur ne voit plus son propre body superposé à son mob

### Optimisations performance
- **Scoreboards persistants par arène** : Plus de recréation à chaque tick — un scoreboard par arène, mis à jour en place
- **Joueurs non-collisionables** : Les cachés ne peuvent plus être poussés par les mobs ou autres joueurs

### Qualité de vie
- **Rejouer/Quitter en fin de partie** : Message cliquable pour rejoindre une autre arène ou rentrer au hub
- **Bouton de sortie** : Item Barrière dans l'inventaire des joueurs en lobby et des spectateurs
- **Plateforme du lobby** : Zone de 8x8 blocs invisibles pour contenir les joueurs en attente
- **Confinement des spectators** : Spectateurs bloqués dans la zone de l'arène

### Corrections de bugs
- **Correction "impossible de frapper les mobs"** : Les mobs ne sont plus increvables, le Seeker peut maintenant les frapper
- **Correction "je vois mon skin au lieu de mon mob"** : Effet d'Invisibilité permanent ajouté pour masquer le joueur

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

1. Téléchargez le fichier `CacheCache.jar` depuis la page des releases
2. Placez le fichier dans le dossier `plugins/` de votre serveur Paper 1.21
3. Redémarrez le serveur

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

**Amusez-vous bien ! 🎮**
