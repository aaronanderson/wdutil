package wdutil.wdjws.ws;

import java.util.Iterator;
import java.util.Map;

import javax.xml.XMLConstants;
import javax.xml.namespace.NamespaceContext;

public class WDNamespaceContext implements NamespaceContext {

	public final static String WD_NS = "urn:com.workday/bsvc";

	private Map<String, String> mappings = null;

	public WDNamespaceContext() {

	}

	public WDNamespaceContext(Map<String, String> mappings) {
		this.mappings = mappings;
	}

	@Override
	public Iterator getPrefixes(String namespaceURI) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getPrefix(String namespaceURI) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getNamespaceURI(String prefix) {
		if ("wd".equals(prefix)) {
			return WD_NS;
		}
		if (mappings != null) {
			String value = mappings.get(prefix);
			if (value != null) {
				return value;
			}
		}
		return XMLConstants.NULL_NS_URI;
	}

}
