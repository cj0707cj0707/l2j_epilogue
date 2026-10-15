package org.l2jmobius.gameserver.config;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.math.BigInteger;
import java.net.Inet6Address;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.IntStream;
import org.l2jmobius.commons.util.ConfigReader;
import org.l2jmobius.commons.util.IXmlReader;
import org.l2jmobius.commons.util.StringUtil;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

public class ServerConfig {
   private static final Logger LOGGER = Logger.getLogger(ServerConfig.class.getName());
   private static final String SERVER_CONFIG_FILE = "./config/Server.ini";
   private static final String IPCONFIG_FILE = "./config/ipconfig.xml";
   private static final String CHAT_FILTER_FILE = "./config/chatfilter.txt";
   private static final String HEXID_FILE = "./config/hexid.txt";
   public static String GAMESERVER_HOSTNAME;
   public static int PORT_GAME;
   public static int GAME_SERVER_LOGIN_PORT;
   public static String GAME_SERVER_LOGIN_HOST;
   public static boolean PACKET_ENCRYPTION;
   public static int REQUEST_ID;
   public static boolean ACCEPT_ALTERNATE_ID;
   public static File DATAPACK_ROOT;
   public static File SCRIPT_ROOT;
   public static Pattern CHARNAME_TEMPLATE_PATTERN;
   public static Pattern PET_NAME_TEMPLATE_PATTERN;
   public static Pattern CLAN_NAME_TEMPLATE_PATTERN;
   public static int MAX_CHARACTERS_NUMBER_PER_ACCOUNT;
   public static int MAXIMUM_ONLINE_USERS;
   public static boolean HARDWARE_INFO_ENABLED;
   public static boolean KICK_MISSING_HWID;
   public static int MAX_PLAYERS_PER_HWID;
   public static List<Integer> PROTOCOL_LIST;
   public static int SERVER_LIST_TYPE;
   public static int SERVER_LIST_AGE;
   public static boolean SERVER_LIST_BRACKET;
   public static boolean DEADLOCK_WATCHER;
   public static int DEADLOCK_CHECK_INTERVAL;
   public static boolean RESTART_ON_DEADLOCK;
   public static boolean SERVER_RESTART_SCHEDULE_ENABLED;
   public static boolean SERVER_RESTART_SCHEDULE_MESSAGE;
   public static int SERVER_RESTART_SCHEDULE_COUNTDOWN;
   public static String[] SERVER_RESTART_SCHEDULE;
   public static List<Integer> SERVER_RESTART_DAYS;
   public static boolean PRECAUTIONARY_RESTART_ENABLED;
   public static boolean PRECAUTIONARY_RESTART_CPU;
   public static boolean PRECAUTIONARY_RESTART_MEMORY;
   public static boolean PRECAUTIONARY_RESTART_CHECKS;
   public static int PRECAUTIONARY_RESTART_PERCENTAGE;
   public static int PRECAUTIONARY_RESTART_DELAY;
   public static List<String> GAME_SERVER_SUBNETS;
   public static List<String> GAME_SERVER_HOSTS;
   public static boolean RESERVE_HOST_ON_LOGIN = false;
   public static List<String> FILTER_LIST;
   public static int SERVER_ID;
   public static byte[] HEX_ID;

