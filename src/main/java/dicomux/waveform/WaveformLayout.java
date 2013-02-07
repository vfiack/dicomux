package dicomux.waveform;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.LayoutManager;
import java.awt.Toolkit;

public class WaveformLayout implements LayoutManager {
	public enum Format {DEFAULT, TWOPARTS, FOURPARTS, FOURPARTS_RYTHM}

	private WaveformPlugin plugin;

	private int mmPerSecond = 25;
	private int mmPerMillivolt = 10;
	
	private Format format;	
    private int displayFactorWidth;
    private int displayFactorHeight;
    
	
	public WaveformLayout(WaveformPlugin plugin, Format format) {
		this.plugin = plugin;
		setFormat(format);
	}
	
	public void setSpeed(int mmPerSecond) {
		this.mmPerSecond = mmPerSecond;
	}
	
	public void setAmplitude(int mmPerMillivolt) {
		this.mmPerMillivolt = mmPerMillivolt;
	}

	public void setFormat(Format format) {
		this.format = format;
		
        if(format == Format.TWOPARTS) {
        	displayFactorWidth = 2;
			displayFactorHeight = 6;
        } else if(format == Format.FOURPARTS) {
        	displayFactorWidth = 4;
			displayFactorHeight = 3;
        } else if(format == Format.FOURPARTS_RYTHM) {
        	displayFactorWidth = 4;
			displayFactorHeight = 4;
        } else {
        	displayFactorWidth = 1;
        	displayFactorHeight = plugin.getNumberOfChannels();
        }
	}
	
	//--
	
	public void addLayoutComponent(String name, Component comp) {}

	public void removeLayoutComponent(Component comp) {}

	public Dimension minimumLayoutSize(Container parent) {
		return preferredLayoutSize(parent);
	}

	public Dimension preferredLayoutSize(Container parent) {
		Insets insets = parent.getInsets();
		Dimension dim = new Dimension(insets.left + insets.right, insets.top + insets.bottom);

		int pixelPerInch = Toolkit.getDefaultToolkit().getScreenResolution();
		double pixelPerMm = pixelPerInch/25.4;
		int secWidth = (int)(mmPerSecond*pixelPerMm);
		int mvHeight = (int)(mmPerMillivolt*pixelPerMm);

		if(format == Format.DEFAULT)
			dim.width += (int)(plugin.getSeconds()*secWidth)/displayFactorWidth;
		else
			dim.width += (int)(10*secWidth); //limit to 10s
			
		dim.height += displayFactorHeight * plugin.getMvCells()*mvHeight;

		return dim;		
	}
	

	public void layoutContainer(Container parent) {
        Insets insets = parent.getInsets();
        int maxWidth = parent.getWidth() - insets.left - insets.right;
        int maxHeight = parent.getHeight() - insets.top - insets.bottom;

		int width = maxWidth/displayFactorWidth;
		int height = maxHeight/displayFactorHeight;
		
		Dimension dim = new Dimension(width, height);
		int i = 0;
		int x = 0;
		int y = 0;
		for(Component c: parent.getComponents()) {			
			if(format == Format.FOURPARTS_RYTHM && i == parent.getComponentCount()-1) {
				//rythm lead, use the whole width
				c.setPreferredSize(new Dimension(maxWidth, height));
				c.setBounds(x, y, maxWidth, height); 
			}
			else {
				//normal lead, split as necessary
				c.setPreferredSize(dim);
				c.setBounds(x, y, width, height);
			}
			
			i++;
			if(i % displayFactorWidth == 0) {
				x = 0;
				y += height;
			} else {
				x += width;
			}
		}

	}
}
