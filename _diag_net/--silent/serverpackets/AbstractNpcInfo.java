package org.l2jmobius.gameserver.network.serverpackets;

import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.config.NpcConfig;
import org.l2jmobius.gameserver.config.custom.ChampionMonstersConfig;
import org.l2jmobius.gameserver.config.custom.MultilingualSupportConfig;
import org.l2jmobius.gameserver.data.sql.ClanTable;
import org.l2jmobius.gameserver.data.xml.NpcNameLocalisationData;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.Summon;
import org.l2jmobius.gameserver.entity.actor.instance.Monster;
import org.l2jmobius.gameserver.entity.actor.instance.Trap;
import org.l2jmobius.gameserver.entity.clan.Clan;
import org.l2jmobius.gameserver.entity.zone.ZoneId;
import org.l2jmobius.gameserver.entity.zone.type.TownZone;
import org.l2jmobius.gameserver.managers.TownManager;
import org.l2jmobius.gameserver.mechanics.skill.AbnormalVisualEffect;

public abstract class AbstractNpcInfo extends ServerPacket {
   protected int _x;
   protected int _y;
   protected int _z;
   protected int _heading;
   protected int _displayId;
   protected boolean _isAttackable;
   protected boolean _isSummoned;
   protected int _mAtkSpd;
   protected int _pAtkSpd;
   protected final int _runSpd;
   protected final int _walkSpd;
   protected final int _swimRunSpd;
   protected final int _swimWalkSpd;
   protected final int _flyRunSpd;
   protected final int _flyWalkSpd;
   protected double _moveMultiplier;
   protected int _rhand;
   protected int _lhand;
   protected int _chest;
   protected int _enchantEffect;
   protected float _collisionHeight;
   protected float _collisionRadius;
   protected String _name = "";
   protected String _title = "";
   protected final boolean _gmSeeInvis;

   public AbstractNpcInfo(Creature creature, boolean gmSeeInvis) {
      this._isSummoned = creature.isShowSummonAnimation();
      this._x = creature.getX();
      this._y = creature.getY();
      this._z = creature.getZ();
      this._heading = creature.getHeading();
      this._mAtkSpd = creature.getMAtkSpd();
      this._pAtkSpd = (int)creature.getPAtkSpd();
      this._moveMultiplier = creature.getMovementSpeedMultiplier();
      this._runSpd = (int)Math.round(creature.getRunSpeed() / this._moveMultiplier);
      this._walkSpd = (int)Math.round(creature.getWalkSpeed() / this._moveMultiplier);
      this._swimRunSpd = (int)Math.round(creature.getSwimRunSpeed() / this._moveMultiplier);
      this._swimWalkSpd = (int)Math.round(creature.getSwimWalkSpeed() / this._moveMultiplier);
      this._flyRunSpd = creature.isFlying() ? this._runSpd : 0;
      this._flyWalkSpd = creature.isFlying() ? this._walkSpd : 0;
      this._gmSeeInvis = gmSeeInvis;
   }

   public static class NpcInfo extends AbstractNpcInfo {
      private final Npc _npc;
      private int _clanCrest = 0;
      private int _allyCrest = 0;
      private int _allyId = 0;
      private int _clanId = 0;
      private int _displayEffect = 0;

      public NpcInfo(Npc cha, Creature attacker) {
         super(cha, attacker.isGM());
         this._npc = cha;
         this._displayId = cha.getTemplate().getDisplayId();
         this._rhand = cha.getRightHandItem();
         this._lhand = cha.getLeftHandItem();
         this._enchantEffect = cha.getEnchantEffect();
         this._collisionHeight = cha.getTemplate().getFCollisionHeight();
         this._collisionRadius = cha.getTemplate().getFCollisionRadius();
         this._isAttackable = cha.isAutoAttackable(attacker);
         if (cha.isNpc()
            && cha.isTargetable()
            && cha.isShowName()
            && !cha.isMonster()
            && cha.isInsideZone(ZoneId.TOWN)
            && (NpcConfig.SHOW_CREST_WITHOUT_QUEST || cha.getCastle().getShowNpcCrest())
            && cha.getCastle().getOwnerId() != 0) {
            TownZone town = TownManager.getTown(this._x, this._y, this._z);
            if (town != null) {
               int townId = town.getTownId();
               if (townId != 33 && townId != 22) {
                  Clan clan = ClanTable.getInstance().getClan(cha.getCastle().getOwnerId());
                  this._clanCrest = clan.getCrestId();
                  this._clanId = clan.getId();
                  this._allyCrest = clan.getAllyCrestId();
                  this._allyId = clan.getAllyId();
               }
            }
         }

         this._displayEffect = cha.getDisplayEffect();
      }

