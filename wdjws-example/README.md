#USAGE

Edit the test.properties file and populate the masked values with connection information for a GMS tenant.

Normally the Java classes that depend on the binding jar file would be located in a separate project but for the sake of reduced complexity both the bindings and example usage exist in the same project. This configuration creates a cyclic dependency that needs to be addressed.

During the first build comment out the following dependency:

```
<!--
<dependency>
	<groupId>com.workday</groupId>
	<artifactId>intsys</artifactId>
	<version>33.0</version>
	<scope>test</scope>
</dependency>
-->

```

perform the build:

`mvn clean install`

After the bindings are successfully built and deployed remove the comments, re-run the build, and observe the successfully JUnit test case executions.  

Also observe the `distributionManagement` element. The wdjws-maven-plugin will copy this element and any extension into the JAX-WS WSDL binding generated POM so that the maven `deploy` command can be successfully invoked and deploy the artifact to a local or remote repository.  



