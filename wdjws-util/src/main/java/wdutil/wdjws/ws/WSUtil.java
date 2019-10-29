package wdutil.wdjws.ws;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.Writer;
import java.net.ConnectException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.namespace.QName;
import javax.xml.soap.MessageFactory;
import javax.xml.soap.SOAPElement;
import javax.xml.soap.SOAPEnvelope;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPHeader;
import javax.xml.soap.SOAPHeaderElement;
import javax.xml.soap.SOAPMessage;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import javax.xml.ws.Binding;
import javax.xml.ws.BindingProvider;
import javax.xml.ws.WebServiceException;
import javax.xml.ws.handler.Handler;
import javax.xml.ws.handler.MessageContext;
import javax.xml.ws.handler.soap.SOAPHandler;
import javax.xml.ws.handler.soap.SOAPMessageContext;

import org.apache.wss4j.common.crypto.Crypto;
import org.apache.wss4j.common.crypto.CryptoFactory;
import org.apache.wss4j.common.ext.WSSecurityException;
import org.apache.wss4j.dom.WSConstants;
import org.apache.wss4j.dom.message.WSSecHeader;
import org.apache.wss4j.dom.message.WSSecSignature;
import org.apache.wss4j.dom.message.WSSecUsernameToken;
import org.w3c.dom.Document;

public class WSUtil {

