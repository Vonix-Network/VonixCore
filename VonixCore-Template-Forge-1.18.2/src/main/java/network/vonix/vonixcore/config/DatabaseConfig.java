package network.vonix.vonixcore.config;

import net.minecraftforge.common.ForgeConfigSpec;
import org.apache.commons.lang3.tuple.Pair;

/**
 * Database configuration for VonixCore.
 * Stored in config/vonixcore-database.toml
 * 
 * Supported database types:
 * - sqlite: Local SQLite file (default, recommended for single servers)
 * - mysql: MySQL/MariaDB server
 * - postgresql: PostgreSQL server
 * - turso: Turso (LibSQL) edge database
 * - supabase: Supabase PostgreSQL database
 */
public class DatabaseConfig {

        public static final ForgeConfigSpec SPEC;
        public static final DatabaseConfig CONFIG;

        // Database type
        public final ForgeConfigSpec.ConfigValue<String> type;

        // SQLite settings
        public final ForgeConfigSpec.ConfigValue<String> sqliteFile;

        // MySQL settings
        public final ForgeConfigSpec.ConfigValue<String> mysqlHost;
        public final ForgeConfigSpec.IntValue mysqlPort;
        public final ForgeConfigSpec.ConfigValue<String> mysqlDatabase;
        public final ForgeConfigSpec.ConfigValue<String> mysqlUsername;
        public final ForgeConfigSpec.ConfigValue<String> mysqlPassword;
        public final ForgeConfigSpec.BooleanValue mysqlSsl;

        // PostgreSQL settings
        public final ForgeConfigSpec.ConfigValue<String> postgresqlHost;
        public final ForgeConfigSpec.IntValue postgresqlPort;
        public final ForgeConfigSpec.ConfigValue<String> postgresqlDatabase;
        public final ForgeConfigSpec.ConfigValue<String> postgresqlUsername;
        public final ForgeConfigSpec.ConfigValue<String> postgresqlPassword;
        public final ForgeConfigSpec.BooleanValue postgresqlSsl;

        // Turso (LibSQL) settings
        public final ForgeConfigSpec.ConfigValue<String> tursoUrl;
        public final ForgeConfigSpec.ConfigValue<String> tursoAuthToken;

        // Supabase settings
        public final ForgeConfigSpec.ConfigValue<String> supabaseHost;
        public final ForgeConfigSpec.IntValue supabasePort;
        public final ForgeConfigSpec.ConfigValue<String> supabaseDatabase;
        public final ForgeConfigSpec.ConfigValue<String> supabasePassword;

        // Connection pool settings
        public final ForgeConfigSpec.IntValue connectionPoolSize;
        public final ForgeConfigSpec.IntValue connectionTimeout;

        // Performance settings
        public final ForgeConfigSpec.IntValue consumerBatchSize;
        public final ForgeConfigSpec.IntValue consumerDelayMs;
        public final ForgeConfigSpec.IntValue dataPurgeDays;

        static {
                Pair<DatabaseConfig, ForgeConfigSpec> pair = new ForgeConfigSpec.Builder()
                                .configure(DatabaseConfig::new);
                CONFIG = pair.getLeft();
                SPEC = pair.getRight();
        }

