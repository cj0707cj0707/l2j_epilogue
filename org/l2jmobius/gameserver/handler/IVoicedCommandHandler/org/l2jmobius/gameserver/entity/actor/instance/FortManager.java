package org.l2jmobius.gameserver.entity.actor.instance;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.StringTokenizer;
import org.l2jmobius.commons.util.StringUtil;
import org.l2jmobius.gameserver.cache.HtmCache;
import org.l2jmobius.gameserver.config.FeatureConfig;
import org.l2jmobius.gameserver.config.custom.WarehouseSortingConfig;
import org.l2jmobius.gameserver.data.xml.SkillData;
import org.l2jmobius.gameserver.data.xml.TeleporterData;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.enums.creature.InstanceType;
import org.l2jmobius.gameserver.entity.actor.templates.NpcTemplate;
import org.l2jmobius.gameserver.entity.clan.ClanAccess;
import org.l2jmobius.gameserver.entity.teleporter.TeleportHolder;
import org.l2jmobius.gameserver.mechanics.effects.EffectType;
import org.l2jmobius.gameserver.mechanics.siege.Fort;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.network.enums.WarehouseListType;
import org.l2jmobius.gameserver.network.serverpackets.ActionFailed;
import org.l2jmobius.gameserver.network.serverpackets.NpcHtmlMessage;
import org.l2jmobius.gameserver.network.serverpackets.SortedWareHouseWithdrawalList;
import org.l2jmobius.gameserver.network.serverpackets.WareHouseDepositList;
import org.l2jmobius.gameserver.network.serverpackets.WareHouseWithdrawalList;

public class FortManager extends Merchant {
   private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
   protected static final int COND_ALL_FALSE = 0;
   protected static final int COND_BUSY_BECAUSE_OF_SIEGE = 1;
   protected static final int COND_OWNER = 2;

   public FortManager(NpcTemplate template) {
      super(template);
      this.setInstanceType(InstanceType.FortManager);
   }

   @Override
   public boolean isWarehouse() {
      return true;
   }

   private void sendHtmlMessage(Player player, NpcHtmlMessage html) {
      html.replace("%objectId%", String.valueOf(this.getObjectId()));
      html.replace("%npcId%", String.valueOf(this.getId()));
      player.sendPacket(html);
   }

