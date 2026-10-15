package org.l2jmobius.gameserver.entity.zone;

public enum ZoneId {
   PVP,
   PEACE,
   SIEGE,
   MOTHER_TREE,
   CLAN_HALL,
   LANDING,
   NO_LANDING,
   WATER,
   JAIL,
   MONSTER_TRACK,
   CASTLE,
   SWAMP,
   NO_SUMMON_FRIEND,
   FORT,
   NO_STORE,
   NO_PVP,
   TOWN,
   SCRIPT,
   HQ,
   DANGER_AREA,
   ALTERED,
   NO_BOOKMARK,
   NO_ITEM_DROP,
   NO_RESTART;

   public static int getZoneCount() {
      return values().length;
   }
}
