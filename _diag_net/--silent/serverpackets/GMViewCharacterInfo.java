package org.l2jmobius.gameserver.network.serverpackets;

import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.appearance.PlayerAppearance;

public class GMViewCharacterInfo extends ServerPacket {
   private final Player _player;
   private final int _runSpd;
   private final int _walkSpd;
   private final int _swimRunSpd;
   private final int _swimWalkSpd;
   private final int _flyRunSpd;
   private final int _flyWalkSpd;
   private final double _moveMultiplier;

   public GMViewCharacterInfo(Player player) {
      this._player = player;
      this._moveMultiplier = player.getMovementSpeedMultiplier();
      this._runSpd = (int)Math.round(player.getRunSpeed() / this._moveMultiplier);
      this._walkSpd = (int)Math.round(player.getWalkSpeed() / this._moveMultiplier);
      this._swimRunSpd = (int)Math.round(player.getSwimRunSpeed() / this._moveMultiplier);
      this._swimWalkSpd = (int)Math.round(player.getSwimWalkSpeed() / this._moveMultiplier);
      this._flyRunSpd = player.isFlying() ? this._runSpd : 0;
      this._flyWalkSpd = player.isFlying() ? this._walkSpd : 0;
   }

   @Override
   public void writeImpl(org.l2jmobius.gameserver.network.GameClient client, WriteBuffer buffer) {
      org.l2jmobius.gameserver.network.ServerPackets.GM_VIEW_CHARACTER_INFO.writeId(this, buffer);
      buffer.writeInt(this._player.getX());
      buffer.writeInt(this._player.getY());
      buffer.writeInt(this._player.getZ());
      buffer.writeInt(this._player.getHeading());
      buffer.writeInt(this._player.getObjectId());
      buffer.writeString(this._player.getName());
      buffer.writeInt(this._player.getRace().ordinal());
      PlayerAppearance appearance = this._player.getAppearance();
      buffer.writeInt(appearance.isFemale());
      buffer.writeInt(this._player.getPlayerClass().getId());
      buffer.writeInt(this._player.getLevel());
      buffer.writeLong(this._player.getExp());
      buffer.writeInt(this._player.getSTR());
      buffer.writeInt(this._player.getDEX());
      buffer.writeInt(this._player.getCON());
      buffer.writeInt(this._player.getINT());
      buffer.writeInt(this._player.getWIT());
      buffer.writeInt(this._player.getMEN());
      buffer.writeInt(this._player.getMaxHp());
      buffer.writeInt((int)this._player.getCurrentHp());
      buffer.writeInt(this._player.getMaxMp());
      buffer.writeInt((int)this._player.getCurrentMp());
      buffer.writeInt((int)this._player.getSp());
      buffer.writeInt(this._player.getCurrentLoad());
      buffer.writeInt(this._player.getMaxLoad());
      buffer.writeInt(this._player.getPkKills());

      for (int slot : this.getPaperdollOrder()) {
         buffer.writeInt(this._player.getInventory().getPaperdollObjectId(slot));
      }

      for (int slot : this.getPaperdollOrder()) {
         buffer.writeInt(this._player.getInventory().getPaperdollItemDisplayId(slot));
      }

      for (int slot : this.getPaperdollOrder()) {
         buffer.writeInt(this._player.getInventory().getPaperdollAugmentationId(slot));
      }

      buffer.writeInt(this._player.getInventory().getTalismanSlots());
      buffer.writeInt(this._player.getInventory().canEquipCloak());
      buffer.writeInt((int)this._player.getPAtk(null));
      buffer.writeInt((int)this._player.getPAtkSpd());
      buffer.writeInt((int)this._player.getPDef(null));
      buffer.writeInt(this._player.getEvasionRate(null));
      buffer.writeInt(this._player.getAccuracy());
      buffer.writeInt(this._player.getCriticalHit(null, null));
      buffer.writeInt((int)this._player.getMAtk(null, null));
      buffer.writeInt(this._player.getMAtkSpd());
      buffer.writeInt((int)this._player.getPAtkSpd());
      buffer.writeInt((int)this._player.getMDef(null, null));
      buffer.writeInt(this._player.getPvpFlag());
      buffer.writeInt(this._player.getKarma());
      buffer.writeInt(this._runSpd);
      buffer.writeInt(this._walkSpd);
      buffer.writeInt(this._swimRunSpd);
      buffer.writeInt(this._swimWalkSpd);
      buffer.writeInt(this._flyRunSpd);
      buffer.writeInt(this._flyWalkSpd);
      buffer.writeInt(this._flyRunSpd);
      buffer.writeInt(this._flyWalkSpd);
      buffer.writeDouble(this._moveMultiplier);
      buffer.writeDouble(this._player.getAttackSpeedMultiplier());
      buffer.writeDouble(this._player.getCollisionRadius());
      buffer.writeDouble(this._player.getCollisionHeight());
      buffer.writeInt(appearance.getHairStyle());
      buffer.writeInt(appearance.getHairColor());
      buffer.writeInt(appearance.getFace());
      buffer.writeInt(this._player.isGM());
      buffer.writeString(this._player.getTitle());
      buffer.writeInt(this._player.getClanId());
      buffer.writeInt(this._player.getClanCrestId());
      buffer.writeInt(this._player.getAllyId());
      buffer.writeByte(this._player.getMountType().ordinal());
      buffer.writeByte(this._player.getPrivateStoreType().getId());
      buffer.writeByte(this._player.hasDwarvenCraft());
      buffer.writeInt(this._player.getPkKills());
      buffer.writeInt(this._player.getPvpKills());
      buffer.writeShort(this._player.getRecomLeft());
      buffer.writeShort(this._player.getRecomHave());
      buffer.writeInt(this._player.getPlayerClass().getId());
      buffer.writeInt(0);
      buffer.writeInt(this._player.getMaxCp());
      buffer.writeInt((int)this._player.getCurrentCp());
      buffer.writeByte(this._player.isRunning());
      buffer.writeByte(321);
      buffer.writeInt(this._player.getPledgeClass());
      buffer.writeByte(this._player.isNoble());
      buffer.writeByte(this._player.isHero());
      buffer.writeInt(appearance.getNameColor());
      buffer.writeInt(appearance.getTitleColor());
      byte attackAttribute = this._player.getAttackElement();
      buffer.writeShort(attackAttribute);
      buffer.writeShort(this._player.getAttackElementValue(attackAttribute));

      for (byte i = 0; i < 6; i++) {
         buffer.writeShort(this._player.getDefenseElementValue(i));
      }

      buffer.writeInt(this._player.getFame());
      buffer.writeInt(this._player.getVitalityPoints());
   }
}
