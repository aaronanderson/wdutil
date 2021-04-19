package wdutil.wdjrs.maven;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.net.ssl.HttpsURLConnection;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

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
import org.openapitools.codegen.ClientOptInput;
import org.openapitools.codegen.DefaultGenerator;
import org.openapitools.codegen.config.CodegenConfigurator;

@Mojo(name = "oapigen", defaultPhase = LifecyclePhase.GENERATE_SOURCES, requiresDependencyResolution = ResolutionScope.RUNTIME)
public class WDJWRMojo extends AbstractMojo {

    //Public site per Google search of "Workday REST API"
    private static String WORKDAY_OPENAPI_URL = "https://community.workday.com/sites/default/files/file-hosting/restapi/index.html";

    public static final Pattern JSON_HREF = Pattern.compile("href=\\'(.*/(\\w+)\\.json)\\'");
    public static final Pattern NAME_FORMAT = Pattern.compile("([^_]+)_([^_]+)_?([\\d]+)?");

    @Parameter(required = true)
    private String wdVersion;

    @Parameter(defaultValue = "${project.basedir}/src/specs")
    private File localSpecsDirectory;

    @Parameter
    private String[] services;

    @Parameter
    private Map aliases = new HashMap<>();

    @Parameter(defaultValue = "false")
    private boolean includeAPIVersion;

    @Parameter(defaultValue = "com.workday")
    private String packageName;

    @Parameter(defaultValue = "${project.groupId}")
    private String groupId;

    @Parameter
    private String artifactId;

    @Parameter
    private String version;

    @Parameter(defaultValue = "8")
    private String jdkVersion;

    ////required for the buiild

    @Parameter(defaultValue = "${project.build.directory}/generated-wdrs")
    private File sourceDestDir;

    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    private MavenProject project;

    @Parameter(defaultValue = "${session}", readonly = true)
    private MavenSession session;

    @Component
    private MavenProjectHelper projectHelper;

    public void execute() throws MojoExecutionException {
        try {

            Set<String> serviceSet = services != null ? new HashSet<>(Arrays.asList(services)) : new HashSet<>();
            Map<String, Spec> latestSpecs = new HashMap<>();

            if (localSpecsDirectory.exists() && localSpecsDirectory.isDirectory() && localSpecsDirectory.list().length > 0) {//if (services != null && services.length > 0) {
                loadSpecsFromLocal(serviceSet, latestSpecs);
            } else {
                loadSpecsFromWebsite(serviceSet, latestSpecs);
            }

            for (Spec spec : latestSpecs.values()) {
                getLog().info(String.format("Processing spec file %s\n", spec.specURL));
                String specFileName = spec.specURL.getPath().substring(spec.specURL.getPath().lastIndexOf("/") + 1);
                String serviceName = aliases.containsKey(spec.specName) ? (String) aliases.get(spec.specName) : packageNameFormat(spec);
                Path serviceBaseDir = sourceDestDir.toPath().resolve(serviceName);
                Path specBaseDir = serviceBaseDir.resolve("src/main/resources/META-INF/openapi");
                Files.createDirectories(specBaseDir);
                Path specPath = specBaseDir.resolve(specFileName);

                if ("file".equals(spec.specURL.getProtocol())) {
                    URLConnection fileConn = spec.specURL.openConnection();
                    Files.copy(fileConn.getInputStream(), specPath, StandardCopyOption.REPLACE_EXISTING);
                } else {
                    HttpsURLConnection httpsConn = (HttpsURLConnection) spec.specURL.openConnection();
                    Files.copy(httpsConn.getInputStream(), specPath, StandardCopyOption.REPLACE_EXISTING);
                    httpsConn.disconnect();
                }

                generateOpenAPIClient(serviceName, specPath, serviceBaseDir);
                //generatePOM(serviceBaseDir, serviceName, spec.specName);
                invokePOM(serviceBaseDir);

            }

        } catch (Throwable e) {
            throw new MojoExecutionException("wsimport error", e);
        }

    }

    public void loadSpecsFromLocal(Set<String> serviceSet, Map<String, Spec> latestSpecs) throws Exception {
        List<File> files = Files.list(localSpecsDirectory.toPath()).filter(Files::isRegularFile).map(Path::toFile).collect(Collectors.toList());
        for (File spec : files) {
            addFilterSpec(spec.toPath().getFileName().toString(), spec.toURI().toURL(), serviceSet, latestSpecs);
        }

    }

