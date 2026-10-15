package org.l2jmobius.gameserver.data.xml;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import org.l2jmobius.commons.util.IXmlReader;
import org.l2jmobius.gameserver.config.PlayerConfig;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.enums.creature.Race;
import org.l2jmobius.gameserver.entity.actor.enums.player.PlayerClass;
import org.l2jmobius.gameserver.entity.actor.enums.player.SocialClass;
import org.l2jmobius.gameserver.entity.actor.holders.player.SubClassHolder;
import org.l2jmobius.gameserver.entity.clan.Clan;
import org.l2jmobius.gameserver.entity.item.holders.ItemHolder;
import org.l2jmobius.gameserver.mechanics.skill.CommonSkill;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.mechanics.skill.enums.AcquireSkillType;
import org.l2jmobius.gameserver.mechanics.skill.holders.SkillHolder;
import org.l2jmobius.gameserver.mechanics.skill.holders.SkillLearn;
import org.l2jmobius.gameserver.util.StatSet;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

public class SkillTreeData implements IXmlReader {
   private final Map<PlayerClass, Map<Integer, SkillLearn>> _classSkillTrees = new ConcurrentHashMap<>();
   private final Map<PlayerClass, Map<Integer, SkillLearn>> _completeClassSkillTree = new HashMap<>();
   private final Map<PlayerClass, Map<Integer, SkillLearn>> _transferSkillTrees = new ConcurrentHashMap<>();
   private final Map<Integer, SkillLearn> _collectSkillTree = new ConcurrentHashMap<>();
   private final Map<Integer, SkillLearn> _fishingSkillTree = new ConcurrentHashMap<>();
   private final Map<Integer, SkillLearn> _pledgeSkillTree = new ConcurrentHashMap<>();
   private final Map<Integer, SkillLearn> _subClassSkillTree = new ConcurrentHashMap<>();
   private final Map<Integer, SkillLearn> _subPledgeSkillTree = new ConcurrentHashMap<>();
   private final Map<Integer, SkillLearn> _transformSkillTree = new ConcurrentHashMap<>();
   private final Map<Integer, SkillLearn> _commonSkillTree = new ConcurrentHashMap<>();
   private final Map<Integer, SkillLearn> _nobleSkillTree = new ConcurrentHashMap<>();
   private final Map<Integer, SkillLearn> _heroSkillTree = new ConcurrentHashMap<>();
   private final Map<Integer, SkillLearn> _gameMasterSkillTree = new ConcurrentHashMap<>();
   private final Map<Integer, SkillLearn> _gameMasterAuraSkillTree = new ConcurrentHashMap<>();
   private Map<Integer, int[]> _skillsByClassIdHashCodes;
   private Map<Integer, int[]> _skillsByRaceHashCodes;
   private int[] _allSkillsHashCodes;
   private final Map<PlayerClass, PlayerClass> _parentClassMap = new LinkedHashMap<>();
   private boolean _loading = true;

   protected SkillTreeData() {
      this.load();
   }

   @Override
   public void load() {
      this._loading = true;
      this._parentClassMap.clear();
      this._classSkillTrees.clear();
      this._collectSkillTree.clear();
      this._fishingSkillTree.clear();
      this._pledgeSkillTree.clear();
      this._subClassSkillTree.clear();
      this._subPledgeSkillTree.clear();
      this._transferSkillTrees.clear();
      this._transformSkillTree.clear();
      this._nobleSkillTree.clear();
      this._heroSkillTree.clear();
      this._gameMasterSkillTree.clear();
      this._gameMasterAuraSkillTree.clear();
      this.parseDatapackDirectory("data/stats/players/skillTrees/", true);
      this._completeClassSkillTree.clear();

      for (Entry<PlayerClass, Map<Integer, SkillLearn>> entry : this._classSkillTrees.entrySet()) {
         Map<Integer, SkillLearn> skillTree = new LinkedHashMap<>();
         skillTree.putAll(this._commonSkillTree);
         PlayerClass entryPlayerClass = entry.getKey();
         PlayerClass currentPlayerClass = entryPlayerClass;

         List<PlayerClass> classSequence;
         for (classSequence = new ArrayList<>(); currentPlayerClass != null; currentPlayerClass = this._parentClassMap.get(currentPlayerClass)) {
            classSequence.addFirst(currentPlayerClass);
         }

         for (PlayerClass cid : classSequence) {
            Map<Integer, SkillLearn> classSkillTree = this._classSkillTrees.get(cid);
            if (classSkillTree != null) {
               skillTree.putAll(classSkillTree);
            }
         }

         this._completeClassSkillTree.put(entryPlayerClass, skillTree);
      }

      this.generateCheckArrays();
      this.report();
      this._loading = false;
   }

