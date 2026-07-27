package com.hideseek.cachecache.map;

import org.bukkit.Material;

public enum Scenario {

    HUNT_INFINITE("Hunt infini", Material.FIREWORK_ROCKET,
            "Les joueurs cachés peuvent utiliser le hunt (feu d'artifice) à l'infini pendant toute la partie."),
    INFINITE_HITS("Coups infinis", Material.GOLDEN_SWORD,
            "Le(s) Seeker(s) ont un nombre de coups illimité (killmax ignoré)."),
    INFINITE_TIME("Temps indéfini", Material.CLOCK,
            "La durée de la partie est indéfinie. Une fois désactivé, le temps initial de la map est restauré."),
    TROLL_SWORD("Épée de troll", Material.IRON_SWORD,
            "Les joueurs cachés peuvent frapper le Seeker, mais cela ne lui fait aucun dégât : juste pour le troll."),
    ANVIL("Enclume", Material.ANVIL,
            "Le Seeker reçoit une enclume non posable/déplaçable : clic droit fait tomber une enclume sur la tête de tous les cachés (recharge 2 min)."),
    TNT_LAUNCHER("TNT propulsive", Material.TNT,
            "Les cachés reçoivent une TNT non posable. Clic droit la lance devant eux ; elle explose après 3s sans dégât, juste un souffle de propulsion."),
    BOW("Arc du Seeker", Material.BOW,
            "Le Seeker reçoit un arc et peut tirer des flèches qui one-shot (infinies si le scénario Coups Infinis est actif)."),
    HOSTILE_MOBS("Oeuf de Squelette", Material.SKELETON_SPAWN_EGG,
            "Si activé, les mobs hostiles attaquent le Seeker (sans lui faire de dégât). Si désactivé, aucun mob n'attaque le Seeker."),
    MOB_SWAP("Charge de vent", Material.WIND_CHARGE,
            "À un moment aléatoire de la partie, tous les joueurs cachés changent de mob d'apparence (une seule fois)."),
    BLAZE_ROD("Bâton de Blaze", Material.BLAZE_ROD,
            "Les cachés reçoivent un bâton de Blaze : clic droit fait tomber la foudre sur le bloc visé, à n'importe quelle distance (recharge 1 min).");

    private final String displayName;
    private final Material icon;
    private final String description;

    Scenario(String displayName, Material icon, String description) {
        this.displayName = displayName;
        this.icon = icon;
        this.description = description;
    }

    public String getDisplayName() { return displayName; }
    public Material getIcon() { return icon; }
    public String getDescription() { return description; }
}