   public static void load() {
      ConfigReader config = new ConfigReader("./config/Server.ini");
      GAMESERVER_HOSTNAME = config.getString("GameserverHostname", "0.0.0.0");
      PORT_GAME = config.getInt("GameserverPort", 7777);
      GAME_SERVER_LOGIN_PORT = config.getInt("LoginPort", 9014);
      GAME_SERVER_LOGIN_HOST = config.getString("LoginHost", "127.0.0.1");
      PACKET_ENCRYPTION = config.getBoolean("PacketEncryption", false);
      REQUEST_ID = config.getInt("RequestServerID", 0);
      ACCEPT_ALTERNATE_ID = config.getBoolean("AcceptAlternateID", true);

      try {
         DATAPACK_ROOT = new File(config.getString("DatapackRoot", ".").replace('\\', '/')).getCanonicalFile();
      } catch (IOException var15) {
         LOGGER.log(Level.WARNING, "Error setting datapack root!", (Throwable)var15);
         DATAPACK_ROOT = new File(".");
      }

      try {
         SCRIPT_ROOT = new File(config.getString("ScriptRoot", "./data/scripts").replace('\\', '/')).getCanonicalFile();
      } catch (Exception var14) {
         LOGGER.log(Level.WARNING, "Error setting script root!", (Throwable)var14);
         SCRIPT_ROOT = new File(".");
      }

      Pattern charNamePattern;
      try {
         charNamePattern = Pattern.compile(config.getString("CnameTemplate", ".*"));
      } catch (PatternSyntaxException var13) {
         LOGGER.log(Level.WARNING, "Character name pattern is invalid!", (Throwable)var13);
         charNamePattern = Pattern.compile(".*");
      }

      CHARNAME_TEMPLATE_PATTERN = charNamePattern;

      Pattern petNamePattern;
      try {
         petNamePattern = Pattern.compile(config.getString("PetNameTemplate", ".*"));
      } catch (PatternSyntaxException var12) {
         LOGGER.log(Level.WARNING, "Pet name pattern is invalid!", (Throwable)var12);
         petNamePattern = Pattern.compile(".*");
      }

      PET_NAME_TEMPLATE_PATTERN = petNamePattern;

      Pattern clanNamePattern;
      try {
         clanNamePattern = Pattern.compile(config.getString("ClanNameTemplate", ".*"));
      } catch (PatternSyntaxException var11) {
         LOGGER.log(Level.WARNING, "Clan name pattern is invalid!", (Throwable)var11);
         clanNamePattern = Pattern.compile(".*");
      }

      CLAN_NAME_TEMPLATE_PATTERN = clanNamePattern;
      MAX_CHARACTERS_NUMBER_PER_ACCOUNT = config.getInt("CharMaxNumber", 7);
      MAXIMUM_ONLINE_USERS = config.getInt("MaximumOnlineUsers", 100);
      HARDWARE_INFO_ENABLED = config.getBoolean("EnableHardwareInfo", false);
      KICK_MISSING_HWID = config.getBoolean("KickMissingHWID", false);
      MAX_PLAYERS_PER_HWID = config.getInt("MaxPlayersPerHWID", 0);
      if (MAX_PLAYERS_PER_HWID > 0) {
         KICK_MISSING_HWID = true;
      }

      String[] protocols = config.getString("AllowedProtocolRevisions", "152").split(";");
      PROTOCOL_LIST = new ArrayList<>(protocols.length);

      for (String protocol : protocols) {
         try {
            PROTOCOL_LIST.add(Integer.parseInt(protocol.trim()));
         } catch (NumberFormatException var10) {
            LOGGER.warning("Wrong config protocol version: " + protocol + ". Skipped.");
         }
      }

      SERVER_LIST_TYPE = getServerTypeId(config.getString("ServerListType", "Free").split(","));
      SERVER_LIST_AGE = config.getInt("ServerListAge", 0);
      SERVER_LIST_BRACKET = config.getBoolean("ServerListBrackets", false);
      DEADLOCK_WATCHER = config.getBoolean("DeadlockWatcher", true);
      DEADLOCK_CHECK_INTERVAL = config.getInt("DeadlockCheckInterval", 20);
      RESTART_ON_DEADLOCK = config.getBoolean("RestartOnDeadlock", false);
      SERVER_RESTART_SCHEDULE_ENABLED = config.getBoolean("ServerRestartScheduleEnabled", false);
      SERVER_RESTART_SCHEDULE_MESSAGE = config.getBoolean("ServerRestartScheduleMessage", false);
      SERVER_RESTART_SCHEDULE_COUNTDOWN = config.getInt("ServerRestartScheduleCountdown", 600);
      SERVER_RESTART_SCHEDULE = config.getString("ServerRestartSchedule", "08:00").split(",");
      SERVER_RESTART_DAYS = new ArrayList<>();

      for (String day : config.getString("ServerRestartDays", "").trim().split(",")) {
         if (StringUtil.isNumeric(day)) {
            SERVER_RESTART_DAYS.add(Integer.parseInt(day));
         }
      }

      PRECAUTIONARY_RESTART_ENABLED = config.getBoolean("PrecautionaryRestartEnabled", false);
      PRECAUTIONARY_RESTART_CPU = config.getBoolean("PrecautionaryRestartCpu", true);
      PRECAUTIONARY_RESTART_MEMORY = config.getBoolean("PrecautionaryRestartMemory", false);
      PRECAUTIONARY_RESTART_CHECKS = config.getBoolean("PrecautionaryRestartChecks", true);
      PRECAUTIONARY_RESTART_PERCENTAGE = config.getInt("PrecautionaryRestartPercentage", 95);
      PRECAUTIONARY_RESTART_DELAY = config.getInt("PrecautionaryRestartDelay", 60) * 1000;
      ServerConfig.IPConfigData ipConfigData = new ServerConfig.IPConfigData();
      GAME_SERVER_SUBNETS = ipConfigData.getSubnets();
      GAME_SERVER_HOSTS = ipConfigData.getHosts();
      loadChatFilter();
      loadHexid();
   }

