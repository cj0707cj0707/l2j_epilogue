package org.l2jmobius.gameserver.util;

import java.io.File;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.parsers.DocumentBuilderFactory;
import org.l2jmobius.gameserver.data.enums.CategoryType;
import org.l2jmobius.gameserver.entity.actor.enums.creature.InstanceType;
import org.l2jmobius.gameserver.entity.actor.enums.creature.Race;
import org.l2jmobius.gameserver.entity.actor.enums.player.PlayerState;
import org.l2jmobius.gameserver.entity.actor.templates.DoorTemplate;
import org.l2jmobius.gameserver.entity.actor.templates.NpcTemplate;
import org.l2jmobius.gameserver.entity.item.ItemTemplate;
import org.l2jmobius.gameserver.entity.item.enums.BodyPart;
import org.l2jmobius.gameserver.entity.item.type.ArmorType;
import org.l2jmobius.gameserver.entity.item.type.WeaponType;
import org.l2jmobius.gameserver.mechanics.conditions.Condition;
import org.l2jmobius.gameserver.mechanics.conditions.ConditionCategoryType;
import org.l2jmobius.gameserver.mechanics.conditions.ConditionChangeWeapon;
import org.l2jmobius.gameserver.mechanics.conditions.ConditionGameChance;
import org.l2jmobius.gameserver.mechanics.conditions.ConditionGameTime;
import org.l2jmobius.gameserver.mechanics.conditions.ConditionLogicAnd;
import org.l2jmobius.gameserver.mechanics.conditions.ConditionLogicNot;
import org.l2jmobius.gameserver.mechanics.conditions.ConditionLogicOr;
import org.l2jmobius.gameserver.mechanics.conditions.ConditionMinDistance;
import org.l2jmobius.gameserver.mechanics.conditions.ConditionPlayerActiveEffectId;
import org.l2jmobius.gameserver.mechanics.conditions.ConditionPlayerActiveSkillId;
import org.l2jmobius.gameserver.mechanics.conditions.ConditionPlayerAgathionId;
import org.l2jmobius.gameserver.mechanics.conditions.ConditionPlayerCallPc;
import org.l2jmobius.gameserver.mechanics.conditions.ConditionPlayerCanCreateBase;
import org.l2jmobius.gameserver.mechanics.conditions.ConditionPlayerCanCreateOutpost;
import org.l2jmobius.gameserver.mechanics.conditions.ConditionPlayerCanEscape;
import org.l2jmobius.gameserver.mechanics.conditions.ConditionPlayerCanRefuelAirship;
import org.l2jmobius.gameserver.mechanics.conditions.ConditionPlayerCanResurrect;
import org.l2jmobius.gameserver.mechanics.conditions.ConditionPlayerCanSummon;
import org.l2jmobius.gameserver.mechanics.conditions.ConditionPlayerCanSummonSiegeGolem;
import org.l2jmobius.gameserver.mechanics.conditions.ConditionPlayerCanSweep;
import org.l2jmobius.gameserver.mechanics.conditions.ConditionPlayerCanTakeCastle;
import org.l2jmobius.gameserver.mechanics.conditions.ConditionPlayerCanTakeFort;
import org.l2jmobius.gameserver.mechanics.conditions.ConditionPlayerCanTransform;
import org.l2jmobius.gameserver.mechanics.conditions.ConditionPlayerCanUntransform;
import org.l2jmobius.gameserver.mechanics.conditions.ConditionPlayerCharges;
import org.l2jmobius.gameserver.mechanics.conditions.ConditionPlayerCheckAbnormal;
import org.l2jmobius.gameserver.mechanics.conditions.ConditionPlayerClassIdRestriction;
import org.l2jmobius.gameserver.mechanics.conditions.ConditionPlayerCloakStatus;
import org.l2jmobius.gameserver.mechanics.conditions.ConditionPlayerCp;
import org.l2jmobius.gameserver.mechanics.conditions.ConditionPlayerFlyMounted;
import org.l2jmobius.gameserver.mechanics.conditions.ConditionPlayerGrade;
import org.l2jmobius.gameserver.mechanics.conditions.ConditionPlayerHasCastle;
import org.l2jmobius.gameserver.mechanics.conditions.ConditionPlayerHasClanHall;
import org.l2jmobius.gameserver.mechanics.conditions.ConditionPlayerHasFort;
import org.l2jmobius.gameserver.mechanics.conditions.ConditionPlayerHasPet;
import org.l2jmobius.gameserver.mechanics.conditions.ConditionPlayerHp;
import org.l2jmobius.gameserver.mechanics.conditions.ConditionPlayerInsideZoneId;
import org.l2jmobius.gameserver.mechanics.conditions.ConditionPlayerInstanceId;
import org.l2jmobius.gameserver.mechanics.conditions.ConditionPlayerInvSize;
import org.l2jmobius.gameserver.mechanics.conditions.ConditionPlayerIsClanLeader;
import org.l2jmobius.gameserver.mechanics.conditions.ConditionPlayerIsHero;
import org.l2jmobius.gameserver.mechanics.conditions.ConditionPlayerIsPvpFlagged;
import org.l2jmobius.gameserver.mechanics.conditions.ConditionPlayerIsRidingStrider;
import org.l2jmobius.gameserver.mechanics.conditions.ConditionPlayerLandingZone;
import org.l2jmobius.gameserver.mechanics.conditions.ConditionPlayerLevel;
import org.l2jmobius.gameserver.mechanics.conditions.ConditionPlayerLevelRange;
import org.l2jmobius.gameserver.mechanics.conditions.ConditionPlayerMp;
import org.l2jmobius.gameserver.mechanics.conditions.ConditionPlayerPkCount;
import org.l2jmobius.gameserver.mechanics.conditions.ConditionPlayerPledgeClass;
import org.l2jmobius.gameserver.mechanics.conditions.ConditionPlayerRace;
import org.l2jmobius.gameserver.mechanics.conditions.ConditionPlayerRangeFromNpc;
import org.l2jmobius.gameserver.mechanics.conditions.ConditionPlayerServitorNpcId;
import org.l2jmobius.gameserver.mechanics.conditions.ConditionPlayerSex;
import org.l2jmobius.gameserver.mechanics.conditions.ConditionPlayerSiegeSide;
import org.l2jmobius.gameserver.mechanics.conditions.ConditionPlayerSouls;
import org.l2jmobius.gameserver.mechanics.conditions.ConditionPlayerState;
import org.l2jmobius.gameserver.mechanics.conditions.ConditionPlayerSubclass;
import org.l2jmobius.gameserver.mechanics.conditions.ConditionPlayerTransformationId;
import org.l2jmobius.gameserver.mechanics.conditions.ConditionPlayerVehicleMounted;
import org.l2jmobius.gameserver.mechanics.conditions.ConditionPlayerWeight;
import org.l2jmobius.gameserver.mechanics.conditions.ConditionSiegeZone;
import org.l2jmobius.gameserver.mechanics.conditions.ConditionSlotItemId;
import org.l2jmobius.gameserver.mechanics.conditions.ConditionTargetAbnormal;
import org.l2jmobius.gameserver.mechanics.conditions.ConditionTargetActiveEffectId;
import org.l2jmobius.gameserver.mechanics.conditions.ConditionTargetActiveSkillId;
import org.l2jmobius.gameserver.mechanics.conditions.ConditionTargetAggro;
import org.l2jmobius.gameserver.mechanics.conditions.ConditionTargetClassIdRestriction;
import org.l2jmobius.gameserver.mechanics.conditions.ConditionTargetInvSize;
import org.l2jmobius.gameserver.mechanics.conditions.ConditionTargetLevel;
import org.l2jmobius.gameserver.mechanics.conditions.ConditionTargetLevelRange;
import org.l2jmobius.gameserver.mechanics.conditions.ConditionTargetMyPartyExceptMe;
import org.l2jmobius.gameserver.mechanics.conditions.ConditionTargetNpcId;
import org.l2jmobius.gameserver.mechanics.conditions.ConditionTargetNpcType;
import org.l2jmobius.gameserver.mechanics.conditions.ConditionTargetPlayable;
import org.l2jmobius.gameserver.mechanics.conditions.ConditionTargetRace;
import org.l2jmobius.gameserver.mechanics.conditions.ConditionTargetUsesWeaponKind;
import org.l2jmobius.gameserver.mechanics.conditions.ConditionTargetWeight;
import org.l2jmobius.gameserver.mechanics.conditions.ConditionUsingItemType;
import org.l2jmobius.gameserver.mechanics.conditions.ConditionUsingSkill;
import org.l2jmobius.gameserver.mechanics.conditions.ConditionUsingSlotType;
import org.l2jmobius.gameserver.mechanics.conditions.ConditionWithSkill;
import org.l2jmobius.gameserver.mechanics.effects.AbstractEffect;
import org.l2jmobius.gameserver.mechanics.skill.AbnormalType;
import org.l2jmobius.gameserver.mechanics.skill.EffectScope;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.mechanics.stats.Stat;
import org.l2jmobius.gameserver.mechanics.stats.functions.FuncTemplate;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

