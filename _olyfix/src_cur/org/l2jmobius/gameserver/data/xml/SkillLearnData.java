package org.l2jmobius.gameserver.data.xml;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.l2jmobius.commons.util.IXmlReader;
import org.l2jmobius.gameserver.entity.actor.enums.player.PlayerClass;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

public class SkillLearnData implements IXmlReader {
   private final Map<Integer, List<PlayerClass>> _skillLearn = new HashMap<>();

   protected SkillLearnData() {
      this.load();
   }

   @Override
   public synchronized void load() {
      this._skillLearn.clear();
      this.parseDatapackFile("data/SkillLearn.xml");
      LOGGER.info(this.getClass().getSimpleName() + ": Loaded " + this._skillLearn.size() + " skill learn data.");
   }

   @Override
   public void parseDocument(Document document, File file) {
      for (Node node = document.getFirstChild(); node != null; node = node.getNextSibling()) {
         if ("list".equalsIgnoreCase(node.getNodeName())) {
            for (Node list_node = node.getFirstChild(); list_node != null; list_node = list_node.getNextSibling()) {
               if ("npc".equalsIgnoreCase(list_node.getNodeName())) {
                  List<PlayerClass> classIds = new ArrayList<>();

                  for (Node c = list_node.getFirstChild(); c != null; c = c.getNextSibling()) {
                     if ("classId".equalsIgnoreCase(c.getNodeName())) {
                        classIds.add(PlayerClass.getPlayerClass(Integer.parseInt(c.getTextContent())));
                     }
                  }

                  this._skillLearn.put(this.parseInteger(list_node.getAttributes(), "id"), classIds);
               }
            }
         }
      }
   }

   public List<PlayerClass> getSkillLearnData(int npcId) {
      return this._skillLearn.get(npcId);
   }

   public static SkillLearnData getInstance() {
      return SkillLearnData.SingletonHolder.INSTANCE;
   }

   private static class SingletonHolder {
      protected static final SkillLearnData INSTANCE = new SkillLearnData();
   }
}
