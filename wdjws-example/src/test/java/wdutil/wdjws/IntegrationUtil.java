package wdutil.wdjws;

import java.net.URLEncoder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.workday.intsys.ws.IntegrationsPort;
import com.workday.intsys.ws.IntegrationsService;
import com.workday.intsys.xml.EventDocuments;
import com.workday.intsys.xml.GetEventDocumentsRequest;
import com.workday.intsys.xml.GetEventDocumentsResponse;
import com.workday.intsys.xml.GetImportProcessMessagesRequest;
import com.workday.intsys.xml.GetImportProcessMessagesResponse;
import com.workday.intsys.xml.GetImportProcessesRequest;
import com.workday.intsys.xml.GetImportProcessesResponse;
import com.workday.intsys.xml.GetIntegrationEventsRequest;
import com.workday.intsys.xml.GetIntegrationEventsResponse;
import com.workday.intsys.xml.GetIntegrationSystemsRequest;
import com.workday.intsys.xml.ImportProcess;
import com.workday.intsys.xml.ImportProcessMessage;
import com.workday.intsys.xml.ImportProcessMessagesRequestCriteria;
import com.workday.intsys.xml.ImportProcessRequestReferences;
import com.workday.intsys.xml.IntegrationESBInvocationAbstractObject;
import com.workday.intsys.xml.IntegrationESBInvocationAbstractObjectID;
import com.workday.intsys.xml.IntegrationEvent;
import com.workday.intsys.xml.IntegrationEventRequestReferences;
import com.workday.intsys.xml.IntegrationLaunchParameterData;
import com.workday.intsys.xml.IntegrationRepositoryDocument;
import com.workday.intsys.xml.IntegrationSystemAuditedObject;
import com.workday.intsys.xml.IntegrationSystemAuditedObjectID;
import com.workday.intsys.xml.IntegrationSystemRequestReferences;
import com.workday.intsys.xml.LaunchIntegrationEventRequest;
import com.workday.intsys.xml.LaunchIntegrationEventResponse;
import com.workday.intsys.xml.RepositoryDocumentObjectID;
import com.workday.intsys.xml.RepositoryDocumentSummaryData;
import com.workday.intsys.xml.WebServiceBackgroundProcessRuntimeObject;
import com.workday.intsys.xml.WebServiceBackgroundProcessRuntimeObjectID;

import wdutil.wdjws.rest.MyReports;
import wdutil.wdjws.ws.Log4JWriter;
import wdutil.wdjws.ws.WSUtil;

public class IntegrationUtil {
	private static final IntegrationsService intsysService = new IntegrationsService();
	private static Logger LOG = LoggerFactory.getLogger(IntegrationUtil.class);

	final IntegrationsPort port;

	final String host;
	final String tenant;
	final String username;
	final String password;
	final String version;

	public IntegrationUtil(String host, String tenant, String username, String password, String version) {
		this.host = host;
		this.tenant = tenant;
		this.username = username;
		this.password = password;
		this.version = "v" + version;

		IntegrationsPort intSysPort = intsysService.getIntegrations();

		String endpoint = String.format("https://%s/ccx/service/%s/%s/%s", host, tenant, "Integrations", "v" + version);

		WSUtil.overridePort(intSysPort, 25000, 25000, endpoint);
		WSUtil.setDescriptors(intSysPort, true);
		WSUtil.setCredentials(intSysPort, username, tenant, password);

		if (LOG.isDebugEnabled()) {
			WSUtil.addLogRequest(intSysPort, new Log4JWriter(LOG));
		}

		this.port = intSysPort;

	}

	public void integrationInformation(String integrationId) throws Exception {

		GetIntegrationSystemsRequest request = new GetIntegrationSystemsRequest().withVersion(version);
		request.setRequestReferences(new IntegrationSystemRequestReferences().withIntegrationSystemReference(new IntegrationSystemAuditedObject().withID(new IntegrationSystemAuditedObjectID().withType("Integration_System_ID").withValue(integrationId))));
		port.getIntegrationSystems(request);

	}

	public IntegrationEvent launchIntegrationAndWaitForFinish(String intId, IntegrationLaunchParameterData... params) throws Exception {
		IntegrationEvent event = launchIntegration(intId, params);
		String eventId = event.getIntegrationEventReference().getID().get(1).getValue();
		return waitForFinish(eventId);
	}

	public void launchIntegrationAndDownload(String intId, Path parent, boolean subDir, Pattern[] filePatterns, IntegrationLaunchParameterData... params) throws Exception {
		IntegrationEvent finished = launchIntegrationAndWaitForFinish(intId);
		downloadFiles(finished, filePatterns, parent, subDir);

	}

	// Integration IDs and search for " Launch Parameter ( Metadata)".

	public IntegrationEvent launchIntegration(String intId, IntegrationLaunchParameterData... params) throws Exception {

		LaunchIntegrationEventRequest lier = new LaunchIntegrationEventRequest();
		lier.setVersion(version);
		lier.setIntegrationSystemReference(new IntegrationSystemAuditedObject().withID(new IntegrationSystemAuditedObjectID().withType("Integration_System_ID").withValue(intId)));

		if (params != null) {
			for (IntegrationLaunchParameterData lpd : params) {
				lier.getIntegrationLaunchParameterData().add(lpd);
			}
		}

		LaunchIntegrationEventResponse response = port.launchIntegration(lier);
		return response.getIntegrationEvent();

	}

