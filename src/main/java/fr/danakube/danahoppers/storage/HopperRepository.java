package fr.danakube.danahoppers.storage;

import fr.danakube.danahoppers.model.CustomHopper;
import fr.danakube.danahoppers.model.FilterMode;
import fr.danakube.danahoppers.model.HopperFilter;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Gestion de la persistance SQL (CRUD asynchrone) pour les CustomHopper.
 */
public class HopperRepository {

    private final DatabaseManager databaseManager;

    public HopperRepository(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    /**
     * Sauvegarde ou met à jour un CustomHopper dans la base de données de manière asynchrone.
     */
    public CompletableFuture<Void> saveHopper(CustomHopper hopper) {
        return CompletableFuture.runAsync(() -> {
            String sql = """
                INSERT INTO danahopper_blocks (
                    hopper_uuid, owner_uuid, world_name, x, y, z,
                    type_id, tier, linked_world, linked_x, linked_y, linked_z,
                    filter_mode, filter_materials, items_transferred,
                    hologram_enabled, teleport_enabled
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT(hopper_uuid) DO UPDATE SET
                    owner_uuid = excluded.owner_uuid,
                    world_name = excluded.world_name,
                    x = excluded.x,
                    y = excluded.y,
                    z = excluded.z,
                    type_id = excluded.type_id,
                    tier = excluded.tier,
                    linked_world = excluded.linked_world,
                    linked_x = excluded.linked_x,
                    linked_y = excluded.linked_y,
                    linked_z = excluded.linked_z,
                    filter_mode = excluded.filter_mode,
                    filter_materials = excluded.filter_materials,
                    items_transferred = excluded.items_transferred,
                    hologram_enabled = excluded.hologram_enabled,
                    teleport_enabled = excluded.teleport_enabled;
                """;

            try (Connection conn = databaseManager.getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(sql)) {

                Location loc = hopper.getLocation();
                Location linked = hopper.getLinkedLocation();

                pstmt.setString(1, hopper.getHopperUuid().toString());
                pstmt.setString(2, hopper.getOwnerUuid().toString());
                pstmt.setString(3, loc.getWorld() != null ? loc.getWorld().getName() : "world");
                pstmt.setInt(4, loc.getBlockX());
                pstmt.setInt(5, loc.getBlockY());
                pstmt.setInt(6, loc.getBlockZ());

                pstmt.setString(7, hopper.getTypeId());
                pstmt.setInt(8, hopper.getTier());

                if (linked != null) {
                    pstmt.setString(9, linked.getWorld() != null ? linked.getWorld().getName() : "world");
                    pstmt.setInt(10, linked.getBlockX());
                    pstmt.setInt(11, linked.getBlockY());
                    pstmt.setInt(12, linked.getBlockZ());
                } else {
                    pstmt.setNull(9, Types.VARCHAR);
                    pstmt.setNull(10, Types.INTEGER);
                    pstmt.setNull(11, Types.INTEGER);
                    pstmt.setNull(12, Types.INTEGER);
                }

                HopperFilter filter = hopper.getFilter();
                pstmt.setString(13, filter != null ? filter.getMode().name() : FilterMode.WHITELIST.name());
                pstmt.setString(14, filter != null ? filter.serializeMaterials() : "");
                pstmt.setLong(15, hopper.getItemsTransferred());
                pstmt.setBoolean(16, hopper.isHologramEnabled());
                pstmt.setBoolean(17, hopper.isTeleportEnabled());

                pstmt.executeUpdate();
            } catch (SQLException e) {
                throw new RuntimeException("Erreur lors de la sauvegarde du hopper " + hopper.getHopperUuid(), e);
            }
        });
    }

    /**
     * Supprime un CustomHopper par son UUID de manière asynchrone.
     */
    public CompletableFuture<Void> deleteHopper(UUID hopperUuid) {
        return CompletableFuture.runAsync(() -> {
            String sql = "DELETE FROM danahopper_blocks WHERE hopper_uuid = ?;";
            try (Connection conn = databaseManager.getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, hopperUuid.toString());
                pstmt.executeUpdate();
            } catch (SQLException e) {
                throw new RuntimeException("Erreur lors de la suppression du hopper " + hopperUuid, e);
            }
        });
    }