   @Override
   public void parseDocument(Document document, File file) {
      int cId = -1;
      PlayerClass playerClass = null;

      for (Node n = document.getFirstChild(); n != null; n = n.getNextSibling()) {
         if ("list".equalsIgnoreCase(n.getNodeName())) {
            for (Node d = n.getFirstChild(); d != null; d = d.getNextSibling()) {
               if ("skillTree".equalsIgnoreCase(d.getNodeName())) {
                  Map<Integer, SkillLearn> classSkillTree = new HashMap<>();
                  Map<Integer, SkillLearn> trasferSkillTree = new HashMap<>();
                  String type = d.getAttributes().getNamedItem("type").getNodeValue();
                  Node attr = d.getAttributes().getNamedItem("classId");
                  if (attr != null) {
                     cId = Integer.parseInt(attr.getNodeValue());
                     playerClass = PlayerClass.getPlayerClass(cId);
                  } else {
                     cId = -1;
                  }

                  attr = d.getAttributes().getNamedItem("parentClassId");
                  if (attr != null) {
                     int parentClassId = Integer.parseInt(attr.getNodeValue());
                     if (cId > -1 && cId != parentClassId && parentClassId > -1 && !this._parentClassMap.containsKey(playerClass)) {
                        this._parentClassMap.put(playerClass, PlayerClass.getPlayerClass(parentClassId));
                     }
                  }

                  for (Node c = d.getFirstChild(); c != null; c = c.getNextSibling()) {
                     if ("skill".equalsIgnoreCase(c.getNodeName())) {
                        StatSet learnSkillSet = new StatSet();
                        NamedNodeMap attrs = c.getAttributes();

                        for (int i = 0; i < attrs.getLength(); i++) {
                           attr = attrs.item(i);
                           learnSkillSet.set(attr.getNodeName(), attr.getNodeValue());
                        }

                        SkillLearn skillLearn = new SkillLearn(learnSkillSet);

                        for (Node b = c.getFirstChild(); b != null; b = b.getNextSibling()) {
                           attrs = b.getAttributes();
                           String var16 = b.getNodeName();
                           switch (var16) {
                              case "item":
                                 skillLearn.addRequiredItem(new ItemHolder(this.parseInteger(attrs, "id"), this.parseInteger(attrs, "count").intValue()));
                                 break;
                              case "preRequisiteSkill":
                                 skillLearn.addPreReqSkill(new SkillHolder(this.parseInteger(attrs, "id"), this.parseInteger(attrs, "lvl")));
                                 break;
                              case "race":
                                 skillLearn.addRace(Race.valueOf(b.getTextContent()));
                                 break;
                              case "residenceId":
                                 skillLearn.addResidenceId(Integer.parseInt(b.getTextContent()));
                                 break;
                              case "socialClass":
                                 skillLearn.setSocialClass(Enum.valueOf(SocialClass.class, b.getTextContent()));
                                 break;
                              case "subClassConditions":
                                 skillLearn.addSubclassConditions(this.parseInteger(attrs, "slot"), this.parseInteger(attrs, "lvl"));
                           }
                        }

                        int skillHashCode = SkillData.getSkillHashCode(skillLearn.getSkillId(), skillLearn.getSkillLevel());
                        switch (type) {
                           case "classSkillTree":
                              if (cId != -1) {
                                 classSkillTree.put(skillHashCode, skillLearn);
                              } else {
                                 this._commonSkillTree.put(skillHashCode, skillLearn);
                              }
                              break;
                           case "transferSkillTree":
                              trasferSkillTree.put(skillHashCode, skillLearn);
                              break;
                           case "collectSkillTree":
                              this._collectSkillTree.put(skillHashCode, skillLearn);
                              break;
                           case "fishingSkillTree":
                              this._fishingSkillTree.put(skillHashCode, skillLearn);
                              break;
                           case "pledgeSkillTree":
                              this._pledgeSkillTree.put(skillHashCode, skillLearn);
                              break;
                           case "subClassSkillTree":
                              this._subClassSkillTree.put(skillHashCode, skillLearn);
                              break;
                           case "subPledgeSkillTree":
                              this._subPledgeSkillTree.put(skillHashCode, skillLearn);
                              break;
                           case "transformSkillTree":
                              this._transformSkillTree.put(skillHashCode, skillLearn);
                              break;
                           case "nobleSkillTree":
                              this._nobleSkillTree.put(skillHashCode, skillLearn);
                              break;
                           case "heroSkillTree":
                              this._heroSkillTree.put(skillHashCode, skillLearn);
                              break;
                           case "gameMasterSkillTree":
                              this._gameMasterSkillTree.put(skillHashCode, skillLearn);
                              break;
                           case "gameMasterAuraSkillTree":
                              this._gameMasterAuraSkillTree.put(skillHashCode, skillLearn);
                              break;
                           default:
                              LOGGER.warning(this.getClass().getSimpleName() + ": Unknown Skill Tree type: " + type + "!");
                        }
                     }
                  }

                  if (type.equals("transferSkillTree")) {
                     this._transferSkillTrees.put(playerClass, trasferSkillTree);
                  } else if (type.equals("classSkillTree") && cId > -1) {
                     if (!this._classSkillTrees.containsKey(playerClass)) {
                        this._classSkillTrees.put(playerClass, classSkillTree);
                     } else {
                        this._classSkillTrees.get(playerClass).putAll(classSkillTree);
                     }
                  }
               }
            }
         }
      }
   }

