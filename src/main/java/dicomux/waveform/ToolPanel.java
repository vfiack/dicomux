package dicomux.waveform;

import static dicomux.Translation.tr;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.print.PageFormat;
import java.awt.print.Paper;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;

import javax.print.attribute.HashPrintRequestAttributeSet;
import javax.print.attribute.PrintRequestAttributeSet;
import javax.print.attribute.standard.OrientationRequested;
import javax.print.attribute.standard.PageRanges;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.plaf.basic.BasicComboBoxRenderer;

import dicomux.Translation;
import dicomux.waveform.WaveformLayout.Format;
import dicomux.waveform.WaveformPlugin.Orientation;

class ToolPanel extends JPanel {
	private static final long serialVersionUID = 2827148456926205919L;
	private WaveformPlugin plugin;
	private JLabel displayLabel;
	private JComboBox displayCombo;

	public ToolPanel(WaveformPlugin plugin) {
		this.plugin = plugin;

		addPrintButton();
		addMeasureOrientationButton();
		add(new JLabel("            ")); //spacer
		addZoomComponents();
		if(plugin.getNumberOfChannels() == 12)
		{
			addDisplayFormatComponent();
		}
	}

	private void addPrintButton() {
		JButton printButton = new JButton(new ImageIcon(this.getClass().getClassLoader().getResource("images/printer.png")));
		printButton.setToolTipText(tr("wfPrint"));
		this.add(printButton);

		printButton.addActionListener(new ActionListener() {			
			public void actionPerformed(ActionEvent e) {
				final PrinterJob job = PrinterJob.getPrinterJob();
				job.setJobName("ECG");
				PageFormat pf = job.defaultPage();

				Paper paper = new Paper();
				double margin = 12;
				paper.setImageableArea(margin, margin, paper.getWidth() - margin * 2, paper.getHeight() - margin * 2);
				pf.setPaper(paper);

				pf.setOrientation(PageFormat.LANDSCAPE);
				job.setPrintable(plugin, pf);

				PrintRequestAttributeSet attrs = new HashPrintRequestAttributeSet();
				attrs.add(new PageRanges(1));
				attrs.add(OrientationRequested.LANDSCAPE);
				if(!job.printDialog(attrs))
					return;

				final int amplitude = plugin.getAmplitude();
				final float speed = plugin.getSpeed();
				plugin.setAmplitude(WaveformLayout.DEFAULT_AMPLITUDE);
				plugin.setSpeed(WaveformLayout.DEFAULT_SPEED);

				SwingUtilities.invokeLater(new Runnable() {
					public void run() {
						try {
							job.print();
						} catch (PrinterException ex) {
							ex.printStackTrace();
						}
						
						plugin.setAmplitude(amplitude);
						plugin.setSpeed(speed);
					}
				});
			}
		});		
	}

	private void addMeasureOrientationButton() {
		//new ImageIcon(this.getClass().getClassLoader().getResource("images/printer.png")
		String s = plugin.getMeasureBarsOrientation().toString().substring(0, 1);
		final JButton orientationButton = new JButton(s);
		orientationButton.setToolTipText(tr("wfMeasureOrientation"));
		this.add(orientationButton);
		this.add(new JLabel("            ")); //spacer

		orientationButton.addActionListener(new ActionListener() {			
			public void actionPerformed(ActionEvent e) {
				Orientation o = plugin.getMeasureBarsOrientation();
				o = (o == Orientation.VERTICAL) ? Orientation.HORIZONTAL : Orientation.VERTICAL;
				plugin.setMeasureBarsOrientation(o);
				orientationButton.setText(o.toString().substring(0, 1));
			}
		});		
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
