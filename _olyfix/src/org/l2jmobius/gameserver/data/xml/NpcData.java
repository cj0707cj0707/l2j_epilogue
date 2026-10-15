package org.l2jmobius.gameserver.data.xml;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import org.l2jmobius.commons.util.IXmlReader;
import org.l2jmobius.gameserver.config.GeneralConfig;
import org.l2jmobius.gameserver.config.RatesConfig;
import org.l2jmobius.gameserver.config.custom.FakePlayersConfig;
import org.l2jmobius.gameserver.entity.actor.enums.npc.AISkillScope;
import org.l2jmobius.gameserver.entity.actor.enums.npc.DropType;
import org.l2jmobius.gameserver.entity.actor.enums.player.PlayerClass;
import org.l2jmobius.gameserver.entity.actor.holders.npc.DropGroupHolder;
import org.l2jmobius.gameserver.entity.actor.holders.npc.DropHolder;
import org.l2jmobius.gameserver.entity.actor.holders.npc.MinionHolder;
import org.l2jmobius.gameserver.entity.actor.templates.NpcTemplate;
import org.l2jmobius.gameserver.mechanics.effects.EffectType;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.mechanics.skill.holders.SkillHolder;
import org.l2jmobius.gameserver.util.ArrayUtil;
import org.l2jmobius.gameserver.util.StatSet;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

public class NpcData implements IXmlReader {
   private final Map<Integer, NpcTemplate> _npcs = new ConcurrentHashMap<>();
   private final Map<String, Integer> _clans = new ConcurrentHashMap<>();
   private static final Collection<Integer> _masterMonsterIDs = ConcurrentHashMap.newKeySet();
   private static Integer _genericClanId = null;

   protected NpcData() {
      this.load();
   }

   @Override
   public synchronized void load() {
      _masterMonsterIDs.clear();
      this.parseDatapackDirectory("data/stats/npcs", false);
      LOGGER.info(this.getClass().getSimpleName() + ": Loaded " + this._npcs.size() + " NPCs.");
      if (GeneralConfig.CUSTOM_NPC_DATA) {
         int npcCount = this._npcs.size();
         this.parseDatapackDirectory("data/stats/npcs/custom", true);
         LOGGER.info(this.getClass().getSimpleName() + ": Loaded " + (this._npcs.size() - npcCount) + " custom NPCs.");
      }

      this.loadNpcsSkillLearn();
   }

