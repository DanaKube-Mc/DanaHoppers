package fr.danakube.danahoppers.storage;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.CompletableFuture;

/**
 * Gère le pool de connexions HikariCP et l'initialisation de la base de données SQLite.
 */
public class DatabaseManager {

    private final Plugin plugin;
    private final File dbFile;
    private HikariDataSource dataSource;

    public DatabaseManager(Plugin plugin) {
        this.plugin = plugin;
        this.dbFile = new File(plugin.getDataFolder(), "danahoppers.db");
    }

    public DatabaseManager(File dbFile) {
        this.plugin = null;
        this.dbFile = dbFile;
    }

    /**
     * Initialise le pool HikariCP et crée les tables de manière asynchrone.
     */
    public CompletableFuture<Void> initAsync() {
        return CompletableFuture.runAsync(() -> {
            try {
                initPool();
                createTables();
                if (plugin != null) {
                    plugin.getLogger().info("Base de données SQLite initialisée avec succès.");
                }
            } catch (Exception e) {
                if (plugin != null) {
                    plugin.getLogger().severe("Erreur lors de l'initialisation de la base SQLite: " + e.getMessage());
                }
                throw new RuntimeException("Échec d'initialisation de la base de données", e);
            }
        });
    }

    private synchronized void initPool() {
        if (dataSource != null && !dataSource.isClosed()) {
            return;
        }

        if (dbFile.getParentFile() != null && !dbFile.getParentFile().exists()) {
            dbFile.getParentFile().mkdirs();
        }

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:sqlite:" + dbFile.getAbsolutePath());
        config.setDriverClassName("org.sqlite.JDBC");
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        config.setIdleTimeout(30000);
        config.setConnectionTimeout(10000);
        config.setPoolName("DanaHoppers-SQLite");

        // Optimisations SQLite pour performances
        config.addDataSourceProperty("journal_mode", "WAL");
        config.addDataSourceProperty("synchronous", "NORMAL");

        this.dataSource = new HikariDataSource(config);
    }

    private void createTables() throws SQLException {
        String query = """
            CREATE TABLE IF NOT EXISTS danahopper_blocks (
                hopper_uuid VARCHAR(36) PRIMARY KEY,
                owner_uuid VARCHAR(36) NOT NULL,
                world_name VARCHAR(64) NOT NULL,
                x INT NOT NULL,
                y INT NOT NULL,
                z INT NOT NULL,
                type_id VARCHAR(64) NOT NULL,
                tier INT NOT NULL,
                linked_world VARCHAR(64),
                linked_x INT,
                linked_y INT,
                linked_z INT,
                filter_mode VARCHAR(16) NOT NULL,
                filter_materials TEXT NOT NULL,
                items_transferred BIGINT NOT NULL DEFAULT 0,
                hologram_enabled BOOLEAN NOT NULL DEFAULT 1,
                teleport_enabled BOOLEAN NOT NULL DEFAULT 0
            );
            """;

        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(query);
        }
    }

    /**
     * Obtient une connexion du pool HikariCP.
     */
    public Connection getConnection() throws SQLException {
        if (dataSource == null || dataSource.isClosed()) {
            throw new SQLException("DatabaseManager n'est pas initialisé ou a été fermé.");
        }
        return dataSource.getConnection();
    }

    /**
     * Ferme proprement le pool de connexions HikariCP.
     */
    public synchronized void close() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            if (plugin != null) {
                plugin.getLogger().info("Pool de connexions SQLite fermé avec succès.");
            }
        }
    }
}
