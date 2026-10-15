package org.l2jmobius.gameserver.network.serverpackets;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import org.l2jmobius.commons.database.DatabaseFactory;
import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.config.ServerConfig;
import org.l2jmobius.gameserver.config.custom.MultilingualSupportConfig;
import org.l2jmobius.gameserver.config.custom.OfflinePlayConfig;
import org.l2jmobius.gameserver.config.custom.OfflineTradeConfig;
import org.l2jmobius.gameserver.data.sql.ClanTable;
import org.l2jmobius.gameserver.entity.World;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.clan.Clan;
import org.l2jmobius.gameserver.network.holders.CharacterInfoHolder;

public class CharSelectionInfo extends ServerPacket {
   private final String _loginName;
   private final int _sessionId;
   private int _activeId;
   private final List<CharacterInfoHolder> _characterPackages;

   public CharSelectionInfo(String loginName, int sessionId) {
      this._sessionId = sessionId;
      this._loginName = loginName;
      this._characterPackages = loadCharacterSelectInfo(this._loginName);
      this._activeId = -1;
   }

   public CharSelectionInfo(String loginName, int sessionId, int activeId) {
      this._sessionId = sessionId;
      this._loginName = loginName;
      this._characterPackages = loadCharacterSelectInfo(this._loginName);
      this._activeId = activeId;
   }

   public List<CharacterInfoHolder> getCharInfo() {
      return this._characterPackages;
   }

   @Override
   public void writeImpl(org.l2jmobius.gameserver.network.GameClient client, WriteBuffer buffer) {
      org.l2jmobius.gameserver.network.ServerPackets.CHARACTER_SELECTION_INFO.writeId(this, buffer);
      int size = this._characterPackages.size();
      buffer.writeInt(size);
      buffer.writeInt(ServerConfig.MAX_CHARACTERS_NUMBER_PER_ACCOUNT);
      buffer.writeByte(0);
      long lastAccess = 0L;
      if (this._activeId == -1) {
         for (int i = 0; i < size; i++) {
            if (lastAccess < this._characterPackages.get(i).getLastAccess()) {
               lastAccess = this._characterPackages.get(i).getLastAccess();
               this._activeId = i;
            }
         }
      }

      for (int ix = 0; ix < size; ix++) {
         CharacterInfoHolder charInfoPackage = this._characterPackages.get(ix);
         buffer.writeString(charInfoPackage.getName());
         buffer.writeInt(charInfoPackage.getObjectId());
         buffer.writeString(this._loginName);
         buffer.writeInt(this._sessionId);
         buffer.writeInt(charInfoPackage.getClanId());
         buffer.writeInt(0);
         buffer.writeInt(charInfoPackage.getSex());
         buffer.writeInt(charInfoPackage.getRace());
         buffer.writeInt(charInfoPackage.getBaseClassId());
         buffer.writeInt(1);
         buffer.writeInt(charInfoPackage.getX());
         buffer.writeInt(charInfoPackage.getY());
         buffer.writeInt(charInfoPackage.getZ());
         buffer.writeDouble(charInfoPackage.getCurrentHp());
         buffer.writeDouble(charInfoPackage.getCurrentMp());
         buffer.writeInt((int)charInfoPackage.getSp());
         buffer.writeLong(charInfoPackage.getExp());
         buffer.writeInt(charInfoPackage.getLevel());
         buffer.writeInt(charInfoPackage.getKarma());
         buffer.writeInt(charInfoPackage.getPkKills());
         buffer.writeInt(charInfoPackage.getPvPKills());
         buffer.writeInt(0);
         buffer.writeInt(0);
         buffer.writeInt(0);
         buffer.writeInt(0);
         buffer.writeInt(0);
         buffer.writeInt(0);
         buffer.writeInt(0);

         for (int slot : this.getPaperdollOrder()) {
            buffer.writeInt(charInfoPackage.getPaperdollItemId(slot));
         }

         buffer.writeInt(charInfoPackage.getHairStyle());
         buffer.writeInt(charInfoPackage.getHairColor());
         buffer.writeInt(charInfoPackage.getFace());
         buffer.writeDouble(charInfoPackage.getMaxHp());
         buffer.writeDouble(charInfoPackage.getMaxMp());
         if (charInfoPackage.getAccessLevel() > -1) {
            if (charInfoPackage.getDeleteTimer() > 0L) {
               buffer.writeInt((int)((charInfoPackage.getDeleteTimer() - System.currentTimeMillis()) / 1000L));
            } else {
               buffer.writeInt(0);
            }
         } else {
            buffer.writeInt(-1);
         }

         buffer.writeInt(charInfoPackage.getClassId());
         buffer.writeInt(ix == this._activeId);
         buffer.writeByte(Math.min(charInfoPackage.getEnchantEffect(), 127));
         buffer.writeInt(charInfoPackage.getAugmentationId());
         buffer.writeInt(0);
      }
   }

