package org.l2jmobius.gameserver.mechanics.events.holders.actor.player;

import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.mechanics.events.EventType;
import org.l2jmobius.gameserver.mechanics.events.holders.IBaseEvent;
import org.l2jmobius.gameserver.mechanics.options.Augmentation;

public class OnPlayerAugment implements IBaseEvent {
   private final Player _player;
   private final Item _item;
   private final Augmentation _augmentation;
   private final boolean _isAugment;

   public OnPlayerAugment(Player player, Item item, Augmentation augment, boolean isAugment) {
      this._player = player;
      this._item = item;
      this._augmentation = augment;
      this._isAugment = isAugment;
   }

   public Player getPlayer() {
      return this._player;
   }

   public Item getItem() {
      return this._item;
   }

   public Augmentation getAugmentation() {
      return this._augmentation;
   }

   public boolean isAugment() {
      return this._isAugment;
   }

   @Override
   public EventType getType() {
      return EventType.ON_PLAYER_AUGMENT;
   }
}
