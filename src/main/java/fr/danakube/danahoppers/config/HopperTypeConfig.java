package fr.danakube.danahoppers.config;

import java.util.List;
import java.util.Map;

/**
 * Représentation d'une famille de hopper (type) regroupant ses différents tiers.
 */
public record HopperTypeConfig(
    String id,
    String name,
    List<String> baseLore,
    Map<Integer, HopperTierConfig> tiers
) {
    /**
     * Récupère la configuration d'un tier spécifique.
     *
     * @param tier le numéro du tier
     * @return la configuration HopperTierConfig ou null si non trouvé
     */
    public HopperTierConfig getTier(int tier) {
        return tiers != null ? tiers.get(tier) : null;
    }

    /**
     * Vérifie si ce type de hopper possède le tier spécifié.
     *
     * @param tier le numéro du tier
     * @return true si le tier existe
     */
    public boolean hasTier(int tier) {
        return tiers != null && tiers.containsKey(tier);
    }
}
