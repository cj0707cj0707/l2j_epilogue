package org.l2jmobius.commons.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.l2jmobius.commons.config.DatabaseConfig;

public class DatabaseFactory {
   private static final Logger LOGGER = Logger.getLogger(DatabaseFactory.class.getName());
   private static HikariDataSource DATABASE_POOL;

   private DatabaseFactory() {
   }

   public static synchronized void init() {
      if (DATABASE_POOL != null && !DATABASE_POOL.isClosed()) {
         LOGGER.warning("Database: Connection pool is already initialized.");
      } else {
         DatabaseConfig.load();

         try {
            HikariConfig config = new HikariConfig();
            config.setDriverClassName(DatabaseConfig.DATABASE_DRIVER);
            config.setJdbcUrl(DatabaseConfig.DATABASE_URL);
            config.setUsername(DatabaseConfig.DATABASE_LOGIN);
            config.setPassword(DatabaseConfig.DATABASE_PASSWORD);
            config.setMaximumPoolSize(determineMaxPoolSize(DatabaseConfig.DATABASE_MAX_CONNECTIONS));
            config.setMinimumIdle(determineMinimumIdle(DatabaseConfig.DATABASE_MAX_CONNECTIONS));
            config.setConnectionTimeout(60000L);
            config.setIdleTimeout(300000L);
            config.setMaxLifetime(600000L);
            config.setLeakDetectionThreshold(600000L);
            config.setPoolName("L2JMobiusPool");
            config.setRegisterMbeans(true);
            config.setInitializationFailTimeout(-1L);
            config.setValidationTimeout(5000L);
            DATABASE_POOL = new HikariDataSource(config);
            LOGGER.info("Database: HikariCP pool initialized successfully.");
            if (DatabaseConfig.DATABASE_TEST_CONNECTIONS) {
               testDatabaseConnections();
            } else {
               testSingleConnection();
            }
         } catch (Exception var1) {
            LOGGER.log(Level.SEVERE, "Database: Failed to initialize HikariCP pool.", (Throwable)var1);
         }
      }
   }

   private static int determineMaxPoolSize(int configuredMax) {
      return Math.clamp((long)configuredMax, 4, 1000);
   }

   private static int determineMinimumIdle(int configuredMax) {
      return Math.max(determineMaxPoolSize(configuredMax) / 10, 2);
   }

   private static void testDatabaseConnections() {
      List<Connection> connections = new ArrayList<>();
      int successfulConnections = 0;

      try {
         LOGGER.info("Database: Testing database connections...");

         for (int i = 0; i < DATABASE_POOL.getMaximumPoolSize(); i++) {
            Connection connection = null;

            try {
               connection = DATABASE_POOL.getConnection();
               connections.add(connection);
               successfulConnections++;
               LOGGER.info("Database: Successfully opened connection " + connection.toString() + ".");
            } catch (SQLException var14) {
               LOGGER.log(Level.SEVERE, "Database: Failed to open connection " + (i + 1) + "!", (Throwable)var14);
               break;
            }
         }

         if (successfulConnections == DATABASE_POOL.getMaximumPoolSize()) {
            LOGGER.info("Database: Initialized with a total of " + successfulConnections + " connections.");
         } else {
            LOGGER.warning("Database: Only " + successfulConnections + " out of " + DATABASE_POOL.getMaximumPoolSize() + " connections were successful.");
            adjustPoolSize(successfulConnections);
         }
      } finally {
         Iterator var6 = connections.iterator();

         while (true) {
            if (!var6.hasNext()) {
               ;
            } else {
               Connection connection = (Connection)var6.next();
               if (connection != null) {
                  try {
                     connection.close();
                  } catch (SQLException var13) {
                     LOGGER.log(Level.SEVERE, "Database: Error closing connection.", (Throwable)var13);
                  }
               }
            }
         }
      }
   }

   private static void adjustPoolSize(int successfulConnections) {
      LOGGER.warning("Database: Adjusting pool size based on successful connections.");
      int newConnectionCount = successfulConnections;
      if (successfulConnections > 100) {
         newConnectionCount = successfulConnections / 100 * 100;
      } else if (successfulConnections > 50) {
         newConnectionCount = successfulConnections / 50 * 50;
      }

      newConnectionCount = Math.max(newConnectionCount, 20);

      try {
         DATABASE_POOL.setMaximumPoolSize(newConnectionCount);
         DATABASE_POOL.setMinimumIdle(determineMinimumIdle(newConnectionCount));
         LOGGER.info("Database: Reinitialized pool size to " + newConnectionCount + ".");
      } catch (Exception var3) {
         LOGGER.log(Level.SEVERE, "Database: Failed to adjust pool size.", (Throwable)var3);
      }
   }

   private static void testSingleConnection() {
      try (Connection connection = DATABASE_POOL.getConnection()) {
         if (connection.isValid(5)) {
            LOGGER.info("Database: Initialized with a valid connection.");
         } else {
            LOGGER.warning("Database: Connection is not valid.");
         }
      } catch (SQLException var5) {
         LOGGER.log(Level.SEVERE, "Database: Problem initializing connection pool.", (Throwable)var5);
      }
   }

   public static Connection getConnection() {
      try {
         return DATABASE_POOL.getConnection();
      } catch (SQLException var1) {
         LOGGER.log(Level.SEVERE, "Database: Could not get a connection.", (Throwable)var1);
         throw new RuntimeException("Unable to obtain a database connection.", var1);
      }
   }

   public static synchronized void close() {
      if (DATABASE_POOL != null && !DATABASE_POOL.isClosed()) {
         try {
            DATABASE_POOL.close();
            LOGGER.info("Database: HikariCP pool closed successfully.");
         } catch (Exception var1) {
            LOGGER.log(Level.SEVERE, "Database: There was a problem closing the data source.", (Throwable)var1);
         }
      }
   }
}
