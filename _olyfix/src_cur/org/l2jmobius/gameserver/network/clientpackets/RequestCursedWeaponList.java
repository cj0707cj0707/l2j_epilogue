package org.l2jmobius.gameserver.network.clientpackets;

import java.util.ArrayList;
import java.util.List;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.managers.CursedWeaponsManager;
import org.l2jmobius.gameserver.network.serverpackets.ExCursedWeaponList;

public class RequestCursedWeaponList extends ClientPacket {
   @Override
   protected void readImpl() {
   }

   @Override
   protected void runImpl() {
      Player player = this.getPlayer();
      if (player != null) {
         List<Integer> list = new ArrayList<>();

         for (int id : CursedWeaponsManager.getInstance().getCursedWeaponsIds()) {
            list.add(id);
         }

         player.sendPacket(new ExCursedWeaponList(list));
      }
   }
}