public abstract class DocumentBase {
   protected final Logger LOGGER = Logger.getLogger(this.getClass().getName());
   private static final Set<String> IGNORED_ELEMENTS = Set.of(
      "param", "conditions", "effect", "add", "sub", "mul", "div", "set", "share", "enchant", "enchanthp"
   );
   private final File _file;
   protected final Map<String, String[]> _tables = new HashMap<>();

   protected DocumentBase(File pFile) {
      this._file = pFile;
   }

   public Document parse() {
      Document document = null;

      try {
         DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
         factory.setValidating(false);
         factory.setIgnoringComments(true);
         document = factory.newDocumentBuilder().parse(this._file);
         this.parseDocument(document);
      } catch (Exception var3) {
         this.LOGGER.log(Level.SEVERE, "Error loading file " + this._file, (Throwable)var3);
      }

      return document;
   }

   protected abstract void parseDocument(Document var1);

   protected abstract StatSet getStatSet();

   protected abstract String getTableValue(String var1);

   protected abstract String getTableValue(String var1, int var2);

   protected void resetTable() {
      this._tables.clear();
   }

   protected void setTable(String name, String[] table) {
      this._tables.put(name, table);
   }

   protected void parseTemplate(Node node, Object template) {
      this.parseTemplate(node, template, null);
   }

   protected void parseTemplate(Node node, Object template, EffectScope effectScope) {
      Condition condition = null;
      Node n = node.getFirstChild();
      if (n != null) {
         if ("conditions".equalsIgnoreCase(n.getNodeName())) {
            condition = this.parseCondition(n.getFirstChild(), template);
            Node msg = n.getAttributes().getNamedItem("msg");
            Node msgId = n.getAttributes().getNamedItem("msgId");
            if (condition != null && msg != null) {
               condition.setMessage(msg.getNodeValue());
            } else if (condition != null && msgId != null) {
               condition.setMessageId(Integer.decode(this.getValue(msgId.getNodeValue(), null)));
               Node addName = n.getAttributes().getNamedItem("addName");
               if (addName != null && Integer.decode(this.getValue(msgId.getNodeValue(), null)) > 0) {
                  condition.addName();
               }
            }

            n = n.getNextSibling();
         }

         for (; n != null; n = n.getNextSibling()) {
            String name = n.getNodeName().toLowerCase();
            switch (name) {
               case "effect":
                  if (template instanceof AbstractEffect) {
                     throw new RuntimeException("Nested effects");
                  }

                  this.attachEffect(n, template, condition, effectScope);
                  break;
               case "add":
               case "sub":
               case "mul":
               case "div":
               case "set":
               case "share":
               case "enchant":
               case "enchanthp":
                  if (n.getAttributes().getNamedItem("stat") == null && n.getAttributes().getNamedItem("val") == null) {
                     this.processAlternativeFuncFormat(n, template, name, condition);
                  } else {
                     this.attachFunc(n, template, name, condition);
                  }
            }
         }
      }
   }

   protected void attachFunc(Node n, Object template, String functionName, Condition attachCond) {
      NamedNodeMap attrs = n.getAttributes();
      Node statNode = attrs.getNamedItem("stat");
      if (statNode != null) {
         Stat stat = Stat.valueOfXml(statNode.getNodeValue());
         int order = -1;
         Node orderNode = attrs.getNamedItem("order");
         if (orderNode != null) {
            order = Integer.parseInt(orderNode.getNodeValue());
         }

         String valueString = null;
         Node valAttr = attrs.getNamedItem("val");
         if (valAttr != null) {
            valueString = valAttr.getNodeValue();
         } else {
            for (Node child = n.getFirstChild(); child != null; child = child.getNextSibling()) {
               if (child.getNodeType() == 1 && "value".equalsIgnoreCase(child.getNodeName())) {
                  valueString = child.getTextContent().trim();
                  break;
               }
            }

            if (valueString == null || valueString.isEmpty()) {
               valueString = n.getTextContent().trim();
            }
         }

         double value = 0.0;
         if (valueString != null && !valueString.isEmpty()) {
            value = valueString.charAt(0) == '#' ? Double.parseDouble(this.getTableValue(valueString)) : Double.parseDouble(valueString);
         }

         Condition applyCond = this.parseCondition(n.getFirstChild(), template);
         FuncTemplate ft = new FuncTemplate(attachCond, applyCond, functionName, order, stat, value);
         if (template instanceof ItemTemplate) {
            ((ItemTemplate)template).attach(ft);
         } else if (template instanceof AbstractEffect) {
            ((AbstractEffect)template).attach(ft);
         }
      }
   }

