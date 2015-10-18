package fr.tikione.steam.cleaner.gui.dialog;

import fr.tikione.ini.InfinitiveLoopException;
import fr.tikione.ini.util.StringHelper;
import fr.tikione.steam.cleaner.Version;
import fr.tikione.steam.cleaner.util.FileComparator;
import fr.tikione.steam.cleaner.util.FileUtils;
import fr.tikione.steam.cleaner.util.GraphicsUtils;
import fr.tikione.steam.cleaner.util.Log;
import fr.tikione.steam.cleaner.util.Redist;
import fr.tikione.steam.cleaner.util.RedistTableModel;
import fr.tikione.steam.cleaner.util.Translation;
import fr.tikione.steam.cleaner.util.UpdateManager;
import fr.tikione.steam.cleaner.util.conf.Config;
import fr.tikione.steam.cleaner.util.conf.CustomFolders;
import fr.tikione.steam.cleaner.util.conf.DangerousItems;
import fr.tikione.steam.cleaner.util.conf.Patterns;
import fr.tikione.steam.cleaner.util.conf.UncheckedItems;
import java.awt.Color;
import java.io.File;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowStateListener;
import java.io.CharConversionException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.JToolBar;
import javax.swing.JOptionPane;
import javax.swing.LayoutStyle.ComponentPlacement;
import javax.swing.ListSelectionModel;
import javax.swing.RowSorter;
import javax.swing.SwingConstants;
import javax.swing.WindowConstants;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;

/**
 * Main window.
 */
@SuppressWarnings("serial")
public class JFrameMain extends JFrame {

	/** Indicate if the application in exiting. */
	private static boolean CLOSING_APP = false;

	/** Initial label of the Reload button. */
	private String btnReloadLabelInitial;

	/** Label of the Reload button when application is searching for redistributable packages. */
	private String btnReloadLabelWorking;

	/** Initial label of the table that contains list of redistributable packages found. */
	private String tblRedistLabelDefault;

	/** Default table model for the list of redistributable packages found. */
	private DefaultTableModel model;

	private final DefaultListModel<File> listModel;

	/** List of non-checked items in the list of redistributable packages. */
	private List<String> uncheckedRedistPathList = new ArrayList<>(8);

	/** Application configuration handler. */
	private final Config config;

	/** The program configuration handler (redist patterns). */
	private final Patterns patternsCfg;

	/** Application configuration handler. */
	private final UncheckedItems uncheckedItems;

	/** Application configuration handler. */
	private final CustomFolders customFolders;

	/** Application configuration handler. */
	private final DangerousItems dangerousItems;

	/** List of folders patterns to exclude from the search path. */
	private final List<Pattern> dangerousFolders;

	/** Steam directory. */
	private File fSteamDir = null;

	/** Translated application's messages handler. */
	private final Translation translation;

	/**
	 * Create new form JFrameMain. This is the main form.
	 *
	 * @throws IOException if an error occurs while loading configuration file.
	 * @throws CharConversionException if an error occurs while loading configuration file.
	 * @throws InfinitiveLoopException if an error occurs while loading configuration file.
	 */

	public JFrameMain()
			throws IOException,
			CharConversionException,
			InfinitiveLoopException {
		super();
		GraphicsUtils.setIcon(this);
		config = Config.getInstance();
		patternsCfg = Patterns.getInstance();
		dangerousItems = DangerousItems.getInstance();
		dangerousFolders = dangerousItems.getDangerousFolders();
		String lngCode = config.getSelectedLanguage();
		translation = new Translation(lngCode);

		model = new RedistTableModel(translation); // Model for the (visual) list a detected redistributable packages.
		initComponents();
		secondInitComponents(translation);
		setTitle("TikiOne Steam Cleaner Plus");
		if (config.getCheckForUpdatesAtStartup()) {
			checkForUpdates();
		}
                
                String a = translation.getString(Translation.SEC_WMAIN, "version.title");
                String b = a.replace("{0}", Version.VERSION);
                
                
		jLabelAppVersion.setText(' ' + b + ' ');

		// Restore latest frame state and center it.
		if (config.getUIState() == JFrame.MAXIMIZED_BOTH) {
			setExtendedState(config.getUIState());
		} else {
			setSize(config.getUILatestWidth(), config.getUILatestHeight());
		}
		GraphicsUtils.setFrameCentered(this);

		// Restore latest saved steam directory. Try to detect it if no steam directory previously found (or first run).
		String latestSteamDirStr = config.getLatestSteamFolder();
		if (latestSteamDirStr.length() > 0) {
			File possibleSteamDir = new File(latestSteamDirStr);
			fSteamDir = possibleSteamDir;
			jTextFieldSteamDir.setText(fSteamDir.getAbsolutePath());
		} else {
			locateSteamDir();
		}

		uncheckedItems = new UncheckedItems();
		uncheckedRedistPathList.addAll(uncheckedItems.getUncheckedItems());
		setTableModelUI();

		listModel = new DefaultListModel<>();
		jListCustomFolders.setModel(listModel);

		customFolders = new CustomFolders();
		List<String> customFoldersAsStr = customFolders.getCustomFolders();
		customFoldersAsStr.stream().forEach((foldername) -> {
			listModel.addElement(new File(foldername));
		});
		
		jButtonReloadRedistList.setEnabled(true);
	}

