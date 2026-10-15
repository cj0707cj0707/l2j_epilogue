package org.l2jmobius.gameserver.managers;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.l2jmobius.commons.database.DatabaseFactory;
import org.l2jmobius.gameserver.entity.WorldObject;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.clan.Clan;
import org.l2jmobius.gameserver.entity.clan.ClanMember;
import org.l2jmobius.gameserver.entity.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.mechanics.siege.Castle;

public class CastleManager {
   private static final Logger LOGGER = Logger.getLogger(CastleManager.class.getName());
   private static final List<Castle> _castles = new CopyOnWriteArrayList<>();
   private static final Map<Integer, Long> _castleSiegeDate = new ConcurrentHashMap<>();
   private static final int[] _castleCirclets = new int[]{0, 6838, 6835, 6839, 6837, 6840, 6834, 6836, 8182, 8183};

   public int findNearestCastleIndex(WorldObject obj) {
      return this.findNearestCastleIndex(obj, Long.MAX_VALUE);
   }

   public int findNearestCastleIndex(WorldObject obj, long maxDistanceValue) {
      int index = this.getCastleIndex(obj);
      if (index < 0) {
         long maxDistance = maxDistanceValue;

         for (int i = 0; i < _castles.size(); i++) {
            Castle castle = _castles.get(i);
            if (castle != null) {
               double distance = castle.getDistance(obj);
               if (maxDistance > distance) {
                  maxDistance = (long)distance;
                  index = i;
               }
            }
         }
      }

      return index;
   }

   public Castle getCastleById(int castleId) {
      for (Castle temp : _castles) {
         if (temp.getResidenceId() == castleId) {
            return temp;
         }
      }

      return null;
   }

   public Castle getCastleByOwner(Clan clan) {
      if (clan == null) {
         return null;
      } else {
         for (Castle temp : _castles) {
            if (temp.getOwnerId() == clan.getId()) {
               return temp;
            }
         }

         return null;
      }
   }

   public Castle getCastle(String name) {
      for (Castle temp : _castles) {
         if (temp.getName().equalsIgnoreCase(name.trim())) {
            return temp;
         }
      }

      return null;
   }

   public Castle getCastle(int x, int y, int z) {
      for (Castle temp : _castles) {
         if (temp.checkIfInZone(x, y, z)) {
            return temp;
         }
      }

      return null;
   }

   public Castle getCastle(WorldObject activeObject) {
      return this.getCastle(activeObject.getX(), activeObject.getY(), activeObject.getZ());
   }

   public int getCastleIndex(int castleId) {
      for (int i = 0; i < _castles.size(); i++) {
         Castle castle = _castles.get(i);
         if (castle != null && castle.getResidenceId() == castleId) {
            return i;
         }
      }

      return -1;
   }

   public int getCastleIndex(WorldObject activeObject) {
      return this.getCastleIndex(activeObject.getX(), activeObject.getY(), activeObject.getZ());
   }

   public int getCastleIndex(int x, int y, int z) {
      for (int i = 0; i < _castles.size(); i++) {
         Castle castle = _castles.get(i);
         if (castle != null && castle.checkIfInZone(x, y, z)) {
            return i;
         }
      }

      return -1;
   }

   public List<Castle> getCastles() {
      return _castles;
   }

   public boolean hasOwnedCastle() {
      boolean hasOwnedCastle = false;

      for (Castle castle : _castles) {
         if (castle.getOwnerId() > 0) {
            hasOwnedCastle = true;
            break;
         }
      }

      return hasOwnedCastle;
   }

   public void validateTaxes(int sealStrifeOwner) {
      int maxTax = switch (sealStrifeOwner) {
         case 1 -> 5;
         case 2 -> 25;
         default -> 15;
      };

      for (Castle castle : _castles) {
         if (castle.getTaxPercent() > maxTax) {
            castle.setTaxPercent(maxTax);
         }
      }
   }

   public int getCirclet() {
      return this.getCircletByCastleId(1);
   }

   public int getCircletByCastleId(int castleId) {
      return castleId > 0 && castleId < 10 ? _castleCirclets[castleId] : 0;
   }

   public void removeCirclet(Clan clan, int castleId) {
      for (ClanMember member : clan.getMembers()) {
         this.removeCirclet(member, castleId);
      }
   }

   public void removeCirclet(ClanMember member, int castleId) {
      if (member != null) {
         Player player = member.getPlayer();
         int circletId = this.getCircletByCastleId(castleId);
         if (circletId != 0) {
            if (player != null) {
               try {
                  Item circlet = player.getInventory().getItemByItemId(circletId);
                  if (circlet != null) {
                     if (circlet.isEquipped()) {
                        player.getInventory().unEquipItemInSlot(circlet.getLocationSlot());
                     }

                     player.destroyItemByItemId(ItemProcessType.DESTROY, circletId, 1L, player, true);
                  }

                  return;
               } catch (NullPointerException var14) {
               }
            }

            try (
               Connection con = DatabaseFactory.getConnection();
               PreparedStatement ps = con.prepareStatement("DELETE FROM items WHERE owner_id = ? and item_id = ?");
            ) {
               ps.setInt(1, member.getObjectId());
               ps.setInt(2, circletId);
               ps.execute();
            } catch (Exception var13) {
               LOGGER.log(
                  Level.WARNING, "Failed to remove castle circlets offline for player " + member.getName() + ": " + var13.getMessage(), (Throwable)var13
               );
            }
         }
      }
   }

   public void loadInstances() {
      try (
         Connection con = DatabaseFactory.getConnection();
         Statement s = con.createStatement();
         ResultSet rs = s.executeQuery("SELECT id FROM castle ORDER BY id");
      ) {
         while (rs.next()) {
            _castles.add(new Castle(rs.getInt("id")));
         }

         LOGGER.info(this.getClass().getSimpleName() + ": Loaded " + _castles.size() + " castles");
      } catch (Exception var12) {
         LOGGER.log(Level.WARNING, "Exception: loadCastleData(): " + var12.getMessage(), (Throwable)var12);
      }
   }

   public void activateInstances() {
      for (Castle castle : _castles) {
         castle.activateInstance();
      }
   }

   public void registerSiegeDate(int castleId, long siegeDate) {
      _castleSiegeDate.put(castleId, siegeDate);
   }

   public int getSiegeDates(long siegeDate) {
      int count = 0;

      for (long date : _castleSiegeDate.values()) {
         if (Math.abs(date - siegeDate) < 1000L) {
            count++;
         }
      }

      return count;
   }

   public static CastleManager getInstance() {
      return CastleManager.SingletonHolder.INSTANCE;
   }

   private static class SingletonHolder {
      protected static final CastleManager INSTANCE = new CastleManager();
   }
}
