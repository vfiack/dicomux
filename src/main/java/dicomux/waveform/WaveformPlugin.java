package dicomux.waveform;

import static dicomux.Translation.tr;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Toolkit;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.List;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;

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
public class WaveformPlugin extends APlugin implements Printable {	
	private DicomObject dicomObject;
	private double channelHeightInMillivolt;
	private double seconds;
	private JScrollPane scroll;
	private JPanel channelpane;
	private int numberOfChannels;
	private Format displayFormat;
	private ToolPanel tools;
	private double frequency;
	private int numberOfSamples;		
	private int samplesPerSecond;
	private int data[][];
	private ChannelDefinition[] channelDefinitions;
	private Annotations annotations;
	
	private double zoom = 1;
	private WaveformLayout waveformLayout;
	private Tool selectedTool;

	public WaveformPlugin() throws Exception {
		super();
		m_keyTag.addKey(Tag.Modality, "ECG");
		m_keyTag.addKey(Tag.WaveformSequence, null);
		m_keyTag.addKey(Tag.WaveformData, null);
		
		this.displayFormat = Format.DEFAULT;
		this.selectedTool = Tool.VERTICAL_MEASURE;
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
		this.dicomObject = dcm;
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
		final JPanel channelwrap = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
		channelwrap.add(channelpane);
		channelwrap.addMouseWheelListener(new MouseWheelListener() {			
			public void mouseWheelMoved(MouseWheelEvent e) {
				int notches = e.getWheelRotation();
				double zoom = getZoom() - (0.2)*notches;
				setZoom(zoom);
				//TODO: center the scrollpane where the cursor was
			}
		});
		
		this.scroll.setViewportView(channelwrap); 
		
		AdjustmentListener adjustmentListener = new AdjustmentListener() {			
			public void adjustmentValueChanged(AdjustmentEvent e) {
				channelwrap.repaint();
			}
		};
		
		this.scroll.getHorizontalScrollBar().addAdjustmentListener(adjustmentListener);
		this.scroll.getVerticalScrollBar().addAdjustmentListener(adjustmentListener);
		

		this.annotations = new Annotations(dicomObject);

		
		// Panel which includes the Buttons for zooming 
		this.tools = new ToolPanel(this);
		this.tools.setLayout(new FlowLayout(FlowLayout.LEFT, 5, 5));
		this.tools.setPreferredSize(new Dimension(m_content.getWidth(), 30));
		
		addDrawingPanels();
		
		Format format = numberOfChannels == 12 ? Format.FOURPARTS_RYTHM : Format.DEFAULT;
		setDisplayFormat(format);
		
		final JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
				scroll, annotations); 
		split.setOneTouchExpandable(true);
		
		split.addPropertyChangeListener(JSplitPane.DIVIDER_LOCATION_PROPERTY, new PropertyChangeListener() {
			public void propertyChange(PropertyChangeEvent evt) {
				channelpane.revalidate();
			}
		});
		
		
		m_content.add(tools, BorderLayout.NORTH);
		m_content.add(split, BorderLayout.CENTER);
		m_content.addComponentListener(new ComponentAdapter() {
			public void componentResized(ComponentEvent e) {
				int rightSize = 230;
				int toggleLeft = 1;
				int toggleRight = split.getWidth() - split.getDividerSize() - 1;
				
				if(split.getDividerLocation() > toggleLeft && split.getDividerLocation() < toggleRight)
					split.setDividerLocation(split.getWidth()-rightSize);
				else
					split.setLastDividerLocation(split.getWidth()-rightSize);
				
				channelpane.revalidate();
			}
		});
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
	
	private void addDrawingPanels() {
		for (int i = 0; i < this.channelDefinitions.length; i++) {
			DrawingPanel panel = new DrawingPanel(this, data[i], channelDefinitions[i]);
			channelpane.add(channelDefinitions[i].getName(), panel);		
			
			/*
			//XXX Annotations a controller par une combobox / des boutons
			List<Annotation> panelAnnotations = new ArrayList<Annotation>();
			for(Annotation a: annotations.getAnnotations()) {
				if(String.valueOf(i+1).equals(a.channel)) {
					if("QRS Onset".equals(a.name) || "QRS Offset".equals(a.name))
						panelAnnotations.add(a);
				}
			}
			panel.setAnnotations(panelAnnotations);
			*/
			
			if(channelDefinitions[i].getName().equalsIgnoreCase("Lead II")) {
				DrawingPanel rhythm = new DrawingPanel(this, data[i], channelDefinitions[i]);
				rhythm.setRhythm(true);
				channelpane.add("rythm", rhythm);
			}
		}
	}
	
	private void displayDefault() {		 
		this.waveformLayout.setFormat(Format.DEFAULT);
		for(Component c: channelpane.getComponents())
			((DrawingPanel)c).setTimeLength(Integer.MAX_VALUE);
		
		this.channelpane.revalidate();
	}
	
