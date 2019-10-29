package wdutil.wdjws.maven;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.net.ssl.HttpsURLConnection;
import javax.xml.XMLConstants;
import javax.xml.namespace.NamespaceContext;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Build;
import org.apache.maven.model.Model;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.PluginExecution;
import org.apache.maven.model.io.xpp3.MavenXpp3Writer;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectHelper;
import org.apache.maven.shared.invoker.DefaultInvocationRequest;
import org.apache.maven.shared.invoker.DefaultInvoker;
import org.apache.maven.shared.invoker.InvocationOutputHandler;
import org.apache.maven.shared.invoker.InvocationRequest;
import org.apache.maven.shared.invoker.InvocationResult;
import org.apache.maven.shared.invoker.Invoker;
import org.apache.maven.shared.invoker.MavenInvocationException;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.codehaus.plexus.util.xml.Xpp3DomBuilder;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.sun.tools.ws.WsImport;

@Mojo(name = "wsimport", defaultPhase = LifecyclePhase.GENERATE_SOURCES, requiresDependencyResolution = ResolutionScope.RUNTIME)
public class WDJWSMojo extends AbstractMojo {

	//Public Web Services -> Web Service ->  view URLs -> REST Workday XML

	@Parameter(required = true)
	private URL wdTenantServiceURL;

	@Parameter(required = true)
	private String wdVersion;

	@Parameter(defaultValue = "${project.basedir}/Public_Web_Services.xml")
	private File wdWebServiceReport;

	@Parameter
	private String[] services;

	@Parameter
	private Map aliases;

	@Parameter(defaultValue = "com.workday")
	private String packageName;

	@Parameter(defaultValue = "${project.groupId}")
	private String groupId;

	@Parameter
	private String artifactId;

	@Parameter
	private String version;

	////required for the buiild

	@Parameter(defaultValue = "${project.build.directory}/generated-wdjws")
	private File sourceDestDir;

	@Parameter(defaultValue = "${project}", readonly = true, required = true)
	private MavenProject project;

	@Parameter(defaultValue = "${session}", readonly = true)
	private MavenSession session;

	@Component
	private MavenProjectHelper projectHelper;

	public static final Map<String, String> DEFAULT_ALIASES;

	static {
		Map<String, String> map = new HashMap<String, String>();
		map.put("Absence_Management", "absence");
		map.put("Academic_Advising", "academicadvising");
		map.put("Academic_Foundation", "academicfoundation");
		map.put("Benefits_Administration", "benefits");
		map.put("Cash_Management", "cash");
		map.put("Compensation", "comp");
		map.put("Compensation_Review", "compreview");
		map.put("Financial_Management", "financial");
		map.put("Identity_Management", "identity");
		map.put("Professional_Services_Automation", "ps");
		map.put("Revenue_Management", "revenue");
		map.put("Workforce_Planning", "workforce");
		map.put("External_Integrations", "extintsys");
		map.put("Human_Resources", "hr");
		map.put("Integrations", "intsys");
		map.put("Payroll_Interface", "pi");
		map.put("Performance_Management", "performance");
		map.put("Resource_Management", "resource");
		map.put("Time_Tracking", "timetracking");
		map.put("Academic_Foundation", "academic");
		map.put("Campus_Engagement", "campus");
		map.put("Payroll_GBR", "gbrpayroll");
		map.put("payroll_FRA", "frapayroll");
		map.put("Professional_Services_Automation", "psa");
		map.put("Settlement_Services", "settlementservices");
		map.put("Student_Finance", "studentfinance");
		map.put("Student_Records", "studentrecords");
		map.put("Student_Recruiting", "studentrecruiting");
		map.put("Financial_Aid", "financialaid");
		map.put("Tenant_Data_Translation", "datatrans");
		map.put("Dynamic_Document_Generation", "dyndocgen");
		map.put("Workday_Connect", "wdconnect");
		DEFAULT_ALIASES = Collections.unmodifiableMap(map);

	}

	public static final String PACKAGE_INFO = "@XmlSchema(namespace = \"@@NAMESPACE@@\", xmlns = { @XmlNs(namespaceURI = \"@@NAMESPACE@@\", prefix = \"wd\") }, elementFormDefault = javax.xml.bind.annotation.XmlNsForm.QUALIFIED)\n\n" + "package @@PACKAGE@@;\n\n" + "import javax.xml.bind.annotation.XmlNs;\n" + "import javax.xml.bind.annotation.XmlSchema;\n";

