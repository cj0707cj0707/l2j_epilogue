package org.l2jmobius.gameserver.mechanics.skill.holders;

import java.util.Set;
import java.util.TreeMap;
import org.l2jmobius.gameserver.data.xml.EnchantSkillGroupsData;

public class EnchantSkillLearn {
   private final int _id;
   private final int _baseLevel;
   private final TreeMap<Integer, Integer> _enchantRoutes = new TreeMap<>();

   public EnchantSkillLearn(int id, int baseLevel) {
      this._id = id;
      this._baseLevel = baseLevel;
   }

   public void addNewEnchantRoute(int route, int group) {
      this._enchantRoutes.put(route, group);
   }

   public int getId() {
      return this._id;
   }

   public int getBaseLevel() {
      return this._baseLevel;
   }

   public static int getEnchantRoute(int level) {
      return level / 100;
   }

   public static int getEnchantIndex(int level) {
      return level % 100 - 1;
   }

   public static int getEnchantType(int level) {
      return (level - 1) / 100 - 1;
   }

   public EnchantSkillGroup getFirstRouteGroup() {
      return EnchantSkillGroupsData.getInstance().getEnchantSkillGroupById(this._enchantRoutes.firstEntry().getValue());
   }

   public Set<Integer> getAllRoutes() {
      return this._enchantRoutes.keySet();
   }

   public int getMinSkillLevel(int level) {
      return level % 100 == 1 ? this._baseLevel : level - 1;
   }

   public boolean isMaxEnchant(int level) {
      int enchantType = getEnchantRoute(level);
      if (enchantType >= 1 && this._enchantRoutes.containsKey(enchantType)) {
         int index = getEnchantIndex(level);
         return index + 1
            >= EnchantSkillGroupsData.getInstance().getEnchantSkillGroupById(this._enchantRoutes.get(enchantType)).getEnchantGroupDetails().size();
      } else {
         return false;
      }
   }

   public EnchantSkillGroup.EnchantSkillHolder getEnchantSkillHolder(int level) {
      int enchantType = getEnchantRoute(level);
      if (enchantType >= 1 && this._enchantRoutes.containsKey(enchantType)) {
         int index = getEnchantIndex(level);
         EnchantSkillGroup group = EnchantSkillGroupsData.getInstance().getEnchantSkillGroupById(this._enchantRoutes.get(enchantType));
         if (index < 0) {
            return group.getEnchantGroupDetails().get(0);
         } else {
            return index >= group.getEnchantGroupDetails().size()
               ? group.getEnchantGroupDetails()
                  .get(EnchantSkillGroupsData.getInstance().getEnchantSkillGroupById(this._enchantRoutes.get(enchantType)).getEnchantGroupDetails().size() - 1)
               : group.getEnchantGroupDetails().get(index);
         }
      } else {
         return null;
      }
   }
}