   @Override
   public void parseDocument(Document document, File file) {
      for (Node node = document.getFirstChild(); node != null; node = node.getNextSibling()) {
         if ("list".equalsIgnoreCase(node.getNodeName())) {
            for (Node listNode = node.getFirstChild(); listNode != null; listNode = listNode.getNextSibling()) {
               if ("npc".equalsIgnoreCase(listNode.getNodeName())) {
                  NamedNodeMap attrs = listNode.getAttributes();
                  StatSet set = new StatSet(new HashMap<>());
                  int npcId = this.parseInteger(attrs, "id");
                  int level = this.parseInteger(attrs, "level", 85);
                  String type = this.parseString(attrs, "type", "Folk");
                  Map<String, Object> parameters = null;
                  Map<Integer, Skill> skills = null;
                  Set<Integer> clans = null;
                  Set<Integer> ignoreClanNpcIds = null;
                  List<DropHolder> dropLists = null;
                  List<DropGroupHolder> dropGroups = null;
                  set.set("id", npcId);
                  set.set("displayId", this.parseInteger(attrs, "displayId"));
                  set.set("level", level);
                  set.set("type", type);
                  set.set("name", this.parseString(attrs, "name"));
                  set.set("usingServerSideName", this.parseBoolean(attrs, "usingServerSideName"));
                  set.set("title", this.parseString(attrs, "title"));
                  set.set("usingServerSideTitle", this.parseBoolean(attrs, "usingServerSideTitle"));

                  for (Node npcNode = listNode.getFirstChild(); npcNode != null; npcNode = npcNode.getNextSibling()) {
                     attrs = npcNode.getAttributes();
                     String aiSkillLists = npcNode.getNodeName().toLowerCase();
                     switch (aiSkillLists) {
                        case "parameters":
                           if (parameters == null) {
                              parameters = new HashMap<>();
                           }

                           for (Node parametersNode = npcNode.getFirstChild(); parametersNode != null; parametersNode = parametersNode.getNextSibling()) {
                              attrs = parametersNode.getAttributes();
                              String var54 = parametersNode.getNodeName().toLowerCase();
                              switch (var54) {
                                 case "param":
                                    parameters.put(this.parseString(attrs, "name"), this.parseString(attrs, "value"));
                                    break;
                                 case "skill":
                                    parameters.put(
                                       this.parseString(attrs, "name"), new SkillHolder(this.parseInteger(attrs, "id"), this.parseInteger(attrs, "level"))
                                    );
                                    break;
                                 case "minions":
                                    List<MinionHolder> minions = new ArrayList<>(1);
                                    Node minionsNode = parametersNode.getFirstChild();

                                    for (; minionsNode != null; minionsNode = minionsNode.getNextSibling()) {
                                       if (minionsNode.getNodeName().equalsIgnoreCase("npc")) {
                                          attrs = minionsNode.getAttributes();
                                          minions.add(
                                             new MinionHolder(
                                                this.parseInteger(attrs, "id"),
                                                this.parseInteger(attrs, "count"),
                                                this.parseInteger(attrs, "max", 0),
                                                this.parseInteger(attrs, "respawnTime").intValue(),
                                                this.parseInteger(attrs, "weightPoint", 0)
                                             )
                                          );
                                       }
                                    }

                                    if (!minions.isEmpty()) {
                                       parameters.put(this.parseString(parametersNode.getAttributes(), "name"), minions);
                                    }
                              }
                           }
                           break;
                        case "race":
                        case "sex":
                           set.set(npcNode.getNodeName(), npcNode.getTextContent().toUpperCase());
                           break;
                        case "equipment":
                           set.set("chestId", this.parseInteger(attrs, "chest"));
                           set.set("rhandId", this.parseInteger(attrs, "rhand"));
                           set.set("lhandId", this.parseInteger(attrs, "lhand"));
                           set.set("weaponEnchant", this.parseInteger(attrs, "weaponEnchant"));
                           break;
                        case "acquire":
                           set.set("exp", this.parseDouble(attrs, "exp"));
                           set.set("sp", this.parseDouble(attrs, "sp"));
                           set.set("raidPoints", this.parseDouble(attrs, "raidPoints"));
                           break;
                        case "stats":
                           set.set("baseSTR", this.parseInteger(attrs, "str"));
                           set.set("baseINT", this.parseInteger(attrs, "int"));
                           set.set("baseDEX", this.parseInteger(attrs, "dex"));
                           set.set("baseWIT", this.parseInteger(attrs, "wit"));
                           set.set("baseCON", this.parseInteger(attrs, "con"));
                           set.set("baseMEN", this.parseInteger(attrs, "men"));

                           for (Node statsNode = npcNode.getFirstChild(); statsNode != null; statsNode = statsNode.getNextSibling()) {
                              attrs = statsNode.getAttributes();
                              String var53 = statsNode.getNodeName().toLowerCase();
                              switch (var53) {
                                 case "vitals":
                                    set.set("baseHpMax", this.parseDouble(attrs, "hp"));
                                    set.set("baseHpReg", this.parseDouble(attrs, "hpRegen"));
                                    set.set("baseMpMax", this.parseDouble(attrs, "mp"));
                                    set.set("baseMpReg", this.parseDouble(attrs, "mpRegen"));
                                    break;
                                 case "attack":
                                    set.set("basePAtk", this.parseDouble(attrs, "physical"));
                                    set.set("baseMAtk", this.parseDouble(attrs, "magical"));
                                    set.set("baseRndDam", this.parseInteger(attrs, "random"));
                                    set.set("baseCritRate", this.parseInteger(attrs, "critical"));
                                    set.set("accuracy", this.parseDouble(attrs, "accuracy"));
                                    set.set("basePAtkSpd", this.parseInteger(attrs, "attackSpeed"));
                                    set.set("reuseDelay", this.parseInteger(attrs, "reuseDelay"));
                                    set.set("baseAtkType", this.parseString(attrs, "type"));
                                    set.set("baseAtkRange", this.parseInteger(attrs, "range"));
                                    set.set("distance", this.parseInteger(attrs, "distance"));
                                    set.set("width", this.parseInteger(attrs, "width"));
                                    break;
                                 case "defence":
                                    set.set("basePDef", this.parseDouble(attrs, "physical"));
                                    set.set("baseMDef", this.parseDouble(attrs, "magical"));
                                    set.set("evasion", this.parseInteger(attrs, "evasion"));
                                    set.set("baseShldDef", this.parseInteger(attrs, "shield"));
                                    set.set("baseShldRate", this.parseInteger(attrs, "shieldRate"));
                                    break;
                                 case "attribute":
                                    for (Node attributeNode = statsNode.getFirstChild(); attributeNode != null; attributeNode = attributeNode.getNextSibling()) {
                                       attrs = attributeNode.getAttributes();
                                       String var71 = attributeNode.getNodeName().toLowerCase();
                                       switch (var71) {
                                          case "attack":
                                             String attackAttributeType = this.parseString(attrs, "type");
                                             String var82 = attackAttributeType.toUpperCase();
                                             switch (var82) {
                                                case "FIRE":
                                                   set.set("baseFire", this.parseInteger(attrs, "value"));
                                                   continue;
                                                case "WATER":
                                                   set.set("baseWater", this.parseInteger(attrs, "value"));
                                                   continue;
                                                case "WIND":
                                                   set.set("baseWind", this.parseInteger(attrs, "value"));
                                                   continue;
                                                case "EARTH":
                                                   set.set("baseEarth", this.parseInteger(attrs, "value"));
                                                   continue;
                                                case "DARK":
                                                   set.set("baseDark", this.parseInteger(attrs, "value"));
                                                   continue;
                                                case "HOLY":
                                                   set.set("baseHoly", this.parseInteger(attrs, "value"));
                                                default:
                                                   continue;
                                             }
                                          case "defence":
                                             set.set("baseFireRes", this.parseInteger(attrs, "fire"));
                                             set.set("baseWaterRes", this.parseInteger(attrs, "water"));
                                             set.set("baseWindRes", this.parseInteger(attrs, "wind"));
                                             set.set("baseEarthRes", this.parseInteger(attrs, "earth"));
                                             set.set("baseHolyRes", this.parseInteger(attrs, "holy"));
                                             set.set("baseDarkRes", this.parseInteger(attrs, "dark"));
                                             set.set("baseElementRes", this.parseInteger(attrs, "default"));
                                       }
                                    }
                                    break;
                                 case "speed":
                                    for (Node speedNode = statsNode.getFirstChild(); speedNode != null; speedNode = speedNode.getNextSibling()) {
                                       attrs = speedNode.getAttributes();
                                       String var70 = speedNode.getNodeName().toLowerCase();
                                       switch (var70) {
                                          case "walk":
                                             double groundWalk = this.parseDouble(attrs, "ground");
                                             set.set("baseWalkSpd", groundWalk <= 0.0 ? 0.1 : groundWalk);
                                             set.set("baseSwimWalkSpd", this.parseDouble(attrs, "swim"));
                                             set.set("baseFlyWalkSpd", this.parseDouble(attrs, "fly"));
                                             break;
                                          case "run":
                                             double runSpeed = this.parseDouble(attrs, "ground");
                                             set.set("baseRunSpd", runSpeed <= 0.0 ? 0.1 : runSpeed);
                                             set.set("baseSwimRunSpd", this.parseDouble(attrs, "swim"));
                                             set.set("baseFlyRunSpd", this.parseDouble(attrs, "fly"));
                                       }
                                    }
                                    break;
                                 case "hittime":
                                    set.set("hitTime", npcNode.getTextContent());
                              }
                           }
                           break;
                        case "status":
                           set.set("unique", this.parseBoolean(attrs, "unique"));
                           set.set("attackable", this.parseBoolean(attrs, "attackable"));
                           set.set("targetable", this.parseBoolean(attrs, "targetable"));
                           set.set("talkable", this.parseBoolean(attrs, "talkable"));
                           set.set("undying", this.parseBoolean(attrs, "undying"));
                           set.set("showName", this.parseBoolean(attrs, "showName"));
                           set.set("randomWalk", this.parseBoolean(attrs, "randomWalk"));
                           set.set("randomAnimation", this.parseBoolean(attrs, "randomAnimation"));
                           set.set("flying", this.parseBoolean(attrs, "flying"));
                           set.set("canMove", this.parseBoolean(attrs, "canMove"));
                           set.set("noSleepMode", this.parseBoolean(attrs, "noSleepMode"));
                           set.set("passableDoor", this.parseBoolean(attrs, "passableDoor"));
                           set.set("hasSummoner", this.parseBoolean(attrs, "hasSummoner"));
                           set.set("canBeSown", this.parseBoolean(attrs, "canBeSown"));
                           break;
                        case "fakeplayer":
                           set.set("fakePlayer", true);
                           set.set("classId", this.parseInteger(attrs, "classId", 1));
                           set.set("hair", this.parseInteger(attrs, "hair", 1));
                           set.set("hairColor", this.parseInteger(attrs, "hairColor", 1));
                           set.set("face", this.parseInteger(attrs, "face", 1));
                           set.set("nameColor", this.parseInteger(attrs, "nameColor", 16777215));
                           set.set("titleColor", this.parseInteger(attrs, "titleColor", 15530402));
                           set.set("equipHead", this.parseInteger(attrs, "equipHead", 0));
                           set.set("equipRHand", this.parseInteger(attrs, "equipRHand", 0));
                           set.set("equipLHand", this.parseInteger(attrs, "equipLHand", 0));
                           set.set("equipGloves", this.parseInteger(attrs, "equipGloves", 0));
                           set.set("equipChest", this.parseInteger(attrs, "equipChest", 0));
                           set.set("equipLegs", this.parseInteger(attrs, "equipLegs", 0));
                           set.set("equipFeet", this.parseInteger(attrs, "equipFeet", 0));
                           set.set("equipCloak", this.parseInteger(attrs, "equipCloak", 0));
                           set.set("equipHair", this.parseInteger(attrs, "equipHair", 0));
                           set.set("equipHair2", this.parseInteger(attrs, "equipHair2", 0));
                           set.set("agathionId", this.parseInteger(attrs, "agathionId", 0));
                           set.set("weaponEnchantLevel", this.parseInteger(attrs, "weaponEnchantLevel", 0));
                           set.set("armorEnchantLevel", this.parseInteger(attrs, "armorEnchantLevel", 0));
                           set.set("fishing", this.parseBoolean(attrs, "fishing", false));
                           set.set("baitLocationX", this.parseInteger(attrs, "baitLocationX", 0));
                           set.set("baitLocationY", this.parseInteger(attrs, "baitLocationY", 0));
                           set.set("baitLocationZ", this.parseInteger(attrs, "baitLocationZ", 0));
                           set.set("recommends", this.parseInteger(attrs, "recommends", 0));
                           set.set("nobleLevel", this.parseInteger(attrs, "nobleLevel", 0));
                           set.set("hero", this.parseBoolean(attrs, "hero", false));
                           set.set("clanId", this.parseInteger(attrs, "clanId", 0));
                           set.set("pledgeStatus", this.parseInteger(attrs, "pledgeStatus", 0));
                           set.set("sitting", this.parseBoolean(attrs, "sitting", false));
                           set.set("privateStoreType", this.parseInteger(attrs, "privateStoreType", 0));
                           set.set("privateStoreMessage", this.parseString(attrs, "privateStoreMessage", ""));
                           set.set("fakePlayerTalkable", this.parseBoolean(attrs, "fakePlayerTalkable", true));
                           break;
                        case "skilllist":
                           skills = new HashMap<>();

                           for (Node skillListNode = npcNode.getFirstChild(); skillListNode != null; skillListNode = skillListNode.getNextSibling()) {
                              if ("skill".equalsIgnoreCase(skillListNode.getNodeName())) {
                                 attrs = skillListNode.getAttributes();
                                 int skillId = this.parseInteger(attrs, "id");
                                 int skillLevel = this.parseInteger(attrs, "level");
                                 Skill skill = SkillData.getInstance().getSkill(skillId, skillLevel);
                                 if (skill != null) {
                                    skills.put(skill.getId(), skill);
                                 } else {
                                    LOGGER.warning(
                                       "[" + file.getName() + "] skill not found. NPC ID: " + npcId + " Skill ID:" + skillId + " Skill Level: " + skillLevel
                                    );
                                 }
                              }
                           }
                           break;
                        case "shots":
                           set.set("soulShot", this.parseInteger(attrs, "soul"));
                           set.set("spiritShot", this.parseInteger(attrs, "spirit"));
                           set.set("soulShotChance", this.parseInteger(attrs, "shotChance"));
                           set.set("spiritShotChance", this.parseInteger(attrs, "spiritChance"));
                           break;
                        case "corpsetime":
                           set.set("corpseTime", npcNode.getTextContent());
                           break;
                        case "excrteffect":
                           set.set("exCrtEffect", npcNode.getTextContent());
                           break;
                        case "snpcprophprate":
                           set.set("sNpcPropHpRate", npcNode.getTextContent());
                           break;
                        case "ai":
                           set.set("aiType", this.parseString(attrs, "type"));
                           set.set("aggroRange", this.parseInteger(attrs, "aggroRange"));
                           set.set("clanHelpRange", this.parseInteger(attrs, "clanHelpRange"));
                           set.set("isChaos", this.parseBoolean(attrs, "isChaos"));
                           set.set("isAggressive", this.parseBoolean(attrs, "isAggressive"));

                           for (Node aiNode = npcNode.getFirstChild(); aiNode != null; aiNode = aiNode.getNextSibling()) {
                              attrs = aiNode.getAttributes();
                              String var51 = aiNode.getNodeName().toLowerCase();
                              switch (var51) {
                                 case "skill":
                                    set.set("minSkillChance", this.parseInteger(attrs, "minChance"));
                                    set.set("maxSkillChance", this.parseInteger(attrs, "maxChance"));
                                    set.set("primarySkillId", this.parseInteger(attrs, "primaryId"));
                                    set.set("shortRangeSkillId", this.parseInteger(attrs, "shortRangeId"));
                                    set.set("shortRangeSkillChance", this.parseInteger(attrs, "shortRangeChance"));
                                    set.set("longRangeSkillId", this.parseInteger(attrs, "longRangeId"));
                                    set.set("longRangeSkillChance", this.parseInteger(attrs, "longRangeChance"));
                                    break;
                                 case "clanlist":
                                    for (Node clanListNode = aiNode.getFirstChild(); clanListNode != null; clanListNode = clanListNode.getNextSibling()) {
                                       attrs = clanListNode.getAttributes();
                                       String var69 = clanListNode.getNodeName().toLowerCase();
                                       switch (var69) {
                                          case "clan":
                                             if (clans == null) {
                                                clans = new HashSet<>(1);
                                             }

                                             clans.add(this.getOrCreateClanId(clanListNode.getTextContent()));
                                             break;
                                          case "ignorenpcid":
                                             if (ignoreClanNpcIds == null) {
                                                ignoreClanNpcIds = new HashSet<>(1);
                                             }

                                             ignoreClanNpcIds.add(Integer.parseInt(clanListNode.getTextContent()));
                                       }
                                    }
                              }
                           }
                           break;
                        case "droplists":
                           for (Node dropListsNode = npcNode.getFirstChild(); dropListsNode != null; dropListsNode = dropListsNode.getNextSibling()) {
                              DropType dropType = null;

                              try {
                                 dropType = Enum.valueOf(DropType.class, dropListsNode.getNodeName().toUpperCase());
                              } catch (Exception var28) {
                              }

                              if (dropType != null) {
                                 for (Node dropNode = dropListsNode.getFirstChild(); dropNode != null; dropNode = dropNode.getNextSibling()) {
                                    String nodeName = dropNode.getNodeName();
                                    if (!nodeName.equalsIgnoreCase("group")) {
                                       if (nodeName.equalsIgnoreCase("item")) {
                                          if (dropLists == null) {
                                             dropLists = new ArrayList<>();
                                          }

                                          NamedNodeMap dropAttrs = dropNode.getAttributes();
                                          int itemId = this.parseInteger(dropAttrs, "id");
                                          if (ItemData.getInstance().getTemplate(itemId) == null) {
                                             LOGGER.warning(this.getClass().getSimpleName() + ": Could not find drop with id " + itemId + ".");
                                          } else {
                                             dropLists.add(
                                                new DropHolder(
                                                   dropType,
                                                   itemId,
                                                   this.parseLong(dropAttrs, "min"),
                                                   this.parseLong(dropAttrs, "max"),
                                                   this.parseDouble(dropAttrs, "chance")
                                                )
                                             );
                                          }
                                       }
                                    } else {
                                       if (dropGroups == null) {
                                          dropGroups = new ArrayList<>();
                                       }

                                       DropGroupHolder group = new DropGroupHolder(this.parseDouble(dropNode.getAttributes(), "chance"));

                                       for (Node groupNode = dropNode.getFirstChild(); groupNode != null; groupNode = groupNode.getNextSibling()) {
                                          if (groupNode.getNodeName().equalsIgnoreCase("item")) {
                                             NamedNodeMap groupAttrs = groupNode.getAttributes();
                                             int itemId = this.parseInteger(groupAttrs, "id");
                                             if (ItemData.getInstance().getTemplate(itemId) == null) {
                                                LOGGER.warning(this.getClass().getSimpleName() + ": Could not find drop with id " + itemId + ".");
                                             } else {
                                                group.addDrop(
                                                   new DropHolder(
                                                      dropType,
                                                      itemId,
                                                      this.parseLong(groupAttrs, "min"),
                                                      this.parseLong(groupAttrs, "max"),
                                                      this.parseDouble(groupAttrs, "chance")
                                                   )
                                                );
                                             }
                                          }
                                       }

                                       group.sortByChance();
                                       dropGroups.add(group);
                                    }
                                 }
                              }
                           }
                           break;
                        case "collision":
                           for (Node collisionNode = npcNode.getFirstChild(); collisionNode != null; collisionNode = collisionNode.getNextSibling()) {
                              attrs = collisionNode.getAttributes();
                              String aiSkillScopes = collisionNode.getNodeName().toLowerCase();
                              switch (aiSkillScopes) {
                                 case "radius":
                                    set.set("collisionRadius", this.parseDouble(attrs, "normal"));
                                    set.set("collisionRadiusGrown", this.parseDouble(attrs, "grown"));
                                    break;
                                 case "height":
                                    set.set("collisionHeight", this.parseDouble(attrs, "normal"));
                                    set.set("collisionHeightGrown", this.parseDouble(attrs, "grown"));
                              }
                           }
                     }
                  }

                  if (FakePlayersConfig.FAKE_PLAYERS_ENABLED || !set.getBoolean("fakePlayer", false)) {
                     NpcTemplate template = this._npcs.get(npcId);
                     if (template == null) {
                        template = new NpcTemplate(set);
                        this._npcs.put(template.getId(), template);
                     } else {
                        template.set(set);
                     }

                     template.setParameters(parameters != null ? new StatSet(Collections.unmodifiableMap(parameters)) : StatSet.EMPTY_STATSET);
                     if (skills != null) {
                        Map<AISkillScope, List<Skill>> aiSkillLists = null;

                        for (Skill skill : skills.values()) {
                           if (!skill.isPassive()) {
                              if (aiSkillLists == null) {
                                 aiSkillLists = new EnumMap<>(AISkillScope.class);
                              }

                              List<AISkillScope> aiSkillScopes = new ArrayList<>();
                              AISkillScope shortOrLongRangeScope = skill.getCastRange() <= 150 ? AISkillScope.SHORT_RANGE : AISkillScope.LONG_RANGE;
                              if (skill.isSuicideAttack()) {
                                 aiSkillScopes.add(AISkillScope.SUICIDE);
                              } else {
                                 aiSkillScopes.add(AISkillScope.GENERAL);
                                 if (skill.isContinuous()) {
                                    if (!skill.isDebuff()) {
                                       aiSkillScopes.add(AISkillScope.BUFF);
                                    } else {
                                       aiSkillScopes.add(AISkillScope.DEBUFF);
                                       aiSkillScopes.add(AISkillScope.COT);
                                       aiSkillScopes.add(shortOrLongRangeScope);
                                    }
                                 } else if (skill.hasEffectType(EffectType.DISPEL, EffectType.DISPEL_BY_SLOT)) {
                                    aiSkillScopes.add(AISkillScope.NEGATIVE);
                                    aiSkillScopes.add(shortOrLongRangeScope);
                                 } else if (skill.hasEffectType(EffectType.HEAL)) {
                                    aiSkillScopes.add(AISkillScope.HEAL);
                                 } else if (skill.hasEffectType(
                                    EffectType.PHYSICAL_ATTACK,
                                    EffectType.PHYSICAL_ATTACK_HP_LINK,
                                    EffectType.MAGICAL_ATTACK,
                                    EffectType.DEATH_LINK,
                                    EffectType.HP_DRAIN
                                 )) {
                                    aiSkillScopes.add(AISkillScope.ATTACK);
                                    aiSkillScopes.add(AISkillScope.UNIVERSAL);
                                    aiSkillScopes.add(shortOrLongRangeScope);
                                 } else if (skill.hasEffectType(EffectType.SLEEP)) {
                                    aiSkillScopes.add(AISkillScope.IMMOBILIZE);
                                 } else if (skill.hasEffectType(EffectType.STUN, EffectType.ROOT)) {
                                    aiSkillScopes.add(AISkillScope.IMMOBILIZE);
                                    aiSkillScopes.add(shortOrLongRangeScope);
                                 } else if (skill.hasEffectType(EffectType.MUTE, EffectType.FEAR)) {
                                    aiSkillScopes.add(AISkillScope.COT);
                                    aiSkillScopes.add(shortOrLongRangeScope);
                                 } else if (skill.hasEffectType(EffectType.PARALYZE)) {
                                    aiSkillScopes.add(AISkillScope.IMMOBILIZE);
                                    aiSkillScopes.add(shortOrLongRangeScope);
                                 } else if (skill.hasEffectType(EffectType.DMG_OVER_TIME, EffectType.DMG_OVER_TIME_PERCENT)) {
                                    aiSkillScopes.add(shortOrLongRangeScope);
                                 } else if (skill.hasEffectType(EffectType.RESURRECTION)) {
                                    aiSkillScopes.add(AISkillScope.RES);
                                 } else {
                                    aiSkillScopes.add(AISkillScope.UNIVERSAL);
                                 }
                              }

                              for (AISkillScope aiSkillScope : aiSkillScopes) {
                                 List<Skill> aiSkills = aiSkillLists.get(aiSkillScope);
                                 if (aiSkills == null) {
                                    aiSkills = new ArrayList<>();
                                    aiSkillLists.put(aiSkillScope, aiSkills);
                                 }

                                 aiSkills.add(skill);
                              }
                           }
                        }

                        template.setSkills(skills);
                        template.setAISkillLists(aiSkillLists);
                     } else {
                        template.setSkills(null);
                        template.setAISkillLists(null);
                     }

                     template.setClans(clans);
                     template.setIgnoreClanNpcIds(ignoreClanNpcIds);
                     template.removeDropGroups();
                     if (dropGroups != null) {
                        template.setDropGroups(dropGroups);
                     }

                     template.removeDrops();
                     if (RatesConfig.BOSS_DROP_ENABLED
                        && type.contains("RaidBoss")
                        && level >= RatesConfig.BOSS_DROP_MIN_LEVEL
                        && level <= RatesConfig.BOSS_DROP_MAX_LEVEL) {
                        if (dropLists == null) {
                           dropLists = new ArrayList<>();
                        }

                        dropLists.addAll(RatesConfig.BOSS_DROP_LIST);
                     }

                     if (dropLists != null) {
                        dropLists.sort((d1, d2) -> Double.compare(d2.getChance(), d1.getChance()));

                        for (DropHolder dropHolder : dropLists) {
                           switch (dropHolder.getDropType()) {
                              case DROP:
                                 template.addDrop(dropHolder);
                                 break;
                              case SPOIL:
                                 template.addSpoil(dropHolder);
                           }
                        }
                     }

                     if (!template.getParameters().getMinionList("Privates").isEmpty() && template.getParameters().getSet().get("SummonPrivateRate") == null) {
                        _masterMonsterIDs.add(template.getId());
                     }
                  }
               }
            }
         }
      }
   }

