JAX-RS Content Negotiation Demo
===============================

The demo shows how to do content negotiation so that the same resource can 
be served using multiple representations. 

A RESTful customer service is provided on URL http://localhost:9000/customers. 
Users access this URI to operate on customer.

A HTTP GET request to URL http://localhost:9000/customerservice/customers/123
with Accept header set to "application/xml" returns a customer instance in XML
format. The XML document returned:

<Customer>
  <id>123</id>
  <name>John</name>
</Customer>

A HTTP GET request to URL http://localhost:9000/customerservice/customers/123
with Accept header set to "application/json" returns a customer instance in JSON
format. The JSON document returned:

{"Customer":{"id":"123","name":"John"}}

A HTTP GET request to URL http://localhost:9000/customerservice/customers/123
without setting Accept header explicitly returns a customer instance in JSON 
format. This is because the Accept header will be absent from the request when using 
HTTP Client, in which case the CXF will treat the Accept content type as "*/*". 
The JSON document returned:

{"Customer":{"id":"123","name":"John"}}

Please review the README in the samples directory before
continuing.


Prerequisites
-------------

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
    

To remove the .class files, run "ant clean".


Building and running the demo using maven
---------------------------------------

From the base directory of this sample (i.e., where this README file is
located), the maven pom.xml file can be used to build and run the demo. 


Using either UNIX or Windows:

  mvn install
  mvn -Pserver  (from one command line window)
  mvn -Pclient  (from a second command line window)
    

To remove the target dir, run mvn clean".


Building the demo using javac
-------------------------------------------

From the base directory of this sample (i.e., where this README file is
located), first create the target directory build/classes and then 
compile the provided client and server applications with the commands:

For UNIX:  
  mkdir -p build/classes
  
  export CLASSPATH=$CLASSPATH:$CXF_HOME/lib/cxf-manifest.jar:./build/classes
  javac -d build/classes src/demo/jaxrs/client/*.java
  javac -d build/classes src/demo/jaxrs/server/*.java

For Windows:
  mkdir build\classes
    Must use back slashes.

  set classpath=%classpath%;%CXF_HOME%\lib\cxf-manifest.jar;.\build\classes
  javac -d build\classes src\demo\jaxrs\client\*.java
  javac -d build\classes src\demo\jaxrs\server\*.java


Finally, copy resource files into the build/classes directory with the commands:

For UNIX:    
  cp ./src/demo/jaxrs/client/*.xml ./build/classes/demo/jaxrs/client
  cp ./src/demo/jaxrs/server/*.xml ./build/classes/demo/jaxrs/server

For Windows:
  copy src\demo\jaxrs\client\*.xml build\classes\demo\jaxrs\client
  copy src\demo\jaxrs\server\*.xml build\classes\demo\jaxrs\server


Running the demo using java
---------------------------

From the samples/jax-rs/content_negotiation directory run the following commands. They 
are entered on a single command line.

For UNIX (must use forward slashes):
    java -Djava.util.logging.config.file=$CXF_HOME/etc/logging.properties
         demo.jaxrs.server.Server &

    java -Djava.util.logging.config.file=$CXF_HOME/etc/logging.properties
         demo.jaxrs.client.Client

The server process starts in the background.  After running the client,
use the kill command to terminate the server process.

For Windows (may use either forward or back slashes):
  start 
    java -Djava.util.logging.config.file=%CXF_HOME%\etc\logging.properties
       demo.jaxrs.server.Server

    java -Djava.util.logging.config.file=%CXF_HOME%\etc\logging.properties
       demo.jaxrs.client.Client

A new command windows opens for the server process.  After running the
client, terminate the server process by issuing Ctrl-C in its command window.

To remove the code generated from the WSDL file and the .class
files, either delete the build directory and its contents or run:

  ant clean
