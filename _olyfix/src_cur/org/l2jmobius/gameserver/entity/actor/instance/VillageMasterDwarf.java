package org.l2jmobius.gameserver.entity.actor.instance;

import org.l2jmobius.gameserver.entity.actor.enums.creature.Race;
import org.l2jmobius.gameserver.entity.actor.enums.player.PlayerClass;
import org.l2jmobius.gameserver.entity.actor.templates.NpcTemplate;

public class VillageMasterDwarf extends VillageMaster {
   public VillageMasterDwarf(NpcTemplate template) {
      super(template);
   }

   @Override
   protected boolean checkVillageMasterRace(PlayerClass pClass) {
      return pClass == null ? false : pClass.getRace() == Race.DWARF;
   }
}
