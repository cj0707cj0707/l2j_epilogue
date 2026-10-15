package org.l2jmobius.gameserver.util;

import org.l2jmobius.gameserver.config.FloodProtectorConfig;
import org.l2jmobius.gameserver.network.GameClient;

public class FloodProtectors {
   private final FloodProtectorAction _useItem;
   private final FloodProtectorAction _rollDice;
   private final FloodProtectorAction _itemPetSummon;
   private final FloodProtectorAction _heroVoice;
   private final FloodProtectorAction _globalChat;
   private final FloodProtectorAction _subclass;
   private final FloodProtectorAction _dropItem;
   private final FloodProtectorAction _serverBypass;
   private final FloodProtectorAction _multiSell;
   private final FloodProtectorAction _transaction;
   private final FloodProtectorAction _manufacture;
   private final FloodProtectorAction _sendMail;
   private final FloodProtectorAction _characterSelect;
   private final FloodProtectorAction _itemAuction;
   private final FloodProtectorAction _playerAction;

   public FloodProtectors(GameClient client) {
      this._useItem = new FloodProtectorAction(client, FloodProtectorConfig.FLOOD_PROTECTOR_USE_ITEM);
      this._rollDice = new FloodProtectorAction(client, FloodProtectorConfig.FLOOD_PROTECTOR_ROLL_DICE);
      this._itemPetSummon = new FloodProtectorAction(client, FloodProtectorConfig.FLOOD_PROTECTOR_ITEM_PET_SUMMON);
      this._heroVoice = new FloodProtectorAction(client, FloodProtectorConfig.FLOOD_PROTECTOR_HERO_VOICE);
      this._globalChat = new FloodProtectorAction(client, FloodProtectorConfig.FLOOD_PROTECTOR_GLOBAL_CHAT);
      this._subclass = new FloodProtectorAction(client, FloodProtectorConfig.FLOOD_PROTECTOR_SUBCLASS);
      this._dropItem = new FloodProtectorAction(client, FloodProtectorConfig.FLOOD_PROTECTOR_DROP_ITEM);
      this._serverBypass = new FloodProtectorAction(client, FloodProtectorConfig.FLOOD_PROTECTOR_SERVER_BYPASS);
      this._multiSell = new FloodProtectorAction(client, FloodProtectorConfig.FLOOD_PROTECTOR_MULTISELL);
      this._transaction = new FloodProtectorAction(client, FloodProtectorConfig.FLOOD_PROTECTOR_TRANSACTION);
      this._manufacture = new FloodProtectorAction(client, FloodProtectorConfig.FLOOD_PROTECTOR_MANUFACTURE);
      this._sendMail = new FloodProtectorAction(client, FloodProtectorConfig.FLOOD_PROTECTOR_SENDMAIL);
      this._characterSelect = new FloodProtectorAction(client, FloodProtectorConfig.FLOOD_PROTECTOR_CHARACTER_SELECT);
      this._itemAuction = new FloodProtectorAction(client, FloodProtectorConfig.FLOOD_PROTECTOR_ITEM_AUCTION);
      this._playerAction = new FloodProtectorAction(client, FloodProtectorConfig.FLOOD_PROTECTOR_PLAYER_ACTION);
   }

   public boolean canUseItem() {
      return this._useItem.canPerformAction();
   }

   public boolean canRollDice() {
      return this._rollDice.canPerformAction();
   }

   public boolean canUsePetSummonItem() {
      return this._itemPetSummon.canPerformAction();
   }

   public boolean canUseHeroVoice() {
      return this._heroVoice.canPerformAction();
   }

   public boolean canUseGlobalChat() {
      return this._globalChat.canPerformAction();
   }

   public boolean canChangeSubclass() {
      return this._subclass.canPerformAction();
   }

   public boolean canDropItem() {
      return this._dropItem.canPerformAction();
   }

   public boolean canUseServerBypass() {
      return this._serverBypass.canPerformAction();
   }

   public boolean canUseMultiSell() {
      return this._multiSell.canPerformAction();
   }

   public boolean canPerformTransaction() {
      return this._transaction.canPerformAction();
   }

   public boolean canManufacture() {
      return this._manufacture.canPerformAction();
   }

   public boolean canSendMail() {
      return this._sendMail.canPerformAction();
   }

   public boolean canSelectCharacter() {
      return this._characterSelect.canPerformAction();
   }

   public boolean canUseItemAuction() {
      return this._itemAuction.canPerformAction();
   }

   public boolean canPerformPlayerAction() {
      return this._playerAction.canPerformAction();
   }
}
