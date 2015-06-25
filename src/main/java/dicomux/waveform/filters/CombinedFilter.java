package dicomux.waveform.filters;

public class CombinedFilter implements Filter {
	private Filter[] filters;
	private int[] data;
	
	public CombinedFilter(Filter ... filters) {
		this.filters = filters;
	}

	public void init(int[] values) {
		this.data = new int[values.length];
		System.arraycopy(values, 0, data, 0, values.length);
		
		for(Filter f: filters) {			
			f.init(this.data);
			for(int i=0;i<data.length;i++) {
				data[i] = f.get(i);
			}
		}
	}

	public int get(int index) {
		return data[index];
	}

}
