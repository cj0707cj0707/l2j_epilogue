package org.l2jmobius.gameserver.network.clientpackets;

import java.util.List;
import org.l2jmobius.commons.util.StringUtil;
import org.l2jmobius.gameserver.config.GeneralConfig;
import org.l2jmobius.gameserver.config.PlayerConfig;
import org.l2jmobius.gameserver.data.xml.SkillData;
import org.l2jmobius.gameserver.data.xml.SkillTreeData;
import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.enums.player.IllegalActionPunishmentType;
import org.l2jmobius.gameserver.entity.actor.instance.Fisherman;
import org.l2jmobius.gameserver.entity.actor.instance.Folk;
import org.l2jmobius.gameserver.entity.actor.instance.VillageMaster;
import org.l2jmobius.gameserver.entity.clan.Clan;
import org.l2jmobius.gameserver.entity.clan.ClanAccess;
import org.l2jmobius.gameserver.entity.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.entity.item.holders.ItemHolder;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.managers.PunishmentManager;
import org.l2jmobius.gameserver.managers.ScriptManager;
import org.l2jmobius.gameserver.mechanics.events.EventDispatcher;
import org.l2jmobius.gameserver.mechanics.events.EventType;
import org.l2jmobius.gameserver.mechanics.events.holders.actor.player.OnPlayerSkillLearn;
import org.l2jmobius.gameserver.mechanics.script.Quest;
import org.l2jmobius.gameserver.mechanics.script.QuestState;
import org.l2jmobius.gameserver.mechanics.skill.CommonSkill;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.mechanics.skill.enums.AcquireSkillType;
import org.l2jmobius.gameserver.mechanics.skill.holders.SkillHolder;
import org.l2jmobius.gameserver.mechanics.skill.holders.SkillLearn;
import org.l2jmobius.gameserver.network.serverpackets.AcquireSkillDone;
import org.l2jmobius.gameserver.network.serverpackets.AcquireSkillList;
import org.l2jmobius.gameserver.network.serverpackets.ExStorageMaxCount;
import org.l2jmobius.gameserver.network.serverpackets.PledgeSkillList;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;

public class RequestAcquireSkill extends ClientPacket {
   private static final String[] QUEST_VAR_NAMES = new String[]{"EmergentAbility65-", "EmergentAbility70-", "ClassAbility75-", "ClassAbility80-"};
   private int _id;
   private int _level;
   private AcquireSkillType _skillType;
   private int _subType;

   @Override
   protected void readImpl() {
      this._id = this.readInt();
      this._level = this.readInt();
      this._skillType = AcquireSkillType.getAcquireSkillType(this.readInt());
      if (this._skillType == AcquireSkillType.SUBPLEDGE) {
         this._subType = this.readInt();
      }
   }

