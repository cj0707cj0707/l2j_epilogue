package org.l2jmobius.gameserver.network.clientpackets;

import java.util.Calendar;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.gameserver.LoginServerThread;
import org.l2jmobius.gameserver.cache.HtmCache;
import org.l2jmobius.gameserver.config.GeneralConfig;
import org.l2jmobius.gameserver.config.PlayerConfig;
import org.l2jmobius.gameserver.config.RatesConfig;
import org.l2jmobius.gameserver.config.ServerConfig;
import org.l2jmobius.gameserver.config.custom.FactionSystemConfig;
import org.l2jmobius.gameserver.config.custom.OfflineTradeConfig;
import org.l2jmobius.gameserver.config.custom.PremiumSystemConfig;
import org.l2jmobius.gameserver.config.custom.ScreenWelcomeMessageConfig;
import org.l2jmobius.gameserver.config.custom.WeddingConfig;
import org.l2jmobius.gameserver.data.sql.AnnouncementsTable;
import org.l2jmobius.gameserver.data.sql.ClanHallTable;
import org.l2jmobius.gameserver.data.sql.OfflineTraderTable;
import org.l2jmobius.gameserver.data.xml.AdminData;
import org.l2jmobius.gameserver.data.xml.EnchantItemGroupsData;
import org.l2jmobius.gameserver.data.xml.SkillTreeData;
import org.l2jmobius.gameserver.entity.World;
import org.l2jmobius.gameserver.entity.WorldObject;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.appearance.PlayerAppearance;
import org.l2jmobius.gameserver.entity.actor.enums.creature.Race;
import org.l2jmobius.gameserver.entity.actor.enums.player.IllegalActionPunishmentType;
import org.l2jmobius.gameserver.entity.actor.enums.player.TeleportWhereType;
import org.l2jmobius.gameserver.entity.actor.holders.player.Couple;
import org.l2jmobius.gameserver.entity.clan.Clan;
import org.l2jmobius.gameserver.entity.item.enums.BodyPart;
import org.l2jmobius.gameserver.entity.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.entity.residences.AuctionableHall;
import org.l2jmobius.gameserver.entity.zone.ZoneId;
import org.l2jmobius.gameserver.managers.AntiFeedManager;
import org.l2jmobius.gameserver.managers.CHSiegeManager;
import org.l2jmobius.gameserver.managers.CastleManager;
import org.l2jmobius.gameserver.managers.CoupleManager;
import org.l2jmobius.gameserver.managers.CursedWeaponsManager;
import org.l2jmobius.gameserver.managers.DimensionalRiftManager;
import org.l2jmobius.gameserver.managers.FortManager;
import org.l2jmobius.gameserver.managers.FortSiegeManager;
import org.l2jmobius.gameserver.managers.InstanceManager;
import org.l2jmobius.gameserver.managers.MailManager;
import org.l2jmobius.gameserver.managers.PcCafePointsManager;
import org.l2jmobius.gameserver.managers.PetitionManager;
import org.l2jmobius.gameserver.managers.PunishmentManager;
import org.l2jmobius.gameserver.managers.ServerRestartManager;
import org.l2jmobius.gameserver.managers.SiegeManager;
import org.l2jmobius.gameserver.managers.TerritoryWarManager;
import org.l2jmobius.gameserver.mechanics.punishment.PunishmentAffect;
import org.l2jmobius.gameserver.mechanics.punishment.PunishmentType;
import org.l2jmobius.gameserver.mechanics.script.Quest;
import org.l2jmobius.gameserver.mechanics.script.QuestState;
import org.l2jmobius.gameserver.mechanics.sevensigns.SevenSigns;
import org.l2jmobius.gameserver.mechanics.siege.Castle;
import org.l2jmobius.gameserver.mechanics.siege.Fort;
import org.l2jmobius.gameserver.mechanics.siege.FortSiege;
import org.l2jmobius.gameserver.mechanics.siege.Siege;
import org.l2jmobius.gameserver.mechanics.siege.clanhalls.SiegableHall;
import org.l2jmobius.gameserver.mechanics.skill.CommonSkill;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.network.enums.ChatType;
import org.l2jmobius.gameserver.network.holders.ClientHardwareInfoHolder;
import org.l2jmobius.gameserver.network.serverpackets.ActionFailed;
import org.l2jmobius.gameserver.network.serverpackets.CreatureSay;
import org.l2jmobius.gameserver.network.serverpackets.Die;
import org.l2jmobius.gameserver.network.serverpackets.EtcStatusUpdate;
import org.l2jmobius.gameserver.network.serverpackets.ExBasicActionList;
import org.l2jmobius.gameserver.network.serverpackets.ExBirthdayPopup;
import org.l2jmobius.gameserver.network.serverpackets.ExGetBookMarkInfoPacket;
import org.l2jmobius.gameserver.network.serverpackets.ExNoticePostArrived;
import org.l2jmobius.gameserver.network.serverpackets.ExNotifyPremiumItem;
import org.l2jmobius.gameserver.network.serverpackets.ExShowScreenMessage;
import org.l2jmobius.gameserver.network.serverpackets.ExStorageMaxCount;
import org.l2jmobius.gameserver.network.serverpackets.FriendList;
import org.l2jmobius.gameserver.network.serverpackets.HennaInfo;
import org.l2jmobius.gameserver.network.serverpackets.ItemList;
import org.l2jmobius.gameserver.network.serverpackets.LeaveWorld;
import org.l2jmobius.gameserver.network.serverpackets.NpcHtmlMessage;
import org.l2jmobius.gameserver.network.serverpackets.PledgeShowMemberListAll;
import org.l2jmobius.gameserver.network.serverpackets.PledgeShowMemberListUpdate;
import org.l2jmobius.gameserver.network.serverpackets.PledgeSkillList;
import org.l2jmobius.gameserver.network.serverpackets.PledgeStatusChanged;
import org.l2jmobius.gameserver.network.serverpackets.QuestList;
import org.l2jmobius.gameserver.network.serverpackets.ShortcutInit;
import org.l2jmobius.gameserver.network.serverpackets.SkillCoolTime;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;
import org.l2jmobius.gameserver.network.serverpackets.UserInfo;
import org.l2jmobius.gameserver.network.serverpackets.ValidateLocation;
import org.l2jmobius.gameserver.taskmanagers.GameTimeTaskManager;