   protected void processAlternativeFuncFormat(Node n, Object template, String functionName, Condition attachCond) {
      for (Node statNode = n.getFirstChild(); statNode != null; statNode = statNode.getNextSibling()) {
         if (statNode.getNodeType() == 1) {
            String statName = statNode.getNodeName();
            String valueString = statNode.getTextContent().trim();

            try {
               Stat stat = Stat.valueOfXml(statName);
               double value;
               if (!valueString.isEmpty() && valueString.charAt(0) == '#') {
                  value = Double.parseDouble(this.getTableValue(valueString));
               } else {
                  value = Double.parseDouble(valueString);
               }

               int order = -1;
               Condition applyCond = this.parseCondition(statNode.getFirstChild(), template);
               FuncTemplate ft = new FuncTemplate(attachCond, applyCond, functionName, -1, stat, value);
               if (template instanceof ItemTemplate) {
                  ((ItemTemplate)template).attach(ft);
               } else {
                  if (!(template instanceof AbstractEffect)) {
                     throw new RuntimeException("Attaching stat to a non-effect template!!!");
                  }

                  ((AbstractEffect)template).attach(ft);
               }
            } catch (NumberFormatException var14) {
               this.LOGGER.warning("Invalid numeric value: '" + valueString + "' for stat: " + statName + ": " + var14.getMessage());
            } catch (IllegalArgumentException var15) {
               this.LOGGER.warning("Unknown stat name: " + statName + " in alternative " + functionName + " format: " + var15.getMessage());
            } catch (Exception var16) {
               this.LOGGER.warning("Error processing alternative " + functionName + " format for " + statName + ": " + var16.getMessage());
            }
         }
      }
   }

   protected void attachEffect(Node n, Object template, Condition attachCond) {
      this.attachEffect(n, template, attachCond, null);
   }

   protected void attachEffect(Node n, Object template, Condition attachCond, EffectScope effectScope) {
      NamedNodeMap attrs = n.getAttributes();
      StatSet set = new StatSet();

      for (int i = 0; i < attrs.getLength(); i++) {
         Node att = attrs.item(i);
         set.set(att.getNodeName(), this.getValue(att.getNodeValue(), template));
      }

      StatSet parameters = this.parseParameters(n.getFirstChild(), template);
      Condition applyCond = this.parseCondition(n.getFirstChild(), template);
      if (template instanceof ItemTemplate) {
         set.set("id", ((ItemTemplate)template).getId());
      } else if (template instanceof NpcTemplate) {
         set.set("id", ((NpcTemplate)template).getId());
      } else if (template instanceof DoorTemplate) {
         set.set("id", ((DoorTemplate)template).getId());
      }

      if ("RestorationRandom".equals(set.getString("name"))) {
         Node itemsNode = null;

         for (Node child = n.getFirstChild(); child != null; child = child.getNextSibling()) {
            if (child.getNodeType() == 1 && "items".equals(child.getNodeName())) {
               itemsNode = child;
               break;
            }
         }

         if (itemsNode != null) {
            Node firstChild = itemsNode.getFirstChild();

            while (firstChild != null && firstChild.getNodeType() != 1) {
               firstChild = firstChild.getNextSibling();
            }

            if (firstChild != null && "value".equals(firstChild.getNodeName())) {
               Skill skill = (Skill)template;

               for (Node valueNode = itemsNode.getFirstChild(); valueNode != null; valueNode = valueNode.getNextSibling()) {
                  if (valueNode.getNodeType() == 1 && "value".equals(valueNode.getNodeName())) {
                     NamedNodeMap valueAttrs = valueNode.getAttributes();
                     Node levelAttr = valueAttrs.getNamedItem("level");
                     int level = levelAttr != null ? Integer.parseInt(levelAttr.getNodeValue()) : 1;
                     if (skill.getLevel() == level) {
                        List<StatSet> items = new ArrayList<>();

                        for (Node itemNode = valueNode.getFirstChild(); itemNode != null; itemNode = itemNode.getNextSibling()) {
                           if (itemNode.getNodeType() == 1 && "item".equals(itemNode.getNodeName())) {
                              this.parseItemNode(itemNode, items);
                           }
                        }

                        parameters.set("items", items);
                     }
                  }
               }
            } else {
               List<StatSet> items = new ArrayList<>();

               for (Node itemNodex = itemsNode.getFirstChild(); itemNodex != null; itemNodex = itemNodex.getNextSibling()) {
                  if (itemNodex.getNodeType() == 1 && "item".equals(itemNodex.getNodeName())) {
                     this.parseItemNode(itemNodex, items);
                  }
               }

               parameters.set("items", items);
            }
         }
      }

      AbstractEffect effect = AbstractEffect.createEffect(attachCond, applyCond, set, parameters);
      this.parseTemplate(n, effect);
      if (template instanceof ItemTemplate) {
         this.LOGGER.severe("Item " + template + " with effects!!!");
      } else if (template instanceof Skill skill) {
         if (effectScope != null) {
            skill.addEffect(effectScope, effect);
         } else if (skill.isPassive()) {
            skill.addEffect(EffectScope.PASSIVE, effect);
         } else {
            skill.addEffect(EffectScope.GENERAL, effect);
         }
      }
   }

   private void parseItemNode(Node itemNode, List<StatSet> items) {
      StatSet group = new StatSet();
      NamedNodeMap attrs = itemNode.getAttributes();
      Node chanceNode = attrs.getNamedItem("chance");
      float chance = chanceNode != null ? Float.parseFloat(chanceNode.getNodeValue()) : 100.0F;
      group.set(".chance", chance);
      List<StatSet> innerItems = new ArrayList<>();

      for (Node innerNode = itemNode.getFirstChild(); innerNode != null; innerNode = innerNode.getNextSibling()) {
         if (innerNode.getNodeType() == 1 && "item".equals(innerNode.getNodeName())) {
            NamedNodeMap itemAttrs = innerNode.getAttributes();
            Node idNode = itemAttrs.getNamedItem("id");
            if (idNode == null) {
               this.LOGGER.warning("Missing item ID in RestorationRandom effect");
            } else {
               int id = Integer.parseInt(idNode.getNodeValue());
               if (id <= 0) {
                  this.LOGGER.warning("Invalid item ID found in RestorationRandom effect: " + id);
               } else {
                  Node countNode = itemAttrs.getNamedItem("count");
                  Node minEnchantNode = itemAttrs.getNamedItem("minEnchant");
                  Node maxEnchantNode = itemAttrs.getNamedItem("maxEnchant");
                  StatSet itemData = new StatSet();
                  itemData.set(".id", id);
                  itemData.set(".count", countNode != null ? Long.parseLong(countNode.getNodeValue()) : 1L);
                  itemData.set(".minEnchant", minEnchantNode != null ? Integer.parseInt(minEnchantNode.getNodeValue()) : 0);
                  itemData.set(".maxEnchant", maxEnchantNode != null ? Integer.parseInt(maxEnchantNode.getNodeValue()) : 0);
                  innerItems.add(itemData);
               }
            }
         }
      }

      group.set(".", innerItems);
      items.add(group);
   }