   @Override
   protected void runImpl() {
      Player player = this.getPlayer();
      if (player != null) {
         if (player.isTransformed() || player.isMounted()) {
            player.sendPacket(
               org.l2jmobius.gameserver.network.SystemMessageId.YOU_CANNOT_USE_THE_SKILL_ENHANCING_FUNCTION_IN_THIS_CLASS_YOU_CAN_USE_THE_SKILL_ENHANCING_FUNCTION_UNDER_OFF_BATTLE_STATUS_AND_CANNOT_USE_THE_FUNCTION_WHILE_TRANSFORMING_BATTLING_AND_ON_BOARD
            );
         } else if (this._level >= 1 && this._level <= 1000 && this._id >= 1) {
            Npc trainer = player.getLastFolkNPC();
            if (trainer != null && trainer.isNpc() && (trainer.canInteract(player) || player.isGM())) {
               Skill skill = SkillData.getInstance().getSkill(this._id, this._level);
               if (skill == null) {
                  org.l2jmobius.gameserver.network.PacketLogger.warning(
                     RequestAcquireSkill.class.getSimpleName()
                        + ": "
                        + player
                        + " is trying to learn a null skill Id: "
                        + this._id
                        + " level: "
                        + this._level
                        + "!"
                  );
               } else {
                  int prevSkillLevel = player.getSkillLevel(this._id);
                  if (prevSkillLevel > 0 && this._skillType != AcquireSkillType.TRANSFER && this._skillType != AcquireSkillType.SUBPLEDGE) {
                     if (prevSkillLevel == this._level) {
                        return;
                     }

                     if (prevSkillLevel != this._level - 1) {
                        player.sendPacket(org.l2jmobius.gameserver.network.SystemMessageId.THE_PREVIOUS_LEVEL_SKILL_HAS_NOT_BEEN_LEARNED);
                        PunishmentManager.handleIllegalPlayerAction(
                           player,
                           player + " is requesting skill Id: " + this._id + " level " + this._level + " without knowing it's previous level!",
                           IllegalActionPunishmentType.NONE
                        );
                        return;
                     }
                  }

                  SkillLearn s = SkillTreeData.getInstance().getSkillLearn(this._skillType, this._id, this._level, player);
                  if (s != null) {
                     switch (this._skillType) {
                        case CLASS:
                           if (this.checkPlayerSkill(player, trainer, s)) {
                              this.giveSkill(player, trainer, skill);
                           }
                           break;
                        case TRANSFORM:
                           if (!canTransform(player)) {
                              player.sendPacket(
                                 org.l2jmobius.gameserver.network.SystemMessageId.YOU_HAVE_NOT_COMPLETED_THE_NECESSARY_QUEST_FOR_SKILL_ACQUISITION
                              );
                              PunishmentManager.handleIllegalPlayerAction(
                                 player,
                                 player + " is requesting skill Id: " + this._id + " level " + this._level + " without required quests!",
                                 IllegalActionPunishmentType.NONE
                              );
                              return;
                           }

                           if (this.checkPlayerSkill(player, trainer, s)) {
                              this.giveSkill(player, trainer, skill);
                           }
                           break;
                        case FISHING:
                           if (this.checkPlayerSkill(player, trainer, s)) {
                              this.giveSkill(player, trainer, skill);
                           }
                           break;
                        case PLEDGE:
                           if (!player.isClanLeader()) {
                              return;
                           }

                           Clan clan = player.getClan();
                           int repCost = s.getLevelUpSp();
                           if (clan.getReputationScore() >= repCost) {
                              if (PlayerConfig.LIFE_CRYSTAL_NEEDED) {
                                 for (ItemHolder item : s.getRequiredItems()) {
                                    if (!player.destroyItemByItemId(null, item.getId(), item.getCount(), trainer, false)) {
                                       player.sendPacket(
                                          org.l2jmobius.gameserver.network.SystemMessageId.YOU_DO_NOT_HAVE_THE_NECESSARY_MATERIALS_OR_PREREQUISITES_TO_LEARN_THIS_SKILL
                                       );
                                       VillageMaster.showPledgeSkillList(player);
                                       return;
                                    }

                                    SystemMessage sm = new SystemMessage(org.l2jmobius.gameserver.network.SystemMessageId.S2_S1_HAS_DISAPPEARED);
                                    sm.addItemName(item.getId());
                                    sm.addLong(item.getCount());
                                    player.sendPacket(sm);
                                 }
                              }

                              clan.takeReputationScore(repCost);
                              SystemMessage cr = new SystemMessage(
                                 org.l2jmobius.gameserver.network.SystemMessageId.S1_POINTS_HAVE_BEEN_DEDUCTED_FROM_THE_CLAN_S_REPUTATION_SCORE
                              );
                              cr.addInt(repCost);
                              player.sendPacket(cr);
                              clan.addNewSkill(skill);
                              clan.broadcastToOnlineMembers(new PledgeSkillList(clan));
                              player.sendPacket(new AcquireSkillDone());
                              VillageMaster.showPledgeSkillList(player);
                           } else {
                              player.sendPacket(
                                 org.l2jmobius.gameserver.network.SystemMessageId.THE_ATTEMPT_TO_ACQUIRE_THE_SKILL_HAS_FAILED_BECAUSE_OF_AN_INSUFFICIENT_CLAN_REPUTATION_SCORE
                              );
                              VillageMaster.showPledgeSkillList(player);
                           }
                           break;
                        case SUBPLEDGE:
                           if (!player.isClanLeader() || !player.hasAccess(ClanAccess.MEMBER_FAME)) {
                              return;
                           }

                           Clan clan = player.getClan();
                           if (clan.getFortId() == 0 && clan.getCastleId() == 0) {
                              return;
                           }

                           if (!clan.isLearnableSubPledgeSkill(skill, this._subType)) {
                              player.sendPacket(org.l2jmobius.gameserver.network.SystemMessageId.THIS_LOWER_CLAN_SKILL_HAS_ALREADY_BEEN_ACQUIRED);
                              PunishmentManager.handleIllegalPlayerAction(
                                 player,
                                 player + " is requesting skill Id: " + this._id + " level " + this._level + " without knowing it's previous level!",
                                 IllegalActionPunishmentType.NONE
                              );
                              return;
                           }

                           int repCost = s.getLevelUpSp();
                           if (clan.getReputationScore() < repCost) {
                              player.sendPacket(
                                 org.l2jmobius.gameserver.network.SystemMessageId.THE_ATTEMPT_TO_ACQUIRE_THE_SKILL_HAS_FAILED_BECAUSE_OF_AN_INSUFFICIENT_CLAN_REPUTATION_SCORE
                              );
                              return;
                           }

                           for (ItemHolder item : s.getRequiredItems()) {
                              if (!player.destroyItemByItemId(ItemProcessType.FEE, item.getId(), item.getCount(), trainer, false)) {
                                 player.sendPacket(
                                    org.l2jmobius.gameserver.network.SystemMessageId.YOU_DO_NOT_HAVE_THE_NECESSARY_MATERIALS_OR_PREREQUISITES_TO_LEARN_THIS_SKILL
                                 );
                                 return;
                              }

                              SystemMessage sm = new SystemMessage(org.l2jmobius.gameserver.network.SystemMessageId.S2_S1_HAS_DISAPPEARED);
                              sm.addItemName(item.getId());
                              sm.addLong(item.getCount());
                              player.sendPacket(sm);
                           }

                           if (repCost > 0) {
                              clan.takeReputationScore(repCost);
                              SystemMessage cr = new SystemMessage(
                                 org.l2jmobius.gameserver.network.SystemMessageId.S1_POINTS_HAVE_BEEN_DEDUCTED_FROM_THE_CLAN_S_REPUTATION_SCORE
                              );
                              cr.addInt(repCost);
                              player.sendPacket(cr);
                           }

                           clan.addNewSkill(skill, this._subType);
                           clan.broadcastToOnlineMembers(new PledgeSkillList(clan));
                           player.sendPacket(new AcquireSkillDone());
                           showSubUnitSkillList(player);
                           break;
                        case TRANSFER:
                           if (this.checkPlayerSkill(player, trainer, s)) {
                              this.giveSkill(player, trainer, skill);
                           }
                           break;
                        case SUBCLASS:
                           if (player.isSubClassActive()) {
                              player.sendPacket(
                                 org.l2jmobius.gameserver.network.SystemMessageId.THIS_SKILL_CANNOT_BE_LEARNED_WHILE_IN_THE_SUB_CLASS_STATE_PLEASE_TRY_AGAIN_AFTER_CHANGING_TO_THE_MAIN_CLASS
                              );
                              PunishmentManager.handleIllegalPlayerAction(
                                 player,
                                 player + " is requesting skill Id: " + this._id + " level " + this._level + " while Sub-Class is active!",
                                 IllegalActionPunishmentType.NONE
                              );
                              return;
                           }

                           if (prevSkillLevel == -1 && this._level > 1) {
                              player.sendPacket(org.l2jmobius.gameserver.network.SystemMessageId.THE_PREVIOUS_LEVEL_SKILL_HAS_NOT_BEEN_LEARNED);
                              PunishmentManager.handleIllegalPlayerAction(
                                 player,
                                 player + " is requesting skill Id: " + this._id + " level " + this._level + " without knowing it's previous level!",
                                 IllegalActionPunishmentType.NONE
                              );
                              return;
                           }

                           QuestState qs = player.getQuestState("SubClassSkills");
                           if (qs == null) {
                              Quest subClassSkilllsQuest = ScriptManager.getInstance().getScript("SubClassSkills");
                              if (subClassSkilllsQuest == null) {
                                 org.l2jmobius.gameserver.network.PacketLogger.warning(
                                    "Null SubClassSkills quest, for Sub-Class skill Id: " + this._id + " level: " + this._level + " for " + player + "!"
                                 );
                                 return;
                              }

                              qs = subClassSkilllsQuest.newQuestState(player);
                           }

                           for (String varName : QUEST_VAR_NAMES) {
                              for (int i = 1; i <= PlayerConfig.MAX_SUBCLASS; i++) {
                                 String itemOID = player.getVariables().getString(varName + i, "");
                                 if (!itemOID.isEmpty() && !itemOID.endsWith(";") && !itemOID.equals("0")) {
                                    if (StringUtil.isNumeric(itemOID)) {
                                       int itemObjId = Integer.parseInt(itemOID);
                                       Item item = player.getInventory().getItemByObjectId(itemObjId);
                                       if (item != null) {
                                          for (ItemHolder itemIdCount : s.getRequiredItems()) {
                                             if (item.getId() == itemIdCount.getId()) {
                                                if (this.checkPlayerSkill(player, trainer, s)) {
                                                   this.giveSkill(player, trainer, skill);
                                                   player.getVariables().set(varName + i, skill.getId() + ";");
                                                }

                                                return;
                                             }
                                          }
                                       } else {
                                          org.l2jmobius.gameserver.network.PacketLogger.warning(
                                             "Inexistent item for object Id "
                                                + itemObjId
                                                + ", for Sub-Class skill Id: "
                                                + this._id
                                                + " level: "
                                                + this._level
                                                + " for "
                                                + player
                                                + "!"
                                          );
                                       }
                                    } else {
                                       org.l2jmobius.gameserver.network.PacketLogger.warning(
                                          "Invalid item object Id "
                                             + itemOID
                                             + ", for Sub-Class skill Id: "
                                             + this._id
                                             + " level: "
                                             + this._level
                                             + " for "
                                             + player
                                             + "!"
                                       );
                                    }
                                 }
                              }
                           }

                           player.sendPacket(
                              org.l2jmobius.gameserver.network.SystemMessageId.YOU_DO_NOT_HAVE_THE_NECESSARY_MATERIALS_OR_PREREQUISITES_TO_LEARN_THIS_SKILL
                           );
                           this.showSkillList(trainer, player);
                           break;
                        case COLLECT:
                           if (this.checkPlayerSkill(player, trainer, s)) {
                              this.giveSkill(player, trainer, skill);
                           }
                           break;
                        default:
                           org.l2jmobius.gameserver.network.PacketLogger.warning(
                              "Recived Wrong Packet Data in Aquired Skill, unknown skill type:" + this._skillType
                           );
                     }
                  }
               }
            }
         } else {
            PunishmentManager.handleIllegalPlayerAction(player, "Wrong Packet Data in Aquired Skill", GeneralConfig.DEFAULT_PUNISH);
            org.l2jmobius.gameserver.network.PacketLogger.warning(
               "Recived Wrong Packet Data in Aquired Skill - id: " + this._id + " level: " + this._level + " for " + player
            );
         }
      }
   }

