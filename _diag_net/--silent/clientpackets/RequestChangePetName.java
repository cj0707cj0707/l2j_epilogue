package org.l2jmobius.gameserver.network.clientpackets;

import org.l2jmobius.gameserver.data.sql.PetNameTable;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.Summon;

public class RequestChangePetName extends ClientPacket {
   private String _name;

   @Override
   protected void readImpl() {
      this._name = this.readString();
   }

   @Override
   protected void runImpl() {
      Player player = this.getPlayer();
      if (player != null) {
         Summon pet = player.getSummon();
         if (pet != null) {
            if (!pet.isPet()) {
               player.sendPacket(org.l2jmobius.gameserver.network.SystemMessageId.YOU_DO_NOT_HAVE_A_PET);
            } else if (pet.getName() != null && !pet.getName().isEmpty()) {
               player.sendPacket(org.l2jmobius.gameserver.network.SystemMessageId.YOU_CANNOT_SET_THE_NAME_OF_THE_PET);
            } else if (PetNameTable.getInstance().doesPetNameExist(this._name, pet.getTemplate().getId())) {
               player.sendPacket(org.l2jmobius.gameserver.network.SystemMessageId.THIS_IS_ALREADY_IN_USE_BY_ANOTHER_PET);
            } else if (this._name.isEmpty() || this._name.length() > 8) {
               player.sendPacket(org.l2jmobius.gameserver.network.SystemMessageId.YOUR_PET_S_NAME_CAN_BE_UP_TO_8_CHARACTERS_IN_LENGTH);
            } else if (this._name.contains(" ")) {
               player.sendPacket(org.l2jmobius.gameserver.network.SystemMessageId.THERE_IS_A_SPACE_IN_THE_NAME);
            } else if (!PetNameTable.getInstance().isValidPetName(this._name)) {
               player.sendPacket(org.l2jmobius.gameserver.network.SystemMessageId.AN_INVALID_CHARACTER_IS_INCLUDED_IN_THE_PET_S_NAME);
            } else {
               pet.setName(this._name);
               pet.storeMe();
               pet.updateAndBroadcastStatus(1);
            }
         }
      }
   }
}
