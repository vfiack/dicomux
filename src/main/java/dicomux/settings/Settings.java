package dicomux.settings;

import java.io.IOException;
import java.util.Properties;
import java.util.prefs.Preferences;

public class Settings {
	private Properties config; //in a file bundled with the jar, default values
	private Preferences prefs; //user preferences, can overload the default config
	
	public Settings() throws IOException {
		this.config = new Properties();
		this.prefs = Preferences.userRoot().node(getClass().getName());
		
		this.config.load(getClass().getClassLoader().getResourceAsStream("settings.properties"));
	}

	public void set(String key, String value) {
		set(key, value, true);
	}
	
	public void set(String key, String value, boolean persistent) {
		if(value == null)
			value = "";
		
		if(persistent)			
			prefs.put(key, value);
		else
			config.setProperty(key, value);
	}
	
	public String get(String key) {
		String defaultVal = config.getProperty(key);
		return prefs.get(key, defaultVal);
	}

	public int getInt(String key) {
		return Integer.valueOf(get(key));
	}
	
	public double getDouble(String key) {
		return Double.valueOf(get(key));
	}
}
