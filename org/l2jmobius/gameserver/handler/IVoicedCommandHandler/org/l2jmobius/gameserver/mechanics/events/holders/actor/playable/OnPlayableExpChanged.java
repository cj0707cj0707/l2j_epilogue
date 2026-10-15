package org.l2jmobius.gameserver.mechanics.events.holders.actor.playable;

import org.l2jmobius.gameserver.entity.actor.Playable;
import org.l2jmobius.gameserver.mechanics.events.EventType;
import org.l2jmobius.gameserver.mechanics.events.holders.IBaseEvent;

public class OnPlayableExpChanged implements IBaseEvent {
   private final Playable _activeChar;
   private final long _oldExp;
   private final long _newExp;

   public OnPlayableExpChanged(Playable activeChar, long oldExp, long newExp) {
      this._activeChar = activeChar;
      this._oldExp = oldExp;
      this._newExp = newExp;
   }

   public Playable getActiveChar() {
      return this._activeChar;
   }

   public long getOldExp() {
      return this._oldExp;
   }

   public long getNewExp() {
      return this._newExp;
   }

   @Override
   public EventType getType() {
      return EventType.ON_PLAYABLE_EXP_CHANGED;
   }
}
