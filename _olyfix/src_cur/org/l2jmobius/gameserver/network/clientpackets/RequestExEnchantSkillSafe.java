package org.l2jmobius.gameserver.network.clientpackets;

import java.util.logging.Level;
import java.util.logging.Logger;
import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.config.GeneralConfig;
import org.l2jmobius.gameserver.data.xml.EnchantSkillGroupsData;
import org.l2jmobius.gameserver.data.xml.SkillData;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.mechanics.skill.holders.EnchantSkillGroup;
import org.l2jmobius.gameserver.mechanics.skill.holders.EnchantSkillLearn;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.ExEnchantSkillInfo;
import org.l2jmobius.gameserver.network.serverpackets.ExEnchantSkillInfoDetail;
import org.l2jmobius.gameserver.network.serverpackets.ExEnchantSkillResult;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;

public class RequestExEnchantSkillSafe extends ClientPacket {
   private static final Logger LOGGER_ENCHANT = Logger.getLogger("enchant.skills");
   private int _skillId;
   private int _skillLevel;

   @Override
   protected void readImpl() {
      this._skillId = this.readInt();
      this._skillLevel = this.readInt();
   }

   @Override
   protected void runImpl() {
      if (this._skillId > 0 && this._skillLevel > 0) {
         Player player = this.getPlayer();
         if (player != null) {
            if (player.getPlayerClass().level() < 3) {
               player.sendPacket(
                  SystemMessageId.YOU_CANNOT_USE_THE_SKILL_ENHANCING_FUNCTION_IN_THIS_CLASS_YOU_CAN_USE_CORRESPONDING_FUNCTION_WHEN_COMPLETING_THE_THIRD_CLASS_CHANGE
               );
            } else if (player.getLevel() < 76) {
               player.sendPacket(
                  SystemMessageId.YOU_CANNOT_USE_THE_SKILL_ENHANCING_FUNCTION_ON_THIS_LEVEL_YOU_CAN_USE_THE_CORRESPONDING_FUNCTION_ON_LEVELS_HIGHER_THAN_76LV
               );
            } else if (!player.isAllowedToEnchantSkills()) {
               player.sendPacket(
                  SystemMessageId.YOU_CANNOT_USE_THE_SKILL_ENHANCING_FUNCTION_IN_THIS_CLASS_YOU_CAN_USE_THE_SKILL_ENHANCING_FUNCTION_UNDER_OFF_BATTLE_STATUS_AND_CANNOT_USE_THE_FUNCTION_WHILE_TRANSFORMING_BATTLING_AND_ON_BOARD
               );
            } else {
               Skill skill = SkillData.getInstance().getSkill(this._skillId, this._skillLevel);
               if (skill != null) {
                  int costMultiplier = EnchantSkillGroupsData.SAFE_ENCHANT_COST_MULTIPLIER;
                  int reqItemId = 9627;
                  EnchantSkillLearn s = EnchantSkillGroupsData.getInstance().getSkillEnchantmentBySkillId(this._skillId);
                  if (s != null) {
                     int beforeEnchantSkillLevel = player.getSkillLevel(this._skillId);
                     if (beforeEnchantSkillLevel == s.getMinSkillLevel(this._skillLevel)) {
                        EnchantSkillGroup.EnchantSkillHolder esd = s.getEnchantSkillHolder(this._skillLevel);
                        int requiredSp = esd.getSpCost() * costMultiplier;
                        int requireditems = esd.getAdenaCost() * costMultiplier;
                        int rate = esd.getRate(player);
                        if (player.getSp() >= requiredSp) {
                           Item spb = player.getInventory().getItemByItemId(9627);
                           if (spb == null) {
                              player.sendPacket(SystemMessageId.YOU_DO_NOT_HAVE_ALL_OF_THE_ITEMS_NEEDED_TO_ENCHANT_THAT_SKILL);
                              return;
                           }

                           if (player.getInventory().getAdena() < requireditems) {
                              player.sendPacket(SystemMessageId.YOU_DO_NOT_HAVE_ALL_OF_THE_ITEMS_NEEDED_TO_ENCHANT_THAT_SKILL);
                              return;
                           }

                           boolean check = player.getStat().removeExpAndSp(0L, requiredSp, false);
                           check &= player.destroyItem(ItemProcessType.NONE, spb.getObjectId(), 1L, player, true);
                           check &= player.destroyItemByItemId(null, 57, requireditems, player, true);
                           if (!check) {
                              player.sendPacket(SystemMessageId.YOU_DO_NOT_HAVE_ALL_OF_THE_ITEMS_NEEDED_TO_ENCHANT_THAT_SKILL);
                              return;
                           }

                           if (Rnd.get(100) <= rate) {
                              if (GeneralConfig.LOG_SKILL_ENCHANTS) {
                                 LOGGER_ENCHANT.log(
                                    Level.INFO,
                                    "Safe Success, Character:"
                                       + player.getName()
                                       + " ["
                                       + player.getObjectId()
                                       + "] Account:"
                                       + player.getAccountName()
                                       + " IP:"
                                       + player.getIPAddress()
                                       + ", Skill:"
                                       + skill
                                       + ", SPB:"
                                       + spb
                                       + ", Rate:"
                                       + rate
                                 );
                              }

                              player.addSkill(skill, true);
                              player.sendPacket(ExEnchantSkillResult.valueOf(true));
                              SystemMessage sm = new SystemMessage(SystemMessageId.SKILL_ENCHANT_WAS_SUCCESSFUL_S1_HAS_BEEN_ENCHANTED);
                              sm.addSkillName(this._skillId);
                              player.sendPacket(sm);
                           } else {
                              if (GeneralConfig.LOG_SKILL_ENCHANTS) {
                                 LOGGER_ENCHANT.log(
                                    Level.INFO,
                                    "Safe Failed, Character:"
                                       + player.getName()
                                       + " ["
                                       + player.getObjectId()
                                       + "] Account:"
                                       + player.getAccountName()
                                       + " IP:"
                                       + player.getIPAddress()
                                       + ", Skill:"
                                       + skill
                                       + ", SPB:"
                                       + spb
                                       + ", Rate:"
                                       + rate
                                 );
                              }

                              SystemMessage sm = new SystemMessage(SystemMessageId.SKILL_ENCHANT_FAILED_CURRENT_LEVEL_OF_ENCHANT_SKILL_S1_WILL_REMAIN_UNCHANGED);
                              sm.addSkillName(this._skillId);
                              player.sendPacket(sm);
                              player.sendPacket(ExEnchantSkillResult.valueOf(false));
                           }

                           player.updateUserInfo();
                           player.sendSkillList();
                           int afterEnchantSkillLevel = player.getSkillLevel(this._skillId);
                           player.sendPacket(new ExEnchantSkillInfo(this._skillId, afterEnchantSkillLevel));
                           player.sendPacket(new ExEnchantSkillInfoDetail(1, this._skillId, afterEnchantSkillLevel + 1, player));
                           player.updateShortcuts(this._skillId, afterEnchantSkillLevel);
                        } else {
                           player.sendPacket(SystemMessageId.YOU_DO_NOT_HAVE_ENOUGH_SP_TO_ENCHANT_THAT_SKILL);
                        }
                     }
                  }
               }
            }
         }
      }
   }
}
