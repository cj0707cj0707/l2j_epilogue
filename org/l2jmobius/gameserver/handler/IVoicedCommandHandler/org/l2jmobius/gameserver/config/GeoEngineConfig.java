package org.l2jmobius.gameserver.config;

import java.nio.file.Path;
import java.nio.file.Paths;
import org.l2jmobius.commons.util.ConfigReader;

public class GeoEngineConfig {
   private static final String GEOENGINE_CONFIG_FILE = "./config/GeoEngine.ini";
   public static Path GEODATA_PATH;
   public static Path PATHNODE_PATH;
   public static Path GEOEDIT_PATH;
   public static int PATHFINDING;
   public static String PATHFIND_BUFFERS;
   public static float LOW_WEIGHT;
   public static float MEDIUM_WEIGHT;
   public static float HIGH_WEIGHT;
   public static boolean ADVANCED_DIAGONAL_STRATEGY;
   public static boolean AVOID_OBSTRUCTED_PATH_NODES;
   public static float DIAGONAL_WEIGHT;
   public static int MAX_POSTFILTER_PASSES;

   public static void load() {
      ConfigReader config = new ConfigReader("./config/GeoEngine.ini");
      GEODATA_PATH = Paths.get(ServerConfig.DATAPACK_ROOT.getPath() + "/" + config.getString("GeoDataPath", "geodata"));
      PATHNODE_PATH = Paths.get(ServerConfig.DATAPACK_ROOT.getPath() + "/" + config.getString("PathnodePath", "pathnode"));
      GEOEDIT_PATH = Paths.get(ServerConfig.DATAPACK_ROOT.getPath() + "/" + config.getString("GeoEditPath", "saves"));
      PATHFINDING = config.getInt("PathFinding", 0);
      PATHFIND_BUFFERS = config.getString("PathFindBuffers", "100x6;128x6;192x6;256x4;320x4;384x4;500x2");
      LOW_WEIGHT = config.getFloat("LowWeight", 0.5F);
      MEDIUM_WEIGHT = config.getFloat("MediumWeight", 2.0F);
      HIGH_WEIGHT = config.getFloat("HighWeight", 3.0F);
      ADVANCED_DIAGONAL_STRATEGY = config.getBoolean("AdvancedDiagonalStrategy", true);
      AVOID_OBSTRUCTED_PATH_NODES = config.getBoolean("AvoidObstructedPathNodes", true);
      DIAGONAL_WEIGHT = config.getFloat("DiagonalWeight", 0.707F);
      MAX_POSTFILTER_PASSES = config.getInt("MaxPostfilterPasses", 3);
   }
}
