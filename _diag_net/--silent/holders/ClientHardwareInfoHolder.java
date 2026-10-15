package org.l2jmobius.gameserver.network.holders;

import org.l2jmobius.gameserver.entity.actor.Player;

public class ClientHardwareInfoHolder {
   private final String _macAddress;
   private final int _windowsPlatformId;
   private final int _windowsMajorVersion;
   private final int _windowsMinorVersion;
   private final int _windowsBuildNumber;
   private final int _directxVersion;
   private final int _directxRevision;
   private final String _cpuName;
   private final int _cpuSpeed;
   private final int _cpuCoreCount;
   private final int _vgaCount;
   private final int _vgaPcxSpeed;
   private final int _physMemorySlot1;
   private final int _physMemorySlot2;
   private final int _physMemorySlot3;
   private final int _videoMemory;
   private final int _vgaVersion;
   private final String _vgaName;
   private final String _vgaDriverVersion;

   public ClientHardwareInfoHolder(
      String macAddress,
      int windowsPlatformId,
      int windowsMajorVersion,
      int windowsMinorVersion,
      int windowsBuildNumber,
      int directxVersion,
      int directxRevision,
      String cpuName,
      int cpuSpeed,
      int cpuCoreCount,
      int vgaCount,
      int vgaPcxSpeed,
      int physMemorySlot1,
      int physMemorySlot2,
      int physMemorySlot3,
      int videoMemory,
      int vgaVersion,
      String vgaName,
      String vgaDriverVersion
   ) {
      this._macAddress = macAddress;
      this._windowsPlatformId = windowsPlatformId;
      this._windowsMajorVersion = windowsMajorVersion;
      this._windowsMinorVersion = windowsMinorVersion;
      this._windowsBuildNumber = windowsBuildNumber;
      this._directxVersion = directxVersion;
      this._directxRevision = directxRevision;
      this._cpuName = cpuName;
      this._cpuSpeed = cpuSpeed;
      this._cpuCoreCount = cpuCoreCount;
      this._vgaCount = vgaCount;
      this._vgaPcxSpeed = vgaPcxSpeed;
      this._physMemorySlot1 = physMemorySlot1;
      this._physMemorySlot2 = physMemorySlot2;
      this._physMemorySlot3 = physMemorySlot3;
      this._videoMemory = videoMemory;
      this._vgaVersion = vgaVersion;
      this._vgaName = vgaName;
      this._vgaDriverVersion = vgaDriverVersion;
   }

   public ClientHardwareInfoHolder(String info) {
      String[] split = info.split("\t");
      this._macAddress = split[0];
      this._windowsPlatformId = Integer.valueOf(split[1]);
      this._windowsMajorVersion = Integer.valueOf(split[2]);
      this._windowsMinorVersion = Integer.valueOf(split[3]);
      this._windowsBuildNumber = Integer.valueOf(split[4]);
      this._directxVersion = Integer.valueOf(split[5]);
      this._directxRevision = Integer.valueOf(split[6]);
      this._cpuName = split[7];
      this._cpuSpeed = Integer.valueOf(split[8]);
      this._cpuCoreCount = Integer.valueOf(split[9]);
      this._vgaCount = Integer.valueOf(split[10]);
      this._vgaPcxSpeed = Integer.valueOf(split[11]);
      this._physMemorySlot1 = Integer.valueOf(split[12]);
      this._physMemorySlot2 = Integer.valueOf(split[13]);
      this._physMemorySlot3 = Integer.valueOf(split[14]);
      this._videoMemory = Integer.valueOf(split[15]);
      this._vgaVersion = Integer.valueOf(split[16]);
      this._vgaName = split[17];
      this._vgaDriverVersion = split[18];
   }

   public void store(Player player) {
      StringBuilder sb = new StringBuilder();
      sb.append(this._macAddress);
      sb.append("\t");
      sb.append(this._windowsPlatformId);
      sb.append("\t");
      sb.append(this._windowsMajorVersion);
      sb.append("\t");
      sb.append(this._windowsMinorVersion);
      sb.append("\t");
      sb.append(this._windowsBuildNumber);
      sb.append("\t");
      sb.append(this._directxVersion);
      sb.append("\t");
      sb.append(this._directxRevision);
      sb.append("\t");
      sb.append(this._cpuName);
      sb.append("\t");
      sb.append(this._cpuSpeed);
      sb.append("\t");
      sb.append(this._cpuCoreCount);
      sb.append("\t");
      sb.append(this._vgaCount);
      sb.append("\t");
      sb.append(this._vgaPcxSpeed);
      sb.append("\t");
      sb.append(this._physMemorySlot1);
      sb.append("\t");
      sb.append(this._physMemorySlot2);
      sb.append("\t");
      sb.append(this._physMemorySlot3);
      sb.append("\t");
      sb.append(this._videoMemory);
      sb.append("\t");
      sb.append(this._vgaVersion);
      sb.append("\t");
      sb.append(this._vgaName);
      sb.append("\t");
      sb.append(this._vgaDriverVersion);
      player.getAccountVariables().set("HWID", sb.toString());
   }

   public String getMacAddress() {
      return this._macAddress;
   }

   public int getWindowsPlatformId() {
      return this._windowsPlatformId;
   }

   public int getWindowsMajorVersion() {
      return this._windowsMajorVersion;
   }

   public int getWindowsMinorVersion() {
      return this._windowsMinorVersion;
   }

   public int getWindowsBuildNumber() {
      return this._windowsBuildNumber;
   }

   public int getDirectxVersion() {
      return this._directxVersion;
   }

   public int getDirectxRevision() {
      return this._directxRevision;
   }

   public String getCpuName() {
      return this._cpuName;
   }

   public int getCpuSpeed() {
      return this._cpuSpeed;
   }

   public int getCpuCoreCount() {
      return this._cpuCoreCount;
   }

   public int getVgaCount() {
      return this._vgaCount;
   }

   public int getVgaPcxSpeed() {
      return this._vgaPcxSpeed;
   }

   public int getPhysMemorySlot1() {
      return this._physMemorySlot1;
   }

   public int getPhysMemorySlot2() {
      return this._physMemorySlot2;
   }

   public int getPhysMemorySlot3() {
      return this._physMemorySlot3;
   }

   public int getVideoMemory() {
      return this._videoMemory;
   }

   public int getVgaVersion() {
      return this._vgaVersion;
   }

   public String getVgaName() {
      return this._vgaName;
   }

   public String getVgaDriverVersion() {
      return this._vgaDriverVersion;
   }

   @Override
   public boolean equals(Object obj) {
      return obj instanceof ClientHardwareInfoHolder && this._macAddress.equals(((ClientHardwareInfoHolder)obj).getMacAddress());
   }
}
