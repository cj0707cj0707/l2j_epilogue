package org.l2jmobius.gameserver.data.xml;

import java.io.File;
import org.l2jmobius.commons.util.IXmlReader;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.taskmanagers.GameTimeTaskManager;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

public class HitConditionBonusData implements IXmlReader {
   private int frontBonus = 0;
   private int sideBonus = 0;
   private int backBonus = 0;
   private int highBonus = 0;
   private int lowBonus = 0;
   private int darkBonus = 0;
   private int rainBonus = 0;

   protected HitConditionBonusData() {
      this.load();
   }

   @Override
   public void load() {
      this.parseDatapackFile("data/stats/hitConditionBonus.xml");
      LOGGER.info(this.getClass().getSimpleName() + ": Loaded hit condition bonuses.");
   }

   @Override
   public void parseDocument(Document document, File file) {
      for (Node d = document.getFirstChild().getFirstChild(); d != null; d = d.getNextSibling()) {
         NamedNodeMap attrs = d.getAttributes();
         String var5 = d.getNodeName();
         switch (var5) {
            case "front":
               this.frontBonus = this.parseInteger(attrs, "val");
               break;
            case "side":
               this.sideBonus = this.parseInteger(attrs, "val");
               break;
            case "back":
               this.backBonus = this.parseInteger(attrs, "val");
               break;
            case "high":
               this.highBonus = this.parseInteger(attrs, "val");
               break;
            case "low":
               this.lowBonus = this.parseInteger(attrs, "val");
               break;
            case "dark":
               this.darkBonus = this.parseInteger(attrs, "val");
               break;
            case "rain":
               this.rainBonus = this.parseInteger(attrs, "val");
         }
      }
   }

   public double getConditionBonus(Creature attacker, Creature target) {
      double mod = 100.0;
      if (attacker.getZ() - target.getZ() > 50) {
         mod += this.highBonus;
      } else if (attacker.getZ() - target.getZ() < -50) {
         mod += this.lowBonus;
      }

      if (GameTimeTaskManager.getInstance().isNight()) {
         mod += this.darkBonus;
      }

      if (attacker.isBehind(target)) {
         mod += this.backBonus;
      } else if (attacker.isInFrontOf(target)) {
         mod += this.frontBonus;
      } else {
         mod += this.sideBonus;
      }

      return Math.max(mod / 100.0, 0.0);
   }

   public static HitConditionBonusData getInstance() {
      return HitConditionBonusData.SingletonHolder.INSTANCE;
   }

   private static class SingletonHolder {
      protected static final HitConditionBonusData INSTANCE = new HitConditionBonusData();
   }
}
