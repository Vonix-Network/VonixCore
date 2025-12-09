package network.vonix.vonixcore.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import network.vonix.vonixcore.VonixCore;
import network.vonix.vonixcore.config.DatabaseConfig;

import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Level;

public class Database {

    public enum DatabaseType {
        SQLITE, MYSQL, POSTGRESQL, TURSO, SUPABASE
    }

    private final VonixCore plugin;
    private HikariDataSource dataSource;
    private DatabaseType databaseType = DatabaseType.SQLITE;

    public Database(VonixCore plugin) {
        this.plugin = plugin;
    }

    public void initialize() throws SQLException {
        String dbType = DatabaseConfig.type.toLowerCase();

        databaseType = switch (dbType) {
            case "mysql" -> DatabaseType.MYSQL;
            case "postgresql", "postgres" -> DatabaseType.POSTGRESQL;
            case "turso", "libsql" -> DatabaseType.TURSO;
            case "supabase" -> DatabaseType.SUPABASE;
            default -> DatabaseType.SQLITE;
        };

        HikariConfig config = new HikariConfig();
        config.setPoolName("VonixCore-DB-Pool");
        config.setMaximumPoolSize(DatabaseConfig.connectionPoolSize);
        config.setMinimumIdle(2);
        config.setIdleTimeout(60000);
        config.setMaxLifetime(1800000);
        config.setConnectionTimeout(DatabaseConfig.connectionTimeout);

        switch (databaseType) {
            case MYSQL -> configureMySql(config);
            case POSTGRESQL -> configurePostgreSql(config);
            case TURSO -> configureTurso(config);
            case SUPABASE -> configureSupabase(config);
            default -> configureSqlite(config);
        }

        dataSource = new HikariDataSource(config);

        createTables();
    }

    private void configureSqlite(HikariConfig config) {
        File dataFolder = plugin.getDataFolder();
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }

        String fileName = DatabaseConfig.sqliteFile;
        File dbFile = new File(dataFolder, fileName);

        config.setDriverClassName("org.sqlite.JDBC");
        config.setJdbcUrl("jdbc:sqlite:" + dbFile.getAbsolutePath());

        config.addDataSourceProperty("journal_mode", "WAL");
        config.addDataSourceProperty("synchronous", "NORMAL");
        config.addDataSourceProperty("cache_size", "10000");
        config.addDataSourceProperty("temp_store", "MEMORY");

