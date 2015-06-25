package dicomux.waveform.filters;

public class MovingAverageFilter implements Filter {
	private int points;
	private int[] data;

	public MovingAverageFilter(int points) {
		this.points = points;
	}
	
	public void init(int[] values) {
		this.data = new int[values.length];
		System.arraycopy(values, 0, data, 0, values.length);

		for(int i=0;i<data.length;i++) {
			int sum = 0;
			int realPoints = 0;
			for(int p=i-points/2;p<=i+points/2;p++) {
				if(p >= 0 && p < data.length) {
					sum += data[p];
					realPoints++;
				}							
			}

			this.data[i] = sum/realPoints;					
		}
	}

	public int get(int index) {
		return data[index];
	}

	public static void main(String[] args) {
		int[] data = {1, 2, 3, 4, 5, 6, 7, 8, 9};
		
		MovingAverageFilter filter = new MovingAverageFilter(8);
		filter.init(data);
		for(int i=0;i<data.length;i++)
			System.out.println( filter.get(i) );
	}
}
