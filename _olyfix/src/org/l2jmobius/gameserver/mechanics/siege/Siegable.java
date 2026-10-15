package org.l2jmobius.gameserver.mechanics.siege;

import java.util.Calendar;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.clan.Clan;

public interface Siegable {
   void startSiege();

   void endSiege();

   SiegeClan getAttackerClan(int var1);

   SiegeClan getAttackerClan(Clan var1);

   Collection<SiegeClan> getAttackerClans();

   List<Player> getAttackersInZone();

   boolean checkIsAttacker(Clan var1);

   SiegeClan getDefenderClan(int var1);

   SiegeClan getDefenderClan(Clan var1);

   Collection<SiegeClan> getDefenderClans();

   boolean checkIsDefender(Clan var1);

   Set<Npc> getFlag(Clan var1);

   Calendar getSiegeDate();

   boolean giveFame();

   int getFameFrequency();

   int getFameAmount();

   void updateSiege();
}
