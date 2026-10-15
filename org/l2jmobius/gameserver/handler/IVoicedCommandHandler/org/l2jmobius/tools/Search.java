package org.l2jmobius.tools;

import java.awt.BorderLayout;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.GraphicsEnvironment;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.ItemListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Scanner;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;
import javax.swing.table.TableRowSorter;
import org.l2jmobius.commons.time.TimeUtil;
import org.l2jmobius.commons.ui.DarkTheme;
import org.l2jmobius.commons.ui.SplashScreen;
import org.l2jmobius.commons.util.ConfigReader;

public class Search extends JFrame {
   private static final Set<String> ALLOWED_EXTENSIONS = new HashSet<>();
   private static final Set<String> IGNORE_LIST = new HashSet<>();
   private static final Path START_DIR = Paths.get(System.getProperty("user.dir"));
   private static int _totalFilesSearched = 0;
   private static int _totalMatchesFound = 0;
   private static int _filesWithMatches = 0;
   private static boolean _caseInsensitive = true;
   private JTextField _searchField;
   private DefaultTableModel _tableModel;
   private JLabel _summaryLabel;
   private JLabel _timeLabel;
   private JButton _searchButton;
   private JCheckBox _allCheckBox;
   private JCheckBox _iniCheckBox;
   private JCheckBox _xmlCheckBox;
   private JCheckBox _javaCheckBox;
   private JCheckBox _sqlCheckBox;
   private JCheckBox _htmlCheckBox;
   private JProgressBar _progressBar;
   private JPanel _summaryPanel;
   private boolean _isSearching = false;
   private List<String> _selectedFilePaths = new ArrayList<>();

   private Search() {
      Path configPath = START_DIR.resolve("dist" + File.separator + "game" + File.separator + "config");
      ConfigReader interfaceConfig;
      if (Files.exists(configPath) && Files.isDirectory(configPath)) {
         interfaceConfig = new ConfigReader(configPath + File.separator + "Interface.ini");
      } else {
         interfaceConfig = new ConfigReader("." + File.separator + "config" + File.separator + "Interface.ini");
      }

      if (interfaceConfig.getBoolean("EnableGUI", true) && !GraphicsEnvironment.isHeadless()) {
         System.setProperty("sun.java2d.opengl", "false");
         System.setProperty("sun.java2d.d3d", "false");
         System.setProperty("sun.java2d.noddraw", "true");
         if (interfaceConfig.getBoolean("DarkTheme", true)) {
            DarkTheme.activate();
         }

         this.gui();
      } else {
         this.console();
      }
   }

