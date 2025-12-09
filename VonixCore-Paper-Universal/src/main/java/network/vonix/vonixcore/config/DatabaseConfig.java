package network.vonix.vonixcore.config;

import org.bukkit.configuration.file.YamlConfiguration;

public class DatabaseConfig {

    public static String type;
    public static String sqliteFile;

    public static String mysqlHost;
    public static int mysqlPort;
    public static String mysqlDatabase;
    public static String mysqlUsername;
    public static String mysqlPassword;
    public static boolean mysqlSsl;

    public static String postgresqlHost;
    public static int postgresqlPort;
    public static String postgresqlDatabase;
    public static String postgresqlUsername;
    public static String postgresqlPassword;
    public static boolean postgresqlSsl;

    public static String tursoUrl;
    public static String tursoAuthToken;

    public static String supabaseHost;
    public static int supabasePort;
    public static String supabaseDatabase;
    public static String supabasePassword;

    public static int connectionPoolSize;
    public static long connectionTimeout;
    public static int consumerBatchSize;
    public static long consumerDelayMs;
    public static int dataPurgeDays;

    public static void load(YamlConfiguration config) {
        type = config.getString("database.type", "sqlite");

        sqliteFile = config.getString("database.sqlite.file", "vonixcore.db");

        mysqlHost = config.getString("database.mysql.host", "localhost");
        mysqlPort = config.getInt("database.mysql.port", 3306);
        mysqlDatabase = config.getString("database.mysql.database", "vonixcore");
        mysqlUsername = config.getString("database.mysql.username", "root");
        mysqlPassword = config.getString("database.mysql.password", "");
        mysqlSsl = config.getBoolean("database.mysql.ssl", false);

        postgresqlHost = config.getString("database.postgresql.host", "localhost");
        postgresqlPort = config.getInt("database.postgresql.port", 5432);
        postgresqlDatabase = config.getString("database.postgresql.database", "vonixcore");
        postgresqlUsername = config.getString("database.postgresql.username", "postgres");
        postgresqlPassword = config.getString("database.postgresql.password", "");
        postgresqlSsl = config.getBoolean("database.postgresql.ssl", false);

        tursoUrl = config.getString("database.turso.url", "libsql://your-database.turso.io");
        tursoAuthToken = config.getString("database.turso.auth_token", "");

        supabaseHost = config.getString("database.supabase.host", "db.xxxxxxxxxxxx.supabase.co");
        supabasePort = config.getInt("database.supabase.port", 5432);
        supabaseDatabase = config.getString("database.supabase.database", "postgres");
        supabasePassword = config.getString("database.supabase.password", "");

        connectionPoolSize = config.getInt("database.pool.max_connections", 10);
        connectionTimeout = config.getLong("database.pool.timeout_ms", 5000);

        consumerBatchSize = config.getInt("database.performance.batch_size", 500);
        consumerDelayMs = config.getLong("database.performance.batch_delay_ms", 500);
        dataPurgeDays = config.getInt("database.performance.purge_days", 30);
    }
}
