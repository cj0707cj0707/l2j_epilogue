package org.l2jmobius.gameserver.entity.actor.instance;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.Map.Entry;
import org.l2jmobius.commons.util.StringUtil;
import org.l2jmobius.gameserver.config.PlayerConfig;
import org.l2jmobius.gameserver.config.custom.SchemeBufferConfig;
import org.l2jmobius.gameserver.data.SchemeBufferTable;
import org.l2jmobius.gameserver.data.xml.SkillData;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.Summon;
import org.l2jmobius.gameserver.entity.actor.holders.npc.BuffSkillHolder;
import org.l2jmobius.gameserver.entity.actor.templates.NpcTemplate;
import org.l2jmobius.gameserver.entity.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.network.serverpackets.NpcHtmlMessage;
import org.l2jmobius.gameserver.util.HtmlUtil;

public class SchemeBuffer extends Npc {
   private static final int PAGE_LIMIT = 6;
   private static final int SCHEME_NAME_MAX_LENGTH = 14;
   private static final int TYPES_PER_ROW = 4;
   private static final int UI_WIDTH = 256;
   private static final int UI_HALF_WIDTH = 128;
   private static final int ICON_COL_WIDTH = 40;
   private static final int BTN_COL_WIDTH = 32;
   private static final int NAME_COL_WIDTH = 184;
   private static final int FOOTER_SIDE_WIDTH = 70;
   private static final int PAGE_CENTER_WIDTH = 116;

   public SchemeBuffer(NpcTemplate template) {
      super(template);
   }