   private void gui() {
      Path imagesPath = START_DIR.resolve("dist" + File.separator + "images");
      if (Files.exists(imagesPath) && Files.isDirectory(imagesPath)) {
         new SplashScreen(imagesPath + File.separator + "splash.png", 3000L, this);
         List<Image> icons = new ArrayList<>();
         icons.add(new ImageIcon(imagesPath + File.separator + "l2jmobius_16x16.png").getImage());
         icons.add(new ImageIcon(imagesPath + File.separator + "l2jmobius_32x32.png").getImage());
         icons.add(new ImageIcon(imagesPath + File.separator + "l2jmobius_64x64.png").getImage());
         icons.add(new ImageIcon(imagesPath + File.separator + "l2jmobius_128x128.png").getImage());
         this.setIconImages(icons);
      } else {
         new SplashScreen(".." + File.separator + "images" + File.separator + "splash.png", 3000L, this);
         List<Image> icons = new ArrayList<>();
         icons.add(new ImageIcon(".." + File.separator + "images" + File.separator + "l2jmobius_16x16.png").getImage());
         icons.add(new ImageIcon(".." + File.separator + "images" + File.separator + "l2jmobius_32x32.png").getImage());
         icons.add(new ImageIcon(".." + File.separator + "images" + File.separator + "l2jmobius_64x64.png").getImage());
         icons.add(new ImageIcon(".." + File.separator + "images" + File.separator + "l2jmobius_128x128.png").getImage());
         this.setIconImages(icons);
      }

      this.setTitle("Mobius - Search");
      this.setMinimumSize(new Dimension(800, 600));
      this.setDefaultCloseOperation(3);
      this.setLocationRelativeTo(null);
      JPanel searchPanel = new JPanel();
      searchPanel.setLayout(new GridLayout(2, 1));
      JPanel searchTextPanel = new JPanel();
      searchTextPanel.add(new JLabel("Search text (regex supported):"));
      this._searchField = new JTextField(30);
      searchTextPanel.add(this._searchField);
      this._searchField.addKeyListener(new KeyAdapter() {
         {
            Objects.requireNonNull(Search.this);
         }

         @Override
         public void keyPressed(KeyEvent e) {
            if (e.getKeyCode() == 10) {
               Search.this.performGuiSearch();
            }
         }
      });
      this._searchButton = new JButton("Search");
      searchTextPanel.add(this._searchButton);
      JCheckBox caseSensitiveCheckBox = new JCheckBox("Case Sensitive", !_caseInsensitive);
      searchTextPanel.add(caseSensitiveCheckBox, "East");
      caseSensitiveCheckBox.addItemListener(var1x -> _caseInsensitive = !caseSensitiveCheckBox.isSelected());
      searchPanel.add(searchTextPanel);
      JPanel checkBoxPanel = new JPanel(new BorderLayout());
      JPanel centerPanel = new JPanel();
      this._allCheckBox = new JCheckBox("All", true);
      this._iniCheckBox = new JCheckBox("ini", true);
      this._xmlCheckBox = new JCheckBox("xml", true);
      this._javaCheckBox = new JCheckBox("java", true);
      this._sqlCheckBox = new JCheckBox("sql", true);
      this._htmlCheckBox = new JCheckBox("html", true);
      centerPanel.add(this._allCheckBox);
      centerPanel.add(this._iniCheckBox);
      centerPanel.add(this._sqlCheckBox);
      centerPanel.add(this._xmlCheckBox);
      centerPanel.add(this._javaCheckBox);
      centerPanel.add(this._htmlCheckBox);
      checkBoxPanel.add(centerPanel, "Center");
      this._allCheckBox.addItemListener(var1x -> {
         boolean isSelected = this._allCheckBox.isSelected();
         this._iniCheckBox.setSelected(isSelected);
         this._xmlCheckBox.setSelected(isSelected);
         this._javaCheckBox.setSelected(isSelected);
         this._sqlCheckBox.setSelected(isSelected);
         this._htmlCheckBox.setSelected(isSelected);
      });
      ItemListener checkboxListener = var1x -> {
         boolean isIniCheckBoxSelected = this._iniCheckBox.isSelected();
         boolean isXmlCheckBoxSelected = this._xmlCheckBox.isSelected();
         boolean isJavaCheckBoxSelected = this._javaCheckBox.isSelected();
         boolean isSqlCheckBoxSelected = this._sqlCheckBox.isSelected();
         boolean isHtmlCheckBoxSelected = this._htmlCheckBox.isSelected();
         if (!isIniCheckBoxSelected || !isXmlCheckBoxSelected || !isJavaCheckBoxSelected || !isSqlCheckBoxSelected || !isHtmlCheckBoxSelected) {
            this._allCheckBox.setSelected(false);
            this._iniCheckBox.setSelected(isIniCheckBoxSelected);
            this._xmlCheckBox.setSelected(isXmlCheckBoxSelected);
            this._javaCheckBox.setSelected(isJavaCheckBoxSelected);
            this._sqlCheckBox.setSelected(isSqlCheckBoxSelected);
            this._htmlCheckBox.setSelected(isHtmlCheckBoxSelected);
         } else if (isIniCheckBoxSelected && isXmlCheckBoxSelected && isJavaCheckBoxSelected && isSqlCheckBoxSelected && isHtmlCheckBoxSelected) {
            this._allCheckBox.setSelected(true);
         }
      };
      this._iniCheckBox.addItemListener(checkboxListener);
      this._xmlCheckBox.addItemListener(checkboxListener);
      this._javaCheckBox.addItemListener(checkboxListener);
      this._sqlCheckBox.addItemListener(checkboxListener);
      this._htmlCheckBox.addItemListener(checkboxListener);
      searchPanel.add(checkBoxPanel);
      this._tableModel = new DefaultTableModel(new String[]{"File Name", "Matches"}, 0) {
         {
            Objects.requireNonNull(Search.this);
         }

         @Override
         public boolean isCellEditable(int row, int column) {
            return false;
         }
      };
      final JTable resultTable = new JTable(this._tableModel);
      resultTable.setAutoCreateRowSorter(true);
      TableColumn matchesColumn1 = resultTable.getColumnModel().getColumn(0);
      matchesColumn1.setPreferredWidth(740);
      TableColumn matchesColumn2 = resultTable.getColumnModel().getColumn(1);
      matchesColumn2.setPreferredWidth(60);
      DefaultTableCellRenderer rightRenderer = new DefaultTableCellRenderer();
      rightRenderer.setHorizontalAlignment(4);
      matchesColumn2.setCellRenderer(rightRenderer);
      TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(this._tableModel);
      resultTable.setRowSorter(sorter);
      sorter.setComparator(1, (o1, o2) -> {
         int matches1 = Integer.parseInt(o1.toString());
         int matches2 = Integer.parseInt(o2.toString());
         return Integer.compare(matches1, matches2);
      });
      this._summaryPanel = new JPanel(new GridLayout(2, 1));
      this._summaryLabel = new JLabel("");
      this._timeLabel = new JLabel();
      this._summaryPanel.add(this._summaryLabel);
      this._summaryPanel.add(this._timeLabel);
      this._progressBar = new JProgressBar(0, 100);
      this._progressBar.setStringPainted(true);
      this._progressBar.setVisible(false);
      this._progressBar.setPreferredSize(new Dimension(this._progressBar.getPreferredSize().width, 30));
      JPanel bottomPanel = new JPanel(new BorderLayout());
      bottomPanel.add(this._progressBar, "Center");
      bottomPanel.add(this._summaryPanel, "South");
      this.setLayout(new BorderLayout());
      this.add(searchPanel, "North");
      this.add(new JScrollPane(resultTable), "Center");
      this.add(bottomPanel, "South");
      this._searchButton.addActionListener(var1x -> this.performGuiSearch());
      final JPopupMenu popupMenu = new JPopupMenu();
      JMenuItem openWithMenuItem = new JMenuItem("Open File");
      popupMenu.add(openWithMenuItem);
      JMenuItem copyFileMenuItem = new JMenuItem("Copy File");
      popupMenu.add(copyFileMenuItem);
      JMenuItem openLocationMenuItem = new JMenuItem("Open Location");
      popupMenu.add(openLocationMenuItem);
      JMenuItem copyLocationMenuItem = new JMenuItem("Copy Location");
      popupMenu.add(copyLocationMenuItem);
      JMenuItem propertiesMenuItem = new JMenuItem("Properties");
      popupMenu.add(propertiesMenuItem);
      openWithMenuItem.addActionListener(var1x -> {
         for (String filePath : this._selectedFilePaths) {
            this.openFile(filePath);
         }
      });
      copyFileMenuItem.addActionListener(var1x -> this.copyFile(this._selectedFilePaths));
      openLocationMenuItem.addActionListener(var1x -> {
         for (String filePath : this._selectedFilePaths) {
            this.openFileLocation(filePath);
         }
      });
      copyLocationMenuItem.addActionListener(var1x -> this.copyFileLocation(this._selectedFilePaths));
      propertiesMenuItem.addActionListener(var1x -> {
         for (String filePath : this._selectedFilePaths) {
            this.showProperties(filePath);
         }
      });
      resultTable.addMouseListener(
         new MouseAdapter() {
            {
               Objects.requireNonNull(Search.this);
            }

            @Override
            public void mouseClicked(MouseEvent event) {
               if (SwingUtilities.isRightMouseButton(event)) {
                  int row = resultTable.rowAtPoint(event.getPoint());
                  if (row >= 0) {
                     if (!resultTable.isRowSelected(row)) {
                        resultTable.setRowSelectionInterval(row, row);
                     }

                     Search.this._selectedFilePaths = Arrays.stream(resultTable.getSelectedRows())
                        .mapToObj(selectedRow -> (String)Search.this._tableModel.getValueAt(resultTable.convertRowIndexToModel(selectedRow), 0))
                        .toList();
                     popupMenu.show(resultTable, event.getX(), event.getY());
                  }
               } else if (event.getClickCount() == 2) {
                  int row = resultTable.getSelectedRow();
                  if (row >= 0) {
                     String filePath = (String)Search.this._tableModel.getValueAt(row, 0);
                     Search.this.openFile(filePath);
                  }
               }
            }
         }
      );
   }

