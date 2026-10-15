package org.l2jmobius.gameserver.network.serverpackets;

import java.util.Map;
import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.mechanics.olympiad.Hero;
import org.l2jmobius.gameserver.util.StatSet;

public class ExHeroList extends ServerPacket {
   private final Map<Integer, StatSet> _heroList = Hero.getInstance().getHeroes();

   @Override
   public void writeImpl(org.l2jmobius.gameserver.network.GameClient client, WriteBuffer buffer) {
      org.l2jmobius.gameserver.network.ServerPackets.EX_HERO_LIST.writeId(this, buffer);
      buffer.writeInt(this._heroList.size());

      for (StatSet hero : this._heroList.values()) {
         buffer.writeString(hero.getString("char_name"));
         buffer.writeInt(hero.getInt("class_id"));
         buffer.writeString(hero.getString("clan_name", ""));
         buffer.writeInt(hero.getInt("clan_crest", 0));
         buffer.writeString(hero.getString("ally_name", ""));
         buffer.writeInt(hero.getInt("ally_crest", 0));
         buffer.writeInt(hero.getInt("count"));
      }
   }
}
