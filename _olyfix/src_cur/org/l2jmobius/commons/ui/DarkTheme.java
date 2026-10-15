package org.l2jmobius.commons.ui;

import java.awt.Color;
import javax.swing.UIManager;
import javax.swing.plaf.nimbus.NimbusLookAndFeel;

public class DarkTheme {
   public static void activate() {
      UIManager.put("control", new Color(128, 128, 128));
      UIManager.put("info", new Color(128, 128, 128));
      UIManager.put("nimbusBase", Color.DARK_GRAY);
      UIManager.put("nimbusAlertYellow", new Color(248, 187, 0));
      UIManager.put("nimbusDisabledText", new Color(128, 128, 128));
      UIManager.put("nimbusFocus", Color.DARK_GRAY);
      UIManager.put("nimbusGreen", new Color(176, 179, 50));
      UIManager.put("nimbusInfoBlue", Color.DARK_GRAY);
      UIManager.put("nimbusLightBackground", Color.DARK_GRAY);
      UIManager.put("nimbusOrange", new Color(191, 98, 4));
      UIManager.put("nimbusRed", new Color(169, 46, 34));
      UIManager.put("nimbusSelectedText", new Color(255, 255, 255));
      UIManager.put("nimbusSelectionBackground", new Color(104, 93, 156));
      UIManager.put("text", new Color(230, 230, 230));

      try {
         UIManager.setLookAndFeel(new NimbusLookAndFeel());
      } catch (Exception var1) {
         var1.printStackTrace();
      }
   }
}
