# 🔮 DanaHoppers

![Minecraft Paper 1.21](https://img.shields.io/badge/Minecraft-Paper%201.21.x-brightgreen?style=for-the-badge&logo=minecraft)
![Java 21](https://img.shields.io/badge/Java-21-orange?style=for-the-badge&logo=openjdk)
![Build Maven](https://img.shields.io/badge/Build-Maven-blue?style=for-the-badge&logo=apachemaven)
![License](https://img.shields.io/badge/License-GPL--3.0-lightgrey?style=for-the-badge)

**DanaHoppers** est un plugin Minecraft hautement optimisé et entièrement configurable développé pour l'écosystème **DanaKube**. Il introduit des entonnoirs (hoppers) personnalisés dotés d'aspiration avancée, de liaison universelle à distance, d'hologrammes natifs et de téléportation de joueur.

---

## ✨ Fonctionnalités Principales

### 📦 1. Familles d'Entonnoirs Personnalisés
* 🔷 **Collecteur Élargi** : Aspire les objets au sol dans une zone en forme de cube autour du bloc. Sa portée s'étend au fil des améliorations de Tiers (Rayons : 3x3x3, 5x5x5, 7x7x7).
* 🔶 **Collecteur de Zone** : Aspire tous les objets déposés dans **l'intégralité du Chunk** (`16 x 384 x 16`), peu importe la hauteur ou les murs.
* 🔮 **Hopper Sautelien** : Un entonnoir orienté mobilité. Lorsqu'un joueur se trouve sur le bloc et s'accroupit (**Touche Maj / Sneak**), il est instantanément et de manière asynchrone téléporté au-dessus du conteneur lié.

### 🔗 2. Liaison Universelle à Distance
* Tous les entonnoirs personnalisés peuvent se lier à un conteneur cible distant (coffre, double coffre, Shulker Box, autre entonnoir).
* **Transfert Direct** : Tout objet aspiré par l'entonnoir est immédiatement expédié dans le conteneur distant lié.
* **Sécurité Anti-Lag** : La tâche d'aspiration et de transfert vérifie que le chunk cible est bien chargé en mémoire pour éviter tout pic de lag serveur (lag spike).

### 🏷️ 3. Hologrammes Natifs Paper (`TextDisplay`)
* Affichage dynamique du statut, du niveau (Tier) et du nombre d'objets transférés au-dessus du bloc.
* Basé sur les entités natives `TextDisplay` de Paper 1.21 : **zero plugin externe requis** (pas besoin de DecentHolograms ou HolographicDisplays), ultra-léger pour le CPU et le réseau.
* Activable ou désactivable par le joueur via l'interface du hopper.

### ⚙️ 4. Performance & Persistence Avancée
* **Aspiration par Stack Complète** : Les objets au sol sont aspirés pile entière par pile entière (Stack merge), préservant le Tick Rate serveur.
* **Double Persistance** :
  * **Bukkit PDC (`PersistentDataContainer`)** : Métadonnées sérialisées directement sur la `TileState` du bloc en jeu.
  * **Base SQLite Asynchrone (HikariCP)** : Indexation SQLite avec écriture 100% asynchrone pour un chargement instantané au démarrage du serveur.
* **Desactivation Vanilla** : Les entonnoirs custom désactivent l'aspiration native Minecraft (`InventoryPickupItemEvent`) pour éliminer tout surcoût CPU vanilla.

### 🛡️ 5. Intégrations & Protections
* 🏝️ **SuperiorSkyblock2** : Vérification des membres de l'île lors du posage, du cassage et de l'interaction avec le hopper.
* 💵 **Vault** : Prise en charge de l'économie pour les coûts d'amélioration (Upgrades).
* 💥 **Protection Anti-Explosion & Anti-Piston** : Les entonnoirs custom ne peuvent pas être déplacés par des pistons ni détruits par les explosions de TNT/Creeper si configuré.
* 🎨 **Charte Adventure MiniMessage** : Interfaces et messages 100% personnalisables avec gradients de couleurs et effets visuels.

---

## 🛠️ Commandes & Permissions

| Commande | Description | Permission |
| :--- | :--- | :--- |
| `/danahopper give <joueur> <type> [tier] [quantité]` | Donne un entonnoir custom à un joueur | `danahoppers.admin` / `danahoppers.give` |
| `/danahopper reload` | Recharge les configurations YAML et fichiers de langue | `danahoppers.admin` / `danahoppers.reload` |
| `/danahopper info` | Affiche les statistiques et informations d'un hopper ciblé | `danahoppers.use` |

### 🔒 Limites de Pose par Permission
Les joueurs peuvent être restreints sur le nombre maximal de hoppers posés en fonction de leurs permissions :
* `danahoppers.place.default` (ex: 3 hoppers)
* `danahoppers.place.vip` (ex: 10 hoppers)
* `danahoppers.place.mvp` (ex: 20 hoppers)
* `danahoppers.admin` (Illimité)

---

## 📁 Structure des Fichiers de Configuration

```text
plugins/DanaHoppers/
├── config.yml                # Configuration globale, BDD, limites & intégrations
├── hoppers/                  # Familles de Hoppers & réglage des Tiers
│   ├── collecteur_elargi.yml
│   ├── collecteur_zone.yml
│   └── sautelien.yml
├── gui/                      # Configuration complète des inventaires (GUIs)
│   ├── main_menu.yml         # Panneau de contrôle principal (27 slots)
│   └── filter_menu.yml       # Interface du Filtre (Whitelist/Blacklist)
└── lang/                     # Fichiers de traductions (Adventure MiniMessage)
    └── fr_FR.yml
```

---

## 💻 Compilation & Installation

### Prérequis
* **JDK 21**
* **Apache Maven 3.8+**
* Serveur Minecraft sous **Paper 1.21.x** (ou fork compatible)

### Compilation du Fichier JAR

Exécutez la commande suivante à la racine du projet :

```bash
mvn clean package
```

Le fichier JAR compilé et ombré (shaded) sera généré sous :
`target/DanaHoppers-1.0.0.jar`

### Installation
1. Glissez le fichier `DanaHoppers-1.0.0.jar` dans le dossier `plugins/` de votre serveur Paper.
2. Démarrez le serveur.
3. Personnalisez les fichiers YML générés dans `plugins/DanaHoppers/` selon vos préférences.

---

## 📄 Licence

Ce projet est sous licence **GPL-3.0**. Développé par l'équipe **DanaKube**.
