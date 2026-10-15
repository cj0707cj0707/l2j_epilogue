package org.l2jmobius.gameserver.managers;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.parsers.DocumentBuilderFactory;
import org.l2jmobius.commons.database.DatabaseFactory;
import org.l2jmobius.gameserver.config.custom.RebirthConfig;
import org.l2jmobius.gameserver.data.xml.ExperienceData;
import org.l2jmobius.gameserver.data.xml.SkillData;
import org.l2jmobius.gameserver.data.xml.SkillTreeData;
import org.l2jmobius.gameserver.entity.World;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.enums.player.PlayerClass;
import org.l2jmobius.gameserver.entity.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.network.serverpackets.ExShowScreenMessage;
import org.l2jmobius.gameserver.network.serverpackets.MagicSkillUse;
import org.l2jmobius.gameserver.network.serverpackets.NpcHtmlMessage;
import org.l2jmobius.gameserver.network.serverpackets.SocialAction;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public final class RebirthManager {
   private static final Logger LOGGER = Logger.getLogger(RebirthManager.class.getName());
   private static final int SKILLS_PER_ROW = 3;
   private static final int SKILL_ICON_PADDING_THRESHOLD = 1000;
   private static final String REBIRTH_BUSY_MESSAGE = "A rebirth operation is already in progress.";
   private static final String SELECT_REBIRTH_COUNT = "SELECT rebirthCount FROM rebirth_system WHERE charId=?";
   private static final String INSERT_FIRST_REBIRTH = "INSERT INTO rebirth_system (charId, rebirthCount) VALUES (?,1)";
   private static final String UPDATE_REBIRTH_COUNT = "UPDATE rebirth_system SET rebirthCount=? WHERE charId=?";
   private static final String SELECT_SELECTED_SKILLS = "SELECT selectedSkills FROM rebirth_system WHERE charId=?";
   private static final String UPDATE_SELECTED_SKILLS = "UPDATE rebirth_system SET selectedSkills=? WHERE charId=?";
   private static final String CLASS_LIST_FILE = "data/stats/players/classList.xml";
   private static final Map<Integer, Integer> ROOT_CLASS_IDS = new ConcurrentHashMap<>();
   private static volatile boolean ROOT_CLASS_IDS_LOADED = false;
   private static final Set<Integer> REBIRTH_OPERATIONS_IN_PROGRESS = ConcurrentHashMap.newKeySet();
   private static final Set<Integer> REBIRTH_SKILL_OPERATIONS_IN_PROGRESS = ConcurrentHashMap.newKeySet();

   private boolean isStartingClassSkill(Player player, Skill skill) {
      if (player != null && skill != null) {
         int rootClassId = this.getRootClassId(player.getBaseClass());
         PlayerClass rootClass = PlayerClass.getPlayerClass(rootClassId);
         return rootClass == null ? false : this.isSkillInTree(skill, SkillTreeData.getInstance().getCompleteClassSkillTree(rootClass));
      } else {
         return false;
      }
   }

   private boolean isTransferSkill(Player player, Skill skill) {
      if (player != null && skill != null) {
         PlayerClass playerClass = PlayerClass.getPlayerClass(player.getBaseClass());
         return playerClass == null ? false : this.isSkillInTree(skill, SkillTreeData.getInstance().getTransferSkillTree(playerClass));
      } else {
         return false;
      }
   }

   private boolean isFishingSkill(Skill skill) {
      return this.isSkillInTree(skill, SkillTreeData.getInstance().getFishingSkillTree());
   }

   private boolean isNobleSkill(Skill skill) {
      return this.isSkillInTree(skill, SkillTreeData.getInstance().getNobleSkillTree());
   }

   private boolean isHeroSkill(Skill skill) {
      return this.isSkillInTree(skill, SkillTreeData.getInstance().getHeroSkillTree());
   }

   private boolean isClanSkill(Skill skill) {
      return this.isSkillInTree(skill, SkillTreeData.getInstance().getPledgeSkillTree());
   }

   private boolean isCollectSkill(Skill skill) {
      return this.isSkillInTree(skill, SkillTreeData.getInstance().getCollectSkillTree());
   }

   private boolean isSubClassSkill(Skill skill) {
      return this.isSkillInTree(skill, SkillTreeData.getInstance().getSubClassSkillTree());
   }

   private boolean isSubPledgeSkill(Skill skill) {
      return this.isSkillInTree(skill, SkillTreeData.getInstance().getSubPledgeSkillTree());
   }

   private boolean isTransformSkill(Skill skill) {
      return this.isSkillInTree(skill, SkillTreeData.getInstance().getTransformSkillTree());
   }

   private boolean isProtectedByGroup(Player player, Skill skill) {
      if (player != null && skill != null) {
         for (String group : RebirthConfig.REBIRTH_DELETE_PROTECTED_SKILL_GROUPS) {
            switch (group) {
               case "STARTING_CLASS":
                  if (this.isStartingClassSkill(player, skill)) {
                     return true;
                  }
                  break;
               case "FISHING":
                  if (this.isFishingSkill(skill)) {
                     return true;
                  }
                  break;
               case "NOBLE":
                  if (this.isNobleSkill(skill)) {
                     return true;
                  }
                  break;
               case "HERO":
                  if (this.isHeroSkill(skill)) {
                     return true;
                  }
                  break;
               case "CLAN":
                  if (this.isClanSkill(skill)) {
                     return true;
                  }
                  break;
               case "COLLECT":
                  if (this.isCollectSkill(skill)) {
                     return true;
                  }
                  break;
               case "SUBCLASS":
                  if (this.isSubClassSkill(skill)) {
                     return true;
                  }
                  break;
               case "SUBPLEDGE":
                  if (this.isSubPledgeSkill(skill)) {
                     return true;
                  }
                  break;
               case "TRANSFER":
                  if (this.isTransferSkill(player, skill)) {
                     return true;
                  }
                  break;
               case "TRANSFORM":
                  if (this.isTransformSkill(skill)) {
                     return true;
                  }
            }
         }

         return false;
      } else {
         return false;
      }
   }

   private int getScreenMessagePosition(String position) {
      if (position == null) {
         return 2;
      } else {
         switch (position) {
            case "TOP_LEFT":
               return 1;
            case "TOP_RIGHT":
               return 3;
            case "MIDDLE_LEFT":
               return 4;
            case "MIDDLE_CENTER":
               return 5;
            case "MIDDLE_RIGHT":
               return 6;
            case "BOTTOM_CENTER":
               return 7;
            case "BOTTOM_RIGHT":
               return 8;
            case "TOP_CENTER":
            default:
               return 2;
         }
      }
   }

   private String buildAcquiredSkillsHtml(Player player, List<Integer> selectedSkills, Map<Integer, String> skillDefinitionMap, boolean showResetButton) {
      if (selectedSkills.isEmpty()) {
         return "<center><font color=\"AAAAAA\">You don't have any skills selected yet.</font></center>";
      } else {
         StringBuilder htmlBuilder = new StringBuilder();
         htmlBuilder.append("<table width=270 border=0 cellpadding=2 cellspacing=2><tr>");
         int column = 0;

         for (int skillId : selectedSkills) {
            String definition = this.getSkillDefinition(skillDefinitionMap, skillId);
            if (definition == null) {
               LOGGER.warning(
                  "Selected rebirth skill definition was not found while building acquired skills HTML: playerId="
                     + player.getObjectId()
                     + " playerName="
                     + player.getName()
                     + " skillId="
                     + skillId
               );
            } else {
               int skillLevel = this.getSkillLevel(definition);
               Skill skill = SkillData.getInstance().getSkill(skillId, skillLevel);
               if (skill == null) {
                  LOGGER.warning(
                     "Selected rebirth skill was not found while building acquired skills HTML: playerId="
                        + player.getObjectId()
                        + " playerName="
                        + player.getName()
                        + " skillId="
                        + skillId
                        + " skillLevel="
                        + skillLevel
                        + " definition="
                        + definition
                  );
               } else {
                  int itemCost = this.getSkillCost(definition);
                  if (column == 3) {
                     htmlBuilder.append("</tr><tr>");
                     column = 0;
                  }

                  String icon = skillId < 1000 ? "0" + skillId : String.valueOf(skillId);
                  htmlBuilder.append("<td width=90 align=center>");
                  htmlBuilder.append("<img src=\"icon.skill").append(icon).append("\" width=32 height=32><br1>");
                  htmlBuilder.append("<font color=\"FFDD99\">").append(skill.getName()).append("</font><br1>");
                  htmlBuilder.append("<font color=\"FFFF99\">Lv. ").append(skill.getLevel()).append("</font><br1>");
                  htmlBuilder.append("<font color=\"99FF99\">Cost: ").append(itemCost).append("</font><br1>");
                  if (showResetButton) {
                     htmlBuilder.append("<button value=\"Reset\" action=\"bypass -h rebirth_resetSkill ")
                        .append(skillId)
                        .append("\" width=65 height=18 back=\"L2UI.pledgeButten2\" fore=\"L2UI.pledgeButten1\"><br1>");
                  }

                  htmlBuilder.append("</td>");
                  column++;
               }
            }
         }

         htmlBuilder.append("</tr></table>");
         return htmlBuilder.toString();
      }
   }

   private List<String> getSkillPool(Player player) {
      return player.getPlayerClass().isMage() ? RebirthConfig.REBIRTH_MAGE_SKILLS : RebirthConfig.REBIRTH_FIGHTER_SKILLS;
   }

   private Map<Integer, String> createSkillDefinitionMap(List<String> skillDefinitions, Player player) {
      Map<Integer, String> skillDefinitionMap = new HashMap<>();

      for (String definition : skillDefinitions) {
         int skillId = this.getSkillId(definition);
         if (skillId <= 0) {
            LOGGER.warning(
               "Skipping invalid rebirth skill definition while building index: playerId="
                  + player.getObjectId()
                  + " playerName="
                  + player.getName()
                  + " definition="
                  + definition
            );
         } else {
            String previous = skillDefinitionMap.put(skillId, definition);
            if (previous != null) {
               LOGGER.warning(
                  "Duplicate rebirth skill definition detected while building index: playerId="
                     + player.getObjectId()
                     + " playerName="
                     + player.getName()
                     + " skillId="
                     + skillId
                     + " previousDefinition="
                     + previous
                     + " newDefinition="
                     + definition
               );
            }
         }
      }

      return skillDefinitionMap;
   }

   private Set<Integer> createSkillSet(List<Integer> skills) {
      return new HashSet<>(skills);
   }

   private List<Integer> loadValidatedSelectedSkills(Player player, Map<Integer, String> skillDefinitionMap) {
      int playerId = player.getObjectId();
      List<Integer> storedSkills = this.getSelectedSkills(playerId);
      if (storedSkills.isEmpty()) {
         return storedSkills;
      } else {
         List<Integer> validSkills = new ArrayList<>(storedSkills.size());
         boolean changed = false;

         for (int skillId : storedSkills) {
            if (skillDefinitionMap.containsKey(skillId)) {
               validSkills.add(skillId);
            } else {
               changed = true;
               LOGGER.warning(
                  "Removing stale rebirth skill from persisted selection: playerId=" + playerId + " playerName=" + player.getName() + " skillId=" + skillId
               );
            }
         }

         if (changed) {
            this.saveSelectedSkills(playerId, validSkills);
         }

         return validSkills;
      }
   }

   private String getSkillDefinition(Map<Integer, String> skillDefinitionMap, int skillId) {
      return skillDefinitionMap.get(skillId);
   }

   private int getSkillId(String definition) {
      String[] parts = definition.split(",");
      if (parts.length < 2) {
         return 0;
      } else {
         try {
            return Integer.parseInt(parts[0].trim());
         } catch (NumberFormatException var4) {
            LOGGER.log(Level.WARNING, "Failed to parse rebirth skill id from definition: definition=" + definition, (Throwable)var4);
            return 0;
         }
      }
   }

   private int getSkillLevel(String definition) {
      String[] parts = definition.split(",");
      if (parts.length < 2) {
         return 1;
      } else {
         try {
            return Integer.parseInt(parts[1].trim());
         } catch (NumberFormatException var4) {
            LOGGER.log(Level.WARNING, "Failed to parse rebirth skill level from definition: definition=" + definition, (Throwable)var4);
            return 1;
         }
      }
   }

   private int getSkillCost(String definition) {
      String[] parts = definition.split(",");
      if (parts.length < 3) {
         return 1;
      } else {
         try {
            return Math.max(1, Integer.parseInt(parts[2].trim()));
         } catch (NumberFormatException var4) {
            LOGGER.log(Level.WARNING, "Failed to parse rebirth skill cost from definition: definition=" + definition, (Throwable)var4);
            return 1;
         }
      }
   }

   private int getRefundAmount(int itemCost) {
      String var2 = RebirthConfig.REBIRTH_SKILL_REFUND_MODE;
      switch (var2) {
         case "FULL":
            return itemCost;
         case "MID":
            return itemCost / 2;
         default:
            return 0;
      }
   }

   private long getItemCount(Player player, int itemId) {
      Item item = player.getInventory().getItemByItemId(itemId);
      return item != null ? item.getCount() : 0L;
   }

   public void displaySkillSelectionWindow(Player player, int objectId) {
      int rebirthCount = this.getRebirthLevel(player);
      if (rebirthCount <= 0) {
         player.sendMessage("You must perform at least one Rebirth before selecting skills.");
      } else {
         List<String> skillDefinitions = this.getSkillPool(player);
         Map<Integer, String> skillDefinitionMap = this.createSkillDefinitionMap(skillDefinitions, player);
         if (skillDefinitionMap.isEmpty()) {
            player.sendMessage("There are no skills configured for your class. Contact an administrator.");
         } else {
            List<Integer> selectedSkills = this.loadValidatedSelectedSkills(player, skillDefinitionMap);
            Set<Integer> selectedSkillSet = this.createSkillSet(selectedSkills);
            int allowedSkills = Math.min(rebirthCount, RebirthConfig.REBIRTH_MAX_SELECTED_SKILLS);
            NpcHtmlMessage html = new NpcHtmlMessage(objectId);
            StringBuilder htmlBuilder = new StringBuilder();
            htmlBuilder.append("<html><body><center>");
            htmlBuilder.append("<title>Select your Rebirth Skills</title>");
            htmlBuilder.append("<br><font color=LEVEL>Maximum allowed: ").append(allowedSkills).append(" skill(s)</font><br1>");
            htmlBuilder.append("<font color=99FF99>Tokens available: ")
               .append(this.getItemCount(player, RebirthConfig.REBIRTH_REWARD_ITEM_ID))
               .append("</font><br><br>");
            htmlBuilder.append("<table width=256 cellpadding=1 cellspacing=2><tr>");
            int column = 0;

            for (Entry<Integer, String> entry : skillDefinitionMap.entrySet()) {
               String definition = entry.getValue();
               int skillLevel = this.getSkillLevel(definition);
               if (skillLevel <= 0) {
                  LOGGER.warning(
                     "Skipping invalid rebirth skill definition in selection window: playerId="
                        + player.getObjectId()
                        + " playerName="
                        + player.getName()
                        + " definition="
                        + definition
                  );
               } else {
                  int skillId = entry.getKey();
                  Skill skill = SkillData.getInstance().getSkill(skillId, skillLevel);
                  if (skill == null) {
                     LOGGER.warning(
                        "Configured rebirth skill was not found in selection window: playerId="
                           + player.getObjectId()
                           + " playerName="
                           + player.getName()
                           + " skillId="
                           + skillId
                           + " skillLevel="
                           + skillLevel
                           + " definition="
                           + definition
                     );
                  } else {
                     int itemCost = this.getSkillCost(definition);
                     if (column == 3) {
                        htmlBuilder.append("</tr><tr>");
                        column = 0;
                     }

                     String icon = skillId < 1000 ? "0" + skillId : String.valueOf(skillId);
                     htmlBuilder.append("<td width=90 align=center>");
                     htmlBuilder.append("<img src=\"icon.skill").append(icon).append("\" width=32 height=32><br1>");
                     htmlBuilder.append("<font color=\"FFDD99\">").append(skill.getName()).append("</font><br1>");
                     htmlBuilder.append("<font color=\"FFFF99\">Lv. ").append(skill.getLevel()).append("</font><br1>");
                     htmlBuilder.append("<font color=\"99FF99\">Cost: ").append(itemCost).append("</font><br1>");
                     if (selectedSkillSet.contains(skillId)) {
                        htmlBuilder.append("<font color=AAAAAA>Owned</font>");
                     } else if (selectedSkills.size() >= allowedSkills) {
                        htmlBuilder.append("<font color=AAAAAA>Limit reached</font>");
                     } else {
                        htmlBuilder.append("<button value=\"Choose\" action=\"bypass -h rebirth_previewSkill ")
                           .append(skillId)
                           .append("\" width=40 height=20 back=\"bigbutton_over\" fore=\"bigbutton\">");
                     }

                     htmlBuilder.append("</td>");
                     column++;
                  }
               }
            }

            htmlBuilder.append("</tr></table><br>");
            htmlBuilder.append("<button value=\"Return\" action=\"bypass -h rebirth_openmenu\" width=95 height=20 back=\"bigbutton_over\" fore=\"bigbutton\">");
            htmlBuilder.append("</center></body></html>");
            html.setHtml(htmlBuilder.toString());
            player.sendPacket(html);
         }
      }
   }

   public void displaySkillPreviewWindow(Player player, int objectId, int skillId) {
      List<String> skillDefinitions = this.getSkillPool(player);
      Map<Integer, String> skillDefinitionMap = this.createSkillDefinitionMap(skillDefinitions, player);
      List<Integer> savedSkills = this.loadValidatedSelectedSkills(player, skillDefinitionMap);
      Set<Integer> savedSkillSet = this.createSkillSet(savedSkills);
      int rebirthCount = this.getRebirthLevel(player);
      this.displaySkillPreviewWindow(player, objectId, skillId, skillDefinitionMap, savedSkills, savedSkillSet, rebirthCount);
   }

   public void displayMainWindow(Player player, int objectId) {
      Map<Integer, String> skillDefinitionMap = this.createSkillDefinitionMap(this.getSkillPool(player), player);
      List<Integer> selectedSkills = this.loadValidatedSelectedSkills(player, skillDefinitionMap);
      int currentRebirthCount = this.getRebirthLevel(player);
      this.displayMainWindow(player, objectId, selectedSkills, currentRebirthCount, skillDefinitionMap);
   }

   public void acquireRebirthSkill(Player player, int objectId, int skillId) {
      int playerId = player.getObjectId();
      if (!REBIRTH_SKILL_OPERATIONS_IN_PROGRESS.add(playerId)) {
         player.sendMessage("A rebirth operation is already in progress.");
      } else {
         try {
            Map<Integer, String> skillDefinitionMap = this.createSkillDefinitionMap(this.getSkillPool(player), player);
            List<Integer> savedSkills = this.loadValidatedSelectedSkills(player, skillDefinitionMap);
            int rebirthCount = this.getRebirthLevel(player);
            if (rebirthCount <= 0) {
               player.sendMessage("You must perform at least one Rebirth before selecting skills.");
               this.displayMainWindow(player, objectId, savedSkills, rebirthCount, skillDefinitionMap);
               return;
            }

            Set<Integer> savedSkillSet = this.createSkillSet(savedSkills);
            if (savedSkillSet.contains(skillId)) {
               player.sendMessage("You already own this rebirth skill.");
               this.displayMainWindow(player, objectId, savedSkills, rebirthCount, skillDefinitionMap);
               return;
            }

            int allowedSkills = Math.min(rebirthCount, RebirthConfig.REBIRTH_MAX_SELECTED_SKILLS);
            if (savedSkills.size() >= allowedSkills) {
               player.sendMessage("You have reached the maximum number of skills allowed. (" + allowedSkills + ").");
               this.displayMainWindow(player, objectId, savedSkills, rebirthCount, skillDefinitionMap);
               return;
            }

            String definition = this.getSkillDefinition(skillDefinitionMap, skillId);
            if (definition == null) {
               player.sendMessage("This skill is not configured for your class.");
               this.displayMainWindow(player, objectId, savedSkills, rebirthCount, skillDefinitionMap);
               return;
            }

            int itemCost = this.getSkillCost(definition);
            if (this.consumeItem(player, RebirthConfig.REBIRTH_REWARD_ITEM_ID, itemCost, ItemProcessType.DESTROY, "rebirth token")) {
               savedSkills.add(skillId);
               this.saveSelectedSkills(playerId, savedSkills);
               this.grantRebirthSkills(player, skillDefinitionMap, savedSkills);
               this.playRebirthSelectFx(player);
               player.sendMessage("Rebirth skill purchased successfully.");
               this.displayMainWindow(player, objectId, savedSkills, rebirthCount, skillDefinitionMap);
               return;
            }

            this.displaySkillPreviewWindow(player, objectId, skillId, skillDefinitionMap, savedSkills, savedSkillSet, rebirthCount);
         } finally {
            REBIRTH_SKILL_OPERATIONS_IN_PROGRESS.remove(playerId);
         }
      }
   }

   public void resetRebirthSkill(Player player, int objectId, int skillId) {
      int playerId = player.getObjectId();
      if (!REBIRTH_SKILL_OPERATIONS_IN_PROGRESS.add(playerId)) {
         player.sendMessage("A rebirth operation is already in progress.");
      } else {
         try {
            Map<Integer, String> skillDefinitionMap = this.createSkillDefinitionMap(this.getSkillPool(player), player);
            List<Integer> savedSkills = this.loadValidatedSelectedSkills(player, skillDefinitionMap);
            Set<Integer> savedSkillSet = this.createSkillSet(savedSkills);
            int rebirthCount = this.getRebirthLevel(player);
            if (!savedSkillSet.contains(skillId)) {
               player.sendMessage("You do not own this rebirth skill.");
               this.displayMainWindow(player, objectId, savedSkills, rebirthCount, skillDefinitionMap);
               return;
            }

            String definition = this.getSkillDefinition(skillDefinitionMap, skillId);
            if (definition != null) {
               int skillLevel = this.getSkillLevel(definition);
               int itemCost = this.getSkillCost(definition);
               int refundAmount = this.getRefundAmount(itemCost);
               savedSkills.remove(Integer.valueOf(skillId));
               this.saveSelectedSkills(playerId, savedSkills);
               Skill skill = SkillData.getInstance().getSkill(skillId, skillLevel);
               if (skill != null) {
                  player.removeSkill(skill, true);
               } else {
                  LOGGER.warning(
                     "Configured rebirth skill could not be removed because it was not found: playerId="
                        + playerId
                        + " playerName="
                        + player.getName()
                        + " skillId="
                        + skillId
                        + " skillLevel="
                        + skillLevel
                        + " definition="
                        + definition
                  );
               }

               player.sendSkillList();
               if (refundAmount > 0) {
                  player.addItem(ItemProcessType.REFUND, RebirthConfig.REBIRTH_REWARD_ITEM_ID, refundAmount, player, true);
                  player.sendMessage("Refunded " + refundAmount + " rebirth token(s).");
               } else {
                  player.sendMessage("Skill removed. Refund mode " + RebirthConfig.REBIRTH_SKILL_REFUND_MODE + " returned no tokens.");
               }

               this.displayMainWindow(player, objectId, savedSkills, rebirthCount, skillDefinitionMap);
               return;
            }

            player.sendMessage("The configured skill data is invalid.");
            this.displayMainWindow(player, objectId, savedSkills, rebirthCount, skillDefinitionMap);
         } finally {
            REBIRTH_SKILL_OPERATIONS_IN_PROGRESS.remove(playerId);
         }
      }
   }

   public void requestRebirth(Player player) {
      int playerId = player.getObjectId();
      if (!REBIRTH_OPERATIONS_IN_PROGRESS.add(playerId)) {
         player.sendMessage("A rebirth operation is already in progress.");
      } else {
         try {
            if (!RebirthConfig.REBIRTH_ALLOW_REBIRTH) {
               player.sendMessage("Rebirth is disabled by configuration.");
               return;
            }

            if (player.getLevel() < RebirthConfig.REBIRTH_MIN_LEVEL) {
               player.sendMessage("You do not meet the minimum level for Rebirth (" + RebirthConfig.REBIRTH_MIN_LEVEL + ").");
               return;
            }

            if (player.isAlikeDead() || player.isCastingNow() || player.isAttackingNow()) {
               player.sendMessage("You cannot request Rebirth right now.");
               return;
            }

            int currentRebirthCount = this.getRebirthLevel(player);
            if (currentRebirthCount >= RebirthConfig.REBIRTH_MAX_COUNT) {
               player.sendMessage("You are currently at your maximum rebirth count.");
               return;
            }

            if (this.consumeItem(player, RebirthConfig.REBIRTH_ITEM_ID, RebirthConfig.REBIRTH_ITEM_AMOUNT, ItemProcessType.DESTROY, "rebirth request")) {
               this.grantRebirth(player, currentRebirthCount + 1, currentRebirthCount == 0);
               return;
            }
         } finally {
            REBIRTH_OPERATIONS_IN_PROGRESS.remove(playerId);
         }
      }
   }

   private void grantRebirth(Player player, int newCount, boolean firstBirth) {
      int playerId = player.getObjectId();
      String playerName = player.getName();

      try {
         boolean updated = firstBirth ? this.insertFirst(playerId) : this.updateCount(playerId, newCount);
         if (!updated) {
            player.sendMessage("Rebirth could not be completed due to a database error.");
            LOGGER.severe(
               "Aborting rebirth because rebirth count persistence failed: playerId="
                  + playerId
                  + " playerName="
                  + playerName
                  + " newCount="
                  + newCount
                  + " firstBirth="
                  + firstBirth
            );
            return;
         }

         long currentExp = player.getExp();
         long targetExp = ExperienceData.getInstance().getExpForLevel(1);
         if (currentExp > targetExp) {
            player.getStat().setLevel((byte)1);
            player.removeExpAndSp(currentExp - targetExp, 0L);
            player.setCurrentHpMp(player.getMaxHp(), player.getMaxMp());
            player.setCurrentCp(player.getMaxCp());
            player.broadcastUserInfo();
         }

         player.setPlayerClass(player.getBaseClass());
         if (RebirthConfig.REBIRTH_DELETE_ALL_SKILLS) {
            for (Skill skill : player.getAllSkills()) {
               if (skill != null && !RebirthConfig.REBIRTH_SKILL_DELETE_WHITELIST.contains(skill.getId()) && !this.isProtectedByGroup(player, skill)) {
                  player.removeSkill(skill, true);
               }
            }
         }

         player.giveAvailableSkills(true, true, true);
         if (RebirthConfig.REBIRTH_DELETE_RESTORE_TEMPORARY_SKILLS) {
            player.regiveTemporarySkills();
         }

         player.storeMe();
      } catch (Exception var14) {
         LOGGER.log(
            Level.SEVERE,
            "Failed during critical rebirth section: playerId="
               + playerId
               + " playerName="
               + playerName
               + " newCount="
               + newCount
               + " firstBirth="
               + firstBirth,
            (Throwable)var14
         );
         player.sendMessage("Rebirth could not be completed due to an internal error.");
         return;
      }

      try {
         Map<Integer, String> skillDefinitionMap = this.createSkillDefinitionMap(this.getSkillPool(player), player);
         List<Integer> selectedSkills = this.loadValidatedSelectedSkills(player, skillDefinitionMap);
         this.grantRewardTokens(player);
         if (!RebirthConfig.REBIRTH_DELETE_RESTORE_TEMPORARY_SKILLS) {
            this.grantRebirthSkills(player, skillDefinitionMap, selectedSkills);
         }

         player.sendSkillList();
         player.broadcastUserInfo();
         player.broadcastStatusUpdate();
         this.displayCongrats(player, newCount);
         player.sendMessage("Rebirth applied successfully. You have been reborn!");
         this.routePostRebirthUi(player, selectedSkills, newCount, skillDefinitionMap);
      } catch (Exception var13) {
         LOGGER.log(
            Level.SEVERE,
            "Failed during post-commit rebirth section: playerId=" + playerId + " playerName=" + playerName + " newCount=" + newCount,
            (Throwable)var13
         );
      }
   }

   private boolean isSkillInTree(Skill skill, Map<?, ?> tree) {
      if (skill != null && tree != null && !tree.isEmpty()) {
         long skillHashCode = SkillData.getSkillHashCode(skill.getId(), skill.getLevel());
         return tree.containsKey((int)skillHashCode) || tree.containsKey(skillHashCode);
      } else {
         return false;
      }
   }

   private int getRootClassId(int classId) {
      this.ensureRootClassIdsLoaded();
      Integer rootClassId = ROOT_CLASS_IDS.get(classId);
      if (rootClassId != null) {
         return rootClassId;
      } else {
         PlayerClass playerClass = PlayerClass.getPlayerClass(classId);
         return playerClass != null ? playerClass.getRootClass().getId() : classId;
      }
   }

   private void ensureRootClassIdsLoaded() {
      if (!ROOT_CLASS_IDS_LOADED) {
         synchronized (ROOT_CLASS_IDS) {
            if (!ROOT_CLASS_IDS_LOADED) {
               this.loadRootClassIds();
               ROOT_CLASS_IDS_LOADED = true;
            }
         }
      }
   }

   private void loadRootClassIds() {
      File file = new File("data/stats/players/classList.xml");
      if (!file.exists()) {
         LOGGER.warning("Rebirth class list file not found: data/stats/players/classList.xml");
      } else {
         Map<Integer, Integer> parentClassIds = new HashMap<>();

         try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            NodeList classNodes = factory.newDocumentBuilder().parse(file).getElementsByTagName("class");

            for (int i = 0; i < classNodes.getLength(); i++) {
               Node classNode = classNodes.item(i);
               int classId = this.parseIntAttribute(classNode, "classId", -1);
               if (classId >= 0) {
                  parentClassIds.put(classId, this.parseIntAttribute(classNode, "parentClassId", -1));
               }
            }

            for (Integer classId : parentClassIds.keySet()) {
               ROOT_CLASS_IDS.put(classId, this.resolveRootClassId(classId, parentClassIds));
            }
         } catch (Exception var8) {
            LOGGER.log(Level.WARNING, "Failed to parse rebirth class list file: data/stats/players/classList.xml", (Throwable)var8);
         }
      }
   }

   private int resolveRootClassId(int classId, Map<Integer, Integer> parentClassIds) {
      int currentClassId = classId;
      Set<Integer> visited = new HashSet<>();

      while (visited.add(currentClassId)) {
         Integer parentClassId = parentClassIds.get(currentClassId);
         if (parentClassId == null || parentClassId < 0) {
            return currentClassId;
         }

         currentClassId = parentClassId;
      }

      return classId;
   }

   private int parseIntAttribute(Node node, String attributeName, int defaultValue) {
      if (node != null && node.getAttributes() != null && node.getAttributes().getNamedItem(attributeName) != null) {
         try {
            return Integer.parseInt(node.getAttributes().getNamedItem(attributeName).getNodeValue());
         } catch (NumberFormatException var5) {
            LOGGER.log(
               Level.WARNING,
               "Failed to parse rebirth class attribute: attribute="
                  + attributeName
                  + " value="
                  + node.getAttributes().getNamedItem(attributeName).getNodeValue(),
               (Throwable)var5
            );
            return defaultValue;
         }
      } else {
         return defaultValue;
      }
   }

   private void grantRewardTokens(Player player) {
      if (RebirthConfig.REBIRTH_REWARD_ITEM_ID > 0 && RebirthConfig.REBIRTH_REWARD_ITEM_AMOUNT > 0) {
         player.addItem(ItemProcessType.REWARD, RebirthConfig.REBIRTH_REWARD_ITEM_ID, RebirthConfig.REBIRTH_REWARD_ITEM_AMOUNT, player, true);
         player.sendMessage("You received " + RebirthConfig.REBIRTH_REWARD_ITEM_AMOUNT + " rebirth token(s).");
      }
   }

   private void displayCongrats(Player player, int rebirthCount) {
      if (RebirthConfig.REBIRTH_FINISH_SOCIAL_ID > 0) {
         player.broadcastPacket(new SocialAction(player.getObjectId(), RebirthConfig.REBIRTH_FINISH_SOCIAL_ID));
      }

      if (RebirthConfig.REBIRTH_FINISH_EFFECT_SKILL_ID > 0) {
         player.broadcastPacket(
            new MagicSkillUse(player, player, RebirthConfig.REBIRTH_FINISH_EFFECT_SKILL_ID, RebirthConfig.REBIRTH_FINISH_EFFECT_SKILL_LVL, 1, 0)
         );
      }

      String screenMessage = RebirthConfig.REBIRTH_SCREEN_MESSAGE
         .replace("%player%", player.getName())
         .replace("%rebirth_count%", String.valueOf(rebirthCount))
         .replace("%max_rebirth%", String.valueOf(RebirthConfig.REBIRTH_MAX_COUNT));
      String globalAnnouncement = RebirthConfig.REBIRTH_GLOBAL_ANNOUNCEMENT
         .replace("%player%", player.getName())
         .replace("%rebirth_count%", String.valueOf(rebirthCount))
         .replace("%max_rebirth%", String.valueOf(RebirthConfig.REBIRTH_MAX_COUNT));
      if (RebirthConfig.REBIRTH_SCREEN_MESSAGE_ENABLED) {
         player.sendPacket(
            new ExShowScreenMessage(
               screenMessage, this.getScreenMessagePosition(RebirthConfig.REBIRTH_SCREEN_MESSAGE_POSITION), RebirthConfig.REBIRTH_SCREEN_MESSAGE_TIME
            )
         );
      }

      if (RebirthConfig.REBIRTH_GLOBAL_ANNOUNCEMENT_ENABLED) {
         World.broadcastToAllOnlinePlayers(globalAnnouncement, RebirthConfig.REBIRTH_GLOBAL_ANNOUNCEMENT_CRITICAL);
      }

      player.sendMessage("Congratulations " + player.getName() + "! You have been Reborn!");
   }

   private void routePostRebirthUi(Player player, List<Integer> selectedSkills, int rebirthCount, Map<Integer, String> skillDefinitionMap) {
      this.displayMainWindow(player, 0, selectedSkills, rebirthCount, skillDefinitionMap);
      player.sendMessage("Use your rebirth tokens to purchase skills.");
   }

   public void playRebirthSelectFx(Player player) {
      if (RebirthConfig.REBIRTH_SELECT_SOCIAL_ID > 0) {
         player.broadcastPacket(new SocialAction(player.getObjectId(), RebirthConfig.REBIRTH_SELECT_SOCIAL_ID));
      }

      if (RebirthConfig.REBIRTH_SELECT_EFFECT_SKILL_ID > 0) {
         player.broadcastPacket(
            new MagicSkillUse(player, player, RebirthConfig.REBIRTH_SELECT_EFFECT_SKILL_ID, RebirthConfig.REBIRTH_SELECT_EFFECT_SKILL_LVL, 1, 0)
         );
      }
   }

   private boolean consumeItem(Player player, int itemId, long amount, ItemProcessType processType, String reason) {
      if (player != null && itemId > 0 && amount > 0L && amount <= 2147483647L) {
         Item item = player.getInventory().getItemByItemId(itemId);
         if (item != null && item.getCount() >= amount) {
            player.getInventory().destroyItem(processType, item, (int)amount, player, null);
            return true;
         } else {
            String itemName = item != null && item.getTemplate() != null ? item.getTemplate().getName() : "Item " + itemId;
            player.sendMessage("You need at least " + amount + " [ " + itemName + " ] for " + reason + ".");
            return false;
         }
      } else {
         LOGGER.warning(
            "Invalid consumeItem request: player=" + player + " itemId=" + itemId + " amount=" + amount + " processType=" + processType + " reason=" + reason
         );
         return false;
      }
   }

   public void grantRebirthSkills(Player player) {
      Map<Integer, String> skillDefinitionMap = this.createSkillDefinitionMap(this.getSkillPool(player), player);
      this.grantRebirthSkills(player, skillDefinitionMap, this.loadValidatedSelectedSkills(player, skillDefinitionMap));
   }

   private void grantRebirthSkills(Player player, Map<Integer, String> skillDefinitionMap, List<Integer> selectedSkills) {
      if (RebirthConfig.REBIRTH_INHERIT_SKILLS_TO_SUBCLASSES || player.getClassIndex() <= 0) {
         for (int skillId : selectedSkills) {
            String definition = this.getSkillDefinition(skillDefinitionMap, skillId);
            if (definition == null) {
               LOGGER.warning(
                  "Skipping selected rebirth skill because definition was not found: playerId="
                     + player.getObjectId()
                     + " playerName="
                     + player.getName()
                     + " skillId="
                     + skillId
               );
            } else {
               Skill skill = SkillData.getInstance().getSkill(skillId, this.getSkillLevel(definition));
               if (skill != null) {
                  player.addSkill(skill, true);
               } else {
                  LOGGER.warning(
                     "Skipping selected rebirth skill because skill data was not found: playerId="
                        + player.getObjectId()
                        + " playerName="
                        + player.getName()
                        + " skillId="
                        + skillId
                        + " definition="
                        + definition
                  );
               }
            }
         }

         player.sendSkillList();
      }
   }

   public int getRebirthLevel(Player player) {
      return this.getCount(player.getObjectId());
   }

   public int getCount(int charId) {
      int count = 0;

      try (
         Connection connection = DatabaseFactory.getConnection();
         PreparedStatement statement = connection.prepareStatement("SELECT rebirthCount FROM rebirth_system WHERE charId=?");
      ) {
         statement.setInt(1, charId);

         try (ResultSet resultSet = statement.executeQuery()) {
            if (resultSet.next()) {
               count = resultSet.getInt(1);
            }
         }
      } catch (Exception var14) {
         LOGGER.log(Level.SEVERE, "Failed to load rebirth count: charId=" + charId, (Throwable)var14);
      }

      return count;
   }

   public boolean insertFirst(int charId) {
      try {
         boolean var4;
         try (
            Connection connection = DatabaseFactory.getConnection();
            PreparedStatement statement = connection.prepareStatement("INSERT INTO rebirth_system (charId, rebirthCount) VALUES (?,1)");
         ) {
            statement.setInt(1, charId);
            var4 = statement.executeUpdate() > 0;
         }

         return var4;
      } catch (Exception var10) {
         LOGGER.log(Level.SEVERE, "Failed to insert first rebirth record: charId=" + charId, (Throwable)var10);
         return false;
      }
   }

   public boolean updateCount(int charId, int newCount) {
      try {
         boolean var5;
         try (
            Connection connection = DatabaseFactory.getConnection();
            PreparedStatement statement = connection.prepareStatement("UPDATE rebirth_system SET rebirthCount=? WHERE charId=?");
         ) {
            statement.setInt(1, newCount);
            statement.setInt(2, charId);
            var5 = statement.executeUpdate() > 0;
         }

         return var5;
      } catch (Exception var11) {
         LOGGER.log(Level.SEVERE, "Failed to update rebirth count: charId=" + charId + " newCount=" + newCount, (Throwable)var11);
         return false;
      }
   }

   public List<Integer> getSelectedSkills(int charId) {
      List<Integer> skills = new ArrayList<>();

      try (
         Connection connection = DatabaseFactory.getConnection();
         PreparedStatement statement = connection.prepareStatement("SELECT selectedSkills FROM rebirth_system WHERE charId=?");
      ) {
         statement.setInt(1, charId);

         try (ResultSet resultSet = statement.executeQuery()) {
            if (resultSet.next()) {
               String data = resultSet.getString(1);
               if (data != null && !data.isEmpty()) {
                  for (String skillValue : data.split(",")) {
                     try {
                        skills.add(Integer.parseInt(skillValue.trim()));
                     } catch (NumberFormatException var15) {
                        LOGGER.log(
                           Level.WARNING,
                           "Failed to parse persisted rebirth skill id: charId=" + charId + " rawValue=" + skillValue + " data=" + data,
                           (Throwable)var15
                        );
                     }
                  }
               }
            }
         }
      } catch (Exception var19) {
         LOGGER.log(Level.SEVERE, "Failed to load selected rebirth skills: charId=" + charId, (Throwable)var19);
      }

      return skills;
   }

   public void saveSelectedSkills(int charId, List<Integer> skills) {
      try (
         Connection connection = DatabaseFactory.getConnection();
         PreparedStatement statement = connection.prepareStatement("UPDATE rebirth_system SET selectedSkills=? WHERE charId=?");
      ) {
         StringBuilder skillBuilder = new StringBuilder();

         for (int i = 0; i < skills.size(); i++) {
            if (i > 0) {
               skillBuilder.append(',');
            }

            skillBuilder.append(skills.get(i));
         }

         statement.setString(1, skillBuilder.toString());
         statement.setInt(2, charId);
         statement.executeUpdate();
      } catch (Exception var11) {
         LOGGER.log(Level.SEVERE, "Failed to save selected rebirth skills: charId=" + charId + " skills=" + skills, (Throwable)var11);
      }
   }

   private void displaySkillPreviewWindow(
      Player player,
      int objectId,
      int skillId,
      Map<Integer, String> skillDefinitionMap,
      List<Integer> savedSkills,
      Set<Integer> savedSkillSet,
      int rebirthCount
   ) {
      String definition = this.getSkillDefinition(skillDefinitionMap, skillId);
      if (definition == null) {
         player.sendMessage("Invalid skill.");
      } else {
         int targetLevel = this.getSkillLevel(definition);
         Skill skill = SkillData.getInstance().getSkill(skillId, targetLevel);
         if (skill == null) {
            LOGGER.warning(
               "Configured rebirth skill preview was not found: playerId="
                  + player.getObjectId()
                  + " playerName="
                  + player.getName()
                  + " skillId="
                  + skillId
                  + " targetLevel="
                  + targetLevel
                  + " definition="
                  + definition
            );
            player.sendMessage("Invalid skill.");
         } else {
            int itemCost = this.getSkillCost(definition);
            int allowedSkills = Math.min(rebirthCount, RebirthConfig.REBIRTH_MAX_SELECTED_SKILLS);
            long availableTokens = this.getItemCount(player, RebirthConfig.REBIRTH_REWARD_ITEM_ID);
            boolean alreadySelected = savedSkillSet.contains(skillId);
            boolean limitReached = savedSkills.size() >= allowedSkills;
            boolean hasEnoughTokens = availableTokens >= itemCost;
            NpcHtmlMessage html = new NpcHtmlMessage(objectId);
            StringBuilder htmlBuilder = new StringBuilder();
            htmlBuilder.append("<html><body><center>");
            htmlBuilder.append("<br>");
            htmlBuilder.append("<font color=\"LEVEL\">Rebirth Manager</font><br>");
            htmlBuilder.append("<table width=256 border=0 cellpadding=1 cellspacing=2>");
            htmlBuilder.append("<tr><td width=95 align=center>");
            htmlBuilder.append("<font color=\"66CC66\">Your Rebirths</font><br1>");
            htmlBuilder.append("<font color=\"FFFF99\">").append(rebirthCount).append('/').append(RebirthConfig.REBIRTH_MAX_COUNT).append("</font><br1>");
            htmlBuilder.append("<font color=\"99FF99\">Tokens: ").append(availableTokens).append("</font>");
            htmlBuilder.append("</td><td width=95 align=center>");
            String icon = skillId < 1000 ? "0" + skillId : String.valueOf(skillId);
            htmlBuilder.append("<img src=\"icon.skill").append(icon).append("\" width=32 height=32><br1>");
            htmlBuilder.append("<font color=\"FFDD99\">").append(skill.getName()).append("</font><br1>");
            htmlBuilder.append("<font color=\"FFFF99\">Lv. ").append(skill.getLevel()).append("</font><br1>");
            htmlBuilder.append("<font color=\"99FF99\">Cost: ").append(itemCost).append("</font>");
            htmlBuilder.append("</td></tr></table>");
            htmlBuilder.append("<br1>");
            htmlBuilder.append("<font color=\"LEVEL\">This purchase consumes ").append(itemCost).append(" token(s).</font><br1>");
            htmlBuilder.append("<font color=\"LEVEL\">Refund: ").append(RebirthConfig.REBIRTH_SKILL_REFUND_MODE).append("</font><br1>");
            htmlBuilder.append("<font color=\"66CC66\">Rebirth Skills Obtained</font><br1>");
            htmlBuilder.append(this.buildAcquiredSkillsHtml(player, savedSkills, skillDefinitionMap, false));
            htmlBuilder.append("<br1>");
            if (!alreadySelected && !limitReached && hasEnoughTokens) {
               htmlBuilder.append("<button value=\"Confirm\" action=\"bypass -h rebirth_confirmSkill ")
                  .append(skillId)
                  .append("\" width=95 height=20 back=\"L2UI.pledgeButten2\" fore=\"L2UI.pledgeButten1\"><br1>");
            } else {
               if (alreadySelected) {
                  htmlBuilder.append("<font color=AAAAAA>Already selected.</font><br1>");
               }

               if (limitReached) {
                  htmlBuilder.append("<font color=AAAAAA>Skill limit reached.</font><br1>");
               }

               if (!hasEnoughTokens) {
                  htmlBuilder.append("<font color=FF6666>Not enough tokens.</font><br1>");
               }
            }

            htmlBuilder.append(
               "<button value=\"Return\" action=\"bypass -h rebirth_selectskills\" width=95 height=20 back=\"L2UI.pledgeButten2\" fore=\"L2UI.pledgeButten1\">"
            );
            htmlBuilder.append("</center></body></html>");
            html.setHtml(htmlBuilder.toString());
            player.sendPacket(html);
         }
      }
   }

   private void displayMainWindow(Player player, int objectId, List<Integer> selectedSkills, int rebirthCount, Map<Integer, String> skillDefinitionMap) {
      long tokenCount = this.getItemCount(player, RebirthConfig.REBIRTH_REWARD_ITEM_ID);
      NpcHtmlMessage html = new NpcHtmlMessage(objectId);
      html.setFile(player, "data/html/default/70001_dynamic.htm");
      html.replace("%rebirth_count%", rebirthCount + "/" + RebirthConfig.REBIRTH_MAX_COUNT);
      html.replace("%rebirth_icons%", this.buildAcquiredSkillsHtml(player, selectedSkills, skillDefinitionMap, true));
      html.replace("%next_skill_icon%", "<img src=\"icon.etc_coins_gold_i00\" width=32 height=32>");
      html.replace("%next_skill_name%", "Rebirth Tokens");
      html.replace("%next_skill_lvl%", String.valueOf(tokenCount));
      html.replace("%next_skill_desc%", "Refund mode: " + RebirthConfig.REBIRTH_SKILL_REFUND_MODE);
      if (rebirthCount < RebirthConfig.REBIRTH_MAX_COUNT) {
         html.replace(
            "%rebirth_button%",
            "<button value=\"Request Rebirth\" action=\"bypass -h rebirth_confirmrequest\" width=95 height=20 back=\"L2UI.pledgeButten2\" fore=\"L2UI.pledgeButten1\">"
         );
      } else {
         html.replace("%rebirth_button%", "<font color=AAAAAA>Max Rebirth reached.</font>");
      }

      player.sendPacket(html);
   }

   public static RebirthManager getInstance() {
      return RebirthManager.SingletonHolder.INSTANCE;
   }

   private static class SingletonHolder {
      protected static final RebirthManager INSTANCE = new RebirthManager();
   }
}
