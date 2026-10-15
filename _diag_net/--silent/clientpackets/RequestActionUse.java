package org.l2jmobius.gameserver.network.clientpackets;

import java.util.Arrays;
import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.ai.Intention;
import org.l2jmobius.gameserver.ai.NextAction;
import org.l2jmobius.gameserver.ai.SummonAI;
import org.l2jmobius.gameserver.config.GeneralConfig;
import org.l2jmobius.gameserver.data.BotReportTable;
import org.l2jmobius.gameserver.data.xml.PetDataTable;
import org.l2jmobius.gameserver.data.xml.PetSkillData;
import org.l2jmobius.gameserver.data.xml.SkillData;
import org.l2jmobius.gameserver.entity.WorldObject;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.Summon;
import org.l2jmobius.gameserver.entity.actor.enums.player.MountType;
import org.l2jmobius.gameserver.entity.actor.enums.player.PrivateStoreType;
import org.l2jmobius.gameserver.entity.actor.instance.BabyPet;
import org.l2jmobius.gameserver.entity.actor.instance.Pet;
import org.l2jmobius.gameserver.entity.actor.instance.SiegeFlag;
import org.l2jmobius.gameserver.entity.actor.instance.StaticObject;
import org.l2jmobius.gameserver.managers.AirShipManager;
import org.l2jmobius.gameserver.mechanics.effects.AbstractEffect;
import org.l2jmobius.gameserver.mechanics.effects.EffectType;
import org.l2jmobius.gameserver.mechanics.skill.AbnormalType;
import org.l2jmobius.gameserver.mechanics.skill.BuffInfo;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.mechanics.skill.holders.SkillHolder;
import org.l2jmobius.gameserver.network.enums.ChatType;
import org.l2jmobius.gameserver.network.serverpackets.ActionFailed;
import org.l2jmobius.gameserver.network.serverpackets.ChairSit;
import org.l2jmobius.gameserver.network.serverpackets.ExBasicActionList;
import org.l2jmobius.gameserver.network.serverpackets.NpcSay;
import org.l2jmobius.gameserver.network.serverpackets.RecipeShopManageList;
import org.l2jmobius.gameserver.network.serverpackets.SocialAction;

public class RequestActionUse extends ClientPacket {
   private static final int SIN_EATER_ID = 12564;
   private static final int SWITCH_STANCE_ID = 6054;
   private static final String[] NPC_STRINGS = new String[]{
      "Using a special skill here could trigger a bloodbath!",
      "Hey, what do you expect of me?",
      "Ugggggh! Push! It's not coming out!",
      "Ah, I missed the mark!"
   };
   private int _actionId;
   private boolean _ctrlPressed;
   private boolean _shiftPressed;

   @Override
   protected void readImpl() {
      this._actionId = this.readInt();
      this._ctrlPressed = this.readInt() == 1;
      this._shiftPressed = this.readByte() == 1;
   }

