Hello World Demo using Document/Literal Style and XMLBeans
=============================================

This demo illustrates the use of the JAX-WS APIs and with the XMLBeans data binding
to run a simple client against a standalone server using SOAP 1.1 over HTTP.

It also shows how CXF configuration can be used to enable schema validation
on the client and/or server side: By default the message parameters would not
be validated, but the presence of the cxf.xml configuration file on
the classpath, and its content change this default behavior:
The configuration file specifies that 

a) if a JAX-WS client proxy is created for port {http://apache.org/hello_world_soap_http}SoapPort
it should have schema validation enabled.

b) if a JAX-WS server endpoint is created for port {http://apache.org/hello_world_soap_http}SoapPort
it should have schema validation enabled.

The client's second greetMe invocation causes an exception (a marshalling
error) on the client side, i.e. before the request with the invalid parameter
goes on the wire.
After commenting the definition of the <jaxws:client> element in cxf.xml you 
will notice that the client's second greetMe invocation still throws an exception,
but that this time the exception is caused by an unmarshalling error on the
server side.
Commenting both elements, or renaming/removing the cfg.xml file, and thus
restoring the default behavior, results in the second greetMe invocation
not causing an exception.

Please review the README in the samples directory before continuing.


Prerequisite
------------

If your environment already includes cxf-manifest.jar on the
CLASSPATH, and the JDK and ant bin directories on the PATH
it is not necessary to set the environment as described in
the samples directory README.  If your environment is not
properly configured, or if you are planning on using wsdl2java,
javac, and java to build and run the demos, you must set the
environment.


Building and running the demo using Ant
---------------------------------------
From the base directory of this sample (i.e., where this README file is
located), the Ant build.xml file can be used to build and run the demo. 
The server and client targets automatically build the demo.

Using either UNIX or Windows:

  ant server  (from one command line window)
  ant client  (from a second command line window)
  
  You can also publish or consumer the demo webservice from spring
  
  ant spring.server  (from one command line window)
  ant spring.client  (from a second command line window)

To remove the code generated from the WSDL file and the .class
files, run "ant clean".


Building and running the demo using Maven
---------------------------------------

From the base directory of this sample (i.e., where this README file is
located), the pom.xml file is used to build and run the demo. 

Using either UNIX or Windows:

  mvn install   (builds the demo)
  mvn -Pserver  (from one command line window)
  mvn -Pclient  (from a second command line window)

To remove the code generated from the WSDL file and the .class
files, run "mvn clean".



Building the demo using wsdl2java and javac
-------------------------------------------

From the base directory of this sample (i.e., where this README file is
located) first create the target directory build/classes and then 
generate code from the WSDL file.

For UNIX:
  mkdir -p build/classes

  wsdl2java -d build/classes -compile ./wsdl/hello_world.wsdl

For Windows:
  mkdir build\classes
    Must use back slashes.

  wsdl2java -d build\classes -compile .\wsdl\hello_world.wsdl
    May use either forward or back slashes.

Now compile the provided client and server applications with the commands:

For UNIX:  
  
  export CLASSPATH=$CLASSPATH:$CXF_HOME/lib/cxf-manifest.jar:./build/classes
  javac -d build/classes src/demo/hw/client/*.java
  javac -d build/classes src/demo/hw/server/*.java

For Windows:
  set classpath=%classpath%;%CXF_HOME%\lib\cxf-manifest.jar;.\build\classes
  javac -d build\classes src\demo\hw\client\*.java
  javac -d build\classes src\demo\hw\server\*.java


Running the demo using java
---------------------------

From the base directory of this sample (i.e., where this README file is
located) run the commands, entered on a single command line:

For UNIX (must use forward slashes):
    java -Djava.util.logging.config.file=$CXF_HOME/etc/logging.properties
         demo.hw.server.Server &

    java -Djava.util.logging.config.file=$CXF_HOME/etc/logging.properties
         demo.hw.client.Client ./wsdl/hello_world.wsdl

The server process starts in the background.  After running the client,
use the kill command to terminate the server process.

For Windows (may use either forward or back slashes):
  start 
    java -Djava.util.logging.config.file=%CXF_HOME%\etc\logging.properties
         demo.hw.server.Server

    java -Djava.util.logging.config.file=%CXF_HOME%\etc\logging.properties
       demo.hw.client.Client .\wsdl\hello_world.wsdl

A new command windows opens for the server process.  After running the
client, terminate the server process by issuing Ctrl-C in its command window.

To remove the code generated from the WSDL file and the .class
files, either delete the build directory and its contents or run:

  ant clean


Building and running the demo in a servlet container
----------------------------------------------------

Please refer to samples directory README for building demo in a servlet container.

Using ant, run the client application with the command:

  ant client-servlet -Dbase.url=http://localhost:#

Where # is the TCP/IP port used by the servlet container,
e.g., 8080.

Or
  ant client-servlet -Dhost=localhost -Dport=8080

You can ignore the -Dhost and -Dport if your tomcat setup is same, i.e ant client-servlet

Using java, run the client application with the command:

  For UNIX:
    
    java -Djava.util.logging.config.file=$CXF_HOME/etc/logging.properties
         demo.hw.client.Client http://localhost:#/helloworld/services/hello_world?wsdl

  For Windows:

    java -Djava.util.logging.config.file=%CXF_HOME%\etc\logging.properties
       demo.hw.client.Client http://localhost:#/helloworld/services/hello_world?wsdl

Where # is the TCP/IP port used by the servlet container,
e.g., 8080.


Running demo with HTTP GET
--------------------------
APACHE CXF support HTTP GET to invoke the service, instead of running 

   ant client

you can use 

   ant client.get 

to invoke the service with simple HttpURLConnection, or you can even
use your favorite browser to get the results back.