    public void loadSpecsFromWebsite(Set<String> serviceSet, Map<String, Spec> latestSpecs) throws Exception {

        URL WorkdayAPIURL = new URL(WORKDAY_OPENAPI_URL);
        HttpsURLConnection indexConn = (HttpsURLConnection) WorkdayAPIURL.openConnection();
        BufferedReader indexPage = new BufferedReader(new InputStreamReader(indexConn.getInputStream()));

        String str = null;
        out: while ((str = indexPage.readLine()) != null) {
            Matcher m = JSON_HREF.matcher(str);
            while (m.find()) {
                String specPath = m.group(1);
                URL specURL = new URL(WORKDAY_OPENAPI_URL.substring(0, WORKDAY_OPENAPI_URL.lastIndexOf('/')) + "/" + specPath);
                final String fspecFileName = m.group(2);
                addFilterSpec(fspecFileName, specURL, serviceSet, latestSpecs);
            }
        }

        indexPage.close();
        indexConn.disconnect();
    }

    private void addFilterSpec(String specFileName, URL specURL, Set<String> serviceSet, Map<String, Spec> latestSpecs) {
        if (serviceSet.isEmpty() || serviceSet.stream().filter(s -> specFileName.contains(s)).findFirst().isPresent()) {
            String nspecFileName = specFileName.replaceAll("^curated_?", "");
            Matcher n = NAME_FORMAT.matcher(nspecFileName);
            if (n.find()) {
                String serviceName = n.group(1);
                String majorVersion = n.group(2);
                String minorVersion = n.group(3);
                Spec currentSpec = latestSpecs.get(serviceName);
                if (currentSpec == null || currentSpec.majorVersion.compareTo(majorVersion) < 0 || (currentSpec.minorVersion != null && minorVersion != null && currentSpec.minorVersion.compareTo(minorVersion) < 0)) {
                    Spec spec = new Spec(serviceName, majorVersion, minorVersion, specURL);
                    latestSpecs.put(spec.specName, spec);
                }

            }
        }

    }

    private String packageNameFormat(Spec spec) {
        StringBuilder packageName = new StringBuilder();
        for (int i = 0; i < spec.specName.length(); i++) {
            char c = spec.specName.charAt(i);
            if (Character.isUpperCase(c)) {
                if (i > 0) {
                    packageName.append('_');
                }
                packageName.append(Character.toLowerCase(c));
            } else {
                packageName.append(c);
            }
        }
        if (includeAPIVersion) {
            packageName.append('_');
            packageName.append(spec.majorVersion);
            if (spec.minorVersion != null) {
                packageName.append('_');
                packageName.append(spec.minorVersion);
            }
        }
        return packageName.toString();
    }

    public static class Spec {
        final URL specURL;
        final String specName;
        final String majorVersion;
        final String minorVersion;

        public Spec(String specName, String majorVersion, String minorVersion, URL specURL) {
            this.specName = specName;
            this.majorVersion = majorVersion;
            this.minorVersion = minorVersion;
            this.specURL = specURL;
        }

    }

    public void generateOpenAPIClient(String serviceName, Path specPath, Path serviceBaseDir) throws IOException, ParserConfigurationException, TransformerException {
        CodegenConfigurator configurator = new CodegenConfigurator();
        configurator.setVerbose(false);
        configurator.setGroupId(groupId);
        configurator.setArtifactId(artifactId != null ? (artifactId + "-" + serviceName) : serviceName);
        configurator.setArtifactVersion(version != null ? version : wdVersion);

        configurator.setOutputDir(serviceBaseDir.toAbsolutePath().toString());

        configurator.setInputSpec(specPath.toAbsolutePath().toString());
        configurator.setGeneratorName("java");
        configurator.setLibrary("microprofile");
        configurator.setInvokerPackage(String.format("%s.%s", packageName, serviceName));
        configurator.setApiPackage(String.format("%s.%s.api", packageName, serviceName));
        configurator.setModelPackage(String.format("%s.%s.model", packageName, serviceName));

        configurator.setGenerateAliasAsModel(true);
        configurator.addAdditionalProperty("disableMultipart", true);
        ClientOptInput input = configurator.toClientOptInput();
        new DefaultGenerator().opts(input).generate();

    }

    public void generatePOM(Path serviceBaseDir, String serviceName, String specName) throws IOException, XmlPullParserException {
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
        mvn.setProperties(new Properties());
        mvn.getProperties().setProperty("project.build.sourceEncoding", "UTF-8");

        addCompile(mvn, jdkVersion);
        addSources(mvn);

        getLog().info(String.format("Writing Maven WSDL %s:%s:%s pom to file %s\n", mvn.getGroupId(), mvn.getArtifactId(), mvn.getVersion(), pomFile));
        new MavenXpp3Writer().write(new FileOutputStream(pomFile.toFile()), mvn);

    }

    public static void addCompile(Model mvn, String jdkVersion) throws IOException, XmlPullParserException {
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
        StringBuilder pluginConfig = new StringBuilder("<configuration><release>").append(jdkVersion).append("</release></configuration>");
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

    public void invokePOM(Path serviceBaseDir) throws MavenInvocationException {

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
            throw new MavenInvocationException("OpenAPI pom invocation failed");
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
