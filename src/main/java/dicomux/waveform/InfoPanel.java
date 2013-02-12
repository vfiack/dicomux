package dicomux.waveform;

import static dicomux.Translation.tr;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.text.DecimalFormat;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

class InfoPanel extends JPanel {
	private static final long serialVersionUID = -470038831713011257L;
	
	// labels for the values
	private JLabel lead;
	private JLabel maximum;
	private JLabel minimum;

	private JLabel currentLabel;
	private JLabel miliVolt;
	private JLabel seconds;
	
	private JLabel startLabel;
	private JLabel startMiliVolt;
	private JLabel startSeconds;
	
	private JLabel stopLabel;
	private JLabel stopMiliVolt;
	private JLabel stopSeconds;
	
	private JLabel selectionLabel;
	private JLabel selectionDifference;
	private JLabel selectionAmplitude;
	private JLabel selectionSeconds;
	
	public InfoPanel() {
		super(new GridLayout(4, 5, 2, 0));
		
		this.lead = new JLabel(" ");
		this.maximum = new JLabel();
		this.minimum = new JLabel();
		
		this.currentLabel = new JLabel();
		this.miliVolt = new JLabel();
		this.seconds = new JLabel();
		
		this.startLabel = new JLabel();
		this.startMiliVolt = new JLabel();
		this.startSeconds = new JLabel();
		
		this.stopLabel = new JLabel();
		this.stopMiliVolt = new JLabel();
		this.stopSeconds = new JLabel();
		
		this.selectionLabel = new JLabel();
		this.selectionDifference = new JLabel();
		this.selectionAmplitude = new JLabel();
		this.selectionSeconds = new JLabel();
		
		this.add(lead);
		this.add(currentLabel);
		this.add(startLabel);
		this.add(stopLabel);
		this.add(selectionLabel);
		
		this.add(minimum);
		this.add(seconds);
		this.add(startSeconds);
		this.add(stopSeconds);
		this.add(selectionSeconds);

		this.add(maximum);
		this.add(miliVolt);
		this.add(startMiliVolt);
		this.add(stopMiliVolt);
		this.add(selectionDifference);
		
		this.add(new JLabel());
		this.add(new JLabel());
		this.add(new JLabel());
		this.add(new JLabel());
		this.add(selectionAmplitude);
	}
	
	public void setLead(String lead) {
		this.lead.setText(lead);
	}
	
	public void setMinMax(double minimum, double maximum) {		
		this.minimum.setText(new DecimalFormat(tr("wfMinimumFormat")).format(minimum));
		this.maximum.setText(new DecimalFormat(tr("wfMaximumFormat")).format(maximum));
	}
	
	public void setCurrentValues(double seconds, double miliVolt) {
		if(seconds < 0) {
			this.currentLabel.setText("");
			this.seconds.setText("");
			this.miliVolt.setText("");			
		} else {
			this.currentLabel.setText(tr("wfPosition"));
			this.seconds.setText(new DecimalFormat(tr("wfTimeFormat")).format(seconds));
			this.miliVolt.setText(new DecimalFormat(tr("wfValueFormat")).format(miliVolt));
		}
	}
	
	public void setStartValues(double seconds, double miliVolt) {
		if(seconds < 0) {
			this.startLabel.setText("");
			this.startSeconds.setText("");
			this.startMiliVolt.setText("");			
		} else {
			this.startLabel.setText(tr("wfStartPos"));
			this.startSeconds.setText(new DecimalFormat(tr("wfTimeFormat")).format(seconds));
			this.startMiliVolt.setText(new DecimalFormat(tr("wfValueFormat")).format(miliVolt));
		}
	}
	
	public void setStopValues(double seconds, double miliVolt) {
		if(seconds < 0) {
			this.stopLabel.setText("");
			this.stopSeconds.setText("");
			this.stopMiliVolt.setText("");			
		} else {
			this.stopLabel.setText(tr("wfStopPos"));
			this.stopSeconds.setText(new DecimalFormat(tr("wfTimeFormat")).format(seconds));
			this.stopMiliVolt.setText(new DecimalFormat(tr("wfValueFormat")).format(miliVolt));
		}
	}
	
	public void setSelectionValues(double seconds, double diff_mV, double amplitude_mV) {
		if(seconds < 0) {
			this.selectionLabel.setText("");
			this.selectionSeconds.setText("");
			this.selectionDifference.setText("");		
			this.selectionAmplitude.setText("");	
		} else {
			this.selectionLabel.setText(tr("wfSelection"));
			this.selectionSeconds.setText(new DecimalFormat(tr("wfTimeFormat")).format(seconds));
			this.selectionDifference.setText(new DecimalFormat(tr("wfValueFormat")).format(diff_mV));
			this.selectionAmplitude.setText(new DecimalFormat(tr("wfValueFormat")).format(amplitude_mV));
		}
	}
	
	//--
	
	public static void main(String[] args) {
		InfoPanel panel = new InfoPanel();
		panel.setMinMax(0, 5);
		panel.setCurrentValues(5, 15);
		panel.setStartValues(3, 12);
		
		
		JFrame frame = new JFrame();
		frame.getContentPane().add(panel, BorderLayout.CENTER);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setSize(400, 100);
		frame.setVisible(true);
	}
}