   private StatSet parseParameters(Node node, Object template) {
      StatSet parameters = null;

      for (Node n = node; n != null; n = n.getNextSibling()) {
         if (n.getNodeType() == 1 && "param".equals(n.getNodeName())) {
            if (parameters == null) {
               parameters = new StatSet();
            }

            NamedNodeMap params = n.getAttributes();

            for (int i = 0; i < params.getLength(); i++) {
               Node att = params.item(i);
               parameters.set(att.getNodeName(), this.getValue(att.getNodeValue(), template));
            }
         }
      }

      for (Node var9 = node; var9 != null; var9 = var9.getNextSibling()) {
         if (var9.getNodeType() == 1 && !IGNORED_ELEMENTS.contains(var9.getNodeName().toLowerCase())) {
            if (parameters == null) {
               parameters = new StatSet();
            }

            String name = var9.getNodeName();
            if (this.hasLevelSpecificValues(var9)) {
               Map<Integer, String> levelValues = this.parseLevelSpecificValues(var9, template);
               if (template instanceof Skill skill) {
                  String resolvedValue = levelValues.get(skill.getLevel());
                  if (resolvedValue != null) {
                     parameters.set(name, resolvedValue);
                  }
               } else if (template instanceof Integer) {
                  String resolvedValue = levelValues.get((Integer)template);
                  if (resolvedValue != null) {
                     parameters.set(name, resolvedValue);
                  }
               } else {
                  parameters.set(name, levelValues);
               }
            } else if (this.hasOnlyTextContent(var9)) {
               String value = var9.getTextContent().trim();
               parameters.set(name, this.getValue(value, template));
            }
         }
      }

      return parameters == null ? StatSet.EMPTY_STATSET : parameters;
   }

   protected Condition parseCondition(Node node, Object template) {
      Node n = node;

      while (n != null && (n.getNodeType() != 1 || "value".equalsIgnoreCase(n.getNodeName()))) {
         n = n.getNextSibling();
      }

      if (n == null) {
         return null;
      } else {
         String var4 = n.getNodeName().toLowerCase();
         switch (var4) {
            case "and":
               return this.parseLogicAnd(n, template);
            case "or":
               return this.parseLogicOr(n, template);
            case "not":
               return this.parseLogicNot(n, template);
            case "player":
               return this.parsePlayerCondition(n, template);
            case "target":
               return this.parseTargetCondition(n, template);
            case "using":
               return this.parseUsingCondition(n);
            case "game":
               return this.parseGameCondition(n);
            default:
               return null;
         }
      }
   }

   protected Condition parseLogicAnd(Node node, Object template) {
      ConditionLogicAnd cond = new ConditionLogicAnd();

      for (Node n = node.getFirstChild(); n != null; n = n.getNextSibling()) {
         if (n.getNodeType() == 1) {
            cond.add(this.parseCondition(n, template));
         }
      }

      if (cond.conditions == null || cond.conditions.length == 0) {
         this.LOGGER.severe("Empty <and> condition in " + this._file);
      }

      return cond;
   }

   protected Condition parseLogicOr(Node node, Object template) {
      ConditionLogicOr cond = new ConditionLogicOr();

      for (Node n = node.getFirstChild(); n != null; n = n.getNextSibling()) {
         if (n.getNodeType() == 1) {
            cond.add(this.parseCondition(n, template));
         }
      }

      if (cond.conditions == null || cond.conditions.length == 0) {
         this.LOGGER.severe("Empty <or> condition in " + this._file);
      }

      return cond;
   }

   protected Condition parseLogicNot(Node node, Object template) {
      for (Node n = node.getFirstChild(); n != null; n = n.getNextSibling()) {
         if (n.getNodeType() == 1) {
            return new ConditionLogicNot(this.parseCondition(n, template));
         }
      }

      this.LOGGER.severe("Empty <not> condition in " + this._file);
      return null;
   }

