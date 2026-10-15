package org.l2jmobius.gameserver.managers;

import org.l2jmobius.gameserver.data.xml.MapRegionData;
import org.l2jmobius.gameserver.entity.zone.ZoneType;
import org.l2jmobius.gameserver.entity.zone.type.TownZone;
import org.l2jmobius.gameserver.mechanics.siege.Castle;

public class TownManager {
   public static int getTownCastle(int townId) {
      switch (townId) {
         case 912:
            return 1;
         case 916:
            return 2;
         case 918:
            return 3;
         case 922:
            return 4;
         case 924:
            return 5;
         case 926:
            return 6;
         case 1537:
            return 8;
         case 1538:
            return 7;
         case 1714:
            return 9;
         default:
            return 0;
      }
   }

   public static boolean townHasCastleInSiege(int townId) {
      int castleIndex = getTownCastle(townId);
      if (castleIndex > 0) {
         Castle castle = CastleManager.getInstance().getCastles().get(CastleManager.getInstance().getCastleIndex(castleIndex));
         if (castle != null) {
            return castle.getSiege().isInProgress();
         }
      }

      return false;
   }

   public static boolean townHasCastleInSiege(int x, int y) {
      return townHasCastleInSiege(MapRegionData.getInstance().getMapRegionLocId(x, y));
   }

   public static TownZone getTown(int townId) {
      for (TownZone temp : ZoneManager.getInstance().getAllZones(TownZone.class)) {
         if (temp.getTownId() == townId) {
            return temp;
         }
      }

      return null;
   }

   public static TownZone getTown(int x, int y, int z) {
      for (ZoneType temp : ZoneManager.getInstance().getZones(x, y, z)) {
         if (temp instanceof TownZone) {
            return (TownZone)temp;
         }
      }

      return null;
   }
}
