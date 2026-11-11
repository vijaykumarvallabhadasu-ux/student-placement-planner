package com.placementplanner.util;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Centralized, high-reliability Oracle Database utility for StudentPlacementPlanner.
 * Features:
 * - Environment variable & System property configuration
 * - Bounded connection pooling (prevents ORA-12516 process/handler exhaustion in Oracle 10g XE)
 * - Intelligent exponential backoff retry for transient listener busy states (ORA-12516 / ORA-12519)
 * - Connection liveness validation (isClosed / isValid / DUAL query)
 * - Safe credential isolation (never logs passwords)
 */
public final class DBUtil {

    private static final Logger LOGGER = Logger.getLogger(DBUtil.class.getName());

    private static final String DEFAULT_URL = "jdbc:oracle:thin:@//127.0.0.1:1521/XE";
    private static final String DEFAULT_USER = "system";
    private static final String DEFAULT_PASSWORD = "system";

    private static final int MAX_POOL_SIZE;
    private static final boolean POOL_ENABLED;

    private static final BlockingQueue<Connection> POOL;
    private static final Set<Connection> ALL_PHYSICAL_CONNECTIONS = ConcurrentHashMap.newKeySet();
    private static final AtomicInteger ACTIVE_CONNECTION_COUNT = new AtomicInteger(0);

    static {
        // Load configuration from System properties or Environment variables
        int maxPool = 5;
        String maxPoolProp = getConfig("db.pool.max", "DB_POOL_MAX", "5");
        try {
            maxPool = Integer.parseInt(maxPoolProp.trim());
            if (maxPool < 1 || maxPool > 20) {
                maxPool = 5;
            }
        } catch (NumberFormatException ignored) {
            maxPool = 5;
        }
        MAX_POOL_SIZE = maxPool;

        String poolEnabledProp = getConfig("db.pool", "DB_POOL", "true");
        POOL_ENABLED = !"false".equalsIgnoreCase(poolEnabledProp.trim());

        if (POOL_ENABLED) {
            POOL = new ArrayBlockingQueue<>(MAX_POOL_SIZE);
            LOGGER.info("DBUtil initialized with Connection Pool (maxSize=" + MAX_POOL_SIZE + ")");
        } else {
            POOL = null;
            LOGGER.info("DBUtil initialized in direct connection mode (pooling disabled)");
        }

        // Register JVM shutdown hook to clean up physical connections
        Runtime.getRuntime().addShutdownHook(new Thread(DBUtil::closePool, "DBUtil-Shutdown-Hook"));
    }

    private DBUtil() {
        // Prevent instantiation
    }

    public static String getUrl() {
        return getConfig("db.url", "DB_URL", DEFAULT_URL);
    }

    public static String getUser() {
        return getConfig("db.user", "DB_USER", DEFAULT_USER);
    }

    private static String getPassword() {
        return getConfig("db.password", "DB_PASSWORD", DEFAULT_PASSWORD);
    }

    private static String getConfig(String sysProp, String envVar, String defaultValue) {
        String val = System.getProperty(sysProp);
        if (val != null && !val.isBlank()) {
            return val.trim();
        }
        val = System.getenv(envVar);
        if (val != null && !val.isBlank()) {
            return val.trim();
        }
        return defaultValue;
    }

    /**
     * Ensures an appropriate Oracle JDBC driver class is loaded into memory.
     */
    private static void ensureDriverLoaded() throws SQLException {
        try {
            Class.forName("oracle.jdbc.OracleDriver");
            return;
        } catch (ClassNotFoundException ignored) {
        }

        try {
            Class.forName("oracle.jdbc.driver.OracleDriver");
        } catch (ClassNotFoundException e) {
            throw new SQLException(
                    "Oracle JDBC Driver class not found in classpath or WEB-INF/lib. "
                    + "Ensure ojdbc JAR is present in WEB-INF/lib.", e);
        }
    }

    /**
     * Obtains a connection to the Oracle Database.
     * When pooling is enabled, borrows a leased connection that auto-returns to the pool on close().
     */
    public static Connection getConnection() throws SQLException {
        ensureDriverLoaded();

        if (!POOL_ENABLED) {
            return createPhysicalConnectionWithRetry();
        }

        return borrowPooledConnection();
    }

