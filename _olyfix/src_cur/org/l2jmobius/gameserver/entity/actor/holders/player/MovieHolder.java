package org.l2jmobius.gameserver.entity.actor.holders.player;

import java.util.Collection;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.network.enums.Movie;

public class MovieHolder {
   private final Movie _movie;
   private final Collection<Player> _players;

   public MovieHolder(Collection<Player> players, Movie movie) {
      this._players = players;
      this._movie = movie;
      this._players.forEach(p -> p.playMovie(this));
   }

   public Movie getMovie() {
      return this._movie;
   }

   public Collection<Player> getPlayers() {
      return this._players;
   }
}
