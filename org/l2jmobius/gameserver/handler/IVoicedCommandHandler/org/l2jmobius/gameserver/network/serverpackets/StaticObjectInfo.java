package org.l2jmobius.gameserver.network.serverpackets;

import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.entity.actor.instance.Door;
import org.l2jmobius.gameserver.entity.actor.instance.StaticObject;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;

public class StaticObjectInfo extends ServerPacket {
   private final int _staticObjectId;
   private final int _objectId;
   private final int _type;
   private final boolean _isTargetable;
   private final int _meshIndex;
   private final boolean _isClosed;
   private final boolean _isEnemy;
   private final int _maxHp;
   private final int _currentHp;
   private final boolean _showHp;
   private final int _damageGrade;

   public StaticObjectInfo(StaticObject staticObject) {
      this._staticObjectId = staticObject.getId();
      this._objectId = staticObject.getObjectId();
      this._type = 0;
      this._isTargetable = true;
      this._meshIndex = staticObject.getMeshIndex();
      this._isClosed = false;
      this._isEnemy = false;
      this._maxHp = 0;
      this._currentHp = 0;
      this._showHp = false;
      this._damageGrade = 0;
   }

   public StaticObjectInfo(Door door, boolean targetable) {
      this._staticObjectId = door.getId();
      this._objectId = door.getObjectId();
      this._type = 1;
      this._isTargetable = door.isTargetable() || targetable;
      this._meshIndex = door.getMeshIndex();
      this._isClosed = !door.isOpen();
      this._isEnemy = door.isEnemy();
      this._maxHp = door.getMaxHp();
      this._currentHp = (int)door.getCurrentHp();
      this._showHp = door.isShowHp();
      this._damageGrade = door.getDamage();
   }

   @Override
   public void writeImpl(GameClient client, WriteBuffer buffer) {
      ServerPackets.STATIC_OBJECT.writeId(this, buffer);
      buffer.writeInt(this._staticObjectId);
      buffer.writeInt(this._objectId);
      buffer.writeInt(this._type);
      buffer.writeInt(this._isTargetable);
      buffer.writeInt(this._meshIndex);
      buffer.writeInt(this._isClosed);
      buffer.writeInt(this._isEnemy);
      buffer.writeInt(this._currentHp);
      buffer.writeInt(this._maxHp);
      buffer.writeInt(this._showHp);
      buffer.writeInt(this._damageGrade);
   }
}
