package dicomux.waveform;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Vector;

import javax.swing.BoxLayout;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import org.dcm4che2.data.DicomElement;
import org.dcm4che2.data.DicomObject;
import org.dcm4che2.data.Tag;

import dicomux.APlugin;
import dicomux.DicomException;

/**
 * This plug-in is for displaying waveform ecg data in a graphical way.
 * @author norbert
 */
public class WaveformPlugin extends APlugin {
	public static final double MAX_ZOOM_OUT = 1.0f;
	public static final double MAX_ZOOM_IN = 10.0f;
	public static final double ZOOM_UNIT = 0.5f;
	public static final double NO_ZOOM = 1.0f;
	
	public static final String DEFAULTFORMAT = "1x10s";
	public static final String FOURPARTS = "4x2.5s";
	public static final String FOURPARTSPLUS = "4x2.5s & RS";
	public static final String TWOPARTS = "2x5s";
	
	
	private Vector<DrawingPanel> pannels = new Vector<DrawingPanel>(12);
	private double zoomLevel;
	private int mvCells;
	private double seconds;
	private boolean fitToPage;
	private JScrollPane scroll;
	private JPanel channelpane;
	private int numberOfChannels;
	private String displayFormat;
	private ToolPanel tools;
	private InfoPanel infoPanel;
	private double frequency;
	private int numberOfSamples;		
	private int samplesPerSecond;
	private int data[][];
	private ChannelDefinition[] channelDefinitions;
	private boolean displayFormatChanged;
	private int displayFactorWidth;
	private int displayFactorHeight;
	
	private DrawingPanel rhythm;
	
	public WaveformPlugin() throws Exception {
		super();
		m_keyTag.addKey(Tag.Modality, "ECG");
		m_keyTag.addKey(Tag.WaveformSequence, null);
		m_keyTag.addKey(Tag.WaveformData, null);
		
		this.zoomLevel = NO_ZOOM;
		this.fitToPage = true;
		this.displayFormat = DEFAULTFORMAT;
		this.displayFormatChanged = false;
		this.rhythm = null;		
	}
	
	public String getName() {
		return "Waveform ECG";
	}
	
	private void readChannelDefinitions(DicomElement channelDef) throws DicomException {
		// read the ChannelDefinitionSequence for additional info about the channels
		if(channelDef == null) 
			throw new DicomException("Could not read ChannelDefinitionSequence");
		
		// iterate over the definitions of the channels
		this.channelDefinitions = new ChannelDefinition[numberOfChannels];
		for(int i = 0; i < channelDef.countItems(); i++) {
			DicomObject object = channelDef.getDicomObject(i);
			channelDefinitions[i] = ChannelDefinition.fromDicom(object);
		}
	}
	
	private void readData(DicomObject dcm) throws Exception {
		// read waveform data which contains the samples
		DicomElement waveformData = dcm.get(Tag.WaveformData);
		if(waveformData == null)
			throw new Exception("Could not read WaveformData");
				
		// read the number of allocated bits per sample 
		// used to differ between general ECG and 12 Lead ECG
		DicomElement bitsAllocated = dcm.get(Tag.WaveformBitsAllocated);
		if(bitsAllocated == null)
			throw new Exception("Could not read WaveformBitsAllocated");
		
		// write the sample data into a 2-dimensional array
		// first dimension: channel
		// second dimension: samples
		
		this.data = new int[numberOfChannels][numberOfSamples];		
		if(bitsAllocated.getInt(true) == 16) {
			
			boolean order = dcm.bigEndian();
			byte[] tmp_bytes = waveformData.getBytes();
			short[] tmp = toShort(tmp_bytes, order);
			
			if(tmp.length/numberOfChannels > numberOfSamples) {
				//XXX WARNING! What should we do when there is more data than declared ?
				//this.numberOfSamples = tmp.length/numberOfChannels;
				//this.seconds = numberOfSamples / frequency;
				//this.samplesPerSecond = (int)(numberOfSamples / seconds);
				//this.data = new int[numberOfChannels][numberOfSamples];
				System.err.println("WARNING: " + numberOfSamples + " samples, "
						+ seconds + " seconds, " +  samplesPerSecond + "sample/sec");
			}
			
			for (int i=0; i<tmp.length && i<numberOfChannels*numberOfSamples; i++) {
				data[i%numberOfChannels][i/numberOfChannels] = (int) tmp[i];
			}
		}
		else if(bitsAllocated.getInt(true) == 8)
		{
			byte[] tmp = waveformData.getBytes();
			for (int i = 0; i < tmp.length; i++ ) {
				data[i%numberOfChannels][i/numberOfChannels] = (int) tmp[i];
			}
		}
		else
			throw new Exception("bitsAllocated is an unexpected value, value: " + bitsAllocated.getInt(true));
		
		//apply baseline
		for(int i=0;i<data.length;i++) {
			ChannelDefinition def = channelDefinitions[i];
			for(int j=0;j<data[i].length;j++)
				data[i][j] += def.getBaseline();
		}
	}
	
