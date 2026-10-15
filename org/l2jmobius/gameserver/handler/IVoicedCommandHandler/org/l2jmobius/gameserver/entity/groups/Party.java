package org.l2jmobius.gameserver.entity.groups;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.config.PlayerConfig;
import org.l2jmobius.gameserver.config.RatesConfig;
import org.l2jmobius.gameserver.config.custom.ClassBalanceConfig;
import org.l2jmobius.gameserver.config.custom.PremiumSystemConfig;
import org.l2jmobius.gameserver.entity.actor.Attackable;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.Summon;
import org.l2jmobius.gameserver.entity.groups.matching.PartyMatchRoomList;
import org.l2jmobius.gameserver.entity.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.entity.item.holders.ItemHolder;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.managers.DuelManager;
import org.l2jmobius.gameserver.managers.ItemManager;
import org.l2jmobius.gameserver.managers.PcCafePointsManager;
import org.l2jmobius.gameserver.mechanics.sevensigns.DimensionalRift;
import org.l2jmobius.gameserver.mechanics.sevensigns.SevenSignsFestival;
import org.l2jmobius.gameserver.mechanics.stats.Stat;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.ExCloseMPCC;
import org.l2jmobius.gameserver.network.serverpackets.ExOpenMPCC;
import org.l2jmobius.gameserver.network.serverpackets.ExPartyPetWindowAdd;
import org.l2jmobius.gameserver.network.serverpackets.ExPartyPetWindowDelete;
import org.l2jmobius.gameserver.network.serverpackets.PartyMemberPosition;
import org.l2jmobius.gameserver.network.serverpackets.PartySmallWindowAdd;
import org.l2jmobius.gameserver.network.serverpackets.PartySmallWindowAll;
import org.l2jmobius.gameserver.network.serverpackets.PartySmallWindowDelete;
import org.l2jmobius.gameserver.network.serverpackets.PartySmallWindowDeleteAll;
import org.l2jmobius.gameserver.network.serverpackets.ServerPacket;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;
import org.l2jmobius.gameserver.taskmanagers.GameTimeTaskManager;
import org.l2jmobius.gameserver.util.LocationUtil;

public class Party extends AbstractPlayerGroup {
   private static final Logger LOGGER = Logger.getLogger(Party.class.getName());
   private static final double[] BONUS_EXP_SP = new double[]{1.0, 1.1, 1.2, 1.3, 1.4, 1.5, 2.0, 2.1, 2.2};
   private static final Duration PARTY_POSITION_BROADCAST_INTERVAL = Duration.ofSeconds(12L);
   private final List<Player> _members = new CopyOnWriteArrayList<>();
   private boolean _pendingInvitation = false;
   private long _pendingInviteTimeout;
   private int _partyLvl = 0;
   private PartyDistributionType _distributionType = PartyDistributionType.FINDERS_KEEPERS;
   private int _itemLastLoot = 0;
   private CommandChannel _commandChannel = null;
   private DimensionalRift _dr;
   private Future<?> _positionBroadcastTask = null;
   protected PartyMemberPosition _positionPacket;
   private boolean _disbanding = false;
   private Object _ucState = null;

   public Party(Player leader, PartyDistributionType partyDistributionType) {
      this._members.add(leader);
      this._partyLvl = leader.getLevel();
      this._distributionType = partyDistributionType;
   }

   public boolean getPendingInvitation() {
      return this._pendingInvitation;
   }

   public void setPendingInvitation(boolean value) {
      this._pendingInvitation = value;
      this._pendingInviteTimeout = GameTimeTaskManager.getInstance().getGameTicks() + 150;
   }

   public boolean isInvitationRequestExpired() {
      return this._pendingInviteTimeout <= GameTimeTaskManager.getInstance().getGameTicks();
   }

   private Player getCheckedRandomMember(int itemId, Creature target) {
      List<Player> availableMembers = new ArrayList<>();

      for (Player member : this._members) {
         if (member.getInventory().validateCapacityByItemId(itemId) && LocationUtil.checkIfInRange(PlayerConfig.ALT_PARTY_RANGE, target, member, true)) {
            availableMembers.add(member);
         }
      }

      return !availableMembers.isEmpty() ? availableMembers.get(Rnd.get(availableMembers.size())) : null;
   }

