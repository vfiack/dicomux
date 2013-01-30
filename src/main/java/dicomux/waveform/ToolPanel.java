package dicomux.waveform;

import static dicomux.Translation.tr;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Vector;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

class ToolPanel extends JPanel {
	private static final long serialVersionUID = 2827148456926205919L;
	private WaveformPlugin plugin;
	private JButton zoomOut;
	private JButton zoomIn;
	private JButton zoomFit;
	private JLabel displayLabel;
	private JComboBox displayCombo;
	private Vector<String> displayFormatsStrings;
	
	public ToolPanel(WaveformPlugin plugin) {
		this.plugin = plugin;
		fillVector();
		
		addZoomButtons();
		if(plugin.getNumberOfChannels() == 12)
		{
			addDisplayFormatComponent();
		}
	}
	
	private void fillVector() {
		this.displayFormatsStrings = new Vector<String>();
		this.displayFormatsStrings.add(tr("wfFormatDefault"));
		this.displayFormatsStrings.add(tr("wfFormatTwoParts"));
		this.displayFormatsStrings.add(tr("wfFormatFourParts"));
		this.displayFormatsStrings.add(tr("wfFormatFourPartsPlus"));
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
			
		displayCombo = new JComboBox(displayFormatsStrings);	
		displayCombo.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				JComboBox cb = (JComboBox) e.getSource();
				String choosen = (String) cb.getSelectedItem();
				if(choosen.equals(tr("wfFormatDefault"))) {
					plugin.setDisplayFormat(WaveformPlugin.DEFAULTFORMAT);
				}
				else if(choosen.equals(tr("wfFormatTwoParts"))) {
					plugin.setDisplayFormat(WaveformPlugin.TWOPARTS);
				}
				else if(choosen.equals(tr("wfFormatFourParts"))) {
					plugin.setDisplayFormat(WaveformPlugin.FOURPARTS);
				}
				else if(choosen.equals(tr("wfFormatFourPartsPlus"))) {
					plugin.setDisplayFormat(WaveformPlugin.FOURPARTSPLUS);
				}
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
			fillVector();
			addDisplayFormatComponent();
		}
	}	
}
