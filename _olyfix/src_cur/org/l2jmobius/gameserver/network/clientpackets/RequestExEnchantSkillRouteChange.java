package org.l2jmobius.gameserver.network.clientpackets;

import java.util.logging.Level;
import java.util.logging.Logger;
import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.config.GeneralConfig;
import org.l2jmobius.gameserver.config.PlayerConfig;
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

public class RequestExEnchantSkillRouteChange extends ClientPacket {
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
                  int reqItemId = 9626;
                  EnchantSkillLearn s = EnchantSkillGroupsData.getInstance().getSkillEnchantmentBySkillId(this._skillId);
                  if (s != null) {
                     int beforeEnchantSkillLevel = player.getSkillLevel(this._skillId);
                     if (beforeEnchantSkillLevel > 100) {
                        int currentEnchantLevel = beforeEnchantSkillLevel % 100;
                        if (currentEnchantLevel == this._skillLevel % 100) {
                           EnchantSkillGroup.EnchantSkillHolder esd = s.getEnchantSkillHolder(this._skillLevel);
                           int requiredSp = esd.getSpCost();
                           int requireditems = esd.getAdenaCost();
                           if (player.getSp() >= requiredSp) {
                              Item spb = player.getInventory().getItemByItemId(9626);
                              if (PlayerConfig.ES_SP_BOOK_NEEDED && spb == null) {
                                 player.sendPacket(SystemMessageId.YOU_DO_NOT_HAVE_ALL_OF_THE_ITEMS_NEEDED_TO_ENCHANT_SKILL_ROUTE_CHANGE);
                                 return;
                              }

                              if (player.getInventory().getAdena() < requireditems) {
                                 player.sendPacket(SystemMessageId.YOU_DO_NOT_HAVE_ALL_OF_THE_ITEMS_NEEDED_TO_ENCHANT_THAT_SKILL);
                                 return;
                              }

                              boolean check = player.getStat().removeExpAndSp(0L, requiredSp, false);
                              if (PlayerConfig.ES_SP_BOOK_NEEDED) {
                                 check &= player.destroyItem(ItemProcessType.NONE, spb.getObjectId(), 1L, player, true);
                              }

                              check &= player.destroyItemByItemId(null, 57, requireditems, player, true);
                              if (!check) {
                                 player.sendPacket(SystemMessageId.YOU_DO_NOT_HAVE_ALL_OF_THE_ITEMS_NEEDED_TO_ENCHANT_THAT_SKILL);
                                 return;
                              }

                              int levelPenalty = Rnd.get(Math.min(4, currentEnchantLevel));
                              this._skillLevel -= levelPenalty;
                              if (this._skillLevel % 100 == 0) {
                                 this._skillLevel = s.getBaseLevel();
                              }

                              skill = SkillData.getInstance().getSkill(this._skillId, this._skillLevel);
                              if (skill != null) {
                                 if (GeneralConfig.LOG_SKILL_ENCHANTS) {
                                    LOGGER_ENCHANT.log(
                                       Level.INFO,
                                       "Route Change:"
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
                                    );
                                 }

                                 player.addSkill(skill, true);
                                 player.sendPacket(ExEnchantSkillResult.valueOf(true));
                              }

                              player.updateUserInfo();
                              if (levelPenalty == 0) {
                                 SystemMessage sm = new SystemMessage(
                                    SystemMessageId.ENCHANT_SKILL_ROUTE_CHANGE_WAS_SUCCESSFUL_LV_OF_ENCHANT_SKILL_S1_WILL_REMAIN
                                 );
                                 sm.addSkillName(this._skillId);
                                 player.sendPacket(sm);
                              } else {
                                 SystemMessage sm = new SystemMessage(
                                    SystemMessageId.ENCHANT_SKILL_ROUTE_CHANGE_WAS_SUCCESSFUL_LV_OF_ENCHANT_SKILL_S1_HAS_BEEN_DECREASED_BY_S2
                                 );
                                 sm.addSkillName(this._skillId);
                                 sm.addInt(levelPenalty);
                                 player.sendPacket(sm);
                              }

                              player.sendSkillList();
                              int afterEnchantSkillLevel = player.getSkillLevel(this._skillId);
                              player.sendPacket(new ExEnchantSkillInfo(this._skillId, afterEnchantSkillLevel));
                              player.sendPacket(new ExEnchantSkillInfoDetail(3, this._skillId, afterEnchantSkillLevel, player));
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
}