   @Override
   protected void runImpl() {
      Player player = this.getPlayer();
      if (player != null) {
         if ((!player.isFakeDeath() || this._actionId == 0) && !player.isDead() && !player.isOutOfControl()) {
            BuffInfo info = player.getEffectList().getBuffInfoByAbnormalType(AbnormalType.BOT_PENALTY);
            if (info != null) {
               for (AbstractEffect effect : info.getEffects()) {
                  if (!effect.checkCondition(this._actionId)) {
                     player.sendPacket(
                        org.l2jmobius.gameserver.network.SystemMessageId.YOU_HAVE_BEEN_REPORTED_AS_AN_ILLEGAL_PROGRAM_USER_AND_YOUR_ACTIVITIES_ARE_ONLY_ALLOWED_WITHIN_LIMITATION
                     );
                     player.sendPacket(ActionFailed.STATIC_PACKET);
                     return;
                  }
               }
            }

            if (player.isTransformed()) {
               int[] allowedActions = player.isTransformed() ? ExBasicActionList.ACTIONS_ON_TRANSFORM : ExBasicActionList.DEFAULT_ACTION_LIST;
               if (Arrays.binarySearch(allowedActions, this._actionId) < 0) {
                  player.sendPacket(ActionFailed.STATIC_PACKET);
                  org.l2jmobius.gameserver.network.PacketLogger.warning(
                     player + " used action which he does not have! Id = " + this._actionId + " transform: " + player.getTransformation().getId()
                  );
                  return;
               }
            }

            Summon summon = player.getSummon();
            WorldObject target = player.getTarget();
            switch (this._actionId) {
               case 0:
                  if (!player.isSitting() && player.isMoving() && !player.isFakeDeath()) {
                     NextAction nextAction = new NextAction(org.l2jmobius.gameserver.ai.Action.ARRIVED, Intention.MOVE_TO, () -> this.useSit(player, target));
                     player.getAI().setNextAction(nextAction);
                  } else {
                     this.useSit(player, target);
                  }
                  break;
               case 1:
                  if (player.isRunning()) {
                     player.setWalking();
                  } else {
                     player.setRunning();
                  }
                  break;
               case 10:
                  player.tryOpenPrivateSellStore(false);
                  break;
               case 12:
                  this.tryBroadcastSocial(player, 2);
                  break;
               case 13:
                  this.tryBroadcastSocial(player, 3);
                  break;
               case 14:
                  this.tryBroadcastSocial(player, 4);
                  break;
               case 15:
                  if (this.validateSummon(player, summon, true)) {
                     ((SummonAI)summon.getAI()).notifyFollowStatusChange();
                  }
                  break;
               case 16:
                  if (this.validateSummon(player, summon, true) && summon.canAttack(this._ctrlPressed)) {
                     summon.doSummonAttack(target);
                  }
                  break;
               case 17:
                  if (this.validateSummon(player, summon, true)) {
                     summon.cancelAction();
                  }
                  break;
               case 19:
                  if (this.validateSummon(player, summon, true)) {
                     if (summon.isDead()) {
                        player.sendPacket(org.l2jmobius.gameserver.network.SystemMessageId.A_DEAD_PET_CANNOT_BE_SENT_BACK);
                     } else if (summon.isMovementDisabled()) {
                        player.sendPacket(org.l2jmobius.gameserver.network.SystemMessageId.YOUR_PET_SERVITOR_IS_CURRENTLY_IN_A_STATE_OF_DISTRESS);
                     } else if (summon.isAttackingNow() || summon.isInCombat()) {
                        player.sendPacket(org.l2jmobius.gameserver.network.SystemMessageId.A_PET_CANNOT_BE_SENT_BACK_DURING_BATTLE);
                     } else if (summon.isPet()) {
                        Pet pet = summon.asPet();
                        float currentFedPercent = (float)pet.getCurrentFed() / pet.getPetLevelData().getPetMaxFeed() * 100.0F;
                        if (currentFedPercent < 40.0F) {
                           player.sendPacket(org.l2jmobius.gameserver.network.SystemMessageId.YOU_MAY_NOT_RESTORE_A_HUNGRY_PET);
                        } else {
                           summon.unSummon(player);
                        }
                     } else {
                        player.sendPacket(
                           org.l2jmobius.gameserver.network.SystemMessageId.THE_HUNTING_HELPER_PET_CANNOT_BE_RETURNED_BECAUSE_THERE_IS_NOT_MUCH_TIME_REMAINING_UNTIL_IT_LEAVES
                        );
                     }
                  }
                  break;
               case 21:
                  if (this.validateSummon(player, summon, false)) {
                     ((SummonAI)summon.getAI()).notifyFollowStatusChange();
                  }
                  break;
               case 22:
                  if (this.validateSummon(player, summon, false) && summon.canAttack(this._ctrlPressed)) {
                     summon.doSummonAttack(target);
                  }
                  break;
               case 23:
                  if (this.validateSummon(player, summon, false)) {
                     summon.cancelAction();
                  }
                  break;
               case 24:
                  this.tryBroadcastSocial(player, 6);
                  break;
               case 25:
                  this.tryBroadcastSocial(player, 5);
                  break;
               case 26:
                  this.tryBroadcastSocial(player, 7);
                  break;
               case 28:
                  player.tryOpenPrivateBuyStore();
                  break;
               case 29:
                  this.tryBroadcastSocial(player, 8);
                  break;
               case 30:
                  this.tryBroadcastSocial(player, 9);
                  break;
               case 31:
                  this.tryBroadcastSocial(player, 10);
                  break;
               case 32:
                  this.useSkill(player, "DDMagic", false);
                  break;
               case 33:
                  this.tryBroadcastSocial(player, 11);
                  break;
               case 34:
                  this.tryBroadcastSocial(player, 12);
                  break;
               case 35:
                  this.tryBroadcastSocial(player, 13);
                  break;
               case 36:
                  this.useSkill(player, "RangeDebuff", false);
                  break;
               case 37:
                  if (player.isAlikeDead() || player.isSellingBuffs()) {
                     player.sendPacket(ActionFailed.STATIC_PACKET);
                     return;
                  }

                  if (player.isMounted()) {
                     player.sendPacket(ActionFailed.STATIC_PACKET);
                     return;
                  }

                  if (player.isInStoreMode()) {
                     player.setPrivateStoreType(PrivateStoreType.NONE);
                     player.broadcastUserInfo();
                  }

                  if (player.isSitting()) {
                     player.standUp();
                  }

                  player.sendPacket(new RecipeShopManageList(player, true));
                  break;
               case 38:
                  player.mountPlayer(summon);
                  break;
               case 39:
                  this.useSkill(player, "RangeDD", false);
                  break;
               case 41:
                  if (this.validateSummon(player, summon, false)) {
                     if (target == null || !target.isDoor() && !(target instanceof SiegeFlag)) {
                        player.sendPacket(org.l2jmobius.gameserver.network.SystemMessageId.INVALID_TARGET);
                     } else {
                        this.useSkill(player, 4230, false);
                     }
                  }
                  break;
               case 42:
                  this.useSkill(player, "HealMagic", false);
                  break;
               case 43:
                  this.useSkill(player, "DDMagic", false);
                  break;
               case 44:
                  this.useSkill(player, "DDMagic", false);
                  break;
               case 45:
                  this.useSkill(player, "HealMagic", player, false);
                  break;
               case 46:
                  this.useSkill(player, "DDMagic", false);
                  break;
               case 47:
                  this.useSkill(player, "DDMagic", false);
                  break;
               case 48:
                  this.useSkill(player, "DDMagic", false);
                  break;
               case 51:
                  if (player.isAlikeDead()) {
                     player.sendPacket(ActionFailed.STATIC_PACKET);
                     return;
                  }

                  if (player.getPrivateStoreType() != PrivateStoreType.NONE) {
                     player.setPrivateStoreType(PrivateStoreType.NONE);
                     player.broadcastUserInfo();
                  }

                  if (player.isSitting()) {
                     player.standUp();
                  }

                  player.sendPacket(new RecipeShopManageList(player, false));
                  break;
               case 52:
                  if (this.validateSummon(player, summon, false)) {
                     if (summon.isMovementDisabled()) {
                        player.sendPacket(org.l2jmobius.gameserver.network.SystemMessageId.YOUR_PET_SERVITOR_IS_CURRENTLY_IN_A_STATE_OF_DISTRESS);
                     } else if (!summon.isAttackingNow() && !summon.isInCombat()) {
                        summon.unSummon(player);
                     } else {
                        player.sendPacket(org.l2jmobius.gameserver.network.SystemMessageId.A_SERVITOR_WHOM_IS_ENGAGED_IN_BATTLE_CANNOT_BE_DE_ACTIVATED);
                     }
                  }
                  break;
               case 53:
                  if (this.validateSummon(player, summon, false) && target != null && summon != target && !summon.isMovementDisabled()) {
                     summon.setFollowStatus(false);
                     summon.getAI().setIntentionMoveTo(target.getLocation());
                  }
                  break;
               case 54:
                  if (this.validateSummon(player, summon, true) && target != null && summon != target && !summon.isMovementDisabled()) {
                     summon.setFollowStatus(false);
                     summon.getAI().setIntentionMoveTo(target.getLocation());
                  }
                  break;
               case 61:
                  player.tryOpenPrivateSellStore(true);
                  break;
               case 62:
                  this.tryBroadcastSocial(player, 14);
                  break;
               case 65:
                  if (GeneralConfig.BOTREPORT_ENABLE) {
                     BotReportTable.getInstance().reportBot(player);
                  } else {
                     player.sendMessage("This feature is disabled.");
                  }
                  break;
               case 66:
                  this.tryBroadcastSocial(player, 15);
                  break;
               case 67:
                  if (player.isInAirShip() && player.getAirShip().setCaptain(player)) {
                     player.broadcastUserInfo();
                  }
                  break;
               case 68:
                  if (player.isInAirShip() && player.getAirShip().isCaptain(player) && player.getAirShip().setCaptain(null)) {
                     player.broadcastUserInfo();
                  }
                  break;
               case 69:
                  AirShipManager.getInstance().sendAirShipTeleportList(player);
                  break;
               case 70:
                  if (player.isInAirShip()) {
                     if (player.getAirShip().isCaptain(player)) {
                        if (player.getAirShip().setCaptain(null)) {
                           player.broadcastUserInfo();
                        }
                     } else if (player.getAirShip().isInDock()) {
                        player.getAirShip().oustPlayer(player);
                     }
                  }
                  break;
               case 1000:
                  if (target != null && target.isDoor()) {
                     this.useSkill(player, 4079, false);
                  }
                  break;
               case 1001:
                  if (this.validateSummon(player, summon, true) && summon.getId() == 12564) {
                     summon.broadcastPacket(new NpcSay(summon.getObjectId(), ChatType.NPC_GENERAL, summon.getId(), NPC_STRINGS[Rnd.get(NPC_STRINGS.length)]));
                  }
                  break;
               case 1003:
                  this.useSkill(player, "PhysicalSpecial", true);
                  break;
               case 1004:
                  this.useSkill(player, "Buff", player, true);
                  break;
               case 1005:
                  this.useSkill(player, "DDMagic", true);
                  break;
               case 1006:
                  this.useSkill(player, "Heal", player, true);
                  break;
               case 1007:
                  this.useSkill(player, "Buff1", player, false);
                  break;
               case 1008:
                  this.useSkill(player, "Buff2", player, false);
                  break;
               case 1009:
                  this.useSkill(player, "DDMagic", false);
                  break;
               case 1010:
                  this.useSkill(player, "Buff1", player, false);
                  break;
               case 1011:
                  this.useSkill(player, "Buff2", player, false);
                  break;
               case 1012:
                  this.useSkill(player, "DDMagic", false);
                  break;
               case 1013:
                  this.useSkill(player, "DeBuff1", false);
                  break;
               case 1014:
                  this.useSkill(player, "DeBuff2", false);
                  break;
               case 1015:
                  this.useSkill(player, "Heal", false);
                  break;
               case 1016:
                  this.useSkill(player, "PhysicalSpecial1", false);
                  break;
               case 1017:
                  this.useSkill(player, "PhysicalSpecial2", false);
                  break;
               case 1031:
                  this.useSkill(player, "PhysicalSpecial1", false);
                  break;
               case 1032:
                  this.useSkill(player, "PhysicalSpecial2", false);
                  break;
               case 1033:
                  this.useSkill(player, "PhysicalSpecial3", false);
                  break;
               case 1034:
                  this.useSkill(player, "PhysicalSpecial1", false);
                  break;
               case 1035:
                  this.useSkill(player, "PhysicalSpecial2", false);
                  break;
               case 1036:
                  this.useSkill(player, "PhysicalSpecial1", false);
                  break;
               case 1037:
                  this.useSkill(player, "PhysicalSpecial2", false);
                  break;
               case 1038:
                  this.useSkill(player, "PhysicalSpecial3", false);
                  break;
               case 1039:
                  this.useSkill(player, 5110, false);
                  break;
               case 1040:
                  this.useSkill(player, 5111, false);
                  break;
               case 1041:
                  this.useSkill(player, "Skill01", true);
                  break;
               case 1042:
                  this.useSkill(player, "Skill03", true);
                  break;
               case 1043:
                  this.useSkill(player, "Skill02", true);
                  break;
               case 1044:
                  this.useSkill(player, "Skill04", true);
                  break;
               case 1045:
                  this.useSkill(player, 5584, true);
                  break;
               case 1046:
                  this.useSkill(player, 5585, true);
                  break;
               case 1047:
                  this.useSkill(player, 5580, false);
                  break;
               case 1048:
                  this.useSkill(player, 5581, false);
                  break;
               case 1049:
                  this.useSkill(player, 5582, false);
                  break;
               case 1050:
                  this.useSkill(player, 5583, false);
                  break;
               case 1051:
                  this.useSkill(player, "buff3", false);
                  break;
               case 1052:
                  this.useSkill(player, "buff4", false);
                  break;
               case 1053:
                  this.useSkill(player, "buff5", false);
                  break;
               case 1054:
                  this.useSkill(player, "buff3", false);
                  break;
               case 1055:
                  this.useSkill(player, "buff4", false);
                  break;
               case 1056:
                  this.useSkill(player, "buff5", false);
                  break;
               case 1057:
                  this.useSkill(player, "buff6", false);
                  break;
               case 1058:
                  this.useSkill(player, "buff3", false);
                  break;
               case 1059:
                  this.useSkill(player, "buff4", false);
                  break;
               case 1060:
                  this.useSkill(player, "buff5", false);
                  break;
               case 1061:
                  this.useSkill(player, 5745, true);
                  break;
               case 1062:
                  this.useSkill(player, 5746, true);
                  break;
               case 1063:
                  this.useSkill(player, 5747, true);
                  break;
               case 1064:
                  this.useSkill(player, 5748, true);
                  break;
               case 1065:
                  this.useSkill(player, 5753, true);
                  break;
               case 1066:
                  this.useSkill(player, 5749, true);
                  break;
               case 1067:
                  this.useSkill(player, 5750, true);
                  break;
               case 1068:
                  this.useSkill(player, 5751, true);
                  break;
               case 1069:
                  this.useSkill(player, 5752, true);
                  break;
               case 1070:
                  this.useSkill(player, 5771, true);
                  break;
               case 1071:
                  this.useSkill(player, "DDMagic", true);
                  break;
               case 1072:
                  this.useSkill(player, 6046, true);
                  break;
               case 1073:
                  this.useSkill(player, 6047, true);
                  break;
               case 1074:
                  this.useSkill(player, 6048, true);
                  break;
               case 1075:
                  this.useSkill(player, 6049, true);
                  break;
               case 1076:
                  this.useSkill(player, 6050, true);
                  break;
               case 1077:
                  this.useSkill(player, 6051, true);
                  break;
               case 1078:
                  this.useSkill(player, 6052, true);
                  break;
               case 1079:
                  this.useSkill(player, 6053, true);
                  break;
               case 1080:
                  this.useSkill(player, 6041, false);
                  break;
               case 1081:
                  this.useSkill(player, 6042, false);
                  break;
               case 1082:
                  this.useSkill(player, 6043, false);
                  break;
               case 1083:
                  this.useSkill(player, 6044, false);
                  break;
               case 1084:
                  if (summon instanceof BabyPet) {
                     this.useSkill(player, 6054, true);
                  }
                  break;
               case 1086:
                  this.useSkill(player, 6094, false);
                  break;
               case 1087:
                  this.useSkill(player, 6095, false);
                  break;
               case 1088:
                  this.useSkill(player, 6096, false);
                  break;
               case 1089:
                  this.useSkill(player, 6199, true);
                  break;
               case 1090:
                  this.useSkill(player, 6205, true);
                  break;
               case 1091:
                  this.useSkill(player, 6206, true);
                  break;
               case 1092:
                  this.useSkill(player, 6207, true);
                  break;
               case 1093:
                  this.useSkill(player, 6618, true);
                  break;
               case 1094:
                  this.useSkill(player, 6681, true);
                  break;
               case 1095:
                  this.useSkill(player, 6619, true);
                  break;
               case 1096:
                  this.useSkill(player, 6682, true);
                  break;
               case 1097:
                  this.useSkill(player, 6683, true);
                  break;
               case 1098:
                  this.useSkill(player, 6684, true);
                  break;
               case 5000:
                  this.useSkill(player, 23155, true);
                  break;
               case 5001:
                  this.useSkill(player, 23167, true);
                  break;
               case 5002:
                  this.useSkill(player, 23168, true);
                  break;
               case 5003:
                  this.useSkill(player, 5749, true);
                  break;
               case 5004:
                  this.useSkill(player, 5750, true);
                  break;
               case 5005:
                  this.useSkill(player, 5751, true);
                  break;
               case 5006:
                  this.useSkill(player, 5771, true);
                  break;
               case 5007:
                  this.useSkill(player, 6046, true);
                  break;
               case 5008:
                  this.useSkill(player, 6047, true);
                  break;
               case 5009:
                  this.useSkill(player, 6048, true);
                  break;
               case 5010:
                  this.useSkill(player, 6049, true);
                  break;
               case 5011:
                  this.useSkill(player, 6050, true);
                  break;
               case 5012:
                  this.useSkill(player, 6051, true);
                  break;
               case 5013:
                  this.useSkill(player, 6052, true);
                  break;
               case 5014:
                  this.useSkill(player, 6053, true);
                  break;
               case 5015:
                  this.useSkill(player, 6054, true);
            }
         } else {
            player.sendPacket(ActionFailed.STATIC_PACKET);
         }
      }
   }

