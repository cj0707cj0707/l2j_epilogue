package org.l2jmobius.gameserver.data.xml;

import java.io.File;
import java.util.Arrays;
import java.util.logging.Logger;
import org.l2jmobius.commons.util.IXmlReader;
import org.l2jmobius.gameserver.config.PlayerConfig;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;

public class KarmaLossData implements IXmlReader {
   private static final Logger LOGGER = Logger.getLogger(KarmaLossData.class.getName());
   private final double[] _levelModifiers = new double[PlayerConfig.PLAYER_MAXIMUM_LEVEL];

   protected KarmaLossData() {
      this.load();
   }

   @Override
   public void load() {
      Arrays.fill(this._levelModifiers, 1.0);
      this.parseDatapackFile("data/stats/players/karmaLoss.xml");
      LOGGER.info(this.getClass().getSimpleName() + ": Loaded " + (this._levelModifiers.length - 1) + " modifiers.");
   }

   @Override
   public void parseDocument(Document document, File file) {
      this.forEach(document, "list", listNode -> this.forEach(listNode, "modifier", modifierNode -> {
         NamedNodeMap attrs = modifierNode.getAttributes();
         int level = this.parseInteger(attrs, "level");
         if (level < PlayerConfig.PLAYER_MAXIMUM_LEVEL) {
            this._levelModifiers[level] = this.parseDouble(attrs, "val");
         }
      }));
   }

   public double getModifier(int level) {
      if (level >= PlayerConfig.PLAYER_MAXIMUM_LEVEL) {
         LOGGER.warning(this.getClass().getSimpleName() + ": Requested too high level (" + level + ").");
         return this._levelModifiers[this._levelModifiers.length - 1];
      } else {
         return this._levelModifiers[level];
      }
   }

   public static KarmaLossData getInstance() {
      return KarmaLossData.SingletonHolder.INSTANCE;
   }

   private static class SingletonHolder {
      protected static final KarmaLossData INSTANCE = new KarmaLossData();
   }
}