        plugin.getLogger().info("Using SQLite database: " + dbFile.getAbsolutePath());
    }

    private void configureMySql(HikariConfig config) {
        String host = DatabaseConfig.mysqlHost;
        int port = DatabaseConfig.mysqlPort;
        String database = DatabaseConfig.mysqlDatabase;
        String username = DatabaseConfig.mysqlUsername;
        String password = DatabaseConfig.mysqlPassword;
        boolean ssl = DatabaseConfig.mysqlSsl;

        config.setDriverClassName("com.mysql.cj.jdbc.Driver");
        config.setJdbcUrl(String.format(
                "jdbc:mysql://%s:%d/%s?useSSL=%s&allowPublicKeyRetrieval=true&serverTimezone=UTC",
                host, port, database, ssl));
        config.setUsername(username);
        config.setPassword(password);

        plugin.getLogger().info("Using MySQL database at " + host + ":" + port + "/" + database);
    }

    private void configurePostgreSql(HikariConfig config) {
        String host = DatabaseConfig.postgresqlHost;
        int port = DatabaseConfig.postgresqlPort;
        String database = DatabaseConfig.postgresqlDatabase;
        String username = DatabaseConfig.postgresqlUsername;
        String password = DatabaseConfig.postgresqlPassword;
        boolean ssl = DatabaseConfig.postgresqlSsl;

        config.setDriverClassName("org.postgresql.Driver");
        config.setJdbcUrl(String.format(
                "jdbc:postgresql://%s:%d/%s?sslmode=%s",
                host, port, database, ssl ? "require" : "disable"));
        config.setUsername(username);
        config.setPassword(password);

        plugin.getLogger().info("Using PostgreSQL database at " + host + ":" + port + "/" + database);
    }

    private void configureTurso(HikariConfig config) {
        String url = DatabaseConfig.tursoUrl;
        String authToken = DatabaseConfig.tursoAuthToken;

        String jdbcUrl = url.replace("libsql://", "jdbc:libsql://");
        if (!jdbcUrl.startsWith("jdbc:")) {
            jdbcUrl = "jdbc:libsql://" + url;
        }

        config.setDriverClassName("org.sqlite.JDBC");
        config.setJdbcUrl(jdbcUrl + "?authToken=" + authToken);

        config.setMaximumPoolSize(Math.min(5, DatabaseConfig.connectionPoolSize));

        plugin.getLogger().info("Using Turso database at " + url);
    }

    private void configureSupabase(HikariConfig config) {
        String host = DatabaseConfig.supabaseHost;
        int port = DatabaseConfig.supabasePort;
        String database = DatabaseConfig.supabaseDatabase;
        String password = DatabaseConfig.supabasePassword;

        config.setDriverClassName("org.postgresql.Driver");
        config.setJdbcUrl(String.format(
                "jdbc:postgresql://%s:%d/%s?sslmode=require&prepareThreshold=0",
                host, port, database));
        config.setUsername("postgres");
        config.setPassword(password);

        config.setMaximumPoolSize(Math.min(5, DatabaseConfig.connectionPoolSize));
        config.addDataSourceProperty("socketTimeout", "30");

        plugin.getLogger().info("Using Supabase database at " + host + ":" + port);
    }

    private void createTables() throws SQLException {
        String autoIncrement = getAutoIncrementSyntax();
        String textType = getTextTypeSyntax();

        try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {

            // Block log table
            stmt.execute(String.format("""
                    CREATE TABLE IF NOT EXISTS vp_block (
                        id INTEGER PRIMARY KEY %s,
                        time BIGINT NOT NULL,
                        user %s NOT NULL,
                        world %s NOT NULL,
                        x INTEGER NOT NULL,
                        y INTEGER NOT NULL,
                        z INTEGER NOT NULL,
                        type %s NOT NULL,
                        old_type %s,
                        old_data %s,
                        new_type %s,
                        new_data %s,
                        action INTEGER NOT NULL,
                        rolled_back INTEGER DEFAULT 0
                    )
                    """, autoIncrement, textType, textType, textType, textType, textType, textType, textType));

            // Container log table
            stmt.execute(String.format("""
                    CREATE TABLE IF NOT EXISTS vp_container (
                        id INTEGER PRIMARY KEY %s,
                        time BIGINT NOT NULL,
                        user %s NOT NULL,
                        world %s NOT NULL,
                        x INTEGER NOT NULL,
                        y INTEGER NOT NULL,
                        z INTEGER NOT NULL,
                        type %s NOT NULL,
                        item %s NOT NULL,
                        amount INTEGER NOT NULL,
                        action INTEGER NOT NULL,
                        rolled_back INTEGER DEFAULT 0
                    )
                    """, autoIncrement, textType, textType, textType, textType));

            // Entity log table
            stmt.execute(String.format("""
                    CREATE TABLE IF NOT EXISTS vp_entity (
                        id INTEGER PRIMARY KEY %s,
                        time BIGINT NOT NULL,
                        user %s NOT NULL,
                        world %s NOT NULL,
                        x INTEGER NOT NULL,
                        y INTEGER NOT NULL,
                        z INTEGER NOT NULL,
                        entity_type %s NOT NULL,
                        entity_data %s,
                        action INTEGER NOT NULL
                    )
                    """, autoIncrement, textType, textType, textType, textType));

            // Chat log table
            stmt.execute(String.format("""
                    CREATE TABLE IF NOT EXISTS vp_chat (
                        id INTEGER PRIMARY KEY %s,
                        time BIGINT NOT NULL,
                        user %s NOT NULL,
                        message %s NOT NULL
                    )
                    """, autoIncrement, textType, textType));

            // Command log table
            stmt.execute(String.format("""
                    CREATE TABLE IF NOT EXISTS vp_command (
                        id INTEGER PRIMARY KEY %s,
                        time BIGINT NOT NULL,
                        user %s NOT NULL,
                        command %s NOT NULL
                    )
                    """, autoIncrement, textType, textType));

            // Sign log table
            stmt.execute(String.format("""
                    CREATE TABLE IF NOT EXISTS vp_sign (
                        id INTEGER PRIMARY KEY %s,
                        time BIGINT NOT NULL,
                        user %s NOT NULL,
                        world %s NOT NULL,
                        x INTEGER NOT NULL,
                        y INTEGER NOT NULL,
                        z INTEGER NOT NULL,
                        line1 %s,
                        line2 %s,
                        line3 %s,
                        line4 %s
                    )
                    """, autoIncrement, textType, textType, textType, textType, textType, textType));

            // User cache table
            stmt.execute(String.format("""
                    CREATE TABLE IF NOT EXISTS vp_user (
                        id INTEGER PRIMARY KEY %s,
                        uuid %s UNIQUE NOT NULL,
                        username %s NOT NULL
                    )
                    """, autoIncrement, textType, textType));

            // Economy table
            stmt.execute(String.format("""
                    CREATE TABLE IF NOT EXISTS vonixcore_economy (
                        uuid %s PRIMARY KEY,
                        username %s NOT NULL,
                        balance DOUBLE PRECISION DEFAULT 0,
                        last_transaction BIGINT DEFAULT 0
                    )
                    """, textType, textType));

            // Homes table
            stmt.execute(String.format("""
                    CREATE TABLE IF NOT EXISTS vonixcore_homes (
                        id INTEGER PRIMARY KEY %s,
                        uuid %s NOT NULL,
                        name %s NOT NULL,
                        world %s NOT NULL,
                        x DOUBLE PRECISION NOT NULL,
                        y DOUBLE PRECISION NOT NULL,
                        z DOUBLE PRECISION NOT NULL,
                        yaw REAL DEFAULT 0,
                        pitch REAL DEFAULT 0
                    )
                    """, autoIncrement, textType, textType, textType));

            // Warps table
            stmt.execute(String.format("""
                    CREATE TABLE IF NOT EXISTS vonixcore_warps (
                        name %s PRIMARY KEY,
                        world %s NOT NULL,
                        x DOUBLE PRECISION NOT NULL,
                        y DOUBLE PRECISION NOT NULL,
                        z DOUBLE PRECISION NOT NULL,
                        yaw REAL DEFAULT 0,
                        pitch REAL DEFAULT 0
                    )
                    """, textType, textType));

            // Permission groups table
            stmt.execute(String.format("""
                    CREATE TABLE IF NOT EXISTS vonixcore_groups (
                        name %s PRIMARY KEY,
                        display_name %s,
                        prefix %s,
                        suffix %s,
                        weight INTEGER DEFAULT 0,
                        parent %s,
                        permissions %s
                    )
                    """, textType, textType, textType, textType, textType, textType));

            // Permission users table
            stmt.execute(String.format("""
                    CREATE TABLE IF NOT EXISTS vonixcore_users (
                        uuid %s PRIMARY KEY,
                        username %s,
                        primary_group %s DEFAULT 'default',
                        groups %s,
                        prefix %s,
                        suffix %s,
                        permissions %s
                    )
                    """, textType, textType, textType, textType, textType, textType, textType));

            // Discord linked accounts table
            stmt.execute(String.format("""
                    CREATE TABLE IF NOT EXISTS vonixcore_discord_links (
                        minecraft_uuid %s PRIMARY KEY,
                        discord_id %s UNIQUE NOT NULL,
                        linked_at BIGINT NOT NULL
                    )
                    """, textType, textType));

            createIndexes(stmt);

        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to create tables", e);
            throw e;
        }
    }

    private String getAutoIncrementSyntax() {
        return switch (databaseType) {
            case MYSQL -> "AUTO_INCREMENT";
            case POSTGRESQL, SUPABASE -> "GENERATED ALWAYS AS IDENTITY";
            default -> "AUTOINCREMENT";
        };
    }

    private String getTextTypeSyntax() {
        return switch (databaseType) {
            case MYSQL -> "VARCHAR(255)";
            case POSTGRESQL, SUPABASE -> "TEXT";
            default -> "TEXT";
        };
    }

    private void createIndexes(Statement stmt) throws SQLException {
        executeIgnoreError(stmt, "CREATE INDEX IF NOT EXISTS idx_block_time ON vp_block (time)");
        executeIgnoreError(stmt, "CREATE INDEX IF NOT EXISTS idx_block_user ON vp_block (user)");
        executeIgnoreError(stmt, "CREATE INDEX IF NOT EXISTS idx_block_location ON vp_block (world, x, y, z)");
        executeIgnoreError(stmt, "CREATE INDEX IF NOT EXISTS idx_block_coords ON vp_block (x, z)");

        executeIgnoreError(stmt, "CREATE INDEX IF NOT EXISTS idx_container_time ON vp_container (time)");
        executeIgnoreError(stmt, "CREATE INDEX IF NOT EXISTS idx_container_location ON vp_container (world, x, y, z)");

        executeIgnoreError(stmt, "CREATE INDEX IF NOT EXISTS idx_entity_time ON vp_entity (time)");
        executeIgnoreError(stmt, "CREATE INDEX IF NOT EXISTS idx_entity_location ON vp_entity (world, x, y, z)");

        executeIgnoreError(stmt, "CREATE INDEX IF NOT EXISTS idx_user_uuid ON vp_user (uuid)");
        executeIgnoreError(stmt, "CREATE INDEX IF NOT EXISTS idx_user_name ON vp_user (username)");

        executeIgnoreError(stmt, "CREATE INDEX IF NOT EXISTS idx_homes_uuid ON vonixcore_homes (uuid)");

        executeIgnoreError(stmt, "CREATE INDEX IF NOT EXISTS idx_economy_balance ON vonixcore_economy (balance DESC)");
    }

    private void executeIgnoreError(Statement stmt, String sql) {
        try {
            stmt.execute(sql);
        } catch (SQLException ignored) {
        }
    }

    public Connection getConnection() throws SQLException {
        if (dataSource == null || dataSource.isClosed()) {
            throw new SQLException("Database not initialized");
        }
        return dataSource.getConnection();
    }

    public void close() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            plugin.getLogger().info("Database connection pool closed");
        }
    }
}
