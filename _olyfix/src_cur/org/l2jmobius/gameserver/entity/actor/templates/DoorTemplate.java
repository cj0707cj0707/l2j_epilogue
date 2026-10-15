package org.l2jmobius.gameserver.entity.actor.templates;

import org.l2jmobius.gameserver.util.StatSet;

public class DoorTemplate extends CreatureTemplate {
   private final int _doorId;
   private final int[] _nodeX;
   private final int[] _nodeY;
   private final int _nodeZ;
   private final int _height;
   private final int _posX;
   private final int _posY;
   private final int _posZ;
   private final int _emmiter;
   private final int _childDoorId;
   private final String _name;
   private final String _groupName;
   private final boolean _showHp;
   private final boolean _isWall;
   private final byte _masterDoorClose;
   private final byte _masterDoorOpen;
   private final boolean _isTargetable;
   private final boolean _default_status;
   private int _openTime;
   private int _randomTime;
   private final int _closeTime;
   private final int _level;
   private final int _openType;
   private final boolean _checkCollision;
   private final boolean _isAttackableDoor;
   private final int _clanhallId;
   private final boolean _stealth;

   public DoorTemplate(StatSet set) {
      super(set);
      this._doorId = set.getInt("id");
      this._name = set.getString("name");
      String[] pos = set.getString("pos").split(",");
      this._height = set.getInt("height");
      this._nodeZ = set.getInt("nodeZ");
      this._nodeX = new int[4];
      this._nodeY = new int[4];

      for (int i = 0; i < 4; i++) {
         String[] split = set.getString("node" + (i + 1)).split(",");
         this._nodeX[i] = Integer.parseInt(split[0]);
         this._nodeY[i] = Integer.parseInt(split[1]);
      }

      this._posX = Integer.parseInt(pos[0]);
      this._posY = Integer.parseInt(pos[1]);
      this._posZ = Math.min(Integer.parseInt(pos[2]), this._nodeZ);
      this._emmiter = set.getInt("emitter_id", 0);
      this._showHp = set.getBoolean("hp_showable", true);
      this._isWall = set.getBoolean("is_wall", false);
      this._groupName = set.getString("group", null);
      this._childDoorId = set.getInt("child_id_event", -1);
      String masterevent = set.getString("master_close_event", "act_nothing");
      this._masterDoorClose = (byte)(masterevent.equals("act_open") ? 1 : (masterevent.equals("act_close") ? -1 : 0));
      masterevent = set.getString("master_open_event", "act_nothing");
      this._masterDoorOpen = (byte)(masterevent.equals("act_open") ? 1 : (masterevent.equals("act_close") ? -1 : 0));
      this._isTargetable = set.getBoolean("targetable", true);
      this._default_status = set.getString("default_status", "close").equals("open");
      this._closeTime = set.getInt("close_time", -1);
      this._level = set.getInt("level", 0);
      this._openType = set.getInt("open_method", 0);
      this._checkCollision = set.getBoolean("check_collision", true);
      if ((this._openType & 2) == 2) {
         this._openTime = set.getInt("open_time");
         this._randomTime = set.getInt("random_time", -1);
      }

      this._isAttackableDoor = set.getBoolean("is_attackable", false);
      this._clanhallId = set.getInt("clanhall_id", 0);
      this._stealth = set.getBoolean("stealth", false);
   }

   public int getId() {
      return this._doorId;
   }

   public String getName() {
      return this._name;
   }

   public int[] getNodeX() {
      return this._nodeX;
   }

   public int[] getNodeY() {
      return this._nodeY;
   }

   public int getNodeZ() {
      return this._nodeZ;
   }

   public int getHeight() {
      return this._height;
   }

   public int getX() {
      return this._posX;
   }

   public int getY() {
      return this._posY;
   }

   public int getZ() {
      return this._posZ;
   }

   public int getEmmiter() {
      return this._emmiter;
   }

   public int getChildDoorId() {
      return this._childDoorId;
   }

   public String getGroupName() {
      return this._groupName;
   }

   public boolean isShowHp() {
      return this._showHp;
   }

   public boolean isWall() {
      return this._isWall;
   }

   public byte getMasterDoorOpen() {
      return this._masterDoorOpen;
   }

   public byte getMasterDoorClose() {
      return this._masterDoorClose;
   }

   public boolean isTargetable() {
      return this._isTargetable;
   }

   public boolean isOpenByDefault() {
      return this._default_status;
   }

   public int getOpenTime() {
      return this._openTime;
   }

   public int getRandomTime() {
      return this._randomTime;
   }

   public int getCloseTime() {
      return this._closeTime;
   }

   public int getLevel() {
      return this._level;
   }

   public int getOpenType() {
      return this._openType;
   }

   public boolean isCheckCollision() {
      return this._checkCollision;
   }

   public boolean isAttackable() {
      return this._isAttackableDoor;
   }

   public int getClanHallId() {
      return this._clanhallId;
   }

   public boolean isStealth() {
      return this._stealth;
   }
}
