package org.l2jmobius.gameserver.entity.actor.instance;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.l2jmobius.commons.util.StringUtil;
import org.l2jmobius.gameserver.config.PlayerConfig;
import org.l2jmobius.gameserver.config.ServerConfig;
import org.l2jmobius.gameserver.data.enums.CategoryType;
import org.l2jmobius.gameserver.data.sql.ClanTable;
import org.l2jmobius.gameserver.data.xml.CategoryData;
import org.l2jmobius.gameserver.data.xml.ClassListData;
import org.l2jmobius.gameserver.data.xml.SkillTreeData;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.enums.creature.InstanceType;
import org.l2jmobius.gameserver.entity.actor.enums.creature.Race;
import org.l2jmobius.gameserver.entity.actor.enums.player.PlayerClass;
import org.l2jmobius.gameserver.entity.actor.holders.player.SubClassHolder;
import org.l2jmobius.gameserver.entity.actor.templates.NpcTemplate;
import org.l2jmobius.gameserver.entity.clan.Clan;
import org.l2jmobius.gameserver.entity.clan.ClanMember;
import org.l2jmobius.gameserver.entity.zone.ZoneId;
import org.l2jmobius.gameserver.managers.CastleManager;
import org.l2jmobius.gameserver.managers.FortSiegeManager;
import org.l2jmobius.gameserver.managers.SiegeManager;
import org.l2jmobius.gameserver.mechanics.olympiad.Olympiad;
import org.l2jmobius.gameserver.mechanics.script.QuestState;
import org.l2jmobius.gameserver.mechanics.siege.Castle;
import org.l2jmobius.gameserver.mechanics.siege.Fort;
import org.l2jmobius.gameserver.mechanics.skill.enums.AcquireSkillType;
import org.l2jmobius.gameserver.mechanics.skill.holders.SkillLearn;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.AcquireSkillList;
import org.l2jmobius.gameserver.network.serverpackets.ActionFailed;
import org.l2jmobius.gameserver.network.serverpackets.MagicSkillLaunched;
import org.l2jmobius.gameserver.network.serverpackets.MagicSkillUse;
import org.l2jmobius.gameserver.network.serverpackets.NpcHtmlMessage;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;

public class VillageMaster extends Folk {
   private static final Logger LOGGER = Logger.getLogger(VillageMaster.class.getName());
   private static final Set<PlayerClass> mainSubclassSet;
   private static final Set<PlayerClass> neverSubclassed = EnumSet.of(PlayerClass.OVERLORD, PlayerClass.WARSMITH);
   private static final Set<PlayerClass> subclasseSet1 = EnumSet.of(
      PlayerClass.DARK_AVENGER, PlayerClass.PALADIN, PlayerClass.TEMPLE_KNIGHT, PlayerClass.SHILLIEN_KNIGHT
   );
   private static final Set<PlayerClass> subclasseSet2 = EnumSet.of(PlayerClass.TREASURE_HUNTER, PlayerClass.ABYSS_WALKER, PlayerClass.PLAINS_WALKER);
   private static final Set<PlayerClass> subclasseSet3 = EnumSet.of(PlayerClass.HAWKEYE, PlayerClass.SILVER_RANGER, PlayerClass.PHANTOM_RANGER);
   private static final Set<PlayerClass> subclasseSet4 = EnumSet.of(PlayerClass.WARLOCK, PlayerClass.ELEMENTAL_SUMMONER, PlayerClass.PHANTOM_SUMMONER);
   private static final Set<PlayerClass> subclasseSet5 = EnumSet.of(PlayerClass.SORCERER, PlayerClass.SPELLSINGER, PlayerClass.SPELLHOWLER);
   private static final EnumMap<PlayerClass, Set<PlayerClass>> subclassSetMap = new EnumMap<>(PlayerClass.class);

   public VillageMaster(NpcTemplate template) {
      super(template);
      this.setInstanceType(InstanceType.VillageMaster);
   }

   @Override
   public String getHtmlPath(int npcId, int value) {
      String pom = "";
      if (value == 0) {
         pom = Integer.toString(npcId);
      } else {
         pom = npcId + "-" + value;
      }

      return "data/html/villagemaster/" + pom + ".htm";
   }

