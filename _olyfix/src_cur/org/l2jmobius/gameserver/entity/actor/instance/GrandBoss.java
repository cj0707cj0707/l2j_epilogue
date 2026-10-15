package org.l2jmobius.gameserver.entity.actor.instance;

import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.enums.creature.InstanceType;
import org.l2jmobius.gameserver.entity.actor.templates.NpcTemplate;
import org.l2jmobius.gameserver.entity.groups.Party;
import org.l2jmobius.gameserver.managers.RaidBossPointsManager;
import org.l2jmobius.gameserver.mechanics.olympiad.Hero;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;

public class GrandBoss extends Monster {
   private boolean _useRaidCurse = true;

   public GrandBoss(NpcTemplate template) {
      super(template);
      this.setInstanceType(InstanceType.GrandBoss);
      this.setIsRaid(true);
      this.setLethalable(false);
   }

   @Override
   public void onSpawn() {
      this.setRandomWalking(false);
      super.onSpawn();
   }

   @Override
   public boolean doDie(Creature killer) {
      if (!super.doDie(killer)) {
         return false;
      } else {
         if (killer != null && killer.isPlayable()) {
            SystemMessage msg = new SystemMessage(SystemMessageId.CONGRATULATIONS_YOUR_RAID_WAS_SUCCESSFUL);
            Player player = killer.asPlayer();
            Party party = player.getParty();
            if (party != null) {
               party.broadcastPacket(msg);

               for (Player member : party.getMembers()) {
                  RaidBossPointsManager.getInstance().addPoints(member, this.getId(), this.getLevel() / 2 + Rnd.get(-5, 5));
                  if (member.isNoble()) {
                     Hero.getInstance().setRBkilled(member.getObjectId(), this.getId());
                  }
               }
            } else {
               player.sendPacket(msg);
               RaidBossPointsManager.getInstance().addPoints(player, this.getId(), this.getLevel() / 2 + Rnd.get(-5, 5));
               if (player.isNoble()) {
                  Hero.getInstance().setRBkilled(player.getObjectId(), this.getId());
               }
            }
         }

         return true;
      }
   }

   @Override
   public float getVitalityPoints(int level, long damage) {
      return -super.getVitalityPoints(level, damage) / 100.0F;
   }

   @Override
   public boolean useVitalityRate() {
      return false;
   }

   public void setUseRaidCurse(boolean value) {
      this._useRaidCurse = value;
   }

   @Override
   public boolean giveRaidCurse() {
      return this._useRaidCurse;
   }
}