      @Override
      public void writeImpl(org.l2jmobius.gameserver.network.GameClient client, WriteBuffer buffer) {
         if (!this._npc.isDecayed()) {
            String[] localisation = null;
            if (MultilingualSupportConfig.MULTILANG_ENABLE) {
               Player player = client.getPlayer();
               if (player != null) {
                  String lang = player.getLang();
                  if (lang != null && !lang.equals("en")) {
                     localisation = NpcNameLocalisationData.getInstance().getLocalisation(lang, this._npc.getId());
                     if (localisation != null) {
                        this._name = localisation[0];
                        this._title = localisation[1];
                     }
                  }
               }
            }

            org.l2jmobius.gameserver.network.ServerPackets.NPC_INFO.writeId(this, buffer);
            buffer.writeInt(this._npc.getObjectId());
            buffer.writeInt(this._displayId + 1000000);
            buffer.writeInt(this._isAttackable);
            buffer.writeInt(this._x);
            buffer.writeInt(this._y);
            buffer.writeInt(this._z);
            buffer.writeInt(this._heading);
            buffer.writeInt(0);
            buffer.writeInt(this._mAtkSpd);
            buffer.writeInt(this._pAtkSpd);
            buffer.writeInt(this._runSpd);
            buffer.writeInt(this._walkSpd);
            buffer.writeInt(this._swimRunSpd);
            buffer.writeInt(this._swimWalkSpd);
            buffer.writeInt(this._flyRunSpd);
            buffer.writeInt(this._flyWalkSpd);
            buffer.writeInt(this._flyRunSpd);
            buffer.writeInt(this._flyWalkSpd);
            buffer.writeDouble(this._moveMultiplier);
            buffer.writeDouble(this._npc.getAttackSpeedMultiplier());
            buffer.writeDouble(this._collisionRadius);
            buffer.writeDouble(this._collisionHeight);
            buffer.writeInt(this._rhand);
            buffer.writeInt(this._chest);
            buffer.writeInt(this._lhand);
            buffer.writeByte(1);
            buffer.writeByte(this._npc.isRunning());
            buffer.writeByte(this._npc.isInCombat());
            buffer.writeByte(this._npc.isAlikeDead());
            buffer.writeByte(this._isSummoned ? 2 : 0);
            if (localisation == null && this._npc.getTemplate().isUsingServerSideName()) {
               this._name = this._npc.getName();
            }

            buffer.writeString(this._name);
            if (this._npc.isInvisible()) {
               this._title = "Invisible";
            } else if (localisation == null) {
               if (this._npc.getTemplate().isUsingServerSideTitle()) {
                  this._title = this._npc.getTemplate().getTitle();
               } else {
                  this._title = this._npc.getTitle();
               }
            }

            if (this._npc.isMonster() && (NpcConfig.SHOW_NPC_LEVEL || NpcConfig.SHOW_NPC_AGGRESSION)) {
               String t1 = "";
               if (NpcConfig.SHOW_NPC_LEVEL) {
                  t1 = t1 + "Lv " + this._npc.getLevel();
               }

               String t2 = "";
               if (NpcConfig.SHOW_NPC_AGGRESSION) {
                  if (!t1.isEmpty()) {
                     t2 = t2 + " ";
                  }

                  Monster monster = this._npc.asMonster();
                  if (monster.isAggressive()) {
                     t2 = t2 + "[A]";
                  }

                  if (monster.getTemplate().getClans() != null && monster.getTemplate().getClanHelpRange() > 0) {
                     t2 = t2 + "[G]";
                  }
               }

               t1 = t1 + t2;
               if (this._title != null && !this._title.isEmpty()) {
                  t1 = t1 + " " + this._title;
               }

               this._title = this._npc.isChampion() ? ChampionMonstersConfig.CHAMP_TITLE + " " + t1 : t1;
            } else if (ChampionMonstersConfig.CHAMPION_ENABLE && this._npc.isChampion()) {
               this._title = ChampionMonstersConfig.CHAMP_TITLE;
            }

            buffer.writeString(this._title);
            buffer.writeInt(0);
            buffer.writeInt(0);
            buffer.writeInt(0);
            buffer.writeInt(
               this._npc.isInvisible() ? this._npc.getAbnormalVisualEffects() | AbnormalVisualEffect.STEALTH.getMask() : this._npc.getAbnormalVisualEffects()
            );
            buffer.writeInt(this._clanId);
            buffer.writeInt(this._clanCrest);
            buffer.writeInt(this._allyId);
            buffer.writeInt(this._allyCrest);
            buffer.writeByte(this._npc.isInsideZone(ZoneId.WATER) ? 1 : (this._npc.isFlying() ? 2 : 0));
            buffer.writeByte(this._npc.getTeam().getId());
            buffer.writeDouble(this._collisionRadius);
            buffer.writeDouble(this._collisionHeight);
            buffer.writeInt(this._enchantEffect);
            buffer.writeInt(this._npc.isFlying());
            buffer.writeInt(0);
            buffer.writeInt(this._npc.getColorEffect());
            buffer.writeByte(this._npc.isTargetable());
            buffer.writeByte(this._npc.isShowName());
            buffer.writeInt(this._npc.getAbnormalVisualEffectSpecial());
            buffer.writeInt(this._displayEffect);
         }
      }
   }