   public static void showSubUnitSkillList(Player player) {
      List<SkillLearn> skills = SkillTreeData.getInstance().getAvailableSubPledgeSkills(player.getClan());
      AcquireSkillList asl = new AcquireSkillList(AcquireSkillType.SUBPLEDGE);
      int count = 0;

      for (SkillLearn s : skills) {
         if (SkillData.getInstance().getSkill(s.getSkillId(), s.getSkillLevel()) != null) {
            asl.addSkill(s.getSkillId(), s.getSkillLevel(), s.getSkillLevel(), s.getLevelUpSp(), 0);
            count++;
         }
      }

      if (count == 0) {
         player.sendPacket(org.l2jmobius.gameserver.network.SystemMessageId.THERE_ARE_NO_OTHER_SKILLS_TO_LEARN);
      } else {
         player.sendPacket(asl);
      }
   }

   private boolean checkPlayerSkill(Player player, Npc trainer, SkillLearn skillLearn) {
      if (skillLearn == null || skillLearn.getSkillId() != this._id || skillLearn.getSkillLevel() != this._level) {
         return false;
      } else if (skillLearn.getGetLevel() > player.getLevel()) {
         player.sendPacket(org.l2jmobius.gameserver.network.SystemMessageId.YOU_DO_NOT_MEET_THE_SKILL_LEVEL_REQUIREMENTS);
         PunishmentManager.handleIllegalPlayerAction(
            player,
            player
               + ", level "
               + player.getLevel()
               + " is requesting skill Id: "
               + this._id
               + " level "
               + this._level
               + " without having minimum required level, "
               + skillLearn.getGetLevel()
               + "!",
            IllegalActionPunishmentType.NONE
         );
         return false;
      } else {
         int levelUpSp = skillLearn.getCalculatedLevelUpSp(player.getPlayerClass(), player.getLearningClass());
         if (levelUpSp > 0 && levelUpSp > player.getSp()) {
            player.sendPacket(org.l2jmobius.gameserver.network.SystemMessageId.YOU_DO_NOT_HAVE_ENOUGH_SP_TO_LEARN_THIS_SKILL);
            this.showSkillList(trainer, player);
            return false;
         } else if (!PlayerConfig.DIVINE_SP_BOOK_NEEDED && this._id == CommonSkill.DIVINE_INSPIRATION.getId()) {
            return true;
         } else {
            if (!skillLearn.getPreReqSkills().isEmpty()) {
               for (SkillHolder skill : skillLearn.getPreReqSkills()) {
                  if (player.getSkillLevel(skill.getSkillId()) < skill.getSkillLevel()) {
                     if (skill.getSkillId() == CommonSkill.ONYX_BEAST_TRANSFORMATION.getId()) {
                        player.sendPacket(org.l2jmobius.gameserver.network.SystemMessageId.YOU_MUST_LEARN_A_GOOD_DEED_SKILL_BEFORE_YOU_CAN_ACQUIRE_NEW_SKILLS);
                     } else {
                        player.sendPacket(
                           org.l2jmobius.gameserver.network.SystemMessageId.YOU_DO_NOT_HAVE_THE_NECESSARY_MATERIALS_OR_PREREQUISITES_TO_LEARN_THIS_SKILL
                        );
                     }

                     return false;
                  }
               }
            }

            if (!skillLearn.getRequiredItems().isEmpty()) {
               long reqItemCount = 0L;

               for (ItemHolder item : skillLearn.getRequiredItems()) {
                  reqItemCount = player.getInventory().getInventoryItemCount(item.getId(), -1);
                  if (reqItemCount < item.getCount()) {
                     player.sendPacket(
                        org.l2jmobius.gameserver.network.SystemMessageId.YOU_DO_NOT_HAVE_THE_NECESSARY_MATERIALS_OR_PREREQUISITES_TO_LEARN_THIS_SKILL
                     );
                     this.showSkillList(trainer, player);
                     return false;
                  }
               }

               for (ItemHolder itemIdCount : skillLearn.getRequiredItems()) {
                  if (!player.destroyItemByItemId(ItemProcessType.FEE, itemIdCount.getId(), itemIdCount.getCount(), trainer, true)) {
                     PunishmentManager.handleIllegalPlayerAction(
                        player,
                        "Somehow "
                           + player
                           + ", level "
                           + player.getLevel()
                           + " lose required item Id: "
                           + itemIdCount.getId()
                           + " to learn skill while learning skill Id: "
                           + this._id
                           + " level "
                           + this._level
                           + "!",
                        IllegalActionPunishmentType.NONE
                     );
                  }
               }
            }

            if (levelUpSp > 0) {
               player.setSp(player.getSp() - levelUpSp);
               player.updateUserInfo();
            }

            return true;
         }
      }
   }

