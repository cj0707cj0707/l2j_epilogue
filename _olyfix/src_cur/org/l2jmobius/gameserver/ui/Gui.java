package org.l2jmobius.gameserver.ui;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import javax.swing.DropMode;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLayeredPane;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import org.l2jmobius.commons.config.InterfaceConfig;
import org.l2jmobius.commons.ui.DarkTheme;
import org.l2jmobius.commons.ui.LineLimitListener;
import org.l2jmobius.commons.ui.SplashScreen;
import org.l2jmobius.commons.util.StringUtil;
import org.l2jmobius.gameserver.Shutdown;
import org.l2jmobius.gameserver.cache.HtmCache;
import org.l2jmobius.gameserver.config.ConfigLoader;
import org.l2jmobius.gameserver.data.xml.AdminData;
import org.l2jmobius.gameserver.data.xml.BuyListData;
import org.l2jmobius.gameserver.data.xml.MultisellData;
import org.l2jmobius.gameserver.data.xml.PrimeShopData;
import org.l2jmobius.gameserver.entity.World;

public class Gui {
   private static final String[] SHUTDOWN_OPTIONS = new String[]{"Shutdown", "Cancel"};
   private static final String[] RESTART_OPTIONS = new String[]{"Restart", "Cancel"};
   private static final String[] ABORT_OPTIONS = new String[]{"Abort", "Cancel"};
   private static final String[] CONFIRM_OPTIONS = new String[]{"Confirm", "Cancel"};
   private final JTextArea _txtrConsole;