   @Override
   public void onBypassFeedback(Player player, String command) {
      String[] commandStr = command.split(" ");
      String actualCommand = commandStr[0];
      String cmdParams = "";
      String cmdParams2 = "";
      if (commandStr.length >= 2) {
         cmdParams = commandStr[1];
      }

      if (commandStr.length >= 3) {
         cmdParams2 = commandStr[2];
      }

      if (actualCommand.equalsIgnoreCase("create_clan")) {
         if (cmdParams.isEmpty()) {
            player.sendPacket(SystemMessageId.PLEASE_CREATE_YOUR_CLAN_NAME);
            return;
         }

         if (!cmdParams2.isEmpty() || !isValidName(cmdParams)) {
            player.sendPacket(SystemMessageId.CLAN_NAME_IS_INVALID);
            return;
         }

         ClanTable.getInstance().createClan(player, cmdParams);
      } else if (actualCommand.equalsIgnoreCase("create_academy")) {
         if (cmdParams.isEmpty()) {
            return;
         }

         this.createSubPledge(player, cmdParams, null, -1, 5);
      } else if (actualCommand.equalsIgnoreCase("rename_pledge")) {
         if (cmdParams.isEmpty() || cmdParams2.isEmpty()) {
            return;
         }

         this.renameSubPledge(player, Integer.parseInt(cmdParams), cmdParams2);
      } else if (actualCommand.equalsIgnoreCase("create_royal")) {
         if (cmdParams.isEmpty()) {
            return;
         }

         this.createSubPledge(player, cmdParams, cmdParams2, 100, 6);
      } else if (actualCommand.equalsIgnoreCase("create_knight")) {
         if (cmdParams.isEmpty()) {
            return;
         }

         this.createSubPledge(player, cmdParams, cmdParams2, 1001, 7);
      } else if (actualCommand.equalsIgnoreCase("assign_subpl_leader")) {
         if (cmdParams.isEmpty()) {
            return;
         }

         this.assignSubPledgeLeader(player, cmdParams, cmdParams2);
      } else if (actualCommand.equalsIgnoreCase("create_ally")) {
         if (cmdParams.isEmpty()) {
            return;
         }

         if (player.getClan() == null) {
            player.sendPacket(SystemMessageId.ONLY_CLAN_LEADERS_MAY_CREATE_ALLIANCES);
         } else {
            player.getClan().createAlly(player, cmdParams);
         }
      } else if (actualCommand.equalsIgnoreCase("dissolve_ally")) {
         player.getClan().dissolveAlly(player);
      } else if (actualCommand.equalsIgnoreCase("dissolve_clan")) {
         this.dissolveClan(player, player.getClanId());
      } else if (actualCommand.equalsIgnoreCase("change_clan_leader")) {
         if (cmdParams.isEmpty()) {
            return;
         }

         if (!player.isClanLeader()) {
            player.sendPacket(SystemMessageId.YOU_ARE_NOT_AUTHORIZED_TO_DO_THAT);
            return;
         }

         if (player.getName().equalsIgnoreCase(cmdParams)) {
            return;
         }

         Clan clan = player.getClan();
         ClanMember member = clan.getClanMember(cmdParams);
         if (member == null) {
            SystemMessage sm = new SystemMessage(SystemMessageId.S1_DOES_NOT_EXIST);
            sm.addString(cmdParams);
            player.sendPacket(sm);
            return;
         }

         if (!member.isOnline()) {
            player.sendPacket(SystemMessageId.THAT_PLAYER_IS_NOT_CURRENTLY_ONLINE);
            return;
         }

         if (member.getPlayer().isAcademyMember()) {
            player.sendPacket(SystemMessageId.THAT_PRIVILEGE_CANNOT_BE_GRANTED_TO_A_CLAN_ACADEMY_MEMBER);
            return;
         }

         if (PlayerConfig.ALT_CLAN_LEADER_INSTANT_ACTIVATION) {
            clan.setNewLeader(member);
         } else {
            NpcHtmlMessage msg = new NpcHtmlMessage(this.getObjectId());
            if (clan.getNewLeaderId() == 0) {
               clan.setNewLeaderId(member.getObjectId(), true);
               msg.setFile(player, "data/scripts/village_master/ClanMaster/9000-07-success.htm");
            } else {
               msg.setFile(player, "data/scripts/village_master/ClanMaster/9000-07-in-progress.htm");
            }

            player.sendPacket(msg);
         }
      } else if (actualCommand.equalsIgnoreCase("cancel_clan_leader_change")) {
         if (!player.isClanLeader()) {
            player.sendPacket(SystemMessageId.YOU_ARE_NOT_AUTHORIZED_TO_DO_THAT);
            return;
         }

         Clan clanx = player.getClan();
         NpcHtmlMessage msg = new NpcHtmlMessage(this.getObjectId());
         if (clanx.getNewLeaderId() != 0) {
            clanx.setNewLeaderId(0, true);
            msg.setFile(player, "data/scripts/village_master/ClanMaster/9000-07-canceled.htm");
         } else {
            msg.setHtml("<html><body>You don't have clan leader delegation applications submitted yet!</body></html>");
         }

         player.sendPacket(msg);
      } else if (actualCommand.equalsIgnoreCase("recover_clan")) {
         this.recoverClan(player, player.getClanId());
      } else if (actualCommand.equalsIgnoreCase("increase_clan_level")) {
         if (player.getClan().levelUpClan(player)) {
            player.broadcastSkillPacket(new MagicSkillUse(player, 5103, 1, 0, 0), player);
            player.broadcastSkillPacket(new MagicSkillLaunched(player, 5103, 1), player);
         }
      } else if (actualCommand.equalsIgnoreCase("learn_clan_skills")) {
         showPledgeSkillList(player);
      } else if (command.startsWith("Subclass")) {
         if (player.isCastingNow() || player.isAllSkillsDisabled()) {
            player.sendPacket(SystemMessageId.SUB_CLASSES_MAY_NOT_BE_CREATED_OR_CHANGED_WHILE_A_SKILL_IS_IN_USE);
            return;
         }

         if (Olympiad.getInstance().isRegisteredInComp(player)) {
            Olympiad.getInstance().unRegisterNoble(player);
         }

         NpcHtmlMessage html = new NpcHtmlMessage(this.getObjectId());
         if (player.getTransformation() != null) {
            html.setFile(player, "data/html/villagemaster/SubClass_NoTransformed.htm");
            player.sendPacket(html);
            return;
         }

         if (player.hasSummon()) {
            html.setFile(player, "data/html/villagemaster/SubClass_NoSummon.htm");
            player.sendPacket(html);
            return;
         }

         if (!player.isInventoryUnder90(true)) {
            player.sendPacket(SystemMessageId.A_SUB_CLASS_CANNOT_BE_CREATED_OR_CHANGED_BECAUSE_YOU_HAVE_EXCEEDED_YOUR_INVENTORY_LIMIT);
            return;
         }

         if (player.getWeightPenalty() >= 2) {
            player.sendPacket(SystemMessageId.A_SUB_CLASS_CANNOT_BE_CREATED_OR_CHANGED_WHILE_YOU_ARE_OVER_YOUR_WEIGHT_LIMIT);
            return;
         }

         int cmdChoice = 0;
         int paramOne = 0;
         int paramTwo = 0;

         try {
            cmdChoice = Integer.parseInt(command.substring(9, 10).trim());
            int endIndex = command.indexOf(32, 11);
            if (endIndex == -1) {
               endIndex = command.length();
            }

            if (command.length() > 11) {
               paramOne = Integer.parseInt(command.substring(11, endIndex).trim());
               if (command.length() > endIndex) {
                  paramTwo = Integer.parseInt(command.substring(endIndex).trim());
               }
            }
         } catch (Exception var17) {
            LOGGER.warning(VillageMaster.class.getName() + ": Wrong numeric values for command " + command);
         }

         Set<PlayerClass> subsAvailable = null;
         switch (cmdChoice) {
            case 0:
               html.setFile(player, this.getSubClassMenu(player.getRace()));
               break;
            case 1:
               if (player.getTotalSubClasses() >= PlayerConfig.MAX_SUBCLASS) {
                  html.setFile(player, this.getSubClassFail());
                  break;
               } else {
                  subsAvailable = this.getAvailableSubClasses(player);
                  if (subsAvailable != null && !subsAvailable.isEmpty()) {
                     html.setFile(player, "data/html/villagemaster/SubClass_Add.htm");
                     StringBuilder content1 = new StringBuilder(200);

                     for (PlayerClass subClass : subsAvailable) {
                        content1.append(
                           "<a action=\"bypass npc_%objectId%_Subclass 4 "
                              + subClass.getId()
                              + "\" msg=\"1268;"
                              + ClassListData.getInstance().getClass(subClass.getId()).getClassName()
                              + "\">"
                              + ClassListData.getInstance().getClass(subClass.getId()).getClassName()
                              + "</a><br>"
                        );
                     }

                     html.replace("%list%", content1.toString());
                     break;
                  }

                  if (player.getRace() == Race.ELF || player.getRace() == Race.DARK_ELF) {
                     html.setFile(player, "data/html/villagemaster/SubClass_Fail_Elves.htm");
                     player.sendPacket(html);
                  } else if (player.getRace() == Race.KAMAEL) {
                     html.setFile(player, "data/html/villagemaster/SubClass_Fail_Kamael.htm");
                     player.sendPacket(html);
                  } else {
                     player.sendMessage("There are no sub classes available at this time.");
                  }

                  return;
               }
            case 2:
               if (player.getSubClasses().isEmpty()) {
                  html.setFile(player, "data/html/villagemaster/SubClass_ChangeNo.htm");
               } else {
                  StringBuilder content2 = new StringBuilder(200);
                  if (this.checkVillageMaster(player.getBaseClass())) {
                     content2.append(
                        "<a action=\"bypass -h npc_%objectId%_Subclass 5 0\">"
                           + ClassListData.getInstance().getClass(player.getBaseClass()).getClassName()
                           + "</a><br>"
                     );
                  }

                  Iterator<SubClassHolder> subList = iterSubClasses(player);

                  while (subList.hasNext()) {
                     SubClassHolder subClass = subList.next();
                     if (this.checkVillageMaster(subClass.getPlayerClass())) {
                        content2.append(
                           "<a action=\"bypass -h npc_%objectId%_Subclass 5 "
                              + subClass.getClassIndex()
                              + "\">"
                              + ClassListData.getInstance().getClass(subClass.getId()).getClassName()
                              + "</a><br>"
                        );
                     }
                  }

                  if (!content2.isEmpty()) {
                     html.setFile(player, "data/html/villagemaster/SubClass_Change.htm");
                     html.replace("%list%", content2.toString());
                  } else {
                     html.setFile(player, "data/html/villagemaster/SubClass_ChangeNotFound.htm");
                  }
               }
               break;
            case 3:
               if (player.getSubClasses() != null && !player.getSubClasses().isEmpty()) {
                  if (player.getTotalSubClasses() > 3) {
                     html.setFile(player, "data/html/villagemaster/SubClass_ModifyCustom.htm");
                     StringBuilder content3 = new StringBuilder(200);
                     int classIndex = 1;
                     Iterator<SubClassHolder> subList = iterSubClasses(player);

                     while (subList.hasNext()) {
                        SubClassHolder subClass = subList.next();
                        content3.append(
                           "Sub-class "
                              + classIndex++
                              + "<br><a action=\"bypass -h npc_%objectId%_Subclass 6 "
                              + subClass.getClassIndex()
                              + "\">"
                              + ClassListData.getInstance().getClass(subClass.getId()).getClassName()
                              + "</a><br>"
                        );
                     }

                     html.replace("%list%", content3.toString());
                  } else {
                     html.setFile(player, "data/html/villagemaster/SubClass_Modify.htm");
                     if (player.getSubClasses().containsKey(1)) {
                        html.replace("%sub1%", ClassListData.getInstance().getClass(player.getSubClasses().get(1).getId()).getClassName());
                     } else {
                        html.replace("<a action=\"bypass npc_%objectId%_Subclass 6 1\">%sub1%</a><br>", "");
                     }

                     if (player.getSubClasses().containsKey(2)) {
                        html.replace("%sub2%", ClassListData.getInstance().getClass(player.getSubClasses().get(2).getId()).getClassName());
                     } else {
                        html.replace("<a action=\"bypass npc_%objectId%_Subclass 6 2\">%sub2%</a><br>", "");
                     }

                     if (player.getSubClasses().containsKey(3)) {
                        html.replace("%sub3%", ClassListData.getInstance().getClass(player.getSubClasses().get(3).getId()).getClassName());
                     } else {
                        html.replace("<a action=\"bypass npc_%objectId%_Subclass 6 3\">%sub3%</a><br>", "");
                     }
                  }
               } else {
                  html.setFile(player, "data/html/villagemaster/SubClass_ModifyEmpty.htm");
               }
               break;
            case 4:
               if (!player.getClient().getFloodProtectors().canChangeSubclass()) {
                  return;
               }

               boolean allowAddition = true;
               if (player.getTotalSubClasses() >= PlayerConfig.MAX_SUBCLASS) {
                  allowAddition = false;
               }

               if (player.getLevel() < 75) {
                  allowAddition = false;
               }

               if (allowAddition && !player.getSubClasses().isEmpty()) {
                  Iterator<SubClassHolder> subList = iterSubClasses(player);

                  while (subList.hasNext()) {
                     SubClassHolder subClass = subList.next();
                     if (subClass.getLevel() < 75) {
                        allowAddition = false;
                        break;
                     }
                  }
               }

               if (allowAddition && !PlayerConfig.ALT_GAME_SUBCLASS_WITHOUT_QUESTS) {
                  allowAddition = this.checkQuests(player);
               }

               if (allowAddition && this.isValidNewSubClass(player, paramOne)) {
                  if (!player.addSubClass(paramOne, player.getTotalSubClasses() + 1)) {
                     return;
                  }

                  player.setActiveClass(player.getTotalSubClasses());
                  html.setFile(player, "data/html/villagemaster/SubClass_AddOk.htm");
                  player.sendPacket(SystemMessageId.THE_NEW_SUBCLASS_HAS_BEEN_ADDED);
               } else {
                  html.setFile(player, this.getSubClassFail());
               }
               break;
            case 5:
               if (!player.getClient().getFloodProtectors().canChangeSubclass()) {
                  return;
               }

               if (player.getClassIndex() != paramOne) {
                  if (paramOne == 0) {
                     if (!this.checkVillageMaster(player.getBaseClass())) {
                        return;
                     }
                  } else {
                     try {
                        if (!this.checkVillageMaster(player.getSubClasses().get(paramOne).getPlayerClass())) {
                           return;
                        }
                     } catch (NullPointerException var16) {
                        return;
                     }
                  }

                  player.setActiveClass(paramOne);
                  player.sendPacket(SystemMessageId.YOU_HAVE_SUCCESSFULLY_SWITCHED_TO_YOUR_SUBCLASS);
                  return;
               }

               html.setFile(player, "data/html/villagemaster/SubClass_Current.htm");
               break;
            case 6:
               if (paramOne < 1 || paramOne > PlayerConfig.MAX_SUBCLASS) {
                  return;
               }

               subsAvailable = this.getAvailableSubClasses(player);
               if (subsAvailable == null || subsAvailable.isEmpty()) {
                  player.sendMessage("There are no sub classes available at this time.");
                  return;
               }

               StringBuilder content6 = new StringBuilder(200);

               for (PlayerClass subClass : subsAvailable) {
                  content6.append(
                     "<a action=\"bypass npc_%objectId%_Subclass 7 "
                        + paramOne
                        + " "
                        + subClass.getId()
                        + "\" msg=\"1445;\">"
                        + ClassListData.getInstance().getClass(subClass.getId()).getClassName()
                        + "</a><br>"
                  );
               }

               switch (paramOne) {
                  case 1:
                     html.setFile(player, "data/html/villagemaster/SubClass_ModifyChoice1.htm");
                     break;
                  case 2:
                     html.setFile(player, "data/html/villagemaster/SubClass_ModifyChoice2.htm");
                     break;
                  case 3:
                     html.setFile(player, "data/html/villagemaster/SubClass_ModifyChoice3.htm");
                     break;
                  default:
                     html.setFile(player, "data/html/villagemaster/SubClass_ModifyChoice.htm");
               }

               html.replace("%list%", content6.toString());
               break;
            case 7:
               if (!player.getClient().getFloodProtectors().canChangeSubclass()) {
                  return;
               }

               if (!this.isValidNewSubClass(player, paramTwo)) {
                  return;
               }

               if (!player.modifySubClass(paramOne, paramTwo)) {
                  player.setActiveClass(0);
                  player.sendMessage("The sub class could not be added, you have been reverted to your base class.");
                  return;
               }

               player.abortCast();
               player.stopAllEffectsExceptThoseThatLastThroughDeath();
               player.stopAllEffectsNotStayOnSubclassChange();
               player.stopCubics();
               player.setActiveClass(paramOne);
               html.setFile(player, "data/html/villagemaster/SubClass_ModifyOk.htm");
               html.replace("%name%", ClassListData.getInstance().getClass(paramTwo).getClassName());
               player.sendPacket(SystemMessageId.THE_NEW_SUBCLASS_HAS_BEEN_ADDED);
         }

         html.replace("%objectId%", String.valueOf(this.getObjectId()));
         player.sendPacket(html);
      } else {
         super.onBypassFeedback(player, command);
      }
   }

