package dicomux.waveform;

import static dicomux.Translation.tr;

import java.awt.Component;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.awt.print.PageFormat;
import java.awt.print.Paper;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;

import javax.print.attribute.HashPrintRequestAttributeSet;
import javax.print.attribute.PrintRequestAttributeSet;
import javax.print.attribute.standard.OrientationRequested;
import javax.print.attribute.standard.PageRanges;
import javax.swing.Box;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JToggleButton;
import javax.swing.JToolBar;
import javax.swing.plaf.basic.BasicComboBoxRenderer;

import dicomux.Translation;
import dicomux.waveform.WaveformLayout.Format;

class ToolBar extends JToolBar {
	private static final long serialVersionUID = 2827148456926205919L;
	private WaveformPlugin plugin;
	private JLabel displayLabel;
	private JComboBox displayCombo;

	public ToolBar(WaveformPlugin plugin) {
		this.plugin = plugin;
		setFloatable(false);

		addPrintButton();	
		addExportButtons();
		addSeparator();
		addToolSelection();
		addRemoveMarkersButton();
		addSeparator();
		addZoomButtons();
		add(Box.createHorizontalGlue());
		addPrecisionComponents();
		
		if(plugin.getNumberOfChannels() == 12) {
			add(Box.createHorizontalGlue());
			addDisplayFormatComponent();
		}
	}
	
	private void addToolSelection() {
		
		final JToggleButton verticalMeasure = new JToggleButton(new ImageIcon(
				this.getClass().getClassLoader().getResource("images/tools/vertical-measure.png")));
		final JToggleButton horizontalMeasure = new JToggleButton(new ImageIcon(
				this.getClass().getClassLoader().getResource("images/tools/horizontal-measure.png")));
		final JToggleButton multipleMarkers = new JToggleButton(new ImageIcon(
				this.getClass().getClassLoader().getResource("images/tools/multiple-markers.png")));
		
		verticalMeasure.setToolTipText(tr("wfTool" + Tool.VERTICAL_MEASURE));
		horizontalMeasure.setToolTipText(tr("wfTool" + Tool.HORIZONTAL_MEASURE));
		multipleMarkers.setToolTipText(tr("wfTool" + Tool.MULTIPLE_MARKERS));
		
		ActionListener listener = new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				verticalMeasure.setSelected(false);
				horizontalMeasure.setSelected(false);
				multipleMarkers.setSelected(false);
				
				if(e.getSource() == verticalMeasure) {
					verticalMeasure.setSelected(true);
					plugin.setSelectedTool(Tool.VERTICAL_MEASURE);
				} else if(e.getSource() == horizontalMeasure) {
					horizontalMeasure.setSelected(true);
					plugin.setSelectedTool(Tool.HORIZONTAL_MEASURE);
				} else if(e.getSource() == multipleMarkers) {
					multipleMarkers.setSelected(true);
					plugin.setSelectedTool(Tool.MULTIPLE_MARKERS);				
				}
			}
		};
		
		verticalMeasure.addActionListener(listener);
		horizontalMeasure.addActionListener(listener);
		multipleMarkers.addActionListener(listener);
		
		verticalMeasure.setSelected(true);
		add(verticalMeasure);
		add(horizontalMeasure);
		add(multipleMarkers);
	}

	private void addRemoveMarkersButton() {
		JButton cleanButton = new JButton(new ImageIcon(this.getClass().getClassLoader().getResource("images/draw_eraser.png")));
		cleanButton.setToolTipText(tr("wfRemoveMarkers"));
		this.add(cleanButton);
		
		cleanButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				plugin.removeAllMarkers();
			}
		});
	}
	
	private void addZoomButtons() {
		JButton zoomReset = new JButton(new ImageIcon(this.getClass().getClassLoader().getResource("images/zoom_reset.png")));
		zoomReset.setToolTipText(tr("wfZoomReset"));
		this.add(zoomReset);
		
		zoomReset.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				plugin.setZoom(1);
			}
		});
		
		JButton zoomAuto = new JButton(new ImageIcon(this.getClass().getClassLoader().getResource("images/zoom_extend.png")));
		zoomAuto.setToolTipText(tr("wfZoomAuto"));
		this.add(zoomAuto);
		
		zoomAuto.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				plugin.setZoom(WaveformPlugin.AUTO_ZOOM);
			}
		});

	}
	
	private void addExportButtons() {
		JButton copyButton = new JButton(new ImageIcon(this.getClass().getClassLoader().getResource("images/clipboard_sign.png")));
		copyButton.setToolTipText(tr("wfCopyToClipboard"));
		this.add(copyButton);
		
		copyButton.addActionListener(new ActionListener() {			
			public void actionPerformed(ActionEvent e) {				
				BufferedImage img = plugin.createImage();
				ImageTransferable transferable = new ImageTransferable( img );
		        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(transferable, null);				
			}
		});
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

				new Thread(new Runnable() {
					public void run() {
						final int amplitude = plugin.getAmplitude();
						final float speed = plugin.getSpeed();
						final double zoom = plugin.getZoom();
						plugin.setAmplitude(WaveformLayout.DEFAULT_AMPLITUDE);
						plugin.setSpeed(WaveformLayout.DEFAULT_SPEED);
						plugin.setZoom(1);					
						
						try {							
							job.print();
						} catch (PrinterException ex) {
							JOptionPane.showMessageDialog(plugin.getContent(), 
									"Error while printing:\n" + ex.toString(), 
									"Dicomux", JOptionPane.ERROR_MESSAGE);
							ex.printStackTrace();							
						}
						
						plugin.setAmplitude(amplitude);
						plugin.setSpeed(speed);
						plugin.setZoom(zoom);
					}
				}).start();
			}
		});		
	}

	
	
	private void addPrecisionComponents() {	
		JLabel precisionLabel = new JLabel(tr("wfPrecision"));
		this.add(precisionLabel);

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
		//float, doesn't work but this is the idea
		//speed.setSelectedItem(WaveformLayout.DEFAULT_SPEED);
		speed.setSelectedIndex(2);
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
		amplitude.setSelectedItem(WaveformLayout.DEFAULT_AMPLITUDE);
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
