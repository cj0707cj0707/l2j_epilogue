package org.l2jmobius.gameserver.entity.actor.enums.player;

import org.l2jmobius.gameserver.data.enums.CategoryType;
import org.l2jmobius.gameserver.data.xml.CategoryData;

public enum MountType {
   NONE,
   STRIDER,
   WYVERN,
   WOLF;

   public static MountType findByNpcId(int npcId) {
      if (CategoryData.getInstance().isInCategory(CategoryType.STRIDER, npcId)) {
         return STRIDER;
      } else if (CategoryData.getInstance().isInCategory(CategoryType.WYVERN_GROUP, npcId)) {
         return WYVERN;
      } else {
         return CategoryData.getInstance().isInCategory(CategoryType.WOLF_GROUP, npcId) ? WOLF : NONE;
      }
   }
}