   private void openFile(String filePath) {
      try {
         File file = new File(filePath);
         if (file.exists()) {
            if (Desktop.isDesktopSupported()) {
               Desktop.getDesktop().open(file);
            } else {
               System.err.println("Desktop is not supported on this platform.");
            }
         } else {
            System.err.println("File does not exist: " + filePath);
         }
      } catch (IOException var3) {
         System.err.println("Failed to open file: " + filePath);
         var3.printStackTrace();
      }
   }

   private void openFileLocation(String filePath) {
      try {
         File file = new File(filePath);
         if (file.exists()) {
            if (Desktop.isDesktopSupported()) {
               try {
                  Desktop.getDesktop().browseFileDirectory(file);
               } catch (Exception var4) {
                  this.openFile(file.getParent());
               }
            } else {
               System.err.println("Desktop is not supported on this platform.");
            }
         } else {
            System.err.println("File does not exist: " + filePath);
         }
      } catch (Exception var5) {
         System.err.println("Failed to open file location: " + filePath);
         var5.printStackTrace();
      }
   }

   private void copyFile(List<String> filePaths) {
      final List<File> files = filePaths.stream().map(File::new).toList();

      for (File file : files) {
         if (!file.exists()) {
            System.err.println("File does not exist: " + file.getAbsolutePath());
            return;
         }
      }

      Transferable transferable = new Transferable() {
         private final List<File> fileList;

         {
            Objects.requireNonNull(Search.this);
            this.fileList = files;
         }

         @Override
         public DataFlavor[] getTransferDataFlavors() {
            return new DataFlavor[]{DataFlavor.javaFileListFlavor};
         }

         @Override
         public boolean isDataFlavorSupported(DataFlavor flavor) {
            return DataFlavor.javaFileListFlavor.equals(flavor);
         }

         @Override
         public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException {
            if (this.isDataFlavorSupported(flavor)) {
               return this.fileList;
            } else {
               throw new UnsupportedFlavorException(flavor);
            }
         }
      };
      Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
      clipboard.setContents(transferable, null);
   }

