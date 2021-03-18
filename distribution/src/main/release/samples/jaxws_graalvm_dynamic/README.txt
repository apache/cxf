JAX-WS GraalVM native-image Demo
===================

This demo shows how JAX-WS services and clients could use GraalVM's native-image 
capabilities and be packaged as native executables. This example demonstrates advanced
techniques to deal with dynamic class generation. 

Pre-requisites
---------------------------------------

GraalVM 20.3.0 or later distribution should be installed and pre-configured as 
default JVM runtime (using JAVA_HOME), see please instructions at [1].

The GraalVM's native-image tool should be installed, see please 
instructions at [2].

Building and running the demo using Maven
---------------------------------------

From the base directory of this sample (i.e., where this README file is
located), the pom.xml file is used to build and run the demo. 

Using either UNIX or Windows:

Than build server:  
  
  mvn clean package -f server -Pserver
  
This goal will produce 'server/target/jaxws-demo-server' executable (platform-dependent) 
which could be run right away: 

  On Windows: server\target\jaxws-demo-server.exe
  On Linux: ./server/target/jaxws-demo-server

Than build the client in capturing mode (please note that the server has to be up and running):
  
  mvn -f client -Pcapture  (from a second command line window)

Than build the client:
  
  mvn clean package -f client -Pclient (from a second command line window)

This goal will produce 'client/target/jaxws-demo-client' executable (platform-dependent) 
which could be run right away: 

  On Windows: client\target\jaxws-demo-client.exe client\	src\main\resources\addNumbers.wsdl
  On Linux: ./client/target/jaxws-demo-client client/src/main/resources/addNumbers.wsdl

The command should produce the following output (assuming the server is up and running):

  org.apache.cxf.wsdl.service.factory.ReflectionServiceFactoryBean buildServiceFromWSDL

  INFO: Creating Service {http://apache.org/handlers}AddNumbersService from WSDL: client/src/main/resources/addNumbers.wsdl
  Invoking addNumbers(10, 20)
  The result of adding 10 and 20 is 30.

  Invoking addNumbers(3, 5)
  The result of adding 3 and 5 is 8.

  Invoking addNumbers(-10, 5)
  Caught AddNumbersFault: Negative number cant be added!

To remove the code generated from the WSDL file and the .class
files, run :
  
  mvn clean -f server
  mvn clean -f client

References
---------------------------------------
[1] https://www.graalvm.org/downloads/
[2] https://www.graalvm.org/reference-manual/native-image/ 