   protected boolean useSit(Player player, WorldObject target) {
      if (player.getMountType() != MountType.NONE) {
         return false;
      } else if (!player.isSitting() && target instanceof StaticObject && ((StaticObject)target).getType() == 1 && player.isInsideRadius2D(target, 150)) {
         ChairSit cs = new ChairSit(player, target.getId());
         player.sendPacket(cs);
         player.sitDown();
         player.broadcastPacket(cs);
         return true;
      } else {
         if (player.isFakeDeath()) {
            player.stopEffects(EffectType.FAKE_DEATH);
         } else if (player.isSitting()) {
            player.standUp();
         } else {
            player.sitDown();
         }

         return true;
      }
   }

   private void useSkill(Player player, int skillId, WorldObject target, boolean pet) {
      Summon summon = player.getSummon();
      if (this.validateSummon(player, summon, pet)) {
         if (this.canControl(player, summon)) {
            int level = 0;
            if (summon.isPet()) {
               level = PetDataTable.getInstance().getPetData(summon.getId()).getAvailableLevel(skillId, summon.getLevel());
            } else {
               level = PetSkillData.getInstance().getAvailableLevel(summon, skillId);
            }

            if (level > 0) {
               summon.setTarget(target);
               summon.useMagic(SkillData.getInstance().getSkill(skillId, level), this._ctrlPressed, this._shiftPressed);
            }

            if (skillId == 6054) {
               summon.switchMode();
            }
         }
      }
   }