   private Player getCheckedNextLooter(int itemId, Creature target) {
      for (int i = 0; i < this.getMemberCount(); i++) {
         if (++this._itemLastLoot >= this.getMemberCount()) {
            this._itemLastLoot = 0;
         }

         try {
            Player member = this._members.get(this._itemLastLoot);
            if (member.getInventory().validateCapacityByItemId(itemId) && LocationUtil.checkIfInRange(PlayerConfig.ALT_PARTY_RANGE, target, member, true)) {
               return member;
            }
         } catch (Exception var6) {
         }
      }

      return null;
   }

   private Player getActualLooter(Player player, int itemId, boolean spoil, Creature target) {
      Player looter = null;
      switch (this._distributionType) {
         case RANDOM:
            if (!spoil) {
               looter = this.getCheckedRandomMember(itemId, target);
            }
            break;
         case RANDOM_INCLUDING_SPOIL:
            looter = this.getCheckedRandomMember(itemId, target);
            break;
         case BY_TURN:
            if (!spoil) {
               looter = this.getCheckedNextLooter(itemId, target);
            }
            break;
         case BY_TURN_INCLUDING_SPOIL:
            looter = this.getCheckedNextLooter(itemId, target);
      }

      return looter != null ? looter : player;
   }

   public void broadcastToPartyMembersNewLeader() {
      for (Player member : this._members) {
         if (member != null) {
            member.sendPacket(PartySmallWindowDeleteAll.STATIC_PACKET);
            member.sendPacket(new PartySmallWindowAll(member, this));
            member.broadcastUserInfo();
         }
      }
   }

   public void broadcastToPartyMembers(Player player, ServerPacket packet) {
      for (Player member : this._members) {
         if (member != null && member.getObjectId() != player.getObjectId()) {
            member.sendPacket(packet);
         }
      }
   }

   public void addPartyMember(Player player) {
      if (!this._members.contains(player)) {
         this._members.add(player);
         player.sendPacket(new PartySmallWindowAll(player, this));

         for (Player pMember : this._members) {
            if (pMember != null && pMember.hasSummon()) {
               player.sendPacket(new ExPartyPetWindowAdd(pMember.getSummon()));
            }
         }

         SystemMessage msg = new SystemMessage(SystemMessageId.YOU_HAVE_JOINED_S1_S_PARTY);
         msg.addString(this.getLeader().getName());
         player.sendPacket(msg);
         msg = new SystemMessage(SystemMessageId.C1_HAS_JOINED_THE_PARTY);
         msg.addString(player.getName());
         this.broadcastToPartyMembers(player, msg);

         for (Player member : this._members) {
            if (member != player) {
               member.sendPacket(new PartySmallWindowAdd(player, this));
            }
         }

         if (player.hasSummon()) {
            this.broadcastPacket(new ExPartyPetWindowAdd(player.getSummon()));
         }

         if (player.getLevel() > this._partyLvl) {
            this._partyLvl = player.getLevel();
         }

         for (Player memberx : this._members) {
            if (memberx != null) {
               memberx.updateEffectIcons(true);
               Summon summon = memberx.getSummon();
               memberx.broadcastUserInfo();
               if (summon != null) {
                  summon.updateEffectIcons();
               }
            }
         }

         if (this.isInDimensionalRift()) {
            this._dr.partyMemberInvited();
         }

         if (this.isInCommandChannel()) {
            player.sendPacket(ExOpenMPCC.STATIC_PACKET);
         }

         if (this._positionBroadcastTask == null) {
            this._positionBroadcastTask = ThreadPool.scheduleAtFixedRate(() -> {
               if (this._positionPacket == null) {
                  this._positionPacket = new PartyMemberPosition(this);
               } else {
                  this._positionPacket.reuse(this);
               }

               this.broadcastPacket(this._positionPacket);
            }, PARTY_POSITION_BROADCAST_INTERVAL.toMillis() / 2L, PARTY_POSITION_BROADCAST_INTERVAL.toMillis());
         }
      }
   }

   public void removePartyMember(String name, PartyMessageType type) {
      this.removePartyMember(this.getPlayerByName(name), type);
   }

