package dicomux.waveform;

public class SampleMarker {
	public enum Type {START, STOP, ANY}
	
	private Tool tool;
	private Type type;
	private int sample;
		
	public SampleMarker(Tool tool, Type type, int sample) {
		super();
		this.tool = tool;
		this.type = type;
		this.sample = sample;
	}
	
	public Tool getTool() {
		return tool;
	}
	
	public void setTool(Tool tool) {
		this.tool = tool;
	}
	
	public Type getType() {
		return type;
	}
	
	public void setType(Type type) {
		this.type = type;
	}
	
	public int getSample() {
		return sample;
	}
	
	public void setSample(int sample) {
		this.sample = sample;
	}
}
