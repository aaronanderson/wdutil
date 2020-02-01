**Workday® is the registered trademark of Workday, Inc. This project is not affiliated with Workday, Inc. and Workday, Inc. does not endorse this project.**


# WD-Util - Workday Java Utility Libraries

This project consists of three different Java utilities for interacting with Workday.

## wdjws-maven-plugin
Workday provides a comprehensive set of [SOAP APIs](https://community.workday.com/api) for interacting with the platform. [JAX-WS](https://github.com/eclipse-ee4j/jax-ws-api) is the standard Java API for interfacing with SOAP services and [Maven plugins](https://github.com/eclipse-ee4j/metro-jax-ws/tree/master/jaxws-ri/extras/jaxws-maven-plugin) exist for building Java binds for Web Services. However there are several challenges with manually managing Workday JAX-WS bindings:

* The Workday SOAP API is partitioned by Workday functional area and often the use of more than one WSDL file is required
* The bindings need to be rebuilt for maintenance releases and version releases.
* The bindings are often deployed to an internal artifact repository for future reference

Due to the number of web services and frequency of updates JAX-WS binding build automation is desirable. wdjws-maven-plugin is a Maven plugin that downloads select Workday WSDL files, executes the JAX-WS import tool to generate the Java bindings, generates a dedicated Maven POM for them, and then packages and deploys the artifact. 

Dependency:

```
<dependency>
  <groupId>io.github.aaronanderson</groupId>
  <artifactId>wdjws-maven-plugin</artifactId>
  <version>1.0.0</version>
</dependency>

```


## wdjws-util
The Workday WSDL file contains the SOAP Endpoint URL in the SOAP Binding Address element and by default the compiled bindings will directly interact with the tenant the WSDL was downloaded from. However, multiple tenants are used in a Workday deployment and it would be advantageous to compile the WSDL bindings once and use them to communicate with multiple tenants. Additionally Workday uses WS-Security to secure web service access to Workday.

The wdjws-util library simplifies configuring the JAX-WS binding by setting the target endpoint and configuring security. It also includes a JAX-RS client for downloading files from the My Reports blobitory.

Dependency:

```
<dependency>
  <groupId>io.github.aaronanderson</groupId>
  <artifactId>wdjws-util</artifactId>
  <version>1.0.0</version>
</dependency>
```

 
## ui-automation
While Workday has an exhaustive API there are some operations that can only be performed through the user interface. Selenium WebDriver is used for programmatically controlling a web browser usually for the purpose of automated application testing. The Workday UI is subject to weekly changes and attempting to build elaborate manual test cases would be cost prohibitive. However in certain circumstances using UI automation to perform specific UI tasks could save time and effort.  

The ui-automation library contains common Selenium WebDriver based UI routines for common Workday tasks such as logging in, conducting a search, performing a related action, etc.

Dependency:

```
<dependency>
  <groupId>io.github.aaronanderson</groupId>
  <artifactId>ui-automation</artifactId>
  <version>1.0.0</version>
</dependency>
```

## Examples

Example usages are provided for both the wdjws and ui-automation libraries in a [separate repository](https://github.com/aaronanderson/wdutil-examples)  