	public void setData(DicomObject dcm) throws Exception {
		m_content = new JPanel(new BorderLayout());
		
		// get WaveformSequence
		DicomElement temp = dcm.get(Tag.WaveformSequence);
		if(temp == null)
			throw new Exception("Could not read WaveformSequence");
		
		dcm = temp.getDicomObject(0);
		
		// read the sampling frequency, used to calculate the seconds 
		DicomElement samplingFrequency = dcm.get(Tag.SamplingFrequency);
		if(samplingFrequency == null)
			throw new Exception("Could not read SamplingFrequency");
		
		this.frequency = samplingFrequency.getDouble(true);
		
		//read number of samples per channel
		DicomElement samples = dcm.get(Tag.NumberOfWaveformSamples);
		if(samples == null)
			throw new Exception("Could not read NumberOfWaveformSamples");
			
		
		// calculate the seconds		
		this.numberOfSamples = samples.getInt(true);
		this.seconds = numberOfSamples / frequency;
		this.samplesPerSecond = (int)(numberOfSamples / seconds);
		
		// read number of channels
		DicomElement channels = dcm.get(Tag.NumberOfWaveformChannels);
		if(channels == null)
			throw new Exception("Could not read NumberOfWaveformChannels");
			
		this.numberOfChannels = channels.getInt(true);
		
		readChannelDefinitions(dcm.get(Tag.ChannelDefinitionSequence));		
		readData(dcm);
				
		//get minmax
		getMinMax(data, channelDefinitions);
		
		
		// in most cases we have to many channels so we use a scrollpane
		this.scroll = new JScrollPane();
		scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		scroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
		
		// Panel which includes the Buttons for zooming 
		this.tools = new ToolPanel(this);
		this.tools.setLayout(new FlowLayout(FlowLayout.LEFT, 5, 5));
		this.tools.setPreferredSize(new Dimension(m_content.getWidth(), 30));
		
		// Panel with information about the channel the mouse cursor is over
		this.infoPanel = new InfoPanel();
		this.infoPanel.setPreferredSize(new Dimension(m_content.getWidth(), 70));
		
		displayDefault();
		
		JPanel wrap = new JPanel(new BorderLayout());
		wrap.add(tools, BorderLayout.NORTH);
		wrap.add(infoPanel, BorderLayout.CENTER);
		
		m_content.add(wrap, BorderLayout.NORTH);
		m_content.add(scroll, BorderLayout.CENTER);
		
		// this gets called when the application is resized
		m_content.addComponentListener(new ComponentAdapter() {	
			public void componentResized(ComponentEvent e) {
				repaintPanels();
			}
		});
	
		this.displayFactorWidth = 1;
		this.displayFactorHeight = this.numberOfChannels;
	}

	public void updateLanguage(String lang) {
		if(this.tools != null) {
			this.tools.updateLanguage();
		}
	}
	
