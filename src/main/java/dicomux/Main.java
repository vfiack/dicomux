package dicomux;

import java.io.IOException;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import dicomux.settings.Settings;

/**
 * Launches Dicomux by determining, which model, view and controller shall be used.
 * @author heidi
 *
 */
public class Main {
	public static void main(String[] args) {
		Options options = new Options();
		options.addOption("appkey", true, "AppKey intrahus");
		options.addOption("patientId", true, "Patient ID for pacs auto search");
		CommandLineParser parser = new BasicParser();
		CommandLine cmdline = null;
		
		try {
			cmdline = parser.parse(options, args);
		} catch (ParseException e) {
			System.err.println(e.getMessage());
			HelpFormatter formatter = new HelpFormatter();
			formatter.printHelp("dicomux", options);
			System.exit(1);
		}
		
		
		Settings settings = null;
		try {
			settings = new Settings();
			settings.set("intrahus.appkey", cmdline.getOptionValue("appkey"), false);
		} catch (IOException e) {
			System.err.println("Config instantiation failed!");
			e.printStackTrace();
			System.exit(1);
		}
		
		Translation.setLocale(settings.get("dicomux.lang"));
		
		// create model and view
		IView view = new View();
		IModel model = new Model(view);
		
		// register the model on the view
		view.registerModel(model);
		
		// create a controller and register him on the view
		Controller ctrl;
		try {
			ctrl = new Controller(settings, model, view);
			view.registerController(ctrl);
						
			if(cmdline.getOptionValue("patientId") != null) {
				settings.set("pacs.patientId", cmdline.getOptionValue("patientId"), false);
				ctrl.openDicomQueryDialog();
			}

			
		} catch (Exception e) {
			System.err.println("Controller instantiation failed!");
			e.printStackTrace();
			System.exit(1);
		}
	}
}
