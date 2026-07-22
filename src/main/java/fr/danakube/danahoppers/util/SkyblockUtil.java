package fr.danakube.danahoppers.util;

import com.bgsoftware.superiorskyblock.api.SuperiorSkyblockAPI;
import com.bgsoftware.superiorskyblock.api.island.Island;
import com.bgsoftware.superiorskyblock.api.wrappers.SuperiorPlayer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.UUID;

/**
 * Classe utilitaire pour isoler l'intégration facultative avec SuperiorSkyblock2.
 */
public final class SkyblockUtil {

    private SkyblockUtil() {
        // Classe utilitaire
    }

    /**
     * Vérifie si le plugin SuperiorSkyblock2 est présent et activé.
     */
    public static boolean isSkyblockActive() {
        return Bukkit.getPluginManager().getPlugin("SuperiorSkyblock2") != null &&
               Bukkit.getPluginManager().getPlugin("SuperiorSkyblock2").isEnabled();
    }

    /**
     * Récupère l'île à un emplacement donné.
     */
    public static Island getIslandAt(Location loc) {
        if (!isSkyblockActive()) {
            return null;
        }
        return SuperiorSkyblockAPI.getIslandAt(loc);
    }

    /**
     * Récupère l'île associée à un joueur.
     */
    public static Island getIsland(Player player) {
        if (!isSkyblockActive()) {
            return null;
        }
        SuperiorPlayer superiorPlayer = SuperiorSkyblockAPI.getPlayer(player.getUniqueId());
        return superiorPlayer != null ? superiorPlayer.getIsland() : null;
    }

    /**
     * Récupère l'île associée à un UUID de joueur.
     */
    public static Island getIsland(UUID playerUuid) {
        if (!isSkyblockActive()) {
            return null;
        }
        SuperiorPlayer superiorPlayer = SuperiorSkyblockAPI.getPlayer(playerUuid);
        return superiorPlayer != null ? superiorPlayer.getIsland() : null;
    }

    /**
     * Vérifie si le joueur est membre de l'île donnée (ou en est le chef).
     */
    public static boolean isIslandMember(Player player, Island island) {
        if (island == null) {
            return false;
        }
        SuperiorPlayer superiorPlayer = SuperiorSkyblockAPI.getPlayer(player.getUniqueId());
        return superiorPlayer != null && (island.isMember(superiorPlayer) || island.getOwner().getUniqueId().equals(player.getUniqueId()));
    }
}
