package org.l2jmobius.gameserver.data.xml;

import java.io.File;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import org.l2jmobius.commons.util.IXmlReader;
import org.l2jmobius.gameserver.entity.actor.enums.player.PlayerClass;
import org.l2jmobius.gameserver.entity.actor.holders.player.ClassInfoHolder;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;

public class ClassListData implements IXmlReader {
   private static final Logger LOGGER = Logger.getLogger(ClassListData.class.getName());
   private final Map<PlayerClass, ClassInfoHolder> _classData = new ConcurrentHashMap<>();

   protected ClassListData() {
      this.load();
   }

   @Override
   public void load() {
      this._classData.clear();
      this.parseDatapackFile("data/stats/players/classList.xml");
      LOGGER.info(this.getClass().getSimpleName() + ": Loaded " + this._classData.size() + " class data.");
   }

   @Override
   public void parseDocument(Document document, File file) {
      this.forEach(
         document,
         "list",
         listNode -> this.forEach(
            listNode,
            "class",
            classNode -> {
               NamedNodeMap attrs = classNode.getAttributes();
               PlayerClass playerClass = PlayerClass.getPlayerClass(this.parseInteger(attrs, "classId"));
               PlayerClass parentPlayerClass = this.parseInteger(attrs, "parentClassId", -1) > 0
                  ? PlayerClass.getPlayerClass(this.parseInteger(attrs, "parentClassId"))
                  : null;
               String className = this.parseString(attrs, "name");
               this._classData.put(playerClass, new ClassInfoHolder(playerClass, parentPlayerClass, className));
            }
         )
      );
   }

   public Map<PlayerClass, ClassInfoHolder> getClassList() {
      return this._classData;
   }

   public ClassInfoHolder getClass(PlayerClass playerClass) {
      return this._classData.get(playerClass);
   }

   public ClassInfoHolder getClass(int classId) {
      PlayerClass id = PlayerClass.getPlayerClass(classId);
      return id != null ? this._classData.get(id) : null;
   }

   public static ClassListData getInstance() {
      return ClassListData.SingletonHolder.INSTANCE;
   }

   private static class SingletonHolder {
      protected static final ClassListData INSTANCE = new ClassListData();
   }
}
