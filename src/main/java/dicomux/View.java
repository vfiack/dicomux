package dicomux;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.text.MessageFormat;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JEditorPane;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.UIManager;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.plaf.basic.BasicButtonUI;
import javax.swing.text.html.HTMLFrameHyperlinkEvent;

/**
 * concrete View for Dicomux
 * @author heidi
 * @author tobi
 *
 */
public class View extends JFrame implements IView {
	private static final long serialVersionUID = -3586989981842552511L;
	
	/**
	 * the path to the last selected folder
	 */
	private static String m_lastSelectedFilePath = null;
	
	/**
	 * contains the tabbed pane which holds all workspaces
	 */
	private JTabbedPane m_tabbedPane;
	
	/**
	 * contains the menu bar of the application
	 */
	private JMenuBar m_menuBar;
	
	/**
	 * a menu which contains all suitable plug-ins for the currently opened DicomObject
	 */
	private JMenu m_pluginMenu;
	
	/**
	 * the model which serves as data source
	 */
	private IModel m_model = new ModelAdapter();
	
	/**
	 * the instance of our dialogs class
	 */
	private StaticDialogs m_dialogs = new StaticDialogs();
	
	/**
	 * the controller which does all the dirty work
	 */
	private IController m_controller = new ControllerAdapter();
	
	/**
	 * The global language setting for the view
	 */
	private ResourceBundle m_languageBundle; 
	
	/**
	 * base name of the language files which are located in etc<br/>
	 * this constant will be used by m_languageBundle
	 */
	private final String m_langBaseName = "language";
	
	/**
	 * path to the language setting file
	 */
	private String m_pathLanguageSetting = System.getProperty("user.dir") + File.separator + "language.setting";

	/**
	 * determins whether there is a refresh of the workspace in progress
	 */
	private boolean m_refreshInProgress = false;
	
	/**
	 * Object for synchronizing the access to m_tabbedPane
	 */
	private final Object m_refreshLock = new Object(); 
	

	public void registerModel(IModel model) {
		m_model = model;
	}
	

	public void notifyView() {
		refreshAllTabs();
	}
	

	public void registerController(IController controller) {
		m_controller = controller;
	}
	

	public int getActiveWorkspaceId() {
		return m_tabbedPane.getSelectedIndex();
	}
	

	public Locale getLanguage() {
		if(m_languageBundle != null)
			return m_languageBundle.getLocale();
		else
			return null;
	}
	
	/**
	 * creates a new view - Don't forget to register a view and a controller!
	 */
	public View() {
		initializeLanguage(getLanguage());
		initializeApplication();
	}
	
	/**
	 * initializes all components of the view
	 */
	private void initializeApplication() {
		// misc initialization
		setTitle("Dicomux");
		setMinimumSize(new Dimension(800, 600));
		setDefaultCloseOperation(EXIT_ON_CLOSE);
		setIconImage(new ImageIcon(this.getClass().getClassLoader().getResource("images/logo.png")).getImage());
		
		// extract own contentPane and set its layout manager
		Container contentPane = getContentPane();
		contentPane.setLayout(new BorderLayout(0, 5));
		
		// create a main menu and add it to the View
		m_menuBar = new JMenuBar();
		setJMenuBar(m_menuBar);
		
		// add menu entries to the main menu
		initializeMenus();
		
		// care about the tabbed pane
		initializeTabbedPane();
		contentPane.add(m_tabbedPane, BorderLayout.CENTER);
		
		// display the frame in the middle of the screen
		pack();
		Point screenCenterPoint = GraphicsEnvironment.getLocalGraphicsEnvironment().getCenterPoint();
		setLocation(new Point (screenCenterPoint.x - getSize().width / 2,
								screenCenterPoint.y - getSize().height / 2));
		setVisible(true);
	}
	
