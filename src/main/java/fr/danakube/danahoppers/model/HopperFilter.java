package fr.danakube.danahoppers.model;

import org.bukkit.Material;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

/**
 * Représente le système de filtrage d'un CustomHopper.
 */
public class HopperFilter {

    private FilterMode mode;
    private final List<Material> materials;

    public HopperFilter() {
        this(FilterMode.BLACKLIST, new ArrayList<>());
    }

    public HopperFilter(FilterMode mode) {
        this(mode, new ArrayList<>());
    }

    public HopperFilter(FilterMode mode, List<Material> materials) {
        this.mode = Objects.requireNonNullElse(mode, FilterMode.BLACKLIST);
        this.materials = new CopyOnWriteArrayList<>();
        if (materials != null) {
            for (Material mat : materials) {
                if (mat != null && !this.materials.contains(mat)) {
                    this.materials.add(mat);
                }
            }
        }
    }

    private static boolean isAirMaterial(Material material) {
        return material == Material.AIR || material == Material.CAVE_AIR || material == Material.VOID_AIR;
    }

    /**
     * Vérifie si un matériau correspond au filtre.
     *
     * @param material Le matériau à tester
     * @return true si le matériau est accepté par le filtre, false sinon
     */
    public boolean matches(Material material) {
        if (material == null || isAirMaterial(material)) {
            return false;
        }

        boolean contains = materials.contains(material);
        if (mode == FilterMode.WHITELIST) {
            return contains;
        } else {
            return !contains;
        }
    }

    /**
     * Ajoute un matériau au filtre.
     *
     * @param material Le matériau à ajouter
     * @return true si le matériau a été ajouté, false s'il était déjà présent ou nul
     */
    public boolean addMaterial(Material material) {
        if (material == null || isAirMaterial(material) || materials.contains(material)) {
            return false;
        }
        return materials.add(material);
    }

    /**
     * Retire un matériau du filtre.
     *
     * @param material Le matériau à retirer
     * @return true si le matériau a été retiré, false sinon
     */
    public boolean removeMaterial(Material material) {
        if (material == null) {
            return false;
        }
        return materials.remove(material);
    }

    /**
     * Vide la liste des matériaux filtrés.
     */
    public void clearMaterials() {
        materials.clear();
    }

    public FilterMode getMode() {
        return mode;
    }

    public void setMode(FilterMode mode) {
        this.mode = Objects.requireNonNull(mode, "Mode cannot be null");
    }

    public List<Material> getMaterials() {
        return Collections.unmodifiableList(new ArrayList<>(materials));
    }

    /**
     * Sérialise la liste de matériaux en une chaîne séparée par des virgules.
     */
    public String serializeMaterials() {
        return materials.stream()
                .map(Enum::name)
                .collect(Collectors.joining(","));
    }

    /**
     * Désérialise une chaîne de matériaux séparés par des virgules.
     */
    public static List<Material> deserializeMaterials(String data) {
        List<Material> list = new ArrayList<>();
        if (data == null || data.isBlank()) {
            return list;
        }
        String[] parts = data.split(",");
        for (String part : parts) {
            String trimmed = part.trim();
            if (!trimmed.isEmpty()) {
                try {
                    Material mat = Material.valueOf(trimmed);
                    if (!list.contains(mat)) {
                        list.add(mat);
                    }
                } catch (IllegalArgumentException ignored) {
                    // Ignorer les matériaux invalides/inconnus
                }
            }
        }
        return list;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        HopperFilter filter = (HopperFilter) o;
        return mode == filter.mode && Objects.equals(materials, filter.materials);
    }

    @Override
    public int hashCode() {
        return Objects.hash(mode, materials);
    }
}