   @Override
   public void onBypassFeedback(Player player, String commandValue) {
      if (player != null && commandValue != null && !commandValue.isEmpty()) {
         SchemeBufferTable schemeBufferTable = SchemeBufferTable.getInstance();
         SkillData skillData = SkillData.getInstance();
         StringTokenizer tokenizer = new StringTokenizer(commandValue.replace("createscheme ", "createscheme;"), ";");
         if (tokenizer.hasMoreTokens()) {
            String currentCommand = tokenizer.nextToken();
            if (currentCommand.startsWith("menu")) {
               this.showMainMenu(player);
            } else if (currentCommand.startsWith("cleanup")) {
               player.stopAllEffects();
               Summon summon = player.getSummon();
               if (summon != null) {
                  summon.stopAllEffects();
               }

               this.showMainMenu(player);
            } else if (currentCommand.startsWith("heal")) {
               player.setCurrentHpMp(player.getMaxHp(), player.getMaxMp());
               player.setCurrentCp(player.getMaxCp());
               Summon summon = player.getSummon();
               if (summon != null) {
                  summon.setCurrentHpMp(summon.getMaxHp(), summon.getMaxMp());
               }

               this.showMainMenu(player);
            } else if (currentCommand.startsWith("support")) {
               this.showGiveBuffsWindow(player);
            } else if (currentCommand.startsWith("givebuffs")) {
               if (tokenizer.hasMoreTokens()) {
                  String schemeName = tokenizer.nextToken();
                  if (tokenizer.hasMoreTokens()) {
                     tokenizer.nextToken();
                  }

                  Map<String, List<Integer>> playerSchemes = schemeBufferTable.getPlayerSchemes(player.getObjectId());
                  if (playerSchemes != null && playerSchemes.containsKey(schemeName)) {
                     List<Integer> scheme = schemeBufferTable.getScheme(player.getObjectId(), schemeName);
                     int cost = getFee(scheme);
                     Creature target = player;
                     if (tokenizer.hasMoreTokens() && "pet".equalsIgnoreCase(tokenizer.nextToken())) {
                        target = player.getSummon();
                     }

                     if (target == null) {
                        player.sendMessage("You don't have a pet.");
                     } else {
                        if (cost == 0
                           || SchemeBufferConfig.BUFFER_ITEM_ID == 57 && player.reduceAdena(ItemProcessType.FEE, cost, this, true)
                           || SchemeBufferConfig.BUFFER_ITEM_ID != 57
                              && player.destroyItemByItemId(ItemProcessType.FEE, SchemeBufferConfig.BUFFER_ITEM_ID, cost, player, true)) {
                           for (int skillId : scheme) {
                              BuffSkillHolder holder = schemeBufferTable.getAvailableBuff(skillId);
                              if (holder != null) {
                                 Skill skill = skillData.getSkill(skillId, holder.getLevel());
                                 if (skill != null) {
                                    skill.applyEffects(this, target);
                                 }
                              }
                           }
                        }
                     }
                  } else {
                     player.sendMessage("Invalid scheme name: " + schemeName + ".");
                     this.showGiveBuffsWindow(player);
                  }
               }
            } else if (currentCommand.startsWith("editschemes")) {
               if (tokenizer.countTokens() >= 3) {
                  String groupType = tokenizer.nextToken();
                  String schemeNamex = tokenizer.nextToken();
                  Integer page = parseUnsignedInt(tokenizer.nextToken());
                  if (page != null) {
                     this.showEditSchemeWindow(player, groupType, schemeNamex, page);
                  }
               }
            } else if (currentCommand.startsWith("skill")) {
               if (tokenizer.countTokens() >= 4) {
                  String groupType = tokenizer.nextToken();
                  String schemeNamex = tokenizer.nextToken();
                  Integer skillIdx = parseUnsignedInt(tokenizer.nextToken());
                  Integer page = parseUnsignedInt(tokenizer.nextToken());
                  if (skillIdx != null && page != null) {
                     Map<String, List<Integer>> playerSchemes = schemeBufferTable.getPlayerSchemes(player.getObjectId());
                     if (playerSchemes != null && playerSchemes.containsKey(schemeNamex)) {
                        List<Integer> schemeSkills = schemeBufferTable.getScheme(player.getObjectId(), schemeNamex);
                        if (currentCommand.startsWith("skillselect") && !schemeNamex.equalsIgnoreCase("none")) {
                           Skill skill = skillData.getSkill(skillIdx, 1);
                           if (skill == null) {
                              this.showEditSchemeWindow(player, groupType, schemeNamex, page);
                              return;
                           }

                           if (skill.isDance()) {
                              if (getCountOf(schemeSkills, true) < PlayerConfig.DANCES_MAX_AMOUNT) {
                                 if (!schemeSkills.contains(skillIdx)) {
                                    schemeBufferTable.addSkillToScheme(player.getObjectId(), schemeNamex, skillIdx);
                                 }
                              } else {
                                 player.sendMessage("This scheme has reached the maximum amount of dances/songs.");
                              }
                           } else if (getCountOf(schemeSkills, false) < player.getStat().getMaxBuffCount()) {
                              if (!schemeSkills.contains(skillIdx)) {
                                 schemeBufferTable.addSkillToScheme(player.getObjectId(), schemeNamex, skillIdx);
                              }
                           } else {
                              player.sendMessage("This scheme has reached the maximum amount of buffs.");
                           }
                        } else if (currentCommand.startsWith("skillunselect")) {
                           schemeBufferTable.removeSkillFromScheme(player.getObjectId(), schemeNamex, skillIdx);
                        }

                        this.showEditSchemeWindow(player, groupType, schemeNamex, page);
                     } else {
                        player.sendMessage("Invalid scheme name: " + schemeNamex + ".");
                        this.showGiveBuffsWindow(player);
                     }
                  }
               }
            } else if (currentCommand.startsWith("createscheme")) {
               if (!tokenizer.hasMoreTokens()) {
                  player.sendMessage("Scheme's name must contain up to 14 chars.");
               } else {
                  String schemeNamex = tokenizer.nextToken().trim();
                  if (!schemeNamex.isEmpty() && schemeNamex.length() <= 14) {
                     String normalized = schemeNamex.replace(" ", "")
                        .replace(".", "")
                        .replace(",", "")
                        .replace("-", "")
                        .replace("+", "")
                        .replace("!", "")
                        .replace("?", "");
                     if (!StringUtil.isAlphaNumeric(normalized)) {
                        player.sendMessage("Please use plain alphanumeric characters.");
                     } else {
                        Map<String, List<Integer>> playerSchemes = schemeBufferTable.getPlayerSchemes(player.getObjectId());
                        if (playerSchemes != null) {
                           if (playerSchemes.size() >= SchemeBufferConfig.BUFFER_MAX_SCHEMES) {
                              player.sendMessage("Maximum schemes amount is already reached.");
                              return;
                           }

                           if (playerSchemes.containsKey(schemeNamex)) {
                              player.sendMessage("The scheme name already exists: " + schemeNamex + ".");
                              return;
                           }
                        }

                        schemeBufferTable.setScheme(player.getObjectId(), schemeNamex, new ArrayList<>());
                        this.showGiveBuffsWindow(player);
                     }
                  } else {
                     player.sendMessage("Scheme's name must contain up to 14 chars.");
                  }
               }
            } else if (currentCommand.startsWith("deletescheme")) {
               if (!tokenizer.hasMoreTokens()) {
                  player.sendMessage("This scheme name is invalid.");
                  this.showGiveBuffsWindow(player);
               } else {
                  String schemeNamex = tokenizer.nextToken();
                  schemeBufferTable.deleteScheme(player.getObjectId(), schemeNamex);
                  this.showGiveBuffsWindow(player);
               }
            } else if (currentCommand.startsWith("manual")) {
               String category = tokenizer.hasMoreTokens() ? tokenizer.nextToken() : "Buffs";
               int page = 1;
               if (tokenizer.hasMoreTokens()) {
                  Integer parsed = parseUnsignedInt(tokenizer.nextToken());
                  if (parsed == null) {
                     this.showMainMenu(player);
                     return;
                  }

                  page = parsed;
               }

               String targetType = tokenizer.hasMoreTokens() ? tokenizer.nextToken() : "me";
               this.showManualWindow(player, category, page, targetType);
            } else if (currentCommand.startsWith("castbuff")) {
               if (tokenizer.countTokens() >= 4) {
                  Integer skillIdx = parseUnsignedInt(tokenizer.nextToken());
                  String category = tokenizer.nextToken();
                  Integer page = parseUnsignedInt(tokenizer.nextToken());
                  String targetType = tokenizer.nextToken();
                  if (skillIdx != null && page != null) {
                     Creature targetx = (Creature)("pet".equalsIgnoreCase(targetType) ? player.getSummon() : player);
                     if (targetx == null) {
                        player.sendMessage("You don't have a pet.");
                        this.showManualWindow(player, category, page, targetType);
                     } else {
                        BuffSkillHolder holder = schemeBufferTable.getAvailableBuff(category, skillIdx);
                        if (holder != null) {
                           Skill skillx = skillData.getSkill(skillIdx, holder.getLevel());
                           if (skillx != null) {
                              skillx.applyEffects(this, targetx);
                           }
                        }

                        this.showManualWindow(player, category, page, targetType);
                     }
                  }
               }
            } else if (currentCommand.startsWith("autobuff")) {
               String targetType = tokenizer.hasMoreTokens() ? tokenizer.nextToken() : "me";
               this.applyAutoBuff(player, targetType);
            } else {
               super.onBypassFeedback(player, commandValue);
            }
         }
      }
   }

