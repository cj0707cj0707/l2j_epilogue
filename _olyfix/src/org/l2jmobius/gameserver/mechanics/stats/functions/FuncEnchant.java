package org.l2jmobius.gameserver.mechanics.stats.functions;

import org.l2jmobius.gameserver.config.OlympiadConfig;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.item.enums.BodyPart;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.entity.item.type.WeaponType;
import org.l2jmobius.gameserver.mechanics.conditions.Condition;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.mechanics.stats.Stat;

public class FuncEnchant extends AbstractFunction {
   public FuncEnchant(Stat stat, int order, Object owner, double value, Condition applyCond) {
      super(stat, order, owner, value, applyCond);
   }

   @Override
   public double calc(Creature effector, Creature effected, Skill skill, double initVal) {
      double value = initVal;
      if (this.getApplyCond() != null && !this.getApplyCond().test(effector, effected, skill)) {
         return initVal;
      } else {
         Item item = (Item)this.getFuncOwner();
         int enchant = item.getEnchantLevel();
         if (enchant <= 0) {
            return initVal;
         } else {
            int overenchant = 0;
            if (enchant > 3) {
               overenchant = enchant - 3;
               enchant = 3;
            }

            if (effector.isPlayer()
               && effector.asPlayer().isInOlympiadMode()
               && OlympiadConfig.OLYMPIAD_ENCHANT_LIMIT >= 0
               && enchant + overenchant > OlympiadConfig.OLYMPIAD_ENCHANT_LIMIT) {
               if (OlympiadConfig.OLYMPIAD_ENCHANT_LIMIT > 3) {
                  overenchant = OlympiadConfig.OLYMPIAD_ENCHANT_LIMIT - 3;
               } else {
                  overenchant = 0;
                  enchant = OlympiadConfig.OLYMPIAD_ENCHANT_LIMIT;
               }
            }

            if (this.getStat() == Stat.MAGIC_DEFENCE || this.getStat() == Stat.POWER_DEFENCE) {
               return initVal + enchant + 3 * overenchant;
            } else if (this.getStat() == Stat.MAGIC_ATTACK) {
               switch (item.getTemplate().getCrystalTypePlus()) {
                  case S:
                     value = initVal + (4 * enchant + 8 * overenchant);
                     break;
                  case A:
                  case B:
                  case C:
                     value = initVal + (3 * enchant + 6 * overenchant);
                     break;
                  case D:
                  case NONE:
                     value = initVal + (2 * enchant + 4 * overenchant);
               }

               return value;
            } else {
               if (item.isWeapon()) {
                  WeaponType type = (WeaponType)item.getItemType();
                  switch (item.getTemplate().getCrystalTypePlus()) {
                     case S:
                        if (item.getWeaponItem().getBodyPart() == BodyPart.LR_HAND) {
                           if (type == WeaponType.BOW) {
                              value = initVal + (10 * enchant + 20 * overenchant);
                           } else if (type == WeaponType.CROSSBOW) {
                              value = initVal + (7 * enchant + 14 * overenchant);
                           } else {
                              value = initVal + (6 * enchant + 12 * overenchant);
                           }
                        } else {
                           value = initVal + (5 * enchant + 10 * overenchant);
                        }
                        break;
                     case A:
                        if (item.getWeaponItem().getBodyPart() == BodyPart.LR_HAND) {
                           if (type == WeaponType.BOW) {
                              value = initVal + (8 * enchant + 16 * overenchant);
                           } else if (type == WeaponType.CROSSBOW) {
                              value = initVal + (6 * enchant + 12 * overenchant);
                           } else {
                              value = initVal + (5 * enchant + 10 * overenchant);
                           }
                        } else {
                           value = initVal + (4 * enchant + 8 * overenchant);
                        }
                        break;
                     case B:
                     case C:
                        if (item.getWeaponItem().getBodyPart() == BodyPart.LR_HAND) {
                           if (type == WeaponType.BOW) {
                              value = initVal + (6 * enchant + 12 * overenchant);
                           } else if (type == WeaponType.CROSSBOW) {
                              value = initVal + (5 * enchant + 10 * overenchant);
                           } else {
                              value = initVal + (4 * enchant + 8 * overenchant);
                           }
                        } else {
                           value = initVal + (3 * enchant + 6 * overenchant);
                        }
                        break;
                     case D:
                     case NONE:
                        switch (type) {
                           case BOW:
                              value = initVal + (4 * enchant + 8 * overenchant);
                              break;
                           case CROSSBOW:
                              value = initVal + (3 * enchant + 6 * overenchant);
                              break;
                           default:
                              value = initVal + (2 * enchant + 4 * overenchant);
                        }
                  }
               }

               return value;
            }
         }
      }
   }
}
