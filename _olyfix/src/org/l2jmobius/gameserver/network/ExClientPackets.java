package org.l2jmobius.gameserver.network;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;
import java.util.function.Supplier;
import org.l2jmobius.gameserver.config.DevelopmentConfig;
import org.l2jmobius.gameserver.network.clientpackets.AnswerJoinPartyRoom;
import org.l2jmobius.gameserver.network.clientpackets.BrEventRankerList;
import org.l2jmobius.gameserver.network.clientpackets.ClientPacket;
import org.l2jmobius.gameserver.network.clientpackets.EndScenePlayer;
import org.l2jmobius.gameserver.network.clientpackets.MoveToLocationAirShip;
import org.l2jmobius.gameserver.network.clientpackets.MoveToLocationInAirShip;
import org.l2jmobius.gameserver.network.clientpackets.RequestAllAgitInfo;
import org.l2jmobius.gameserver.network.clientpackets.RequestAllCastleInfo;
import org.l2jmobius.gameserver.network.clientpackets.RequestAllFortressInfo;
import org.l2jmobius.gameserver.network.clientpackets.RequestAskJoinPartyRoom;
import org.l2jmobius.gameserver.network.clientpackets.RequestAutoSoulShot;
import org.l2jmobius.gameserver.network.clientpackets.RequestBidItemAuction;
import org.l2jmobius.gameserver.network.clientpackets.RequestBrBuyProduct;
import org.l2jmobius.gameserver.network.clientpackets.RequestBrGamePoint;
import org.l2jmobius.gameserver.network.clientpackets.RequestBrProductInfo;
import org.l2jmobius.gameserver.network.clientpackets.RequestBrProductList;
import org.l2jmobius.gameserver.network.clientpackets.RequestBrRecentProductList;
import org.l2jmobius.gameserver.network.clientpackets.RequestBuySellUIClose;
import org.l2jmobius.gameserver.network.clientpackets.RequestCancelPostAttachment;
import org.l2jmobius.gameserver.network.clientpackets.RequestChangeNicknameColor;
import org.l2jmobius.gameserver.network.clientpackets.RequestChangePartyLeader;
import org.l2jmobius.gameserver.network.clientpackets.RequestConfirmCancelItem;
import org.l2jmobius.gameserver.network.clientpackets.RequestConfirmGemStone;
import org.l2jmobius.gameserver.network.clientpackets.RequestConfirmRefinerItem;
import org.l2jmobius.gameserver.network.clientpackets.RequestConfirmTargetItem;
import org.l2jmobius.gameserver.network.clientpackets.RequestCursedWeaponList;
import org.l2jmobius.gameserver.network.clientpackets.RequestCursedWeaponLocation;
import org.l2jmobius.gameserver.network.clientpackets.RequestDeleteReceivedPost;
import org.l2jmobius.gameserver.network.clientpackets.RequestDeleteSentPost;
import org.l2jmobius.gameserver.network.clientpackets.RequestDismissPartyRoom;
import org.l2jmobius.gameserver.network.clientpackets.RequestDispel;
import org.l2jmobius.gameserver.network.clientpackets.RequestDominionInfo;
import org.l2jmobius.gameserver.network.clientpackets.RequestDuelAnswerStart;
import org.l2jmobius.gameserver.network.clientpackets.RequestDuelStart;
import org.l2jmobius.gameserver.network.clientpackets.RequestDuelSurrender;
import org.l2jmobius.gameserver.network.clientpackets.RequestExAcceptJoinMPCC;
import org.l2jmobius.gameserver.network.clientpackets.RequestExAskJoinMPCC;
import org.l2jmobius.gameserver.network.clientpackets.RequestExCancelEnchantItem;
import org.l2jmobius.gameserver.network.clientpackets.RequestExChangeName;
import org.l2jmobius.gameserver.network.clientpackets.RequestExCubeGameChangeTeam;
import org.l2jmobius.gameserver.network.clientpackets.RequestExCubeGameReadyAnswer;
import org.l2jmobius.gameserver.network.clientpackets.RequestExEnchantItemAttribute;
import org.l2jmobius.gameserver.network.clientpackets.RequestExEnchantSkill;
import org.l2jmobius.gameserver.network.clientpackets.RequestExEnchantSkillInfo;
import org.l2jmobius.gameserver.network.clientpackets.RequestExEnchantSkillInfoDetail;
import org.l2jmobius.gameserver.network.clientpackets.RequestExEnchantSkillRouteChange;
import org.l2jmobius.gameserver.network.clientpackets.RequestExEnchantSkillSafe;
import org.l2jmobius.gameserver.network.clientpackets.RequestExEnchantSkillUntrain;
import org.l2jmobius.gameserver.network.clientpackets.RequestExFishRanking;
import org.l2jmobius.gameserver.network.clientpackets.RequestExMPCCShowPartyMembersInfo;
import org.l2jmobius.gameserver.network.clientpackets.RequestExMagicSkillUseGround;
import org.l2jmobius.gameserver.network.clientpackets.RequestExOustFromMPCC;
import org.l2jmobius.gameserver.network.clientpackets.RequestExPledgeCrestLarge;
import org.l2jmobius.gameserver.network.clientpackets.RequestExRemoveItemAttribute;
import org.l2jmobius.gameserver.network.clientpackets.RequestExRqItemLink;
import org.l2jmobius.gameserver.network.clientpackets.RequestExSetPledgeCrestLarge;
import org.l2jmobius.gameserver.network.clientpackets.RequestExTryToPutEnchantSupportItem;
import org.l2jmobius.gameserver.network.clientpackets.RequestExTryToPutEnchantTargetItem;
import org.l2jmobius.gameserver.network.clientpackets.RequestExitPartyMatchingWaitingRoom;
import org.l2jmobius.gameserver.network.clientpackets.RequestFortressMapInfo;
import org.l2jmobius.gameserver.network.clientpackets.RequestFortressSiegeInfo;
import org.l2jmobius.gameserver.network.clientpackets.RequestGetBossRecord;
import org.l2jmobius.gameserver.network.clientpackets.RequestGotoLobby;
import org.l2jmobius.gameserver.network.clientpackets.RequestInfoItemAuction;
import org.l2jmobius.gameserver.network.clientpackets.RequestJoinDominionWar;
import org.l2jmobius.gameserver.network.clientpackets.RequestKeyMapping;
import org.l2jmobius.gameserver.network.clientpackets.RequestListPartyMatchingWaitingRoom;
import org.l2jmobius.gameserver.network.clientpackets.RequestManorList;
import org.l2jmobius.gameserver.network.clientpackets.RequestOlympiadMatchList;
import org.l2jmobius.gameserver.network.clientpackets.RequestOlympiadObserverEnd;
import org.l2jmobius.gameserver.network.clientpackets.RequestOustFromPartyRoom;
import org.l2jmobius.gameserver.network.clientpackets.RequestPCCafeCouponUse;
import org.l2jmobius.gameserver.network.clientpackets.RequestPledgeMemberInfo;
import org.l2jmobius.gameserver.network.clientpackets.RequestPledgeMemberPowerInfo;
import org.l2jmobius.gameserver.network.clientpackets.RequestPledgePowerGradeList;
import org.l2jmobius.gameserver.network.clientpackets.RequestPledgeReorganizeMember;
import org.l2jmobius.gameserver.network.clientpackets.RequestPledgeSetAcademyMaster;
import org.l2jmobius.gameserver.network.clientpackets.RequestPledgeSetMemberPowerGrade;
import org.l2jmobius.gameserver.network.clientpackets.RequestPledgeWarList;
import org.l2jmobius.gameserver.network.clientpackets.RequestPostAttachment;
import org.l2jmobius.gameserver.network.clientpackets.RequestPostItemList;
import org.l2jmobius.gameserver.network.clientpackets.RequestProcureCropList;
import org.l2jmobius.gameserver.network.clientpackets.RequestReceivedPost;
import org.l2jmobius.gameserver.network.clientpackets.RequestReceivedPostList;
import org.l2jmobius.gameserver.network.clientpackets.RequestRefine;
import org.l2jmobius.gameserver.network.clientpackets.RequestRefineCancel;
import org.l2jmobius.gameserver.network.clientpackets.RequestRefundItem;
import org.l2jmobius.gameserver.network.clientpackets.RequestRejectPostAttachment;
import org.l2jmobius.gameserver.network.clientpackets.RequestResetNickname;
import org.l2jmobius.gameserver.network.clientpackets.RequestSaveInventoryOrder;
import org.l2jmobius.gameserver.network.clientpackets.RequestSaveKeyMapping;
import org.l2jmobius.gameserver.network.clientpackets.RequestSeedPhase;
import org.l2jmobius.gameserver.network.clientpackets.RequestSendPost;
import org.l2jmobius.gameserver.network.clientpackets.RequestSentPost;
import org.l2jmobius.gameserver.network.clientpackets.RequestSentPostList;
import org.l2jmobius.gameserver.network.clientpackets.RequestSetCrop;
import org.l2jmobius.gameserver.network.clientpackets.RequestSetSeed;
import org.l2jmobius.gameserver.network.clientpackets.RequestWithDrawPremiumItem;
import org.l2jmobius.gameserver.network.clientpackets.RequestWithdrawPartyRoom;
import org.l2jmobius.gameserver.network.clientpackets.RequestWriteHeroWords;
import org.l2jmobius.gameserver.network.clientpackets.SetPrivateStoreWholeMsg;

