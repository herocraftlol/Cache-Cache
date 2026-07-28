# 🦊 CacheCache — Plugin Paper 1.21

Plugin de mini-jeu **Cache-Cache** (style Prop Hunt) pour serveur Minecraft Paper 1.21.

> *Un Seeker doit retrouver et éliminer tous les joueurs cachés avant la fin du temps !*

---

## 🎮 Fonctionnalités principales

### Mode Cache-Cache classique
- Les **joueurs cachés** se camouflent en mobs (Zombie, Squelette, Enderman, Vache, etc.)
- Le **Seeker** doit les retrouver et les éliminer avec un nombre limité de coups
- Comptez sur votre sens de l'observation et votre adresse pour survivre !

### Camouflage 100% natif (sans dépendances)
- Le vrai joueur est **invisible** pour tous les autres joueurs
- Un **mob fantôme** le remplace visuellement et suit ses mouvements
- Les autres joueurs ne peuvent voir et interagir qu'avec ce mob fantôme

### Scénarios可选 (10 scénarios)
- **Blind** : Le Seeker a un bandeau sur les yeux
- **Flash** : Lumière aveuglante périodique
- **Speed** : Les cachés sont plus rapides
- **Invisible** : Les cachés sont invisibles (même le mob)
- **Enclume** : Des enclumes tombent périodiquement
- **Double Jump** : Les cachés ont un double saut
- **Compass** : Le Seeker a une boussole
- Et plus encore !

### Modes de jeu
- **Mode Virus** : Le Seeker infecte les cachés au lieu de les tuer
- **Multi-Seekers** : Plusieurs Seekers peuvent jouer simultanément
- **Hunt** : Des événements périodiques révèlent brièvement la position des Seekers

### Interface graphique (GUI)
- Liste des arènes avec indication du statut (en attente, en cours, pleine)
- Sélection des scénarios pour chaque map
- Menu de configuration intuitif

---

## ✨ Nouveautés de la version 1.0.8

- **Scoreboards persistants par arène** : Les scoreboards sont maintenant créés une seule fois par arène et mis à jour en place, pour de meilleures performances
- **Joueurs non-collisionables** : Les joueurs cachés ne peuvent plus être poussés par les mobs ou autres joueurs
- **Confinement des spectateurs** : Les spectateurs sont limités à la zone de l'arène et renvoyés au centre s'ils atteignent les bords
- **Protection anti-bug de frappe** : Correction d'un problème où le Seeker ne pouvait pas frapper les mobs
- **Amélioration de la synchronisation** : Réduction des mouvements parasites des mobs fantômes
- **Protection de la map** : Les blocs ne peuvent pas être cassés ou posés pendant les parties

---

## 📋 Commandes

| Commande | Description |
|----------|-------------|
| `/cc create <nom>` | Créer une nouvelle arène |
| `/cc <nom> pos1/pos2` | Définir la zone de jeu |
| `/cc <nom> posconfirm` | Confirmer la zone |
| `/cc <nom> spawnseek` | Définir le point d'apparition du Seeker |
| `/cc <nom> lobby` | Définir le lobby d'attente |
| `/cc <nom> time <ticks>` | Définir la durée de la partie |
| `/cc <nom> killmax <n>` | Nombre de coups du Seeker |
| `/cc <nom> maxplayers <n>` | Nombre maximum de joueurs |
| `/cc <nom> mob <type> [%]` | Ajouter un type de mob |
| `/cc <nom> seeker <n>` | Nombre de Seekers |
| `/cc <nom> seeker virus` | Activer le mode virus |
| `/cc <nom> scenario` | Ouvrir le GUI des scénarios |
| `/cc <nom> save` | Sauvegarder l'arène |
| `/cc <nom> config` | Repasser en mode édition |
| `/cc <nom> delete` | Supprimer l'arène |
| `/cc <nom> rename <nom>` | Renommer l'arène |
| `/cc list` | Lister toutes les arènes |
| `/cc gui` | Ouvrir la liste des parties |
| `/cc join <nom>` | Rejoindre une arène |
| `/cc leave` | Quitter l'arène actuelle |
| `/cc hub` | Définir le hub principal |
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

Le plugin crée automatiquement les fichiers de configuration nécessaires dans `plugins/CacheCache/`.

---

## 🎯 Tips pour les joueurs

- **Cachés** : Restez mobiles et confusez-vous parmi les mobs de décor !
- **Seeker** : Écoutez les bruits de pas et surveillez les mouvements suspects

---

**Amusez-vous bien ! 🎮**
