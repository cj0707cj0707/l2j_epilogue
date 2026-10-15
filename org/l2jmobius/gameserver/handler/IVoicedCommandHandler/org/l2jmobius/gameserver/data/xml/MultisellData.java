package org.l2jmobius.gameserver.data.xml;

import java.io.File;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.regex.Pattern;
import org.l2jmobius.commons.util.IXmlReader;
import org.l2jmobius.gameserver.config.ConfigLoader;
import org.l2jmobius.gameserver.config.GeneralConfig;
import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.clan.Clan;
import org.l2jmobius.gameserver.entity.item.ItemTemplate;
import org.l2jmobius.gameserver.mechanics.multisell.Entry;
import org.l2jmobius.gameserver.mechanics.multisell.Ingredient;
import org.l2jmobius.gameserver.mechanics.multisell.ListContainer;
import org.l2jmobius.gameserver.mechanics.multisell.PreparedListContainer;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.ExPCCafePointInfo;
import org.l2jmobius.gameserver.network.serverpackets.MultiSellList;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;
import org.l2jmobius.gameserver.util.StatSet;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

public class MultisellData implements IXmlReader {
   private static final Pattern XML_FILE_PATTERN = Pattern.compile("\\d+\\.xml");
   private final Map<Integer, ListContainer> _entries = new ConcurrentHashMap<>();
   public static final int PAGE_SIZE = 40;
   public static final int PC_CAFE_POINTS = -100;
   public static final int CLAN_REPUTATION = -200;
   public static final int FAME = -300;

   protected MultisellData() {
      this.load();
   }

   @Override
   public void load() {
      this._entries.clear();
      this.parseDatapackDirectory("data/multisell", false);
      if (GeneralConfig.CUSTOM_MULTISELL_LOAD) {
         this.parseDatapackDirectory("data/multisell/custom", false);
      }

      this.verify();
      LOGGER.log(Level.INFO, this.getClass().getSimpleName() + ": Loaded " + this._entries.size() + " multisell lists.");
   }

   @Override
   public void parseDocument(Document document, File file) {
      try {
         int id = Integer.parseInt(file.getName().replace(".xml", ""));
         int entryId = 1;
         ListContainer list = new ListContainer(id);

         for (Node n = document.getFirstChild(); n != null; n = n.getNextSibling()) {
            if ("list".equalsIgnoreCase(n.getNodeName())) {
               Node att = n.getAttributes().getNamedItem("applyTaxes");
               list.setApplyTaxes(att != null && Boolean.parseBoolean(att.getNodeValue()));
               att = n.getAttributes().getNamedItem("useRate");
               if (att != null) {
                  try {
                     list.setUseRate(Double.parseDouble(att.getNodeValue()));
                     if (list.getUseRate() <= 1.0E-6) {
                        throw new NumberFormatException("The value cannot be 0");
                     }
                  } catch (NumberFormatException var11) {
                     try {
                        list.setUseRate(ConfigLoader.class.getField(att.getNodeValue()).getDouble(ConfigLoader.class));
                     } catch (Exception var10) {
                        LOGGER.warning(var10.getMessage() + document.getLocalName());
                        list.setUseRate(1.0);
                     }
                  } catch (DOMException var12) {
                     LOGGER.warning(var12.getMessage() + document.getLocalName());
                  }
               }

               att = n.getAttributes().getNamedItem("maintainEnchantment");
               list.setMaintainEnchantment(att != null && Boolean.parseBoolean(att.getNodeValue()));

               for (Node d = n.getFirstChild(); d != null; d = d.getNextSibling()) {
                  if ("item".equalsIgnoreCase(d.getNodeName())) {
                     Entry e = this.parseEntry(d, entryId++, list);
                     list.getEntries().add(e);
                  } else if ("npcs".equalsIgnoreCase(d.getNodeName())) {
                     for (Node b = d.getFirstChild(); b != null; b = b.getNextSibling()) {
                        if ("npc".equalsIgnoreCase(b.getNodeName())) {
                           list.allowNpc(Integer.parseInt(b.getTextContent()));
                        }
                     }
                  }
               }
            }
         }

         this._entries.put(id, list);
      } catch (Exception var13) {
         LOGGER.log(Level.SEVERE, this.getClass().getSimpleName() + ": Error in file " + file, (Throwable)var13);
      }
   }