	/**
	 * Additional components initialization : translate messages and titles, and make some custom UI transformations.
	 *
	 * @param translation the program language translation handler.
	 */
	private void secondInitComponents(Translation translation) {
		btnReloadLabelInitial = translation.getString(Translation.SEC_WMAIN, "button.search");
		btnReloadLabelWorking = translation.getString(Translation.SEC_WMAIN, "button.search.working");
		tblRedistLabelDefault = translation.getString(Translation.SEC_WMAIN, "redistList.title");
		jButtonLocateSteamDir.setText(translation.getString(Translation.SEC_WMAIN, "button.locateSteam"));
		jButtonManualSteamDirSearch.setText(translation.getString(Translation.SEC_WMAIN, "button.locateSteamManually"));
		jButtonReloadRedistList.setText(btnReloadLabelInitial);
		jButtonRemoveRedistItemsFromDisk.setText(translation.getString(Translation.SEC_WMAIN, "button.removeSelectedItems"));
		jLabelSteamDir.setText(translation.getString(Translation.SEC_WMAIN, "label.steamDir"));
		jButtonToolbarOptions.setToolTipText(translation.getString(Translation.SEC_WMAIN, "menu.options"));
		jButtonToolbarCheckforupdates.setToolTipText(translation.getString(Translation.SEC_WMAIN, "menu.checkForUpdates"));
		jButtonToolbarAbout.setToolTipText(translation.getString(Translation.SEC_WMAIN, "menu.about"));
		jPanelList.setBorder(BorderFactory.createTitledBorder(tblRedistLabelDefault));
		jButtonAddCustomFolder.setText(translation.getString(Translation.SEC_WMAIN, "button.add.custom.folder"));
		jButtonRemoveCustomFolder.setText(translation.getString(Translation.SEC_WMAIN, "button.rem.custom.folder"));
		jLabelCustomFolders.setText("<html>" + translation.getString(Translation.SEC_WMAIN, "label.custom.folders.list") + "</html>");
		
		jButtonStopSearch.setVisible(false);
		jButtonGithub.setToolTipText("<html><body>" + "TikiOne <b>GitHub</b>"
				+ "<br><font color=\"blue\">https://github.com/wbsdty331/TikiOne-steam-Cleaner-Plus</font></body></html>");

	}

	/**
	 * Get an array of File objetcs from the list of custom folders.
	 *
	 * @return an array of folders.
	 */
	private File[] customFoldersListStrToFiles() {
		int nbFolders = listModel.size();
		List<File> folders = new ArrayList<>(nbFolders + 1);
		for (int i = 0; i < nbFolders; i++) {
			folders.add(listModel.get(i));
		}
		return folders.toArray(new File[folders.size()]);
	}

	/**
	 * Save the list of custom folders to a configuration file.
	 */
	private void memorizeCustomFoldersToConf() {
		try {
			File[] folders = customFoldersListStrToFiles();
			List<String> foldersAsStr = new ArrayList<>(folders.length + 1);
			for (File folder : folders) {
				foldersAsStr.add(folder.getAbsolutePath());
			}
			customFolders.setCustomFolders(foldersAsStr);
		} catch (CharConversionException | InfinitiveLoopException ex) {
			Log.error(ex);
		}
	}
	
	private void recomputeTotalSizeMB() {
		float fsize = 0;
		for (int i = 0; i < model.getRowCount(); i++) {
			if (Boolean.parseBoolean(model.getValueAt(i, 0).toString())) {
				fsize += Float.parseFloat(model.getValueAt(i, 2).toString());
			}
		}
		String btnLabel = translation.getString(Translation.SEC_WMAIN, "button.removeSelectedItems");
		jButtonRemoveRedistItemsFromDisk.setText(btnLabel + " (" + (int)fsize + "MB)");
	}

	/**
	 * Store the list of unchecked items (redistributable packages shown in the main table) to a configuration file.
	 */
	private void memorizeUncheckedItemsToConf() {
		try {
			if (fSteamDir != null) {
				List<String> nowUncheckedItemsList = new ArrayList<>(8);
				List<String> nowCheckedItemsList = new ArrayList<>(8);
				List<String> newUncheckedItemsList = new ArrayList<>(8);
				List<String> prevUncheckedItems = uncheckedItems.getUncheckedItems();
				for (int row = 0; row < model.getRowCount(); row++) {
					boolean checked = (Boolean) model.getValueAt(row, 0);
					String redistFullPath = (String) model.getValueAt(row, 1);
					if (checked) {
						nowCheckedItemsList.add(redistFullPath);
					} else {
						nowUncheckedItemsList.add(redistFullPath);
					}
				}
				newUncheckedItemsList.addAll(nowUncheckedItemsList);
				for (String prevUncheckedItem : prevUncheckedItems) {
					if (!nowCheckedItemsList.contains(prevUncheckedItem) && !newUncheckedItemsList.contains(prevUncheckedItem)) {
						newUncheckedItemsList.add(prevUncheckedItem);
					}
				}
				uncheckedItems.setUncheckedItems(newUncheckedItemsList);
			}
		} catch (CharConversionException | InfinitiveLoopException ex) {
			Log.error(ex);
		}
	}

