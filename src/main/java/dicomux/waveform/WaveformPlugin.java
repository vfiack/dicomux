package dicomux.waveform;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import javax.swing.JPanel;
import javax.swing.JScrollPane;

import org.dcm4che2.data.DicomElement;
import org.dcm4che2.data.DicomObject;
import org.dcm4che2.data.Tag;

import dicomux.APlugin;
import dicomux.DicomException;
import dicomux.waveform.WaveformLayout.Format;

/**
 * This plug-in is for displaying waveform ecg data in a graphical way.
 * @author norbert
 */
public class WaveformPlugin extends APlugin {	
	private int mvCells;
	private double seconds;
	private JScrollPane scroll;
	private JPanel channelpane;
	private int numberOfChannels;
	private Format displayFormat;
	private ToolPanel tools;
	private InfoPanel infoPanel;
	private double frequency;
	private int numberOfSamples;		
	private int samplesPerSecond;
	private int data[][];
	private ChannelDefinition[] channelDefinitions;
	
	private WaveformLayout waveformLayout;
	private int mmPerSecond = 25;
	private int mmPerMillivolt = 10;
	
	public WaveformPlugin() throws Exception {
		super();
		m_keyTag.addKey(Tag.Modality, "ECG");
		m_keyTag.addKey(Tag.WaveformSequence, null);
		m_keyTag.addKey(Tag.WaveformData, null);
		
		this.displayFormat = Format.DEFAULT;
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

		this.waveformLayout = new WaveformLayout(this, Format.DEFAULT);
		this.channelpane = new JPanel(waveformLayout);
		JPanel channelwrap = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
		channelwrap.add(channelpane);
		this.scroll.setViewportView(channelwrap); 

		
		// Panel which includes the Buttons for zooming 
		this.tools = new ToolPanel(this);
		this.tools.setLayout(new FlowLayout(FlowLayout.LEFT, 5, 5));
		this.tools.setPreferredSize(new Dimension(m_content.getWidth(), 30));
		
		// Panel with information about the channel the mouse cursor is over
		this.infoPanel = new InfoPanel();
		this.infoPanel.setPreferredSize(new Dimension(m_content.getWidth(), 70));
		
		resetZoom();
		displayDefault();
		
		JPanel wrap = new JPanel(new BorderLayout());
		wrap.add(tools, BorderLayout.NORTH);
		wrap.add(infoPanel, BorderLayout.CENTER);
		
		m_content.add(wrap, BorderLayout.NORTH);
		m_content.add(scroll, BorderLayout.CENTER);
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
	
	private void displayDefault() {		 
		this.waveformLayout.setFormat(Format.DEFAULT);
		this.channelpane.removeAll();
		
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
		}		
		
	}
	
	private void displayTwoParts() {
		this.waveformLayout.setFormat(Format.TWOPARTS);
		this.channelpane.removeAll();
		
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
		}
	}
	
	private void displayFourParts() {
		this.waveformLayout.setFormat(Format.FOURPARTS);
		this.channelpane.removeAll();

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
			channelpane.add(drawPannel);
		}
	}
	
	private void displayFourPartsPlus() {		
		displayFourParts();
		this.waveformLayout.setFormat(Format.FOURPARTS_RYTHM);
				
		int rhythm_index = 0;
		for (int i = 0; i < this.channelDefinitions.length; i++) {
			if(channelDefinitions[i].getName().equalsIgnoreCase("Lead II")) {
				rhythm_index = i;
			}
		}
		
		DrawingPanel rhythm = new DrawingPanel(this, data[rhythm_index], 0, channelDefinitions[rhythm_index]);
		rhythm.setRhythm(true);
		
		this.channelpane.add(rhythm);
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
		this.mmPerMillivolt = 10;
		this.mmPerSecond = 25;
		
		this.waveformLayout.setAmplitude(mmPerMillivolt);
		this.waveformLayout.setSpeed(mmPerSecond);
		this.channelpane.revalidate();
	}
	
	public void decreaseZoomLevel() {
		if(this.mmPerMillivolt <= 5)
			return;
		
		this.mmPerMillivolt -= 5;
		this.mmPerSecond -= 10;
		
		this.waveformLayout.setAmplitude(mmPerMillivolt);
		this.waveformLayout.setSpeed(mmPerSecond);
		this.channelpane.revalidate();
	}
	
	public void increaseZoomLevel() {
		if(this.mmPerMillivolt >= 25)
			return;
		
		this.mmPerMillivolt += 5;
		this.mmPerSecond += 10;
		
		this.waveformLayout.setAmplitude(mmPerMillivolt);
		this.waveformLayout.setSpeed(mmPerSecond);
		this.channelpane.revalidate();
	}
	
	public void setDisplayFormat(Format format) {
		displayFormat = format;
		if(displayFormat == Format.DEFAULT)
			displayDefault();
		else if(displayFormat == Format.FOURPARTS)
			displayFourParts();
		else if(displayFormat == Format.FOURPARTS_RYTHM)
			displayFourPartsPlus();
		else if(displayFormat == Format.TWOPARTS)
			displayTwoParts(); 
		
		this.channelpane.revalidate();
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

	public Format getDisplayFormat() {
		return displayFormat;
	}

	public InfoPanel getInfoPanel() {
		return infoPanel;
	}	
}