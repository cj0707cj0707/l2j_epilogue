package org.l2jmobius.gameserver.data.xml;

import java.io.File;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import org.l2jmobius.commons.util.IXmlReader;
import org.l2jmobius.gameserver.entity.actor.instance.StaticObject;
import org.l2jmobius.gameserver.entity.actor.templates.CreatureTemplate;
import org.l2jmobius.gameserver.util.StatSet;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

public class StaticObjectData implements IXmlReader {
   private final Map<Integer, StaticObject> _staticObjects = new HashMap<>();

   protected StaticObjectData() {
      this.load();
   }

   @Override
   public void load() {
      this._staticObjects.clear();
      this.parseDatapackFile("data/StaticObjects.xml");
      LOGGER.info(this.getClass().getSimpleName() + ": Loaded " + this._staticObjects.size() + " static object templates.");
   }

   @Override
   public void parseDocument(Document document, File file) {
      for (Node n = document.getFirstChild(); n != null; n = n.getNextSibling()) {
         if ("list".equalsIgnoreCase(n.getNodeName())) {
            for (Node d = n.getFirstChild(); d != null; d = d.getNextSibling()) {
               if ("object".equalsIgnoreCase(d.getNodeName())) {
                  NamedNodeMap attrs = d.getAttributes();
                  StatSet set = new StatSet();

                  for (int i = 0; i < attrs.getLength(); i++) {
                     Node att = attrs.item(i);
                     set.set(att.getNodeName(), att.getNodeValue());
                  }

                  this.addObject(set);
               }
            }
         }
      }
   }

   private void addObject(StatSet set) {
      StaticObject obj = new StaticObject(new CreatureTemplate(new StatSet()), set.getInt("id"));
      obj.setType(set.getInt("type", 0));
      obj.setName(set.getString("name"));
      obj.setMap(set.getString("texture", "none"), set.getInt("map_x", 0), set.getInt("map_y", 0));
      obj.spawnMe(set.getInt("x"), set.getInt("y"), set.getInt("z"));
      this._staticObjects.put(obj.getObjectId(), obj);
   }

   public Collection<StaticObject> getStaticObjects() {
      return this._staticObjects.values();
   }

   public static StaticObjectData getInstance() {
      return StaticObjectData.SingletonHolder.INSTANCE;
   }

   private static class SingletonHolder {
      protected static final StaticObjectData INSTANCE = new StaticObjectData();
   }
}
