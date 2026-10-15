package org.l2jmobius.gameserver.util;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import org.l2jmobius.commons.util.StringUtil;
import org.l2jmobius.gameserver.data.xml.EnchantSkillGroupsData;
import org.l2jmobius.gameserver.mechanics.conditions.Condition;
import org.l2jmobius.gameserver.mechanics.skill.EffectScope;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

public class DocumentSkill extends DocumentBase {
   private DocumentSkill.DocumentSkillDataHolder _currentSkill;
   private final List<Skill> _skillsInFile = new ArrayList<>();

   public DocumentSkill(File file) {
      super(file);
   }

   private void setCurrentSkill(DocumentSkill.DocumentSkillDataHolder skill) {
      this._currentSkill = skill;
   }

   @Override
   protected StatSet getStatSet() {
      return this._currentSkill.sets[this._currentSkill.currentLevel];
   }

   @Override
   protected String getTableValue(String name) {
      try {
         return this._tables.get(name)[this._currentSkill.currentLevel];
      } catch (RuntimeException var3) {
         this.LOGGER.log(Level.SEVERE, "Error in table: " + name + " of Skill Id " + this._currentSkill.id, (Throwable)var3);
         return "";
      }
   }

   @Override
   protected String getTableValue(String name, int idx) {
      try {
         return this._tables.get(name)[idx - 1];
      } catch (RuntimeException var4) {
         this.LOGGER.log(Level.SEVERE, "Wrong level count in skill Id " + this._currentSkill.id + " name: " + name + " index : " + idx, (Throwable)var4);
         return "";
      }
   }

   @Override
   protected void parseDocument(Document document) {
      for (Node n = document.getFirstChild(); n != null; n = n.getNextSibling()) {
         if ("list".equalsIgnoreCase(n.getNodeName())) {
            for (Node d = n.getFirstChild(); d != null; d = d.getNextSibling()) {
               if ("skill".equalsIgnoreCase(d.getNodeName())) {
                  this.setCurrentSkill(new DocumentSkill.DocumentSkillDataHolder());
                  this.parseSkill(d);
                  this._skillsInFile.addAll(this._currentSkill.skills);
                  this.resetTable();
               }
            }
         } else if ("skill".equalsIgnoreCase(n.getNodeName())) {
            this.setCurrentSkill(new DocumentSkill.DocumentSkillDataHolder());
            this.parseSkill(n);
            this._skillsInFile.addAll(this._currentSkill.skills);
         }
      }
   }