   protected Condition parsePlayerCondition(Node n, Object template) {
      Condition cond = null;
      NamedNodeMap attrs = n.getAttributes();

      for (int i = 0; i < attrs.getLength(); i++) {
         Node a = attrs.item(i);
         String var7 = a.getNodeName().toLowerCase();
         switch (var7) {
            case "races":
               String[] racesVal = a.getNodeValue().split(",");
               Set<Race> races = EnumSet.noneOf(Race.class);
               int r = 0;

               for (; r < racesVal.length; r++) {
                  if (racesVal[r] != null) {
                     races.add(Race.valueOf(racesVal[r]));
                  }
               }

               cond = this.joinAnd(cond, new ConditionPlayerRace(races));
               break;
            case "level":
               int lvl = Integer.decode(this.getValue(a.getNodeValue(), template));
               cond = this.joinAnd(cond, new ConditionPlayerLevel(lvl));
               break;
            case "levelrange":
               String[] range = this.getValue(a.getNodeValue(), template).split(";");
               if (range.length == 2) {
                  int[] lvlRange = new int[]{
                     Integer.decode(this.getValue(a.getNodeValue(), template).split(";")[0]),
                     Integer.decode(this.getValue(a.getNodeValue(), template).split(";")[1])
                  };
                  cond = this.joinAnd(cond, new ConditionPlayerLevelRange(lvlRange));
               }
               break;
            case "resting": {
               boolean val = Boolean.parseBoolean(a.getNodeValue());
               cond = this.joinAnd(cond, new ConditionPlayerState(PlayerState.RESTING, val));
               break;
            }
            case "flying": {
               boolean val = Boolean.parseBoolean(a.getNodeValue());
               cond = this.joinAnd(cond, new ConditionPlayerState(PlayerState.FLYING, val));
               break;
            }
            case "moving": {
               boolean val = Boolean.parseBoolean(a.getNodeValue());
               cond = this.joinAnd(cond, new ConditionPlayerState(PlayerState.MOVING, val));
               break;
            }
            case "running": {
               boolean val = Boolean.parseBoolean(a.getNodeValue());
               cond = this.joinAnd(cond, new ConditionPlayerState(PlayerState.RUNNING, val));
               break;
            }
            case "standing": {
               boolean val = Boolean.parseBoolean(a.getNodeValue());
               cond = this.joinAnd(cond, new ConditionPlayerState(PlayerState.STANDING, val));
               break;
            }
            case "behind": {
               boolean val = Boolean.parseBoolean(a.getNodeValue());
               cond = this.joinAnd(cond, new ConditionPlayerState(PlayerState.BEHIND, val));
               break;
            }
            case "front": {
               boolean val = Boolean.parseBoolean(a.getNodeValue());
               cond = this.joinAnd(cond, new ConditionPlayerState(PlayerState.FRONT, val));
               break;
            }
            case "chaotic": {
               boolean val = Boolean.parseBoolean(a.getNodeValue());
               cond = this.joinAnd(cond, new ConditionPlayerState(PlayerState.CHAOTIC, val));
               break;
            }
            case "olympiad": {
               boolean val = Boolean.parseBoolean(a.getNodeValue());
               cond = this.joinAnd(cond, new ConditionPlayerState(PlayerState.OLYMPIAD, val));
               break;
            }
            case "ishero": {
               boolean val = Boolean.parseBoolean(a.getNodeValue());
               cond = this.joinAnd(cond, new ConditionPlayerIsHero(val));
               break;
            }
            case "ispvpflagged": {
               boolean val = Boolean.parseBoolean(a.getNodeValue());
               cond = this.joinAnd(cond, new ConditionPlayerIsPvpFlagged(val));
               break;
            }
            case "transformationid":
               int id = Integer.parseInt(a.getNodeValue());
               cond = this.joinAnd(cond, new ConditionPlayerTransformationId(id));
               break;
            case "isridingstrider": {
               boolean val = Boolean.parseBoolean(a.getNodeValue());
               cond = this.joinAnd(cond, new ConditionPlayerIsRidingStrider(val));
               break;
            }
            case "hp":
               int hp = Integer.decode(this.getValue(a.getNodeValue(), null));
               cond = this.joinAnd(cond, new ConditionPlayerHp(hp));
               break;
            case "mp":
               int mp = Integer.decode(this.getValue(a.getNodeValue(), null));
               cond = this.joinAnd(cond, new ConditionPlayerMp(mp));
               break;
            case "cp":
               int cp = Integer.decode(this.getValue(a.getNodeValue(), null));
               cond = this.joinAnd(cond, new ConditionPlayerCp(cp));
               break;
            case "grade": {
               int expIndex = Integer.decode(this.getValue(a.getNodeValue(), template));
               cond = this.joinAnd(cond, new ConditionPlayerGrade(expIndex));
               break;
            }
            case "pkcount": {
               int expIndex = Integer.decode(this.getValue(a.getNodeValue(), template));
               cond = this.joinAnd(cond, new ConditionPlayerPkCount(expIndex));
               break;
            }
            case "siegezone": {
               int value = Integer.decode(this.getValue(a.getNodeValue(), null));
               cond = this.joinAnd(cond, new ConditionSiegeZone(value, true));
               break;
            }
            case "siegeside": {
               int value = Integer.decode(this.getValue(a.getNodeValue(), null));
               cond = this.joinAnd(cond, new ConditionPlayerSiegeSide(value));
               break;
            }
            case "charges": {
               int value = Integer.decode(this.getValue(a.getNodeValue(), template));
               cond = this.joinAnd(cond, new ConditionPlayerCharges(value));
               break;
            }
            case "souls": {
               int value = Integer.decode(this.getValue(a.getNodeValue(), template));
               cond = this.joinAnd(cond, new ConditionPlayerSouls(value));
               break;
            }
            case "weight":
               int weight = Integer.decode(this.getValue(a.getNodeValue(), null));
               cond = this.joinAnd(cond, new ConditionPlayerWeight(weight));
               break;
            case "invsize":
               int size = Integer.decode(this.getValue(a.getNodeValue(), null));
               cond = this.joinAnd(cond, new ConditionPlayerInvSize(size));
               break;
            case "isclanleader": {
               boolean val = Boolean.parseBoolean(a.getNodeValue());
               cond = this.joinAnd(cond, new ConditionPlayerIsClanLeader(val));
               break;
            }
            case "pledgeclass":
               int pledgeClass = Integer.decode(this.getValue(a.getNodeValue(), null));
               cond = this.joinAnd(cond, new ConditionPlayerPledgeClass(pledgeClass));
               break;
            case "clanhall":
               StringTokenizer st = new StringTokenizer(a.getNodeValue(), ",");
               List<Integer> array = new ArrayList<>(st.countTokens());

               while (st.hasMoreTokens()) {
                  String item = st.nextToken().trim();
                  array.add(Integer.decode(this.getValue(item, null)));
               }

               cond = this.joinAnd(cond, new ConditionPlayerHasClanHall(array));
               break;
            case "fort":
               int fort = Integer.decode(this.getValue(a.getNodeValue(), null));
               cond = this.joinAnd(cond, new ConditionPlayerHasFort(fort));
               break;
            case "castle":
               int castle = Integer.decode(this.getValue(a.getNodeValue(), null));
               cond = this.joinAnd(cond, new ConditionPlayerHasCastle(castle));
               break;
            case "sex":
               int sex = Integer.decode(this.getValue(a.getNodeValue(), null));
               cond = this.joinAnd(cond, new ConditionPlayerSex(sex));
               break;
            case "flymounted": {
               boolean val = Boolean.parseBoolean(a.getNodeValue());
               cond = this.joinAnd(cond, new ConditionPlayerFlyMounted(val));
               break;
            }
            case "vehiclemounted": {
               boolean val = Boolean.parseBoolean(a.getNodeValue());
               cond = this.joinAnd(cond, new ConditionPlayerVehicleMounted(val));
               break;
            }
            case "landingzone": {
               boolean val = Boolean.parseBoolean(a.getNodeValue());
               cond = this.joinAnd(cond, new ConditionPlayerLandingZone(val));
               break;
            }
            case "active_effect_id": {
               int effect_id = Integer.decode(this.getValue(a.getNodeValue(), template));
               cond = this.joinAnd(cond, new ConditionPlayerActiveEffectId(effect_id));
               break;
            }
            case "active_effect_id_lvl": {
               String val = this.getValue(a.getNodeValue(), template);
               int effect_id = Integer.decode(this.getValue(val.split(",")[0], template));
               int effect_lvl = Integer.decode(this.getValue(val.split(",")[1], template));
               cond = this.joinAnd(cond, new ConditionPlayerActiveEffectId(effect_id, effect_lvl));
               break;
            }
            case "active_skill_id": {
               int skill_id = Integer.decode(this.getValue(a.getNodeValue(), template));
               cond = this.joinAnd(cond, new ConditionPlayerActiveSkillId(skill_id));
               break;
            }
            case "active_skill_id_lvl": {
               String val = this.getValue(a.getNodeValue(), template);
               int skill_id = Integer.decode(this.getValue(val.split(",")[0], template));
               int skill_lvl = Integer.decode(this.getValue(val.split(",")[1], template));
               cond = this.joinAnd(cond, new ConditionPlayerActiveSkillId(skill_id, skill_lvl));
               break;
            }
            case "class_id_restriction":
               StringTokenizer st = new StringTokenizer(a.getNodeValue(), ",");
               Set<Integer> array = new HashSet<>(st.countTokens());

               while (st.hasMoreTokens()) {
                  String item = st.nextToken().trim();
                  array.add(Integer.decode(this.getValue(item, null)));
               }

               cond = this.joinAnd(cond, new ConditionPlayerClassIdRestriction(array));
               break;
            case "subclass": {
               boolean val = Boolean.parseBoolean(a.getNodeValue());
               cond = this.joinAnd(cond, new ConditionPlayerSubclass(val));
               break;
            }
            case "instanceid":
               StringTokenizer st = new StringTokenizer(a.getNodeValue(), ",");
               Set<Integer> set = new HashSet<>(st.countTokens());

               while (st.hasMoreTokens()) {
                  String item = st.nextToken().trim();
                  set.add(Integer.decode(this.getValue(item, null)));
               }

               cond = this.joinAnd(cond, new ConditionPlayerInstanceId(set));
               break;
            case "agathionid":
               int agathionId = Integer.decode(a.getNodeValue());
               cond = this.joinAnd(cond, new ConditionPlayerAgathionId(agathionId));
               break;
            case "cloakstatus": {
               boolean val = Boolean.parseBoolean(a.getNodeValue());
               cond = this.joinAnd(cond, new ConditionPlayerCloakStatus(val));
               break;
            }
            case "haspet":
               StringTokenizer st = new StringTokenizer(a.getNodeValue(), ",");
               List<Integer> array = new ArrayList<>(st.countTokens());

               while (st.hasMoreTokens()) {
                  String item = st.nextToken().trim();
                  array.add(Integer.decode(this.getValue(item, null)));
               }

               cond = this.joinAnd(cond, new ConditionPlayerHasPet(array));
               break;
            case "servitornpcid":
               StringTokenizer st = new StringTokenizer(a.getNodeValue(), ",");
               List<Integer> array = new ArrayList<>(st.countTokens());

               while (st.hasMoreTokens()) {
                  String item = st.nextToken().trim();
                  array.add(Integer.decode(this.getValue(item, null)));
               }

               cond = this.joinAnd(cond, new ConditionPlayerServitorNpcId(array));
               break;
            case "npcidradius": {
               StringTokenizer st = new StringTokenizer(a.getNodeValue(), ",");
               if (st.countTokens() != 3) {
                  break;
               }

               String[] ids = st.nextToken().split(";");
               Set<Integer> npcIds = new HashSet<>(ids.length);

               for (int index = 0; index < ids.length; index++) {
                  npcIds.add(Integer.parseInt(this.getValue(ids[index], template)));
               }

               int radius = Integer.parseInt(st.nextToken());
               boolean val = Boolean.parseBoolean(st.nextToken());
               cond = this.joinAnd(cond, new ConditionPlayerRangeFromNpc(npcIds, radius, val));
               break;
            }
            case "callpc":
               cond = this.joinAnd(cond, new ConditionPlayerCallPc(Boolean.parseBoolean(a.getNodeValue())));
               break;
            case "cancreatebase":
               cond = this.joinAnd(cond, new ConditionPlayerCanCreateBase(Boolean.parseBoolean(a.getNodeValue())));
               break;
            case "cancreateoutpost":
               cond = this.joinAnd(cond, new ConditionPlayerCanCreateOutpost(Boolean.parseBoolean(a.getNodeValue())));
               break;
            case "canescape":
               cond = this.joinAnd(cond, new ConditionPlayerCanEscape(Boolean.parseBoolean(a.getNodeValue())));
               break;
            case "canrefuelairship":
               cond = this.joinAnd(cond, new ConditionPlayerCanRefuelAirship(Integer.parseInt(a.getNodeValue())));
               break;
            case "canresurrect":
               cond = this.joinAnd(cond, new ConditionPlayerCanResurrect(Boolean.parseBoolean(a.getNodeValue())));
               break;
            case "cansummon":
               cond = this.joinAnd(cond, new ConditionPlayerCanSummon(Boolean.parseBoolean(a.getNodeValue())));
               break;
            case "cansummonsiegegolem":
               cond = this.joinAnd(cond, new ConditionPlayerCanSummonSiegeGolem(Boolean.parseBoolean(a.getNodeValue())));
               break;
            case "cansweep":
               cond = this.joinAnd(cond, new ConditionPlayerCanSweep(Boolean.parseBoolean(a.getNodeValue())));
               break;
            case "cantakecastle":
               cond = this.joinAnd(cond, new ConditionPlayerCanTakeCastle());
               break;
            case "cantakefort":
               cond = this.joinAnd(cond, new ConditionPlayerCanTakeFort(Boolean.parseBoolean(a.getNodeValue())));
               break;
            case "cantransform":
               cond = this.joinAnd(cond, new ConditionPlayerCanTransform(Boolean.parseBoolean(a.getNodeValue())));
               break;
            case "canuntransform":
               cond = this.joinAnd(cond, new ConditionPlayerCanUntransform(Boolean.parseBoolean(a.getNodeValue())));
               break;
            case "insidezoneid":
               StringTokenizer st = new StringTokenizer(a.getNodeValue(), ",");
               Set<Integer> set = new HashSet<>(st.countTokens());

               while (st.hasMoreTokens()) {
                  String item = st.nextToken().trim();
                  set.add(Integer.decode(this.getValue(item, null)));
               }

               cond = this.joinAnd(cond, new ConditionPlayerInsideZoneId(set));
               break;
            case "checkabnormal": {
               String value = a.getNodeValue();
               if (value.contains(",")) {
                  String[] values = value.split(",");
                  cond = this.joinAnd(
                     cond, new ConditionPlayerCheckAbnormal(AbnormalType.valueOf(values[0]), Integer.decode(this.getValue(values[1], template)))
                  );
               } else {
                  cond = this.joinAnd(cond, new ConditionPlayerCheckAbnormal(AbnormalType.valueOf(value)));
               }
               break;
            }
            case "categorytype": {
               String[] values = a.getNodeValue().split(",");
               Set<CategoryType> array = new HashSet<>(values.length);

               for (String value : values) {
                  array.add(CategoryType.valueOf(this.getValue(value, null)));
               }

               cond = this.joinAnd(cond, new ConditionCategoryType(array));
            }
         }
      }

      if (cond == null) {
         this.LOGGER.severe("Unrecognized <player> condition in " + this._file);
      }

      return cond;
   }

