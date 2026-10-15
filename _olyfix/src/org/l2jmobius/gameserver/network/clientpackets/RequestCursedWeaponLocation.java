package org.l2jmobius.gameserver.network.clientpackets;

import java.util.ArrayList;
import java.util.List;
import org.l2jmobius.gameserver.entity.Location;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.holders.player.CursedWeapon;
import org.l2jmobius.gameserver.managers.CursedWeaponsManager;
import org.l2jmobius.gameserver.network.serverpackets.ExCursedWeaponLocation;

public class RequestCursedWeaponLocation extends ClientPacket {
   @Override
   protected void readImpl() {
   }

   @Override
   protected void runImpl() {
      Player player = this.getPlayer();
      if (player != null) {
         List<ExCursedWeaponLocation.CursedWeaponInfo> list = new ArrayList<>();

         for (CursedWeapon cw : CursedWeaponsManager.getInstance().getCursedWeapons()) {
            if (cw.isActive()) {
               Location pos = cw.getWorldPosition();
               if (pos != null) {
                  list.add(new ExCursedWeaponLocation.CursedWeaponInfo(pos, cw.getItemId(), cw.isActivated() ? 1 : 0));
               }
            }
         }

         if (!list.isEmpty()) {
            player.sendPacket(new ExCursedWeaponLocation(list));
         }
      }
   }
}