	private void searchRedistPackagesOnDisk(final JButton buttonReload) {
		buttonReload.setEnabled(false);
		memorizeUncheckedItemsToConf();
		jPanelList.setBorder(BorderFactory.createTitledBorder(tblRedistLabelDefault));
		String sSteamDir = jTextFieldSteamDir.getText();
		if (!listModel.isEmpty() || !StringHelper.strIsEmpty(sSteamDir)) {
			if (!StringHelper.strIsEmpty(sSteamDir) && !sSteamDir.endsWith("/") && !sSteamDir.endsWith(File.separator)) {
				sSteamDir += File.separatorChar;
			}
			fSteamDir = new File(sSteamDir);
			boolean steamExists = fSteamDir.isDirectory() && fSteamDir.exists();
			if (!listModel.isEmpty() || steamExists) {
				enableAllUI(false);
				buttonReload.setText(btnReloadLabelWorking);
				final JFrame thisframe = this;
				Thread tJob = new Thread(() -> {
					try {
						jButtonStopSearch.setVisible(true);
						String nameFile = translation.getString(Translation.SEC_WMAIN, "redistList.item.file");
						String nameFileUpCase = translation.getString(Translation.SEC_WMAIN, "redistList.item.fileUpperCase");
						String nameFiles = translation.getString(Translation.SEC_WMAIN, "redistList.item.files");
						String nameFolder = translation.getString(Translation.SEC_WMAIN, "redistList.item.folder");
						String nameFolderUpCase = translation.getString(Translation.SEC_WMAIN, "redistList.item.folderUppercase");
						String nameFolders = translation.getString(Translation.SEC_WMAIN, "redistList.item.folders");
						model = new RedistTableModel(translation);
						setTableModelUI();
						List<File> srcFolders = new ArrayList<>(32);
						if (fSteamDir.exists()) {
							srcFolders.add(fSteamDir);
						}
						File[] customFolders1 = customFoldersListStrToFiles();
						for (File customFolder : customFolders1) {
							if (!srcFolders.contains(customFolder)) {
								srcFolders.add(customFolder);
							}
						}
						List<File> allFiles = new ArrayList<>(1024);
						FileUtils.listDir(thisframe, allFiles, srcFolders, config.getMaDepth(), dangerousFolders);
						List<Redist> checkedFiles = new ArrayList<>(128);
						List<Redist> checkedFolders = new ArrayList<>(128);
						FileComparator tc = new FileComparator(
								allFiles,
								patternsCfg.getRedistFilePatternsAndDesc(patternsCfg.getEnableExperimentalPatterns()),
								checkedFiles, true);
						try {
							//Log.info("debug: FileComparator on files");
							tc.start();
						} catch (InterruptedException ex) {
							Log.error(ex);
						}
						tc = new FileComparator(
								allFiles,
								patternsCfg.getRedistFolderPatternsAndDesc(patternsCfg.getEnableExperimentalPatterns()),
								checkedFolders, false);
						try {
							//Log.info("debug: FileComparator on folders");
							tc.start();
						} catch (InterruptedException ex) {
							Log.error(ex);
						}
						uncheckedRedistPathList = uncheckedItems.getUncheckedItems();
						checkedFiles.stream().forEach((redist) -> {
							boolean checked = !uncheckedRedistPathList.contains(redist.getFile().getAbsolutePath());
							model.addRow(new Object[]{
								checked,
								redist.getFile().getAbsolutePath(),
								redist.getSize(),
								" (" + nameFileUpCase + ") " + redist.getDescription()});
						});
						checkedFolders.stream().forEach((redist) -> {
							boolean check = !uncheckedRedistPathList.contains(redist.getFile().getAbsolutePath() + File.separatorChar);
							model.addRow(new Object[]{
								check,
								redist.getFile().getAbsolutePath()
										+ File.separatorChar,
								redist.getSize(),
								" (" + nameFolderUpCase + ") " + redist.getDescription()});
						});
						int nbFiles = checkedFiles.size();
						int nbFolders = checkedFolders.size();
						jPanelList.setBorder(BorderFactory.createTitledBorder(tblRedistLabelDefault + " "
								+ nbFiles + " " + (nbFiles > 1 ? nameFiles : nameFile) + ", " + checkedFolders.size() + " "
								+ (nbFolders > 1 ? nameFolders : nameFolder)));
						recomputeTotalSizeMB();
					}catch (InfinitiveLoopException | IOException ex) {
							Log.error(ex);
					} finally {
						jButtonStopSearch.setVisible(false);
						buttonReload.setText(btnReloadLabelInitial);
						enableAllUI(true);
						boolean rdistFound = model.getRowCount() > 0;
						jButtonRemoveRedistItemsFromDisk.setEnabled(rdistFound);
						buttonReload.setEnabled(true);
					}
				});
				tJob.start();
			} else {
				jButtonRemoveRedistItemsFromDisk.setEnabled(false);
				buttonReload.setEnabled(true);
			}
		} else {
			jButtonRemoveRedistItemsFromDisk.setEnabled(false);
			buttonReload.setEnabled(true);
		}
	}

	private void setTableModelUI() {
		jTableRedistList.setModel(model);
		RowSorter<TableModel> sorter = new TableRowSorter<>(model);
		jTableRedistList.setRowSorter(sorter);
		jTableRedistList.getColumnModel().getColumn(0).setPreferredWidth(50);
		jTableRedistList.getColumnModel().getColumn(0).setMinWidth(10);
		jTableRedistList.getColumnModel().getColumn(0).setMaxWidth(100);
		jTableRedistList.getColumnModel().getColumn(2).setPreferredWidth(80);
		jTableRedistList.getColumnModel().getColumn(2).setMinWidth(10);
		jTableRedistList.getColumnModel().getColumn(2).setMaxWidth(200);
		jTableRedistList.getColumnModel().getColumn(3).setPreferredWidth(275);
		jTableRedistList.getColumnModel().getColumn(3).setMinWidth(50);
		jTableRedistList.getColumnModel().getColumn(3).setMaxWidth(500);
	}

	private void enableAllUI(boolean enable) {
		jButtonLocateSteamDir.setEnabled(enable);
		jButtonManualSteamDirSearch.setEnabled(enable);
		jButtonRemoveRedistItemsFromDisk.setEnabled(enable);
		jTableRedistList.setEnabled(enable);
		jTextFieldSteamDir.setEnabled(enable);
		jButtonAddCustomFolder.setEnabled(enable);
		jButtonRemoveCustomFolder.setEnabled(enable);
	}

