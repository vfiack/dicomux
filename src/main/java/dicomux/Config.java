package dicomux;

import java.io.IOException;
import java.util.Properties;

public class Config {
	private Properties properties;
	
	public Config() {
		this.properties = new Properties();
		try {
			this.properties.load(getClass().getClassLoader().getResourceAsStream("config.properties"));
		} catch (IOException e) {
			//XXX graphic alert
			e.printStackTrace();
		}
	}
	
	public String get(String key, String defaultValue) {
		return properties.getProperty(key, defaultValue);
	}
	
	public String get(String key) {
		return get(key, null);
	}

	public int getInt(String key) {
		return Integer.valueOf(get(key));
	}
}