   public Map<Integer, SkillLearn> getCompleteClassSkillTree(PlayerClass playerClass) {
      return this._completeClassSkillTree.getOrDefault(playerClass, Collections.emptyMap());
   }

   public Map<Integer, SkillLearn> getTransferSkillTree(PlayerClass playerClass) {
      return playerClass.level() >= 3 ? this.getTransferSkillTree(playerClass.getParent()) : this._transferSkillTrees.get(playerClass);
   }

   public Map<Integer, SkillLearn> getCommonSkillTree() {
      return this._commonSkillTree;
   }

   public Map<Integer, SkillLearn> getCollectSkillTree() {
      return this._collectSkillTree;
   }

   public Map<Integer, SkillLearn> getFishingSkillTree() {
      return this._fishingSkillTree;
   }

   public Map<Integer, SkillLearn> getPledgeSkillTree() {
      return this._pledgeSkillTree;
   }

   public Map<Integer, SkillLearn> getSubClassSkillTree() {
      return this._subClassSkillTree;
   }

   public Map<Integer, SkillLearn> getSubPledgeSkillTree() {
      return this._subPledgeSkillTree;
   }

   public Map<Integer, SkillLearn> getTransformSkillTree() {
      return this._transformSkillTree;
   }

   public Map<Integer, Skill> getNobleSkillTree() {
      Map<Integer, Skill> tree = new HashMap<>();
      SkillData st = SkillData.getInstance();

      for (Entry<Integer, SkillLearn> e : this._nobleSkillTree.entrySet()) {
         tree.put(e.getKey(), st.getSkill(e.getValue().getSkillId(), e.getValue().getSkillLevel()));
      }

      return tree;
   }

   public Map<Integer, Skill> getHeroSkillTree() {
      Map<Integer, Skill> tree = new HashMap<>();
      SkillData st = SkillData.getInstance();

      for (Entry<Integer, SkillLearn> e : this._heroSkillTree.entrySet()) {
         tree.put(e.getKey(), st.getSkill(e.getValue().getSkillId(), e.getValue().getSkillLevel()));
      }

      return tree;
   }

   public Map<Integer, Skill> getGMSkillTree() {
      Map<Integer, Skill> tree = new HashMap<>();
      SkillData st = SkillData.getInstance();

      for (Entry<Integer, SkillLearn> e : this._gameMasterSkillTree.entrySet()) {
         tree.put(e.getKey(), st.getSkill(e.getValue().getSkillId(), e.getValue().getSkillLevel()));
      }

      return tree;
   }

   public Map<Integer, Skill> getGMAuraSkillTree() {
      Map<Integer, Skill> tree = new HashMap<>();
      SkillData st = SkillData.getInstance();

      for (Entry<Integer, SkillLearn> e : this._gameMasterAuraSkillTree.entrySet()) {
         tree.put(e.getKey(), st.getSkill(e.getValue().getSkillId(), e.getValue().getSkillLevel()));
      }

      return tree;
   }

   public Collection<SkillLearn> getAvailableSkills(Player player, PlayerClass playerClass, boolean includeByFs, boolean includeAutoGet) {
      return this.getAvailableSkills(player, playerClass, includeByFs, includeAutoGet, true, player.getSkills());
   }