   protected Condition parseTargetCondition(Node n, Object template) {
      Condition cond = null;
      NamedNodeMap attrs = n.getAttributes();

      for (int i = 0; i < attrs.getLength(); i++) {
         Node a = attrs.item(i);
         String var7 = a.getNodeName().toLowerCase();
         switch (var7) {
            case "aggro": {
               boolean val = Boolean.parseBoolean(a.getNodeValue());
               cond = this.joinAnd(cond, new ConditionTargetAggro(val));
               break;
            }
            case "siegezone":
               int value = Integer.decode(this.getValue(a.getNodeValue(), null));
               cond = this.joinAnd(cond, new ConditionSiegeZone(value, false));
               break;
            case "level":
               int lvl = Integer.decode(this.getValue(a.getNodeValue(), template));
               cond = this.joinAnd(cond, new ConditionTargetLevel(lvl));
               break;
            case "levelrange":
               String[] range = this.getValue(a.getNodeValue(), template).split(";");
               if (range.length == 2) {
                  int[] lvlRange = new int[]{
                     Integer.decode(this.getValue(a.getNodeValue(), template).split(";")[0]),
                     Integer.decode(this.getValue(a.getNodeValue(), template).split(";")[1])
                  };
                  cond = this.joinAnd(cond, new ConditionTargetLevelRange(lvlRange));
               }
               break;
            case "mypartyexceptme":
               cond = this.joinAnd(cond, new ConditionTargetMyPartyExceptMe(Boolean.parseBoolean(a.getNodeValue())));
               break;
            case "playable":
               cond = this.joinAnd(cond, new ConditionTargetPlayable());
               break;
            case "class_id_restriction":
               StringTokenizer st = new StringTokenizer(a.getNodeValue(), ",");
               Set<Integer> set = new HashSet<>(st.countTokens());

               while (st.hasMoreTokens()) {
                  String item = st.nextToken().trim();
                  set.add(Integer.decode(this.getValue(item, null)));
               }

               cond = this.joinAnd(cond, new ConditionTargetClassIdRestriction(set));
               break;
            case "active_effect_id": {
               int effect_id = Integer.decode(this.getValue(a.getNodeValue(), template));
               cond = this.joinAnd(cond, new ConditionTargetActiveEffectId(effect_id));
               break;
            }
            case "active_effect_id_lvl": {
               String val = this.getValue(a.getNodeValue(), template);
               int effect_id = Integer.decode(this.getValue(val.split(",")[0], template));
               int effect_lvl = Integer.decode(this.getValue(val.split(",")[1], template));
               cond = this.joinAnd(cond, new ConditionTargetActiveEffectId(effect_id, effect_lvl));
               break;
            }
            case "active_skill_id": {
               int skill_id = Integer.decode(this.getValue(a.getNodeValue(), template));
               cond = this.joinAnd(cond, new ConditionTargetActiveSkillId(skill_id));
               break;
            }
            case "active_skill_id_lvl": {
               String val = this.getValue(a.getNodeValue(), template);
               int skill_id = Integer.decode(this.getValue(val.split(",")[0], template));
               int skill_lvl = Integer.decode(this.getValue(val.split(",")[1], template));
               cond = this.joinAnd(cond, new ConditionTargetActiveSkillId(skill_id, skill_lvl));
               break;
            }
            case "abnormal":
               int abnormalId = Integer.decode(this.getValue(a.getNodeValue(), template));
               cond = this.joinAnd(cond, new ConditionTargetAbnormal(abnormalId));
               break;
            case "mindistance":
               int distance = Integer.decode(this.getValue(a.getNodeValue(), null));
               cond = this.joinAnd(cond, new ConditionMinDistance(distance));
               break;
            case "race":
               cond = this.joinAnd(cond, new ConditionTargetRace(Race.valueOf(a.getNodeValue())));
               break;
            case "using":
               int mask = 0;
               StringTokenizer st = new StringTokenizer(a.getNodeValue(), ",");

               while (st.hasMoreTokens()) {
                  String item = st.nextToken().trim();

                  for (WeaponType wt : WeaponType.values()) {
                     if (wt.name().equals(item)) {
                        mask |= wt.mask();
                        break;
                     }
                  }

                  for (ArmorType at : ArmorType.values()) {
                     if (at.name().equals(item)) {
                        mask |= at.mask();
                        break;
                     }
                  }
               }

               cond = this.joinAnd(cond, new ConditionTargetUsesWeaponKind(mask));
               break;
            case "npcid":
               StringTokenizer st = new StringTokenizer(a.getNodeValue(), ",");
               Set<Integer> set = new HashSet<>(st.countTokens());

               while (st.hasMoreTokens()) {
                  String item = st.nextToken().trim();
                  set.add(Integer.decode(this.getValue(item, null)));
               }

               cond = this.joinAnd(cond, new ConditionTargetNpcId(set));
               break;
            case "npctype":
               String values = this.getValue(a.getNodeValue(), template).trim();
               String[] valuesSplit = values.split(",");
               InstanceType[] types = new InstanceType[valuesSplit.length];

               for (int j = 0; j < valuesSplit.length; j++) {
                  InstanceType type = Enum.valueOf(InstanceType.class, valuesSplit[j]);
                  if (type == null) {
                     throw new IllegalArgumentException("Instance type not recognized: " + valuesSplit[j]);
                  }

                  types[j] = type;
               }

               cond = this.joinAnd(cond, new ConditionTargetNpcType(types));
               break;
            case "weight":
               int weight = Integer.decode(this.getValue(a.getNodeValue(), null));
               cond = this.joinAnd(cond, new ConditionTargetWeight(weight));
               break;
            case "invsize":
               int size = Integer.decode(this.getValue(a.getNodeValue(), null));
               cond = this.joinAnd(cond, new ConditionTargetInvSize(size));
         }
      }

      if (cond == null) {
         this.LOGGER.severe("Unrecognized <target> condition in " + this._file);
      }

      return cond;
   }

