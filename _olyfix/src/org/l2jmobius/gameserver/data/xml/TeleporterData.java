package org.l2jmobius.gameserver.data.xml;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import org.l2jmobius.commons.util.IXmlReader;
import org.l2jmobius.gameserver.entity.actor.enums.player.TeleportType;
import org.l2jmobius.gameserver.entity.teleporter.TeleportHolder;
import org.l2jmobius.gameserver.util.StatSet;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;

public class TeleporterData implements IXmlReader {
   private static final Logger LOGGER = Logger.getLogger(TeleporterData.class.getName());
   private final Map<Integer, Map<String, TeleportHolder>> _teleporters = new ConcurrentHashMap<>();

   protected TeleporterData() {
      this.load();
   }

   @Override
   public void load() {
      this._teleporters.clear();
      this.parseDatapackDirectory("data/teleporters", true);
      LOGGER.info(this.getClass().getSimpleName() + ": Loaded " + this._teleporters.size() + " npc teleporters.");
   }

   @Override
   public void parseDocument(Document document, File file) {
      this.forEach(document, "list", list -> this.forEach(list, "npc", npc -> {
         Map<String, TeleportHolder> teleList = new HashMap<>();
         int npcId = this.parseInteger(npc.getAttributes(), "id");
         this.forEach(npc, node -> {
            String s0$ = node.getNodeName();
            switch (s0$) {
               case "teleport":
                  NamedNodeMap nodeAttrs = node.getAttributes();
                  TeleportType type = this.parseEnum(nodeAttrs, TeleportType.class, "type");
                  String name = this.parseString(nodeAttrs, "name", type.name());
                  TeleportHolder holder = new TeleportHolder(name, type);
                  this.forEach(node, "location", location -> holder.registerLocation(new StatSet(this.parseAttributes(location))));
                  if (teleList.putIfAbsent(name, holder) != null) {
                     LOGGER.warning("Duplicate teleport list (" + name + ") has been found for NPC: " + npcId);
                  }
                  break;
               case "npcs":
                  this.forEach(node, "npc", npcNode -> {
                     int id = this.parseInteger(npcNode.getAttributes(), "id");
                     this.registerTeleportList(id, teleList);
                  });
            }
         });
         this.registerTeleportList(npcId, teleList);
      }));
   }

   public int getTeleporterCount() {
      return this._teleporters.size();
   }

   private void registerTeleportList(int npcId, Map<String, TeleportHolder> teleList) {
      this._teleporters.computeIfAbsent(npcId, k -> new ConcurrentHashMap<>()).putAll(teleList);
   }

   public TeleportHolder getHolder(int npcId, String listName) {
      return this._teleporters.getOrDefault(npcId, Collections.emptyMap()).get(listName);
   }

   public static TeleporterData getInstance() {
      return TeleporterData.SingletonHolder.INSTANCE;
   }

   private static class SingletonHolder {
      protected static final TeleporterData INSTANCE = new TeleporterData();
   }
}
