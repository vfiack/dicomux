package dicomux.waveform;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JToggleButton;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;

import org.dcm4che2.data.DicomElement;
import org.dcm4che2.data.DicomObject;
import org.dcm4che2.data.Tag;

import dicomux.Translation;

public class Annotations extends JPanel {
	private static final List<String> FILTER = Arrays.asList(
		"patient name", "patient sex", "birth date",
		"electrode placement", 
		"rr interval", "pr interval",
		"qrs duration", "qt interval", "qtc interval", 
		"qrs axis", "pp interval", "p axis", "p duration",
		"high-pass filter", "low-pass filter", "notch filter");
	
	private List<Annotation> annotations;
	private List<Annotation> annotationsFiltered;	
	private List<Annotation> measures;
	private String text;	
	
	private JTable annotationTable;
	private JTable measureTable;
	
	public Annotations(DicomObject dcm) {
		super(new BorderLayout());
		
		this.annotations = new ArrayList<Annotation>();
		this.annotationsFiltered = new ArrayList<Annotation>();
		this.measures = new ArrayList<Annotation>();
		this.text = "";
		
		readPatientData(dcm);
		readAcquisitionContext(dcm);
		readFilters(dcm);
		readWaveformAnnotations(dcm);		
		filterAnnotations();
		
		JPanel annotationPanel = new JPanel(new BorderLayout());		
		this.annotationTable = new JTable(new AnnotationTableModel(annotationsFiltered));
		this.annotationTable.setDefaultRenderer(String.class, new AnnotationTableCellRenderer(annotationsFiltered));
		JScrollPane annotationScroll = new JScrollPane(annotationTable);
		annotationPanel.add(annotationScroll, BorderLayout.CENTER);

		final JToggleButton filter = new JToggleButton(Translation.tr("annotations.filter"), true);
		filter.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if(filter.isSelected()) {
					annotationTable.setModel(new AnnotationTableModel(annotationsFiltered));
					annotationTable.setDefaultRenderer(String.class, new AnnotationTableCellRenderer(annotationsFiltered));					
				}
				else {
					annotationTable.setModel(new AnnotationTableModel(annotations));
					annotationTable.setDefaultRenderer(String.class, new AnnotationTableCellRenderer(annotations));
				}
			}
		});
		annotationPanel.add(filter, BorderLayout.NORTH);
		
		this.measureTable = new JTable(new MeasureTableModel(measures));
		this.measureTable.setDefaultRenderer(String.class, new AnnotationTableCellRenderer(measures));
		JScrollPane measureScroll = new JScrollPane(measureTable);
		
		JPanel wrapper = new JPanel(new GridLayout(2, 1));
		wrapper.add(annotationPanel);
		wrapper.add(measureScroll);
				
		this.add(wrapper, BorderLayout.CENTER);
		
		JTextArea area = new JTextArea(text);
		area.setEditable(false);
		area.setWrapStyleWord(true);
		area.setLineWrap(true);
		this.add(area, BorderLayout.SOUTH);
		
		this.setMeasure("cursor time", "-", "", "", false);
		this.setMeasure("cursor value", "-", "", "", false);
	}	
	
	private void readPatientData(DicomObject dcm) {
		annotations.add(new Annotation("Patient Name", dcm.getString(Tag.PatientName)));
		annotations.add(new Annotation("Patient Sex", dcm.getString(Tag.PatientSex)));
		annotations.add(new Annotation("Birth Date", dcm.getString(Tag.PatientBirthDate)));
	}
	
	private void readAcquisitionContext(DicomObject dcm) {
		DicomElement sequence = dcm.get(Tag.AcquisitionContextSequence);
		if(sequence == null)
			return;
		
		for(int i=0;i<sequence.countItems();i++) {
			DicomObject item = sequence.getDicomObject(i);
			
			try {
				String name = item.get(Tag.ConceptNameCodeSequence).getDicomObject(0).getString(Tag.CodeMeaning);
				String value = "";
				if("CODE".equalsIgnoreCase(item.getString(Tag.ValueType))) {					
					value =  item.get(Tag.ConceptCodeSequence).getDicomObject(0).getString(Tag.CodeMeaning);
				} else if("NUMERIC".equalsIgnoreCase(item.getString(Tag.ValueType))) {
					value = item.getString(Tag.NumericValue); 
				}
				
				annotations.add(new Annotation(name, value));
			} catch(Exception e) {
				e.printStackTrace();
				continue;
			}
		}
	}
	
	private void readFilters(DicomObject dcm) {
		DicomElement waveformSequence = dcm.get(Tag.WaveformSequence);
		if(waveformSequence == null || waveformSequence.countItems() < 1)
				return;
		
		double filterLow = -1;
		double filterHigh = -1;
		double notch = -1;
		boolean mixed = false;
		
		DicomObject waveform = waveformSequence.getDicomObject(0);
		DicomElement sequence = waveform.get(Tag.ChannelDefinitionSequence);
		for(int i=0;i<sequence.countItems();i++) {
			DicomObject item = sequence.getDicomObject(i);
			double flf = item.getDouble(Tag.FilterLowFrequency);
			double fhf = item.getDouble(Tag.FilterHighFrequency);
			double nff = item.getDouble(Tag.NotchFilterFrequency);
			
			//first affectation
			if(filterLow < 0)
				filterLow = flf;
			if(filterHigh < 0)
				filterHigh = fhf;
			if(notch < 0)
				notch = nff;
			
			//check for mixed filters in different channels. 
			//This would be strange, but better safe than sorry 
			if(filterLow != flf || filterHigh != fhf || notch != nff) {
				mixed = true;
				break;
			}
		}
				
		if(mixed) {
			annotations.add(new Annotation("Low-Pass Filter", "mixed!"));
			annotations.add(new Annotation("High-Pass Filter", "mixed!"));
			annotations.add(new Annotation("Notch-Pass Filter", "mixed!"));
		} else {
			NumberFormat format = new DecimalFormat("#.## Hz");
			annotations.add(new Annotation("Low-Pass Filter", format.format(filterLow)));
			annotations.add(new Annotation("High-Pass Filter", format.format(filterHigh)));
			annotations.add(new Annotation("Notch Filter", format.format(notch)));
		}

	}
	
	private void readWaveformAnnotations(DicomObject dcm) {
		DicomElement sequence = dcm.get(Tag.WaveformAnnotationSequence);
		if(sequence == null)
			return;
		
		for(int i=0;i<sequence.countItems();i++) {
			DicomObject item = sequence.getDicomObject(i);
			
			try {
				//case 1: unformatted text
				String unformatted = item.getString(Tag.UnformattedTextValue);
				if(unformatted != null) {
					this.text += unformatted + "\n";
					continue;
				}

				//case 2: named value with unit
				if(item.get(Tag.MeasurementUnitsCodeSequence) != null) {
					String name = item.get(Tag.ConceptNameCodeSequence).getDicomObject(0).getString(Tag.CodeMeaning);
					String unit =  item.get(Tag.MeasurementUnitsCodeSequence).getDicomObject(0).getString(Tag.CodeValue);
					String value = item.getString(Tag.NumericValue);
					String channel = item.getString(Tag.ReferencedWaveformChannels);
					String annotationGroup = item.getString(Tag.AnnotationGroupNumber);										
					annotations.add(new Annotation(name, value, unit, channel, annotationGroup));
					
					//special case for RR Interval, we want to compute the bpm
					if("RR Interval".equalsIgnoreCase(name) && "ms".equals(unit)) {
						int bpm = 60000/Integer.valueOf(value);
						annotations.add(new Annotation(name, String.valueOf(bpm), "bpm", channel, "computed"));
					}
					continue;
				} 
				
				//case 3: temporal value
				if("POINT".equals(item.getString(Tag.TemporalRangeType))) {
					String name = item.get(Tag.ConceptNameCodeSequence).getDicomObject(0).getString(Tag.CodeMeaning);
					String unit = item.getString(Tag.TemporalRangeType);
					String value = item.getString(Tag.ReferencedSamplePositions);
					String channel = item.getString(Tag.ReferencedWaveformChannels);
					String annotationGroup = item.getString(Tag.AnnotationGroupNumber);										
					annotations.add(new Annotation(name, value, unit, channel, annotationGroup));
					continue;
				}
			} catch(Exception e) {
				e.printStackTrace();
				continue;
			}
		}
	}
	
	private void filterAnnotations() {
		this.annotationsFiltered.clear();
		for(Annotation a: annotations) {
			if(FILTER.contains(a.name.toLowerCase()))
				annotationsFiltered.add(a);
		}
	}
	
	public Annotation getAnnotation(String name) {
		for(Annotation a: annotations) {
			if(name.equalsIgnoreCase(a.name)) {
				return a;
			}
		}
		
		return new Annotation(name, "");
	}
	
	public String getText() {
		return text;
	}
	
	public List<Annotation> getAnnotations() {
		return annotations;
	}

	public List<Annotation> getAnnotationsFiltered() {
		return annotationsFiltered;
	}
	
	public List<Annotation> getMeasures() {
		return measures;
	}
	
	private void addOrReplaceAnnotation(List<Annotation> list, Annotation annotation) {
		int oldIndex = -1;
		for(int i=0; i<list.size();i ++) {
			Annotation a = list.get(i);
			if(a.annotationGroup.equals(annotation.annotationGroup) 
					&& a.name.equals(annotation.name)
					&& a.channel.equals(annotation.channel)
					&& a.unit.equals(annotation.unit)) {
				oldIndex = i;
				break;
			}
		}
		
		if(oldIndex >= 0) {
			list.add(oldIndex, annotation);
			list.remove(oldIndex+1);
		} else {
			list.add(annotation);
		}
	}
	
	public void setManualAnnotation(String name, String value, String unit, boolean important) {
		Annotation annotation = new Annotation(name, value, unit, "-", "measure", important); 		
		addOrReplaceAnnotation(annotations, annotation);
		addOrReplaceAnnotation(annotationsFiltered, annotation);
		
		((AnnotationTableModel)annotationTable.getModel()).fireTableDataChanged();
	}
	
	public void setMeasure(String name, String channel, String value, String unit, boolean important) {
		Annotation annotation = new Annotation(name, value, unit, channel, "measure", important); 		
		addOrReplaceAnnotation(measures, annotation);
		
		Collections.sort(measures, new Comparator<Annotation>() {
			public int compare(Annotation o1, Annotation o2) {
				int channel = o1.channel.compareTo(o2.channel);
				if(channel != 0)
					return channel;
				
				return o1.name.compareTo(o2.name);
			}
		});
		
		((MeasureTableModel)measureTable.getModel()).fireTableDataChanged();
	}
	
	public boolean removeMeasures(String name, String channel) {
		boolean removed = false;
		for(int i=0; i<measures.size();i++) {
			Annotation a = measures.get(i);
			if(a.channel.equals(channel) && (name == null || a.name.equals(name))) {
				measures.remove(i);
				((MeasureTableModel)measureTable.getModel()).fireTableDataChanged();
				i--;
				removed = true;
			}
		}
		
		return removed;
	}
	
	
	//--
	
	class AnnotationTableModel extends AbstractTableModel {
		private final String[] COLUMNS = new String[] {"name", "value"};
		private List<Annotation> data;
		
		public AnnotationTableModel(List<Annotation> data) {
			this.data = data;
		}
		
		@Override
		public String getColumnName(int column) {
			return Translation.tr("annotation." + COLUMNS[column]);
		}

		@Override
		public Class<?> getColumnClass(int columnIndex) {
			return String.class;
		}

		public int getRowCount() {
			return data.size();
		}

		public int getColumnCount() {
			return COLUMNS.length;
		}

		public Object getValueAt(int rowIndex, int columnIndex) {
			Annotation a = data.get(rowIndex);
			if(columnIndex == 0)
				return a.name;
			if(columnIndex == 1)
				return a.value + " " + a.unit;

			return null;
		}
	}
	
	//--
	
	class MeasureTableModel extends AbstractTableModel {
		private final String[] COLUMNS = new String[] {"channel", "name", "value"};
		private List<Annotation> data;
		
		public MeasureTableModel(List<Annotation> data) {
			this.data = data;
		}
		
		@Override
		public void fireTableDataChanged() {		
			super.fireTableDataChanged();
		}
		
		@Override
		public String getColumnName(int column) {
			return Translation.tr("measure." + COLUMNS[column]);
		}

		@Override
		public Class<?> getColumnClass(int columnIndex) {
			return String.class;
		}

		public int getRowCount() {
			return data.size();
		}

		public int getColumnCount() {
			return COLUMNS.length;
		}

		public Object getValueAt(int rowIndex, int columnIndex) {
			Annotation a = data.get(rowIndex);
			if(columnIndex == 0)
				return a.channel;
			if(columnIndex == 1)
				return a.name;
			if(columnIndex == 2)
				return a.value + " " + a.unit;

			return null;
		}
	}
	
	//--
	
	class AnnotationTableCellRenderer extends DefaultTableCellRenderer {
		private List<Annotation> data;
		
		public AnnotationTableCellRenderer(List<Annotation> data) {
			this.data = data;
		}

		@Override
		public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
			JLabel label = (JLabel)super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
			if(data.get(row).important)
				label.setFont(label.getFont().deriveFont(Font.BOLD));
			return label;
		}	
	}
}