   @Override
   public String getHtmlPath(int npcId, int value) {
      return "data/html/mods/SchemeBuffer/" + (value == 0 ? Integer.toString(npcId) : npcId + "-" + value) + ".htm";
   }

   private void showMainMenu(Player player) {
      NpcHtmlMessage html = new NpcHtmlMessage(this.getObjectId());
      html.setFile(player, this.getHtmlPath(this.getId(), 0));
      html.replace("%objectId%", this.getObjectId());
      player.sendPacket(html);
   }

   private void showGiveBuffsWindow(Player player) {
      StringBuilder htmlBuilder = new StringBuilder(200);
      Map<String, List<Integer>> playerSchemes = SchemeBufferTable.getInstance().getPlayerSchemes(player.getObjectId());
      if (playerSchemes != null && !playerSchemes.isEmpty()) {
         for (Entry<String, List<Integer>> scheme : playerSchemes.entrySet()) {
            int cost = getFee(scheme.getValue());
            htmlBuilder.append("<font color=\"LEVEL\">")
               .append(scheme.getKey())
               .append(" [")
               .append(scheme.getValue().size())
               .append(" skill(s)]")
               .append(cost > 0 ? " - cost: " + NumberFormat.getInstance(Locale.ENGLISH).format((long)cost) : "")
               .append("</font><br1>");
            htmlBuilder.append("<a action=\"bypass -h npc_%objectId%_givebuffs;")
               .append(scheme.getKey())
               .append(';')
               .append(cost)
               .append("\">Use on Me</a>&nbsp;|&nbsp;");
            htmlBuilder.append("<a action=\"bypass -h npc_%objectId%_givebuffs;")
               .append(scheme.getKey())
               .append(';')
               .append(cost)
               .append(";pet\">Use on Pet</a>&nbsp;|&nbsp;");
            htmlBuilder.append("<a action=\"bypass -h npc_%objectId%_editschemes;Buffs;").append(scheme.getKey()).append(";1\">Edit</a>&nbsp;|&nbsp;");
            htmlBuilder.append("<a action=\"bypass -h npc_%objectId%_deletescheme;").append(scheme.getKey()).append("\">Delete</a><br>");
         }
      } else {
         htmlBuilder.append("<font color=\"LEVEL\">You haven't defined any scheme.</font>");
      }

      NpcHtmlMessage html = new NpcHtmlMessage(this.getObjectId());
      html.setFile(player, this.getHtmlPath(this.getId(), 1));
      html.replace("%schemes%", htmlBuilder.toString());
      html.replace("%max_schemes%", SchemeBufferConfig.BUFFER_MAX_SCHEMES);
      html.replace("%objectId%", this.getObjectId());
      player.sendPacket(html);
   }

