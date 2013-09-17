package dicomux.waveform;

import static java.lang.Math.max;
import static java.lang.Math.min;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.LayoutManager;
import java.awt.Toolkit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import dicomux.waveform.WaveformLayout.Format;

public class WaveformLayout implements LayoutManager {
	public enum Format {DEFAULT, TWOPARTS, FOURPARTS, FOURPARTS_RYTHM}
	public static final int AUTO_AMPLITUDE = -1;
	public static final float AUTO_SPEED = -1f;
	public static final int DEFAULT_AMPLITUDE = 10;
	public static final int DEFAULT_SPEED = 25;
	
	private WaveformPlugin plugin;	
	private LinkedHashMap<String, Component> components; //insertion order is important for layout
	private Layout layout;

	private float mmPerSecond = DEFAULT_SPEED;
	private int mmPerMillivolt = DEFAULT_AMPLITUDE;
	
	private Format format;	
    private int displayFactorWidth;
    private int displayFactorHeight;
    
	
	public WaveformLayout(WaveformPlugin plugin, Format format) {
		this.plugin = plugin;
		this.components = new LinkedHashMap<String, Component>();
		
		setFormat(format);
	}
	
	public void setSpeed(float mmPerSecond) {
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
	
	private Layout getMatchingLayout() {
		if(this.layout == null) {
			Layout[] layouts = {new StandardLayout(), new FallbackLayout()};
			for(Layout layout: layouts) {
				if(layout.matches(components.keySet())) {
					this.layout = layout;
					break;
				}
			}
		}
		
		return this.layout;
	}
	
	public List<Component> getOrderedComponents(Container parent) {
		return getMatchingLayout().getSortedComponents(format, components);
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
		double pixelPerMm = pixelPerInch/25.4 * plugin.getZoom();
		int secWidth = (int)(mmPerSecond*pixelPerMm);
		int mvHeight = (int)(mmPerMillivolt*pixelPerMm);

		double seconds = (format == Format.DEFAULT) ? plugin.getSeconds() : 10; 
		
		if(mmPerMillivolt == AUTO_AMPLITUDE) {
			//auto height, based on the scrollpane size
			double h = parent.getParent().getParent().getSize().height * plugin.getZoom();
			int auto = (int)(h / plugin.getChannelHeightInMillivolt() / displayFactorHeight);
			mvHeight = max((int)pixelPerMm, min(auto, (int)(DEFAULT_AMPLITUDE*pixelPerMm)));					
		}
		
		if(mmPerSecond == AUTO_SPEED) {
			//auto width, based on the scrollpane size
			double w = parent.getParent().getParent().getSize().width * plugin.getZoom();
			int auto = (int)(w / seconds);			
			secWidth = max((int)pixelPerMm, min(auto, (int)(DEFAULT_SPEED*pixelPerMm)));
		}
		
		dim.width += (int)(seconds*secWidth);			
		dim.height += displayFactorHeight * plugin.getChannelHeightInMillivolt()*mvHeight;
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

	public float getSpeed() {
		return mmPerSecond;
	}

	public float getAmplitude() {
		return mmPerMillivolt;
	}
}

interface Layout {
	boolean matches(Set<String> leadNames);
	List<Component> getSortedComponents(Format format, LinkedHashMap<String, Component> components);
}

class StandardLayout implements Layout {
	public boolean matches(Set<String> leadNames) {
		return leadNames.containsAll(Arrays.asList(
				"lead i", "lead v1", 
				"lead ii", "lead v2",
				"lead iii", "lead v3", 
				"lead avr", "lead v4", 
				"lead avl", "lead v5",
				"lead avf", "lead v6",
				"rythm"));
	}

	public 	List<Component> getSortedComponents(Format format, LinkedHashMap<String, Component> components) {
		List<Component> list = new ArrayList<Component>(components.size());

		String[] order = {};
		switch(format) {
			case TWOPARTS:
				order = new String[] {
						"lead i", "lead v1", 
						"lead ii", "lead v2",
						"lead iii", "lead v3", 
						"lead avr", "lead v4", 
						"lead avl", "lead v5",
						"lead avf", "lead v6"};
				break;
			case FOURPARTS:
				order = new String[] {					
						"lead i", "lead avr", "lead v1", "lead v4", 
						"lead ii", "lead avl", "lead v2", "lead v5",
						"lead iii", "lead avf", "lead v3",  "lead v6"};
				break;
			case FOURPARTS_RYTHM:
				order = new String[] {
						"lead i", "lead avr", "lead v1", "lead v4", 
						"lead ii", "lead avl", "lead v2", "lead v5",
						"lead iii", "lead avf", "lead v3",  "lead v6",
						"rythm"};
				break;
			default:
				order = new String[] {"lead i", "lead ii", "lead iii", 
						"lead avr","lead avl","lead avf", 
						"lead v1", "lead v2", "lead v3", 
						"lead v4", "lead v5", "lead v6"};
		}
				
		for(String lead: order) {
			list.add(components.get(lead));
		}
		
		return list;
	}	
}

class FallbackLayout implements Layout {
	public boolean matches(Set<String> leadNames) {
		//accept all
		return true;
	}

	public List<Component> getSortedComponents(Format format, LinkedHashMap<String, Component> components) {
		List<Component> insertorder = new ArrayList<Component>(components.size());
		Component rythm = null;
		//default, don't sort, separate rythm
		for(Component c: components.values()) {
			if((c instanceof DrawingPanel) && ((DrawingPanel)c).isRythm()) {
				rythm = c;
			} else {
				insertorder.add(c);
			}
		}
		
		//by default, rythm is the second lead
		if(rythm == null)
			rythm = insertorder.get(1);
		
		List<Component> list = new ArrayList<Component>(insertorder.size());		
		switch(format) {
			case TWOPARTS:
				for(int i=0;i<6;i++) {
					list.add(insertorder.get(i));
					list.add(insertorder.get(6+i));
				}
				break;
			case FOURPARTS:
				for(int i=0;i<3;i++) {
					list.add(insertorder.get(i));
					list.add(insertorder.get(3+i));
					list.add(insertorder.get(6+i));
					list.add(insertorder.get(9+i));
				}
				break;
			case FOURPARTS_RYTHM:
				for(int i=0;i<3;i++) {
					list.add(insertorder.get(i));
					list.add(insertorder.get(3+i));
					list.add(insertorder.get(6+i));
					list.add(insertorder.get(9+i));
				}
				list.add(rythm);
				break;
			default:
				list = insertorder;				
		}
		
		return list;
	}	
}