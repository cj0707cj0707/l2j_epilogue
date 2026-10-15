package org.l2jmobius.gameserver.entity.actor.transform;

import org.l2jmobius.gameserver.entity.item.holders.ItemHolder;

public class AdditionalItemHolder extends ItemHolder {
   private final boolean _allowed;

   public AdditionalItemHolder(int id, boolean allowed) {
      super(id, 0L);
      this._allowed = allowed;
   }

   public boolean isAllowedToUse() {
      return this._allowed;
   }
}
