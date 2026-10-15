package org.l2jmobius.commons.config;

import org.l2jmobius.commons.util.ConfigReader;

public class DatabaseConfig {
   private static final String DATABASE_CONFIG_FILE = "./config/Database.ini";
   public static String DATABASE_DRIVER;
   public static String DATABASE_URL;
   public static String DATABASE_LOGIN;
   public static String DATABASE_PASSWORD;
   public static int DATABASE_MAX_CONNECTIONS;
   public static boolean DATABASE_TEST_CONNECTIONS;
   public static boolean BACKUP_DATABASE;
   public static String MYSQL_BIN_PATH;
   public static String BACKUP_PATH;
   public static int BACKUP_DAYS;

   public static void load() {
      ConfigReader config = new ConfigReader("./config/Database.ini");
      DATABASE_DRIVER = config.getString("Driver", "com.mysql.cj.jdbc.Driver");
      DATABASE_URL = config.getString("URL", "jdbc:mysql://localhost/l2jmobius");
      DATABASE_LOGIN = config.getString("Login", "root");
      DATABASE_PASSWORD = config.getString("Password", "");
      DATABASE_MAX_CONNECTIONS = config.getInt("MaximumDatabaseConnections", 10);
      DATABASE_TEST_CONNECTIONS = config.getBoolean("TestDatabaseConnections", false);
      BACKUP_DATABASE = config.getBoolean("BackupDatabase", false);
      MYSQL_BIN_PATH = config.getString("MySqlBinLocation", "C:/xampp/mysql/bin/");
      BACKUP_PATH = config.getString("BackupPath", "../backup/");
      BACKUP_DAYS = config.getInt("BackupDays", 30);
   }
}