   private void showEditSchemeWindow(Player player, String groupType, String schemeName, int page) {
      NpcHtmlMessage html = new NpcHtmlMessage(this.getObjectId());
      List<Integer> schemeSkills = SchemeBufferTable.getInstance().getScheme(player.getObjectId(), schemeName);
      html.setFile(player, this.getHtmlPath(this.getId(), 2));
      html.replace("%schemename%", schemeName);
      html.replace(
         "%count%",
         getCountOf(schemeSkills, false)
            + " / "
            + player.getStat().getMaxBuffCount()
            + " buffs, "
            + getCountOf(schemeSkills, true)
            + " / "
            + PlayerConfig.DANCES_MAX_AMOUNT
            + " dances/songs"
      );
      html.replace("%typesframe%", getTypesFrame(groupType, schemeName));
      html.replace("%skilllistframe%", this.getGroupSkillList(player, groupType, schemeName, page));
      html.replace("%objectId%", this.getObjectId());
      player.sendPacket(html);
   }

   private void showManualWindow(Player player, String category, int pageValue, String targetType) {
      SchemeBufferTable schemeBufferTable = SchemeBufferTable.getInstance();
      SkillData skillData = SkillData.getInstance();
      List<Integer> skillIds = schemeBufferTable.getSkillsIdsByType(category);
      if (skillIds.isEmpty()) {
         player.sendMessage("That category doesn't contain any skills.");
      } else {
         int maxPage = HtmlUtil.countPageNumber(skillIds.size(), 6);
         int page = pageValue;
         if (pageValue > maxPage) {
            page = maxPage;
         }

         if (page < 1) {
            page = 1;
         }

         skillIds = skillIds.subList((page - 1) * 6, Math.min(page * 6, skillIds.size()));
         StringBuilder htmlBuilder = new StringBuilder(skillIds.size() * 200);
         htmlBuilder.append("<table width=\"").append(256).append("\"><tr>");
         htmlBuilder.append("<td width=\"").append(128).append("\" align=\"center\">");
         if ("me".equalsIgnoreCase(targetType)) {
            htmlBuilder.append("<font color=\"LEVEL\">Me</font>");
         } else {
            htmlBuilder.append("<a action=\"bypass -h npc_")
               .append(this.getObjectId())
               .append("_manual;")
               .append(category)
               .append(';')
               .append(page)
               .append(";me\">Me</a>");
         }

         htmlBuilder.append("</td>");
         htmlBuilder.append("<td width=\"").append(128).append("\" align=\"center\">");
         if ("pet".equalsIgnoreCase(targetType)) {
            htmlBuilder.append("<font color=\"LEVEL\">Pet</font>");
         } else {
            htmlBuilder.append("<a action=\"bypass -h npc_")
               .append(this.getObjectId())
               .append("_manual;")
               .append(category)
               .append(';')
               .append(page)
               .append(";pet\">Pet</a>");
         }

         htmlBuilder.append("</td>");
         htmlBuilder.append("</tr></table><br1>");
         int row = 0;

         for (int skillId : skillIds) {
            BuffSkillHolder holder = schemeBufferTable.getAvailableBuff(category, skillId);
            if (holder != null) {
               Skill skill = skillData.getSkill(skillId, holder.getLevel());
               if (skill != null) {
                  htmlBuilder.append(row % 2 == 0 ? "<table width=\"256\" bgcolor=\"000000\"><tr>" : "<table width=\"256\"><tr>");
                  htmlBuilder.append("<td height=40 width=")
                     .append(40)
                     .append(" align=center><img src=\"")
                     .append(skill.getIcon())
                     .append("\" width=32 height=32></td>");
                  htmlBuilder.append("<td width=").append(184).append('>').append(skill.getName()).append("<br1>");
                  htmlBuilder.append("<font color=\"B09878\">").append(holder.getDescription()).append("</font></td>");
                  htmlBuilder.append("<td width=")
                     .append(32)
                     .append(" align=center><button action=\"bypass -h npc_")
                     .append(this.getObjectId())
                     .append("_castbuff;")
                     .append(skillId)
                     .append(';')
                     .append(category)
                     .append(';')
                     .append(page)
                     .append(';')
                     .append(targetType)
                     .append("\" ")
                     .append("width=32 height=32 back=\"L2UI_CH3.mapbutton_zoomin2\" fore=\"L2UI_CH3.mapbutton_zoomin1\"></td>");
                  htmlBuilder.append("</tr></table>");
                  htmlBuilder.append("<img src=\"L2UI.SquareGray\" width=").append(256).append(" height=1>");
                  row++;
               }
            }
         }

         htmlBuilder.append("<br1><img src=\"L2UI.SquareGray\" width=")
            .append(256)
            .append(" height=1><table width=\"")
            .append(256)
            .append("\" bgcolor=000000><tr>");
         if (page > 1) {
            htmlBuilder.append("<td align=left width=")
               .append(70)
               .append("><a action=\"bypass -h npc_")
               .append(this.getObjectId())
               .append("_manual;")
               .append(category)
               .append(';')
               .append(page - 1)
               .append(';')
               .append(targetType)
               .append("\">Previous</a></td>");
         } else {
            htmlBuilder.append("<td align=left width=").append(70).append(">Previous</td>");
         }

         htmlBuilder.append("<td align=center width=").append(116).append(">Page ").append(page).append("</td>");
         if (page < maxPage) {
            htmlBuilder.append("<td align=right width=")
               .append(70)
               .append("><a action=\"bypass -h npc_")
               .append(this.getObjectId())
               .append("_manual;")
               .append(category)
               .append(';')
               .append(page + 1)
               .append(';')
               .append(targetType)
               .append("\">Next</a></td>");
         } else {
            htmlBuilder.append("<td align=right width=").append(70).append(">Next</td>");
         }

         htmlBuilder.append("</tr></table><img src=\"L2UI.SquareGray\" width=").append(256).append(" height=1>");
         htmlBuilder.append("<br1><center><a action=\"bypass -h npc_").append(this.getObjectId()).append("_menu\">Back</a></center>");
         NpcHtmlMessage html = new NpcHtmlMessage(this.getObjectId());
         html.setFile(player, this.getHtmlPath(this.getId(), 3));
         html.replace("%category%", category);
         html.replace("%skills%", htmlBuilder.toString());
         html.replace("%objectId%", this.getObjectId());
         player.sendPacket(html);
      }
   }

