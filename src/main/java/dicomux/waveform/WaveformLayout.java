package dicomux.waveform;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.LayoutManager;
import java.awt.Toolkit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WaveformLayout implements LayoutManager {
	public enum Format {DEFAULT, TWOPARTS, FOURPARTS, FOURPARTS_RYTHM}
		
	private static final String[] orderTwoParts = {
		"lead i", "lead v1", 
		"lead ii", "lead v2",
		"lead iii", "lead v3", 
		"lead avr", "lead v4", 
		"lead avl", "lead v5",
		"lead avf", "lead v6"		
	};
	
	private static final String[] orderFourParts = {
		"lead i", "lead avr", "lead v1", "lead v4", 
		"lead ii", "lead avl", "lead v2", "lead v5",
		"lead iii", "lead avf", "lead v3",  "lead v6"		
	};
	
	private static final String[] orderFourPartsRythm = {
		"lead i", "lead avr", "lead v1", "lead v4", 
		"lead ii", "lead avl", "lead v2", "lead v5",
		"lead iii", "lead avf", "lead v3",  "lead v6",
		"rythm"
	};
	
	
	private WaveformPlugin plugin;	
	private Map<String, Component> components;

	private int mmPerSecond = 25;
	private int mmPerMillivolt = 10;
	
	private Format format;	
    private int displayFactorWidth;
    private int displayFactorHeight;
    
	
	public WaveformLayout(WaveformPlugin plugin, Format format) {
		this.plugin = plugin;
		this.components = new HashMap<String, Component>();
		
		setFormat(format);
	}
	
	public int getSpeed() {
		return mmPerSecond;
	}
	
	public void setSpeed(int mmPerSecond) {
		this.mmPerSecond = mmPerSecond;
	}
	
	public int getAmplitude() {
		return mmPerMillivolt;
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
	
	public List<Component> getOrderedComponents(Container parent) {
		List<Component> list = new ArrayList<Component>(parent.getComponentCount());
		
		if(format == Format.DEFAULT) {
			//default, don't sort, return everything except rythm
			for(Component c: parent.getComponents()) {
				if((c instanceof DrawingPanel) && ((DrawingPanel)c).isRythm()) {
					//don't add rythm
				} else {
					list.add(c);
				}
			}
			return list;			
		}

		//specified format, respect lead order
		String[] order = {};
		if(format == Format.TWOPARTS)
			order = orderTwoParts;
		else if(format == Format.FOURPARTS)
			order = orderFourParts;
		else if(format == Format.FOURPARTS_RYTHM)
			order = orderFourPartsRythm;
		
		for(String lead: order) {
			list.add(components.get(lead));
		}
		
		return list;
	}
	
	//--
	
	public void addLayoutComponent(String name, Component comp) {
		components.put(name.toLowerCase().trim(), comp);
	}

	public void removeLayoutComponent(Component comp) {
		String key = null;
		for(Map.Entry<String, Component> entry: components.entrySet()) {
			if(comp == entry.getValue()) {
				key = entry.getKey();
				break;
			}				
		}
		
		components.remove(key);
	}

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
		
		List<Component> ordered = getOrderedComponents(parent);
		for(Component c: parent.getComponents()) {
			c.setVisible(ordered.contains(c));
				
		}
		
		for(Component c: ordered) {		
			if(c != null) {
				if((c instanceof DrawingPanel) && ((DrawingPanel)c).isRythm()) {
					//rythm lead, use the whole width
					c.setPreferredSize(new Dimension(maxWidth, height));
					c.setBounds(x, y, maxWidth, height); 
				}
				else {
					//normal lead, split as necessary
					c.setPreferredSize(dim);
					c.setBounds(x, y, width, height);
				}
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
