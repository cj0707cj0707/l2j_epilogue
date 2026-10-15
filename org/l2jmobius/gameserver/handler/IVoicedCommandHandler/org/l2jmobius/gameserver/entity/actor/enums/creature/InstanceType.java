package org.l2jmobius.gameserver.entity.actor.enums.creature;

public enum InstanceType {
   WorldObject(null),
   Item(WorldObject),
   Creature(WorldObject),
   Npc(Creature),
   Playable(Creature),
   Summon(Playable),
   Player(Playable),
   Folk(Npc),
   Merchant(Folk),
   Warehouse(Folk),
   StaticObject(Creature),
   Door(Creature),
   TerrainObject(Npc),
   EffectPoint(Npc),
   Servitor(Summon),
   Pet(Summon),
   BabyPet(Pet),
   Decoy(Creature),
   Trap(Npc),
   Attackable(Npc),
   Guard(Attackable),
   QuestGuard(Guard),
   Monster(Attackable),
   Chest(Monster),
   FeedableBeast(Monster),
   TamedBeast(FeedableBeast),
   FriendlyMob(Attackable),
   RiftInvader(Monster),
   RaidBoss(Monster),
   GrandBoss(RaidBoss),
   FlyNpc(Folk),
   FlyMonster(Monster),
   FlyRaidBoss(RaidBoss),
   FlyTerrainObject(Npc),
   FestivalGuide(Npc),
   FestivalMonster(Monster),
   Vehicle(Creature),
   Boat(Vehicle),
   AirShip(Vehicle),
   ControllableAirShip(AirShip),
   Defender(Attackable),
   Artefact(Folk),
   ControlTower(Npc),
   FlameTower(Npc),
   SiegeFlag(Npc),
   FortCommander(Defender),
   FortLogistics(Merchant),
   FortManager(Merchant),
   SignsPriest(Npc),
   DawnPriest(SignsPriest),
   DuskPriest(SignsPriest),
   DungeonGatekeeper(Npc),
   Adventurer(Folk),
   Auctioneer(Npc),
   BroadcastingTower(Npc),
   ClanHallManager(Merchant),
   Fisherman(Merchant),
   OlympiadManager(Npc),
   PetManager(Merchant),
   RaceManager(Npc),
   Teleporter(Npc),
   Trainer(Folk),
   VillageMaster(Folk),
   Doorman(Folk),
   CastleDoorman(Doorman),
   FortDoorman(Doorman),
   ClanHallDoorman(Doorman),
   ClassMaster(Folk),
   SchemeBuffer(Npc),
   EventMob(Npc),
   UCManagerInstance(Folk);

   private final InstanceType _parent;
   private final long _typeL;
   private final long _typeH;
   private final long _maskL;
   private final long _maskH;

   private InstanceType(InstanceType parent) {
      this._parent = parent;
      int high = this.ordinal() - 63;
      if (high < 0) {
         this._typeL = 1L << this.ordinal();
         this._typeH = 0L;
      } else {
         this._typeL = 0L;
         this._typeH = 1L << high;
      }

      if (this._typeL >= 0L && this._typeH >= 0L) {
         if (parent != null) {
            this._maskL = this._typeL | parent._maskL;
            this._maskH = this._typeH | parent._maskH;
         } else {
            this._maskL = this._typeL;
            this._maskH = this._typeH;
         }
      } else {
         throw new Error("Too many instance types, failed to load " + this.name());
      }
   }

   public InstanceType getParent() {
      return this._parent;
   }

   public boolean isType(InstanceType it) {
      return (this._maskL & it._typeL) > 0L || (this._maskH & it._typeH) > 0L;
   }

   public boolean isTypes(InstanceType... it) {
      for (InstanceType i : it) {
         if (this.isType(i)) {
            return true;
         }
      }

      return false;
   }
}
