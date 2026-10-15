package org.l2jmobius.gameserver.util;

public class FloodProtectorSettings {
   private final String _floodProtectorType;
   private int _protectionInterval;
   private boolean _logFlooding;
   private int _punishmentLimit;
   private String _punishmentType;
   private long _punishmentTime;

   public FloodProtectorSettings(String floodProtectorType) {
      this._floodProtectorType = floodProtectorType;
   }

   public String getFloodProtectorType() {
      return this._floodProtectorType;
   }

   public int getProtectionInterval() {
      return this._protectionInterval;
   }

   public void setProtectionInterval(int protectionInterval) {
      this._protectionInterval = protectionInterval;
   }

   public void setLogFlooding(boolean logFlooding) {
      this._logFlooding = logFlooding;
   }

   public boolean isLogFlooding() {
      return this._logFlooding;
   }

   public void setPunishmentLimit(int punishmentLimit) {
      this._punishmentLimit = punishmentLimit;
   }

   public int getPunishmentLimit() {
      return this._punishmentLimit;
   }

   public void setPunishmentType(String punishmentType) {
      this._punishmentType = punishmentType;
   }

   public String getPunishmentType() {
      return this._punishmentType;
   }

   public void setPunishmentTime(long punishmentTime) {
      this._punishmentTime = punishmentTime;
   }

   public long getPunishmentTime() {
      return this._punishmentTime;
   }
}