	/**
	 * Convert an byte array to a short array
	 * 
	 * @param data				the array to convert
	 * @param isBigEndian		tell the convertion the byte order of the data 
	 *                          true if the bytes are in big endian order
	 *                          false if the bytes are in little endian order
	 * @return
	 */
	private short[] toShort(byte[] data, boolean isBigEndian) {
		
		short[] retdata = new short[data.length / 2];
		int pos = 0;
		ByteBuffer bb = ByteBuffer.allocate(2);
		if(isBigEndian)
		{
			bb.order(ByteOrder.BIG_ENDIAN);
		}
		else
		{
			bb.order(ByteOrder.LITTLE_ENDIAN);
		}
		
		for (int i = 0; i < data.length; ++i) {
			byte firstByte = data[i];
			byte secondByte = data[++i];
			
			bb.put(firstByte);
			bb.put(secondByte);
			retdata[pos] = bb.getShort(0);
			pos++;
			bb.clear();
		}
		
		return retdata;
	}
	
	
	/**
	 * Iterate over all ChannelPanels, set their size to the given Dimension and repaints them
	 */
	private void repaintPanels() {
		if(!this.pannels.isEmpty()) {

			if( this.numberOfChannels == 12 && displayFormatChanged) {
				if(displayFormat.equals(DEFAULTFORMAT)) {
					displayDefault();
					this.rhythm = null;
					displayFactorHeight = this.numberOfChannels;
					displayFactorWidth = 1;
				}
				if(displayFormat.equals(FOURPARTS)) {
					this.channelpane = displayFourParts(this.channelpane);
					this.scroll.setViewportView(this.channelpane); 
					this.rhythm = null;
					displayFactorWidth = 4;
					displayFactorHeight = 3;
				}
				if(displayFormat.equals(FOURPARTSPLUS)) {
					displayFourPartsPlus();
					displayFactorWidth = 4;
					displayFactorHeight = 4;
				}
				if(displayFormat.equals(TWOPARTS)) {
					displayTwoParts(); 
					this.rhythm = null;
					displayFactorWidth = 2;
					displayFactorHeight = 6;
				}
			}
			
			Dimension m_content_dim = m_content.getSize();
			double width = (m_content.getWidth()-3)/displayFactorWidth;
			double height = (m_content_dim.getHeight() - tools.getHeight() - infoPanel.getHeight()-5)/displayFactorHeight;

			
			if(!fitToPage)
			{
				width *= zoomLevel;
				height *= zoomLevel;
			}
			
			Dimension dim = new Dimension((int) width, (int)height);
			for (DrawingPanel p : this.pannels) {
				p.setPreferredSize(dim);
				p.setSize(dim);
				p.repaint();
			}
			
			if(this.rhythm != null) {
				dim = new Dimension((int)width*displayFactorWidth, (int) height);
				this.rhythm.setPreferredSize(dim);
				this.rhythm.setSize(dim);
				this.rhythm.repaint();
			}
			
			this.channelpane.repaint();
			this.scroll.revalidate();
			m_content.repaint();
		}
	}
	
