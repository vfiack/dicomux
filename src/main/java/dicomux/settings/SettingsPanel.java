package dicomux.settings;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import dicomux.IController;

public class SettingsPanel extends JPanel {
	private IController controller;
	
	private JTextField localAET;
	
	private JTextField remoteAET;
	private JTextField remoteHost;
	private JTextField remotePort;
	
	private JTextField wadoUrl;
	private JButton apply;
	
	public SettingsPanel(IController controller) {
		super(new BorderLayout());
				
		this.controller = controller;
		this.localAET = new JTextField(controller.getSettings().get("dicomux.local.aet"));
		this.remoteAET = new JTextField(controller.getSettings().get("dicomux.pacs.aet")); 
		this.remoteHost = new JTextField(controller.getSettings().get("dicomux.pacs.host")); 
		this.remotePort = new JTextField(controller.getSettings().get("dicomux.pacs.port")); 
		this.wadoUrl = new JTextField(controller.getSettings().get("dicomux.pacs.wado"));
		this.apply = new JButton("Apply");
		
		this.apply.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				SwingUtilities.invokeLater(new Runnable() {
					public void run() {
						applyChanges();	
					}
				});					
			}
		});		
		
		JPanel form = buildForm();
		add(form, BorderLayout.CENTER);
	}
	
	private void setSetting(String key, JTextField field) {
		controller.getSettings().set(key, field.getText());
	}
	
	private void applyChanges() {
		setSetting("dicomux.local.aet", localAET);
		setSetting("dicomux.pacs.aet", remoteAET);
		setSetting("dicomux.pacs.host", remoteHost);
		setSetting("dicomux.pacs.port", remotePort);
		setSetting("dicomux.pacs.wado", wadoUrl);
				
		JOptionPane.showMessageDialog(this, "Settings applied successfully!");
		controller.closeWorkspace();
	}
	
	private JPanel buildForm() {
		JPanel panel = new JPanel(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		Insets noPadding = new Insets(0,0,0,0);
		Insets rightPadding = new Insets(0,0,0,10);
				
		c.gridy = 0;
		c.gridx = 0;
		c.weightx = 0;
		c.insets = noPadding;
		panel.add(new JLabel("Local AET: "), c);

		c.gridx = 1;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.weightx = 1;
		c.insets = rightPadding;
		panel.add(localAET, c);

		c.gridx = 0;
		c.gridy = 1;
		c.fill = GridBagConstraints.NONE;
		c.weightx = 0;
		c.insets = noPadding;
		panel.add(new JLabel("Remote AET: "), c);
		
		c.gridx = 1;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.weightx = 1;
		c.insets = rightPadding;
		panel.add(remoteAET, c);
		
		c.gridx = 2;
		c.fill = GridBagConstraints.NONE;
		c.weightx = 0;
		c.insets = noPadding;
		panel.add(new JLabel("Remote Host: "), c);
		
		c.gridx = 3;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.weightx = 1;
		c.insets = rightPadding;
		panel.add(remoteHost, c);

		c.gridx = 4;
		c.weightx = 0;
		c.insets = noPadding;		
		panel.add(new JLabel("Remote Port: "), c);
		
		c.gridx = 5;
		c.weightx = 1;
		c.insets = rightPadding;
		panel.add(remotePort, c);
		
		c.gridx = 0;
		c.gridy = 2;
		c.weightx = 0;
		c.insets = noPadding;		
		panel.add(new JLabel("Wado URL: "), c);
		
		c.gridx = 1;
		c.gridwidth = 5;
		c.weightx = 1;
		c.insets = rightPadding;
		panel.add(wadoUrl, c);
	
		
		c.gridx = 1;
		c.gridy = 3;
		c.weightx = 0;
		c.gridwidth = 1;
		panel.add(apply, c);	
		return panel;
	}
}