   protected String getSubClassMenu(Race race) {
      return !PlayerConfig.ALT_GAME_SUBCLASS_EVERYWHERE && race == Race.KAMAEL
         ? "data/html/villagemaster/SubClass_NoOther.htm"
         : "data/html/villagemaster/SubClass.htm";
   }

   protected String getSubClassFail() {
      return "data/html/villagemaster/SubClass_Fail.htm";
   }

   protected boolean checkQuests(Player player) {
      if (player.isNoble()) {
         return true;
      } else {
         QuestState qs = player.getQuestState("Q00234_FatesWhisper");
         if (qs != null && qs.isCompleted()) {
            qs = player.getQuestState("Q00235_MimirsElixir");
            return qs != null && qs.isCompleted();
         } else {
            return false;
         }
      }
   }

   private Set<PlayerClass> getAvailableSubClasses(Player player) {
      int currentBaseId = player.getBaseClass();
      PlayerClass baseCID = PlayerClass.getPlayerClass(currentBaseId);
      int baseClassId;
      if (baseCID.level() > 2) {
         baseClassId = baseCID.getParent().getId();
      } else {
         baseClassId = currentBaseId;
      }

      Set<PlayerClass> availSubs = this.getSubclasses(player, baseClassId);
      if (availSubs != null && !availSubs.isEmpty()) {
         Iterator<PlayerClass> availSub = availSubs.iterator();

         while (availSub.hasNext()) {
            PlayerClass pclass = availSub.next();
            if (!this.checkVillageMaster(pclass)) {
               availSub.remove();
            } else {
               int availClassId = pclass.getId();
               PlayerClass cid = PlayerClass.getPlayerClass(availClassId);
               Iterator<SubClassHolder> subList = iterSubClasses(player);

               while (subList.hasNext()) {
                  SubClassHolder prevSubClass = subList.next();
                  PlayerClass subClassId = PlayerClass.getPlayerClass(prevSubClass.getId());
                  if (subClassId.equalsOrChildOf(cid)) {
                     availSub.remove();
                     break;
                  }
               }
            }
         }
      }

      return availSubs;
   }