	public void execute() throws MojoExecutionException {
		try {

			List<WSDL> wsdls = null;
			if (services.length > 0) {
				wsdls = loadWSDLFromOptions();
			} else {
				wsdls = loadWSDLFromReport();
			}

			Map<String, String> serviceAliases = DEFAULT_ALIASES;
			if (!aliases.isEmpty()) {
				serviceAliases = aliases;
			}

			for (WSDL wsdl : wsdls) {
				String wsdlFileName = wsdl.wsdlName + ".wsdl";
				String serviceName = serviceAliases.get(wsdl.wsdlName) != null ? (String) serviceAliases.get(wsdl.wsdlName) : wsdl.wsdlName.toLowerCase();
				Path serviceBaseDir = sourceDestDir.toPath().resolve(serviceName);
				Path wsdlBaseDir = serviceBaseDir.resolve("src/main/resources/META-INF/wsdl");
				Files.createDirectories(wsdlBaseDir);
				Path wsdlPath = wsdlBaseDir.resolve(wsdlFileName);
				getLog().info(String.format("Downloading WSDL %s to file %s\n", wsdl.wsdlURL, wsdlPath));

				HttpsURLConnection wsdlConn = (HttpsURLConnection) wsdl.wsdlURL.openConnection();
				Files.copy(wsdlConn.getInputStream(), wsdlPath, StandardCopyOption.REPLACE_EXISTING);

				setupService(serviceBaseDir, serviceName, wsdl.wsdlName);
				generateBindingFile(serviceBaseDir, serviceName, wsdl.wsdlName);
				executeJAXWSImport(serviceBaseDir, serviceName, wsdl.wsdlName);
				generatePOM(serviceBaseDir, serviceName, wsdl.wsdlName);
				invokePOM(serviceBaseDir, serviceName, wsdl.wsdlName);

			}

		} catch (Throwable e) {
			throw new MojoExecutionException("wsimport error", e);
		}

	}

	public List<WSDL> loadWSDLFromReport() throws Exception {
		List<WSDL> wsdlURLs = new LinkedList<>();

		XPathFactory xpf = XPathFactory.newInstance();
		XPath xp = xpf.newXPath();
		xp.setNamespaceContext(new NamespaceContext() {

			@Override
			public Iterator getPrefixes(String namespaceURI) {
				return null;
			}

			@Override
			public String getPrefix(String namespaceURI) {
				return null;
			}

			@Override
			public String getNamespaceURI(String prefix) {
				if ("ws".equals(prefix)) {
					return "urn:com.workday.report/Public_Web_Services";
				}
				return XMLConstants.NULL_NS_URI;
			}
		});
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		dbf.setNamespaceAware(true);
		DocumentBuilder db = dbf.newDocumentBuilder();
		Document doc = db.parse(wdWebServiceReport);
		NodeList defs = (NodeList) xp.evaluate("//ws:Web_Service/@ws:Descriptor", doc.getDocumentElement(), XPathConstants.NODESET);
		for (int i = 0; i < defs.getLength(); i++) {
			String service = ((Attr) defs.item(i)).getTextContent();
			if (service.contains("(Do Not Use)")) {
				continue;
			}
			service = service.substring(0, service.length() - 9).replaceAll("\\s", "_");
			String wsdl = String.format("%s/%s/%s?wsdl", wdTenantServiceURL, service, wdVersion.startsWith("v") ? wdVersion : "v" + wdVersion);
			wsdlURLs.add(new WSDL(service, new URL(wsdl)));
		}
		return wsdlURLs;
	}

	public List<WSDL> loadWSDLFromOptions() throws Exception {
		List<WSDL> wsdlURLs = new LinkedList<>();
		for (String service : services) {
			String wsdl = String.format("%s/%s/%s?wsdl", wdTenantServiceURL, service, wdVersion.startsWith("v") ? wdVersion : "v" + wdVersion);
			wsdlURLs.add(new WSDL(service, new URL(wsdl)));
		}
		return wsdlURLs;
	}

	public static class WSDL {
		URL wsdlURL;
		String wsdlName;

		public WSDL(String wsdlName, URL wsdlURL) {
			this.wsdlURL = wsdlURL;
			this.wsdlName = wsdlName;
		}

	}

