package org.l2jmobius.gameserver.cache;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.l2jmobius.gameserver.config.GeneralConfig;
import org.l2jmobius.gameserver.config.ServerConfig;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.network.enums.ChatType;
import org.l2jmobius.gameserver.network.serverpackets.CreatureSay;

public class HtmCache {
   private static final Logger LOGGER = Logger.getLogger(HtmCache.class.getName());
   private static final Map<String, String> HTML_CACHE = (Map<String, String>)(GeneralConfig.HTM_CACHE ? new HashMap<>() : new ConcurrentHashMap<>());
   private int _loadedFiles;
   private long _bytesBuffLen;

   protected HtmCache() {
      this.reload();
   }

   public void reload() {
      this.reload(ServerConfig.DATAPACK_ROOT);
   }

   public void reload(File file) {
      if (GeneralConfig.HTM_CACHE) {
         LOGGER.info("Html cache start...");
         this.parseDir(file);
         LOGGER.info("Cache[HTML]: " + String.format("%.3f", this.getMemoryUsage()) + " megabytes on " + this._loadedFiles + " files loaded.");
      } else {
         HTML_CACHE.clear();
         this._loadedFiles = 0;
         this._bytesBuffLen = 0L;
         LOGGER.info("Cache[HTML]: Running lazy cache.");
      }
   }

   public void reloadPath(File file) {
      this.parseDir(file);
      LOGGER.info("Cache[HTML]: Reloaded specified path.");
   }

   public double getMemoryUsage() {
      return (float)this._bytesBuffLen / 1048576.0F;
   }

   public int getLoadedFiles() {
      return this._loadedFiles;
   }

   private void parseDir(File dir) {
      File[] files = dir.listFiles();
      if (files != null) {
         for (File file : files) {
            if (!file.isDirectory()) {
               this.loadFile(file);
            } else {
               this.parseDir(file);
            }
         }
      }
   }

   public String loadFile(File file) {
      if (file != null && file.isFile()) {
         String lowerCaseName = file.getName().toLowerCase();
         if (!lowerCaseName.endsWith(".htm") && !lowerCaseName.endsWith(".html")) {
            return null;
         } else {
            String filePath = null;
            String content = null;

            try (
               FileInputStream fis = new FileInputStream(file);
               BufferedInputStream bis = new BufferedInputStream(fis);
            ) {
               int bytes = bis.available();
               byte[] raw = new byte[bytes];
               bis.read(raw);
               content = new String(raw, StandardCharsets.UTF_8);
               content = content.replaceAll("(?s)<!--.*?-->", "");
               content = content.replaceAll("[\\t\\n]", "");
               filePath = file.toURI().getPath().substring(ServerConfig.DATAPACK_ROOT.toURI().getPath().length());
               if (GeneralConfig.CHECK_HTML_ENCODING && !filePath.startsWith("data/lang") && !StandardCharsets.US_ASCII.newEncoder().canEncode(content)) {
                  LOGGER.warning("HTML encoding check: File " + filePath + " contains non ASCII content.");
               }

               String oldContent = HTML_CACHE.put(filePath, content);
               if (oldContent == null) {
                  this._bytesBuffLen += bytes;
                  this._loadedFiles++;
               } else {
                  this._bytesBuffLen = this._bytesBuffLen - oldContent.length() + bytes;
               }
            } catch (Exception var14) {
               LOGGER.log(Level.WARNING, "Problem with htm file:", (Throwable)var14);
            }

            return content;
         }
      } else {
         return null;
      }
   }

   public String getHtm(Player player, String path) {
      String prefix = player != null ? player.getHtmlPrefix() : "";
      String newPath = prefix + path;
      String content = HTML_CACHE.get(newPath);
      if (!GeneralConfig.HTM_CACHE && content == null) {
         content = this.loadFile(new File(ServerConfig.DATAPACK_ROOT, newPath));
         if (content == null) {
            content = this.loadFile(new File(ServerConfig.SCRIPT_ROOT, newPath));
         }
      }

      if (content == null && !prefix.contentEquals("")) {
         content = HTML_CACHE.get(path);
         newPath = path;
      }

      if (player != null && player.isGM() && GeneralConfig.GM_DEBUG_HTML_PATHS) {
         player.sendPacket(new CreatureSay(null, ChatType.GENERAL, "HTML", newPath.substring(5)));
      }

      return content;
   }

   public boolean contains(String path) {
      return HTML_CACHE.containsKey(path);
   }

   public static HtmCache getInstance() {
      return HtmCache.SingletonHolder.INSTANCE;
   }

   private static class SingletonHolder {
      protected static final HtmCache INSTANCE = new HtmCache();
   }
}