	/**
	 * convenience method - creates the m_tabbedPane and sets a proper ChangeListener
	 */
	private void initializeTabbedPane() {
		m_tabbedPane = new JTabbedPane(JTabbedPane.TOP, JTabbedPane.SCROLL_TAB_LAYOUT);
		m_tabbedPane.addChangeListener(new ChangeListener() {
		
			public void stateChanged(ChangeEvent e) {
				synchronized (m_refreshLock) {
					if (!m_refreshInProgress) {
						// get the index of the selected workspace
						int newWorkspaceIndex = m_tabbedPane.getSelectedIndex();
						
						// tell the controller what happened
						m_controller.setActiveWorkspace(newWorkspaceIndex);
						
						// update the plug-ins menu
						addPluginMenuEntries(m_model.getWorkspace(newWorkspaceIndex).getSuitablePlugins());
					}
				}
			}
		});
	}
	
	/**
	 * convenience method - initializes all language settings by checking the config file
	 * @param loc the language which should be used for setting the language directly<br/>
	 * if loc is null, the language in the file at m_pathLanguageSetting will be used<br/>
	 * if the file at m_pathLanguageSetting doesn't exist, get the system.user setting<br/>
	 * if the language from the file or the system user language is not available, select english
	 */
	private void initializeLanguage(Locale loc) {
		Locale locale = null;
		
		if(loc == null){
			try { // try to load the language setting from file
				BufferedReader br = new BufferedReader(new FileReader(new File(m_pathLanguageSetting)));
				String lang = br.readLine();
				locale = new Locale(lang);
				br.close();
			} catch (IOException e) { // get the language of the system
				locale = new Locale(System.getProperty("user.language"));
			}
			
			// check if the automatically selected language is available - otherwise select english
			if (!getAvailableLanguages().contains(locale.getLanguage()))
				locale = new Locale("en");
		}
		else{ // set language directly
			locale = loc;
		}
		
		// set the global language for all GUI Elements (load the ResourceBundle)
		m_languageBundle = ResourceBundle.getBundle(m_langBaseName, locale);
		// initialize all localization entries for JFileChooser
		initializeJFileChooser();
	}
	