   private Collection<SkillLearn> getAvailableSkills(
      Player player, PlayerClass playerClass, boolean includeByFs, boolean includeAutoGet, boolean includeRequiredItems, Map<Integer, Skill> existingSkills
   ) {
      List<SkillLearn> result = new ArrayList<>();
      Map<Integer, SkillLearn> skills = this.getCompleteClassSkillTree(playerClass);
      if (skills.isEmpty()) {
         LOGGER.warning(this.getClass().getSimpleName() + ": Skilltree for class " + playerClass + " is not defined!");
         return result;
      } else {
         for (SkillLearn skill : skills.values()) {
            if ((skill.getSkillId() != CommonSkill.DIVINE_INSPIRATION.getId() || PlayerConfig.AUTO_LEARN_DIVINE_INSPIRATION || !includeAutoGet || player.isGM())
               && (includeAutoGet && skill.isAutoGet() || skill.isLearnedByNpc() || includeByFs && skill.isLearnedByFS())
               && player.getLevel() >= skill.getGetLevel()
               && (includeRequiredItems || skill.getRequiredItems().isEmpty() || skill.isLearnedByFS())) {
               Skill oldSkill = existingSkills.get(skill.getSkillId());
               if (oldSkill != null) {
                  if (oldSkill.getLevel() == skill.getSkillLevel() - 1) {
                     result.add(skill);
                  }
               } else if (skill.getSkillLevel() == 1) {
                  result.add(skill);
               }
            }
         }

         return result;
      }
   }

   public Collection<Skill> getAllAvailableSkills(
      Player player, PlayerClass playerClass, boolean includeByFs, boolean includeAutoGet, boolean includeRequiredItems
   ) {
      Map<Integer, Skill> result = new HashMap<>();

      for (Skill skill : player.getSkills().values()) {
         if (this.isSkillAllowed(player, skill)) {
            result.put(skill.getId(), skill);
         }
      }

      Collection<SkillLearn> previousLearnableSkills = null;

      while (true) {
         Collection<SkillLearn> learnableSkills = this.getAvailableSkills(player, playerClass, includeByFs, includeAutoGet, includeRequiredItems, result);
         if (learnableSkills.isEmpty() || learnableSkills.equals(previousLearnableSkills)) {
            return result.values();
         }

         for (SkillLearn skillLearn : learnableSkills) {
            Skill skillx = SkillData.getInstance().getSkill(skillLearn.getSkillId(), skillLearn.getSkillLevel());
            result.put(skillx.getId(), skillx);
         }

         previousLearnableSkills = learnableSkills;
      }
   }

   public List<SkillLearn> getAvailableAutoGetSkills(Player player) {
      List<SkillLearn> result = new ArrayList<>();
      Map<Integer, SkillLearn> skills = this.getCompleteClassSkillTree(player.getPlayerClass());
      if (skills.isEmpty()) {
         LOGGER.warning(this.getClass().getSimpleName() + ": Skill Tree for this class Id(" + player.getPlayerClass() + ") is not defined!");
         return result;
      } else {
         Race race = player.getRace();

         for (SkillLearn skill : skills.values()) {
            if ((skill.getRaces().isEmpty() || skill.getRaces().contains(race)) && skill.isAutoGet() && player.getLevel() >= skill.getGetLevel()) {
               Skill oldSkill = player.getSkills().get(skill.getSkillId());
               if (oldSkill != null) {
                  if (oldSkill.getLevel() < skill.getSkillLevel()) {
                     result.add(skill);
                  }
               } else {
                  result.add(skill);
               }
            }
         }

         return result;
      }
   }

   public List<SkillLearn> getAvailableFishingSkills(Player player) {
      List<SkillLearn> result = new ArrayList<>();
      Race playerRace = player.getRace();

      for (SkillLearn skill : this._fishingSkillTree.values()) {
         if ((skill.getRaces().isEmpty() || skill.getRaces().contains(playerRace)) && skill.isLearnedByNpc() && player.getLevel() >= skill.getGetLevel()) {
            Skill oldSkill = player.getSkills().get(skill.getSkillId());
            if (oldSkill != null) {
               if (oldSkill.getLevel() == skill.getSkillLevel() - 1) {
                  result.add(skill);
               }
            } else if (skill.getSkillLevel() == 1) {
               result.add(skill);
            }
         }
      }

      return result;
   }