   protected Condition parseUsingCondition(Node n) {
      Condition cond = null;
      NamedNodeMap attrs = n.getAttributes();

      for (int i = 0; i < attrs.getLength(); i++) {
         Node a = attrs.item(i);
         String var6 = a.getNodeName().toLowerCase();
         switch (var6) {
            case "kind":
               int mask = 0;
               StringTokenizer st = new StringTokenizer(a.getNodeValue(), ",");

               while (st.hasMoreTokens()) {
                  int old = mask;
                  String item = st.nextToken().trim();

                  for (WeaponType wt : WeaponType.values()) {
                     if (wt.name().equals(item)) {
                        mask |= wt.mask();
                     }
                  }

                  for (ArmorType at : ArmorType.values()) {
                     if (at.name().equals(item)) {
                        mask |= at.mask();
                     }
                  }

                  if (old == mask) {
                     this.LOGGER.info("[parseUsingCondition=\"kind\"] Unknown item type name: " + item);
                  }
               }

               cond = this.joinAnd(cond, new ConditionUsingItemType(mask));
               break;
            case "slot":
               int mask = 0;
               StringTokenizer st = new StringTokenizer(a.getNodeValue(), ",");

               while (st.hasMoreTokens()) {
                  int old = mask;
                  String item = st.nextToken().trim();
                  BodyPart bodyPart = BodyPart.fromName(item);
                  if (bodyPart != null) {
                     mask |= bodyPart.getMask();
                  }

                  if (old == mask) {
                     this.LOGGER.info("[parseUsingCondition=\"slot\"] Unknown item slot name: " + item);
                  }
               }

               cond = this.joinAnd(cond, new ConditionUsingSlotType(mask));
               break;
            case "skill": {
               int id = Integer.parseInt(a.getNodeValue());
               cond = this.joinAnd(cond, new ConditionUsingSkill(id));
               break;
            }
            case "slotitem": {
               StringTokenizer st = new StringTokenizer(a.getNodeValue(), ";");
               int id = Integer.parseInt(st.nextToken().trim());
               int slot = Integer.parseInt(st.nextToken().trim());
               int enchant = 0;
               if (st.hasMoreTokens()) {
                  enchant = Integer.parseInt(st.nextToken().trim());
               }

               cond = this.joinAnd(cond, new ConditionSlotItemId(slot, id, enchant));
               break;
            }
            case "weaponchange":
               boolean val = Boolean.parseBoolean(a.getNodeValue());
               cond = this.joinAnd(cond, new ConditionChangeWeapon(val));
         }
      }

      if (cond == null) {
         this.LOGGER.severe("Unrecognized <using> condition in " + this._file);
      }

      return cond;
   }

