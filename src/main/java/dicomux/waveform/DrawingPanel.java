package dicomux.waveform;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;

import javax.swing.ImageIcon;
import javax.swing.JPanel;

import dicomux.waveform.WaveformLayout.Format;



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
	private float scalingWidth;
	private ChannelDefinition definition;
	private int mvCellCount;
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
	
	public void setRhythm(boolean mode) {
		this.isRhythm = mode;
	}
	
	private void setHighlightedSample(int sample) {
		if(sample < 0 || sample >= data.length) {
			highlightedSample = -1;
			plugin.getInfoPanel().setCurrentValues(-1, -1);
		}
		else { 
			highlightedSample = sample;
			double sec = highlightedSample / (double)plugin.getSamplesPerSecond();
			double uV = data[highlightedSample] * valueScaling;	
			plugin.getInfoPanel().setCurrentValues(sec, uV/1000);
		}			
	}
	
	private void setStartSample(int sample) {
		plugin.getInfoPanel().setDeltaValues(-1, -1);
		
		if(sample < 0 || sample >= data.length) {
			startSample = -1;
			plugin.getInfoPanel().setStartValues(-1, -1);
		}
		else { 
			startSample = sample;
			double sec = startSample / (double)plugin.getSamplesPerSecond();
			double uV = data[startSample] * valueScaling;	
			plugin.getInfoPanel().setStartValues(sec, uV/1000);
			
			if(stopSample > startSample) {
				double dsec = (stopSample-startSample) / (double)plugin.getSamplesPerSecond();
				double duV = (data[stopSample] - data[startSample]) * valueScaling;
				plugin.getInfoPanel().setDeltaValues(dsec, duV/1000);
			}
		}			
	}
	
	private void setStopSample(int sample) {
		plugin.getInfoPanel().setDeltaValues(-1, -1);
		
		if(sample < 0 || sample >= data.length) {
			stopSample = -1;
			plugin.getInfoPanel().setStopValues(-1, -1);
		}
		else { 
			stopSample = sample;
			double sec = stopSample / (double)plugin.getSamplesPerSecond();
			double uV = data[stopSample] * valueScaling;		
			plugin.getInfoPanel().setStopValues(sec, uV/1000);
			
			if(stopSample > startSample && startSample >= 0) {
				double dsec = (stopSample-startSample) / (double)plugin.getSamplesPerSecond();
				double duV = (data[stopSample] - data[startSample]) * valueScaling;
				plugin.getInfoPanel().setDeltaValues(dsec, duV/1000);
			}
		}			
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
				Toolkit toolkit = Toolkit.getDefaultToolkit();  
				Image image = new ImageIcon(this.getClass().getClassLoader().getResource("images/cursorHand.png")).getImage();					
				Point hotspot = new Point(7,0);
				Cursor cursor = toolkit.createCustomCursor(image, hotspot, "dicomux"); 
				setCursor(cursor);
				
				setBackground(new Color(255, 255, 215));
				
				plugin.getInfoPanel().setLead(definition.getName());
				plugin.getInfoPanel().setMinMax(definition.getMinimum_uV()/1000, definition.getMaximum_uV()/1000);
				setStartSample(startSample);
				setStopSample(stopSample);
			}
			
			public void mouseExited(MouseEvent e) {
				Cursor normal = new Cursor(Cursor.DEFAULT_CURSOR);
				setCursor(normal);
				
				setBackground(Color.WHITE);
				setHighlightedSample(-1);
				repaint();
			}
		});
	}		
	
	public void paintComponent( Graphics g ) {
		
		super.paintComponent(g);
		final Graphics2D g2 = (Graphics2D) g;
		
		// set rendering options
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);    
		g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
		g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_NORMALIZE);
		g2.setRenderingHint(RenderingHints.KEY_DITHERING, RenderingHints.VALUE_DITHER_ENABLE);
		
		if(plugin.getDisplayFormat() == Format.DEFAULT || isRhythm) {
			this.secsCellCount = plugin.getSeconds() * 10;
			this.end = this.start + this.data.length;
		}
		else if(plugin.getDisplayFormat() == Format.FOURPARTS || plugin.getDisplayFormat() == Format.FOURPARTS_RYTHM) {
			this.secsCellCount = (int) (2.5 * 10);
			this.end = this.start + (int) (2.5 * plugin.getSamplesPerSecond());
		}
		else if(plugin.getDisplayFormat() == Format.TWOPARTS) {
			this.secsCellCount = 5 * 10;
			this.end = this.start + 5 * plugin.getSamplesPerSecond();
		}
					
		this.dim = getPreferredSize();
		// calculate height and width of the cells
		this.cellHeight = dim.getHeight() / this.mvCellCount;
		this.cellWidth = dim.getWidth() / this.secsCellCount;
		
		// calculate the scaling which is dependent to the width	
		this.scalingWidth =  (float) (cellWidth / ((this.end - this.start) / secsCellCount ));			
		
		drawMeasureBackground(g2);
		drawGrid(g2);
		drawGraph(g2);
		drawName(g2);
		drawMeasureBars(g2);
		drawBorder(g2);
	}
	
	private void drawGrid(Graphics2D g2) {
		// set line color
		g2.setColor(new Color(231, 84, 72));
		// draw horizontal lines
		g2.setStroke(new BasicStroke(2.0f));
		for(int i = 0; i < mvCellCount; i++) {
			g2.draw(new Line2D.Double(0, i * cellHeight, 
					dim.getWidth(), i * cellHeight));			
		}
		
		// draw vertical lines
		for(int i = 0; i < secsCellCount; i++ ) {
			// draw every 10th line which represents a full second bigger 
			if(i % 10 == 0)
			{
				g2.setStroke(new BasicStroke(2.0f));
			}
			else
			{
				g2.setStroke(new BasicStroke(1.0f));
			}
			g2.draw(new Line2D.Double(i * cellWidth , 0, 
					i * cellWidth, dim.getHeight()));
		}
	}
	
	private void drawGraph(Graphics2D g2) {
		// draw waveform as line using the given values
		g2.setColor(Color.BLACK);
		g2.setStroke(new BasicStroke(1.2f));
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
		if(startSample < 0 || stopSample < 0 || stopSample <= startSample)
			return;

		Color background = new Color(230, 230, 230, 200);
		
		g2.setColor(background);
		double startX = this.scalingWidth * (startSample - this.start);
		double stopX = this.scalingWidth * (stopSample - this.start);
		Rectangle2D rect = new Rectangle2D.Double(startX, 0, stopX-startX, this.dim.height);
		g2.fill(rect);
	}
	
	private void drawMeasureBars(Graphics2D g2) {
		drawBar(g2, Color.CYAN, highlightedSample);
		drawBar(g2, Color.GREEN, startSample);
		drawBar(g2, Color.BLUE, stopSample);
	}
	
	private void drawBar(Graphics2D g2, Color color, int sample) {
		if(sample < 0)
			return;
		
		double x = this.scalingWidth * (sample - this.start); 
		Line2D line = new Line2D.Double(x, 0, x, this.dim.height);
		
		g2.setColor(color);
		g2.setStroke(new BasicStroke(1.2f));
		g2.draw(line);
	}
	
	private void drawBorder(Graphics2D g2) {
		g2.setColor(Color.GRAY);
		g2.setStroke(new BasicStroke(2f));
		g2.drawRect(0, 0, this.dim.width-1, this.dim.height-1);
	}
	
	
	private void drawName(Graphics2D g2) {
		g2.setColor(Color.black);			
		g2.setFont(new Font("SanSerif", Font.BOLD, 12));		
		g2.drawString(definition.getName(), 5, 15);			
	}
}