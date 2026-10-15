package org.l2jmobius.gameserver.entity.actor.instance;

import java.io.File;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.StringTokenizer;
import org.l2jmobius.gameserver.config.FeatureConfig;
import org.l2jmobius.gameserver.config.ServerConfig;
import org.l2jmobius.gameserver.data.sql.ClanHallTable;
import org.l2jmobius.gameserver.data.xml.SkillData;
import org.l2jmobius.gameserver.data.xml.TeleporterData;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.enums.creature.InstanceType;
import org.l2jmobius.gameserver.entity.actor.templates.NpcTemplate;
import org.l2jmobius.gameserver.entity.clan.ClanAccess;
import org.l2jmobius.gameserver.entity.residences.AuctionableHall;
import org.l2jmobius.gameserver.entity.residences.ClanHall;
import org.l2jmobius.gameserver.entity.teleporter.TeleportHolder;
import org.l2jmobius.gameserver.managers.CHSiegeManager;
import org.l2jmobius.gameserver.mechanics.effects.EffectType;
import org.l2jmobius.gameserver.mechanics.siege.clanhalls.SiegableHall;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.network.serverpackets.ActionFailed;
import org.l2jmobius.gameserver.network.serverpackets.AgitDecoInfo;
import org.l2jmobius.gameserver.network.serverpackets.NpcHtmlMessage;

public class ClanHallManager extends Merchant {
   private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
   protected static final int COND_OWNER_FALSE = 0;
   protected static final int COND_ALL_FALSE = 1;
   protected static final int COND_BUSY_BECAUSE_OF_SIEGE = 2;
   protected static final int COND_OWNER = 3;
   private int _clanHallId = -1;

   public ClanHallManager(NpcTemplate template) {
      super(template);
      this.setInstanceType(InstanceType.ClanHallManager);
   }

   @Override
   public boolean isWarehouse() {
      return true;
   }