   public void removePartyMember(Player player, PartyMessageType type) {
      if (this._members.contains(player)) {
         boolean isLeader = this.isLeader(player);
         if (!this._disbanding && (this._members.size() == 2 || isLeader && !PlayerConfig.ALT_LEAVE_PARTY_LEADER && type != PartyMessageType.DISCONNECTED)) {
            this.disbandParty();
            return;
         }

         this._members.remove(player);
         this.recalculatePartyLevel();
         if (player.isFestivalParticipant()) {
            SevenSignsFestival.getInstance().updateParticipants(player, this);
         }

         if (player.isInDuel()) {
            DuelManager.getInstance().onRemoveFromParty(player);
         }

         try {
            if (player.isChanneling() && player.getSkillChannelizer().hasChannelized()) {
               player.abortCast();
            } else if (player.isChannelized()) {
               player.getSkillChannelized().abortChannelization();
            }
         } catch (Exception var6) {
            LOGGER.log(Level.WARNING, "removePartyMember: ", (Throwable)var6);
         }

         if (type == PartyMessageType.EXPELLED) {
            player.sendPacket(SystemMessageId.YOU_HAVE_BEEN_EXPELLED_FROM_THE_PARTY);
            SystemMessage msg = new SystemMessage(SystemMessageId.C1_WAS_EXPELLED_FROM_THE_PARTY);
            msg.addString(player.getName());
            this.broadcastPacket(msg);
         } else if (type == PartyMessageType.LEFT || type == PartyMessageType.DISCONNECTED) {
            player.sendPacket(SystemMessageId.YOU_HAVE_WITHDRAWN_FROM_THE_PARTY);
            SystemMessage msg = new SystemMessage(SystemMessageId.C1_HAS_LEFT_THE_PARTY);
            msg.addString(player.getName());
            this.broadcastPacket(msg);
         }

         player.sendPacket(PartySmallWindowDeleteAll.STATIC_PACKET);
         player.setParty(null);
         this.broadcastPacket(new PartySmallWindowDelete(player));
         if (player.hasSummon()) {
            this.broadcastPacket(new ExPartyPetWindowDelete(player.getSummon()));
         }

         if (this.isInDimensionalRift()) {
            this._dr.partyMemberExited(player);
         }

         if (this.isInCommandChannel()) {
            player.sendPacket(new ExCloseMPCC());
         }

         if (!isLeader || this._members.size() <= 1 || !PlayerConfig.ALT_LEAVE_PARTY_LEADER && type != PartyMessageType.DISCONNECTED) {
            if (this._members.size() == 1) {
               if (this.isInCommandChannel()) {
                  if (this._commandChannel.getLeader().getObjectId() == this.getLeader().getObjectId()) {
                     this._commandChannel.disbandChannel();
                  } else {
                     this._commandChannel.removeParty(this);
                  }
               }

               Player leader = this.getLeader();
               if (leader != null) {
                  leader.setParty(null);
                  if (leader.isInDuel()) {
                     DuelManager.getInstance().onRemoveFromParty(leader);
                  }
               }

               if (this._positionBroadcastTask != null) {
                  this._positionBroadcastTask.cancel(false);
                  this._positionBroadcastTask = null;
               }

               this._members.clear();
            }
         } else {
            SystemMessage msg = new SystemMessage(SystemMessageId.C1_HAS_BECOME_THE_PARTY_LEADER);
            msg.addString(this.getLeader().getName());
            this.broadcastPacket(msg);
            this.broadcastToPartyMembersNewLeader();
         }
      }
   }

   public void disbandParty() {
      this._disbanding = true;
      this.broadcastPacket(new SystemMessage(SystemMessageId.THE_PARTY_HAS_DISPERSED));

      for (Player member : this._members) {
         if (member != null) {
            this.removePartyMember(member, PartyMessageType.NONE);
         }
      }
   }

   public void changePartyLeader(String name) {
      this.setLeader(this.getPlayerByName(name));
   }

