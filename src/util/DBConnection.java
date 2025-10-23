package util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DBConnection {
    // Defaults (can be overridden by environment variables below)
    private static final String DEFAULT_URL = "jdbc:mysql://localhost:3306/library_management";
    private static final String DEFAULT_USER = "root";
    private static final String DEFAULT_PASSWORD = "Blad@123";

    private static volatile Connection connection;

    private static String getEnvOrDefault(String key, String fallback) {
        String v = System.getenv(key);
        return v != null && !v.isBlank() ? v : fallback;
    }

    private static synchronized void openConnectionIfNeeded() throws SQLException {
        if (connection != null) {
            try {
                if (!connection.isClosed() && connection.isValid(2)) {
                    return;
                }
            } catch (SQLException ignored) {
                // will reopen below
            }
        }

        String url = getEnvOrDefault("DB_URL", DEFAULT_URL);
        String user = getEnvOrDefault("DB_USER", DEFAULT_USER);
        String pass = getEnvOrDefault("DB_PASSWORD", DEFAULT_PASSWORD);

        try {
            // Ensure MySQL driver is loaded (useful in some runtime environments)
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException ignored) {
            // DriverManager will still try via SPI if available on classpath
        }

        connection = DriverManager.getConnection(url, user, pass);
        System.out.println("✅ Đã kết nối MySQL: " + url);
    }

    public static Connection getConnection() {
        try {
            openConnectionIfNeeded();
            return connection;
        } catch (SQLException e) {
            System.err.println("❌ Lỗi kết nối MySQL: " + e.getMessage());
            return null;
        }
    }

    public static synchronized void closeQuietly() {
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException ignored) {
            } finally {
                connection = null;
            }
        }
    }
}