public enum ExClientPackets {
   REQUEST_GOTO_LOBBY(54, RequestGotoLobby::new, ConnectionState.AUTHENTICATED),
   REQUEST_MANOR_LIST(1, RequestManorList::new, ConnectionState.IN_GAME),
   REQUEST_PROCEDURE_CROP_LIST(2, RequestProcureCropList::new, ConnectionState.IN_GAME),
   REQUEST_SET_SEED(3, RequestSetSeed::new, ConnectionState.IN_GAME),
   REQUEST_SET_CROP(4, RequestSetCrop::new, ConnectionState.IN_GAME),
   REQUEST_WRITE_HERO_WORDS(5, RequestWriteHeroWords::new, ConnectionState.IN_GAME),
   REQUEST_EX_ASK_JOIN_MPCC(6, RequestExAskJoinMPCC::new, ConnectionState.IN_GAME),
   REQUEST_EX_ACCEPT_JOIN_MPCC(7, RequestExAcceptJoinMPCC::new, ConnectionState.IN_GAME),
   REQUEST_EX_OUST_FROM_MPCC(8, RequestExOustFromMPCC::new, ConnectionState.IN_GAME),
   REQUEST_OUST_FROM_PARTY_ROOM(9, RequestOustFromPartyRoom::new, ConnectionState.IN_GAME),
   REQUEST_DISMISS_PARTY_ROOM(10, RequestDismissPartyRoom::new, ConnectionState.IN_GAME),
   REQUEST_WITHDRAW_PARTY_ROOM(11, RequestWithdrawPartyRoom::new, ConnectionState.IN_GAME),
   REQUEST_CHANGE_PARTY_LEADER(12, RequestChangePartyLeader::new, ConnectionState.IN_GAME),
   REQUEST_AUTO_SOULSHOT(13, RequestAutoSoulShot::new, ConnectionState.IN_GAME),
   REQUEST_EX_ENCHANT_SKILL_INFO(14, RequestExEnchantSkillInfo::new, ConnectionState.IN_GAME),
   REQUEST_EX_ENCHANT_SKILL(15, RequestExEnchantSkill::new, ConnectionState.IN_GAME),
   REQUEST_EX_PLEDGE_CREST_LARGE(16, RequestExPledgeCrestLarge::new, ConnectionState.IN_GAME),
   REQUEST_EX_SET_PLEDGE_CREST_LARGE(17, RequestExSetPledgeCrestLarge::new, ConnectionState.IN_GAME),
   REQUEST_PLEDGE_SET_ACADEMY_MASTER(18, RequestPledgeSetAcademyMaster::new, ConnectionState.IN_GAME),
   REQUEST_PLEDGE_POWER_GRADE_LIST(19, RequestPledgePowerGradeList::new, ConnectionState.IN_GAME),
   REQUEST_PLEDGE_MEMBER_POWER_INFO(20, RequestPledgeMemberPowerInfo::new, ConnectionState.IN_GAME),
   REQUEST_PLEDGE_SET_MEMBER_POWER_GRADE(21, RequestPledgeSetMemberPowerGrade::new, ConnectionState.IN_GAME),
   REQUEST_PLEDGE_MEMBER_INFO(22, RequestPledgeMemberInfo::new, ConnectionState.IN_GAME),
   REQUEST_PLEDGE_WAR_LIST(23, RequestPledgeWarList::new, ConnectionState.IN_GAME),
   REQUEST_EX_FISH_RANKING(24, RequestExFishRanking::new, ConnectionState.IN_GAME),
   REQUEST_PCCAFE_COUPON_USE(25, RequestPCCafeCouponUse::new, ConnectionState.IN_GAME),
   REQUEST_SERVER_LOGIN(26, null, ConnectionState.IN_GAME),
   REQUEST_DUEL_START(27, RequestDuelStart::new, ConnectionState.IN_GAME),
   REQUEST_DUAL_ANSWER_START(28, RequestDuelAnswerStart::new, ConnectionState.IN_GAME),
   REQUEST_EX_SET_TUTORIAL(29, null, ConnectionState.IN_GAME),
   REQUEST_EX_RQ_ITEM_LINK(30, RequestExRqItemLink::new, ConnectionState.IN_GAME),
   CANNOT_MOVE_ANYMORE_AIR_SHIP(31, null, ConnectionState.IN_GAME),
   MOVE_TO_LOCATION_IN_AIR_SHIP(32, MoveToLocationInAirShip::new, ConnectionState.IN_GAME),
   REQUEST_KEY_MAPPING(33, RequestKeyMapping::new, ConnectionState.ENTERING, ConnectionState.IN_GAME),
   REQUEST_SAVE_KEY_MAPPING(34, RequestSaveKeyMapping::new, ConnectionState.IN_GAME),
   REQUEST_EX_REMOVE_ITEM_ATTRIBUTE(35, RequestExRemoveItemAttribute::new, ConnectionState.IN_GAME),
   REQUEST_SAVE_INVENTORY_ORDER(36, RequestSaveInventoryOrder::new, ConnectionState.IN_GAME),
   REQUEST_EXIT_PARTY_MATCHING_WAITING_ROOM(37, RequestExitPartyMatchingWaitingRoom::new, ConnectionState.IN_GAME),
   REQUEST_CONFIRM_TARGET_ITEM(38, RequestConfirmTargetItem::new, ConnectionState.IN_GAME),
   REQUEST_CONFIRM_REFINER_ITEM(39, RequestConfirmRefinerItem::new, ConnectionState.IN_GAME),
   REQUEST_CONFIRM_GEMSTONE(40, RequestConfirmGemStone::new, ConnectionState.IN_GAME),
   REQUEST_OLYMPIAD_OBSERVER_END(41, RequestOlympiadObserverEnd::new, ConnectionState.IN_GAME),
   REQUEST_CURSED_WEAPON_LIST(42, RequestCursedWeaponList::new, ConnectionState.IN_GAME),
   REQUEST_CURSED_WEAPON_LOCATION(43, RequestCursedWeaponLocation::new, ConnectionState.IN_GAME),
   REQUEST_PLEDGE_REORGANIZE_MEMBER(44, RequestPledgeReorganizeMember::new, ConnectionState.IN_GAME),
   REQUEST_EX_MPCC_SHOW_PARTY_MEMBERS_INFO(45, RequestExMPCCShowPartyMembersInfo::new, ConnectionState.IN_GAME),
   REQUEST_OLYMPIAD_MATCH_LIST(46, RequestOlympiadMatchList::new, ConnectionState.IN_GAME),
   REQUEST_ASK_JOIN_PARTY_ROOM(47, RequestAskJoinPartyRoom::new, ConnectionState.IN_GAME),
   ANSWER_JOIN_PARTY_ROOM(48, AnswerJoinPartyRoom::new, ConnectionState.IN_GAME),
   REQUEST_LIST_PARTY_MATCHING_WAITING_ROOM(49, RequestListPartyMatchingWaitingRoom::new, ConnectionState.IN_GAME),
   REQUEST_EX_ENCHANT_SKILL_SAFE(50, RequestExEnchantSkillSafe::new, ConnectionState.IN_GAME),
   REQUEST_EX_ENCHANT_SKILL_UNTRAIN(51, RequestExEnchantSkillUntrain::new, ConnectionState.IN_GAME),
   REQUEST_EX_ENCHANT_SKILL_ROUTE_CHANGE(52, RequestExEnchantSkillRouteChange::new, ConnectionState.IN_GAME),
   REQUEST_EX_ENCHANT_ITEM_ATTRIBUTE(53, RequestExEnchantItemAttribute::new, ConnectionState.IN_GAME),
   MOVE_TO_LOCATION_AIR_SHIP(56, MoveToLocationAirShip::new, ConnectionState.IN_GAME),
   REQUEST_BID_ITEM_AUCTION(57, RequestBidItemAuction::new, ConnectionState.IN_GAME),
   REQUEST_INFO_ITEM_AUCTION(58, RequestInfoItemAuction::new, ConnectionState.IN_GAME),
   REQUEST_EX_CHANGE_NAME(59, RequestExChangeName::new, ConnectionState.IN_GAME),
   REQUEST_ALL_CASTLE_INFO(60, RequestAllCastleInfo::new, ConnectionState.IN_GAME),
   REQUEST_ALL_FORTRESS_INFO(61, RequestAllFortressInfo::new, ConnectionState.IN_GAME),
   REQUEST_ALL_AGIT_INGO(62, RequestAllAgitInfo::new, ConnectionState.IN_GAME),
   REQUEST_FORTRESS_SIEGE_INFO(63, RequestFortressSiegeInfo::new, ConnectionState.IN_GAME),
   REQUEST_GET_BOSS_RECORD(64, RequestGetBossRecord::new, ConnectionState.IN_GAME),
   REQUEST_REFINE(65, RequestRefine::new, ConnectionState.IN_GAME),
   REQUEST_CONFIRM_CANCEL_ITEM(66, RequestConfirmCancelItem::new, ConnectionState.IN_GAME),
   REQUEST_REFINE_CANCEL(67, RequestRefineCancel::new, ConnectionState.IN_GAME),
   REQUEST_EX_MAGIC_SKILL_USE_GROUND(68, RequestExMagicSkillUseGround::new, ConnectionState.IN_GAME),
   REQUEST_DUEL_SURRENDER(69, RequestDuelSurrender::new, ConnectionState.IN_GAME),
   REQUEST_EX_ENCHANT_SKILL_INFO_DETAIL(70, RequestExEnchantSkillInfoDetail::new, ConnectionState.IN_GAME),
   REQUEST_FORTRESS_MAP_INFO(72, RequestFortressMapInfo::new, ConnectionState.IN_GAME),
   SET_PRIVATE_STORE_WHOLE_MSG(73, SetPrivateStoreWholeMsg::new, ConnectionState.IN_GAME),
   REQUEST_DISPEL(75, RequestDispel::new, ConnectionState.IN_GAME),
   REQUEST_EX_TRY_TO_PUT_ENCHANT_TARGET_ITEM(76, RequestExTryToPutEnchantTargetItem::new, ConnectionState.IN_GAME),
   REQUEST_EX_TRY_TO_PUT_ENCHANT_SUPPORT_ITEM(77, RequestExTryToPutEnchantSupportItem::new, ConnectionState.IN_GAME),
   REQUEST_EX_CANCEL_ENCHANT_ITEM(78, RequestExCancelEnchantItem::new, ConnectionState.IN_GAME),
   REQUEST_CHANGE_NICKNAME_COLOR(79, RequestChangeNicknameColor::new, ConnectionState.IN_GAME),
   REQUEST_RESET_NICKNAME(80, RequestResetNickname::new, ConnectionState.IN_GAME),
   EX_BOOKMARK_PACKET(81, null, ConnectionState.IN_GAME),
   REQUEST_WITHDRAW_PREMIUM_ITEM(82, RequestWithDrawPremiumItem::new, ConnectionState.IN_GAME),
   REQUEST_EX_JUMP(83, null, ConnectionState.IN_GAME),
   REQUEST_EX_START_SHOW_CRATAE_CUBE_RANK(84, null, ConnectionState.IN_GAME),
   REQUEST_EX_STOP_SHOW_CRATAE_CUBE_RANK(85, null, ConnectionState.IN_GAME),
   NOTIFY_START_MINI_GAME(86, null, ConnectionState.IN_GAME),
   REQUEST_EX_JOIN_DOMINION_WAR(87, RequestJoinDominionWar::new, ConnectionState.IN_GAME),
   REQUEST_EX_DOMINION_INFO(88, RequestDominionInfo::new, ConnectionState.IN_GAME),
   REQUEST_EX_CLEFT_ENTER(89, null, ConnectionState.IN_GAME),
   REQUEST_EX_CUBE_GAME_CHANGE_TEAM(90, RequestExCubeGameChangeTeam::new, ConnectionState.IN_GAME),
   END_SCENE_PLAYER(91, EndScenePlayer::new, ConnectionState.IN_GAME),
   REQUEST_EX_CUBE_GAME_READY_ANSWER(92, RequestExCubeGameReadyAnswer::new, ConnectionState.IN_GAME),
   REQUEST_EX_LIST_MPCC_WAITING(93, null, ConnectionState.IN_GAME),
   REQUEST_EX_MANAGE_MPCC_ROOM(94, null, ConnectionState.IN_GAME),
   REQUEST_EX_JOIN_MPCC_ROOM(95, null, ConnectionState.IN_GAME),
   REQUEST_EX_OUST_FROM_MPCC_ROOM(96, null, ConnectionState.IN_GAME),
   REQUEST_EX_DISMISS_MPCC_ROOM(97, null, ConnectionState.IN_GAME),
   REQUEST_EX_WITHDRAW_MPCC_ROOM(98, null, ConnectionState.IN_GAME),
   REQUEST_SEED_PHASE(99, RequestSeedPhase::new, ConnectionState.IN_GAME),
   REQUEST_EX_MPCC_PARTYMASTER_LIST(100, null, ConnectionState.IN_GAME),
   REQUEST_POST_ITEM_LIST(101, RequestPostItemList::new, ConnectionState.IN_GAME),
   REQUEST_SEND_POST(102, RequestSendPost::new, ConnectionState.IN_GAME),
   REQUEST_RECEIVED_POST_LIST(103, RequestReceivedPostList::new, ConnectionState.IN_GAME),
   REQUEST_DELETE_RECEIVED_POST(104, RequestDeleteReceivedPost::new, ConnectionState.IN_GAME),
   REQUEST_RECEIVED_POST(105, RequestReceivedPost::new, ConnectionState.IN_GAME),
   REQUEST_POST_ATTACHMENT(106, RequestPostAttachment::new, ConnectionState.IN_GAME),
   REQUEST_REJECT_POST_ATTACHMENT(107, RequestRejectPostAttachment::new, ConnectionState.IN_GAME),
   REQUEST_SENT_POST_LIST(108, RequestSentPostList::new, ConnectionState.IN_GAME),
   REQUEST_DELETE_SENT_POST(109, RequestDeleteSentPost::new, ConnectionState.IN_GAME),
   REQUEST_SENT_POST(110, RequestSentPost::new, ConnectionState.IN_GAME),
   REQUEST_CANCEL_SENT_POST(111, RequestCancelPostAttachment::new, ConnectionState.IN_GAME),
   REQUEST_SHOW_NEW_USER_PETITION(112, null, ConnectionState.IN_GAME),
   REQUEST_SHOW_STEP_TWO(113, null, ConnectionState.IN_GAME),
   REQUEST_SHOW_STEP_THREE(114, null, ConnectionState.IN_GAME),
   EX_CONNECT_TO_RAID_SERVER(115, null, ConnectionState.IN_GAME),
   EX_RETURN_FROM_RAID_SERVER(116, null, ConnectionState.IN_GAME),
   REQUEST_REFUND_ITEM(117, RequestRefundItem::new, ConnectionState.IN_GAME),
   REQUEST_BUI_SELL_UI_CLOSE(118, RequestBuySellUIClose::new, ConnectionState.IN_GAME),
   REQUEST_EX_EVENT_MATCH_OBSERVER_END(119, null, ConnectionState.IN_GAME),
   REQUEST_BR_GAME_POINT(120, RequestBrGamePoint::new, ConnectionState.IN_GAME),
   REQUEST_BR_PRODUCT_LIST(121, RequestBrProductList::new, ConnectionState.IN_GAME),
   REQUEST_BR_PRODUCT_INFO(122, RequestBrProductInfo::new, ConnectionState.IN_GAME),
   REQUEST_BR_BUI_PRODUCT(123, RequestBrBuyProduct::new, ConnectionState.IN_GAME),
   REQUEST_BR_RECENT_PRODUCT_LIST(124, RequestBrRecentProductList::new, ConnectionState.IN_GAME),
   BR_EVENT_RANKER_LIST(125, BrEventRankerList::new, ConnectionState.IN_GAME),
   REQUEST_BR_MINI_GAME_LOAD_SCORES(126, null, ConnectionState.IN_GAME),
   REQUEST_BR_MINI_GAME_INSERT_SCORE(127, null, ConnectionState.IN_GAME),
   REQUEST_BR_LECTURE_MARK(128, null, ConnectionState.IN_GAME),
   EX_MAX(129, null, ConnectionState.IN_GAME);

