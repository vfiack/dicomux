package dicomux.waveform.tools;

import java.awt.event.MouseEvent;

import dicomux.waveform.DrawingPanel;
import dicomux.waveform.SampleMarker;
import dicomux.waveform.WaveformPlugin;

public class MeasureToolListener extends ToolMouseAdapter {
	private WaveformPlugin plugin;
	private DrawingPanel parent;
	private String channel;

	public MeasureToolListener(WaveformPlugin plugin, DrawingPanel parent, String channel) {
		this.plugin = plugin;
		this.parent = parent;
		this.channel = channel;
	}
	
	public void mouseDragged(MouseEvent e) {
		mouseMoved(e);
	}
	
	public void mouseMoved(MouseEvent e) {			
		if(parent.getHightlightedSample() < 0)
			return;
		
		if((e.getModifiersEx() & MouseEvent.BUTTON1_DOWN_MASK) == MouseEvent.BUTTON1_DOWN_MASK) {
			parent.setMeasureMarker(parent.getHightlightedSample(), SampleMarker.Type.START);
			parent.repaint();						
		}
		if((e.getModifiersEx() & MouseEvent.BUTTON3_DOWN_MASK) == MouseEvent.BUTTON3_DOWN_MASK) {
			parent.setMeasureMarker(parent.getHightlightedSample(), SampleMarker.Type.STOP);
			parent.repaint();						
		}
	}

	public void mouseClicked(MouseEvent e) {
		if(parent.getHightlightedSample() < 0)
			return;

		if(e.getButton() == MouseEvent.BUTTON1)
			parent.setMeasureMarker(parent.getHightlightedSample(), SampleMarker.Type.START);
		else if(e.getButton() == MouseEvent.BUTTON3)
			parent.setMeasureMarker(parent.getHightlightedSample(), SampleMarker.Type.STOP);
		else if(e.getButton() == MouseEvent.BUTTON2) {
			parent.removeMarkers();
			plugin.getAnnotations().removeMeasures(null, channel);
		}
		
		parent.repaint();
	}
}
