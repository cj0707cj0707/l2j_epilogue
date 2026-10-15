package org.l2jmobius.gameserver.data.xml;

import java.io.File;
import java.util.Collections;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import org.l2jmobius.commons.util.IXmlReader;
import org.l2jmobius.gameserver.mechanics.options.OptionSkillHolder;
import org.l2jmobius.gameserver.mechanics.options.OptionSkillType;
import org.l2jmobius.gameserver.mechanics.options.Options;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.mechanics.stats.Stat;
import org.l2jmobius.gameserver.mechanics.stats.functions.FuncTemplate;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

public class OptionData implements IXmlReader {
   private static final Logger LOGGER = Logger.getLogger(OptionData.class.getName());
   private static Options[] _options;
   private static Map<Integer, Options> _optionMap = new ConcurrentHashMap<>();

   protected OptionData() {
      this.load();
   }

   @Override
   public synchronized void load() {
      this.parseDatapackDirectory("data/stats/augmentation/options", false);
      _options = new Options[Collections.max(_optionMap.keySet()) + 1];

      for (Entry<Integer, Options> option : _optionMap.entrySet()) {
         _options[option.getKey()] = option.getValue();
      }

      LOGGER.info(this.getClass().getSimpleName() + ": Loaded " + _optionMap.size() + " options.");
      _optionMap.clear();
   }

   @Override
   public void parseDocument(Document document, File file) {
      for (Node n = document.getFirstChild(); n != null; n = n.getNextSibling()) {
         if ("list".equalsIgnoreCase(n.getNodeName())) {
            for (Node d = n.getFirstChild(); d != null; d = d.getNextSibling()) {
               if ("option".equalsIgnoreCase(d.getNodeName())) {
                  int id = this.parseInteger(d.getAttributes(), "id");
                  Options option = new Options(id);

                  for (Node cd = d.getFirstChild(); cd != null; cd = cd.getNextSibling()) {
                     String var8 = cd.getNodeName();
                     switch (var8) {
                        case "stats":
                           for (Node fd = cd.getFirstChild(); fd != null; fd = fd.getNextSibling()) {
                              String var22 = fd.getNodeName();
                              switch (var22) {
                                 case "add":
                                 case "sub":
                                 case "mul":
                                 case "div":
                                 case "set":
                                 case "share":
                                 case "enchant":
                                 case "enchanthp":
                                    this.parseFuncs(fd.getAttributes(), fd.getNodeName(), option);
                                    break;
                              }
                           }
                           break;
                        case "active_skill":
                           int skillIdxxxx = this.parseInteger(cd.getAttributes(), "id");
                           int skillLevelxxxx = this.parseInteger(cd.getAttributes(), "level");
                           Skill skillxxxx = SkillData.getInstance().getSkill(skillIdxxxx, skillLevelxxxx);
                           if (skillxxxx != null) {
                              option.setActiveSkill(skillxxxx);
                           } else {
                              LOGGER.info(
                                 this.getClass().getSimpleName()
                                    + ": Could not find skill "
                                    + skillIdxxxx
                                    + "("
                                    + skillLevelxxxx
                                    + ") used by option "
                                    + id
                                    + "."
                              );
                           }
                           break;
                        case "passive_skill":
                           int skillIdxxx = this.parseInteger(cd.getAttributes(), "id");
                           int skillLevelxxx = this.parseInteger(cd.getAttributes(), "level");
                           Skill skillxxx = SkillData.getInstance().getSkill(skillIdxxx, skillLevelxxx);
                           if (skillxxx != null) {
                              option.setPassiveSkill(skillxxx);
                           } else {
                              LOGGER.info(
                                 this.getClass().getSimpleName()
                                    + ": Could not find skill "
                                    + skillIdxxx
                                    + "("
                                    + skillLevelxxx
                                    + ") used by option "
                                    + id
                                    + "."
                              );
                           }
                           break;
                        case "attack_skill":
                           int skillIdxx = this.parseInteger(cd.getAttributes(), "id");
                           int skillLevelxx = this.parseInteger(cd.getAttributes(), "level");
                           Skill skillxx = SkillData.getInstance().getSkill(skillIdxx, skillLevelxx);
                           if (skillxx != null) {
                              option.addActivationSkill(new OptionSkillHolder(skillxx, this.parseDouble(cd.getAttributes(), "chance"), OptionSkillType.ATTACK));
                           } else {
                              LOGGER.info(
                                 this.getClass().getSimpleName() + ": Could not find skill " + skillIdxx + "(" + skillLevelxx + ") used by option " + id + "."
                              );
                           }
                           break;
                        case "magic_skill":
                           int skillIdx = this.parseInteger(cd.getAttributes(), "id");
                           int skillLevelx = this.parseInteger(cd.getAttributes(), "level");
                           Skill skillx = SkillData.getInstance().getSkill(skillIdx, skillLevelx);
                           if (skillx != null) {
                              option.addActivationSkill(new OptionSkillHolder(skillx, this.parseDouble(cd.getAttributes(), "chance"), OptionSkillType.MAGIC));
                           } else {
                              LOGGER.info(
                                 this.getClass().getSimpleName() + ": Could not find skill " + skillIdx + "(" + skillLevelx + ") used by option " + id + "."
                              );
                           }
                           break;
                        case "critical_skill":
                           int skillId = this.parseInteger(cd.getAttributes(), "id");
                           int skillLevel = this.parseInteger(cd.getAttributes(), "level");
                           Skill skill = SkillData.getInstance().getSkill(skillId, skillLevel);
                           if (skill != null) {
                              option.addActivationSkill(new OptionSkillHolder(skill, this.parseDouble(cd.getAttributes(), "chance"), OptionSkillType.CRITICAL));
                           } else {
                              LOGGER.info(
                                 this.getClass().getSimpleName() + ": Could not find skill " + skillId + "(" + skillLevel + ") used by option " + id + "."
                              );
                           }
                     }
                  }

                  _optionMap.put(option.getId(), option);
               }
            }
         }
      }
   }

   private void parseFuncs(NamedNodeMap attrs, String functionName, Options op) {
      Stat stat = Stat.valueOfXml(this.parseString(attrs, "stat"));
      double val = this.parseDouble(attrs, "val");
      int order = -1;
      Node orderNode = attrs.getNamedItem("order");
      if (orderNode != null) {
         order = Integer.parseInt(orderNode.getNodeValue());
      }

      op.addFunc(new FuncTemplate(null, null, functionName, order, stat, val));
   }

   public Options getOptions(int id) {
      return _options.length > id ? _options[id] : null;
   }

   public static OptionData getInstance() {
      return OptionData.SingletonHolder.INSTANCE;
   }

   private static class SingletonHolder {
      protected static final OptionData INSTANCE = new OptionData();
   }
}
