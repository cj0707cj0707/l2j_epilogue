package org.l2jmobius.gameserver.network.serverpackets;

import java.util.ArrayList;
import java.util.List;
import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.data.xml.EnchantSkillGroupsData;
import org.l2jmobius.gameserver.mechanics.skill.holders.EnchantSkillLearn;
import org.l2jmobius.gameserver.mechanics.skill.holders.EnchantSkillGroup.EnchantSkillHolder;

public class ExEnchantSkillInfo extends ServerPacket {
   private final List<Integer> _routes = new ArrayList<>();
   private final int _id;
   private final int _level;
   private boolean _maxEnchanted = false;

   public ExEnchantSkillInfo(int id, int level) {
      this._id = id;
      this._level = level;
      EnchantSkillLearn enchantLearn = EnchantSkillGroupsData.getInstance().getSkillEnchantmentBySkillId(this._id);
      if (enchantLearn != null) {
         if (this._level > 100) {
            this._maxEnchanted = enchantLearn.isMaxEnchant(this._level);
            EnchantSkillHolder esd = enchantLearn.getEnchantSkillHolder(this._level);
            if (esd != null) {
               this._routes.add(this._level);
            }

            int skillLevel = this._level % 100;

            for (int route : enchantLearn.getAllRoutes()) {
               if (route * 100 + skillLevel != this._level) {
                  this._routes.add(route * 100 + skillLevel);
               }
            }
         } else {
            for (int routex : enchantLearn.getAllRoutes()) {
               this._routes.add(routex * 100 + 1);
            }
         }
      }
   }

   @Override
   public void writeImpl(org.l2jmobius.gameserver.network.GameClient client, WriteBuffer buffer) {
      org.l2jmobius.gameserver.network.ServerPackets.EX_ENCHANT_SKILL_INFO.writeId(this, buffer);
      buffer.writeInt(this._id);
      buffer.writeInt(this._level);
      buffer.writeInt(!this._maxEnchanted);
      buffer.writeInt(this._level > 100);
      buffer.writeInt(this._routes.size());

      for (int level : this._routes) {
         buffer.writeInt(level);
      }
   }
}
