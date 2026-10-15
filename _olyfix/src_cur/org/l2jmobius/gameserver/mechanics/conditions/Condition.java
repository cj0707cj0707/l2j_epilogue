package org.l2jmobius.gameserver.mechanics.conditions;

import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.item.ItemTemplate;
import org.l2jmobius.gameserver.mechanics.skill.Skill;

public abstract class Condition implements ConditionListener {
   private ConditionListener _listener;
   private String _msg;
   private int _msgId;
   private boolean _addName = false;
   private boolean _result;

   public void setMessage(String msg) {
      this._msg = msg;
   }

   public String getMessage() {
      return this._msg;
   }

   public void setMessageId(int msgId) {
      this._msgId = msgId;
   }

   public int getMessageId() {
      return this._msgId;
   }

   public void addName() {
      this._addName = true;
   }

   public boolean isAddName() {
      return this._addName;
   }

   void setListener(ConditionListener listener) {
      this._listener = listener;
      this.notifyChanged();
   }

   ConditionListener getListener() {
      return this._listener;
   }

   public boolean test(Creature caster, Creature target, Skill skill) {
      return this.test(caster, target, skill, null);
   }

   public boolean test(Creature caster, Creature target, ItemTemplate item) {
      return this.test(caster, target, null, null);
   }

   public boolean test(Creature caster, Creature target, Skill skill, ItemTemplate item) {
      boolean res = this.testImpl(caster, target, skill, item);
      if (this._listener != null && res != this._result) {
         this._result = res;
         this.notifyChanged();
      }

      return res;
   }

   public abstract boolean testImpl(Creature var1, Creature var2, Skill var3, ItemTemplate var4);

   @Override
   public void notifyChanged() {
      if (this._listener != null) {
         this._listener.notifyChanged();
      }
   }
}
