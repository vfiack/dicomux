package dicomux.waveform;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionAdapter;
import java.awt.event.MouseMotionListener;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JPanel;

import dicomux.waveform.filters.Filter;
import dicomux.waveform.filters.NoopFilter;
import dicomux.waveform.tools.MarkersToolListener;
import dicomux.waveform.tools.MeasureToolListener;


/**
 * This class handles the drawing of the waveform
 * 
 * @author norbert
 *
 */
public class DrawingPanel extends JPanel {	
	private static final long serialVersionUID = 856943381513072262L;
	
	private WaveformPlugin plugin;
	private int[] data;
	private List<Annotation> annotations;
	private float scalingWidth;
	private ChannelDefinition definition;		
	private int sampleCount;
	private double valueScaling;
	private boolean isRhythm;
	private Filter filter;
	
	//selected positions for measures
	private int highlightedSample;
	private List<SampleMarker> markers;
	private MeasureToolListener measureToolListener;
	private MarkersToolListener markersToolListener;
	
	public DrawingPanel(WaveformPlugin plugin, int[] values, ChannelDefinition definition) {
		this.plugin = plugin;
		this.data = values;
		this.definition = definition;					
		this.sampleCount = data.length;
		this.valueScaling = this.definition.getScaling();
	
		this.highlightedSample = -1;
		this.markers = new ArrayList<SampleMarker>();		
		this.isRhythm = false;
		
		this.filter = new NoopFilter();
		filter.init(values);
	
		setBackground(Color.WHITE);
		
		this.measureToolListener = new MeasureToolListener(plugin, this, definition.getName());
		this.markersToolListener = new MarkersToolListener(plugin, this, definition.getName());
		
		this.addMouseListener(basicMouseListener);
		this.addMouseMotionListener(basicMouseMotionListener);		

		this.addMouseListener(measureToolListener);
		this.addMouseMotionListener(measureToolListener);		
	}
	
	public void setFilter(Filter filter) {
		this.filter = filter;
		filter.init(this.data);
		repaint();		
	}
	
	public void setTimeLength(double length) {
		if(length > plugin.getSeconds())
			length = plugin.getSeconds();
				
		this.sampleCount = (int)(length*plugin.getSamplesPerSecond());
	}
	
	public boolean isRythm() {
		return isRhythm;
	}
	
	public void setRhythm(boolean mode) {
		this.isRhythm = mode;
	}
	
	public void setAnnotations(List<Annotation> annotations) {
		this.annotations = annotations;
	}
	
	public void setHighlightedSample(int sample) {
		if(sample < 0 || sample >= data.length) {
			highlightedSample = -1;
			plugin.getAnnotations().setMeasure("cursor time", "-", "", "", false);
			plugin.getAnnotations().setMeasure("cursor value", "-", "", "", false);
		}
		else { 
			highlightedSample = sample;
			double sec = highlightedSample / (double)plugin.getSamplesPerSecond();
			double uV = data[highlightedSample] * valueScaling;	
			
			DecimalFormat format = new DecimalFormat("##.####;-##.####");
			plugin.getAnnotations().setMeasure("cursor time", "-", format.format(sec*1000), "ms", false);
			plugin.getAnnotations().setMeasure("cursor value", "-", format.format(uV/1000), "mV", false);
		}			
	}
	
	public int getHightlightedSample() {
		return highlightedSample;
	}
	
	public int getSampleCount() {
		return sampleCount;
	}
	
	//-- actions
	
	//puts min & max values when entering a panel
	private MouseListener basicMouseListener = new MouseAdapter() {
		public void mouseEntered(MouseEvent e) {
			setBackground(new Color(255, 255, 215));
			
			DecimalFormat format = new DecimalFormat("##.####;-##.####");
			plugin.getAnnotations().setMeasure("minimum", definition.getName(), format.format(definition.getMinimum_uV()/1000), "mV", false);
			plugin.getAnnotations().setMeasure("maximum", definition.getName(), format.format(definition.getMaximum_uV()/1000), "mV", false);				
		}
		
		public void mouseExited(MouseEvent e) {
			plugin.getAnnotations().removeMeasures("minimum", definition.getName());
			plugin.getAnnotations().removeMeasures("maximum", definition.getName());
			
			setBackground(Color.WHITE);
			setHighlightedSample(-1);
			repaint();
		}
	};
	
