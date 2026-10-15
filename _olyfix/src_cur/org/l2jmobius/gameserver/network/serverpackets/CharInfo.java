package org.l2jmobius.gameserver.network.serverpackets;

import java.util.Map;
import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.config.GeneralConfig;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.appearance.PlayerAppearance;
import org.l2jmobius.gameserver.entity.actor.instance.Cubic;
import org.l2jmobius.gameserver.entity.actor.instance.Decoy;
import org.l2jmobius.gameserver.entity.clan.Clan;
import org.l2jmobius.gameserver.entity.zone.ZoneId;
import org.l2jmobius.gameserver.managers.CursedWeaponsManager;
import org.l2jmobius.gameserver.managers.TerritoryWarManager;
import org.l2jmobius.gameserver.mechanics.skill.AbnormalVisualEffect;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;

public class CharInfo extends ServerPacket {
   private static final int[] PAPERDOLL_ORDER = new int[]{0, 1, 5, 7, 10, 6, 11, 12, 23, 5, 2, 3, 16, 15, 17, 18, 19, 20, 21, 22, 24};
   private final Player _player;
   private final Clan _clan;
   private int _objId;
   private int _x;
   private int _y;
   private int _z;
   private int _heading;
   private final int _mAtkSpd;
   private final int _pAtkSpd;
   private final int _runSpd;
   private final int _walkSpd;
   private final int _swimRunSpd;
   private final int _swimWalkSpd;
   private final int _flyRunSpd;
   private final int _flyWalkSpd;
   private final double _moveMultiplier;
   private final int _territoryId;
   private final boolean _isDisguised;
   private int _vehicleId = 0;
   private final boolean _gmSeeInvis;

   public CharInfo(Player player, boolean gmSeeInvis) {
      this._player = player;
      this._objId = player.getObjectId();
      this._clan = player.getClan();
      if (this._player.getVehicle() != null && this._player.getInVehiclePosition() != null) {
         this._x = this._player.getInVehiclePosition().getX();
         this._y = this._player.getInVehiclePosition().getY();
         this._z = this._player.getInVehiclePosition().getZ();
         this._vehicleId = this._player.getVehicle().getObjectId();
      } else {
         this._x = this._player.getX();
         this._y = this._player.getY();
         this._z = this._player.getZ();
      }

      this._heading = this._player.getHeading();
      this._mAtkSpd = this._player.getMAtkSpd();
      this._pAtkSpd = (int)this._player.getPAtkSpd();
      this._moveMultiplier = player.getMovementSpeedMultiplier();
      this._runSpd = (int)Math.round(player.getRunSpeed() / this._moveMultiplier);
      this._walkSpd = (int)Math.round(player.getWalkSpeed() / this._moveMultiplier);
      this._swimRunSpd = (int)Math.round(player.getSwimRunSpeed() / this._moveMultiplier);
      this._swimWalkSpd = (int)Math.round(player.getSwimWalkSpeed() / this._moveMultiplier);
      this._flyRunSpd = player.isFlying() ? this._runSpd : 0;
      this._flyWalkSpd = player.isFlying() ? this._walkSpd : 0;
      this._gmSeeInvis = gmSeeInvis;
      this._territoryId = TerritoryWarManager.getInstance().getRegisteredTerritoryId(player);
      this._isDisguised = TerritoryWarManager.getInstance().isDisguised(player.getObjectId());
   }

   public CharInfo(Decoy decoy, boolean gmSeeInvis) {
      this(decoy.asPlayer(), gmSeeInvis);
      this._objId = decoy.getObjectId();
      this._x = decoy.getX();
      this._y = decoy.getY();
      this._z = decoy.getZ();
      this._heading = decoy.getHeading();
   }

