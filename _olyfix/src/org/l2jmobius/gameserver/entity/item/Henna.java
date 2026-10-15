package org.l2jmobius.gameserver.entity.item;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.l2jmobius.gameserver.entity.actor.enums.player.PlayerClass;
import org.l2jmobius.gameserver.util.StatSet;

public class Henna {
   private final int _dyeId;
   private final int _dyeItemId;
   private final int _str;
   private final int _con;
   private final int _dex;
   private final int _int;
   private final int _men;
   private final int _wit;
   private final int _wearFee;
   private final int _wearCount;
   private final int _cancelFee;
   private final int _cancelCount;
   private final Set<PlayerClass> _wearClass;

   public Henna(StatSet set) {
      this._dyeId = set.getInt("dyeId");
      this._dyeItemId = set.getInt("dyeItemId");
      this._str = set.getInt("str", 0);
      this._con = set.getInt("con", 0);
      this._dex = set.getInt("dex", 0);
      this._int = set.getInt("int", 0);
      this._men = set.getInt("men", 0);
      this._wit = set.getInt("wit", 0);
      this._wearFee = set.getInt("wear_fee");
      this._wearCount = set.getInt("wear_count");
      this._cancelFee = set.getInt("cancel_fee");
      this._cancelCount = set.getInt("cancel_count");
      this._wearClass = new HashSet<>();
   }

   public int getDyeId() {
      return this._dyeId;
   }

   public int getDyeItemId() {
      return this._dyeItemId;
   }

   public int getStatSTR() {
      return this._str;
   }

   public int getStatCON() {
      return this._con;
   }

   public int getStatDEX() {
      return this._dex;
   }

   public int getStatINT() {
      return this._int;
   }

   public int getStatMEN() {
      return this._men;
   }

   public int getStatWIT() {
      return this._wit;
   }

   public int getWearFee() {
      return this._wearFee;
   }

   public int getWearCount() {
      return this._wearCount;
   }

   public int getCancelFee() {
      return this._cancelFee;
   }

   public int getCancelCount() {
      return this._cancelCount;
   }

   public Set<PlayerClass> getAllowedWearClass() {
      return this._wearClass;
   }

   public boolean isAllowedClass(PlayerClass playerClass) {
      return this._wearClass.contains(playerClass);
   }

   public void setWearClassIds(List<PlayerClass> wearClassIds) {
      this._wearClass.addAll(wearClassIds);
   }
}