   public Set<PlayerClass> getSubclasses(Player player, int classId) {
      Set<PlayerClass> subclasses = null;
      PlayerClass pClass = PlayerClass.getPlayerClass(classId);
      if (CategoryData.getInstance().isInCategory(CategoryType.THIRD_CLASS_GROUP, classId)
         || CategoryData.getInstance().isInCategory(CategoryType.FOURTH_CLASS_GROUP, classId)) {
         subclasses = EnumSet.copyOf(mainSubclassSet);
         subclasses.remove(pClass);
         if (player.getRace() == Race.KAMAEL) {
            for (PlayerClass cid : PlayerClass.values()) {
               if (cid.getRace() != Race.KAMAEL) {
                  subclasses.remove(cid);
               }
            }

            if (player.getAppearance().isFemale()) {
               subclasses.remove(PlayerClass.MALE_SOULBREAKER);
            } else {
               subclasses.remove(PlayerClass.FEMALE_SOULBREAKER);
            }

            if (!player.getSubClasses().containsKey(2) || player.getSubClasses().get(2).getLevel() < 75) {
               subclasses.remove(PlayerClass.INSPECTOR);
            }
         } else {
            if (player.getRace() == Race.ELF) {
               for (PlayerClass cidx : PlayerClass.values()) {
                  if (cidx.getRace() == Race.DARK_ELF) {
                     subclasses.remove(cidx);
                  }
               }
            } else if (player.getRace() == Race.DARK_ELF) {
               for (PlayerClass cidxx : PlayerClass.values()) {
                  if (cidxx.getRace() == Race.ELF) {
                     subclasses.remove(cidxx);
                  }
               }
            }

            for (PlayerClass cidxxx : PlayerClass.values()) {
               if (cidxxx.getRace() == Race.KAMAEL) {
                  subclasses.remove(cidxxx);
               }
            }
         }

         Set<PlayerClass> unavailableClasses = subclassSetMap.get(pClass);
         if (unavailableClasses != null) {
            subclasses.removeAll(unavailableClasses);
         }
      }

      if (subclasses != null) {
         PlayerClass currClassId = player.getPlayerClass();

         for (PlayerClass tempClass : subclasses) {
            if (currClassId.equalsOrChildOf(tempClass)) {
               subclasses.remove(tempClass);
            }
         }
      }

      return subclasses;
   }