   private static void loadChatFilter() {
      try {
         FILTER_LIST = Files.lines(Paths.get("./config/chatfilter.txt"), StandardCharsets.UTF_8)
            .map(String::trim)
            .filter(line -> !line.isEmpty() && line.charAt(0) != '#')
            .toList();
         LOGGER.info("Loaded " + FILTER_LIST.size() + " Filter Words.");
      } catch (IOException var1) {
         LOGGER.log(Level.WARNING, "Error while loading chat filter words!", (Throwable)var1);
      }
   }

   private static void loadHexid() {
      File hexIdFile = new File("./config/hexid.txt");
      if (hexIdFile.exists()) {
         ConfigReader hexId = new ConfigReader("./config/hexid.txt");
         if (hexId.containsKey("ServerID") && hexId.containsKey("HexID")) {
            SERVER_ID = hexId.getInt("ServerID", 1);

            try {
               HEX_ID = new BigInteger(hexId.getString("HexID", null), 16).toByteArray();
            } catch (Exception var3) {
               LOGGER.warning("Could not load HexID file (./config/hexid.txt). Hopefully login will give us one.");
            }
         }
      }

      if (HEX_ID == null) {
         LOGGER.warning("Could not load HexID file (./config/hexid.txt). Hopefully login will give us one.");
      }
   }

   public static void saveHexid(int serverId, String hexId) {
      saveHexid(serverId, hexId, "./config/hexid.txt");
   }

   private static void saveHexid(int serverId, String hexId, String fileName) {
      try {
         Properties hexSetting = new Properties();
         File file = new File(fileName);
         if (!file.exists()) {
            try (OutputStream out = new FileOutputStream(file)) {
               hexSetting.setProperty("ServerID", String.valueOf(serverId));
               hexSetting.setProperty("HexID", hexId);
               hexSetting.store(out, "The HexId to Auth into LoginServer");
               LOGGER.log(Level.INFO, "Gameserver: Generated new HexID file for server id " + serverId + ".");
            }
         }
      } catch (Exception var10) {
         LOGGER.warning(StringUtil.concat("Failed to save hex id to ", fileName, " File."));
         LOGGER.warning("Config: " + var10.getMessage());
      }
   }

   public static int getServerTypeId(String[] serverTypes) {
      int serverType = 0;

      for (String cType : serverTypes) {
         String var6 = cType.trim().toLowerCase();
         switch (var6) {
            case "normal":
               serverType |= 1;
               break;
            case "relax":
               serverType |= 2;
               break;
            case "test":
               serverType |= 4;
               break;
            case "nolabel":
               serverType |= 8;
               break;
            case "restricted":
               serverType |= 16;
               break;
            case "event":
               serverType |= 32;
               break;
            case "free":
               serverType |= 64;
         }
      }

      return serverType;
   }

   private static class IPConfigData implements IXmlReader {
      private static final List<String> _subnets = new ArrayList<>(5);
      private static final List<String> _hosts = new ArrayList<>(5);

      public IPConfigData() {
         this.load();
      }

      @Override
      public void load() {
         File file = new File("./config/ipconfig.xml");
         if (file.exists()) {
            LOGGER.info("Network Config: ipconfig.xml exists, using manual configuration...");
            this.parseFile(new File("./config/ipconfig.xml"));
         } else {
            LOGGER.info("Network Config: ipconfig.xml does not exist, using automatic configuration...");
            this.autoIpConfig();
         }
      }

