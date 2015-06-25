package dicomux.waveform.filters;

public class LowPassFilter implements Filter {
	private double dt;
	private double rc;
	private int[] data;

	public LowPassFilter(int samplesPerSecond, double freq) {
		this.dt = 1 / (double)samplesPerSecond;
		this.rc = 1 / (2*Math.PI * freq);
	}
	
	public void init(int[] values) {
		this.data = new int[values.length];
		
		double alpha = dt / (rc+dt);
		data[0] = values[0];

		for(int i=1;i<data.length;i++) {			
			this.data[i] = (int)(alpha*values[i] + (1-alpha)*data[i-1]);					
		}
	}

	public int get(int index) {
		return data[index];
	}

	public static void main(String[] args) {
		int[] data = {1, 2, 3, 4, 5, 6, 7, 8, 9};
		
		LowPassFilter filter = new LowPassFilter(1000, 60);
		filter.init(data);
		for(int i=0;i<data.length;i++)
			System.out.println( filter.get(i) );
	}
}