	private void displayDefault() {
		 
		this.channelpane = new JPanel();
		channelpane.setBackground(Color.BLACK);
		channelpane.setLayout(new GridLayout(0, 1));
		
		// remove all panels, as we are about to create them again
		this.pannels.removeAllElements();
		
		// try to sort leads
		boolean sortable = true;
		int[][] temp_data = new int[this.data.length][this.data[0].length];
		ChannelDefinition[] temp_definitions = new ChannelDefinition[this.channelDefinitions.length];
		for (int i = 0; i < this.data.length; i++) {
			if(this.channelDefinitions[i].getName().equalsIgnoreCase("Lead I")) {
				temp_data[0] = this.data[i];
				temp_definitions[0] = this.channelDefinitions[i]; 
			}
			else if (this.channelDefinitions[i].getName().equalsIgnoreCase("Lead II")) {
				temp_data[1] = this.data[i];
				temp_definitions[1] = this.channelDefinitions[i];
			}
			else if (this.channelDefinitions[i].getName().equalsIgnoreCase("Lead III")) {
				temp_data[2] = this.data[i];
				temp_definitions[2] = this.channelDefinitions[i];
			}
			else if (this.channelDefinitions[i].getName().equalsIgnoreCase("Lead aVR")) {
				temp_data[3] = this.data[i];
				temp_definitions[3] = this.channelDefinitions[i];
			}
			else if (this.channelDefinitions[i].getName().equalsIgnoreCase("Lead aVL")) {
				temp_data[4] = this.data[i];
				temp_definitions[4] = this.channelDefinitions[i];
			}
			else if (this.channelDefinitions[i].getName().equalsIgnoreCase("Lead aVF")) {
				temp_data[5] = this.data[i];
				temp_definitions[5] = this.channelDefinitions[i];
			}
			else if (this.channelDefinitions[i].getName().equalsIgnoreCase("Lead V1")) {
				temp_data[6] = this.data[i];
				temp_definitions[6] = this.channelDefinitions[i];
			}
			else if (this.channelDefinitions[i].getName().equalsIgnoreCase("Lead V2")) {
				temp_data[7] = this.data[i];
				temp_definitions[7] = this.channelDefinitions[i];
			}
			else if (this.channelDefinitions[i].getName().equalsIgnoreCase("Lead V3")) {
				temp_data[8] = this.data[i];
				temp_definitions[8] = this.channelDefinitions[i];
			}
			else if (this.channelDefinitions[i].getName().equalsIgnoreCase("Lead V4")) {
				temp_data[9] = this.data[i];
				temp_definitions[9] = this.channelDefinitions[i];
			}
			else if (this.channelDefinitions[i].getName().equalsIgnoreCase("Lead V5")) {
				temp_data[10] = this.data[i];
				temp_definitions[10] = this.channelDefinitions[i];
			}
			else if (this.channelDefinitions[i].getName().equalsIgnoreCase("Lead V6")) {
				temp_data[11] = this.data[i];
				temp_definitions[11] = this.channelDefinitions[i];
			}
			else {
				//unknown lead, don't sort
				sortable = false;
				break;
			}
		}
		
		if(sortable) {
			this.data = temp_data;
			this.channelDefinitions = temp_definitions;
		}
		
		// creating the Panels for each channel 
		for(int i = 0; i < numberOfChannels; i++) {
			DrawingPanel drawPannel = new DrawingPanel(this, data[i], 0, channelDefinitions[i]);
			channelpane.add(drawPannel);
			this.pannels.add(drawPannel);
		}		
		
		this.scroll.setViewportView(this.channelpane); 
	}
	
	private void displayTwoParts() {
		this.channelpane = new JPanel();
		channelpane.setBackground(Color.BLACK);
		
		GridLayout layout = new GridLayout(6, 2);
		channelpane.setLayout(layout);
		
		this.pannels.removeAllElements();
		// sort Leads
		int[][] temp_data = new int[this.data.length][this.data[0].length];
		ChannelDefinition[] temp_definitions = new ChannelDefinition[this.channelDefinitions.length];
		for (int i = 0; i < this.data.length; i++) {
			if(this.channelDefinitions[i].getName().equalsIgnoreCase("Lead I")) {
				temp_data[0] = this.data[i];
				temp_definitions[0] = this.channelDefinitions[i]; 
			}
			else if (this.channelDefinitions[i].getName().equalsIgnoreCase("Lead V1")) {
				temp_data[1] = this.data[i];
				temp_definitions[1] = this.channelDefinitions[i];
			}
			else if (this.channelDefinitions[i].getName().equalsIgnoreCase("Lead II")) {
				temp_data[2] = this.data[i];
				temp_definitions[2] = this.channelDefinitions[i];
			}
			else if (this.channelDefinitions[i].getName().equalsIgnoreCase("Lead V2")) {
				temp_data[3] = this.data[i];
				temp_definitions[3] = this.channelDefinitions[i];
			}
			else if (this.channelDefinitions[i].getName().equalsIgnoreCase("Lead III")) {
				temp_data[4] = this.data[i];
				temp_definitions[4] = this.channelDefinitions[i];
			}
			else if (this.channelDefinitions[i].getName().equalsIgnoreCase("Lead V3")) {
				temp_data[5] = this.data[i];
				temp_definitions[5] = this.channelDefinitions[i];
			}
			else if (this.channelDefinitions[i].getName().equalsIgnoreCase("Lead aVR")) {
				temp_data[6] = this.data[i];
				temp_definitions[6] = this.channelDefinitions[i];
			}
			else if (this.channelDefinitions[i].getName().equalsIgnoreCase("Lead V4")) {
				temp_data[7] = this.data[i];
				temp_definitions[7] = this.channelDefinitions[i];
			}
			else if (this.channelDefinitions[i].getName().equalsIgnoreCase("Lead aVL")) {
				temp_data[8] = this.data[i];
				temp_definitions[8] = this.channelDefinitions[i];
			}
			else if (this.channelDefinitions[i].getName().equalsIgnoreCase("Lead V5")) {
				temp_data[9] = this.data[i];
				temp_definitions[9] = this.channelDefinitions[i];
			}
			else if (this.channelDefinitions[i].getName().equalsIgnoreCase("Lead aVF")) {
				temp_data[10] = this.data[i];
				temp_definitions[10] = this.channelDefinitions[i];
			}
			else if (this.channelDefinitions[i].getName().equalsIgnoreCase("Lead V6")) {
				temp_data[11] = this.data[i];
				temp_definitions[11] = this.channelDefinitions[i];
			}
		}
		this.data = temp_data;
		this.channelDefinitions = temp_definitions;
		
		double start = 0;
		for(int i = 0; i < numberOfChannels; i++) {
			if(i != 0 && (i % 2) != 0)
			{
				start = 5.0;
			}
			else
			{
				start = 0;
			}
			DrawingPanel drawPannel = new DrawingPanel(this, data[i], start, channelDefinitions[i]);
			channelpane.add(drawPannel);
			// add panel to vector
			this.pannels.add(drawPannel);
		}
		this.scroll.setViewportView(this.channelpane); 
		
	}
	