	public void setupService(Path serviceBaseDir, String serviceName, String wsdlName) throws IOException, ParserConfigurationException, TransformerException {
		Path packageBaseDirectory = serviceBaseDir.resolve("src/main/java");

		String servicePackageName = packageName + "." + serviceName;
		String servicePackageXMLName = servicePackageName + ".xml";
		Path packageDirectory = packageBaseDirectory.resolve(servicePackageXMLName.replaceAll("\\.", "/"));

		Files.createDirectories(packageDirectory);
		Path packageFile = packageDirectory.resolve("package-info.java");
		getLog().info(String.format("Creating package file %s\n", packageFile));
		String fileContents = PACKAGE_INFO.replace("@@PACKAGE@@", servicePackageXMLName);
		fileContents = fileContents.replace("@@NAMESPACE@@", "urn:com.workday/bsvc");
		Files.copy(new ByteArrayInputStream(fileContents.getBytes()), packageFile, StandardCopyOption.REPLACE_EXISTING);

		Path xmlAdaptersFile = packageDirectory.resolve("XmlAdapters.java");
		InputStream is = getClass().getResourceAsStream("/META-INF/jaxws/XmlAdapters.java");
		StringBuilder sb = new StringBuilder(2048);
		char[] read = new char[128];
		try (InputStreamReader ir = new InputStreamReader(is, StandardCharsets.UTF_8)) {
			for (int i = 0; -1 != (i = ir.read(read));) {
				sb.append(read, 0, i);
			}

		}
		fileContents = sb.toString().replace("@@PACKAGE@@", servicePackageName);
		getLog().info(String.format("Writing XmlAdapter to file %s\n", xmlAdaptersFile));
		Files.copy(new ByteArrayInputStream(fileContents.getBytes()), xmlAdaptersFile, StandardCopyOption.REPLACE_EXISTING);

		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		DocumentBuilder db = dbf.newDocumentBuilder();

		Document cat = db.newDocument();
		Element catalog = cat.createElement("catalog");
		cat.appendChild(catalog);
		catalog.setAttribute("xmlns", "urn:oasis:names:tc:entity:xmlns:xml:catalog");// avoid setting up DOM namespace awareness
		catalog.setAttribute("prefer", "system");

		Element system = cat.createElement("system");
		String wsdlFileName = wsdlName + ".wsdl";
		system.setAttribute("systemId", "file:/META-INF/wsdl/" + wsdlFileName);
		system.setAttribute("uri", "wsdl/" + wsdlFileName);
		catalog.appendChild(system);

		Path catalogFile = serviceBaseDir.resolve("src/main/resources/META-INF/jax-ws-catalog.xml");
		TransformerFactory tf = TransformerFactory.newInstance();
		Transformer t = tf.newTransformer();
		t.setOutputProperty(OutputKeys.INDENT, "yes");
		getLog().info(String.format("Writing catalog to file %s\n", catalogFile));
		Files.createDirectories(catalogFile.getParent());
		t.transform(new DOMSource(cat), new StreamResult(catalogFile.toFile()));

	}

	public void generateBindingFile(Path serviceBaseDir, String serviceName, String wsdlName) throws IOException, TransformerException {
		String servicePackageName = packageName + "." + serviceName;
		String wsdlFileName = wsdlName + ".wsdl";

		TransformerFactory tFactory = TransformerFactory.newInstance();
		StreamSource stylesource = new StreamSource(getClass().getResourceAsStream("/META-INF/jaxws/binding.xsl"));
		Transformer transformer = tFactory.newTransformer(stylesource);
		transformer.setOutputProperty(OutputKeys.INDENT, "yes");
		transformer.setParameter("pkgName", servicePackageName);
		transformer.setParameter("fileName", wsdlName);
		transformer.setParameter("namespace", "urn:com.workday/bsvc");

		Path bindingFile = serviceBaseDir.resolve(wsdlFileName.replace(".wsdl", ".jaxws"));
		StreamSource source = new StreamSource(serviceBaseDir.resolve("src/main/resources/META-INF/wsdl").resolve(wsdlFileName).toFile());
		StreamResult result = new StreamResult(bindingFile.toFile());
		getLog().info(String.format("Generating JAX-WS binding to file %s\n", bindingFile));
		transformer.transform(source, result);
	}

	public void executeJAXWSImport(Path serviceBaseDir, String serviceName, String wsdlName) throws Throwable {
		List<String> args = new LinkedList<>();
		Path classesDirectory = serviceBaseDir.resolve("target/classes");
		Files.createDirectories(classesDirectory);
		args.add("-keep");
		args.add("-s");
		args.add(serviceBaseDir.resolve("src/main/java").toString());
		args.add("-d");
		args.add(classesDirectory.toString());
		args.add("-extension");
		args.add("-Xnocompile");
		args.add("-catalog");
		args.add(serviceBaseDir.resolve("src/main/resources/META-INF/jax-ws-catalog.xml").toString());
		args.add("-wsdllocation");
		args.add("file:/META-INF/wsdl/" + wsdlName + ".wsdl");
		args.add("-target");
		args.add("2.2");
		args.add("-B-npa");
		args.add("-B-Xfluent-api");
		args.add("-b");
		args.add(serviceBaseDir.resolve(wsdlName + ".jaxws").toString());
		args.add(serviceBaseDir.resolve("src/main/resources/META-INF/wsdl/" + wsdlName + ".wsdl").toString());
		getLog().info(String.format("Invoking wsimport with arguments %s\n", args));
		int result = WsImport.doMain(args.toArray(new String[args.size()]));
		if (result != 0) {
			throw new Exception("wsimport failed");
		}
	}

