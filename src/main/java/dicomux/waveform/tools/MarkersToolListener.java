package dicomux.waveform.tools;

import java.awt.event.MouseEvent;

import dicomux.waveform.DrawingPanel;
import dicomux.waveform.SampleMarker;
import dicomux.waveform.WaveformPlugin;

public class MarkersToolListener extends ToolMouseAdapter {
	private WaveformPlugin plugin;
	private DrawingPanel parent;
	private String channel;

	private int prevX; //keep position for marker shifting
	
	public MarkersToolListener(WaveformPlugin plugin, DrawingPanel parent, String channel) {
		this.plugin = plugin;
		this.parent = parent;
		this.channel = channel;
	}
	
	public void mouseClicked(MouseEvent e) {
		if(parent.getHightlightedSample() < 0)
			return;
		
		if(e.getButton() == MouseEvent.BUTTON1)
			parent.addBasicMarker(parent.getHightlightedSample());
		else if(e.getButton() == MouseEvent.BUTTON2) {
			parent.removeMarkers();
			plugin.getAnnotations().removeMeasures(null, channel);
		}
		
		parent.repaint();
	}
	
	public void mousePressed(MouseEvent e) {
		prevX = e.getX();
	}
	
	public void mouseReleased(MouseEvent e) {
		prevX = -1;
	}
	
	public void mouseDragged(MouseEvent e) {
		if(e.getX() != prevX && prevX >= 0) {
			int coords = e.getX() - prevX;
			double seconds = coords / parent.getCellWidth() * 0.1;
			int samples = (int)Math.round(plugin.getSamplesPerSecond() * seconds);
			
			parent.shiftMarkers(plugin.getSelectedTool(), SampleMarker.Type.ANY, samples);
			prevX = e.getX();
		}
	}		
}