   @Override
   public boolean isValidXmlFile(File file) {
      return file != null && file.isFile() && XML_FILE_PATTERN.matcher(file.getName().toLowerCase()).matches();
   }

   private Entry parseEntry(Node node, int entryId, ListContainer list) {
      Node first = node.getFirstChild();
      Entry entry = new Entry(entryId);

      for (Node n = first; n != null; n = n.getNextSibling()) {
         if ("ingredient".equalsIgnoreCase(n.getNodeName())) {
            NamedNodeMap attrs = n.getAttributes();
            StatSet set = new StatSet();

            for (int i = 0; i < attrs.getLength(); i++) {
               Node att = attrs.item(i);
               set.set(att.getNodeName(), att.getNodeValue());
            }

            entry.addIngredient(new Ingredient(set));
         } else if ("production".equalsIgnoreCase(n.getNodeName())) {
            NamedNodeMap attrs = n.getAttributes();
            StatSet set = new StatSet();

            for (int i = 0; i < attrs.getLength(); i++) {
               Node att = attrs.item(i);
               set.set(att.getNodeName(), att.getNodeValue());
            }

            entry.addProduct(new Ingredient(set));
         }
      }

      if (GeneralConfig.CORRECT_PRICES && entry.getIngredients().size() == 1 && entry.getProducts().size() == 1) {
         Ingredient ingredient = entry.getIngredients().get(0);
         if (ingredient.getItemId() == 57) {
            long totalPrice = 0L;

            for (Ingredient product : entry.getProducts()) {
               ItemTemplate template = ItemData.getInstance().getTemplate(product.getItemId());
               if (template != null) {
                  totalPrice += product.getItemCount() * (template.getReferencePrice() / 2);
               }
            }

            long adenaCount = ingredient.getItemCount();
            if (totalPrice > adenaCount) {
               ingredient.setItemCount(totalPrice);
               LOGGER.warning(
                  "Buy price " + adenaCount + " is less than sell price " + totalPrice + " at entry " + entryId + " of multisell " + list.getListId() + "."
               );
            }
         }
      }

      return entry;
   }

   public void separateAndSend(int listId, Player player, Npc npc, boolean inventoryOnly, double productMultiplier, double ingredientMultiplier) {
      ListContainer template = this._entries.get(listId);
      if (template == null) {
         LOGGER.warning(
            this.getClass().getSimpleName()
               + ": can't find list id: "
               + listId
               + " requested by player: "
               + player.getName()
               + ", npcId:"
               + (npc != null ? npc.getId() : 0)
         );
      } else {
         if (!template.isNpcAllowed(-1) && (npc == null || !template.isNpcAllowed(npc.getId()))) {
            if (!player.isGM()) {
               LOGGER.warning(
                  this.getClass().getSimpleName() + ": " + player + " attempted to open multisell " + listId + " from npc " + npc + " which is not allowed!"
               );
               return;
            }

            player.sendMessage("Multisell " + listId + " is restricted. Under current conditions cannot be used. Only GMs are allowed to use it.");
         }

         PreparedListContainer list = new PreparedListContainer(template, inventoryOnly, player, npc);
         if (productMultiplier != 1.0 || ingredientMultiplier != 1.0) {
            list.getEntries().forEach(entry -> {
               entry.getProducts().forEach(product -> product.setItemCount((long)Math.max(product.getItemCount() * productMultiplier, 1.0)));
               entry.getIngredients().forEach(ingredient -> ingredient.setItemCount((long)Math.max(ingredient.getItemCount() * ingredientMultiplier, 1.0)));
            });
         }

         int index = 0;

         do {
            player.sendPacket(new MultiSellList(list, index));
            index += 40;
         } while (index < list.getEntries().size());

         player.setMultiSell(list);
      }
   }

