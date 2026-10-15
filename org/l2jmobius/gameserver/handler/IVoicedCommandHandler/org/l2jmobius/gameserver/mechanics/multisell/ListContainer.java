package org.l2jmobius.gameserver.mechanics.multisell;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ListContainer {
   private final int _listId;
   private boolean _applyTaxes = false;
   private boolean _maintainEnchantment = false;
   private double _useRate = 1.0;
   protected List<Entry> _entries = new ArrayList<>();
   protected Set<Integer> _npcsAllowed = null;

   public ListContainer(int listId) {
      this._listId = listId;
   }

   public List<Entry> getEntries() {
      return this._entries;
   }

   public int getListId() {
      return this._listId;
   }

   public void setApplyTaxes(boolean applyTaxes) {
      this._applyTaxes = applyTaxes;
   }

   public boolean getApplyTaxes() {
      return this._applyTaxes;
   }

   public void setMaintainEnchantment(boolean maintainEnchantment) {
      this._maintainEnchantment = maintainEnchantment;
   }

   public double getUseRate() {
      return this._useRate;
   }

   public void setUseRate(double rate) {
      this._useRate = rate;
   }

   public boolean getMaintainEnchantment() {
      return this._maintainEnchantment;
   }

   public void allowNpc(int npcId) {
      if (this._npcsAllowed == null) {
         this._npcsAllowed = new HashSet<>();
      }

      this._npcsAllowed.add(npcId);
   }

   public boolean isNpcAllowed(int npcId) {
      return this._npcsAllowed != null && this._npcsAllowed.contains(npcId);
   }
}
