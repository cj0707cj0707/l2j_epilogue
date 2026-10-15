package org.l2jmobius.gameserver.data.xml;

import java.io.File;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import org.l2jmobius.commons.util.IXmlReader;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.enums.player.MacroType;
import org.l2jmobius.gameserver.entity.actor.enums.player.PlayerClass;
import org.l2jmobius.gameserver.entity.actor.enums.player.ShortcutType;
import org.l2jmobius.gameserver.entity.actor.holders.player.Macro;
import org.l2jmobius.gameserver.entity.actor.holders.player.MacroCmd;
import org.l2jmobius.gameserver.entity.actor.holders.player.Shortcut;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.network.serverpackets.ShortcutRegister;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

public class InitialShortcutData implements IXmlReader {
   private static final Logger LOGGER = Logger.getLogger(InitialShortcutData.class.getName());
   private final Map<PlayerClass, List<Shortcut>> _initialShortcutData = new EnumMap<>(PlayerClass.class);
   private final List<Shortcut> _initialGlobalShortcutList = new ArrayList<>();
   private final Map<Integer, Macro> _macroPresets = new HashMap<>();

   protected InitialShortcutData() {
      this.load();
   }

   @Override
   public void load() {
      this._initialShortcutData.clear();
      this._initialGlobalShortcutList.clear();
      this.parseDatapackFile("data/stats/players/initialShortcuts.xml");
      LOGGER.info(this.getClass().getSimpleName() + ": Loaded " + this._initialGlobalShortcutList.size() + " initial global shortcut data.");
      LOGGER.info(this.getClass().getSimpleName() + ": Loaded " + this._initialShortcutData.size() + " initial shortcut data.");
      LOGGER.info(this.getClass().getSimpleName() + ": Loaded " + this._macroPresets.size() + " macro presets.");
   }

   @Override
   public void parseDocument(Document document, File file) {
      for (Node node = document.getFirstChild(); node != null; node = node.getNextSibling()) {
         if ("list".equals(node.getNodeName())) {
            for (Node dataNode = node.getFirstChild(); dataNode != null; dataNode = dataNode.getNextSibling()) {
               String var5 = dataNode.getNodeName();
               switch (var5) {
                  case "shortcuts":
                     NamedNodeMap attributes = dataNode.getAttributes();
                     Node classIdNode = attributes.getNamedItem("classId");
                     List<Shortcut> shortcutList = new ArrayList<>();
                     Node childNode = dataNode.getFirstChild();

                     for (; childNode != null; childNode = childNode.getNextSibling()) {
                        if ("page".equals(childNode.getNodeName())) {
                           attributes = childNode.getAttributes();
                           int pageId = this.parseInteger(attributes, "pageId");

                           for (Node slotNode = childNode.getFirstChild(); slotNode != null; slotNode = slotNode.getNextSibling()) {
                              if ("slot".equals(slotNode.getNodeName())) {
                                 NamedNodeMap slotAttributes = slotNode.getAttributes();
                                 int slotId = this.parseInteger(slotAttributes, "slotId");
                                 ShortcutType shortcutType = this.parseEnum(slotAttributes, ShortcutType.class, "shortcutType");
                                 int shortcutId = this.parseInteger(slotAttributes, "shortcutId");
                                 int shortcutLevel = this.parseInteger(slotAttributes, "shortcutLevel", 0);
                                 shortcutList.add(new Shortcut(slotId, pageId, shortcutType, shortcutId, shortcutLevel, 0));
                              }
                           }
                        }
                     }

                     if (classIdNode != null) {
                        this._initialShortcutData.put(PlayerClass.getPlayerClass(Integer.parseInt(classIdNode.getNodeValue())), shortcutList);
                     } else {
                        this._initialGlobalShortcutList.addAll(shortcutList);
                     }
                     break;
                  case "macros":
                     for (Node childNode = dataNode.getFirstChild(); childNode != null; childNode = childNode.getNextSibling()) {
                        if ("macro".equals(childNode.getNodeName())) {
                           NamedNodeMap attributes = childNode.getAttributes();
                           if (this.parseBoolean(attributes, "enabled", true)) {
                              int macroId = this.parseInteger(attributes, "macroId");
                              int icon = this.parseInteger(attributes, "icon");
                              String name = this.parseString(attributes, "name");
                              String description = this.parseString(attributes, "description");
                              String acronym = this.parseString(attributes, "acronym");
                              List<MacroCmd> commands = new ArrayList<>(1);
                              int entryIndex = 0;

                              for (Node commandNode = childNode.getFirstChild(); commandNode != null; commandNode = commandNode.getNextSibling()) {
                                 if ("command".equals(commandNode.getNodeName())) {
                                    attributes = commandNode.getAttributes();
                                    MacroType type = this.parseEnum(attributes, MacroType.class, "type");
                                    int parameterOne = 0;
                                    int parameterTwo = 0;
                                    String commandText = commandNode.getTextContent();
                                    switch (type) {
                                       case SKILL:
                                          parameterOne = this.parseInteger(attributes, "skillId");
                                          parameterTwo = this.parseInteger(attributes, "skillLevel", 0);
                                          break;
                                       case ACTION:
                                          parameterOne = this.parseInteger(attributes, "actionId");
                                       case TEXT:
                                       default:
                                          break;
                                       case SHORTCUT:
                                          parameterOne = this.parseInteger(attributes, "page");
                                          parameterTwo = this.parseInteger(attributes, "slot", 0);
                                          break;
                                       case ITEM:
                                          parameterOne = this.parseInteger(attributes, "itemId");
                                          break;
                                       case DELAY:
                                          parameterOne = this.parseInteger(attributes, "delay");
                                    }

                                    commands.add(new MacroCmd(entryIndex++, type, parameterOne, parameterTwo, commandText));
                                 }
                              }

                              this._macroPresets.put(macroId, new Macro(macroId, icon, name, description, acronym, commands));
                           }
                        }
                     }
               }
            }
         }
      }
   }

