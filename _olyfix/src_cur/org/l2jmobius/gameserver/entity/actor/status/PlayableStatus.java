package org.l2jmobius.gameserver.entity.actor.status;

import org.l2jmobius.gameserver.entity.actor.Playable;

public class PlayableStatus extends CreatureStatus {
   public PlayableStatus(Playable activeChar) {
      super(activeChar);
   }

   public Playable getActiveChar() {
      return super.getActiveChar().asPlayable();
   }
}