	private MouseMotionListener basicMouseMotionListener = new MouseMotionAdapter() {	
		public void mouseDragged(MouseEvent e) {
			mouseMoved(e);
		}
		
		public void mouseMoved(MouseEvent e) {			
			double sampleWidth = getPreferredSize().getWidth() / sampleCount;
			double sample = e.getPoint().getX() / sampleWidth;
			setHighlightedSample((int)Math.round(sample));
			repaint();						
		}
	};
	
	//-- markers  & tools

	public void selectedToolChanged(Tool tool) {
		removeMarkers();

		removeMouseListener(measureToolListener);
		removeMouseMotionListener(measureToolListener);
		removeMouseListener(markersToolListener);
		removeMouseMotionListener(markersToolListener);
		
		if(tool == Tool.MULTIPLE_MARKERS) {
			addMouseListener(markersToolListener);
			addMouseMotionListener(markersToolListener);
		} else {		
			addMouseListener(measureToolListener);
			addMouseMotionListener(measureToolListener);
		}
	}
	
	public void removeMarkers() {
		markers.clear();
		plugin.getAnnotations().removeMeasures(null, definition.getName());
	}	
	
	private void removeMarkers(Tool tool, SampleMarker.Type type) {
		for(int i=0;i<markers.size();i++) {
			SampleMarker marker = markers.get(i);
			if(tool == marker.getTool() && (type == null || type == marker.getType())) {
				markers.remove(i);
				i--;
			}
		}
	}
	
	private SampleMarker getMarker(Tool tool, SampleMarker.Type type) {
		for(SampleMarker marker: markers) {
			if(tool == marker.getTool() && (type == null || type == marker.getType())) {
				return marker;
			}
		}
		
		return null;
	}
	
	public void shiftMarkers(Tool tool, SampleMarker.Type type, int howMuch) {
		for(SampleMarker marker: markers) {
			if(tool == marker.getTool() && (type == null || type == marker.getType())) {
				marker.setSample(marker.getSample() + howMuch);
			}
		}
	}
	
	public void addBasicMarker(int sample) {
		markers.add(new SampleMarker(plugin.getSelectedTool(), SampleMarker.Type.ANY, sample));
	}
	
	public void setMeasureMarker(int sample, SampleMarker.Type type) {
		removeMarkers(plugin.getSelectedTool(), type);

		plugin.getAnnotations().removeMeasures("duration", definition.getName());
		plugin.getAnnotations().removeMeasures("difference", definition.getName());
		plugin.getAnnotations().removeMeasures("amplitude", definition.getName());

		String prefix = (type == SampleMarker.Type.START ? "start" : "stop");
		if(sample < 0 || sample >= data.length) {
			plugin.getAnnotations().removeMeasures(prefix + " time", definition.getName());
			plugin.getAnnotations().removeMeasures(prefix + " value", definition.getName());
		}
		else { 
			double sec = sample / (double)plugin.getSamplesPerSecond();
			double uV = data[sample] * valueScaling;
			markers.add(new SampleMarker(plugin.getSelectedTool(), type, sample));
			
			DecimalFormat format = new DecimalFormat("##.####;-##.####");			
			plugin.getAnnotations().setMeasure(prefix + " value", definition.getName(), format.format(uV/1000), "mV", false);
			if(plugin.getSelectedTool() == Tool.VERTICAL_MEASURE)
				plugin.getAnnotations().setMeasure(prefix + " time", definition.getName(), format.format(sec*1000), "ms", false);

			setSelection();
		}			
	}

