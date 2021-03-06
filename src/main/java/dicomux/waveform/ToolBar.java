package dicomux.waveform;

import static dicomux.Translation.tr;

import java.awt.Component;
import java.awt.Desktop;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.awt.print.PageFormat;
import java.awt.print.Paper;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.io.File;

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

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.edit.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.graphics.xobject.PDPixelMap;
import org.dcm4che2.data.Tag;

import dicomux.Translation;
import dicomux.waveform.WaveformLayout.Format;

class ToolBar extends JToolBar {
	private static final long serialVersionUID = 2827148456926205919L;
	private WaveformPlugin plugin;
	private JLabel displayLabel;
	private JComboBox displayCombo;
	
	private JButton rrButton;
	private JButton qtButton;

	public ToolBar(WaveformPlugin plugin) {
		this.plugin = plugin;
		setFloatable(false);

		addPrintButton();	
		addPdfButton();
		addExportButton();
		addSeparator();
		
		addToolSelection();
		addRemoveMarkersButton();
		addSeparator();
		
		addIntervalButtons();
		addSeparator();
		
		addZoomButtons();
		add(Box.createHorizontalGlue());
		addPrecisionComponents();
		
		if(plugin.getSettings().getBoolean("dicomux.waveform.showFilterList")) {
			addSeparator();
			addFilterComponents();
		}
		
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
					rrButton.setEnabled(true);
					qtButton.setEnabled(true);
				} else if(e.getSource() == horizontalMeasure) {
					horizontalMeasure.setSelected(true);
					plugin.setSelectedTool(Tool.HORIZONTAL_MEASURE);
					rrButton.setEnabled(false);
					qtButton.setEnabled(false);
				} else if(e.getSource() == multipleMarkers) {
					multipleMarkers.setSelected(true);
					plugin.setSelectedTool(Tool.MULTIPLE_MARKERS);				
					rrButton.setEnabled(false);
					qtButton.setEnabled(false);
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
	
	private void addIntervalButtons() {
		rrButton = new JButton("RR");
		rrButton.setToolTipText(tr("wfMarkAsRR"));
		this.add(rrButton);

		qtButton = new JButton("QT");
		qtButton.setToolTipText(tr("wfMarkAsQT"));
		this.add(qtButton);
		
		ActionListener actionListener = new ActionListener() {
			private double qt = -1;
			private double rr = -1;
			
			private Annotation findDuration() {
				Annotation found = null;
				for(Annotation a: plugin.getAnnotations().getMeasures()) {
					if("duration".equals(a.name)) {
						if(found == null)
							found = a;
						else {
							//ambiguity: more than one lead has been marked
							JOptionPane.showMessageDialog(plugin.getContent(), 
									tr("wfMarkIntervalAmbiguityError"), 
									"Dicomux", JOptionPane.ERROR_MESSAGE);
							return null;
						}
					}					
				}
				
				if(found == null) {
					JOptionPane.showMessageDialog(plugin.getContent(), 
							tr("wfMarkNoIntervalFoundError"), 
							"Dicomux", JOptionPane.ERROR_MESSAGE);
				}
				
				return found;
			}
			
			public void actionPerformed(ActionEvent e) {
				Annotation a = findDuration();
				if(a == null)
					return;
				
				if(e.getSource() == rrButton) {
					this.rr = Double.valueOf(a.value);
					int bpm = (int)(60000/rr);
					plugin.getAnnotations().setManualAnnotation("*RR Interval", a.value, a.unit, true);
					plugin.getAnnotations().setManualAnnotation("*RR Interval", String.valueOf(bpm), "bpm", true);
				}
				else if(e.getSource() == qtButton) {
					this.qt = Double.valueOf(a.value);					
					plugin.getAnnotations().setManualAnnotation("*QT Interval", a.value, a.unit, true);
				}			
				
				if(qt > 0 && rr > 0) {
					int qtc = (int)(1000 * (qt/1000)/Math.sqrt(rr/1000));
					plugin.getAnnotations().setManualAnnotation("*QTc Interval", String.valueOf(qtc), a.unit, true);
				}
			}
		};
		
		rrButton.addActionListener(actionListener);
		qtButton.addActionListener(actionListener);
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
	
	private void addExportButton() {
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
	
	private void addPdfButton() {
		JButton pdfButton = new JButton(new ImageIcon(this.getClass().getClassLoader().getResource("images/pdf.png")));
		pdfButton.setToolTipText(tr("wfPdfExport"));
		this.add(pdfButton);
		
		pdfButton.addActionListener(new ActionListener() {			
			public void actionPerformed(ActionEvent e) {				
				
				new Thread(new Runnable() {
					public void run() {
						int margin = 13;
						int headerHeight = 3*margin;
						int annotationsWidth = 180;
						
						try {
							PDDocument document = new PDDocument();
							PDRectangle landscape = new PDRectangle(PDPage.PAGE_SIZE_A4.getHeight(), PDPage.PAGE_SIZE_A4.getWidth());
							PDPage page = new PDPage(landscape);
							document.addPage(page);
							
							PDFont font = PDType1Font.HELVETICA;
							PDFont boldFont = PDType1Font.HELVETICA_BOLD;

							PDPageContentStream contentStream = new PDPageContentStream(document, page);						

							contentStream.beginText();
							contentStream.setFont(font, 10);
							contentStream.moveTextPositionByAmount(margin, landscape.getHeight() - 2*margin);
							contentStream.drawString(tr("print.patientName") 
									+ plugin.getAnnotations().getAnnotation("patient name").value.replace("^", " ")
									+ "        " + tr("print.patientSex")
									+ plugin.getAnnotations().getAnnotation("patient sex").value
									+ "        " + tr("print.birthDate")
									+ plugin.getAnnotations().getAnnotation("birth date").value
									+ "        " + tr("print.patientId")
									+ plugin.getDicomObject().getString(Tag.PatientID));
									
							//study info
							String studyDescription = plugin.getDicomObject().getString(Tag.StudyDescription);
							if(studyDescription == null)
								studyDescription = "";
							if(studyDescription.length() > 80)
								studyDescription = studyDescription.substring(0, 80) + "[...]";
							
							contentStream.moveTextPositionByAmount(0, -margin);						
							contentStream.drawString(tr("print.studyDate")
									+ plugin.getDicomObject().getString(Tag.StudyDate)
									+ "        " + tr("print.studyTime")
									+ plugin.getDicomObject().getString(Tag.StudyTime)
									+ "        " + tr("print.description")
									+ studyDescription);		
							contentStream.endText();

							//annotations
							contentStream.beginText();
							contentStream.setFont(boldFont, 8);
							contentStream.moveTextPositionByAmount(landscape.getWidth()-annotationsWidth, landscape.getHeight() - headerHeight - 2*margin);
							contentStream.drawString(tr("annotation.name"));
							contentStream.moveTextPositionByAmount(80, 0);
							contentStream.drawString(tr("annotation.value"));
							
							contentStream.setFont(font, 8);
							for(Annotation a: plugin.getAnnotations().getAnnotationsFiltered()) {
								contentStream.moveTextPositionByAmount(-80, -margin);
								contentStream.drawString(a.name);
								contentStream.moveTextPositionByAmount(80, 0);
								contentStream.drawString(a.value + " " + a.unit);
							}

							contentStream.moveTextPositionByAmount(-80, -margin);
							contentStream.drawString(tr("wfPrecision").replace(":", "").trim());
							contentStream.moveTextPositionByAmount(80, 0);
							contentStream.drawString(((int)plugin.getSpeed()) + "mm/s, " 
									+ plugin.getAmplitude() + " mm/mV");							
							contentStream.endText();
							
							//resize image to fit page
							BufferedImage img = plugin.createImage();
							double imgRatio = img.getWidth()/(double)img.getHeight();

							PDRectangle mediabox = page.getMediaBox();
							int width = (int)mediabox.getWidth() - 2*margin - annotationsWidth;
							int height = (int)mediabox.getHeight() - 2*margin - headerHeight;							
							if(imgRatio * height > width) {
								height = (int)(width / imgRatio);
							} else {
								width = (int)(imgRatio * height);
							}
															
							int bottom = (int)mediabox.getHeight() - headerHeight - margin - height;
												
							PDPixelMap pixelMap = new PDPixelMap(document, img);
							contentStream.drawXObject(pixelMap, margin, bottom, width, height);
							contentStream.close();

							File tempFile = File.createTempFile("dicomux-", ".pdf");
							document.save(tempFile);
							document.close();
							
							Desktop.getDesktop().open(tempFile);	
						} catch(Exception ex) {
							JOptionPane.showMessageDialog(plugin.getContent(), 
									"Error while exporting:\n" + ex.toString(), 
									"Dicomux", JOptionPane.ERROR_MESSAGE);
							ex.printStackTrace();					
						}					
					}
				}).start();
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
	
	private void addFilterComponents() {	
		JLabel filterLabel = new JLabel(tr("wfFilter"));
		this.add(filterLabel);

		JComboBox filter = new JComboBox(new String[] {"Noop", "HighPass", "LowPass", "Combined", "Smooth", "Smoother"});
		filter.setRenderer(new BasicComboBoxRenderer() {
			public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
				Component c = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
				((BasicComboBoxRenderer)c).setText(Translation.tr("wfFilter" + value));
				return c;
			}
		});
		filter.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				JComboBox cb = (JComboBox) e.getSource();
				plugin.setFilter((String)cb.getSelectedItem());
			}
		});
		filter.setSelectedIndex(0);
		filter.setFocusable(false);
		this.add(filter);
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
