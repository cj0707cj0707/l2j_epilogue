package org.l2jmobius.gameserver.network.clientpackets;

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

public class RequestExEnchantSkill extends ClientPacket {
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
      if (this.getClient().getFloodProtectors().canPerformPlayerAction()) {
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
               } else if (!player.isSellingBuffs()) {
                  Skill skill = SkillData.getInstance().getSkill(this._skillId, this._skillLevel);
                  if (skill != null) {
                     EnchantSkillLearn s = EnchantSkillGroupsData.getInstance().getSkillEnchantmentBySkillId(this._skillId);
                     if (s != null) {
                        int beforeEnchantSkillLevel = player.getSkillLevel(this._skillId);
                        if (beforeEnchantSkillLevel == s.getMinSkillLevel(this._skillLevel)) {
                           EnchantSkillGroup.EnchantSkillHolder esd = s.getEnchantSkillHolder(this._skillLevel);
                           int costMultiplier = EnchantSkillGroupsData.NORMAL_ENCHANT_COST_MULTIPLIER;
                           int requiredSp = esd.getSpCost() * costMultiplier;
                           if (player.getSp() >= requiredSp) {
                              boolean usesBook = this._skillLevel % 100 == 1;
                              int reqItemId = 6622;
                              Item spb = player.getInventory().getItemByItemId(6622);
                              if (PlayerConfig.ES_SP_BOOK_NEEDED && usesBook && spb == null) {
                                 player.sendPacket(SystemMessageId.YOU_DO_NOT_HAVE_ALL_OF_THE_ITEMS_NEEDED_TO_ENCHANT_THAT_SKILL);
                                 return;
                              }

                              int requiredAdena = esd.getAdenaCost() * costMultiplier;
                              if (player.getInventory().getAdena() < requiredAdena) {
                                 player.sendPacket(SystemMessageId.YOU_DO_NOT_HAVE_ALL_OF_THE_ITEMS_NEEDED_TO_ENCHANT_THAT_SKILL);
                                 return;
                              }

                              boolean check = player.getStat().removeExpAndSp(0L, requiredSp, false);
                              if (PlayerConfig.ES_SP_BOOK_NEEDED && usesBook) {
                                 check &= player.destroyItem(ItemProcessType.NONE, spb.getObjectId(), 1L, player, true);
                              }

                              check &= player.destroyItemByItemId(null, 57, requiredAdena, player, true);
                              if (!check) {
                                 player.sendPacket(SystemMessageId.YOU_DO_NOT_HAVE_ALL_OF_THE_ITEMS_NEEDED_TO_ENCHANT_THAT_SKILL);
                                 return;
                              }

                              int rate = esd.getRate(player);
                              if (Rnd.get(100) <= rate) {
                                 if (GeneralConfig.LOG_SKILL_ENCHANTS) {
                                    StringBuilder sb = new StringBuilder();
                                    LOGGER_ENCHANT.info(
                                       sb.append("Success, Character:")
                                          .append(player.getName())
                                          .append(" [")
                                          .append(player.getObjectId())
                                          .append("] Account:")
                                          .append(player.getAccountName())
                                          .append(" IP:")
                                          .append(player.getIPAddress())
                                          .append(", Skill:")
                                          .append(skill)
                                          .append(", SPB:")
                                          .append(spb)
                                          .append(", Rate:")
                                          .append(rate)
                                          .toString()
                                    );
                                 }

                                 player.addSkill(skill, true);
                                 player.sendPacket(ExEnchantSkillResult.valueOf(true));
                                 SystemMessage sm = new SystemMessage(SystemMessageId.SKILL_ENCHANT_WAS_SUCCESSFUL_S1_HAS_BEEN_ENCHANTED);
                                 sm.addSkillName(this._skillId);
                                 player.sendPacket(sm);
                              } else {
                                 player.addSkill(SkillData.getInstance().getSkill(this._skillId, s.getBaseLevel()), true);
                                 player.sendPacket(SystemMessageId.SKILL_ENCHANT_FAILED_THE_SKILL_WILL_BE_INITIALIZED);
                                 player.sendPacket(ExEnchantSkillResult.valueOf(false));
                                 if (GeneralConfig.LOG_SKILL_ENCHANTS) {
                                    StringBuilder sb = new StringBuilder();
                                    LOGGER_ENCHANT.info(
                                       sb.append("Failed, Character:")
                                          .append(player.getName())
                                          .append(" [")
                                          .append(player.getObjectId())
                                          .append("] Account:")
                                          .append(player.getAccountName())
                                          .append(" IP:")
                                          .append(player.getIPAddress())
                                          .append(", Skill:")
                                          .append(skill)
                                          .append(", SPB:")
                                          .append(spb)
                                          .append(", Rate:")
                                          .append(rate)
                                          .toString()
                                    );
                                 }
                              }

                              player.updateUserInfo();
                              player.sendSkillList();
                              int afterEnchantSkillLevel = player.getSkillLevel(this._skillId);
                              player.sendPacket(new ExEnchantSkillInfo(this._skillId, afterEnchantSkillLevel));
                              player.sendPacket(new ExEnchantSkillInfoDetail(0, this._skillId, afterEnchantSkillLevel + 1, player));
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
