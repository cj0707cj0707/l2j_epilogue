package org.l2jmobius.gameserver.network.serverpackets;

import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.entity.actor.Summon;
import org.l2jmobius.gameserver.entity.actor.instance.Pet;
import org.l2jmobius.gameserver.entity.actor.instance.Servitor;
import org.l2jmobius.gameserver.entity.zone.ZoneId;

public class PetInfo extends ServerPacket {
   private final Summon _summon;
   private final int _x;
   private final int _y;
   private final int _z;
   private final int _heading;
   private final boolean _isSummoned;
   private final int _value;
   private final int _mAtkSpd;
   private final int _pAtkSpd;
   private final int _runSpd;
   private final int _walkSpd;
   private final int _swimRunSpd;
   private final int _swimWalkSpd;
   private final int _flyRunSpd;
   private final int _flyWalkSpd;
   private final double _moveMultiplier;
   private final int _maxHp;
   private final int _maxMp;
   private int _maxFed;
   private int _curFed;

   public PetInfo(Summon summon, int value) {
      this._summon = summon;
      this._isSummoned = summon.isShowSummonAnimation();
      this._x = summon.getX();
      this._y = summon.getY();
      this._z = summon.getZ();
      this._heading = summon.getHeading();
      this._mAtkSpd = summon.getMAtkSpd();
      this._pAtkSpd = (int)summon.getPAtkSpd();
      this._moveMultiplier = summon.getMovementSpeedMultiplier();
      this._runSpd = (int)Math.round(summon.getRunSpeed() / this._moveMultiplier);
      this._walkSpd = (int)Math.round(summon.getWalkSpeed() / this._moveMultiplier);
      this._swimRunSpd = (int)Math.round(summon.getSwimRunSpeed() / this._moveMultiplier);
      this._swimWalkSpd = (int)Math.round(summon.getSwimWalkSpeed() / this._moveMultiplier);
      this._flyRunSpd = summon.isFlying() ? this._runSpd : 0;
      this._flyWalkSpd = summon.isFlying() ? this._walkSpd : 0;
      this._maxHp = summon.getMaxHp();
      this._maxMp = summon.getMaxMp();
      this._value = value;
      if (summon.isPet()) {
         Pet pet = this._summon.asPet();
         this._curFed = pet.getCurrentFed();
         this._maxFed = pet.getMaxFed();
      } else if (summon.isServitor()) {
         Servitor sum = this._summon.asServitor();
         this._curFed = sum.getLifeTimeRemaining();
         this._maxFed = sum.getLifeTime();
      }
   }

   @Override
   public void writeImpl(org.l2jmobius.gameserver.network.GameClient client, WriteBuffer buffer) {
      org.l2jmobius.gameserver.network.ServerPackets.PET_INFO.writeId(this, buffer);
      buffer.writeInt(this._summon.getSummonType());
      buffer.writeInt(this._summon.getObjectId());
      buffer.writeInt(this._summon.getTemplate().getDisplayId() + 1000000);
      buffer.writeInt(0);
      buffer.writeInt(this._x);
      buffer.writeInt(this._y);
      buffer.writeInt(this._z);
      buffer.writeInt(this._heading);
      buffer.writeInt(0);
      buffer.writeInt(this._mAtkSpd);
      buffer.writeInt(this._pAtkSpd);
      buffer.writeInt(this._runSpd);
      buffer.writeInt(this._walkSpd);
      buffer.writeInt(this._swimRunSpd);
      buffer.writeInt(this._swimWalkSpd);
      buffer.writeInt(this._flyRunSpd);
      buffer.writeInt(this._flyWalkSpd);
      buffer.writeInt(this._flyRunSpd);
      buffer.writeInt(this._flyWalkSpd);
      buffer.writeDouble(this._moveMultiplier);
      buffer.writeDouble(this._summon.getAttackSpeedMultiplier());
      buffer.writeDouble(this._summon.getTemplate().getFCollisionRadius());
      buffer.writeDouble(this._summon.getTemplate().getFCollisionHeight());
      buffer.writeInt(this._summon.getWeapon());
      buffer.writeInt(this._summon.getArmor());
      buffer.writeInt(0);
      buffer.writeByte(this._summon.getOwner() != null);
      buffer.writeByte(this._summon.isRunning());
      buffer.writeByte(this._summon.isInCombat());
      buffer.writeByte(this._summon.isAlikeDead());
      buffer.writeByte(this._isSummoned ? 2 : this._value);
      if (this._summon.isPet()) {
         buffer.writeString(this._summon.getName());
      } else {
         buffer.writeString(this._summon.getTemplate().isUsingServerSideName() ? this._summon.getName() : "");
      }

      buffer.writeString(this._summon.getTitle());
      buffer.writeInt(1);
      buffer.writeInt(this._summon.getPvpFlag());
      buffer.writeInt(this._summon.getKarma());
      buffer.writeInt(this._curFed);
      buffer.writeInt(this._maxFed);
      buffer.writeInt((int)this._summon.getCurrentHp());
      buffer.writeInt(this._maxHp);
      buffer.writeInt((int)this._summon.getCurrentMp());
      buffer.writeInt(this._maxMp);
      buffer.writeInt((int)this._summon.getStat().getSp());
      buffer.writeInt(this._summon.getLevel());
      buffer.writeLong(this._summon.getStat().getExp());
      if (this._summon.getExpForThisLevel() > this._summon.getStat().getExp()) {
         buffer.writeLong(this._summon.getStat().getExp());
      } else {
         buffer.writeLong(this._summon.getExpForThisLevel());
      }

      buffer.writeLong(this._summon.getExpForNextLevel());
      buffer.writeInt(this._summon.isPet() ? this._summon.getInventory().getTotalWeight() : 0);
      buffer.writeInt(this._summon.getMaxLoad());
      buffer.writeInt((int)this._summon.getPAtk(null));
      buffer.writeInt((int)this._summon.getPDef(null));
      buffer.writeInt((int)this._summon.getMAtk(null, null));
      buffer.writeInt((int)this._summon.getMDef(null, null));
      buffer.writeInt(this._summon.getAccuracy());
      buffer.writeInt(this._summon.getEvasionRate(null));
      buffer.writeInt(this._summon.getCriticalHit(null, null));
      buffer.writeInt((int)this._summon.getMoveSpeed());
      buffer.writeInt((int)this._summon.getPAtkSpd());
      buffer.writeInt(this._summon.getMAtkSpd());
      buffer.writeInt(this._summon.getAbnormalVisualEffects());
      buffer.writeShort(this._summon.isMountable());
      buffer.writeByte(this._summon.isInsideZone(ZoneId.WATER) ? 1 : (this._summon.isFlying() ? 2 : 0));
      buffer.writeShort(0);
      buffer.writeByte(this._summon.getTeam().getId());
      buffer.writeInt(this._summon.getSoulShotsPerHit());
      buffer.writeInt(this._summon.getSpiritShotsPerHit());
      buffer.writeInt(this._summon.getFormId());
      buffer.writeInt(this._summon.getAbnormalVisualEffectSpecial());
   }
}