	/**
	 * big and ugly convenience method - initializes the JFileChooser settings
	 */
	public void initializeJFileChooser() {
		UIManager.put("FileChooser.readOnly", Boolean.TRUE);
		UIManager.put("FileChooser.openDialogTitleText", m_languageBundle.getString("openDialogTitleText"));
		UIManager.put("FileChooser.saveDialogTitleText", m_languageBundle.getString("saveDialogTitleText"));
		UIManager.put("FileChooser.saveInLabelText", m_languageBundle.getString("saveInLabelText"));
		UIManager.put("FileChooser.fileNameHeaderText", m_languageBundle.getString("fileNameHeaderText"));
		UIManager.put("FileChooser.fileSizeHeaderText", m_languageBundle.getString("fileSizeHeaderText"));
		UIManager.put("FileChooser.fileTypeHeaderText", m_languageBundle.getString("fileTypeHeaderText"));
		UIManager.put("FileChooser.fileDateHeaderText", m_languageBundle.getString("fileDateHeaderText"));
		UIManager.put("FileChooser.fileAttrHeaderText", m_languageBundle.getString("fileAttrHeaderText"));
		UIManager.put("FileChooser.directoryOpenButtonText", m_languageBundle.getString("directoryOpenButtonText"));
		UIManager.put("FileChooser.directoryOpenButtonToolTipText", m_languageBundle.getString("directoryOpenButtonToolTipText"));
		UIManager.put("FileChooser.acceptAllFileFilterText" , m_languageBundle.getString("acceptAllFileFilterText"));
		UIManager.put("FileChooser.cancelButtonText" , m_languageBundle.getString("cancelButtonText"));
		UIManager.put("FileChooser.cancelButtonToolTipText" , m_languageBundle.getString("cancelButtonToolTipText"));
		UIManager.put("FileChooser.detailsViewButtonAccessibleName" , m_languageBundle.getString("detailsViewButtonAccessibleName"));
		UIManager.put("FileChooser.detailsViewButtonToolTipText" , m_languageBundle.getString("detailsViewButtonToolTipText"));
		UIManager.put("FileChooser.directoryDescriptionText" , m_languageBundle.getString("directoryDescriptionText"));
		UIManager.put("FileChooser.fileDescriptionText" , m_languageBundle.getString("fileDescriptionText"));
		UIManager.put("FileChooser.fileNameLabelText" , m_languageBundle.getString("fileNameLabelText"));
		UIManager.put("FileChooser.filesOfTypeLabelText" , m_languageBundle.getString("filesOfTypeLabelText"));
		UIManager.put("FileChooser.helpButtonText" , m_languageBundle.getString("helpButtonText"));
		UIManager.put("FileChooser.helpButtonToolTipText" , m_languageBundle.getString("helpButtonToolTipText"));
		UIManager.put("FileChooser.homeFolderAccessibleName" , m_languageBundle.getString("homeFolderAccessibleName"));
		UIManager.put("FileChooser.homeFolderToolTipText" , m_languageBundle.getString("homeFolderToolTipText"));
		UIManager.put("FileChooser.listViewButtonAccessibleName" , m_languageBundle.getString("listViewButtonAccessibleName"));
		UIManager.put("FileChooser.listViewButtonToolTipText" , m_languageBundle.getString("listViewButtonToolTipText"));
		UIManager.put("FileChooser.lookInLabelText" , m_languageBundle.getString("lookInLabelText"));
		UIManager.put("FileChooser.newFolderAccessibleName" , m_languageBundle.getString("newFolderAccessibleName"));
		UIManager.put("FileChooser.newFolderErrorText" , m_languageBundle.getString("newFolderErrorText"));
		UIManager.put("FileChooser.newFolderToolTipText" , m_languageBundle.getString("newFolderToolTipText"));
		UIManager.put("FileChooser.openButtonText" , m_languageBundle.getString("openButtonText"));
		UIManager.put("FileChooser.openButtonToolTipText" , m_languageBundle.getString("openButtonToolTipText"));
		UIManager.put("FileChooser.refreshActionLabelText" , m_languageBundle.getString("refreshActionLabelText"));
		UIManager.put("FileChooser.saveButtonText" , m_languageBundle.getString("saveButtonText"));
		UIManager.put("FileChooser.saveButtonToolTipText" , m_languageBundle.getString("saveButtonToolTipText"));
		UIManager.put("FileChooser.updateButtonText" , m_languageBundle.getString("updateButtonText"));
		UIManager.put("FileChooser.updateButtonToolTipText" , m_languageBundle.getString("updateButtonToolTipText"));
		UIManager.put("FileChooser.upFolderAccessibleName" , m_languageBundle.getString("upFolderAccessibleName"));
		UIManager.put("FileChooser.upFolderToolTipText" , m_languageBundle.getString("upFolderToolTipText"));
	}
	
	/**
	 * convenience method - initializes the whole main menu
	 */
	private void initializeMenus() {
		m_menuBar.removeAll();
		addFileMenu();
		addPluginMenu();
		addLanguageMenu();
		addHelpMenu();
	}
	
