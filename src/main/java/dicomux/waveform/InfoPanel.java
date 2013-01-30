package dicomux.waveform;

import static dicomux.Translation.tr;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.text.DecimalFormat;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;

class InfoPanel extends JPanel {
	private static final long serialVersionUID = -470038831713011257L;
	
	// labels for the values
	private JLabel maximum;
	private JLabel minimum;
	private JLabel miliVolt;
	private JLabel seconds;
	private JLabel lead;
	// labels for the identification of the values
	private JLabel maximumLabel;
	private JLabel minimumLabel;
	private JLabel positionLabel;
	private JLabel secondsLabel;
	
	private JPanel nameMinMaxPanel;
	private JPanel positionPanel;
	
	public InfoPanel() {
		this.maximum = new JLabel();
		this.minimum = new JLabel();
		this.miliVolt = new JLabel();
		this.seconds = new JLabel();
		this.lead = new JLabel(" ");
		this.maximumLabel = new JLabel();
		this.minimumLabel = new JLabel();
		this.positionLabel = new JLabel();
		this.secondsLabel = new JLabel();
		
		this.setLayout(new BoxLayout(this, BoxLayout.LINE_AXIS));
		
		nameMinMaxPanel = new JPanel();
		nameMinMaxPanel.setPreferredSize(new Dimension(150, 70));
		nameMinMaxPanel.setMinimumSize(new Dimension(150, 70));
		nameMinMaxPanel.setMaximumSize(new Dimension(150, 70));
		
		GridBagLayout nameMinMaxlayout = new GridBagLayout();
		nameMinMaxPanel.setLayout(nameMinMaxlayout);
		
		GridBagConstraints c1 = new GridBagConstraints();
		c1.weightx = 0.5;
		c1.gridwidth = 3;
		c1.gridx = 0;
		c1.gridy = 0;
		c1.ipady = 5;
		c1.anchor = GridBagConstraints.LINE_START;
		
		nameMinMaxPanel.add(this.lead, c1);
		
		GridBagConstraints c2 = new GridBagConstraints();
		c2.weightx = 0.5;
		c2.gridwidth = 2;
		c2.gridx = 0;
		c2.gridy = 1;
		c2.ipady = 5;
		c2.anchor = GridBagConstraints.LINE_START;
		
		this.minimumLabel = new JLabel(tr("wfMinimum"));
		nameMinMaxPanel.add(this.minimumLabel, c2);
		
		GridBagConstraints c3 = new GridBagConstraints();
		c3.weightx = 0.5;
		c3.gridx = 1;
		c3.gridy = 1;
		c3.ipady = 5;
		c3.anchor = GridBagConstraints.LINE_END;
		
		nameMinMaxPanel.add(this.minimum, c3);
		
		GridBagConstraints c4 = new GridBagConstraints();
		c4.weightx = 0.5;
		c4.gridwidth = 2;
		c4.gridx = 0;
		c4.gridy = 2;
		c4.ipady = 5;
		c4.anchor = GridBagConstraints.LINE_START;
		
		this.maximumLabel = new JLabel(tr("wfMaximum"));
		nameMinMaxPanel.add(this.maximumLabel, c4);
		
		GridBagConstraints c5 = new GridBagConstraints();
		c5.weightx = 0.5;
		c5.gridx = 1;
		c5.gridy = 2;
		c5.ipady = 5;
		c5.anchor = GridBagConstraints.LINE_END;
		
		nameMinMaxPanel.add(this.maximum, c5);
		
		positionPanel = new JPanel();
		positionPanel.setPreferredSize(new Dimension(200, 70));
		positionPanel.setMinimumSize(new Dimension(200, 70));
		positionPanel.setMaximumSize(new Dimension(200, 70));
		GridBagLayout positionLayout = new GridBagLayout();
		positionPanel.setLayout(positionLayout);
		
		GridBagConstraints c6 = new GridBagConstraints();
		c6.weightx = 0.5;
		c6.gridwidth = 3;
		c6.gridx = 0;
		c6.gridy = 3;
		c6.ipady = 5;
		c6.anchor = GridBagConstraints.LINE_START;
		
		this.positionLabel = new JLabel(tr("wfPosition"));
		positionPanel.add(this.positionLabel, c6);
		
		GridBagConstraints c7 = new GridBagConstraints();
		c7.weightx = 0.5;
		c7.gridx = 0;
		c7.gridy = 4;
		c7.ipady = 5;
		c7.anchor = GridBagConstraints.LINE_START;
		
		JLabel mv_pos = new JLabel("mV:");
		positionPanel.add(mv_pos, c7);
		
		GridBagConstraints c8 = new GridBagConstraints();
		c8.weightx = 0.5;
		c8.gridx = 1;
		c8.gridy = 4;
		c8.ipady = 5;
		c8.anchor = GridBagConstraints.LINE_END;
		
		positionPanel.add(this.miliVolt, c8);
		
		GridBagConstraints c9 = new GridBagConstraints();
		c9.weightx = 0.5;
		c9.gridx = 0;
		c9.gridy = 5;
		c9.ipady = 5;
		c9.anchor = GridBagConstraints.LINE_START;
		
		this.secondsLabel = new JLabel(tr("wfSecond"));
		positionPanel.add(this.secondsLabel, c9);

		GridBagConstraints c10 = new GridBagConstraints();
		c10.weightx = 0.5;
		c10.gridx = 1;
		c10.gridy = 5;
		c10.ipady = 5;
		c10.anchor = GridBagConstraints.LINE_END;
		
		positionPanel.add(this.seconds, c10);
		
		this.add(nameMinMaxPanel);
		this.add(Box.createRigidArea(new Dimension(5,0)));
		this.add(positionPanel);
	}
	
	public void setLead(String lead) {
		this.lead.setText(lead);
	}
	
	public void setMaximum(double maximum) {
		
		DecimalFormat form = new DecimalFormat("####.##");
		this.maximum.setText(form.format(maximum));
	}
	
	public void setMinimum(double minimum) {
		DecimalFormat form = new DecimalFormat("####.##");
		this.minimum.setText(form.format(minimum));
	}
	
	public void setMiliVolt(double miliVolt) {
		DecimalFormat form = new DecimalFormat("####.##");
		this.miliVolt.setText(form.format(miliVolt));
	}
	
	public void setSeconds(double seconds) {
		DecimalFormat form = new DecimalFormat("####.##");
		this.seconds.setText(form.format(seconds));
	}
	
	public void paintComponent( Graphics g ) {
		super.paintComponent(g); 
		
	    nameMinMaxPanel.setPreferredSize(new Dimension(150, 70));
	    nameMinMaxPanel.setMinimumSize(new Dimension(150, 70));
		nameMinMaxPanel.setMaximumSize(new Dimension(150, 70));
		
		positionPanel.setPreferredSize(new Dimension(200, 70));
		positionPanel.setMinimumSize(new Dimension(200, 70));
		positionPanel.setMaximumSize(new Dimension(200, 70));

	}
	
	public void updateLanguage() {
		this.minimumLabel.setText(tr("wfMinimum"));
		this.maximumLabel.setText(tr("wfMaximum"));
		this.positionLabel.setText(tr("wfPosition"));
		this.secondsLabel.setText(tr("wfSecond"));
	}
	
}