	private void setSelection() {
		SampleMarker start = getMarker(plugin.getSelectedTool(), SampleMarker.Type.START);
		SampleMarker stop = getMarker(plugin.getSelectedTool(), SampleMarker.Type.STOP);
		if(start == null || stop == null)
			return;
		
		int startSample = start.getSample();
		int stopSample = stop.getSample();
		
		double time = (stopSample-startSample) / (double)plugin.getSamplesPerSecond();
		double diff_uV = (data[stopSample] - data[startSample]) * valueScaling;
						
		int min = Integer.MAX_VALUE;
		int max = Integer.MIN_VALUE;
		for(int i=startSample;i<=stopSample;i++) {
			if(data[i] > max)
				max = data[i];
			if(data[i] < min)
				min = data[i];
		}
		double amplitude_uV = (max-min) * valueScaling;		
		
		//duration, difference, amplitude
		DecimalFormat format = new DecimalFormat("##.####;-##.####");
		if(plugin.getSelectedTool() == Tool.VERTICAL_MEASURE) {
			plugin.getAnnotations().setMeasure("difference", definition.getName(), format.format(diff_uV/1000), "mV", false);
			plugin.getAnnotations().setMeasure("duration", definition.getName(), format.format(time*1000), "ms", true);
			plugin.getAnnotations().setMeasure("amplitude", definition.getName(), format.format(amplitude_uV/1000), "mV", false);			
		} else if(plugin.getSelectedTool() == Tool.HORIZONTAL_MEASURE) {
			plugin.getAnnotations().setMeasure("difference", definition.getName(), format.format(diff_uV/1000), "mV", true);
		}
	}
		
	
	//-- drawing methods
	
	public void paintComponent(Graphics g) {		
		super.paintComponent(g);
		paintComponent(g, true);
	}
	
	public void printComponent(Graphics g) {		
		super.printComponent(g);
		paintComponent(g, false);
	}
	
	private void paintComponent(Graphics g, boolean clipLeadName) {		
		super.paintComponent(g);
		final Graphics2D g2 = (Graphics2D) g;
				
		// set rendering options
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);    
		g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
		g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
							
		
		// calculate the scaling which is dependent to the width		
		this.scalingWidth =  (float) (getPreferredSize().getWidth() / this.sampleCount);			
		
