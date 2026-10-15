package org.l2jmobius.gameserver.data.xml;

import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.l2jmobius.commons.util.IXmlReader;
import org.l2jmobius.gameserver.config.PlayerConfig;
import org.l2jmobius.gameserver.data.enums.AbsorbCrystalType;
import org.l2jmobius.gameserver.data.holders.LevelingSoulCrystalInfo;
import org.l2jmobius.gameserver.data.holders.SoulCrystal;
import org.l2jmobius.gameserver.data.holders.SoulCrystalDisplay;
import org.l2jmobius.gameserver.entity.actor.templates.NpcTemplate;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

public class LevelUpCrystalData implements IXmlReader {
   private static final Logger LOGGER = Logger.getLogger(LevelUpCrystalData.class.getName());
   private static final Map<Integer, SoulCrystal> _soulCrystalsData = new HashMap<>();
   private static final Map<Integer, Map<Integer, LevelingSoulCrystalInfo>> _npcLevelingInfo = new HashMap<>();
   private static List<SoulCrystalDisplay> _crystalDisplayInfo = new ArrayList<>();

   protected LevelUpCrystalData() {
      this.load();
   }

   @Override
   public synchronized void load() {
      _soulCrystalsData.clear();
      _npcLevelingInfo.clear();
      this.parseDatapackFile("data/LevelUpCrystalData.xml");
      LOGGER.info("Soul crystal data : Loaded " + _soulCrystalsData.size());
      LOGGER.info("Npc leveling crystal data : Loaded " + _npcLevelingInfo.size());
   }

   public Map<Integer, Map<Integer, LevelingSoulCrystalInfo>> getNpcsSoulInfo() {
      return _npcLevelingInfo;
   }

   public Map<Integer, LevelingSoulCrystalInfo> getNpcSoulInfo(int npcid) {
      return _npcLevelingInfo.get(npcid);
   }

   public Map<Integer, SoulCrystal> getSoulCrystals() {
      return _soulCrystalsData;
   }

   public SoulCrystal getSoulCrystal(int itemid) {
      return _soulCrystalsData.get(itemid);
   }

   public List<SoulCrystalDisplay> getSoulCrystalInfo() {
      return _crystalDisplayInfo;
   }

