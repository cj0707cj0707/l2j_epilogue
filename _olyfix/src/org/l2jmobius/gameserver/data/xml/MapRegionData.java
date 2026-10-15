package org.l2jmobius.gameserver.data.xml;

import java.io.File;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import org.l2jmobius.commons.util.IXmlReader;
import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.config.PlayerConfig;
import org.l2jmobius.gameserver.config.custom.FactionSystemConfig;
import org.l2jmobius.gameserver.data.sql.ClanHallTable;
import org.l2jmobius.gameserver.entity.Location;
import org.l2jmobius.gameserver.entity.WorldObject;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.enums.creature.Race;
import org.l2jmobius.gameserver.entity.actor.enums.player.TeleportWhereType;
import org.l2jmobius.gameserver.entity.clan.Clan;
import org.l2jmobius.gameserver.entity.instancezone.Instance;
import org.l2jmobius.gameserver.entity.residences.ClanHall;
import org.l2jmobius.gameserver.entity.zone.type.ClanHallZone;
import org.l2jmobius.gameserver.entity.zone.type.RespawnZone;
import org.l2jmobius.gameserver.interfaces.ILocational;
import org.l2jmobius.gameserver.managers.CastleManager;
import org.l2jmobius.gameserver.managers.FortManager;
import org.l2jmobius.gameserver.managers.InstanceManager;
import org.l2jmobius.gameserver.managers.ZoneManager;
import org.l2jmobius.gameserver.mechanics.sevensigns.SevenSigns;
import org.l2jmobius.gameserver.mechanics.siege.Castle;
import org.l2jmobius.gameserver.mechanics.siege.Fort;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

public class MapRegionData implements IXmlReader {
   private static final Logger LOGGER = Logger.getLogger(MapRegionData.class.getName());
   private static final String DEFAULT_RESPAWN = "talking_island_town";
   private static final String FALLBACK_FLYING_RESPAWN = "union_base_of_kserth";
   private static final String DEFAULT_TOWN = "Aden Castle Town";
   private static final int MAP_REGION_X_OFFSET = 20;
   private static final int MAP_REGION_Y_OFFSET = 18;
   private static final int COORDINATE_SHIFT = 15;
   private static final int GRID_SIZE = 32;
   private final Map<String, MapRegionData.RegionData> _regions = new HashMap<>();
   private final MapRegionData.RegionData[][] _spatialGrid = new MapRegionData.RegionData[32][32];
   private MapRegionData.RegionData _defaultRespawnRegion;
   private MapRegionData.RegionData _fallbackFlyingRegion;

   protected MapRegionData() {
      this.load();
   }

   @Override
   public void load() {
      this._regions.clear();

      for (int i = 0; i < 32; i++) {
         for (int j = 0; j < 32; j++) {
            this._spatialGrid[i][j] = null;
         }
      }

      this.parseDatapackDirectory("data/mapregion", false);
      this.buildSpatialIndex();
      this._defaultRespawnRegion = this._regions.get("talking_island_town");
      this._fallbackFlyingRegion = this._regions.get("union_base_of_kserth");
      LOGGER.info(this.getClass().getSimpleName() + ": Loaded " + this._regions.size() + " map regions.");
   }

   private void buildSpatialIndex() {
      for (MapRegionData.RegionData region : this._regions.values()) {
         if (region.maps != null) {
            for (int[] map : region.maps) {
               int x = map[0];
               int y = map[1];
               if (x >= 0 && x < 32 && y >= 0 && y < 32) {
                  this._spatialGrid[x][y] = region;
               }
            }
         }
      }
   }