   private void copyFileLocation(List<String> filePaths) {
      String filePathsString = String.join(System.lineSeparator(), filePaths);
      StringSelection stringSelection = new StringSelection(filePathsString);
      Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
      clipboard.setContents(stringSelection, null);
   }

   private void showProperties(String filePath) {
      try {
         Path path = Paths.get(filePath);
         File file = path.toFile();
         BasicFileAttributes attributes = Files.readAttributes(path, BasicFileAttributes.class);
         String creationTime = TimeUtil.getDateTimeString(attributes.creationTime().toMillis());
         String lastModifiedTime = TimeUtil.getDateTimeString(attributes.lastModifiedTime().toMillis());
         String fileSize = this.formatFileSize(file.length());
         String message = "File Name: "
            + file.getName()
            + System.lineSeparator()
            + "File Size: "
            + fileSize
            + System.lineSeparator()
            + "Creation date: "
            + creationTime
            + System.lineSeparator()
            + "Last Modified: "
            + lastModifiedTime;
         JOptionPane.showMessageDialog(null, message, "File Properties", 1);
      } catch (Exception var9) {
         var9.printStackTrace();
         JOptionPane.showMessageDialog(null, "Unable to retrieve file properties.", "Error", 0);
      }
   }

   private String formatFileSize(long size) {
      if (size < 1024L) {
         return size + " bytes";
      } else if (size < 1048576L) {
         return String.format("%.2f KB", size / 1024.0);
      } else {
         return size < 1073741824L ? String.format("%.2f MB", size / 1048576.0) : String.format("%.2f GB", size / 1.0737418E9F);
      }
   }