	/**
	 * a convenience method for adding a file menu to the main menu
	 */
	private void addFileMenu() {
		JMenu menu = new JMenu(m_languageBundle.getString("key_file"));
		JMenuItem tmp = new JMenuItem(m_languageBundle.getString("key_openFile"));
		tmp.addActionListener(new ActionListener() {
		
			public void actionPerformed(ActionEvent e) {
				m_controller.openDicomFileDialog();
			}
		});
		menu.add(tmp);
		tmp = new JMenuItem(m_languageBundle.getString("key_openDir"));
		tmp.addActionListener(new ActionListener() {
		
			public void actionPerformed(ActionEvent e) {
				m_controller.openDicomDirectoryDialog();
			}
		});
		menu.add(tmp);
		menu.addSeparator();
		tmp = new JMenuItem(m_languageBundle.getString("key_closeTab"));
		tmp.addActionListener(new ActionListener() {
		
			public void actionPerformed(ActionEvent e) {
				m_controller.closeWorkspace();
			}
		});
		menu.add(tmp);
		tmp = new JMenuItem(m_languageBundle.getString("key_closeAllTabs"));
		tmp.addActionListener(new ActionListener() {
		
			public void actionPerformed(ActionEvent e) {
				m_controller.closeAllWorkspaces();
			}
		});
		menu.add(tmp);
		menu.addSeparator();
		tmp = new JMenuItem(m_languageBundle.getString("key_exit"));
		tmp.addActionListener(new ActionListener() {
		
			public void actionPerformed(ActionEvent e) {
				m_controller.closeApplication();
			}
		});
		menu.add(tmp);
		m_menuBar.add(menu);
	}
	
	/**
	 * a convenience method for adding a menu for plugin selection to the main menu
	 */
	private void addPluginMenu() {
		m_pluginMenu = new JMenu(m_languageBundle.getString("key_view"));
		addPluginMenuEntries(null);
		m_menuBar.add(m_pluginMenu);
	}
	
	/**
	 * a convenience method for adding all suitable plug-ins of the currently opened workspace to the plug-in menu
	 * @param suitablePlugins
	 */
	private void addPluginMenuEntries(Vector<APlugin> suitablePlugins) {
		if (suitablePlugins == null || suitablePlugins.size() == 0) {
			m_pluginMenu.setEnabled(false);
			m_pluginMenu.removeAll();
		}
		else {
			m_pluginMenu.setEnabled(true);
			m_pluginMenu.removeAll();
			for (final APlugin i: suitablePlugins) {
				JMenuItem tmp = new JMenuItem(i.getName());
				tmp.addActionListener(new ActionListener() {
				
					public void actionPerformed(ActionEvent arg0) {
						m_controller.setActivePlugin(i.getName());
					}
				});
				m_pluginMenu.add(tmp);
			}
		}
	}
	
	/**
	 * a convenience method for adding a menu for language selection to the main menu
	 */
	private void addLanguageMenu() {
		ActionListener langAL = new ActionListener() { // the action listener for all language change actions
		
			public void actionPerformed(ActionEvent arg0) {
				// build new Locale
				Locale newLocale = new Locale(arg0.getActionCommand());
				
				// inform the controller what happened
				m_controller.setLanguage(newLocale);
				
				// reinitialize the view
				initializeLanguage(newLocale);
				initializeMenus();
				refreshAllTabs();
				repaint();
			}
		};
		
		// create a new language menu and add all available languages to it
		JMenu menu = new JMenu(m_languageBundle.getString("key_language"));
		Vector<String> langList = getAvailableLanguages();
		for (String i : langList) {
			JMenuItem tmp = new JMenuItem(i);
			tmp.addActionListener(langAL);
			menu.add(tmp);
		}
		menu.addSeparator();
		JMenuItem tmp = new JMenuItem(m_languageBundle.getString("key_languageNotification"));
		tmp.setEnabled(false);
		menu.add(tmp);
		
		m_menuBar.add(menu);
	}
	