   private void parseSkill(Node node) {
      NamedNodeMap attrs = node.getAttributes();
      int enchantLevels1 = 0;
      int enchantLevels2 = 0;
      int enchantLevels3 = 0;
      int enchantLevels4 = 0;
      int enchantLevels5 = 0;
      int enchantLevels6 = 0;
      int enchantLevels7 = 0;
      int enchantLevels8 = 0;
      int skillId = Integer.parseInt(attrs.getNamedItem("id").getNodeValue());
      String skillName = attrs.getNamedItem("name").getNodeValue();
      String levels = attrs.getNamedItem("levels").getNodeValue();
      int lastLvl = Integer.parseInt(levels);
      if (attrs.getNamedItem("enchantGroup1") != null) {
         enchantLevels1 = EnchantSkillGroupsData.getInstance()
            .addNewRouteForSkill(skillId, lastLvl, 1, Integer.parseInt(attrs.getNamedItem("enchantGroup1").getNodeValue()));
      }

      if (attrs.getNamedItem("enchantGroup2") != null) {
         enchantLevels2 = EnchantSkillGroupsData.getInstance()
            .addNewRouteForSkill(skillId, lastLvl, 2, Integer.parseInt(attrs.getNamedItem("enchantGroup2").getNodeValue()));
      }

      if (attrs.getNamedItem("enchantGroup3") != null) {
         enchantLevels3 = EnchantSkillGroupsData.getInstance()
            .addNewRouteForSkill(skillId, lastLvl, 3, Integer.parseInt(attrs.getNamedItem("enchantGroup3").getNodeValue()));
      }

      if (attrs.getNamedItem("enchantGroup4") != null) {
         enchantLevels4 = EnchantSkillGroupsData.getInstance()
            .addNewRouteForSkill(skillId, lastLvl, 4, Integer.parseInt(attrs.getNamedItem("enchantGroup4").getNodeValue()));
      }

      if (attrs.getNamedItem("enchantGroup5") != null) {
         enchantLevels5 = EnchantSkillGroupsData.getInstance()
            .addNewRouteForSkill(skillId, lastLvl, 5, Integer.parseInt(attrs.getNamedItem("enchantGroup5").getNodeValue()));
      }

      if (attrs.getNamedItem("enchantGroup6") != null) {
         enchantLevels6 = EnchantSkillGroupsData.getInstance()
            .addNewRouteForSkill(skillId, lastLvl, 6, Integer.parseInt(attrs.getNamedItem("enchantGroup6").getNodeValue()));
      }

      if (attrs.getNamedItem("enchantGroup7") != null) {
         enchantLevels7 = EnchantSkillGroupsData.getInstance()
            .addNewRouteForSkill(skillId, lastLvl, 7, Integer.parseInt(attrs.getNamedItem("enchantGroup7").getNodeValue()));
      }

      if (attrs.getNamedItem("enchantGroup8") != null) {
         enchantLevels8 = EnchantSkillGroupsData.getInstance()
            .addNewRouteForSkill(skillId, lastLvl, 8, Integer.parseInt(attrs.getNamedItem("enchantGroup8").getNodeValue()));
      }

      this._currentSkill.id = skillId;
      this._currentSkill.name = skillName;
      this._currentSkill.sets = new StatSet[lastLvl];
      this._currentSkill.enchsets1 = new StatSet[enchantLevels1];
      this._currentSkill.enchsets2 = new StatSet[enchantLevels2];
      this._currentSkill.enchsets3 = new StatSet[enchantLevels3];
      this._currentSkill.enchsets4 = new StatSet[enchantLevels4];
      this._currentSkill.enchsets5 = new StatSet[enchantLevels5];
      this._currentSkill.enchsets6 = new StatSet[enchantLevels6];
      this._currentSkill.enchsets7 = new StatSet[enchantLevels7];
      this._currentSkill.enchsets8 = new StatSet[enchantLevels8];

      for (int i = 0; i < lastLvl; i++) {
         this._currentSkill.sets[i] = new StatSet();
         this._currentSkill.sets[i].set("skill_id", this._currentSkill.id);
         this._currentSkill.sets[i].set("level", i + 1);
         this._currentSkill.sets[i].set("name", this._currentSkill.name);
      }

      if (this._currentSkill.sets.length != lastLvl) {
         throw new RuntimeException("Skill id=" + skillId + " number of levels missmatch, " + lastLvl + " levels expected");
      } else {
         Node first = node.getFirstChild();

         for (Node n = first; n != null; n = n.getNextSibling()) {
            if ("table".equalsIgnoreCase(n.getNodeName())) {
               this.parseTable(n);
            }
         }

         for (int i = 1; i <= lastLvl; i++) {
            for (Node var30 = first; var30 != null; var30 = var30.getNextSibling()) {
               if (var30.getNodeType() == 1) {
                  String nodeName = var30.getNodeName();
                  if (!nodeName.equalsIgnoreCase("table")
                     && !nodeName.toLowerCase().startsWith("enchant")
                     && !nodeName.toLowerCase().endsWith("effects")
                     && !nodeName.equalsIgnoreCase("conditions")) {
                     if ("set".equalsIgnoreCase(nodeName)) {
                        this.parseBeanSet(var30, this._currentSkill.sets[i - 1], i);
                     } else if (var30.getAttributes().getNamedItem("levelValues") != null) {
                        this.parseInlineLevelTable(var30, this._currentSkill.sets[i - 1], i);
                     } else {
                        this.parseElementValue(var30, this._currentSkill.sets[i - 1], i);
                     }
                  }
               }
            }
         }

         for (int i = 0; i < enchantLevels1; i++) {
            this._currentSkill.enchsets1[i] = new StatSet();
            this._currentSkill.enchsets1[i].set("skill_id", this._currentSkill.id);
            this._currentSkill.enchsets1[i].set("level", i + 101);
            this._currentSkill.enchsets1[i].set("name", this._currentSkill.name);

            for (Node var31 = first; var31 != null; var31 = var31.getNextSibling()) {
               if (var31.getNodeType() == 1) {
                  String nodeName = var31.getNodeName();
                  if (!nodeName.equalsIgnoreCase("table")
                     && !nodeName.equalsIgnoreCase("conditions")
                     && (!nodeName.toLowerCase().startsWith("enchant") || nodeName.equalsIgnoreCase("enchant1"))) {
                     if ("set".equalsIgnoreCase(nodeName)) {
                        this.parseBeanSet(var31, this._currentSkill.enchsets1[i], this._currentSkill.sets.length);
                     } else if ("enchant1".equalsIgnoreCase(nodeName)) {
                        this.parseBeanSet(var31, this._currentSkill.enchsets1[i], i + 1);
                     } else if (!nodeName.toLowerCase().endsWith("effects")) {
                        if (var31.getAttributes().getNamedItem("subLevel1Values") != null) {
                           this.parseInlineLevelTable(var31, this._currentSkill.enchsets1[i], i + 1, 1);
                        } else {
                           this.parseElementValue(var31, this._currentSkill.enchsets1[i], this._currentSkill.sets.length);
                        }
                     }
                  }
               }
            }
         }

         for (int i = 0; i < enchantLevels2; i++) {
            this._currentSkill.enchsets2[i] = new StatSet();
            this._currentSkill.enchsets2[i].set("skill_id", this._currentSkill.id);
            this._currentSkill.enchsets2[i].set("level", i + 201);
            this._currentSkill.enchsets2[i].set("name", this._currentSkill.name);

            for (Node var32 = first; var32 != null; var32 = var32.getNextSibling()) {
               if (var32.getNodeType() == 1) {
                  String nodeName = var32.getNodeName();
                  if (!nodeName.equalsIgnoreCase("table")
                     && !nodeName.equalsIgnoreCase("conditions")
                     && (!nodeName.toLowerCase().startsWith("enchant") || nodeName.equalsIgnoreCase("enchant2"))) {
                     if ("set".equalsIgnoreCase(nodeName)) {
                        this.parseBeanSet(var32, this._currentSkill.enchsets2[i], this._currentSkill.sets.length);
                     } else if ("enchant2".equalsIgnoreCase(nodeName)) {
                        this.parseBeanSet(var32, this._currentSkill.enchsets2[i], i + 1);
                     } else if (!nodeName.toLowerCase().endsWith("effects")) {
                        if (var32.getAttributes().getNamedItem("subLevel2Values") != null) {
                           this.parseInlineLevelTable(var32, this._currentSkill.enchsets2[i], i + 1, 2);
                        } else {
                           this.parseElementValue(var32, this._currentSkill.enchsets2[i], this._currentSkill.sets.length);
                        }
                     }
                  }
               }
            }
         }

         for (int i = 0; i < enchantLevels3; i++) {
            this._currentSkill.enchsets3[i] = new StatSet();
            this._currentSkill.enchsets3[i].set("skill_id", this._currentSkill.id);
            this._currentSkill.enchsets3[i].set("level", i + 301);
            this._currentSkill.enchsets3[i].set("name", this._currentSkill.name);

            for (Node var33 = first; var33 != null; var33 = var33.getNextSibling()) {
               if (var33.getNodeType() == 1) {
                  String nodeName = var33.getNodeName();
                  if (!nodeName.equalsIgnoreCase("table")
                     && !nodeName.equalsIgnoreCase("conditions")
                     && (!nodeName.toLowerCase().startsWith("enchant") || nodeName.equalsIgnoreCase("enchant3"))) {
                     if ("set".equalsIgnoreCase(nodeName)) {
                        this.parseBeanSet(var33, this._currentSkill.enchsets3[i], this._currentSkill.sets.length);
                     } else if ("enchant3".equalsIgnoreCase(nodeName)) {
                        this.parseBeanSet(var33, this._currentSkill.enchsets3[i], i + 1);
                     } else if (!nodeName.toLowerCase().endsWith("effects")) {
                        if (var33.getAttributes().getNamedItem("subLevel3Values") != null) {
                           this.parseInlineLevelTable(var33, this._currentSkill.enchsets3[i], i + 1, 3);
                        } else {
                           this.parseElementValue(var33, this._currentSkill.enchsets3[i], this._currentSkill.sets.length);
                        }
                     }
                  }
               }
            }
         }

         for (int i = 0; i < enchantLevels4; i++) {
            this._currentSkill.enchsets4[i] = new StatSet();
            this._currentSkill.enchsets4[i].set("skill_id", this._currentSkill.id);
            this._currentSkill.enchsets4[i].set("level", i + 401);
            this._currentSkill.enchsets4[i].set("name", this._currentSkill.name);

            for (Node var34 = first; var34 != null; var34 = var34.getNextSibling()) {
               if (var34.getNodeType() == 1) {
                  String nodeName = var34.getNodeName();
                  if (!nodeName.equalsIgnoreCase("table")
                     && !nodeName.equalsIgnoreCase("conditions")
                     && (!nodeName.toLowerCase().startsWith("enchant") || nodeName.equalsIgnoreCase("enchant4"))) {
                     if ("set".equalsIgnoreCase(nodeName)) {
                        this.parseBeanSet(var34, this._currentSkill.enchsets4[i], this._currentSkill.sets.length);
                     } else if ("enchant4".equalsIgnoreCase(nodeName)) {
                        this.parseBeanSet(var34, this._currentSkill.enchsets4[i], i + 1);
                     } else if (!nodeName.toLowerCase().endsWith("effects")) {
                        if (var34.getAttributes().getNamedItem("subLevel4Values") != null) {
                           this.parseInlineLevelTable(var34, this._currentSkill.enchsets4[i], i + 1, 4);
                        } else {
                           this.parseElementValue(var34, this._currentSkill.enchsets4[i], this._currentSkill.sets.length);
                        }
                     }
                  }
               }
            }
         }

         for (int i = 0; i < enchantLevels5; i++) {
            this._currentSkill.enchsets5[i] = new StatSet();
            this._currentSkill.enchsets5[i].set("skill_id", this._currentSkill.id);
            this._currentSkill.enchsets5[i].set("level", i + 501);
            this._currentSkill.enchsets5[i].set("name", this._currentSkill.name);

            for (Node var35 = first; var35 != null; var35 = var35.getNextSibling()) {
               if (var35.getNodeType() == 1) {
                  String nodeName = var35.getNodeName();
                  if (!nodeName.equalsIgnoreCase("table")
                     && !nodeName.equalsIgnoreCase("conditions")
                     && (!nodeName.toLowerCase().startsWith("enchant") || nodeName.equalsIgnoreCase("enchant5"))) {
                     if ("set".equalsIgnoreCase(nodeName)) {
                        this.parseBeanSet(var35, this._currentSkill.enchsets5[i], this._currentSkill.sets.length);
                     } else if ("enchant5".equalsIgnoreCase(nodeName)) {
                        this.parseBeanSet(var35, this._currentSkill.enchsets5[i], i + 1);
                     } else if (!nodeName.toLowerCase().endsWith("effects")) {
                        if (var35.getAttributes().getNamedItem("subLevel5Values") != null) {
                           this.parseInlineLevelTable(var35, this._currentSkill.enchsets5[i], i + 1, 5);
                        } else {
                           this.parseElementValue(var35, this._currentSkill.enchsets5[i], this._currentSkill.sets.length);
                        }
                     }
                  }
               }
            }
         }

         for (int i = 0; i < enchantLevels6; i++) {
            this._currentSkill.enchsets6[i] = new StatSet();
            this._currentSkill.enchsets6[i].set("skill_id", this._currentSkill.id);
            this._currentSkill.enchsets6[i].set("level", i + 601);
            this._currentSkill.enchsets6[i].set("name", this._currentSkill.name);

            for (Node var36 = first; var36 != null; var36 = var36.getNextSibling()) {
               if (var36.getNodeType() == 1) {
                  String nodeName = var36.getNodeName();
                  if (!nodeName.equalsIgnoreCase("table")
                     && !nodeName.equalsIgnoreCase("conditions")
                     && (!nodeName.toLowerCase().startsWith("enchant") || nodeName.equalsIgnoreCase("enchant6"))) {
                     if ("set".equalsIgnoreCase(nodeName)) {
                        this.parseBeanSet(var36, this._currentSkill.enchsets6[i], this._currentSkill.sets.length);
                     } else if ("enchant6".equalsIgnoreCase(nodeName)) {
                        this.parseBeanSet(var36, this._currentSkill.enchsets6[i], i + 1);
                     } else if (!nodeName.toLowerCase().endsWith("effects")) {
                        if (var36.getAttributes().getNamedItem("subLevel6Values") != null) {
                           this.parseInlineLevelTable(var36, this._currentSkill.enchsets6[i], i + 1, 6);
                        } else {
                           this.parseElementValue(var36, this._currentSkill.enchsets6[i], this._currentSkill.sets.length);
                        }
                     }
                  }
               }
            }
         }

         for (int i = 0; i < enchantLevels7; i++) {
            this._currentSkill.enchsets7[i] = new StatSet();
            this._currentSkill.enchsets7[i].set("skill_id", this._currentSkill.id);
            this._currentSkill.enchsets7[i].set("level", i + 701);
            this._currentSkill.enchsets7[i].set("name", this._currentSkill.name);

            for (Node var37 = first; var37 != null; var37 = var37.getNextSibling()) {
               if (var37.getNodeType() == 1) {
                  String nodeName = var37.getNodeName();
                  if (!nodeName.equalsIgnoreCase("table")
                     && !nodeName.equalsIgnoreCase("conditions")
                     && (!nodeName.toLowerCase().startsWith("enchant") || nodeName.equalsIgnoreCase("enchant7"))) {
                     if ("set".equalsIgnoreCase(nodeName)) {
                        this.parseBeanSet(var37, this._currentSkill.enchsets7[i], this._currentSkill.sets.length);
                     } else if ("enchant7".equalsIgnoreCase(nodeName)) {
                        this.parseBeanSet(var37, this._currentSkill.enchsets7[i], i + 1);
                     } else if (!nodeName.toLowerCase().endsWith("effects")) {
                        if (var37.getAttributes().getNamedItem("subLevel7Values") != null) {
                           this.parseInlineLevelTable(var37, this._currentSkill.enchsets7[i], i + 1, 7);
                        } else {
                           this.parseElementValue(var37, this._currentSkill.enchsets7[i], this._currentSkill.sets.length);
                        }
                     }
                  }
               }
            }
         }

         for (int i = 0; i < enchantLevels8; i++) {
            this._currentSkill.enchsets8[i] = new StatSet();
            this._currentSkill.enchsets8[i].set("skill_id", this._currentSkill.id);
            this._currentSkill.enchsets8[i].set("level", i + 801);
            this._currentSkill.enchsets8[i].set("name", this._currentSkill.name);

            for (Node var38 = first; var38 != null; var38 = var38.getNextSibling()) {
               if (var38.getNodeType() == 1) {
                  String nodeName = var38.getNodeName();
                  if (!nodeName.equalsIgnoreCase("table")
                     && !nodeName.equalsIgnoreCase("conditions")
                     && (!nodeName.toLowerCase().startsWith("enchant") || nodeName.equalsIgnoreCase("enchant8"))) {
                     if ("set".equalsIgnoreCase(nodeName)) {
                        this.parseBeanSet(var38, this._currentSkill.enchsets8[i], this._currentSkill.sets.length);
                     } else if ("enchant8".equalsIgnoreCase(nodeName)) {
                        this.parseBeanSet(var38, this._currentSkill.enchsets8[i], i + 1);
                     } else if (!nodeName.toLowerCase().endsWith("effects")) {
                        if (var38.getAttributes().getNamedItem("subLevel8Values") != null) {
                           this.parseInlineLevelTable(var38, this._currentSkill.enchsets8[i], i + 1, 8);
                        } else {
                           this.parseElementValue(var38, this._currentSkill.enchsets8[i], this._currentSkill.sets.length);
                        }
                     }
                  }
               }
            }
         }

         if (this._currentSkill.enchsets8.length != enchantLevels8) {
            throw new RuntimeException("Skill id=" + skillId + " number of levels missmatch, " + enchantLevels8 + " levels expected");
         } else {
            this.makeSkills();

            for (int i = 0; i < lastLvl; i++) {
               this._currentSkill.currentLevel = i;

               for (Node var39 = first; var39 != null; var39 = var39.getNextSibling()) {
                  if ("conditions".equalsIgnoreCase(var39.getNodeName())) {
                     Condition condition = this.parseCondition(var39.getFirstChild(), this._currentSkill.currentSkills.get(i));
                     Node msg = var39.getAttributes().getNamedItem("msg");
                     Node msgId = var39.getAttributes().getNamedItem("msgId");
                     if (condition != null && msg != null) {
                        condition.setMessage(msg.getNodeValue());
                     } else if (condition != null && msgId != null) {
                        condition.setMessageId(Integer.decode(this.getValue(msgId.getNodeValue(), null)));
                        Node addName = var39.getAttributes().getNamedItem("addName");
                        if (addName != null && Integer.decode(this.getValue(msgId.getNodeValue(), null)) > 0) {
                           condition.addName();
                        }
                     }

                     this._currentSkill.currentSkills.get(i).attach(condition, false);
                  } else if ("effects".equalsIgnoreCase(var39.getNodeName())) {
                     this.parseTemplate(var39, this._currentSkill.currentSkills.get(i));
                  } else if ("startEffects".equalsIgnoreCase(var39.getNodeName())) {
                     this.parseTemplate(var39, this._currentSkill.currentSkills.get(i), EffectScope.START);
                  } else if ("channelingEffects".equalsIgnoreCase(var39.getNodeName())) {
                     this.parseTemplate(var39, this._currentSkill.currentSkills.get(i), EffectScope.CHANNELING);
                  } else if ("pveEffects".equalsIgnoreCase(var39.getNodeName())) {
                     this.parseTemplate(var39, this._currentSkill.currentSkills.get(i), EffectScope.PVE);
                  } else if ("pvpEffects".equalsIgnoreCase(var39.getNodeName())) {
                     this.parseTemplate(var39, this._currentSkill.currentSkills.get(i), EffectScope.PVP);
                  } else if ("endEffects".equalsIgnoreCase(var39.getNodeName())) {
                     this.parseTemplate(var39, this._currentSkill.currentSkills.get(i), EffectScope.END);
                  } else if ("selfEffects".equalsIgnoreCase(var39.getNodeName())) {
                     this.parseTemplate(var39, this._currentSkill.currentSkills.get(i), EffectScope.SELF);
                  }
               }
            }

            for (int i = lastLvl; i < lastLvl + enchantLevels1; i++) {
               this._currentSkill.currentLevel = i - lastLvl;
               boolean foundConditions = false;
               boolean foundEffects = false;
               boolean foundChannelingEffects = false;
               boolean foundStartEffects = false;
               boolean foundPveEffects = false;
               boolean foundPvpEffects = false;
               boolean foundEndEffects = false;
               boolean foundSelfEffects = false;

               for (Node var40 = first; var40 != null; var40 = var40.getNextSibling()) {
                  if ("enchant1conditions".equalsIgnoreCase(var40.getNodeName())) {
                     foundConditions = true;
                     Condition condition = this.parseCondition(var40.getFirstChild(), this._currentSkill.currentSkills.get(i));
                     Node msg = var40.getAttributes().getNamedItem("msg");
                     Node msgId = var40.getAttributes().getNamedItem("msgId");
                     if (condition != null && msg != null) {
                        condition.setMessage(msg.getNodeValue());
                     } else if (condition != null && msgId != null) {
                        condition.setMessageId(Integer.decode(this.getValue(msgId.getNodeValue(), null)));
                        Node addName = var40.getAttributes().getNamedItem("addName");
                        if (addName != null && Integer.decode(this.getValue(msgId.getNodeValue(), null)) > 0) {
                           condition.addName();
                        }
                     }

                     this._currentSkill.currentSkills.get(i).attach(condition, false);
                  } else if ("enchant1effects".equalsIgnoreCase(var40.getNodeName())) {
                     foundEffects = true;
                     this.parseTemplate(var40, this._currentSkill.currentSkills.get(i));
                  } else if ("enchant1startEffects".equalsIgnoreCase(var40.getNodeName())) {
                     foundStartEffects = true;
                     this.parseTemplate(var40, this._currentSkill.currentSkills.get(i), EffectScope.START);
                  } else if ("enchant1channelingEffects".equalsIgnoreCase(var40.getNodeName())) {
                     foundChannelingEffects = true;
                     this.parseTemplate(var40, this._currentSkill.currentSkills.get(i), EffectScope.CHANNELING);
                  } else if ("enchant1pveEffects".equalsIgnoreCase(var40.getNodeName())) {
                     foundPveEffects = true;
                     this.parseTemplate(var40, this._currentSkill.currentSkills.get(i), EffectScope.PVE);
                  } else if ("enchant1pvpEffects".equalsIgnoreCase(var40.getNodeName())) {
                     foundPvpEffects = true;
                     this.parseTemplate(var40, this._currentSkill.currentSkills.get(i), EffectScope.PVP);
                  } else if ("enchant1endEffects".equalsIgnoreCase(var40.getNodeName())) {
                     foundEndEffects = true;
                     this.parseTemplate(var40, this._currentSkill.currentSkills.get(i), EffectScope.END);
                  } else if ("enchant1selfEffects".equalsIgnoreCase(var40.getNodeName())) {
                     foundSelfEffects = true;
                     this.parseTemplate(var40, this._currentSkill.currentSkills.get(i), EffectScope.SELF);
                  }
               }

               if (!foundConditions
                  || !foundEffects
                  || !foundChannelingEffects
                  || !foundStartEffects
                  || !foundPveEffects
                  || !foundPvpEffects
                  || !foundEndEffects
                  || !foundSelfEffects) {
                  this._currentSkill.currentLevel = lastLvl - 1;

                  for (Node var41 = first; var41 != null; var41 = var41.getNextSibling()) {
                     if (!foundConditions && "conditions".equalsIgnoreCase(var41.getNodeName())) {
                        Condition condition = this.parseCondition(var41.getFirstChild(), this._currentSkill.currentSkills.get(i));
                        Node msg = var41.getAttributes().getNamedItem("msg");
                        Node msgId = var41.getAttributes().getNamedItem("msgId");
                        if (condition != null && msg != null) {
                           condition.setMessage(msg.getNodeValue());
                        } else if (condition != null && msgId != null) {
                           condition.setMessageId(Integer.decode(this.getValue(msgId.getNodeValue(), null)));
                           Node addName = var41.getAttributes().getNamedItem("addName");
                           if (addName != null && Integer.decode(this.getValue(msgId.getNodeValue(), null)) > 0) {
                              condition.addName();
                           }
                        }

                        this._currentSkill.currentSkills.get(i).attach(condition, false);
                     } else if (!foundEffects && "effects".equalsIgnoreCase(var41.getNodeName())) {
                        this.parseTemplate(var41, this._currentSkill.currentSkills.get(i));
                     } else if (!foundStartEffects && "startEffects".equalsIgnoreCase(var41.getNodeName())) {
                        this.parseTemplate(var41, this._currentSkill.currentSkills.get(i), EffectScope.START);
                     } else if (!foundChannelingEffects && "channelingEffects".equalsIgnoreCase(var41.getNodeName())) {
                        this.parseTemplate(var41, this._currentSkill.currentSkills.get(i), EffectScope.CHANNELING);
                     } else if (!foundPveEffects && "pveEffects".equalsIgnoreCase(var41.getNodeName())) {
                        this.parseTemplate(var41, this._currentSkill.currentSkills.get(i), EffectScope.PVE);
                     } else if (!foundPvpEffects && "pvpEffects".equalsIgnoreCase(var41.getNodeName())) {
                        this.parseTemplate(var41, this._currentSkill.currentSkills.get(i), EffectScope.PVP);
                     } else if (!foundEndEffects && "endEffects".equalsIgnoreCase(var41.getNodeName())) {
                        this.parseTemplate(var41, this._currentSkill.currentSkills.get(i), EffectScope.END);
                     } else if (!foundSelfEffects && "selfEffects".equalsIgnoreCase(var41.getNodeName())) {
                        this.parseTemplate(var41, this._currentSkill.currentSkills.get(i), EffectScope.SELF);
                     }
                  }
               }
            }

            for (int i = lastLvl + enchantLevels1; i < lastLvl + enchantLevels1 + enchantLevels2; i++) {
               boolean foundConditions = false;
               boolean foundEffects = false;
               boolean foundChannelingEffects = false;
               boolean foundStartEffects = false;
               boolean foundPveEffects = false;
               boolean foundPvpEffects = false;
               boolean foundEndEffects = false;
               boolean foundSelfEffects = false;
               this._currentSkill.currentLevel = i - lastLvl - enchantLevels1;

               for (Node var42 = first; var42 != null; var42 = var42.getNextSibling()) {
                  if ("enchant2conditions".equalsIgnoreCase(var42.getNodeName())) {
                     foundConditions = true;
                     Condition condition = this.parseCondition(var42.getFirstChild(), this._currentSkill.currentSkills.get(i));
                     Node msg = var42.getAttributes().getNamedItem("msg");
                     Node msgId = var42.getAttributes().getNamedItem("msgId");
                     if (condition != null && msg != null) {
                        condition.setMessage(msg.getNodeValue());
                     } else if (condition != null && msgId != null) {
                        condition.setMessageId(Integer.decode(this.getValue(msgId.getNodeValue(), null)));
                        Node addName = var42.getAttributes().getNamedItem("addName");
                        if (addName != null && Integer.decode(this.getValue(msgId.getNodeValue(), null)) > 0) {
                           condition.addName();
                        }
                     }

                     this._currentSkill.currentSkills.get(i).attach(condition, false);
                  } else if ("enchant2effects".equalsIgnoreCase(var42.getNodeName())) {
                     foundEffects = true;
                     this.parseTemplate(var42, this._currentSkill.currentSkills.get(i));
                  } else if ("enchant2startEffects".equalsIgnoreCase(var42.getNodeName())) {
                     foundStartEffects = true;
                     this.parseTemplate(var42, this._currentSkill.currentSkills.get(i), EffectScope.START);
                  } else if ("enchant2channelingEffects".equalsIgnoreCase(var42.getNodeName())) {
                     foundChannelingEffects = true;
                     this.parseTemplate(var42, this._currentSkill.currentSkills.get(i), EffectScope.CHANNELING);
                  } else if ("enchant2pveEffects".equalsIgnoreCase(var42.getNodeName())) {
                     foundPveEffects = true;
                     this.parseTemplate(var42, this._currentSkill.currentSkills.get(i), EffectScope.PVE);
                  } else if ("enchant2pvpEffects".equalsIgnoreCase(var42.getNodeName())) {
                     foundPvpEffects = true;
                     this.parseTemplate(var42, this._currentSkill.currentSkills.get(i), EffectScope.PVP);
                  } else if ("enchant2endEffects".equalsIgnoreCase(var42.getNodeName())) {
                     foundEndEffects = true;
                     this.parseTemplate(var42, this._currentSkill.currentSkills.get(i), EffectScope.END);
                  } else if ("enchant2selfEffects".equalsIgnoreCase(var42.getNodeName())) {
                     foundSelfEffects = true;
                     this.parseTemplate(var42, this._currentSkill.currentSkills.get(i), EffectScope.SELF);
                  }
               }

               if (!foundConditions
                  || !foundEffects
                  || !foundChannelingEffects
                  || !foundStartEffects
                  || !foundPveEffects
                  || !foundPvpEffects
                  || !foundEndEffects
                  || !foundSelfEffects) {
                  this._currentSkill.currentLevel = lastLvl - 1;

                  for (Node var43 = first; var43 != null; var43 = var43.getNextSibling()) {
                     if (!foundConditions && "conditions".equalsIgnoreCase(var43.getNodeName())) {
                        Condition condition = this.parseCondition(var43.getFirstChild(), this._currentSkill.currentSkills.get(i));
                        Node msg = var43.getAttributes().getNamedItem("msg");
                        Node msgId = var43.getAttributes().getNamedItem("msgId");
                        if (condition != null && msg != null) {
                           condition.setMessage(msg.getNodeValue());
                        } else if (condition != null && msgId != null) {
                           condition.setMessageId(Integer.decode(this.getValue(msgId.getNodeValue(), null)));
                           Node addName = var43.getAttributes().getNamedItem("addName");
                           if (addName != null && Integer.decode(this.getValue(msgId.getNodeValue(), null)) > 0) {
                              condition.addName();
                           }
                        }

                        this._currentSkill.currentSkills.get(i).attach(condition, false);
                     } else if (!foundEffects && "effects".equalsIgnoreCase(var43.getNodeName())) {
                        this.parseTemplate(var43, this._currentSkill.currentSkills.get(i));
                     } else if (!foundChannelingEffects && "channelingEffects".equalsIgnoreCase(var43.getNodeName())) {
                        this.parseTemplate(var43, this._currentSkill.currentSkills.get(i), EffectScope.CHANNELING);
                     } else if (!foundPveEffects && "pveEffects".equalsIgnoreCase(var43.getNodeName())) {
                        this.parseTemplate(var43, this._currentSkill.currentSkills.get(i), EffectScope.PVE);
                     } else if (!foundPvpEffects && "pvpEffects".equalsIgnoreCase(var43.getNodeName())) {
                        this.parseTemplate(var43, this._currentSkill.currentSkills.get(i), EffectScope.PVP);
                     } else if (!foundEndEffects && "endEffects".equalsIgnoreCase(var43.getNodeName())) {
                        this.parseTemplate(var43, this._currentSkill.currentSkills.get(i), EffectScope.END);
                     } else if (!foundSelfEffects && "selfEffects".equalsIgnoreCase(var43.getNodeName())) {
                        this.parseTemplate(var43, this._currentSkill.currentSkills.get(i), EffectScope.SELF);
                     }
                  }
               }
            }

            for (int i = lastLvl + enchantLevels1 + enchantLevels2; i < lastLvl + enchantLevels1 + enchantLevels2 + enchantLevels3; i++) {
               boolean foundConditions = false;
               boolean foundEffects = false;
               boolean foundChannelingEffects = false;
               boolean foundStartEffects = false;
               boolean foundPveEffects = false;
               boolean foundPvpEffects = false;
               boolean foundEndEffects = false;
               boolean foundSelfEffects = false;
               this._currentSkill.currentLevel = i - lastLvl - enchantLevels1 - enchantLevels2;

               for (Node var44 = first; var44 != null; var44 = var44.getNextSibling()) {
                  if ("enchant3conditions".equalsIgnoreCase(var44.getNodeName())) {
                     foundConditions = true;
                     Condition condition = this.parseCondition(var44.getFirstChild(), this._currentSkill.currentSkills.get(i));
                     Node msg = var44.getAttributes().getNamedItem("msg");
                     Node msgId = var44.getAttributes().getNamedItem("msgId");
                     if (condition != null && msg != null) {
                        condition.setMessage(msg.getNodeValue());
                     } else if (condition != null && msgId != null) {
                        condition.setMessageId(Integer.decode(this.getValue(msgId.getNodeValue(), null)));
                        Node addName = var44.getAttributes().getNamedItem("addName");
                        if (addName != null && Integer.decode(this.getValue(msgId.getNodeValue(), null)) > 0) {
                           condition.addName();
                        }
                     }

                     this._currentSkill.currentSkills.get(i).attach(condition, false);
                  } else if ("enchant3effects".equalsIgnoreCase(var44.getNodeName())) {
                     foundEffects = true;
                     this.parseTemplate(var44, this._currentSkill.currentSkills.get(i));
                  } else if ("enchant3startEffects".equalsIgnoreCase(var44.getNodeName())) {
                     foundStartEffects = true;
                     this.parseTemplate(var44, this._currentSkill.currentSkills.get(i), EffectScope.START);
                  } else if ("enchant3channelingEffects".equalsIgnoreCase(var44.getNodeName())) {
                     foundChannelingEffects = true;
                     this.parseTemplate(var44, this._currentSkill.currentSkills.get(i), EffectScope.CHANNELING);
                  } else if ("enchant3pveEffects".equalsIgnoreCase(var44.getNodeName())) {
                     foundPveEffects = true;
                     this.parseTemplate(var44, this._currentSkill.currentSkills.get(i), EffectScope.PVE);
                  } else if ("enchant3pvpEffects".equalsIgnoreCase(var44.getNodeName())) {
                     foundPvpEffects = true;
                     this.parseTemplate(var44, this._currentSkill.currentSkills.get(i), EffectScope.PVP);
                  } else if ("enchant3endEffects".equalsIgnoreCase(var44.getNodeName())) {
                     foundEndEffects = true;
                     this.parseTemplate(var44, this._currentSkill.currentSkills.get(i), EffectScope.END);
                  } else if ("enchant3selfEffects".equalsIgnoreCase(var44.getNodeName())) {
                     foundSelfEffects = true;
                     this.parseTemplate(var44, this._currentSkill.currentSkills.get(i), EffectScope.SELF);
                  }
               }

               if (!foundConditions
                  || !foundEffects
                  || !foundChannelingEffects
                  || !foundStartEffects
                  || !foundPveEffects
                  || !foundPvpEffects
                  || !foundEndEffects
                  || !foundSelfEffects) {
                  this._currentSkill.currentLevel = lastLvl - 1;

                  for (Node var45 = first; var45 != null; var45 = var45.getNextSibling()) {
                     if (!foundConditions && "conditions".equalsIgnoreCase(var45.getNodeName())) {
                        Condition condition = this.parseCondition(var45.getFirstChild(), this._currentSkill.currentSkills.get(i));
                        Node msg = var45.getAttributes().getNamedItem("msg");
                        Node msgId = var45.getAttributes().getNamedItem("msgId");
                        if (condition != null && msg != null) {
                           condition.setMessage(msg.getNodeValue());
                        } else if (condition != null && msgId != null) {
                           condition.setMessageId(Integer.decode(this.getValue(msgId.getNodeValue(), null)));
                           Node addName = var45.getAttributes().getNamedItem("addName");
                           if (addName != null && Integer.decode(this.getValue(msgId.getNodeValue(), null)) > 0) {
                              condition.addName();
                           }
                        }

                        this._currentSkill.currentSkills.get(i).attach(condition, false);
                     } else if (!foundEffects && "effects".equalsIgnoreCase(var45.getNodeName())) {
                        this.parseTemplate(var45, this._currentSkill.currentSkills.get(i));
                     } else if (!foundStartEffects && "startEffects".equalsIgnoreCase(var45.getNodeName())) {
                        this.parseTemplate(var45, this._currentSkill.currentSkills.get(i), EffectScope.START);
                     } else if (!foundChannelingEffects && "channelingEffects".equalsIgnoreCase(var45.getNodeName())) {
                        this.parseTemplate(var45, this._currentSkill.currentSkills.get(i), EffectScope.CHANNELING);
                     } else if (!foundPveEffects && "pveEffects".equalsIgnoreCase(var45.getNodeName())) {
                        this.parseTemplate(var45, this._currentSkill.currentSkills.get(i), EffectScope.PVE);
                     } else if (!foundPvpEffects && "pvpEffects".equalsIgnoreCase(var45.getNodeName())) {
                        this.parseTemplate(var45, this._currentSkill.currentSkills.get(i), EffectScope.PVP);
                     } else if (!foundEndEffects && "endEffects".equalsIgnoreCase(var45.getNodeName())) {
                        this.parseTemplate(var45, this._currentSkill.currentSkills.get(i), EffectScope.END);
                     } else if (!foundSelfEffects && "selfEffects".equalsIgnoreCase(var45.getNodeName())) {
                        this.parseTemplate(var45, this._currentSkill.currentSkills.get(i), EffectScope.SELF);
                     }
                  }
               }
            }

            for (int i = lastLvl + enchantLevels1 + enchantLevels2 + enchantLevels3;
               i < lastLvl + enchantLevels1 + enchantLevels2 + enchantLevels3 + enchantLevels4;
               i++
            ) {
               boolean foundConditions = false;
               boolean foundEffects = false;
               boolean foundChannelingEffects = false;
               boolean foundStartEffects = false;
               boolean foundPveEffects = false;
               boolean foundPvpEffects = false;
               boolean foundEndEffects = false;
               boolean foundSelfEffects = false;
               this._currentSkill.currentLevel = i - lastLvl - enchantLevels1 - enchantLevels2 - enchantLevels3;

               for (Node var46 = first; var46 != null; var46 = var46.getNextSibling()) {
                  if ("enchant4conditions".equalsIgnoreCase(var46.getNodeName())) {
                     foundConditions = true;
                     Condition condition = this.parseCondition(var46.getFirstChild(), this._currentSkill.currentSkills.get(i));
                     Node msg = var46.getAttributes().getNamedItem("msg");
                     Node msgId = var46.getAttributes().getNamedItem("msgId");
                     if (condition != null && msg != null) {
                        condition.setMessage(msg.getNodeValue());
                     } else if (condition != null && msgId != null) {
                        condition.setMessageId(Integer.decode(this.getValue(msgId.getNodeValue(), null)));
                        Node addName = var46.getAttributes().getNamedItem("addName");
                        if (addName != null && Integer.decode(this.getValue(msgId.getNodeValue(), null)) > 0) {
                           condition.addName();
                        }
                     }

                     this._currentSkill.currentSkills.get(i).attach(condition, false);
                  } else if ("enchant4effects".equalsIgnoreCase(var46.getNodeName())) {
                     foundEffects = true;
                     this.parseTemplate(var46, this._currentSkill.currentSkills.get(i));
                  } else if ("enchant4startEffects".equalsIgnoreCase(var46.getNodeName())) {
                     foundStartEffects = true;
                     this.parseTemplate(var46, this._currentSkill.currentSkills.get(i), EffectScope.START);
                  } else if ("enchant4channelingEffects".equalsIgnoreCase(var46.getNodeName())) {
                     foundChannelingEffects = true;
                     this.parseTemplate(var46, this._currentSkill.currentSkills.get(i), EffectScope.CHANNELING);
                  } else if ("enchant4pveEffects".equalsIgnoreCase(var46.getNodeName())) {
                     foundPveEffects = true;
                     this.parseTemplate(var46, this._currentSkill.currentSkills.get(i), EffectScope.PVE);
                  } else if ("enchant4pvpEffects".equalsIgnoreCase(var46.getNodeName())) {
                     foundPvpEffects = true;
                     this.parseTemplate(var46, this._currentSkill.currentSkills.get(i), EffectScope.PVP);
                  } else if ("enchant4endEffects".equalsIgnoreCase(var46.getNodeName())) {
                     foundEndEffects = true;
                     this.parseTemplate(var46, this._currentSkill.currentSkills.get(i), EffectScope.END);
                  } else if ("enchant4selfEffects".equalsIgnoreCase(var46.getNodeName())) {
                     foundSelfEffects = true;
                     this.parseTemplate(var46, this._currentSkill.currentSkills.get(i), EffectScope.SELF);
                  }
               }

               if (!foundConditions
                  || !foundEffects
                  || !foundChannelingEffects
                  || !foundStartEffects
                  || !foundPveEffects
                  || !foundPvpEffects
                  || !foundEndEffects
                  || !foundSelfEffects) {
                  this._currentSkill.currentLevel = lastLvl - 1;

                  for (Node var47 = first; var47 != null; var47 = var47.getNextSibling()) {
                     if (!foundConditions && "conditions".equalsIgnoreCase(var47.getNodeName())) {
                        Condition condition = this.parseCondition(var47.getFirstChild(), this._currentSkill.currentSkills.get(i));
                        Node msg = var47.getAttributes().getNamedItem("msg");
                        Node msgId = var47.getAttributes().getNamedItem("msgId");
                        if (condition != null && msg != null) {
                           condition.setMessage(msg.getNodeValue());
                        } else if (condition != null && msgId != null) {
                           condition.setMessageId(Integer.decode(this.getValue(msgId.getNodeValue(), null)));
                           Node addName = var47.getAttributes().getNamedItem("addName");
                           if (addName != null && Integer.decode(this.getValue(msgId.getNodeValue(), null)) > 0) {
                              condition.addName();
                           }
                        }

                        this._currentSkill.currentSkills.get(i).attach(condition, false);
                     } else if (!foundEffects && "effects".equalsIgnoreCase(var47.getNodeName())) {
                        this.parseTemplate(var47, this._currentSkill.currentSkills.get(i));
                     } else if (!foundStartEffects && "startEffects".equalsIgnoreCase(var47.getNodeName())) {
                        this.parseTemplate(var47, this._currentSkill.currentSkills.get(i), EffectScope.START);
                     } else if (!foundChannelingEffects && "channelingEffects".equalsIgnoreCase(var47.getNodeName())) {
                        this.parseTemplate(var47, this._currentSkill.currentSkills.get(i), EffectScope.CHANNELING);
                     } else if (!foundPveEffects && "pveEffects".equalsIgnoreCase(var47.getNodeName())) {
                        this.parseTemplate(var47, this._currentSkill.currentSkills.get(i), EffectScope.PVE);
                     } else if (!foundPvpEffects && "pvpEffects".equalsIgnoreCase(var47.getNodeName())) {
                        this.parseTemplate(var47, this._currentSkill.currentSkills.get(i), EffectScope.PVP);
                     } else if (!foundEndEffects && "endEffects".equalsIgnoreCase(var47.getNodeName())) {
                        this.parseTemplate(var47, this._currentSkill.currentSkills.get(i), EffectScope.END);
                     } else if (!foundSelfEffects && "selfEffects".equalsIgnoreCase(var47.getNodeName())) {
                        this.parseTemplate(var47, this._currentSkill.currentSkills.get(i), EffectScope.SELF);
                     }
                  }
               }
            }

            for (int i = lastLvl + enchantLevels1 + enchantLevels2 + enchantLevels3 + enchantLevels4;
               i < lastLvl + enchantLevels1 + enchantLevels2 + enchantLevels3 + enchantLevels4 + enchantLevels5;
               i++
            ) {
               boolean foundConditions = false;
               boolean foundEffects = false;
               boolean foundChannelingEffects = false;
               boolean foundStartEffects = false;
               boolean foundPveEffects = false;
               boolean foundPvpEffects = false;
               boolean foundEndEffects = false;
               boolean foundSelfEffects = false;
               this._currentSkill.currentLevel = i - lastLvl - enchantLevels1 - enchantLevels2 - enchantLevels3 - enchantLevels4;

               for (Node var48 = first; var48 != null; var48 = var48.getNextSibling()) {
                  if ("enchant5conditions".equalsIgnoreCase(var48.getNodeName())) {
                     foundConditions = true;
                     Condition condition = this.parseCondition(var48.getFirstChild(), this._currentSkill.currentSkills.get(i));
                     Node msg = var48.getAttributes().getNamedItem("msg");
                     Node msgId = var48.getAttributes().getNamedItem("msgId");
                     if (condition != null && msg != null) {
                        condition.setMessage(msg.getNodeValue());
                     } else if (condition != null && msgId != null) {
                        condition.setMessageId(Integer.decode(this.getValue(msgId.getNodeValue(), null)));
                        Node addName = var48.getAttributes().getNamedItem("addName");
                        if (addName != null && Integer.decode(this.getValue(msgId.getNodeValue(), null)) > 0) {
                           condition.addName();
                        }
                     }

                     this._currentSkill.currentSkills.get(i).attach(condition, false);
                  } else if ("enchant5effects".equalsIgnoreCase(var48.getNodeName())) {
                     foundEffects = true;
                     this.parseTemplate(var48, this._currentSkill.currentSkills.get(i));
                  } else if ("enchant5startEffects".equalsIgnoreCase(var48.getNodeName())) {
                     foundStartEffects = true;
                     this.parseTemplate(var48, this._currentSkill.currentSkills.get(i), EffectScope.START);
                  } else if ("enchant5channelingEffects".equalsIgnoreCase(var48.getNodeName())) {
                     foundChannelingEffects = true;
                     this.parseTemplate(var48, this._currentSkill.currentSkills.get(i), EffectScope.CHANNELING);
                  } else if ("enchant5pveEffects".equalsIgnoreCase(var48.getNodeName())) {
                     foundPveEffects = true;
                     this.parseTemplate(var48, this._currentSkill.currentSkills.get(i), EffectScope.PVE);
                  } else if ("enchant5pvpEffects".equalsIgnoreCase(var48.getNodeName())) {
                     foundPvpEffects = true;
                     this.parseTemplate(var48, this._currentSkill.currentSkills.get(i), EffectScope.PVP);
                  } else if ("enchant5endEffects".equalsIgnoreCase(var48.getNodeName())) {
                     foundEndEffects = true;
                     this.parseTemplate(var48, this._currentSkill.currentSkills.get(i), EffectScope.END);
                  } else if ("enchant5selfEffects".equalsIgnoreCase(var48.getNodeName())) {
                     foundSelfEffects = true;
                     this.parseTemplate(var48, this._currentSkill.currentSkills.get(i), EffectScope.SELF);
                  }
               }

               if (!foundConditions
                  || !foundEffects
                  || !foundChannelingEffects
                  || !foundStartEffects
                  || !foundPveEffects
                  || !foundPvpEffects
                  || !foundEndEffects
                  || !foundSelfEffects) {
                  this._currentSkill.currentLevel = lastLvl - 1;

                  for (Node var49 = first; var49 != null; var49 = var49.getNextSibling()) {
                     if (!foundConditions && "conditions".equalsIgnoreCase(var49.getNodeName())) {
                        Condition condition = this.parseCondition(var49.getFirstChild(), this._currentSkill.currentSkills.get(i));
                        Node msg = var49.getAttributes().getNamedItem("msg");
                        Node msgId = var49.getAttributes().getNamedItem("msgId");
                        if (condition != null && msg != null) {
                           condition.setMessage(msg.getNodeValue());
                        } else if (condition != null && msgId != null) {
                           condition.setMessageId(Integer.decode(this.getValue(msgId.getNodeValue(), null)));
                           Node addName = var49.getAttributes().getNamedItem("addName");
                           if (addName != null && Integer.decode(this.getValue(msgId.getNodeValue(), null)) > 0) {
                              condition.addName();
                           }
                        }

                        this._currentSkill.currentSkills.get(i).attach(condition, false);
                     } else if (!foundEffects && "effects".equalsIgnoreCase(var49.getNodeName())) {
                        this.parseTemplate(var49, this._currentSkill.currentSkills.get(i));
                     } else if (!foundStartEffects && "startEffects".equalsIgnoreCase(var49.getNodeName())) {
                        this.parseTemplate(var49, this._currentSkill.currentSkills.get(i), EffectScope.START);
                     } else if (!foundChannelingEffects && "channelingEffects".equalsIgnoreCase(var49.getNodeName())) {
                        this.parseTemplate(var49, this._currentSkill.currentSkills.get(i), EffectScope.CHANNELING);
                     } else if (!foundPveEffects && "pveEffects".equalsIgnoreCase(var49.getNodeName())) {
                        this.parseTemplate(var49, this._currentSkill.currentSkills.get(i), EffectScope.PVE);
                     } else if (!foundPvpEffects && "pvpEffects".equalsIgnoreCase(var49.getNodeName())) {
                        this.parseTemplate(var49, this._currentSkill.currentSkills.get(i), EffectScope.PVP);
                     } else if (!foundEndEffects && "endEffects".equalsIgnoreCase(var49.getNodeName())) {
                        this.parseTemplate(var49, this._currentSkill.currentSkills.get(i), EffectScope.END);
                     } else if (!foundSelfEffects && "selfEffects".equalsIgnoreCase(var49.getNodeName())) {
                        this.parseTemplate(var49, this._currentSkill.currentSkills.get(i), EffectScope.SELF);
                     }
                  }
               }
            }

            for (int i = lastLvl + enchantLevels1 + enchantLevels2 + enchantLevels3 + enchantLevels4 + enchantLevels5;
               i < lastLvl + enchantLevels1 + enchantLevels2 + enchantLevels3 + enchantLevels4 + enchantLevels5 + enchantLevels6;
               i++
            ) {
               boolean foundConditions = false;
               boolean foundEffects = false;
               boolean foundChannelingEffects = false;
               boolean foundStartEffects = false;
               boolean foundPveEffects = false;
               boolean foundPvpEffects = false;
               boolean foundEndEffects = false;
               boolean foundSelfEffects = false;
               this._currentSkill.currentLevel = i - lastLvl - enchantLevels1 - enchantLevels2 - enchantLevels3 - enchantLevels4 - enchantLevels5;

               for (Node var50 = first; var50 != null; var50 = var50.getNextSibling()) {
                  if ("enchant6conditions".equalsIgnoreCase(var50.getNodeName())) {
                     foundConditions = true;
                     Condition condition = this.parseCondition(var50.getFirstChild(), this._currentSkill.currentSkills.get(i));
                     Node msg = var50.getAttributes().getNamedItem("msg");
                     Node msgId = var50.getAttributes().getNamedItem("msgId");
                     if (condition != null && msg != null) {
                        condition.setMessage(msg.getNodeValue());
                     } else if (condition != null && msgId != null) {
                        condition.setMessageId(Integer.decode(this.getValue(msgId.getNodeValue(), null)));
                        Node addName = var50.getAttributes().getNamedItem("addName");
                        if (addName != null && Integer.decode(this.getValue(msgId.getNodeValue(), null)) > 0) {
                           condition.addName();
                        }
                     }

                     this._currentSkill.currentSkills.get(i).attach(condition, false);
                  } else if ("enchant6effects".equalsIgnoreCase(var50.getNodeName())) {
                     foundEffects = true;
                     this.parseTemplate(var50, this._currentSkill.currentSkills.get(i));
                  } else if ("enchant6startEffects".equalsIgnoreCase(var50.getNodeName())) {
                     foundStartEffects = true;
                     this.parseTemplate(var50, this._currentSkill.currentSkills.get(i), EffectScope.START);
                  } else if ("enchant6channelingEffects".equalsIgnoreCase(var50.getNodeName())) {
                     foundChannelingEffects = true;
                     this.parseTemplate(var50, this._currentSkill.currentSkills.get(i), EffectScope.CHANNELING);
                  } else if ("enchant6pveEffects".equalsIgnoreCase(var50.getNodeName())) {
                     foundPveEffects = true;
                     this.parseTemplate(var50, this._currentSkill.currentSkills.get(i), EffectScope.PVE);
                  } else if ("enchant6pvpEffects".equalsIgnoreCase(var50.getNodeName())) {
                     foundPvpEffects = true;
                     this.parseTemplate(var50, this._currentSkill.currentSkills.get(i), EffectScope.PVP);
                  } else if ("enchant6endEffects".equalsIgnoreCase(var50.getNodeName())) {
                     foundEndEffects = true;
                     this.parseTemplate(var50, this._currentSkill.currentSkills.get(i), EffectScope.END);
                  } else if ("enchant6selfEffects".equalsIgnoreCase(var50.getNodeName())) {
                     foundSelfEffects = true;
                     this.parseTemplate(var50, this._currentSkill.currentSkills.get(i), EffectScope.SELF);
                  }
               }

               if (!foundConditions
                  || !foundEffects
                  || !foundChannelingEffects
                  || !foundStartEffects
                  || !foundPveEffects
                  || !foundPvpEffects
                  || !foundEndEffects
                  || !foundSelfEffects) {
                  this._currentSkill.currentLevel = lastLvl - 1;

                  for (Node var51 = first; var51 != null; var51 = var51.getNextSibling()) {
                     if (!foundConditions && "conditions".equalsIgnoreCase(var51.getNodeName())) {
                        Condition condition = this.parseCondition(var51.getFirstChild(), this._currentSkill.currentSkills.get(i));
                        Node msg = var51.getAttributes().getNamedItem("msg");
                        Node msgId = var51.getAttributes().getNamedItem("msgId");
                        if (condition != null && msg != null) {
                           condition.setMessage(msg.getNodeValue());
                        } else if (condition != null && msgId != null) {
                           condition.setMessageId(Integer.decode(this.getValue(msgId.getNodeValue(), null)));
                           Node addName = var51.getAttributes().getNamedItem("addName");
                           if (addName != null && Integer.decode(this.getValue(msgId.getNodeValue(), null)) > 0) {
                              condition.addName();
                           }
                        }

                        this._currentSkill.currentSkills.get(i).attach(condition, false);
                     } else if (!foundEffects && "effects".equalsIgnoreCase(var51.getNodeName())) {
                        this.parseTemplate(var51, this._currentSkill.currentSkills.get(i));
                     } else if (!foundStartEffects && "startEffects".equalsIgnoreCase(var51.getNodeName())) {
                        this.parseTemplate(var51, this._currentSkill.currentSkills.get(i), EffectScope.START);
                     } else if (!foundChannelingEffects && "channelingEffects".equalsIgnoreCase(var51.getNodeName())) {
                        this.parseTemplate(var51, this._currentSkill.currentSkills.get(i), EffectScope.CHANNELING);
                     } else if (!foundPveEffects && "pveEffects".equalsIgnoreCase(var51.getNodeName())) {
                        this.parseTemplate(var51, this._currentSkill.currentSkills.get(i), EffectScope.PVE);
                     } else if (!foundPvpEffects && "pvpEffects".equalsIgnoreCase(var51.getNodeName())) {
                        this.parseTemplate(var51, this._currentSkill.currentSkills.get(i), EffectScope.PVP);
                     } else if (!foundEndEffects && "endEffects".equalsIgnoreCase(var51.getNodeName())) {
                        this.parseTemplate(var51, this._currentSkill.currentSkills.get(i), EffectScope.END);
                     } else if (!foundSelfEffects && "selfEffects".equalsIgnoreCase(var51.getNodeName())) {
                        this.parseTemplate(var51, this._currentSkill.currentSkills.get(i), EffectScope.SELF);
                     }
                  }
               }
            }

            for (int i = lastLvl + enchantLevels1 + enchantLevels2 + enchantLevels3 + enchantLevels4 + enchantLevels5 + enchantLevels6;
               i < lastLvl + enchantLevels1 + enchantLevels2 + enchantLevels3 + enchantLevels4 + enchantLevels5 + enchantLevels6 + enchantLevels7;
               i++
            ) {
               boolean foundConditions = false;
               boolean foundEffects = false;
               boolean foundChannelingEffects = false;
               boolean foundStartEffects = false;
               boolean foundPveEffects = false;
               boolean foundPvpEffects = false;
               boolean foundEndEffects = false;
               boolean foundSelfEffects = false;
               this._currentSkill.currentLevel = i
                  - lastLvl
                  - enchantLevels1
                  - enchantLevels2
                  - enchantLevels3
                  - enchantLevels4
                  - enchantLevels5
                  - enchantLevels6;

               for (Node var52 = first; var52 != null; var52 = var52.getNextSibling()) {
                  if ("enchant7conditions".equalsIgnoreCase(var52.getNodeName())) {
                     foundConditions = true;
                     Condition condition = this.parseCondition(var52.getFirstChild(), this._currentSkill.currentSkills.get(i));
                     Node msg = var52.getAttributes().getNamedItem("msg");
                     Node msgId = var52.getAttributes().getNamedItem("msgId");
                     if (condition != null && msg != null) {
                        condition.setMessage(msg.getNodeValue());
                     } else if (condition != null && msgId != null) {
                        condition.setMessageId(Integer.decode(this.getValue(msgId.getNodeValue(), null)));
                        Node addName = var52.getAttributes().getNamedItem("addName");
                        if (addName != null && Integer.decode(this.getValue(msgId.getNodeValue(), null)) > 0) {
                           condition.addName();
                        }
                     }

                     this._currentSkill.currentSkills.get(i).attach(condition, false);
                  } else if ("enchant7effects".equalsIgnoreCase(var52.getNodeName())) {
                     foundEffects = true;
                     this.parseTemplate(var52, this._currentSkill.currentSkills.get(i));
                  } else if ("enchant7startEffects".equalsIgnoreCase(var52.getNodeName())) {
                     foundStartEffects = true;
                     this.parseTemplate(var52, this._currentSkill.currentSkills.get(i), EffectScope.START);
                  } else if ("enchant7channelingEffects".equalsIgnoreCase(var52.getNodeName())) {
                     foundChannelingEffects = true;
                     this.parseTemplate(var52, this._currentSkill.currentSkills.get(i), EffectScope.CHANNELING);
                  } else if ("enchant7pveEffects".equalsIgnoreCase(var52.getNodeName())) {
                     foundPveEffects = true;
                     this.parseTemplate(var52, this._currentSkill.currentSkills.get(i), EffectScope.PVE);
                  } else if ("enchant7pvpEffects".equalsIgnoreCase(var52.getNodeName())) {
                     foundPvpEffects = true;
                     this.parseTemplate(var52, this._currentSkill.currentSkills.get(i), EffectScope.PVP);
                  } else if ("enchant7endEffects".equalsIgnoreCase(var52.getNodeName())) {
                     foundEndEffects = true;
                     this.parseTemplate(var52, this._currentSkill.currentSkills.get(i), EffectScope.END);
                  } else if ("enchant7selfEffects".equalsIgnoreCase(var52.getNodeName())) {
                     foundSelfEffects = true;
                     this.parseTemplate(var52, this._currentSkill.currentSkills.get(i), EffectScope.SELF);
                  }
               }

               if (!foundConditions
                  || !foundEffects
                  || !foundChannelingEffects
                  || !foundStartEffects
                  || !foundPveEffects
                  || !foundPvpEffects
                  || !foundEndEffects
                  || !foundSelfEffects) {
                  this._currentSkill.currentLevel = lastLvl - 1;

                  for (Node var53 = first; var53 != null; var53 = var53.getNextSibling()) {
                     if (!foundConditions && "conditions".equalsIgnoreCase(var53.getNodeName())) {
                        Condition condition = this.parseCondition(var53.getFirstChild(), this._currentSkill.currentSkills.get(i));
                        Node msg = var53.getAttributes().getNamedItem("msg");
                        Node msgId = var53.getAttributes().getNamedItem("msgId");
                        if (condition != null && msg != null) {
                           condition.setMessage(msg.getNodeValue());
                        } else if (condition != null && msgId != null) {
                           condition.setMessageId(Integer.decode(this.getValue(msgId.getNodeValue(), null)));
                           Node addName = var53.getAttributes().getNamedItem("addName");
                           if (addName != null && Integer.decode(this.getValue(msgId.getNodeValue(), null)) > 0) {
                              condition.addName();
                           }
                        }

                        this._currentSkill.currentSkills.get(i).attach(condition, false);
                     } else if (!foundEffects && "effects".equalsIgnoreCase(var53.getNodeName())) {
                        this.parseTemplate(var53, this._currentSkill.currentSkills.get(i));
                     } else if (!foundChannelingEffects && "startEffects".equalsIgnoreCase(var53.getNodeName())) {
                        this.parseTemplate(var53, this._currentSkill.currentSkills.get(i), EffectScope.START);
                     } else if (!foundChannelingEffects && "channelingEffects".equalsIgnoreCase(var53.getNodeName())) {
                        this.parseTemplate(var53, this._currentSkill.currentSkills.get(i), EffectScope.CHANNELING);
                     } else if (!foundPveEffects && "pveEffects".equalsIgnoreCase(var53.getNodeName())) {
                        this.parseTemplate(var53, this._currentSkill.currentSkills.get(i), EffectScope.PVE);
                     } else if (!foundPvpEffects && "pvpEffects".equalsIgnoreCase(var53.getNodeName())) {
                        this.parseTemplate(var53, this._currentSkill.currentSkills.get(i), EffectScope.PVP);
                     } else if (!foundEndEffects && "endEffects".equalsIgnoreCase(var53.getNodeName())) {
                        this.parseTemplate(var53, this._currentSkill.currentSkills.get(i), EffectScope.END);
                     } else if (!foundSelfEffects && "selfEffects".equalsIgnoreCase(var53.getNodeName())) {
                        this.parseTemplate(var53, this._currentSkill.currentSkills.get(i), EffectScope.SELF);
                     }
                  }
               }
            }

            for (int i = lastLvl + enchantLevels1 + enchantLevels2 + enchantLevels3 + enchantLevels4 + enchantLevels5 + enchantLevels6 + enchantLevels7;
               i
                  < lastLvl
                     + enchantLevels1
                     + enchantLevels2
                     + enchantLevels3
                     + enchantLevels4
                     + enchantLevels5
                     + enchantLevels6
                     + enchantLevels7
                     + enchantLevels8;
               i++
            ) {
               boolean foundConditions = false;
               boolean foundEffects = false;
               boolean foundChannelingEffects = false;
               boolean foundStartEffects = false;
               boolean foundPveEffects = false;
               boolean foundPvpEffects = false;
               boolean foundEndEffects = false;
               boolean foundSelfEffects = false;
               this._currentSkill.currentLevel = i
                  - lastLvl
                  - enchantLevels1
                  - enchantLevels2
                  - enchantLevels3
                  - enchantLevels4
                  - enchantLevels5
                  - enchantLevels6
                  - enchantLevels7;

               for (Node var54 = first; var54 != null; var54 = var54.getNextSibling()) {
                  if ("enchant8conditions".equalsIgnoreCase(var54.getNodeName())) {
                     foundConditions = true;
                     Condition condition = this.parseCondition(var54.getFirstChild(), this._currentSkill.currentSkills.get(i));
                     Node msg = var54.getAttributes().getNamedItem("msg");
                     Node msgId = var54.getAttributes().getNamedItem("msgId");
                     if (condition != null && msg != null) {
                        condition.setMessage(msg.getNodeValue());
                     } else if (condition != null && msgId != null) {
                        condition.setMessageId(Integer.decode(this.getValue(msgId.getNodeValue(), null)));
                        Node addName = var54.getAttributes().getNamedItem("addName");
                        if (addName != null && Integer.decode(this.getValue(msgId.getNodeValue(), null)) > 0) {
                           condition.addName();
                        }
                     }

                     this._currentSkill.currentSkills.get(i).attach(condition, false);
                  } else if ("enchant8effects".equalsIgnoreCase(var54.getNodeName())) {
                     foundEffects = true;
                     this.parseTemplate(var54, this._currentSkill.currentSkills.get(i));
                  } else if ("enchant8startEffects".equalsIgnoreCase(var54.getNodeName())) {
                     foundStartEffects = true;
                     this.parseTemplate(var54, this._currentSkill.currentSkills.get(i), EffectScope.START);
                  } else if ("enchant8channelingEffects".equalsIgnoreCase(var54.getNodeName())) {
                     foundChannelingEffects = true;
                     this.parseTemplate(var54, this._currentSkill.currentSkills.get(i), EffectScope.CHANNELING);
                  } else if ("enchant8pveEffects".equalsIgnoreCase(var54.getNodeName())) {
                     foundPveEffects = true;
                     this.parseTemplate(var54, this._currentSkill.currentSkills.get(i), EffectScope.PVE);
                  } else if ("enchant8pvpEffects".equalsIgnoreCase(var54.getNodeName())) {
                     foundPvpEffects = true;
                     this.parseTemplate(var54, this._currentSkill.currentSkills.get(i), EffectScope.PVP);
                  } else if ("enchant8endEffects".equalsIgnoreCase(var54.getNodeName())) {
                     foundEndEffects = true;
                     this.parseTemplate(var54, this._currentSkill.currentSkills.get(i), EffectScope.END);
                  } else if ("enchant8selfEffects".equalsIgnoreCase(var54.getNodeName())) {
                     foundSelfEffects = true;
                     this.parseTemplate(var54, this._currentSkill.currentSkills.get(i), EffectScope.SELF);
                  }
               }

               if (!foundConditions
                  || !foundEffects
                  || !foundChannelingEffects
                  || !foundStartEffects
                  || !foundPveEffects
                  || !foundPvpEffects
                  || !foundEndEffects
                  || !foundSelfEffects) {
                  this._currentSkill.currentLevel = lastLvl - 1;

                  for (Node var55 = first; var55 != null; var55 = var55.getNextSibling()) {
                     if (!foundConditions && "conditions".equalsIgnoreCase(var55.getNodeName())) {
                        Condition condition = this.parseCondition(var55.getFirstChild(), this._currentSkill.currentSkills.get(i));
                        Node msg = var55.getAttributes().getNamedItem("msg");
                        Node msgId = var55.getAttributes().getNamedItem("msgId");
                        if (condition != null && msg != null) {
                           condition.setMessage(msg.getNodeValue());
                        } else if (condition != null && msgId != null) {
                           condition.setMessageId(Integer.decode(this.getValue(msgId.getNodeValue(), null)));
                           Node addName = var55.getAttributes().getNamedItem("addName");
                           if (addName != null && Integer.decode(this.getValue(msgId.getNodeValue(), null)) > 0) {
                              condition.addName();
                           }
                        }

                        this._currentSkill.currentSkills.get(i).attach(condition, false);
                     } else if (!foundEffects && "effects".equalsIgnoreCase(var55.getNodeName())) {
                        this.parseTemplate(var55, this._currentSkill.currentSkills.get(i));
                     } else if (!foundStartEffects && "startEffects".equalsIgnoreCase(var55.getNodeName())) {
                        this.parseTemplate(var55, this._currentSkill.currentSkills.get(i), EffectScope.START);
                     } else if (!foundChannelingEffects && "channelingEffects".equalsIgnoreCase(var55.getNodeName())) {
                        this.parseTemplate(var55, this._currentSkill.currentSkills.get(i), EffectScope.CHANNELING);
                     } else if (!foundPveEffects && "pveEffects".equalsIgnoreCase(var55.getNodeName())) {
                        this.parseTemplate(var55, this._currentSkill.currentSkills.get(i), EffectScope.PVE);
                     } else if (!foundPvpEffects && "pvpEffects".equalsIgnoreCase(var55.getNodeName())) {
                        this.parseTemplate(var55, this._currentSkill.currentSkills.get(i), EffectScope.PVP);
                     } else if (!foundEndEffects && "endEffects".equalsIgnoreCase(var55.getNodeName())) {
                        this.parseTemplate(var55, this._currentSkill.currentSkills.get(i), EffectScope.END);
                     } else if (!foundSelfEffects && "selfEffects".equalsIgnoreCase(var55.getNodeName())) {
                        this.parseTemplate(var55, this._currentSkill.currentSkills.get(i), EffectScope.SELF);
                     }
                  }
               }
            }

            this._currentSkill.skills.addAll(this._currentSkill.currentSkills);
         }
      }
   }

