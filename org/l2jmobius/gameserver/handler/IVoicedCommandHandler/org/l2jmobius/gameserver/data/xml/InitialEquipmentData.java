package org.l2jmobius.gameserver.data.xml;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import org.l2jmobius.commons.util.IXmlReader;
import org.l2jmobius.gameserver.entity.actor.enums.player.PlayerClass;
import org.l2jmobius.gameserver.entity.item.holders.InitialEquipment;
import org.l2jmobius.gameserver.util.StatSet;
import org.w3c.dom.Document;

public class InitialEquipmentData implements IXmlReader {
   private static final Logger LOGGER = Logger.getLogger(InitialEquipmentData.class.getName());
   private final Map<PlayerClass, List<InitialEquipment>> _classEquipment = new EnumMap<>(PlayerClass.class);

   protected InitialEquipmentData() {
      this.load();
   }

   @Override
   public void load() {
      this._classEquipment.clear();
      this.parseDatapackFile("data/stats/players/initialEquipment.xml");
      LOGGER.info(this.getClass().getSimpleName() + ": Loaded " + this._classEquipment.size() + " initial equipment data.");
   }

   @Override
   public void parseDocument(Document document, File file) {
      this.forEach(document, "list", listNode -> this.forEach(listNode, "equipment", equipmentNode -> {
         Map<String, Object> attributes = this.parseAttributes(equipmentNode);
         List<InitialEquipment> equipment = new ArrayList<>();
         this.forEach(equipmentNode, "item", itemNode -> {
            StatSet set = new StatSet();
            this.parseAttributes(itemNode).forEach(set::set);
            equipment.add(new InitialEquipment(set));
         });
         PlayerClass playerClass = PlayerClass.getPlayerClass(Integer.parseInt((String)attributes.get("classId")));
         this._classEquipment.put(playerClass, equipment);
      }));
   }

   public Collection<InitialEquipment> getClassEquipment(PlayerClass playerClass) {
      return this._classEquipment.get(playerClass);
   }

   public static InitialEquipmentData getInstance() {
      return InitialEquipmentData.SingletonHolder.INSTANCE;
   }

   private static class SingletonHolder {
      protected static final InitialEquipmentData INSTANCE = new InitialEquipmentData();
   }
}
