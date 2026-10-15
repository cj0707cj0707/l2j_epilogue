package org.l2jmobius.gameserver.network.serverpackets;

import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.data.sql.ClanTable;
import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.actor.enums.player.Sex;
import org.l2jmobius.gameserver.entity.actor.holders.npc.FakePlayerHolder;
import org.l2jmobius.gameserver.entity.clan.Clan;
import org.l2jmobius.gameserver.entity.zone.ZoneId;

public class FakePlayerInfo extends ServerPacket {
   private final Npc _npc;
   private final int _objId;
   private final int _x;
   private final int _y;
   private final int _z;
   private final int _heading;
   private final int _mAtkSpd;
   private final int _pAtkSpd;
   private final int _runSpd;
   private final int _walkSpd;
   private final int _swimRunSpd;
   private final int _swimWalkSpd;
   private final int _flyRunSpd;
   private final int _flyWalkSpd;
   private final double _moveMultiplier;
   private final float _attackSpeedMultiplier;
   private final FakePlayerHolder _fpcHolder;
   private final Clan _clan;

   public FakePlayerInfo(Npc npc) {
      this._npc = npc;
      this._objId = npc.getObjectId();
      this._x = npc.getX();
      this._y = npc.getY();
      this._z = npc.getZ();
      this._heading = npc.getHeading();
      this._mAtkSpd = npc.getMAtkSpd();
      this._pAtkSpd = (int)npc.getPAtkSpd();
      this._attackSpeedMultiplier = npc.getAttackSpeedMultiplier();
      this._moveMultiplier = npc.getMovementSpeedMultiplier();
      this._runSpd = (int)Math.round(npc.getRunSpeed() / this._moveMultiplier);
      this._walkSpd = (int)Math.round(npc.getWalkSpeed() / this._moveMultiplier);
      this._swimRunSpd = (int)Math.round(npc.getSwimRunSpeed() / this._moveMultiplier);
      this._swimWalkSpd = (int)Math.round(npc.getSwimWalkSpeed() / this._moveMultiplier);
      this._flyRunSpd = npc.isFlying() ? this._runSpd : 0;
      this._flyWalkSpd = npc.isFlying() ? this._walkSpd : 0;
      this._fpcHolder = npc.getTemplate().getFakePlayerInfo();
      this._clan = ClanTable.getInstance().getClan(this._fpcHolder.getClanId());
   }

