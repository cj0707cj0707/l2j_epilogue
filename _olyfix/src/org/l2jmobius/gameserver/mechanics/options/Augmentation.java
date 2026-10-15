package org.l2jmobius.gameserver.mechanics.options;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.l2jmobius.gameserver.data.xml.OptionData;
import org.l2jmobius.gameserver.entity.actor.Player;

public class Augmentation {
   private int _effectsId = 0;
   private Augmentation.AugmentationStatBonus _bonus = null;

   public Augmentation(int effects) {
      this._effectsId = effects;
      this._bonus = new Augmentation.AugmentationStatBonus(this._effectsId);
   }

   public int getAugmentationId() {
      return this._effectsId;
   }

   public void applyBonus(Player player) {
      this._bonus.applyBonus(player);
   }

   public void removeBonus(Player player) {
      this._bonus.removeBonus(player);
   }

   public static class AugmentationStatBonus {
      private static final Logger LOGGER = Logger.getLogger(Augmentation.AugmentationStatBonus.class.getName());
      private final List<Options> _options = new ArrayList<>();
      private boolean _active = false;

      public AugmentationStatBonus(int augmentationId) {
         int[] stats = new int[]{65535 & augmentationId, augmentationId >> 16};

         for (int stat : stats) {
            Options op = OptionData.getInstance().getOptions(stat);
            if (op != null) {
               this._options.add(op);
            } else {
               LOGGER.log(Level.WARNING, this.getClass().getSimpleName() + ": Couldn't find option: " + stat);
            }
         }
      }

      public void applyBonus(Player player) {
         if (!this._active) {
            for (Options op : this._options) {
               op.apply(player);
            }

            this._active = true;
         }
      }

      public void removeBonus(Player player) {
         if (this._active) {
            for (Options op : this._options) {
               op.remove(player);
            }

            this._active = false;
         }
      }
   }
}
