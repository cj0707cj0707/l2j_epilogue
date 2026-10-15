package org.l2jmobius.gameserver.mechanics.events.holders.olympiad;

import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.mechanics.events.EventType;
import org.l2jmobius.gameserver.mechanics.events.holders.IBaseEvent;
import org.l2jmobius.gameserver.mechanics.olympiad.CompetitionType;

public class OnOlympiadMatchResult implements IBaseEvent {
   private final Player _winner;
   private final Player _loser;
   private final CompetitionType _type;

   public OnOlympiadMatchResult(Player winner, Player looser, CompetitionType type) {
      this._winner = winner;
      this._loser = looser;
      this._type = type;
   }

   public Player getWinner() {
      return this._winner;
   }

   public Player getLoser() {
      return this._loser;
   }

   public CompetitionType getCompetitionType() {
      return this._type;
   }

   @Override
   public EventType getType() {
      return EventType.ON_OLYMPIAD_MATCH_RESULT;
   }
}