   public void separateAndSend(int listId, Player player, Npc npc, boolean inventoryOnly) {
      this.separateAndSend(listId, player, npc, inventoryOnly, 1.0, 1.0);
   }

   public static boolean hasSpecialIngredient(int id, long amount, Player player) {
      switch (id) {
         case -300:
            if (player.getFame() >= amount) {
               return true;
            }

            player.sendPacket(SystemMessageId.YOU_DON_T_HAVE_ENOUGH_REPUTATION_SCORE);
            break;
         case -200:
            Clan clan = player.getClan();
            if (clan == null) {
               player.sendPacket(SystemMessageId.YOU_ARE_NOT_A_CLAN_MEMBER_AND_CANNOT_PERFORM_THIS_ACTION);
            } else if (!player.isClanLeader()) {
               player.sendPacket(SystemMessageId.ONLY_THE_CLAN_LEADER_IS_ENABLED);
            } else {
               if (clan.getReputationScore() >= amount) {
                  return true;
               }

               player.sendPacket(SystemMessageId.THE_CLAN_REPUTATION_SCORE_IS_TOO_LOW);
            }
            break;
         case -100:
            if (player.getPcCafePoints() >= amount) {
               return true;
            }

            player.sendPacket(SystemMessageId.YOU_ARE_SHORT_OF_ACCUMULATED_POINTS);
      }

      return false;
   }

   public static boolean takeSpecialIngredient(int id, long amount, Player player) {
      switch (id) {
         case -300:
            player.setFame(player.getFame() - (int)amount);
            player.updateUserInfo();
            return true;
         case -200:
            player.getClan().takeReputationScore((int)amount);
            SystemMessage smsg = new SystemMessage(SystemMessageId.S1_POINTS_HAVE_BEEN_DEDUCTED_FROM_THE_CLAN_S_REPUTATION_SCORE);
            smsg.addLong(amount);
            player.sendPacket(smsg);
            return true;
         case -100:
            player.setPcCafePoints(player.getPcCafePoints() - (int)amount);
            player.sendPacket(new ExPCCafePointInfo(player.getPcCafePoints(), (int)(-amount), 0));
            return true;
         default:
            return false;
      }
   }

   public static void giveSpecialProduct(int id, long amount, Player player) {
      switch (id) {
         case -300:
            player.setFame((int)(player.getFame() + amount));
            player.updateUserInfo();
            break;
         case -200:
            player.getClan().addReputationScore((int)amount);
            break;
         case -100:
            player.setPcCafePoints((int)(player.getPcCafePoints() + amount));
            player.sendPacket(new ExPCCafePointInfo(player.getPcCafePoints(), (int)amount, 0));
      }
   }

   private void verify() {
      for (ListContainer list : this._entries.values()) {
         for (Entry ent : list.getEntries()) {
            for (Ingredient ing : ent.getIngredients()) {
               if (!this.verifyIngredient(ing)) {
                  LOGGER.warning(this.getClass().getSimpleName() + ": can't find ingredient with itemId: " + ing.getItemId() + " in list: " + list.getListId());
               }
            }

            for (Ingredient ingx : ent.getProducts()) {
               if (!this.verifyIngredient(ingx)) {
                  LOGGER.warning(this.getClass().getSimpleName() + ": can't find product with itemId: " + ingx.getItemId() + " in list: " + list.getListId());
               }
            }
         }
      }
   }

   private boolean verifyIngredient(Ingredient ing) {
      switch (ing.getItemId()) {
         case -300:
         case -200:
         case -100:
            return true;
         default:
            return ing.getTemplate() != null;
      }
   }

   public static MultisellData getInstance() {
      return MultisellData.SingletonHolder.INSTANCE;
   }

   private static class SingletonHolder {
      protected static final MultisellData INSTANCE = new MultisellData();
   }
}