   @Override
   public void onBypassFeedback(Player player, String command) {
      if (!this.getClanHall().isSiegableHall() || !((SiegableHall)this.getClanHall()).isInSiege()) {
         int condition = this.validateCondition(player);
         if (condition > 1) {
            if (condition == 3) {
               StringTokenizer st = new StringTokenizer(command, " ");
               String actualCommand = st.nextToken();
               String val = "";
               if (st.countTokens() >= 1) {
                  val = st.nextToken();
               }

               if (actualCommand.equalsIgnoreCase("banish_foreigner")) {
                  NpcHtmlMessage html = new NpcHtmlMessage(this.getObjectId());
                  if (player.hasAccess(ClanAccess.HALL_BANISH)) {
                     if (val.equalsIgnoreCase("list")) {
                        html.setFile(player, "data/html/clanHallManager/banish-list.htm");
                     } else if (val.equalsIgnoreCase("banish")) {
                        this.getClanHall().banishForeigners();
                        html.setFile(player, "data/html/clanHallManager/banish.htm");
                     }
                  } else {
                     html.setFile(player, "data/html/clanHallManager/not_authorized.htm");
                  }

                  this.sendHtmlMessage(player, html);
                  return;
               }

               if (actualCommand.equalsIgnoreCase("manage_vault")) {
                  NpcHtmlMessage html = new NpcHtmlMessage(this.getObjectId());
                  if (player.hasAccess(ClanAccess.ACCESS_WAREHOUSE)) {
                     if (this.getClanHall().getLease() <= 0) {
                        html.setFile(player, "data/html/clanHallManager/vault-chs.htm");
                     } else {
                        html.setFile(player, "data/html/clanHallManager/vault.htm");
                        html.replace("%rent%", String.valueOf(this.getClanHall().getLease()));
                        html.replace("%date%", Instant.ofEpochMilli(this.getClanHall().getPaidUntil()).atZone(ZoneId.systemDefault()).format(DATE_FORMAT));
                     }

                     this.sendHtmlMessage(player, html);
                  } else {
                     html.setFile(player, "data/html/clanHallManager/not_authorized.htm");
                     this.sendHtmlMessage(player, html);
                  }

                  return;
               }

               if (actualCommand.equalsIgnoreCase("door")) {
                  NpcHtmlMessage html = new NpcHtmlMessage(this.getObjectId());
                  if (player.hasAccess(ClanAccess.HALL_OPEN_DOOR)) {
                     if (val.equalsIgnoreCase("open")) {
                        this.getClanHall().openCloseDoors(true);
                        html.setFile(player, "data/html/clanHallManager/door-open.htm");
                     } else if (val.equalsIgnoreCase("close")) {
                        this.getClanHall().openCloseDoors(false);
                        html.setFile(player, "data/html/clanHallManager/door-close.htm");
                     } else {
                        html.setFile(player, "data/html/clanHallManager/door.htm");
                     }

                     this.sendHtmlMessage(player, html);
                  } else {
                     html.setFile(player, "data/html/clanHallManager/not_authorized.htm");
                     this.sendHtmlMessage(player, html);
                  }

                  return;
               }

               if (actualCommand.equalsIgnoreCase("functions")) {
                  if (val.equalsIgnoreCase("tele")) {
                     NpcHtmlMessage html = new NpcHtmlMessage(this.getObjectId());
                     if (this.getClanHall().getFunction(1) == null) {
                        html.setFile(player, "data/html/clanHallManager/chamberlain-nac.htm");
                     } else {
                        html.setFile(
                           player, "data/html/clanHallManager/tele" + this.getClanHall().getLocation() + this.getClanHall().getFunction(1).getLevel() + ".htm"
                        );
                     }

                     this.sendHtmlMessage(player, html);
                  } else if (val.equalsIgnoreCase("item_creation")) {
                     if (this.getClanHall().getFunction(2) == null) {
                        NpcHtmlMessage html = new NpcHtmlMessage(this.getObjectId());
                        html.setFile(player, "data/html/clanHallManager/chamberlain-nac.htm");
                        this.sendHtmlMessage(player, html);
                        return;
                     }

                     if (st.countTokens() < 1) {
                        return;
                     }

                     int valbuy = Integer.parseInt(st.nextToken()) + this.getClanHall().getFunction(2).getLevel() * 100000;
                     this.showBuyWindow(player, valbuy);
                  } else if (val.equalsIgnoreCase("support")) {
                     NpcHtmlMessage html = new NpcHtmlMessage(this.getObjectId());
                     if (this.getClanHall().getFunction(6) == null) {
                        html.setFile(player, "data/html/clanHallManager/chamberlain-nac.htm");
                     } else {
                        html.setFile(player, "data/html/clanHallManager/support" + this.getClanHall().getFunction(6).getLevel() + ".htm");
                        html.replace("%mp%", String.valueOf((int)this.getCurrentMp()));
                     }

                     this.sendHtmlMessage(player, html);
                  } else if (val.equalsIgnoreCase("back")) {
                     this.showChatWindow(player);
                  } else {
                     NpcHtmlMessage html = new NpcHtmlMessage(this.getObjectId());
                     html.setFile(player, "data/html/clanHallManager/functions.htm");
                     if (this.getClanHall().getFunction(5) != null) {
                        html.replace("%xp_regen%", String.valueOf(this.getClanHall().getFunction(5).getLevel()));
                     } else {
                        html.replace("%xp_regen%", "0");
                     }

                     if (this.getClanHall().getFunction(3) != null) {
                        html.replace("%hp_regen%", String.valueOf(this.getClanHall().getFunction(3).getLevel()));
                     } else {
                        html.replace("%hp_regen%", "0");
                     }

                     if (this.getClanHall().getFunction(4) != null) {
                        html.replace("%mp_regen%", String.valueOf(this.getClanHall().getFunction(4).getLevel()));
                     } else {
                        html.replace("%mp_regen%", "0");
                     }

                     this.sendHtmlMessage(player, html);
                  }

                  return;
               }

               if (actualCommand.equalsIgnoreCase("manage")) {
                  if (player.hasAccess(ClanAccess.HALL_MANAGE_FUNCTIONS)) {
                     if (val.equalsIgnoreCase("recovery")) {
                        if (st.countTokens() >= 1) {
                           if (this.getClanHall().getOwnerId() == 0) {
                              player.sendMessage("This clan hall has no owner, you cannot change the configuration.");
                              return;
                           }

                           val = st.nextToken();
                           if (val.equalsIgnoreCase("hp_cancel")) {
                              NpcHtmlMessage htmlx = new NpcHtmlMessage(this.getObjectId());
                              htmlx.setFile(player, "data/html/clanHallManager/functions-cancel.htm");
                              htmlx.replace("%apply%", "recovery hp 0");
                              this.sendHtmlMessage(player, htmlx);
                              return;
                           }

                           if (val.equalsIgnoreCase("mp_cancel")) {
                              NpcHtmlMessage htmlx = new NpcHtmlMessage(this.getObjectId());
                              htmlx.setFile(player, "data/html/clanHallManager/functions-cancel.htm");
                              htmlx.replace("%apply%", "recovery mp 0");
                              this.sendHtmlMessage(player, htmlx);
                              return;
                           }

                           if (val.equalsIgnoreCase("exp_cancel")) {
                              NpcHtmlMessage htmlx = new NpcHtmlMessage(this.getObjectId());
                              htmlx.setFile(player, "data/html/clanHallManager/functions-cancel.htm");
                              htmlx.replace("%apply%", "recovery exp 0");
                              this.sendHtmlMessage(player, htmlx);
                              return;
                           }

                           if (val.equalsIgnoreCase("edit_hp")) {
                              val = st.nextToken();
                              NpcHtmlMessage html = new NpcHtmlMessage(this.getObjectId());
                              html.setFile(player, "data/html/clanHallManager/functions-apply.htm");
                              html.replace("%name%", "Fireplace (HP Recovery Device)");
                              int percent = Integer.parseInt(val);

                              html.replace("%cost%", switch (percent) {
                                 case 20 -> FeatureConfig.CH_HPREG1_FEE;
                                 case 40 -> FeatureConfig.CH_HPREG2_FEE;
                                 case 80 -> FeatureConfig.CH_HPREG3_FEE;
                                 case 100 -> FeatureConfig.CH_HPREG4_FEE;
                                 case 120 -> FeatureConfig.CH_HPREG5_FEE;
                                 case 140 -> FeatureConfig.CH_HPREG6_FEE;
                                 case 160 -> FeatureConfig.CH_HPREG7_FEE;
                                 case 180 -> FeatureConfig.CH_HPREG8_FEE;
                                 case 200 -> FeatureConfig.CH_HPREG9_FEE;
                                 case 220 -> FeatureConfig.CH_HPREG10_FEE;
                                 case 240 -> FeatureConfig.CH_HPREG11_FEE;
                                 case 260 -> FeatureConfig.CH_HPREG12_FEE;
                                 default -> FeatureConfig.CH_HPREG13_FEE;
                              } + "</font>Adena /" + FeatureConfig.CH_HPREG_FEE_RATIO / 1000L / 60L / 60L / 24L + " Day</font>)");
                              html.replace(
                                 "%use%", "Provides additional HP recovery for clan members in the clan hall.<font color=\"00FFFF\">" + percent + "%</font>"
                              );
                              html.replace("%apply%", "recovery hp " + percent);
                              this.sendHtmlMessage(player, html);
                              return;
                           }

                           if (val.equalsIgnoreCase("edit_mp")) {
                              val = st.nextToken();
                              NpcHtmlMessage html = new NpcHtmlMessage(this.getObjectId());
                              html.setFile(player, "data/html/clanHallManager/functions-apply.htm");
                              html.replace("%name%", "Carpet (MP Recovery)");
                              int percent = Integer.parseInt(val);

                              html.replace("%cost%", switch (percent) {
                                 case 5 -> FeatureConfig.CH_MPREG1_FEE;
                                 case 10 -> FeatureConfig.CH_MPREG2_FEE;
                                 case 15 -> FeatureConfig.CH_MPREG3_FEE;
                                 case 30 -> FeatureConfig.CH_MPREG4_FEE;
                                 default -> FeatureConfig.CH_MPREG5_FEE;
                              } + "</font>Adena /" + FeatureConfig.CH_MPREG_FEE_RATIO / 1000L / 60L / 60L / 24L + " Day</font>)");
                              html.replace(
                                 "%use%", "Provides additional MP recovery for clan members in the clan hall.<font color=\"00FFFF\">" + percent + "%</font>"
                              );
                              html.replace("%apply%", "recovery mp " + percent);
                              this.sendHtmlMessage(player, html);
                              return;
                           }

                           if (val.equalsIgnoreCase("edit_exp")) {
                              val = st.nextToken();
                              NpcHtmlMessage html = new NpcHtmlMessage(this.getObjectId());
                              html.setFile(player, "data/html/clanHallManager/functions-apply.htm");
                              html.replace("%name%", "Chandelier (EXP Recovery Device)");
                              int percent = Integer.parseInt(val);

                              html.replace("%cost%", switch (percent) {
                                 case 5 -> FeatureConfig.CH_EXPREG1_FEE;
                                 case 10 -> FeatureConfig.CH_EXPREG2_FEE;
                                 case 15 -> FeatureConfig.CH_EXPREG3_FEE;
                                 case 25 -> FeatureConfig.CH_EXPREG4_FEE;
                                 case 35 -> FeatureConfig.CH_EXPREG5_FEE;
                                 case 40 -> FeatureConfig.CH_EXPREG6_FEE;
                                 default -> FeatureConfig.CH_EXPREG7_FEE;
                              } + "</font>Adena /" + FeatureConfig.CH_EXPREG_FEE_RATIO / 1000L / 60L / 60L / 24L + " Day</font>)");
                              html.replace(
                                 "%use%",
                                 "Restores the Exp of any clan member who is resurrected in the clan hall.<font color=\"00FFFF\">" + percent + "%</font>"
                              );
                              html.replace("%apply%", "recovery exp " + percent);
                              this.sendHtmlMessage(player, html);
                              return;
                           }

                           if (val.equalsIgnoreCase("hp")) {
                              if (st.countTokens() >= 1) {
                                 val = st.nextToken();
                                 NpcHtmlMessage htmlx = new NpcHtmlMessage(this.getObjectId());
                                 htmlx.setFile(player, "data/html/clanHallManager/functions-apply_confirmed.htm");
                                 if (this.getClanHall().getFunction(3) != null && this.getClanHall().getFunction(3).getLevel() == Integer.parseInt(val)) {
                                    htmlx.setFile(player, "data/html/clanHallManager/functions-used.htm");
                                    htmlx.replace("%val%", val + "%");
                                    this.sendHtmlMessage(player, htmlx);
                                    return;
                                 }

                                 int percent = Integer.parseInt(val);
                                 int fee;
                                 switch (percent) {
                                    case 0:
                                       fee = 0;
                                       htmlx.setFile(player, "data/html/clanHallManager/functions-cancel_confirmed.htm");
                                       break;
                                    case 20:
                                       fee = FeatureConfig.CH_HPREG1_FEE;
                                       break;
                                    case 40:
                                       fee = FeatureConfig.CH_HPREG2_FEE;
                                       break;
                                    case 80:
                                       fee = FeatureConfig.CH_HPREG3_FEE;
                                       break;
                                    case 100:
                                       fee = FeatureConfig.CH_HPREG4_FEE;
                                       break;
                                    case 120:
                                       fee = FeatureConfig.CH_HPREG5_FEE;
                                       break;
                                    case 140:
                                       fee = FeatureConfig.CH_HPREG6_FEE;
                                       break;
                                    case 160:
                                       fee = FeatureConfig.CH_HPREG7_FEE;
                                       break;
                                    case 180:
                                       fee = FeatureConfig.CH_HPREG8_FEE;
                                       break;
                                    case 200:
                                       fee = FeatureConfig.CH_HPREG9_FEE;
                                       break;
                                    case 220:
                                       fee = FeatureConfig.CH_HPREG10_FEE;
                                       break;
                                    case 240:
                                       fee = FeatureConfig.CH_HPREG11_FEE;
                                       break;
                                    case 260:
                                       fee = FeatureConfig.CH_HPREG12_FEE;
                                       break;
                                    default:
                                       fee = FeatureConfig.CH_HPREG13_FEE;
                                 }

                                 if (!this.getClanHall()
                                    .updateFunctions(player, 3, percent, fee, FeatureConfig.CH_HPREG_FEE_RATIO, this.getClanHall().getFunction(3) == null)) {
                                    htmlx.setFile(player, "data/html/clanHallManager/low_adena.htm");
                                    this.sendHtmlMessage(player, htmlx);
                                 } else {
                                    this.revalidateDeco(player);
                                 }

                                 this.sendHtmlMessage(player, htmlx);
                              }

                              return;
                           }

                           if (val.equalsIgnoreCase("mp")) {
                              if (st.countTokens() >= 1) {
                                 val = st.nextToken();
                                 NpcHtmlMessage htmlxx = new NpcHtmlMessage(this.getObjectId());
                                 htmlxx.setFile(player, "data/html/clanHallManager/functions-apply_confirmed.htm");
                                 if (this.getClanHall().getFunction(4) != null && this.getClanHall().getFunction(4).getLevel() == Integer.parseInt(val)) {
                                    htmlxx.setFile(player, "data/html/clanHallManager/functions-used.htm");
                                    htmlxx.replace("%val%", val + "%");
                                    this.sendHtmlMessage(player, htmlxx);
                                    return;
                                 }

                                 int percent = Integer.parseInt(val);
                                 int fee;
                                 switch (percent) {
                                    case 0:
                                       fee = 0;
                                       htmlxx.setFile(player, "data/html/clanHallManager/functions-cancel_confirmed.htm");
                                       break;
                                    case 5:
                                       fee = FeatureConfig.CH_MPREG1_FEE;
                                       break;
                                    case 10:
                                       fee = FeatureConfig.CH_MPREG2_FEE;
                                       break;
                                    case 15:
                                       fee = FeatureConfig.CH_MPREG3_FEE;
                                       break;
                                    case 30:
                                       fee = FeatureConfig.CH_MPREG4_FEE;
                                       break;
                                    default:
                                       fee = FeatureConfig.CH_MPREG5_FEE;
                                 }

                                 if (!this.getClanHall()
                                    .updateFunctions(player, 4, percent, fee, FeatureConfig.CH_MPREG_FEE_RATIO, this.getClanHall().getFunction(4) == null)) {
                                    htmlxx.setFile(player, "data/html/clanHallManager/low_adena.htm");
                                    this.sendHtmlMessage(player, htmlxx);
                                 } else {
                                    this.revalidateDeco(player);
                                 }

                                 this.sendHtmlMessage(player, htmlxx);
                              }

                              return;
                           }

                           if (val.equalsIgnoreCase("exp")) {
                              if (st.countTokens() >= 1) {
                                 val = st.nextToken();
                                 NpcHtmlMessage htmlxxx = new NpcHtmlMessage(this.getObjectId());
                                 htmlxxx.setFile(player, "data/html/clanHallManager/functions-apply_confirmed.htm");
                                 if (this.getClanHall().getFunction(5) != null && this.getClanHall().getFunction(5).getLevel() == Integer.parseInt(val)) {
                                    htmlxxx.setFile(player, "data/html/clanHallManager/functions-used.htm");
                                    htmlxxx.replace("%val%", val + "%");
                                    this.sendHtmlMessage(player, htmlxxx);
                                    return;
                                 }

                                 int percent = Integer.parseInt(val);
                                 int fee;
                                 switch (percent) {
                                    case 0:
                                       fee = 0;
                                       htmlxxx.setFile(player, "data/html/clanHallManager/functions-cancel_confirmed.htm");
                                       break;
                                    case 5:
                                       fee = FeatureConfig.CH_EXPREG1_FEE;
                                       break;
                                    case 10:
                                       fee = FeatureConfig.CH_EXPREG2_FEE;
                                       break;
                                    case 15:
                                       fee = FeatureConfig.CH_EXPREG3_FEE;
                                       break;
                                    case 25:
                                       fee = FeatureConfig.CH_EXPREG4_FEE;
                                       break;
                                    case 35:
                                       fee = FeatureConfig.CH_EXPREG5_FEE;
                                       break;
                                    case 40:
                                       fee = FeatureConfig.CH_EXPREG6_FEE;
                                       break;
                                    default:
                                       fee = FeatureConfig.CH_EXPREG7_FEE;
                                 }

                                 if (!this.getClanHall()
                                    .updateFunctions(player, 5, percent, fee, FeatureConfig.CH_EXPREG_FEE_RATIO, this.getClanHall().getFunction(5) == null)) {
                                    htmlxxx.setFile(player, "data/html/clanHallManager/low_adena.htm");
                                    this.sendHtmlMessage(player, htmlxxx);
                                 } else {
                                    this.revalidateDeco(player);
                                 }

                                 this.sendHtmlMessage(player, htmlxxx);
                              }

                              return;
                           }
                        }

                        NpcHtmlMessage htmlxxxx = new NpcHtmlMessage(this.getObjectId());
                        htmlxxxx.setFile(player, "data/html/clanHallManager/edit_recovery.htm");
                        String hp_grade0 = "[<a action=\"bypass -h npc_%objectId%_manage recovery edit_hp 20\">20%</a>][<a action=\"bypass -h npc_%objectId%_manage recovery edit_hp 40\">40%</a>][<a action=\"bypass -h npc_%objectId%_manage recovery edit_hp 220\">220%</a>]";
                        String hp_grade1 = "[<a action=\"bypass -h npc_%objectId%_manage recovery edit_hp 40\">40%</a>][<a action=\"bypass -h npc_%objectId%_manage recovery edit_hp 100\">100%</a>][<a action=\"bypass -h npc_%objectId%_manage recovery edit_hp 160\">160%</a>]";
                        String hp_grade2 = "[<a action=\"bypass -h npc_%objectId%_manage recovery edit_hp 80\">80%</a>][<a action=\"bypass -h npc_%objectId%_manage recovery edit_hp 140\">140%</a>][<a action=\"bypass -h npc_%objectId%_manage recovery edit_hp 200\">200%</a>][<a action=\"bypass -h npc_%objectId%_manage recovery edit_hp 260\">260%</a>]";
                        String hp_grade3 = "[<a action=\"bypass -h npc_%objectId%_manage recovery edit_hp 80\">80%</a>][<a action=\"bypass -h npc_%objectId%_manage recovery edit_hp 120\">120%</a>][<a action=\"bypass -h npc_%objectId%_manage recovery edit_hp 180\">180%</a>][<a action=\"bypass -h npc_%objectId%_manage recovery edit_hp 240\">240%</a>][<a action=\"bypass -h npc_%objectId%_manage recovery edit_hp 300\">300%</a>]";
                        String exp_grade0 = "[<a action=\"bypass -h npc_%objectId%_manage recovery edit_exp 5\">5%</a>][<a action=\"bypass -h npc_%objectId%_manage recovery edit_exp 10\">10%</a>][<a action=\"bypass -h npc_%objectId%_manage recovery edit_exp 25\">25%</a>]";
                        String exp_grade1 = "[<a action=\"bypass -h npc_%objectId%_manage recovery edit_exp 5\">5%</a>][<a action=\"bypass -h npc_%objectId%_manage recovery edit_exp 15\">15%</a>][<a action=\"bypass -h npc_%objectId%_manage recovery edit_exp 30\">30%</a>]";
                        String exp_grade2 = "[<a action=\"bypass -h npc_%objectId%_manage recovery edit_exp 5\">5%</a>][<a action=\"bypass -h npc_%objectId%_manage recovery edit_exp 15\">15%</a>][<a action=\"bypass -h npc_%objectId%_manage recovery edit_exp 25\">25%</a>][<a action=\"bypass -h npc_%objectId%_manage recovery edit_exp 40\">40%</a>]";
                        String exp_grade3 = "[<a action=\"bypass -h npc_%objectId%_manage recovery edit_exp 15\">15%</a>][<a action=\"bypass -h npc_%objectId%_manage recovery edit_exp 25\">25%</a>][<a action=\"bypass -h npc_%objectId%_manage recovery edit_exp 35\">35%</a>][<a action=\"bypass -h npc_%objectId%_manage recovery edit_exp 50\">50%</a>]";
                        String mp_grade0 = "[<a action=\"bypass -h npc_%objectId%_manage recovery edit_mp 5\">5%</a>][<a action=\"bypass -h npc_%objectId%_manage recovery edit_mp 10\">10%</a>][<a action=\"bypass -h npc_%objectId%_manage recovery edit_mp 25\">25%</a>]";
                        String mp_grade1 = "[<a action=\"bypass -h npc_%objectId%_manage recovery edit_mp 5\">5%</a>][<a action=\"bypass -h npc_%objectId%_manage recovery edit_mp 15\">15%</a>][<a action=\"bypass -h npc_%objectId%_manage recovery edit_mp 25\">25%</a>]";
                        String mp_grade2 = "[<a action=\"bypass -h npc_%objectId%_manage recovery edit_mp 5\">5%</a>][<a action=\"bypass -h npc_%objectId%_manage recovery edit_mp 15\">15%</a>][<a action=\"bypass -h npc_%objectId%_manage recovery edit_mp 30\">30%</a>]";
                        String mp_grade3 = "[<a action=\"bypass -h npc_%objectId%_manage recovery edit_mp 5\">5%</a>][<a action=\"bypass -h npc_%objectId%_manage recovery edit_mp 15\">15%</a>][<a action=\"bypass -h npc_%objectId%_manage recovery edit_mp 30\">30%</a>][<a action=\"bypass -h npc_%objectId%_manage recovery edit_mp 40\">40%</a>]";
                        if (this.getClanHall().getFunction(3) != null) {
                           htmlxxxx.replace(
                              "%hp_recovery%",
                              this.getClanHall().getFunction(3).getLevel()
                                 + "%</font> (<font color=\"FFAABB\">"
                                 + this.getClanHall().getFunction(3).getLease()
                                 + "</font>Adena /"
                                 + FeatureConfig.CH_HPREG_FEE_RATIO / 1000L / 60L / 60L / 24L
                                 + " Day)"
                           );
                           htmlxxxx.replace(
                              "%hp_period%",
                              "Withdraw the fee for the next time at "
                                 + Instant.ofEpochMilli(this.getClanHall().getFunction(3).getEndTime()).atZone(ZoneId.systemDefault()).format(DATE_FORMAT)
                           );
                           int grade = this.getClanHall().getGrade();
                           switch (grade) {
                              case 0:
                                 htmlxxxx.replace(
                                    "%change_hp%",
                                    "[<a action=\"bypass -h npc_%objectId%_manage recovery hp_cancel\">Deactivate</a>][<a action=\"bypass -h npc_%objectId%_manage recovery edit_hp 20\">20%</a>][<a action=\"bypass -h npc_%objectId%_manage recovery edit_hp 40\">40%</a>][<a action=\"bypass -h npc_%objectId%_manage recovery edit_hp 220\">220%</a>]"
                                 );
                                 break;
                              case 1:
                                 htmlxxxx.replace(
                                    "%change_hp%",
                                    "[<a action=\"bypass -h npc_%objectId%_manage recovery hp_cancel\">Deactivate</a>][<a action=\"bypass -h npc_%objectId%_manage recovery edit_hp 40\">40%</a>][<a action=\"bypass -h npc_%objectId%_manage recovery edit_hp 100\">100%</a>][<a action=\"bypass -h npc_%objectId%_manage recovery edit_hp 160\">160%</a>]"
                                 );
                                 break;
                              case 2:
                                 htmlxxxx.replace(
                                    "%change_hp%",
                                    "[<a action=\"bypass -h npc_%objectId%_manage recovery hp_cancel\">Deactivate</a>][<a action=\"bypass -h npc_%objectId%_manage recovery edit_hp 80\">80%</a>][<a action=\"bypass -h npc_%objectId%_manage recovery edit_hp 140\">140%</a>][<a action=\"bypass -h npc_%objectId%_manage recovery edit_hp 200\">200%</a>][<a action=\"bypass -h npc_%objectId%_manage recovery edit_hp 260\">260%</a>]"
                                 );
                                 break;
                              case 3:
                                 htmlxxxx.replace(
                                    "%change_hp%",
                                    "[<a action=\"bypass -h npc_%objectId%_manage recovery hp_cancel\">Deactivate</a>][<a action=\"bypass -h npc_%objectId%_manage recovery edit_hp 80\">80%</a>][<a action=\"bypass -h npc_%objectId%_manage recovery edit_hp 120\">120%</a>][<a action=\"bypass -h npc_%objectId%_manage recovery edit_hp 180\">180%</a>][<a action=\"bypass -h npc_%objectId%_manage recovery edit_hp 240\">240%</a>][<a action=\"bypass -h npc_%objectId%_manage recovery edit_hp 300\">300%</a>]"
                                 );
                           }
                        } else {
                           htmlxxxx.replace("%hp_recovery%", "none");
                           htmlxxxx.replace("%hp_period%", "none");
                           int grade = this.getClanHall().getGrade();
                           switch (grade) {
                              case 0:
                                 htmlxxxx.replace(
                                    "%change_hp%",
                                    "[<a action=\"bypass -h npc_%objectId%_manage recovery edit_hp 20\">20%</a>][<a action=\"bypass -h npc_%objectId%_manage recovery edit_hp 40\">40%</a>][<a action=\"bypass -h npc_%objectId%_manage recovery edit_hp 220\">220%</a>]"
                                 );
                                 break;
                              case 1:
                                 htmlxxxx.replace(
                                    "%change_hp%",
                                    "[<a action=\"bypass -h npc_%objectId%_manage recovery edit_hp 40\">40%</a>][<a action=\"bypass -h npc_%objectId%_manage recovery edit_hp 100\">100%</a>][<a action=\"bypass -h npc_%objectId%_manage recovery edit_hp 160\">160%</a>]"
                                 );
                                 break;
                              case 2:
                                 htmlxxxx.replace(
                                    "%change_hp%",
                                    "[<a action=\"bypass -h npc_%objectId%_manage recovery edit_hp 80\">80%</a>][<a action=\"bypass -h npc_%objectId%_manage recovery edit_hp 140\">140%</a>][<a action=\"bypass -h npc_%objectId%_manage recovery edit_hp 200\">200%</a>][<a action=\"bypass -h npc_%objectId%_manage recovery edit_hp 260\">260%</a>]"
                                 );
                                 break;
                              case 3:
                                 htmlxxxx.replace(
                                    "%change_hp%",
                                    "[<a action=\"bypass -h npc_%objectId%_manage recovery edit_hp 80\">80%</a>][<a action=\"bypass -h npc_%objectId%_manage recovery edit_hp 120\">120%</a>][<a action=\"bypass -h npc_%objectId%_manage recovery edit_hp 180\">180%</a>][<a action=\"bypass -h npc_%objectId%_manage recovery edit_hp 240\">240%</a>][<a action=\"bypass -h npc_%objectId%_manage recovery edit_hp 300\">300%</a>]"
                                 );
                           }
                        }

                        if (this.getClanHall().getFunction(5) != null) {
                           htmlxxxx.replace(
                              "%exp_recovery%",
                              this.getClanHall().getFunction(5).getLevel()
                                 + "%</font> (<font color=\"FFAABB\">"
                                 + this.getClanHall().getFunction(5).getLease()
                                 + "</font>Adena /"
                                 + FeatureConfig.CH_EXPREG_FEE_RATIO / 1000L / 60L / 60L / 24L
                                 + " Day)"
                           );
                           htmlxxxx.replace(
                              "%exp_period%",
                              "Withdraw the fee for the next time at "
                                 + Instant.ofEpochMilli(this.getClanHall().getFunction(5).getEndTime()).atZone(ZoneId.systemDefault()).format(DATE_FORMAT)
                           );
                           int grade = this.getClanHall().getGrade();
                           switch (grade) {
                              case 0:
                                 htmlxxxx.replace(
                                    "%change_exp%",
                                    "[<a action=\"bypass -h npc_%objectId%_manage recovery exp_cancel\">Deactivate</a>][<a action=\"bypass -h npc_%objectId%_manage recovery edit_exp 5\">5%</a>][<a action=\"bypass -h npc_%objectId%_manage recovery edit_exp 10\">10%</a>][<a action=\"bypass -h npc_%objectId%_manage recovery edit_exp 25\">25%</a>]"
                                 );
                                 break;
                              case 1:
                                 htmlxxxx.replace(
                                    "%change_exp%",
                                    "[<a action=\"bypass -h npc_%objectId%_manage recovery exp_cancel\">Deactivate</a>][<a action=\"bypass -h npc_%objectId%_manage recovery edit_exp 5\">5%</a>][<a action=\"bypass -h npc_%objectId%_manage recovery edit_exp 15\">15%</a>][<a action=\"bypass -h npc_%objectId%_manage recovery edit_exp 30\">30%</a>]"
                                 );
                                 break;
                              case 2:
                                 htmlxxxx.replace(
                                    "%change_exp%",
                                    "[<a action=\"bypass -h npc_%objectId%_manage recovery exp_cancel\">Deactivate</a>][<a action=\"bypass -h npc_%objectId%_manage recovery edit_exp 5\">5%</a>][<a action=\"bypass -h npc_%objectId%_manage recovery edit_exp 15\">15%</a>][<a action=\"bypass -h npc_%objectId%_manage recovery edit_exp 25\">25%</a>][<a action=\"bypass -h npc_%objectId%_manage recovery edit_exp 40\">40%</a>]"
                                 );
                                 break;
                              case 3:
                                 htmlxxxx.replace(
                                    "%change_exp%",
                                    "[<a action=\"bypass -h npc_%objectId%_manage recovery exp_cancel\">Deactivate</a>][<a action=\"bypass -h npc_%objectId%_manage recovery edit_exp 15\">15%</a>][<a action=\"bypass -h npc_%objectId%_manage recovery edit_exp 25\">25%</a>][<a action=\"bypass -h npc_%objectId%_manage recovery edit_exp 35\">35%</a>][<a action=\"bypass -h npc_%objectId%_manage recovery edit_exp 50\">50%</a>]"
                                 );
                           }
                        } else {
                           htmlxxxx.replace("%exp_recovery%", "none");
                           htmlxxxx.replace("%exp_period%", "none");
                           int grade = this.getClanHall().getGrade();
                           switch (grade) {
                              case 0:
                                 htmlxxxx.replace(
                                    "%change_exp%",
                                    "[<a action=\"bypass -h npc_%objectId%_manage recovery edit_exp 5\">5%</a>][<a action=\"bypass -h npc_%objectId%_manage recovery edit_exp 10\">10%</a>][<a action=\"bypass -h npc_%objectId%_manage recovery edit_exp 25\">25%</a>]"
                                 );
                                 break;
                              case 1:
                                 htmlxxxx.replace(
                                    "%change_exp%",
                                    "[<a action=\"bypass -h npc_%objectId%_manage recovery edit_exp 5\">5%</a>][<a action=\"bypass -h npc_%objectId%_manage recovery edit_exp 15\">15%</a>][<a action=\"bypass -h npc_%objectId%_manage recovery edit_exp 30\">30%</a>]"
                                 );
                                 break;
                              case 2:
                                 htmlxxxx.replace(
                                    "%change_exp%",
                                    "[<a action=\"bypass -h npc_%objectId%_manage recovery edit_exp 5\">5%</a>][<a action=\"bypass -h npc_%objectId%_manage recovery edit_exp 15\">15%</a>][<a action=\"bypass -h npc_%objectId%_manage recovery edit_exp 25\">25%</a>][<a action=\"bypass -h npc_%objectId%_manage recovery edit_exp 40\">40%</a>]"
                                 );
                                 break;
                              case 3:
                                 htmlxxxx.replace(
                                    "%change_exp%",
                                    "[<a action=\"bypass -h npc_%objectId%_manage recovery edit_exp 15\">15%</a>][<a action=\"bypass -h npc_%objectId%_manage recovery edit_exp 25\">25%</a>][<a action=\"bypass -h npc_%objectId%_manage recovery edit_exp 35\">35%</a>][<a action=\"bypass -h npc_%objectId%_manage recovery edit_exp 50\">50%</a>]"
                                 );
                           }
                        }

                        if (this.getClanHall().getFunction(4) != null) {
                           htmlxxxx.replace(
                              "%mp_recovery%",
                              this.getClanHall().getFunction(4).getLevel()
                                 + "%</font> (<font color=\"FFAABB\">"
                                 + this.getClanHall().getFunction(4).getLease()
                                 + "</font>Adena /"
                                 + FeatureConfig.CH_MPREG_FEE_RATIO / 1000L / 60L / 60L / 24L
                                 + " Day)"
                           );
                           htmlxxxx.replace(
                              "%mp_period%",
                              "Withdraw the fee for the next time at "
                                 + Instant.ofEpochMilli(this.getClanHall().getFunction(4).getEndTime()).atZone(ZoneId.systemDefault()).format(DATE_FORMAT)
                           );
                           int grade = this.getClanHall().getGrade();
                           switch (grade) {
                              case 0:
                                 htmlxxxx.replace(
                                    "%change_mp%",
                                    "[<a action=\"bypass -h npc_%objectId%_manage recovery mp_cancel\">Deactivate</a>][<a action=\"bypass -h npc_%objectId%_manage recovery edit_mp 5\">5%</a>][<a action=\"bypass -h npc_%objectId%_manage recovery edit_mp 10\">10%</a>][<a action=\"bypass -h npc_%objectId%_manage recovery edit_mp 25\">25%</a>]"
                                 );
                                 break;
                              case 1:
                                 htmlxxxx.replace(
                                    "%change_mp%",
                                    "[<a action=\"bypass -h npc_%objectId%_manage recovery mp_cancel\">Deactivate</a>][<a action=\"bypass -h npc_%objectId%_manage recovery edit_mp 5\">5%</a>][<a action=\"bypass -h npc_%objectId%_manage recovery edit_mp 15\">15%</a>][<a action=\"bypass -h npc_%objectId%_manage recovery edit_mp 25\">25%</a>]"
                                 );
                                 break;
                              case 2:
                                 htmlxxxx.replace(
                                    "%change_mp%",
                                    "[<a action=\"bypass -h npc_%objectId%_manage recovery mp_cancel\">Deactivate</a>][<a action=\"bypass -h npc_%objectId%_manage recovery edit_mp 5\">5%</a>][<a action=\"bypass -h npc_%objectId%_manage recovery edit_mp 15\">15%</a>][<a action=\"bypass -h npc_%objectId%_manage recovery edit_mp 30\">30%</a>]"
                                 );
                                 break;
                              case 3:
                                 htmlxxxx.replace(
                                    "%change_mp%",
                                    "[<a action=\"bypass -h npc_%objectId%_manage recovery mp_cancel\">Deactivate</a>][<a action=\"bypass -h npc_%objectId%_manage recovery edit_mp 5\">5%</a>][<a action=\"bypass -h npc_%objectId%_manage recovery edit_mp 15\">15%</a>][<a action=\"bypass -h npc_%objectId%_manage recovery edit_mp 30\">30%</a>][<a action=\"bypass -h npc_%objectId%_manage recovery edit_mp 40\">40%</a>]"
                                 );
                           }
                        } else {
                           htmlxxxx.replace("%mp_recovery%", "none");
                           htmlxxxx.replace("%mp_period%", "none");
                           int grade = this.getClanHall().getGrade();
                           switch (grade) {
                              case 0:
                                 htmlxxxx.replace(
                                    "%change_mp%",
                                    "[<a action=\"bypass -h npc_%objectId%_manage recovery edit_mp 5\">5%</a>][<a action=\"bypass -h npc_%objectId%_manage recovery edit_mp 10\">10%</a>][<a action=\"bypass -h npc_%objectId%_manage recovery edit_mp 25\">25%</a>]"
                                 );
                                 break;
                              case 1:
                                 htmlxxxx.replace(
                                    "%change_mp%",
                                    "[<a action=\"bypass -h npc_%objectId%_manage recovery edit_mp 5\">5%</a>][<a action=\"bypass -h npc_%objectId%_manage recovery edit_mp 15\">15%</a>][<a action=\"bypass -h npc_%objectId%_manage recovery edit_mp 25\">25%</a>]"
                                 );
                                 break;
                              case 2:
                                 htmlxxxx.replace(
                                    "%change_mp%",
                                    "[<a action=\"bypass -h npc_%objectId%_manage recovery edit_mp 5\">5%</a>][<a action=\"bypass -h npc_%objectId%_manage recovery edit_mp 15\">15%</a>][<a action=\"bypass -h npc_%objectId%_manage recovery edit_mp 30\">30%</a>]"
                                 );
                                 break;
                              case 3:
                                 htmlxxxx.replace(
                                    "%change_mp%",
                                    "[<a action=\"bypass -h npc_%objectId%_manage recovery edit_mp 5\">5%</a>][<a action=\"bypass -h npc_%objectId%_manage recovery edit_mp 15\">15%</a>][<a action=\"bypass -h npc_%objectId%_manage recovery edit_mp 30\">30%</a>][<a action=\"bypass -h npc_%objectId%_manage recovery edit_mp 40\">40%</a>]"
                                 );
                           }
                        }

                        this.sendHtmlMessage(player, htmlxxxx);
                     } else if (val.equalsIgnoreCase("other")) {
                        if (st.countTokens() >= 1) {
                           if (this.getClanHall().getOwnerId() == 0) {
                              player.sendMessage("This clan hall has no owner, you cannot change the configuration.");
                              return;
                           }

                           val = st.nextToken();
                           if (val.equalsIgnoreCase("item_cancel")) {
                              NpcHtmlMessage htmlxxxxx = new NpcHtmlMessage(this.getObjectId());
                              htmlxxxxx.setFile(player, "data/html/clanHallManager/functions-cancel.htm");
                              htmlxxxxx.replace("%apply%", "other item 0");
                              this.sendHtmlMessage(player, htmlxxxxx);
                              return;
                           }

                           if (val.equalsIgnoreCase("tele_cancel")) {
                              NpcHtmlMessage htmlxxxxx = new NpcHtmlMessage(this.getObjectId());
                              htmlxxxxx.setFile(player, "data/html/clanHallManager/functions-cancel.htm");
                              htmlxxxxx.replace("%apply%", "other tele 0");
                              this.sendHtmlMessage(player, htmlxxxxx);
                              return;
                           }

                           if (val.equalsIgnoreCase("support_cancel")) {
                              NpcHtmlMessage htmlxxxxx = new NpcHtmlMessage(this.getObjectId());
                              htmlxxxxx.setFile(player, "data/html/clanHallManager/functions-cancel.htm");
                              htmlxxxxx.replace("%apply%", "other support 0");
                              this.sendHtmlMessage(player, htmlxxxxx);
                              return;
                           }

                           if (val.equalsIgnoreCase("edit_item")) {
                              val = st.nextToken();
                              NpcHtmlMessage html = new NpcHtmlMessage(this.getObjectId());
                              html.setFile(player, "data/html/clanHallManager/functions-apply.htm");
                              html.replace("%name%", "Magic Equipment (Item Production Facilities)");
                              int stage = Integer.parseInt(val);

                              html.replace("%cost%", switch (stage) {
                                 case 1 -> FeatureConfig.CH_ITEM1_FEE;
                                 case 2 -> FeatureConfig.CH_ITEM2_FEE;
                                 default -> FeatureConfig.CH_ITEM3_FEE;
                              } + "</font>Adena /" + FeatureConfig.CH_ITEM_FEE_RATIO / 1000L / 60L / 60L / 24L + " Day</font>)");
                              html.replace("%use%", "Allow the purchase of special items at fixed intervals.");
                              html.replace("%apply%", "other item " + stage);
                              this.sendHtmlMessage(player, html);
                              return;
                           }

                           if (val.equalsIgnoreCase("edit_support")) {
                              val = st.nextToken();
                              NpcHtmlMessage html = new NpcHtmlMessage(this.getObjectId());
                              html.setFile(player, "data/html/clanHallManager/functions-apply.htm");
                              html.replace("%name%", "Insignia (Supplementary Magic)");
                              int stage = Integer.parseInt(val);

                              html.replace("%cost%", switch (stage) {
                                 case 1 -> FeatureConfig.CH_SUPPORT1_FEE;
                                 case 2 -> FeatureConfig.CH_SUPPORT2_FEE;
                                 case 3 -> FeatureConfig.CH_SUPPORT3_FEE;
                                 case 4 -> FeatureConfig.CH_SUPPORT4_FEE;
                                 case 5 -> FeatureConfig.CH_SUPPORT5_FEE;
                                 case 6 -> FeatureConfig.CH_SUPPORT6_FEE;
                                 case 7 -> FeatureConfig.CH_SUPPORT7_FEE;
                                 default -> FeatureConfig.CH_SUPPORT8_FEE;
                              } + "</font>Adena /" + FeatureConfig.CH_SUPPORT_FEE_RATIO / 1000L / 60L / 60L / 24L + " Day</font>)");
                              html.replace("%use%", "Enables the use of supplementary magic.");
                              html.replace("%apply%", "other support " + stage);
                              this.sendHtmlMessage(player, html);
                              return;
                           }

                           if (val.equalsIgnoreCase("edit_tele")) {
                              val = st.nextToken();
                              NpcHtmlMessage html = new NpcHtmlMessage(this.getObjectId());
                              html.setFile(player, "data/html/clanHallManager/functions-apply.htm");
                              html.replace("%name%", "Mirror (Teleportation Device)");
                              int stage = Integer.parseInt(val);

                              html.replace("%cost%", switch (stage) {
                                 case 1 -> FeatureConfig.CH_TELE1_FEE;
                                 default -> FeatureConfig.CH_TELE2_FEE;
                              } + "</font>Adena /" + FeatureConfig.CH_TELE_FEE_RATIO / 1000L / 60L / 60L / 24L + " Day</font>)");
                              html.replace(
                                 "%use%", "Teleports clan members in a clan hall to the target <font color=\"00FFFF\">Stage " + stage + "</font> staging area"
                              );
                              html.replace("%apply%", "other tele " + stage);
                              this.sendHtmlMessage(player, html);
                              return;
                           }

                           if (val.equalsIgnoreCase("item")) {
                              if (st.countTokens() >= 1) {
                                 if (this.getClanHall().getOwnerId() == 0) {
                                    player.sendMessage("This clan hall has no owner, you cannot change the configuration.");
                                    return;
                                 }

                                 val = st.nextToken();
                                 NpcHtmlMessage htmlxxxxx = new NpcHtmlMessage(this.getObjectId());
                                 htmlxxxxx.setFile(player, "data/html/clanHallManager/functions-apply_confirmed.htm");
                                 if (this.getClanHall().getFunction(2) != null && this.getClanHall().getFunction(2).getLevel() == Integer.parseInt(val)) {
                                    htmlxxxxx.setFile(player, "data/html/clanHallManager/functions-used.htm");
                                    htmlxxxxx.replace("%val%", "Stage " + val);
                                    this.sendHtmlMessage(player, htmlxxxxx);
                                    return;
                                 }

                                 int level = Integer.parseInt(val);
                                 int fee;
                                 switch (level) {
                                    case 0:
                                       fee = 0;
                                       htmlxxxxx.setFile(player, "data/html/clanHallManager/functions-cancel_confirmed.htm");
                                       break;
                                    case 1:
                                       fee = FeatureConfig.CH_ITEM1_FEE;
                                       break;
                                    case 2:
                                       fee = FeatureConfig.CH_ITEM2_FEE;
                                       break;
                                    default:
                                       fee = FeatureConfig.CH_ITEM3_FEE;
                                 }

                                 if (!this.getClanHall()
                                    .updateFunctions(player, 2, level, fee, FeatureConfig.CH_ITEM_FEE_RATIO, this.getClanHall().getFunction(2) == null)) {
                                    htmlxxxxx.setFile(player, "data/html/clanHallManager/low_adena.htm");
                                    this.sendHtmlMessage(player, htmlxxxxx);
                                 } else {
                                    this.revalidateDeco(player);
                                 }

                                 this.sendHtmlMessage(player, htmlxxxxx);
                              }

                              return;
                           }

                           if (val.equalsIgnoreCase("tele")) {
                              if (st.countTokens() >= 1) {
                                 val = st.nextToken();
                                 NpcHtmlMessage htmlxxxxxx = new NpcHtmlMessage(this.getObjectId());
                                 htmlxxxxxx.setFile(player, "data/html/clanHallManager/functions-apply_confirmed.htm");
                                 if (this.getClanHall().getFunction(1) != null && this.getClanHall().getFunction(1).getLevel() == Integer.parseInt(val)) {
                                    htmlxxxxxx.setFile(player, "data/html/clanHallManager/functions-used.htm");
                                    htmlxxxxxx.replace("%val%", "Stage " + val);
                                    this.sendHtmlMessage(player, htmlxxxxxx);
                                    return;
                                 }

                                 int level = Integer.parseInt(val);
                                 int fee;
                                 switch (level) {
                                    case 0:
                                       fee = 0;
                                       htmlxxxxxx.setFile(player, "data/html/clanHallManager/functions-cancel_confirmed.htm");
                                       break;
                                    case 1:
                                       fee = FeatureConfig.CH_TELE1_FEE;
                                       break;
                                    default:
                                       fee = FeatureConfig.CH_TELE2_FEE;
                                 }

                                 if (!this.getClanHall()
                                    .updateFunctions(player, 1, level, fee, FeatureConfig.CH_TELE_FEE_RATIO, this.getClanHall().getFunction(1) == null)) {
                                    htmlxxxxxx.setFile(player, "data/html/clanHallManager/low_adena.htm");
                                    this.sendHtmlMessage(player, htmlxxxxxx);
                                 } else {
                                    this.revalidateDeco(player);
                                 }

                                 this.sendHtmlMessage(player, htmlxxxxxx);
                              }

                              return;
                           }

                           if (val.equalsIgnoreCase("support")) {
                              if (st.countTokens() >= 1) {
                                 val = st.nextToken();
                                 NpcHtmlMessage htmlxxxxxxx = new NpcHtmlMessage(this.getObjectId());
                                 htmlxxxxxxx.setFile(player, "data/html/clanHallManager/functions-apply_confirmed.htm");
                                 if (this.getClanHall().getFunction(6) != null && this.getClanHall().getFunction(6).getLevel() == Integer.parseInt(val)) {
                                    htmlxxxxxxx.setFile(player, "data/html/clanHallManager/functions-used.htm");
                                    htmlxxxxxxx.replace("%val%", "Stage " + val);
                                    this.sendHtmlMessage(player, htmlxxxxxxx);
                                    return;
                                 }

                                 int level = Integer.parseInt(val);
                                 int fee;
                                 switch (level) {
                                    case 0:
                                       fee = 0;
                                       htmlxxxxxxx.setFile(player, "data/html/clanHallManager/functions-cancel_confirmed.htm");
                                       break;
                                    case 1:
                                       fee = FeatureConfig.CH_SUPPORT1_FEE;
                                       break;
                                    case 2:
                                       fee = FeatureConfig.CH_SUPPORT2_FEE;
                                       break;
                                    case 3:
                                       fee = FeatureConfig.CH_SUPPORT3_FEE;
                                       break;
                                    case 4:
                                       fee = FeatureConfig.CH_SUPPORT4_FEE;
                                       break;
                                    case 5:
                                       fee = FeatureConfig.CH_SUPPORT5_FEE;
                                       break;
                                    case 6:
                                       fee = FeatureConfig.CH_SUPPORT6_FEE;
                                       break;
                                    case 7:
                                       fee = FeatureConfig.CH_SUPPORT7_FEE;
                                       break;
                                    default:
                                       fee = FeatureConfig.CH_SUPPORT8_FEE;
                                 }

                                 if (!this.getClanHall()
                                    .updateFunctions(player, 6, level, fee, FeatureConfig.CH_SUPPORT_FEE_RATIO, this.getClanHall().getFunction(6) == null)) {
                                    htmlxxxxxxx.setFile(player, "data/html/clanHallManager/low_adena.htm");
                                    this.sendHtmlMessage(player, htmlxxxxxxx);
                                 } else {
                                    this.revalidateDeco(player);
                                 }

                                 this.sendHtmlMessage(player, htmlxxxxxxx);
                              }

                              return;
                           }
                        }

                        NpcHtmlMessage htmlxxxxxxxx = new NpcHtmlMessage(this.getObjectId());
                        htmlxxxxxxxx.setFile(player, "data/html/clanHallManager/edit_other.htm");
                        String tele = "[<a action=\"bypass -h npc_%objectId%_manage other edit_tele 1\">Level 1</a>][<a action=\"bypass -h npc_%objectId%_manage other edit_tele 2\">Level 2</a>]";
                        String support_grade0 = "[<a action=\"bypass -h npc_%objectId%_manage other edit_support 1\">Level 1</a>][<a action=\"bypass -h npc_%objectId%_manage other edit_support 2\">Level 2</a>]";
                        String support_grade1 = "[<a action=\"bypass -h npc_%objectId%_manage other edit_support 1\">Level 1</a>][<a action=\"bypass -h npc_%objectId%_manage other edit_support 2\">Level 2</a>][<a action=\"bypass -h npc_%objectId%_manage other edit_support 4\">Level 4</a>]";
                        String support_grade2 = "[<a action=\"bypass -h npc_%objectId%_manage other edit_support 3\">Level 3</a>][<a action=\"bypass -h npc_%objectId%_manage other edit_support 4\">Level 4</a>][<a action=\"bypass -h npc_%objectId%_manage other edit_support 5\">Level 5</a>]";
                        String support_grade3 = "[<a action=\"bypass -h npc_%objectId%_manage other edit_support 3\">Level 3</a>][<a action=\"bypass -h npc_%objectId%_manage other edit_support 5\">Level 5</a>][<a action=\"bypass -h npc_%objectId%_manage other edit_support 7\">Level 7</a>][<a action=\"bypass -h npc_%objectId%_manage other edit_support 8\">Level 8</a>]";
                        String item = "[<a action=\"bypass -h npc_%objectId%_manage other edit_item 1\">Level 1</a>][<a action=\"bypass -h npc_%objectId%_manage other edit_item 2\">Level 2</a>][<a action=\"bypass -h npc_%objectId%_manage other edit_item 3\">Level 3</a>]";
                        if (this.getClanHall().getFunction(1) != null) {
                           htmlxxxxxxxx.replace(
                              "%tele%",
                              "Stage "
                                 + this.getClanHall().getFunction(1).getLevel()
                                 + "</font> (<font color=\"FFAABB\">"
                                 + this.getClanHall().getFunction(1).getLease()
                                 + "</font>Adena /"
                                 + FeatureConfig.CH_TELE_FEE_RATIO / 1000L / 60L / 60L / 24L
                                 + " Day)"
                           );
                           htmlxxxxxxxx.replace(
                              "%tele_period%",
                              "Withdraw the fee for the next time at "
                                 + Instant.ofEpochMilli(this.getClanHall().getFunction(1).getEndTime()).atZone(ZoneId.systemDefault()).format(DATE_FORMAT)
                           );
                           htmlxxxxxxxx.replace(
                              "%change_tele%",
                              "[<a action=\"bypass -h npc_%objectId%_manage other tele_cancel\">Deactivate</a>][<a action=\"bypass -h npc_%objectId%_manage other edit_tele 1\">Level 1</a>][<a action=\"bypass -h npc_%objectId%_manage other edit_tele 2\">Level 2</a>]"
                           );
                        } else {
                           htmlxxxxxxxx.replace("%tele%", "none");
                           htmlxxxxxxxx.replace("%tele_period%", "none");
                           htmlxxxxxxxx.replace(
                              "%change_tele%",
                              "[<a action=\"bypass -h npc_%objectId%_manage other edit_tele 1\">Level 1</a>][<a action=\"bypass -h npc_%objectId%_manage other edit_tele 2\">Level 2</a>]"
                           );
                        }

                        if (this.getClanHall().getFunction(6) != null) {
                           htmlxxxxxxxx.replace(
                              "%support%",
                              "Stage "
                                 + this.getClanHall().getFunction(6).getLevel()
                                 + "</font> (<font color=\"FFAABB\">"
                                 + this.getClanHall().getFunction(6).getLease()
                                 + "</font>Adena /"
                                 + FeatureConfig.CH_SUPPORT_FEE_RATIO / 1000L / 60L / 60L / 24L
                                 + " Day)"
                           );
                           htmlxxxxxxxx.replace(
                              "%support_period%",
                              "Withdraw the fee for the next time at "
                                 + Instant.ofEpochMilli(this.getClanHall().getFunction(6).getEndTime()).atZone(ZoneId.systemDefault()).format(DATE_FORMAT)
                           );
                           int grade = this.getClanHall().getGrade();
                           switch (grade) {
                              case 0:
                                 htmlxxxxxxxx.replace(
                                    "%change_support%",
                                    "[<a action=\"bypass -h npc_%objectId%_manage other support_cancel\">Deactivate</a>][<a action=\"bypass -h npc_%objectId%_manage other edit_support 1\">Level 1</a>][<a action=\"bypass -h npc_%objectId%_manage other edit_support 2\">Level 2</a>]"
                                 );
                                 break;
                              case 1:
                                 htmlxxxxxxxx.replace(
                                    "%change_support%",
                                    "[<a action=\"bypass -h npc_%objectId%_manage other support_cancel\">Deactivate</a>][<a action=\"bypass -h npc_%objectId%_manage other edit_support 1\">Level 1</a>][<a action=\"bypass -h npc_%objectId%_manage other edit_support 2\">Level 2</a>][<a action=\"bypass -h npc_%objectId%_manage other edit_support 4\">Level 4</a>]"
                                 );
                                 break;
                              case 2:
                                 htmlxxxxxxxx.replace(
                                    "%change_support%",
                                    "[<a action=\"bypass -h npc_%objectId%_manage other support_cancel\">Deactivate</a>][<a action=\"bypass -h npc_%objectId%_manage other edit_support 3\">Level 3</a>][<a action=\"bypass -h npc_%objectId%_manage other edit_support 4\">Level 4</a>][<a action=\"bypass -h npc_%objectId%_manage other edit_support 5\">Level 5</a>]"
                                 );
                                 break;
                              case 3:
                                 htmlxxxxxxxx.replace(
                                    "%change_support%",
                                    "[<a action=\"bypass -h npc_%objectId%_manage other support_cancel\">Deactivate</a>][<a action=\"bypass -h npc_%objectId%_manage other edit_support 3\">Level 3</a>][<a action=\"bypass -h npc_%objectId%_manage other edit_support 5\">Level 5</a>][<a action=\"bypass -h npc_%objectId%_manage other edit_support 7\">Level 7</a>][<a action=\"bypass -h npc_%objectId%_manage other edit_support 8\">Level 8</a>]"
                                 );
                           }
                        } else {
                           htmlxxxxxxxx.replace("%support%", "none");
                           htmlxxxxxxxx.replace("%support_period%", "none");
                           int grade = this.getClanHall().getGrade();
                           switch (grade) {
                              case 0:
                                 htmlxxxxxxxx.replace(
                                    "%change_support%",
                                    "[<a action=\"bypass -h npc_%objectId%_manage other edit_support 1\">Level 1</a>][<a action=\"bypass -h npc_%objectId%_manage other edit_support 2\">Level 2</a>]"
                                 );
                                 break;
                              case 1:
                                 htmlxxxxxxxx.replace(
                                    "%change_support%",
                                    "[<a action=\"bypass -h npc_%objectId%_manage other edit_support 1\">Level 1</a>][<a action=\"bypass -h npc_%objectId%_manage other edit_support 2\">Level 2</a>][<a action=\"bypass -h npc_%objectId%_manage other edit_support 4\">Level 4</a>]"
                                 );
                                 break;
                              case 2:
                                 htmlxxxxxxxx.replace(
                                    "%change_support%",
                                    "[<a action=\"bypass -h npc_%objectId%_manage other edit_support 3\">Level 3</a>][<a action=\"bypass -h npc_%objectId%_manage other edit_support 4\">Level 4</a>][<a action=\"bypass -h npc_%objectId%_manage other edit_support 5\">Level 5</a>]"
                                 );
                                 break;
                              case 3:
                                 htmlxxxxxxxx.replace(
                                    "%change_support%",
                                    "[<a action=\"bypass -h npc_%objectId%_manage other edit_support 3\">Level 3</a>][<a action=\"bypass -h npc_%objectId%_manage other edit_support 5\">Level 5</a>][<a action=\"bypass -h npc_%objectId%_manage other edit_support 7\">Level 7</a>][<a action=\"bypass -h npc_%objectId%_manage other edit_support 8\">Level 8</a>]"
                                 );
                           }
                        }

                        if (this.getClanHall().getFunction(2) != null) {
                           htmlxxxxxxxx.replace(
                              "%item%",
                              "Stage "
                                 + this.getClanHall().getFunction(2).getLevel()
                                 + "</font> (<font color=\"FFAABB\">"
                                 + this.getClanHall().getFunction(2).getLease()
                                 + "</font>Adena /"
                                 + FeatureConfig.CH_ITEM_FEE_RATIO / 1000L / 60L / 60L / 24L
                                 + " Day)"
                           );
                           htmlxxxxxxxx.replace(
                              "%item_period%",
                              "Withdraw the fee for the next time at "
                                 + Instant.ofEpochMilli(this.getClanHall().getFunction(2).getEndTime()).atZone(ZoneId.systemDefault()).format(DATE_FORMAT)
                           );
                           htmlxxxxxxxx.replace(
                              "%change_item%",
                              "[<a action=\"bypass -h npc_%objectId%_manage other item_cancel\">Deactivate</a>][<a action=\"bypass -h npc_%objectId%_manage other edit_item 1\">Level 1</a>][<a action=\"bypass -h npc_%objectId%_manage other edit_item 2\">Level 2</a>][<a action=\"bypass -h npc_%objectId%_manage other edit_item 3\">Level 3</a>]"
                           );
                        } else {
                           htmlxxxxxxxx.replace("%item%", "none");
                           htmlxxxxxxxx.replace("%item_period%", "none");
                           htmlxxxxxxxx.replace(
                              "%change_item%",
                              "[<a action=\"bypass -h npc_%objectId%_manage other edit_item 1\">Level 1</a>][<a action=\"bypass -h npc_%objectId%_manage other edit_item 2\">Level 2</a>][<a action=\"bypass -h npc_%objectId%_manage other edit_item 3\">Level 3</a>]"
                           );
                        }

                        this.sendHtmlMessage(player, htmlxxxxxxxx);
                     } else if (val.equalsIgnoreCase("deco") && !this.getClanHall().isSiegableHall()) {
                        if (st.countTokens() >= 1) {
                           if (this.getClanHall().getOwnerId() == 0) {
                              player.sendMessage("This clan hall has no owner, you cannot change the configuration.");
                              return;
                           }

                           val = st.nextToken();
                           if (val.equalsIgnoreCase("curtains_cancel")) {
                              NpcHtmlMessage htmlxxxxxxxxx = new NpcHtmlMessage(this.getObjectId());
                              htmlxxxxxxxxx.setFile(player, "data/html/clanHallManager/functions-cancel.htm");
                              htmlxxxxxxxxx.replace("%apply%", "deco curtains 0");
                              this.sendHtmlMessage(player, htmlxxxxxxxxx);
                              return;
                           }

                           if (val.equalsIgnoreCase("fixtures_cancel")) {
                              NpcHtmlMessage htmlxxxxxxxxx = new NpcHtmlMessage(this.getObjectId());
                              htmlxxxxxxxxx.setFile(player, "data/html/clanHallManager/functions-cancel.htm");
                              htmlxxxxxxxxx.replace("%apply%", "deco fixtures 0");
                              this.sendHtmlMessage(player, htmlxxxxxxxxx);
                              return;
                           }

                           if (val.equalsIgnoreCase("edit_curtains")) {
                              val = st.nextToken();
                              NpcHtmlMessage html = new NpcHtmlMessage(this.getObjectId());
                              html.setFile(player, "data/html/clanHallManager/functions-apply.htm");
                              html.replace("%name%", "Curtains (Decoration)");
                              int stage = Integer.parseInt(val);

                              html.replace("%cost%", switch (stage) {
                                 case 1 -> FeatureConfig.CH_CURTAIN1_FEE;
                                 default -> FeatureConfig.CH_CURTAIN2_FEE;
                              } + "</font>Adena /" + FeatureConfig.CH_CURTAIN_FEE_RATIO / 1000L / 60L / 60L / 24L + " Day</font>)");
                              html.replace("%use%", "These curtains can be used to decorate the clan hall.");
                              html.replace("%apply%", "deco curtains " + stage);
                              this.sendHtmlMessage(player, html);
                              return;
                           }

                           if (val.equalsIgnoreCase("edit_fixtures")) {
                              val = st.nextToken();
                              NpcHtmlMessage html = new NpcHtmlMessage(this.getObjectId());
                              html.setFile(player, "data/html/clanHallManager/functions-apply.htm");
                              html.replace("%name%", "Front Platform (Decoration)");
                              int stage = Integer.parseInt(val);

                              html.replace("%cost%", switch (stage) {
                                 case 1 -> FeatureConfig.CH_FRONT1_FEE;
                                 default -> FeatureConfig.CH_FRONT2_FEE;
                              } + "</font>Adena /" + FeatureConfig.CH_FRONT_FEE_RATIO / 1000L / 60L / 60L / 24L + " Day</font>)");
                              html.replace("%use%", "Used to decorate the clan hall.");
                              html.replace("%apply%", "deco fixtures " + stage);
                              this.sendHtmlMessage(player, html);
                              return;
                           }

                           if (val.equalsIgnoreCase("curtains")) {
                              if (st.countTokens() >= 1) {
                                 val = st.nextToken();
                                 NpcHtmlMessage htmlxxxxxxxxx = new NpcHtmlMessage(this.getObjectId());
                                 htmlxxxxxxxxx.setFile(player, "data/html/clanHallManager/functions-apply_confirmed.htm");
                                 if (this.getClanHall().getFunction(8) != null && this.getClanHall().getFunction(8).getLevel() == Integer.parseInt(val)) {
                                    htmlxxxxxxxxx.setFile(player, "data/html/clanHallManager/functions-used.htm");
                                    htmlxxxxxxxxx.replace("%val%", "Stage " + val);
                                    this.sendHtmlMessage(player, htmlxxxxxxxxx);
                                    return;
                                 }

                                 int level = Integer.parseInt(val);
                                 int fee;
                                 switch (level) {
                                    case 0:
                                       fee = 0;
                                       htmlxxxxxxxxx.setFile(player, "data/html/clanHallManager/functions-cancel_confirmed.htm");
                                       break;
                                    case 1:
                                       fee = FeatureConfig.CH_CURTAIN1_FEE;
                                       break;
                                    default:
                                       fee = FeatureConfig.CH_CURTAIN2_FEE;
                                 }

                                 if (!this.getClanHall()
                                    .updateFunctions(player, 8, level, fee, FeatureConfig.CH_CURTAIN_FEE_RATIO, this.getClanHall().getFunction(8) == null)) {
                                    htmlxxxxxxxxx.setFile(player, "data/html/clanHallManager/low_adena.htm");
                                    this.sendHtmlMessage(player, htmlxxxxxxxxx);
                                 } else {
                                    this.revalidateDeco(player);
                                 }

                                 this.sendHtmlMessage(player, htmlxxxxxxxxx);
                              }

                              return;
                           }

                           if (val.equalsIgnoreCase("fixtures")) {
                              if (st.countTokens() >= 1) {
                                 val = st.nextToken();
                                 NpcHtmlMessage htmlxxxxxxxxxx = new NpcHtmlMessage(this.getObjectId());
                                 htmlxxxxxxxxxx.setFile(player, "data/html/clanHallManager/functions-apply_confirmed.htm");
                                 if (this.getClanHall().getFunction(7) != null && this.getClanHall().getFunction(7).getLevel() == Integer.parseInt(val)) {
                                    htmlxxxxxxxxxx.setFile(player, "data/html/clanHallManager/functions-used.htm");
                                    htmlxxxxxxxxxx.replace("%val%", "Stage " + val);
                                    this.sendHtmlMessage(player, htmlxxxxxxxxxx);
                                    return;
                                 }

                                 int level = Integer.parseInt(val);
                                 int fee;
                                 switch (level) {
                                    case 0:
                                       fee = 0;
                                       htmlxxxxxxxxxx.setFile(player, "data/html/clanHallManager/functions-cancel_confirmed.htm");
                                       break;
                                    case 1:
                                       fee = FeatureConfig.CH_FRONT1_FEE;
                                       break;
                                    default:
                                       fee = FeatureConfig.CH_FRONT2_FEE;
                                 }

                                 if (!this.getClanHall()
                                    .updateFunctions(player, 7, level, fee, FeatureConfig.CH_FRONT_FEE_RATIO, this.getClanHall().getFunction(7) == null)) {
                                    htmlxxxxxxxxxx.setFile(player, "data/html/clanHallManager/low_adena.htm");
                                    this.sendHtmlMessage(player, htmlxxxxxxxxxx);
                                 } else {
                                    this.revalidateDeco(player);
                                 }

                                 this.sendHtmlMessage(player, htmlxxxxxxxxxx);
                              }

                              return;
                           }
                        }

                        NpcHtmlMessage htmlxxxxxxxxxxx = new NpcHtmlMessage(this.getObjectId());
                        htmlxxxxxxxxxxx.setFile(player, "data/html/clanHallManager/deco.htm");
                        String curtains = "[<a action=\"bypass -h npc_%objectId%_manage deco edit_curtains 1\">Level 1</a>][<a action=\"bypass -h npc_%objectId%_manage deco edit_curtains 2\">Level 2</a>]";
                        String fixtures = "[<a action=\"bypass -h npc_%objectId%_manage deco edit_fixtures 1\">Level 1</a>][<a action=\"bypass -h npc_%objectId%_manage deco edit_fixtures 2\">Level 2</a>]";
                        if (this.getClanHall().getFunction(8) != null) {
                           htmlxxxxxxxxxxx.replace(
                              "%curtain%",
                              "Stage "
                                 + this.getClanHall().getFunction(8).getLevel()
                                 + "</font> (<font color=\"FFAABB\">"
                                 + this.getClanHall().getFunction(8).getLease()
                                 + "</font>Adena /"
                                 + FeatureConfig.CH_CURTAIN_FEE_RATIO / 1000L / 60L / 60L / 24L
                                 + " Day)"
                           );
                           htmlxxxxxxxxxxx.replace(
                              "%curtain_period%",
                              "Withdraw the fee for the next time at "
                                 + Instant.ofEpochMilli(this.getClanHall().getFunction(8).getEndTime()).atZone(ZoneId.systemDefault()).format(DATE_FORMAT)
                           );
                           htmlxxxxxxxxxxx.replace(
                              "%change_curtain%",
                              "[<a action=\"bypass -h npc_%objectId%_manage deco curtains_cancel\">Deactivate</a>][<a action=\"bypass -h npc_%objectId%_manage deco edit_curtains 1\">Level 1</a>][<a action=\"bypass -h npc_%objectId%_manage deco edit_curtains 2\">Level 2</a>]"
                           );
                        } else {
                           htmlxxxxxxxxxxx.replace("%curtain%", "none");
                           htmlxxxxxxxxxxx.replace("%curtain_period%", "none");
                           htmlxxxxxxxxxxx.replace(
                              "%change_curtain%",
                              "[<a action=\"bypass -h npc_%objectId%_manage deco edit_curtains 1\">Level 1</a>][<a action=\"bypass -h npc_%objectId%_manage deco edit_curtains 2\">Level 2</a>]"
                           );
                        }

                        if (this.getClanHall().getFunction(7) != null) {
                           htmlxxxxxxxxxxx.replace(
                              "%fixture%",
                              "Stage "
                                 + this.getClanHall().getFunction(7).getLevel()
                                 + "</font> (<font color=\"FFAABB\">"
                                 + this.getClanHall().getFunction(7).getLease()
                                 + "</font>Adena /"
                                 + FeatureConfig.CH_FRONT_FEE_RATIO / 1000L / 60L / 60L / 24L
                                 + " Day)"
                           );
                           htmlxxxxxxxxxxx.replace(
                              "%fixture_period%",
                              "Withdraw the fee for the next time at "
                                 + Instant.ofEpochMilli(this.getClanHall().getFunction(7).getEndTime()).atZone(ZoneId.systemDefault()).format(DATE_FORMAT)
                           );
                           htmlxxxxxxxxxxx.replace(
                              "%change_fixture%",
                              "[<a action=\"bypass -h npc_%objectId%_manage deco fixtures_cancel\">Deactivate</a>][<a action=\"bypass -h npc_%objectId%_manage deco edit_fixtures 1\">Level 1</a>][<a action=\"bypass -h npc_%objectId%_manage deco edit_fixtures 2\">Level 2</a>]"
                           );
                        } else {
                           htmlxxxxxxxxxxx.replace("%fixture%", "none");
                           htmlxxxxxxxxxxx.replace("%fixture_period%", "none");
                           htmlxxxxxxxxxxx.replace(
                              "%change_fixture%",
                              "[<a action=\"bypass -h npc_%objectId%_manage deco edit_fixtures 1\">Level 1</a>][<a action=\"bypass -h npc_%objectId%_manage deco edit_fixtures 2\">Level 2</a>]"
                           );
                        }

                        this.sendHtmlMessage(player, htmlxxxxxxxxxxx);
                     } else if (val.equalsIgnoreCase("back")) {
                        this.showChatWindow(player);
                     } else {
                        NpcHtmlMessage htmlxxxxxxxxxxxx = new NpcHtmlMessage(this.getObjectId());
                        htmlxxxxxxxxxxxx.setFile(
                           player,
                           this.getClanHall().isSiegableHall() ? "data/html/clanHallManager/manage_siegable.htm" : "data/html/clanHallManager/manage.htm"
                        );
                        this.sendHtmlMessage(player, htmlxxxxxxxxxxxx);
                     }
                  } else {
                     NpcHtmlMessage htmlxxxxxxxxxxxx = new NpcHtmlMessage(1);
                     htmlxxxxxxxxxxxx.setFile(player, "data/html/clanHallManager/not_authorized.htm");
                     this.sendHtmlMessage(player, htmlxxxxxxxxxxxx);
                  }

                  return;
               }

               if (actualCommand.equalsIgnoreCase("support")) {
                  if (player.isCursedWeaponEquipped()) {
                     player.sendMessage("The wielder of a cursed weapon cannot receive outside heals or buffs");
                     return;
                  }

                  this.setTarget(player);
                  if (val.isEmpty()) {
                     return;
                  }

                  try {
                     int skillId = Integer.parseInt(val);

                     try {
                        int skillLevel = 0;
                        if (st.countTokens() >= 1) {
                           skillLevel = Integer.parseInt(st.nextToken());
                        }

                        Skill skill = SkillData.getInstance().getSkill(skillId, skillLevel);
                        if (skill.hasEffectType(EffectType.SUMMON)) {
                           player.doSimultaneousCast(skill);
                        } else {
                           int mpCost = skill.getMpConsume() + skill.getMpInitialConsume();
                           if (!(this.getCurrentMp() >= mpCost) && !FeatureConfig.CH_BUFF_FREE) {
                              NpcHtmlMessage htmlxxxxxxxxxxxx = new NpcHtmlMessage(this.getObjectId());
                              htmlxxxxxxxxxxxx.setFile(player, "data/html/clanHallManager/support-no_mana.htm");
                              htmlxxxxxxxxxxxx.replace("%mp%", String.valueOf((int)this.getCurrentMp()));
                              this.sendHtmlMessage(player, htmlxxxxxxxxxxxx);
                              return;
                           }

                           this.doCast(skill);
                        }

                        if (this.getClanHall().getFunction(6) == null) {
                           return;
                        }

                        if (this.getClanHall().getFunction(6).getLevel() == 0) {
                           return;
                        }

                        NpcHtmlMessage htmlxxxxxxxxxxxx = new NpcHtmlMessage(this.getObjectId());
                        htmlxxxxxxxxxxxx.setFile(player, "data/html/clanHallManager/support-done.htm");
                        htmlxxxxxxxxxxxx.replace("%mp%", String.valueOf((int)this.getCurrentMp()));
                        this.sendHtmlMessage(player, htmlxxxxxxxxxxxx);
                     } catch (Exception var21) {
                        player.sendMessage("Invalid skill level, contact your admin!");
                     }
                  } catch (Exception var22) {
                     player.sendMessage("Invalid skill level, contact your admin!");
                  }

                  return;
               }

               if (actualCommand.equalsIgnoreCase("list_back")) {
                  NpcHtmlMessage htmlxxxxxxxxxxxx = new NpcHtmlMessage(this.getObjectId());
                  String fileName = "data/html/clanHallManager/chamberlain-" + this.getId() + ".htm";
                  File file = new File(ServerConfig.DATAPACK_ROOT, fileName);
                  if (!file.isFile()) {
                     fileName = "data/html/clanHallManager/chamberlain.htm";
                  }

                  htmlxxxxxxxxxxxx.setFile(player, fileName);
                  htmlxxxxxxxxxxxx.replace("%objectId%", String.valueOf(this.getObjectId()));
                  htmlxxxxxxxxxxxx.replace("%npcname%", this.getName());
                  this.sendHtmlMessage(player, htmlxxxxxxxxxxxx);
                  return;
               }

               if (actualCommand.equalsIgnoreCase("support_back")) {
                  if (this.getClanHall().getFunction(6).getLevel() == 0) {
                     return;
                  }

                  NpcHtmlMessage htmlxxxxxxxxxxxx = new NpcHtmlMessage(this.getObjectId());
                  htmlxxxxxxxxxxxx.setFile(player, "data/html/clanHallManager/support" + this.getClanHall().getFunction(6).getLevel() + ".htm");
                  htmlxxxxxxxxxxxx.replace("%mp%", String.valueOf((int)this.getStatus().getCurrentMp()));
                  this.sendHtmlMessage(player, htmlxxxxxxxxxxxx);
                  return;
               }

               if (actualCommand.equalsIgnoreCase("goto")) {
                  ClanHall.ClanHallFunction function = this.getClanHall().getFunction(1);
                  if (function != null) {
                     int teleportLevel = function.getLevel();
                     if (teleportLevel > 0) {
                        TeleportHolder holder = TeleporterData.getInstance().getHolder(this.getId(), "tel" + teleportLevel);
                        if (holder != null) {
                           holder.doTeleport(player, this, Integer.parseInt(command.split(" ")[1]));
                           player.sendPacket(ActionFailed.STATIC_PACKET);
                        }
                     }
                  }

                  return;
               }
            }

            super.onBypassFeedback(player, command);
         }
      }
   }