   public List<SkillLearn> getAvailableCollectSkills(Player player) {
      List<SkillLearn> result = new ArrayList<>();

      for (SkillLearn skill : this._collectSkillTree.values()) {
         Skill oldSkill = player.getSkills().get(skill.getSkillId());
         if (oldSkill != null) {
            if (oldSkill.getLevel() == skill.getSkillLevel() - 1) {
               result.add(skill);
            }
         } else if (skill.getSkillLevel() == 1) {
            result.add(skill);
         }
      }

      return result;
   }

   public List<SkillLearn> getAvailableTransferSkills(Player player) {
      List<SkillLearn> result = new ArrayList<>();
      PlayerClass playerClass = player.getPlayerClass();
      if (playerClass.level() == 3) {
         playerClass = playerClass.getParent();
      }

      if (!this._transferSkillTrees.containsKey(playerClass)) {
         return result;
      } else {
         for (SkillLearn skill : this._transferSkillTrees.get(playerClass).values()) {
            if (player.getKnownSkill(skill.getSkillId()) == null) {
               result.add(skill);
            }
         }

         return result;
      }
   }

   public List<SkillLearn> getAvailableTransformSkills(Player player) {
      List<SkillLearn> result = new ArrayList<>();
      Race race = player.getRace();

      for (SkillLearn skill : this._transformSkillTree.values()) {
         if (player.getLevel() >= skill.getGetLevel() && (skill.getRaces().isEmpty() || skill.getRaces().contains(race))) {
            Skill oldSkill = player.getSkills().get(skill.getSkillId());
            if (oldSkill != null) {
               if (oldSkill.getLevel() == skill.getSkillLevel() - 1) {
                  result.add(skill);
               }
            } else if (skill.getSkillLevel() == 1) {
               result.add(skill);
            }
         }
      }

      return result;
   }

   public List<SkillLearn> getAvailablePledgeSkills(Clan clan) {
      List<SkillLearn> result = new ArrayList<>();

      for (SkillLearn skill : this._pledgeSkillTree.values()) {
         if (!skill.isResidencialSkill() && clan.getLevel() >= skill.getGetLevel()) {
            Skill oldSkill = clan.getSkills().get(skill.getSkillId());
            if (oldSkill != null) {
               if (oldSkill.getLevel() + 1 == skill.getSkillLevel()) {
                  result.add(skill);
               }
            } else if (skill.getSkillLevel() == 1) {
               result.add(skill);
            }
         }
      }

      return result;
   }

   public Map<Integer, SkillLearn> getMaxPledgeSkills(Clan clan, boolean includeSquad) {
      Map<Integer, SkillLearn> result = new HashMap<>();

      for (SkillLearn skill : this._pledgeSkillTree.values()) {
         if (!skill.isResidencialSkill() && clan.getLevel() >= skill.getGetLevel()) {
            Skill oldSkill = clan.getSkills().get(skill.getSkillId());
            if (oldSkill == null || oldSkill.getLevel() < skill.getSkillLevel()) {
               result.put(skill.getSkillId(), skill);
            }
         }
      }

      if (includeSquad) {
         for (SkillLearn skillx : this._subPledgeSkillTree.values()) {
            if (clan.getLevel() >= skillx.getGetLevel()) {
               Skill oldSkill = clan.getSkills().get(skillx.getSkillId());
               if (oldSkill == null || oldSkill.getLevel() < skillx.getSkillLevel()) {
                  result.put(skillx.getSkillId(), skillx);
               }
            }
         }
      }

      return result;
   }

   public List<SkillLearn> getAvailableSubPledgeSkills(Clan clan) {
      List<SkillLearn> result = new ArrayList<>();

      for (SkillLearn skill : this._subPledgeSkillTree.values()) {
         if (clan.getLevel() >= skill.getGetLevel() && clan.isLearnableSubSkill(skill.getSkillId(), skill.getSkillLevel())) {
            result.add(skill);
         }
      }

      return result;
   }