	/**
	 * This method is called from within the constructor to initialize the form.
	 * WARNING: Do NOT modify this code. The content of this method is always regenerated by the Form Editor.
	 */
	
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jPanelMain = new JPanel();
        jLabelSteamDir = new JLabel();
        jTextFieldSteamDir = new JTextField();
        jButtonLocateSteamDir = new JButton();
        jButtonManualSteamDirSearch = new JButton();
        jPanelList = new JPanel();
        jButtonReloadRedistList = new JButton();
        jButtonRemoveRedistItemsFromDisk = new JButton();
        jScrollPane1 = new JScrollPane();
        jTableRedistList = new JTable();
        jButtonStopSearch = new JButton();
        jLabelCustomFolders = new JLabel();
        jScrollPane2 = new JScrollPane();
        jListCustomFolders = new JList<>();
        jButtonAddCustomFolder = new JButton();
        jButtonRemoveCustomFolder = new JButton();
        jToolBarMain = new JToolBar();
        jSeparatorA = new JToolBar.Separator();
        jButtonToolbarOptions = new JButton();
        jSeparatorB = new JToolBar.Separator();
        jButtonToolbarCheckforupdates = new JButton();
        jButtonToolbarAbout = new JButton();
        jPanelAppVersion = new JPanel();
        jLabelAppVersion = new JLabel();
        jButtonGithub = new JButton();

        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setBackground(new Color(255, 255, 255));
        addWindowStateListener(new WindowStateListener() {
            public void windowStateChanged(WindowEvent evt) {
                formWindowStateChanged(evt);
            }
        });
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent evt) {
                formWindowClosing(evt);
            }
        });

        jPanelMain.setBackground(new Color(255, 255, 255));

        jLabelSteamDir.setFont(new Font("Dialog", 0, 13)); // NOI18N
        jLabelSteamDir.setText("Steam folder :");

        jTextFieldSteamDir.setFont(new Font("DialogInput", 0, 13)); // NOI18N
        jTextFieldSteamDir.addKeyListener(new KeyAdapter() {
            public void keyReleased(KeyEvent evt) {
                jTextFieldSteamDirKeyReleased(evt);
            }
        });

        jButtonLocateSteamDir.setFont(new Font("Dialog", 1, 12)); // NOI18N
        jButtonLocateSteamDir.setText("Auto Scan");
        jButtonLocateSteamDir.setToolTipText("");
        jButtonLocateSteamDir.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                jButtonLocateSteamDirActionPerformed(evt);
            }
        });

        jButtonManualSteamDirSearch.setFont(new Font("Dialog", 1, 11)); // NOI18N
        jButtonManualSteamDirSearch.setText("Browse...");
        jButtonManualSteamDirSearch.setToolTipText("");
        jButtonManualSteamDirSearch.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                jButtonManualSteamDirSearchActionPerformed(evt);
            }
        });

        jPanelList.setBackground(new Color(255, 255, 255));
        jPanelList.setBorder(BorderFactory.createTitledBorder("Redistributable packages found :"));
        jPanelList.setFont(new Font("Dialog", 0, 13)); // NOI18N

        jButtonReloadRedistList.setFont(new Font("Dialog", 1, 12)); // NOI18N
        jButtonReloadRedistList.setIcon(new ImageIcon(getClass().getResource("/fr/tikione/steam/cleaner/gui/icons/famfamfam_btn_search.png"))); // NOI18N
        jButtonReloadRedistList.setText("Search");
        jButtonReloadRedistList.setEnabled(false);
        jButtonReloadRedistList.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                jButtonReloadRedistListActionPerformed(evt);
            }
        });

        jButtonRemoveRedistItemsFromDisk.setFont(new Font("Dialog", 1, 12)); // NOI18N
        jButtonRemoveRedistItemsFromDisk.setForeground(new Color(0, 0, 153));
        jButtonRemoveRedistItemsFromDisk.setIcon(new ImageIcon(getClass().getResource("/fr/tikione/steam/cleaner/gui/icons/famfamfam_btn_clean.png"))); // NOI18N
        jButtonRemoveRedistItemsFromDisk.setText("Remove selected items from disk");
        jButtonRemoveRedistItemsFromDisk.setEnabled(false);
        jButtonRemoveRedistItemsFromDisk.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                jButtonRemoveRedistItemsFromDiskActionPerformed(evt);
            }
        });

        jTableRedistList.setFont(new Font("Tahoma", 0, 12)); // NOI18N
        jTableRedistList.setForeground(new Color(51, 51, 51));
        jTableRedistList.setModel(new DefaultTableModel(
            new Object [][] {
                {},
                {},
                {},
                {}
            },
            new String [] {

            }
        ));
        jTableRedistList.setRowMargin(2);
        jTableRedistList.setSelectionBackground(new Color(255, 255, 153));
        jTableRedistList.setSelectionForeground(new Color(0, 0, 0));
        jTableRedistList.setShowHorizontalLines(false);
        jTableRedistList.setShowVerticalLines(false);
        jTableRedistList.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent evt) {
                jTableRedistListMouseClicked(evt);
            }
        });
        jTableRedistList.addKeyListener(new KeyAdapter() {
            public void keyPressed(KeyEvent evt) {
                jTableRedistListKeyPressed(evt);
            }
        });
        jScrollPane1.setViewportView(jTableRedistList);

        jButtonStopSearch.setFont(new Font("Dialog", 0, 13)); // NOI18N
        jButtonStopSearch.setIcon(new ImageIcon(getClass().getResource("/fr/tikione/steam/cleaner/gui/icons/famfamfam_stop_search.png"))); // NOI18N
        jButtonStopSearch.setFocusable(false);
        jButtonStopSearch.setRequestFocusEnabled(false);
        jButtonStopSearch.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                jButtonStopSearchActionPerformed(evt);
            }
        });

        GroupLayout jPanelListLayout = new GroupLayout(jPanelList);
        jPanelList.setLayout(jPanelListLayout);
        jPanelListLayout.setHorizontalGroup(jPanelListLayout.createParallelGroup(Alignment.LEADING)
            .addGroup(jPanelListLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanelListLayout.createParallelGroup(Alignment.LEADING)
                    .addComponent(jScrollPane1, GroupLayout.DEFAULT_SIZE, 610, Short.MAX_VALUE)
                    .addGroup(jPanelListLayout.createSequentialGroup()
                        .addComponent(jButtonReloadRedistList)
                        .addPreferredGap(ComponentPlacement.RELATED)
                        .addComponent(jButtonStopSearch, GroupLayout.PREFERRED_SIZE, 29, GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(ComponentPlacement.RELATED, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(jButtonRemoveRedistItemsFromDisk)))
                .addContainerGap())
        );
        jPanelListLayout.setVerticalGroup(jPanelListLayout.createParallelGroup(Alignment.LEADING)
            .addGroup(Alignment.TRAILING, jPanelListLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanelListLayout.createParallelGroup(Alignment.CENTER)
                    .addComponent(jButtonReloadRedistList)
                    .addComponent(jButtonRemoveRedistItemsFromDisk)
                    .addComponent(jButtonStopSearch))
                .addPreferredGap(ComponentPlacement.RELATED)
                .addComponent(jScrollPane1, GroupLayout.PREFERRED_SIZE, 141, GroupLayout.PREFERRED_SIZE)
                .addContainerGap(GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jLabelCustomFolders.setFont(new Font("Dialog", 0, 13)); // NOI18N
        jLabelCustomFolders.setText("<html>Custom folders list :</html>");
        jLabelCustomFolders.setVerticalAlignment(SwingConstants.TOP);

        jListCustomFolders.setFont(new Font("DialogInput", 0, 13)); // NOI18N
        jListCustomFolders.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        jListCustomFolders.setRequestFocusEnabled(false);
        jScrollPane2.setViewportView(jListCustomFolders);

        jButtonAddCustomFolder.setFont(new Font("Dialog", 1, 12)); // NOI18N
        jButtonAddCustomFolder.setIcon(new ImageIcon(getClass().getResource("/fr/tikione/steam/cleaner/gui/icons/famfamfam_btn_add_folder.png"))); // NOI18N
        jButtonAddCustomFolder.setText("Add to list...");
        jButtonAddCustomFolder.setMaximumSize(new Dimension(119, 23));
        jButtonAddCustomFolder.setMinimumSize(new Dimension(119, 23));
        jButtonAddCustomFolder.setRequestFocusEnabled(false);
        jButtonAddCustomFolder.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                jButtonAddCustomFolderActionPerformed(evt);
            }
        });

        jButtonRemoveCustomFolder.setFont(new Font("Dialog", 1, 12)); // NOI18N
        jButtonRemoveCustomFolder.setIcon(new ImageIcon(getClass().getResource("/fr/tikione/steam/cleaner/gui/icons/famfamfam_btn_del_folder.png"))); // NOI18N
        jButtonRemoveCustomFolder.setText("Remove from list");
        jButtonRemoveCustomFolder.setMaximumSize(new Dimension(149, 23));
        jButtonRemoveCustomFolder.setMinimumSize(new Dimension(149, 23));
        jButtonRemoveCustomFolder.setRequestFocusEnabled(false);
        jButtonRemoveCustomFolder.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                jButtonRemoveCustomFolderActionPerformed(evt);
            }
        });

        GroupLayout jPanelMainLayout = new GroupLayout(jPanelMain);
        jPanelMain.setLayout(jPanelMainLayout);
        jPanelMainLayout.setHorizontalGroup(jPanelMainLayout.createParallelGroup(Alignment.LEADING)
            .addComponent(jPanelList, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addGroup(jPanelMainLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanelMainLayout.createParallelGroup(Alignment.LEADING)
                    .addComponent(jLabelSteamDir)
                    .addComponent(jLabelCustomFolders, GroupLayout.PREFERRED_SIZE, 104, GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(ComponentPlacement.RELATED)
                .addGroup(jPanelMainLayout.createParallelGroup(Alignment.LEADING, false)
                    .addComponent(jTextFieldSteamDir)
                    .addComponent(jScrollPane2, GroupLayout.DEFAULT_SIZE, 254, Short.MAX_VALUE))
                .addPreferredGap(ComponentPlacement.UNRELATED)
                .addGroup(jPanelMainLayout.createParallelGroup(Alignment.LEADING)
                    .addComponent(jButtonRemoveCustomFolder, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                    .addComponent(jButtonAddCustomFolder, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                    .addGroup(jPanelMainLayout.createSequentialGroup()
                        .addComponent(jButtonManualSteamDirSearch)
                        .addPreferredGap(ComponentPlacement.RELATED)
                        .addComponent(jButtonLocateSteamDir, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                .addGap(36, 36, 36))
        );
        jPanelMainLayout.setVerticalGroup(jPanelMainLayout.createParallelGroup(Alignment.LEADING)
            .addGroup(jPanelMainLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanelMainLayout.createParallelGroup(Alignment.BASELINE)
                    .addComponent(jLabelSteamDir)
                    .addComponent(jTextFieldSteamDir, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                    .addComponent(jButtonManualSteamDirSearch, GroupLayout.PREFERRED_SIZE, 26, GroupLayout.PREFERRED_SIZE)
                    .addComponent(jButtonLocateSteamDir))
                .addGap(15, 15, 15)
                .addGroup(jPanelMainLayout.createParallelGroup(Alignment.LEADING)
                    .addComponent(jLabelCustomFolders, GroupLayout.PREFERRED_SIZE, 81, GroupLayout.PREFERRED_SIZE)
                    .addGroup(jPanelMainLayout.createSequentialGroup()
                        .addComponent(jButtonAddCustomFolder, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(ComponentPlacement.RELATED)
                        .addComponent(jButtonRemoveCustomFolder, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
                    .addComponent(jScrollPane2, GroupLayout.PREFERRED_SIZE, 71, GroupLayout.PREFERRED_SIZE))
                .addGap(18, 22, Short.MAX_VALUE)
                .addComponent(jPanelList, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
        );

        jToolBarMain.setBorder(BorderFactory.createEmptyBorder(1, 1, 1, 1));
        jToolBarMain.setFloatable(false);
        jToolBarMain.setRollover(true);
        jToolBarMain.setAlignmentY(0.5F);
        jToolBarMain.add(jSeparatorA);

        jButtonToolbarOptions.setIcon(new ImageIcon(getClass().getResource("/fr/tikione/steam/cleaner/gui/icons/famfamfam_btn_options.png"))); // NOI18N
        jButtonToolbarOptions.setToolTipText("Options");
        jButtonToolbarOptions.setFocusable(false);
        jButtonToolbarOptions.setHorizontalTextPosition(SwingConstants.CENTER);
        jButtonToolbarOptions.setVerticalTextPosition(SwingConstants.BOTTOM);
        jButtonToolbarOptions.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                jButtonToolbarOptionsActionPerformed(evt);
            }
        });
        jToolBarMain.add(jButtonToolbarOptions);
        jToolBarMain.add(jSeparatorB);

        jButtonToolbarCheckforupdates.setIcon(new ImageIcon(getClass().getResource("/fr/tikione/steam/cleaner/gui/icons/famfamfam_btn_update.png"))); // NOI18N
        jButtonToolbarCheckforupdates.setToolTipText("Check for updates");
        jButtonToolbarCheckforupdates.setFocusable(false);
        jButtonToolbarCheckforupdates.setHorizontalTextPosition(SwingConstants.CENTER);
        jButtonToolbarCheckforupdates.setVerticalTextPosition(SwingConstants.BOTTOM);
        jButtonToolbarCheckforupdates.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                jButtonToolbarCheckforupdatesActionPerformed(evt);
            }
        });
        jToolBarMain.add(jButtonToolbarCheckforupdates);

        jButtonToolbarAbout.setIcon(new ImageIcon(getClass().getResource("/fr/tikione/steam/cleaner/gui/icons/famfamfam_btn_about.png"))); // NOI18N
        jButtonToolbarAbout.setToolTipText("About TikiOne steam Cleaner");
        jButtonToolbarAbout.setFocusable(false);
        jButtonToolbarAbout.setHorizontalTextPosition(SwingConstants.CENTER);
        jButtonToolbarAbout.setVerticalTextPosition(SwingConstants.BOTTOM);
        jButtonToolbarAbout.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                jButtonToolbarAboutActionPerformed(evt);
            }
        });
        jToolBarMain.add(jButtonToolbarAbout);

        jPanelAppVersion.setBackground(new Color(0, 153, 255));
        jPanelAppVersion.setForeground(new Color(255, 255, 255));

        jLabelAppVersion.setFont(new Font("Dialog", 1, 14)); // NOI18N
        jLabelAppVersion.setForeground(new Color(255, 255, 255));
        jLabelAppVersion.setHorizontalAlignment(SwingConstants.CENTER);
        jLabelAppVersion.setText("version here");

        GroupLayout jPanelAppVersionLayout = new GroupLayout(jPanelAppVersion);
        jPanelAppVersion.setLayout(jPanelAppVersionLayout);
        jPanelAppVersionLayout.setHorizontalGroup(jPanelAppVersionLayout.createParallelGroup(Alignment.LEADING)
            .addComponent(jLabelAppVersion, GroupLayout.DEFAULT_SIZE, 119, Short.MAX_VALUE)
        );
        jPanelAppVersionLayout.setVerticalGroup(jPanelAppVersionLayout.createParallelGroup(Alignment.LEADING)
            .addComponent(jLabelAppVersion, GroupLayout.DEFAULT_SIZE, 25, Short.MAX_VALUE)
        );

        jButtonGithub.setIcon(new ImageIcon(getClass().getResource("/fr/tikione/steam/cleaner/gui/icons/social_github.png"))); // NOI18N
        jButtonGithub.setToolTipText("");
        jButtonGithub.setContentAreaFilled(false);
        jButtonGithub.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
        jButtonGithub.setFocusPainted(false);
        jButtonGithub.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                jButtonGithubActionPerformed(evt);
            }
        });

        GroupLayout layout = new GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(layout.createParallelGroup(Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(jToolBarMain, GroupLayout.PREFERRED_SIZE, 93, GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(ComponentPlacement.RELATED, 395, Short.MAX_VALUE)
                .addComponent(jButtonGithub, GroupLayout.PREFERRED_SIZE, 29, GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(ComponentPlacement.RELATED)
                .addComponent(jPanelAppVersion, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
            .addComponent(jPanelMain, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(layout.createParallelGroup(Alignment.LEADING)
            .addGroup(Alignment.TRAILING, layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(Alignment.LEADING)
                    .addComponent(jToolBarMain, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                    .addComponent(jPanelAppVersion, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                    .addComponent(jButtonGithub))
                .addGap(10, 10, 10)
                .addComponent(jPanelMain, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void jTextFieldSteamDirKeyReleased(KeyEvent evt) {//GEN-FIRST:event_jTextFieldSteamDirKeyReleased
    }//GEN-LAST:event_jTextFieldSteamDirKeyReleased

	private void locateSteamDir() {
		try {
			fSteamDir = null;
			List<String> steamPossibleFoldersTbl = config.getPossibleSteamFolders();
			for (String s : steamPossibleFoldersTbl) {
				try {
					File fPossibleSteamDir = new File(s);
					if (fPossibleSteamDir.exists() && fPossibleSteamDir.isDirectory()) {
						jTextFieldSteamDir.setText(fPossibleSteamDir.getAbsolutePath());
						fSteamDir = fPossibleSteamDir;
						break;
					}
				} catch (Exception ex) {
					Log.error(ex);
				}
			}
			if (fSteamDir == null) {
				jTextFieldSteamDir.setText("");
				if (listModel == null || listModel.isEmpty()) {
				}
			}
		} catch (CharConversionException | InfinitiveLoopException ex) {
			Log.error(ex);
		}
	}

    private void jButtonLocateSteamDirActionPerformed(ActionEvent evt) {//GEN-FIRST:event_jButtonLocateSteamDirActionPerformed
		locateSteamDir();
    }//GEN-LAST:event_jButtonLocateSteamDirActionPerformed

    private void jButtonManualSteamDirSearchActionPerformed(ActionEvent evt) {//GEN-FIRST:event_jButtonManualSteamDirSearchActionPerformed
		JFileChooser dialogue = new JFileChooser();
		dialogue.setMultiSelectionEnabled(false);
		dialogue.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		dialogue.setAcceptAllFileFilterUsed(false);         
		if (dialogue.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
		File folder = dialogue.getSelectedFile();
            File q=new File(folder.getAbsolutePath() + "\\steam.exe"); 
            if(!q.exists()) {
                JOptionPane.showMessageDialog(null, translation.getString(Translation.SEC_WMAIN, "errmsg.notsteamdir"), "Oops!",JOptionPane.ERROR_MESSAGE);
                jTextFieldSteamDir.setText("");    
                fSteamDir = new File((""));
            }else {
                jTextFieldSteamDir.setText(folder.getAbsolutePath());
                fSteamDir = new File(jTextFieldSteamDir.getText());                    
                    }
}                    
                        
    }//GEN-LAST:event_jButtonManualSteamDirSearchActionPerformed

    private void jButtonReloadRedistListActionPerformed(ActionEvent evt) {//GEN-FIRST:event_jButtonReloadRedistListActionPerformed
		searchRedistPackagesOnDisk(jButtonReloadRedistList);
		CLOSING_APP = false;
    }//GEN-LAST:event_jButtonReloadRedistListActionPerformed

    private void jButtonRemoveRedistItemsFromDiskActionPerformed(ActionEvent evt) {//GEN-FIRST:event_jButtonRemoveRedistItemsFromDiskActionPerformed
		memorizeUncheckedItemsToConf();
		jButtonRemoveRedistItemsFromDisk.setText(translation.getString(Translation.SEC_WMAIN, "button.removeSelectedItems"));
		List<File> filesDoDel = new ArrayList<>(8);
		List<File> foldersToDel = new ArrayList<>(8);
		for (int row = 0; row < model.getRowCount(); row++) {
			boolean selected = (Boolean) model.getValueAt(row, 0);
			if (selected) {
				String path = (String) model.getValueAt(row, 1);
				if (path.endsWith(File.separator)) {
					foldersToDel.add(new File(path));
				} else {
					filesDoDel.add(new File(path));
				}
			}
		}
		try {
			JDialogDeletionDirect delFrame = new JDialogDeletionDirect(this, true, translation);
			delFrame.setFilesToDelete(filesDoDel, foldersToDel);
			delFrame.setVisible(true);
			jButtonRemoveRedistItemsFromDisk.setEnabled(false);
			model = new RedistTableModel(translation);
			setTableModelUI();
			jPanelList.setBorder(BorderFactory.createTitledBorder(tblRedistLabelDefault));
		} catch (IOException ex) {
			Log.error(ex);
		}
    }//GEN-LAST:event_jButtonRemoveRedistItemsFromDiskActionPerformed

	private void checkForUpdates() {
		new Thread(() -> {
			String latestVersion = UpdateManager.getLatestVersion();
			if (!UpdateManager.IsUpToDate(latestVersion)) {
				setTitle(getTitle() + "   " + translation.getString(Translation.SEC_WMAIN, "title.newVersionMsg")
						.replace("{0}", UpdateManager.normalizeVersionStr(latestVersion)));
			}
		}).start();
	}

	public static boolean isCLOSING_APP() {
		return CLOSING_APP;
	}

	public static void setCLOSING_APP(boolean closingApp) {
		JFrameMain.CLOSING_APP = closingApp;
	}

    private void formWindowClosing(WindowEvent evt) {//GEN-FIRST:event_formWindowClosing
		setCLOSING_APP(true);
		memorizeUncheckedItemsToConf();
		Dimension windowDimension = this.getSize();
		int height = windowDimension.height;
		int width = windowDimension.width;
		config.setUILatestHeight(height);
		config.setUILatestWidth(width);
		if (!jTextFieldSteamDir.getText().isEmpty() && null != fSteamDir) {
			String steamDirToSave = fSteamDir.getAbsolutePath().replaceAll("\\\\", "/");
			if (!steamDirToSave.endsWith("/")) {
				steamDirToSave += "/";
			}
			config.setLatestSteamFolder(steamDirToSave);
		} else {
			config.setLatestSteamFolder("");
		}
		try {
			config.save();
			patternsCfg.save();
			uncheckedItems.save();
			customFolders.save();
		} catch (IOException ex) {
			Log.error(ex);
		}
    }//GEN-LAST:event_formWindowClosing

    private void formWindowStateChanged(WindowEvent evt) {//GEN-FIRST:event_formWindowStateChanged
		config.setUIState(evt.getNewState());
    }//GEN-LAST:event_formWindowStateChanged

    private void jButtonToolbarOptionsActionPerformed(ActionEvent evt) {//GEN-FIRST:event_jButtonToolbarOptionsActionPerformed
		try {
			JDialogOptionsTabs optionsFrame = new JDialogOptionsTabs(this, true, translation);
			optionsFrame.setVisible(true);
		} catch (IOException ex) {
			Log.error(ex);
		}
    }//GEN-LAST:event_jButtonToolbarOptionsActionPerformed

    private void jButtonToolbarCheckforupdatesActionPerformed(ActionEvent evt) {//GEN-FIRST:event_jButtonToolbarCheckforupdatesActionPerformed
		JDialogCheckForUpdates checkForUpdatesFrame = new JDialogCheckForUpdates(this, true, translation);
		checkForUpdatesFrame.setVisible(true);
    }//GEN-LAST:event_jButtonToolbarCheckforupdatesActionPerformed

    private void jButtonToolbarAboutActionPerformed(ActionEvent evt) {//GEN-FIRST:event_jButtonToolbarAboutActionPerformed
		JDialogAbout aboutFrame = new JDialogAbout(this, true, translation);
		aboutFrame.setVisible(true);
    }//GEN-LAST:event_jButtonToolbarAboutActionPerformed

    private void jButtonAddCustomFolderActionPerformed(ActionEvent evt) {//GEN-FIRST:event_jButtonAddCustomFolderActionPerformed
		JFileChooser dialogue = new JFileChooser();
		dialogue.setMultiSelectionEnabled(false);
		dialogue.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		dialogue.setAcceptAllFileFilterUsed(false);
		if (dialogue.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
			File folder = dialogue.getSelectedFile();
			listModel.addElement(folder);
			memorizeCustomFoldersToConf();
		}
    }//GEN-LAST:event_jButtonAddCustomFolderActionPerformed

    private void jButtonRemoveCustomFolderActionPerformed(ActionEvent evt) {//GEN-FIRST:event_jButtonRemoveCustomFolderActionPerformed
		int selIdx = jListCustomFolders.getSelectedIndex();
		if (selIdx > -1) {
			listModel.remove(selIdx);
			memorizeCustomFoldersToConf();
			if (listModel.isEmpty() && fSteamDir != null && !fSteamDir.exists()) {
			}
		}
    }//GEN-LAST:event_jButtonRemoveCustomFolderActionPerformed

    private void jButtonStopSearchActionPerformed(ActionEvent evt) {//GEN-FIRST:event_jButtonStopSearchActionPerformed
		CLOSING_APP = true;
    }//GEN-LAST:event_jButtonStopSearchActionPerformed

    private void jButtonGithubActionPerformed(ActionEvent evt) {//GEN-FIRST:event_jButtonGithubActionPerformed
		UpdateManager.extBrowser("https://github.com/wbsdty331/TikiOne-steam-Cleaner-Plus");
    }//GEN-LAST:event_jButtonGithubActionPerformed

    private void jTableRedistListMouseClicked(MouseEvent evt) {//GEN-FIRST:event_jTableRedistListMouseClicked
        memorizeCustomFoldersToConf();
		recomputeTotalSizeMB();
    }//GEN-LAST:event_jTableRedistListMouseClicked

    private void jTableRedistListKeyPressed(KeyEvent evt) {//GEN-FIRST:event_jTableRedistListKeyPressed
        memorizeCustomFoldersToConf();
		recomputeTotalSizeMB();
    }//GEN-LAST:event_jTableRedistListKeyPressed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private JButton jButtonAddCustomFolder;
    private JButton jButtonGithub;
    private JButton jButtonLocateSteamDir;
    private JButton jButtonManualSteamDirSearch;
    private JButton jButtonReloadRedistList;
    private JButton jButtonRemoveCustomFolder;
    private JButton jButtonRemoveRedistItemsFromDisk;
    private JButton jButtonStopSearch;
    private JButton jButtonToolbarAbout;
    private JButton jButtonToolbarCheckforupdates;
    private JButton jButtonToolbarOptions;
    private JLabel jLabelAppVersion;
    private JLabel jLabelCustomFolders;
    private JLabel jLabelSteamDir;
    private JList<File> jListCustomFolders;
    private JPanel jPanelAppVersion;
    private JPanel jPanelList;
    private JPanel jPanelMain;
    private JScrollPane jScrollPane1;
    private JScrollPane jScrollPane2;
    private JToolBar.Separator jSeparatorA;
    private JToolBar.Separator jSeparatorB;
    private JTable jTableRedistList;
    private JTextField jTextFieldSteamDir;
    private JToolBar jToolBarMain;
    // End of variables declaration//GEN-END:variables
}