	private JPanel displayFourParts(JPanel pane) {
		pane = new JPanel();
		pane.setBackground(Color.BLACK);
		
		GridLayout layout = new GridLayout(3, 4);
		pane.setLayout(layout);
		
		this.pannels.removeAllElements();
		// sort Leads
		int[][] temp_data = new int[this.data.length][this.data[0].length];
		ChannelDefinition[] temp_definitions = new ChannelDefinition[this.channelDefinitions.length];
		for (int i = 0; i < this.data.length; i++) {
			if(this.channelDefinitions[i].getName().equalsIgnoreCase("Lead I")) {
				temp_data[0] = this.data[i];
				temp_definitions[0] = this.channelDefinitions[i]; 
			}
			else if (this.channelDefinitions[i].getName().equalsIgnoreCase("Lead aVR")) {
				temp_data[1] = this.data[i];
				temp_definitions[1] = this.channelDefinitions[i];
			}
			else if (this.channelDefinitions[i].getName().equalsIgnoreCase("Lead V1")) {
				temp_data[2] = this.data[i];
				temp_definitions[2] = this.channelDefinitions[i];
			}
			else if (this.channelDefinitions[i].getName().equalsIgnoreCase("Lead V4")) {
				temp_data[3] = this.data[i];
				temp_definitions[3] = this.channelDefinitions[i];
			}
			else if (this.channelDefinitions[i].getName().equalsIgnoreCase("Lead II")) {
				temp_data[4] = this.data[i];
				temp_definitions[4] = this.channelDefinitions[i];
			}
			else if (this.channelDefinitions[i].getName().equalsIgnoreCase("Lead aVL")) {
				temp_data[5] = this.data[i];
				temp_definitions[5] = this.channelDefinitions[i];
			}
			else if (this.channelDefinitions[i].getName().equalsIgnoreCase("Lead V2")) {
				temp_data[6] = this.data[i];
				temp_definitions[6] = this.channelDefinitions[i];
			}
			else if (this.channelDefinitions[i].getName().equalsIgnoreCase("Lead V5")) {
				temp_data[7] = this.data[i];
				temp_definitions[7] = this.channelDefinitions[i];
			}
			else if (this.channelDefinitions[i].getName().equalsIgnoreCase("Lead III")) {
				temp_data[8] = this.data[i];
				temp_definitions[8] = this.channelDefinitions[i];
			}
			else if (this.channelDefinitions[i].getName().equalsIgnoreCase("Lead aVF")) {
				temp_data[9] = this.data[i];
				temp_definitions[9] = this.channelDefinitions[i];
			}
			else if (this.channelDefinitions[i].getName().equalsIgnoreCase("Lead V3")) {
				temp_data[10] = this.data[i];
				temp_definitions[10] = this.channelDefinitions[i];
			}
			else if (this.channelDefinitions[i].getName().equalsIgnoreCase("Lead V6")) {
				temp_data[11] = this.data[i];
				temp_definitions[11] = this.channelDefinitions[i];
			}
		}
		this.data = temp_data;
		this.channelDefinitions = temp_definitions;
		
		double start = 0;
		
		for(int i = 0; i < numberOfChannels; i++) {
			
			switch(i) {
				case 0:
				case 4:
				case 8:
					start = 0;
					break;
				case 1:
				case 5:
				case 9:
					start = 2.5;
					break;
				case 2:
				case 6:
				case 10:
					start = 5.0;
					break;
				case 3:
				case 7:
				case 11:
					start = 7.5;
					break;
			}
			
			DrawingPanel drawPannel = new DrawingPanel(this, data[i], start, channelDefinitions[i]);
			pane.add(drawPannel);
			// add panel to vector
			this.pannels.add(drawPannel);
		}
		return pane;
	}
	