   public List<SkillLearn> getAvailableSubClassSkills(Player player) {
      List<SkillLearn> result = new ArrayList<>();

      for (SkillLearn skill : this._subClassSkillTree.values()) {
         if (player.getLevel() >= skill.getGetLevel()) {
            List<SkillLearn.SubClassData> subClassConds = null;

            for (SubClassHolder subClass : player.getSubClasses().values()) {
               subClassConds = skill.getSubClassConditions();
               if (!subClassConds.isEmpty()
                  && subClass.getClassIndex() <= subClassConds.size()
                  && subClass.getClassIndex() == subClassConds.get(subClass.getClassIndex() - 1).getSlot()
                  && subClassConds.get(subClass.getClassIndex() - 1).getLvl() <= subClass.getLevel()) {
                  Skill oldSkill = player.getSkills().get(skill.getSkillId());
                  if (oldSkill != null) {
                     if (oldSkill.getLevel() == skill.getSkillLevel() - 1) {
                        result.add(skill);
                     }
                  } else if (skill.getSkillLevel() == 1) {
                     result.add(skill);
                  }
               }
            }
         }
      }

      return result;
   }

   public List<SkillLearn> getAvailableResidentialSkills(int residenceId) {
      List<SkillLearn> result = new ArrayList<>();

      for (SkillLearn skill : this._pledgeSkillTree.values()) {
         if (skill.isResidencialSkill() && skill.getResidenceIds().contains(residenceId)) {
            result.add(skill);
         }
      }

      return result;
   }

   public SkillLearn getSkillLearn(AcquireSkillType skillType, int id, int lvl, Player player) {
      SkillLearn sl = null;
      switch (skillType) {
         case CLASS:
            sl = this.getClassSkill(id, lvl, player.getLearningClass());
            break;
         case TRANSFORM:
            sl = this.getTransformSkill(id, lvl);
            break;
         case FISHING:
            sl = this.getFishingSkill(id, lvl);
            break;
         case PLEDGE:
            sl = this.getPledgeSkill(id, lvl);
            break;
         case SUBPLEDGE:
            sl = this.getSubPledgeSkill(id, lvl);
            break;
         case TRANSFER:
            sl = this.getTransferSkill(id, lvl, player.getPlayerClass());
            break;
         case SUBCLASS:
            sl = this.getSubClassSkill(id, lvl);
            break;
         case COLLECT:
            sl = this.getCollectSkill(id, lvl);
      }

      return sl;
   }

   public SkillLearn getTransformSkill(int id, int lvl) {
      return this._transformSkillTree.get(SkillData.getSkillHashCode(id, lvl));
   }

   public SkillLearn getClassSkill(int id, int lvl, PlayerClass playerClass) {
      return this.getCompleteClassSkillTree(playerClass).get(SkillData.getSkillHashCode(id, lvl));
   }

   public SkillLearn getFishingSkill(int id, int lvl) {
      return this._fishingSkillTree.get(SkillData.getSkillHashCode(id, lvl));
   }

   public SkillLearn getPledgeSkill(int id, int lvl) {
      return this._pledgeSkillTree.get(SkillData.getSkillHashCode(id, lvl));
   }

   public SkillLearn getSubPledgeSkill(int id, int lvl) {
      return this._subPledgeSkillTree.get(SkillData.getSkillHashCode(id, lvl));
   }

   public SkillLearn getTransferSkill(int id, int lvl, PlayerClass playerClass) {
      if (playerClass.getParent() != null) {
         PlayerClass parentId = playerClass.getParent();
         if (this._transferSkillTrees.get(parentId) != null) {
            return this._transferSkillTrees.get(parentId).get(SkillData.getSkillHashCode(id, lvl));
         }
      }

      return null;
   }

   public SkillLearn getSubClassSkill(int id, int lvl) {
      return this._subClassSkillTree.get(SkillData.getSkillHashCode(id, lvl));
   }

   public SkillLearn getCommonSkill(int id, int lvl) {
      return this._commonSkillTree.get(SkillData.getSkillHashCode(id, lvl));
   }

   public SkillLearn getCollectSkill(int id, int lvl) {
      return this._collectSkillTree.get(SkillData.getSkillHashCode(id, lvl));
   }

   public int getMinLevelForNewSkill(Player player, Map<Integer, SkillLearn> skillTree) {
      int minLevel = 0;
      if (skillTree.isEmpty()) {
         LOGGER.warning(this.getClass().getSimpleName() + ": SkillTree is not defined for getMinLevelForNewSkill!");
      } else {
         for (SkillLearn s : skillTree.values()) {
            if (s.isLearnedByNpc() && player.getLevel() < s.getGetLevel() && (minLevel == 0 || minLevel > s.getGetLevel())) {
               minLevel = s.getGetLevel();
            }
         }
      }

      return minLevel;
   }