   private static List<CharacterInfoHolder> loadCharacterSelectInfo(String loginName) {
      List<CharacterInfoHolder> characterList = new ArrayList<>();

      try (
         Connection con = DatabaseFactory.getConnection();
         PreparedStatement statement = con.prepareStatement("SELECT * FROM characters WHERE account_name=? ORDER BY createDate");
      ) {
         statement.setString(1, loginName);

         try (ResultSet charList = statement.executeQuery()) {
            while (charList.next()) {
               CharacterInfoHolder charInfopackage = restoreChar(charList);
               if (charInfopackage != null) {
                  characterList.add(charInfopackage);
                  if (OfflineTradeConfig.OFFLINE_DISCONNECT_SAME_ACCOUNT) {
                     Player player = World.getPlayer(charInfopackage.getObjectId());
                     if (player != null && player.isInStoreMode()) {
                        org.l2jmobius.gameserver.network.Disconnection.of(player).storeAndDelete();
                        continue;
                     }
                  }

                  if (OfflinePlayConfig.OFFLINE_PLAY_DISCONNECT_SAME_ACCOUNT) {
                     Player player = World.getPlayer(charInfopackage.getObjectId());
                     if (player != null && player.isOfflinePlay()) {
                        org.l2jmobius.gameserver.network.Disconnection.of(player).storeAndDelete();
                     }
                  }
               }
            }
         }
      } catch (Exception var14) {
         org.l2jmobius.gameserver.network.PacketLogger.warning("Could not restore char info: " + var14.getMessage() + " " + var14);
      }

      return characterList;
   }

   private static void loadCharacterSubclassInfo(CharacterInfoHolder charInfopackage, int objectId, int activeClassId) {
      try (
         Connection con = DatabaseFactory.getConnection();
         PreparedStatement statement = con.prepareStatement("SELECT exp, sp, level FROM character_subclasses WHERE charId=? AND class_id=? ORDER BY charId");
      ) {
         statement.setInt(1, objectId);
         statement.setInt(2, activeClassId);

         try (ResultSet charList = statement.executeQuery()) {
            if (charList.next()) {
               charInfopackage.setExp(charList.getLong("exp"));
               charInfopackage.setSp(charList.getInt("sp"));
               charInfopackage.setLevel(charList.getInt("level"));
            }
         }
      } catch (Exception var14) {
         org.l2jmobius.gameserver.network.PacketLogger.warning("Could not restore char subclass info: " + var14.getMessage() + " " + var14);
      }
   }

