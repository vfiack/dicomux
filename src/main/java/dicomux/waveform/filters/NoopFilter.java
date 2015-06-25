package dicomux.waveform.filters;

public class NoopFilter implements Filter {
	private int[] data;

	public void init(int[] values) {
		this.data = new int[values.length];
		System.arraycopy(values, 0, data, 0, values.length);
	}

	public int get(int index) {
		return data[index];
	}

}
