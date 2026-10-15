package org.l2jmobius.gameserver.network.clientpackets;

import java.util.Collection;
import java.util.logging.Logger;
import org.l2jmobius.commons.util.StringUtil;
import org.l2jmobius.gameserver.config.PlayerConfig;
import org.l2jmobius.gameserver.config.ServerConfig;
import org.l2jmobius.gameserver.config.custom.AllowedPlayerRacesConfig;
import org.l2jmobius.gameserver.config.custom.FactionSystemConfig;
import org.l2jmobius.gameserver.config.custom.StartingLocationConfig;
import org.l2jmobius.gameserver.config.custom.StartingTitleConfig;
import org.l2jmobius.gameserver.data.sql.CharInfoTable;
import org.l2jmobius.gameserver.data.xml.FakePlayerData;
import org.l2jmobius.gameserver.data.xml.InitialEquipmentData;
import org.l2jmobius.gameserver.data.xml.InitialShortcutData;
import org.l2jmobius.gameserver.data.xml.PlayerTemplateData;
import org.l2jmobius.gameserver.data.xml.SkillData;
import org.l2jmobius.gameserver.data.xml.SkillTreeData;
import org.l2jmobius.gameserver.entity.Location;
import org.l2jmobius.gameserver.entity.World;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.appearance.PlayerAppearance;
import org.l2jmobius.gameserver.entity.actor.enums.player.PlayerClass;
import org.l2jmobius.gameserver.entity.actor.stat.PlayerStat;
import org.l2jmobius.gameserver.entity.actor.templates.PlayerTemplate;
import org.l2jmobius.gameserver.entity.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.entity.item.holders.InitialEquipment;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.mechanics.events.Containers;
import org.l2jmobius.gameserver.mechanics.events.EventDispatcher;
import org.l2jmobius.gameserver.mechanics.events.EventType;
import org.l2jmobius.gameserver.mechanics.events.holders.actor.player.OnPlayerCreate;
import org.l2jmobius.gameserver.mechanics.skill.holders.SkillLearn;
import org.l2jmobius.gameserver.network.Disconnection;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.PacketLogger;
import org.l2jmobius.gameserver.network.serverpackets.CharCreateFail;
import org.l2jmobius.gameserver.network.serverpackets.CharCreateOk;
import org.l2jmobius.gameserver.network.serverpackets.CharSelectionInfo;

public class CharacterCreate extends ClientPacket {
   protected static final Logger LOGGER_ACCOUNTING = Logger.getLogger("accounting");
   private String _name;
   private boolean _isFemale;
   private int _classId;
   private byte _hairStyle;
   private byte _hairColor;
   private byte _face;

   @Override
   protected void readImpl() {
      this._name = this.readString();
      this.readInt();
      this._isFemale = this.readInt() != 0;
      this._classId = this.readInt();
      this.readInt();
      this.readInt();
      this.readInt();
      this.readInt();
      this.readInt();
      this.readInt();
      this._hairStyle = (byte)this.readInt();
      this._hairColor = (byte)this.readInt();
      this._face = (byte)this.readInt();
   }

   @Override
   protected void runImpl() {
      GameClient client = this.getClient();
      if (!this._name.isEmpty() && this._name.length() <= 16) {
         if (PlayerConfig.FORBIDDEN_NAMES.length > 0) {
            for (String st : PlayerConfig.FORBIDDEN_NAMES) {
               if (this._name.toLowerCase().contains(st.toLowerCase())) {
                  client.sendPacket(new CharCreateFail(4));
                  return;
               }
            }
         }

         if (FakePlayerData.getInstance().getProperName(this._name) != null) {
            client.sendPacket(new CharCreateFail(4));
         } else if (!StringUtil.isAlphaNumeric(this._name) || !this.isValidName(this._name)) {
            client.sendPacket(new CharCreateFail(4));
         } else if (this._face > 2 || this._face < 0) {
            PacketLogger.warning("Character Creation Failure: Character face " + this._face + " is invalid. Possible client hack. " + client);
            client.sendPacket(new CharCreateFail(0));
         } else if (this._hairStyle >= 0 && (this._isFemale || this._hairStyle <= 4) && (!this._isFemale || this._hairStyle <= 6)) {
            if (this._hairColor <= 3 && this._hairColor >= 0) {
               Player newChar = null;
               PlayerTemplate template = null;
               synchronized (CharInfoTable.getInstance()) {
                  if (CharInfoTable.getInstance().getAccountCharacterCount(client.getAccountName()) >= ServerConfig.MAX_CHARACTERS_NUMBER_PER_ACCOUNT
                     && ServerConfig.MAX_CHARACTERS_NUMBER_PER_ACCOUNT != 0) {
                     client.sendPacket(new CharCreateFail(1));
                     return;
                  }

                  if (CharInfoTable.getInstance().doesCharNameExist(this._name)) {
                     client.sendPacket(new CharCreateFail(2));
                     return;
                  }

                  template = PlayerTemplateData.getInstance().getTemplate(this._classId);
                  if (template == null || PlayerClass.getPlayerClass(this._classId).level() > 0) {
                     client.sendPacket(new CharCreateFail(0));
                     return;
                  }

                  switch (template.getRace()) {
                     case HUMAN:
                        if (!AllowedPlayerRacesConfig.ALLOW_HUMAN) {
                           client.sendPacket(new CharCreateFail(0));
                           return;
                        }
                        break;
                     case ELF:
                        if (!AllowedPlayerRacesConfig.ALLOW_ELF) {
                           client.sendPacket(new CharCreateFail(0));
                           return;
                        }
                        break;
                     case DARK_ELF:
                        if (!AllowedPlayerRacesConfig.ALLOW_DARKELF) {
                           client.sendPacket(new CharCreateFail(0));
                           return;
                        }
                        break;
                     case ORC:
                        if (!AllowedPlayerRacesConfig.ALLOW_ORC) {
                           client.sendPacket(new CharCreateFail(0));
                           return;
                        }
                        break;
                     case DWARF:
                        if (!AllowedPlayerRacesConfig.ALLOW_DWARF) {
                           client.sendPacket(new CharCreateFail(0));
                           return;
                        }
                        break;
                     case KAMAEL:
                        if (!AllowedPlayerRacesConfig.ALLOW_KAMAEL) {
                           client.sendPacket(new CharCreateFail(0));
                           return;
                        }
                  }

                  newChar = Player.create(
                     template, client.getAccountName(), this._name, new PlayerAppearance(this._face, this._hairColor, this._hairStyle, this._isFemale)
                  );
               }

               newChar.setCurrentHp(newChar.getMaxHp());
               newChar.setCurrentMp(newChar.getMaxMp());
               this.initNewChar(client, newChar);
               client.sendPacket(CharCreateOk.STATIC_PACKET);
               LOGGER_ACCOUNTING.info("Created new character, " + newChar + ", " + client);
            } else {
               PacketLogger.warning("Character Creation Failure: Character hair color " + this._hairColor + " is invalid. Possible client hack. " + client);
               client.sendPacket(new CharCreateFail(0));
            }
         } else {
            PacketLogger.warning("Character Creation Failure: Character hair style " + this._hairStyle + " is invalid. Possible client hack. " + client);
            client.sendPacket(new CharCreateFail(0));
         }
      } else {
         client.sendPacket(new CharCreateFail(3));
      }
   }

