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


Alternative: WAR deployment of service to Tomcat (either embedded or standalone)
---------------------------------------------------------------------------
1.) Update the endpointAddress value in the client.Client class to the WAR-hosted 
value, for running this sample on your local machine most likely:
"http://localhost:8080/java_first_jaxws/services/hello_world".

2.) Standalone Tomcat: Manually copy the generated WAR file to the Tomcat webapps folder, 
or, if you have Maven and Tomcat set up to use the Tomcat Maven Plugin 
(http://tomcat.apache.org/maven-plugin-2.1/tomcat7-maven-plugin/index.html) you can
use the mvn tomcat7:redeploy command instead.  

For embedded Tomcat: Just run mvn tomcat7:run-war from the project base folder.

Prior to running the client (mvn -Pclient) it would be good to confirm the 
generated WSDL can be seen from a web browser at:
http://localhost:8080/java_first_jaxws/services/hello_world?wsdl