   @Override
   public void parseDocument(Document document, File file) {
      for (Node n = document.getFirstChild(); n != null; n = n.getNextSibling()) {
         if ("list".equalsIgnoreCase(n.getNodeName())) {
            for (Node d = n.getFirstChild(); d != null; d = d.getNextSibling()) {
               if ("region".equalsIgnoreCase(d.getNodeName())) {
                  NamedNodeMap attrs = d.getAttributes();
                  String name = attrs.getNamedItem("name").getNodeValue();
                  String town = attrs.getNamedItem("town").getNodeValue();
                  int locId = this.parseInteger(attrs, "locId");
                  int castle = this.parseInteger(attrs, "castle");
                  int bbs = this.parseInteger(attrs, "bbs");
                  MapRegionData.RegionData region = new MapRegionData.RegionData(town, locId, castle, bbs);

                  for (Node c = d.getFirstChild(); c != null; c = c.getNextSibling()) {
                     attrs = c.getAttributes();
                     if ("respawnPoint".equalsIgnoreCase(c.getNodeName())) {
                        int spawnX = this.parseInteger(attrs, "X");
                        int spawnY = this.parseInteger(attrs, "Y");
                        int spawnZ = this.parseInteger(attrs, "Z");
                        boolean other = this.parseBoolean(attrs, "isOther", false);
                        boolean chaotic = this.parseBoolean(attrs, "isChaotic", false);
                        boolean banish = this.parseBoolean(attrs, "isBanish", false);
                        if (other) {
                           if (region.otherSpawnLocs == null) {
                              region.otherSpawnLocs = new ArrayList<>();
                           }

                           region.otherSpawnLocs.add(new Location(spawnX, spawnY, spawnZ));
                        } else if (chaotic) {
                           if (region.chaoticSpawnLocs == null) {
                              region.chaoticSpawnLocs = new ArrayList<>();
                           }

                           region.chaoticSpawnLocs.add(new Location(spawnX, spawnY, spawnZ));
                        } else if (banish) {
                           if (region.banishSpawnLocs == null) {
                              region.banishSpawnLocs = new ArrayList<>();
                           }

                           region.banishSpawnLocs.add(new Location(spawnX, spawnY, spawnZ));
                        } else {
                           if (region.spawnLocs == null) {
                              region.spawnLocs = new ArrayList<>();
                           }

                           region.spawnLocs.add(new Location(spawnX, spawnY, spawnZ));
                        }
                     } else if ("map".equalsIgnoreCase(c.getNodeName())) {
                        if (region.maps == null) {
                           region.maps = new ArrayList<>();
                        }

                        region.maps.add(new int[]{this.parseInteger(attrs, "X"), this.parseInteger(attrs, "Y")});
                     } else if ("banned".equalsIgnoreCase(c.getNodeName())) {
                        if (region.bannedRaces == null) {
                           region.bannedRaces = new EnumMap<>(Race.class);
                        }

                        region.bannedRaces.put(Race.valueOf(attrs.getNamedItem("race").getNodeValue()), attrs.getNamedItem("point").getNodeValue());
                     }
                  }

                  this._regions.put(name, region);
               }
            }
         }
      }
   }

   private Location getSpawnLoc(MapRegionData.RegionData region) {
      if (region.spawnLocs == null) {
         return null;
      } else {
         return PlayerConfig.RANDOM_RESPAWN_IN_TOWN_ENABLED ? region.spawnLocs.get(Rnd.get(region.spawnLocs.size())) : region.spawnLocs.get(0);
      }
   }

   private Location getChaoticSpawnLoc(MapRegionData.RegionData region) {
      if (region.chaoticSpawnLocs != null) {
         return PlayerConfig.RANDOM_RESPAWN_IN_TOWN_ENABLED
            ? region.chaoticSpawnLocs.get(Rnd.get(region.chaoticSpawnLocs.size()))
            : region.chaoticSpawnLocs.get(0);
      } else {
         return this.getSpawnLoc(region);
      }
   }

   public MapRegionData.RegionData getMapRegion(int locX, int locY) {
      int regionX = this.getMapRegionX(locX);
      int regionY = this.getMapRegionY(locY);
      return regionX >= 0 && regionX < 32 && regionY >= 0 && regionY < 32 ? this._spatialGrid[regionX][regionY] : null;
   }

   public int getMapRegionLocId(int locX, int locY) {
      MapRegionData.RegionData region = this.getMapRegion(locX, locY);
      return region != null ? region.locId : 0;
   }

   public MapRegionData.RegionData getMapRegion(WorldObject obj) {
      return this.getMapRegion(obj.getX(), obj.getY());
   }

   public int getMapRegionLocId(WorldObject obj) {
      return this.getMapRegionLocId(obj.getX(), obj.getY());
   }

   public int getMapRegionX(int posX) {
      return (posX >> 15) + 20;
   }

   public int getMapRegionY(int posY) {
      return (posY >> 15) + 18;
   }

   public String getClosestTownName(Creature creature) {
      MapRegionData.RegionData region = this.getMapRegion(creature);
      return region == null ? "Aden Castle Town" : region.town;
   }

   public int getAreaCastle(Creature creature) {
      MapRegionData.RegionData region = this.getMapRegion(creature);
      return region == null ? 0 : region.castle;
   }