   private void applyAutoBuff(Player player, String targetType) {
      SchemeBufferTable schemeBufferTable = SchemeBufferTable.getInstance();
      SkillData skillData = SkillData.getInstance();
      Creature target = (Creature)("pet".equalsIgnoreCase(targetType) ? player.getSummon() : player);
      if (target == null) {
         player.sendMessage("You don't have a pet.");
         this.showMainMenu(player);
      } else {
         String category = player.isMageClass() ? "MAGE_GROUP" : "FIGHTER_GROUP";
         List<Integer> skillIds = schemeBufferTable.getSkillsIdsByType(category);
         if (skillIds.isEmpty()) {
            player.sendMessage("Auto buff configuration is not available.");
            this.showMainMenu(player);
         } else {
            for (int skillId : skillIds) {
               BuffSkillHolder holder = schemeBufferTable.getAvailableBuff(category, skillId);
               if (holder != null) {
                  Skill skill = skillData.getSkill(skillId, holder.getLevel());
                  if (skill != null) {
                     skill.applyEffects(this, target);
                  }
               }
            }

            player.sendMessage("Auto buff applied successfully!");
            this.showMainMenu(player);
         }
      }
   }

   private String getGroupSkillList(Player player, String groupType, String schemeName, int pageValue) {
      SchemeBufferTable schemeBufferTable = SchemeBufferTable.getInstance();
      SkillData skillData = SkillData.getInstance();
      List<Integer> skillIds = schemeBufferTable.getSkillsIdsByType(groupType);
      if (skillIds.isEmpty()) {
         return "That group doesn't contain any skills.";
      } else {
         int maxPage = HtmlUtil.countPageNumber(skillIds.size(), 6);
         int page = pageValue;
         if (pageValue > maxPage) {
            page = maxPage;
         }

         if (page < 1) {
            page = 1;
         }

         skillIds = skillIds.subList((page - 1) * 6, Math.min(page * 6, skillIds.size()));
         List<Integer> schemeSkills = schemeBufferTable.getScheme(player.getObjectId(), schemeName);
         StringBuilder htmlBuilder = new StringBuilder(skillIds.size() * 150);
         int row = 0;

         for (int skillId : skillIds) {
            BuffSkillHolder holder = schemeBufferTable.getAvailableBuff(groupType, skillId);
            if (holder != null) {
               Skill skill = skillData.getSkill(skillId, 1);
               if (skill != null) {
                  htmlBuilder.append(row % 2 == 0 ? "<table width=\"256\" bgcolor=\"000000\"><tr>" : "<table width=\"256\"><tr>");
                  htmlBuilder.append("<td height=40 width=")
                     .append(40)
                     .append(" align=center><img src=\"")
                     .append(skill.getIcon())
                     .append("\" width=32 height=32></td>");
                  htmlBuilder.append("<td width=")
                     .append(184)
                     .append('>')
                     .append(skill.getName())
                     .append("<br1><font color=\"B09878\">")
                     .append(holder.getDescription())
                     .append("</font></td>");
                  if (schemeSkills.contains(skillId)) {
                     htmlBuilder.append("<td width=")
                        .append(32)
                        .append(" align=center><button action=\"bypass -h npc_%objectId%_skillunselect;")
                        .append(groupType)
                        .append(';')
                        .append(schemeName)
                        .append(';')
                        .append(skillId)
                        .append(';')
                        .append(page)
                        .append("\" ")
                        .append("width=32 height=32 back=\"L2UI_CH3.mapbutton_zoomout2\" fore=\"L2UI_CH3.mapbutton_zoomout1\"></td>");
                  } else {
                     htmlBuilder.append("<td width=")
                        .append(32)
                        .append(" align=center><button action=\"bypass -h npc_%objectId%_skillselect;")
                        .append(groupType)
                        .append(';')
                        .append(schemeName)
                        .append(';')
                        .append(skillId)
                        .append(';')
                        .append(page)
                        .append("\" ")
                        .append("width=32 height=32 back=\"L2UI_CH3.mapbutton_zoomin2\" fore=\"L2UI_CH3.mapbutton_zoomin1\"></td>");
                  }

                  htmlBuilder.append("</tr></table><img src=\"L2UI.SquareGray\" width=").append(256).append(" height=1>");
                  row++;
               }
            }
         }

         htmlBuilder.append("<br1><img src=\"L2UI.SquareGray\" width=")
            .append(256)
            .append(" height=1><table width=\"")
            .append(256)
            .append("\" bgcolor=000000><tr>");
         if (page > 1) {
            htmlBuilder.append("<td align=left width=")
               .append(70)
               .append("><a action=\"bypass -h npc_")
               .append(this.getObjectId())
               .append("_editschemes;")
               .append(groupType)
               .append(';')
               .append(schemeName)
               .append(';')
               .append(page - 1)
               .append("\">Previous</a></td>");
         } else {
            htmlBuilder.append("<td align=left width=").append(70).append(">Previous</td>");
         }

         htmlBuilder.append("<td align=center width=").append(116).append(">Page ").append(page).append("</td>");
         if (page < maxPage) {
            htmlBuilder.append("<td align=right width=")
               .append(70)
               .append("><a action=\"bypass -h npc_")
               .append(this.getObjectId())
               .append("_editschemes;")
               .append(groupType)
               .append(';')
               .append(schemeName)
               .append(';')
               .append(page + 1)
               .append("\">Next</a></td>");
         } else {
            htmlBuilder.append("<td align=right width=").append(70).append(">Next</td>");
         }

         htmlBuilder.append("</tr></table><img src=\"L2UI.SquareGray\" width=").append(256).append(" height=1>");
         return htmlBuilder.toString();
      }
   }

