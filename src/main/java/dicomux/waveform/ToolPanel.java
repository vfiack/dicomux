package dicomux.waveform;

import static dicomux.Translation.tr;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.ImageIcon;
import javax.swing.JButton;
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
	private JButton zoomOut;
	private JButton zoomIn;
	private JButton zoomFit;
	private JLabel displayLabel;
	private JComboBox displayCombo;
	
	public ToolPanel(WaveformPlugin plugin) {
		this.plugin = plugin;
		
		addZoomButtons();
		if(plugin.getNumberOfChannels() == 12)
		{
			addDisplayFormatComponent();
		}
	}
		
	private void addZoomButtons() {		
		this.zoomOut = new JButton();
		this.zoomOut.setIcon(new ImageIcon(this.getClass().getClassLoader().getResource("images/zoomOut.png")));
		this.zoomOut.addActionListener(new ActionListener() {
		
			public void actionPerformed(ActionEvent arg0) {
					plugin.decreaseZoomLevel();
			}});
		this.add(this.zoomOut);		
		
		this.zoomIn = new JButton();
		this.zoomIn.setIcon(new ImageIcon(this.getClass().getClassLoader().getResource("images/zoomIn.png")));
		this.zoomIn.addActionListener(new ActionListener() {
		
			public void actionPerformed(ActionEvent arg0) {
				plugin.increaseZoomLevel();
			}});
		this.add(zoomIn);
		
		zoomFit = new JButton();
		zoomFit.setIcon(new ImageIcon(this.getClass().getClassLoader().getResource("images/fitToPage.png")));
		zoomFit.addActionListener(new ActionListener() {
		
			public void actionPerformed(ActionEvent arg0) {
				plugin.resetZoom();				
			}});
		this.add(zoomFit);
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
	
	public void updateLanguage() {
		if(plugin.getNumberOfChannels() == 12)
		{
			this.remove(this.displayLabel);
			this.remove(this.displayCombo);
			addDisplayFormatComponent();
		}
	}	
}
