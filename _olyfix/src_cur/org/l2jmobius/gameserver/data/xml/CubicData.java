package org.l2jmobius.gameserver.data.xml;

import java.io.File;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import org.l2jmobius.commons.util.IXmlReader;
import org.l2jmobius.gameserver.entity.actor.templates.CubicTemplate;
import org.l2jmobius.gameserver.entity.cubic.CubicSkill;
import org.l2jmobius.gameserver.entity.cubic.ICubicConditionHolder;
import org.l2jmobius.gameserver.entity.cubic.conditions.HealthCondition;
import org.l2jmobius.gameserver.entity.cubic.conditions.HpCondition;
import org.l2jmobius.gameserver.entity.cubic.conditions.RangeCondition;
import org.l2jmobius.gameserver.util.StatSet;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

public class CubicData implements IXmlReader {
   private static final Logger LOGGER = Logger.getLogger(CubicData.class.getName());
   private volatile Map<Integer, Map<Integer, CubicTemplate>> _cubics = Collections.emptyMap();
   private volatile Map<Integer, Map<Integer, CubicTemplate>> _loadingCubics;

   protected CubicData() {
      this.load();
   }

   @Override
   public synchronized void load() {
      Map<Integer, Map<Integer, CubicTemplate>> loadedCubics = new ConcurrentHashMap<>();
      this._loadingCubics = loadedCubics;

      try {
         this.parseDatapackDirectory("data/stats/cubics", true);
         int count = 0;

         for (Map<Integer, CubicTemplate> levels : loadedCubics.values()) {
            count += levels.size();
         }

         if (count != 0) {
            this._cubics = loadedCubics;
            LOGGER.info(this.getClass().getSimpleName() + ": Loaded " + count + " cubic templates.");
            return;
         }

         LOGGER.warning(this.getClass().getSimpleName() + ": No cubic templates loaded. Keeping previous data.");
      } finally {
         this._loadingCubics = null;
      }
   }

   @Override
   public void parseDocument(Document document, File file) {
      this.forEach(
         document,
         "list",
         listNode -> this.forEach(
            listNode, "cubic", cubicNode -> this.parseTemplate(cubicNode, new CubicTemplate(new StatSet(this.parseAttributes(cubicNode))))
         )
      );
   }

   private void parseTemplate(Node cubicNode, CubicTemplate template) {
      this.forEach(cubicNode, IXmlReader::isNode, innerNode -> {
         String s0$ = innerNode.getNodeName();
         switch (s0$) {
            case "conditions":
               this.parseConditions(innerNode, template, template);
               break;
            case "skills":
               this.parseSkills(innerNode, template);
         }
      });
      Map<Integer, Map<Integer, CubicTemplate>> loadingCubics = this._loadingCubics;
      if (loadingCubics == null) {
         LOGGER.warning("Skipping cubic template outside loading context. cubicId: " + template.getId() + " level: " + template.getLevel());
      } else {
         loadingCubics.computeIfAbsent(template.getId(), key -> new ConcurrentHashMap<>()).put(template.getLevel(), template);
      }
   }

   private void parseConditions(Node conditionsNode, CubicTemplate template, ICubicConditionHolder holder) {
      this.forEach(
         conditionsNode,
         IXmlReader::isNode,
         conditionNode -> {
            String s0$ = conditionNode.getNodeName();
            switch (s0$) {
               case "hp":
                  HpCondition.HpConditionType type = this.parseEnum(conditionNode.getAttributes(), HpCondition.HpConditionType.class, "type");
                  holder.addCondition(new HpCondition(type, this.parseInteger(conditionNode.getAttributes(), "percent")));
                  break;
               case "range":
                  holder.addCondition(new RangeCondition(this.parseInteger(conditionNode.getAttributes(), "value")));
                  break;
               case "healthPercent":
                  int min = this.parseInteger(conditionNode.getAttributes(), "min");
                  int max = this.parseInteger(conditionNode.getAttributes(), "max");
                  holder.addCondition(new HealthCondition(min, max));
                  break;
               default:
                  LOGGER.warning(
                     "Unsupported cubic condition: " + conditionNode.getNodeName() + " cubicId: " + template.getId() + " level: " + template.getLevel()
                  );
            }
         }
      );
   }

   private void parseSkills(Node skillsNode, CubicTemplate template) {
      this.forEach(skillsNode, "skill", skillNode -> {
         CubicSkill skill = new CubicSkill(new StatSet(this.parseAttributes(skillNode)));
         this.forEach(skillNode, "conditions", conditionsNode -> this.parseConditions(conditionsNode, template, skill));
         template.addCubicSkill(skill);
      });
   }

   public CubicTemplate getCubicTemplate(int id, int level) {
      return this._cubics.getOrDefault(id, Collections.emptyMap()).get(level);
   }

   public static CubicData getInstance() {
      return CubicData.SingletonHolder.INSTANCE;
   }

   private static class SingletonHolder {
      protected static final CubicData INSTANCE = new CubicData();
   }
}