   private int getOrCreateClanId(String clanName) {
      Integer id = this._clans.get(clanName);
      if (id == null) {
         id = this._clans.size();
         this._clans.put(clanName, id);
      }

      return id;
   }

   public int getClanId(String clanName) {
      Integer id = this._clans.get(clanName);
      return id != null ? id : -1;
   }

   public int getGenericClanId() {
      if (_genericClanId != null) {
         return _genericClanId;
      } else {
         synchronized (this) {
            _genericClanId = this._clans.get("ALL");
            if (_genericClanId == null) {
               _genericClanId = -1;
            }
         }

         return _genericClanId;
      }
   }

   public NpcTemplate getTemplate(int id) {
      return this._npcs.get(id);
   }

   public NpcTemplate getTemplateByName(String name) {
      for (NpcTemplate npcTemplate : this._npcs.values()) {
         if (npcTemplate.getName().equalsIgnoreCase(name)) {
            return npcTemplate;
         }
      }

      return null;
   }

   public List<NpcTemplate> getTemplates(Predicate<NpcTemplate> filter) {
      List<NpcTemplate> result = new ArrayList<>();

      for (NpcTemplate npcTemplate : this._npcs.values()) {
         if (filter.test(npcTemplate)) {
            result.add(npcTemplate);
         }
      }

      return result;
   }