   private boolean isValidNewSubClass(Player player, int classId) {
      if (!this.checkVillageMaster(classId)) {
         return false;
      } else {
         PlayerClass cid = PlayerClass.getPlayerClass(classId);
         Iterator<SubClassHolder> subList = iterSubClasses(player);

         while (subList.hasNext()) {
            SubClassHolder sub = subList.next();
            PlayerClass subClassId = PlayerClass.getPlayerClass(sub.getId());
            if (subClassId.equalsOrChildOf(cid)) {
               return false;
            }
         }

         int currentBaseId = player.getBaseClass();
         PlayerClass baseCID = PlayerClass.getPlayerClass(currentBaseId);
         int baseClassId;
         if (baseCID.level() > 2) {
            baseClassId = baseCID.getParent().getId();
         } else {
            baseClassId = currentBaseId;
         }

         Set<PlayerClass> availSubs = this.getSubclasses(player, baseClassId);
         if (availSubs != null && !availSubs.isEmpty()) {
            boolean found = false;

            for (PlayerClass pclass : availSubs) {
               if (pclass.getId() == classId) {
                  found = true;
                  break;
               }
            }

            return found;
         } else {
            return false;
         }
      }
   }

   protected boolean checkVillageMasterRace(PlayerClass pClass) {
      return true;
   }