   protected void parseInlineLevelTable(Node n, StatSet set, int level, int enchantRoute) {
      String nodeName = n.getNodeName().trim();
      NamedNodeMap attrs = n.getAttributes();
      if (enchantRoute == 0) {
         Node levelTableNode = attrs.getNamedItem("levelValues");
         if (levelTableNode != null) {
            String tableValue = levelTableNode.getNodeValue().trim();
            String[] values = tableValue.split("\\s+");
            if (values.length > 0 && level > 0 && level <= values.length) {
               set.set(nodeName, StringUtil.parseValue(values[level - 1]));
               return;
            }
         }
      }

      if (enchantRoute > 0) {
         Node subLevelNode = attrs.getNamedItem("subLevel" + enchantRoute + "Values");
         if (subLevelNode != null) {
            String tableValue = subLevelNode.getNodeValue().trim();
            String[] values = tableValue.split("\\s+");
            if (values.length > 0 && level > 0 && level <= values.length) {
               set.set(nodeName, StringUtil.parseValue(values[level - 1]));
               return;
            }
         }
      }
   }

   protected void parseInlineLevelTable(Node n, StatSet set, int level) {
      this.parseInlineLevelTable(n, set, level, 0);
   }

   private void makeSkills() {
      int count = 0;
      this._currentSkill.currentSkills = new ArrayList<>(
         this._currentSkill.sets.length
            + this._currentSkill.enchsets1.length
            + this._currentSkill.enchsets2.length
            + this._currentSkill.enchsets3.length
            + this._currentSkill.enchsets4.length
            + this._currentSkill.enchsets5.length
            + this._currentSkill.enchsets6.length
            + this._currentSkill.enchsets7.length
            + this._currentSkill.enchsets8.length
      );

      for (int i = 0; i < this._currentSkill.sets.length; i++) {
         StatSet set = this._currentSkill.sets[i];

         try {
            this._currentSkill.currentSkills.add(i, new Skill(set));
            count++;
         } catch (Exception var14) {
            this.LOGGER.log(Level.SEVERE, "Skill id=" + set.getInt("skill_id") + "level" + set.getInt("level"), (Throwable)var14);
         }
      }

      int count2 = count;

      for (int i = 0; i < this._currentSkill.enchsets1.length; i++) {
         StatSet set = this._currentSkill.enchsets1[i];

         try {
            this._currentSkill.currentSkills.add(count2 + i, new Skill(set));
            count++;
         } catch (Exception var13) {
            this.LOGGER.log(Level.SEVERE, "Skill id=" + set.getInt("skill_id") + "level" + set.getInt("level"), (Throwable)var13);
         }
      }

      count2 = count;

      for (int i = 0; i < this._currentSkill.enchsets2.length; i++) {
         StatSet set = this._currentSkill.enchsets2[i];

         try {
            this._currentSkill.currentSkills.add(count2 + i, new Skill(set));
            count++;
         } catch (Exception var12) {
            this.LOGGER.log(Level.SEVERE, "Skill id=" + set.getInt("skill_id") + "level" + set.getInt("level"), (Throwable)var12);
         }
      }

      count2 = count;

      for (int i = 0; i < this._currentSkill.enchsets3.length; i++) {
         StatSet set = this._currentSkill.enchsets3[i];

         try {
            this._currentSkill.currentSkills.add(count2 + i, new Skill(set));
            count++;
         } catch (Exception var11) {
            this.LOGGER.log(Level.SEVERE, "Skill id=" + set.getInt("skill_id") + "level" + set.getInt("level"), (Throwable)var11);
         }
      }

      count2 = count;

      for (int i = 0; i < this._currentSkill.enchsets4.length; i++) {
         StatSet set = this._currentSkill.enchsets4[i];

         try {
            this._currentSkill.currentSkills.add(count2 + i, new Skill(set));
            count++;
         } catch (Exception var10) {
            this.LOGGER.log(Level.SEVERE, "Skill id=" + set.getInt("skill_id") + "level" + set.getInt("level"), (Throwable)var10);
         }
      }

      count2 = count;

      for (int i = 0; i < this._currentSkill.enchsets5.length; i++) {
         StatSet set = this._currentSkill.enchsets5[i];

         try {
            this._currentSkill.currentSkills.add(count2 + i, new Skill(set));
            count++;
         } catch (Exception var9) {
            this.LOGGER.log(Level.SEVERE, "Skill id=" + set.getInt("skill_id") + "level" + set.getInt("level"), (Throwable)var9);
         }
      }

      count2 = count;

      for (int i = 0; i < this._currentSkill.enchsets6.length; i++) {
         StatSet set = this._currentSkill.enchsets6[i];

         try {
            this._currentSkill.currentSkills.add(count2 + i, new Skill(set));
            count++;
         } catch (Exception var8) {
            this.LOGGER.log(Level.SEVERE, "Skill id=" + set.getInt("skill_id") + "level" + set.getInt("level"), (Throwable)var8);
         }
      }

      count2 = count;

      for (int i = 0; i < this._currentSkill.enchsets7.length; i++) {
         StatSet set = this._currentSkill.enchsets7[i];

         try {
            this._currentSkill.currentSkills.add(count2 + i, new Skill(set));
            count++;
         } catch (Exception var7) {
            this.LOGGER.log(Level.SEVERE, "Skill id=" + set.getInt("skill_id") + "level" + set.getInt("level"), (Throwable)var7);
         }
      }

      count2 = count;

      for (int i = 0; i < this._currentSkill.enchsets8.length; i++) {
         StatSet set = this._currentSkill.enchsets8[i];

         try {
            this._currentSkill.currentSkills.add(count2 + i, new Skill(set));
            count++;
         } catch (Exception var6) {
            this.LOGGER.log(Level.SEVERE, "Skill id=" + set.getInt("skill_id") + "level" + set.getInt("level"), (Throwable)var6);
         }
      }
   }

   public List<Skill> getSkills() {
      return this._skillsInFile;
   }

   private class DocumentSkillDataHolder {
      public int id;
      public String name;
      public StatSet[] sets;
      public StatSet[] enchsets1;
      public StatSet[] enchsets2;
      public StatSet[] enchsets3;
      public StatSet[] enchsets4;
      public StatSet[] enchsets5;
      public StatSet[] enchsets6;
      public StatSet[] enchsets7;
      public StatSet[] enchsets8;
      public int currentLevel;
      public List<Skill> skills;
      public List<Skill> currentSkills;

      public DocumentSkillDataHolder() {
         Objects.requireNonNull(DocumentSkill.this);
         super();
         this.skills = new ArrayList<>();
         this.currentSkills = new ArrayList<>();
      }
   }
}