   public static class SummonInfo extends AbstractNpcInfo {
      private final Summon _summon;
      private final int _form;
      private final int _value;

      public SummonInfo(Summon cha, Creature attacker, int value) {
         super(cha, attacker.isGM());
         this._summon = cha;
         this._value = value;
         this._form = cha.getFormId();
         this._isAttackable = cha.isAutoAttackable(attacker);
         this._rhand = cha.getWeapon();
         this._lhand = 0;
         this._chest = cha.getArmor();
         this._enchantEffect = cha.getTemplate().getWeaponEnchant();
         this._name = cha.getName();
         this._title = cha.getOwner() != null && cha.getOwner().isOnline() ? cha.getOwner().getName() : "";
         this._displayId = cha.getTemplate().getDisplayId();
         this._collisionHeight = cha.getTemplate().getFCollisionHeight();
         this._collisionRadius = cha.getTemplate().getFCollisionRadius();
      }

      @Override
      public void writeImpl(org.l2jmobius.gameserver.network.GameClient client, WriteBuffer buffer) {
         org.l2jmobius.gameserver.network.ServerPackets.NPC_INFO.writeId(this, buffer);
         buffer.writeInt(this._summon.getObjectId());
         buffer.writeInt(this._displayId + 1000000);
         buffer.writeInt(this._isAttackable);
         buffer.writeInt(this._x);
         buffer.writeInt(this._y);
         buffer.writeInt(this._z);
         buffer.writeInt(this._heading);
         buffer.writeInt(0);
         buffer.writeInt(this._mAtkSpd);
         buffer.writeInt(this._pAtkSpd);
         buffer.writeInt(this._runSpd);
         buffer.writeInt(this._walkSpd);
         buffer.writeInt(this._swimRunSpd);
         buffer.writeInt(this._swimWalkSpd);
         buffer.writeInt(this._flyRunSpd);
         buffer.writeInt(this._flyWalkSpd);
         buffer.writeInt(this._flyRunSpd);
         buffer.writeInt(this._flyWalkSpd);
         buffer.writeDouble(this._moveMultiplier);
         buffer.writeDouble(this._summon.getAttackSpeedMultiplier());
         buffer.writeDouble(this._collisionRadius);
         buffer.writeDouble(this._collisionHeight);
         buffer.writeInt(this._rhand);
         buffer.writeInt(this._chest);
         buffer.writeInt(this._lhand);
         buffer.writeByte(1);
         buffer.writeByte(1);
         buffer.writeByte(this._summon.isInCombat());
         buffer.writeByte(this._summon.isAlikeDead());
         buffer.writeByte(this._isSummoned ? 2 : this._value);
         buffer.writeString(this._name);
         buffer.writeString(this._title);
         buffer.writeInt(1);
         buffer.writeInt(this._summon.getPvpFlag());
         buffer.writeInt(this._summon.getKarma());
         buffer.writeInt(
            this._gmSeeInvis && this._summon.isInvisible()
               ? this._summon.getAbnormalVisualEffects() | AbnormalVisualEffect.STEALTH.getMask()
               : this._summon.getAbnormalVisualEffects()
         );
         buffer.writeInt(0);
         buffer.writeInt(0);
         buffer.writeInt(0);
         buffer.writeInt(0);
         buffer.writeByte(this._summon.isInsideZone(ZoneId.WATER) ? 1 : (this._summon.isFlying() ? 2 : 0));
         buffer.writeByte(this._summon.getTeam().getId());
         buffer.writeDouble(this._collisionRadius);
         buffer.writeDouble(this._collisionHeight);
         buffer.writeInt(this._enchantEffect);
         buffer.writeInt(0);
         buffer.writeInt(0);
         buffer.writeInt(this._form);
         buffer.writeByte(1);
         buffer.writeByte(1);
         buffer.writeInt(this._summon.getAbnormalVisualEffectSpecial());
      }
   }