   @Override
   public void setLeader(Player player) {
      if (player != null && !player.isInDuel()) {
         if (this._members.contains(player)) {
            if (this.isLeader(player)) {
               player.sendPacket(SystemMessageId.SLOW_DOWN_YOU_ARE_ALREADY_THE_PARTY_LEADER);
            } else {
               Player temp = this.getLeader();
               int p1 = this._members.indexOf(player);
               this._members.set(0, player);
               this._members.set(p1, temp);
               SystemMessage msg = new SystemMessage(SystemMessageId.C1_HAS_BECOME_THE_PARTY_LEADER);
               msg.addString(this.getLeader().getName());
               this.broadcastPacket(msg);
               this.broadcastToPartyMembersNewLeader();
               if (this.isInCommandChannel() && this._commandChannel.isLeader(temp)) {
                  this._commandChannel.setLeader(this.getLeader());
                  msg = new SystemMessage(SystemMessageId.COMMAND_CHANNEL_AUTHORITY_HAS_BEEN_TRANSFERRED_TO_C1);
                  msg.addString(this._commandChannel.getLeader().getName());
                  this._commandChannel.broadcastPacket(msg);
               }

               if (player.isInPartyMatchRoom()) {
                  PartyMatchRoomList.getInstance().getPlayerRoom(player).changeLeader(player);
               }
            }
         } else {
            player.sendPacket(SystemMessageId.YOU_MAY_ONLY_TRANSFER_PARTY_LEADERSHIP_TO_ANOTHER_MEMBER_OF_THE_PARTY);
         }
      }
   }

   public Player getPlayerByName(String name) {
      for (Player member : this._members) {
         if (member.getName().equalsIgnoreCase(name)) {
            return member;
         }
      }

      return null;
   }

   public void distributeItem(Player player, Item item) {
      if (item.getId() == 57) {
         this.distributeAdena(player, item.getCount(), player);
         ItemManager.destroyItem(ItemProcessType.LOOT, item, player, null);
      } else {
         Player target = this.getActualLooter(player, item.getId(), false, player);
         target.addItem(ItemProcessType.LOOT, item, player, true);
         if (item.getCount() > 1L) {
            SystemMessage msg = new SystemMessage(SystemMessageId.C1_HAS_OBTAINED_S3_S2);
            msg.addString(target.getName());
            msg.addItemName(item);
            msg.addLong(item.getCount());
            this.broadcastToPartyMembers(target, msg);
         } else {
            SystemMessage msg = new SystemMessage(SystemMessageId.C1_HAS_OBTAINED_S2);
            msg.addString(target.getName());
            msg.addItemName(item);
            this.broadcastToPartyMembers(target, msg);
         }
      }
   }

   public void distributeItem(Player player, int itemId, long itemCount, boolean spoil, Attackable target) {
      if (itemId == 57) {
         this.distributeAdena(player, itemCount, target);
      } else {
         Player looter = this.getActualLooter(player, itemId, spoil, target);
         looter.addItem(spoil ? ItemProcessType.SWEEP : ItemProcessType.LOOT, itemId, itemCount, target, true);
         if (itemCount > 1L) {
            SystemMessage msg = spoil
               ? new SystemMessage(SystemMessageId.C1_HAS_OBTAINED_S3_S2_BY_USING_SWEEPER)
               : new SystemMessage(SystemMessageId.C1_HAS_OBTAINED_S3_S2);
            msg.addString(looter.getName());
            msg.addItemName(itemId);
            msg.addLong(itemCount);
            this.broadcastToPartyMembers(looter, msg);
         } else {
            SystemMessage msg = spoil
               ? new SystemMessage(SystemMessageId.C1_HAS_OBTAINED_S2_BY_USING_SWEEPER)
               : new SystemMessage(SystemMessageId.C1_HAS_OBTAINED_S2);
            msg.addString(looter.getName());
            msg.addItemName(itemId);
            this.broadcastToPartyMembers(looter, msg);
         }
      }
   }

   public void distributeItem(Player player, ItemHolder item, boolean spoil, Attackable target) {
      this.distributeItem(player, item.getId(), item.getCount(), spoil, target);
   }

   public void distributeAdena(Player player, long adena, Creature target) {
      List<Player> toReward = new ArrayList<>();

      for (Player member : this._members) {
         if (LocationUtil.checkIfInRange(PlayerConfig.ALT_PARTY_RANGE, target, member, true)) {
            toReward.add(member);
         }
      }

      if (!toReward.isEmpty() && adena > 0L) {
         long baseShare = adena / toReward.size();
         long remainder = adena % toReward.size();
         Map<Player, Long> adenaDistribution = new HashMap<>();

         for (Player memberx : toReward) {
            adenaDistribution.put(memberx, baseShare);
         }

         if (remainder > 0L) {
            int randomIndex = Rnd.get(toReward.size());

            for (int i = 0; i < remainder; i++) {
               Player memberx = toReward.get((randomIndex + i) % toReward.size());
               adenaDistribution.put(memberx, adenaDistribution.get(memberx) + 1L);
            }
         }

         for (Entry<Player, Long> entry : adenaDistribution.entrySet()) {
            long amount = entry.getValue();
            if (amount > 0L) {
               entry.getKey().addAdena(ItemProcessType.LOOT, amount, player, true);
            }
         }
      }
   }

