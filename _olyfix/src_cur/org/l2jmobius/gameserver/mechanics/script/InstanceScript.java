package org.l2jmobius.gameserver.mechanics.script;

import java.util.Calendar;
import java.util.List;
import org.l2jmobius.gameserver.config.GeneralConfig;
import org.l2jmobius.gameserver.data.holders.InstanceReenterTimeHolder;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.Summon;
import org.l2jmobius.gameserver.entity.instancezone.Instance;
import org.l2jmobius.gameserver.entity.instancezone.InstanceReenterType;
import org.l2jmobius.gameserver.entity.instancezone.InstanceWorld;
import org.l2jmobius.gameserver.managers.InstanceManager;
import org.l2jmobius.gameserver.mechanics.skill.BuffInfo;
import org.l2jmobius.gameserver.mechanics.skill.enums.SkillFinishType;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;

public abstract class InstanceScript extends Script {
   private static final int MIDNIGHT_HOUR = 12;
   private static final int CALENDAR_DAY_OFFSET = 1;
   private static final long NO_TIME_SET = -1L;

   protected InstanceScript() {
   }

   protected void enterInstance(Player player, int templateId) {
      InstanceWorld existingWorld = InstanceManager.getInstance().getPlayerWorld(player);
      if (existingWorld != null) {
         if (existingWorld.getTemplateId() != templateId) {
            if (existingWorld.getPlayersCount() > 0) {
               player.sendPacket(SystemMessageId.YOU_HAVE_ENTERED_ANOTHER_INSTANCE_ZONE_THEREFORE_YOU_CANNOT_ENTER_CORRESPONDING_DUNGEON);
               return;
            }

            existingWorld.destroy();
         }

         this.onEnterInstance(player, existingWorld, false);
         Instance instance = InstanceManager.getInstance().getInstance(existingWorld.getInstanceId());
         if (instance.isRemoveBuffEnabled()) {
            this.handleRemoveBuffs(player, existingWorld);
         }

         if (!instance.getEnterLocs().isEmpty()) {
            this.teleportPlayer(player, instance.getEnterLocs().stream().findAny().get(), existingWorld.getInstanceId(), false);
         }
      } else if (this.checkConditions(player)) {
         InstanceWorld newWorld = new InstanceWorld();
         newWorld.setInstance(InstanceManager.getInstance().createDynamicInstance(templateId));
         InstanceManager.getInstance().addWorld(newWorld);
         this.onEnterInstance(player, newWorld, true);
         Instance instancex = InstanceManager.getInstance().getInstance(newWorld.getInstanceId());
         if (instancex.getReenterType() == InstanceReenterType.ON_ENTER) {
            this.handleReenterTime(newWorld);
         }

         if (instancex.isRemoveBuffEnabled()) {
            this.handleRemoveBuffs(newWorld);
         }

         if (!instancex.getEnterLocs().isEmpty()) {
            this.teleportPlayer(player, instancex.getEnterLocs().stream().findAny().get(), newWorld.getInstanceId(), false);
         }
      }
   }

   protected void finishInstance(InstanceWorld world) {
      this.finishInstance(world, GeneralConfig.INSTANCE_FINISH_TIME);
   }

   protected void finishInstance(InstanceWorld world, int duration) {
      Instance instance = InstanceManager.getInstance().getInstance(world.getInstanceId());
      if (instance.getReenterType() == InstanceReenterType.ON_FINISH) {
         this.handleReenterTime(world);
      }

      if (duration == 0) {
         InstanceManager.getInstance().destroyInstance(instance.getId());
      } else if (duration > 0) {
         instance.setDuration(duration);
         instance.setEmptyDestroyTime(0L);
      }
   }

