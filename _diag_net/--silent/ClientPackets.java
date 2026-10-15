package org.l2jmobius.gameserver.network;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;
import java.util.function.Supplier;
import org.l2jmobius.gameserver.config.DevelopmentConfig;
import org.l2jmobius.gameserver.network.clientpackets.Action;
import org.l2jmobius.gameserver.network.clientpackets.AddTradeItem;
import org.l2jmobius.gameserver.network.clientpackets.AllyDismiss;
import org.l2jmobius.gameserver.network.clientpackets.AllyLeave;
import org.l2jmobius.gameserver.network.clientpackets.AnswerTradeRequest;
import org.l2jmobius.gameserver.network.clientpackets.Appearing;
import org.l2jmobius.gameserver.network.clientpackets.AttackRequest;
import org.l2jmobius.gameserver.network.clientpackets.AuthLogin;
import org.l2jmobius.gameserver.network.clientpackets.BypassUserCmd;
import org.l2jmobius.gameserver.network.clientpackets.CannotMoveAnymore;
import org.l2jmobius.gameserver.network.clientpackets.CannotMoveAnymoreInVehicle;
import org.l2jmobius.gameserver.network.clientpackets.CharacterCreate;
import org.l2jmobius.gameserver.network.clientpackets.CharacterDelete;
import org.l2jmobius.gameserver.network.clientpackets.CharacterRestore;
import org.l2jmobius.gameserver.network.clientpackets.CharacterSelect;
import org.l2jmobius.gameserver.network.clientpackets.ClientPacket;
import org.l2jmobius.gameserver.network.clientpackets.DlgAnswer;
import org.l2jmobius.gameserver.network.clientpackets.EnterWorld;
import org.l2jmobius.gameserver.network.clientpackets.FinishRotating;
import org.l2jmobius.gameserver.network.clientpackets.GameGuardReply;
import org.l2jmobius.gameserver.network.clientpackets.Logout;
import org.l2jmobius.gameserver.network.clientpackets.MoveToLocation;
import org.l2jmobius.gameserver.network.clientpackets.MoveWithDelta;
import org.l2jmobius.gameserver.network.clientpackets.MultiSellChoose;
import org.l2jmobius.gameserver.network.clientpackets.NewCharacter;
import org.l2jmobius.gameserver.network.clientpackets.ObserverReturn;
import org.l2jmobius.gameserver.network.clientpackets.ProtocolVersion;
import org.l2jmobius.gameserver.network.clientpackets.RequestAcquireSkill;
import org.l2jmobius.gameserver.network.clientpackets.RequestAcquireSkillInfo;
import org.l2jmobius.gameserver.network.clientpackets.RequestActionUse;
import org.l2jmobius.gameserver.network.clientpackets.RequestAllyCrest;
import org.l2jmobius.gameserver.network.clientpackets.RequestAllyInfo;
import org.l2jmobius.gameserver.network.clientpackets.RequestAnswerFriendInvite;
import org.l2jmobius.gameserver.network.clientpackets.RequestAnswerJoinAlly;
import org.l2jmobius.gameserver.network.clientpackets.RequestAnswerJoinParty;
import org.l2jmobius.gameserver.network.clientpackets.RequestAnswerJoinPledge;
import org.l2jmobius.gameserver.network.clientpackets.RequestBBSwrite;
import org.l2jmobius.gameserver.network.clientpackets.RequestBlock;
import org.l2jmobius.gameserver.network.clientpackets.RequestBuyItem;
import org.l2jmobius.gameserver.network.clientpackets.RequestBuySeed;
import org.l2jmobius.gameserver.network.clientpackets.RequestBypassToServer;
import org.l2jmobius.gameserver.network.clientpackets.RequestChangePetName;
import org.l2jmobius.gameserver.network.clientpackets.RequestConfirmSiegeWaitingList;
import org.l2jmobius.gameserver.network.clientpackets.RequestCrystallizeItem;
import org.l2jmobius.gameserver.network.clientpackets.RequestDeleteMacro;
import org.l2jmobius.gameserver.network.clientpackets.RequestDestroyItem;
import org.l2jmobius.gameserver.network.clientpackets.RequestDismissAlly;
import org.l2jmobius.gameserver.network.clientpackets.RequestDropItem;
import org.l2jmobius.gameserver.network.clientpackets.RequestEnchantItem;
import org.l2jmobius.gameserver.network.clientpackets.RequestEvaluate;
import org.l2jmobius.gameserver.network.clientpackets.RequestFriendDel;
import org.l2jmobius.gameserver.network.clientpackets.RequestFriendInvite;
import org.l2jmobius.gameserver.network.clientpackets.RequestFriendList;
import org.l2jmobius.gameserver.network.clientpackets.RequestGMCommand;
import org.l2jmobius.gameserver.network.clientpackets.RequestGetItemFromPet;
import org.l2jmobius.gameserver.network.clientpackets.RequestGetOffVehicle;
import org.l2jmobius.gameserver.network.clientpackets.RequestGetOnVehicle;
import org.l2jmobius.gameserver.network.clientpackets.RequestGiveItemToPet;
import org.l2jmobius.gameserver.network.clientpackets.RequestGiveNickName;
import org.l2jmobius.gameserver.network.clientpackets.RequestGmList;
import org.l2jmobius.gameserver.network.clientpackets.RequestHennaEquip;
import org.l2jmobius.gameserver.network.clientpackets.RequestHennaItemInfo;
import org.l2jmobius.gameserver.network.clientpackets.RequestHennaItemList;
import org.l2jmobius.gameserver.network.clientpackets.RequestHennaItemRemoveInfo;
import org.l2jmobius.gameserver.network.clientpackets.RequestHennaRemove;
import org.l2jmobius.gameserver.network.clientpackets.RequestHennaRemoveList;
import org.l2jmobius.gameserver.network.clientpackets.RequestItemList;
import org.l2jmobius.gameserver.network.clientpackets.RequestJoinAlly;
import org.l2jmobius.gameserver.network.clientpackets.RequestJoinParty;
import org.l2jmobius.gameserver.network.clientpackets.RequestJoinPledge;
import org.l2jmobius.gameserver.network.clientpackets.RequestJoinSiege;
import org.l2jmobius.gameserver.network.clientpackets.RequestLinkHtml;
import org.l2jmobius.gameserver.network.clientpackets.RequestMagicSkillUse;
import org.l2jmobius.gameserver.network.clientpackets.RequestMakeMacro;
import org.l2jmobius.gameserver.network.clientpackets.RequestMoveToLocationInVehicle;
import org.l2jmobius.gameserver.network.clientpackets.RequestNetPing;
import org.l2jmobius.gameserver.network.clientpackets.RequestOustPartyMember;
import org.l2jmobius.gameserver.network.clientpackets.RequestOustPledgeMember;
import org.l2jmobius.gameserver.network.clientpackets.RequestPackageSend;
import org.l2jmobius.gameserver.network.clientpackets.RequestPackageSendableItemList;
import org.l2jmobius.gameserver.network.clientpackets.RequestPartyMatchConfig;
import org.l2jmobius.gameserver.network.clientpackets.RequestPartyMatchDetail;
import org.l2jmobius.gameserver.network.clientpackets.RequestPartyMatchList;
import org.l2jmobius.gameserver.network.clientpackets.RequestPetGetItem;
import org.l2jmobius.gameserver.network.clientpackets.RequestPetUseItem;
import org.l2jmobius.gameserver.network.clientpackets.RequestPetition;
import org.l2jmobius.gameserver.network.clientpackets.RequestPetitionCancel;
import org.l2jmobius.gameserver.network.clientpackets.RequestPetitionFeedback;
import org.l2jmobius.gameserver.network.clientpackets.RequestPledgeCrest;
import org.l2jmobius.gameserver.network.clientpackets.RequestPledgeExtendedInfo;
import org.l2jmobius.gameserver.network.clientpackets.RequestPledgeInfo;
import org.l2jmobius.gameserver.network.clientpackets.RequestPledgeMemberList;
import org.l2jmobius.gameserver.network.clientpackets.RequestPledgePower;
import org.l2jmobius.gameserver.network.clientpackets.RequestPreviewItem;
import org.l2jmobius.gameserver.network.clientpackets.RequestPrivateStoreBuy;
import org.l2jmobius.gameserver.network.clientpackets.RequestPrivateStoreManageBuy;
import org.l2jmobius.gameserver.network.clientpackets.RequestPrivateStoreManageSell;
import org.l2jmobius.gameserver.network.clientpackets.RequestPrivateStoreQuitBuy;
import org.l2jmobius.gameserver.network.clientpackets.RequestPrivateStoreQuitSell;
import org.l2jmobius.gameserver.network.clientpackets.RequestPrivateStoreSell;
import org.l2jmobius.gameserver.network.clientpackets.RequestQuestAbort;
import org.l2jmobius.gameserver.network.clientpackets.RequestQuestList;
import org.l2jmobius.gameserver.network.clientpackets.RequestRecipeBookDestroy;
import org.l2jmobius.gameserver.network.clientpackets.RequestRecipeBookOpen;
import org.l2jmobius.gameserver.network.clientpackets.RequestRecipeItemMakeInfo;
import org.l2jmobius.gameserver.network.clientpackets.RequestRecipeItemMakeSelf;
import org.l2jmobius.gameserver.network.clientpackets.RequestRecipeShopListSet;
import org.l2jmobius.gameserver.network.clientpackets.RequestRecipeShopMakeInfo;
import org.l2jmobius.gameserver.network.clientpackets.RequestRecipeShopMakeItem;
import org.l2jmobius.gameserver.network.clientpackets.RequestRecipeShopManagePrev;
import org.l2jmobius.gameserver.network.clientpackets.RequestRecipeShopManageQuit;
import org.l2jmobius.gameserver.network.clientpackets.RequestRecipeShopMessageSet;
import org.l2jmobius.gameserver.network.clientpackets.RequestRecordInfo;
import org.l2jmobius.gameserver.network.clientpackets.RequestReplyStartPledgeWar;
import org.l2jmobius.gameserver.network.clientpackets.RequestReplyStopPledgeWar;
import org.l2jmobius.gameserver.network.clientpackets.RequestReplySurrenderPledgeWar;
import org.l2jmobius.gameserver.network.clientpackets.RequestRestart;
import org.l2jmobius.gameserver.network.clientpackets.RequestRestartPoint;
import org.l2jmobius.gameserver.network.clientpackets.RequestSSQStatus;
import org.l2jmobius.gameserver.network.clientpackets.RequestSellItem;
import org.l2jmobius.gameserver.network.clientpackets.RequestSendFriendMsg;
import org.l2jmobius.gameserver.network.clientpackets.RequestSetAllyCrest;
import org.l2jmobius.gameserver.network.clientpackets.RequestSetCastleSiegeTime;
import org.l2jmobius.gameserver.network.clientpackets.RequestSetPledgeCrest;
import org.l2jmobius.gameserver.network.clientpackets.RequestShortcutDel;
import org.l2jmobius.gameserver.network.clientpackets.RequestShortcutReg;
import org.l2jmobius.gameserver.network.clientpackets.RequestShowBoard;
import org.l2jmobius.gameserver.network.clientpackets.RequestShowMiniMap;
import org.l2jmobius.gameserver.network.clientpackets.RequestSiegeAttackerList;
import org.l2jmobius.gameserver.network.clientpackets.RequestSiegeDefenderList;
import org.l2jmobius.gameserver.network.clientpackets.RequestSiegeInfo;
import org.l2jmobius.gameserver.network.clientpackets.RequestSkillCoolTime;
import org.l2jmobius.gameserver.network.clientpackets.RequestSkillList;
import org.l2jmobius.gameserver.network.clientpackets.RequestStartPledgeWar;
import org.l2jmobius.gameserver.network.clientpackets.RequestStopPledgeWar;
import org.l2jmobius.gameserver.network.clientpackets.RequestSurrenderPledgeWar;
import org.l2jmobius.gameserver.network.clientpackets.RequestTargetCanceld;
import org.l2jmobius.gameserver.network.clientpackets.RequestTutorialClientEvent;
import org.l2jmobius.gameserver.network.clientpackets.RequestTutorialLinkHtml;
import org.l2jmobius.gameserver.network.clientpackets.RequestTutorialPassCmdToServer;
import org.l2jmobius.gameserver.network.clientpackets.RequestTutorialQuestionMark;
import org.l2jmobius.gameserver.network.clientpackets.RequestUnEquipItem;
import org.l2jmobius.gameserver.network.clientpackets.RequestWithDrawalParty;
import org.l2jmobius.gameserver.network.clientpackets.RequestWithdrawalPledge;
import org.l2jmobius.gameserver.network.clientpackets.Say2;
import org.l2jmobius.gameserver.network.clientpackets.SendBypassBuildCmd;
import org.l2jmobius.gameserver.network.clientpackets.SendWareHouseDepositList;
import org.l2jmobius.gameserver.network.clientpackets.SendWareHouseWithDrawList;
import org.l2jmobius.gameserver.network.clientpackets.SetPrivateStoreListBuy;
import org.l2jmobius.gameserver.network.clientpackets.SetPrivateStoreListSell;
import org.l2jmobius.gameserver.network.clientpackets.SetPrivateStoreMsgBuy;
import org.l2jmobius.gameserver.network.clientpackets.SetPrivateStoreMsgSell;
import org.l2jmobius.gameserver.network.clientpackets.SnoopQuit;
import org.l2jmobius.gameserver.network.clientpackets.StartRotating;
import org.l2jmobius.gameserver.network.clientpackets.TradeDone;
import org.l2jmobius.gameserver.network.clientpackets.TradeRequest;
import org.l2jmobius.gameserver.network.clientpackets.UseItem;
import org.l2jmobius.gameserver.network.clientpackets.ValidatePosition;

