package wdutil.wdjws;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.OffsetDateTime;
import java.util.Properties;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.workday.intsys.ws.IntegrationsPort;
import com.workday.intsys.ws.IntegrationsService;
import com.workday.intsys.xml.GetReferencesRequest;
import com.workday.intsys.xml.GetReferencesRequestCriteria;
import com.workday.intsys.xml.GetReferencesResponse;
import com.workday.intsys.xml.InstanceID;
import com.workday.intsys.xml.InstanceObject;
import com.workday.intsys.xml.IntegrationAbstractValueData;
import com.workday.intsys.xml.IntegrationEvent;
import com.workday.intsys.xml.IntegrationLaunchParameterData;
import com.workday.intsys.xml.LaunchParameterObject;
import com.workday.intsys.xml.LaunchParameterObjectID;

import wdutil.wdjws.ws.Log4JWriter;
import wdutil.wdjws.ws.WSUtil;

public class IntegrationTest {

	private static Logger LOG = LoggerFactory.getLogger(IntegrationTest.class);
	private static final IntegrationsService intsysService = new IntegrationsService();
	private static Properties testProperties;

	@BeforeAll
	public static void init() throws Exception {
		testProperties = new Properties();
		testProperties.load(Files.newInputStream(Paths.get("test.properties"), StandardOpenOption.READ));
	}

	public IntegrationsPort newPort() throws Exception {

		IntegrationsPort intSysPort = intsysService.getIntegrations();

		String endpoint = String.format("https://%s/ccx/service/%s/%s/%s", testProperties.getProperty("workday.host"), testProperties.getProperty("workday.tenant"), "Integrations", "v" + testProperties.getProperty("workday.version"));

		WSUtil.overridePort(intSysPort, 25000, 25000, endpoint);
		WSUtil.setDescriptors(intSysPort, true);
		WSUtil.setCredentials(intSysPort, testProperties.getProperty("workday.username"), testProperties.getProperty("workday.tenant"), testProperties.getProperty("workday.password"));

		if (LOG.isDebugEnabled()) {
			WSUtil.addLogRequest(intSysPort, new Log4JWriter(LOG));
		}
		return intSysPort;
	}

	@Test
	public void getReferencesTest() throws Exception {
		IntegrationsPort intSysPort = newPort();

		GetReferencesRequest request = new GetReferencesRequest().withVersion("v" + testProperties.getProperty("workday.version"));
		request.setRequestCriteria(new GetReferencesRequestCriteria().withReferenceIDType("Pay_Rate_Type_ID"));
		GetReferencesResponse response = intSysPort.getReferences(request);
		assertNotNull(response.getResponseData());
		assertFalse(response.getResponseData().getReferenceID().isEmpty());
		String payRateTypeIDs = response.getResponseData().getReferenceID().stream().map(i -> i.getReferenceIDData().getID()).collect(Collectors.joining(","));
		LOG.info("Retrieved pay Rate Type IDs {}", payRateTypeIDs);
	}

	@Test
	public void getFirstEventFile() throws Exception {
		IntegrationUtil intUtil = new IntegrationUtil(testProperties.getProperty("workday.host"), testProperties.getProperty("workday.tenant"), testProperties.getProperty("workday.username"), testProperties.getProperty("workday.password"), testProperties.getProperty("workday.version"));

		//Integration IDs -> Launch Parameter (Metadata), filter by instance, confirm Parent Value matches integration service name associated with launch parameter.
		IntegrationLaunchParameterData[] lier = new IntegrationLaunchParameterData[4];

		IntegrationLaunchParameterData lp = new IntegrationLaunchParameterData();
		lp.setLaunchParameterReference(new LaunchParameterObject().withID(new LaunchParameterObjectID().withParentType("Workday_Integration_Service_Name").withParentId("Core Connector: Date Launch Parameters").withType("Workday_Launch_Parameter_Name").withValue("As Of Entry Moment")));
		lp.setLaunchParameterValueData(new IntegrationAbstractValueData().withDateTime(OffsetDateTime.now()));
		lier[0] = lp;

		lp = new IntegrationLaunchParameterData();
		lp.setLaunchParameterReference(new LaunchParameterObject().withID(new LaunchParameterObjectID().withParentType("Workday_Integration_Service_Name").withParentId("Core Connector: Date Launch Parameters").withType("Workday_Launch_Parameter_Name").withValue("Effective Date")));
		lp.setLaunchParameterValueData(new IntegrationAbstractValueData().withDate(OffsetDateTime.now().toLocalDate()));
		lier[1] = lp;

		lp = new IntegrationLaunchParameterData();
		lp.setLaunchParameterReference(new LaunchParameterObject().withID(new LaunchParameterObjectID().withParentType("Workday_Integration_Service_Name").withParentId("Core Connector: Worker Integration Configuration").withType("Workday_Launch_Parameter_Name").withValue("Workers")));
		lp.setLaunchParameterValueData(new IntegrationAbstractValueData().withInstanceReference(new InstanceObject().withID(new InstanceID().withType("Employee_ID").withValue("21001"))));
		lier[2] = lp;

		lp = new IntegrationLaunchParameterData();
		lp.setLaunchParameterReference(new LaunchParameterObject().withID(new LaunchParameterObjectID().withParentType("Workday_Integration_Service_Name").withParentId("Core Connector: Worker Integration Configuration").withType("Workday_Launch_Parameter_Name").withValue("Full File")));
		lp.setLaunchParameterValueData(new IntegrationAbstractValueData().withBoolean(true));
		lier[3] = lp;

		IntegrationEvent event = intUtil.launchIntegration("Cloud Connect  - Worker", lier);
		assertNotNull(event);
		event = intUtil.waitForFinish(event.getIntegrationEventReference().getID().stream().filter(i -> "Background_Process_Instance_ID".contentEquals(i.getType())).findFirst().get().getValue());
		intUtil.downloadFiles(event, new Pattern[] { Pattern.compile(".*\\.xml") }, Paths.get("target/wd-files"), false);
		assertTrue(Files.exists(Paths.get("target/wd-files/output.xml")));
	}
}
