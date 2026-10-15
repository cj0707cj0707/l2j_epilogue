package org.l2jmobius.gameserver.network.clientpackets;

import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.holders.player.MovieHolder;
import org.l2jmobius.gameserver.network.enums.Movie;

public class EndScenePlayer extends ClientPacket {
   private int _movieId;

   @Override
   protected void readImpl() {
      this._movieId = this.readInt();
   }

   @Override
   protected void runImpl() {
      Player player = this.getPlayer();
      if (player != null && this._movieId != 0) {
         MovieHolder movieHolder = player.getMovieHolder();
         if (movieHolder != null) {
            Movie movie = movieHolder.getMovie();
            if (movie.getClientId() == this._movieId) {
               player.stopMovie();
               player.setTeleporting(true, false);
               player.decayMe();
               player.spawnMe(player.getX(), player.getY(), player.getZ());
               player.setTeleporting(false, false);
            }
         }
      }
   }
}