   @Override
   public void writeImpl(GameClient client, WriteBuffer buffer) {
      ServerPackets.CHAR_INFO.writeId(this, buffer);
      buffer.writeInt(this._x);
      buffer.writeInt(this._y);
      buffer.writeInt(this._z);
      buffer.writeInt(this._vehicleId);
      buffer.writeInt(this._objId);
      PlayerAppearance appearance = this._player.getAppearance();
      buffer.writeString(appearance.getVisibleName());
      buffer.writeInt(this._player.getRace().ordinal());
      buffer.writeInt(appearance.isFemale());
      buffer.writeInt(this._player.getBaseClass());

      for (int slot : this.getPaperdollOrder()) {
         buffer.writeInt(this._player.getInventory().getPaperdollItemDisplayId(slot));
      }

      for (int slot : this.getPaperdollOrder()) {
         buffer.writeInt(this._player.getInventory().getPaperdollAugmentationId(slot));
      }

      buffer.writeInt(this._player.getInventory().getTalismanSlots());
      buffer.writeInt(this._player.getInventory().canEquipCloak());
      buffer.writeInt(this._player.getPvpFlag());
      buffer.writeInt(this._player.getKarma());
      buffer.writeInt(this._mAtkSpd);
      buffer.writeInt(this._pAtkSpd);
      buffer.writeInt(this._player.getPvpFlag());
      buffer.writeInt(this._player.getKarma());
      buffer.writeInt(this._runSpd);
      buffer.writeInt(this._walkSpd);
      buffer.writeInt(this._swimRunSpd);
      buffer.writeInt(this._swimWalkSpd);
      buffer.writeInt(this._flyRunSpd);
      buffer.writeInt(this._flyWalkSpd);
      buffer.writeInt(this._flyRunSpd);
      buffer.writeInt(this._flyWalkSpd);
      buffer.writeDouble(this._moveMultiplier);
      buffer.writeDouble(this._player.getAttackSpeedMultiplier());
      buffer.writeDouble(this._player.getCollisionRadius());
      buffer.writeDouble(this._player.getCollisionHeight());
      buffer.writeInt(appearance.getHairStyle());
      buffer.writeInt(appearance.getHairColor());
      buffer.writeInt(appearance.getFace());
      buffer.writeString(this._gmSeeInvis ? "Invisible" : appearance.getVisibleTitle());
      if (!this._player.isCursedWeaponEquipped()) {
         buffer.writeInt(this._player.getClanId());
         buffer.writeInt(this._player.getClanCrestId());
         buffer.writeInt(this._player.getAllyId());
         buffer.writeInt(this._player.getAllyCrestId());
      } else {
         buffer.writeInt(0);
         buffer.writeInt(0);
         buffer.writeInt(0);
         buffer.writeInt(0);
      }

      buffer.writeInt(0);
      buffer.writeByte(!this._player.isSitting());
      buffer.writeByte(this._player.isRunning());
      buffer.writeByte(this._player.isInCombat());
      buffer.writeByte(!this._player.isInOlympiadMode() && this._player.isAlikeDead());
      buffer.writeByte(!this._gmSeeInvis && this._player.isInvisible());
      buffer.writeByte(this._player.getMountType().ordinal());
      buffer.writeByte(this._player.getPrivateStoreType().getId());
      Map<Integer, Cubic> cubics = this._player.getCubics();
      buffer.writeShort(cubics.size());

      for (int cubicId : cubics.keySet()) {
         buffer.writeShort(cubicId);
      }

      buffer.writeByte(this._player.isInPartyMatchRoom());
      buffer.writeInt(
         this._gmSeeInvis ? this._player.getAbnormalVisualEffects() | AbnormalVisualEffect.STEALTH.getMask() : this._player.getAbnormalVisualEffects()
      );
      buffer.writeByte(this._player.isInsideZone(ZoneId.WATER) ? 1 : (this._player.isFlyingMounted() ? 2 : 0));
      buffer.writeShort(this._player.getRecomHave());
      buffer.writeInt(this._player.getMountNpcId() + 1000000);
      buffer.writeInt(this._player.getPlayerClass().getId());
      buffer.writeInt(0);
      buffer.writeByte(this._player.isMounted() ? 0 : this._player.getEnchantEffect());
      buffer.writeByte(this._player.getTeam().getId());
      buffer.writeInt(this._player.getClanCrestLargeId());
      buffer.writeByte(this._player.isNoble());
      buffer.writeByte(this._player.isHero() || this._player.isGM() && GeneralConfig.GM_HERO_AURA);
      buffer.writeByte(this._player.isFishing());
      buffer.writeInt(this._player.getFishX());
      buffer.writeInt(this._player.getFishY());
      buffer.writeInt(this._player.getFishZ());
      buffer.writeInt(appearance.getNameColor());
      buffer.writeInt(this._heading);
      buffer.writeInt(this._player.getPledgeClass());
      buffer.writeInt(this._player.getPledgeType());
      buffer.writeInt(appearance.getTitleColor());
      buffer.writeInt(this._player.isCursedWeaponEquipped() ? CursedWeaponsManager.getInstance().getLevel(this._player.getCursedWeaponEquippedId()) : 0);
      buffer.writeInt(this._clan != null ? this._clan.getReputationScore() : 0);
      buffer.writeInt(this._player.getTransformationDisplayId());
      buffer.writeInt(this._player.getAgathionId());
      buffer.writeInt(1);
      buffer.writeInt(this._player.getAbnormalVisualEffectSpecial());
      buffer.writeInt(this._territoryId);
      buffer.writeInt(this._isDisguised);
      buffer.writeInt(this._territoryId);
   }

   @Override
   public int[] getPaperdollOrder() {
      return PAPERDOLL_ORDER;
   }
}