   private void useSkill(Player player, String skillName, WorldObject target, boolean pet) {
      Summon summon = player.getSummon();
      if (this.validateSummon(player, summon, pet)) {
         if (this.canControl(player, summon)) {
            if (summon instanceof BabyPet && !((BabyPet)summon).isInSupportMode()) {
               player.sendPacket(org.l2jmobius.gameserver.network.SystemMessageId.A_PET_ON_AUXILIARY_MODE_CANNOT_USE_SKILLS);
            } else {
               SkillHolder skillHolder = summon.getTemplate().getParameters().getSkillHolder(skillName);
               if (skillHolder != null) {
                  Skill skill = skillHolder.getSkill();
                  if (skill != null) {
                     summon.setTarget(target);
                     summon.useMagic(skill, this._ctrlPressed, this._shiftPressed);
                     if (skill.getId() == 6054) {
                        summon.switchMode();
                     }
                  }
               }
            }
         }
      }
   }

   private boolean canControl(Player player, Summon summon) {
      if (summon instanceof BabyPet && !((BabyPet)summon).isInSupportMode()) {
         player.sendPacket(org.l2jmobius.gameserver.network.SystemMessageId.A_PET_ON_AUXILIARY_MODE_CANNOT_USE_SKILLS);
         return false;
      } else if (summon.isPet() && summon.getLevel() - player.getLevel() > 20) {
         player.sendPacket(org.l2jmobius.gameserver.network.SystemMessageId.YOUR_PET_IS_TOO_HIGH_LEVEL_TO_CONTROL);
         return false;
      } else {
         return true;
      }
   }