public class EnterWorld extends ClientPacket {
   private static final Map<String, ClientHardwareInfoHolder> TRACE_HWINFO = new ConcurrentHashMap<>();
   private final int[][] _tracert = new int[5][4];

   @Override
   protected void readImpl() {
      this.readBytes(32);
      this.readInt();
      this.readInt();
      this.readInt();
      this.readInt();
      this.readBytes(32);
      this.readInt();

      for (int i = 0; i < 5; i++) {
         for (int o = 0; o < 4; o++) {
            this._tracert[i][o] = this.readUnsignedByte();
         }
      }
   }

   @Override
   protected void runImpl() {
      org.l2jmobius.gameserver.network.GameClient client = (org.l2jmobius.gameserver.network.GameClient)this.getClient();
      Player player = client.getPlayer();
      if (player == null) {
         org.l2jmobius.gameserver.network.PacketLogger.warning("EnterWorld failed! player returned 'null'.");
         org.l2jmobius.gameserver.network.Disconnection.of(client).storeAndDeleteWith(LeaveWorld.STATIC_PACKET);
      } else {
         client.setConnectionState(org.l2jmobius.gameserver.network.ConnectionState.IN_GAME);
         String[] adress = new String[5];

         for (int i = 0; i < 5; i++) {
            adress[i] = this._tracert[i][0] + "." + this._tracert[i][1] + "." + this._tracert[i][2] + "." + this._tracert[i][3];
         }

         LoginServerThread.getInstance().sendClientTracert(player.getAccountName(), adress);
         client.setClientTracert(this._tracert);
         player.sendPacket(new UserInfo(player));
         if (GeneralConfig.RESTORE_PLAYER_INSTANCE) {
            player.setInstanceId(InstanceManager.getInstance().getPlayer(player.getObjectId()));
         } else {
            int instanceId = InstanceManager.getInstance().getPlayer(player.getObjectId());
            if (instanceId > 0) {
               InstanceManager.getInstance().getInstance(instanceId).removePlayer(player.getObjectId());
            }
         }

         if (!player.isGM()) {
            player.updatePvpTitleAndColor(false);
         } else {
            if (GeneralConfig.GM_STARTUP_BUILDER_HIDE && AdminData.getInstance().hasAccess("admin_hide", player.getAccessLevel())) {
               player.setHiding(true);
               player.sendSysMessage("hide is default for builder.");
               player.sendSysMessage("FriendAddOff is default for builder.");
               player.sendSysMessage("whisperoff is default for builder.");
            } else {
               if (GeneralConfig.GM_STARTUP_INVULNERABLE && AdminData.getInstance().hasAccess("admin_invul", player.getAccessLevel())) {
                  player.setInvul(true);
               }

               if (GeneralConfig.GM_STARTUP_INVISIBLE && AdminData.getInstance().hasAccess("admin_invisible", player.getAccessLevel())) {
                  player.setInvisible(true);
               }

               if (GeneralConfig.GM_STARTUP_SILENCE && AdminData.getInstance().hasAccess("admin_silence", player.getAccessLevel())) {
                  player.setSilenceMode(true);
               }

               if (GeneralConfig.GM_STARTUP_DIET_MODE && AdminData.getInstance().hasAccess("admin_diet", player.getAccessLevel())) {
                  player.setDietMode(true);
                  player.refreshOverloaded();
               }
            }

            if (GeneralConfig.GM_STARTUP_AUTO_LIST && AdminData.getInstance().hasAccess("admin_gmliston", player.getAccessLevel())) {
               AdminData.getInstance().addGm(player, false);
            } else {
               AdminData.getInstance().addGm(player, true);
            }

            if (GeneralConfig.GM_GIVE_SPECIAL_SKILLS) {
               SkillTreeData.getInstance().addSkills(player, false);
            }

            if (GeneralConfig.GM_GIVE_SPECIAL_AURA_SKILLS) {
               SkillTreeData.getInstance().addSkills(player, true);
            }
         }

         if (player.getCurrentHp() < 0.5) {
            player.setDead(true);
         }

         boolean showClanNotice = false;
         Clan clan = player.getClan();
         if (clan != null) {
            player.sendPacket(new PledgeSkillList(clan));
            this.notifyClanMembers(player);
            this.notifySponsorOrApprentice(player);
            AuctionableHall clanHall = ClanHallTable.getInstance().getClanHallByOwner(clan);
            if (clanHall != null && !clanHall.getPaid()) {
               SystemMessage sm = new SystemMessage(
                  org.l2jmobius.gameserver.network.SystemMessageId.PAYMENT_FOR_YOUR_CLAN_HALL_HAS_NOT_BEEN_MADE_PLEASE_MAKE_PAYMENT_TO_YOUR_CLAN_WAREHOUSE_BY_S1_TOMORROW
               );
               sm.addInt(clanHall.getLease());
               player.sendPacket(sm);
            }

            for (Siege siege : SiegeManager.getInstance().getSieges()) {
               if (siege.isInProgress()) {
                  if (siege.checkIsAttacker(clan)) {
                     player.setSiegeState((byte)1);
                     player.setSiegeSide(siege.getCastle().getResidenceId());
                  } else if (siege.checkIsDefender(clan)) {
                     player.setSiegeState((byte)2);
                     player.setSiegeSide(siege.getCastle().getResidenceId());
                  }
               }
            }

            for (FortSiege siegex : FortSiegeManager.getInstance().getSieges()) {
               if (siegex.isInProgress()) {
                  if (siegex.checkIsAttacker(clan)) {
                     player.setSiegeState((byte)1);
                     player.setSiegeSide(siegex.getFort().getResidenceId());
                  } else if (siegex.checkIsDefender(clan)) {
                     player.setSiegeState((byte)2);
                     player.setSiegeSide(siegex.getFort().getResidenceId());
                  }
               }
            }

            for (SiegableHall hall : CHSiegeManager.getInstance().getConquerableHalls().values()) {
               if (hall.isInSiege() && hall.isRegistered(clan)) {
                  player.setSiegeState((byte)1);
                  player.setSiegeSide(hall.getId());
                  player.setInHideoutSiege(true);
               }
            }

            player.sendPacket(new PledgeShowMemberListAll(clan, player));
            player.sendPacket(new PledgeStatusChanged(clan));
            if (clan.getCastleId() > 0) {
               Castle castle = CastleManager.getInstance().getCastleByOwner(clan);
               if (castle != null) {
                  castle.giveResidentialSkills(player);
               }
            }

            if (clan.getFortId() > 0) {
               Fort fort = FortManager.getInstance().getFortByOwner(clan);
               if (fort != null) {
                  fort.giveResidentialSkills(player);
               }
            }

            showClanNotice = clan.isNoticeEnabled();
         }

         if (TerritoryWarManager.getInstance().getRegisteredTerritoryId(player) > 0) {
            if (TerritoryWarManager.getInstance().isTWInProgress()) {
               player.setSiegeState((byte)1);
            }

            player.setSiegeSide(TerritoryWarManager.getInstance().getRegisteredTerritoryId(player));
         }

         if (SevenSigns.getInstance().isSealValidationPeriod() && SevenSigns.getInstance().getSealOwner(3) != 0) {
            int cabal = SevenSigns.getInstance().getPlayerCabal(player.getObjectId());
            if (cabal != 0) {
               if (cabal == SevenSigns.getInstance().getSealOwner(3)) {
                  player.addSkill(CommonSkill.THE_VICTOR_OF_WAR.getSkill());
               } else {
                  player.addSkill(CommonSkill.THE_VANQUISHED_OF_WAR.getSkill());
               }
            }
         } else {
            player.removeSkill(CommonSkill.THE_VICTOR_OF_WAR.getSkill());
            player.removeSkill(CommonSkill.THE_VANQUISHED_OF_WAR.getSkill());
         }

         if (PlayerConfig.ENABLE_VITALITY && PlayerConfig.RECOVER_VITALITY_ON_RECONNECT) {
            float points = RatesConfig.RATE_RECOVERY_ON_RECONNECT * (float)(System.currentTimeMillis() - player.getLastAccess()) / 60000.0F;
            if (points > 0.0F) {
               player.updateVitalityPoints(points, false, true);
            }
         }

         player.getMacros().sendUpdate();
         player.sendPacket(new ItemList(player, false));
         player.sendPacket(new ExGetBookMarkInfoPacket(player));
         player.sendPacket(new ShortcutInit(player));
         player.sendPacket(ExBasicActionList.STATIC_PACKET);
         player.sendPacket(new HennaInfo(player));
         Quest.playerEnter(player);
         if (FactionSystemConfig.FACTION_SYSTEM_ENABLED) {
            PlayerAppearance appearance = player.getAppearance();
            if (player.isGood()) {
               appearance.setNameColor(FactionSystemConfig.FACTION_GOOD_NAME_COLOR);
               appearance.setTitleColor(FactionSystemConfig.FACTION_GOOD_NAME_COLOR);
               player.sendMessage("Welcome " + player.getName() + ", you are fighting for the " + FactionSystemConfig.FACTION_GOOD_TEAM_NAME + " faction.");
               player.sendPacket(
                  new ExShowScreenMessage(
                     "Welcome " + player.getName() + ", you are fighting for the " + FactionSystemConfig.FACTION_GOOD_TEAM_NAME + " faction.", 10000
                  )
               );
               player.updateUserInfo();
            } else if (player.isEvil()) {
               appearance.setNameColor(FactionSystemConfig.FACTION_EVIL_NAME_COLOR);
               appearance.setTitleColor(FactionSystemConfig.FACTION_EVIL_NAME_COLOR);
               player.sendMessage("Welcome " + player.getName() + ", you are fighting for the " + FactionSystemConfig.FACTION_EVIL_TEAM_NAME + " faction.");
               player.sendPacket(
                  new ExShowScreenMessage(
                     "Welcome " + player.getName() + ", you are fighting for the " + FactionSystemConfig.FACTION_EVIL_TEAM_NAME + " faction.", 10000
                  )
               );
               player.updateUserInfo();
            }
         }

         player.sendPacket(new QuestList(player));
         if (PlayerConfig.PLAYER_SPAWN_PROTECTION > 0) {
            player.setSpawnProtection(true);
         }

         player.spawnMe(player.getX(), player.getY(), player.getZ());
         if (player.isCursedWeaponEquipped()) {
            CursedWeaponsManager.getInstance().getCursedWeapon(player.getCursedWeaponEquippedId()).cursedOnLogin();
         }

         player.updateEffectIcons();
         player.sendPacket(new EtcStatusUpdate(player));
         player.sendPacket(new ExStorageMaxCount(player));
         player.sendPacket(new FriendList(player));
         SystemMessage sm = new SystemMessage(org.l2jmobius.gameserver.network.SystemMessageId.S1_FRIEND_HAS_LOGGED_IN);
         sm.addString(player.getName());

         for (int id : player.getFriendList()) {
            WorldObject obj = World.findObject(id);
            if (obj != null) {
               obj.sendPacket(sm);
            }
         }

         player.sendPacket(org.l2jmobius.gameserver.network.SystemMessageId.WELCOME_TO_THE_WORLD_OF_LINEAGE_II);
         SevenSigns.getInstance().sendCurrentPeriodMsg(player);
         AnnouncementsTable.getInstance().showAnnouncements(player);
         if (ServerConfig.SERVER_RESTART_SCHEDULE_ENABLED && ServerConfig.SERVER_RESTART_SCHEDULE_MESSAGE) {
            player.sendPacket(
               new CreatureSay(
                  null, ChatType.BATTLEFIELD, "[SERVER]", "Next restart is scheduled at " + ServerRestartManager.getInstance().getNextRestartTime() + "."
               )
            );
         }

         if (showClanNotice) {
            NpcHtmlMessage notice = new NpcHtmlMessage();
            notice.setFile(player, "data/html/clanNotice.htm");
            notice.replace("%clan_name%", player.getClan().getName());
            notice.replace("%notice_text%", player.getClan().getNotice().replaceAll("(\r\n|\n)", "<br>"));
            notice.disableValidation();
            player.sendPacket(notice);
         } else if (GeneralConfig.SERVER_NEWS) {
            String serverNews = HtmCache.getInstance().getHtm(player, "data/html/servnews.htm");
            if (serverNews != null) {
               player.sendPacket(new NpcHtmlMessage(serverNews));
            }
         }

         if (PlayerConfig.PETITIONING_ALLOWED) {
            PetitionManager.getInstance().checkPetitionMessages(player);
         }

         if (player.isAlikeDead()) {
            player.sendPacket(new Die(player));
         }

         player.onPlayerEnter();
         player.getInventory().applyItemSkills();
         player.sendSkillList();
         player.sendPacket(new SkillCoolTime(player));

         for (Item i : player.getInventory().getItems()) {
            if (i.isTimeLimitedItem()) {
               i.scheduleLifeTimeTask();
            }

            if (i.isShadowItem() && i.isEquipped()) {
               i.decreaseMana(false);
            }
         }

         for (Item i : player.getWarehouse().getItems()) {
            if (i.isTimeLimitedItem()) {
               i.scheduleLifeTimeTask();
            }
         }

         if (DimensionalRiftManager.getInstance().checkIfInRiftZone(player.getX(), player.getY(), player.getZ(), false)) {
            DimensionalRiftManager.getInstance().teleportToWaitingRoom(player);
         }

         if (player.getClanJoinExpiryTime() > System.currentTimeMillis()) {
            player.sendPacket(
               org.l2jmobius.gameserver.network.SystemMessageId.YOU_HAVE_RECENTLY_BEEN_DISMISSED_FROM_A_CLAN_YOU_ARE_NOT_ALLOWED_TO_JOIN_ANOTHER_CLAN_FOR_24_HOURS
            );
         }

         if (player.getInventory().getItemByItemId(9819) != null) {
            Fort fort = FortManager.getInstance().getFort(player);
            if (fort != null) {
               FortSiegeManager.getInstance().dropCombatFlag(player, fort.getResidenceId());
            } else {
               BodyPart bodyPart = BodyPart.fromItem(player.getInventory().getItemByItemId(9819));
               player.getInventory().unEquipItemInBodySlot(bodyPart);
               player.destroyItem(ItemProcessType.DESTROY, player.getInventory().getItemByItemId(9819), null, true);
            }
         }

         if (!player.isGM() && player.isInsideZone(ZoneId.SIEGE) && (!player.isInSiege() || player.getSiegeState() < 2)) {
            player.teleToLocation(TeleportWhereType.TOWN);
         }

         if (PlayerConfig.OVER_ENCHANT_PROTECTION && !player.isGM()) {
            boolean punish = false;

            for (Item item : player.getInventory().getItems()) {
               if (item.isEquipable()
                  && (
                     item.isWeapon() && item.getEnchantLevel() > EnchantItemGroupsData.getInstance().getMaxWeaponEnchant()
                        || item.getTemplate().getType2() == 2 && item.getEnchantLevel() > EnchantItemGroupsData.getInstance().getMaxAccessoryEnchant()
                        || item.isArmor()
                           && item.getTemplate().getType2() != 2
                           && item.getEnchantLevel() > EnchantItemGroupsData.getInstance().getMaxArmorEnchant()
                  )) {
                  org.l2jmobius.gameserver.network.PacketLogger.info(
                     "Over-enchanted (+" + item.getEnchantLevel() + ") " + item + " has been removed from " + player
                  );
                  player.getInventory().destroyItem(ItemProcessType.DESTROY, item, player, null);
                  punish = true;
               }
            }

            if (punish && PlayerConfig.OVER_ENCHANT_PUNISHMENT != IllegalActionPunishmentType.NONE) {
               player.sendMessage("[Server]: You have over-enchanted items!");
               player.sendMessage("[Server]: Respect our server rules.");
               player.sendPacket(new ExShowScreenMessage("You have over-enchanted items!", 6000));
               PunishmentManager.handleIllegalPlayerAction(player, player.getName() + " has over-enchanted items.", PlayerConfig.OVER_ENCHANT_PUNISHMENT);
            }
         }

         if (player.getInventory().getItemByItemId(8190) != null && !player.isCursedWeaponEquipped()) {
            player.destroyItem(ItemProcessType.DESTROY, player.getInventory().getItemByItemId(8190), null, true);
         }

         if (player.getInventory().getItemByItemId(8689) != null && !player.isCursedWeaponEquipped()) {
            player.destroyItem(ItemProcessType.DESTROY, player.getInventory().getItemByItemId(8689), null, true);
         }

         if (GeneralConfig.ALLOW_MAIL && MailManager.getInstance().hasUnreadPost(player)) {
            player.sendPacket(ExNoticePostArrived.valueOf(false));
         }

         if (ScreenWelcomeMessageConfig.WELCOME_MESSAGE_ENABLED) {
            player.sendPacket(new ExShowScreenMessage(ScreenWelcomeMessageConfig.WELCOME_MESSAGE_TEXT, ScreenWelcomeMessageConfig.WELCOME_MESSAGE_TIME));
         }

         int birthday = player.checkBirthDay();
         if (birthday == 0) {
            QuestState qs = player.getQuestState("CharacterBirthday");
            String nextBirthday = null;
            if (qs != null) {
               nextBirthday = qs.get("Birthday");
            }

            Calendar now = Calendar.getInstance();
            now.setTimeInMillis(System.currentTimeMillis());
            if (nextBirthday == null || Integer.parseInt(nextBirthday) == now.get(1)) {
               player.sendPacket(
                  org.l2jmobius.gameserver.network.SystemMessageId.YOUR_BIRTHDAY_GIFT_HAS_ARRIVED_YOU_CAN_OBTAIN_IT_FROM_THE_GATEKEEPER_IN_ANY_VILLAGE
               );
               player.sendPacket(new ExBirthdayPopup());
            }
         } else if (birthday != -1) {
            sm = new SystemMessage(
               org.l2jmobius.gameserver.network.SystemMessageId.THERE_ARE_S1_DAYS_UNTIL_YOUR_CHARACTER_S_BIRTHDAY_ON_THAT_DAY_YOU_CAN_OBTAIN_A_SPECIAL_GIFT_FROM_THE_GATEKEEPER_IN_ANY_VILLAGE
            );
            sm.addString(Integer.toString(birthday));
            player.sendPacket(sm);
         }

         if (!player.getPremiumItemList().isEmpty()) {
            player.sendPacket(ExNotifyPremiumItem.STATIC_PACKET);
         }

         if ((OfflineTradeConfig.OFFLINE_TRADE_ENABLE || OfflineTradeConfig.OFFLINE_CRAFT_ENABLE) && OfflineTradeConfig.STORE_OFFLINE_TRADE_IN_REALTIME) {
            OfflineTraderTable.getInstance().onTransaction(player, true, false);
         }

         if (player.getVariables().getBoolean("EXPOFF", false)) {
            player.disableExpGain();
            player.sendMessage("Experience gain is disabled.");
         }

         player.broadcastUserInfo();
         player.sendPacket(new ValidateLocation(player));
         player.sendPacket(ActionFailed.STATIC_PACKET);
         if (ServerConfig.HARDWARE_INFO_ENABLED) {
            ThreadPool.schedule(() -> {
               StringBuilder sb = new StringBuilder();

               for (int[] ix : this._tracert) {
                  for (int j : ix) {
                     sb.append(j);
                     sb.append('.');
                  }
               }

               String trace = sb.toString();
               ClientHardwareInfoHolder hwInfo = client.getHardwareInfo();
               if (hwInfo != null) {
                  hwInfo.store(player);
                  TRACE_HWINFO.put(trace, hwInfo);
               } else {
                  hwInfo = TRACE_HWINFO.get(trace);
                  if (hwInfo != null) {
                     hwInfo.store(player);
                     client.setHardwareInfo(hwInfo);
                  } else {
                     String storedInfo = player.getAccountVariables().getString("HWID", "");
                     if (!storedInfo.isEmpty()) {
                        hwInfo = new ClientHardwareInfoHolder(storedInfo);
                        TRACE_HWINFO.put(trace, hwInfo);
                        client.setHardwareInfo(hwInfo);
                     }
                  }
               }

               if (hwInfo != null && PunishmentManager.getInstance().hasPunishment(hwInfo.getMacAddress(), PunishmentAffect.HWID, PunishmentType.BAN)) {
                  org.l2jmobius.gameserver.network.Disconnection.of(client).storeAndDeleteWith(LeaveWorld.STATIC_PACKET);
               } else {
                  if (ServerConfig.KICK_MISSING_HWID && hwInfo == null) {
                     org.l2jmobius.gameserver.network.Disconnection.of(client).storeAndDeleteWith(LeaveWorld.STATIC_PACKET);
                  } else if (ServerConfig.MAX_PLAYERS_PER_HWID > 0) {
                     int count = 0;

                     for (Player plr : World.getPlayers()) {
                        if (plr.isOnlineInt() == 1) {
                           ClientHardwareInfoHolder hwi = plr.getClient().getHardwareInfo();
                           if (hwi != null && hwi.equals(hwInfo)) {
                              count++;
                           }
                        }
                     }

                     if (count > ServerConfig.MAX_PLAYERS_PER_HWID) {
                        org.l2jmobius.gameserver.network.Disconnection.of(client).storeAndDeleteWith(LeaveWorld.STATIC_PACKET);
                     }
                  }
               }
            }, 5000L);
         }

         if (player.getRace() == Race.DARK_ELF) {
            Skill shadowSense = player.getKnownSkill(CommonSkill.SHADOW_SENSE.getId());
            if (shadowSense != null) {
               boolean isNight = GameTimeTaskManager.getInstance().isNight();
               if (isNight) {
                  player.sendPacket(
                     new SystemMessage(org.l2jmobius.gameserver.network.SystemMessageId.IT_IS_NOW_MIDNIGHT_AND_THE_EFFECT_OF_S1_CAN_BE_FELT)
                        .addSkillName(shadowSense)
                  );
               } else {
                  player.sendPacket(
                     new SystemMessage(org.l2jmobius.gameserver.network.SystemMessageId.IT_IS_DAWN_AND_THE_EFFECT_OF_S1_WILL_NOW_DISAPPEAR)
                        .addSkillName(shadowSense)
                  );
               }
            }
         }

         AntiFeedManager.getInstance().removePlayer(4, player);
         player.setEnteredWorld();
         if (WeddingConfig.ALLOW_WEDDING) {
            int playerObjectId = player.getObjectId();

            for (Couple couple : CoupleManager.getInstance().getCouples()) {
               if (couple.getPlayer1Id() == playerObjectId || couple.getPlayer2Id() == playerObjectId) {
                  if (couple.getMaried()) {
                     player.setMarried(true);
                  }

                  player.setCoupleId(couple.getId());
                  if (couple.getPlayer1Id() == playerObjectId) {
                     player.setPartnerId(couple.getPlayer2Id());
                  } else {
                     player.setPartnerId(couple.getPlayer1Id());
                  }
               }
            }

            int partnerId = player.getPartnerId();
            if (partnerId != 0) {
               Player partner = World.getPlayer(partnerId);
               if (partner != null) {
                  partner.sendMessage("Your partner has logged in.");
               }
            }
         }

         if ((player.hasPremiumStatus() || !PremiumSystemConfig.PC_CAFE_ONLY_PREMIUM) && PremiumSystemConfig.PC_CAFE_RETAIL_LIKE) {
            PcCafePointsManager.getInstance().run(player);
         }
      }
   }

