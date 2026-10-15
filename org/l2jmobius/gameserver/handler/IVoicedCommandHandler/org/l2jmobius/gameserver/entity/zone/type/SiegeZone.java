package org.l2jmobius.gameserver.entity.zone.type;

import java.util.Objects;
import org.l2jmobius.gameserver.config.FeatureConfig;
import org.l2jmobius.gameserver.data.xml.SkillData;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.Summon;
import org.l2jmobius.gameserver.entity.actor.enums.player.MountType;
import org.l2jmobius.gameserver.entity.actor.enums.player.TeleportWhereType;
import org.l2jmobius.gameserver.entity.actor.transform.Transform;
import org.l2jmobius.gameserver.entity.item.enums.BodyPart;
import org.l2jmobius.gameserver.entity.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.entity.zone.AbstractZoneSettings;
import org.l2jmobius.gameserver.entity.zone.ZoneId;
import org.l2jmobius.gameserver.entity.zone.ZoneType;
import org.l2jmobius.gameserver.managers.CHSiegeManager;
import org.l2jmobius.gameserver.managers.CastleManager;
import org.l2jmobius.gameserver.managers.FortManager;
import org.l2jmobius.gameserver.managers.FortSiegeManager;
import org.l2jmobius.gameserver.managers.ZoneManager;
import org.l2jmobius.gameserver.mechanics.siege.Castle;
import org.l2jmobius.gameserver.mechanics.siege.Fort;
import org.l2jmobius.gameserver.mechanics.siege.FortSiege;
import org.l2jmobius.gameserver.mechanics.siege.Siegable;
import org.l2jmobius.gameserver.mechanics.siege.clanhalls.SiegableHall;
import org.l2jmobius.gameserver.mechanics.skill.BuffInfo;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.network.SystemMessageId;

public class SiegeZone extends ZoneType {
   private static final int DISMOUNT_DELAY = 5;

   public SiegeZone(int id) {
      super(id);
      AbstractZoneSettings settings = ZoneManager.getSettings(this.getName());
      if (settings == null) {
         settings = new SiegeZone.Settings();
      }

      this.setSettings(settings);
   }

   public SiegeZone.Settings getSettings() {
      return (SiegeZone.Settings)super.getSettings();
   }

   @Override
   public void setParameter(String name, String value) {
      if (name.equals("castleId")) {
         if (this.getSettings().getSiegeableId() != -1) {
            throw new IllegalArgumentException("Siege object already defined!");
         }

         this.getSettings().setSiegeableId(Integer.parseInt(value));
      } else if (name.equals("fortId")) {
         if (this.getSettings().getSiegeableId() != -1) {
            throw new IllegalArgumentException("Siege object already defined!");
         }

         this.getSettings().setSiegeableId(Integer.parseInt(value));
      } else if (name.equals("clanHallId")) {
         if (this.getSettings().getSiegeableId() != -1) {
            throw new IllegalArgumentException("Siege object already defined!");
         }

         this.getSettings().setSiegeableId(Integer.parseInt(value));
         SiegableHall hall = CHSiegeManager.getInstance().getConquerableHalls().get(this.getSettings().getSiegeableId());
         if (hall != null) {
            hall.setSiegeZone(this);
         } else {
            LOGGER.warning("SiegeZone: Siegable clan hall with id " + value + " does not exist!");
         }
      } else {
         super.setParameter(name, value);
      }
   }

   @Override
   protected void onEnter(Creature creature) {
      if (this.getSettings().isActiveSiege()) {
         creature.setInsideZone(ZoneId.PVP, true);
         creature.setInsideZone(ZoneId.SIEGE, true);
         creature.setInsideZone(ZoneId.NO_SUMMON_FRIEND, true);
         if (creature.isPlayer()) {
            Player player = creature.asPlayer();
            if (player.isRegisteredOnThisSiegeField(this.getSettings().getSiegeableId())) {
               player.setInSiege(true);
               if (this.getSettings().getSiege().giveFame() && this.getSettings().getSiege().getFameFrequency() > 0) {
                  player.startFameTask(this.getSettings().getSiege().getFameFrequency() * 1000, this.getSettings().getSiege().getFameAmount());
               }
            }

            creature.sendPacket(SystemMessageId.YOU_HAVE_ENTERED_A_COMBAT_ZONE);
            if (!FeatureConfig.ALLOW_MOUNTS_DURING_SIEGE) {
               if (player.isGM()) {
                  player.sendMessage("You have entered a siege zone. GM dismount restrictions are ignored.");
               } else {
                  Castle castle = CastleManager.getInstance().getCastleById(this.getSettings().getSiegeableId());
                  boolean isCastleLord = castle != null && player.isClanLeader() && player.getClanId() == castle.getOwnerId();
                  if (player.getMountType() == MountType.WYVERN) {
                     if (!isCastleLord) {
                        player.sendPacket(
                           SystemMessageId.THIS_AREA_CANNOT_BE_ENTERED_WHILE_MOUNTED_ATOP_OF_A_WYVERN_YOU_WILL_BE_DISMOUNTED_FROM_YOUR_WYVERN_IF_YOU_DO_NOT_LEAVE
                        );
                        player.enteredNoLanding(5);
                     }
                  } else if (player.isMounted() && player.getMountType() != MountType.WYVERN) {
                     boolean isCastleLordOnStrider = player.getMountType() == MountType.STRIDER && isCastleLord;
                     if (!isCastleLordOnStrider) {
                        player.dismount();
                     }
                  }

                  Transform transform = player.getTransformation();
                  if (transform != null && transform.isRiding()) {
                     player.untransform();
                  }
               }
            }
         }
      }
   }