   @Override
   public void onBypassFeedback(Player player, String command) {
      if (player.getLastFolkNPC().getObjectId() == this.getObjectId()) {
         int condition = this.validateCondition(player);
         if (condition > 0) {
            if (condition != 1) {
               if (condition == 2) {
                  StringTokenizer st = new StringTokenizer(command, " ");
                  String actualCommand = st.nextToken();
                  String val = "";
                  if (st.countTokens() >= 1) {
                     val = st.nextToken();
                  }

                  if (actualCommand.equalsIgnoreCase("expel")) {
                     if (player.hasAccess(ClanAccess.CASTLE_BANISH)) {
                        NpcHtmlMessage html = new NpcHtmlMessage(this.getObjectId());
                        html.setFile(player, "data/html/fortress/foreman-expel.htm");
                        html.replace("%objectId%", String.valueOf(this.getObjectId()));
                        player.sendPacket(html);
                     } else {
                        NpcHtmlMessage html = new NpcHtmlMessage(this.getObjectId());
                        html.setFile(player, "data/html/fortress/foreman-noprivs.htm");
                        html.replace("%objectId%", String.valueOf(this.getObjectId()));
                        player.sendPacket(html);
                     }

                     return;
                  }

                  if (actualCommand.equalsIgnoreCase("banish_foreigner")) {
                     if (player.hasAccess(ClanAccess.CASTLE_BANISH)) {
                        this.getFort().banishForeigners();
                        NpcHtmlMessage html = new NpcHtmlMessage(this.getObjectId());
                        html.setFile(player, "data/html/fortress/foreman-expeled.htm");
                        html.replace("%objectId%", String.valueOf(this.getObjectId()));
                        player.sendPacket(html);
                     } else {
                        NpcHtmlMessage html = new NpcHtmlMessage(this.getObjectId());
                        html.setFile(player, "data/html/fortress/foreman-noprivs.htm");
                        html.replace("%objectId%", String.valueOf(this.getObjectId()));
                        player.sendPacket(html);
                     }

                     return;
                  }

                  if (actualCommand.equalsIgnoreCase("receive_report")) {
                     if (this.getFort().getFortState() < 2) {
                        NpcHtmlMessage html = new NpcHtmlMessage(this.getObjectId());
                        html.setFile(player, "data/html/fortress/foreman-report.htm");
                        html.replace("%objectId%", String.valueOf(this.getObjectId()));
                        if (FeatureConfig.FS_MAX_OWN_TIME > 0) {
                           int hour = this.getFort().getTimeTillRebelArmy() / 3600;
                           int minutes = (this.getFort().getTimeTillRebelArmy() - hour * 3600) / 60;
                           html.replace("%hr%", String.valueOf(hour));
                           html.replace("%min%", String.valueOf(minutes));
                        } else {
                           int hour = this.getFort().getOwnedTime() / 3600;
                           int minutes = (this.getFort().getOwnedTime() - hour * 3600) / 60;
                           html.replace("%hr%", String.valueOf(hour));
                           html.replace("%min%", String.valueOf(minutes));
                        }

                        player.sendPacket(html);
                     } else {
                        NpcHtmlMessage html = new NpcHtmlMessage(this.getObjectId());
                        html.setFile(player, "data/html/fortress/foreman-castlereport.htm");
                        html.replace("%objectId%", String.valueOf(this.getObjectId()));
                        if (FeatureConfig.FS_MAX_OWN_TIME > 0) {
                           int hour = this.getFort().getTimeTillRebelArmy() / 3600;
                           int minutes = (this.getFort().getTimeTillRebelArmy() - hour * 3600) / 60;
                           html.replace("%hr%", String.valueOf(hour));
                           html.replace("%min%", String.valueOf(minutes));
                        } else {
                           int hour = this.getFort().getOwnedTime() / 3600;
                           int minutes = (this.getFort().getOwnedTime() - hour * 3600) / 60;
                           html.replace("%hr%", String.valueOf(hour));
                           html.replace("%min%", String.valueOf(minutes));
                        }

                        int var81 = (int)(this.getFort().getTimeTillNextFortUpdate() / 3600L);
                        int var101 = (int)((this.getFort().getTimeTillNextFortUpdate() - var81 * 3600) / 60L);
                        html.replace("%castle%", this.getFort().getContractedCastle().getName());
                        html.replace("%hr2%", String.valueOf(var81));
                        html.replace("%min2%", String.valueOf(var101));
                        player.sendPacket(html);
                     }

                     return;
                  }

                  if (actualCommand.equalsIgnoreCase("operate_door")) {
                     if (player.hasAccess(ClanAccess.CASTLE_OPEN_DOOR)) {
                        if (!val.isEmpty()) {
                           boolean open = Integer.parseInt(val) == 1;

                           while (st.hasMoreTokens()) {
                              this.getFort().openCloseDoor(player, Integer.parseInt(st.nextToken()), open);
                           }

                           if (open) {
                              NpcHtmlMessage html = new NpcHtmlMessage(this.getObjectId());
                              html.setFile(player, "data/html/fortress/foreman-opened.htm");
                              html.replace("%objectId%", String.valueOf(this.getObjectId()));
                              player.sendPacket(html);
                           } else {
                              NpcHtmlMessage html = new NpcHtmlMessage(this.getObjectId());
                              html.setFile(player, "data/html/fortress/foreman-closed.htm");
                              html.replace("%objectId%", String.valueOf(this.getObjectId()));
                              player.sendPacket(html);
                           }
                        } else {
                           NpcHtmlMessage html = new NpcHtmlMessage(this.getObjectId());
                           html.setFile(player, "data/html/fortress/" + this.getTemplate().getId() + "-d.htm");
                           html.replace("%objectId%", String.valueOf(this.getObjectId()));
                           html.replace("%npcname%", this.getName());
                           player.sendPacket(html);
                        }
                     } else {
                        NpcHtmlMessage html = new NpcHtmlMessage(this.getObjectId());
                        html.setFile(player, "data/html/fortress/foreman-noprivs.htm");
                        html.replace("%objectId%", String.valueOf(this.getObjectId()));
                        player.sendPacket(html);
                     }

                     return;
                  }

                  if (actualCommand.equalsIgnoreCase("manage_vault")) {
                     NpcHtmlMessage html = new NpcHtmlMessage(this.getObjectId());
                     if (player.hasAccess(ClanAccess.ACCESS_WAREHOUSE)) {
                        if (val.equalsIgnoreCase("deposit")) {
                           this.showVaultWindowDeposit(player);
                        } else if (val.equalsIgnoreCase("withdraw")) {
                           if (WarehouseSortingConfig.ENABLE_WAREHOUSESORTING_CLAN) {
                              String htmFile = "data/html/mods/WhSortedC.htm";
                              String htmContent = HtmCache.getInstance().getHtm(player, "data/html/mods/WhSortedC.htm");
                              if (htmContent != null) {
                                 NpcHtmlMessage npcHtmlMessage = new NpcHtmlMessage(this.getObjectId());
                                 npcHtmlMessage.setHtml(htmContent);
                                 npcHtmlMessage.replace("%objectId%", String.valueOf(this.getObjectId()));
                                 player.sendPacket(npcHtmlMessage);
                              } else {
                                 LOGGER.warning("Missing htm: data/html/mods/WhSortedC.htm !");
                              }
                           } else {
                              this.showVaultWindowWithdraw(player, null, (byte)0);
                           }
                        } else {
                           html.setFile(player, "data/html/fortress/foreman-vault.htm");
                           this.sendHtmlMessage(player, html);
                        }
                     } else {
                        html.setFile(player, "data/html/fortress/foreman-noprivs.htm");
                        this.sendHtmlMessage(player, html);
                     }

                     return;
                  }

                  if (actualCommand.startsWith("WithdrawSortedC")) {
                     String[] param = command.split("_");
                     if (param.length > 2) {
                        this.showVaultWindowWithdraw(player, WarehouseListType.valueOf(param[1]), SortedWareHouseWithdrawalList.getOrder(param[2]));
                     } else if (param.length > 1) {
                        this.showVaultWindowWithdraw(player, WarehouseListType.valueOf(param[1]), (byte)1);
                     } else {
                        this.showVaultWindowWithdraw(player, WarehouseListType.ALL, (byte)1);
                     }

                     return;
                  }

                  if (actualCommand.equalsIgnoreCase("functions")) {
                     if (val.equalsIgnoreCase("tele")) {
                        NpcHtmlMessage html = new NpcHtmlMessage(this.getObjectId());
                        Fort.FortFunction func = this.getFort().getFunction(1);
                        if (func == null) {
                           html.setFile(player, "data/html/fortress/foreman-nac.htm");
                           this.sendHtmlMessage(player, html);
                        } else {
                           int funcLvl = func.getLevel();
                           if (funcLvl > 0) {
                              TeleportHolder holder = TeleporterData.getInstance().getHolder(this.getId(), "tel" + funcLvl);
                              if (holder != null) {
                                 holder.showTeleportList(player, this, "npc_" + this.getObjectId() + "_goto");
                              }
                           }
                        }
                     } else if (val.equalsIgnoreCase("support")) {
                        NpcHtmlMessage html = new NpcHtmlMessage(this.getObjectId());
                        if (this.getFort().getFunction(5) == null) {
                           html.setFile(player, "data/html/fortress/foreman-nac.htm");
                        } else {
                           html.setFile(player, "data/html/fortress/support" + this.getFort().getFunction(5).getLevel() + ".htm");
                           html.replace("%mp%", String.valueOf((int)this.getCurrentMp()));
                        }

                        this.sendHtmlMessage(player, html);
                     } else if (val.equalsIgnoreCase("back")) {
                        this.showChatWindow(player);
                     } else {
                        NpcHtmlMessage html = new NpcHtmlMessage(this.getObjectId());
                        html.setFile(player, "data/html/fortress/foreman-functions.htm");
                        if (this.getFort().getFunction(4) != null) {
                           html.replace("%xp_regen%", String.valueOf(this.getFort().getFunction(4).getLevel()));
                        } else {
                           html.replace("%xp_regen%", "0");
                        }

                        if (this.getFort().getFunction(2) != null) {
                           html.replace("%hp_regen%", String.valueOf(this.getFort().getFunction(2).getLevel()));
                        } else {
                           html.replace("%hp_regen%", "0");
                        }

                        if (this.getFort().getFunction(3) != null) {
                           html.replace("%mp_regen%", String.valueOf(this.getFort().getFunction(3).getLevel()));
                        } else {
                           html.replace("%mp_regen%", "0");
                        }

                        this.sendHtmlMessage(player, html);
                     }

                     return;
                  }

                  if (actualCommand.equalsIgnoreCase("manage")) {
                     if (player.hasAccess(ClanAccess.CASTLE_MANAGE_FUNCTIONS)) {
                        if (val.equalsIgnoreCase("recovery")) {
                           if (st.countTokens() >= 1) {
                              if (this.getFort().getOwnerClan() == null) {
                                 player.sendMessage("This fortress has no owner, you cannot change the configuration.");
                                 return;
                              }

                              val = st.nextToken();
                              if (val.equalsIgnoreCase("hp_cancel")) {
                                 NpcHtmlMessage htmlx = new NpcHtmlMessage(this.getObjectId());
                                 htmlx.setFile(player, "data/html/fortress/functions-cancel.htm");
                                 htmlx.replace("%apply%", "recovery hp 0");
                                 this.sendHtmlMessage(player, htmlx);
                                 return;
                              }

                              if (val.equalsIgnoreCase("mp_cancel")) {
                                 NpcHtmlMessage htmlx = new NpcHtmlMessage(this.getObjectId());
                                 htmlx.setFile(player, "data/html/fortress/functions-cancel.htm");
                                 htmlx.replace("%apply%", "recovery mp 0");
                                 this.sendHtmlMessage(player, htmlx);
                                 return;
                              }

                              if (val.equalsIgnoreCase("exp_cancel")) {
                                 NpcHtmlMessage htmlx = new NpcHtmlMessage(this.getObjectId());
                                 htmlx.setFile(player, "data/html/fortress/functions-cancel.htm");
                                 htmlx.replace("%apply%", "recovery exp 0");
                                 this.sendHtmlMessage(player, htmlx);
                                 return;
                              }

                              if (val.equalsIgnoreCase("edit_hp")) {
                                 val = st.nextToken();
                                 NpcHtmlMessage html = new NpcHtmlMessage(this.getObjectId());
                                 html.setFile(player, "data/html/fortress/functions-apply.htm");
                                 html.replace("%name%", "(HP Recovery Device)");
                                 int percent = Integer.parseInt(val);

                                 html.replace("%cost%", switch (percent) {
                                    case 300 -> FeatureConfig.FS_HPREG1_FEE;
                                    default -> FeatureConfig.FS_HPREG2_FEE;
                                 } + "</font>Adena /" + FeatureConfig.FS_HPREG_FEE_RATIO / 1000L / 60L / 60L / 24L + " Day</font>)");
                                 html.replace(
                                    "%use%", "Provides additional HP recovery for clan members in the fortress.<font color=\"00FFFF\">" + percent + "%</font>"
                                 );
                                 html.replace("%apply%", "recovery hp " + percent);
                                 this.sendHtmlMessage(player, html);
                                 return;
                              }

                              if (val.equalsIgnoreCase("edit_mp")) {
                                 val = st.nextToken();
                                 NpcHtmlMessage html = new NpcHtmlMessage(this.getObjectId());
                                 html.setFile(player, "data/html/fortress/functions-apply.htm");
                                 html.replace("%name%", "(MP Recovery)");
                                 int percent = Integer.parseInt(val);

                                 html.replace("%cost%", switch (percent) {
                                    case 40 -> FeatureConfig.FS_MPREG1_FEE;
                                    default -> FeatureConfig.FS_MPREG2_FEE;
                                 } + "</font>Adena /" + FeatureConfig.FS_MPREG_FEE_RATIO / 1000L / 60L / 60L / 24L + " Day</font>)");
                                 html.replace(
                                    "%use%", "Provides additional MP recovery for clan members in the fortress.<font color=\"00FFFF\">" + percent + "%</font>"
                                 );
                                 html.replace("%apply%", "recovery mp " + percent);
                                 this.sendHtmlMessage(player, html);
                                 return;
                              }

                              if (val.equalsIgnoreCase("edit_exp")) {
                                 val = st.nextToken();
                                 NpcHtmlMessage html = new NpcHtmlMessage(this.getObjectId());
                                 html.setFile(player, "data/html/fortress/functions-apply.htm");
                                 html.replace("%name%", "(EXP Recovery Device)");
                                 int percent = Integer.parseInt(val);

                                 html.replace("%cost%", switch (percent) {
                                    case 45 -> FeatureConfig.FS_EXPREG1_FEE;
                                    default -> FeatureConfig.FS_EXPREG2_FEE;
                                 } + "</font>Adena /" + FeatureConfig.FS_EXPREG_FEE_RATIO / 1000L / 60L / 60L / 24L + " Day</font>)");
                                 html.replace(
                                    "%use%",
                                    "Restores the Exp of any clan member who is resurrected in the fortress.<font color=\"00FFFF\">" + percent + "%</font>"
                                 );
                                 html.replace("%apply%", "recovery exp " + percent);
                                 this.sendHtmlMessage(player, html);
                                 return;
                              }

                              if (val.equalsIgnoreCase("hp")) {
                                 if (st.countTokens() >= 1) {
                                    val = st.nextToken();
                                    NpcHtmlMessage htmlx = new NpcHtmlMessage(this.getObjectId());
                                    htmlx.setFile(player, "data/html/fortress/functions-apply_confirmed.htm");
                                    if (this.getFort().getFunction(2) != null && this.getFort().getFunction(2).getLevel() == Integer.parseInt(val)) {
                                       htmlx.setFile(player, "data/html/fortress/functions-used.htm");
                                       htmlx.replace("%val%", val + "%");
                                       this.sendHtmlMessage(player, htmlx);
                                       return;
                                    }

                                    int percent = Integer.parseInt(val);
                                    int fee;
                                    switch (percent) {
                                       case 0:
                                          fee = 0;
                                          htmlx.setFile(player, "data/html/fortress/functions-cancel_confirmed.htm");
                                          break;
                                       case 300:
                                          fee = FeatureConfig.FS_HPREG1_FEE;
                                          break;
                                       default:
                                          fee = FeatureConfig.FS_HPREG2_FEE;
                                    }

                                    if (!this.getFort()
                                       .updateFunctions(player, 2, percent, fee, FeatureConfig.FS_HPREG_FEE_RATIO, this.getFort().getFunction(2) == null)) {
                                       htmlx.setFile(player, "data/html/fortress/low_adena.htm");
                                       this.sendHtmlMessage(player, htmlx);
                                    }

                                    this.sendHtmlMessage(player, htmlx);
                                 }

                                 return;
                              }

                              if (val.equalsIgnoreCase("mp")) {
                                 if (st.countTokens() >= 1) {
                                    val = st.nextToken();
                                    NpcHtmlMessage htmlxx = new NpcHtmlMessage(this.getObjectId());
                                    htmlxx.setFile(player, "data/html/fortress/functions-apply_confirmed.htm");
                                    if (this.getFort().getFunction(3) != null && this.getFort().getFunction(3).getLevel() == Integer.parseInt(val)) {
                                       htmlxx.setFile(player, "data/html/fortress/functions-used.htm");
                                       htmlxx.replace("%val%", val + "%");
                                       this.sendHtmlMessage(player, htmlxx);
                                       return;
                                    }

                                    int percent = Integer.parseInt(val);
                                    int fee;
                                    switch (percent) {
                                       case 0:
                                          fee = 0;
                                          htmlxx.setFile(player, "data/html/fortress/functions-cancel_confirmed.htm");
                                          break;
                                       case 40:
                                          fee = FeatureConfig.FS_MPREG1_FEE;
                                          break;
                                       default:
                                          fee = FeatureConfig.FS_MPREG2_FEE;
                                    }

                                    if (!this.getFort()
                                       .updateFunctions(player, 3, percent, fee, FeatureConfig.FS_MPREG_FEE_RATIO, this.getFort().getFunction(3) == null)) {
                                       htmlxx.setFile(player, "data/html/fortress/low_adena.htm");
                                       this.sendHtmlMessage(player, htmlxx);
                                    }

                                    this.sendHtmlMessage(player, htmlxx);
                                 }

                                 return;
                              }

                              if (val.equalsIgnoreCase("exp")) {
                                 if (st.countTokens() >= 1) {
                                    val = st.nextToken();
                                    NpcHtmlMessage htmlxxx = new NpcHtmlMessage(this.getObjectId());
                                    htmlxxx.setFile(player, "data/html/fortress/functions-apply_confirmed.htm");
                                    if (this.getFort().getFunction(4) != null && this.getFort().getFunction(4).getLevel() == Integer.parseInt(val)) {
                                       htmlxxx.setFile(player, "data/html/fortress/functions-used.htm");
                                       htmlxxx.replace("%val%", val + "%");
                                       this.sendHtmlMessage(player, htmlxxx);
                                       return;
                                    }

                                    int percent = Integer.parseInt(val);
                                    int fee;
                                    switch (percent) {
                                       case 0:
                                          fee = 0;
                                          htmlxxx.setFile(player, "data/html/fortress/functions-cancel_confirmed.htm");
                                          break;
                                       case 45:
                                          fee = FeatureConfig.FS_EXPREG1_FEE;
                                          break;
                                       default:
                                          fee = FeatureConfig.FS_EXPREG2_FEE;
                                    }

                                    if (!this.getFort()
                                       .updateFunctions(player, 4, percent, fee, FeatureConfig.FS_EXPREG_FEE_RATIO, this.getFort().getFunction(4) == null)) {
                                       htmlxxx.setFile(player, "data/html/fortress/low_adena.htm");
                                       this.sendHtmlMessage(player, htmlxxx);
                                    }

                                    this.sendHtmlMessage(player, htmlxxx);
                                 }

                                 return;
                              }
                           }

                           NpcHtmlMessage htmlxxxx = new NpcHtmlMessage(this.getObjectId());
                           htmlxxxx.setFile(player, "data/html/fortress/edit_recovery.htm");
                           String hp = "[<a action=\"bypass -h npc_%objectId%_manage recovery edit_hp 300\">300%</a>][<a action=\"bypass -h npc_%objectId%_manage recovery edit_hp 400\">400%</a>]";
                           String exp = "[<a action=\"bypass -h npc_%objectId%_manage recovery edit_exp 45\">45%</a>][<a action=\"bypass -h npc_%objectId%_manage recovery edit_exp 50\">50%</a>]";
                           String mp = "[<a action=\"bypass -h npc_%objectId%_manage recovery edit_mp 40\">40%</a>][<a action=\"bypass -h npc_%objectId%_manage recovery edit_mp 50\">50%</a>]";
                           if (this.getFort().getFunction(2) != null) {
                              htmlxxxx.replace(
                                 "%hp_recovery%",
                                 this.getFort().getFunction(2).getLevel()
                                    + "%</font> (<font color=\"FFAABB\">"
                                    + this.getFort().getFunction(2).getLease()
                                    + "</font>Adena /"
                                    + FeatureConfig.FS_HPREG_FEE_RATIO / 1000L / 60L / 60L / 24L
                                    + " Day)"
                              );
                              htmlxxxx.replace(
                                 "%hp_period%",
                                 "Withdraw the fee for the next time at "
                                    + Instant.ofEpochMilli(this.getFort().getFunction(2).getEndTime()).atZone(ZoneId.systemDefault()).format(DATE_FORMAT)
                              );
                              htmlxxxx.replace(
                                 "%change_hp%",
                                 "[<a action=\"bypass -h npc_%objectId%_manage recovery hp_cancel\">Deactivate</a>][<a action=\"bypass -h npc_%objectId%_manage recovery edit_hp 300\">300%</a>][<a action=\"bypass -h npc_%objectId%_manage recovery edit_hp 400\">400%</a>]"
                              );
                           } else {
                              htmlxxxx.replace("%hp_recovery%", "none");
                              htmlxxxx.replace("%hp_period%", "none");
                              htmlxxxx.replace(
                                 "%change_hp%",
                                 "[<a action=\"bypass -h npc_%objectId%_manage recovery edit_hp 300\">300%</a>][<a action=\"bypass -h npc_%objectId%_manage recovery edit_hp 400\">400%</a>]"
                              );
                           }

                           if (this.getFort().getFunction(4) != null) {
                              htmlxxxx.replace(
                                 "%exp_recovery%",
                                 this.getFort().getFunction(4).getLevel()
                                    + "%</font> (<font color=\"FFAABB\">"
                                    + this.getFort().getFunction(4).getLease()
                                    + "</font>Adena /"
                                    + FeatureConfig.FS_EXPREG_FEE_RATIO / 1000L / 60L / 60L / 24L
                                    + " Day)"
                              );
                              htmlxxxx.replace(
                                 "%exp_period%",
                                 "Withdraw the fee for the next time at "
                                    + Instant.ofEpochMilli(this.getFort().getFunction(4).getEndTime()).atZone(ZoneId.systemDefault()).format(DATE_FORMAT)
                              );
                              htmlxxxx.replace(
                                 "%change_exp%",
                                 "[<a action=\"bypass -h npc_%objectId%_manage recovery exp_cancel\">Deactivate</a>][<a action=\"bypass -h npc_%objectId%_manage recovery edit_exp 45\">45%</a>][<a action=\"bypass -h npc_%objectId%_manage recovery edit_exp 50\">50%</a>]"
                              );
                           } else {
                              htmlxxxx.replace("%exp_recovery%", "none");
                              htmlxxxx.replace("%exp_period%", "none");
                              htmlxxxx.replace(
                                 "%change_exp%",
                                 "[<a action=\"bypass -h npc_%objectId%_manage recovery edit_exp 45\">45%</a>][<a action=\"bypass -h npc_%objectId%_manage recovery edit_exp 50\">50%</a>]"
                              );
                           }

                           if (this.getFort().getFunction(3) != null) {
                              htmlxxxx.replace(
                                 "%mp_recovery%",
                                 this.getFort().getFunction(3).getLevel()
                                    + "%</font> (<font color=\"FFAABB\">"
                                    + this.getFort().getFunction(3).getLease()
                                    + "</font>Adena /"
                                    + FeatureConfig.FS_MPREG_FEE_RATIO / 1000L / 60L / 60L / 24L
                                    + " Day)"
                              );
                              htmlxxxx.replace(
                                 "%mp_period%",
                                 "Withdraw the fee for the next time at "
                                    + Instant.ofEpochMilli(this.getFort().getFunction(3).getEndTime()).atZone(ZoneId.systemDefault()).format(DATE_FORMAT)
                              );
                              htmlxxxx.replace(
                                 "%change_mp%",
                                 "[<a action=\"bypass -h npc_%objectId%_manage recovery mp_cancel\">Deactivate</a>][<a action=\"bypass -h npc_%objectId%_manage recovery edit_mp 40\">40%</a>][<a action=\"bypass -h npc_%objectId%_manage recovery edit_mp 50\">50%</a>]"
                              );
                           } else {
                              htmlxxxx.replace("%mp_recovery%", "none");
                              htmlxxxx.replace("%mp_period%", "none");
                              htmlxxxx.replace(
                                 "%change_mp%",
                                 "[<a action=\"bypass -h npc_%objectId%_manage recovery edit_mp 40\">40%</a>][<a action=\"bypass -h npc_%objectId%_manage recovery edit_mp 50\">50%</a>]"
                              );
                           }

                           this.sendHtmlMessage(player, htmlxxxx);
                        } else if (val.equalsIgnoreCase("other")) {
                           if (st.countTokens() >= 1) {
                              if (this.getFort().getOwnerClan() == null) {
                                 player.sendMessage("This fortress has no owner, you cannot change the configuration.");
                                 return;
                              }

                              val = st.nextToken();
                              if (val.equalsIgnoreCase("tele_cancel")) {
                                 NpcHtmlMessage htmlxxxxx = new NpcHtmlMessage(this.getObjectId());
                                 htmlxxxxx.setFile(player, "data/html/fortress/functions-cancel.htm");
                                 htmlxxxxx.replace("%apply%", "other tele 0");
                                 this.sendHtmlMessage(player, htmlxxxxx);
                                 return;
                              }

                              if (val.equalsIgnoreCase("support_cancel")) {
                                 NpcHtmlMessage htmlxxxxx = new NpcHtmlMessage(this.getObjectId());
                                 htmlxxxxx.setFile(player, "data/html/fortress/functions-cancel.htm");
                                 htmlxxxxx.replace("%apply%", "other support 0");
                                 this.sendHtmlMessage(player, htmlxxxxx);
                                 return;
                              }

                              if (val.equalsIgnoreCase("edit_support")) {
                                 val = st.nextToken();
                                 NpcHtmlMessage html = new NpcHtmlMessage(this.getObjectId());
                                 html.setFile(player, "data/html/fortress/functions-apply.htm");
                                 html.replace("%name%", "Insignia (Supplementary Magic)");
                                 int stage = Integer.parseInt(val);

                                 html.replace("%cost%", switch (stage) {
                                    case 1 -> FeatureConfig.FS_SUPPORT1_FEE;
                                    default -> FeatureConfig.FS_SUPPORT2_FEE;
                                 } + "</font>Adena /" + FeatureConfig.FS_SUPPORT_FEE_RATIO / 1000L / 60L / 60L / 24L + " Day</font>)");
                                 html.replace("%use%", "Enables the use of supplementary magic.");
                                 html.replace("%apply%", "other support " + stage);
                                 this.sendHtmlMessage(player, html);
                                 return;
                              }

                              if (val.equalsIgnoreCase("edit_tele")) {
                                 val = st.nextToken();
                                 NpcHtmlMessage html = new NpcHtmlMessage(this.getObjectId());
                                 html.setFile(player, "data/html/fortress/functions-apply.htm");
                                 html.replace("%name%", "Mirror (Teleportation Device)");
                                 int stage = Integer.parseInt(val);

                                 html.replace("%cost%", switch (stage) {
                                    case 1 -> FeatureConfig.FS_TELE1_FEE;
                                    default -> FeatureConfig.FS_TELE2_FEE;
                                 } + "</font>Adena /" + FeatureConfig.FS_TELE_FEE_RATIO / 1000L / 60L / 60L / 24L + " Day</font>)");
                                 html.replace(
                                    "%use%", "Teleports clan members in a fort to the target <font color=\"00FFFF\">Stage " + stage + "</font> staging area"
                                 );
                                 html.replace("%apply%", "other tele " + stage);
                                 this.sendHtmlMessage(player, html);
                                 return;
                              }

                              if (val.equalsIgnoreCase("tele")) {
                                 if (st.countTokens() >= 1) {
                                    val = st.nextToken();
                                    NpcHtmlMessage htmlxxxxx = new NpcHtmlMessage(this.getObjectId());
                                    htmlxxxxx.setFile(player, "data/html/fortress/functions-apply_confirmed.htm");
                                    if (this.getFort().getFunction(1) != null && this.getFort().getFunction(1).getLevel() == Integer.parseInt(val)) {
                                       htmlxxxxx.setFile(player, "data/html/fortress/functions-used.htm");
                                       htmlxxxxx.replace("%val%", "Stage " + val);
                                       this.sendHtmlMessage(player, htmlxxxxx);
                                       return;
                                    }

                                    int level = Integer.parseInt(val);
                                    int fee;
                                    switch (level) {
                                       case 0:
                                          fee = 0;
                                          htmlxxxxx.setFile(player, "data/html/fortress/functions-cancel_confirmed.htm");
                                          break;
                                       case 1:
                                          fee = FeatureConfig.FS_TELE1_FEE;
                                          break;
                                       default:
                                          fee = FeatureConfig.FS_TELE2_FEE;
                                    }

                                    if (!this.getFort()
                                       .updateFunctions(player, 1, level, fee, FeatureConfig.FS_TELE_FEE_RATIO, this.getFort().getFunction(1) == null)) {
                                       htmlxxxxx.setFile(player, "data/html/fortress/low_adena.htm");
                                       this.sendHtmlMessage(player, htmlxxxxx);
                                    }

                                    this.sendHtmlMessage(player, htmlxxxxx);
                                 }

                                 return;
                              }

                              if (val.equalsIgnoreCase("support")) {
                                 if (st.countTokens() >= 1) {
                                    val = st.nextToken();
                                    NpcHtmlMessage htmlxxxxxx = new NpcHtmlMessage(this.getObjectId());
                                    htmlxxxxxx.setFile(player, "data/html/fortress/functions-apply_confirmed.htm");
                                    if (this.getFort().getFunction(5) != null && this.getFort().getFunction(5).getLevel() == Integer.parseInt(val)) {
                                       htmlxxxxxx.setFile(player, "data/html/fortress/functions-used.htm");
                                       htmlxxxxxx.replace("%val%", "Stage " + val);
                                       this.sendHtmlMessage(player, htmlxxxxxx);
                                       return;
                                    }

                                    int level = Integer.parseInt(val);
                                    int fee;
                                    switch (level) {
                                       case 0:
                                          fee = 0;
                                          htmlxxxxxx.setFile(player, "data/html/fortress/functions-cancel_confirmed.htm");
                                          break;
                                       case 1:
                                          fee = FeatureConfig.FS_SUPPORT1_FEE;
                                          break;
                                       default:
                                          fee = FeatureConfig.FS_SUPPORT2_FEE;
                                    }

                                    if (!this.getFort()
                                       .updateFunctions(player, 5, level, fee, FeatureConfig.FS_SUPPORT_FEE_RATIO, this.getFort().getFunction(5) == null)) {
                                       htmlxxxxxx.setFile(player, "data/html/fortress/low_adena.htm");
                                       this.sendHtmlMessage(player, htmlxxxxxx);
                                    } else {
                                       this.sendHtmlMessage(player, htmlxxxxxx);
                                    }
                                 }

                                 return;
                              }
                           }

                           NpcHtmlMessage htmlxxxxxxx = new NpcHtmlMessage(this.getObjectId());
                           htmlxxxxxxx.setFile(player, "data/html/fortress/edit_other.htm");
                           String tele = "[<a action=\"bypass -h npc_%objectId%_manage other edit_tele 1\">Level 1</a>][<a action=\"bypass -h npc_%objectId%_manage other edit_tele 2\">Level 2</a>]";
                           String support = "[<a action=\"bypass -h npc_%objectId%_manage other edit_support 1\">Level 1</a>][<a action=\"bypass -h npc_%objectId%_manage other edit_support 2\">Level 2</a>]";
                           if (this.getFort().getFunction(1) != null) {
                              htmlxxxxxxx.replace(
                                 "%tele%",
                                 "Stage "
                                    + this.getFort().getFunction(1).getLevel()
                                    + "</font> (<font color=\"FFAABB\">"
                                    + this.getFort().getFunction(1).getLease()
                                    + "</font>Adena /"
                                    + FeatureConfig.FS_TELE_FEE_RATIO / 1000L / 60L / 60L / 24L
                                    + " Day)"
                              );
                              htmlxxxxxxx.replace(
                                 "%tele_period%",
                                 "Withdraw the fee for the next time at "
                                    + Instant.ofEpochMilli(this.getFort().getFunction(1).getEndTime()).atZone(ZoneId.systemDefault()).format(DATE_FORMAT)
                              );
                              htmlxxxxxxx.replace(
                                 "%change_tele%",
                                 "[<a action=\"bypass -h npc_%objectId%_manage other tele_cancel\">Deactivate</a>][<a action=\"bypass -h npc_%objectId%_manage other edit_tele 1\">Level 1</a>][<a action=\"bypass -h npc_%objectId%_manage other edit_tele 2\">Level 2</a>]"
                              );
                           } else {
                              htmlxxxxxxx.replace("%tele%", "none");
                              htmlxxxxxxx.replace("%tele_period%", "none");
                              htmlxxxxxxx.replace(
                                 "%change_tele%",
                                 "[<a action=\"bypass -h npc_%objectId%_manage other edit_tele 1\">Level 1</a>][<a action=\"bypass -h npc_%objectId%_manage other edit_tele 2\">Level 2</a>]"
                              );
                           }

                           if (this.getFort().getFunction(5) != null) {
                              htmlxxxxxxx.replace(
                                 "%support%",
                                 "Stage "
                                    + this.getFort().getFunction(5).getLevel()
                                    + "</font> (<font color=\"FFAABB\">"
                                    + this.getFort().getFunction(5).getLease()
                                    + "</font>Adena /"
                                    + FeatureConfig.FS_SUPPORT_FEE_RATIO / 1000L / 60L / 60L / 24L
                                    + " Day)"
                              );
                              htmlxxxxxxx.replace(
                                 "%support_period%",
                                 "Withdraw the fee for the next time at "
                                    + Instant.ofEpochMilli(this.getFort().getFunction(5).getEndTime()).atZone(ZoneId.systemDefault()).format(DATE_FORMAT)
                              );
                              htmlxxxxxxx.replace(
                                 "%change_support%",
                                 "[<a action=\"bypass -h npc_%objectId%_manage other support_cancel\">Deactivate</a>][<a action=\"bypass -h npc_%objectId%_manage other edit_support 1\">Level 1</a>][<a action=\"bypass -h npc_%objectId%_manage other edit_support 2\">Level 2</a>]"
                              );
                           } else {
                              htmlxxxxxxx.replace("%support%", "none");
                              htmlxxxxxxx.replace("%support_period%", "none");
                              htmlxxxxxxx.replace(
                                 "%change_support%",
                                 "[<a action=\"bypass -h npc_%objectId%_manage other edit_support 1\">Level 1</a>][<a action=\"bypass -h npc_%objectId%_manage other edit_support 2\">Level 2</a>]"
                              );
                           }

                           this.sendHtmlMessage(player, htmlxxxxxxx);
                        } else if (val.equalsIgnoreCase("back")) {
                           this.showChatWindow(player);
                        } else {
                           NpcHtmlMessage htmlxxxxxxxx = new NpcHtmlMessage(this.getObjectId());
                           htmlxxxxxxxx.setFile(player, "data/html/fortress/manage.htm");
                           this.sendHtmlMessage(player, htmlxxxxxxxx);
                        }
                     } else {
                        NpcHtmlMessage htmlxxxxxxxx = new NpcHtmlMessage(this.getObjectId());
                        htmlxxxxxxxx.setFile(player, "data/html/fortress/foreman-noprivs.htm");
                        this.sendHtmlMessage(player, htmlxxxxxxxx);
                     }

                     return;
                  }

                  if (actualCommand.equalsIgnoreCase("support")) {
                     this.setTarget(player);
                     if (val.isEmpty()) {
                        return;
                     }

                     try {
                        int skillId = Integer.parseInt(val);

                        try {
                           if (this.getFort().getFunction(5) == null) {
                              return;
                           }

                           if (this.getFort().getFunction(5).getLevel() == 0) {
                              return;
                           }

                           NpcHtmlMessage htmlxxxxxxxx = new NpcHtmlMessage(this.getObjectId());
                           int skillLevel = 0;
                           if (st.countTokens() >= 1) {
                              skillLevel = Integer.parseInt(st.nextToken());
                           }

                           Skill skill = SkillData.getInstance().getSkill(skillId, skillLevel);
                           if (skill.hasEffectType(EffectType.SUMMON)) {
                              player.doCast(skill);
                           } else {
                              if (!(skill.getMpConsume() + skill.getMpInitialConsume() <= this.getCurrentMp())) {
                                 htmlxxxxxxxx.setFile(player, "data/html/fortress/support-no_mana.htm");
                                 htmlxxxxxxxx.replace("%mp%", String.valueOf((int)this.getCurrentMp()));
                                 this.sendHtmlMessage(player, htmlxxxxxxxx);
                                 return;
                              }

                              this.doCast(skill);
                           }

                           htmlxxxxxxxx.setFile(player, "data/html/fortress/support-done.htm");
                           htmlxxxxxxxx.replace("%mp%", String.valueOf((int)this.getCurrentMp()));
                           this.sendHtmlMessage(player, htmlxxxxxxxx);
                        } catch (Exception var11) {
                           player.sendMessage("Invalid skill level, contact your admin!");
                        }
                     } catch (Exception var12) {
                        player.sendMessage("Invalid skill level, contact your admin!");
                     }

                     return;
                  }

                  if (actualCommand.equalsIgnoreCase("support_back")) {
                     if (this.getFort().getFunction(5).getLevel() == 0) {
                        return;
                     }

                     NpcHtmlMessage htmlxxxxxxxxx = new NpcHtmlMessage(this.getObjectId());
                     htmlxxxxxxxxx.setFile(player, "data/html/fortress/support" + this.getFort().getFunction(5).getLevel() + ".htm");
                     htmlxxxxxxxxx.replace("%mp%", String.valueOf((int)this.getStatus().getCurrentMp()));
                     this.sendHtmlMessage(player, htmlxxxxxxxxx);
                     return;
                  }

                  if (actualCommand.equalsIgnoreCase("goto")) {
                     Fort.FortFunction func = this.getFort().getFunction(1);
                     if (func == null) {
                        return;
                     }

                     String listName = command.split(" ")[1];
                     TeleportHolder holder = TeleporterData.getInstance().getHolder(this.getId(), listName);
                     if (holder == null) {
                        LOGGER.warning(player + " requested unknown teleport list: " + listName + " for npc: " + this.getId() + "!");
                        return;
                     }

                     holder.doTeleport(player, this, StringUtil.parseNextInt(st, -1));
                     return;
                  }

                  super.onBypassFeedback(player, command);
               }
            }
         }
      }
   }