public enum ClientPackets {
   LOGOUT(0, Logout::new, org.l2jmobius.gameserver.network.ConnectionState.AUTHENTICATED, org.l2jmobius.gameserver.network.ConnectionState.IN_GAME),
   ATTACK(1, AttackRequest::new, org.l2jmobius.gameserver.network.ConnectionState.IN_GAME),
   REQUEST_START_PLEDGE_WAR(3, RequestStartPledgeWar::new, org.l2jmobius.gameserver.network.ConnectionState.IN_GAME),
   REQUEST_REPLY_START_PLEDGE(4, RequestReplyStartPledgeWar::new, org.l2jmobius.gameserver.network.ConnectionState.IN_GAME),
   REQUEST_STOP_PLEDGE_WAR(5, RequestStopPledgeWar::new, org.l2jmobius.gameserver.network.ConnectionState.IN_GAME),
   REQUEST_REPLY_STOP_PLEDGE_WAR(6, RequestReplyStopPledgeWar::new, org.l2jmobius.gameserver.network.ConnectionState.IN_GAME),
   REQUEST_SURRENDER_PLEDGE_WAR(7, RequestSurrenderPledgeWar::new, org.l2jmobius.gameserver.network.ConnectionState.IN_GAME),
   REQUEST_REPLY_SURRENDER_PLEDGE_WAR(8, RequestReplySurrenderPledgeWar::new, org.l2jmobius.gameserver.network.ConnectionState.IN_GAME),
   REQUEST_SET_PLEDGE_CREST(9, RequestSetPledgeCrest::new, org.l2jmobius.gameserver.network.ConnectionState.IN_GAME),
   REQUEST_GIVE_NICK_NAME(11, RequestGiveNickName::new, org.l2jmobius.gameserver.network.ConnectionState.IN_GAME),
   CHARACTER_CREATE(12, CharacterCreate::new, org.l2jmobius.gameserver.network.ConnectionState.AUTHENTICATED),
   CHARACTER_DELETE(13, CharacterDelete::new, org.l2jmobius.gameserver.network.ConnectionState.AUTHENTICATED),
   PROTOCOL_VERSION(14, ProtocolVersion::new, org.l2jmobius.gameserver.network.ConnectionState.CONNECTED),
   MOVE_TO_LOCATION(15, MoveToLocation::new, org.l2jmobius.gameserver.network.ConnectionState.IN_GAME),
   ENTER_WORLD(17, EnterWorld::new, org.l2jmobius.gameserver.network.ConnectionState.ENTERING),
   CHARACTER_SELECT(18, CharacterSelect::new, org.l2jmobius.gameserver.network.ConnectionState.AUTHENTICATED),
   NEW_CHARACTER(19, NewCharacter::new, org.l2jmobius.gameserver.network.ConnectionState.AUTHENTICATED),
   REQUEST_ITEM_LIST(20, RequestItemList::new, org.l2jmobius.gameserver.network.ConnectionState.IN_GAME),
   REQUEST_UN_EQUIP_ITEM(22, RequestUnEquipItem::new, org.l2jmobius.gameserver.network.ConnectionState.IN_GAME),
   REQUEST_DROP_ITEM(23, RequestDropItem::new, org.l2jmobius.gameserver.network.ConnectionState.IN_GAME),
   USE_ITEM(25, UseItem::new, org.l2jmobius.gameserver.network.ConnectionState.IN_GAME),
   TRADE_REQUEST(26, TradeRequest::new, org.l2jmobius.gameserver.network.ConnectionState.IN_GAME),
   ADD_TRADE_ITEM(27, AddTradeItem::new, org.l2jmobius.gameserver.network.ConnectionState.IN_GAME),
   TRADE_DONE(28, TradeDone::new, org.l2jmobius.gameserver.network.ConnectionState.IN_GAME),
   ACTION(31, Action::new, org.l2jmobius.gameserver.network.ConnectionState.IN_GAME),
   REQUEST_LINK_HTML(34, RequestLinkHtml::new, org.l2jmobius.gameserver.network.ConnectionState.IN_GAME),
   REQUEST_BYPASS_TO_SERVER(35, RequestBypassToServer::new, org.l2jmobius.gameserver.network.ConnectionState.IN_GAME),
   REQUEST_BBS_WRITE(36, RequestBBSwrite::new, org.l2jmobius.gameserver.network.ConnectionState.IN_GAME),
   REQUEST_JOIN_PLEDGE(38, RequestJoinPledge::new, org.l2jmobius.gameserver.network.ConnectionState.IN_GAME),
   REQUEST_ANSWER_JOIN_PLEDGE(39, RequestAnswerJoinPledge::new, org.l2jmobius.gameserver.network.ConnectionState.IN_GAME),
   REQUEST_WITHDRAWAL_PLEDGE(40, RequestWithdrawalPledge::new, org.l2jmobius.gameserver.network.ConnectionState.IN_GAME),
   REQUEST_OUST_PLEDGE_MEMBER(41, RequestOustPledgeMember::new, org.l2jmobius.gameserver.network.ConnectionState.IN_GAME),
   AUTH_LOGIN(43, AuthLogin::new, org.l2jmobius.gameserver.network.ConnectionState.CONNECTED),
   REQUEST_GET_ITEM_FROM_PET(44, RequestGetItemFromPet::new, org.l2jmobius.gameserver.network.ConnectionState.IN_GAME),
   REQUEST_ALLY_INFO(46, RequestAllyInfo::new, org.l2jmobius.gameserver.network.ConnectionState.IN_GAME),
   REQUEST_CRYSTALLIZE_ITEM(47, RequestCrystallizeItem::new, org.l2jmobius.gameserver.network.ConnectionState.IN_GAME),
   REQUEST_PRIVATE_STORE_MANAGE_SELL(48, RequestPrivateStoreManageSell::new, org.l2jmobius.gameserver.network.ConnectionState.IN_GAME),
   SET_PRIVATE_STORE_LIST_SELL(49, SetPrivateStoreListSell::new, org.l2jmobius.gameserver.network.ConnectionState.IN_GAME),
   ATTACK_REQUEST(50, AttackRequest::new, org.l2jmobius.gameserver.network.ConnectionState.IN_GAME),
   REQUEST_TELEPORT(51, null, org.l2jmobius.gameserver.network.ConnectionState.IN_GAME),
   SOCIAL_ACTION(52, null, org.l2jmobius.gameserver.network.ConnectionState.IN_GAME),
   CHANGE_MOVE_TYPE(53, null, org.l2jmobius.gameserver.network.ConnectionState.IN_GAME),
   CHANGE_WAIT_TYPE(54, null, org.l2jmobius.gameserver.network.ConnectionState.IN_GAME),
   REQUEST_SELL_ITEM(55, RequestSellItem::new, org.l2jmobius.gameserver.network.ConnectionState.IN_GAME),
   REQUEST_MAGIC_SKILL_LIST(56, null, org.l2jmobius.gameserver.network.ConnectionState.IN_GAME),
   REQUEST_MAGIC_SKILL_USE(57, RequestMagicSkillUse::new, org.l2jmobius.gameserver.network.ConnectionState.IN_GAME),
   APPEARING(58, Appearing::new, org.l2jmobius.gameserver.network.ConnectionState.IN_GAME),
   SEND_WARE_HOUSE_DEPOSIT_LIST(59, SendWareHouseDepositList::new, org.l2jmobius.gameserver.network.ConnectionState.IN_GAME),
   SEND_WARE_HOUSE_WITH_DRAW_LIST(60, SendWareHouseWithDrawList::new, org.l2jmobius.gameserver.network.ConnectionState.IN_GAME),
   REQUEST_SHORT_CUT_REG(61, RequestShortcutReg::new, org.l2jmobius.gameserver.network.ConnectionState.IN_GAME),
   REQUEST_SHORT_CUT_DEL(63, RequestShortcutDel::new, org.l2jmobius.gameserver.network.ConnectionState.IN_GAME),
   REQUEST_BUY_ITEM(64, RequestBuyItem::new, org.l2jmobius.gameserver.network.ConnectionState.IN_GAME),
   REQUEST_JOIN_PARTY(66, RequestJoinParty::new, org.l2jmobius.gameserver.network.ConnectionState.IN_GAME),
   REQUEST_ANSWER_JOIN_PARTY(67, RequestAnswerJoinParty::new, org.l2jmobius.gameserver.network.ConnectionState.IN_GAME),
   REQUEST_WITH_DRAWAL_PARTY(68, RequestWithDrawalParty::new, org.l2jmobius.gameserver.network.ConnectionState.IN_GAME),
   REQUEST_OUST_PARTY_MEMBER(69, RequestOustPartyMember::new, org.l2jmobius.gameserver.network.ConnectionState.IN_GAME),
   CANNOT_MOVE_ANYMORE(71, CannotMoveAnymore::new, org.l2jmobius.gameserver.network.ConnectionState.IN_GAME),
   REQUEST_TARGET_CANCELD(72, RequestTargetCanceld::new, org.l2jmobius.gameserver.network.ConnectionState.IN_GAME),
   SAY2(73, Say2::new, org.l2jmobius.gameserver.network.ConnectionState.IN_GAME),
   REQUEST_PLEDGE_MEMBER_LIST(77, RequestPledgeMemberList::new, org.l2jmobius.gameserver.network.ConnectionState.IN_GAME),
   REQUEST_MAGIC_LIST(79, null, org.l2jmobius.gameserver.network.ConnectionState.IN_GAME),
   REQUEST_SKILL_LIST(80, RequestSkillList::new, org.l2jmobius.gameserver.network.ConnectionState.IN_GAME),
   MOVE_WITH_DELTA(82, MoveWithDelta::new, org.l2jmobius.gameserver.network.ConnectionState.IN_GAME),
   REQUEST_GET_ON_VEHICLE(83, RequestGetOnVehicle::new, org.l2jmobius.gameserver.network.ConnectionState.IN_GAME),
   REQUEST_GET_OFF_VEHICLE(84, RequestGetOffVehicle::new, org.l2jmobius.gameserver.network.ConnectionState.IN_GAME),
   ANSWER_TRADE_REQUEST(85, AnswerTradeRequest::new, org.l2jmobius.gameserver.network.ConnectionState.IN_GAME),
   REQUEST_ACTION_USE(86, RequestActionUse::new, org.l2jmobius.gameserver.network.ConnectionState.IN_GAME),
   REQUEST_RESTART(87, RequestRestart::new, org.l2jmobius.gameserver.network.ConnectionState.IN_GAME),
   VALIDATE_POSITION(89, ValidatePosition::new, org.l2jmobius.gameserver.network.ConnectionState.IN_GAME),
   START_ROTATING(91, StartRotating::new, org.l2jmobius.gameserver.network.ConnectionState.IN_GAME),
   FINISH_ROTATING(92, FinishRotating::new, org.l2jmobius.gameserver.network.ConnectionState.IN_GAME),
   REQUEST_SHOW_BOARD(94, RequestShowBoard::new, org.l2jmobius.gameserver.network.ConnectionState.IN_GAME),
   REQUEST_ENCHANT_ITEM(95, RequestEnchantItem::new, org.l2jmobius.gameserver.network.ConnectionState.IN_GAME),
   REQUEST_DESTROY_ITEM(96, RequestDestroyItem::new, org.l2jmobius.gameserver.network.ConnectionState.IN_GAME),
   REQUEST_QUEST_LIST(98, RequestQuestList::new, org.l2jmobius.gameserver.network.ConnectionState.IN_GAME),
   REQUEST_QUEST_ABORT(99, RequestQuestAbort::new, org.l2jmobius.gameserver.network.ConnectionState.IN_GAME),
   REQUEST_PLEDGE_INFO(101, RequestPledgeInfo::new, org.l2jmobius.gameserver.network.ConnectionState.IN_GAME),
   REQUEST_PLEDGE_EXTENDED_INFO(102, RequestPledgeExtendedInfo::new, org.l2jmobius.gameserver.network.ConnectionState.IN_GAME),
   REQUEST_PLEDGE_CREST(103, RequestPledgeCrest::new, org.l2jmobius.gameserver.network.ConnectionState.IN_GAME),
   REQUEST_SEND_FRIEND_MSG(107, RequestSendFriendMsg::new, org.l2jmobius.gameserver.network.ConnectionState.IN_GAME),
   REQUEST_SHOW_MINI_MAP(108, RequestShowMiniMap::new, org.l2jmobius.gameserver.network.ConnectionState.IN_GAME),
   REQUEST_RECORD_INFO(110, RequestRecordInfo::new, org.l2jmobius.gameserver.network.ConnectionState.IN_GAME),
   REQUEST_HENNA_EQUIP(111, RequestHennaEquip::new, org.l2jmobius.gameserver.network.ConnectionState.IN_GAME),
   REQUEST_HENNA_REMOVE_LIST(112, RequestHennaRemoveList::new, org.l2jmobius.gameserver.network.ConnectionState.IN_GAME),
   REQUEST_HENNA_ITEM_REMOVE_INFO(113, RequestHennaItemRemoveInfo::new, org.l2jmobius.gameserver.network.ConnectionState.IN_GAME),
   REQUEST_HENNA_REMOVE(114, RequestHennaRemove::new, org.l2jmobius.gameserver.network.ConnectionState.IN_GAME),
   REQUEST_ACQUIRE_SKILL_INFO(115, RequestAcquireSkillInfo::new, org.l2jmobius.gameserver.network.ConnectionState.IN_GAME),
   SEND_BYPASS_BUILD_CMD(116, SendBypassBuildCmd::new, org.l2jmobius.gameserver.network.ConnectionState.IN_GAME),
   REQUEST_MOVE_TO_LOCATION_IN_VEHICLE(117, RequestMoveToLocationInVehicle::new, org.l2jmobius.gameserver.network.ConnectionState.IN_GAME),
   CANNOT_MOVE_ANYMORE_IN_VEHICLE(118, CannotMoveAnymoreInVehicle::new, org.l2jmobius.gameserver.network.ConnectionState.IN_GAME),
   REQUEST_FRIEND_INVITE(119, RequestFriendInvite::new, org.l2jmobius.gameserver.network.ConnectionState.IN_GAME),
   REQUEST_ANSWER_FRIEND_INVITE(120, RequestAnswerFriendInvite::new, org.l2jmobius.gameserver.network.ConnectionState.IN_GAME),
   REQUEST_FRIEND_LIST(121, RequestFriendList::new, org.l2jmobius.gameserver.network.ConnectionState.IN_GAME),
   REQUEST_FRIEND_DEL(122, RequestFriendDel::new, org.l2jmobius.gameserver.network.ConnectionState.IN_GAME),
   CHARACTER_RESTORE(123, CharacterRestore::new, org.l2jmobius.gameserver.network.ConnectionState.AUTHENTICATED),
   REQUEST_ACQUIRE_SKILL(124, RequestAcquireSkill::new, org.l2jmobius.gameserver.network.ConnectionState.IN_GAME),
   REQUEST_RESTART_POINT(125, RequestRestartPoint::new, org.l2jmobius.gameserver.network.ConnectionState.IN_GAME),
   REQUEST_GM_COMMAND(126, RequestGMCommand::new, org.l2jmobius.gameserver.network.ConnectionState.IN_GAME),
   REQUEST_PARTY_MATCH_CONFIG(127, RequestPartyMatchConfig::new, org.l2jmobius.gameserver.network.ConnectionState.IN_GAME),
   REQUEST_PARTY_MATCH_LIST(128, RequestPartyMatchList::new, org.l2jmobius.gameserver.network.ConnectionState.IN_GAME),
   REQUEST_PARTY_MATCH_DETAIL(129, RequestPartyMatchDetail::new, org.l2jmobius.gameserver.network.ConnectionState.IN_GAME),
   REQUEST_PRIVATE_STORE_BUY(131, RequestPrivateStoreBuy::new, org.l2jmobius.gameserver.network.ConnectionState.IN_GAME),
   REQUEST_TUTORIAL_LINK_HTML(133, RequestTutorialLinkHtml::new, org.l2jmobius.gameserver.network.ConnectionState.IN_GAME),
   REQUEST_TUTORIAL_PASS_CMD_TO_SERVER(134, RequestTutorialPassCmdToServer::new, org.l2jmobius.gameserver.network.ConnectionState.IN_GAME),
   REQUEST_TUTORIAL_QUESTION_MARK(135, RequestTutorialQuestionMark::new, org.l2jmobius.gameserver.network.ConnectionState.IN_GAME),
   REQUEST_TUTORIAL_CLIENT_EVENT(136, RequestTutorialClientEvent::new, org.l2jmobius.gameserver.network.ConnectionState.IN_GAME),
   REQUEST_PETITION(137, RequestPetition::new, org.l2jmobius.gameserver.network.ConnectionState.IN_GAME),
   REQUEST_PETITION_CANCEL(138, RequestPetitionCancel::new, org.l2jmobius.gameserver.network.ConnectionState.IN_GAME),
   REQUEST_GM_LIST(139, RequestGmList::new, org.l2jmobius.gameserver.network.ConnectionState.IN_GAME),
   REQUEST_JOIN_ALLY(140, RequestJoinAlly::new, org.l2jmobius.gameserver.network.ConnectionState.IN_GAME),
   REQUEST_ANSWER_JOIN_ALLY(141, RequestAnswerJoinAlly::new, org.l2jmobius.gameserver.network.ConnectionState.IN_GAME),
   ALLY_LEAVE(142, AllyLeave::new, org.l2jmobius.gameserver.network.ConnectionState.IN_GAME),
   ALLY_DISMISS(143, AllyDismiss::new, org.l2jmobius.gameserver.network.ConnectionState.IN_GAME),
   REQUEST_DISMISS_ALLY(144, RequestDismissAlly::new, org.l2jmobius.gameserver.network.ConnectionState.IN_GAME),
   REQUEST_SET_ALLY_CREST(145, RequestSetAllyCrest::new, org.l2jmobius.gameserver.network.ConnectionState.IN_GAME),
   REQUEST_ALLY_CREST(146, RequestAllyCrest::new, org.l2jmobius.gameserver.network.ConnectionState.IN_GAME),
   REQUEST_CHANGE_PET_NAME(147, RequestChangePetName::new, org.l2jmobius.gameserver.network.ConnectionState.IN_GAME),
   REQUEST_PET_USE_ITEM(148, RequestPetUseItem::new, org.l2jmobius.gameserver.network.ConnectionState.IN_GAME),
   REQUEST_GIVE_ITEM_TO_PET(149, RequestGiveItemToPet::new, org.l2jmobius.gameserver.network.ConnectionState.IN_GAME),
   REQUEST_PRIVATE_STORE_QUIT_SELL(150, RequestPrivateStoreQuitSell::new, org.l2jmobius.gameserver.network.ConnectionState.IN_GAME),
   SET_PRIVATE_STORE_MSG_SELL(151, SetPrivateStoreMsgSell::new, org.l2jmobius.gameserver.network.ConnectionState.IN_GAME),
   REQUEST_PET_GET_ITEM(152, RequestPetGetItem::new, org.l2jmobius.gameserver.network.ConnectionState.IN_GAME),
   REQUEST_PRIVATE_STORE_MANAGE_BUY(153, RequestPrivateStoreManageBuy::new, org.l2jmobius.gameserver.network.ConnectionState.IN_GAME),
   SET_PRIVATE_STORE_LIST_BUY(154, SetPrivateStoreListBuy::new, org.l2jmobius.gameserver.network.ConnectionState.IN_GAME),
   REQUEST_PRIVATE_STORE_QUIT_BUY(156, RequestPrivateStoreQuitBuy::new, org.l2jmobius.gameserver.network.ConnectionState.IN_GAME),
   SET_PRIVATE_STORE_MSG_BUY(157, SetPrivateStoreMsgBuy::new, org.l2jmobius.gameserver.network.ConnectionState.IN_GAME),
   REQUEST_PRIVATE_STORE_SELL(159, RequestPrivateStoreSell::new, org.l2jmobius.gameserver.network.ConnectionState.IN_GAME),
   SEND_TIME_CHECK_PACKET(160, null, org.l2jmobius.gameserver.network.ConnectionState.IN_GAME),
   REQUEST_SKILL_COOL_TIME(166, RequestSkillCoolTime::new, org.l2jmobius.gameserver.network.ConnectionState.IN_GAME),
   REQUEST_PACKAGE_SENDABLE_ITEM_LIST(167, RequestPackageSendableItemList::new, org.l2jmobius.gameserver.network.ConnectionState.IN_GAME),
   REQUEST_PACKAGE_SEND(168, RequestPackageSend::new, org.l2jmobius.gameserver.network.ConnectionState.IN_GAME),
   REQUEST_BLOCK(169, RequestBlock::new, org.l2jmobius.gameserver.network.ConnectionState.IN_GAME),
   REQUEST_SIEGE_INFO(170, RequestSiegeInfo::new, org.l2jmobius.gameserver.network.ConnectionState.IN_GAME),
   REQUEST_SIEGE_ATTACKER_LIST(171, RequestSiegeAttackerList::new, org.l2jmobius.gameserver.network.ConnectionState.IN_GAME),
   REQUEST_SIEGE_DEFENDER_LIST(172, RequestSiegeDefenderList::new, org.l2jmobius.gameserver.network.ConnectionState.IN_GAME),
   REQUEST_JOIN_SIEGE(173, RequestJoinSiege::new, org.l2jmobius.gameserver.network.ConnectionState.IN_GAME),
   REQUEST_CONFIRM_SIEGE_WAITING_LIST(174, RequestConfirmSiegeWaitingList::new, org.l2jmobius.gameserver.network.ConnectionState.IN_GAME),
   REQUEST_SET_CASTLE_SIEGE_TIME(175, RequestSetCastleSiegeTime::new, org.l2jmobius.gameserver.network.ConnectionState.IN_GAME),
   MULTI_SELL_CHOOSE(176, MultiSellChoose::new, org.l2jmobius.gameserver.network.ConnectionState.IN_GAME),
   NET_PING(177, RequestNetPing::new, org.l2jmobius.gameserver.network.ConnectionState.IN_GAME),
   REQUEST_REMAIN_TIME(178, null, org.l2jmobius.gameserver.network.ConnectionState.IN_GAME),
   BYPASS_USER_CMD(179, BypassUserCmd::new, org.l2jmobius.gameserver.network.ConnectionState.IN_GAME),
   SNOOP_QUIT(180, SnoopQuit::new, org.l2jmobius.gameserver.network.ConnectionState.IN_GAME),
   REQUEST_RECIPE_BOOK_OPEN(181, RequestRecipeBookOpen::new, org.l2jmobius.gameserver.network.ConnectionState.IN_GAME),
   REQUEST_RECIPE_BOOK_DESTROY(182, RequestRecipeBookDestroy::new, org.l2jmobius.gameserver.network.ConnectionState.IN_GAME),
   REQUEST_RECIPE_ITEM_MAKE_INFO(183, RequestRecipeItemMakeInfo::new, org.l2jmobius.gameserver.network.ConnectionState.IN_GAME),
   REQUEST_RECIPE_ITEM_MAKE_SELF(184, RequestRecipeItemMakeSelf::new, org.l2jmobius.gameserver.network.ConnectionState.IN_GAME),
   REQUEST_RECIPE_SHOP_MANAGE_LIST(185, null, org.l2jmobius.gameserver.network.ConnectionState.IN_GAME),
   REQUEST_RECIPE_SHOP_MESSAGE_SET(186, RequestRecipeShopMessageSet::new, org.l2jmobius.gameserver.network.ConnectionState.IN_GAME),
   REQUEST_RECIPE_SHOP_LIST_SET(187, RequestRecipeShopListSet::new, org.l2jmobius.gameserver.network.ConnectionState.IN_GAME),
   REQUEST_RECIPE_SHOP_MANAGE_QUIT(188, RequestRecipeShopManageQuit::new, org.l2jmobius.gameserver.network.ConnectionState.IN_GAME),
   REQUEST_RECIPE_SHOP_MANAGE_CANCEL(189, null, org.l2jmobius.gameserver.network.ConnectionState.IN_GAME),
   REQUEST_RECIPE_SHOP_MAKE_INFO(190, RequestRecipeShopMakeInfo::new, org.l2jmobius.gameserver.network.ConnectionState.IN_GAME),
   REQUEST_RECIPE_SHOP_MAKE_ITEM(191, RequestRecipeShopMakeItem::new, org.l2jmobius.gameserver.network.ConnectionState.IN_GAME),
   REQUEST_RECIPE_SHOP_MANAGE_PREV(192, RequestRecipeShopManagePrev::new, org.l2jmobius.gameserver.network.ConnectionState.IN_GAME),
   OBSERVER_RETURN(193, ObserverReturn::new, org.l2jmobius.gameserver.network.ConnectionState.IN_GAME),
   REQUEST_EVALUATE(194, RequestEvaluate::new, org.l2jmobius.gameserver.network.ConnectionState.IN_GAME),
   REQUEST_HENNA_ITEM_LIST(195, RequestHennaItemList::new, org.l2jmobius.gameserver.network.ConnectionState.IN_GAME),
   REQUEST_HENNA_ITEM_INFO(196, RequestHennaItemInfo::new, org.l2jmobius.gameserver.network.ConnectionState.IN_GAME),
   REQUEST_BUY_SEED(197, RequestBuySeed::new, org.l2jmobius.gameserver.network.ConnectionState.IN_GAME),
   DLG_ANSWER(198, DlgAnswer::new, org.l2jmobius.gameserver.network.ConnectionState.IN_GAME),
   REQUEST_PREVIEW_ITEM(199, RequestPreviewItem::new, org.l2jmobius.gameserver.network.ConnectionState.IN_GAME),
   REQUEST_SSQ_STATUS(200, RequestSSQStatus::new, org.l2jmobius.gameserver.network.ConnectionState.IN_GAME),
   REQUEST_PETITION_FEEDBACK(201, RequestPetitionFeedback::new, org.l2jmobius.gameserver.network.ConnectionState.IN_GAME),
   GAME_GUARD_REPLY(203, GameGuardReply::new, org.l2jmobius.gameserver.network.ConnectionState.IN_GAME),
   REQUEST_PLEDGE_POWER(204, RequestPledgePower::new, org.l2jmobius.gameserver.network.ConnectionState.IN_GAME),
   REQUEST_MAKE_MACRO(205, RequestMakeMacro::new, org.l2jmobius.gameserver.network.ConnectionState.IN_GAME),
   REQUEST_DELETE_MACRO(206, RequestDeleteMacro::new, org.l2jmobius.gameserver.network.ConnectionState.IN_GAME),
   REQUEST_BUY_PROCURE(207, null, org.l2jmobius.gameserver.network.ConnectionState.IN_GAME),
   EX_PACKET(208, null, org.l2jmobius.gameserver.network.ConnectionState.values());

