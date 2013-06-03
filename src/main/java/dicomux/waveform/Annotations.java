package dicomux.waveform;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JToggleButton;
import javax.swing.table.AbstractTableModel;

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
		"qrs axis", "pp interval", "p axis", "p duration");
	
	private List<Annotation> annotations;
	private List<Annotation> annotationsFiltered;	
	private String text;	
	
	public Annotations(DicomObject dcm) {
		super(new BorderLayout());
		
		this.annotations = new ArrayList<Annotation>();
		this.annotationsFiltered = new ArrayList<Annotation>();
		
		this.text = "";
		
		readPatientData(dcm);
		readAcquisitionContext(dcm);
		readWaveformAnnotations(dcm);		
		filterAnnotations();
		
		final JTable table = new JTable(new AnnotationTableModel(annotationsFiltered));
		JScrollPane scroll = new JScrollPane(table);
		this.add(scroll, BorderLayout.CENTER);

		final JToggleButton filter = new JToggleButton(Translation.tr("annotations.filter"), true);
		filter.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if(filter.isSelected())
					table.setModel(new AnnotationTableModel(annotationsFiltered));
				else
					table.setModel(new AnnotationTableModel(annotations));
			}
		});
		this.add(filter, BorderLayout.NORTH);
		
		JTextArea area = new JTextArea(text);
		area.setEditable(false);
		area.setWrapStyleWord(true);
		area.setLineWrap(true);
		this.add(area, BorderLayout.SOUTH);
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
	
	public List<Annotation> getAnnotations() {
		return annotations;
	}
	
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
}
