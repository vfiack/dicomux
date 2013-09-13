package dicomux.waveform;

class Annotation {
	public final String name;
	public final String value;
	public final String unit;
	public final String channel;
	public final String annotationGroup;
	public final boolean important;

	public Annotation(String name, String value) {
		this(name, value, "", "", "");
	}

	public Annotation(String name, String value, String unit, String channel, String annotationGroup) {
		this(name, value, unit, channel, annotationGroup, false);
	}

	public Annotation(String name, String value, String unit, String channel, String annotationGroup, boolean important) {
		this.name = name;
		this.value = value;
		this.unit = unit;
		this.channel = channel;
		this.annotationGroup = annotationGroup;
		this.important = important;
	}

	
	public String toString() {
		return name + ": " + value + " " + unit;
	}	
}