   public static final ExClientPackets[] PACKET_ARRAY;
   private final int _packetId;
   private final Supplier<ClientPacket> _packetSupplier;
   private final Set<ConnectionState> _connectionStates;

   private ExClientPackets(int packetId, Supplier<ClientPacket> packetSupplier, ConnectionState... connectionStates) {
      if (packetId > 65535) {
         throw new IllegalArgumentException("Packet id must not be bigger than 0xFFFF");
      } else {
         this._packetId = packetId;
         this._packetSupplier = packetSupplier != null ? packetSupplier : () -> null;
         EnumSet<ConnectionState> states = EnumSet.noneOf(ConnectionState.class);
         Collections.addAll(states, connectionStates);
         this._connectionStates = states;
      }
   }

   public int getPacketId() {
      return this._packetId;
   }

   public ClientPacket newPacket() {
      ClientPacket packet = this._packetSupplier.get();
      if (DevelopmentConfig.DEBUG_EX_CLIENT_PACKETS) {
         if (packet != null) {
            String name = packet.getClass().getSimpleName();
            if (!DevelopmentConfig.EXCLUDED_DEBUG_PACKETS.contains(name)) {
               PacketLogger.info("[C EX] " + name);
            }
         } else if (DevelopmentConfig.DEBUG_UNKNOWN_PACKETS) {
            PacketLogger.info("[C EX] 0x" + Integer.toHexString(this._packetId).toUpperCase());
         }
      }

      return packet;
   }

   public Set<ConnectionState> getConnectionStates() {
      return this._connectionStates;
   }

   static {
      int maxPacketId = Arrays.stream(values()).mapToInt(ExClientPackets::getPacketId).max().orElse(0);
      PACKET_ARRAY = new ExClientPackets[maxPacketId + 1];

      for (ExClientPackets packet : values()) {
         PACKET_ARRAY[packet.getPacketId()] = packet;
      }
   }
}
