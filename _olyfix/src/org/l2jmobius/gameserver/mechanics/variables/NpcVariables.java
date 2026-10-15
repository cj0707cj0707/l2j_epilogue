package org.l2jmobius.gameserver.mechanics.variables;

import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.Summon;
import org.l2jmobius.gameserver.util.StatSet;

public class NpcVariables extends StatSet {
   @Override
   public int getInt(String key) {
      return super.getInt(key, 0);
   }

   public boolean hasVariable(String name) {
      return this.getSet().containsKey(name);
   }

   public Player getPlayer(String name) {
      return this.getObject(name, Player.class);
   }

   public Summon getSummon(String name) {
      return this.getObject(name, Summon.class);
   }
}