   protected Condition parseGameCondition(Node n) {
      Condition cond = null;
      NamedNodeMap attrs = n.getAttributes();

      for (int i = 0; i < attrs.getLength(); i++) {
         Node a = attrs.item(i);
         if ("skill".equalsIgnoreCase(a.getNodeName())) {
            boolean val = Boolean.parseBoolean(a.getNodeValue());
            cond = this.joinAnd(cond, new ConditionWithSkill(val));
         }

         if ("night".equalsIgnoreCase(a.getNodeName())) {
            boolean val = Boolean.parseBoolean(a.getNodeValue());
            cond = this.joinAnd(cond, new ConditionGameTime(val));
         }

         if ("chance".equalsIgnoreCase(a.getNodeName())) {
            int val = Integer.decode(this.getValue(a.getNodeValue(), null));
            cond = this.joinAnd(cond, new ConditionGameChance(val));
         }
      }

      if (cond == null) {
         this.LOGGER.severe("Unrecognized <game> condition in " + this._file);
      }

      return cond;
   }

   protected void parseTable(Node n) {
      NamedNodeMap attrs = n.getAttributes();
      String name = attrs.getNamedItem("name").getNodeValue();
      if (name.charAt(0) != '#') {
         throw new IllegalArgumentException("Table name must start with #");
      } else {
         StringTokenizer data = new StringTokenizer(n.getFirstChild().getNodeValue());
         List<String> array = new ArrayList<>(data.countTokens());

         while (data.hasMoreTokens()) {
            array.add(data.nextToken());
         }

         this.setTable(name, array.toArray(new String[0]));
      }
   }

   protected void parseBeanSet(Node n, StatSet set, Integer level) {
      NamedNodeMap attrs = n.getAttributes();
      Node nameNode = attrs.getNamedItem("name");
      if (nameNode != null) {
         String name = nameNode.getNodeValue().trim();
         Node valNode = attrs.getNamedItem("val");
         String value;
         if (valNode != null) {
            value = valNode.getNodeValue().trim();
         } else {
            value = n.getTextContent().trim();
         }

         if (!value.isEmpty()) {
            char ch = value.charAt(0);
            if (ch != '#' && ch != '-' && !Character.isDigit(ch)) {
               set.set(name, value);
            } else {
               set.set(name, this.getValue(value, level));
            }
         }
      }
   }

   protected void parseElementValue(Node n, StatSet set, Integer level) {
      String value = n.getTextContent().trim();
      if (!value.isEmpty()) {
         Node nameAttr = n.getAttributes().getNamedItem("name");
         String statName = nameAttr != null ? nameAttr.getNodeValue().trim() : n.getNodeName().trim();
         char ch = value.charAt(0);
         if (ch != '#' && ch != '-' && !Character.isDigit(ch)) {
            set.set(statName, value);
         } else {
            set.set(statName, this.getValue(value, level));
         }
      }
   }

   protected boolean hasOnlyTextContent(Node node) {
      for (Node child = node.getFirstChild(); child != null; child = child.getNextSibling()) {
         if (child.getNodeType() == 1) {
            return false;
         }
      }

      return true;
   }

   protected boolean hasLevelSpecificValues(Node node) {
      for (Node child = node.getFirstChild(); child != null; child = child.getNextSibling()) {
         if (child.getNodeType() == 1 && "value".equals(child.getNodeName())) {
            NamedNodeMap attributes = child.getAttributes();
            if (attributes != null && attributes.getNamedItem("level") != null) {
               return true;
            }
         }
      }

      return false;
   }

   protected Map<Integer, String> parseLevelSpecificValues(Node node, Object template) {
      Map<Integer, String> levelValues = new HashMap<>();

      for (Node child = node.getFirstChild(); child != null; child = child.getNextSibling()) {
         if (child.getNodeType() == 1 && "value".equals(child.getNodeName())) {
            NamedNodeMap attributes = child.getAttributes();
            Node levelAttr = attributes.getNamedItem("level");
            if (levelAttr != null) {
               int level = Integer.parseInt(levelAttr.getNodeValue());
               String value = child.getTextContent().trim();
               levelValues.put(level, this.getValue(value, template));
            }
         }
      }

      return levelValues;
   }

   protected String getValue(String value, Object template) {
      if (value == null || value.isEmpty() || value.charAt(0) != '#') {
         return value;
      } else if (template instanceof Skill) {
         return this.getTableValue(value);
      } else if (template instanceof Integer) {
         return this.getTableValue(value, (Integer)template);
      } else {
         throw new IllegalStateException();
      }
   }

   protected Condition joinAnd(Condition cond, Condition c) {
      if (cond == null) {
         return c;
      } else if (cond instanceof ConditionLogicAnd) {
         ((ConditionLogicAnd)cond).add(c);
         return cond;
      } else {
         ConditionLogicAnd and = new ConditionLogicAnd();
         and.add(cond);
         and.add(c);
         return and;
      }
   }
}
