package dicomux.waveform;




// used to save information about a channel
class ChannelDefinition {
	public enum ChannelUnit {uV, mV};
	
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