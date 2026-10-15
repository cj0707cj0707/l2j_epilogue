package org.l2jmobius.gameserver.mechanics.siege;

import org.l2jmobius.gameserver.entity.Location;
import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.item.enums.BodyPart;
import org.l2jmobius.gameserver.entity.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.managers.ItemManager;
import org.l2jmobius.gameserver.managers.TerritoryWarManager;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.InventoryUpdate;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;

public class TerritoryWard {
   protected Player _player = null;
   public int playerId = 0;
   private Item _item = null;
   private Npc _npc = null;
   private Location _location;
   private Location _oldLocation;
   private final int _itemId;
   private int _ownerCastleId;
   private final int _territoryId;

   public TerritoryWard(int territoryId, int x, int y, int z, int heading, int itemId, int castleId, Npc npc) {
      this._territoryId = territoryId;
      this._location = new Location(x, y, z, heading);
      this._itemId = itemId;
      this._ownerCastleId = castleId;
      this._npc = npc;
   }

   public int getTerritoryId() {
      return this._territoryId;
   }

   public int getOwnerCastleId() {
      return this._ownerCastleId;
   }

   public void setOwnerCastleId(int newOwner) {
      this._ownerCastleId = newOwner;
   }

   public Npc getNpc() {
      return this._npc;
   }

   public void setNpc(Npc npc) {
      this._npc = npc;
   }

   public Player getPlayer() {
      return this._player;
   }

   public synchronized void spawnBack() {
      if (this._player != null) {
         this.dropIt();
      }

      this._npc = TerritoryWarManager.getInstance().spawnNPC(36491 + this._territoryId, this._oldLocation);
   }

   public synchronized void spawnMe() {
      if (this._player != null) {
         this.dropIt();
      }

      this._npc = TerritoryWarManager.getInstance().spawnNPC(36491 + this._territoryId, this._location);
   }

   public synchronized void unSpawnMe() {
      if (this._player != null) {
         this.dropIt();
      }

      if (this._npc != null && !this._npc.isDecayed()) {
         this._npc.deleteMe();
      }
   }

   public boolean activate(Player player, Item item) {
      if (player.isMounted()) {
         player.sendPacket(SystemMessageId.YOU_DO_NOT_MEET_THE_REQUIRED_CONDITION_TO_EQUIP_THAT_ITEM);
         player.destroyItem(ItemProcessType.DESTROY, item, null, true);
         this.spawnMe();
         return false;
      } else if (TerritoryWarManager.getInstance().getRegisteredTerritoryId(player) == 0) {
         player.sendMessage("Non participants can't pickup Territory Wards!");
         player.destroyItem(ItemProcessType.DESTROY, item, null, true);
         this.spawnMe();
         return false;
      } else {
         this._player = player;
         this.playerId = this._player.getObjectId();
         this._oldLocation = new Location(this._npc.getX(), this._npc.getY(), this._npc.getZ(), this._npc.getHeading());
         this._npc = null;
         if (item == null) {
            this._item = ItemManager.createItem(ItemProcessType.PICKUP, this._itemId, 1L, null, null);
         } else {
            this._item = item;
         }

         this._player.getInventory().equipItem(this._item);
         SystemMessage sm = new SystemMessage(SystemMessageId.YOU_HAVE_EQUIPPED_YOUR_S1);
         sm.addItemName(this._item);
         this._player.sendPacket(sm);
         InventoryUpdate iu = new InventoryUpdate();
         iu.addItem(this._item);
         this._player.sendInventoryUpdate(iu);
         this._player.broadcastUserInfo();
         this._player.setCombatFlagEquipped(true);
         this._player.sendPacket(SystemMessageId.YOU_VE_ACQUIRED_THE_WARD_MOVE_QUICKLY_TO_YOUR_FORCES_OUTPOST);
         TerritoryWarManager.getInstance().giveTWPoint(player, this._territoryId, 5);
         return true;
      }
   }

   public void dropIt() {
      this._player.setCombatFlagEquipped(false);
      BodyPart bodyPart = BodyPart.fromItem(this._item);
      this._player.getInventory().unEquipItemInBodySlot(bodyPart);
      this._player.destroyItem(ItemProcessType.DESTROY, this._item, null, true);
      this._item = null;
      this._player.broadcastUserInfo();
      this._location = new Location(this._player.getX(), this._player.getY(), this._player.getZ(), this._player.getHeading());
      this._player = null;
      this.playerId = 0;
   }
}