   public Location getTeleToLocation(Creature creature, TeleportWhereType teleportWhere) {
      if (creature.isPlayer()) {
         Player player = creature.asPlayer();
         if (player.getUCState() != 0) {
            return null;
         }

         Castle castle = null;
         Fort fort = null;
         ClanHall clanhall = null;
         Clan clan = player.getClan();
         if (clan != null && !player.isFlyingMounted() && !player.isFlying()) {
            if (teleportWhere == TeleportWhereType.CLANHALL) {
               clanhall = ClanHallTable.getInstance().getAbstractHallByOwner(clan);
               if (clanhall != null) {
                  ClanHallZone zone = clanhall.getZone();
                  if (zone != null && !player.isFlyingMounted()) {
                     if (player.getKarma() > 0) {
                        return zone.getChaoticSpawnLoc();
                     }

                     return zone.getSpawnLoc();
                  }
               }
            }

            if (teleportWhere == TeleportWhereType.CASTLE) {
               castle = CastleManager.getInstance().getCastleByOwner(clan);
               if (castle == null) {
                  castle = CastleManager.getInstance().getCastle(player);
                  if (castle == null || !castle.getSiege().isInProgress() || castle.getSiege().getDefenderClan(clan) == null) {
                     castle = null;
                  }
               }

               if (castle != null && castle.getResidenceId() > 0) {
                  if (player.getKarma() > 0) {
                     return castle.getResidenceZone().getChaoticSpawnLoc();
                  }

                  return castle.getResidenceZone().getSpawnLoc();
               }
            }

            if (teleportWhere == TeleportWhereType.FORTRESS) {
               fort = FortManager.getInstance().getFortByOwner(clan);
               if (fort == null) {
                  fort = FortManager.getInstance().getFort(player);
                  if (fort == null || !fort.getSiege().isInProgress() || fort.getOwnerClan() != clan) {
                     fort = null;
                  }
               }

               if (fort != null && fort.getResidenceId() > 0) {
                  if (player.getKarma() > 0) {
                     return fort.getResidenceZone().getChaoticSpawnLoc();
                  }

                  return fort.getResidenceZone().getSpawnLoc();
               }
            }

            if (teleportWhere == TeleportWhereType.SIEGEFLAG) {
               castle = CastleManager.getInstance().getCastle(player);
               fort = FortManager.getInstance().getFort(player);
               if (castle != null) {
                  if (castle.getSiege().isInProgress()) {
                     Set<Npc> flags = castle.getSiege().getFlag(clan);
                     if (flags != null && !flags.isEmpty()) {
                        return flags.stream().findAny().get().getLocation();
                     }
                  }
               } else if (fort != null && fort.getSiege().isInProgress()) {
                  Set<Npc> flags = fort.getSiege().getFlag(clan);
                  if (flags != null && !flags.isEmpty()) {
                     return flags.stream().findAny().get().getLocation();
                  }
               }
            }
         }

         if (player.getKarma() > 0) {
            return this.getNearestKarmaRespawn(player);
         }

         castle = CastleManager.getInstance().getCastle(player);
         if (castle != null
            && castle.getSiege().isInProgress()
            && (castle.getSiege().checkIsDefender(clan) || castle.getSiege().checkIsAttacker(clan))
            && SevenSigns.getInstance().getSealOwner(3) == 2) {
            return castle.getResidenceZone().getOtherSpawnLoc();
         }

         if (player.isInInstance()) {
            Instance inst = InstanceManager.getInstance().getInstance(player.getInstanceId());
            if (inst != null) {
               Location loc = inst.getExitLoc();
               if (loc != null) {
                  return loc;
               }
            }
         }

         if (FactionSystemConfig.FACTION_SYSTEM_ENABLED && FactionSystemConfig.FACTION_RESPAWN_AT_BASE) {
            if (player.isGood()) {
               return FactionSystemConfig.FACTION_GOOD_BASE_LOCATION;
            }

            if (player.isEvil()) {
               return FactionSystemConfig.FACTION_EVIL_BASE_LOCATION;
            }
         }
      }

      return this.getNearestTownRespawn(creature);
   }

   public Location getNearestKarmaRespawn(Player player) {
      try {
         RespawnZone zone = ZoneManager.getInstance().getZone(player, RespawnZone.class);
         if (zone != null) {
            return this.getRestartRegionChaoticSpawnLoc(player, zone.getRespawnPoint(player));
         }

         MapRegionData.RegionData region = this.getMapRegion(player);
         if (region != null) {
            if (region.bannedRaces != null && region.bannedRaces.containsKey(player.getRace())) {
               String alternateRegionName = region.bannedRaces.get(player.getRace());
               MapRegionData.RegionData alternateRegion = this._regions.get(alternateRegionName);
               if (alternateRegion != null) {
                  return this.getChaoticSpawnLoc(alternateRegion);
               }
            }

            return this.getChaoticSpawnLoc(region);
         }
      } catch (Exception var6) {
      }

      if (player.isFlyingMounted() && this._fallbackFlyingRegion != null) {
         return this.getChaoticSpawnLoc(this._fallbackFlyingRegion);
      } else {
         return this._defaultRespawnRegion != null ? this.getChaoticSpawnLoc(this._defaultRespawnRegion) : null;
      }
   }