   @Override
   public void writeImpl(org.l2jmobius.gameserver.network.GameClient client, WriteBuffer buffer) {
      org.l2jmobius.gameserver.network.ServerPackets.CHAR_INFO.writeId(this, buffer);
      buffer.writeInt(this._x);
      buffer.writeInt(this._y);
      buffer.writeInt(this._z);
      buffer.writeInt(0);
      buffer.writeInt(this._objId);
      buffer.writeString(this._npc.getName());
      buffer.writeInt(this._npc.getRace().ordinal());
      buffer.writeInt(this._npc.getTemplate().getSex() == Sex.FEMALE);
      buffer.writeInt(this._fpcHolder.getPlayerClass().getId());
      buffer.writeInt(0);
      buffer.writeInt(this._fpcHolder.getEquipHead());
      buffer.writeInt(this._fpcHolder.getEquipRHand());
      buffer.writeInt(this._fpcHolder.getEquipLHand());
      buffer.writeInt(this._fpcHolder.getEquipGloves());
      buffer.writeInt(this._fpcHolder.getEquipChest());
      buffer.writeInt(this._fpcHolder.getEquipLegs());
      buffer.writeInt(this._fpcHolder.getEquipFeet());
      buffer.writeInt(this._fpcHolder.getEquipCloak());
      buffer.writeInt(this._fpcHolder.getEquipRHand());
      buffer.writeInt(this._fpcHolder.getEquipHair());
      buffer.writeInt(this._fpcHolder.getEquipHair2());
      buffer.writeInt(0);
      buffer.writeInt(0);
      buffer.writeInt(0);
      buffer.writeInt(0);
      buffer.writeInt(0);
      buffer.writeInt(0);
      buffer.writeInt(0);
      buffer.writeInt(0);
      buffer.writeInt(0);

      for (int i = 0; i < 21; i++) {
         buffer.writeInt(0);
      }

      buffer.writeInt(0);
      buffer.writeInt(1);
      buffer.writeInt(this._npc.getScriptValue());
      buffer.writeInt(this._npc.getKarma());
      buffer.writeInt(this._mAtkSpd);
      buffer.writeInt(this._pAtkSpd);
      buffer.writeInt(this._npc.getScriptValue());
      buffer.writeInt(this._npc.getKarma());
      buffer.writeInt(this._runSpd);
      buffer.writeInt(this._walkSpd);
      buffer.writeInt(this._swimRunSpd);
      buffer.writeInt(this._swimWalkSpd);
      buffer.writeInt(this._flyRunSpd);
      buffer.writeInt(this._flyWalkSpd);
      buffer.writeInt(this._flyRunSpd);
      buffer.writeInt(this._flyWalkSpd);
      buffer.writeDouble(this._moveMultiplier);
      buffer.writeDouble(this._attackSpeedMultiplier);
      buffer.writeDouble(this._npc.getCollisionRadius());
      buffer.writeDouble(this._npc.getCollisionHeight());
      buffer.writeInt(this._fpcHolder.getHair());
      buffer.writeInt(this._fpcHolder.getHairColor());
      buffer.writeInt(this._fpcHolder.getFace());
      buffer.writeString(this._npc.getTemplate().getTitle());
      if (this._clan != null) {
         buffer.writeInt(this._clan.getId());
         buffer.writeInt(this._clan.getCrestId());
         buffer.writeInt(this._clan.getAllyId());
         buffer.writeInt(this._clan.getAllyCrestId());
      } else {
         buffer.writeInt(0);
         buffer.writeInt(0);
         buffer.writeInt(0);
         buffer.writeInt(0);
      }

      buffer.writeInt(0);
      buffer.writeByte(!this._fpcHolder.isSitting());
      buffer.writeByte(this._npc.isRunning());
      buffer.writeByte(this._npc.isInCombat());
      buffer.writeByte(this._npc.isAlikeDead());
      buffer.writeByte(this._npc.isInvisible());
      buffer.writeByte(0);
      buffer.writeByte(this._fpcHolder.getPrivateStoreType());
      buffer.writeShort(0);
      buffer.writeByte(0);
      buffer.writeInt(this._npc.getAbnormalVisualEffects());
      buffer.writeByte(this._npc.isInsideZone(ZoneId.WATER));
      buffer.writeShort(this._fpcHolder.getRecommends());
      buffer.writeInt(0);
      buffer.writeInt(this._fpcHolder.getPlayerClass().getId());
      buffer.writeInt(0);
      buffer.writeByte(this._fpcHolder.getWeaponEnchantLevel());
      buffer.writeByte(this._npc.getTeam().getId());
      buffer.writeInt(this._clan != null ? this._clan.getCrestLargeId() : 0);
      buffer.writeByte(this._fpcHolder.getNobleLevel());
      buffer.writeByte(this._fpcHolder.isHero());
      buffer.writeByte(this._fpcHolder.isFishing());
      buffer.writeInt(this._fpcHolder.getBaitLocationX());
      buffer.writeInt(this._fpcHolder.getBaitLocationY());
      buffer.writeInt(this._fpcHolder.getBaitLocationZ());
      buffer.writeInt(this._fpcHolder.getNameColor());
      buffer.writeInt(this._heading);
      buffer.writeInt(this._fpcHolder.getPledgeStatus());
      buffer.writeInt(0);
      buffer.writeInt(this._fpcHolder.getTitleColor());
      buffer.writeInt(0);
      buffer.writeInt(0);
      buffer.writeInt(0);
      buffer.writeInt(this._fpcHolder.getAgathionId());
      buffer.writeInt(1);
      buffer.writeInt(this._npc.getAbnormalVisualEffectSpecial());
      buffer.writeInt(0);
      buffer.writeInt(0);
      buffer.writeInt(0);
   }
}
