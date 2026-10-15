package org.l2jmobius.gameserver.network.holders;

import org.l2jmobius.gameserver.entity.itemcontainer.PlayerInventory;

public class CharacterInfoHolder {
   private String _name;
   private int _objectId = 0;
   private long _exp = 0L;
   private long _sp = 0L;
   private int _clanId = 0;
   private int _race = 0;
   private int _classId = 0;
   private int _baseClassId = 0;
   private long _deleteTimer = 0L;
   private long _lastAccess = 0L;
   private int _face = 0;
   private int _hairStyle = 0;
   private int _hairColor = 0;
   private int _sex = 0;
   private int _level = 1;
   private int _maxHp = 0;
   private double _currentHp = 0.0;
   private int _maxMp = 0;
   private double _currentMp = 0.0;
   private final int[][] _paperdoll;
   private int _karma = 0;
   private int _pkKills = 0;
   private int _pvpKills = 0;
   private int _augmentationId = 0;
   private int _x = 0;
   private int _y = 0;
   private int _z = 0;
   private String _htmlPrefix = null;
   private boolean _isGood = false;
   private boolean _isEvil = false;
   private int _vitalityPoints = 0;
   private int _accessLevel = 0;

   public CharacterInfoHolder(int objectId, String name) {
      this.setObjectId(objectId);
      this._name = name;
      this._paperdoll = PlayerInventory.restoreVisibleInventory(objectId);
   }

   public int getObjectId() {
      return this._objectId;
   }

   public void setObjectId(int objectId) {
      this._objectId = objectId;
   }

   public int getAccessLevel() {
      return this._accessLevel;
   }

   public void setAccessLevel(int level) {
      this._accessLevel = level;
   }

   public boolean isGood() {
      return this._isGood;
   }

   public void setGood() {
      this._isGood = true;
      this._isEvil = false;
   }

   public boolean isEvil() {
      return this._isEvil;
   }

   public void setEvil() {
      this._isGood = false;
      this._isEvil = true;
   }

   public int getClanId() {
      return this._clanId;
   }

   public void setClanId(int clanId) {
      this._clanId = clanId;
   }

   public int getClassId() {
      return this._classId;
   }

   public int getBaseClassId() {
      return this._baseClassId;
   }

   public void setClassId(int classId) {
      this._classId = classId;
   }

   public void setBaseClassId(int baseClassId) {
      this._baseClassId = baseClassId;
   }

   public double getCurrentHp() {
      return this._currentHp;
   }

   public void setCurrentHp(double currentHp) {
      this._currentHp = currentHp;
   }

   public double getCurrentMp() {
      return this._currentMp;
   }

   public void setCurrentMp(double currentMp) {
      this._currentMp = currentMp;
   }

   public long getDeleteTimer() {
      return this._deleteTimer;
   }

   public void setDeleteTimer(long deleteTimer) {
      this._deleteTimer = deleteTimer;
   }

   public long getLastAccess() {
      return this._lastAccess;
   }

   public void setLastAccess(long lastAccess) {
      this._lastAccess = lastAccess;
   }

   public long getExp() {
      return this._exp;
   }

   public void setExp(long exp) {
      this._exp = exp;
   }

   public int getFace() {
      return this._face;
   }

   public void setFace(int face) {
      this._face = face;
   }

   public int getHairColor() {
      return this._hairColor;
   }

   public void setHairColor(int hairColor) {
      this._hairColor = hairColor;
   }

   public int getHairStyle() {
      return this._hairStyle;
   }

   public void setHairStyle(int hairStyle) {
      this._hairStyle = hairStyle;
   }

   public int getPaperdollObjectId(int slot) {
      return this._paperdoll[slot][0];
   }

   public int getPaperdollItemId(int slot) {
      return this._paperdoll[slot][1];
   }

   public int getLevel() {
      return this._level;
   }

   public void setLevel(int level) {
      this._level = level;
   }

   public int getMaxHp() {
      return this._maxHp;
   }

   public void setMaxHp(int maxHp) {
      this._maxHp = maxHp;
   }

   public int getMaxMp() {
      return this._maxMp;
   }

   public void setMaxMp(int maxMp) {
      this._maxMp = maxMp;
   }

   public String getName() {
      return this._name;
   }

   public void setName(String name) {
      this._name = name;
   }

   public int getRace() {
      return this._race;
   }

   public void setRace(int race) {
      this._race = race;
   }

   public int getSex() {
      return this._sex;
   }

   public void setSex(int sex) {
      this._sex = sex;
   }

   public long getSp() {
      return this._sp;
   }

   public void setSp(long sp) {
      this._sp = sp;
   }

   public int getEnchantEffect() {
      return this._paperdoll[5][2] > 0 ? this._paperdoll[5][2] : this._paperdoll[5][2];
   }

   public void setKarma(int k) {
      this._karma = k;
   }

   public int getKarma() {
      return this._karma;
   }

   public void setAugmentationId(int augmentationId) {
      this._augmentationId = augmentationId;
   }

   public int getAugmentationId() {
      return this._augmentationId;
   }

   public void setPkKills(int pkKills) {
      this._pkKills = pkKills;
   }

   public int getPkKills() {
      return this._pkKills;
   }

   public void setPvPKills(int pvpKills) {
      this._pvpKills = pvpKills;
   }

   public int getPvPKills() {
      return this._pvpKills;
   }

   public int getX() {
      return this._x;
   }

   public int getY() {
      return this._y;
   }

   public int getZ() {
      return this._z;
   }

   public void setX(int x) {
      this._x = x;
   }

   public void setY(int y) {
      this._y = y;
   }

   public void setZ(int z) {
      this._z = z;
   }

   public String getHtmlPrefix() {
      return this._htmlPrefix;
   }

   public void setHtmlPrefix(String s) {
      this._htmlPrefix = s;
   }

   public void setVitalityPoints(int points) {
      this._vitalityPoints = points;
   }

   public int getVitalityPoints() {
      return this._vitalityPoints;
   }
}