	private void displayTwoParts() {
		this.waveformLayout.setFormat(Format.TWOPARTS);
		
		List<Component> ordered = waveformLayout.getOrderedComponents(channelpane);
		for(int i = 0; i < numberOfChannels; i++) {
			DrawingPanel drawPannel = (DrawingPanel)ordered.get(i);
			drawPannel.setTimeLength(5);
		}
		
		this.channelpane.revalidate();
	}
	
	private void displayFourParts() {
		this.waveformLayout.setFormat(Format.FOURPARTS);
		
		List<Component> ordered = waveformLayout.getOrderedComponents(channelpane);
		for(int i = 0; i < numberOfChannels; i++) {
			DrawingPanel drawPannel = (DrawingPanel)ordered.get(i);
			drawPannel.setTimeLength(2.5);
		}
		
		this.channelpane.revalidate();
	}
	
	private void displayFourPartsPlus() {		
		displayFourParts();
		this.waveformLayout.setFormat(Format.FOURPARTS_RYTHM);
		this.channelpane.revalidate();
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
			double margin = 0.3; //small margin to avoid having the graph touching the channel border
			this.channelHeightInMillivolt = minmax_mV * 2 + margin;
	}

	//--
	
	public void setSpeed(float mmPerSecond) {
		this.waveformLayout.setSpeed(mmPerSecond);
		this.channelpane.revalidate();
	}
	
	public float getSpeed() {
		return waveformLayout.getSpeed();
	}
	
	public void setAmplitude(int mmPerMillivolt) {
		this.waveformLayout.setAmplitude(mmPerMillivolt);		
		this.channelpane.revalidate();
	}
	
	public int getAmplitude() {
		return (int)waveformLayout.getAmplitude();
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
		
		this.tools.selectDisplayFormat(format);
		this.channelpane.revalidate();
	}
	
	public void setZoom(double zoom) {
		this.zoom = Math.max(zoom, 0.5);
		channelpane.revalidate();
	}
	
	public void setSelectedTool(Tool tool) {
		selectedTool = tool;

		for(Component c: channelpane.getComponents())
			((DrawingPanel)c).selectedToolChanged(tool);

		channelpane.repaint();
	}
	
	public void removeAllMarkers() {
		for(Component c: channelpane.getComponents())
			((DrawingPanel)c).removeMarkers();

		channelpane.repaint();
	}
	
	//--
	
	public int print(Graphics g, PageFormat pf, int page) throws PrinterException {
		if(page > 0)
			return NO_SUCH_PAGE;
		
		for(Component c: waveformLayout.getOrderedComponents(channelpane)) {
			c.setBackground(Color.WHITE);
			((DrawingPanel)c).setHighlightedSample(-1);
		}

		Graphics2D g2d = (Graphics2D)g;
		g2d.setColor(Color.BLACK);
		g2d.setFont(Font.decode("Arial-PLAIN-10"));
	
		//patient info
		g2d.drawString(tr("print.patientName") 
				+ annotations.getAnnotation("patient name").value.replace("^", " ")
				+ "        " + tr("print.patientSex")
				+ annotations.getAnnotation("patient sex").value
				+ "        " + tr("print.birthDate")
				+ annotations.getAnnotation("birth date").value
				+ "        " + tr("print.patientId")
				+ dicomObject.getString(Tag.PatientID)
				, 40, 25);
				
		//study info
		String studyDescription = dicomObject.getString(Tag.StudyDescription);
		if(studyDescription == null)
			studyDescription = "";
		if(studyDescription.length() > 80)
			studyDescription = studyDescription.substring(0, 80) + "[...]";
		
		
		g2d.drawString(tr("print.studyDate")
				+ dicomObject.getString(Tag.StudyDate)
				+ "        " + tr("print.studyTime")
				+ dicomObject.getString(Tag.StudyTime)
				+ "        " + tr("print.studyId")
				+ dicomObject.getString(Tag.StudyID)
				+ "        " + tr("print.description")
				+ studyDescription
				, 40, 35);		
		
		//so that the paper measurement are corrects
		int pixelPerInch = Toolkit.getDefaultToolkit().getScreenResolution();
		double dotsPerPixel = 72.0 / pixelPerInch; 				
        g2d.scale(dotsPerPixel, dotsPerPixel); 
        
        //margins
        g2d.translate(50, 60);
        
        channelpane.printComponents(g2d);
 
        return PAGE_EXISTS;		
	}
	
	//-- getters & setters
		
	public DicomObject getDicomObject() {
		return dicomObject;
	}
	
	public int getNumberOfChannels() {
		return numberOfChannels;
	}

	public double getChannelHeightInMillivolt() {
		return channelHeightInMillivolt;
	}

	public double getSeconds() {
		return seconds;
	}

	public int getSamplesPerSecond() {
		return samplesPerSecond;
	}
	
	public double getZoom() {
		return zoom;
	}

	public Format getDisplayFormat() {
		return displayFormat;
	}
	
	public Annotations getAnnotations() {
		return annotations;
	}

	public Tool getSelectedTool() {
		return selectedTool;
	}
}