      @Override
      public void parseDocument(Document document, File file) {
         for (Node n = document.getFirstChild(); n != null; n = n.getNextSibling()) {
            if ("gameserver".equalsIgnoreCase(n.getNodeName())) {
               for (Node d = n.getFirstChild(); d != null; d = d.getNextSibling()) {
                  if ("define".equalsIgnoreCase(d.getNodeName())) {
                     NamedNodeMap attrs = d.getAttributes();
                     _subnets.add(attrs.getNamedItem("subnet").getNodeValue());
                     _hosts.add(attrs.getNamedItem("address").getNodeValue());
                     if (_hosts.size() != _subnets.size()) {
                        LOGGER.warning("Failed to Load ./config/ipconfig.xml File - subnets does not match server addresses.");
                     }
                  }
               }

               Node att = n.getAttributes().getNamedItem("address");
               if (att == null) {
                  LOGGER.warning("Failed to load ./config/ipconfig.xml file - default server address is missing.");
                  _hosts.add("127.0.0.1");
               } else {
                  _hosts.add(att.getNodeValue());
               }

               _subnets.add("0.0.0.0/0");
            }
         }
      }

      protected void autoIpConfig() {
         String externalIp = "127.0.0.1";

         try {
            URL autoIp = URI.create("http://checkip.amazonaws.com").toURL();

            try (BufferedReader in = new BufferedReader(new InputStreamReader(autoIp.openStream()))) {
               externalIp = in.readLine();
            }
         } catch (IOException var15) {
            LOGGER.log(Level.INFO, "Failed to connect to checkip.amazonaws.com please check your internet connection using 127.0.0.1!");
            externalIp = "127.0.0.1";
         }

         try {
            Enumeration<NetworkInterface> niList = NetworkInterface.getNetworkInterfaces();

            while (niList.hasMoreElements()) {
               NetworkInterface ni = niList.nextElement();
               if (ni.isUp() && !ni.isVirtual() && (ni.isLoopback() || ni.getHardwareAddress() != null && ni.getHardwareAddress().length == 6)) {
                  for (InterfaceAddress ia : ni.getInterfaceAddresses()) {
                     if (!(ia.getAddress() instanceof Inet6Address)) {
                        String hostAddress = ia.getAddress().getHostAddress();
                        int subnetPrefixLength = ia.getNetworkPrefixLength();
                        int subnetMaskInt = IntStream.rangeClosed(1, subnetPrefixLength).reduce((r, var1x) -> (r << 1) + 1).orElse(0)
                           << 32 - subnetPrefixLength;
                        int hostAddressInt = Arrays.stream(hostAddress.split("\\.")).mapToInt(Integer::parseInt).reduce((r, ex) -> (r << 8) + ex).orElse(0);
                        int subnetAddressInt = hostAddressInt & subnetMaskInt;
                        String subnetAddress = (subnetAddressInt >> 24 & 0xFF)
                           + "."
                           + (subnetAddressInt >> 16 & 0xFF)
                           + "."
                           + (subnetAddressInt >> 8 & 0xFF)
                           + "."
                           + (subnetAddressInt & 0xFF);
                        String subnet = subnetAddress + "/" + subnetPrefixLength;
                        if (!_subnets.contains(subnet) && !subnet.equals("0.0.0.0/0")) {
                           _subnets.add(subnet);
                           _hosts.add(hostAddress);
                           LOGGER.info("Network Config: Adding new subnet: " + subnet + " address: " + hostAddress);
                        }
                     }
                  }
               }
            }

            _hosts.add(externalIp);
            _subnets.add("0.0.0.0/0");
            LOGGER.info("Network Config: Adding new subnet: 0.0.0.0/0 address: " + externalIp);
         } catch (SocketException var16) {
            LOGGER.log(Level.INFO, "Network Config: Configuration failed please configure manually using ipconfig.xml", (Throwable)var16);
            System.exit(0);
         }
      }

      protected List<String> getSubnets() {
         return _subnets.isEmpty() ? Arrays.asList("0.0.0.0/0") : _subnets;
      }

      protected List<String> getHosts() {
         return _hosts.isEmpty() ? Arrays.asList("127.0.0.1") : _hosts;
      }
   }
}
