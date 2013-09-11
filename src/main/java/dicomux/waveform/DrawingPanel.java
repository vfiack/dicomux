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
import java.awt.event.MouseMotionAdapter;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.text.DecimalFormat;
import java.util.List;

import javax.swing.JPanel;

import dicomux.waveform.WaveformPlugin.Orientation;


/**
 * This class handles the drawing of the waveform
 * 
 * @author norbert
 *
 */
class DrawingPanel extends JPanel {	
	private static final long serialVersionUID = 856943381513072262L;
	
	private WaveformPlugin plugin;
	private int[] data;
	private List<Annotation> annotations;
	private float scalingWidth;
	private ChannelDefinition definition;
	private double mvCellCount;
	private double secsCellCount;
	private double cellHeight;
	private double cellWidth;
	private Dimension dim;
	private int start;
	private int end;
	private double valueScaling;
	private double offset; 
	private boolean isRhythm;
	
	//selected positions for measures
	private int highlightedSample;
	private int startSample;
	private int stopSample;
	
	public DrawingPanel(WaveformPlugin plugin, int[] values, double start, ChannelDefinition definition) {
		this.plugin = plugin;
		this.data = values;
		this.definition = definition;			
		this.mvCellCount = plugin.getMvCells();
		this.secsCellCount = plugin.getSeconds() * 10;
		this.dim = getPreferredSize();
		// calculate height and width of the cells
		this.cellHeight = dim.getHeight() / mvCellCount;
		this.cellWidth = dim.getWidth() / secsCellCount;
		this.start = (int) (start * plugin.getSamplesPerSecond());
		this.end = data.length;
		this.offset = start;
		// calculate scaling of the sample values
		this.valueScaling = this.definition.getScaling();
		
		this.highlightedSample = -1;
		this.startSample = -1;
		this.stopSample = -1;
		
		addListeners();
		this.isRhythm = false;
		
		setBackground(Color.WHITE);
	}
	
