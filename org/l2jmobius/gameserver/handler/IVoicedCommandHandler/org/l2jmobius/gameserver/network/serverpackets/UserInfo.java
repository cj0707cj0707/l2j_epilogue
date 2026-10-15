package org.l2jmobius.gameserver.network.serverpackets;

import java.util.Map;
import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.config.GeneralConfig;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.appearance.PlayerAppearance;
import org.l2jmobius.gameserver.entity.actor.instance.Cubic;
import org.l2jmobius.gameserver.entity.zone.ZoneId;
import org.l2jmobius.gameserver.managers.CursedWeaponsManager;
import org.l2jmobius.gameserver.managers.TerritoryWarManager;
import org.l2jmobius.gameserver.mechanics.skill.AbnormalVisualEffect;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;

public class UserInfo extends ServerPacket {
   private final Player _player;
   private int _relation;
   private int _territoryId;
   private final boolean _isDisguised;
   private int _airShipHelm;
   private final int _runSpd;
   private final int _walkSpd;
   private final int _swimRunSpd;
   private final int _swimWalkSpd;
   private final int _flyRunSpd;
   private final int _flyWalkSpd;
   private final double _moveMultiplier;

   public UserInfo(Player player) {
      this._player = player;
      int _territoryId = TerritoryWarManager.getInstance().getRegisteredTerritoryId(player);
      this._relation = this._player.isClanLeader() ? 64 : 0;
      if (this._player.getSiegeState() == 1) {
         if (_territoryId == 0) {
            this._relation |= 384;
         } else {
            this._relation |= 4096;
         }
      }

      if (this._player.getSiegeState() == 2) {
         this._relation |= 128;
      }

      this._isDisguised = TerritoryWarManager.getInstance().isDisguised(this._player.getObjectId());
      if (this._player.isInAirShip() && this._player.getAirShip().isCaptain(this._player)) {
         this._airShipHelm = this._player.getAirShip().getHelmItemId();
      } else {
         this._airShipHelm = 0;
      }

      this._moveMultiplier = player.getMovementSpeedMultiplier();
      this._runSpd = (int)Math.round(player.getRunSpeed() / this._moveMultiplier);
      this._walkSpd = (int)Math.round(player.getWalkSpeed() / this._moveMultiplier);
      this._swimRunSpd = (int)Math.round(player.getSwimRunSpeed() / this._moveMultiplier);
      this._swimWalkSpd = (int)Math.round(player.getSwimWalkSpeed() / this._moveMultiplier);
      this._flyRunSpd = player.isFlying() ? this._runSpd : 0;
      this._flyWalkSpd = player.isFlying() ? this._walkSpd : 0;
   }

