package org.l2jmobius.gameserver.entity.actor.instance;

import org.l2jmobius.gameserver.entity.actor.enums.creature.InstanceType;
import org.l2jmobius.gameserver.entity.actor.templates.NpcTemplate;

public class Warehouse extends Folk {
   public Warehouse(NpcTemplate template) {
      super(template);
      this.setInstanceType(InstanceType.Warehouse);
   }

   @Override
   public boolean isWarehouse() {
      return true;
   }

   @Override
   public String getHtmlPath(int npcId, int value) {
      String pom = "";
      if (value == 0) {
         pom = Integer.toString(npcId);
      } else {
         pom = npcId + "-" + value;
      }

      return "data/html/warehouse/" + pom + ".htm";
   }
}