   private static String getTypesFrame(String groupType, String schemeName) {
      StringBuilder htmlBuilder = new StringBuilder(500);
      htmlBuilder.append("<table>");
      int count = 0;

      for (String type : SchemeBufferTable.getInstance().getSkillTypes()) {
         if (count == 0) {
            htmlBuilder.append("<tr>");
         }

         if (groupType.equalsIgnoreCase(type)) {
            htmlBuilder.append("<td width=65>").append(type).append("</td>");
         } else {
            htmlBuilder.append("<td width=65><a action=\"bypass -h npc_%objectId%_editschemes;")
               .append(type)
               .append(';')
               .append(schemeName)
               .append(";1\">")
               .append(type)
               .append("</a></td>");
         }

         if (++count == 4) {
            htmlBuilder.append("</tr>");
            count = 0;
         }
      }

      if (count != 0) {
         htmlBuilder.append("</tr>");
      }

      htmlBuilder.append("</table>");
      return htmlBuilder.toString();
   }

   private static Integer parseUnsignedInt(String value) {
      if (value != null && !value.isEmpty()) {
         int result = 0;

         for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c < '0' || c > '9') {
               return null;
            }

            int digit = c - '0';
            if (result > (Integer.MAX_VALUE - digit) / 10) {
               return null;
            }

            result = result * 10 + digit;
         }

         return result;
      } else {
         return null;
      }
   }

   private static int getFee(List<Integer> list) {
      if (SchemeBufferConfig.BUFFER_STATIC_BUFF_COST > 0) {
         return list.size() * SchemeBufferConfig.BUFFER_STATIC_BUFF_COST;
      } else {
         int fee = 0;

         for (int skillId : list) {
            BuffSkillHolder holder = SchemeBufferTable.getInstance().getAvailableBuff(skillId);
            if (holder != null) {
               fee += holder.getPrice();
            }
         }

         return fee;
      }
   }

   private static int getCountOf(List<Integer> skills, boolean dances) {
      SkillData skillData = SkillData.getInstance();
      int count = 0;

      for (int skillId : skills) {
         Skill skill = skillData.getSkill(skillId, 1);
         if (skill != null && skill.isDance() == dances) {
            count++;
         }
      }

      return count;
   }
}