		drawMeasureBackground(g2);
		drawGrid(g2);
		drawGraph(g2);
		drawName(g2, clipLeadName);
		drawMeasureBars(g2);
		drawBorder(g2);
		drawAnnotations(g2);
	}
	
	private void drawGrid(Graphics2D g2) {
		int pixelPerInch = Toolkit.getDefaultToolkit().getScreenResolution();
		double pixelPerMm = pixelPerInch/25.4 * plugin.getZoom();
		
		BasicStroke thin = new BasicStroke(0.25f);
		BasicStroke thick = new BasicStroke(0.5f);
		g2.setColor(new Color(231, 84, 72, 200));		
		
		// draw horizontal lines
		Dimension dim = getPreferredSize();
		for(int i=0; i < dim.height/pixelPerMm; i++) {
			g2.setStroke(i % 5 == 0 ? thick : thin);
			g2.draw(new Line2D.Double(0, i*pixelPerMm, dim.getWidth(), i*pixelPerMm));
		}
		
		// draw vertical lines
		for(int i=0; i < dim.width/pixelPerMm; i++) {
			g2.setStroke(i % 5 == 0 ? thick : thin);
			g2.draw(new Line2D.Double(i*pixelPerMm, 0, i*pixelPerMm, dim.getHeight()));
		}
	}
	
	private void drawGraph(Graphics2D g2) {
		// draw waveform as line using the given values
		g2.setColor(Color.BLACK);
		g2.setStroke(new BasicStroke(0.5f));
		int height = getPreferredSize().height;
		double mvHeight = height / plugin.getChannelHeightInMillivolt();

		for(int i = 0; i < (this.sampleCount - 1); i++) {
			int a = i;
			int b = i + 1;
			// draw a line between two points
			// dim.height / 2 is our base line
			Line2D line = new Line2D.Double(
					this.scalingWidth * a, 
					(height /2 - this.valueScaling * ( (float)(filter.get(a) / (float) 1000) * mvHeight) ), 
					this.scalingWidth * b, 
					(height /2 - this.valueScaling * ( (float)(filter.get(b) / (float) 1000) * mvHeight ) ));
			g2.draw(line);
		 }	
	}
	
	private void drawMeasureBackground(Graphics2D g2) {
		SampleMarker start = getMarker(Tool.VERTICAL_MEASURE, SampleMarker.Type.START);
		SampleMarker stop = getMarker(Tool.VERTICAL_MEASURE, SampleMarker.Type.STOP);
		if(start == null || stop == null)
			return;
		
		Color background = new Color(230, 230, 230, 200);		
		g2.setColor(background);
		
		double startX = this.scalingWidth * start.getSample();
		double stopX = this.scalingWidth * stop.getSample();
		if(startX > stopX) {
			double tmp = stopX;
			stopX = startX;
			startX = tmp;
		}
		
		Rectangle2D rect = new Rectangle2D.Double(startX, 0, stopX-startX, getPreferredSize().height);
		g2.fill(rect);
	}
	
	private void drawMeasureBars(Graphics2D g2) {
		drawVerticalBar(g2, Color.CYAN, highlightedSample);
		
		if(plugin.getSelectedTool() == Tool.HORIZONTAL_MEASURE)
			drawHorizontalBar(g2, Color.CYAN, highlightedSample);
		
		
		for(SampleMarker marker: markers) {
			Color color = Color.BLUE;
			if(marker.getType() == SampleMarker.Type.START)
				color = Color.GREEN;
			
			if(marker.getTool() == Tool.HORIZONTAL_MEASURE)
				drawHorizontalBar(g2, color, marker.getSample());
			else
				drawVerticalBar(g2, color, marker.getSample());
		}		
	}
	
	private void drawVerticalBar(Graphics2D g2, Color color, int sample) {
		if(sample < 0)
			return;
		
		double x = this.scalingWidth * sample; 
		Line2D line = new Line2D.Double(x, 0, x, getPreferredSize().height);
		
		g2.setColor(color);
		g2.setStroke(new BasicStroke(0.9f));
		g2.draw(line);
	}
	
	private void drawHorizontalBar(Graphics2D g2, Color color, int sample) {
		if(sample < 0)
			return;
		
		Dimension dim = getPreferredSize();
		double mvHeight = dim.height / plugin.getChannelHeightInMillivolt();
		double y = (dim.height /2 - this.valueScaling * ( (float)(filter.get(sample) / (float) 1000) * mvHeight)); 
		Line2D line = new Line2D.Double(0, y, dim.width, y);
		
		g2.setColor(color);
		g2.setStroke(new BasicStroke(0.9f));
		g2.draw(line);
	}
	
	
	private void drawHalfBar(Graphics2D g2, Color color, int sample) {
		if(sample < 0)
			return;
		
		double x = this.scalingWidth * sample; 
		Dimension dim = getPreferredSize();
		int h = dim.height/5;
		Line2D line = new Line2D.Double(x, h, x, dim.height-h);
		
		g2.setColor(color);
		g2.setStroke(new BasicStroke(0.9f));
		g2.draw(line);
	}
	
	private void drawAnnotations(Graphics2D g2) {
		if(annotations == null)
			return;
		
		for(Annotation a: annotations) {			
			if("ms".equals(a.unit)) {
				int ms = Integer.valueOf(a.value);
				int sample = (int)(ms * (double)plugin.getSamplesPerSecond()) / 1000;
				drawHalfBar(g2, Color.BLUE, sample);
			}	
			else if("POINT".equals(a.unit)) {
				int sample = Integer.valueOf(a.value);
				drawHalfBar(g2, Color.BLUE, sample);
			}
		}
	}
	
	private void drawBorder(Graphics2D g2) {
		g2.setColor(Color.GRAY);
		g2.setStroke(new BasicStroke(0.5f));
		
		Dimension dim = getPreferredSize();
		g2.drawRect(0, 0, dim.width-1, dim.height-1);
	}
		
	private void drawName(Graphics2D g2, boolean clipLeadName) {	
		g2.setColor(Color.black);			
		g2.setFont(new Font("SanSerif", Font.BOLD, 11));	
		
		int x = 5;
		int y = 15;
		
		if(clipLeadName) {
			x += g2.getClipBounds().x;
			y += g2.getClipBounds().y;
		}

		g2.drawString(definition.getName(), x, y);		
	}
}