   public boolean isHeroSkill(int skillId, int skillLevel) {
      if (this._heroSkillTree.containsKey(SkillData.getSkillHashCode(skillId, skillLevel))) {
         return true;
      } else {
         for (SkillLearn skill : this._heroSkillTree.values()) {
            if (skill.getSkillId() == skillId && skillLevel == -1) {
               return true;
            }
         }

         return false;
      }
   }

   public boolean isGMSkill(int skillId, int skillLevel) {
      if (skillLevel <= 0) {
         return this._gameMasterSkillTree.values().stream().anyMatch(s -> s.getSkillId() == skillId)
            || this._gameMasterAuraSkillTree.values().stream().anyMatch(s -> s.getSkillId() == skillId);
      } else {
         int hashCode = SkillData.getSkillHashCode(skillId, skillLevel);
         return this._gameMasterSkillTree.containsKey(hashCode) || this._gameMasterAuraSkillTree.containsKey(hashCode);
      }
   }

   public boolean isClanSkill(int skillId, int skillLevel) {
      int hashCode = SkillData.getSkillHashCode(skillId, skillLevel);
      return this._pledgeSkillTree.containsKey(hashCode) || this._subPledgeSkillTree.containsKey(hashCode);
   }

   public void addSkills(Player gmchar, boolean auraSkills) {
      Collection<SkillLearn> skills = auraSkills ? this._gameMasterAuraSkillTree.values() : this._gameMasterSkillTree.values();
      SkillData st = SkillData.getInstance();

      for (SkillLearn sl : skills) {
         gmchar.addSkill(st.getSkill(sl.getSkillId(), sl.getSkillLevel()), false);
      }
   }

   private void generateCheckArrays() {
      Set<PlayerClass> playerClassSet = this._classSkillTrees.keySet();
      this._skillsByClassIdHashCodes = new HashMap<>(playerClassSet.size());

      for (PlayerClass playerClass : playerClassSet) {
         int index = 0;
         Map<Integer, SkillLearn> skillLearnMap = new HashMap<>(this.getCompleteClassSkillTree(playerClass));
         int[] skillHashes = new int[skillLearnMap.size()];

         for (int skillHash : skillLearnMap.keySet()) {
            skillHashes[index++] = skillHash;
         }

         skillLearnMap.clear();
         Arrays.sort(skillHashes);
         this._skillsByClassIdHashCodes.put(playerClass.getId(), skillHashes);
      }

      List<Integer> skillHashList = new ArrayList<>();
      this._skillsByRaceHashCodes = new HashMap<>(Race.values().length);

      for (Race race : Race.values()) {
         for (SkillLearn skillLearn : this._fishingSkillTree.values()) {
            if (skillLearn.getRaces().contains(race)) {
               skillHashList.add(SkillData.getSkillHashCode(skillLearn.getSkillId(), skillLearn.getSkillLevel()));
            }
         }

         for (SkillLearn skillLearnx : this._transformSkillTree.values()) {
            if (skillLearnx.getRaces().contains(race)) {
               skillHashList.add(SkillData.getSkillHashCode(skillLearnx.getSkillId(), skillLearnx.getSkillLevel()));
            }
         }

         int index = 0;
         int[] skillHashes = new int[skillHashList.size()];

         for (int skillHash : skillHashList) {
            skillHashes[index++] = skillHash;
         }

         Arrays.sort(skillHashes);
         this._skillsByRaceHashCodes.put(race.ordinal(), skillHashes);
         skillHashList.clear();
      }

      for (SkillLearn skillLearnxx : this._commonSkillTree.values()) {
         if (skillLearnxx.getRaces().isEmpty()) {
            skillHashList.add(SkillData.getSkillHashCode(skillLearnxx.getSkillId(), skillLearnxx.getSkillLevel()));
         }
      }

      for (SkillLearn skillLearnxxx : this._fishingSkillTree.values()) {
         if (skillLearnxxx.getRaces().isEmpty()) {
            skillHashList.add(SkillData.getSkillHashCode(skillLearnxxx.getSkillId(), skillLearnxxx.getSkillLevel()));
         }
      }

      for (SkillLearn skillLearnxxxx : this._transformSkillTree.values()) {
         if (skillLearnxxxx.getRaces().isEmpty()) {
            skillHashList.add(SkillData.getSkillHashCode(skillLearnxxxx.getSkillId(), skillLearnxxxx.getSkillLevel()));
         }
      }

      for (SkillLearn skillLearnxxxxx : this._collectSkillTree.values()) {
         skillHashList.add(SkillData.getSkillHashCode(skillLearnxxxxx.getSkillId(), skillLearnxxxxx.getSkillLevel()));
      }

      this._allSkillsHashCodes = new int[skillHashList.size()];
      int hashIndex = 0;

      for (int skillHash : skillHashList) {
         this._allSkillsHashCodes[hashIndex++] = skillHash;
      }

      Arrays.sort(this._allSkillsHashCodes);
   }

