package org.l2jmobius.gameserver.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.Toolkit;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.logging.Logger;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListCellRenderer;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.UIManager;
import javax.swing.text.BadLocationException;
import javax.swing.text.Highlighter;
import javax.swing.text.DefaultHighlighter.DefaultHighlightPainter;
import javax.swing.text.Highlighter.HighlightPainter;
import org.l2jmobius.commons.config.InterfaceConfig;
import org.l2jmobius.commons.ui.DarkTheme;

public class LogPanel extends JFrame {
   private static final Logger LOGGER = Logger.getLogger(LogPanel.class.getName());
   private static final String ROOT_LOG_PATH = new File("log").getAbsolutePath();
   private String _currentLogPath = ROOT_LOG_PATH;
   private final JTextArea _logTextArea = new JTextArea();
   private JComboBox<String> _fileComboBox;
   private JTextField _searchField;
   private JButton _searchButton;
   private JProgressBar _progressBar;
   private JLabel _fileLabel;
   private JLabel _fileSizeLabel;
   private List<Integer> _searchIndexes = new ArrayList<>();
   private int _currentSearchIndex = -1;

   protected LogPanel(boolean deleteMode) {
      if (InterfaceConfig.DARK_THEME) {
         DarkTheme.activate();
      }

      this.setTitle(deleteMode ? "Mobius - Delete Log File" : "Mobius - Log Viewer");
      this.setMinimumSize(deleteMode ? new Dimension(400, 200) : new Dimension(1000, 600));
      this.setDefaultCloseOperation(2);
      this.setLayout(new BorderLayout());
      JPanel topPanel = this.createTopPanel(deleteMode);
      this.add(topPanel, "North");
      List<Image> icons = new ArrayList<>();
      icons.add(new ImageIcon(".." + File.separator + "images" + File.separator + "l2jmobius_16x16.png").getImage());
      icons.add(new ImageIcon(".." + File.separator + "images" + File.separator + "l2jmobius_32x32.png").getImage());
      icons.add(new ImageIcon(".." + File.separator + "images" + File.separator + "l2jmobius_64x64.png").getImage());
      icons.add(new ImageIcon(".." + File.separator + "images" + File.separator + "l2jmobius_128x128.png").getImage());
      if (!deleteMode) {
         this._logTextArea.setEditable(false);
         this._logTextArea.setFont(new Font("Monospaced", 0, 12));
         JScrollPane scrollPane = new JScrollPane(this._logTextArea);
         this.add(scrollPane, "Center");
      }

      this.setLocationRelativeTo(null);
      this.setIconImages(icons);
      this.setVisible(true);
   }