   protected boolean checkVillageMasterTeachType(PlayerClass pClass) {
      return true;
   }

   public boolean checkVillageMaster(int classId) {
      return this.checkVillageMaster(PlayerClass.getPlayerClass(classId));
   }

   public boolean checkVillageMaster(PlayerClass pclass) {
      return PlayerConfig.ALT_GAME_SUBCLASS_EVERYWHERE ? true : this.checkVillageMasterRace(pclass) && this.checkVillageMasterTeachType(pclass);
   }

   private static Iterator<SubClassHolder> iterSubClasses(Player player) {
      return player.getSubClasses().values().iterator();
   }

   private void dissolveClan(Player player, int clanId) {
      if (!player.isClanLeader()) {
         player.sendPacket(SystemMessageId.YOU_ARE_NOT_AUTHORIZED_TO_DO_THAT);
      } else {
         Clan clan = player.getClan();
         if (clan.getAllyId() != 0) {
            player.sendPacket(SystemMessageId.YOU_CANNOT_DISPERSE_THE_CLANS_IN_YOUR_ALLIANCE);
         } else if (clan.isAtWar()) {
            player.sendPacket(SystemMessageId.YOU_CANNOT_DISSOLVE_A_CLAN_WHILE_ENGAGED_IN_A_WAR);
         } else if (clan.getCastleId() == 0 && clan.getHideoutId() == 0 && clan.getFortId() == 0) {
            for (Castle castle : CastleManager.getInstance().getCastles()) {
               if (SiegeManager.getInstance().checkIsRegistered(clan, castle.getResidenceId())) {
                  player.sendPacket(SystemMessageId.YOU_CANNOT_DISSOLVE_A_CLAN_DURING_A_SIEGE_OR_WHILE_PROTECTING_A_CASTLE);
                  return;
               }
            }

            for (Fort fort : org.l2jmobius.gameserver.managers.FortManager.getInstance().getForts()) {
               if (FortSiegeManager.getInstance().checkIsRegistered(clan, fort.getResidenceId())) {
                  player.sendPacket(SystemMessageId.YOU_CANNOT_DISSOLVE_A_CLAN_DURING_A_SIEGE_OR_WHILE_PROTECTING_A_CASTLE);
                  return;
               }
            }

            if (player.isInsideZone(ZoneId.SIEGE)) {
               player.sendPacket(SystemMessageId.YOU_CANNOT_DISSOLVE_A_CLAN_DURING_A_SIEGE_OR_WHILE_PROTECTING_A_CASTLE);
            } else if (clan.getDissolvingExpiryTime() > System.currentTimeMillis()) {
               player.sendPacket(SystemMessageId.YOU_HAVE_ALREADY_REQUESTED_THE_DISSOLUTION_OF_YOUR_CLAN);
            } else {
               clan.setDissolvingExpiryTime(System.currentTimeMillis() + PlayerConfig.ALT_CLAN_DISSOLVE_DAYS * 86400000);
               clan.updateClanInDB();
               player.calculateDeathExpPenalty(null, false);
               ClanTable.getInstance().scheduleRemoveClan(clan.getId());
            }
         } else {
            player.sendPacket(SystemMessageId.YOU_CANNOT_DISSOLVE_A_CLAN_WHILE_OWNING_A_CLAN_HALL_OR_CASTLE);
         }
      }
   }

   private void recoverClan(Player player, int clanId) {
      if (!player.isClanLeader()) {
         player.sendPacket(SystemMessageId.YOU_ARE_NOT_AUTHORIZED_TO_DO_THAT);
      } else {
         Clan clan = player.getClan();
         clan.setDissolvingExpiryTime(0L);
         clan.updateClanInDB();
      }
   }