	public static void overridePort(Object port, int connectionTimeout, int requestTimeout, String endpoint) {
		Map<String, Object> requestContext = ((BindingProvider) port).getRequestContext();
		requestContext.put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, endpoint);
		//CXF should honor these per https://examples.javacodegeeks.com/enterprise-java/jws/jax-ws-client-timeout-example/
		requestContext.put("javax.xml.ws.client.connectionTimeout", connectionTimeout);
		requestContext.put("javax.xml.ws.client.receiveTimeout", requestTimeout);
		requestContext.put("set-jaxb-validation-event-handler", Boolean.FALSE);
	}

	public static String getEndpointAddress(Object port) {
		if (port != null) {
			return (String) ((BindingProvider) port).getRequestContext().get(BindingProvider.ENDPOINT_ADDRESS_PROPERTY);
		}
		return null;
	}

	public static boolean isConnectionFailure(Throwable t) {
		if (t instanceof WebServiceException) {
			if (t.getCause() instanceof ConnectException) {
				return true;
			}
		}
		return false;
	}

	public static void addLogRequest(Object port, Writer writer) {
		addLogRequest(((BindingProvider) port), writer);
	}

	public static void removeLogRequest(Object port) {
		removeLogRequest(((BindingProvider) port));
	}

	public static void setCredentials(Object port, String userId, String companyId, String password) {
		Binding binding = ((BindingProvider) port).getBinding();
		List handlers = binding.getHandlerChain();
		if (handlers == null)
			handlers = new ArrayList();
		PasswordSecurityHandler securityHandler = new PasswordSecurityHandler(userId, companyId, password);
		handlers.add(securityHandler);
		binding.setHandlerChain(handlers);
	}

	public static void setCredentials(Object port, String userId, String companyId, byte[] privateKey, X509Certificate certificate) throws WSSecurityException {
		Binding binding = ((BindingProvider) port).getBinding();
		List handlers = binding.getHandlerChain();
		if (handlers == null)
			handlers = new ArrayList();
		X509SecurityHandler securityHandler = new X509SecurityHandler(userId, companyId, privateKey, certificate);
		handlers.add(securityHandler);
		binding.setHandlerChain(handlers);
	}

	public static void setDescriptors(Object port, boolean enableDescriptors) {
		Binding binding = ((BindingProvider) port).getBinding();
		List handlers = binding.getHandlerChain();
		if (handlers == null)
			handlers = new ArrayList();
		DescriptorHandler descriptorHandler = new DescriptorHandler(enableDescriptors);
		handlers.add(descriptorHandler);
		binding.setHandlerChain(handlers);
	}

	public static void setValidateOnly(Object port) {
		Binding binding = ((BindingProvider) port).getBinding();
		List handlers = binding.getHandlerChain();
		if (handlers == null)
			handlers = new ArrayList();
		ValidationOnlyHandler validateOnlyHandler = new ValidationOnlyHandler();
		handlers.add(validateOnlyHandler);
		binding.setHandlerChain(handlers);
	}

	public static void adjustNamespaces(Object port) {
		Binding binding = ((BindingProvider) port).getBinding();
		List handlers = binding.getHandlerChain();
		if (handlers == null)
			handlers = new ArrayList();
		SOAPNamespaceHandler namespaceHandler = new SOAPNamespaceHandler();
		handlers.add(namespaceHandler);
		binding.setHandlerChain(handlers);
	}

	public static void addLogRequest(BindingProvider port, Writer out) {
		Binding binding = port.getBinding();
		List<Handler> handlers = binding.getHandlerChain();
		if (handlers == null)
			handlers = new ArrayList();
		LoggingHandler loggingHandler = new LoggingHandler(out);
		handlers.add(loggingHandler);
		binding.setHandlerChain(handlers);
	}

	public static void removeLogRequest(BindingProvider port) {
		Binding binding = port.getBinding();
		List<Handler> handlers = binding.getHandlerChain();
		List<Handler> newHandlers = new ArrayList();
		if (handlers != null) {
			for (Handler h : handlers) {
				if (!(h instanceof LoggingHandler)) {
					newHandlers.add(h);
				}
			}
		}
		binding.setHandlerChain(newHandlers);
	}

	public static class LoggingHandler implements SOAPHandler<SOAPMessageContext> {

		Writer out;

		public LoggingHandler(Writer out) {
			this.out = out;
		}

		@Override
		public boolean handleMessage(SOAPMessageContext c) {
			SOAPMessage message = c.getMessage();
			boolean request = ((Boolean) c.get(SOAPMessageContext.MESSAGE_OUTBOUND_PROPERTY)).booleanValue();
			try {
				if (request) {
					printMessage(String.format("REQUEST: %s - \n", c.get(BindingProvider.ENDPOINT_ADDRESS_PROPERTY)), obfuscate(message), out);
				} else {
					printMessage("RESPONSE:\n", message, out);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
			return true;
		}

		@Override
		public boolean handleFault(SOAPMessageContext c) {
			SOAPMessage message = c.getMessage();
			try {
				printMessage("RESPONSE:\n", message, out);
			} catch (Exception e) {
				e.printStackTrace();
			}
			return true;
		}

		@Override
		public void close(MessageContext c) {

		}

		@Override
		public Set getHeaders() {
			return null;
		}
	}

	public static boolean ENABLE_OBFUSCATE = true;

	public static SOAPMessage obfuscate(SOAPMessage message) {
		if (!ENABLE_OBFUSCATE) {
			return message;
		}
		try {
			MessageFactory messageFactory = MessageFactory.newInstance();
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			message.writeTo(baos);
			ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
			SOAPMessage obfuscated = messageFactory.createMessage(null, bais);

			String WSSE_NS = "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd";

			Iterator security = obfuscated.getSOAPHeader().getChildElements(new QName(WSSE_NS, "Security"));
			if (security.hasNext()) {
				Iterator userNameToken = ((SOAPElement) security.next()).getChildElements(new QName(WSSE_NS, "UsernameToken"));
				if (userNameToken.hasNext()) {
					Iterator password = ((SOAPElement) userNameToken.next()).getChildElements(new QName(WSSE_NS, "Password"));
					if (password.hasNext()) {
						SOAPElement passwordValue = (SOAPElement) password.next();
						passwordValue.setValue(passwordValue.getValue().replaceAll(".", "#"));
					}
				}
			}
			return obfuscated;
		} catch (SOAPException | IOException e) {
			e.printStackTrace();
			return message;
		}
	}

	public static void printMessage(String title, SOAPMessage message, Writer out) {
		try {
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			bos.write(title.getBytes());
			// pretty print the result
			ByteArrayOutputStream tempbos = new ByteArrayOutputStream();
			message.writeTo(tempbos);
			TransformerFactory transformerFactory = TransformerFactory.newInstance();
			Transformer transformer = transformerFactory.newTransformer();
			transformer.setOutputProperty(OutputKeys.INDENT, "yes");
			transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
			StreamResult result = new StreamResult(bos);
			transformer.transform(new StreamSource(new ByteArrayInputStream(tempbos.toByteArray())), result);
			out.write(bos.toString());
			out.write('\n');
			out.flush();
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	public static class PasswordSecurityHandler implements SOAPHandler<SOAPMessageContext> {

		String userId;
		String tenantId;
		String password;

		public PasswordSecurityHandler(String userId, String companyId, String password) {
			this.userId = userId;
			this.tenantId = companyId;
			this.password = password;
		}

		public boolean handleMessage(SOAPMessageContext msgCtx) {

			final Boolean outInd = (Boolean) msgCtx.get(MessageContext.MESSAGE_OUTBOUND_PROPERTY);

			if (outInd.booleanValue()) {
				try {
					addSecurityHeader(msgCtx.getMessage());
				} catch (final Exception e) {
					e.printStackTrace();
					return false;
				}
			}
			return true;
		}

		public void addSecurityHeader(SOAPMessage soapMessage) throws WSSecurityException, SOAPException {
			Document doc = soapMessage.getSOAPPart();
			WSSecHeader secHeader = new WSSecHeader(doc);
			secHeader.insertSecurityHeader();
			WSSecUsernameToken builder = new WSSecUsernameToken(secHeader);
			builder.setUserInfo(String.format("%s@%s", userId, tenantId), password);
			builder.setPasswordType(WSConstants.PASSWORD_TEXT);
			builder.build();
			soapMessage.saveChanges();

		}

		public boolean handleFault(SOAPMessageContext context) {
			return false;
		}

		public void close(MessageContext context) {

		}

		public Set<QName> getHeaders() {
			return null;
		}

	}

	public static class X509SecurityHandler implements SOAPHandler<SOAPMessageContext> {

		String userId;
		String companyId;
		X509Certificate cert;
		byte[] secretKey;
		Crypto crypto;

		public X509SecurityHandler(String userId, String companyId, byte[] secretKey, X509Certificate cert) throws WSSecurityException {
			this.userId = userId;
			this.companyId = companyId;
			this.cert = cert;
			this.secretKey = secretKey;
			this.crypto = CryptoFactory.getInstance();
		}

		public boolean handleMessage(SOAPMessageContext msgCtx) {

			final Boolean outInd = (Boolean) msgCtx.get(MessageContext.MESSAGE_OUTBOUND_PROPERTY);

			if (outInd.booleanValue()) {
				try {
					addSecurityHeader(msgCtx.getMessage());
				} catch (final Exception e) {
					e.printStackTrace();
					return false;
				}
			}
			return true;
		}

		public void addSecurityHeader(SOAPMessage soapMessage) throws WSSecurityException, SOAPException {
			Document doc = soapMessage.getSOAPPart();
			WSSecHeader secHeader = new WSSecHeader(doc);
			WSSecSignature builder = new WSSecSignature(secHeader);
			builder.setX509Certificate(cert);
			builder.setSecretKey(secretKey);
			builder.setUserInfo(String.format("%s@%s", userId, companyId), "security");
			secHeader.insertSecurityHeader();
			Document signedDoc = builder.build(crypto);
			soapMessage.getSOAPPart().setContent(new DOMSource(signedDoc));
			soapMessage.saveChanges();

		}

		public boolean handleFault(SOAPMessageContext context) {
			return false;
		}

		public void close(MessageContext context) {

		}

		public Set<QName> getHeaders() {
			return null;
		}

	}

	public static class DescriptorHandler implements SOAPHandler<SOAPMessageContext> {

		boolean enableDescriptors;

		public DescriptorHandler(boolean enableDescriptors) {
			this.enableDescriptors = enableDescriptors;
		}

		public boolean handleMessage(SOAPMessageContext msgCtx) {

			final Boolean outInd = (Boolean) msgCtx.get(MessageContext.MESSAGE_OUTBOUND_PROPERTY);

			if (outInd.booleanValue()) {
				try {
					addDescriptorHeader(msgCtx.getMessage());
				} catch (final Exception e) {
					e.printStackTrace();
					return false;
				}
			}
			return true;
		}

		public void addDescriptorHeader(SOAPMessage soapMessage) throws SOAPException {
			SOAPEnvelope envelope = soapMessage.getSOAPPart().getEnvelope();
			SOAPHeader header = envelope.getHeader();
			if (header == null)
				header = envelope.addHeader();

			QName commonHeader = new QName("urn:com.workday/bsvc", "Workday_Common_Header", "wd");
			SOAPHeaderElement commonnHeaderElement = header.addHeaderElement(commonHeader);
			QName descriptorHeader = new QName("urn:com.workday/bsvc", "Include_Reference_Descriptors_In_Response", "wd");
			SOAPElement descriptorHeaderElement = commonnHeaderElement.addChildElement(descriptorHeader);
			descriptorHeaderElement.addTextNode(this.enableDescriptors ? "1" : "0");

			soapMessage.saveChanges();

		}

		public boolean handleFault(SOAPMessageContext context) {
			return false;
		}

		public void close(MessageContext context) {

		}

		public Set<QName> getHeaders() {
			return null;
		}

	}

	public static class ValidationOnlyHandler implements SOAPHandler<SOAPMessageContext> {
		public static ThreadLocal<Boolean> ENABLE_VALIDATE = ThreadLocal.withInitial(() -> false);

		public static void startValidateMode() {
			ENABLE_VALIDATE.set(true);
		}

		public static void endValidateMode() {
			ENABLE_VALIDATE.set(false);
		}

		public static Boolean isValidateMode() {
			return ENABLE_VALIDATE.get();
		}

		public boolean handleMessage(SOAPMessageContext msgCtx) {

			final Boolean outInd = (Boolean) msgCtx.get(MessageContext.MESSAGE_OUTBOUND_PROPERTY);

			if (outInd.booleanValue() && isValidateMode()) {
				try {
					addValidationOnlyHeader(msgCtx);
				} catch (final Exception e) {
					e.printStackTrace();
					return false;
				}
			}
			return true;
		}

		public void addValidationOnlyHeader(SOAPMessageContext context) throws SOAPException {
			Map<String, List<String>> headers = (Map<String, List<String>>) context.get(MessageContext.HTTP_REQUEST_HEADERS);
			if (null == headers) {
				headers = new HashMap<String, List<String>>();
			}
			headers.put("X-Validate-Only", Collections.singletonList("1"));//"0"
			context.put(MessageContext.HTTP_REQUEST_HEADERS, headers);
		}

		public boolean handleFault(SOAPMessageContext context) {
			return false;
		}

		public void close(MessageContext context) {

		}

		public Set<QName> getHeaders() {
			return null;
		}

	}

	public static class SOAPNamespaceHandler implements SOAPHandler<SOAPMessageContext> {

		public boolean handleMessage(SOAPMessageContext context) {
			if ((Boolean) context.get(MessageContext.MESSAGE_OUTBOUND_PROPERTY)) { // Check here that the message being intercepted is an outbound message from your service, otherwise ignore.
				try {
					SOAPMessage soapMsg = context.getMessage();
					soapMsg.getSOAPPart().getEnvelope().setAttributeNS("http://www.w3.org/2000/xmlns/", "xmlns:env", "http://schemas.xmlsoap.org/soap/envelope/");
					soapMsg.getSOAPPart().getEnvelope().removeAttributeNS("http://schemas.xmlsoap.org/soap/envelope/", "S");
					soapMsg.getSOAPPart().getEnvelope().removeAttribute("xmlns:S");
					soapMsg.getSOAPPart().getEnvelope().setPrefix("env");
					soapMsg.getSOAPBody().setPrefix("env");
					soapMsg.getSOAPPart().getEnvelope().getHeader().detachNode();

				} catch (SOAPException ex) {
					ex.printStackTrace();
				}
			}
			return true; // indicates to the context to proceed with (normal)message processing
		}

		public boolean handleFault(SOAPMessageContext context) {
			return false;
		}

		public void close(MessageContext context) {

		}

		public Set<QName> getHeaders() {
			return null;
		}

	}
}
