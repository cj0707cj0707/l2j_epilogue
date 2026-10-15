package org.l2jmobius.gameserver.data.xml;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.l2jmobius.commons.util.IXmlReader;
import org.l2jmobius.gameserver.config.DevelopmentConfig;
import org.l2jmobius.gameserver.config.ServerConfig;
import org.l2jmobius.gameserver.config.custom.FakePlayersConfig;
import org.l2jmobius.gameserver.data.SpawnGroupTable;
import org.l2jmobius.gameserver.data.SpawnTable;
import org.l2jmobius.gameserver.entity.Location;
import org.l2jmobius.gameserver.entity.actor.templates.NpcTemplate;
import org.l2jmobius.gameserver.entity.spawns.Spawn;
import org.l2jmobius.gameserver.entity.spawns.SpawnGroup;
import org.l2jmobius.gameserver.entity.spawns.SpawnGroupEntry;
import org.l2jmobius.gameserver.entity.spawns.SpawnSelection;
import org.l2jmobius.gameserver.entity.zone.ZoneForm;
import org.l2jmobius.gameserver.entity.zone.form.ZoneCuboid;
import org.l2jmobius.gameserver.entity.zone.form.ZoneCylinder;
import org.l2jmobius.gameserver.entity.zone.form.ZoneNPoly;
import org.l2jmobius.gameserver.entity.zone.type.NpcSpawnTerritory;
import org.l2jmobius.gameserver.managers.DayNightSpawnManager;
import org.l2jmobius.gameserver.managers.ZoneManager;
import org.l2jmobius.gameserver.taskmanagers.SpawnGroupTaskManager;
import org.l2jmobius.gameserver.util.StatSet;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

public class SpawnData implements IXmlReader {
   private static final Logger LOGGER = Logger.getLogger(SpawnData.class.getName());
   private static final String OTHER_XML_FOLDER = "data/spawns/Others";
   private final Map<Integer, String> _spawnTemplates = new ConcurrentHashMap<>();
   private final Set<String> _parsedFiles = ConcurrentHashMap.newKeySet();
   private int _spawnCount = 0;

   protected SpawnData() {
      this.load();
   }

   @Override
   public void load() {
      this._spawnTemplates.put(0, "None");
      if (!DevelopmentConfig.NO_SPAWNS) {
         this._spawnCount = 0;
         this._parsedFiles.clear();

         for (Set<Spawn> spawns : SpawnTable.getInstance().getSpawnTable().values()) {
            Iterator<Spawn> iterator = spawns.iterator();

            while (iterator.hasNext()) {
               if (iterator.next().getGroup() != null) {
                  iterator.remove();
               }
            }
         }

         SpawnGroupTable.getInstance().clear();
         SpawnGroupTaskManager.getInstance().clear();
         LOGGER.info(this.getClass().getSimpleName() + ": Initializing spawns...");
         this.parseDatapackDirectory("data/spawns", true);
         this.checkMissingFiles(new File(".", "data/spawns"));

         for (SpawnGroup group : SpawnGroupTable.getInstance().getGroups()) {
            this._spawnCount = this._spawnCount + group.initialSpawn();
         }

         if (!SpawnGroupTable.getInstance().getGroups().isEmpty()) {
            LOGGER.info(this.getClass().getSimpleName() + ": " + SpawnGroupTable.getInstance().getGroups().size() + " spawn groups initialized.");
         }

         LOGGER.info(this.getClass().getSimpleName() + ": " + this._parsedFiles.size() + " spawn files parsed.");
         LOGGER.info(this.getClass().getSimpleName() + ": " + this._spawnCount + " spawns have been initialized!");
      }
   }

   private void checkMissingFiles(File directory) {
      File[] files = directory.listFiles();
      if (files != null) {
         for (File file : files) {
            if (file.isDirectory()) {
               this.checkMissingFiles(file);
            } else if (file.getName().toLowerCase().endsWith(".xml") && !this._parsedFiles.contains(file.getAbsolutePath())) {
               LOGGER.severe(this.getClass().getSimpleName() + ": " + file.getPath() + " was not parsed!");
            }
         }
      }
   }

