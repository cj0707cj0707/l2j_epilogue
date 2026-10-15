package org.l2jmobius.gameserver.data.xml;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.l2jmobius.commons.util.IXmlReader;
import org.l2jmobius.gameserver.entity.actor.enums.player.PlayerClass;
import org.l2jmobius.gameserver.entity.item.Henna;
import org.l2jmobius.gameserver.util.StatSet;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

public class HennaData implements IXmlReader {
   private final Map<Integer, Henna> _hennaList = new HashMap<>();

   protected HennaData() {
      this.load();
   }

   @Override
   public void load() {
      this._hennaList.clear();
      this.parseDatapackFile("data/stats/hennaList.xml");
      LOGGER.info(this.getClass().getSimpleName() + ": Loaded " + this._hennaList.size() + " henna data.");
   }

   @Override
   public void parseDocument(Document document, File file) {
      for (Node n = document.getFirstChild(); n != null; n = n.getNextSibling()) {
         if ("list".equals(n.getNodeName())) {
            for (Node d = n.getFirstChild(); d != null; d = d.getNextSibling()) {
               if ("henna".equals(d.getNodeName())) {
                  this.parseHenna(d);
               }
            }
         }
      }
   }

   private void parseHenna(Node d) {
      StatSet set = new StatSet();
      List<PlayerClass> wearClassIds = new ArrayList<>();
      NamedNodeMap attrs = d.getAttributes();

      for (int i = 0; i < attrs.getLength(); i++) {
         Node attr = attrs.item(i);
         set.set(attr.getNodeName(), attr.getNodeValue());
      }

      for (Node c = d.getFirstChild(); c != null; c = c.getNextSibling()) {
         String name = c.getNodeName();
         attrs = c.getAttributes();
         switch (name) {
            case "stats":
               for (int i = 0; i < attrs.getLength(); i++) {
                  Node attrx = attrs.item(i);
                  set.set(attrx.getNodeName(), attrx.getNodeValue());
               }
               break;
            case "wear": {
               Node attr = attrs.getNamedItem("count");
               set.set("wear_count", attr.getNodeValue());
               attr = attrs.getNamedItem("fee");
               set.set("wear_fee", attr.getNodeValue());
               break;
            }
            case "cancel": {
               Node attr = attrs.getNamedItem("count");
               set.set("cancel_count", attr.getNodeValue());
               attr = attrs.getNamedItem("fee");
               set.set("cancel_fee", attr.getNodeValue());
               break;
            }
            case "classId":
               wearClassIds.add(PlayerClass.getPlayerClass(Integer.parseInt(c.getTextContent())));
         }
      }

      Henna henna = new Henna(set);
      henna.setWearClassIds(wearClassIds);
      this._hennaList.put(henna.getDyeId(), henna);
   }

   public Henna getHenna(int id) {
      return this._hennaList.get(id);
   }

   public List<Henna> getHennaList(PlayerClass playerClass) {
      List<Henna> list = new ArrayList<>();

      for (Henna henna : this._hennaList.values()) {
         if (henna.isAllowedClass(playerClass)) {
            list.add(henna);
         }
      }

      return list;
   }

   public static HennaData getInstance() {
      return HennaData.SingletonHolder.INSTANCE;
   }

   private static class SingletonHolder {
      protected static final HennaData INSTANCE = new HennaData();
   }
}