   public static final org.l2jmobius.gameserver.network.ClientPackets[] PACKET_ARRAY;
   private final int _packetId;
   private final Supplier<ClientPacket> _packetSupplier;
   private final Set<org.l2jmobius.gameserver.network.ConnectionState> _connectionStates;

   private ClientPackets(int packetId, Supplier<ClientPacket> packetSupplier, org.l2jmobius.gameserver.network.ConnectionState... connectionStates) {
      if (packetId > 255) {
         throw new IllegalArgumentException("Packet id must not be bigger than 0xFF");
      } else {
         this._packetId = packetId;
         this._packetSupplier = packetSupplier != null ? packetSupplier : () -> null;
         EnumSet<org.l2jmobius.gameserver.network.ConnectionState> states = EnumSet.noneOf(org.l2jmobius.gameserver.network.ConnectionState.class);
         Collections.addAll(states, connectionStates);
         this._connectionStates = states;
      }
   }

   public int getPacketId() {
      return this._packetId;
   }

   public ClientPacket newPacket() {
      ClientPacket packet = this._packetSupplier.get();
      if (DevelopmentConfig.DEBUG_CLIENT_PACKETS) {
         if (packet != null) {
            String name = packet.getClass().getSimpleName();
            if (!DevelopmentConfig.EXCLUDED_DEBUG_PACKETS.contains(name)) {
               org.l2jmobius.gameserver.network.PacketLogger.info("[C] " + name);
            }
         } else if (DevelopmentConfig.DEBUG_UNKNOWN_PACKETS) {
            org.l2jmobius.gameserver.network.PacketLogger.info("[C] 0x" + Integer.toHexString(this._packetId).toUpperCase());
         }
      }

      return packet;
   }

   public Set<org.l2jmobius.gameserver.network.ConnectionState> getConnectionStates() {
      return this._connectionStates;
   }

   static {
      int maxPacketId = Arrays.stream(values()).mapToInt(org.l2jmobius.gameserver.network.ClientPackets::getPacketId).max().orElse(0);
      PACKET_ARRAY = new org.l2jmobius.gameserver.network.ClientPackets[maxPacketId + 1];

      for (org.l2jmobius.gameserver.network.ClientPackets packet : values()) {
         PACKET_ARRAY[packet.getPacketId()] = packet;
      }
   }
}