   private boolean checkTemplate(int npcId) {
      NpcTemplate npcTemplate = NpcData.getInstance().getTemplate(npcId);
      if (npcTemplate != null) {
         return npcTemplate.isType("SiegeGuard") || npcTemplate.isType("RaidBoss")
            ? false
            : FakePlayersConfig.FAKE_PLAYERS_ENABLED || !npcTemplate.isFakePlayer();
      } else {
         if (FakePlayersConfig.FAKE_PLAYERS_ENABLED || npcId < 80000 && npcId > 89999) {
            LOGGER.warning(this.getClass().getSimpleName() + ": Requested spawn for non-existing NPC: " + npcId + ".");
         }

         return false;
      }
   }

   @Override
   public void parseDocument(Document document, File file) {
      this._parsedFiles.add(file.getAbsolutePath());

      for (Node list = document.getFirstChild(); list != null; list = list.getNextSibling()) {
         if (list.getNodeName().equalsIgnoreCase("list")) {
            NamedNodeMap attrs = list.getAttributes();
            if (Boolean.parseBoolean(attrs.getNamedItem("enabled").getNodeValue())) {
               for (Node param = list.getFirstChild(); param != null; param = param.getNextSibling()) {
                  attrs = param.getAttributes();
                  if (param.getNodeName().equalsIgnoreCase("spawn")) {
                     String territoryName = null;
                     String spawnName = null;
                     Map<String, Integer> map = null;
                     if (attrs.getNamedItem("name") != null) {
                        spawnName = this.parseString(attrs, "name");
                     }

                     if (attrs.getNamedItem("zone") != null) {
                        territoryName = this.parseString(attrs, "zone");
                     }

                     String[] territoryNames = null;
                     if (attrs.getNamedItem("zones") != null) {
                        if (territoryName != null) {
                           LOGGER.warning(this.getClass().getSimpleName() + ": zone and zones are exclusive, ignoring zones in " + file.getName());
                        } else {
                           territoryNames = this.parseString(attrs, "zones").split(";");
                           territoryName = territoryNames[0];
                        }
                     }

                     SpawnGroup group = null;
                     if (attrs.getNamedItem("selection") != null) {
                        String selectionValue = this.parseString(attrs, "selection");
                        SpawnSelection selection;
                        if ("random".equalsIgnoreCase(selectionValue)) {
                           selection = SpawnSelection.RANDOM;
                        } else if ("randomFill".equalsIgnoreCase(selectionValue)) {
                           selection = SpawnSelection.RANDOM_FILL;
                        } else {
                           if (!"all".equalsIgnoreCase(selectionValue)) {
                              LOGGER.warning(
                                 this.getClass().getSimpleName() + ": Unknown selection \"" + selectionValue + "\" in " + file.getName() + ", using all."
                              );
                           }

                           selection = SpawnSelection.ALL;
                        }

                        int maximumNpc = attrs.getNamedItem("maximumNpc") != null ? this.parseInteger(attrs, "maximumNpc") : 0;
                        if (selection == SpawnSelection.RANDOM && maximumNpc > 0) {
                           LOGGER.warning(this.getClass().getSimpleName() + ": maximumNpc is not read by random selection, ignoring it in " + file.getName());
                           maximumNpc = 0;
                        }

                        if (selection == SpawnSelection.RANDOM_FILL && maximumNpc <= 0) {
                           LOGGER.warning(
                              this.getClass().getSimpleName() + ": randomFill requires maximumNpc, the group in " + file.getName() + " will spawn nothing."
                           );
                        }

                        String groupName = spawnName != null ? spawnName : (territoryName != null ? territoryName : file.getName());
                        group = new SpawnGroup(groupName, selection, maximumNpc);
                        SpawnGroupTable.getInstance().addGroup(group);
                     } else if (territoryNames != null) {
                        LOGGER.warning(
                           this.getClass().getSimpleName() + ": zones requires an entry selection, using only the first territory in " + file.getName()
                        );
                        territoryNames = null;
                     }

                     for (Node npctag = param.getFirstChild(); npctag != null; npctag = npctag.getNextSibling()) {
                        attrs = npctag.getAttributes();
                        if (npctag.getNodeName().equalsIgnoreCase("AIData")) {
                           attrs = npctag.getAttributes();
                           if (map == null) {
                              map = new HashMap<>();
                           }

                           for (Node c = npctag.getFirstChild(); c != null; c = c.getNextSibling()) {
                              if (!c.getNodeName().equals("#text")) {
                                 String var40 = c.getNodeName();

                                 map.put(c.getNodeName(), switch (var40) {
                                    case "disableRandomAnimation", "disableRandomWalk" -> Boolean.parseBoolean(c.getTextContent()) ? 1 : 0;
                                    default -> Integer.parseInt(c.getTextContent());
                                 });
                              }
                           }
                        } else if (npctag.getNodeName().equalsIgnoreCase("territory")) {
                           String declaredName = attrs.getNamedItem("name") != null ? this.parseString(attrs, "name") : territoryName;
                           if (!ZoneManager.getInstance().spawnTerritoryExists(declaredName)) {
                              int minZ = this.parseInteger(attrs, "minZ");
                              int maxZ = this.parseInteger(attrs, "maxZ");
                              String zoneShape = this.parseString(attrs, "shape", "NPoly");
                              List<int[]> rs = new ArrayList<>();
                              ZoneForm zoneForm = null;

                              try {
                                 for (Node cx = npctag.getFirstChild(); cx != null; cx = cx.getNextSibling()) {
                                    if ("node".equalsIgnoreCase(cx.getNodeName())) {
                                       attrs = cx.getAttributes();
                                       int[] point = new int[]{this.parseInteger(attrs, "x"), this.parseInteger(attrs, "y")};
                                       rs.add(point);
                                    }
                                 }

                                 int[][] coords = rs.toArray(new int[0][2]);
                                 rs.clear();
                                 if (coords == null || coords.length == 0) {
                                    LOGGER.warning(
                                       this.getClass().getSimpleName()
                                          + ": SpawnTable: missing data for spawn territory: "
                                          + declaredName
                                          + " XML file: "
                                          + file.getName()
                                    );
                                    continue;
                                 }

                                 if (zoneShape.equalsIgnoreCase("Cuboid")) {
                                    if (coords.length != 2) {
                                       LOGGER.warning(
                                          this.getClass().getSimpleName()
                                             + ": SpawnTable: Missing cuboid vertex data for territory: "
                                             + declaredName
                                             + " in file: "
                                             + file.getName()
                                       );
                                       continue;
                                    }

                                    zoneForm = new ZoneCuboid(coords[0][0], coords[1][0], coords[0][1], coords[1][1], minZ, maxZ);
                                 } else if (zoneShape.equalsIgnoreCase("NPoly")) {
                                    if (coords.length <= 2) {
                                       LOGGER.warning(
                                          this.getClass().getSimpleName()
                                             + ": SpawnTable: Bad data for territory: "
                                             + declaredName
                                             + " in file: "
                                             + file.getName()
                                       );
                                       continue;
                                    }

                                    int[] aX = new int[coords.length];
                                    int[] aY = new int[coords.length];

                                    for (int i = 0; i < coords.length; i++) {
                                       aX[i] = coords[i][0];
                                       aY[i] = coords[i][1];
                                    }

                                    zoneForm = new ZoneNPoly(aX, aY, minZ, maxZ);
                                 } else {
                                    if (!zoneShape.equalsIgnoreCase("Cylinder")) {
                                       LOGGER.warning(
                                          this.getClass().getSimpleName()
                                             + ": SpawnTable: Unknown shape: \""
                                             + zoneShape
                                             + "\"  for territory: "
                                             + declaredName
                                             + " in file: "
                                             + file.getName()
                                       );
                                       continue;
                                    }

                                    int zoneRad = Integer.parseInt(attrs.getNamedItem("rad").getNodeValue());
                                    if (coords.length != 1 || zoneRad <= 0) {
                                       LOGGER.warning(
                                          this.getClass().getSimpleName()
                                             + ": SpawnTable: Bad data for territory: "
                                             + declaredName
                                             + " in file: "
                                             + file.getName()
                                       );
                                       continue;
                                    }

                                    zoneForm = new ZoneCylinder(coords[0][0], coords[0][1], minZ, maxZ, zoneRad);
                                 }
                              } catch (Exception var23) {
                                 LOGGER.log(
                                    Level.WARNING,
                                    this.getClass().getSimpleName()
                                       + ": SpawnTable: Failed to load territory "
                                       + declaredName
                                       + " coordinates: "
                                       + var23.getMessage(),
                                    (Throwable)var23
                                 );
                              }

                              ZoneManager.getInstance().addSpawnTerritory(declaredName, new NpcSpawnTerritory(declaredName, zoneForm));
                           }
                        } else if (npctag.getNodeName().equalsIgnoreCase("banned_territory")) {
                           if (!ZoneManager.getInstance().spawnTerritoryExists(territoryName)) {
                              LOGGER.warning(
                                 this.getClass().getSimpleName() + ": SpawnTable: banned_territory " + territoryName + " must be set after normal territory."
                              );
                           } else {
                              int minZ = this.parseInteger(attrs, "minZ");
                              int maxZ = this.parseInteger(attrs, "maxZ");
                              String zoneShape = this.parseString(attrs, "shape", "NPoly");
                              List<int[]> rs = new ArrayList<>();
                              ZoneForm zoneForm = null;

                              try {
                                 for (Node cxx = npctag.getFirstChild(); cxx != null; cxx = cxx.getNextSibling()) {
                                    if ("node".equalsIgnoreCase(cxx.getNodeName())) {
                                       attrs = cxx.getAttributes();
                                       int[] point = new int[]{this.parseInteger(attrs, "x"), this.parseInteger(attrs, "y")};
                                       rs.add(point);
                                    }
                                 }

                                 int[][] coordsx = rs.toArray(new int[0][2]);
                                 rs.clear();
                                 if (coordsx == null || coordsx.length == 0) {
                                    LOGGER.warning(
                                       this.getClass().getSimpleName()
                                          + ": SpawnTable: missing data for spawn banned_territory: "
                                          + territoryName
                                          + " XML file: "
                                          + file.getName()
                                    );
                                    continue;
                                 }

                                 if (zoneShape.equalsIgnoreCase("Cuboid")) {
                                    if (coordsx.length != 2) {
                                       LOGGER.warning(
                                          this.getClass().getSimpleName()
                                             + ": SpawnTable: Missing cuboid vertex data for banned_territory: "
                                             + territoryName
                                             + " in file: "
                                             + file.getName()
                                       );
                                       continue;
                                    }

                                    zoneForm = new ZoneCuboid(coordsx[0][0], coordsx[1][0], coordsx[0][1], coordsx[1][1], minZ, maxZ);
                                 } else if (zoneShape.equalsIgnoreCase("NPoly")) {
                                    if (coordsx.length <= 2) {
                                       LOGGER.warning(
                                          this.getClass().getSimpleName()
                                             + ": SpawnTable: Bad data for banned_territory: "
                                             + territoryName
                                             + " in file: "
                                             + file.getName()
                                       );
                                       continue;
                                    }

                                    int[] aX = new int[coordsx.length];
                                    int[] aY = new int[coordsx.length];

                                    for (int i = 0; i < coordsx.length; i++) {
                                       aX[i] = coordsx[i][0];
                                       aY[i] = coordsx[i][1];
                                    }

                                    zoneForm = new ZoneNPoly(aX, aY, minZ, maxZ);
                                 } else {
                                    if (!zoneShape.equalsIgnoreCase("Cylinder")) {
                                       LOGGER.warning(
                                          this.getClass().getSimpleName()
                                             + ": SpawnTable: Unknown shape: \""
                                             + zoneShape
                                             + "\"  for banned_territory: "
                                             + territoryName
                                             + " in file: "
                                             + file.getName()
                                       );
                                       continue;
                                    }

                                    int zoneRad = Integer.parseInt(attrs.getNamedItem("rad").getNodeValue());
                                    if (coordsx.length != 1 || zoneRad <= 0) {
                                       LOGGER.warning(
                                          this.getClass().getSimpleName()
                                             + ": SpawnTable: Bad data for banned_territory: "
                                             + territoryName
                                             + " in file: "
                                             + file.getName()
                                       );
                                       continue;
                                    }

                                    zoneForm = new ZoneCylinder(coordsx[0][0], coordsx[0][1], minZ, maxZ, zoneRad);
                                 }
                              } catch (Exception var24) {
                                 LOGGER.log(
                                    Level.WARNING,
                                    this.getClass().getSimpleName()
                                       + ": SpawnTable: Failed to load banned_territory "
                                       + territoryName
                                       + " coordinates: "
                                       + var24.getMessage(),
                                    (Throwable)var24
                                 );
                              }

                              ZoneManager.getInstance().getSpawnTerritory(territoryName).addBannedTerritory(zoneForm);
                           }
                        } else if (npctag.getNodeName().equalsIgnoreCase("npc")) {
                           int templateId = this.parseInteger(attrs, "id");
                           if (this.checkTemplate(templateId)) {
                              int x = 0;
                              int y = 0;
                              int z = 0;

                              try {
                                 x = this.parseInteger(attrs, "x");
                                 y = this.parseInteger(attrs, "y");
                                 z = this.parseInteger(attrs, "z");
                              } catch (NullPointerException var22) {
                              }

                              if (x == 0 && y == 0 && territoryName == null) {
                                 LOGGER.warning("XML Spawnlist: Spawn could not be initialized, both coordinates and zone are unspecified for ID " + templateId);
                              } else {
                                 Node countNode = attrs.getNamedItem("count");
                                 int count = countNode == null ? 1 : this.parseInteger(attrs, "count");
                                 StatSet spawnInfo = new StatSet();
                                 spawnInfo.set("npcTemplateid", templateId);
                                 spawnInfo.set("x", x);
                                 spawnInfo.set("y", y);
                                 spawnInfo.set("z", z);
                                 spawnInfo.set("territoryName", territoryName);
                                 spawnInfo.set("spawnName", spawnName);
                                 if (attrs.getNamedItem("heading") != null) {
                                    spawnInfo.set("heading", this.parseInteger(attrs, "heading"));
                                 }

                                 if (attrs.getNamedItem("respawnDelay") != null) {
                                    spawnInfo.set("respawnDelay", this.parseInteger(attrs, "respawnDelay"));
                                 }

                                 if (attrs.getNamedItem("respawnRandom") != null) {
                                    spawnInfo.set("respawnRandom", this.parseInteger(attrs, "respawnRandom"));
                                 }

                                 if (attrs.getNamedItem("chaseRange") != null) {
                                    spawnInfo.set("chaseRange", this.parseInteger(attrs, "chaseRange"));
                                 }

                                 if (attrs.getNamedItem("periodOfDay") != null) {
                                    String period = attrs.getNamedItem("periodOfDay").getNodeValue();
                                    if (period.equalsIgnoreCase("day") || period.equalsIgnoreCase("night")) {
                                       spawnInfo.set("periodOfDay", period.equalsIgnoreCase("day") ? 1 : 2);
                                    }
                                 }

                                 spawnInfo.set("fileName", file.getPath());
                                 if (group != null) {
                                    if (spawnInfo.getInt("periodOfDay", 0) != 0) {
                                       LOGGER.warning(
                                          this.getClass().getSimpleName()
                                             + ": periodOfDay cannot be combined with an entry selection, skipping NPC "
                                             + templateId
                                             + " in "
                                             + file.getName()
                                       );
                                    } else {
                                       this.addGroupSpawn(group, spawnInfo, map, count, territoryNames);
                                    }
                                 } else {
                                    for (int i = 0; i < count; i++) {
                                       this._spawnCount = this._spawnCount + this.addSpawn(spawnInfo, map);
                                    }
                                 }
                              }
                           }
                        }
                     }
                  }
               }
            }
         }
      }
   }

