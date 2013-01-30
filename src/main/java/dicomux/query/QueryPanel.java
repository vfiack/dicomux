package dicomux.query;

import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.net.MalformedURLException;
import java.net.URL;
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

import dicomux.IController;
import dicomux.settings.Settings;

public class QueryPanel extends JPanel {
	private IController controller;
	private JTextField patientId;
	private JTextField patientLastName;
	private JTextField patientFirstName;
	private JTextField fromDate;
	private JTextField toDate;
	private JButton search;
	private JTable result;
	
	public QueryPanel(final IController controller) {
		super(new BorderLayout());
				
		this.controller = controller;
		this.patientId = new JTextField();
		this.patientLastName = new JTextField(); 
		this.patientFirstName = new JTextField(); 
		this.fromDate = new JTextField(); 
		this.toDate = new JTextField();
		this.search = new JButton(new ImageIcon(this.getClass().getClassLoader().getResource("images/system-search.png")));
		this.result = new JTable();
		result.setModel(new DicomTableModel());
		
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
		JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, form, new JScrollPane(result));
		split.setBorder(BorderFactory.createEmptyBorder());
		split.setOneTouchExpandable(true);
		add(split, BorderLayout.CENTER);
	}
	
	private URL getWadoUrl(DicomObject selected) throws MalformedURLException  {
		String wadoUrlPattern = controller.getSettings().get("dicomux.pacs.wado");
		String wadoUrl = wadoUrlPattern
			.replace("${studyUID}", selected.getString(Tag.StudyInstanceUID))
			.replace("${seriesUID}", selected.getString(Tag.SeriesInstanceUID))
			.replace("${objectUID}", selected.getString(Tag.SOPInstanceUID));

		return new URL(wadoUrl);
	}
	
	private void runQuery() {
		DicomQuery query = new DicomQuery();
		
		Settings settings = controller.getSettings();
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
		if(! fromDate.getText().trim().isEmpty())
			dateRange = fromDate.getText().trim();
		if(! toDate.getText().trim().isEmpty()) {
			if(dateRange == null)
				dateRange = "";
			else 
				dateRange += "-";
			
			dateRange += toDate.getText().trim();
		}
		
		try {
			query.connect();
			List<DicomObject> objects = query.query(pid, pname, dateRange);
			result.setModel(new DicomTableModel(objects));
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
		c.insets = noPadding;
		panel.add(new JLabel("Patient Id: "), c);

		c.gridx = 1;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.weightx = 1;
		c.insets = rightPadding;
		panel.add(patientId, c);

		c.gridx = 2;
		c.fill = GridBagConstraints.NONE;
		c.weightx = 0;
		c.insets = noPadding;
		panel.add(new JLabel("Last name: "), c);
		
		c.gridx = 3;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.weightx = 1;
		c.insets = rightPadding;
		panel.add(patientLastName, c);
		
		c.gridx = 4;
		c.fill = GridBagConstraints.NONE;
		c.weightx = 0;
		c.insets = noPadding;
		panel.add(new JLabel("First name: "), c);
		
		c.gridx = 5;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.weightx = 1;
		c.insets = rightPadding;
		panel.add(patientFirstName, c);

		c.gridy = 1;
		c.gridx = 0;
		c.weightx = 0;
		c.insets = noPadding;		
		panel.add(new JLabel("Date From: "), c);
		
		c.gridx = 1;
		c.weightx = 1;
		c.insets = rightPadding;
		panel.add(fromDate, c);
		
		c.gridx = 2;
		c.weightx = 0;
		c.insets = noPadding;		
		panel.add(new JLabel("To: "), c);
		
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
