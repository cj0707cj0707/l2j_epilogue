package org.l2jmobius.gameserver.entity.actor.instance;

import java.util.StringTokenizer;
import java.util.logging.Level;
import org.l2jmobius.gameserver.cache.HtmCache;
import org.l2jmobius.gameserver.config.FeatureConfig;
import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.enums.creature.InstanceType;
import org.l2jmobius.gameserver.entity.actor.templates.NpcTemplate;
import org.l2jmobius.gameserver.entity.clan.Clan;
import org.l2jmobius.gameserver.entity.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.mechanics.sevensigns.SevenSigns;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.NpcHtmlMessage;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;

public class SignsPriest extends Npc {
   public SignsPriest(NpcTemplate template) {
      super(template);
      this.setInstanceType(InstanceType.SignsPriest);
   }

   @Override
   public void onBypassFeedback(Player player, String command) {
      if (player.getLastFolkNPC() != null && player.getLastFolkNPC().getObjectId() == this.getObjectId()) {
         if (command.startsWith("SevenSignsDesc")) {
            int val = Integer.parseInt(command.substring(15));
            this.showChatWindow(player, val, null, true);
         } else if (command.startsWith("SevenSigns")) {
            int cabal = 0;
            int stoneType = 0;
            long ancientAdenaAmount = player.getAncientAdena();
            int val = Integer.parseInt(command.substring(11, 12).trim());
            if (command.length() > 12) {
               val = Integer.parseInt(command.substring(11, 13).trim());
            }

            if (command.length() > 13) {
               try {
                  cabal = Integer.parseInt(command.substring(14, 15).trim());
               } catch (Exception var38) {
                  try {
                     cabal = Integer.parseInt(command.substring(13, 14).trim());
                  } catch (Exception var37) {
                     try {
                        StringTokenizer st = new StringTokenizer(command.trim());
                        st.nextToken();
                        cabal = Integer.parseInt(st.nextToken());
                     } catch (Exception var36) {
                        LOGGER.warning("Failed to retrieve cabal from bypass command. NpcId: " + this.getId() + "; Command: " + command);
                     }
                  }
               }
            }

            switch (val) {
               case 2:
                  if (!player.getInventory().validateCapacity(1L)) {
                     player.sendPacket(SystemMessageId.YOUR_INVENTORY_IS_FULL);
                  } else if (!player.reduceAdena(ItemProcessType.FEE, 500L, this, true)) {
                     player.sendPacket(SystemMessageId.YOU_DO_NOT_HAVE_ENOUGH_ADENA);
                  } else {
                     player.getInventory().addItem(ItemProcessType.REWARD, 5707, 1L, player, this);
                     SystemMessage sm = new SystemMessage(SystemMessageId.YOU_HAVE_EARNED_S1);
                     sm.addItemName(5707);
                     player.sendPacket(sm);
                     if (this instanceof DawnPriest) {
                        this.showChatWindow(player, val, "dawn", false);
                     } else {
                        this.showChatWindow(player, val, "dusk", false);
                     }
                  }
                  break;
               case 3:
               case 8:
                  this.showChatWindow(player, val, SevenSigns.getCabalShortName(cabal), false);
                  break;
               case 4:
                  int newSeal = Integer.parseInt(command.substring(15));
                  if (player.getPlayerClass().level() >= 1) {
                     Clan clan = player.getClan();
                     if (cabal == 1 && FeatureConfig.ALT_GAME_CASTLE_DUSK && clan != null && clan.getCastleId() > 0) {
                        this.showChatWindow(player, "data/html/seven_signs/signs_33_dusk_no.htm");
                        return;
                     }

                     if (FeatureConfig.ALT_GAME_CASTLE_DAWN && cabal == 2) {
                        boolean allowJoinDawn = false;
                        if (clan != null && clan.getCastleId() > 0) {
                           allowJoinDawn = true;
                        } else if (player.destroyItemByItemId(ItemProcessType.FEE, FeatureConfig.SSQ_MANORS_AGREEMENT_ID, 1L, this, true)) {
                           allowJoinDawn = true;
                        } else if (player.reduceAdena(ItemProcessType.FEE, FeatureConfig.SSQ_JOIN_DAWN_ADENA_FEE, this, true)) {
                           allowJoinDawn = true;
                        }

                        if (!allowJoinDawn) {
                           this.showChatWindow(player, "data/html/seven_signs/signs_33_dawn_fee.htm");
                           return;
                        }
                     }
                  }

                  SevenSigns.getInstance().setPlayerInfo(player.getObjectId(), cabal, newSeal);
                  if (cabal == 2) {
                     player.sendPacket(SystemMessageId.YOU_WILL_PARTICIPATE_IN_THE_SEVEN_SIGNS_AS_A_MEMBER_OF_THE_LORDS_OF_DAWN);
                  } else {
                     player.sendPacket(SystemMessageId.YOU_WILL_PARTICIPATE_IN_THE_SEVEN_SIGNS_AS_A_MEMBER_OF_THE_REVOLUTIONARIES_OF_DUSK);
                  }

                  switch (newSeal) {
                     case 1:
                        player.sendPacket(SystemMessageId.YOU_VE_CHOSEN_TO_FIGHT_FOR_THE_SEAL_OF_AVARICE_DURING_THIS_QUEST_EVENT_PERIOD);
                        break;
                     case 2:
                        player.sendPacket(SystemMessageId.YOU_VE_CHOSEN_TO_FIGHT_FOR_THE_SEAL_OF_GNOSIS_DURING_THIS_QUEST_EVENT_PERIOD);
                        break;
                     case 3:
                        player.sendPacket(SystemMessageId.YOU_VE_CHOSEN_TO_FIGHT_FOR_THE_SEAL_OF_STRIFE_DURING_THIS_QUEST_EVENT_PERIOD);
                  }

                  this.showChatWindow(player, 4, SevenSigns.getCabalShortName(cabal), false);
                  break;
               case 5:
                  if (this instanceof DawnPriest) {
                     if (SevenSigns.getInstance().getPlayerCabal(player.getObjectId()) == 0) {
                        this.showChatWindow(player, val, "dawn_no", false);
                     } else {
                        this.showChatWindow(player, val, "dawn", false);
                     }
                  } else if (SevenSigns.getInstance().getPlayerCabal(player.getObjectId()) == 0) {
                     this.showChatWindow(player, val, "dusk_no", false);
                  } else {
                     this.showChatWindow(player, val, "dusk", false);
                  }
                  break;
               case 6:
                  stoneType = Integer.parseInt(command.substring(13));
                  Item blueStones = player.getInventory().getItemByItemId(6360);
                  Item greenStones = player.getInventory().getItemByItemId(6361);
                  Item redStones = player.getInventory().getItemByItemId(6362);
                  long blueStoneCount = blueStones == null ? 0L : blueStones.getCount();
                  long greenStoneCount = greenStones == null ? 0L : greenStones.getCount();
                  long redStoneCount = redStones == null ? 0L : redStones.getCount();
                  long contribScore = SevenSigns.getInstance().getPlayerContribScore(player.getObjectId());
                  boolean stonesFound = false;
                  if (contribScore == FeatureConfig.ALT_MAXIMUM_PLAYER_CONTRIB) {
                     player.sendPacket(SystemMessageId.CONTRIBUTION_LEVEL_HAS_EXCEEDED_THE_LIMIT_YOU_MAY_NOT_CONTINUE);
                  } else {
                     long redContribCount = 0L;
                     long greenContribCount = 0L;
                     long blueContribCount = 0L;
                     String contribStoneColor = null;
                     String stoneColorContr = null;
                     long stoneCountContr = 0L;
                     int stoneIdContr = 0;
                     switch (stoneType) {
                        case 1:
                           contribStoneColor = "Blue";
                           stoneColorContr = "blue";
                           stoneIdContr = 6360;
                           stoneCountContr = blueStoneCount;
                           break;
                        case 2:
                           contribStoneColor = "Green";
                           stoneColorContr = "green";
                           stoneIdContr = 6361;
                           stoneCountContr = greenStoneCount;
                           break;
                        case 3:
                           contribStoneColor = "Red";
                           stoneColorContr = "red";
                           stoneIdContr = 6362;
                           stoneCountContr = redStoneCount;
                           break;
                        case 4:
                           redContribCount = (FeatureConfig.ALT_MAXIMUM_PLAYER_CONTRIB - contribScore) / 10L;
                           if (redContribCount > redStoneCount) {
                              redContribCount = redStoneCount;
                           }

                           long contentContr = contribScore + redContribCount * 10L;
                           greenContribCount = (FeatureConfig.ALT_MAXIMUM_PLAYER_CONTRIB - contentContr) / 5L;
                           if (greenContribCount > greenStoneCount) {
                              greenContribCount = greenStoneCount;
                           }

                           contentContr += greenContribCount * 5L;
                           blueContribCount = (FeatureConfig.ALT_MAXIMUM_PLAYER_CONTRIB - contentContr) / 3L;
                           if (blueContribCount > blueStoneCount) {
                              blueContribCount = blueStoneCount;
                           }

                           if (redContribCount > 0L && player.destroyItemByItemId(ItemProcessType.FEE, 6362, redContribCount, this, false)) {
                              stonesFound = true;
                              SystemMessage msg = new SystemMessage(SystemMessageId.S2_S1_HAS_DISAPPEARED);
                              msg.addItemName(6362);
                              msg.addLong(redContribCount);
                              player.sendPacket(msg);
                           }

                           if (greenContribCount > 0L && player.destroyItemByItemId(ItemProcessType.FEE, 6361, greenContribCount, this, false)) {
                              stonesFound = true;
                              SystemMessage msg = new SystemMessage(SystemMessageId.S2_S1_HAS_DISAPPEARED);
                              msg.addItemName(6361);
                              msg.addLong(greenContribCount);
                              player.sendPacket(msg);
                           }

                           if (blueContribCount > 0L && player.destroyItemByItemId(ItemProcessType.FEE, 6360, blueContribCount, this, false)) {
                              stonesFound = true;
                              SystemMessage msg = new SystemMessage(SystemMessageId.S2_S1_HAS_DISAPPEARED);
                              msg.addItemName(6360);
                              msg.addLong(blueContribCount);
                              player.sendPacket(msg);
                           }

                           if (!stonesFound) {
                              if (this instanceof DawnPriest) {
                                 this.showChatWindow(player, val, "dawn_no_stones", false);
                              } else {
                                 this.showChatWindow(player, val, "dusk_no_stones", false);
                              }
                           } else {
                              contribScore = SevenSigns.getInstance()
                                 .addPlayerStoneContrib(player.getObjectId(), blueContribCount, greenContribCount, redContribCount);
                              SystemMessage sm = new SystemMessage(SystemMessageId.YOUR_CONTRIBUTION_SCORE_IS_INCREASED_BY_S1);
                              sm.addLong(contribScore);
                              player.sendPacket(sm);
                              if (this instanceof DawnPriest) {
                                 this.showChatWindow(player, 6, "dawn", false);
                              } else {
                                 this.showChatWindow(player, 6, "dusk", false);
                              }
                           }

                           return;
                     }

                     String pathx;
                     if (this instanceof DawnPriest) {
                        pathx = "data/html/seven_signs/signs_6_dawn_contribute.htm";
                     } else {
                        pathx = "data/html/seven_signs/signs_6_dusk_contribute.htm";
                     }

                     String contentContrx = HtmCache.getInstance().getHtm(player, pathx);
                     if (contentContrx != null) {
                        contentContrx = contentContrx.replace("%contribStoneColor%", contribStoneColor);
                        contentContrx = contentContrx.replace("%stoneColor%", stoneColorContr);
                        contentContrx = contentContrx.replace("%stoneCount%", String.valueOf(stoneCountContr));
                        contentContrx = contentContrx.replace("%stoneItemId%", String.valueOf(stoneIdContr));
                        contentContrx = contentContrx.replace("%objectId%", String.valueOf(this.getObjectId()));
                        NpcHtmlMessage html = new NpcHtmlMessage(this.getObjectId());
                        html.setHtml(contentContrx);
                        player.sendPacket(html);
                     } else {
                        LOGGER.warning("Problem with HTML text " + pathx);
                     }
                  }
                  break;
               case 7:
                  long ancientAdenaConvert = 0L;

                  try {
                     ancientAdenaConvert = Long.parseLong(command.substring(13).trim());
                  } catch (NumberFormatException var39) {
                     this.showChatWindow(player, "data/html/seven_signs/blkmrkt_3.htm");
                     break;
                  } catch (StringIndexOutOfBoundsException var40) {
                     this.showChatWindow(player, "data/html/seven_signs/blkmrkt_3.htm");
                     break;
                  }

                  if (ancientAdenaConvert < 1L) {
                     this.showChatWindow(player, "data/html/seven_signs/blkmrkt_3.htm");
                  } else if (ancientAdenaAmount < ancientAdenaConvert) {
                     this.showChatWindow(player, "data/html/seven_signs/blkmrkt_4.htm");
                  } else {
                     player.reduceAncientAdena(ItemProcessType.SELL, ancientAdenaConvert, this, true);
                     player.addAdena(ItemProcessType.BUY, ancientAdenaConvert, this, true);
                     this.showChatWindow(player, "data/html/seven_signs/blkmrkt_5.htm");
                  }
                  break;
               case 9:
                  int playerCabal = SevenSigns.getInstance().getPlayerCabal(player.getObjectId());
                  int winningCabal = SevenSigns.getInstance().getCabalHighestScore();
                  if (SevenSigns.getInstance().isSealValidationPeriod() && playerCabal == winningCabal) {
                     int ancientAdenaReward = SevenSigns.getInstance().getAncientAdenaReward(player.getObjectId(), true);
                     if (ancientAdenaReward < 3) {
                        if (this instanceof DawnPriest) {
                           this.showChatWindow(player, 9, "dawn_b", false);
                        } else {
                           this.showChatWindow(player, 9, "dusk_b", false);
                        }
                     } else {
                        player.addAncientAdena(ItemProcessType.REWARD, ancientAdenaReward, this, true);
                        if (this instanceof DawnPriest) {
                           this.showChatWindow(player, 9, "dawn_a", false);
                        } else {
                           this.showChatWindow(player, 9, "dusk_a", false);
                        }
                     }
                  }
                  break;
               case 10:
               case 12:
               case 13:
               case 14:
               case 15:
               case 22:
               case 23:
               case 24:
               case 25:
               case 26:
               case 27:
               case 28:
               case 29:
               case 30:
               case 31:
               case 32:
               default:
                  this.showChatWindow(player, val, null, false);
                  break;
               case 11:
                  try {
                     String portInfo = command.substring(14).trim();
                     StringTokenizer st = new StringTokenizer(portInfo);
                     int x = Integer.parseInt(st.nextToken());
                     int y = Integer.parseInt(st.nextToken());
                     int z = Integer.parseInt(st.nextToken());
                     long ancientAdenaCost = Long.parseLong(st.nextToken());
                     if (ancientAdenaCost <= 0L || player.reduceAncientAdena(ItemProcessType.FEE, ancientAdenaCost, this, true)) {
                        player.teleToLocation(x, y, z);
                     }
                  } catch (Exception var42) {
                     LOGGER.log(Level.WARNING, "SevenSigns: Error occurred while teleporting player: " + var42.getMessage(), (Throwable)var42);
                  }
                  break;
               case 16:
                  if (this instanceof DawnPriest) {
                     this.showChatWindow(player, val, "dawn", false);
                  } else {
                     this.showChatWindow(player, val, "dusk", false);
                  }
                  break;
               case 17:
                  stoneType = Integer.parseInt(command.substring(14));
                  int stoneId = 0;
                  long stoneCount = 0L;
                  int stoneValue = 0;
                  String stoneColor = null;
                  switch (stoneType) {
                     case 1:
                        stoneColor = "blue";
                        stoneId = 6360;
                        stoneValue = 3;
                        break;
                     case 2:
                        stoneColor = "green";
                        stoneId = 6361;
                        stoneValue = 5;
                        break;
                     case 3:
                        stoneColor = "red";
                        stoneId = 6362;
                        stoneValue = 10;
                        break;
                     case 4:
                        Item blueStonesAll = player.getInventory().getItemByItemId(6360);
                        Item greenStonesAll = player.getInventory().getItemByItemId(6361);
                        Item redStonesAll = player.getInventory().getItemByItemId(6362);
                        long blueStoneCountAll = blueStonesAll == null ? 0L : blueStonesAll.getCount();
                        long greenStoneCountAll = greenStonesAll == null ? 0L : greenStonesAll.getCount();
                        long redStoneCountAll = redStonesAll == null ? 0L : redStonesAll.getCount();
                        long ancientAdenaRewardAll = 0L;
                        ancientAdenaRewardAll = SevenSigns.calcAncientAdenaReward(blueStoneCountAll, greenStoneCountAll, redStoneCountAll);
                        if (ancientAdenaRewardAll == 0L) {
                           if (this instanceof DawnPriest) {
                              this.showChatWindow(player, 18, "dawn_no_stones", false);
                           } else {
                              this.showChatWindow(player, 18, "dusk_no_stones", false);
                           }

                           return;
                        }

                        if (blueStoneCountAll > 0L) {
                           player.destroyItemByItemId(ItemProcessType.FEE, 6360, blueStoneCountAll, this, true);
                        }

                        if (greenStoneCountAll > 0L) {
                           player.destroyItemByItemId(ItemProcessType.FEE, 6361, greenStoneCountAll, this, true);
                        }

                        if (redStoneCountAll > 0L) {
                           player.destroyItemByItemId(ItemProcessType.FEE, 6362, redStoneCountAll, this, true);
                        }

                        player.addAncientAdena(ItemProcessType.REWARD, ancientAdenaRewardAll, this, true);
                        if (this instanceof DawnPriest) {
                           this.showChatWindow(player, 18, "dawn", false);
                        } else {
                           this.showChatWindow(player, 18, "dusk", false);
                        }

                        return;
                  }

                  Item stoneInstance = player.getInventory().getItemByItemId(stoneId);
                  if (stoneInstance != null) {
                     stoneCount = stoneInstance.getCount();
                  }

                  String path;
                  if (this instanceof DawnPriest) {
                     path = "data/html/seven_signs/signs_17_dawn.htm";
                  } else {
                     path = "data/html/seven_signs/signs_17_dusk.htm";
                  }

                  String content = HtmCache.getInstance().getHtm(player, path);
                  if (content != null) {
                     content = content.replace("%stoneColor%", stoneColor);
                     content = content.replace("%stoneValue%", String.valueOf(stoneValue));
                     content = content.replace("%stoneCount%", String.valueOf(stoneCount));
                     content = content.replace("%stoneItemId%", String.valueOf(stoneId));
                     content = content.replace("%objectId%", String.valueOf(this.getObjectId()));
                     NpcHtmlMessage html = new NpcHtmlMessage(this.getObjectId());
                     html.setHtml(content);
                     player.sendPacket(html);
                  } else {
                     LOGGER.warning("Problem with HTML text data/html/seven_signs/signs_17.htm: " + path);
                  }
                  break;
               case 18:
                  int convertStoneId = Integer.parseInt(command.substring(14, 18));
                  long convertCount = 0L;

                  try {
                     convertCount = Long.parseLong(command.substring(19).trim());
                  } catch (Exception var41) {
                     if (this instanceof DawnPriest) {
                        this.showChatWindow(player, 18, "dawn_failed", false);
                     } else {
                        this.showChatWindow(player, 18, "dusk_failed", false);
                     }
                     break;
                  }

                  Item convertItem = player.getInventory().getItemByItemId(convertStoneId);
                  if (convertItem != null) {
                     long ancientAdenaReward = 0L;
                     long totalCount = convertItem.getCount();
                     if (convertCount <= totalCount && convertCount > 0L) {
                        switch (convertStoneId) {
                           case 6360:
                              ancientAdenaReward = SevenSigns.calcAncientAdenaReward(convertCount, 0L, 0L);
                              break;
                           case 6361:
                              ancientAdenaReward = SevenSigns.calcAncientAdenaReward(0L, convertCount, 0L);
                              break;
                           case 6362:
                              ancientAdenaReward = SevenSigns.calcAncientAdenaReward(0L, 0L, convertCount);
                        }

                        if (player.destroyItemByItemId(ItemProcessType.FEE, convertStoneId, convertCount, this, true)) {
                           player.addAncientAdena(ItemProcessType.REWARD, ancientAdenaReward, this, true);
                           if (this instanceof DawnPriest) {
                              this.showChatWindow(player, 18, "dawn", false);
                           } else {
                              this.showChatWindow(player, 18, "dusk", false);
                           }
                        }
                     } else if (this instanceof DawnPriest) {
                        this.showChatWindow(player, 18, "dawn_low_stones", false);
                     } else {
                        this.showChatWindow(player, 18, "dusk_low_stones", false);
                     }
                  } else if (this instanceof DawnPriest) {
                     this.showChatWindow(player, 18, "dawn_no_stones", false);
                  } else {
                     this.showChatWindow(player, 18, "dusk_no_stones", false);
                  }
                  break;
               case 19:
                  int chosenSeal = Integer.parseInt(command.substring(16));
                  String fileSuffix = SevenSigns.getSealName(chosenSeal, true) + "_" + SevenSigns.getCabalShortName(cabal);
                  this.showChatWindow(player, val, fileSuffix, false);
                  break;
               case 20:
                  StringBuilder contentBuffer = new StringBuilder();
                  if (this instanceof DawnPriest) {
                     contentBuffer.append("<html><body>Priest of Dawn:<br><font color=\"LEVEL\">[ Seal Status ]</font><br>");
                  } else {
                     contentBuffer.append("<html><body>Dusk Priestess:<br><font color=\"LEVEL\">[ Status of the Seals ]</font><br>");
                  }

                  for (int i = 1; i < 4; i++) {
                     int sealOwner = SevenSigns.getInstance().getSealOwner(i);
                     if (sealOwner != 0) {
                        contentBuffer.append("[" + SevenSigns.getSealName(i, false) + ": " + SevenSigns.getCabalName(sealOwner) + "]<br>");
                     } else {
                        contentBuffer.append("[" + SevenSigns.getSealName(i, false) + ": Nothingness]<br>");
                     }
                  }

                  contentBuffer.append("<a action=\"bypass -h npc_" + this.getObjectId() + "_Chat 0\">Go back.</a></body></html>");
                  NpcHtmlMessage html = new NpcHtmlMessage(this.getObjectId());
                  html.setHtml(contentBuffer.toString());
                  player.sendPacket(html);
                  break;
               case 21:
                  int contribStoneId = Integer.parseInt(command.substring(14, 18));
                  Item contribBlueStones = player.getInventory().getItemByItemId(6360);
                  Item contribGreenStones = player.getInventory().getItemByItemId(6361);
                  Item contribRedStones = player.getInventory().getItemByItemId(6362);
                  long contribBlueStoneCount = contribBlueStones == null ? 0L : contribBlueStones.getCount();
                  long contribGreenStoneCount = contribGreenStones == null ? 0L : contribGreenStones.getCount();
                  long contribRedStoneCount = contribRedStones == null ? 0L : contribRedStones.getCount();
                  long score = SevenSigns.getInstance().getPlayerContribScore(player.getObjectId());
                  long contributionCount = 0L;
                  boolean contribStonesFound = false;
                  long redContrib = 0L;
                  long greenContrib = 0L;
                  long blueContrib = 0L;

                  try {
                     contributionCount = Long.parseLong(command.substring(19).trim());
                  } catch (Exception var43) {
                     if (this instanceof DawnPriest) {
                        this.showChatWindow(player, 6, "dawn_failure", false);
                     } else {
                        this.showChatWindow(player, 6, "dusk_failure", false);
                     }
                     break;
                  }

                  switch (contribStoneId) {
                     case 6360:
                        blueContrib = (FeatureConfig.ALT_MAXIMUM_PLAYER_CONTRIB - score) / 3L;
                        if (blueContrib > contribBlueStoneCount) {
                           blueContrib = contributionCount;
                        }
                        break;
                     case 6361:
                        greenContrib = (FeatureConfig.ALT_MAXIMUM_PLAYER_CONTRIB - score) / 5L;
                        if (greenContrib > contribGreenStoneCount) {
                           greenContrib = contributionCount;
                        }
                        break;
                     case 6362:
                        redContrib = (FeatureConfig.ALT_MAXIMUM_PLAYER_CONTRIB - score) / 10L;
                        if (redContrib > contribRedStoneCount) {
                           redContrib = contributionCount;
                        }
                  }

                  if (redContrib > 0L && player.destroyItemByItemId(ItemProcessType.FEE, 6362, redContrib, this, false)) {
                     contribStonesFound = true;
                     SystemMessage msg = new SystemMessage(SystemMessageId.S2_S1_HAS_DISAPPEARED);
                     msg.addItemName(6362);
                     msg.addLong(redContrib);
                     player.sendPacket(msg);
                  }

                  if (greenContrib > 0L && player.destroyItemByItemId(ItemProcessType.FEE, 6361, greenContrib, this, false)) {
                     contribStonesFound = true;
                     SystemMessage msg = new SystemMessage(SystemMessageId.S2_S1_HAS_DISAPPEARED);
                     msg.addItemName(6361);
                     msg.addLong(greenContrib);
                     player.sendPacket(msg);
                  }

                  if (blueContrib > 0L && player.destroyItemByItemId(ItemProcessType.FEE, 6360, blueContrib, this, false)) {
                     contribStonesFound = true;
                     SystemMessage msg = new SystemMessage(SystemMessageId.S2_S1_HAS_DISAPPEARED);
                     msg.addItemName(6360);
                     msg.addLong(blueContrib);
                     player.sendPacket(msg);
                  }

                  if (!contribStonesFound) {
                     if (this instanceof DawnPriest) {
                        this.showChatWindow(player, 6, "dawn_low_stones", false);
                     } else {
                        this.showChatWindow(player, 6, "dusk_low_stones", false);
                     }
                  } else {
                     score = SevenSigns.getInstance().addPlayerStoneContrib(player.getObjectId(), blueContrib, greenContrib, redContrib);
                     SystemMessage sm = new SystemMessage(SystemMessageId.YOUR_CONTRIBUTION_SCORE_IS_INCREASED_BY_S1);
                     sm.addLong(score);
                     player.sendPacket(sm);
                     if (this instanceof DawnPriest) {
                        this.showChatWindow(player, 6, "dawn", false);
                     } else {
                        this.showChatWindow(player, 6, "dusk", false);
                     }
                  }
                  break;
               case 33:
                  int oldCabal = SevenSigns.getInstance().getPlayerCabal(player.getObjectId());
                  if (oldCabal != 0) {
                     if (this instanceof DawnPriest) {
                        this.showChatWindow(player, val, "dawn_member", false);
                     } else {
                        this.showChatWindow(player, val, "dusk_member", false);
                     }

                     return;
                  }

                  if (player.getPlayerClass().level() == 0) {
                     if (this instanceof DawnPriest) {
                        this.showChatWindow(player, val, "dawn_firstclass", false);
                     } else {
                        this.showChatWindow(player, val, "dusk_firstclass", false);
                     }

                     return;
                  }

                  if (cabal == 1 && FeatureConfig.ALT_GAME_CASTLE_DUSK) {
                     Clan clanx = player.getClan();
                     if (clanx != null && clanx.getCastleId() > 0) {
                        this.showChatWindow(player, "data/html/seven_signs/signs_33_dusk_no.htm");
                        break;
                     }
                  } else if (cabal == 2 && FeatureConfig.ALT_GAME_CASTLE_DAWN) {
                     Clan clanx = player.getClan();
                     if (clanx == null || clanx.getCastleId() == 0) {
                        this.showChatWindow(player, "data/html/seven_signs/signs_33_dawn_fee.htm");
                        break;
                     }
                  }

                  if (this instanceof DawnPriest) {
                     this.showChatWindow(player, val, "dawn", false);
                  } else {
                     this.showChatWindow(player, val, "dusk", false);
                  }
                  break;
               case 34:
                  if (player.getPlayerClass().level() <= 0
                     || player.getAdena() < FeatureConfig.SSQ_JOIN_DAWN_ADENA_FEE
                        && player.getInventory().getInventoryItemCount(FeatureConfig.SSQ_MANORS_AGREEMENT_ID, -1) <= 0L) {
                     this.showChatWindow(player, "data/html/seven_signs/signs_33_dawn_no.htm");
                  } else {
                     this.showChatWindow(player, "data/html/seven_signs/signs_33_dawn.htm");
                  }
            }
         } else {
            super.onBypassFeedback(player, command);
         }
      }
   }

   private void showChatWindow(Player player, int value, String suffix, boolean isDescription) {
      String filename = "data/html/seven_signs/";
      filename = filename + (isDescription ? "desc_" + value : "signs_" + value);
      filename = filename + (suffix != null ? "_" + suffix + ".htm" : ".htm");
      this.showChatWindow(player, filename);
   }
}