   @Override
   public void writeImpl(GameClient client, WriteBuffer buffer) {
      ServerPackets.USER_INFO.writeId(this, buffer);
      buffer.writeInt(this._player.getX());
      buffer.writeInt(this._player.getY());
      buffer.writeInt(this._player.getZ());
      buffer.writeInt(this._player.getVehicle() != null ? this._player.getVehicle().getObjectId() : 0);
      buffer.writeInt(this._player.getObjectId());
      PlayerAppearance appearance = this._player.getAppearance();
      buffer.writeString(appearance.getVisibleName());
      buffer.writeInt(this._player.getRace().ordinal());
      buffer.writeInt(appearance.isFemale());
      buffer.writeInt(this._player.getBaseClass());
      buffer.writeInt(this._player.getLevel());
      buffer.writeLong(this._player.getExp());
      buffer.writeInt(this._player.getSTR());
      buffer.writeInt(this._player.getDEX());
      buffer.writeInt(this._player.getCON());
      buffer.writeInt(this._player.getINT());
      buffer.writeInt(this._player.getWIT());
      buffer.writeInt(this._player.getMEN());
      buffer.writeInt(this._player.getMaxHp());
      buffer.writeInt((int)Math.round(this._player.getCurrentHp()));
      buffer.writeInt(this._player.getMaxMp());
      buffer.writeInt((int)Math.round(this._player.getCurrentMp()));
      buffer.writeInt((int)this._player.getSp());
      buffer.writeInt(this._player.getCurrentLoad());
      buffer.writeInt(this._player.getMaxLoad());
      buffer.writeInt(this._player.getActiveWeaponItem() != null ? 40 : 20);

      for (int slot : this.getPaperdollOrder()) {
         buffer.writeInt(this._player.getInventory().getPaperdollObjectId(slot));
      }

      for (int slot : this.getPaperdollOrder()) {
         buffer.writeInt(this._player.getInventory().getPaperdollItemDisplayId(slot));
      }

      for (int slot : this.getPaperdollOrder()) {
         buffer.writeInt(this._player.getInventory().getPaperdollAugmentationId(slot));
      }

      buffer.writeInt(this._player.getInventory().getTalismanSlots());
      buffer.writeInt(this._player.getInventory().canEquipCloak());
      buffer.writeInt((int)this._player.getPAtk(null));
      buffer.writeInt((int)this._player.getPAtkSpd());
      buffer.writeInt((int)this._player.getPDef(null));
      buffer.writeInt(this._player.getEvasionRate(null));
      buffer.writeInt(this._player.getAccuracy());
      buffer.writeInt(this._player.getCriticalHit(null, null));
      buffer.writeInt((int)this._player.getMAtk(null, null));
      buffer.writeInt(this._player.getMAtkSpd());
      buffer.writeInt((int)this._player.getPAtkSpd());
      buffer.writeInt((int)this._player.getMDef(null, null));
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
      buffer.writeInt(this._player.isGM());
      String title = this._player.getTitle();
      if (this._player.isGM() && this._player.isInvisible()) {
         title = "[Invisible]";
      }

      buffer.writeString(title);
      buffer.writeInt(this._player.getClanId());
      buffer.writeInt(this._player.getClanCrestId());
      buffer.writeInt(this._player.getAllyId());
      buffer.writeInt(this._player.getAllyCrestId());
      buffer.writeInt(this._relation);
      buffer.writeByte(this._player.getMountType().ordinal());
      buffer.writeByte(this._player.getPrivateStoreType().getId());
      buffer.writeByte(this._player.hasDwarvenCraft());
      buffer.writeInt(this._player.getPkKills());
      buffer.writeInt(this._player.getPvpKills());
      Map<Integer, Cubic> cubics = this._player.getCubics();
      buffer.writeShort(cubics.size());

      for (int cubicId : cubics.keySet()) {
         buffer.writeShort(cubicId);
      }

      buffer.writeByte(this._player.isInPartyMatchRoom());
      buffer.writeInt(
         this._player.isInvisible()
            ? this._player.getAbnormalVisualEffects() | AbnormalVisualEffect.STEALTH.getMask()
            : this._player.getAbnormalVisualEffects()
      );
      buffer.writeByte(this._player.isInsideZone(ZoneId.WATER) ? 1 : (this._player.isFlyingMounted() ? 2 : 0));
      buffer.writeInt(this._player.getClanPrivileges().getMask());
      buffer.writeShort(this._player.getRecomLeft());
      buffer.writeShort(this._player.getRecomHave());
      buffer.writeInt(this._player.getMountNpcId() > 0 ? this._player.getMountNpcId() + 1000000 : 0);
      buffer.writeShort(this._player.getInventoryLimit());
      buffer.writeInt(this._player.getPlayerClass().getId());
      buffer.writeInt(0);
      buffer.writeInt(this._player.getMaxCp());
      buffer.writeInt((int)this._player.getCurrentCp());
      buffer.writeByte(!this._player.isMounted() && this._airShipHelm == 0 ? this._player.getEnchantEffect() : 0);
      buffer.writeByte(this._player.getTeam().getId());
      buffer.writeInt(this._player.getClanCrestLargeId());
      buffer.writeByte(this._player.isNoble());
      buffer.writeByte(this._player.isHero() || this._player.isGM() && GeneralConfig.GM_HERO_AURA);
      buffer.writeByte(this._player.isFishing());
      buffer.writeInt(this._player.getFishX());
      buffer.writeInt(this._player.getFishY());
      buffer.writeInt(this._player.getFishZ());
      buffer.writeInt(appearance.getNameColor());
      buffer.writeByte(this._player.isRunning());
      buffer.writeInt(this._player.getPledgeClass());
      buffer.writeInt(this._player.getPledgeType());
      buffer.writeInt(appearance.getTitleColor());
      buffer.writeInt(this._player.isCursedWeaponEquipped() ? CursedWeaponsManager.getInstance().getLevel(this._player.getCursedWeaponEquippedId()) : 0);
      buffer.writeInt(this._player.getTransformationDisplayId());
      byte attackAttribute = this._player.getAttackElement();
      buffer.writeShort((short)attackAttribute);
      buffer.writeShort(this._player.getAttackElementValue(attackAttribute));
      buffer.writeShort(this._player.getDefenseElementValue((byte)0));
      buffer.writeShort(this._player.getDefenseElementValue((byte)1));
      buffer.writeShort(this._player.getDefenseElementValue((byte)2));
      buffer.writeShort(this._player.getDefenseElementValue((byte)3));
      buffer.writeShort(this._player.getDefenseElementValue((byte)4));
      buffer.writeShort(this._player.getDefenseElementValue((byte)5));
      buffer.writeInt(this._player.getAgathionId());
      buffer.writeInt(this._player.getFame());
      buffer.writeInt(this._player.isMinimapAllowed());
      buffer.writeInt(this._player.getVitalityPoints());
      buffer.writeInt(this._player.getAbnormalVisualEffectSpecial());
      buffer.writeInt(this._territoryId);
      buffer.writeInt(this._isDisguised);
      buffer.writeInt(this._territoryId);
   }
}