   @Override
   protected void onExit(Creature creature) {
      creature.setInsideZone(ZoneId.PVP, false);
      creature.setInsideZone(ZoneId.SIEGE, false);
      creature.setInsideZone(ZoneId.NO_SUMMON_FRIEND, false);
      if (this.getSettings().isActiveSiege() && creature.isPlayer()) {
         Player player = creature.asPlayer();
         creature.sendPacket(SystemMessageId.YOU_HAVE_LEFT_A_COMBAT_ZONE);
         if (player.getMountType() == MountType.WYVERN) {
            player.exitedNoLanding();
         }

         if (player.getPvpFlag() == 0) {
            player.startPvPFlag();
         }
      }

      if (creature.isPlayer()) {
         Player playerx = creature.asPlayer();
         playerx.stopFameTask();
         playerx.setInSiege(false);
         if (this.getSettings().getSiege() instanceof FortSiege && playerx.getInventory().getItemByItemId(9819) != null) {
            Fort fort = FortManager.getInstance().getFortById(this.getSettings().getSiegeableId());
            if (fort != null) {
               FortSiegeManager.getInstance().dropCombatFlag(playerx, fort.getResidenceId());
            } else {
               BodyPart bodyPart = BodyPart.fromItem(playerx.getInventory().getItemByItemId(9819));
               playerx.getInventory().unEquipItemInBodySlot(bodyPart);
               playerx.destroyItem(ItemProcessType.DESTROY, playerx.getInventory().getItemByItemId(9819), null, true);
            }

            Summon summon = playerx.getSummon();
            if (summon != null) {
               summon.abortAttack();
               summon.abortCast();
               summon.stopAllEffects();
               summon.unSummon(playerx);
            }
         }
      }
   }

   @Override
   public void onDieInside(Creature creature) {
      if (this.getSettings().isActiveSiege() && creature.isPlayer() && creature.asPlayer().isRegisteredOnThisSiegeField(this.getSettings().getSiegeableId())) {
         int level = 1;
         BuffInfo info = creature.getEffectList().getBuffInfoBySkillId(5660);
         if (info != null) {
            level = Math.min(level + info.getSkill().getLevel(), 5);
         }

         Skill skill = SkillData.getInstance().getSkill(5660, level);
         if (skill != null) {
            skill.applyEffects(creature, creature);
         }
      }
   }

   @Override
   public void onPlayerLogoutInside(Player player) {
      if (player.getClanId() != this.getSettings().getSiegeableId()) {
         player.teleToLocation(TeleportWhereType.TOWN);
      }
   }

   public void updateZoneStatusForCharactersInside() {
      if (this.getSettings().isActiveSiege()) {
         for (Creature creature : this.getCharactersInside()) {
            if (creature != null) {
               this.onEnter(creature);
            }
         }
      } else {
         for (Creature creaturex : this.getCharactersInside()) {
            if (creaturex != null) {
               creaturex.setInsideZone(ZoneId.PVP, false);
               creaturex.setInsideZone(ZoneId.SIEGE, false);
               creaturex.setInsideZone(ZoneId.NO_SUMMON_FRIEND, false);
               if (creaturex.isPlayer()) {
                  Player player = creaturex.asPlayer();
                  creaturex.sendPacket(SystemMessageId.YOU_HAVE_LEFT_A_COMBAT_ZONE);
                  player.stopFameTask();
                  if (player.getMountType() == MountType.WYVERN) {
                     player.exitedNoLanding();
                  }
               }
            }
         }
      }
   }

   public void announceToPlayers(String message) {
      for (Player player : this.getPlayersInside()) {
         if (player != null) {
            player.sendMessage(message);
         }
      }
   }

   public int getSiegeObjectId() {
      return this.getSettings().getSiegeableId();
   }

   public boolean isActive() {
      return this.getSettings().isActiveSiege();
   }

   public void setActive(boolean value) {
      this.getSettings().setActiveSiege(value);
   }

   public void setSiegeInstance(Siegable siege) {
      this.getSettings().setSiege(siege);
   }

   public void banishForeigners(int owningClanId) {
      for (Player temp : this.getPlayersInside()) {
         if (temp.getClanId() != owningClanId) {
            temp.teleToLocation(TeleportWhereType.TOWN);
         }
      }
   }

   public class Settings extends AbstractZoneSettings {
      private int _siegableId;
      private Siegable _siege;
      private boolean _isActiveSiege;

      protected Settings() {
         Objects.requireNonNull(SiegeZone.this);
         super();
         this._siegableId = -1;
         this._siege = null;
         this._isActiveSiege = false;
      }

      public int getSiegeableId() {
         return this._siegableId;
      }

      protected void setSiegeableId(int id) {
         this._siegableId = id;
      }

      public Siegable getSiege() {
         return this._siege;
      }

      public void setSiege(Siegable s) {
         this._siege = s;
      }

      public boolean isActiveSiege() {
         return this._isActiveSiege;
      }

      public void setActiveSiege(boolean value) {
         this._isActiveSiege = value;
      }

      @Override
      public void clear() {
         this._siegableId = -1;
         this._siege = null;
         this._isActiveSiege = false;
      }
   }
}
