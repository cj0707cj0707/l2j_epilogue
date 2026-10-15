package org.l2jmobius.gameserver.mechanics.stats;

import java.io.File;
import java.util.NoSuchElementException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.parsers.DocumentBuilderFactory;
import org.l2jmobius.gameserver.config.ServerConfig;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

public enum BaseStat {
   STR(new BaseStat.STR()),
   INT(new BaseStat.INT()),
   DEX(new BaseStat.DEX()),
   WIT(new BaseStat.WIT()),
   CON(new BaseStat.CON()),
   MEN(new BaseStat.MEN()),
   NONE(new BaseStat.NONE());

   private static final Logger LOGGER = Logger.getLogger(BaseStat.class.getName());
   public static final int MAX_STAT_VALUE = 100;
   protected static final double[] STRbonus = new double[100];
   protected static final double[] INTbonus = new double[100];
   protected static final double[] DEXbonus = new double[100];
   protected static final double[] WITbonus = new double[100];
   protected static final double[] CONbonus = new double[100];
   protected static final double[] MENbonus = new double[100];
   private final IBaseStatFunction _stat;

   public String getValue() {
      return this._stat.getClass().getSimpleName();
   }

   private BaseStat(IBaseStatFunction s) {
      this._stat = s;
   }

   public double calcBonus(Creature actor) {
      return actor != null ? this._stat.calcBonus(actor) : 1.0;
   }

   public static BaseStat valueOfXml(String name) {
      String internName = name.intern();

      for (BaseStat s : values()) {
         if (s.getValue().equalsIgnoreCase(internName)) {
            return s;
         }
      }

      throw new NoSuchElementException("Unknown name '" + internName + "' for enum BaseStats");
   }

   static {
      DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
      factory.setValidating(false);
      factory.setIgnoringComments(true);
      File file = new File(ServerConfig.DATAPACK_ROOT, "data/stats/statBonus.xml");
      Document document = null;
      if (file.exists()) {
         try {
            document = factory.newDocumentBuilder().parse(file);
         } catch (Exception var12) {
            LOGGER.log(Level.WARNING, "[BaseStats] Could not parse file: " + var12.getMessage(), (Throwable)var12);
         }

         if (document != null) {
            for (Node list = document.getFirstChild(); list != null; list = list.getNextSibling()) {
               if ("list".equalsIgnoreCase(list.getNodeName())) {
                  for (Node stat = list.getFirstChild(); stat != null; stat = stat.getNextSibling()) {
                     String statName = stat.getNodeName();

                     for (Node value = stat.getFirstChild(); value != null; value = value.getNextSibling()) {
                        if ("stat".equalsIgnoreCase(value.getNodeName())) {
                           NamedNodeMap attrs = value.getAttributes();

                           int val;
                           double bonus;
                           try {
                              val = Integer.parseInt(attrs.getNamedItem("value").getNodeValue());
                              bonus = Double.parseDouble(attrs.getNamedItem("bonus").getNodeValue());
                           } catch (Exception var13) {
                              LOGGER.severe("[BaseStats] Invalid stats value: " + value.getNodeValue() + ", skipping");
                              continue;
                           }

                           if ("STR".equalsIgnoreCase(statName)) {
                              STRbonus[val] = bonus;
                           } else if ("INT".equalsIgnoreCase(statName)) {
                              INTbonus[val] = bonus;
                           } else if ("DEX".equalsIgnoreCase(statName)) {
                              DEXbonus[val] = bonus;
                           } else if ("WIT".equalsIgnoreCase(statName)) {
                              WITbonus[val] = bonus;
                           } else if ("CON".equalsIgnoreCase(statName)) {
                              CONbonus[val] = bonus;
                           } else if ("MEN".equalsIgnoreCase(statName)) {
                              MENbonus[val] = bonus;
                           } else {
                              LOGGER.severe("[BaseStats] Invalid stats name: " + statName + ", skipping");
                           }
                        }
                     }
                  }
               }
            }
         }
      } else {
         throw new Error("[BaseStats] File not found: " + file.getName());
      }
   }

   protected static class CON implements IBaseStatFunction {
      @Override
      public double calcBonus(Creature actor) {
         return BaseStat.CONbonus[actor.getCON()];
      }
   }

   protected static class DEX implements IBaseStatFunction {
      @Override
      public double calcBonus(Creature actor) {
         return BaseStat.DEXbonus[actor.getDEX()];
      }
   }

   protected static class INT implements IBaseStatFunction {
      @Override
      public double calcBonus(Creature actor) {
         return BaseStat.INTbonus[actor.getINT()];
      }
   }

   protected static class MEN implements IBaseStatFunction {
      @Override
      public double calcBonus(Creature actor) {
         return BaseStat.MENbonus[actor.getMEN()];
      }
   }

   protected static class NONE implements IBaseStatFunction {
      @Override
      public double calcBonus(Creature actor) {
         return 1.0;
      }
   }

   protected static class STR implements IBaseStatFunction {
      @Override
      public double calcBonus(Creature actor) {
         return BaseStat.STRbonus[actor.getSTR()];
      }
   }

   protected static class WIT implements IBaseStatFunction {
      @Override
      public double calcBonus(Creature actor) {
         return BaseStat.WITbonus[actor.getWIT()];
      }
   }
}