   private void createSubPledge(Player player, String clanName, String leaderName, int pledgeType, int minClanLvl) {
      if (!player.isClanLeader()) {
         player.sendPacket(SystemMessageId.YOU_ARE_NOT_AUTHORIZED_TO_DO_THAT);
      } else {
         Clan clan = player.getClan();
         if (clan.getLevel() < minClanLvl) {
            if (pledgeType == -1) {
               player.sendPacket(SystemMessageId.TO_ESTABLISH_A_CLAN_ACADEMY_YOUR_CLAN_MUST_BE_LEVEL_5_OR_HIGHER);
            } else {
               player.sendPacket(SystemMessageId.THE_CONDITIONS_NECESSARY_TO_CREATE_A_MILITARY_UNIT_HAVE_NOT_BEEN_MET);
            }
         } else if (StringUtil.isAlphaNumeric(clanName) && isValidName(clanName) && 2 <= clanName.length()) {
            if (clanName.length() > 16) {
               player.sendPacket(SystemMessageId.CLAN_NAME_S_LENGTH_IS_INCORRECT);
            } else {
               for (Clan tempClan : ClanTable.getInstance().getClans()) {
                  if (tempClan.getSubPledge(clanName) != null) {
                     if (pledgeType == -1) {
                        SystemMessage sm = new SystemMessage(SystemMessageId.S1_ALREADY_EXISTS);
                        sm.addString(clanName);
                        player.sendPacket(sm);
                     } else {
                        player.sendPacket(SystemMessageId.ANOTHER_MILITARY_UNIT_IS_ALREADY_USING_THAT_NAME_PLEASE_ENTER_A_DIFFERENT_NAME);
                     }

                     return;
                  }
               }

               if (pledgeType != -1) {
                  ClanMember member = clan.getClanMember(leaderName);
                  if (member == null || member.getPledgeType() != 0 || clan.getLeaderSubPledge(member.getObjectId()) > 0) {
                     if (pledgeType >= 1001) {
                        player.sendPacket(SystemMessageId.THE_CAPTAIN_OF_THE_ORDER_OF_KNIGHTS_CANNOT_BE_APPOINTED);
                     } else if (pledgeType >= 100) {
                        player.sendPacket(SystemMessageId.THE_CAPTAIN_OF_THE_ROYAL_GUARD_CANNOT_BE_APPOINTED);
                     }

                     return;
                  }
               }

               int leaderId = pledgeType != -1 ? clan.getClanMember(leaderName).getObjectId() : 0;
               if (clan.createSubPledge(player, pledgeType, leaderId, clanName) != null) {
                  SystemMessage sm;
                  if (pledgeType == -1) {
                     sm = new SystemMessage(SystemMessageId.CONGRATULATIONS_THE_S1_S_CLAN_ACADEMY_HAS_BEEN_CREATED);
                     sm.addString(player.getClan().getName());
                  } else if (pledgeType >= 1001) {
                     sm = new SystemMessage(SystemMessageId.THE_KNIGHTS_OF_S1_HAVE_BEEN_CREATED);
                     sm.addString(player.getClan().getName());
                  } else if (pledgeType >= 100) {
                     sm = new SystemMessage(SystemMessageId.THE_ROYAL_GUARD_OF_S1_HAVE_BEEN_CREATED);
                     sm.addString(player.getClan().getName());
                  } else {
                     sm = new SystemMessage(SystemMessageId.YOUR_CLAN_HAS_BEEN_CREATED);
                  }

                  player.sendPacket(sm);
                  if (pledgeType != -1) {
                     ClanMember leaderSubPledge = clan.getClanMember(leaderName);
                     Player leaderPlayer = leaderSubPledge.getPlayer();
                     if (leaderPlayer != null) {
                        leaderPlayer.setPledgeClass(ClanMember.calculatePledgeClass(leaderPlayer));
                        leaderPlayer.updateUserInfo();
                     }
                  }
               }
            }
         } else {
            player.sendPacket(SystemMessageId.CLAN_NAME_IS_INVALID);
         }
      }
   }

   private void renameSubPledge(Player player, int pledgeType, String pledgeName) {
      if (!player.isClanLeader()) {
         player.sendPacket(SystemMessageId.YOU_ARE_NOT_AUTHORIZED_TO_DO_THAT);
      } else {
         Clan.SubPledge subPledge = player.getClan().getSubPledge(pledgeType);
         if (subPledge == null) {
            player.sendMessage("Pledge don't exists.");
         } else if (StringUtil.isAlphaNumeric(pledgeName) && isValidName(pledgeName) && 2 <= pledgeName.length()) {
            if (pledgeName.length() > 16) {
               player.sendPacket(SystemMessageId.CLAN_NAME_S_LENGTH_IS_INCORRECT);
            } else {
               subPledge.setName(pledgeName);
               Clan clan = player.getClan();
               clan.updateSubPledgeInDB(subPledge.getId());
               clan.broadcastClanStatus();
               player.sendMessage("Pledge name changed.");
            }
         } else {
            player.sendPacket(SystemMessageId.CLAN_NAME_IS_INVALID);
         }
      }
   }

