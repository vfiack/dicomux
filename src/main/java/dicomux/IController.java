package dicomux;

import java.net.URL;

import dicomux.settings.Settings;

/**
 * all concrete controllers have to implement this interface
 * @author heidi
 *
 */
public interface IController {
	/**
	 * Get the settings
	 */
	public Settings getSettings();
	
	/**
	 * closes the currently active workspace
	 */
	public void closeWorkspace();
	
	/**
	 * closes all workspaces and opens a file open dialog
	 */
	public void closeAllWorkspaces();
	
	/**
	 * Opens the error dialog
	 */
	public void showErrorMessage(String msg);
	
	/**
	 * show the settings
	 */
	public void openSettings();
	
	/**
	 * opens a query form
	 */
	public void openDicomQueryDialog();
	
	/**
	 * opens a file open dialog
	 */
	public void openDicomFileDialog();
	
	/**
	 * opens a directory open dialog
	 */
	public void openDicomDirectoryDialog();
	
	/**
	 * opens the about information
	 */
	public void openAbout();
	
	/**
	 * closes the application
	 */
	public void closeApplication();
	
	/**
	 * opens a dicom file
	 * @param path file path of the dicom file
	 */
	public void openDicomFile(String path);
	
	/**
	 * opens a dicom file from an url (WADO)
	 * @param url the path to the dicom object
	 */
	public void openDicomURL(URL url);	
	
	/**
	 * tells the model, which workspace is currently active<br/>
	 * this call will not trigger a refresh of the view
	 * @param n id of the workspace
	 */
	public void setActiveWorkspace(int n);
	
	/**
	 * opens a dicom directory
	 * @param path
	 */
	public void openDicomDirectory(String path);
	
	/**
	 * informs the model that the language of the application has been changed
	 * @param locale 
	 */
	void setLanguage(String lang);
	
	/**
	 * sets the active plug-in for the active workspace
	 * @param name the name of the plug-in
	 */
	public void setActivePlugin(String name);
	
//	public void selectPlugin(int n);
}