   @Override
   public void parseDocument(Document document, File file) {
      if (!file.exists()) {
         LOGGER.log(Level.WARNING, "Missing LevelUpCrystalData.xml. The quest wont work without it!");
      } else {
         Node first = document.getFirstChild();
         if (first != null && "list".equalsIgnoreCase(first.getNodeName())) {
            for (Node n = first.getFirstChild(); n != null; n = n.getNextSibling()) {
               if ("crystal".equalsIgnoreCase(n.getNodeName())) {
                  for (Node d = n.getFirstChild(); d != null; d = d.getNextSibling()) {
                     if ("item".equalsIgnoreCase(d.getNodeName())) {
                        NamedNodeMap attrs = d.getAttributes();
                        Node att = attrs.getNamedItem("itemId");
                        if (att == null) {
                           LOGGER.info("Missing itemId in Crystal List, skipping!");
                        } else {
                           int itemId = Integer.parseInt(attrs.getNamedItem("itemId").getNodeValue());
                           att = attrs.getNamedItem("level");
                           if (att == null) {
                              LOGGER.info("Missing level in Crystal List item Id " + itemId + " skipping!");
                           } else {
                              att = attrs.getNamedItem("leveledItemId");
                              if (att == null) {
                                 LOGGER.info("Missing leveledItemId in Crystal List item Id " + itemId + " skipping!");
                              } else {
                                 int level = Integer.parseInt(attrs.getNamedItem("level").getNodeValue());
                                 int leveledItemId = Integer.parseInt(attrs.getNamedItem("leveledItemId").getNodeValue());
                                 _soulCrystalsData.put(itemId, new SoulCrystal(level, itemId, leveledItemId));
                              }
                           }
                        }
                     }
                  }
               } else if ("npc".equalsIgnoreCase(n.getNodeName())) {
                  for (Node dx = n.getFirstChild(); dx != null; dx = dx.getNextSibling()) {
                     if ("item".equalsIgnoreCase(dx.getNodeName())) {
                        NamedNodeMap attrs = dx.getAttributes();
                        Node att = attrs.getNamedItem("npcId");
                        if (att == null) {
                           LOGGER.info("Missing npc Id in NPC List, skipping!");
                        } else {
                           int npcId = Integer.parseInt(att.getNodeValue());
                           Map<Integer, LevelingSoulCrystalInfo> temp = new HashMap<>();

                           for (Node cd = dx.getFirstChild(); cd != null; cd = cd.getNextSibling()) {
                              boolean isSkillNeeded = false;
                              int chance = 5;
                              AbsorbCrystalType absorbType = AbsorbCrystalType.LAST_HIT;
                              if ("detail".equalsIgnoreCase(cd.getNodeName())) {
                                 attrs = cd.getAttributes();
                                 att = attrs.getNamedItem("absorbType");
                                 if (att != null) {
                                    absorbType = Enum.valueOf(AbsorbCrystalType.class, att.getNodeValue());
                                 }

                                 att = attrs.getNamedItem("chance");
                                 if (att != null) {
                                    chance = Integer.parseInt(att.getNodeValue());
                                 }

                                 att = attrs.getNamedItem("skill");
                                 if (att != null) {
                                    isSkillNeeded = Boolean.parseBoolean(att.getNodeValue());
                                 }

                                 Node att1 = attrs.getNamedItem("maxLevel");
                                 Node att2 = attrs.getNamedItem("levelList");
                                 if (att1 == null && att2 == null) {
                                    LOGGER.info("Missing maxlevel/levelList in NPC List npc Id " + npcId + " skipping!");
                                 } else {
                                    chance = (int)(chance * PlayerConfig.SOUL_CRYSTAL_CHANCE_MULTIPLIER);
                                    chance = chance > 100 ? 100 : chance;
                                    LevelingSoulCrystalInfo info = new LevelingSoulCrystalInfo(absorbType, isSkillNeeded, chance);
                                    if (att1 != null) {
                                       int maxLevel = Integer.parseInt(att1.getNodeValue());

                                       for (int i = 0; i <= maxLevel; i++) {
                                          temp.put(i, info);
                                       }
                                    } else if (att2 != null) {
                                       StringTokenizer st = new StringTokenizer(att2.getNodeValue(), ",");
                                       int tokenCount = st.countTokens();

                                       for (int i = 0; i < tokenCount; i++) {
                                          Integer value = Integer.decode(st.nextToken().trim());
                                          if (value == null) {
                                             LOGGER.info("Bad Level value!! npc Id " + npcId + ", token " + i + "!");
                                             value = 0;
                                          }

                                          temp.put(value, info);
                                       }
                                    }
                                 }
                              }
                           }

                           if (temp.isEmpty()) {
                              LOGGER.info("No leveling info for npc Id " + npcId + " skipping!");
                           } else {
                              _npcLevelingInfo.put(npcId, temp);
                           }
                        }
                     }
                  }
               }
            }
         }

         List<SoulCrystalDisplay> _crystalDisplayAux = new ArrayList<>();

         for (Entry<Integer, Map<Integer, LevelingSoulCrystalInfo>> entry : _npcLevelingInfo.entrySet()) {
            NpcTemplate npc = NpcData.getInstance().getTemplate(entry.getKey());
            int npcLvl = npc.getLevel();
            String name = npc.getName();

            for (Entry<Integer, LevelingSoulCrystalInfo> entry1 : entry.getValue().entrySet()) {
               int fromLevel = entry1.getKey();
               int chance = entry1.getValue().getChance();
               AbsorbCrystalType absorb = entry1.getValue().getAbsorbCrystalType();
               _crystalDisplayAux.add(new SoulCrystalDisplay(fromLevel, chance, absorb, npcLvl, name));
            }
         }

         _crystalDisplayInfo = _crystalDisplayAux.stream()
            .sorted(
               Comparator.comparingInt(SoulCrystalDisplay::getFromLevel)
                  .reversed()
                  .thenComparing(Comparator.comparingInt(SoulCrystalDisplay::getChance))
                  .reversed()
            )
            .toList();

         for (Entry<Integer, SoulCrystal> entry : _soulCrystalsData.entrySet()) {
            entry.getValue().getLeveledItemId();
         }
      }
   }

   public static LevelUpCrystalData getInstance() {
      return LevelUpCrystalData.SingletonHolder.INSTANCE;
   }

   private static class SingletonHolder {
      protected static final LevelUpCrystalData INSTANCE = new LevelUpCrystalData();
   }
}