   @Override
   public void showChatWindow(Player player) {
      player.sendPacket(ActionFailed.STATIC_PACKET);
      String filename = "data/html/fortress/foreman-no.htm";
      int condition = this.validateCondition(player);
      if (condition > 0) {
         if (condition == 1) {
            filename = "data/html/fortress/foreman-busy.htm";
         } else if (condition == 2) {
            filename = "data/html/fortress/foreman.htm";
         }
      }

      NpcHtmlMessage html = new NpcHtmlMessage(this.getObjectId());
      html.setFile(player, filename);
      html.replace("%objectId%", String.valueOf(this.getObjectId()));
      html.replace("%npcname%", this.getName());
      player.sendPacket(html);
   }

   protected int validateCondition(Player player) {
      if (this.getFort() != null && this.getFort().getResidenceId() > 0 && player.getClan() != null) {
         if (this.getFort().getZone().isActive()) {
            return 1;
         }

         if (this.getFort().getOwnerClan() != null && this.getFort().getOwnerClan().getId() == player.getClanId()) {
            return 2;
         }
      }

      return 0;
   }

   private void showVaultWindowDeposit(Player player) {
      player.sendPacket(ActionFailed.STATIC_PACKET);
      player.setActiveWarehouse(player.getClan().getWarehouse());
      player.sendPacket(new WareHouseDepositList(player, 4));
   }

   private void showVaultWindowWithdraw(Player player, WarehouseListType itemtype, byte sortorder) {
      if (!player.isClanLeader() && !player.hasAccess(ClanAccess.ACCESS_WAREHOUSE)) {
         NpcHtmlMessage html = new NpcHtmlMessage(this.getObjectId());
         html.setFile(player, "data/html/fortress/foreman-noprivs.htm");
         this.sendHtmlMessage(player, html);
      } else {
         player.sendPacket(ActionFailed.STATIC_PACKET);
         player.setActiveWarehouse(player.getClan().getWarehouse());
         if (itemtype != null) {
            player.sendPacket(new SortedWareHouseWithdrawalList(player, 4, itemtype, sortorder));
         } else {
            player.sendPacket(new WareHouseWithdrawalList(player, 4));
         }
      }
   }
}
