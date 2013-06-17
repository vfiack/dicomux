package dicomux.waveform;

import static dicomux.Translation.tr;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.plaf.basic.BasicComboBoxRenderer;

import dicomux.Translation;
import dicomux.waveform.WaveformLayout.Format;

class ToolPanel extends JPanel {
	private static final long serialVersionUID = 2827148456926205919L;
	private WaveformPlugin plugin;
	private JLabel displayLabel;
	private JComboBox displayCombo;
	
	public ToolPanel(WaveformPlugin plugin) {
		this.plugin = plugin;
		
		addZoomComponents();
		if(plugin.getNumberOfChannels() == 12)
		{
			addDisplayFormatComponent();
		}
	}
		
	private void addZoomComponents() {	
		JLabel zoomLabel = new JLabel(tr("wfZoom"));
		this.add(zoomLabel);
		
		JComboBox speed = new JComboBox(new Float[] {WaveformLayout.AUTO_SPEED, 12.5f, 25f, 50f, 100f});
		speed.setRenderer(new BasicComboBoxRenderer() {			
			public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
				Component c = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
				
				String text;
				float val = (Float)value;
				if(val == WaveformLayout.AUTO_SPEED)
					text = "auto mm/s";
				else if(val == Math.floor(val))
					text = String.valueOf((int)val) + " mm/s";
				else
					text = String.valueOf(val) + "mm/s";
				
				((BasicComboBoxRenderer)c).setText(text);
				return c;
			}
		});		
		speed.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				JComboBox cb = (JComboBox) e.getSource();
				plugin.setSpeed((Float)cb.getSelectedItem());
			}
		});
		speed.setSelectedItem(WaveformLayout.AUTO_SPEED);
		speed.setFocusable(false);
		this.add(speed);

		
		JComboBox amplitude = new JComboBox(new Integer[] {WaveformLayout.AUTO_AMPLITUDE, 3, 5, 10, 15, 20, 30});
		amplitude.setRenderer(new BasicComboBoxRenderer() {
			public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
				Component c = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
				String text = ((Integer)value == WaveformLayout.AUTO_AMPLITUDE) ? "auto mm/mV" : String.valueOf(value) + " mm/mV"; 
				((BasicComboBoxRenderer)c).setText(text);
				return c;
			}
		});		
		amplitude.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				JComboBox cb = (JComboBox) e.getSource();
				plugin.setAmplitude((Integer)cb.getSelectedItem());
			}
		});
		amplitude.setSelectedItem(WaveformLayout.AUTO_AMPLITUDE);
		amplitude.setFocusable(false);
		this.add(amplitude);
	}
	
	private void addDisplayFormatComponent() {
		displayLabel = new JLabel(tr("wfDisplayFormat"));
		this.add(displayLabel);
			
		displayCombo = new JComboBox(Format.values());
		displayCombo.setRenderer(new BasicComboBoxRenderer() {
			public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
				Component c = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
				((BasicComboBoxRenderer)c).setText(Translation.tr("wfFormat" + value));
				return c;
			}
		});
		
		displayCombo.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				JComboBox cb = (JComboBox) e.getSource();
				plugin.setDisplayFormat((Format) cb.getSelectedItem());
			}
		});
		displayCombo.setFocusable(false);
		this.add(displayCombo);
	}
	
	public void paintComponent( Graphics g ) {
		super.paintComponent(g); 
		
		int width = plugin.getContent().getWidth();
		
	    this.setPreferredSize(new Dimension(width, 35));
	    this.setSize(new Dimension(width, 35));
	    this.setMinimumSize(new Dimension(width, 35));
		this.setMaximumSize(new Dimension(width, 35));

	}
	
	public void selectDisplayFormat(Format format) {
		if(displayCombo != null)
			displayCombo.setSelectedItem(format);
	}
	
	public void updateLanguage() {
		if(plugin.getNumberOfChannels() == 12)
		{
			this.remove(this.displayLabel);
			this.remove(this.displayCombo);
			addDisplayFormatComponent();
		}
	}	
}
