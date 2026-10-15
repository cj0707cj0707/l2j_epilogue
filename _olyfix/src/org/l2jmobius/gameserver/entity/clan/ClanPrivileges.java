package org.l2jmobius.gameserver.entity.clan;

public class ClanPrivileges {
   private int _mask;

   public ClanPrivileges() {
      this._mask = 0;
   }

   public ClanPrivileges(int mask) {
      this._mask = mask;
   }

   public int getMask() {
      return this._mask;
   }

   public void setMask(int mask) {
      this._mask = mask;
   }

   public void disableAll() {
      this._mask = 0;
   }

   public void enableAll() {
      this._mask = getCompleteMask();
   }

   public static int getCompleteMask() {
      int mask = 0;

      for (ClanAccess access : ClanAccess.values()) {
         mask |= 1 << access.ordinal();
      }

      return mask;
   }

   public void setPrivileges(ClanAccess... privileges) {
      this.disableAll();

      for (ClanAccess access : privileges) {
         this._mask = this._mask | 1 << access.ordinal();
      }
   }

   public void setMinimumPrivileges(ClanAccess minimum, ClanAccess... privileges) {
      this.disableAll();
      this.addMinimumPrivileges(minimum, privileges);
   }

   public void addMinimumPrivileges(ClanAccess minimum, ClanAccess... privileges) {
      this._mask = this._mask | 1 << minimum.ordinal();

      for (ClanAccess access : privileges) {
         this._mask = this._mask | 1 << access.ordinal();
      }
   }

   public void removeMinimumPrivileges(ClanAccess minimum, ClanAccess... privileges) {
      this._mask = this._mask & ~(1 << minimum.ordinal());

      for (ClanAccess access : privileges) {
         this._mask = this._mask & ~(1 << access.ordinal());
      }
   }

   public boolean hasMinimumPrivileges(ClanAccess minimum, ClanAccess... privileges) {
      if ((this._mask & 1 << minimum.ordinal()) == 0) {
         return false;
      } else {
         for (ClanAccess access : privileges) {
            if ((this._mask & 1 << access.ordinal()) == 0) {
               return false;
            }
         }

         return true;
      }
   }

   public ClanPrivileges clone() {
      return new ClanPrivileges(this._mask);
   }
}
