package org.l2jmobius.gameserver.ui;

import java.awt.Color;
import java.awt.Font;
import java.io.File;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.logging.Logger;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.LineBorder;
import org.l2jmobius.commons.config.InterfaceConfig;
import org.l2jmobius.gameserver.GameServer;
import org.l2jmobius.gameserver.config.ServerConfig;
import org.l2jmobius.gameserver.entity.World;
import org.l2jmobius.gameserver.util.Locator;

public class SystemPanel extends JPanel {
   protected static final Logger LOGGER = Logger.getLogger(SystemPanel.class.getName());
   protected static final long START_TIME = System.currentTimeMillis();

   public SystemPanel() {
      if (!InterfaceConfig.DARK_THEME) {
         this.setBackground(Color.WHITE);
      }

      this.setBounds(500, 20, 284, 140);
      this.setBorder(new LineBorder(new Color(0, 0, 0), 1, false));
      this.setOpaque(true);
      this.setLayout(null);
      final JLabel lblProtocol = new JLabel("Protocol");
      lblProtocol.setFont(new Font("Monospaced", 0, 16));
      lblProtocol.setBounds(10, 5, 264, 17);
      this.add(lblProtocol);
      final JLabel lblConnected = new JLabel("Connected");
      lblConnected.setFont(new Font("Monospaced", 0, 16));
      lblConnected.setBounds(10, 23, 264, 17);
      this.add(lblConnected);
      final JLabel lblMaxConnected = new JLabel("Max connected");
      lblMaxConnected.setFont(new Font("Monospaced", 0, 16));
      lblMaxConnected.setBounds(10, 41, 264, 17);
      this.add(lblMaxConnected);
      final JLabel lblOfflineShops = new JLabel("Offline trade");
      lblOfflineShops.setFont(new Font("Monospaced", 0, 16));
      lblOfflineShops.setBounds(10, 59, 264, 17);
      this.add(lblOfflineShops);
      final JLabel lblElapsedTime = new JLabel("Elapsed time");
      lblElapsedTime.setFont(new Font("Monospaced", 0, 16));
      lblElapsedTime.setBounds(10, 77, 264, 17);
      this.add(lblElapsedTime);
      JLabel lblJavaVersion = new JLabel("Build JDK");
      lblJavaVersion.setFont(new Font("Monospaced", 0, 16));
      lblJavaVersion.setBounds(10, 95, 264, 17);
      this.add(lblJavaVersion);
      JLabel lblBuildDate = new JLabel("Build date");
      lblBuildDate.setFont(new Font("Monospaced", 0, 16));
      lblBuildDate.setBounds(10, 113, 264, 17);
      this.add(lblBuildDate);
      lblProtocol.setText("Protocol: 0");
      lblConnected.setText("Connected: 0");
      lblMaxConnected.setText("Max connected: 0");
      lblOfflineShops.setText("Offline trade: 0");
      lblElapsedTime.setText("Elapsed: 0 sec");
      lblJavaVersion.setText("Java version: " + System.getProperty("java.version"));
      lblBuildDate.setText("Build date: Unavailable");

      try {
         File jarName = Locator.getClassSource(GameServer.class);
         JarFile jarFile = new JarFile(jarName);
         Attributes attrs = jarFile.getManifest().getMainAttributes();
         lblBuildDate.setText("Build date: " + attrs.getValue("Build-Date").split(" ")[0]);
         jarFile.close();
      } catch (Exception var11) {
      }

      new Timer().schedule(new TimerTask() {
         {
            Objects.requireNonNull(SystemPanel.this);
         }

         @Override
         public void run() {
            lblProtocol.setText((ServerConfig.PROTOCOL_LIST.size() > 1 ? "Protocols: " : "Protocol: ") + ServerConfig.PROTOCOL_LIST.toString());
         }
      }, 4500L);
      new Timer().scheduleAtFixedRate(new TimerTask() {
         {
            Objects.requireNonNull(SystemPanel.this);
         }

         @Override
         public void run() {
            int playerCount = World.getPlayers().size();
            if (World.MAX_CONNECTED_COUNT < playerCount) {
               World.MAX_CONNECTED_COUNT = playerCount;
               if (playerCount > 1) {
                  SystemPanel.LOGGER.info("New maximum connected count of " + playerCount + "!");
               }
            }

            lblConnected.setText("Connected: " + playerCount);
            lblMaxConnected.setText("Max connected: " + World.MAX_CONNECTED_COUNT);
            lblOfflineShops.setText("Offline trade: " + World.OFFLINE_TRADE_COUNT);
            lblElapsedTime.setText("Elapsed: " + SystemPanel.getDurationBreakdown(System.currentTimeMillis() - SystemPanel.START_TIME));
         }
      }, 1000L, 1000L);
   }

   static String getDurationBreakdown(long millis) {
      long days = TimeUnit.MILLISECONDS.toDays(millis);
      long remaining = millis - TimeUnit.DAYS.toMillis(days);
      long hours = TimeUnit.MILLISECONDS.toHours(remaining);
      remaining -= TimeUnit.HOURS.toMillis(hours);
      long minutes = TimeUnit.MILLISECONDS.toMinutes(remaining);
      remaining -= TimeUnit.MINUTES.toMillis(minutes);
      long seconds = TimeUnit.MILLISECONDS.toSeconds(remaining);
      return days + "d " + hours + "h " + minutes + "m " + seconds + "s";
   }
}