   private int addSpawn(StatSet spawnInfo, Map<String, Integer> aiData) {
      int ret = 0;

      try {
         Spawn spawnDat = this.createSpawn(spawnInfo, aiData);
         switch (spawnInfo.getInt("periodOfDay", 0)) {
            case 0:
               ret += spawnDat.init();
               break;
            case 1:
               DayNightSpawnManager.getInstance().addDayCreature(spawnDat);
               ret = 1;
               break;
            case 2:
               DayNightSpawnManager.getInstance().addNightCreature(spawnDat);
               ret = 1;
         }

         this.registerSpawn(spawnDat, spawnInfo.getString("fileName", "None"));
      } catch (Exception var6) {
         LOGGER.log(Level.WARNING, "Spawn could not be initialized: " + var6.getMessage(), (Throwable)var6);
      }

      return ret;
   }

   private void addGroupSpawn(SpawnGroup group, StatSet spawnInfo, Map<String, Integer> aiData, int total, String[] territoryNames) {
      try {
         SpawnGroupEntry entry = new SpawnGroupEntry(total, spawnInfo.getInt("respawnDelay", 0), spawnInfo.getInt("respawnRandom", 0));
         int slotCount = territoryNames != null ? territoryNames.length : 1;

         for (int i = 0; i < slotCount; i++) {
            if (territoryNames != null) {
               spawnInfo.set("territoryName", territoryNames[i]);
            }

            Spawn spawnDat = this.createSpawn(spawnInfo, aiData);
            spawnDat.setAmount(total);
            if (group.getSelection() != SpawnSelection.ALL || spawnInfo.getInt("respawnDelay", 0) <= 0) {
               spawnDat.stopRespawn();
            }

            NpcSpawnTerritory slotTerritory = spawnDat.getSpawnTerritory();
            if (slotTerritory != null) {
               spawnDat.setXYZ(slotTerritory.getRandomPoint());
            }

            spawnDat.setGroup(group);
            entry.addSlot(spawnDat);
         }

         for (Spawn slot : entry.getSlots()) {
            this.registerSpawn(slot, spawnInfo.getString("fileName", "None"));
         }

         group.addEntry(entry);
      } catch (Exception var11) {
         LOGGER.log(Level.WARNING, "Group spawn could not be initialized: " + var11.getMessage(), (Throwable)var11);
      }
   }