   public Gui() {
      System.setProperty("sun.java2d.opengl", "false");
      System.setProperty("sun.java2d.d3d", "false");
      System.setProperty("sun.java2d.noddraw", "true");
      if (InterfaceConfig.DARK_THEME) {
         DarkTheme.activate();
      }

      this._txtrConsole = new JTextArea();
      this._txtrConsole.setEditable(false);
      this._txtrConsole.setLineWrap(true);
      this._txtrConsole.setWrapStyleWord(true);
      this._txtrConsole.setDropMode(DropMode.INSERT);
      this._txtrConsole.setFont(new Font("Monospaced", 0, 16));
      this._txtrConsole.getDocument().addDocumentListener(new LineLimitListener(500));
      this.addRightClickCopyMenu();
      JMenuBar menuBar = new JMenuBar();
      menuBar.setFont(new Font("Segoe UI", 0, 14));
      JMenu mnActions = new JMenu("Actions");
      mnActions.setFont(new Font("Segoe UI", 0, 13));
      menuBar.add(mnActions);
      JMenuItem mntmShutdown = new JMenuItem("Shutdown");
      mntmShutdown.setFont(new Font("Segoe UI", 0, 13));
      mntmShutdown.addActionListener(var0 -> {
         if (JOptionPane.showOptionDialog(null, "Shutdown GameServer?", "Select an option", 0, 3, null, SHUTDOWN_OPTIONS, SHUTDOWN_OPTIONS[1]) == 0) {
            Object answer = JOptionPane.showInputDialog(null, "Shutdown delay in seconds", "Input", 1, null, null, "600");
            if (answer != null) {
               String input = ((String)answer).trim();
               if (StringUtil.isNumeric(input)) {
                  int delay = Integer.parseInt(input);
                  if (delay > 0) {
                     Shutdown.getInstance().startShutdown(null, delay, false);
                  }
               }
            }
         }
      });
      mnActions.add(mntmShutdown);
      JMenuItem mntmRestart = new JMenuItem("Restart");
      mntmRestart.setFont(new Font("Segoe UI", 0, 13));
      mntmRestart.addActionListener(var0 -> {
         if (JOptionPane.showOptionDialog(null, "Restart GameServer?", "Select an option", 0, 3, null, RESTART_OPTIONS, RESTART_OPTIONS[1]) == 0) {
            Object answer = JOptionPane.showInputDialog(null, "Restart delay in seconds", "Input", 1, null, null, "600");
            if (answer != null) {
               String input = ((String)answer).trim();
               if (StringUtil.isNumeric(input)) {
                  int delay = Integer.parseInt(input);
                  if (delay > 0) {
                     Shutdown.getInstance().startShutdown(null, delay, true);
                  }
               }
            }
         }
      });
      mnActions.add(mntmRestart);
      JMenuItem mntmAbort = new JMenuItem("Abort");
      mntmAbort.setFont(new Font("Segoe UI", 0, 13));
      mntmAbort.addActionListener(var0 -> {
         if (JOptionPane.showOptionDialog(null, "Abort server shutdown?", "Select an option", 0, 3, null, ABORT_OPTIONS, ABORT_OPTIONS[1]) == 0) {
            Shutdown.getInstance().abort(null);
         }
      });
      mnActions.add(mntmAbort);
      JMenu mnReload = new JMenu("Reload");
      mnReload.setFont(new Font("Segoe UI", 0, 13));
      menuBar.add(mnReload);
      JMenuItem mntmConfigs = new JMenuItem("Configs");
      mntmConfigs.setFont(new Font("Segoe UI", 0, 13));
      mntmConfigs.addActionListener(var0 -> {
         if (JOptionPane.showOptionDialog(null, "Reload configs?", "Select an option", 0, 3, null, CONFIRM_OPTIONS, CONFIRM_OPTIONS[1]) == 0) {
            ConfigLoader.init();
         }
      });
      mnReload.add(mntmConfigs);
      JMenuItem mntmAccess = new JMenuItem("Access");
      mntmAccess.setFont(new Font("Segoe UI", 0, 13));
      mntmAccess.addActionListener(var0 -> {
         if (JOptionPane.showOptionDialog(null, "Reload admin access levels?", "Select an option", 0, 3, null, CONFIRM_OPTIONS, CONFIRM_OPTIONS[1]) == 0) {
            AdminData.getInstance().load();
         }
      });
      mnReload.add(mntmAccess);
      JMenuItem mntmHtml = new JMenuItem("HTML");
      mntmHtml.setFont(new Font("Segoe UI", 0, 13));
      mntmHtml.addActionListener(var0 -> {
         if (JOptionPane.showOptionDialog(null, "Reload HTML files?", "Select an option", 0, 3, null, CONFIRM_OPTIONS, CONFIRM_OPTIONS[1]) == 0) {
            HtmCache.getInstance().reload();
         }
      });
      mnReload.add(mntmHtml);
      JMenuItem mntmMultisells = new JMenuItem("Multisells");
      mntmMultisells.setFont(new Font("Segoe UI", 0, 13));
      mntmMultisells.addActionListener(var0 -> {
         if (JOptionPane.showOptionDialog(null, "Reload multisells?", "Select an option", 0, 3, null, CONFIRM_OPTIONS, CONFIRM_OPTIONS[1]) == 0) {
            MultisellData.getInstance().load();
         }
      });
      mnReload.add(mntmMultisells);
      JMenuItem mntmBuylists = new JMenuItem("Buylists");
      mntmBuylists.setFont(new Font("Segoe UI", 0, 13));
      mntmBuylists.addActionListener(var0 -> {
         if (JOptionPane.showOptionDialog(null, "Reload buylists?", "Select an option", 0, 3, null, CONFIRM_OPTIONS, CONFIRM_OPTIONS[1]) == 0) {
            BuyListData.getInstance().load();
         }
      });
      mnReload.add(mntmBuylists);
      JMenuItem mntmPrimeShop = new JMenuItem("PrimeShop");
      mntmPrimeShop.setFont(new Font("Segoe UI", 0, 13));
      mntmPrimeShop.addActionListener(var0 -> {
         if (JOptionPane.showOptionDialog(null, "Reload PrimeShop?", "Select an option", 0, 3, null, CONFIRM_OPTIONS, CONFIRM_OPTIONS[1]) == 0) {
            PrimeShopData.getInstance().load();
         }
      });
      mnReload.add(mntmPrimeShop);
      JMenu mnAnnounce = new JMenu("Announce");
      mnAnnounce.setFont(new Font("Segoe UI", 0, 13));
      menuBar.add(mnAnnounce);
      JMenuItem mntmNormal = new JMenuItem("Normal");
      mntmNormal.setFont(new Font("Segoe UI", 0, 13));
      mntmNormal.addActionListener(var0 -> {
         Object input = JOptionPane.showInputDialog(null, "Announce message", "Input", 1, null, null, "");
         if (input != null) {
            String message = ((String)input).trim();
            if (!message.isEmpty()) {
               World.broadcastToAllOnlinePlayers(message, false);
            }
         }
      });
      mnAnnounce.add(mntmNormal);
      JMenuItem mntmCritical = new JMenuItem("Critical");
      mntmCritical.setFont(new Font("Segoe UI", 0, 13));
      mntmCritical.addActionListener(var0 -> {
         Object input = JOptionPane.showInputDialog(null, "Critical announce message", "Input", 1, null, null, "");
         if (input != null) {
            String message = ((String)input).trim();
            if (!message.isEmpty()) {
               World.broadcastToAllOnlinePlayers(message, true);
            }
         }
      });
      mnAnnounce.add(mntmCritical);
      JMenu mnLogs = new JMenu("Logs");
      mnLogs.setFont(new Font("Segoe UI", 0, 13));
      menuBar.add(mnLogs);
      JMenuItem mntmLogs = new JMenuItem("View");
      mntmLogs.setFont(new Font("Segoe UI", 0, 13));
      mntmLogs.addActionListener(var0 -> new LogPanel(false));
      mnLogs.add(mntmLogs);
      JMenuItem mntmDeleteLogs = new JMenuItem("Delete");
      mntmDeleteLogs.setFont(new Font("Segoe UI", 0, 13));
      mntmDeleteLogs.addActionListener(var0 -> new LogPanel(true));
      mnLogs.add(mntmDeleteLogs);
      JMenu mnFont = new JMenu("Font");
      mnFont.setFont(new Font("Segoe UI", 0, 13));
      menuBar.add(mnFont);
      String[] fonts = new String[]{"16", "21", "27", "33"};

      for (String font : fonts) {
         JMenuItem mntmFont = new JMenuItem(font);
         mntmFont.setFont(new Font("Segoe UI", 0, 13));
         mntmFont.addActionListener(var2x -> this._txtrConsole.setFont(new Font("Monospaced", 0, Integer.parseInt(font))));
         mnFont.add(mntmFont);
      }

      JMenu mnHelp = new JMenu("Help");
      mnHelp.setFont(new Font("Segoe UI", 0, 13));
      menuBar.add(mnHelp);
      JMenuItem mntmAbout = new JMenuItem("About");
      mntmAbout.setFont(new Font("Segoe UI", 0, 13));
      mntmAbout.addActionListener(var0 -> new AboutFrame());
      mnHelp.add(mntmAbout);
      List<Image> icons = new ArrayList<>();
      icons.add(new ImageIcon(".." + File.separator + "images" + File.separator + "l2jmobius_16x16.png").getImage());
      icons.add(new ImageIcon(".." + File.separator + "images" + File.separator + "l2jmobius_32x32.png").getImage());
      icons.add(new ImageIcon(".." + File.separator + "images" + File.separator + "l2jmobius_64x64.png").getImage());
      icons.add(new ImageIcon(".." + File.separator + "images" + File.separator + "l2jmobius_128x128.png").getImage());
      final JPanel systemPanel = new SystemPanel();
      final JScrollPane scrollPanel = new JScrollPane(this._txtrConsole);
      scrollPanel.setBounds(0, 0, 800, 550);
      JLayeredPane layeredPanel = new JLayeredPane();
      layeredPanel.add(scrollPanel, 0, 0);
      layeredPanel.add(systemPanel, 1, 0);
      final JFrame frame = new JFrame("Mobius - GameServer");
      frame.setDefaultCloseOperation(0);
      frame.addWindowListener(
         new WindowAdapter() {
            {
               Objects.requireNonNull(Gui.this);
            }

            @Override
            public void windowClosing(WindowEvent ev) {
               if (JOptionPane.showOptionDialog(
                     null, "Shutdown server immediately?", "Select an option", 0, 0, null, Gui.SHUTDOWN_OPTIONS, Gui.SHUTDOWN_OPTIONS[1]
                  )
                  == 0) {
                  Shutdown.getInstance().startShutdown(null, 1, false);
               }
            }
         }
      );
      frame.addComponentListener(new ComponentAdapter() {
         {
            Objects.requireNonNull(Gui.this);
         }

         @Override
         public void componentResized(ComponentEvent ev) {
            scrollPanel.setSize(frame.getContentPane().getSize());
            systemPanel.setLocation(frame.getContentPane().getWidth() - systemPanel.getWidth() - 34, systemPanel.getY());
         }
      });
      frame.setJMenuBar(menuBar);
      frame.setIconImages(icons);
      frame.add(layeredPanel, "Center");
      frame.getContentPane().setPreferredSize(new Dimension(InterfaceConfig.DARK_THEME ? 815 : 800, 550));
      frame.pack();
      frame.setLocationRelativeTo(null);
      this.redirectSystemStreams();
      new SplashScreen(".." + File.separator + "images" + File.separator + "splash.png", 5000L, frame);
   }