   private JPanel createTopPanel(boolean deleteMode) {
      JPanel topPanel = new JPanel();
      if (deleteMode) {
         topPanel.setLayout(new BoxLayout(topPanel, 1));
         topPanel.add(Box.createRigidArea(new Dimension(0, 20)));
         this._progressBar = new JProgressBar(0, 100);
         this._progressBar.setStringPainted(true);
         this._progressBar.setPreferredSize(new Dimension(160, 25));
         this._progressBar.setMaximumSize(new Dimension(160, 25));
         this._progressBar.setMinimumSize(new Dimension(160, 25));
         this._progressBar.setVisible(true);
         topPanel.add(this._progressBar);
         topPanel.add(Box.createRigidArea(new Dimension(0, 10)));
         JPanel middlePanel = new JPanel();
         middlePanel.setLayout(new FlowLayout(0, 10, 5));
         this._fileComboBox = new JComboBox<>();
         this.loadLogFiles();
         this._fileComboBox.setPreferredSize(new Dimension(180, 28));
         this._fileComboBox.setToolTipText("Select a log file.");
         this._fileComboBox.addActionListener(var2x -> {
            if (!deleteMode) {
               this.loadLogs();
            }
         });
         middlePanel.add(new JLabel("Log File: "));
         middlePanel.add(this._fileComboBox);
         JButton deleteButton = new JButton("❌ Delete Log");
         deleteButton.setToolTipText("Click to delete the selected log file.");
         deleteButton.setBackground(new Color(220, 53, 69));
         deleteButton.setForeground(Color.WHITE);
         deleteButton.setPreferredSize(new Dimension(110, 30));
         deleteButton.setEnabled(false);
         deleteButton.addActionListener(var1x -> {
            this._progressBar.setValue(0);
            SwingUtilities.invokeLater(() -> this.deleteLogFiles(this._progressBar));
         });
         this._fileComboBox.addActionListener(var2x -> {
            String selectedFile = (String)this._fileComboBox.getSelectedItem();
            if (selectedFile != null) {
               if (selectedFile.equals("..//")) {
                  this.navigateBack();
               } else {
                  File selectedPath = new File(this._currentLogPath, selectedFile);
                  if (selectedPath.isDirectory()) {
                     this._currentLogPath = selectedPath.getAbsolutePath();
                     this.loadLogFiles();
                     deleteButton.setEnabled(false);
                  } else {
                     deleteButton.setEnabled(true);
                     if (selectedPath.exists() && selectedPath.isFile()) {
                        long fileSize = selectedPath.length();
                        String humanReadableSize = this.formatFileSize(fileSize);
                        this._fileLabel.setText("Selected File: " + selectedPath.getName());
                        this._fileSizeLabel.setText("Size: " + humanReadableSize);
                     }
                  }
               }
            }
         });
         JPanel floatingPanel = new JPanel();
         floatingPanel.setLayout(new GridBagLayout());
         floatingPanel.setBackground(new Color(230, 230, 230));
         floatingPanel.setPreferredSize(new Dimension(200, 30));
         floatingPanel.setBorder(BorderFactory.createLineBorder(new Color(180, 180, 180), 1, true));
         GridBagConstraints gbc = new GridBagConstraints();
         gbc.gridx = 0;
         gbc.gridy = 0;
         gbc.anchor = 10;
         this._fileLabel = new JLabel("Selected File: None");
         this._fileLabel.setFont(new Font("Arial", 1, 12));
         this._fileLabel.setForeground(Color.DARK_GRAY);
         this._fileLabel.setVerticalAlignment(0);
         this._fileSizeLabel = new JLabel("Size: (0 KB)");
         this._fileSizeLabel.setFont(new Font("Arial", 0, 11));
         this._fileSizeLabel.setForeground(Color.GRAY);
         this._fileSizeLabel.setHorizontalAlignment(0);
         floatingPanel.add(this._fileLabel, gbc);
         gbc.gridy = 1;
         floatingPanel.add(this._fileSizeLabel, gbc);
         this._fileComboBox.addActionListener(var1x -> {
            String selectedFile = (String)this._fileComboBox.getSelectedItem();
            new Thread(() -> {
               for (float i = 1.0F; i >= 0.0F; i -= 0.1F) {
                  this._fileLabel.setForeground(new Color(0.0F, 0.0F, 0.0F, i));

                  try {
                     Thread.sleep(50L);
                  } catch (InterruptedException var5x) {
                  }
               }

               SwingUtilities.invokeLater(() -> this._fileLabel.setText("Selected File: " + selectedFile));

               for (float i = 0.0F; i <= 1.0F; i += 0.1F) {
                  this._fileLabel.setForeground(new Color(0.0F, 0.0F, 0.0F, i));

                  try {
                     Thread.sleep(50L);
                  } catch (InterruptedException var4x) {
                  }
               }
            }).start();
         });
         topPanel.add(floatingPanel);
         topPanel.add(middlePanel);
         middlePanel.add(deleteButton);
         floatingPanel.add(this._fileLabel);
      } else {
         topPanel.setLayout(new FlowLayout(0, 10, 5));
         topPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
         this._fileComboBox = new JComboBox<>();
         this.loadLogFiles();
         this._fileComboBox.setPreferredSize(new Dimension(180, 28));
         this._fileComboBox.setToolTipText("Select a log file.");
         this._fileComboBox.addActionListener(var1x -> this.loadLogs());
         topPanel.add(new JLabel("Log File: "));
         topPanel.add(this._fileComboBox);
         if (!deleteMode) {
            this._searchField = new JTextField(15);
            this._searchField.setToolTipText("Enter search term.");
            this._searchField.setPreferredSize(new Dimension(150, 28));
            this._searchField.addActionListener(var1x -> this.searchLogs());
            this._searchButton = new JButton("\ud83d\udd0d");
            this._searchButton.setToolTipText("Click to search.");
            this._searchButton.setPreferredSize(new Dimension(50, 28));
            this._searchButton.addActionListener(var1x -> this.searchLogs());
            topPanel.add(new JLabel("\ud83d\udd0d Search: "));
            topPanel.add(this._searchField);
            topPanel.add(this._searchButton);
            this._progressBar = new JProgressBar(0, 100);
            this._progressBar.setStringPainted(true);
            this._progressBar.setPreferredSize(new Dimension(160, 25));
            this._progressBar.setMaximumSize(new Dimension(160, 25));
            this._progressBar.setMinimumSize(new Dimension(160, 25));
            this._progressBar.setVisible(true);
            topPanel.add(this._progressBar);
         }
      }

      return topPanel;
   }