	/**
	 * convenience method for getting a list of all available languages<br/>
	 * This method invokes availableLanguages.settings</br>
	 * If the file is not available, the Vector will be empty.
	 * @return a Vector containing all available languages in the following form ("de", "en", ...)
	 */
	private Vector<String> getAvailableLanguages() {
		URL listFilePath = this.getClass().getClassLoader().getResource("availableLanguages.settings");
		Vector<String> langs = new Vector<String>(2);
		if (listFilePath != null) {
			try {
				InputStreamReader iStream = new InputStreamReader(listFilePath.openStream()); 
				BufferedReader listFile = new BufferedReader(iStream);
				
				String line = listFile.readLine();
				while(line != null) {
					langs.add(line);
					line = listFile.readLine();
				}
				
				listFile.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		return langs;
	}
	

	public void setLanguage(Locale locale) {
		// We try to save the language setting now. We simply want to load it at the next start.
		try {
			FileWriter fw = new FileWriter(m_pathLanguageSetting);
			fw.write(locale.getLanguage());
			fw.close();
		} catch (IOException e) {
			System.err.println("ERROR: Couldn't write language setting to " + m_pathLanguageSetting);
		}
	}
	
	/**
	 * A convenience method for adding a help menu to the main menu.
	 */
	private void addHelpMenu() {
		JMenu menu = new JMenu(m_languageBundle.getString("key_help"));
		JMenuItem tmp = new JMenuItem(m_languageBundle.getString("key_about"));
		tmp.addActionListener(new ActionListener() {
		
			public void actionPerformed(ActionEvent e) {
				m_controller.openAbout();
			}
		});
		menu.add(tmp);
		m_menuBar.add(menu);
	}
	
	/**
	 * A convenience method for fetching new information from the model. 
	 * This is a really expensive action. Be aware of that!
	 */
	private void refreshAllTabs() {
		synchronized (m_refreshLock) {
			// disable the ChangeListener of m_tabbedPane
			m_refreshInProgress = true;
			
			// disable the user to modify m_tabbedPane while processing
			m_tabbedPane.setEnabled(false);
			
			// remove all tabs
			m_tabbedPane.removeAll();
			
			// load everything from the model (really expensive)
			for (int i = 0; i < m_model.getWorkspaceCount(); ++i) {
				TabObject tmp = m_model.getWorkspace(i);
				String name = "";
				
				// create a new tab with a certain content
				switch (tmp.getTabState()) {
				case WELCOME:
					m_tabbedPane.add(m_dialogs.makeWelcomeTab());
					name = m_languageBundle.getString("key_welcome");
					break;
				case FILE_OPEN:
					m_tabbedPane.add(m_dialogs.makeOpenFileTab());
					name = m_languageBundle.getString("key_open");
					break;
				case DIR_OPEN:
					m_tabbedPane.add(m_dialogs.makeOpenDirTab());
					name = m_languageBundle.getString("key_open");
					break;
				case ERROR_OPEN:
					m_tabbedPane.add(m_dialogs.makeErrorOpenTab(tmp.getName()));
					name = m_languageBundle.getString("key_error");
					break;
				case ABOUT:
					m_tabbedPane.add(m_dialogs.makeAboutTab());
					name = m_languageBundle.getString("key_about");
					break;
				case PLUGIN_ACTIVE:
					m_tabbedPane.add(tmp.getContent());
					name = tmp.getName();
					break;
				}
				
				// add a title and a close button to the TabObject
				m_tabbedPane.setTabComponentAt(m_tabbedPane.getTabCount() - 1, new TabTitle(name));
				
				// select the tab if the model wants that to happen and get all supported plug-ins from the TabObject
				if (tmp.isTabActive()) {
					m_tabbedPane.setSelectedIndex(i);
					addPluginMenuEntries(tmp.getSuitablePlugins());
				}
			}
			
			// reactivate the ChangeListener of m_tabbedPane
			m_refreshInProgress = false;
			
			// enable the user to use m_tabbedPane
			m_tabbedPane.setEnabled(true);
		}
	}
	
	/**
	 * This class holds all static dialogs and their convenience functions
	 * @author heidi
	 */
	private class StaticDialogs {
		/**
		 * convenience method for building an welcome tab
		 * @return a JPanel
		 */
		protected JComponent makeWelcomeTab() {
			JPanel content = new JPanel(new BorderLayout(5 , 5), false);
			JPanel contentHead = new JPanel(new BorderLayout(5, 0), false);
			content.add(contentHead, BorderLayout.NORTH);
			
			contentHead.add(makeMessage(m_languageBundle.getString("key_html_welcome")), BorderLayout.NORTH);
			contentHead.add(makeOpenButtons(), BorderLayout.SOUTH);
			
			return content;
		}
		
		/**
		 * convenience method for building an file open dialog tab
		 * @return a JPanel
		 */
		protected JComponent makeOpenFileTab() {
			JPanel content = new JPanel(new BorderLayout(5 , 5), false);
			content.add(makeMessage(m_languageBundle.getString("key_html_openFile")), BorderLayout.NORTH);
			
			JFileChooser filechooser = new JFileChooser(m_lastSelectedFilePath);
			filechooser.setDialogType(JFileChooser.OPEN_DIALOG);
			filechooser.setLocale(m_languageBundle.getLocale());		// Set the current language to the file chooser!
			filechooser.addActionListener(new ActionListener() {
			
				public void actionPerformed(ActionEvent e) {			// declare what to do if the user presses OK / Cancel
					JFileChooser chooser = (JFileChooser) e.getSource();
					if (JFileChooser.APPROVE_SELECTION.equals(e.getActionCommand())) {
						m_lastSelectedFilePath = chooser.getSelectedFile().getAbsolutePath();
						m_controller.openDicomFile(chooser.getSelectedFile().getPath());
					}
					else if (JFileChooser.CANCEL_SELECTION.equals(e.getActionCommand()))
						m_controller.closeWorkspace();
				}
			});
			content.add(filechooser, BorderLayout.CENTER);
			
			return content;
		}
		
		/**
		 * convenience method for building an file open dialog tab
		 * @return a JPanel
		 */
		protected JComponent makeOpenDirTab() {
			JPanel content = new JPanel(new BorderLayout(5 , 5), false);
			content.add(makeMessage(m_languageBundle.getString("key_html_openDir")), BorderLayout.NORTH);
			
			JFileChooser filechooser = new JFileChooser();
			filechooser.setDialogType(JFileChooser.OPEN_DIALOG);
			filechooser.setLocale(m_languageBundle.getLocale());		// Set the current language to the file chooser!
			filechooser.addActionListener(new ActionListener() {
			
				public void actionPerformed(ActionEvent e) {			// declare what to do if the user presses OK / Cancel
					JFileChooser chooser = (JFileChooser) e.getSource();
					if (JFileChooser.APPROVE_SELECTION.equals(e.getActionCommand()))
						m_controller.openDicomDirectory(chooser.getSelectedFile().getPath());
					else if (JFileChooser.CANCEL_SELECTION.equals(e.getActionCommand()))
						m_controller.closeWorkspace();
				}
			});
			content.add(filechooser, BorderLayout.CENTER);
			
			return content;
		}
		
		/**
		 * convenience method for building an error open tab
		 * @return a JPanel
		 */
		protected JComponent makeErrorOpenTab(String msg) {
			JPanel content = new JPanel(new BorderLayout(5 , 5), false);
			JPanel contentHead = new JPanel(new BorderLayout(5, 0), false);
			content.add(contentHead, BorderLayout.NORTH);
			
			contentHead.add(makeMessage(m_languageBundle.getString("key_html_errOpenFile")), BorderLayout.NORTH);
			contentHead.add(makeOpenButtons(), BorderLayout.CENTER);
			contentHead.add(makeMessage("Error code: " + msg), BorderLayout.SOUTH);
			
			return content;
		}
		
		/**
		 * convenience method for building an about tab
		 * @return a JPanel
		 */
		protected JComponent makeAboutTab() {
			JPanel content = new JPanel(new BorderLayout(5 , 5), false);
			content.add(getHTMLPane("key_html_about"), BorderLayout.CENTER);
			
			return content;
		}
		
		/**
		 * convenience method for creating a HTML panel
		 * @param propKey the key from the property files which contains the HTML code we want to render
		 * @return the HTML panel
		 */
		private JEditorPane getHTMLPane(String propKey) {
			JEditorPane content = new JEditorPane("text/html", getParsedHTML(m_languageBundle.getString(propKey)));
			content.setEditable(false);
			content.setBorder(BorderFactory.createLineBorder(Color.BLACK, 0));
			
			if (Desktop.isDesktopSupported()) {		// a link has to be opened in the default web browser
				content.addHyperlinkListener(new HyperlinkListener() {
				
					public void hyperlinkUpdate(HyperlinkEvent e) {
						if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED &&
								!(e instanceof HTMLFrameHyperlinkEvent)) {
							try {
								Desktop.getDesktop().browse(e.getURL().toURI());
							} 
							catch (Throwable t) {
								System.out.println("Could not open web link with the default browser.");
							}
						}
					}
				});
			}
			return content;
		}
		
		/**
		 * convenience method<br/>
		 * searches for all image tags in a HTML String and corrects the file path with the help of the classloader<br/>
		 * e.g. <img src="image.png"> - image.png will be searched by the classloader and the path will be repaired
		 * @param source a string containing HTML
		 * @return parsed HTML code
		 */
		private String getParsedHTML(String source) {
			final String imagePrefix = "<img src=\"";
			final String imageSuffix = "\"";
			String src = source;
			String retVal = new String();
			
			while (!src.isEmpty()) {
				int startIndex = src.indexOf(imagePrefix);	// search for our indicators
				int endIndex = src.indexOf(imageSuffix, startIndex + imagePrefix.length());
				
				if (startIndex >= 0 && endIndex >= 0 && endIndex > startIndex) {	// we found an image in the HTML code
					String item = src.substring(startIndex + imagePrefix.length(), endIndex); // extract item file name
					System.out.println("Parser found item " + item);
					
					String imagePath = "";
					URL filePath = this.getClass().getClassLoader().getResource(item);	// ask classloader for a correct file path
					if (filePath != null) {
						imagePath = MessageFormat.format(imagePrefix + "{0}" + imageSuffix, filePath);
						System.out.println("Classloader found image at " + filePath);
					}
					else
						System.out.println("Classloader couldn't find the file " + item);
					
					retVal += src.substring(0, startIndex) + imagePath;		// write our work to the retVal
					src = src.substring(endIndex + imageSuffix.length());	// trunkate everything before the end of our image
				}
				else { // we found no image in the HTML code
					retVal += src;
					break; // we are done
				}
			}
			
			System.out.println(retVal);
			return retVal;
		}
		/**
		 * convenience method for adding a headline to a static dialog
		 * @param msg the message - this might be HTML
		 * @return a JPanel with the message
		 */
		private JComponent makeMessage(String msg) {
			JPanel retVal = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0),false);
			JLabel filler = new JLabel(msg);
			retVal.add(filler);
			return retVal;
		}
		