   private void addRightClickCopyMenu() {
      final JPopupMenu popupMenu = new JPopupMenu();
      final JMenuItem copyItem = new JMenuItem("Copy all messages");
      copyItem.setFont(new Font("Segoe UI", 0, 13));
      popupMenu.addPopupMenuListener(new PopupMenuListener() {
         {
            Objects.requireNonNull(Gui.this);
         }

         @Override
         public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
            Gui.this.updateCopyMenuItemText(copyItem);
            popupMenu.pack();
         }

         @Override
         public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
         }

         @Override
         public void popupMenuCanceled(PopupMenuEvent e) {
         }
      });
      copyItem.addActionListener(var1x -> {
         String textToCopy = this._txtrConsole.getSelectedText();
         if (textToCopy == null || textToCopy.isEmpty()) {
            textToCopy = this._txtrConsole.getText();
         }

         if (textToCopy != null && !textToCopy.isEmpty()) {
            StringSelection selection = new StringSelection(textToCopy);
            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            clipboard.setContents(selection, selection);
         }
      });
      popupMenu.add(copyItem);
      this._txtrConsole.setComponentPopupMenu(popupMenu);
   }

   private void updateCopyMenuItemText(JMenuItem copyItem) {
      String selectedText = this._txtrConsole.getSelectedText();
      if (selectedText != null && !selectedText.isEmpty()) {
         copyItem.setText("Copy selection");
      } else {
         copyItem.setText("Copy all messages");
      }
   }

   void updateTextArea(String text) {
      SwingUtilities.invokeLater(() -> {
         this._txtrConsole.append(text);
         this._txtrConsole.setCaretPosition(this._txtrConsole.getText().length());
      });
   }

   private void redirectSystemStreams() {
      OutputStream out = new OutputStream() {
         {
            Objects.requireNonNull(Gui.this);
         }

         @Override
         public void write(int b) {
            Gui.this.updateTextArea(String.valueOf((char)b));
         }

         @Override
         public void write(byte[] b, int off, int len) {
            Gui.this.updateTextArea(new String(b, off, len));
         }

         @Override
         public void write(byte[] b) {
            this.write(b, 0, b.length);
         }
      };
      System.setOut(new PrintStream(out, true));
      System.setErr(new PrintStream(out, true));
   }
}