    /**
     * Borrows a connection from the internal bounded pool.
     */
    private static Connection borrowPooledConnection() throws SQLException {
        long startTime = System.currentTimeMillis();
        long maxWaitMs = 8000;

        while ((System.currentTimeMillis() - startTime) < maxWaitMs) {
            // Check if there is an idle connection available in pool
            Connection raw = POOL.poll();
            if (raw != null) {
                if (isConnectionHealthy(raw)) {
                    return wrapConnection(raw);
                } else {
                    closePhysicalQuietly(raw);
                    ACTIVE_CONNECTION_COUNT.decrementAndGet();
                }
            }

            // If pool is empty, can we create a new physical connection?
            if (ACTIVE_CONNECTION_COUNT.get() < MAX_POOL_SIZE) {
                if (ACTIVE_CONNECTION_COUNT.incrementAndGet() <= MAX_POOL_SIZE) {
                    try {
                        Connection newPhysical = createPhysicalConnectionWithRetry();
                        ALL_PHYSICAL_CONNECTIONS.add(newPhysical);
                        return wrapConnection(newPhysical);
                    } catch (SQLException e) {
                        ACTIVE_CONNECTION_COUNT.decrementAndGet();
                        throw e;
                    }
                } else {
                    ACTIVE_CONNECTION_COUNT.decrementAndGet();
                }
            }

            // Wait for a connection to be returned
            try {
                Connection returned = POOL.poll(500, TimeUnit.MILLISECONDS);
                if (returned != null) {
                    if (isConnectionHealthy(returned)) {
                        return wrapConnection(returned);
                    } else {
                        closePhysicalQuietly(returned);
                        ACTIVE_CONNECTION_COUNT.decrementAndGet();
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new SQLException("Thread interrupted while waiting for database connection from pool", e);
            }
        }

        throw new SQLException("Database connection pool timeout (" + maxWaitMs
                + "ms). All " + MAX_POOL_SIZE + " Oracle connections are currently in use.");
    }

    /**
     * Checks whether an existing physical connection is open and responsive.
     */
    private static boolean isConnectionHealthy(Connection conn) {
        if (conn == null) {
            return false;
        }
        try {
            if (conn.isClosed()) {
                return false;
            }
            // Quick ping using standard DUAL query (compatible with Oracle 10g through 23ai)
            try (Statement stmt = conn.createStatement()) {
                stmt.setQueryTimeout(2);
                try (ResultSet rs = stmt.executeQuery("SELECT 1 FROM DUAL")) {
                    return rs.next();
                }
            }
        } catch (SQLException e) {
            return false;
        }
    }

    /**
     * Creates a new physical connection to Oracle, with intelligent retry for ORA-12516/ORA-12519.
     */
    private static Connection createPhysicalConnectionWithRetry() throws SQLException {
        String url = getUrl();
        String user = getUser();
        String password = getPassword();

        int maxRetries = 3;
        SQLException lastEx = null;

        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                return DriverManager.getConnection(url, user, password);
            } catch (SQLException e) {
                lastEx = e;
                int errCode = e.getErrorCode();
                String msg = e.getMessage() != null ? e.getMessage() : "";

                // Handle ORA-12514 / ORA-12505 (Service Name vs SID format variation)
                if ((errCode == 12514 || errCode == 12505) && attempt == 1) {
                    String altUrl = url;
                    if (url.contains("/XE") || url.contains("/xe")) {
                        altUrl = url.replaceFirst("(?i)/xe$", ":XE").replaceFirst("@//", "@");
                    } else if (url.contains(":XE") || url.contains(":xe")) {
                        altUrl = url.replaceFirst("(?i):xe$", "/XE").replaceFirst("@(?!//)", "@//");
                    }
                    if (!altUrl.equals(url)) {
                        LOGGER.log(Level.INFO, "Testing alternate connection descriptor: " + altUrl);
                        try {
                            return DriverManager.getConnection(altUrl, user, password);
                        } catch (SQLException ignored) {
                        }
                    }
                }

                // ORA-12516 / ORA-12519 listener handler busy
                boolean isListenerBusy = (errCode == 12516 || errCode == 12519
                        || msg.contains("12516") || msg.contains("12519")
                        || msg.contains("protocol handler") || msg.contains("service handler"));

                if (isListenerBusy && attempt < maxRetries) {
                    LOGGER.log(Level.WARNING, "Oracle listener reported busy handler (ORA-" + errCode
                            + "). Retrying connection attempt " + (attempt + 1) + " of " + maxRetries
                            + " after " + (attempt * 300) + "ms backoff...");
                    try {
                        Thread.sleep(300L * attempt);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new SQLException("Interrupted during listener retry backoff", ie);
                    }
                } else {
                    String sanitizedUrl = url.replaceAll(":[^:@]+@", ":***@");
                    LOGGER.log(Level.SEVERE, "Failed to connect to Oracle database at " + sanitizedUrl
                            + " as user '" + user + "' (Error code: ORA-" + errCode + "): " + e.getMessage());
                    throw e;
                }
            }
        }

        throw lastEx;
    }