   public void registerAllShortcuts(Player player) {
      if (player != null) {
         for (Shortcut shortcut : this._initialGlobalShortcutList) {
            int shortcutId = shortcut.getId();
            switch (shortcut.getType()) {
               case ITEM:
                  Item item = player.getInventory().getItemByItemId(shortcutId);
                  if (item == null) {
                     continue;
                  }

                  shortcutId = item.getObjectId();
                  break;
               case SKILL:
                  if (!player.getSkills().containsKey(shortcutId)) {
                     continue;
                  }
                  break;
               case MACRO:
                  Macro macro = this._macroPresets.get(shortcutId);
                  if (macro == null) {
                     continue;
                  }

                  player.registerMacro(macro);
            }

            Shortcut newShortcut = new Shortcut(
               shortcut.getSlot(), shortcut.getPage(), shortcut.getType(), shortcutId, shortcut.getLevel(), shortcut.getCharacterType()
            );
            player.sendPacket(new ShortcutRegister(newShortcut));
            player.registerShortcut(newShortcut);
         }

         if (this._initialShortcutData.containsKey(player.getPlayerClass())) {
            for (Shortcut shortcut : this._initialShortcutData.get(player.getPlayerClass())) {
               int shortcutId = shortcut.getId();
               switch (shortcut.getType()) {
                  case ITEM:
                     Item item = player.getInventory().getItemByItemId(shortcutId);
                     if (item == null) {
                        continue;
                     }

                     shortcutId = item.getObjectId();
                     break;
                  case SKILL:
                     if (!player.getSkills().containsKey(shortcut.getId())) {
                        continue;
                     }
                     break;
                  case MACRO:
                     Macro macro = this._macroPresets.get(shortcutId);
                     if (macro == null) {
                        continue;
                     }

                     player.registerMacro(macro);
               }

               Shortcut newShortcut = new Shortcut(
                  shortcut.getSlot(), shortcut.getPage(), shortcut.getType(), shortcutId, shortcut.getLevel(), shortcut.getCharacterType()
               );
               player.sendPacket(new ShortcutRegister(newShortcut));
               player.registerShortcut(newShortcut);
            }
         }
      }
   }

   public static InitialShortcutData getInstance() {
      return InitialShortcutData.SingletonHolder.INSTANCE;
   }

   private static class SingletonHolder {
      protected static final InitialShortcutData INSTANCE = new InitialShortcutData();
   }
}