   public Location getNearestTownRespawn(Creature creature) {
      try {
         RespawnZone zone = ZoneManager.getInstance().getZone(creature, RespawnZone.class);
         if (zone != null) {
            return this.getRestartRegionSpawnLoc(creature, zone.getRespawnPoint(creature.asPlayer()));
         }

         MapRegionData.RegionData region = this.getMapRegion(creature);
         if (region != null) {
            if (creature.isPlayer()) {
               Player player = creature.asPlayer();
               if (region.bannedRaces != null && region.bannedRaces.containsKey(player.getRace())) {
                  String alternateRegionName = region.bannedRaces.get(player.getRace());
                  MapRegionData.RegionData alternateRegion = this._regions.get(alternateRegionName);
                  if (alternateRegion != null) {
                     return this.getSpawnLoc(alternateRegion);
                  }
               }
            }

            return this.getSpawnLoc(region);
         }
      } catch (Exception var7) {
      }

      return this._defaultRespawnRegion != null ? this.getSpawnLoc(this._defaultRespawnRegion) : null;
   }

   public Location getRestartRegionSpawnLoc(Creature creature, String point) {
      try {
         Player player = creature.asPlayer();
         MapRegionData.RegionData region = this._regions.get(point);
         if (region == null) {
            region = this._defaultRespawnRegion;
         }

         return region.bannedRaces != null && region.bannedRaces.containsKey(player.getRace())
            ? this.getRestartRegionSpawnLoc(player, region.bannedRaces.get(player.getRace()))
            : this.getSpawnLoc(region);
      } catch (Exception var5) {
         return this._defaultRespawnRegion != null ? this.getSpawnLoc(this._defaultRespawnRegion) : null;
      }
   }

   public Location getRestartRegionChaoticSpawnLoc(Creature creature, String point) {
      try {
         MapRegionData.RegionData region = this._regions.get(point);
         if (region == null) {
            region = this._defaultRespawnRegion;
         }

         Player player = creature.asPlayer();
         return region.bannedRaces != null && region.bannedRaces.containsKey(player.getRace())
            ? this.getRestartRegionChaoticSpawnLoc(player, region.bannedRaces.get(player.getRace()))
            : this.getChaoticSpawnLoc(region);
      } catch (Exception var5) {
         return this._defaultRespawnRegion != null ? this.getChaoticSpawnLoc(this._defaultRespawnRegion) : null;
      }
   }

   public int getRestartRegionLocId(Creature creature, String point) {
      try {
         MapRegionData.RegionData region = this._regions.get(point);
         if (region == null) {
            return this._defaultRespawnRegion != null ? this._defaultRespawnRegion.locId : 0;
         } else {
            Player player = creature.asPlayer();
            return region.bannedRaces != null && region.bannedRaces.containsKey(player.getRace())
               ? this.getRestartRegionLocId(player, region.bannedRaces.get(player.getRace()))
               : region.locId;
         }
      } catch (Exception var5) {
         return this._defaultRespawnRegion != null ? this._defaultRespawnRegion.locId : 0;
      }
   }

   public int getRestartRegionLocId(Player player, String point) {
      return this.getRestartRegionLocId((Creature)player, point);
   }

   public Location getSpawnLocByRegionName(String regionName) {
      MapRegionData.RegionData region = this._regions.get(regionName);
      if (region == null) {
         return null;
      } else if (region.spawnLocs == null) {
         return null;
      } else {
         return PlayerConfig.RANDOM_RESPAWN_IN_TOWN_ENABLED ? region.spawnLocs.get(Rnd.get(region.spawnLocs.size())) : region.spawnLocs.get(0);
      }
   }

   public String getMapRegionTown(String regionName) {
      MapRegionData.RegionData region = this._regions.get(regionName);
      return region != null ? region.town : null;
   }

   public int getBBs(ILocational loc) {
      MapRegionData.RegionData region = this.getMapRegion(loc.getX(), loc.getY());
      if (region != null) {
         return region.bbs;
      } else {
         return this._defaultRespawnRegion != null ? this._defaultRespawnRegion.bbs : 0;
      }
   }

   public static MapRegionData getInstance() {
      return MapRegionData.SingletonHolder.INSTANCE;
   }

   private static class RegionData {
      final String town;
      final int locId;
      final int castle;
      final int bbs;
      List<int[]> maps;
      List<Location> spawnLocs;
      List<Location> otherSpawnLocs;
      List<Location> chaoticSpawnLocs;
      List<Location> banishSpawnLocs;
      Map<Race, String> bannedRaces;

      RegionData(String town, int locId, int castle, int bbs) {
         this.town = town;
         this.locId = locId;
         this.castle = castle;
         this.bbs = bbs;
      }
   }

   private static class SingletonHolder {
      protected static final MapRegionData INSTANCE = new MapRegionData();
   }
}
