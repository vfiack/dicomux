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

import javax.swing.ImageIcon;
import javax.swing.JPanel;



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
	private int mv_cell_count;
	private int secs_cell_count;
	private double cellheight;
	private double cellwidth;
	private Dimension dim;
	private int start;
	private int end;
	private double valueScaling;
	private double offset; 
	private boolean isRhythm;
	private int highlightedSample;
	
	public DrawingPanel(WaveformPlugin plugin, int[] values, double start, ChannelDefinition definition) {
		this.plugin = plugin;
		this.data = values;
		this.definition = definition;			
		this.mv_cell_count = plugin.getMvCells();
		this.secs_cell_count = plugin.getSeconds() * 10;
		this.dim = getPreferredSize();
		// calculate height and width of the cells
		this.cellheight = dim.getHeight() / mv_cell_count;
		this.cellwidth = dim.getWidth() / secs_cell_count;
		this.start = (int) (start * plugin.getSamplesPerSecond());
		this.end = data.length;
		this.offset = start;
		// calculate scaling of the sample values
		this.valueScaling = this.definition.getScaling();
		this.highlightedSample = -1;
		
		addListeners();
		this.isRhythm = false;
	}
	
	public void setRhythm(boolean mode) {
		this.isRhythm = mode;
	}
	
	private void addListeners() {
		// used to get the current position of the mouse pointer into the information panel
		this.addMouseMotionListener( new MouseMotionAdapter() {						
				public void mouseMoved(MouseEvent e) {			
					double sec = offset + (e.getPoint().getX() / cellwidth * 0.1);

					// lookup for the nearest sample
					highlightedSample = (int)Math.round(plugin.getSamplesPerSecond() * sec);
					if(highlightedSample >= data.length) {
						highlightedSample = -1;
						plugin.getInfoPanel().setSeconds(0);
						plugin.getInfoPanel().setMiliVolt(0);
					} else {	
						sec = highlightedSample / (double)plugin.getSamplesPerSecond();
						double uV = data[highlightedSample] * valueScaling;		
						plugin.getInfoPanel().setSeconds(sec);
						plugin.getInfoPanel().setMiliVolt(uV/1000);
					}
					
					repaint();						
				}
			}
		);
		
		this.addMouseListener( new MouseAdapter() {
			
			public void mouseEntered(MouseEvent e) {
				Toolkit toolkit = Toolkit.getDefaultToolkit();  
				Image image = new ImageIcon(this.getClass().getClassLoader().getResource("images/cursorHand.png")).getImage();					
				Point hotspot = new Point(7,0);
				Cursor cursor = toolkit.createCustomCursor(image, hotspot, "dicomux"); 
				setCursor(cursor);
				
				plugin.getInfoPanel().setLead(definition.getName());
				plugin.getInfoPanel().setMaximum(definition.getMaximum_uV()/1000);
				plugin.getInfoPanel().setMinimum(definition.getMinimum_uV()/1000);
			}
			
			public void mouseExited(MouseEvent e) {
				Cursor normal = new Cursor(Cursor.DEFAULT_CURSOR);
				setCursor(normal);
				highlightedSample = -1;
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
		
		if(plugin.getDisplayFormat().equals(WaveformPlugin.DEFAULTFORMAT) || isRhythm) {
			this.secs_cell_count = plugin.getSeconds() * 10;
			this.end = this.start + this.data.length;
		}
		else if(plugin.getDisplayFormat().equals(WaveformPlugin.FOURPARTS)) {
			this.secs_cell_count = (int) (2.5 * 10);
			this.end = this.start + (int) (2.5 * plugin.getSamplesPerSecond());
		}
		else if(plugin.getDisplayFormat().equals(WaveformPlugin.FOURPARTSPLUS)) {
			this.secs_cell_count = (int) (2.5 * 10);
			this.end = this.start + (int) (2.5 * plugin.getSamplesPerSecond());
		}
		else if(plugin.getDisplayFormat().equals(WaveformPlugin.TWOPARTS)) {
			this.secs_cell_count = 5 * 10;
			this.end = this.start + 5 * plugin.getSamplesPerSecond();
		}

		//set background color to white
		this.setBackground(Color.WHITE);
					
		this.dim = getPreferredSize();
		// calculate height and width of the cells
		this.cellheight = dim.getHeight() / this.mv_cell_count;
		this.cellwidth = dim.getWidth() / this.secs_cell_count;
		
		// calculate the scaling which is dependent to the width	
		this.scalingWidth =  (float) (cellwidth / ((this.end - this.start) / secs_cell_count ));			
		
		drawGrid(g2);
		drawGraph(g2);
		drawName(g2);
		highlightSample(g2);
	}
	
	private void drawGrid(Graphics2D g2) {
		// set line color
		g2.setColor(new Color(231, 84, 72));
		// draw horizontal lines
		g2.setStroke(new BasicStroke(2.0f));
		for(int i = 0; i < mv_cell_count; i++) {
			g2.draw(new Line2D.Double(0, i * cellheight, 
					dim.getWidth(), i * cellheight));			
		}
		
		// draw vertical lines
		for(int i = 0; i < secs_cell_count; i++ ) {
			// draw every 10th line which represents a full second bigger 
			if(i % 10 == 0)
			{
				g2.setStroke(new BasicStroke(2.0f));
			}
			else
			{
				g2.setStroke(new BasicStroke(1.0f));
			}
			g2.draw(new Line2D.Double(i * cellwidth , 0, 
					i * cellwidth, dim.getHeight()));
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
					(this.dim.height /2 - this.valueScaling * ( (float)(this.data[a] / (float) 1000) * this.cellheight) ), 
					this.scalingWidth * (b - this.start), 
					( this.dim.height /2 - this.valueScaling * ( (float)(this.data[b] / (float) 1000) * this.cellheight ) ));
			g2.draw(line);
		 }	
	}
	
	private void highlightSample(Graphics2D g2) {
		double x = this.scalingWidth * (highlightedSample - this.start); 
		Line2D line = new Line2D.Double(x, 0, x, this.dim.height);
		
		g2.setColor(Color.GREEN);
		g2.setStroke(new BasicStroke(1f));
		g2.draw(line);
	}
	
	private void drawName(Graphics2D g2) {
		g2.setColor(Color.black);			
		g2.setFont(new Font("SanSerif", Font.BOLD, 12));		
		g2.drawString(definition.getName(), 5, 15);			
	}
}