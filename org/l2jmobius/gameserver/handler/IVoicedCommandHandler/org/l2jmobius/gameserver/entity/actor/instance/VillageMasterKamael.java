package org.l2jmobius.gameserver.entity.actor.instance;

import org.l2jmobius.gameserver.config.PlayerConfig;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.enums.creature.Race;
import org.l2jmobius.gameserver.entity.actor.enums.player.PlayerClass;
import org.l2jmobius.gameserver.entity.actor.templates.NpcTemplate;
import org.l2jmobius.gameserver.mechanics.script.QuestState;

public class VillageMasterKamael extends VillageMaster {
   public VillageMasterKamael(NpcTemplate template) {
      super(template);
   }

   @Override
   protected String getSubClassMenu(Race race) {
      return !PlayerConfig.ALT_GAME_SUBCLASS_EVERYWHERE && race != Race.KAMAEL
         ? "data/html/villagemaster/SubClass_NoKamael.htm"
         : "data/html/villagemaster/SubClass.htm";
   }

   @Override
   protected String getSubClassFail() {
      return "data/html/villagemaster/SubClass_Fail_Kamael.htm";
   }

   @Override
   protected boolean checkQuests(Player player) {
      if (player.isNoble()) {
         return true;
      } else {
         QuestState qs = player.getQuestState("Q00234_FatesWhisper");
         if (qs != null && qs.isCompleted()) {
            qs = player.getQuestState("Q00236_SeedsOfChaos");
            return qs != null && qs.isCompleted();
         } else {
            return false;
         }
      }
   }

   @Override
   protected boolean checkVillageMasterRace(PlayerClass pClass) {
      return pClass == null ? false : pClass.getRace() == Race.KAMAEL;
   }
}
