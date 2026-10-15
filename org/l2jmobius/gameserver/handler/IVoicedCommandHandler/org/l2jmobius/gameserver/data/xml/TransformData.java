package org.l2jmobius.gameserver.data.xml;

import java.io.File;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import org.l2jmobius.commons.util.IXmlReader;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.transform.AdditionalItemHolder;
import org.l2jmobius.gameserver.entity.actor.transform.AdditionalSkillHolder;
import org.l2jmobius.gameserver.entity.actor.transform.Transform;
import org.l2jmobius.gameserver.entity.actor.transform.TransformLevelData;
import org.l2jmobius.gameserver.entity.actor.transform.TransformTemplate;
import org.l2jmobius.gameserver.mechanics.skill.holders.SkillHolder;
import org.l2jmobius.gameserver.network.serverpackets.ExBasicActionList;
import org.l2jmobius.gameserver.util.StatSet;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

public class TransformData implements IXmlReader {
   private final Map<Integer, Transform> _transformData = new ConcurrentHashMap<>();

   protected TransformData() {
      this.load();
   }

   @Override
   public synchronized void load() {
      this._transformData.clear();
      this.parseDatapackDirectory("data/stats/transformations", false);
      LOGGER.log(Level.INFO, this.getClass().getSimpleName() + ": Loaded " + this._transformData.size() + " transform templates.");
   }

   @Override
   public void parseDocument(Document document, File file) {
      for (Node n = document.getFirstChild(); n != null; n = n.getNextSibling()) {
         if ("list".equalsIgnoreCase(n.getNodeName())) {
            for (Node d = n.getFirstChild(); d != null; d = d.getNextSibling()) {
               if ("transform".equalsIgnoreCase(d.getNodeName())) {
                  NamedNodeMap attrs = d.getAttributes();
                  StatSet set = new StatSet();

                  for (int i = 0; i < attrs.getLength(); i++) {
                     Node att = attrs.item(i);
                     set.set(att.getNodeName(), att.getNodeValue());
                  }

                  Transform transform = new Transform(set);

                  for (Node cd = d.getFirstChild(); cd != null; cd = cd.getNextSibling()) {
                     boolean isMale = "Male".equalsIgnoreCase(cd.getNodeName());
                     if ("Male".equalsIgnoreCase(cd.getNodeName()) || "Female".equalsIgnoreCase(cd.getNodeName())) {
                        TransformTemplate templateData = null;

                        for (Node z = cd.getFirstChild(); z != null; z = z.getNextSibling()) {
                           String var12 = z.getNodeName();
                           switch (var12) {
                              case "common":
                                 Node s = z.getFirstChild();

                                 while (s != null) {
                                    String var34 = s.getNodeName();
                                    switch (var34) {
                                       case "base":
                                       case "stats":
                                       case "defense":
                                       case "magicDefense":
                                       case "collision":
                                       case "moving":
                                          attrs = s.getAttributes();

                                          for (int i = 0; i < attrs.getLength(); i++) {
                                             Node att = attrs.item(i);
                                             set.set(att.getNodeName(), att.getNodeValue());
                                          }
                                       default:
                                          s = s.getNextSibling();
                                    }
                                 }

                                 templateData = new TransformTemplate(set);
                                 transform.setTemplate(isMale, templateData);
                                 break;
                              case "skills":
                                 if (templateData == null) {
                                    templateData = new TransformTemplate(set);
                                    transform.setTemplate(isMale, templateData);
                                 }

                                 for (Node sxx = z.getFirstChild(); sxx != null; sxx = sxx.getNextSibling()) {
                                    if ("skill".equals(sxx.getNodeName())) {
                                       attrs = sxx.getAttributes();
                                       int skillId = this.parseInteger(attrs, "id");
                                       int skillLevel = this.parseInteger(attrs, "level");
                                       templateData.addSkill(new SkillHolder(skillId, skillLevel));
                                    }
                                 }
                                 break;
                              case "actions":
                                 if (templateData == null) {
                                    templateData = new TransformTemplate(set);
                                    transform.setTemplate(isMale, templateData);
                                 }

                                 set.set("actions", z.getTextContent());
                                 int[] actions = set.getIntArray("actions", " ");
                                 templateData.setBasicActionList(new ExBasicActionList(actions));
                                 break;
                              case "additionalSkills":
                                 if (templateData == null) {
                                    templateData = new TransformTemplate(set);
                                    transform.setTemplate(isMale, templateData);
                                 }

                                 for (Node sx = z.getFirstChild(); sx != null; sx = sx.getNextSibling()) {
                                    if ("skill".equals(sx.getNodeName())) {
                                       attrs = sx.getAttributes();
                                       int skillId = this.parseInteger(attrs, "id");
                                       int skillLevel = this.parseInteger(attrs, "level");
                                       int minLevel = this.parseInteger(attrs, "minLevel");
                                       templateData.addAdditionalSkill(new AdditionalSkillHolder(skillId, skillLevel, minLevel));
                                    }
                                 }
                                 break;
                              case "items":
                                 if (templateData == null) {
                                    templateData = new TransformTemplate(set);
                                    transform.setTemplate(isMale, templateData);
                                 }

                                 for (Node s = z.getFirstChild(); s != null; s = s.getNextSibling()) {
                                    if ("item".equals(s.getNodeName())) {
                                       attrs = s.getAttributes();
                                       int itemId = this.parseInteger(attrs, "id");
                                       boolean allowed = this.parseBoolean(attrs, "allowed");
                                       templateData.addAdditionalItem(new AdditionalItemHolder(itemId, allowed));
                                    }
                                 }
                                 break;
                              case "levels":
                                 if (templateData == null) {
                                    templateData = new TransformTemplate(set);
                                    transform.setTemplate(isMale, templateData);
                                 }

                                 StatSet levelsSet = new StatSet();

                                 for (Node sxxx = z.getFirstChild(); sxxx != null; sxxx = sxxx.getNextSibling()) {
                                    if ("level".equals(sxxx.getNodeName())) {
                                       attrs = sxxx.getAttributes();

                                       for (int i = 0; i < attrs.getLength(); i++) {
                                          Node att = attrs.item(i);
                                          levelsSet.set(att.getNodeName(), att.getNodeValue());
                                       }
                                    }
                                 }

                                 templateData.addLevelData(new TransformLevelData(levelsSet));
                           }
                        }
                     }
                  }

                  this._transformData.put(transform.getId(), transform);
               }
            }
         }
      }
   }

   public Transform getTransform(int id) {
      return this._transformData.get(id);
   }

   public boolean transformPlayer(int id, Player player) {
      Transform transform = this.getTransform(id);
      if (transform != null) {
         player.transform(transform);
      }

      return transform != null;
   }

   public static TransformData getInstance() {
      return TransformData.SingletonHolder.INSTANCE;
   }

   private static class SingletonHolder {
      protected static final TransformData INSTANCE = new TransformData();
   }
}
