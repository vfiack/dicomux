package dicomux.query;

import java.util.Collections;
import java.util.List;

import javax.swing.table.AbstractTableModel;

import org.dcm4che2.data.DicomObject;
import org.dcm4che2.data.Tag;

import dicomux.Translation;

public class DicomTableModel extends AbstractTableModel {
	private final static String[] COLUMNS = {
		"query.col.date",
		"query.col.time",
		"query.col.patientId",
		"query.col.patientName",
		"query.col.studyId",
		"query.col.description"
	};
	
	private final static int[] COL_TAGS = {
		Tag.StudyDate, Tag.StudyTime,
		Tag.PatientID, Tag.PatientName,
		Tag.StudyID, Tag.StudyDescription
	};
	
	private List<DicomObject> data;
	
	public DicomTableModel() {
		this.data = Collections.emptyList();
	}
	
	public DicomTableModel(List<DicomObject> data) {
		this.data = data;
	}
	
	public DicomObject getObjectAt(int row) {
		return data.get(row);
	}
	
	//--
	
	public Class<?> getColumnClass(int columnIndex) {
		return String.class;
	}

	public int getColumnCount() {
		return COLUMNS.length;
	}

	public String getColumnName(int column) {
		return Translation.tr(COLUMNS[column]);
	}
	
	public int getRowCount() {
		return data.size();
	}
		
	public Object getValueAt(int rowIndex, int columnIndex) {
		return data.get(rowIndex).getString(COL_TAGS[columnIndex]);
	}
		
}