   protected void handleReenterTime(InstanceWorld world) {
      Instance instance = InstanceManager.getInstance().getInstance(world.getInstanceId());
      List<InstanceReenterTimeHolder> reenterDataList = instance.getReenterData();
      long earliestReenterTime = -1L;

      for (InstanceReenterTimeHolder reenterData : reenterDataList) {
         if (reenterData.getTime() > 0L) {
            earliestReenterTime = System.currentTimeMillis() + reenterData.getTime();
            break;
         }

         Calendar calendar = Calendar.getInstance();
         calendar.set(9, reenterData.getHour() >= 12 ? 1 : 0);
         calendar.set(11, reenterData.getHour());
         calendar.set(12, reenterData.getMinute());
         calendar.set(13, 0);
         if (calendar.getTimeInMillis() <= System.currentTimeMillis()) {
            calendar.add(5, 1);
         }

         if (reenterData.getDay() != null) {
            while (calendar.get(7) != reenterData.getDay().getValue() + 1) {
               calendar.add(5, 1);
            }
         }

         if (earliestReenterTime == -1L) {
            earliestReenterTime = calendar.getTimeInMillis();
         } else if (calendar.getTimeInMillis() < earliestReenterTime) {
            earliestReenterTime = calendar.getTimeInMillis();
         }
      }

      if (earliestReenterTime > 0L) {
         this.setReenterTime(world, earliestReenterTime);
      }
   }

   protected void handleRemoveBuffs(InstanceWorld world) {
      for (Player player : world.getAllowed()) {
         if (player != null) {
            this.handleRemoveBuffs(player, world);
         }
      }
   }

   protected abstract void onEnterInstance(Player var1, InstanceWorld var2, boolean var3);

   protected boolean checkConditions(Player player) {
      return true;
   }

   protected void setReenterTime(InstanceWorld world, long timeInMilliseconds) {
      for (Player player : world.getAllowed()) {
         if (player != null) {
            InstanceManager.getInstance().setInstanceTime(player.getObjectId(), world.getTemplateId(), timeInMilliseconds);
            if (player.isOnline()) {
               String instanceName = InstanceManager.getInstance().getInstanceIdName(world.getTemplateId());
               player.sendPacket(
                  new SystemMessage(
                        SystemMessageId.INSTANT_ZONE_FROM_HERE_S1_S_ENTRY_HAS_BEEN_RESTRICTED_YOU_CAN_CHECK_THE_NEXT_ENTRY_POSSIBLE_TIME_WITH_THE_COMMAND_INSTANCEZONE
                     )
                     .addString(instanceName)
               );
            }
         }
      }
   }

   private void handleRemoveBuffs(Player player, InstanceWorld world) {
      Instance instance = InstanceManager.getInstance().getInstance(world.getInstanceId());
      switch (instance.getRemoveBuffType()) {
         case ALL:
            player.stopAllEffectsExceptThoseThatLastThroughDeath();
            Summon summon = player.getSummon();
            if (summon != null) {
               summon.stopAllEffectsExceptThoseThatLastThroughDeath();
            }
            break;
         case WHITELIST:
            for (BuffInfo buffInfoxx : player.getEffectList().getBuffs()) {
               if (!instance.getBuffExceptionList().contains(buffInfoxx.getSkill().getId())) {
                  buffInfoxx.getEffected().getEffectList().stopSkillEffects(SkillFinishType.REMOVED, buffInfoxx.getSkill());
               }
            }

            Summon summonx = player.getSummon();
            if (summonx != null) {
               for (BuffInfo buffInfoxxx : summonx.getEffectList().getBuffs()) {
                  if (!instance.getBuffExceptionList().contains(buffInfoxxx.getSkill().getId())) {
                     buffInfoxxx.getEffected().getEffectList().stopSkillEffects(SkillFinishType.REMOVED, buffInfoxxx.getSkill());
                  }
               }
            }
            break;
         case BLACKLIST:
            for (BuffInfo buffInfo : player.getEffectList().getBuffs()) {
               if (instance.getBuffExceptionList().contains(buffInfo.getSkill().getId())) {
                  buffInfo.getEffected().getEffectList().stopSkillEffects(SkillFinishType.REMOVED, buffInfo.getSkill());
               }
            }

            Summon summonx = player.getSummon();
            if (summonx != null) {
               for (BuffInfo buffInfox : summonx.getEffectList().getBuffs()) {
                  if (instance.getBuffExceptionList().contains(buffInfox.getSkill().getId())) {
                     buffInfox.getEffected().getEffectList().stopSkillEffects(SkillFinishType.REMOVED, buffInfox.getSkill());
                  }
               }
            }
      }
   }
}