   public void distributeXpAndSp(double xpRewardValue, double spRewardValue, List<Player> rewardedMembers, int topLvl, long partyDmg, Attackable target) {
      List<Player> validMembers = this.getValidMembers(rewardedMembers, topLvl, target);
      double xpReward = xpRewardValue * this.getExpBonus(validMembers.size());
      double spReward = spRewardValue * this.getSpBonus(validMembers.size());
      int sqLevelSum = 0;
      int averagePartyMemberLevel = 0;

      for (Player member : validMembers) {
         sqLevelSum += member.getLevel() * member.getLevel();
         averagePartyMemberLevel += member.getLevel();
      }

      averagePartyMemberLevel /= Math.max(1, validMembers.size());
      float vitalityPoints = target.getVitalityPoints(averagePartyMemberLevel, partyDmg) * RatesConfig.RATE_PARTY_XP / validMembers.size();
      boolean useVitalityRate = target.useVitalityRate();

      for (Player member : rewardedMembers) {
         if (!member.isDead()) {
            if (validMembers.contains(member)) {
               float penalty = member.hasServitor() ? member.getSummon().asServitor().getExpMultiplier() : 1.0F;
               double sqLevel = member.getLevel() * member.getLevel();
               double preCalculation = sqLevel / sqLevelSum * penalty;
               double addexp = Math.round(member.calcStat(Stat.EXPSP_RATE, xpReward * preCalculation, null, null));
               double addsp = member.calcStat(Stat.EXPSP_RATE, spReward * preCalculation, null, null);
               addexp = this.calculateExpSpPartyCutoff(member.asPlayer(), topLvl, addexp, addsp, useVitalityRate);
               if (addexp > 0.0) {
                  member.updateVitalityPoints(vitalityPoints, true, false);
                  PcCafePointsManager.getInstance().givePcCafePoint(member, addexp);
               }
            } else {
               member.addExpAndSp(0.0, 0.0);
            }
         }
      }
   }

   private double calculateExpSpPartyCutoff(Player player, int topLvl, double addExpValue, double addSpValue, boolean vit) {
      double addExp = addExpValue * ClassBalanceConfig.EXP_AMOUNT_MULTIPLIERS[player.getPlayerClass().getId()];
      double addSp = addSpValue * ClassBalanceConfig.SP_AMOUNT_MULTIPLIERS[player.getPlayerClass().getId()];
      if (player.hasPremiumStatus()) {
         addExp *= PremiumSystemConfig.PREMIUM_RATE_XP;
         addSp *= PremiumSystemConfig.PREMIUM_RATE_SP;
      }

      double xp = addExp;
      if (PlayerConfig.PARTY_XP_CUTOFF_METHOD == PartyExpType.HIGHFIVE) {
         int i = 0;
         int levelDiff = topLvl - player.getLevel();

         for (int[] gap : PlayerConfig.PARTY_XP_CUTOFF_GAPS) {
            if (levelDiff >= gap[0] && levelDiff <= gap[1]) {
               xp = addExp * PlayerConfig.PARTY_XP_CUTOFF_GAP_PERCENTS[i] / 100.0;
               double sp = addSp * PlayerConfig.PARTY_XP_CUTOFF_GAP_PERCENTS[i] / 100.0;
               player.addExpAndSp(xp, sp, vit);
               break;
            }

            i++;
         }
      } else {
         player.addExpAndSp(addExp, addSp, vit);
      }

      return xp;
   }

   public void recalculatePartyLevel() {
      int newLevel = 0;

      for (Player member : this._members) {
         if (member == null) {
            this._members.remove(member);
         } else if (member.getLevel() > newLevel) {
            newLevel = member.getLevel();
         }
      }

      this._partyLvl = newLevel;
   }

