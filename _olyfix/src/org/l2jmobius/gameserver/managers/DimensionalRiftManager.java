package org.l2jmobius.gameserver.managers;

import java.io.File;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.parsers.DocumentBuilderFactory;
import org.l2jmobius.commons.database.DatabaseFactory;
import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.config.GeneralConfig;
import org.l2jmobius.gameserver.config.ServerConfig;
import org.l2jmobius.gameserver.data.SpawnTable;
import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.groups.Party;
import org.l2jmobius.gameserver.entity.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.entity.spawns.Spawn;
import org.l2jmobius.gameserver.mechanics.sevensigns.DimensionalRift;
import org.l2jmobius.gameserver.mechanics.sevensigns.DimensionalRiftRoom;
import org.l2jmobius.gameserver.network.serverpackets.NpcHtmlMessage;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

public class DimensionalRiftManager {
   private static final Logger LOGGER = Logger.getLogger(DimensionalRiftManager.class.getName());
   private final Map<Byte, Map<Byte, DimensionalRiftRoom>> _rooms = new HashMap<>(7);
   private static final int DIMENSIONAL_FRAGMENT_ITEM_ID = 7079;

   public static DimensionalRiftManager getInstance() {
      return DimensionalRiftManager.SingletonHolder.INSTANCE;
   }

   protected DimensionalRiftManager() {
      this.loadRooms();
      this.loadSpawns();
   }

   public DimensionalRiftRoom getRoom(byte type, byte room) {
      return this._rooms.get(type) == null ? null : this._rooms.get(type).get(room);
   }

   private void loadRooms() {
      try (
         Connection con = DatabaseFactory.getConnection();
         Statement s = con.createStatement();
         ResultSet rs = s.executeQuery("SELECT * FROM dimensional_rift");
      ) {
         while (rs.next()) {
            byte type = rs.getByte("type");
            byte room_id = rs.getByte("room_id");
            int xMin = rs.getInt("xMin");
            int xMax = rs.getInt("xMax");
            int yMin = rs.getInt("yMin");
            int yMax = rs.getInt("yMax");
            int z1 = rs.getInt("zMin");
            int z2 = rs.getInt("zMax");
            int xT = rs.getInt("xT");
            int yT = rs.getInt("yT");
            int zT = rs.getInt("zT");
            boolean isBossRoom = rs.getByte("boss") > 0;
            this._rooms.computeIfAbsent(type, k -> new HashMap<>(9));
            this._rooms.get(type).put(room_id, new DimensionalRiftRoom(type, room_id, xMin, xMax, yMin, yMax, z1, z2, xT, yT, zT, isBossRoom));
         }
      } catch (Exception var22) {
         LOGGER.log(Level.WARNING, "Can't load Dimension Rift zones. " + var22.getMessage(), (Throwable)var22);
      }

      int typeSize = this._rooms.keySet().size();
      int roomSize = 0;

      for (Map<Byte, DimensionalRiftRoom> room : this._rooms.values()) {
         roomSize += room.keySet().size();
      }

      LOGGER.info(this.getClass().getSimpleName() + ": Loaded " + typeSize + " room types with " + roomSize + " rooms.");
   }