   private void loadLogFiles() {
      File logDirectory = new File(this._currentLogPath);
      if (!logDirectory.exists()) {
         JOptionPane.showMessageDialog(this, "Log directory not found: " + logDirectory.getAbsolutePath(), "Error", 0);
         LOGGER.warning(this.getClass().getName() + ": Log directory not found: " + logDirectory.getAbsolutePath());
      } else {
         this._fileComboBox.removeAllItems();
         this.listLogFiles(logDirectory);
      }
   }

   private void deleteLogFiles(JProgressBar progressBar) {
      final String selectedFile = (String)this._fileComboBox.getSelectedItem();
      if (selectedFile == null) {
         JOptionPane.showMessageDialog(this, "No log file selected.", "Error", 0);
      } else {
         int confirm = JOptionPane.showConfirmDialog(this, "Are you sure you want to delete " + selectedFile + "?", "Confirm Deletion", 0);
         if (confirm == 0) {
            final File logFile = new File(ROOT_LOG_PATH, selectedFile);
            if (logFile.isDirectory()) {
               JOptionPane.showMessageDialog(this, "Cannot delete a directory. Please select a log file.", "Error", 0);
               return;
            }

            if (logFile.exists()) {
               this._progressBar.setValue(0);
               this._progressBar.setStringPainted(true);
               this._progressBar.setString("Deleting...");
               SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
                  {
                     Objects.requireNonNull(LogPanel.this);
                  }

                  protected Void doInBackground() throws Exception {
                     for (int i = 0; i <= 100; i += 25) {
                        Thread.sleep(100L);
                        this.setProgress(i);
                     }

                     if (!logFile.delete()) {
                        throw new Exception("Failed to delete the file.");
                     } else {
                        return null;
                     }
                  }

                  @Override
                  protected void done() {
                     try {
                        this.get();
                        Toolkit.getDefaultToolkit().beep();
                        JOptionPane.showMessageDialog(null, "Log file deleted successfully.");
                        LogPanel.this._fileComboBox.removeItem(selectedFile);
                        LogPanel.this._progressBar.setValue(100);
                        LogPanel.this._progressBar.setString("Done!");
                     } catch (Exception var2) {
                        JOptionPane.showMessageDialog(null, "Error deleting log file.", "Error", 0);
                        LogPanel.this._progressBar.setString("Error!");
                     }
                  }
               };
               worker.addPropertyChangeListener(event -> {
                  if ("progress".equals(event.getPropertyName())) {
                     this._progressBar.setValue((Integer)event.getNewValue());
                  }
               });
               worker.execute();
            } else {
               JOptionPane.showMessageDialog(this, "File not found: " + selectedFile, "Error", 0);
            }
         }
      }
   }

   private void listLogFiles(File directory) {
      this._fileComboBox.removeAllItems();
      if (!this._currentLogPath.equals(ROOT_LOG_PATH)) {
         this._fileComboBox.addItem("..//");
      }

      File[] files = directory.listFiles();
      if (files != null) {
         Arrays.sort(files, (f1, f2) -> {
            if (f1.isDirectory() && !f2.isDirectory()) {
               return -1;
            } else {
               return !f1.isDirectory() && f2.isDirectory() ? 1 : f1.getName().compareToIgnoreCase(f2.getName());
            }
         });

         for (File file : files) {
            if (file.isDirectory()) {
               this._fileComboBox.addItem(file.getName() + "/");
            } else if (file.isFile() && (file.getName().endsWith(".log") || file.getName().endsWith(".txt"))) {
               this._fileComboBox.addItem(file.getName());
            }
         }
      }

      this._fileComboBox.setRenderer(new LogPanel.FileComboBoxRenderer());
   }

   private void loadLogs() {
      final String selectedItem = (String)this._fileComboBox.getSelectedItem();
      if (selectedItem != null && !selectedItem.isEmpty()) {
         if (selectedItem.equals("..//")) {
            this.navigateBack();
         } else if (selectedItem.endsWith("/")) {
            String folderName = selectedItem.substring(0, selectedItem.length() - 1);
            File folder = new File(this._currentLogPath + File.separator + folderName);
            if (folder.exists() && folder.isDirectory()) {
               this._currentLogPath = folder.getAbsolutePath();
               this.listLogFiles(folder);
            }
         } else {
            final File logFile = new File(this._currentLogPath + File.separator + selectedItem);
            if (logFile.exists() && logFile.isFile()) {
               this._searchIndexes = new ArrayList<>();
               this._currentSearchIndex = -1;
               this._searchButton.setEnabled(false);
               this._fileComboBox.setEnabled(false);
               this._progressBar.setValue(0);
               this._progressBar.setStringPainted(true);
               SwingWorker<String, Integer> worker = new SwingWorker<String, Integer>() {
                  {
                     Objects.requireNonNull(LogPanel.this);
                  }

                  protected String doInBackground() throws Exception {
                     StringBuilder logContent = new StringBuilder();

                     try (BufferedReader reader = new BufferedReader(new FileReader(logFile))) {
                        long bytesRead = 0L;
                        int lastReportedProgress = 0;
                        long fileSize = logFile.length();

                        String line;
                        while ((line = reader.readLine()) != null) {
                           logContent.append(line).append('\n');
                           bytesRead += line.length();
                           int progress = (int)(bytesRead * 100L / fileSize);
                           if (progress >= lastReportedProgress + 25) {
                              this.publish(new Integer[]{progress});
                              lastReportedProgress = progress;
                           }
                        }
                     }

                     return logContent.toString();
                  }

                  @Override
                  protected void process(List<Integer> chunks) {
                     int latestProgress = chunks.get(chunks.size() - 1);
                     LogPanel.this._progressBar.setValue(latestProgress);
                     LogPanel.this._progressBar.setString(latestProgress + "%");
                  }

                  @Override
                  protected void done() {
                     try {
                        String content = this.get();
                        LogPanel.this._logTextArea.setText(content);
                        LogPanel.this._logTextArea.setCaretPosition(0);
                        if (content.isEmpty()) {
                           Toolkit.getDefaultToolkit().beep();
                           JOptionPane.showMessageDialog(null, "The selected log file is empty.", "Empty Log", 1);
                        }
                     } catch (Exception var5) {
                        JOptionPane.showMessageDialog(null, "Error reading log file: " + selectedItem, "Error", 0);
                        LogPanel.LOGGER.warning(this.getClass().getName() + ": Error reading log file: " + selectedItem + " - " + var5.getMessage());
                     } finally {
                        LogPanel.this._searchButton.setEnabled(true);
                        LogPanel.this._fileComboBox.setEnabled(true);
                        LogPanel.this._progressBar.setValue(100);
                        LogPanel.this._progressBar.setString("Done!");
                     }
                  }
               };
               worker.execute();
            } else {
               JOptionPane.showMessageDialog(this, "Warning: The selected log file does not exist.", "File Not Found", 2);
            }
         }
      }
   }

   private void searchLogs() {
      final String searchTerm = this._searchField.getText().trim();
      if (!searchTerm.isEmpty()) {
         this.clearHighlights();
         this._searchButton.setEnabled(false);
         this._progressBar.setVisible(true);
         this._progressBar.setValue(0);
         this._progressBar.setStringPainted(true);
         SwingWorker<List<Integer>, Integer> worker = new SwingWorker<List<Integer>, Integer>() {
            {
               Objects.requireNonNull(LogPanel.this);
            }

            protected List<Integer> doInBackground() {
               List<Integer> indexes = new ArrayList<>();
               String logContent = LogPanel.this._logTextArea.getText();
               if (logContent.isEmpty()) {
                  return indexes;
               } else {
                  String lowerLog = logContent.toLowerCase();
                  String lowerSearch = searchTerm.toLowerCase();
                  int totalLength = logContent.length();
                  int index = lowerLog.indexOf(lowerSearch);
                  int processedChars = 0;

                  for (int lastReportedProgress = 0; index >= 0; index = lowerLog.indexOf(lowerSearch, index + 1)) {
                     indexes.add(index);
                     processedChars += index + searchTerm.length();
                     int progress = (int)(processedChars * 100.0 / totalLength);
                     if (progress >= lastReportedProgress + 25) {
                        this.publish(new Integer[]{progress});
                        lastReportedProgress = progress;
                     }
                  }

                  return indexes;
               }
            }

            @Override
            protected void process(List<Integer> chunks) {
               if (!chunks.isEmpty()) {
                  int latestProgress = chunks.get(chunks.size() - 1);
                  LogPanel.this._progressBar.setValue(latestProgress);
                  LogPanel.this._progressBar.setString(latestProgress + "%");
                  LogPanel.this._progressBar.repaint();
               }
            }

            @Override
            protected void done() {
               try {
                  LogPanel.this._searchIndexes = this.get();
                  if (LogPanel.this._searchIndexes.isEmpty()) {
                     Toolkit.getDefaultToolkit().beep();
                     JOptionPane.showMessageDialog(LogPanel.this._searchButton.getParent(), "No matches found for: " + searchTerm, "Search Results", 1);
                     LogPanel.this._currentSearchIndex = -1;
                  } else {
                     LogPanel.this._currentSearchIndex = (LogPanel.this._currentSearchIndex + 1) % LogPanel.this._searchIndexes.size();
                     LogPanel.this.highlightSearchResult(LogPanel.this._currentSearchIndex);
                  }
               } catch (Exception var5) {
                  JOptionPane.showMessageDialog(LogPanel.this._searchButton.getParent(), "Error during search: " + var5.getMessage(), "Search Error", 0);
                  LogPanel.LOGGER.warning(this.getClass().getName() + ": Error during search: " + var5.getMessage());
               } finally {
                  LogPanel.this._searchButton.setEnabled(true);
                  LogPanel.this._progressBar.setValue(100);
                  LogPanel.this._progressBar.setString("100%");
               }
            }
         };
         worker.execute();
      }
   }

   private void highlightSearchResult(int index) {
      if (!this._searchIndexes.isEmpty()) {
         int start = this._searchIndexes.get(index);
         int end = start + this._searchField.getText().length();
         Highlighter highlighter = this._logTextArea.getHighlighter();
         HighlightPainter painter = new DefaultHighlightPainter(Color.MAGENTA);

         try {
            highlighter.addHighlight(start, end, painter);
         } catch (BadLocationException var7) {
            LOGGER.warning(this.getClass().getName() + ": Bad location exception. " + var7.getMessage());
         }

         this._logTextArea.setCaretPosition(start);
         this._logTextArea.select(start, end);
      }
   }

   private void clearHighlights() {
      Highlighter highlighter = this._logTextArea.getHighlighter();
      highlighter.removeAllHighlights();
   }

   private void refreshComboBox() {
      this._fileComboBox.removeAllItems();
      File currentDir = new File(this._currentLogPath);
      if (!this._currentLogPath.equals(ROOT_LOG_PATH)) {
         this._fileComboBox.addItem("..//");
      }

      File[] files = currentDir.listFiles();
      if (files != null) {
         Arrays.sort(files, (f1, f2) -> {
            if (f1.isDirectory() && !f2.isDirectory()) {
               return -1;
            } else {
               return !f1.isDirectory() && f2.isDirectory() ? 1 : f1.getName().compareToIgnoreCase(f2.getName());
            }
         });

         for (File file : files) {
            if (file.isDirectory()) {
               this._fileComboBox.addItem(file.getName() + "/");
            } else if (file.isFile() && (file.getName().endsWith(".log") || file.getName().endsWith(".txt"))) {
               this._fileComboBox.addItem(file.getName());
            }
         }
      }

      this._fileComboBox.setRenderer(new LogPanel.FileComboBoxRenderer());
   }

   private void navigateBack() {
      File currentDir = new File(this._currentLogPath);
      File parentDir = currentDir.getParentFile();
      if (parentDir != null && parentDir.getAbsolutePath().equals(ROOT_LOG_PATH)) {
         this._currentLogPath = ROOT_LOG_PATH;
         this.refreshComboBox();
      } else if (parentDir != null && !this._currentLogPath.equals(ROOT_LOG_PATH)) {
         this._currentLogPath = parentDir.getAbsolutePath();
         this.refreshComboBox();
      }
   }

   private String formatFileSize(long size) {
      if (size >= 1048576L) {
         return String.format("%.2f MB", size / 1048576.0);
      } else {
         return size >= 1024L ? String.format("%.2f KB", size / 1024.0) : size + " Bytes";
      }
   }

   public static LogPanel getInstance(boolean deleteMode) {
      return LogPanel.SingletonHolder.getInstance(deleteMode);
   }

   private class FileComboBoxRenderer extends DefaultListCellRenderer {
      private final Icon folderIcon;
      private final Icon fileIcon;

      private FileComboBoxRenderer() {
         Objects.requireNonNull(LogPanel.this);
         super();
         this.folderIcon = UIManager.getIcon("FileView.directoryIcon");
         this.fileIcon = UIManager.getIcon("FileView.fileIcon");
      }

      @Override
      public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
         JLabel label = (JLabel)super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
         if (value != null) {
            String text = value.toString();
            if (text.endsWith("/")) {
               label.setIcon(this.folderIcon);
               label.setText(text.substring(0, text.length() - 1));
            } else {
               label.setIcon(this.fileIcon);
            }
         }

         return label;
      }
   }

   private static class SingletonHolder {
      private static LogPanel instance;

      public static LogPanel getInstance(boolean deleteMode) {
         if (instance == null) {
            instance = new LogPanel(deleteMode);
         }

         return instance;
      }
   }
}