	private void displayFourPartsPlus() {		
		JPanel pane = new JPanel();
		
		pane = displayFourParts(pane);
		
		this.channelpane = new JPanel();
		this.channelpane.setLayout(new BoxLayout(channelpane, BoxLayout.PAGE_AXIS));
		this.channelpane.setBackground(Color.BLACK);
		
		int rhythm_index = 0;
		for (int i = 0; i < this.channelDefinitions.length; i++) {
			if(channelDefinitions[i].getName().equalsIgnoreCase("Lead II")) {
				rhythm_index = i;
			}
		}
		
		this.rhythm = new DrawingPanel(this, data[rhythm_index], 0, channelDefinitions[rhythm_index]);
		this.rhythm.setRhythm(true);
		
		this.channelpane.add(pane);
		this.channelpane.add(this.rhythm);
		
		this.scroll.setViewportView(this.channelpane); 
		
	}
	
	private void getMinMax(int data[][], ChannelDefinition definitions[]) {
						
			for(int i = 0; i < data.length; i++) {
				double min = 0;
				double max = 0;
				double scalingValue = definitions[i].getScaling();
				for(int j = 0; j < data[i].length; j++) {
					if(min > data[i][j] * scalingValue)
					{
						min = data[i][j] * scalingValue;
					}
					if(max < data[i][j] * scalingValue)
					{
						max = data[i][j] * scalingValue;
					}
				}
				definitions[i].setMaximum_uV(max);
				definitions[i].setMinimum_uV(min);
			}
			
			double max = Double.MIN_VALUE;
			double min = Double.MAX_VALUE;
			for(int i = 0; i < definitions.length; i++) {
				if(min > definitions[i].getMinimum_uV()){
					min = definitions[i].getMinimum_uV();
				}
				if(max < definitions[i].getMaximum_uV()) {
					max = definitions[i].getMaximum_uV();
				}
			}
			
			double minmax_uV = Math.max(Math.abs(max), Math.abs(min));
			double minmax_mV = minmax_uV/1000;
			this.mvCells = (int)Math.ceil(minmax_mV) * 2;
	}

	//--
	
	public void resetZoom() {
		zoomLevel = NO_ZOOM;
		fitToPage = true;		
		repaintPanels();
	}
	
	public void decreaseZoomLevel() {
		if(zoomLevel <= MAX_ZOOM_OUT)
			return;
			
		zoomLevel -= ZOOM_UNIT;
		fitToPage = false;
		repaintPanels();
	}
	
	public void increaseZoomLevel() {
		if(zoomLevel >= WaveformPlugin.MAX_ZOOM_IN)
			return;
		
		zoomLevel += WaveformPlugin.ZOOM_UNIT;		
		fitToPage = false;		
		repaintPanels();
	}
	
	public void setDisplayFormat(String format) {
		displayFormat = format;
		displayFormatChanged = true;
		repaintPanels();
		displayFormatChanged = false;
	}
	
	//-- getters & setters
	
	public int getNumberOfChannels() {
		return numberOfChannels;
	}

	public int getMvCells() {
		return mvCells;
	}

	public double getSeconds() {
		return seconds;
	}

	public int getSamplesPerSecond() {
		return samplesPerSecond;
	}

	public String getDisplayFormat() {
		return displayFormat;
	}

	public InfoPanel getInfoPanel() {
		return infoPanel;
	}
	
	
}