	public void generatePOM(Path serviceBaseDir, String serviceName, String wsdlName) throws IOException, XmlPullParserException {
		Path pomFile = serviceBaseDir.resolve("pom.xml");

		Model mvn = new Model();
		mvn.setModelVersion("4.0.0");
		mvn.setGroupId(groupId);
		mvn.setArtifactId(artifactId != null ? (artifactId + "-" + serviceName) : serviceName);
		mvn.setVersion(version != null ? version : wdVersion);
		mvn.setPackaging("jar");
		mvn.setBuild(new Build());
		mvn.getBuild().setExtensions(project.getBuild() != null ? project.getBuildExtensions() : Collections.EMPTY_LIST);
		mvn.setDistributionManagement(project.getDistributionManagement());

		addCompile(mvn);
		addSources(mvn);

		getLog().info(String.format("Writing Maven WSDL %s:%s:%s pom to file %s\n", mvn.getGroupId(), mvn.getArtifactId(), mvn.getVersion(), pomFile));
		new MavenXpp3Writer().write(new FileOutputStream(pomFile.toFile()), mvn);

	}

	public static void addCompile(Model mvn) throws IOException, XmlPullParserException {
		Plugin plugin = new Plugin();
		mvn.getBuild().getPlugins().add(plugin);
		plugin.setGroupId("org.apache.maven.plugins");
		plugin.setArtifactId("maven-compiler-plugin");
		plugin.setVersion("3.8.1");
		PluginExecution execution = new PluginExecution();
		plugin.addExecution(execution);

		execution.setId("default-compile");
		execution.setPhase("compile");
		execution.addGoal("compile");

		//<skipMain>true</skipMain>
		StringBuilder pluginConfig = new StringBuilder("<configuration><release>8</release></configuration>");
		Xpp3Dom configuration = Xpp3DomBuilder.build(new ByteArrayInputStream(pluginConfig.toString().getBytes()), "UTF-8");
		execution.setConfiguration(configuration);
	}

	public static void addSources(Model mvn) throws IOException {
		Plugin plugin = new Plugin();
		mvn.getBuild().getPlugins().add(plugin);
		plugin.setGroupId("org.apache.maven.plugins");
		plugin.setArtifactId("maven-source-plugin");
		plugin.setVersion("3.0.1");
		PluginExecution execution = new PluginExecution();
		plugin.addExecution(execution);
		execution.setId("attach-sources");
		execution.setPhase("verify");
		execution.addGoal("jar-no-fork");

	}

	public void invokePOM(Path serviceBaseDir, String serviceName, String wsdlName) throws MavenInvocationException {

		MavenExecutionRequest executionRequest = session.getRequest();
		List<String> goals = executionRequest.getGoals();
		InvocationRequest invocationRequest = new DefaultInvocationRequest();
		invocationRequest.setBatchMode(true);
		invocationRequest.setPomFile(serviceBaseDir.resolve("pom.xml").toFile());
		invocationRequest.setGoals(goals);
		invocationRequest.setDebug(session.getRequest().isShowErrors());

		Invoker invoker = new DefaultInvoker();
		invoker.setOutputHandler(new LogOutputHandler(getLog()));

		// execute:
		InvocationResult invocationResult = invoker.execute(invocationRequest);
		if (invocationResult.getExitCode() != 0) {
			throw new MavenInvocationException("WSDL pom invocation failed");
		}
	}

	public static class LogOutputHandler implements InvocationOutputHandler {
		private Log log;

		public LogOutputHandler(Log log) {
			this.log = log;
		}

		@Override
		public void consumeLine(String line) {
			if (line.startsWith("[INFO]")) {
				log.info(line.substring(6));
			} else if (line.startsWith("[WARNING]")) {
				log.warn(line.substring(9));
			} else if (line.startsWith("[ERROR]")) {
				log.error(line.substring(7));
			} else if (line.startsWith("[DEBUG]")) {
				log.debug(line.substring(7));
			}

		}

	}
}