   public boolean isSkillAllowed(Player player, Skill skill) {
      if (skill.isExcludedFromCheck()) {
         return true;
      } else if (player.isGM() && skill.isGMSkill()) {
         return true;
      } else if (this._loading) {
         return true;
      } else {
         int maxLevel = SkillData.getInstance().getMaxLevel(skill.getId());
         int hashCode = SkillData.getSkillHashCode(skill.getId(), Math.min(skill.getLevel(), maxLevel));
         if (Arrays.binarySearch(this._skillsByClassIdHashCodes.get(player.getPlayerClass().getId()), hashCode) >= 0) {
            return true;
         } else if (Arrays.binarySearch(this._skillsByRaceHashCodes.get(player.getRace().ordinal()), hashCode) >= 0) {
            return true;
         } else {
            return Arrays.binarySearch(this._allSkillsHashCodes, hashCode) >= 0
               ? true
               : this.getTransferSkill(skill.getId(), Math.min(skill.getLevel(), maxLevel), player.getPlayerClass()) != null;
         }
      }
   }

   private void report() {
      int classSkillTreeCount = 0;

      for (Map<Integer, SkillLearn> classSkillTree : this._classSkillTrees.values()) {
         classSkillTreeCount += classSkillTree.size();
      }

      int trasferSkillTreeCount = 0;

      for (Map<Integer, SkillLearn> trasferSkillTree : this._transferSkillTrees.values()) {
         trasferSkillTreeCount += trasferSkillTree.size();
      }

      int dwarvenOnlyFishingSkillCount = 0;

      for (SkillLearn fishSkill : this._fishingSkillTree.values()) {
         if (fishSkill.getRaces().contains(Race.DWARF)) {
            dwarvenOnlyFishingSkillCount++;
         }
      }

      int resSkillCount = 0;

      for (SkillLearn pledgeSkill : this._pledgeSkillTree.values()) {
         if (pledgeSkill.isResidencialSkill()) {
            resSkillCount++;
         }
      }

      String className = this.getClass().getSimpleName();
      LOGGER.info(className + ": Loaded " + classSkillTreeCount + " Class Skills for " + this._classSkillTrees.size() + " class skill trees.");
      LOGGER.info(className + ": Loaded " + this._subClassSkillTree.size() + " sub-class skills.");
      LOGGER.info(className + ": Loaded " + trasferSkillTreeCount + " transfer skills for " + this._transferSkillTrees.size() + " transfer skill trees.");
      LOGGER.info(
         className + ": Loaded " + this._fishingSkillTree.size() + " fishing skills, " + dwarvenOnlyFishingSkillCount + " Dwarven only fishing skills."
      );
      LOGGER.info(className + ": Loaded " + this._collectSkillTree.size() + " collect skills.");
      LOGGER.info(
         className
            + ": Loaded "
            + this._pledgeSkillTree.size()
            + " clan skills, "
            + (this._pledgeSkillTree.size() - resSkillCount)
            + " for clan and "
            + resSkillCount
            + " residential."
      );
      LOGGER.info(className + ": Loaded " + this._subPledgeSkillTree.size() + " sub-pledge skills.");
      LOGGER.info(className + ": Loaded " + this._transformSkillTree.size() + " transform skills.");
      LOGGER.info(className + ": Loaded " + this._nobleSkillTree.size() + " noble skills.");
      LOGGER.info(className + ": Loaded " + this._heroSkillTree.size() + " hero skills.");
      LOGGER.info(className + ": Loaded " + this._gameMasterSkillTree.size() + " game master skills.");
      LOGGER.info(className + ": Loaded " + this._gameMasterAuraSkillTree.size() + " game master aura skills.");
      int commonSkills = this._commonSkillTree.size();
      if (commonSkills > 0) {
         LOGGER.info(className + ": Loaded " + commonSkills + " common skills.");
      }
   }

   public static SkillTreeData getInstance() {
      return SkillTreeData.SingletonHolder.INSTANCE;
   }

   private static class SingletonHolder {
      protected static final SkillTreeData INSTANCE = new SkillTreeData();
   }
}
