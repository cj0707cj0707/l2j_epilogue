package org.l2jmobius.gameserver.entity.actor.tasks.attackable;

import org.l2jmobius.gameserver.entity.actor.Attackable;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.mechanics.script.Quest;

public class OnKillNotifyTask implements Runnable {
   private final Attackable _attackable;
   private final Quest _quest;
   private final Player _killer;
   private final boolean _isSummon;

   public OnKillNotifyTask(Attackable attackable, Quest quest, Player killer, boolean isSummon) {
      this._attackable = attackable;
      this._quest = quest;
      this._killer = killer;
      this._isSummon = isSummon;
   }

   @Override
   public void run() {
      if (this._quest != null && this._attackable != null && this._killer != null) {
         this._quest.onKill(this._attackable, this._killer, this._isSummon);
      }
   }
}
