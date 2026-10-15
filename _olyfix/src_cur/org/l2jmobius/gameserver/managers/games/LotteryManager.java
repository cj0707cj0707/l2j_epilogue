package org.l2jmobius.gameserver.managers.games;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Calendar;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.l2jmobius.commons.database.DatabaseFactory;
import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.config.GeneralConfig;
import org.l2jmobius.gameserver.entity.World;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;
import org.l2jmobius.gameserver.util.MathUtil;

public class LotteryManager {
   public static final long SECOND = 1000L;
   public static final long MINUTE = 60000L;
   protected static final Logger LOGGER = Logger.getLogger(LotteryManager.class.getName());
   private static final String INSERT_LOTTERY = "INSERT INTO lottery(id, idnr, enddate, prize, newprize) VALUES (?, ?, ?, ?, ?)";
   private static final String UPDATE_PRICE = "UPDATE lottery SET prize=?, newprize=? WHERE id = 1 AND idnr = ?";
   private static final String UPDATE_LOTTERY = "UPDATE lottery SET finished=1, prize=?, newprize=?, number1=?, number2=?, prize1=?, prize2=?, prize3=? WHERE id=1 AND idnr=?";
   private static final String SELECT_LAST_LOTTERY = "SELECT idnr, prize, newprize, enddate, finished FROM lottery WHERE id = 1 ORDER BY idnr DESC LIMIT 1";
   private static final String SELECT_LOTTERY_ITEM = "SELECT enchant_level, custom_type2 FROM items WHERE item_id = 4442 AND custom_type1 = ?";
   private static final String SELECT_LOTTERY_TICKET = "SELECT number1, number2, prize1, prize2, prize3 FROM lottery WHERE id = 1 and idnr = ?";
   protected int _number = 1;
   protected long _prize = GeneralConfig.ALT_LOTTERY_PRIZE;
   protected boolean _isSellingTickets = false;
   protected boolean _isStarted = false;
   protected long _enddate = System.currentTimeMillis();

   protected LotteryManager() {
      if (GeneralConfig.ALLOW_LOTTERY) {
         new LotteryManager.startLottery().run();
      }
   }

   public int getId() {
      return this._number;
   }

   public long getPrize() {
      return this._prize;
   }

   public long getEndDate() {
      return this._enddate;
   }

   public void increasePrize(long count) {
      this._prize += count;

      try (
         Connection con = DatabaseFactory.getConnection();
         PreparedStatement ps = con.prepareStatement("UPDATE lottery SET prize=?, newprize=? WHERE id = 1 AND idnr = ?");
      ) {
         ps.setLong(1, this._prize);
         ps.setLong(2, this._prize);
         ps.setInt(3, this._number);
         ps.execute();
      } catch (SQLException var11) {
         LOGGER.log(Level.WARNING, "Lottery: Could not increase current lottery prize: " + var11.getMessage(), (Throwable)var11);
      }
   }

   public boolean isSellableTickets() {
      return this._isSellingTickets;
   }

   public boolean isStarted() {
      return this._isStarted;
   }

   public int[] decodeNumbers(int enchantValue, int type2Value) {
      int[] res = new int[5];
      int id = 0;
      int nr = 1;

      for (int enchant = enchantValue; enchant > 0; nr++) {
         int val = enchant / 2;
         if (val != enchant / 2.0) {
            res[id] = nr;
            id++;
         }

         enchant /= 2;
      }

      nr = 17;

      for (int type2 = type2Value; type2 > 0; nr++) {
         int val = type2 / 2;
         if (val != type2 / 2.0) {
            res[id] = nr;
            id++;
         }

         type2 /= 2;
      }

      return res;
   }

   public long[] checkTicket(Item item) {
      return this.checkTicket(item.getCustomType1(), item.getEnchantLevel(), item.getCustomType2());
   }