   private static CharacterInfoHolder restoreChar(ResultSet chardata) throws Exception {
      int objectId = chardata.getInt("charId");
      String name = chardata.getString("char_name");
      long deletetime = chardata.getLong("deletetime");
      if (deletetime > 0L && System.currentTimeMillis() > deletetime) {
         Clan clan = ClanTable.getInstance().getClan(chardata.getInt("clanid"));
         if (clan != null) {
            clan.removeClanMember(objectId, 0L);
         }

         org.l2jmobius.gameserver.network.GameClient.deleteCharByObjId(objectId);
         return null;
      } else {
         CharacterInfoHolder charInfopackage = new CharacterInfoHolder(objectId, name);
         charInfopackage.setAccessLevel(chardata.getInt("accesslevel"));
         charInfopackage.setLevel(chardata.getInt("level"));
         charInfopackage.setMaxHp(chardata.getInt("maxhp"));
         charInfopackage.setCurrentHp(chardata.getDouble("curhp"));
         charInfopackage.setMaxMp(chardata.getInt("maxmp"));
         charInfopackage.setCurrentMp(chardata.getDouble("curmp"));
         charInfopackage.setKarma(chardata.getInt("karma"));
         charInfopackage.setPkKills(chardata.getInt("pkkills"));
         charInfopackage.setPvPKills(chardata.getInt("pvpkills"));
         charInfopackage.setFace(chardata.getInt("face"));
         charInfopackage.setHairStyle(chardata.getInt("hairstyle"));
         charInfopackage.setHairColor(chardata.getInt("haircolor"));
         charInfopackage.setSex(chardata.getInt("sex"));
         charInfopackage.setExp(chardata.getLong("exp"));
         charInfopackage.setSp(chardata.getLong("sp"));
         charInfopackage.setVitalityPoints(chardata.getInt("vitality_points"));
         charInfopackage.setClanId(chardata.getInt("clanid"));
         charInfopackage.setRace(chardata.getInt("race"));
         int baseClassId = chardata.getInt("base_class");
         int activeClassId = chardata.getInt("classid");
         charInfopackage.setX(chardata.getInt("x"));
         charInfopackage.setY(chardata.getInt("y"));
         charInfopackage.setZ(chardata.getInt("z"));
         int faction = chardata.getInt("faction");
         if (faction == 1) {
            charInfopackage.setGood();
         }

         if (faction == 2) {
            charInfopackage.setEvil();
         }

         if (MultilingualSupportConfig.MULTILANG_ENABLE) {
            String lang = chardata.getString("language");
            if (!MultilingualSupportConfig.MULTILANG_ALLOWED.contains(lang)) {
               lang = MultilingualSupportConfig.MULTILANG_DEFAULT;
            }

            charInfopackage.setHtmlPrefix("data/lang/" + lang + "/");
         }

         if (baseClassId != activeClassId) {
            loadCharacterSubclassInfo(charInfopackage, objectId, activeClassId);
         }

         charInfopackage.setClassId(activeClassId);
         int weaponObjId = charInfopackage.getPaperdollObjectId(5);
         if (weaponObjId < 1) {
            weaponObjId = charInfopackage.getPaperdollObjectId(5);
         }

         if (weaponObjId > 0) {
            try (
               Connection con = DatabaseFactory.getConnection();
               PreparedStatement statement = con.prepareStatement("SELECT augAttributes FROM item_attributes WHERE itemId=?");
            ) {
               statement.setInt(1, weaponObjId);

               try (ResultSet result = statement.executeQuery()) {
                  if (result.next()) {
                     int augment = result.getInt("augAttributes");
                     charInfopackage.setAugmentationId(augment == -1 ? 0 : augment);
                  }
               }
            } catch (Exception var21) {
               org.l2jmobius.gameserver.network.PacketLogger.warning("Could not restore augmentation info: " + var21.getMessage() + " " + var21);
            }
         }

         if (baseClassId == 0 && activeClassId > 0) {
            charInfopackage.setBaseClassId(activeClassId);
         } else {
            charInfopackage.setBaseClassId(baseClassId);
         }

         charInfopackage.setDeleteTimer(deletetime);
         charInfopackage.setLastAccess(chardata.getLong("lastAccess"));
         return charInfopackage;
      }
   }
}
