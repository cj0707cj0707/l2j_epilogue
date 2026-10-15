package org.l2jmobius.commons.database;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;
import org.l2jmobius.commons.config.DatabaseConfig;

public class DatabaseBackup {
   private static final DateTimeFormatter BACKUP_FORMAT = DateTimeFormatter.ofPattern("_yyyy_MM_dd_HH_mm'.sql'");

   public static void performBackup(String description) {
      if (DatabaseConfig.BACKUP_DAYS > 0) {
         long cut = LocalDateTime.now().minusDays(DatabaseConfig.BACKUP_DAYS).toEpochSecond(ZoneOffset.UTC);
         Path path = Paths.get(DatabaseConfig.BACKUP_PATH);

         try {
            Files.list(path).filter(n -> {
               try {
                  return Files.getLastModifiedTime(n).to(TimeUnit.SECONDS) < cut;
               } catch (Exception var4x) {
                  return false;
               }
            }).forEach(n -> {
               try {
                  Files.delete(n);
               } catch (Exception var2x) {
               }
            });
         } catch (Exception var7) {
         }
      }

      String mysqldumpPath = System.getProperty("os.name").toLowerCase().contains("win") ? DatabaseConfig.MYSQL_BIN_PATH : "";

      try {
         String backupFileName = DatabaseConfig.BACKUP_PATH + description + BACKUP_FORMAT.format(LocalDateTime.now());
         String databaseName = DatabaseConfig.DATABASE_URL.replace("jdbc:mysql://", "").replaceAll(".*\\/|\\?.*", "");
         String[] command = new String[]{
            mysqldumpPath + "mysqldump",
            "-u",
            DatabaseConfig.DATABASE_LOGIN,
            DatabaseConfig.DATABASE_PASSWORD.trim().isEmpty() ? "" : "-p" + DatabaseConfig.DATABASE_PASSWORD,
            databaseName,
            "-r",
            backupFileName
         };
         Process process = Runtime.getRuntime().exec(command);
         process.waitFor();
      } catch (Exception var6) {
      }
   }
}