	public void setTime(double start, double length) {
		this.start = (int) (start * plugin.getSamplesPerSecond());
		this.offset = start;
				
		if(start+length > plugin.getSeconds())
			length = plugin.getSeconds()-start;
				
		this.secsCellCount = (int)(length*10);
		this.end = this.start + (int)(length*plugin.getSamplesPerSecond());
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
			plugin.getAnnotations().setMeasure("cursor time", "-", "", "");
			plugin.getAnnotations().setMeasure("cursor value", "-", "", "");
		}
		else { 
			highlightedSample = sample;
			double sec = highlightedSample / (double)plugin.getSamplesPerSecond();
			double uV = data[highlightedSample] * valueScaling;	
			
			DecimalFormat format = new DecimalFormat("##.####;-##.####");
			plugin.getAnnotations().setMeasure("cursor time", "-", format.format(sec*1000), "ms");
			plugin.getAnnotations().setMeasure("cursor value", "-", format.format(uV/1000), "mV");
		}			
	}
	
	private void setStartSample(int sample) {
		plugin.getAnnotations().removeMeasure("duration", definition.getName());
		plugin.getAnnotations().removeMeasure("difference", definition.getName());
		plugin.getAnnotations().removeMeasure("amplitude", definition.getName());
		
		if(sample < 0 || sample >= data.length) {
			startSample = -1;
			
			plugin.getAnnotations().removeMeasure("start time", definition.getName());
			plugin.getAnnotations().removeMeasure("start value", definition.getName());
		}
		else { 
			startSample = sample;
			double sec = startSample / (double)plugin.getSamplesPerSecond();
			double uV = data[startSample] * valueScaling;	
			
			DecimalFormat format = new DecimalFormat("##.####;-##.####");			
			plugin.getAnnotations().setMeasure("start time", definition.getName(), format.format(sec*1000), "ms");
			plugin.getAnnotations().setMeasure("start value", definition.getName(), format.format(uV/1000), "mV");
			
			if(stopSample >= 0)
				setSelection();
		}			
	}
	
	private void setStopSample(int sample) {
		plugin.getAnnotations().removeMeasure("duration", definition.getName());
		plugin.getAnnotations().removeMeasure("difference", definition.getName());
		plugin.getAnnotations().removeMeasure("amplitude", definition.getName());
		
		if(sample < 0 || sample >= data.length) {
			stopSample = -1;

			plugin.getAnnotations().removeMeasure("stop time", definition.getName());
			plugin.getAnnotations().removeMeasure("stop value", definition.getName());
		}
		else { 
			stopSample = sample;
			double sec = stopSample / (double)plugin.getSamplesPerSecond();
			double uV = data[stopSample] * valueScaling;		

			DecimalFormat format = new DecimalFormat("##.####;-##.####");
			plugin.getAnnotations().setMeasure("stop time", definition.getName(), format.format(sec*1000), "ms");
			plugin.getAnnotations().setMeasure("stop value", definition.getName(), format.format(uV/1000), "mV");
			
			if(startSample >= 0)
				setSelection();
		}			
	}
	
	private void setSelection() {
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
		plugin.getAnnotations().setMeasure("duration", definition.getName(), format.format(time*1000), "ms");
		plugin.getAnnotations().setMeasure("difference", definition.getName(), format.format(diff_uV/1000), "mV");
		plugin.getAnnotations().setMeasure("amplitude", definition.getName(), format.format(amplitude_uV/1000), "mV");
	}
	
	
	private void addListeners() {
		// used to get the current position of the mouse pointer into the information panel
		this.addMouseMotionListener( new MouseMotionAdapter() {	
			public void mouseDragged(MouseEvent e) {
				mouseMoved(e);
			}
			
			public void mouseMoved(MouseEvent e) {			
				double sec = offset + (e.getPoint().getX() / cellWidth * 0.1);
				setHighlightedSample((int)Math.round(plugin.getSamplesPerSecond() * sec));

				if(highlightedSample >= 0) {
					if((e.getModifiersEx() & MouseEvent.BUTTON1_DOWN_MASK) == MouseEvent.BUTTON1_DOWN_MASK)
						setStartSample(highlightedSample);
					if((e.getModifiersEx() & MouseEvent.BUTTON3_DOWN_MASK) == MouseEvent.BUTTON3_DOWN_MASK)
						setStopSample(highlightedSample);
				}
				
				repaint();						
			}
		});
		
		this.addMouseListener( new MouseAdapter() {
			public void mouseClicked(MouseEvent e) {
				if(e.getButton() == MouseEvent.BUTTON1)
					setStartSample(highlightedSample);
				else if(e.getButton() == MouseEvent.BUTTON3)
					setStopSample(highlightedSample);
				else if(e.getButton() == MouseEvent.BUTTON2) {
					setStartSample(-1);
					setStopSample(-1);
				}
				
				repaint();
			}
			
			public void mouseEntered(MouseEvent e) {
				setBackground(new Color(255, 255, 215));
				
				DecimalFormat format = new DecimalFormat("##.####;-##.####");
				plugin.getAnnotations().setMeasure("minimum", definition.getName(), format.format(definition.getMinimum_uV()/1000), "mV");
				plugin.getAnnotations().setMeasure("maximum", definition.getName(), format.format(definition.getMaximum_uV()/1000), "mV");
				
				setStartSample(startSample);
				setStopSample(stopSample);
			}
			
			public void mouseExited(MouseEvent e) {
				plugin.getAnnotations().removeMeasure("minimum", definition.getName());
				plugin.getAnnotations().removeMeasure("maximum", definition.getName());
				
				setBackground(Color.WHITE);
				setHighlightedSample(-1);
				repaint();
			}
		});
	}		
	
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
		g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_NORMALIZE);
		g2.setRenderingHint(RenderingHints.KEY_DITHERING, RenderingHints.VALUE_DITHER_ENABLE);
							
		this.dim = getPreferredSize();
		// calculate height and width of the cells
		this.cellHeight = dim.getHeight() / this.mvCellCount;
		this.cellWidth = dim.getWidth() / this.secsCellCount;
		
		// calculate the scaling which is dependent to the width	
		this.scalingWidth =  (float) (cellWidth / ((this.end - this.start) / secsCellCount ));			
		
		drawMeasureBackground(g2);
		drawGrid(g2);
		drawGraph(g2);
		drawName(g2, clipLeadName);
		drawMeasureBars(g2);
		drawBorder(g2);
		drawAnnotations(g2);
	}

	/*
	private void drawTimeBasedGrid(Graphics2D g2) {
		// draw horizontal lines
		g2.setColor(new Color(231, 84, 72, 200));
		g2.setStroke(new BasicStroke(0.5f));
		double padding = (mvCellCount - Math.floor(mvCellCount))/2;
		int shift = (int)(padding * cellHeight);
		for(int i = 0; i < mvCellCount; i++) {
			g2.draw(new Line2D.Double(0, i * cellHeight + shift, 
					dim.getWidth(), i * cellHeight + shift));			
		}
		
		// draw vertical lines
		for(int i = 0; i < secsCellCount; i++ ) {
			// draw every 10th line which represents a full second bigger 
			if(i % 10 == 0)
			{
				g2.setStroke(new BasicStroke(1.0f));
			}
			else
			{
				g2.setStroke(new BasicStroke(0.5f));
			}
			g2.draw(new Line2D.Double(i * cellWidth , 0, 
					i * cellWidth, dim.getHeight()));
		}
	}
	*/
	
	private void drawGrid(Graphics2D g2) {
		int pixelPerInch = Toolkit.getDefaultToolkit().getScreenResolution();
		double pixelPerMm = pixelPerInch/25.4;
		
		BasicStroke thin = new BasicStroke(0.25f);
		BasicStroke thick = new BasicStroke(0.5f);
		g2.setColor(new Color(231, 84, 72, 200));		
		
		// draw horizontal lines
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
		g2.setStroke(new BasicStroke(1f));
		for(int i  = this.start; i < (this.end - 1); i++) {
			int a = i;
			int b = i + 1;
			// draw a line between two points
			// dim.height / 2 is our base line
			Line2D line = new Line2D.Double(
					this.scalingWidth * (a - this.start), 
					(this.dim.height /2 - this.valueScaling * ( (float)(this.data[a] / (float) 1000) * this.cellHeight) ), 
					this.scalingWidth * (b - this.start), 
					( this.dim.height /2 - this.valueScaling * ( (float)(this.data[b] / (float) 1000) * this.cellHeight ) ));
			g2.draw(line);
		 }	
	}
	
	private void drawMeasureBackground(Graphics2D g2) {
		if(startSample < 0 || stopSample < 0 || plugin.getMeasureBarsOrientation() == Orientation.HORIZONTAL)
			return;

		Color background = new Color(230, 230, 230, 200);		
		g2.setColor(background);
		
		double startX = this.scalingWidth * (startSample - this.start);
		double stopX = this.scalingWidth * (stopSample - this.start);
		if(startX > stopX) {
			double tmp = stopX;
			stopX = startX;
			startX = tmp;
		}
		
		Rectangle2D rect = new Rectangle2D.Double(startX, 0, stopX-startX, this.dim.height);
	
		
		g2.fill(rect);
	}
	
	private void drawMeasureBars(Graphics2D g2) {
		drawVerticalBar(g2, Color.CYAN, highlightedSample);
		
		if(plugin.getMeasureBarsOrientation() == WaveformPlugin.Orientation.HORIZONTAL) {
			drawHorizontalBar(g2, Color.CYAN, highlightedSample);
			drawHorizontalBar(g2, Color.GREEN, startSample);
			drawHorizontalBar(g2, Color.BLUE, stopSample);
		} else {
			drawVerticalBar(g2, Color.GREEN, startSample);
			drawVerticalBar(g2, Color.BLUE, stopSample);
		}
	}
	
	private void drawVerticalBar(Graphics2D g2, Color color, int sample) {
		if(sample < 0)
			return;
		
		double x = this.scalingWidth * (sample - this.start); 
		Line2D line = new Line2D.Double(x, 0, x, this.dim.height);
		
		g2.setColor(color);
		g2.setStroke(new BasicStroke(1.2f));
		g2.draw(line);
	}
	
	private void drawHorizontalBar(Graphics2D g2, Color color, int sample) {
		if(sample < 0)
			return;
		
		double y = (this.dim.height /2 - this.valueScaling * ( (float)(this.data[sample] / (float) 1000) * this.cellHeight)); 
		Line2D line = new Line2D.Double(0, y, this.dim.width, y);
		
		g2.setColor(color);
		g2.setStroke(new BasicStroke(1.2f));
		g2.draw(line);
	}
	
	
	private void drawHalfBar(Graphics2D g2, Color color, int sample) {
		if(sample < 0)
			return;
		
		double x = this.scalingWidth * (sample - this.start); 
		int h = this.dim.height/5;
		Line2D line = new Line2D.Double(x, h, x, this.dim.height-h);
		
		g2.setColor(color);
		g2.setStroke(new BasicStroke(1.2f));
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
		g2.drawRect(0, 0, this.dim.width-1, this.dim.height-1);
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