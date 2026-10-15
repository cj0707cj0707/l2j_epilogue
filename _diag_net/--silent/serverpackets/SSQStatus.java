package org.l2jmobius.gameserver.network.serverpackets;

import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.mechanics.sevensigns.SevenSigns;
import org.l2jmobius.gameserver.mechanics.sevensigns.SevenSignsFestival;
import org.l2jmobius.gameserver.util.StatSet;

public class SSQStatus extends ServerPacket {
   private final int _objectId;
   private final int _page;

   public SSQStatus(int objectId, int recordPage) {
      this._objectId = objectId;
      this._page = recordPage;
   }

   @Override
   public void writeImpl(org.l2jmobius.gameserver.network.GameClient client, WriteBuffer buffer) {
      int winningCabal = SevenSigns.getInstance().getCabalHighestScore();
      int totalDawnMembers = SevenSigns.getInstance().getTotalMembers(2);
      int totalDuskMembers = SevenSigns.getInstance().getTotalMembers(1);
      org.l2jmobius.gameserver.network.ServerPackets.SSQ_STATUS.writeId(this, buffer);
      buffer.writeByte(this._page);
      buffer.writeByte(SevenSigns.getInstance().getCurrentPeriod());
      int dawnPercent = 0;
      int duskPercent = 0;
      switch (this._page) {
         case 1:
            buffer.writeInt(SevenSigns.getInstance().getCurrentCycle());
            int currentPeriod = SevenSigns.getInstance().getCurrentPeriod();
            switch (currentPeriod) {
               case 0:
                  buffer.writeInt(org.l2jmobius.gameserver.network.SystemMessageId.THIS_IS_THE_INITIAL_PERIOD.getId());
                  break;
               case 1:
                  buffer.writeInt(org.l2jmobius.gameserver.network.SystemMessageId.THIS_IS_A_QUEST_EVENT_PERIOD.getId());
                  break;
               case 2:
                  buffer.writeInt(org.l2jmobius.gameserver.network.SystemMessageId.THIS_IS_A_PERIOD_OF_CALCULATING_STATISTICS_IN_THE_SERVER.getId());
                  break;
               case 3:
                  buffer.writeInt(org.l2jmobius.gameserver.network.SystemMessageId.THIS_IS_THE_SEAL_VALIDATION_PERIOD.getId());
            }

            switch (currentPeriod) {
               case 0:
               case 2:
                  buffer.writeInt(org.l2jmobius.gameserver.network.SystemMessageId.UNTIL_TODAY_AT_6_00_P_M.getId());
                  break;
               case 1:
               case 3:
                  buffer.writeInt(org.l2jmobius.gameserver.network.SystemMessageId.UNTIL_NEXT_MONDAY_AT_6_00_P_M.getId());
            }

            buffer.writeByte(SevenSigns.getInstance().getPlayerCabal(this._objectId));
            buffer.writeByte(SevenSigns.getInstance().getPlayerSeal(this._objectId));
            buffer.writeLong(SevenSigns.getInstance().getPlayerStoneContrib(this._objectId));
            buffer.writeLong(SevenSigns.getInstance().getPlayerAdenaCollect(this._objectId));
            double dawnStoneScore = SevenSigns.getInstance().getCurrentStoneScore(2);
            int dawnFestivalScore = SevenSigns.getInstance().getCurrentFestivalScore(2);
            double duskStoneScore = SevenSigns.getInstance().getCurrentStoneScore(1);
            int duskFestivalScore = SevenSigns.getInstance().getCurrentFestivalScore(1);
            double totalStoneScore = duskStoneScore + dawnStoneScore;
            int duskStoneScoreProp = 0;
            int dawnStoneScoreProp = 0;
            if (totalStoneScore != 0.0) {
               duskStoneScoreProp = Math.round((float)duskStoneScore / (float)totalStoneScore * 500.0F);
               dawnStoneScoreProp = Math.round((float)dawnStoneScore / (float)totalStoneScore * 500.0F);
            }

            int duskTotalScore = SevenSigns.getInstance().getCurrentScore(1);
            int dawnTotalScore = SevenSigns.getInstance().getCurrentScore(2);
            int totalOverallScore = duskTotalScore + dawnTotalScore;
            if (totalOverallScore != 0) {
               dawnPercent = Math.round((float)dawnTotalScore / totalOverallScore * 100.0F);
               duskPercent = Math.round((float)duskTotalScore / totalOverallScore * 100.0F);
            }

            buffer.writeLong(duskStoneScoreProp);
            buffer.writeLong(duskFestivalScore);
            buffer.writeLong(duskTotalScore);
            buffer.writeByte(duskPercent);
            buffer.writeLong(dawnStoneScoreProp);
            buffer.writeLong(dawnFestivalScore);
            buffer.writeLong(dawnTotalScore);
            buffer.writeByte(dawnPercent);
            break;
         case 2:
            buffer.writeShort(1);
            buffer.writeByte(5);

            for (int ix = 0; ix < 5; ix++) {
               buffer.writeByte(ix + 1);
               buffer.writeInt(SevenSignsFestival.FESTIVAL_LEVEL_SCORES[ix]);
               int duskScore = SevenSignsFestival.getInstance().getHighestScore(1, ix);
               int dawnScore = SevenSignsFestival.getInstance().getHighestScore(2, ix);
               buffer.writeLong(duskScore);
               StatSet highScoreData = SevenSignsFestival.getInstance().getHighestScoreData(1, ix);
               String[] partyMembers = highScoreData.getString("members").split(",");
               if (partyMembers != null) {
                  buffer.writeByte(partyMembers.length);

                  for (String partyMember : partyMembers) {
                     buffer.writeString(partyMember);
                  }
               } else {
                  buffer.writeByte(0);
               }

               buffer.writeLong(dawnScore);
               highScoreData = SevenSignsFestival.getInstance().getHighestScoreData(2, ix);
               partyMembers = highScoreData.getString("members").split(",");
               if (partyMembers != null) {
                  buffer.writeByte(partyMembers.length);

                  for (String partyMember : partyMembers) {
                     buffer.writeString(partyMember);
                  }
               } else {
                  buffer.writeByte(0);
               }
            }
            break;
         case 3:
            buffer.writeByte(10);
            buffer.writeByte(35);
            buffer.writeByte(3);

            for (int i = 1; i < 4; i++) {
               int dawnProportion = SevenSigns.getInstance().getSealProportion(i, 2);
               int duskProportion = SevenSigns.getInstance().getSealProportion(i, 1);
               buffer.writeByte(i);
               buffer.writeByte(SevenSigns.getInstance().getSealOwner(i));
               if (totalDuskMembers == 0) {
                  if (totalDawnMembers == 0) {
                     buffer.writeByte(0);
                     buffer.writeByte(0);
                  } else {
                     buffer.writeByte(0);
                     buffer.writeByte(Math.round((float)dawnProportion / totalDawnMembers * 100.0F));
                  }
               } else if (totalDawnMembers == 0) {
                  buffer.writeByte(Math.round((float)duskProportion / totalDuskMembers * 100.0F));
                  buffer.writeByte(0);
               } else {
                  buffer.writeByte(Math.round((float)duskProportion / totalDuskMembers * 100.0F));
                  buffer.writeByte(Math.round((float)dawnProportion / totalDawnMembers * 100.0F));
               }
            }
            break;
         case 4:
            buffer.writeByte(winningCabal);
            buffer.writeByte(3);

            for (int i = 1; i < 4; i++) {
               int dawnProportion = SevenSigns.getInstance().getSealProportion(i, 2);
               int duskProportion = SevenSigns.getInstance().getSealProportion(i, 1);
               dawnPercent = Math.round(dawnProportion / (totalDawnMembers == 0 ? 1.0F : totalDawnMembers) * 100.0F);
               duskPercent = Math.round(duskProportion / (totalDuskMembers == 0 ? 1.0F : totalDuskMembers) * 100.0F);
               int sealOwner = SevenSigns.getInstance().getSealOwner(i);
               buffer.writeByte(i);
               switch (sealOwner) {
                  case 0:
                     switch (winningCabal) {
                        case 0:
                           buffer.writeByte(0);
                           buffer.writeInt(
                              org.l2jmobius.gameserver.network.SystemMessageId.THE_COMPETITION_HAS_ENDED_IN_A_TIE_THEREFORE_NOBODY_HAS_BEEN_AWARDED_THE_SEAL
                                 .getId()
                           );
                           continue;
                        case 1:
                           if (duskPercent >= 35) {
                              buffer.writeByte(1);
                              buffer.writeInt(
                                 org.l2jmobius.gameserver.network.SystemMessageId.ALTHOUGH_THE_SEAL_WAS_NOT_OWNED_SINCE_35_OR_MORE_PEOPLE_HAVE_VOTED.getId()
                              );
                           } else {
                              buffer.writeByte(0);
                              buffer.writeInt(
                                 org.l2jmobius.gameserver.network.SystemMessageId.SINCE_THE_SEAL_WAS_NOT_OWNED_DURING_THE_PREVIOUS_PERIOD_AND_SINCE_LESS_THAN_35_PERCENT_OF_PEOPLE_HAVE_VOTED
                                    .getId()
                              );
                           }
                           continue;
                        case 2:
                           if (dawnPercent >= 35) {
                              buffer.writeByte(2);
                              buffer.writeInt(
                                 org.l2jmobius.gameserver.network.SystemMessageId.ALTHOUGH_THE_SEAL_WAS_NOT_OWNED_SINCE_35_OR_MORE_PEOPLE_HAVE_VOTED.getId()
                              );
                           } else {
                              buffer.writeByte(0);
                              buffer.writeInt(
                                 org.l2jmobius.gameserver.network.SystemMessageId.SINCE_THE_SEAL_WAS_NOT_OWNED_DURING_THE_PREVIOUS_PERIOD_AND_SINCE_LESS_THAN_35_PERCENT_OF_PEOPLE_HAVE_VOTED
                                    .getId()
                              );
                           }
                        default:
                           continue;
                     }
                  case 1:
                     switch (winningCabal) {
                        case 0:
                           if (duskPercent >= 10) {
                              buffer.writeByte(1);
                              buffer.writeInt(
                                 org.l2jmobius.gameserver.network.SystemMessageId.SINCE_THE_SEAL_WAS_OWNED_DURING_THE_PREVIOUS_PERIOD_AND_10_OR_MORE_PEOPLE_HAVE_VOTED
                                    .getId()
                              );
                           } else {
                              buffer.writeByte(0);
                              buffer.writeInt(
                                 org.l2jmobius.gameserver.network.SystemMessageId.THE_COMPETITION_HAS_ENDED_IN_A_TIE_THEREFORE_NOBODY_HAS_BEEN_AWARDED_THE_SEAL
                                    .getId()
                              );
                           }
                           continue;
                        case 1:
                           if (duskPercent >= 10) {
                              buffer.writeByte(sealOwner);
                              buffer.writeInt(
                                 org.l2jmobius.gameserver.network.SystemMessageId.SINCE_THE_SEAL_WAS_OWNED_DURING_THE_PREVIOUS_PERIOD_AND_10_OR_MORE_PEOPLE_HAVE_VOTED
                                    .getId()
                              );
                           } else {
                              buffer.writeByte(0);
                              buffer.writeInt(
                                 org.l2jmobius.gameserver.network.SystemMessageId.ALTHOUGH_THE_SEAL_WAS_OWNED_DURING_THE_PREVIOUS_PERIOD_BECAUSE_LESS_THAN_10_OF_PEOPLE_HAVE_VOTED
                                    .getId()
                              );
                           }
                           continue;
                        case 2:
                           if (dawnPercent >= 35) {
                              buffer.writeByte(2);
                              buffer.writeInt(
                                 org.l2jmobius.gameserver.network.SystemMessageId.ALTHOUGH_THE_SEAL_WAS_NOT_OWNED_SINCE_35_OR_MORE_PEOPLE_HAVE_VOTED.getId()
                              );
                           } else if (duskPercent >= 10) {
                              buffer.writeByte(sealOwner);
                              buffer.writeInt(
                                 org.l2jmobius.gameserver.network.SystemMessageId.SINCE_THE_SEAL_WAS_OWNED_DURING_THE_PREVIOUS_PERIOD_AND_10_OR_MORE_PEOPLE_HAVE_VOTED
                                    .getId()
                              );
                           } else {
                              buffer.writeByte(0);
                              buffer.writeInt(
                                 org.l2jmobius.gameserver.network.SystemMessageId.ALTHOUGH_THE_SEAL_WAS_OWNED_DURING_THE_PREVIOUS_PERIOD_BECAUSE_LESS_THAN_10_OF_PEOPLE_HAVE_VOTED
                                    .getId()
                              );
                           }
                        default:
                           continue;
                     }
                  case 2:
                     switch (winningCabal) {
                        case 0:
                           if (dawnPercent >= 10) {
                              buffer.writeByte(2);
                              buffer.writeInt(
                                 org.l2jmobius.gameserver.network.SystemMessageId.SINCE_THE_SEAL_WAS_OWNED_DURING_THE_PREVIOUS_PERIOD_AND_10_OR_MORE_PEOPLE_HAVE_VOTED
                                    .getId()
                              );
                           } else {
                              buffer.writeByte(0);
                              buffer.writeInt(
                                 org.l2jmobius.gameserver.network.SystemMessageId.THE_COMPETITION_HAS_ENDED_IN_A_TIE_THEREFORE_NOBODY_HAS_BEEN_AWARDED_THE_SEAL
                                    .getId()
                              );
                           }
                           break;
                        case 1:
                           if (duskPercent >= 35) {
                              buffer.writeByte(1);
                              buffer.writeInt(
                                 org.l2jmobius.gameserver.network.SystemMessageId.ALTHOUGH_THE_SEAL_WAS_NOT_OWNED_SINCE_35_OR_MORE_PEOPLE_HAVE_VOTED.getId()
                              );
                           } else if (dawnPercent >= 10) {
                              buffer.writeByte(2);
                              buffer.writeInt(
                                 org.l2jmobius.gameserver.network.SystemMessageId.SINCE_THE_SEAL_WAS_OWNED_DURING_THE_PREVIOUS_PERIOD_AND_10_OR_MORE_PEOPLE_HAVE_VOTED
                                    .getId()
                              );
                           } else {
                              buffer.writeByte(0);
                              buffer.writeInt(
                                 org.l2jmobius.gameserver.network.SystemMessageId.ALTHOUGH_THE_SEAL_WAS_OWNED_DURING_THE_PREVIOUS_PERIOD_BECAUSE_LESS_THAN_10_OF_PEOPLE_HAVE_VOTED
                                    .getId()
                              );
                           }
                           break;
                        case 2:
                           if (dawnPercent >= 10) {
                              buffer.writeByte(sealOwner);
                              buffer.writeInt(
                                 org.l2jmobius.gameserver.network.SystemMessageId.SINCE_THE_SEAL_WAS_OWNED_DURING_THE_PREVIOUS_PERIOD_AND_10_OR_MORE_PEOPLE_HAVE_VOTED
                                    .getId()
                              );
                           } else {
                              buffer.writeByte(0);
                              buffer.writeInt(
                                 org.l2jmobius.gameserver.network.SystemMessageId.ALTHOUGH_THE_SEAL_WAS_OWNED_DURING_THE_PREVIOUS_PERIOD_BECAUSE_LESS_THAN_10_OF_PEOPLE_HAVE_VOTED
                                    .getId()
                              );
                           }
                     }
               }
            }
      }
   }
}
