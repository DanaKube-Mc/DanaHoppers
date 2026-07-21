package fr.danakube.danahoppers.manager;

import fr.danakube.danahoppers.model.CustomHopper;
import fr.danakube.danahoppers.storage.HopperRepository;
import org.bukkit.Location;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Gère le cache mémoire thread-safe (ConcurrentHashMap) et l'interaction avec le HopperRepository.
 */
public class HopperManager {

    private final HopperRepository repository;
    private final ConcurrentHashMap<Location, CustomHopper> hopperCache;

    public HopperManager(HopperRepository repository) {
        this.repository = repository;
        this.hopperCache = new ConcurrentHashMap<>();
    }

    /**
     * Normalise une localisation pour l'utiliser comme clé de bloc exacte.
     */
    public static Location toBlockLocation(Location loc) {
        if (loc == null) return null;
        return new Location(loc.getWorld(), loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
    }

    /**
     * Enregistre un CustomHopper dans le cache mémoire et déclenche la sauvegarde en base.
     */
    public CompletableFuture<Void> registerHopper(CustomHopper hopper) {
        if (hopper == null || hopper.getLocation() == null) {
            return CompletableFuture.completedFuture(null);
        }

        Location blockLoc = toBlockLocation(hopper.getLocation());
        hopperCache.put(blockLoc, hopper);

        if (repository != null) {
            return repository.saveHopper(hopper);
        }
        return CompletableFuture.completedFuture(null);
    }

    /**
     * Supprime un CustomHopper du cache mémoire et déclenche sa suppression en base.
     */
    public CompletableFuture<Void> unregisterHopper(Location location) {
        if (location == null) {
            return CompletableFuture.completedFuture(null);
        }

        Location blockLoc = toBlockLocation(location);
        CustomHopper removed = hopperCache.remove(blockLoc);

        if (repository != null) {
            if (removed != null) {
                return repository.deleteHopper(removed.getHopperUuid());
            } else {
                return repository.deleteHopper(blockLoc);
            }
        }
        return CompletableFuture.completedFuture(null);
    }

    /**
     * Récupère un CustomHopper depuis le cache mémoire (O(1), ultra-rapide).
     */
    public CustomHopper getHopper(Location location) {
        if (location == null) return null;
        return hopperCache.get(toBlockLocation(location));
    }

    /**
     * Récupère un CustomHopper sous forme d'Optional.
     */
    public Optional<CustomHopper> getHopperOptional(Location location) {
        return Optional.ofNullable(getHopper(location));
    }

    /**
     * Indique si un bloc est un CustomHopper enregistré.
     */
    public boolean isCustomHopper(Location location) {
        if (location == null) return false;
        return hopperCache.containsKey(toBlockLocation(location));
    }

    /**
     * Charge l'ensemble des hoppers de la base de données vers le cache mémoire.
     */
    public CompletableFuture<Void> loadAllAsync() {
        if (repository == null) {
            return CompletableFuture.completedFuture(null);
        }

        return repository.loadAllHoppers().thenAccept(hoppers -> {
            hopperCache.clear();
            for (CustomHopper hopper : hoppers) {
                if (hopper.getLocation() != null) {
                    hopperCache.put(toBlockLocation(hopper.getLocation()), hopper);
                }
            }
        });
    }

    /**
     * Sauvegarde tous les hoppers actuellement en cache.
     */
    public CompletableFuture<Void> saveAllAsync() {
        if (repository == null) {
            return CompletableFuture.completedFuture(null);
        }

        CompletableFuture<?>[] futures = hopperCache.values().stream()
                .map(repository::saveHopper)
                .toArray(CompletableFuture[]::new);

        return CompletableFuture.allOf(futures);
    }

    /**
     * Récupère une vue non modifiable des hoppers en cache.
     */
    public Collection<CustomHopper> getAllHoppers() {
        return Collections.unmodifiableCollection(hopperCache.values());
    }

    /**
     * Récupère la carte mémoire complète.
     */
    public Map<Location, CustomHopper> getHopperMap() {
        return Collections.unmodifiableMap(hopperCache);
    }

    /**
     * Vide le cache mémoire.
     */
    public void clearCache() {
        hopperCache.clear();
    }
}