   public List<NpcTemplate> getAllOfLevel(int... levels) {
      return this.getTemplates(template -> ArrayUtil.contains(levels, template.getLevel()));
   }

   public List<NpcTemplate> getAllMonstersOfLevel(int... levels) {
      return this.getTemplates(template -> ArrayUtil.contains(levels, template.getLevel()) && template.isType("Monster"));
   }

   public List<NpcTemplate> getAllNpcStartingWith(String text) {
      return this.getTemplates(template -> template.isType("Folk") && template.getName().startsWith(text));
   }

   public List<NpcTemplate> getAllNpcOfClassType(String... classTypes) {
      return this.getTemplates(template -> ArrayUtil.contains(classTypes, template.getType(), true));
   }

   public void loadNpcsSkillLearn() {
      this._npcs.values().forEach(template -> {
         List<PlayerClass> teachInfo = SkillLearnData.getInstance().getSkillLearnData(template.getId());
         if (teachInfo != null) {
            template.addTeachInfo(teachInfo);
         }
      });
   }

   public static Collection<Integer> getMasterMonsterIDs() {
      return _masterMonsterIDs;
   }

   public static NpcData getInstance() {
      return NpcData.SingletonHolder.INSTANCE;
   }

   private static class SingletonHolder {
      protected static final NpcData INSTANCE = new NpcData();
   }
}