    /**
     * Supprime un CustomHopper par sa position de manière asynchrone.
     */
    public CompletableFuture<Void> deleteHopper(Location location) {
        return CompletableFuture.runAsync(() -> {
            String sql = "DELETE FROM danahopper_blocks WHERE world_name = ? AND x = ? AND y = ? AND z = ?;";
            try (Connection conn = databaseManager.getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, location.getWorld() != null ? location.getWorld().getName() : "world");
                pstmt.setInt(2, location.getBlockX());
                pstmt.setInt(3, location.getBlockY());
                pstmt.setInt(4, location.getBlockZ());
                pstmt.executeUpdate();
            } catch (SQLException e) {
                throw new RuntimeException("Erreur lors de la suppression du hopper à la position " + location, e);
            }
        });
    }

    /**
     * Charge tous les CustomHoppers depuis la base de données de manière asynchrone.
     */
    public CompletableFuture<List<CustomHopper>> loadAllHoppers() {
        return CompletableFuture.supplyAsync(() -> {
            List<CustomHopper> hoppers = new ArrayList<>();
            String sql = """
                SELECT hopper_uuid, owner_uuid, world_name, x, y, z, type_id, tier,
                       linked_world, linked_x, linked_y, linked_z,
                       filter_mode, filter_materials, items_transferred, hologram_enabled, teleport_enabled
                FROM danahopper_blocks;
                """;

            try (Connection conn = databaseManager.getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(sql);
                 ResultSet rs = pstmt.executeQuery()) {

                while (rs.next()) {
                    UUID hopperUuid = UUID.fromString(rs.getString("hopper_uuid"));
                    UUID ownerUuid = UUID.fromString(rs.getString("owner_uuid"));

                    String worldName = rs.getString("world_name");
                    int x = rs.getInt("x");
                    int y = rs.getInt("y");
                    int z = rs.getInt("z");
                    World world = Bukkit.getServer() != null ? Bukkit.getWorld(worldName) : null;
                    Location loc = new Location(world, x, y, z);

                    String typeId = rs.getString("type_id");
                    int tier = rs.getInt("tier");

                    String linkedWorldName = rs.getString("linked_world");
                    Location linkedLoc = null;
                    if (linkedWorldName != null) {
                        int lx = rs.getInt("linked_x");
                        int ly = rs.getInt("linked_y");
                        int lz = rs.getInt("linked_z");
                        World linkedWorld = Bukkit.getServer() != null ? Bukkit.getWorld(linkedWorldName) : null;
                        linkedLoc = new Location(linkedWorld, lx, ly, lz);
                    }

                    FilterMode mode = FilterMode.WHITELIST;
                    try {
                        mode = FilterMode.valueOf(rs.getString("filter_mode"));
                    } catch (IllegalArgumentException ignored) {}

                    List<Material> materials = HopperFilter.deserializeMaterials(rs.getString("filter_materials"));
                    HopperFilter filter = new HopperFilter(mode, materials);

                    long itemsTransferred = rs.getLong("items_transferred");
                    boolean hologramEnabled = rs.getBoolean("hologram_enabled");
                    boolean teleportEnabled = rs.getBoolean("teleport_enabled");

                    CustomHopper hopper = new CustomHopper(
                            hopperUuid,
                            ownerUuid,
                            loc,
                            typeId,
                            tier,
                            linkedLoc,
                            filter,
                            itemsTransferred,
                            hologramEnabled,
                            teleportEnabled
                    );
                    hoppers.add(hopper);
                }
            } catch (SQLException e) {
                throw new RuntimeException("Erreur lors du chargement des hoppers", e);
            }

            return hoppers;
        });
    }
}