   private void assignSubPledgeLeader(Player player, String clanName, String leaderName) {
      if (!player.isClanLeader()) {
         player.sendPacket(SystemMessageId.YOU_ARE_NOT_AUTHORIZED_TO_DO_THAT);
      } else if (leaderName.length() > 16) {
         player.sendPacket(SystemMessageId.YOUR_TITLE_CANNOT_EXCEED_16_CHARACTERS_IN_LENGTH_PLEASE_TRY_AGAIN);
      } else if (player.getName().equals(leaderName)) {
         player.sendPacket(SystemMessageId.THE_CAPTAIN_OF_THE_ROYAL_GUARD_CANNOT_BE_APPOINTED);
      } else {
         Clan.SubPledge subPledge = player.getClan().getSubPledge(clanName);
         if (subPledge != null && subPledge.getId() != -1) {
            Clan clan = player.getClan();
            ClanMember member = clan.getClanMember(leaderName);
            if (member != null && member.getPledgeType() == 0 && clan.getLeaderSubPledge(member.getObjectId()) <= 0) {
               subPledge.setLeaderId(member.getObjectId());
               clan.updateSubPledgeInDB(subPledge.getId());
               Player leaderPlayer = member.getPlayer();
               if (leaderPlayer != null) {
                  leaderPlayer.setPledgeClass(ClanMember.calculatePledgeClass(leaderPlayer));
                  leaderPlayer.updateUserInfo();
               }

               clan.broadcastClanStatus();
               SystemMessage sm = new SystemMessage(SystemMessageId.C1_HAS_BEEN_SELECTED_AS_THE_CAPTAIN_OF_S2);
               sm.addString(leaderName);
               sm.addString(clanName);
               clan.broadcastToOnlineMembers(sm);
            } else {
               if (subPledge.getId() >= 1001) {
                  player.sendPacket(SystemMessageId.THE_CAPTAIN_OF_THE_ORDER_OF_KNIGHTS_CANNOT_BE_APPOINTED);
               } else if (subPledge.getId() >= 100) {
                  player.sendPacket(SystemMessageId.THE_CAPTAIN_OF_THE_ROYAL_GUARD_CANNOT_BE_APPOINTED);
               }
            }
         } else {
            player.sendPacket(SystemMessageId.CLAN_NAME_IS_INVALID);
         }
      }
   }

   public static void showPledgeSkillList(Player player) {
      if (!player.isClanLeader()) {
         NpcHtmlMessage html = new NpcHtmlMessage();
         html.setFile(player, "data/html/villagemaster/NotClanLeader.htm");
         player.sendPacket(html);
         player.sendPacket(ActionFailed.STATIC_PACKET);
      } else {
         Clan clan = player.getClan();
         List<SkillLearn> skills = SkillTreeData.getInstance().getAvailablePledgeSkills(clan);
         AcquireSkillList asl = new AcquireSkillList(AcquireSkillType.PLEDGE);
         int counts = 0;

         for (SkillLearn s : skills) {
            asl.addSkill(s.getSkillId(), s.getSkillLevel(), s.getSkillLevel(), s.getLevelUpSp(), s.getSocialClass().ordinal());
            counts++;
         }

         if (counts == 0) {
            if (clan.getLevel() < 8) {
               SystemMessage sm = new SystemMessage(SystemMessageId.YOU_DO_NOT_HAVE_ANY_FURTHER_SKILLS_TO_LEARN_COME_BACK_WHEN_YOU_HAVE_REACHED_LEVEL_S1);
               if (clan.getLevel() < 5) {
                  sm.addInt(5);
               } else {
                  sm.addInt(clan.getLevel() + 1);
               }

               player.sendPacket(sm);
            } else {
               NpcHtmlMessage html = new NpcHtmlMessage();
               html.setFile(player, "data/html/villagemaster/NoMoreSkills.htm");
               player.sendPacket(html);
            }
         } else {
            player.sendPacket(asl);
         }

         player.sendPacket(ActionFailed.STATIC_PACKET);
      }
   }

   private static boolean isValidName(String name) {
      return ServerConfig.CLAN_NAME_TEMPLATE_PATTERN.matcher(name).matches();
   }

   static {
      Set<PlayerClass> subclasses = CategoryData.getInstance()
         .getCategoryByType(CategoryType.THIRD_CLASS_GROUP)
         .stream()
         .map(PlayerClass::getPlayerClass)
         .collect(Collectors.toSet());
      subclasses.removeAll(neverSubclassed);
      mainSubclassSet = subclasses;
      subclassSetMap.put(PlayerClass.DARK_AVENGER, subclasseSet1);
      subclassSetMap.put(PlayerClass.PALADIN, subclasseSet1);
      subclassSetMap.put(PlayerClass.TEMPLE_KNIGHT, subclasseSet1);
      subclassSetMap.put(PlayerClass.SHILLIEN_KNIGHT, subclasseSet1);
      subclassSetMap.put(PlayerClass.TREASURE_HUNTER, subclasseSet2);
      subclassSetMap.put(PlayerClass.ABYSS_WALKER, subclasseSet2);
      subclassSetMap.put(PlayerClass.PLAINS_WALKER, subclasseSet2);
      subclassSetMap.put(PlayerClass.HAWKEYE, subclasseSet3);
      subclassSetMap.put(PlayerClass.SILVER_RANGER, subclasseSet3);
      subclassSetMap.put(PlayerClass.PHANTOM_RANGER, subclasseSet3);
      subclassSetMap.put(PlayerClass.WARLOCK, subclasseSet4);
      subclassSetMap.put(PlayerClass.ELEMENTAL_SUMMONER, subclasseSet4);
      subclassSetMap.put(PlayerClass.PHANTOM_SUMMONER, subclasseSet4);
      subclassSetMap.put(PlayerClass.SORCERER, subclasseSet5);
      subclassSetMap.put(PlayerClass.SPELLSINGER, subclasseSet5);
      subclassSetMap.put(PlayerClass.SPELLHOWLER, subclasseSet5);
   }
}