   private void performGuiSearch() {
      if (this._isSearching) {
         this._isSearching = false;
         this._searchButton.setText("Search");
      } else {
         String regexPattern = this._searchField.getText();
         if (regexPattern.isEmpty()) {
            this._summaryLabel.setText("Please enter a search text.");
         } else {
            if (!this._iniCheckBox.isSelected()
               && !this._xmlCheckBox.isSelected()
               && !this._javaCheckBox.isSelected()
               && !this._sqlCheckBox.isSelected()
               && !this._htmlCheckBox.isSelected()) {
               this._allCheckBox.setSelected(true);
            }

            this._searchField.setEnabled(false);
            this._searchButton.setText("  Stop  ");
            this._isSearching = true;
            this._tableModel.setRowCount(0);
            _totalFilesSearched = 0;
            _totalMatchesFound = 0;
            _filesWithMatches = 0;
            this._summaryLabel.setVisible(false);
            this._timeLabel.setVisible(false);
            ALLOWED_EXTENSIONS.clear();
            if (this._allCheckBox.isSelected()) {
               ALLOWED_EXTENSIONS.add(".ini");
               ALLOWED_EXTENSIONS.add(".sql");
               ALLOWED_EXTENSIONS.add(".xml");
               ALLOWED_EXTENSIONS.add(".java");
               ALLOWED_EXTENSIONS.add(".htm");
               ALLOWED_EXTENSIONS.add(".html");
            } else {
               if (this._iniCheckBox.isSelected()) {
                  ALLOWED_EXTENSIONS.add(".ini");
               }

               if (this._sqlCheckBox.isSelected()) {
                  ALLOWED_EXTENSIONS.add(".sql");
               }

               if (this._xmlCheckBox.isSelected()) {
                  ALLOWED_EXTENSIONS.add(".xml");
               }

               if (this._javaCheckBox.isSelected()) {
                  ALLOWED_EXTENSIONS.add(".java");
               }

               if (this._htmlCheckBox.isSelected()) {
                  ALLOWED_EXTENSIONS.add(".htm");
                  ALLOWED_EXTENSIONS.add(".html");
               }
            }

            if (Files.exists(START_DIR) && Files.isDirectory(START_DIR)) {
               this._summaryPanel.remove(this._summaryLabel);
               this._summaryPanel.remove(this._timeLabel);
               this._summaryLabel = new JLabel("");
               this._timeLabel = new JLabel();
               this._summaryPanel.add(this._summaryLabel);
               this._summaryPanel.add(this._timeLabel);
               final Pattern pattern = Pattern.compile(regexPattern, _caseInsensitive ? 2 : 0);
               String truncatedPattern = regexPattern.length() > 50 ? regexPattern.substring(0, 50) : regexPattern;
               this.setTitle("Mobius - Searching for " + truncatedPattern + "...");
               final long startTime = System.currentTimeMillis();
               this._progressBar.setVisible(true);
               this._progressBar.setValue(0);
               SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
                  {
                     Objects.requireNonNull(Search.this);
                  }

                  protected Void doInBackground() {
                     try (Stream<Path> stream = Files.walk(Search.START_DIR)) {
                        long totalFiles = Files.walk(Search.START_DIR)
                           .filter(x$0 -> Files.isRegularFile(x$0))
                           .filter(pathx -> Search.this.shouldProcessFile(pathx))
                           .count();
                        long processedFiles = 0L;

                        for (Path path : stream.filter(x$0 -> Files.isRegularFile(x$0))::iterator) {
                           if (!Search.this._isSearching) {
                              break;
                           }

                           if (Search.this.shouldProcessFile(path)) {
                              Search._totalFilesSearched++;
                              int matchCount = Search.this.countPatternMatches(path, pattern);
                              if (matchCount > 0) {
                                 Search._totalMatchesFound += matchCount;
                                 Search._filesWithMatches++;
                                 SwingUtilities.invokeLater(() -> Search.this._tableModel.addRow(new Object[]{path.toAbsolutePath().toString(), matchCount}));
                              }

                              processedFiles++;
                              int progress = (int)(processedFiles * 100L / totalFiles);
                              SwingUtilities.invokeLater(() -> Search.this._progressBar.setValue(progress));
                           }
                        }
                     } catch (Exception var12) {
                        SwingUtilities.invokeLater(() -> {
                           Search.this._summaryLabel.setText("An error occurred while searching: " + var12.getMessage());
                           Search.this._summaryLabel.setVisible(true);
                           Search.this._timeLabel.setVisible(true);
                        });
                     }

                     return null;
                  }

                  @Override
                  protected void done() {
                     Search.this._searchField.setEnabled(true);
                     Search.this._searchButton.setText("Search");
                     Search.this._isSearching = false;
                     Search.this._progressBar.setVisible(false);
                     Search.this._summaryLabel.setVisible(true);
                     Search.this._timeLabel.setVisible(true);
                     if (Search._filesWithMatches == 0) {
                        Search.this._summaryLabel.setText("No files matching the text were found.");
                     } else {
                        Search.this._summaryLabel
                           .setText(
                              "From "
                                 + Search._totalFilesSearched
                                 + " files searched, found "
                                 + Search._totalMatchesFound
                                 + " matches in "
                                 + Search._filesWithMatches
                                 + " files."
                           );
                     }

                     Search.this._timeLabel.setText("Search complete in " + TimeUtil.formatDuration(System.currentTimeMillis() - startTime) + ".");
                     Search.this.setTitle("Mobius - Search");
                  }
               };
               worker.execute();
            } else {
               this._summaryLabel.setText("The user directory does not exist or is not a directory.");
               this._summaryLabel.setVisible(true);
               this._timeLabel.setVisible(true);
            }
         }
      }
   }

   private void console() {
      System.out.println("=========================================================");
      System.out.println("             L2jMobius Development - Search              ");
      System.out.println("=========================================================");
      Scanner scanner = new Scanner(System.in);
      System.out.print("Enter search text (regex supported): ");
      String regexPattern = scanner.nextLine();
      scanner.close();
      if (Files.exists(START_DIR) && Files.isDirectory(START_DIR)) {
         Pattern pattern = Pattern.compile(regexPattern, _caseInsensitive ? 2 : 0);
         _totalFilesSearched = 0;
         _totalMatchesFound = 0;
         _filesWithMatches = 0;
         String truncatedPattern = regexPattern.length() > 50 ? regexPattern.substring(0, 50) : regexPattern;
         System.out.println("Searching for " + truncatedPattern + "...");
         long startTime = System.currentTimeMillis();

         try (Stream<Path> stream = Files.walk(START_DIR)) {
            stream.filter(x$0 -> Files.isRegularFile(x$0)).filter(path -> this.shouldProcessFile(path)).forEach(path -> {
               _totalFilesSearched++;
               int matchCount = this.countPatternMatches(path, pattern);
               if (matchCount > 0) {
                  _totalMatchesFound += matchCount;
                  _filesWithMatches++;
                  System.out.println(path.toAbsolutePath() + " - " + matchCount + " matches");
               }
            });
         } catch (Exception var12) {
            System.err.println("An error occurred while searching: " + var12.getMessage());
         }

         if (_filesWithMatches == 0) {
            System.out.println("No files matching the text were found.");
         } else {
            System.out.println("From " + _totalFilesSearched + " files searched, found " + _totalMatchesFound + " matches in " + _filesWithMatches + " files.");
         }

         System.out.println("Search complete in " + TimeUtil.formatDuration(System.currentTimeMillis() - startTime) + ".");
      } else {
         System.err.println("The user directory does not exist or is not a directory.");
      }
   }

   private boolean shouldProcessFile(Path path) {
      String pathString = path.toString();

      for (String ignore : IGNORE_LIST) {
         if (pathString.contains(ignore)) {
            return false;
         }
      }

      if (!ALLOWED_EXTENSIONS.isEmpty()) {
         String fileName = path.getFileName().toString().toLowerCase();

         for (String extention : ALLOWED_EXTENSIONS) {
            if (fileName.endsWith(extention)) {
               return true;
            }
         }

         return false;
      } else {
         return true;
      }
   }

   private int countPatternMatches(Path file, Pattern pattern) {
      try {
         String content = Files.readString(file, StandardCharsets.UTF_8);
         Matcher matcher = pattern.matcher(content);
         int matchCount = 0;

         while (matcher.find()) {
            matchCount++;
         }

         return matchCount;
      } catch (IOException var6) {
         return 0;
      }
   }

   public static void main(String[] args) {
      for (String arg : args) {
         if (arg.startsWith("-ext=")) {
            String extensions = arg.substring("-ext=".length());
            ALLOWED_EXTENSIONS.addAll(Arrays.stream(extensions.split(",")).map(ext -> "." + ext.toLowerCase()).collect(Collectors.toSet()));
         } else if (arg.equalsIgnoreCase("-caseSensitive")) {
            _caseInsensitive = false;
         }
      }

      SwingUtilities.invokeLater(Search::new);
   }

   static {
      IGNORE_LIST.add(File.separator + "bin" + File.separator);
      IGNORE_LIST.add(".svn" + File.separator);
      IGNORE_LIST.add(".git" + File.separator);
   }
}
