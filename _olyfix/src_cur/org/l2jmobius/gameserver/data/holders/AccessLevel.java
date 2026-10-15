package org.l2jmobius.gameserver.data.holders;

import org.l2jmobius.gameserver.data.xml.AdminData;
import org.l2jmobius.gameserver.util.StatSet;

public class AccessLevel {
   private int _accessLevel = 0;
   private String _name = null;
   AccessLevel _childsAccessLevel = null;
   private int _child = 0;
   private int _nameColor = 0;
   private int _titleColor = 0;
   private boolean _isGm = false;
   private boolean _allowPeaceAttack = false;
   private boolean _allowFixedRes = false;
   private boolean _allowTransaction = false;
   private boolean _allowAltG = false;
   private boolean _giveDamage = false;
   private boolean _takeAggro = false;
   private boolean _gainExp = false;

   public AccessLevel(StatSet set) {
      this._accessLevel = set.getInt("level");
      this._name = set.getString("name");
      this._nameColor = Integer.decode("0x" + set.getString("nameColor", "FFFFFF"));
      this._titleColor = Integer.decode("0x" + set.getString("titleColor", "FFFFFF"));
      this._child = set.getInt("childAccess", 0);
      this._isGm = set.getBoolean("isGM", false);
      this._allowPeaceAttack = set.getBoolean("allowPeaceAttack", false);
      this._allowFixedRes = set.getBoolean("allowFixedRes", false);
      this._allowTransaction = set.getBoolean("allowTransaction", true);
      this._allowAltG = set.getBoolean("allowAltg", false);
      this._giveDamage = set.getBoolean("giveDamage", true);
      this._takeAggro = set.getBoolean("takeAggro", true);
      this._gainExp = set.getBoolean("gainExp", true);
   }

   public AccessLevel() {
      this._accessLevel = 0;
      this._name = "User";
      this._nameColor = Integer.decode("0xFFFFFF");
      this._titleColor = Integer.decode("0xFFFFFF");
      this._child = 0;
      this._isGm = false;
      this._allowPeaceAttack = false;
      this._allowFixedRes = false;
      this._allowTransaction = true;
      this._allowAltG = false;
      this._giveDamage = true;
      this._takeAggro = true;
      this._gainExp = true;
   }

   public int getLevel() {
      return this._accessLevel;
   }

   public String getName() {
      return this._name;
   }

   public int getNameColor() {
      return this._nameColor;
   }

   public int getTitleColor() {
      return this._titleColor;
   }

   public boolean isGm() {
      return this._isGm;
   }

   public boolean allowPeaceAttack() {
      return this._allowPeaceAttack;
   }

   public boolean allowFixedRes() {
      return this._allowFixedRes;
   }

   public boolean allowTransaction() {
      return this._allowTransaction;
   }

   public boolean allowAltG() {
      return this._allowAltG;
   }

   public boolean canGiveDamage() {
      return this._giveDamage;
   }

   public boolean canTakeAggro() {
      return this._takeAggro;
   }

   public boolean canGainExp() {
      return this._gainExp;
   }

   public boolean hasChildAccess(AccessLevel accessLevel) {
      if (this._childsAccessLevel == null) {
         if (this._child <= 0) {
            return false;
         }

         this._childsAccessLevel = AdminData.getInstance().getAccessLevel(this._child);
      }

      return this._childsAccessLevel != null
         && (this._childsAccessLevel.getLevel() == accessLevel.getLevel() || this._childsAccessLevel.hasChildAccess(accessLevel));
   }
}
