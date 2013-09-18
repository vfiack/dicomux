package dicomux.query;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import javax.swing.ImageIcon;
import javax.swing.table.AbstractTableModel;

import org.dcm4che2.data.DicomObject;
import org.dcm4che2.data.Tag;
import org.dcm4che2.data.UID;

import dicomux.Translation;

//pour l'icone
//Waveform: SOPClassUID 1.2.840.10008.5.1.4.1.1.9*
//PDF: SOPUID: 1.2.840.10008.5.1.4.1.1.104.1


public class DicomTableModel extends AbstractTableModel {
	private static final String[] KNOWN_WAVEFORM_SOP_CLASSES = {
		UID.TwelveLeadECGWaveformStorage,
		UID.GeneralECGWaveformStorage,
		UID.AmbulatoryECGWaveformStorage};
	
	private final static String[] COLUMNS = {
		"",
		"query.col.date",
		"query.col.time",
		"query.col.patientId",
		"query.col.patientName",
		"query.col.studyId",
		"query.col.description"
	};
	
	private final static int[] COL_TAGS = {
		Tag.SOPClassUID,
		Tag.StudyDate, Tag.StudyTime,
		Tag.PatientID, Tag.PatientName,
		Tag.StudyID, Tag.StudyDescription
	};

	private DateFormat dicomDateFormat = new SimpleDateFormat("yyyyMMdd");
	private DateFormat readableDateFormat = DateFormat.getDateInstance();
	private DateFormat dicomTimeFormat = new SimpleDateFormat("HHmmss");
	private DateFormat readableTimeFormat = DateFormat.getTimeInstance();
	
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
		if(COL_TAGS[columnIndex] == Tag.SOPClassUID)
			return ImageIcon.class;
		
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
		int tag = COL_TAGS[columnIndex];
		String value = data.get(rowIndex).getString(tag);
		
		if(tag == Tag.SOPClassUID) {
			if(contains(KNOWN_WAVEFORM_SOP_CLASSES, value))
				return new ImageIcon(getClass().getClassLoader().getResource("images/types/waveform.png"));
			if(UID.EncapsulatedPDFStorage.equals(value))
				return new ImageIcon(getClass().getClassLoader().getResource("images/types/pdf.png"));
			return new ImageIcon(getClass().getClassLoader().getResource("images/types/unknown.png"));
		}
		
		if(tag == Tag.PatientName)
			return value.replace("^", " ");
		
		if(tag == Tag.StudyDate) {
			try {
				Date d = dicomDateFormat.parse(value);
				return readableDateFormat.format(d);
			} catch(ParseException e) {
				//unable to convert date, ignore
			}
		}
		
		if(tag == Tag.StudyTime) {
			try {
				Date d = dicomTimeFormat.parse(value);
				return readableTimeFormat.format(d);
			} catch(ParseException e) {
				//unable to convert date, ignore
			}
		}
		
		return value;
	}
		
	//--
	
	private boolean contains(String[] array, String value) {
		for(String s: array) {
			if(s.equals(value))
				return true;
		}
		
		return false;
	}
}
