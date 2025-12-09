package network.vonix.vonixcore.config;

import net.neoforged.neoforge.common.ModConfigSpec;
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

        public static final ModConfigSpec SPEC;
        public static final DatabaseConfig CONFIG;

        // Database type
        public final ModConfigSpec.ConfigValue<String> type;

        // SQLite settings
        public final ModConfigSpec.ConfigValue<String> sqliteFile;

        // MySQL settings
        public final ModConfigSpec.ConfigValue<String> mysqlHost;
        public final ModConfigSpec.IntValue mysqlPort;
        public final ModConfigSpec.ConfigValue<String> mysqlDatabase;
        public final ModConfigSpec.ConfigValue<String> mysqlUsername;
        public final ModConfigSpec.ConfigValue<String> mysqlPassword;
        public final ModConfigSpec.BooleanValue mysqlSsl;

        // PostgreSQL settings
        public final ModConfigSpec.ConfigValue<String> postgresqlHost;
        public final ModConfigSpec.IntValue postgresqlPort;
        public final ModConfigSpec.ConfigValue<String> postgresqlDatabase;
        public final ModConfigSpec.ConfigValue<String> postgresqlUsername;
        public final ModConfigSpec.ConfigValue<String> postgresqlPassword;
        public final ModConfigSpec.BooleanValue postgresqlSsl;

        // Turso (LibSQL) settings
        public final ModConfigSpec.ConfigValue<String> tursoUrl;
        public final ModConfigSpec.ConfigValue<String> tursoAuthToken;

        // Supabase settings
        public final ModConfigSpec.ConfigValue<String> supabaseHost;
        public final ModConfigSpec.IntValue supabasePort;
        public final ModConfigSpec.ConfigValue<String> supabaseDatabase;
        public final ModConfigSpec.ConfigValue<String> supabasePassword;

        // Connection pool settings
        public final ModConfigSpec.IntValue connectionPoolSize;
        public final ModConfigSpec.IntValue connectionTimeout;

        // Performance settings
        public final ModConfigSpec.IntValue consumerBatchSize;
        public final ModConfigSpec.IntValue consumerDelayMs;
        public final ModConfigSpec.IntValue dataPurgeDays;

        static {
                Pair<DatabaseConfig, ModConfigSpec> pair = new ModConfigSpec.Builder()
                                .configure(DatabaseConfig::new);
                CONFIG = pair.getLeft();
                SPEC = pair.getRight();
        }

        private DatabaseConfig(ModConfigSpec.Builder builder) {
                builder.comment(
                                "VonixCore Database Configuration",
                                "Configure database connection and performance settings",
                                "",
                                "Supported types: sqlite, mysql, postgresql, turso, supabase")
                                .push("database");

                type = builder.comment(
                                "Database type to use:",
                                "  'sqlite' - Local SQLite file (default, best for single servers)",
                                "  'mysql' - MySQL or MariaDB server",
                                "  'postgresql' - PostgreSQL server",
                                "  'turso' - Turso LibSQL edge database",
                                "  'supabase' - Supabase PostgreSQL database")
                                .define("type", "sqlite");

                builder.pop().comment(
                                "SQLite Configuration",
                                "Used when type = 'sqlite'",
                                "All data stored in a single file in your world folder")
                                .push("sqlite");

                sqliteFile = builder.comment(
                                "SQLite database file name",
                                "Stored in: world/vonixcore/<filename>")
                                .define("file", "vonixcore.db");

                builder.pop().comment(
                                "MySQL/MariaDB Configuration",
                                "Used when type = 'mysql'")
                                .push("mysql");

                mysqlHost = builder.comment("MySQL server hostname or IP")
                                .define("host", "localhost");

                mysqlPort = builder.comment("MySQL server port")
                                .defineInRange("port", 3306, 1, 65535);

                mysqlDatabase = builder.comment("Database name (must exist)")
                                .define("database", "vonixcore");

                mysqlUsername = builder.comment("MySQL username")
                                .define("username", "root");

                mysqlPassword = builder.comment("MySQL password")
                                .define("password", "");

                mysqlSsl = builder.comment("Enable SSL connection")
                                .define("ssl", false);

                builder.pop().comment(
                                "PostgreSQL Configuration",
                                "Used when type = 'postgresql'")
                                .push("postgresql");

                postgresqlHost = builder.comment("PostgreSQL server hostname or IP")
                                .define("host", "localhost");

                postgresqlPort = builder.comment("PostgreSQL server port")
                                .defineInRange("port", 5432, 1, 65535);

                postgresqlDatabase = builder.comment("Database name")
                                .define("database", "vonixcore");

                postgresqlUsername = builder.comment("PostgreSQL username")
                                .define("username", "postgres");

                postgresqlPassword = builder.comment("PostgreSQL password")
                                .define("password", "");

                postgresqlSsl = builder.comment("Enable SSL connection")
                                .define("ssl", false);

                builder.pop().comment(
                                "Turso Configuration (LibSQL Edge Database)",
                                "Used when type = 'turso'",
                                "Get your URL and token from: https://turso.tech")
                                .push("turso");

                tursoUrl = builder.comment(
                                "Turso database URL",
                                "Format: libsql://your-database-name.turso.io")
                                .define("url", "libsql://your-database.turso.io");

                tursoAuthToken = builder.comment(
                                "Turso authentication token",
                                "Generate from Turso dashboard - KEEP SECRET!")
                                .define("auth_token", "");

                builder.pop().comment(
                                "Supabase Configuration",
                                "Used when type = 'supabase'",
                                "Supabase provides PostgreSQL databases",
                                "Get credentials from: https://supabase.com")
                                .push("supabase");

                supabaseHost = builder.comment(
                                "Supabase database host",
                                "Format: db.xxxxxxxxxxxx.supabase.co")
                                .define("host", "db.xxxxxxxxxxxx.supabase.co");

                supabasePort = builder.comment("Supabase database port (usually 5432 or 6543)")
                                .defineInRange("port", 5432, 1, 65535);

                supabaseDatabase = builder.comment("Database name (usually 'postgres')")
                                .define("database", "postgres");

                supabasePassword = builder.comment(
                                "Database password from Supabase dashboard",
                                "KEEP SECRET!")
                                .define("password", "");

                builder.pop().comment(
                                "Connection Pool Settings",
                                "Applies to all database types")
                                .push("pool");

                connectionPoolSize = builder.comment(
                                "Maximum connections in the pool",
                                "Recommended: 5-10 for small servers, 10-20 for large")
                                .defineInRange("max_connections", 10, 1, 50);

                connectionTimeout = builder.comment(
                                "Connection timeout in milliseconds")
                                .defineInRange("timeout_ms", 5000, 1000, 30000);

                builder.pop().comment(
                                "Performance Tuning",
                                "Adjust for your server's needs")
                                .push("performance");

                consumerBatchSize = builder.comment(
                                "Records to batch before writing",
                                "Higher = fewer writes, more memory")
                                .defineInRange("batch_size", 500, 50, 5000);

                consumerDelayMs = builder.comment(
                                "Delay between batch writes (ms)")
                                .defineInRange("batch_delay_ms", 500, 100, 5000);

                dataPurgeDays = builder.comment(
                                "Auto-purge data older than X days",
                                "0 = never purge")
                                .defineInRange("purge_days", 30, 0, 365);

                builder.pop();
        }
}