   private Spawn createSpawn(StatSet spawnInfo, Map<String, Integer> aiData) throws Exception {
      Spawn spawnDat = new Spawn(spawnInfo.getInt("npcTemplateid"));
      spawnDat.setAmount(spawnInfo.getInt("count", 1));
      spawnDat.setXYZ(spawnInfo.getInt("x", 0), spawnInfo.getInt("y", 0), spawnInfo.getInt("z", 0));
      spawnDat.setHeading(spawnInfo.getInt("heading", -1));
      spawnDat.setRespawnDelay(spawnInfo.getInt("respawnDelay", 0), spawnInfo.getInt("respawnRandom", 0));
      spawnDat.setChaseRange(spawnInfo.getInt("chaseRange", 0));
      spawnDat.setLocationId(spawnInfo.getInt("locId", 0));
      String territoryName = spawnInfo.getString("territoryName", "");
      String spawnName = spawnInfo.getString("spawnName", "");
      if (!spawnName.isEmpty()) {
         spawnDat.setName(spawnName);
      }

      if (!territoryName.isEmpty()) {
         spawnDat.setSpawnTerritory(ZoneManager.getInstance().getSpawnTerritory(territoryName));
      }

      NpcPersonalAIData.getInstance().storeData(spawnDat, aiData);
      return spawnDat;
   }