   private List<Player> getValidMembers(List<Player> members, int topLvl, Attackable target) {
      List<Player> validMembers = new ArrayList<>();
      switch (PlayerConfig.PARTY_XP_CUTOFF_METHOD) {
         case LEVEL:
            for (Player memberxxxxx : members) {
               if (target.getInstanceId() == memberxxxxx.getInstanceId() && topLvl - memberxxxxx.getLevel() <= PlayerConfig.PARTY_XP_CUTOFF_LEVEL) {
                  validMembers.add(memberxxxxx);
               }
            }
            break;
         case PERCENTAGE:
            int sqLevelSum = 0;

            for (Player memberxxx : members) {
               if (target.getInstanceId() == memberxxx.getInstanceId()) {
                  sqLevelSum += memberxxx.getLevel() * memberxxx.getLevel();
               }
            }

            for (Player memberxxxx : members) {
               int sqLevel = memberxxxx.getLevel() * memberxxxx.getLevel();
               if (target.getInstanceId() == memberxxxx.getInstanceId() && sqLevel * 100 >= sqLevelSum * PlayerConfig.PARTY_XP_CUTOFF_PERCENT) {
                  validMembers.add(memberxxxx);
               }
            }
            break;
         case AUTO:
            int sqLevelSum = 0;

            for (Player memberxx : members) {
               sqLevelSum += memberxx.getLevel() * memberxx.getLevel();
            }

            int i = members.size() - 1;
            if (i < 1) {
               return members;
            }

            if (i >= BONUS_EXP_SP.length) {
               i = BONUS_EXP_SP.length - 1;
            }

            for (Player memberxx : members) {
               int sqLevel = memberxx.getLevel() * memberxx.getLevel();
               if (target.getInstanceId() == memberxx.getInstanceId() && sqLevel >= sqLevelSum / (members.size() * members.size())) {
                  validMembers.add(memberxx);
               }
            }
            break;
         case HIGHFIVE:
            for (Player memberx : members) {
               if (target.getInstanceId() == memberx.getInstanceId()) {
                  validMembers.add(memberx);
               }
            }
            break;
         case NONE:
            for (Player member : members) {
               if (target.getInstanceId() == member.getInstanceId()) {
                  validMembers.add(member);
               }
            }
      }

      return validMembers;
   }

   private double getBaseExpSpBonus(int membersCount) {
      int i = membersCount - 1;
      if (i < 1) {
         return 1.0;
      } else {
         if (i >= BONUS_EXP_SP.length) {
            i = BONUS_EXP_SP.length - 1;
         }

         return BONUS_EXP_SP[i];
      }
   }

   private double getExpBonus(int membersCount) {
      return membersCount < 2 ? this.getBaseExpSpBonus(membersCount) : this.getBaseExpSpBonus(membersCount) * RatesConfig.RATE_PARTY_XP;
   }

   private double getSpBonus(int membersCount) {
      return membersCount < 2 ? this.getBaseExpSpBonus(membersCount) : this.getBaseExpSpBonus(membersCount) * RatesConfig.RATE_PARTY_SP;
   }

   @Override
   public int getLevel() {
      return this._partyLvl;
   }

   public PartyDistributionType getDistributionType() {
      return this._distributionType;
   }

   public boolean isInCommandChannel() {
      return this._commandChannel != null;
   }

   public CommandChannel getCommandChannel() {
      return this._commandChannel;
   }

   public void setCommandChannel(CommandChannel channel) {
      this._commandChannel = channel;
   }

   public boolean isInDimensionalRift() {
      return this._dr != null;
   }

   public void setDimensionalRift(DimensionalRift dr) {
      this._dr = dr;
   }

   public DimensionalRift getDimensionalRift() {
      return this._dr;
   }

   @Override
   public Player getLeader() {
      if (this._members.isEmpty()) {
         return null;
      } else {
         try {
            return this._members.get(0);
         } catch (Exception var3) {
            return null;
         }
      }
   }

   @Override
   public List<Player> getMembers() {
      return this._members;
   }

   public Object getUCState() {
      return this._ucState;
   }

   public void setUCState(Object uc) {
      this._ucState = uc;
   }

   public boolean equals(Party party) {
      return party != null && this.getLeaderObjectId() == party.getLeaderObjectId();
   }
}