   private void sendHtmlMessage(Player player, NpcHtmlMessage html) {
      html.replace("%objectId%", String.valueOf(this.getObjectId()));
      html.replace("%npcId%", String.valueOf(this.getId()));
      player.sendPacket(html);
   }

   @Override
   public void showChatWindow(Player player) {
      player.sendPacket(ActionFailed.STATIC_PACKET);
      String fileName = "data/html/clanHallManager/chamberlain-no.htm";
      int condition = this.validateCondition(player);
      if (condition == 3) {
         fileName = "data/html/clanHallManager/chamberlain-" + this.getId() + ".htm";
         File file = new File(ServerConfig.DATAPACK_ROOT, fileName);
         if (!file.isFile()) {
            fileName = "data/html/clanHallManager/chamberlain.htm";
         }
      } else if (condition == 0) {
         fileName = "data/html/clanHallManager/chamberlain-of.htm";
      }

      NpcHtmlMessage html = new NpcHtmlMessage(this.getObjectId());
      html.setFile(player, fileName);
      html.replace("%objectId%", String.valueOf(this.getObjectId()));
      html.replace("%npcId%", String.valueOf(this.getId()));
      player.sendPacket(html);
   }

   protected int validateCondition(Player player) {
      if (this.getClanHall() == null) {
         return 1;
      } else if (player.isGM()) {
         return 3;
      } else if (player.getClan() != null) {
         return this.getClanHall().getOwnerId() == player.getClanId() ? 3 : 0;
      } else {
         return 1;
      }
   }

   public ClanHall getClanHall() {
      if (this._clanHallId < 0) {
         ClanHall temp = ClanHallTable.getInstance().getNearbyClanHall(this.getX(), this.getY(), 500);
         if (temp == null) {
            temp = CHSiegeManager.getInstance().getNearbyClanHall(this);
         }

         if (temp != null) {
            this._clanHallId = temp.getId();
         }

         if (this._clanHallId < 0) {
            return null;
         }
      }

      return ClanHallTable.getInstance().getClanHallById(this._clanHallId);
   }

   private void revalidateDeco(Player player) {
      AuctionableHall ch = ClanHallTable.getInstance().getClanHallByOwner(player.getClan());
      if (ch != null) {
         player.sendPacket(new AgitDecoInfo(ch));
      }
   }
}