   private void notifyClanMembers(Player player) {
      Clan clan = player.getClan();
      if (clan != null) {
         clan.getClanMember(player.getObjectId()).setPlayer(player);
         SystemMessage msg = new SystemMessage(org.l2jmobius.gameserver.network.SystemMessageId.CLAN_MEMBER_S1_HAS_LOGGED_INTO_GAME);
         msg.addString(player.getName());
         clan.broadcastToOtherOnlineMembers(msg, player);
         clan.broadcastToOtherOnlineMembers(new PledgeShowMemberListUpdate(player), player);
      }
   }

   private void notifySponsorOrApprentice(Player player) {
      if (player.getSponsor() != 0) {
         Player sponsor = World.getPlayer(player.getSponsor());
         if (sponsor != null) {
            SystemMessage msg = new SystemMessage(org.l2jmobius.gameserver.network.SystemMessageId.YOUR_APPRENTICE_S1_HAS_LOGGED_IN);
            msg.addString(player.getName());
            sponsor.sendPacket(msg);
         }
      } else if (player.getApprentice() != 0) {
         Player apprentice = World.getPlayer(player.getApprentice());
         if (apprentice != null) {
            SystemMessage msg = new SystemMessage(org.l2jmobius.gameserver.network.SystemMessageId.YOUR_SPONSOR_C1_HAS_LOGGED_IN);
            msg.addString(player.getName());
            apprentice.sendPacket(msg);
         }
      }
   }
}
