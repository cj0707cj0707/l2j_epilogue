package org.l2jmobius.gameserver.network.serverpackets;

import org.l2jmobius.commons.network.buffer.WriteBuffer;

public class ExEnchantSkillResult extends ServerPacket {
   private static final ExEnchantSkillResult STATIC_PACKET_TRUE = new ExEnchantSkillResult(true);
   private static final ExEnchantSkillResult STATIC_PACKET_FALSE = new ExEnchantSkillResult(false);
   private final boolean _enchanted;

   public static ExEnchantSkillResult valueOf(boolean result) {
      return result ? STATIC_PACKET_TRUE : STATIC_PACKET_FALSE;
   }

   public ExEnchantSkillResult(boolean enchanted) {
      this._enchanted = enchanted;
   }

   @Override
   public void writeImpl(org.l2jmobius.gameserver.network.GameClient client, WriteBuffer buffer) {
      org.l2jmobius.gameserver.network.ServerPackets.EX_ENCHANT_SKILL_RESULT.writeId(this, buffer);
      buffer.writeInt(this._enchanted);
   }
}
