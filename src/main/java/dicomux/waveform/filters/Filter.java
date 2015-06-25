package dicomux.waveform.filters;

public interface Filter {
	public void init(int[] values);
	public int get(int index);
}