    /**
     * Wraps a physical connection in a dynamic proxy that redirects close() back to the pool.
     */
    private static Connection wrapConnection(Connection physical) {
        return (Connection) Proxy.newProxyInstance(
                DBUtil.class.getClassLoader(),
                new Class<?>[]{Connection.class},
                new PooledConnectionHandler(physical)
        );
    }

    /**
     * Dynamic proxy invocation handler for leased pooled connections.
     */
    private static class PooledConnectionHandler implements InvocationHandler {
        private final Connection physical;
        private boolean isClosed = false;

        PooledConnectionHandler(Connection physical) {
            this.physical = physical;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            String methodName = method.getName();

            if ("close".equals(methodName)) {
                if (!isClosed) {
                    isClosed = true;
                    returnConnectionToPool(physical);
                }
                return null;
            }

            if ("isClosed".equals(methodName)) {
                return isClosed || physical.isClosed();
            }

            if (isClosed) {
                throw new SQLException("Connection is closed and cannot be used.");
            }

            try {
                return method.invoke(physical, args);
            } catch (Throwable t) {
                if (t instanceof java.lang.reflect.InvocationTargetException ite) {
                    throw ite.getTargetException();
                }
                throw t;
            }
        }
    }

    /**
     * Returns a leased physical connection back to the pool.
     */
    private static void returnConnectionToPool(Connection physical) {
        if (physical == null) {
            return;
        }

        try {
            if (!physical.isClosed()) {
                // Ensure autocommit is reset if left in transaction
                if (!physical.getAutoCommit()) {
                    try {
                        physical.rollback();
                    } catch (SQLException ignored) {
                    }
                    physical.setAutoCommit(true);
                }

                if (POOL != null && POOL.offer(physical)) {
                    return;
                }
            }
        } catch (SQLException ignored) {
        }

        // If not accepted into pool or unviable, close physical
        closePhysicalQuietly(physical);
        ACTIVE_CONNECTION_COUNT.decrementAndGet();
    }

    private static void closePhysicalQuietly(Connection physical) {
        if (physical != null) {
            ALL_PHYSICAL_CONNECTIONS.remove(physical);
            try {
                physical.close();
            } catch (SQLException ignored) {
            }
        }
    }

    /**
     * Closes all physical connections maintained by the pool.
     */
    public static void closePool() {
        if (POOL != null) {
            Connection conn;
            while ((conn = POOL.poll()) != null) {
                closePhysicalQuietly(conn);
            }
        }
        for (Connection conn : ALL_PHYSICAL_CONNECTIONS) {
            closePhysicalQuietly(conn);
        }
        ALL_PHYSICAL_CONNECTIONS.clear();
        ACTIVE_CONNECTION_COUNT.set(0);
        LOGGER.info("DBUtil connection pool closed successfully.");
    }

    /**
     * Safe column-presence inspector across diverse Oracle schema revisions.
     */
    public static boolean hasColumn(ResultSet rs, String columnName) {
        if (rs == null || columnName == null) {
            return false;
        }

        try {
            ResultSetMetaData metadata = rs.getMetaData();
            int count = metadata.getColumnCount();
            for (int i = 1; i <= count; i++) {
                if (columnName.equalsIgnoreCase(metadata.getColumnName(i))
                        || columnName.equalsIgnoreCase(metadata.getColumnLabel(i))) {
                    return true;
                }
            }
        } catch (SQLException ignored) {
        }

        return false;
    }
}
