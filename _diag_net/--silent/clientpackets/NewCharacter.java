package org.l2jmobius.gameserver.network.clientpackets;

import org.l2jmobius.gameserver.data.xml.PlayerTemplateData;
import org.l2jmobius.gameserver.entity.actor.enums.player.PlayerClass;
import org.l2jmobius.gameserver.network.serverpackets.NewCharacterSuccess;

public class NewCharacter extends ClientPacket {
   @Override
   protected void readImpl() {
   }

   @Override
   protected void runImpl() {
      NewCharacterSuccess ct = new NewCharacterSuccess();
      ct.addChar(PlayerTemplateData.getInstance().getTemplate(PlayerClass.FIGHTER));
      ct.addChar(PlayerTemplateData.getInstance().getTemplate(PlayerClass.MAGE));
      ct.addChar(PlayerTemplateData.getInstance().getTemplate(PlayerClass.ELVEN_FIGHTER));
      ct.addChar(PlayerTemplateData.getInstance().getTemplate(PlayerClass.ELVEN_MAGE));
      ct.addChar(PlayerTemplateData.getInstance().getTemplate(PlayerClass.DARK_FIGHTER));
      ct.addChar(PlayerTemplateData.getInstance().getTemplate(PlayerClass.DARK_MAGE));
      ct.addChar(PlayerTemplateData.getInstance().getTemplate(PlayerClass.ORC_FIGHTER));
      ct.addChar(PlayerTemplateData.getInstance().getTemplate(PlayerClass.ORC_MAGE));
      ct.addChar(PlayerTemplateData.getInstance().getTemplate(PlayerClass.DWARVEN_FIGHTER));
      ct.addChar(PlayerTemplateData.getInstance().getTemplate(PlayerClass.MALE_SOLDIER));
      ct.addChar(PlayerTemplateData.getInstance().getTemplate(PlayerClass.FEMALE_SOLDIER));
      ((org.l2jmobius.gameserver.network.GameClient)this.getClient()).sendPacket(ct);
   }
}
