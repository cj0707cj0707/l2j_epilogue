package org.l2jmobius.gameserver.ai;

import java.util.ArrayList;
import java.util.List;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.instance.Defender;

public class SpecialSiegeGuardAI extends SiegeGuardAI {
   private final List<Integer> _allied = new ArrayList<>();

   public SpecialSiegeGuardAI(Defender creature) {
      super(creature);
   }

   public List<Integer> getAlly() {
      return this._allied;
   }

   @Override
   protected boolean autoAttackCondition(Creature target) {
      return this._allied.contains(target.getObjectId()) ? false : super.autoAttackCondition(target);
   }
}
