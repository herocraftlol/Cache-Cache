package com.hideseek.cachecache.map;

import org.bukkit.Material;

public enum Scenario {

    HUNT_INFINITE("§d§l✦ Traque Éternelle", Material.FIREWORK_ROCKET,
            "§d\"La nuit ne s'arrête jamais...\"",
            "Les joueurs cachés peuvent utiliser leur feu d'artifice de traque à l'infini, pendant toute la partie."),

    INFINITE_HITS("§6§l⚔ Fureur Dorée", Material.GOLDEN_SWORD,
            "§6\"Aucune limite à sa soif de victoire.\"",
            "Le(s) Seeker(s) disposent d'un nombre de coups illimité (le killmax est ignoré)."),

    INFINITE_TIME("§b§l⏳ Temps Suspendu", Material.CLOCK,
            "§b\"Ici, les secondes n'existent plus.\"",
            "La durée de la partie devient indéfinie. La désactiver restaure la durée initiale de la map."),

    TROLL_SWORD("§7§l🗡 Provocation", Material.IRON_SWORD,
            "§7\"Frappe fort... mais pour rien.\"",
            "Les joueurs cachés peuvent frapper le Seeker, mais cela ne lui inflige aucun dégât. Un pur outil de troll."),

    ANVIL("§8§l⬛ Pluie de Fer", Material.ANVIL,
            "§8\"Attention à ce qui tombe du ciel.\"",
            "Le Seeker reçoit une enclume indestructible : un clic droit fait tomber une enclume sur la tête de tous les cachés (recharge : 2 min)."),

    TNT_LAUNCHER("§c§l💥 Souffle Explosif", Material.TNT,
            "§c\"Boum, mais sans dégâts !\"",
            "Les cachés reçoivent une TNT non posable. Un clic droit la propulse devant eux ; elle explose après 3s sans aucun dégât, juste un souffle."),

    BOW("§e§l🏹 Œil de Faucon", Material.BOW,
            "§e\"Il ne rate jamais sa cible.\"",
            "Le Seeker reçoit un arc capable d'one-shot à distance (tirs illimités si Fureur Dorée est active)."),

    HOSTILE_MOBS("§2§l☠ Instinct Sauvage", Material.SKELETON_SPAWN_EGG,
            "§2\"La nature elle-même se retourne contre lui.\"",
            "Activé : les mobs hostiles attaquent le Seeker (sans lui infliger de dégât). Désactivé : aucun mob n'attaque jamais le Seeker."),

    MOB_SWAP("§5§l🌀 Métamorphose", Material.WIND_CHARGE,
            "§5\"Rien n'est jamais figé bien longtemps.\"",
            "À un moment aléatoire de la partie, tous les joueurs cachés changent une seule fois de mob d'apparence."),

    BLAZE_ROD("§4§l⚡ Colère Céleste", Material.BLAZE_ROD,
            "§4\"La foudre frappe où il le décide.\"",
            "Les cachés reçoivent un bâton de Blaze : un clic droit fait tomber la foudre sur le bloc visé, à n'importe quelle distance (recharge : 1 min).");

    private final String displayName;
    private final Material icon;
    private final String flavor;
    private final String description;

    Scenario(String displayName, Material icon, String flavor, String description) {
        this.displayName = displayName;
        this.icon = icon;
        this.flavor = flavor;
        this.description = description;
    }

    public String getDisplayName() { return displayName; }
    public Material getIcon() { return icon; }
    public String getFlavor() { return flavor; }
    public String getDescription() { return description; }
}
