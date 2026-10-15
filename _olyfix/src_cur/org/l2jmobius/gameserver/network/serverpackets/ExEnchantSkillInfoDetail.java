package org.l2jmobius.gameserver.network.serverpackets;

import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.config.PlayerConfig;
import org.l2jmobius.gameserver.data.xml.EnchantSkillGroupsData;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.mechanics.skill.holders.EnchantSkillGroup;
import org.l2jmobius.gameserver.mechanics.skill.holders.EnchantSkillLearn;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;

public class ExEnchantSkillInfoDetail extends ServerPacket {
   private static final int TYPE_NORMAL_ENCHANT = 0;
   private static final int TYPE_SAFE_ENCHANT = 1;
   private static final int TYPE_UNTRAIN_ENCHANT = 2;
   private static final int TYPE_CHANGE_ENCHANT = 3;
   private int bookId = 0;
   private boolean reqCount = false;
   private int multi = 1;
   private final int _type;
   private final int _skillId;
   private final int _skillLevel;
   private final int _chance;
   private int _sp;
   private final int _adenacount;

   public ExEnchantSkillInfoDetail(int type, int skillId, int skillLevel, Player ply) {
      EnchantSkillLearn enchantLearn = EnchantSkillGroupsData.getInstance().getSkillEnchantmentBySkillId(skillId);
      EnchantSkillGroup.EnchantSkillHolder esd = null;
      if (enchantLearn != null) {
         if (skillLevel > 100) {
            esd = enchantLearn.getEnchantSkillHolder(skillLevel);
         } else {
            esd = enchantLearn.getFirstRouteGroup().getEnchantGroupDetails().get(0);
         }
      }

      if (esd == null) {
         throw new IllegalArgumentException("Skill " + skillId + " does not have enchant data for level " + skillLevel);
      } else {
         if (type == 0) {
            this.multi = EnchantSkillGroupsData.NORMAL_ENCHANT_COST_MULTIPLIER;
         } else if (type == 1) {
            this.multi = EnchantSkillGroupsData.SAFE_ENCHANT_COST_MULTIPLIER;
         }

         this._chance = esd.getRate(ply);
         this._sp = esd.getSpCost();
         if (type == 2) {
            this._sp = (int)(0.8 * this._sp);
         }

         this._adenacount = esd.getAdenaCost() * this.multi;
         this._type = type;
         this._skillId = skillId;
         this._skillLevel = skillLevel;
         switch (type) {
            case 0:
               this.bookId = 6622;
               this.reqCount = this._skillLevel % 100 < 2;
               break;
            case 1:
               this.bookId = 9627;
               this.reqCount = true;
               break;
            case 2:
               this.bookId = 9625;
               this.reqCount = true;
               break;
            case 3:
               this.bookId = 9626;
               this.reqCount = true;
               break;
            default:
               return;
         }

         if (type != 1 && !PlayerConfig.ES_SP_BOOK_NEEDED) {
            this.reqCount = false;
         }
      }
   }

   @Override
   public void writeImpl(GameClient client, WriteBuffer buffer) {
      ServerPackets.EX_ENCHANT_SKILL_INFO_DETAIL.writeId(this, buffer);
      buffer.writeInt(this._type);
      buffer.writeInt(this._skillId);
      buffer.writeInt(this._skillLevel);
      buffer.writeInt(this._sp * this.multi);
      buffer.writeInt(this._chance);
      buffer.writeInt(2);
      buffer.writeInt(57);
      buffer.writeInt(this._adenacount);
      buffer.writeInt(this.bookId);
      buffer.writeInt(this.reqCount);
   }
}