   public static class TrapInfo extends AbstractNpcInfo {
      private final Trap _trap;

      public TrapInfo(Trap cha, Creature attacker) {
         super(cha, attacker != null && attacker.isGM());
         this._trap = cha;
         this._displayId = cha.getTemplate().getDisplayId();
         this._isAttackable = cha.isAutoAttackable(attacker);
         this._rhand = 0;
         this._lhand = 0;
         this._collisionHeight = this._trap.getTemplate().getFCollisionHeight();
         this._collisionRadius = this._trap.getTemplate().getFCollisionRadius();
         if (cha.getTemplate().isUsingServerSideName()) {
            this._name = cha.getName();
         }

         this._title = cha.getOwner() != null ? cha.getOwner().getName() : "";
      }

      @Override
      public void writeImpl(org.l2jmobius.gameserver.network.GameClient client, WriteBuffer buffer) {
         org.l2jmobius.gameserver.network.ServerPackets.NPC_INFO.writeId(this, buffer);
         buffer.writeInt(this._trap.getObjectId());
         buffer.writeInt(this._displayId + 1000000);
         buffer.writeInt(this._isAttackable);
         buffer.writeInt(this._x);
         buffer.writeInt(this._y);
         buffer.writeInt(this._z);
         buffer.writeInt(this._heading);
         buffer.writeInt(0);
         buffer.writeInt(this._mAtkSpd);
         buffer.writeInt(this._pAtkSpd);
         buffer.writeInt(this._runSpd);
         buffer.writeInt(this._walkSpd);
         buffer.writeInt(this._swimRunSpd);
         buffer.writeInt(this._swimWalkSpd);
         buffer.writeInt(this._flyRunSpd);
         buffer.writeInt(this._flyWalkSpd);
         buffer.writeInt(this._flyRunSpd);
         buffer.writeInt(this._flyWalkSpd);
         buffer.writeDouble(this._moveMultiplier);
         buffer.writeDouble(this._trap.getAttackSpeedMultiplier());
         buffer.writeDouble(this._collisionRadius);
         buffer.writeDouble(this._collisionHeight);
         buffer.writeInt(this._rhand);
         buffer.writeInt(this._chest);
         buffer.writeInt(this._lhand);
         buffer.writeByte(1);
         buffer.writeByte(1);
         buffer.writeByte(this._trap.isInCombat());
         buffer.writeByte(this._trap.isAlikeDead());
         buffer.writeByte(this._isSummoned ? 2 : 0);
         buffer.writeString(this._name);
         buffer.writeString(this._title);
         buffer.writeInt(0);
         buffer.writeInt(this._trap.getPvpFlag());
         buffer.writeInt(this._trap.getKarma());
         buffer.writeInt(
            this._trap.isInvisible() ? this._trap.getAbnormalVisualEffects() | AbnormalVisualEffect.STEALTH.getMask() : this._trap.getAbnormalVisualEffects()
         );
         buffer.writeInt(0);
         buffer.writeInt(0);
         buffer.writeInt(0);
         buffer.writeInt(0);
         buffer.writeByte(0);
         buffer.writeByte(this._trap.getTeam().getId());
         buffer.writeDouble(this._collisionRadius);
         buffer.writeDouble(this._collisionHeight);
         buffer.writeInt(0);
         buffer.writeInt(0);
         buffer.writeInt(0);
         buffer.writeInt(0);
         buffer.writeByte(1);
         buffer.writeByte(1);
         buffer.writeInt(0);
      }
   }
}
