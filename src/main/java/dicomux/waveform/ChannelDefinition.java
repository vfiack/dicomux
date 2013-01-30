package dicomux.waveform;

import org.dcm4che2.data.DicomElement;
import org.dcm4che2.data.DicomObject;
import org.dcm4che2.data.SpecificCharacterSet;
import org.dcm4che2.data.Tag;

import dicomux.DicomException;

class ChannelDefinition {
	public enum ChannelUnit {uV, mV};

	//--
	
	public static ChannelDefinition fromDicom(DicomObject object) throws DicomException {		
		// read ChannelSensitivity used to calculate the real sample value
		// ChannelSensitivity is the unit of each waveform sample
		DicomElement channelSensitivity = object.get(Tag.ChannelSensitivity);
		if(channelSensitivity == null)
			throw new DicomException("Could not read ChannelSensitivity");
		// unfortunately had to go the complicated way and read the value as string
		String tmp_value = channelSensitivity.getValueAsString(new SpecificCharacterSet("UTF-8"), 50);
		double sensitivity = Double.parseDouble(tmp_value);
		
		// read ChannelSensitivityCorrectionFactor used to calculate the real sample value
		// ChannelSensitivityCorrectionFactor is a form of calibration of the values
		DicomElement channelSensitivityCorrection = object.get(Tag.ChannelSensitivityCorrectionFactor);
		if(channelSensitivityCorrection == null)
			throw new DicomException("Could not read ChannelSensitivityCorrectionFactor");
		// and again we are going the long way
		tmp_value = channelSensitivityCorrection.getValueAsString(new SpecificCharacterSet("UTF-8"), 50);
		double help = Double.parseDouble(tmp_value);
		int sensitivityCorrection = (int) help;
		
		// read channel source sequence which contains the name of the channel (lead)
		DicomElement tmpElement =  object.get(Tag.ChannelSourceSequence);
		if(tmpElement == null)
			throw new DicomException("Could not read ChannelSourceSequence");
		// read the DicomObject which contains to get the needed DicomEelements
		DicomObject channelSS =  tmpElement.getDicomObject();
		if(channelSS == null) 
			throw new DicomException("Could not read ChannelSourceSequence DicomObject");
		// read The name of the channel
		DicomElement meaning = channelSS.get(Tag.CodeMeaning);
		if(meaning == null) 
			throw new DicomException("Could not read Code Meaning");
		
		// read the baseline
		double baseline = object.getDouble(Tag.ChannelBaseline);
		
		// read the unit
		DicomElement sensitivityUnitsSequence = object.get(Tag.ChannelSensitivityUnitsSequence);
		DicomObject sensitivityUnit = sensitivityUnitsSequence.getDicomObject();
		String sensitivityUnitValue = sensitivityUnit.getString(Tag.CodeValue);
		ChannelUnit channelUnit = null;
		try {
			channelUnit = ChannelUnit.valueOf(sensitivityUnitValue);
		} catch(Exception e) {
			throw new DicomException("Unsupported sensitivity unit: " + sensitivityUnitValue, e);
		}

		String name = meaning.getValueAsString(new SpecificCharacterSet("UTF-8"), 50);

		return new ChannelDefinition(name, baseline, sensitivity, sensitivityCorrection, channelUnit);
	}
	
	//--
	
	private String name;
	private double baseline;
	private double sensitivity;
	private int sensitivityCorrection;
	private ChannelUnit unit;
	private double minimum_uV;
	private double maximum_uV;
				
	public ChannelDefinition(String name, double baseline,
			double sensitity, int sensitivityCorrection, 
			ChannelUnit unit) {
		this.name = name;
		this.baseline = baseline;
		this.sensitivity = sensitity;
		this.sensitivityCorrection = sensitivityCorrection;
		this.unit = unit;
		this.maximum_uV = 0.0;
		this.minimum_uV = 0.0;
	}
	
	public double getScaling() {
		int unitScaling;		
		if(unit == ChannelUnit.uV)
			unitScaling = 1;
		else if(unit == ChannelUnit.mV)
			unitScaling = 1000;
		else
			throw new IllegalStateException("Unsupported unit in ChannelDefinition");
		
		return sensitivity * sensitivityCorrection * unitScaling;
	}
	
	public String getName() {
		return name;
	}

	public double getBaseline() {
		return baseline;
	}
	
	public double getSensitity() {
		return sensitivity;
	}

	public int getSensitivityCorrection() {
		return sensitivityCorrection;
	}

	public double getMinimum_uV() {
		return minimum_uV;
	}

	public void setMinimum_uV(double minimum) {
		this.minimum_uV = minimum;
	}

	public double getMaximum_uV() {
		return maximum_uV;
	}

	public void setMaximum_uV(double maximum) {
		this.maximum_uV = maximum;
	}

	@Override
	public String toString() {
		return "<" + name + ", " + sensitivity + ", " + sensitivityCorrection + ", " + unit + ">";
	}
}