   private boolean isValidName(String text) {
      return ServerConfig.CHARNAME_TEMPLATE_PATTERN.matcher(text).matches();
   }

   private void initNewChar(GameClient client, Player newChar) {
      World.addObject(newChar);
      if (PlayerConfig.STARTING_ADENA > 0L) {
         newChar.addAdena(ItemProcessType.REWARD, PlayerConfig.STARTING_ADENA, null, false);
      }

      PlayerTemplate template = newChar.getTemplate();
      if (StartingLocationConfig.CUSTOM_STARTING_LOC) {
         Location createLoc = new Location(
            StartingLocationConfig.CUSTOM_STARTING_LOC_X, StartingLocationConfig.CUSTOM_STARTING_LOC_Y, StartingLocationConfig.CUSTOM_STARTING_LOC_Z
         );
         newChar.setXYZInvisible(createLoc.getX(), createLoc.getY(), createLoc.getZ());
      } else if (FactionSystemConfig.FACTION_SYSTEM_ENABLED) {
         newChar.setXYZInvisible(
            FactionSystemConfig.FACTION_STARTING_LOCATION.getX(),
            FactionSystemConfig.FACTION_STARTING_LOCATION.getY(),
            FactionSystemConfig.FACTION_STARTING_LOCATION.getZ()
         );
      } else {
         Location createLoc = template.getCreationPoint();
         newChar.setXYZInvisible(createLoc.getX(), createLoc.getY(), createLoc.getZ());
      }

      newChar.setTitle(StartingTitleConfig.ENABLE_CUSTOM_STARTING_TITLE ? StartingTitleConfig.CUSTOM_STARTING_TITLE : "");
      if (PlayerConfig.ENABLE_VITALITY) {
         newChar.setVitalityPoints(Math.min(PlayerConfig.STARTING_VITALITY_POINTS, PlayerStat.MAX_VITALITY_POINTS), true);
      }

      if (PlayerConfig.STARTING_LEVEL > 1) {
         newChar.getStat().addLevel((byte)(PlayerConfig.STARTING_LEVEL - 1));
      }

      if (PlayerConfig.STARTING_SP > 0) {
         newChar.getStat().addSp(PlayerConfig.STARTING_SP);
      }

      Collection<InitialEquipment> classEquipment = InitialEquipmentData.getInstance().getClassEquipment(newChar.getPlayerClass());
      if (classEquipment != null) {
         for (InitialEquipment equipment : classEquipment) {
            Item item = newChar.getInventory().addItem(ItemProcessType.REWARD, equipment.getId(), equipment.getCount(), newChar, null);
            if (item == null) {
               PacketLogger.warning("Could not create item during player creation: itemId " + equipment.getId() + ", amount " + equipment.getCount() + ".");
            } else if (item.isEquipable() && equipment.isEquipped()) {
               newChar.getInventory().equipItem(item);
            }
         }
      }

      for (SkillLearn skill : SkillTreeData.getInstance().getAvailableSkills(newChar, newChar.getPlayerClass(), false, true)) {
         newChar.addSkill(SkillData.getInstance().getSkill(skill.getSkillId(), skill.getSkillLevel()), true);
      }

      InitialShortcutData.getInstance().registerAllShortcuts(newChar);
      if (EventDispatcher.getInstance().hasListener(EventType.ON_PLAYER_CREATE, Containers.Players())) {
         EventDispatcher.getInstance().notifyEvent(new OnPlayerCreate(newChar, newChar.getObjectId(), newChar.getName(), client), Containers.Players());
      }

      newChar.setOnlineStatus(true, false);
      Disconnection.of(client, newChar).storeAndDelete();
      CharSelectionInfo cl = new CharSelectionInfo(client.getAccountName(), client.getSessionId().playOkID1);
      client.setCharSelection(cl.getCharInfo());
   }
}