   public long[] checkTicket(int id, int enchant, int type2) {
      long[] res = new long[]{0L, 0L};

      try (
         Connection con = DatabaseFactory.getConnection();
         PreparedStatement ps = con.prepareStatement("SELECT number1, number2, prize1, prize2, prize3 FROM lottery WHERE id = 1 and idnr = ?");
      ) {
         ps.setInt(1, id);

         try (ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
               int curenchant = rs.getInt("number1") & enchant;
               int curtype2 = rs.getInt("number2") & type2;
               if (curenchant == 0 && curtype2 == 0) {
                  return res;
               } else {
                  int count = 0;

                  for (int i = 1; i <= 16; i++) {
                     int val = curenchant / 2;
                     if (val != Math.round(curenchant / 2.0)) {
                        count++;
                     }

                     int val2 = curtype2 / 2;
                     if (val2 != curtype2 / 2.0) {
                        count++;
                     }

                     curenchant = val;
                     curtype2 = val2;
                  }

                  switch (count) {
                     case 0:
                        return res;
                     case 1:
                     case 2:
                     default:
                        res[0] = 4L;
                        res[1] = GeneralConfig.ALT_LOTTERY_2_AND_1_NUMBER_PRIZE;
                        return res;
                     case 3:
                        res[0] = 3L;
                        res[1] = rs.getLong("prize3");
                        return res;
                     case 4:
                        res[0] = 2L;
                        res[1] = rs.getLong("prize2");
                        return res;
                     case 5:
                        res[0] = 1L;
                        res[1] = rs.getLong("prize1");
                  }
               }
            }

            return res;
         }
      } catch (SQLException var20) {
         LOGGER.log(Level.WARNING, "Lottery: Could not check lottery ticket #" + id + ": " + var20.getMessage(), (Throwable)var20);
         return res;
      }
   }

   public static LotteryManager getInstance() {
      return LotteryManager.SingletonHolder.INSTANCE;
   }

   private static class SingletonHolder {
      protected static final LotteryManager INSTANCE = new LotteryManager();
   }

   private class finishLottery implements Runnable {
      protected finishLottery() {
         Objects.requireNonNull(LotteryManager.this);
         super();
      }

      @Override
      public void run() {
         int[] luckynums = new int[5];
         int luckynum = 0;

         for (int i = 0; i < 5; i++) {
            boolean found = true;

            while (found) {
               luckynum = Rnd.get(20) + 1;
               found = false;

               for (int j = 0; j < i; j++) {
                  if (luckynums[j] == luckynum) {
                     found = true;
                  }
               }
            }

            luckynums[i] = luckynum;
         }

         int enchant = 0;
         int type2 = 0;

         for (int i = 0; i < 5; i++) {
            if (luckynums[i] < 17) {
               enchant = (int)(enchant + MathUtil.pow(2.0, luckynums[i] - 1));
            } else {
               type2 = (int)(type2 + MathUtil.pow(2.0, luckynums[i] - 17));
            }
         }

         int count1 = 0;
         int count2 = 0;
         int count3 = 0;
         int count4 = 0;

         try (
            Connection con = DatabaseFactory.getConnection();
            PreparedStatement ps = con.prepareStatement("SELECT enchant_level, custom_type2 FROM items WHERE item_id = 4442 AND custom_type1 = ?");
         ) {
            ps.setInt(1, LotteryManager.this._number);

            try (ResultSet rset = ps.executeQuery()) {
               while (rset.next()) {
                  int curenchant = rset.getInt("enchant_level") & enchant;
                  int curtype2 = rset.getInt("custom_type2") & type2;
                  if (curenchant != 0 || curtype2 != 0) {
                     int count = 0;

                     for (int ix = 1; ix <= 16; ix++) {
                        int val = curenchant / 2;
                        if (val != Math.round(curenchant / 2.0)) {
                           count++;
                        }

                        int val2 = curtype2 / 2;
                        if (val2 != curtype2 / 2.0) {
                           count++;
                        }

                        curenchant = val;
                        curtype2 = val2;
                     }

                     if (count == 5) {
                        count1++;
                     } else if (count == 4) {
                        count2++;
                     } else if (count == 3) {
                        count3++;
                     } else if (count > 0) {
                        count4++;
                     }
                  }
               }
            }
         } catch (SQLException var35) {
            LotteryManager.LOGGER.log(Level.WARNING, "Lottery: Could restore lottery data: " + var35.getMessage(), (Throwable)var35);
         }

         long prize4 = count4 * GeneralConfig.ALT_LOTTERY_2_AND_1_NUMBER_PRIZE;
         long prize1 = 0L;
         long prize2 = 0L;
         long prize3 = 0L;
         if (count1 > 0) {
            prize1 = (long)((float)(LotteryManager.this._prize - prize4) * GeneralConfig.ALT_LOTTERY_5_NUMBER_RATE / count1);
         }

         if (count2 > 0) {
            prize2 = (long)((float)(LotteryManager.this._prize - prize4) * GeneralConfig.ALT_LOTTERY_4_NUMBER_RATE / count2);
         }

         if (count3 > 0) {
            prize3 = (long)((float)(LotteryManager.this._prize - prize4) * GeneralConfig.ALT_LOTTERY_3_NUMBER_RATE / count3);
         }

         long newprize = LotteryManager.this._prize - (prize1 + prize2 + prize3 + prize4);
         SystemMessage sm;
         if (count1 > 0) {
            sm = new SystemMessage(SystemMessageId.THE_PRIZE_AMOUNT_FOR_THE_WINNER_OF_LOTTERY_S1_IS_S2_ADENA_WE_HAVE_S3_FIRST_PRIZE_WINNERS);
            sm.addInt(LotteryManager.this._number);
            sm.addLong(LotteryManager.this._prize);
            sm.addLong(count1);
         } else {
            sm = new SystemMessage(
               SystemMessageId.THE_PRIZE_AMOUNT_FOR_LUCKY_LOTTERY_S1_IS_S2_ADENA_THERE_WAS_NO_FIRST_PRIZE_WINNER_IN_THIS_DRAWING_THEREFORE_THE_JACKPOT_WILL_BE_ADDED_TO_THE_NEXT_DRAWING
            );
            sm.addInt(LotteryManager.this._number);
            sm.addLong(LotteryManager.this._prize);
         }

         World.broadcastToAllOnlinePlayers(sm);

         try (
            Connection con = DatabaseFactory.getConnection();
            PreparedStatement ps = con.prepareStatement(
               "UPDATE lottery SET finished=1, prize=?, newprize=?, number1=?, number2=?, prize1=?, prize2=?, prize3=? WHERE id=1 AND idnr=?"
            );
         ) {
            ps.setLong(1, LotteryManager.this._prize);
            ps.setLong(2, newprize);
            ps.setInt(3, enchant);
            ps.setInt(4, type2);
            ps.setLong(5, prize1);
            ps.setLong(6, prize2);
            ps.setLong(7, prize3);
            ps.setInt(8, LotteryManager.this._number);
            ps.execute();
         } catch (SQLException var31) {
            LotteryManager.LOGGER.log(Level.WARNING, "Lottery: Could not store finished lottery data: " + var31.getMessage(), (Throwable)var31);
         }

         ThreadPool.schedule(LotteryManager.this.new startLottery(), 60000L);
         LotteryManager.this._number++;
         LotteryManager.this._isStarted = false;
      }
   }

   private class startLottery implements Runnable {
      protected startLottery() {
         Objects.requireNonNull(LotteryManager.this);
         super();
      }

      @Override
      public void run() {
         try (
            Connection con = DatabaseFactory.getConnection();
            Statement statement = con.createStatement();
            ResultSet rset = statement.executeQuery("SELECT idnr, prize, newprize, enddate, finished FROM lottery WHERE id = 1 ORDER BY idnr DESC LIMIT 1");
         ) {
            if (rset.next()) {
               LotteryManager.this._number = rset.getInt("idnr");
               if (rset.getInt("finished") == 1) {
                  LotteryManager.this._number++;
                  LotteryManager.this._prize = rset.getLong("newprize");
               } else {
                  LotteryManager.this._prize = rset.getLong("prize");
                  LotteryManager.this._enddate = rset.getLong("enddate");
                  if (LotteryManager.this._enddate <= System.currentTimeMillis() + 120000L) {
                     LotteryManager.this.new finishLottery().run();
                     return;
                  } else if (LotteryManager.this._enddate > System.currentTimeMillis()) {
                     LotteryManager.this._isStarted = true;
                     ThreadPool.schedule(LotteryManager.this.new finishLottery(), LotteryManager.this._enddate - System.currentTimeMillis());
                     if (LotteryManager.this._enddate > System.currentTimeMillis() + 720000L) {
                        LotteryManager.this._isSellingTickets = true;
                        ThreadPool.schedule(LotteryManager.this.new stopSellingTickets(), LotteryManager.this._enddate - System.currentTimeMillis() - 600000L);
                     }

                     return;
                  }
               }
            }
         } catch (SQLException var19) {
            LotteryManager.LOGGER.log(Level.WARNING, "Lottery: Could not restore lottery data: " + var19.getMessage(), (Throwable)var19);
         }

         LotteryManager.this._isSellingTickets = true;
         LotteryManager.this._isStarted = true;
         World.broadcastToAllOnlinePlayers("Lottery tickets are now available for Lucky Lottery #" + LotteryManager.this._number + ".");
         Calendar finishtime = Calendar.getInstance();
         finishtime.setTimeInMillis(LotteryManager.this._enddate);
         finishtime.set(12, 0);
         finishtime.set(13, 0);
         if (finishtime.get(7) == 1) {
            finishtime.set(11, 19);
            LotteryManager.this._enddate = finishtime.getTimeInMillis();
            LotteryManager.this._enddate += 604800000L;
         } else {
            finishtime.set(7, 1);
            finishtime.set(11, 19);
            LotteryManager.this._enddate = finishtime.getTimeInMillis();
         }

         long endDate = LotteryManager.this._enddate - System.currentTimeMillis();
         ThreadPool.schedule(LotteryManager.this.new stopSellingTickets(), Math.max(endDate - 600000L, 0L));
         ThreadPool.schedule(LotteryManager.this.new finishLottery(), Math.max(endDate, 0L));

         try (
            Connection conx = DatabaseFactory.getConnection();
            PreparedStatement ps = conx.prepareStatement("INSERT INTO lottery(id, idnr, enddate, prize, newprize) VALUES (?, ?, ?, ?, ?)");
         ) {
            ps.setInt(1, 1);
            ps.setInt(2, LotteryManager.this._number);
            ps.setLong(3, LotteryManager.this._enddate);
            ps.setLong(4, LotteryManager.this._prize);
            ps.setLong(5, LotteryManager.this._prize);
            ps.execute();
         } catch (SQLException var15) {
            LotteryManager.LOGGER.log(Level.WARNING, "Lottery: Could not store new lottery data: " + var15.getMessage(), (Throwable)var15);
         }
      }
   }

   private class stopSellingTickets implements Runnable {
      protected stopSellingTickets() {
         Objects.requireNonNull(LotteryManager.this);
         super();
      }

      @Override
      public void run() {
         LotteryManager.this._isSellingTickets = false;
         World.broadcastToAllOnlinePlayers(new SystemMessage(SystemMessageId.LOTTERY_TICKET_SALES_HAVE_BEEN_TEMPORARILY_SUSPENDED));
      }
   }
}