   private void registerSpawn(Spawn spawnDat, String fileName) {
      if (this._spawnTemplates.values().contains(fileName)) {
         for (Entry<Integer, String> entry : this._spawnTemplates.entrySet()) {
            if (entry.getValue().equals(fileName)) {
               spawnDat.setSpawnTemplateId(entry.getKey());
               break;
            }
         }
      } else {
         int newId = this._spawnTemplates.size();
         this._spawnTemplates.put(newId, fileName);
         spawnDat.setSpawnTemplateId(newId);
      }

      SpawnTable.getInstance().addSpawn(spawnDat);
   }

   public synchronized void addNewSpawn(Spawn spawn) {
      SpawnTable.getInstance().addSpawn(spawn);
      File outputDirectory = new File("data/spawns/Others");
      if (!outputDirectory.exists()) {
         boolean result = false;

         try {
            outputDirectory.mkdir();
            result = true;
         } catch (SecurityException var21) {
         }

         if (result) {
            LOGGER.info(this.getClass().getSimpleName() + ": Created directory: data/spawns/Others");
         }
      }

      int x = (spawn.getX() - -294912 >> 15) + 11;
      int y = (spawn.getY() - -262144 >> 15) + 10;
      File spawnFile = new File("data/spawns/Others/" + x + "_" + y + ".xml");
      String spawnId = String.valueOf(spawn.getId());
      String spawnCount = String.valueOf(spawn.getAmount());
      String spawnX = String.valueOf(spawn.getX());
      String spawnY = String.valueOf(spawn.getY());
      String spawnZ = String.valueOf(spawn.getZ());
      String spawnHeading = String.valueOf(spawn.getHeading());
      String spawnDelay = String.valueOf(spawn.getRespawnDelay() / 1000);
      if (spawnFile.exists()) {
         File tempFile = new File(spawnFile.getAbsolutePath().substring(ServerConfig.DATAPACK_ROOT.getAbsolutePath().length() + 1).replace('\\', '/') + ".tmp");

         try {
            BufferedReader reader = new BufferedReader(new FileReader(spawnFile));
            BufferedWriter writer = new BufferedWriter(new FileWriter(tempFile));

            String currentLine;
            while ((currentLine = reader.readLine()) != null) {
               if (currentLine.contains("</spawn>")) {
                  NpcTemplate template = NpcData.getInstance().getTemplate(spawn.getId());
                  String title = template.getTitle();
                  String name = title.isEmpty() ? template.getName() : template.getName() + " - " + title;
                  writer.write(
                     "\t\t<npc id=\""
                        + spawnId
                        + (spawn.getAmount() > 1 ? "\" count=\"" + spawnCount : "")
                        + "\" x=\""
                        + spawnX
                        + "\" y=\""
                        + spawnY
                        + "\" z=\""
                        + spawnZ
                        + (spawn.getHeading() > 0 ? "\" heading=\"" + spawnHeading : "")
                        + "\" respawnDelay=\""
                        + spawnDelay
                        + "\" /> <!-- "
                        + name
                        + " -->"
                        + System.lineSeparator()
                  );
                  writer.write(currentLine + System.lineSeparator());
               } else {
                  writer.write(currentLine + System.lineSeparator());
               }
            }

            writer.close();
            reader.close();
            spawnFile.delete();
            tempFile.renameTo(spawnFile);
         } catch (Exception var22) {
            LOGGER.warning(this.getClass().getSimpleName() + ": Could not store spawn in the spawn XML files: " + var22);
         }
      } else {
         try {
            NpcTemplate template = NpcData.getInstance().getTemplate(spawn.getId());
            String title = template.getTitle();
            String name = title.isEmpty() ? template.getName() : template.getName() + " - " + title;
            BufferedWriter writer = new BufferedWriter(new FileWriter(spawnFile));
            writer.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + System.lineSeparator());
            writer.write(
               "<list enabled=\"true\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:noNamespaceSchemaLocation=\"../../xsd/spawns.xsd\">"
                  + System.lineSeparator()
            );
            writer.write("\t<spawn name=\"" + x + "_" + y + "\">" + System.lineSeparator());
            writer.write(
               "\t\t<npc id=\""
                  + spawnId
                  + (spawn.getAmount() > 1 ? "\" count=\"" + spawnCount : "")
                  + "\" x=\""
                  + spawnX
                  + "\" y=\""
                  + spawnY
                  + "\" z=\""
                  + spawnZ
                  + (spawn.getHeading() > 0 ? "\" heading=\"" + spawnHeading : "")
                  + "\" respawnDelay=\""
                  + spawnDelay
                  + "\" /> <!-- "
                  + name
                  + " -->"
                  + System.lineSeparator()
            );
            writer.write("\t</spawn>" + System.lineSeparator());
            writer.write("</list>" + System.lineSeparator());
            writer.close();
            LOGGER.info(this.getClass().getSimpleName() + ": Created file: data/spawns/Others/" + x + "_" + y + ".xml");
         } catch (Exception var20) {
            LOGGER.warning(this.getClass().getSimpleName() + ": Spawn " + spawn + " could not be added to the spawn XML files: " + var20);
         }
      }
   }

   public synchronized void deleteSpawn(Spawn spawn) {
      SpawnTable.getInstance().removeSpawn(spawn);
      int x = (spawn.getX() - -294912 >> 15) + 11;
      int y = (spawn.getY() - -262144 >> 15) + 10;
      int npcSpawnTemplateId = spawn.getNpcSpawnTemplateId();
      Location spawnLocation = spawn.getSpawnLocation();
      File spawnFile = npcSpawnTemplateId > 0 ? new File(this._spawnTemplates.get(npcSpawnTemplateId)) : new File("data/spawns/Others/" + x + "_" + y + ".xml");
      File tempFile = new File(spawnFile.getAbsolutePath().substring(ServerConfig.DATAPACK_ROOT.getAbsolutePath().length() + 1).replace('\\', '/') + ".tmp");

      try {
         BufferedReader reader = new BufferedReader(new FileReader(spawnFile));
         BufferedWriter writer = new BufferedWriter(new FileWriter(tempFile));
         boolean found = false;
         boolean isMultiLine = false;
         boolean lastLineFound = false;
         int lineCount = 0;
         NpcSpawnTerritory npcSpawnTerritory = spawn.getSpawnTerritory();
         String currentLine;
         if (npcSpawnTerritory == null) {
            String spawnId = String.valueOf(spawn.getId());
            String spawnX = String.valueOf(spawnLocation != null ? spawnLocation.getX() : spawn.getX());
            String spawnY = String.valueOf(spawnLocation != null ? spawnLocation.getY() : spawn.getY());
            String spawnZ = String.valueOf(spawnLocation != null ? spawnLocation.getZ() : spawn.getZ());

            while ((currentLine = reader.readLine()) != null) {
               if (!found) {
                  if (isMultiLine) {
                     if (currentLine.contains("</npc>")) {
                        found = true;
                     }
                     continue;
                  }

                  if (currentLine.contains(spawnId) && currentLine.contains(spawnX) && currentLine.contains(spawnY) && currentLine.contains(spawnZ)) {
                     if (!currentLine.contains("/>") && !currentLine.contains("</npc>")) {
                        isMultiLine = true;
                     } else {
                        found = true;
                     }
                     continue;
                  }
               }

               writer.write(currentLine + System.lineSeparator());
               if (currentLine.contains("</list>")) {
                  lastLineFound = true;
               }

               if (!lastLineFound) {
                  lineCount++;
               }
            }
         } else {
            while ((currentLine = reader.readLine()) != null) {
               if (!found) {
                  if (isMultiLine) {
                     if (currentLine.contains("</spawn>")) {
                        found = true;
                     }
                     continue;
                  }

                  if (currentLine.contains("\"" + npcSpawnTerritory.getName() + "\"")) {
                     isMultiLine = true;
                     continue;
                  }
               }

               writer.write(currentLine + System.lineSeparator());
               if (currentLine.contains("</list>")) {
                  lastLineFound = true;
               }

               if (!lastLineFound) {
                  lineCount++;
               }
            }
         }

         writer.close();
         reader.close();
         spawnFile.delete();
         tempFile.renameTo(spawnFile);
         if (lineCount < 5) {
            LOGGER.info(
               this.getClass().getSimpleName()
                  + ": Deleted empty file: "
                  + spawnFile.getAbsolutePath().substring(ServerConfig.DATAPACK_ROOT.getAbsolutePath().length() + 1).replace('\\', '/')
            );
            spawnFile.delete();
         }
      } catch (Exception var20) {
         LOGGER.warning(this.getClass().getSimpleName() + ": Spawn " + spawn + " could not be removed from the spawn XML files: " + var20);
      }
   }

   public String getSpawnFile(int templateId) {
      return this._spawnTemplates.get(templateId);
   }

   public static SpawnData getInstance() {
      return SpawnData.SingletonHolder.INSTANCE;
   }

   private static class SingletonHolder {
      protected static final SpawnData INSTANCE = new SpawnData();
   }
}