	public IntegrationEvent waitForFinish(String eventId) throws Exception {
		while (true) {
			Thread.sleep(3000);
			IntegrationEvent event = getIntegrationEvent(eventId);
			if (event != null) {
				String status = event.getBackgroundProcessInstanceData().getBackgroundProcessInstanceStatusReference().getDescriptor();
				if (!"Initiated".equals(status) && !"Requested".equals(status) && !"Processing".equals(status) && !"Dispatched".equals(status)) {
					return event;
				} else {
					LOG.info("{}% Complete", event.getIntegrationEventData().getPercentComplete());
				}
			}
		}
	}

	public void downloadFiles(IntegrationEvent ie, Pattern[] filePatterns, Path parent, boolean subDir) throws Exception {
		Path intDir = parent;
		if (subDir) {
			intDir = parent.resolve(String.format("%1$tY-%1$tm-%1$tdT%1$tH", ie.getIntegrationEventData().getCompletedDateTime()));
		}
		Files.createDirectories(intDir);

		for (IntegrationRepositoryDocument doc : ie.getBackgroundProcessInstanceData().getOutputDocumentData()) {
			String fileName = doc.getIntegrationRepositoryDocumentData().getFileName();
			fileName = fileName.replaceAll(":", "");
			if (fileName.startsWith(".")) {
				fileName = fileName.substring(1);
			}
			for (Pattern filePattern : filePatterns) {
				Matcher m = filePattern.matcher(fileName);
				if (m.matches()) {
					Path filePath = null;

					filePath = intDir.resolve(fileName);
					String[] did = doc.getIntegrationRepositoryDocumentData().getDocumentID().split("\\/");
					String fileURL = MyReports.getMyReportsFileURL(host, tenant, String.format("%s/%s", URLEncoder.encode(did[0], "UTF-8"), did[1]));
					LOG.info("Downloading file {} at {} to path {}", fileName, fileURL, filePath);
					try {
						byte[] file = MyReports.downloadFile(fileURL, username, tenant, password);
						Files.write(filePath, file);
						LOG.info("Download complete");
					} catch (Exception e) {
						LOG.error("Download error {}", fileURL, e);
					}

				}
			}
		}

	}

	public String getEventDocument(String eventId, Pattern fileName) throws Exception {

		GetEventDocumentsRequest request = new GetEventDocumentsRequest();
		request.setVersion(version);

		// request.setRequestReferences(new IntegrationEventRequestReferences().withIntegrationEventReference(new IntegrationEventObject().withID(new IntegrationEventObjectID().withType("Background_Process_Instance_ID").withValue(eventId))));

		GetEventDocumentsResponse response = port.getEventDocuments(request);
		for (EventDocuments docs : response.getResponseData().getEventDocuments()) {
			for (RepositoryDocumentSummaryData doc : docs.getRepositoryDocument()) {
				if (fileName.matcher(doc.getRepositoryDocumentReference().getDescriptor()).matches()) {
					for (RepositoryDocumentObjectID id : doc.getRepositoryDocumentReference().getID()) {
						if ("Document_ID".equals(id.getType())) {
							return id.getValue();
						}
					}
				}
			}

		}
		throw new Exception(String.format("file %s not found", fileName.pattern()));
	}

	public IntegrationEvent getIntegrationEvent(String eventId) throws Exception {

		GetIntegrationEventsRequest request = new GetIntegrationEventsRequest();
		request.setVersion(version);

		request.setRequestReferences(new IntegrationEventRequestReferences().withIntegrationEventReference(new IntegrationESBInvocationAbstractObject().withID(new IntegrationESBInvocationAbstractObjectID().withType("Background_Process_Instance_ID").withValue(eventId))));
		GetIntegrationEventsResponse response = port.getIntegrationEvents(request);
		if (response != null) {
			List<IntegrationEvent> event = response.getResponseData().getIntegrationEvent();
			if (!event.isEmpty()) {
				return event.get(0);
			}
		}
		return null;
	}

	public ImportProcess getImportProcess(String processId) throws Exception {

		GetImportProcessesRequest request = new GetImportProcessesRequest();
		request.setVersion(version);

		request.setRequestReferences(new ImportProcessRequestReferences().withImportProcessReference(new WebServiceBackgroundProcessRuntimeObject().withID(new WebServiceBackgroundProcessRuntimeObjectID().withType("WID").withValue(processId))));

		GetImportProcessesResponse response = port.getImportProcesses(request);
		if (response.getResponseData() != null) {
			return response.getResponseData().getImportProcess().get(0);
		}
		return null;
	}

	public List<ImportProcessMessage> getImportProcessMessages(String processId) throws Exception {

		GetImportProcessMessagesRequest request = new GetImportProcessMessagesRequest();
		request.setVersion(version);

		request.setRequestCriteria(new ImportProcessMessagesRequestCriteria().withImportProcessReference(new WebServiceBackgroundProcessRuntimeObject().withID(new WebServiceBackgroundProcessRuntimeObjectID().withType("WID").withValue(processId))));

		GetImportProcessMessagesResponse response = port.getImportProcessMessages(request);
		if (response.getResponseData() != null && !response.getResponseData().isEmpty()) {
			return response.getResponseData().get(0).getImportProcessMessage();
		}
		return null;
	}

	public String waitForImport(String processId) throws Exception {
		while (true) {
			Thread.sleep(3000);
			ImportProcess ip = getImportProcess(processId);
			String status = ip.getImportProcessData().getStatusReference().getID().get(0).getValue();
			if (!"Initiated".equals(status) && !"Requested".equals(status) && !"Processing".equals(status) && !"Dispatched".equals(status)) {
				return status;
			}
		}
	}
}