   private void useSkill(Player player, int skillId, boolean pet) {
      this.useSkill(player, skillId, player.getTarget(), pet);
   }

   private void useSkill(Player player, String skillName, boolean pet) {
      this.useSkill(player, skillName, player.getTarget(), pet);
   }

   private boolean validateSummon(Player player, Summon summon, boolean checkPet) {
      if (summon == null || (!checkPet || !summon.isPet()) && !summon.isServitor()) {
         if (checkPet) {
            player.sendPacket(org.l2jmobius.gameserver.network.SystemMessageId.YOU_DO_NOT_HAVE_A_PET);
         } else {
            player.sendPacket(org.l2jmobius.gameserver.network.SystemMessageId.YOU_DO_NOT_HAVE_A_SERVITOR);
         }

         return false;
      } else if (summon.isPet() && summon.asPet().isUncontrollable()) {
         player.sendPacket(
            org.l2jmobius.gameserver.network.SystemMessageId.ONLY_A_CLAN_LEADER_THAT_IS_A_NOBLESSE_CAN_VIEW_THE_SIEGE_WAR_STATUS_WINDOW_DURING_A_SIEGE_WAR
         );
         return false;
      } else if (summon.isBetrayed()) {
         player.sendPacket(org.l2jmobius.gameserver.network.SystemMessageId.YOUR_PET_SERVITOR_IS_UNRESPONSIVE_AND_WILL_NOT_OBEY_ANY_ORDERS);
         return false;
      } else {
         return true;
      }
   }

   private void tryBroadcastSocial(Player player, int id) {
      if (player.isFishing()) {
         player.sendPacket(org.l2jmobius.gameserver.network.SystemMessageId.YOU_CANNOT_DO_THAT_WHILE_FISHING_3);
      } else {
         if (player.canMakeSocialAction()) {
            player.broadcastPacket(new SocialAction(player.getObjectId(), id));
         }
      }
   }
}