		/**
		 * convenience method for adding open buttons to a static dialog
		 * @return a JPanel with open buttons
		 */
		private JComponent makeOpenButtons() {
			JPanel retVal = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0), false);
			JButton tmp = new JButton(m_languageBundle.getString("key_openFile"));
			tmp.setIcon(new ImageIcon(this.getClass().getClassLoader().getResource("images/text-x-generic.png")));
			tmp.addActionListener(new ActionListener() {
			
				public void actionPerformed(ActionEvent e) {
					m_controller.openDicomFileDialog();
				}
			});
			retVal.add(tmp);
			
			tmp = new JButton(m_languageBundle.getString("key_openDir"));
			tmp.setIcon(new ImageIcon(this.getClass().getClassLoader().getResource("images/folder.png")));
			tmp.addActionListener(new ActionListener() {
			
				public void actionPerformed(ActionEvent e) {
					m_controller.openDicomDirectoryDialog();
				}
			});
			retVal.add(tmp);
			
			tmp = new JButton(m_languageBundle.getString("key_exit"));
			tmp.setIcon(new ImageIcon(this.getClass().getClassLoader().getResource("images/system-log-out.png")));
			tmp.addActionListener(new ActionListener() {
			
				public void actionPerformed(ActionEvent e) {
					m_controller.closeApplication();
				}
			});
			retVal.add(tmp);
			
			return retVal;
		}
	}
	
	/**
	 * A convenience class for creating a JPanel with a title and a button, <br/>
	 * which triggers the close action of the currently active workspace<br/>
	 * This might be used for the title of all tabs
	 * 
	 * This class was a part of the Java Tutorial
	 * http://java.sun.com/docs/books/tutorial/uiswing/examples/components/TabComponentsDemoProject/src/components/ButtonTabComponent.java
	 *
	 * Copyright (c) 1995 - 2008 Sun Microsystems, Inc.  All rights reserved.
	 *
	 * Redistribution and use in source and binary forms, with or without
	 * modification, are permitted provided that the following conditions
	 * are met:
	 *
	 *   - Redistributions of source code must retain the above copyright
	 *     notice, this list of conditions and the following disclaimer.
	 *
	 *   - Redistributions in binary form must reproduce the above copyright
	 *     notice, this list of conditions and the following disclaimer in the
	 *     documentation and/or other materials provided with the distribution.
	 *
	 *   - Neither the name of Sun Microsystems nor the names of its
	 *     contributors may be used to endorse or promote products derived
	 *     from this software without specific prior written permission.
	 *
	 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
	 * IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
	 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
	 * PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL THE COPYRIGHT OWNER OR
	 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
	 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
	 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
	 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
	 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
	 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
	 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
	 */
	private class TabTitle extends JPanel {
		private static final long serialVersionUID = 682821987337403501L;
		
		public TabTitle(String name) {
			super(new FlowLayout(FlowLayout.LEFT, 0, 0));
			
			if (name == null) {
				throw new NullPointerException();
			}
			setOpaque(false);
			
			JLabel label = new JLabel(name);
			//add more space between the label and the button
			label.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 5));
			add(label);
			
			//tab button
			JButton button = new TabButton();
			add(button);
			
			//add more space to the top of the component
			setBorder(BorderFactory.createEmptyBorder(2, 0, 0, 0));
		}
		
		private class TabButton extends JButton implements ActionListener {
			private static final long serialVersionUID = 6661492050736259563L;
			private final int buttonSize = 17;
			
			public TabButton() {
				setPreferredSize(new Dimension(buttonSize, buttonSize));
				setToolTipText(m_languageBundle.getString("key_closeTab"));
				setUI(new BasicButtonUI());
				setContentAreaFilled(false);
				setFocusable(false);
				setBorder(BorderFactory.createEtchedBorder());
				setBorderPainted(false);
				setRolloverEnabled(true);
				addActionListener(this);
			}
			
			public void actionPerformed(ActionEvent e) {
				int i = m_tabbedPane.indexOfTabComponent(TabTitle.this);
				if (i != -1) {
					m_tabbedPane.setSelectedIndex(i);
					m_controller.closeWorkspace();
				}
			}
			
			protected void paintComponent(Graphics g) {
				super.paintComponent(g);
				Graphics2D g2 = (Graphics2D) g.create();
				
				if (getModel().isPressed()) {
					g2.translate(1, 1);
				}
				g2.setStroke(new BasicStroke(2));
				g2.setColor(Color.BLACK);
				if (getModel().isRollover()) {
					g2.setColor(Color.LIGHT_GRAY);
				}
				int delta = 6;
				g2.drawLine(delta, delta, getWidth() - delta - 1, getHeight() - delta - 1);
				g2.drawLine(getWidth() - delta - 1, delta, delta, getHeight() - delta - 1);
				g2.dispose();
			}
		}
	}
}
