package dicomux.query;

import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import org.dcm4che2.data.DicomObject;
import org.dcm4che2.data.Tag;

import com.michaelbaranov.microba.calendar.DatePicker;

import dicomux.IController;
import dicomux.Translation;
import dicomux.query.web.DicomWebQuery;
import dicomux.settings.Settings;

public class QueryPanel extends JPanel {
	private IController controller;
	private JTextField patientId;
	private JTextField patientLastName;
	private JTextField patientFirstName;
	private DatePicker fromDate;
	private DatePicker toDate;
	private JButton search;	
	private JSplitPane split;
	private JLabel noResult;
	private JTable result;
	
	
	
	public QueryPanel(final IController controller) {
		super(new BorderLayout());
		
		this.controller = controller;
		this.patientId = new JTextField();
		this.patientLastName = new JTextField(); 
		this.patientFirstName = new JTextField(); 
		this.fromDate = new DatePicker(null); 
		this.toDate = new DatePicker(null);
		this.search = new JButton(new ImageIcon(this.getClass().getClassLoader().getResource("images/system-search.png")));
		this.noResult = new JLabel();
		noResult.setVerticalAlignment(JLabel.TOP);
		this.result = new JTable();	
		result.setModel(new DicomTableModel());
		result.getColumnModel().getColumn(0).setMaxWidth(20);
		
		KeyListener searchOnEnterPressed = new KeyAdapter() {
			public void keyPressed(KeyEvent e) {
				if(e.getKeyCode() == KeyEvent.VK_ENTER)
					search.doClick();
			}
		};
		patientId.addKeyListener(searchOnEnterPressed);
		patientLastName.addKeyListener(searchOnEnterPressed);
		patientFirstName.addKeyListener(searchOnEnterPressed);
		fromDate.addKeyListener(searchOnEnterPressed);
		toDate.addKeyListener(searchOnEnterPressed);		
		
		this.search.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				SwingUtilities.invokeLater(new Runnable() {
					public void run() {
						runQuery();	
					}
				});					
			}
		});
		
		this.result.addMouseListener(new MouseAdapter() {
			public void mouseClicked(MouseEvent e) {
				if(e.getClickCount() < 2 || e.getButton() != MouseEvent.BUTTON1)
					return;
				
				int row = result.getSelectedRow();
				if(row < 0)
					return;
				
				DicomObject selected = ((DicomTableModel)result.getModel()).getObjectAt(row);

				try {
					final URL url = getWadoUrl(selected);
					setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
					SwingUtilities.invokeLater(new Runnable() {					
						public void run() {
							controller.openDicomURL(url);		
							setCursor(Cursor.getDefaultCursor());
						}
					});							
				} catch (MalformedURLException ex) {
					controller.showErrorMessage("WADO configuration error: " + ex.getMessage());
					ex.printStackTrace();
					return;
				}
				 
			}
		});
		
		JPanel form = buildForm();
		split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, form, new JLabel());
		split.setBorder(BorderFactory.createEmptyBorder());
		split.setOneTouchExpandable(true);
		add(split, BorderLayout.CENTER);	
		
		autoSearch();		
	}
	
	private void autoSearch() {
		String patientId = controller.getSettings().get("pacs.patientId");
		if(patientId != null && patientId.length() > 0) {
			this.patientId.setText(patientId);
			this.search.doClick();
		}
	}
	
	private URL getWadoUrl(DicomObject selected) throws MalformedURLException  {
		String wadoUrlPattern = controller.getSettings().get("dicomux.pacs.wado");
		String appKey = controller.getSettings().get("intrahus.appkey");
		String wadoUrl = wadoUrlPattern
			.replace("${studyUID}", selected.getString(Tag.StudyInstanceUID))
			.replace("${seriesUID}", selected.getString(Tag.SeriesInstanceUID))
			.replace("${objectUID}", selected.getString(Tag.SOPInstanceUID));
		
		try {
			wadoUrl += "&appkey=" + URLEncoder.encode(appKey, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			//should not happen, utf-8 is standard. 			
		}
		
		return new URL(wadoUrl);
	}
	
	private void runQuery() {		
		Settings settings = controller.getSettings();
		
		DicomQuery query = null;
		String qbroker = settings.get("dicomux.qbroker");
		if(qbroker != null && !qbroker.isEmpty()) {
			String appKey = settings.get("intrahus.appkey");
			String pacsId = settings.get("dicomux.qbroker.pacsId");
			try {
				URL url = new URL(qbroker);
				query = new DicomWebQuery(appKey, pacsId, url);
			} catch(MalformedURLException e) {
				noResult.setText("Error: " + e);
				split.setBottomComponent(noResult);
				e.printStackTrace();
				return;
			}
		} else {
			query = new DicomQuery();
		}

		
		query.setLocalInfos(settings.get("dicomux.local.aet"));
		query.setRemoteInfos(
				settings.get("dicomux.pacs.aet"),
				settings.get("dicomux.pacs.host"),
				settings.getInt("dicomux.pacs.port"));
		
		String pid = null;
		if(! patientId.getText().trim().isEmpty())
			pid = patientId.getText().trim();
		
		String pname = null;
		if(! patientLastName.getText().trim().isEmpty())
			pname = patientLastName.getText().trim() + "*";
		if(! patientFirstName.getText().trim().isEmpty()) {
			if(pname == null)
				pname = "*^";
			
			pname += patientFirstName.getText().trim() + "*";
		}
		

		String dateRange = null;
		Date from = fromDate.getDate();
		Date to = toDate.getDate();
		
		//only one date specified, take a 3 month range starting or ending at this date
		if(from != null && to == null) {
			Calendar cal = Calendar.getInstance();
			cal.setTime(from);
			cal.add(Calendar.MONTH, 3);
			to = cal.getTime();
		} else if(from == null && to != null) {
			Calendar cal = Calendar.getInstance();
			cal.setTime(to);
			cal.add(Calendar.MONTH, -3);
			from = cal.getTime();
		}
					
		//a date range is specified, format
		if(from != null && to != null) {
			DateFormat dicomDateFormat = new SimpleDateFormat("yyyyMMdd");
		
			dateRange = dicomDateFormat.format(from);
			dateRange += "-";
			dateRange += dicomDateFormat.format(to);
		}
		
		try {
			query.connect();
			List<DicomObject> objects = query.query(pid, pname, dateRange);
			
			if(objects.isEmpty()) {
				noResult.setText(Translation.tr("query.noResult"));
				split.setBottomComponent(noResult);
			} else {
				split.setBottomComponent(new JScrollPane(result));
				result.setModel(new DicomTableModel(objects));
				result.getColumnModel().getColumn(0).setMaxWidth(20);
			}
			query.close();
		} catch(Exception e) {
			controller.showErrorMessage(e.getMessage());
			e.printStackTrace();
		}
	}
	
	private JPanel buildForm() {
		JPanel panel = new JPanel(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		Insets noPadding = new Insets(0,0,0,0);
		Insets rightPadding = new Insets(0,0,0,10);
				
		c.gridy = 0;
		c.gridx = 0;
		c.weightx = 0;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.insets = noPadding;
		panel.add(new JLabel(Translation.tr("query.patientId")), c);

		c.gridx = 1;
		c.weightx = 1;
		c.insets = rightPadding;
		panel.add(patientId, c);

		c.gridx = 2;
		c.weightx = 0;
		c.insets = noPadding;
		panel.add(new JLabel(Translation.tr("query.lastName")), c);
		
		c.gridx = 3;
		c.weightx = 1;
		c.insets = rightPadding;
		panel.add(patientLastName, c);
		
		c.gridx = 4;
		c.weightx = 0;
		c.insets = noPadding;
		panel.add(new JLabel(Translation.tr("query.firstName")), c);
		
		c.gridx = 5;
		c.weightx = 1;
		c.insets = rightPadding;
		panel.add(patientFirstName, c);

		c.gridy = 1;
		c.gridx = 0;
		c.weightx = 0;
		c.insets = noPadding;		
		panel.add(new JLabel(Translation.tr("query.dateFrom")), c);
		
		c.gridx = 1;
		c.weightx = 1;
		c.insets = rightPadding;
		panel.add(fromDate, c);
		
		c.gridx = 2;
		c.weightx = 0;
		c.insets = noPadding;		
		panel.add(new JLabel(Translation.tr("query.dateTo")), c);
		
		c.gridx = 3;
		c.weightx = 1;
		c.insets = rightPadding;
		panel.add(toDate, c);
		
		c.gridx = 6;
		c.gridy = 0;
		c.gridheight = 2;
		c.fill = GridBagConstraints.BOTH;
		c.weightx = 0;
		panel.add(search, c);	
		return panel;
	}
}
