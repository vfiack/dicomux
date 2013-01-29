package dicomux.query;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;

import org.dcm4che2.data.BasicDicomObject;
import org.dcm4che2.data.DicomObject;
import org.dcm4che2.data.Tag;
import org.dcm4che2.data.UID;
import org.dcm4che2.data.VR;
import org.dcm4che2.net.Association;
import org.dcm4che2.net.CommandUtils;
import org.dcm4che2.net.ConfigurationException;
import org.dcm4che2.net.Device;
import org.dcm4che2.net.DimseRSP;
import org.dcm4che2.net.NetworkApplicationEntity;
import org.dcm4che2.net.NetworkConnection;
import org.dcm4che2.net.NewThreadExecutor;
import org.dcm4che2.net.TransferCapability;

public class DicomQuery {
	private static final TransferCapability TC_FIND = new TransferCapability(
			UID.StudyRootQueryRetrieveInformationModelFIND,
			new String[] {UID.ImplicitVRLittleEndian}, 
			TransferCapability.SCU);
	private static final TransferCapability TC_ECHO = new TransferCapability(
			UID.VerificationSOPClass,
			new String[] {UID.ImplicitVRLittleEndian}, 
			TransferCapability.SCU);
	
	
	private String localAET;
	private String remoteAET;
	private String remoteHost;
	private int remotePort;
	
	private Association association;

	public void setLocalInfos(String aet) {
		this.localAET = aet;
	}
	
	public void setRemoteInfos(String aet, String host, int port) {
		this.remoteAET = aet;
		this.remoteHost = host;
		this.remotePort = port;
	}
	
	public void connect()
	throws ConfigurationException, IOException, InterruptedException {
		Device device = new Device(localAET);
		Executor executor = new NewThreadExecutor(localAET);
		NetworkApplicationEntity remoteAE = new NetworkApplicationEntity();
		NetworkApplicationEntity ae = new NetworkApplicationEntity();

		NetworkConnection conn = new NetworkConnection();
		NetworkConnection remoteConn = new NetworkConnection();
		remoteConn.setHostname(remoteHost);
		remoteConn.setPort(remotePort);

		remoteAE.setAETitle(remoteAET);
		remoteAE.setInstalled(true);
		remoteAE.setAssociationAcceptor(true);
		remoteAE.setNetworkConnection(new NetworkConnection[] { remoteConn });

		device.setNetworkApplicationEntity(ae);
		device.setNetworkConnection(conn);
		ae.setNetworkConnection(conn);
		ae.setAssociationInitiator(true);
		ae.setAssociationAcceptor(true);
		ae.setAETitle(localAET);

		remoteAE.setPackPDV(true);
		ae.setTransferCapability(new TransferCapability[] {TC_ECHO, TC_FIND});

		this.association = ae.connect(remoteAE, executor);
	}
	
	public void close() throws InterruptedException {
		if(this.association != null)
			this.association.release(false);
	}

	public List<DicomObject> query(String patientId, String patientName, String dateRange) 
	throws IOException, InterruptedException {
		List<DicomObject> result = new ArrayList<DicomObject>();
				
		DicomObject keys = new BasicDicomObject();
		keys.putString(Tag.QueryRetrieveLevel, VR.CS, "IMAGE");
		keys.putString(Tag.Modality, VR.CS, "ECG");

		keys.putString(Tag.PatientID, VR.LO, patientId);
		keys.putString(Tag.PatientName, VR.PN, patientName);
		keys.putString(Tag.StudyDate, VR.DA, dateRange);
		
		keys.putNull(Tag.InstanceNumber, VR.IS);
		keys.putNull(Tag.SOPClassUID, VR.UI);
		keys.putNull(Tag.SOPInstanceUID, VR.UI);

		keys.putNull(Tag.StudyID, VR.SH);
		keys.putNull(Tag.StudyTime, VR.TM);
		keys.putNull(Tag.StudyDescription, VR.LO);				
		
		
		int priority = 0;
		int cancelAfter = Integer.MAX_VALUE;

		DimseRSP rsp = association.cfind(TC_FIND.getSopClass(), priority, keys, 
				TC_FIND.getTransferSyntax()[0], cancelAfter);
		while (rsp.next()) {
			DicomObject cmd = rsp.getCommand();
			int status = cmd.getInt(Tag.Status);
			if(status >= 0xA000 && status < 0xB000 || status >= 0xC000 && status < 0xD000) {
				throw new IOException("DICOM Query Failed: " + cmd.getString(Tag.ErrorComment));
			}
			
			System.err.println(cmd);
			if (CommandUtils.isPending(cmd)) {
				DicomObject data = rsp.getDataset();
				result.add(data);
			}
		}

		return result;
	}
}
