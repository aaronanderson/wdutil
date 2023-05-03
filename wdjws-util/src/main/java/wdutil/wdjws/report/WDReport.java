package wdutil.wdjws.report;

import java.io.Writer;

import jakarta.xml.soap.MessageFactory;
import jakarta.xml.soap.SOAPBody;
import jakarta.xml.soap.SOAPConnection;
import jakarta.xml.soap.SOAPConnectionFactory;
import jakarta.xml.soap.SOAPElement;
import jakarta.xml.soap.SOAPEnvelope;
import jakarta.xml.soap.SOAPMessage;
import jakarta.xml.soap.SOAPPart;

import org.apache.wss4j.dom.WSConstants;
import org.apache.wss4j.dom.message.WSSecHeader;
import org.apache.wss4j.dom.message.WSSecUsernameToken;
import org.w3c.dom.Document;

import wdutil.wdjws.ws.WSUtil;

public class WDReport {

	public static final String REPORT_PREFIX = "rpt";

	public static SOAPMessage createReportRequest(String reportNS) throws Exception {
		MessageFactory messageFactory = MessageFactory.newInstance();
		SOAPMessage soapMessage = messageFactory.createMessage();
		SOAPPart soapPart = soapMessage.getSOAPPart();

		SOAPEnvelope envelope = soapPart.getEnvelope();
		envelope.addNamespaceDeclaration("xsd", "http://www.w3.org/2001/XMLSchema");

		SOAPBody soapBody = envelope.getBody();
		SOAPElement execReport = soapBody.addChildElement("Execute_Report", REPORT_PREFIX, reportNS);

		execReport.addChildElement("Report_Parameters", REPORT_PREFIX);
		soapMessage.saveChanges();

		//soapMessage.writeTo(System.out);
		//System.out.println();
		return soapMessage;
	}

	public static void setSecurity(SOAPMessage message, String userId, String companyId, String password) throws Exception {

		Document doc = message.getSOAPPart();
		WSSecHeader secHeader = new WSSecHeader(doc);
		secHeader.insertSecurityHeader();
		WSSecUsernameToken builder = new WSSecUsernameToken(secHeader);
		builder.setUserInfo(String.format("%s@%s", userId, companyId), password);
		builder.setPasswordType(WSConstants.PASSWORD_TEXT);
		builder.build();
		message.saveChanges();

	}

	public static SOAPMessage executeReport(String serviceHost, String clientId, String reportName, SOAPMessage requestMessage, Writer out) throws Exception {

		// Create SOAP Connection
		SOAPConnectionFactory soapConnectionFactory = SOAPConnectionFactory.newInstance();
		SOAPConnection soapConnection = soapConnectionFactory.createConnection();
		try {
			String url = String.format("https://%s/ccx/service/Report2/%s/%s", serviceHost, clientId, reportName);
			if (out != null) {
				WSUtil.printMessage(String.format("REQUEST: %s - \n", url), requestMessage, out);
			}
			SOAPMessage responseMessage = soapConnection.call(requestMessage, url);
			if (out != null) {
				WSUtil.printMessage("RESPONSE:\n", responseMessage, out);
			}
			return responseMessage;

		} finally {
			soapConnection.close();
		}

	}
}