   public void loadSpawns() {
      int countGood = 0;
      int countBad = 0;

      try {
         DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
         factory.setValidating(false);
         factory.setIgnoringComments(true);
         File file = new File(ServerConfig.DATAPACK_ROOT, "data/DimensionalRift.xml");
         if (!file.exists()) {
            LOGGER.log(Level.WARNING, this.getClass().getSimpleName() + ": Couldn't find data/" + file.getName());
            return;
         }

         Document document = factory.newDocumentBuilder().parse(file);

         for (Node rift = document.getFirstChild(); rift != null; rift = rift.getNextSibling()) {
            if ("rift".equalsIgnoreCase(rift.getNodeName())) {
               for (Node area = rift.getFirstChild(); area != null; area = area.getNextSibling()) {
                  if ("area".equalsIgnoreCase(area.getNodeName())) {
                     NamedNodeMap attrs = area.getAttributes();
                     byte type = Byte.parseByte(attrs.getNamedItem("type").getNodeValue());

                     for (Node room = area.getFirstChild(); room != null; room = room.getNextSibling()) {
                        if ("room".equalsIgnoreCase(room.getNodeName())) {
                           attrs = room.getAttributes();
                           byte roomId = Byte.parseByte(attrs.getNamedItem("id").getNodeValue());

                           for (Node spawn = room.getFirstChild(); spawn != null; spawn = spawn.getNextSibling()) {
                              if ("spawn".equalsIgnoreCase(spawn.getNodeName())) {
                                 attrs = spawn.getAttributes();
                                 int mobId = Integer.parseInt(attrs.getNamedItem("mobId").getNodeValue());
                                 int delay = Integer.parseInt(attrs.getNamedItem("delay").getNodeValue());
                                 int count = Integer.parseInt(attrs.getNamedItem("count").getNodeValue());
                                 if (!this._rooms.containsKey(type)) {
                                    LOGGER.warning("Type " + type + " not found!");
                                 } else if (!this._rooms.get(type).containsKey(roomId)) {
                                    LOGGER.warning("Room " + roomId + " in Type " + type + " not found!");
                                 }

                                 for (int i = 0; i < count; i++) {
                                    DimensionalRiftRoom riftRoom = this._rooms.get(type).get(roomId);
                                    int x = riftRoom.getRandomX();
                                    int y = riftRoom.getRandomY();
                                    int z = riftRoom.getTeleportCoorinates().getZ();
                                    if (this._rooms.containsKey(type) && this._rooms.get(type).containsKey(roomId)) {
                                       Spawn spawnDat = new Spawn(mobId);
                                       spawnDat.setAmount(1);
                                       spawnDat.setXYZ(x, y, z);
                                       spawnDat.setHeading(-1);
                                       spawnDat.setRespawnDelay(delay);
                                       SpawnTable.getInstance().addSpawn(spawnDat);
                                       this._rooms.get(type).get(roomId).getSpawns().add(spawnDat);
                                       countGood++;
                                    } else {
                                       countBad++;
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
      } catch (Exception var22) {
         LOGGER.log(Level.WARNING, "Error on loading dimensional rift spawns: " + var22.getMessage(), (Throwable)var22);
      }

      LOGGER.info(this.getClass().getSimpleName() + ": Loaded " + countGood + " dimensional rift spawns, " + countBad + " errors.");
   }

   public void reload() {
      for (Map<Byte, DimensionalRiftRoom> rooms : this._rooms.values()) {
         for (DimensionalRiftRoom room : rooms.values()) {
            room.getSpawns().clear();
         }

         rooms.clear();
      }

      this._rooms.clear();
      this.loadRooms();
      this.loadSpawns();
   }

   public boolean checkIfInRiftZone(int x, int y, int z, boolean ignorePeaceZone) {
      return ignorePeaceZone
         ? this._rooms.get((byte)0).get((byte)1).checkIfInZone(x, y, z)
         : this._rooms.get((byte)0).get((byte)1).checkIfInZone(x, y, z) && !this._rooms.get((byte)0).get((byte)0).checkIfInZone(x, y, z);
   }

   public boolean checkIfInPeaceZone(int x, int y, int z) {
      return this._rooms.get((byte)0).get((byte)0).checkIfInZone(x, y, z);
   }

   public void teleportToWaitingRoom(Player player) {
      player.teleToLocation(this.getRoom((byte)0, (byte)0).getTeleportCoorinates());
   }

   public synchronized void start(Player player, byte type, Npc npc) {
      boolean canPass = true;
      if (!player.isInParty()) {
         this.showHtmlFile(player, "data/html/seven_signs/rift/NoParty.htm", npc);
      } else {
         Party party = player.getParty();
         if (party.getLeaderObjectId() != player.getObjectId()) {
            this.showHtmlFile(player, "data/html/seven_signs/rift/NotPartyLeader.htm", npc);
         } else if (party.isInDimensionalRift()) {
            this.handleCheat(player, npc);
         } else if (party.getMemberCount() < GeneralConfig.RIFT_MIN_PARTY_SIZE) {
            NpcHtmlMessage html = new NpcHtmlMessage(npc.getObjectId());
            html.setFile(player, "data/html/seven_signs/rift/SmallParty.htm");
            html.replace("%npc_name%", npc.getName());
            html.replace("%count%", Integer.toString(GeneralConfig.RIFT_MIN_PARTY_SIZE));
            player.sendPacket(html);
         } else if (!this.isAllowedEnter(type)) {
            player.sendMessage("Rift is full. Try later.");
         } else {
            for (Player p : party.getMembers()) {
               if (!this.checkIfInPeaceZone(p.getX(), p.getY(), p.getZ())) {
                  canPass = false;
                  break;
               }
            }

            if (!canPass) {
               this.showHtmlFile(player, "data/html/seven_signs/rift/NotInWaitingRoom.htm", npc);
            } else {
               int count = this.getNeededItems(type);

               for (Player px : party.getMembers()) {
                  Item i = px.getInventory().getItemByItemId(7079);
                  if (i == null) {
                     canPass = false;
                     break;
                  }

                  if (i.getCount() > 0L) {
                     if (i.getCount() >= this.getNeededItems(type)) {
                        continue;
                     }

                     canPass = false;
                     break;
                  }

                  canPass = false;
                  break;
               }

               if (!canPass) {
                  NpcHtmlMessage html = new NpcHtmlMessage(npc.getObjectId());
                  html.setFile(player, "data/html/seven_signs/rift/NoFragments.htm");
                  html.replace("%npc_name%", npc.getName());
                  html.replace("%count%", Integer.toString(count));
                  player.sendPacket(html);
               } else {
                  for (Player px : party.getMembers()) {
                     Item ix = px.getInventory().getItemByItemId(7079);
                     if (!px.destroyItem(ItemProcessType.FEE, ix, count, null, false)) {
                        NpcHtmlMessage html = new NpcHtmlMessage(npc.getObjectId());
                        html.setFile(player, "data/html/seven_signs/rift/NoFragments.htm");
                        html.replace("%npc_name%", npc.getName());
                        html.replace("%count%", Integer.toString(count));
                        player.sendPacket(html);
                        return;
                     }
                  }

                  byte room;
                  do {
                     List<Byte> emptyRooms = this.getFreeRooms(type);
                     room = emptyRooms.get(Rnd.get(1, emptyRooms.size()) - 1);
                  } while (this._rooms.get(type).get(room).isPartyInside());

                  new DimensionalRift(party, type, room);
               }
            }
         }
      }
   }

   public void killRift(DimensionalRift d) {
      if (d.getTeleportTimerTask() != null) {
         d.getTeleportTimerTask().cancel();
      }

      d.setTeleportTimerTask(null);
      if (d.getTeleportTimer() != null) {
         d.getTeleportTimer().cancel();
      }

      d.setTeleportTimer(null);
      if (d.getSpawnTimerTask() != null) {
         d.getSpawnTimerTask().cancel();
      }

      d.setSpawnTimerTask(null);
      if (d.getSpawnTimer() != null) {
         d.getSpawnTimer().cancel();
      }

      d.setSpawnTimer(null);
   }

   private int getNeededItems(byte type) {
      switch (type) {
         case 1:
            return GeneralConfig.RIFT_ENTER_COST_RECRUIT;
         case 2:
            return GeneralConfig.RIFT_ENTER_COST_SOLDIER;
         case 3:
            return GeneralConfig.RIFT_ENTER_COST_OFFICER;
         case 4:
            return GeneralConfig.RIFT_ENTER_COST_CAPTAIN;
         case 5:
            return GeneralConfig.RIFT_ENTER_COST_COMMANDER;
         case 6:
            return GeneralConfig.RIFT_ENTER_COST_HERO;
         default:
            throw new IndexOutOfBoundsException();
      }
   }

   public void showHtmlFile(Player player, String file, Npc npc) {
      NpcHtmlMessage html = new NpcHtmlMessage(npc.getObjectId());
      html.setFile(player, file);
      html.replace("%npc_name%", npc.getName());
      player.sendPacket(html);
   }

   public void handleCheat(Player player, Npc npc) {
      this.showHtmlFile(player, "data/html/seven_signs/rift/Cheater.htm", npc);
      if (!player.isGM()) {
         LOGGER.warning(player + " was cheating in dimension rift area!");
         PunishmentManager.handleIllegalPlayerAction(
            player, "Warning!! Character " + player.getName() + " tried to cheat in dimensional rift.", GeneralConfig.DEFAULT_PUNISH
         );
      }
   }

   public boolean isAllowedEnter(byte type) {
      int count = 0;

      for (DimensionalRiftRoom room : this._rooms.get(type).values()) {
         if (room.isPartyInside()) {
            count++;
         }
      }

      return count < this._rooms.get(type).size() - 1;
   }

   public List<Byte> getFreeRooms(byte type) {
      List<Byte> list = new ArrayList<>();

      for (DimensionalRiftRoom room : this._rooms.get(type).values()) {
         if (!room.isPartyInside()) {
            list.add(room.getRoom());
         }
      }

      return list;
   }

   private static class SingletonHolder {
      protected static final DimensionalRiftManager INSTANCE = new DimensionalRiftManager();
   }
}