        private DatabaseConfig(ForgeConfigSpec.Builder builder) {
                builder.comment(
                                "VonixCore Database Configuration",
                                "Configure database connection and performance settings",
                                "",
                                "Supported types: sqlite, mysql, postgresql, turso, supabase")
                                .push("database");

                type = builder.comment(
                                "Database type to use:",
                                "  'sqlite' - Local SQLite file (default, best for single servers)",
                                "  'mysql' - MySQL/MariaDB server",
                                "  'postgresql' - PostgreSQL server",
                                "  'turso' - Turso (LibSQL) edge database",
                                "  'supabase' - Supabase PostgreSQL database")
                                .define("type", "sqlite");

                builder.pop().comment(
                                "SQLite Settings",
                                "Local SQLite database file settings")
                                .push("sqlite");

                sqliteFile = builder.comment("SQLite database file name (relative to config directory)")
                                .define("file", "vonixcore.db");

                builder.pop().comment(
                                "MySQL Settings",
                                "MySQL/MariaDB server connection settings")
                                .push("mysql");

                mysqlHost = builder.comment("MySQL server hostname")
                                .define("host", "localhost");

                mysqlPort = builder.comment("MySQL server port")
                                .defineInRange("port", 3306, 1, 65535);

                mysqlDatabase = builder.comment("MySQL database name")
                                .define("database", "vonixcore");

                mysqlUsername = builder.comment("MySQL username")
                                .define("username", "root");

                mysqlPassword = builder.comment("MySQL password")
                                .define("password", "");

                mysqlSsl = builder.comment("Use SSL connection")
                                .define("ssl", false);

                builder.pop().comment(
                                "PostgreSQL Settings",
                                "PostgreSQL server connection settings")
                                .push("postgresql");

                postgresqlHost = builder.comment("PostgreSQL server hostname")
                                .define("host", "localhost");

                postgresqlPort = builder.comment("PostgreSQL server port")
                                .defineInRange("port", 5432, 1, 65535);

                postgresqlDatabase = builder.comment("PostgreSQL database name")
                                .define("database", "vonixcore");

                postgresqlUsername = builder.comment("PostgreSQL username")
                                .define("username", "postgres");

                postgresqlPassword = builder.comment("PostgreSQL password")
                                .define("password", "");

                postgresqlSsl = builder.comment("Use SSL connection")
                                .define("ssl", false);

                builder.pop().comment(
                                "Turso Settings",
                                "Turso (LibSQL) edge database settings")
                                .push("turso");

                tursoUrl = builder.comment("Turso database URL")
                                .define("url", "libsql://your-database.turso.io");

                tursoAuthToken = builder.comment("Turso authentication token")
                                .define("auth_token", "");

                builder.pop().comment(
                                "Supabase Settings",
                                "Supabase PostgreSQL database settings")
                                .push("supabase");

                supabaseHost = builder.comment("Supabase database host")
                                .define("host", "db.xxxxxxxxxxxx.supabase.co");

                supabasePort = builder.comment("Supabase database port")
                                .defineInRange("port", 5432, 1, 65535);

                supabaseDatabase = builder.comment("Supabase database name")
                                .define("database", "postgres");

                supabasePassword = builder.comment("Supabase database password")
                                .define("password", "");

                builder.pop().comment(
                                "Connection Pool Settings",
                                "Database connection pool configuration")
                                .push("pool");

                connectionPoolSize = builder.comment("Maximum number of connections in the pool")
                                .defineInRange("max_connections", 10, 1, 100);

                connectionTimeout = builder.comment("Connection timeout in milliseconds")
                                .defineInRange("timeout_ms", 5000, 1000, 60000);

                builder.pop().comment(
                                "Performance Settings",
                                "Database performance and maintenance settings")
                                .push("performance");

                consumerBatchSize = builder.comment("Batch size for database operations")
                                .defineInRange("batch_size", 500, 10, 5000);

                consumerDelayMs = builder.comment("Delay between batch operations in milliseconds")
                                .defineInRange("batch_delay_ms", 500, 10, 5000);

                dataPurgeDays = builder.comment("Days to keep old data before purging")
                                .defineInRange("purge_days", 30, 1, 365);

                builder.pop();
        }

        // ============ Getters ============

        public String getType() {
                return CONFIG.type.get();
        }

        // SQLite
        public String getSqliteFile() {
                return CONFIG.sqliteFile.get();
        }

        // MySQL
        public String getMysqlHost() {
                return CONFIG.mysqlHost.get();
        }

        public int getMysqlPort() {
                return CONFIG.mysqlPort.get();
        }

        public String getMysqlDatabase() {
                return CONFIG.mysqlDatabase.get();
        }

        public String getMysqlUsername() {
                return CONFIG.mysqlUsername.get();
        }

        public String getMysqlPassword() {
                return CONFIG.mysqlPassword.get();
        }

        public boolean getMysqlSsl() {
                return CONFIG.mysqlSsl.get();
        }

        // PostgreSQL
        public String getPostgresqlHost() {
                return CONFIG.postgresqlHost.get();
        }

        public int getPostgresqlPort() {
                return CONFIG.postgresqlPort.get();
        }

        public String getPostgresqlDatabase() {
                return CONFIG.postgresqlDatabase.get();
        }

        public String getPostgresqlUsername() {
                return CONFIG.postgresqlUsername.get();
        }

        public String getPostgresqlPassword() {
                return CONFIG.postgresqlPassword.get();
        }

        public boolean getPostgresqlSsl() {
                return CONFIG.postgresqlSsl.get();
        }

        // Turso
        public String getTursoUrl() {
                return CONFIG.tursoUrl.get();
        }

        public String getTursoAuthToken() {
                return CONFIG.tursoAuthToken.get();
        }

        // Supabase
        public String getSupabaseHost() {
                return CONFIG.supabaseHost.get();
        }

        public int getSupabasePort() {
                return CONFIG.supabasePort.get();
        }

        public String getSupabaseDatabase() {
                return CONFIG.supabaseDatabase.get();
        }

        public String getSupabasePassword() {
                return CONFIG.supabasePassword.get();
        }

        // Pool
        public int getConnectionPoolSize() {
                return CONFIG.connectionPoolSize.get();
        }

        public int getConnectionTimeout() {
                return CONFIG.connectionTimeout.get();
        }

        // Performance
        public int getConsumerBatchSize() {
                return CONFIG.consumerBatchSize.get();
        }

        public int getConsumerDelayMs() {
                return CONFIG.consumerDelayMs.get();
        }

        public int getDataPurgeDays() {
                return CONFIG.dataPurgeDays.get();
        }
}
