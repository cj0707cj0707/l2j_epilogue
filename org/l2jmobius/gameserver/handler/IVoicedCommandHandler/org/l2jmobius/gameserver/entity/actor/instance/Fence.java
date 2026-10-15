package org.l2jmobius.gameserver.entity.actor.instance;

import org.l2jmobius.gameserver.data.xml.FenceData;
import org.l2jmobius.gameserver.entity.World;
import org.l2jmobius.gameserver.entity.WorldObject;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.enums.creature.FenceState;
import org.l2jmobius.gameserver.managers.IdManager;
import org.l2jmobius.gameserver.network.serverpackets.DeleteObject;
import org.l2jmobius.gameserver.network.serverpackets.ExColosseumFenceInfo;

public class Fence extends WorldObject {
   private final int _xMin;
   private final int _xMax;
   private final int _yMin;
   private final int _yMax;
   private final String _name;
   private final int _width;
   private final int _length;
   private FenceState _state;
   private int[] _heightFences;

   public Fence(int x, int y, String name, int width, int length, int height, FenceState state) {
      super(IdManager.getInstance().getNextId());
      this._xMin = x - width / 2;
      this._xMax = x + width / 2;
      this._yMin = y - length / 2;
      this._yMax = y + length / 2;
      this._name = name;
      this._width = width;
      this._length = length;
      this._state = state;
      if (height > 1) {
         this._heightFences = new int[height - 1];

         for (int i = 0; i < this._heightFences.length; i++) {
            this._heightFences[i] = IdManager.getInstance().getNextId();
         }
      }
   }

   @Override
   public int getId() {
      return this.getObjectId();
   }

   @Override
   public String getName() {
      return this._name;
   }

   @Override
   public boolean isAutoAttackable(Creature attacker) {
      return false;
   }

   @Override
   public void sendInfo(Player player) {
      player.sendPacket(new ExColosseumFenceInfo(this));
      if (this._heightFences != null) {
         for (int objId : this._heightFences) {
            player.sendPacket(new ExColosseumFenceInfo(objId, this.getX(), this.getY(), this.getZ(), this._width, this._length, this._state));
         }
      }
   }

   @Override
   public boolean decayMe() {
      if (this._heightFences != null) {
         DeleteObject[] deleteObjects = new DeleteObject[this._heightFences.length];

         for (int i = 0; i < this._heightFences.length; i++) {
            deleteObjects[i] = new DeleteObject(this._heightFences[i]);
         }

         World.forEachVisibleObject(this, Player.class, player -> {
            for (int ix = 0; ix < this._heightFences.length; ix++) {
               player.sendPacket(deleteObjects[ix]);
            }
         });
      }

      return super.decayMe();
   }

   public boolean deleteMe() {
      this.decayMe();
      FenceData.getInstance().removeFence(this);
      return false;
   }

   public FenceState getState() {
      return this._state;
   }

   public void setState(FenceState type) {
      this._state = type;
      this.broadcastInfo();
   }

   public int getWidth() {
      return this._width;
   }

   public int getLength() {
      return this._length;
   }

   public int getXMin() {
      return this._xMin;
   }

   public int getYMin() {
      return this._yMin;
   }

   public int getXMax() {
      return this._xMax;
   }

   public int getYMax() {
      return this._yMax;
   }

   @Override
   public boolean isFence() {
      return true;
   }
}
