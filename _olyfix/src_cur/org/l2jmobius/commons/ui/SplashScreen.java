package org.l2jmobius.commons.ui;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Toolkit;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JWindow;

public class SplashScreen extends JWindow {
   private final Image _image;

   public SplashScreen(String path, long time, final JFrame parent) {
      this.setBackground(new Color(0, 255, 0, 0));
      this._image = Toolkit.getDefaultToolkit().getImage(path);
      ImageIcon imageIcon = new ImageIcon(this._image);
      this.setSize(imageIcon.getIconWidth(), imageIcon.getIconHeight());
      this.setLocationRelativeTo(null);
      this.setAlwaysOnTop(true);
      this.setVisible(true);
      new Timer().schedule(new TimerTask() {
         {
            Objects.requireNonNull(SplashScreen.this);
         }

         @Override
         public void run() {
            SplashScreen.this.setVisible(false);
            if (parent != null) {
               parent.setVisible(true);
               parent.toFront();
               parent.setState(1);
               parent.setState(0);
            }

            SplashScreen.this.dispose();
         }
      }, imageIcon.getIconWidth() > 0 ? time : 100L);
   }

   @Override
   public void paint(Graphics g) {
      g.drawImage(this._image, 0, 0, null);
   }
}
