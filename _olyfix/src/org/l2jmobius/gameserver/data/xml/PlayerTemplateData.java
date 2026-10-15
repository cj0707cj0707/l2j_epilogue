package org.l2jmobius.gameserver.data.xml;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;
import org.l2jmobius.commons.util.IXmlReader;
import org.l2jmobius.gameserver.entity.Location;
import org.l2jmobius.gameserver.entity.actor.enums.player.PlayerClass;
import org.l2jmobius.gameserver.entity.actor.templates.PlayerTemplate;
import org.l2jmobius.gameserver.util.StatSet;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

public class PlayerTemplateData implements IXmlReader {
   private static final Logger LOGGER = Logger.getLogger(PlayerTemplateData.class.getName());
   private final Map<PlayerClass, PlayerTemplate> _playerTemplates = new ConcurrentHashMap<>();
   final AtomicInteger _levelUpGainCount = new AtomicInteger();

   protected PlayerTemplateData() {
      this.load();
   }

   @Override
   public void load() {
      this._playerTemplates.clear();
      this._levelUpGainCount.set(0);
      this.parseDatapackDirectory("data/stats/players/templates", true);
      LOGGER.info(this.getClass().getSimpleName() + ": Loaded " + this._playerTemplates.size() + " player templates.");
      LOGGER.info(this.getClass().getSimpleName() + ": Loaded " + this._levelUpGainCount.get() + " level up gain records.");
   }

   @Override
   public void parseDocument(Document document, File file) {
      int maxLevel = ExperienceData.getInstance().getMaxLevel();
      int classId = 0;

      for (Node n = document.getFirstChild(); n != null; n = n.getNextSibling()) {
         if ("list".equalsIgnoreCase(n.getNodeName())) {
            for (Node d = n.getFirstChild(); d != null; d = d.getNextSibling()) {
               if ("classId".equalsIgnoreCase(d.getNodeName())) {
                  classId = Integer.parseInt(d.getTextContent());
               } else if ("staticData".equalsIgnoreCase(d.getNodeName())) {
                  StatSet set = new StatSet();
                  set.set("classId", classId);
                  List<Location> creationPoints = new ArrayList<>();

                  for (Node nd = d.getFirstChild(); nd != null; nd = nd.getNextSibling()) {
                     if (!nd.getNodeName().equals("#text")) {
                        if (nd.getChildNodes().getLength() > 1) {
                           for (Node cnd = nd.getFirstChild(); cnd != null; cnd = cnd.getNextSibling()) {
                              if (nd.getNodeName().equalsIgnoreCase("collisionMale")) {
                                 if (cnd.getNodeName().equalsIgnoreCase("radius")) {
                                    set.set("collisionRadius", cnd.getTextContent());
                                 } else if (cnd.getNodeName().equalsIgnoreCase("height")) {
                                    set.set("collisionHeight", cnd.getTextContent());
                                 }
                              }

                              if ("node".equalsIgnoreCase(cnd.getNodeName())) {
                                 NamedNodeMap attributes = cnd.getAttributes();
                                 creationPoints.add(
                                    new Location(this.parseInteger(attributes, "x"), this.parseInteger(attributes, "y"), this.parseInteger(attributes, "z"))
                                 );
                              } else if ("walk".equalsIgnoreCase(cnd.getNodeName())) {
                                 set.set("baseWalkSpd", cnd.getTextContent());
                              } else if ("run".equalsIgnoreCase(cnd.getNodeName())) {
                                 set.set("baseRunSpd", cnd.getTextContent());
                              } else if ("slowSwim".equals(cnd.getNodeName())) {
                                 set.set("baseSwimWalkSpd", cnd.getTextContent());
                              } else if ("fastSwim".equals(cnd.getNodeName())) {
                                 set.set("baseSwimRunSpd", cnd.getTextContent());
                              } else if (!cnd.getNodeName().equals("#text")) {
                                 set.set(nd.getNodeName() + cnd.getNodeName(), cnd.getTextContent());
                              }
                           }
                        } else {
                           set.set(nd.getNodeName(), nd.getTextContent());
                        }
                     }
                  }

                  set.set(
                     "basePDef",
                     set.getInt("basePDefchest", 0)
                        + set.getInt("basePDeflegs", 0)
                        + set.getInt("basePDefhead", 0)
                        + set.getInt("basePDeffeet", 0)
                        + set.getInt("basePDefgloves", 0)
                        + set.getInt("basePDefunderwear", 0)
                        + set.getInt("basePDefcloak", 0)
                  );
                  set.set(
                     "baseMDef",
                     set.getInt("baseMDefrear", 0)
                        + set.getInt("baseMDeflear", 0)
                        + set.getInt("baseMDefrfinger", 0)
                        + set.getInt("baseMDefrfinger", 0)
                        + set.getInt("baseMDefneck", 0)
                  );
                  this._playerTemplates.put(PlayerClass.getPlayerClass(classId), new PlayerTemplate(set, creationPoints));
               } else if ("lvlUpgainData".equalsIgnoreCase(d.getNodeName())) {
                  int level = 0;
                  PlayerTemplate template = this._playerTemplates.get(PlayerClass.getPlayerClass(classId));

                  for (Node lvlNode = d.getFirstChild(); lvlNode != null; lvlNode = lvlNode.getNextSibling()) {
                     if ("level".equalsIgnoreCase(lvlNode.getNodeName())) {
                        NamedNodeMap attributes = lvlNode.getAttributes();
                        level = this.parseInteger(attributes, "val");
                        if (level > maxLevel - 1) {
                           return;
                        }

                        for (Node valNode = lvlNode.getFirstChild(); valNode != null; valNode = valNode.getNextSibling()) {
                           String nodeName = valNode.getNodeName();
                           if (nodeName.startsWith("hp") || nodeName.startsWith("mp") || nodeName.startsWith("cp")) {
                              template.setUpgainValue(nodeName, level, Double.parseDouble(valNode.getTextContent()));
                              this._levelUpGainCount.incrementAndGet();
                           }
                        }
                     }
                  }
               }
            }
         }
      }
   }

   public PlayerTemplate getTemplate(PlayerClass playerClass) {
      return this._playerTemplates.get(playerClass);
   }

   public PlayerTemplate getTemplate(int classId) {
      return this._playerTemplates.get(PlayerClass.getPlayerClass(classId));
   }

   public static PlayerTemplateData getInstance() {
      return PlayerTemplateData.SingletonHolder.INSTANCE;
   }

   private static class SingletonHolder {
      protected static final PlayerTemplateData INSTANCE = new PlayerTemplateData();
   }
}
