package org.l2jmobius.gameserver.data.xml;

import java.io.File;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.l2jmobius.commons.util.IXmlReader;
import org.l2jmobius.gameserver.data.holders.PetData;
import org.l2jmobius.gameserver.data.holders.PetLevelData;
import org.l2jmobius.gameserver.entity.actor.enums.player.MountType;
import org.l2jmobius.gameserver.util.StatSet;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

public class PetDataTable implements IXmlReader {
   private final Map<Integer, PetData> _pets = new ConcurrentHashMap<>();

   protected PetDataTable() {
      this.load();
   }

   @Override
   public void load() {
      this._pets.clear();
      this.parseDatapackDirectory("data/stats/pets", false);
      LOGGER.info(this.getClass().getSimpleName() + ": Loaded " + this._pets.size() + " pets.");
   }

   @Override
   public void parseDocument(Document document, File file) {
      for (Node d = document.getFirstChild().getFirstChild(); d != null; d = d.getNextSibling()) {
         if (d.getNodeName().equals("pet")) {
            int npcId = this.parseInteger(d.getAttributes(), "id");
            int itemId = this.parseInteger(d.getAttributes(), "itemId");
            PetData data = new PetData(npcId, itemId);

            for (Node p = d.getFirstChild(); p != null; p = p.getNextSibling()) {
               if (p.getNodeName().equals("set")) {
                  NamedNodeMap attrs = p.getAttributes();
                  String type = attrs.getNamedItem("name").getNodeValue();
                  if ("food".equals(type)) {
                     for (String foodId : attrs.getNamedItem("val").getNodeValue().split(";")) {
                        data.addFood(Integer.parseInt(foodId));
                     }
                  } else if ("load".equals(type)) {
                     data.setLoad(this.parseInteger(attrs, "val"));
                  } else if ("hungry_limit".equals(type)) {
                     data.setHungryLimit(this.parseInteger(attrs, "val"));
                  } else if ("sync_level".equals(type)) {
                     data.setSyncLevel(this.parseInteger(attrs, "val") == 1);
                  }
               } else if (p.getNodeName().equals("skills")) {
                  for (Node s = p.getFirstChild(); s != null; s = s.getNextSibling()) {
                     if (s.getNodeName().equals("skill")) {
                        NamedNodeMap attrs = s.getAttributes();
                        data.addNewSkill(this.parseInteger(attrs, "skillId"), this.parseInteger(attrs, "skillLevel"), this.parseInteger(attrs, "minLevel"));
                     }
                  }
               } else if (p.getNodeName().equals("stats")) {
                  for (Node sx = p.getFirstChild(); sx != null; sx = sx.getNextSibling()) {
                     if (sx.getNodeName().equals("stat")) {
                        int level = Integer.parseInt(sx.getAttributes().getNamedItem("level").getNodeValue());
                        StatSet set = new StatSet();

                        for (Node bean = sx.getFirstChild(); bean != null; bean = bean.getNextSibling()) {
                           if (bean.getNodeName().equals("set")) {
                              NamedNodeMap attrs = bean.getAttributes();
                              if (attrs.getNamedItem("name").getNodeValue().equals("speed_on_ride")) {
                                 set.set("walkSpeedOnRide", attrs.getNamedItem("walk").getNodeValue());
                                 set.set("runSpeedOnRide", attrs.getNamedItem("run").getNodeValue());
                                 set.set("slowSwimSpeedOnRide", attrs.getNamedItem("slowSwim").getNodeValue());
                                 set.set("fastSwimSpeedOnRide", attrs.getNamedItem("fastSwim").getNodeValue());
                                 if (attrs.getNamedItem("slowFly") != null) {
                                    set.set("slowFlySpeedOnRide", attrs.getNamedItem("slowFly").getNodeValue());
                                 }

                                 if (attrs.getNamedItem("fastFly") != null) {
                                    set.set("fastFlySpeedOnRide", attrs.getNamedItem("fastFly").getNodeValue());
                                 }
                              } else {
                                 set.set(attrs.getNamedItem("name").getNodeValue(), attrs.getNamedItem("val").getNodeValue());
                              }
                           }
                        }

                        data.addNewStat(level, new PetLevelData(set));
                     }
                  }
               }
            }

            this._pets.put(npcId, data);
         }
      }
   }

   public PetData getPetDataByItemId(int itemId) {
      for (PetData data : this._pets.values()) {
         if (data.getItemId() == itemId) {
            return data;
         }
      }

      return null;
   }

   public PetLevelData getPetLevelData(int petId, int petLevel) {
      PetData pd = this.getPetData(petId);
      if (pd != null) {
         return petLevel > pd.getMaxLevel() ? pd.getPetLevelData(pd.getMaxLevel()) : pd.getPetLevelData(petLevel);
      } else {
         return null;
      }
   }

   public PetData getPetData(int petId) {
      if (!this._pets.containsKey(petId)) {
         LOGGER.info(this.getClass().getSimpleName() + ": Missing pet data for npcid: " + petId);
      }

      return this._pets.get(petId);
   }

   public int getPetMinLevel(int petId) {
      return this._pets.get(petId).getMinLevel();
   }

   public int getPetItemsByNpc(int npcId) {
      return this._pets.get(npcId).getItemId();
   }

   public static boolean isMountable(int npcId) {
      return MountType.findByNpcId(npcId) != MountType.NONE;
   }

   public static PetDataTable getInstance() {
      return PetDataTable.SingletonHolder.INSTANCE;
   }

   private static class SingletonHolder {
      protected static final PetDataTable INSTANCE = new PetDataTable();
   }
}
