package dicomux.waveform.filters;

public class HighPassFilter implements Filter {
	private double dt;
	private double rc;
	private int[] data;

	public HighPassFilter(int samplesPerSecond, double freq) {
		this.dt = 1 / (double)samplesPerSecond;
		this.rc = 1 / (2*Math.PI * freq);
	}
	
	public void init(int[] values) {
		this.data = new int[values.length];

		double alpha = rc / (rc+dt);
		data[0] = values[0];

		for(int i=1;i<data.length;i++) {			
			this.data[i] = (int)(alpha*data[i-1] + alpha*(values[i] - values[i-1]));			
		}
	}

	public int get(int index) {
		return data[index];
	}

	public static void main(String[] args) {
		int[] data = {1, 2, 3, 4, 5, 6, 7, 8, 9};
		
		HighPassFilter filter = new HighPassFilter(1000, 0.05);
		filter.init(data);
		for(int i=0;i<data.length;i++)
			System.out.println( filter.get(i) );
	}
}
