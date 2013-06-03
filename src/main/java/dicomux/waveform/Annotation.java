package dicomux.waveform;

class Annotation {
	public final String name;
	public final String value;
	public final String unit;
	public final String channel;
	public final String annotationGroup;

	public Annotation(String name, String value) {
		this(name, value, "", "", "");
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