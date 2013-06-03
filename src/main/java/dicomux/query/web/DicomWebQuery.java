package dicomux.query.web;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.dcm4che2.data.BasicDicomObject;
import org.dcm4che2.data.DicomObject;
import org.dcm4che2.data.Tag;
import org.dcm4che2.data.VR;
import org.dcm4che2.data.VRMap;
import org.dcm4che2.net.ConfigurationException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import dicomux.query.DicomQuery;

/**
 * Used to call a json web service instead of calling the PACS directly
 * This avoids having to declare each client as a modality in the pacs. 
 */
public class DicomWebQuery extends DicomQuery {
	private String appKey;
	private QueryClient qclient;

	public DicomWebQuery(String appKey, String pacsId, URL url) {
		this.appKey = appKey;
		this.qclient = new QueryClient(pacsId, url);
	}
	
	public void connect()
	throws ConfigurationException, IOException, InterruptedException {
		//nothing
	}
	
	public void close() throws InterruptedException {
		//nothing
	}

	@SuppressWarnings("unchecked")
	public List<DicomObject> query(String patientId, String patientName, String dateRange) 
	throws IOException, InterruptedException {
		List<DicomObject> result = new ArrayList<DicomObject>();
				
		if(patientId == null)
			patientId = "";
		if(patientName == null)
			patientName = "";
		if(dateRange == null)
			dateRange = "";
		
		QueryResult qr = qclient.findECG(appKey, patientId, patientName, dateRange);
		if(! qr.getSuccess())
			throw new IOException(qr.getMessage());
		
		try {
			JSONArray data = qr.getJSONArray("data");
			for(int i=0;i<data.length();i++) {
				JSONObject item = data.getJSONObject(i);
				DicomObject dco = new BasicDicomObject();

				Iterator<String> keys = (Iterator<String>)item.keys();
				while(keys.hasNext()) {
					String key = keys.next();
			    	if(!item.isNull(key)) {					
						int tag = Tag.forName(key);
				    	VR vr = VRMap.getVRMap().vrOf(tag);
				    	String value = item.getString(key);
				    	dco.putString(tag, vr, value);
			    	}				    
				}
				
				result.add(dco);
			}
		} catch(JSONException e) {
			throw new IOException(e);
		}
		
		return result;
	}
}
