package org.l2jmobius.gameserver.entity.actor.holders.npc;

import org.l2jmobius.gameserver.data.xml.FakePlayerData;
import org.l2jmobius.gameserver.entity.actor.enums.player.PlayerClass;
import org.l2jmobius.gameserver.util.StatSet;

public class FakePlayerHolder {
   private final PlayerClass _playerClass;
   private final int _hair;
   private final int _hairColor;
   private final int _face;
   private final int _nameColor;
   private final int _titleColor;
   private final int _equipHead;
   private final int _equipRHand;
   private final int _equipLHand;
   private final int _equipGloves;
   private final int _equipChest;
   private final int _equipLegs;
   private final int _equipFeet;
   private final int _equipCloak;
   private final int _equipHair;
   private final int _equipHair2;
   private final int _agathionId;
   private final int _weaponEnchantLevel;
   private final int _armorEnchantLevel;
   private final boolean _fishing;
   private final int _baitLocationX;
   private final int _baitLocationY;
   private final int _baitLocationZ;
   private final int _recommends;
   private final int _nobleLevel;
   private final boolean _hero;
   private final int _clanId;
   private final int _pledgeStatus;
   private final boolean _isSitting;
   private final int _privateStoreType;
   private final String _privateStoreMessage;
   private final boolean _talkable;

   public FakePlayerHolder(StatSet set) {
      this._playerClass = PlayerClass.getPlayerClass(set.getInt("classId", 1));
      this._hair = set.getInt("hair", 1);
      this._hairColor = set.getInt("hairColor", 1);
      this._face = set.getInt("face", 1);
      this._nameColor = set.getInt("nameColor", 16777215);
      this._titleColor = set.getInt("titleColor", 15530402);
      this._equipHead = set.getInt("equipHead", 0);
      this._equipRHand = set.getInt("equipRHand", 0);
      this._equipLHand = set.getInt("equipLHand", 0);
      this._equipGloves = set.getInt("equipGloves", 0);
      this._equipChest = set.getInt("equipChest", 0);
      this._equipLegs = set.getInt("equipLegs", 0);
      this._equipFeet = set.getInt("equipFeet", 0);
      this._equipCloak = set.getInt("equipCloak", 0);
      this._equipHair = set.getInt("equipHair", 0);
      this._equipHair2 = set.getInt("equipHair2", 0);
      this._agathionId = set.getInt("agathionId", 0);
      this._weaponEnchantLevel = set.getInt("weaponEnchantLevel", 0);
      this._armorEnchantLevel = set.getInt("armorEnchantLevel", 0);
      this._fishing = set.getBoolean("fishing", false);
      this._baitLocationX = set.getInt("baitLocationX", 0);
      this._baitLocationY = set.getInt("baitLocationY", 0);
      this._baitLocationZ = set.getInt("baitLocationZ", 0);
      this._recommends = set.getInt("recommends", 0);
      this._nobleLevel = set.getInt("nobleLevel", 0);
      this._hero = set.getBoolean("hero", false);
      this._clanId = set.getInt("clanId", 0);
      this._pledgeStatus = set.getInt("pledgeStatus", 0);
      this._isSitting = set.getBoolean("sitting", false);
      this._privateStoreType = set.getInt("privateStoreType", 0);
      this._privateStoreMessage = set.getString("privateStoreMessage", "");
      this._talkable = set.getBoolean("fakePlayerTalkable", true);
      String name = set.getString("name", "");
      FakePlayerData.getInstance().addFakePlayerId(name, set.getInt("id", 0));
      String lowercaseName = name.toLowerCase();
      FakePlayerData.getInstance().addFakePlayerName(lowercaseName, name);
      if (this._talkable) {
         FakePlayerData.getInstance().addTalkableFakePlayerName(lowercaseName);
      }
   }

   public PlayerClass getPlayerClass() {
      return this._playerClass;
   }

   public int getHair() {
      return this._hair;
   }

   public int getHairColor() {
      return this._hairColor;
   }

   public int getFace() {
      return this._face;
   }

   public int getNameColor() {
      return this._nameColor;
   }

   public int getTitleColor() {
      return this._titleColor;
   }

   public int getEquipHead() {
      return this._equipHead;
   }

   public int getEquipRHand() {
      return this._equipRHand;
   }

   public int getEquipLHand() {
      return this._equipLHand;
   }

   public int getEquipGloves() {
      return this._equipGloves;
   }

   public int getEquipChest() {
      return this._equipChest;
   }

   public int getEquipLegs() {
      return this._equipLegs;
   }

   public int getEquipFeet() {
      return this._equipFeet;
   }

   public int getEquipCloak() {
      return this._equipCloak;
   }

   public int getEquipHair() {
      return this._equipHair;
   }

   public int getEquipHair2() {
      return this._equipHair2;
   }

   public int getAgathionId() {
      return this._agathionId;
   }

   public int getWeaponEnchantLevel() {
      return this._weaponEnchantLevel;
   }

   public int getArmorEnchantLevel() {
      return this._armorEnchantLevel;
   }

   public boolean isFishing() {
      return this._fishing;
   }

   public int getBaitLocationX() {
      return this._baitLocationX;
   }

   public int getBaitLocationY() {
      return this._baitLocationY;
   }

   public int getBaitLocationZ() {
      return this._baitLocationZ;
   }

   public int getRecommends() {
      return this._recommends;
   }

   public int getNobleLevel() {
      return this._nobleLevel;
   }

   public boolean isHero() {
      return this._hero;
   }

   public int getClanId() {
      return this._clanId;
   }

   public int getPledgeStatus() {
      return this._pledgeStatus;
   }

   public boolean isSitting() {
      return this._isSitting;
   }

   public int getPrivateStoreType() {
      return this._privateStoreType;
   }

   public String getPrivateStoreMessage() {
      return this._privateStoreMessage;
   }

   public boolean isTalkable() {
      return this._talkable;
   }
}
