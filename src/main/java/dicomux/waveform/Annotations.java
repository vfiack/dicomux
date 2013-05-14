package dicomux.waveform;

import java.awt.BorderLayout;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;

import org.dcm4che2.data.DicomElement;
import org.dcm4che2.data.DicomObject;
import org.dcm4che2.data.Tag;

public class Annotations extends JPanel {
	private List<Annotation> annotations;
	private String text;
	
	
	public Annotations(DicomObject dcm) {
		super(new BorderLayout());
		
		this.annotations = new ArrayList<Annotations.Annotation>();
		this.text = "";
		
		readAcquisitionContext(dcm);
		readWaveformAnnotations(dcm);
		
		this.add(new JLabel(text), BorderLayout.NORTH);
		
		JTable table = new JTable(new AnnotationTableModel(annotations));
		JScrollPane scroll = new JScrollPane(table);
		this.add(scroll, BorderLayout.CENTER);
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
					continue;
				} 
				
				//case 3: temporal value
				//name, channel, annot group
				//TemporalRangeType: POINT
				//ReferencedSamplePosition: int
			} catch(Exception e) {
				e.printStackTrace();
				continue;
			}
		}
	}
	
	class Annotation {
		public final String name;
		public final String value;
		public final String unit;
		public final String channel;
		public final String annotationGroup;

		public Annotation(String name, String value) {
			this(name, value, "", "", "Acquisition");
		}

		public Annotation(String name, String value, String unit, String channel, String annotationGroup) {
			this.name = name;
			this.value = value;
			this.unit = unit;
			this.channel = channel;
			this.annotationGroup = annotationGroup;
		}

		public String toString() {
			return name + ": " + value + " " + unit;
		}	
	}
	
	class AnnotationTableModel extends AbstractTableModel {
		private final String[] COLUMNS = new String[] {"name", "value", "channel", "group"};
		private List<Annotation> data;
		
		public AnnotationTableModel(List<Annotation> data) {
			this.data = data;
		}
		
		@Override
		public String getColumnName(int column) {
			return COLUMNS[column];
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
			if(columnIndex == 2)
				return a.channel;
			if(columnIndex == 3)
				return a.annotationGroup;

			return null;
		}
		
	}
}
