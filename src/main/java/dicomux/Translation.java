package dicomux;


import java.io.*;
import java.util.*;

import javax.swing.UIManager;

/**
 * Handles translations of messages
 */
public class Translation
{
	private static String lang;
    private static ResourceBundle bundle;
    private static ResourceBundle defaultBundle;
    
    /**
     * Places the correct Locale.
     * Load the properties file
     */
    public static void setLocale(String lang)
    {
    	Translation.lang = lang;
    	Locale.setDefault(new Locale(lang));
		try {
			InputStream is = Translation.class.getClassLoader().getResourceAsStream("language.properties");
			Translation.bundle = new PropertyResourceBundle(is);
			Translation.defaultBundle = Translation.bundle;
		} catch(Exception e) {
			Translation.bundle = null;
			Translation.defaultBundle = null;
		}		
		
		try {
			InputStream is = Translation.class.getClassLoader().getResourceAsStream("language_" + lang + ".properties");
			Translation.bundle = new PropertyResourceBundle(is);
		} catch(Exception e) {
			if(Translation.bundle == null)
				System.err.println("unable to set language");
		}
		
        try {
	        if(Translation.bundle != null)
    	    	Translation.changeUIMessages();            
		} catch(UnsatisfiedLinkError ule) {
			//awt is not available
		}
    }
    
    public static String getLocale() {
    	return lang;
    }
    
    /**
     * Message translaion
     *
     * @param origin string to fetch
     * @return the correct string
     */
    public static String tr(String origin)
    {        
		try {
			return bundle.getString(origin);
		} catch(Exception e) {    
			try {
				return defaultBundle.getString(origin);
			} catch(Exception e2) {    
				return origin;
			}
		}
    }
        
    /**
     * Change the messages for Swing objects
     */
    private static void changeUIMessages()
    throws UnsatisfiedLinkError
	{
    	String[] messages = {
    			"FileChooser.acceptAllFileFilterText",
				"FileChooser.cancelButtonText",
				"FileChooser.cancelButtonToolTipText",
				"FileChooser.detailsViewButtonToolTipText",
				"FileChooser.directoryDescriptionText",
				"FileChooser.fileDescriptionText",
				"FileChooser.fileNameLabelText",
				"FileChooser.filesOfTypeLabelText",
				"FileChooser.helpButtonText",
				"FileChooser.helpButtonToolTipText",
				"FileChooser.homeFolderToolTipText",
				"FileChooser.listViewButtonToolTipText",
				"FileChooser.lookInLabelText",
				"FileChooser.newFolderErrorText",
				"FileChooser.newFolderToolTipText",
				"FileChooser.openButtonText",
				"FileChooser.openButtonToolTipText",
				"FileChooser.saveButtonText",
				"FileChooser.saveButtonToolTipText",
				"FileChooser.updateButtonText",
				"FileChooser.updateButtonToolTipText",
    			"FileChooser.upFolderToolTipText"};
    	
    	for(int i=0;i<messages.length;i++)
    		UIManager.put(messages[i], Translation.tr(messages[i]));
    }    
}

