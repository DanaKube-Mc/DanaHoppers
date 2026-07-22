package fr.danakube.danahoppers.config;

/**
 * Représentation immuable des caractéristiques d'un niveau (tier) d'un hopper.
 */
public record HopperTierConfig(
    int tier,
    String displayName,
    String material,
    int customModelData,
    String suctionType,
    int radiusX,
    int radiusY,
    int radiusZ,
    double intervalSeconds,
    int maxLinkingDistance,
    double upgradeCostMoney,
    String nextHopperId,
    int nextTier
) {}
