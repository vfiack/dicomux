package dicomux;

import java.io.File;
import java.io.IOException;

import org.apache.commons.cli.*;

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
			
			// check if we have some files as argument
			if(args.length >0){
				for (String arg : args) {
					File f = new File(arg); 
					// check if it is a file
					if(f.exists() && f.isFile()){
						//check if it is a directory file
						if(arg.toLowerCase().contains("dir")) {
							ctrl.openDicomDirectory(arg);
						}
						else{
							ctrl.openDicomFile(arg);
						}
					}
				}
			}
		} catch (Exception e) {
			System.err.println("Controller instantiation failed!");
			e.printStackTrace();
			System.exit(1);
		}
	}
}