   private void giveSkill(Player player, Npc trainer, Skill skill) {
      SystemMessage sm = new SystemMessage(org.l2jmobius.gameserver.network.SystemMessageId.YOU_HAVE_EARNED_S1_2);
      sm.addSkillName(skill);
      player.sendPacket(sm);
      player.sendPacket(new AcquireSkillDone());
      player.addSkill(skill, true);
      player.sendSkillList();
      player.updateShortcuts(this._id, this._level);
      this.showSkillList(trainer, player);
      if (this._id >= 1368 && this._id <= 1372) {
         player.sendPacket(new ExStorageMaxCount(player));
      }

      if (EventDispatcher.getInstance().hasListener(EventType.ON_PLAYER_SKILL_LEARN, trainer)) {
         EventDispatcher.getInstance().notifyEventAsync(new OnPlayerSkillLearn(trainer, player, skill, this._skillType), trainer);
      }
   }

   private void showSkillList(Npc trainer, Player player) {
      if (this._skillType != AcquireSkillType.TRANSFORM && this._skillType != AcquireSkillType.SUBCLASS && this._skillType != AcquireSkillType.TRANSFER) {
         if (trainer instanceof Fisherman) {
            Fisherman.showFishSkillList(player);
         } else {
            Folk.showSkillList(player, trainer, player.getLearningClass());
         }
      }
   }

   public static boolean canTransform(Player player) {
      if (PlayerConfig.ALLOW_TRANSFORM_WITHOUT_QUEST) {
         return true;
      } else {
         QuestState qs = player.getQuestState("Q00136_MoreThanMeetsTheEye");
         return qs != null && qs.isCompleted();
      }
   }
}
