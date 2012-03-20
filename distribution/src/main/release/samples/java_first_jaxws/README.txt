Java First demo using jax-ws APIs and jsr-181
=============================================
This demo illustrates how to develop a service use the "code first"
approach using the JAX-WS APIs.

Building and running the demo using Maven
---------------------------------------
From the base directory of this sample (i.e., where this README file is
located), the pom.xml file is used to build and run the demo. 

Using either UNIX or Windows:

  mvn clean install   (builds the demo and creates a WAR file for optional Tomcat deployment)
  mvn -Pserver  (from one command line window -- only if using a non-WAR standalone service)
  mvn -Pclient  (from a second command line window)


To remove the code generated from the WSDL file and the .class
files, run "mvn clean".


Alternative: WAR deployment of service in a servlet container (e.g. Tomcat)
---------------------------------------------------------------------------

1.) Update the endpointAddress value in the client.Client class to the WAR-hosted 
value. The endpointAddress value will be installation-specific, but could be 
"http://localhost:8080/java_first_jaxws-{CXF Version}/services/hello_world" 
for a default local installation of Tomcat.  Replace {CXF Version} with the
CXF version declared in the parent POM -- for example, "2.6.1", "2.6.1-SNAPSHOT", 
etc.

2.) Manually copy the generated WAR file to the Tomcat webapps folder, or, if you
have Maven and Tomcat set up to use the Tomcat Maven Plugin 
(http://mojo.codehaus.org/tomcat-maven-plugin/) you can use the mvn tomcat:redeploy
command instead.  Important: if you're using this command and deploying on Tomcat 6
instead of Tomcat 7, update the tomcat-maven-plugin configuration in the pom.xml,
switching to the the Tomcat 6-specific "url" element.

Prior to running the client (mvn -Pclient) good to confirm the generated WSDL 
can be seen from a web browser at: 
http://{server}:{port}/java_first_jaxws-{CXF Version}/services/hello_world?wsdl
