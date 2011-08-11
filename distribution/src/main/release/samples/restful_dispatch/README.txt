RESTful Hello World Demo 
========================

The demo shows REST based Web Services using the JAX-WS Provider/Dispatch. 
The REST server provides the following services: 

A RESTful customer service is provided on URL http://localhost:9000/customerservice/customer. 
Users access this URI to query or update customer info.

A HTTP GET request to URL http://localhost:9000/customerservice/customer returns 
a list of customer hyperlinks. This allows client navigates through the 
application states. The XML document returned:

<Customers>
  <Customer href="http://localhost/customerservice/customer?id=1234">
      <id>1234</id>
  </Customer>
  <Customer href="http://localhost/customerservice/customer?id=1235"> 
      <id>1235</id>
  </Customer>
  <Customer href="http://localhost/customerservice/customer?id=1236"> 
      <id>1236</id>
  </Customer>
</Customers>

A HTTP GET request to URL http://localhost:9000/customerservice/customer?id=1234 
returns a customer instance whose id is 1234. The XML document returned:

<Customer>
  <id>1234</id>
  <name>John</name>
  <phoneNumber>123456</phoneNumber>
</Customer>

A HTTP POST request to URL http://localhost:9000/customerservice/customer 
with the data:

<Customer>
  <id>1234</id>
  <name>John</name>
  <phoneNumber>123456</phoneNumber>
</Customer>

updates customer 1234 with the data provided. 

The client code demonstrates how to send HTTP POST with XML data using 
JAX-WS Dispatch and how to send HTTP GET using URL.openStream(). The 
server code demonstrates how to build a RESTful endpoints through 
JAX-WS Provider interface.

Please review the README in the samples directory before continuing.


Prerequisites
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
    

To remove the code generated from the WSDL file and the .class
files, run "ant clean".


Building and running the demo using maven
---------------------------------------

From the base directory of this sample (i.e., where this README file is
located), the maven pom.xml file can be used to build and run the demo. 


Using either UNIX or Windows:

  mvn install
  mvn -Pserver  (from one command line window)
  mvn -Pclient  (from a second command line window)
    

To remove the target dir, run mvn clean".


Building the demo using wsdl2java and javac
-------------------------------------------

From the base directory of this sample (i.e., where this README file is
located), first create the target directory build/classes and then 
compile the provided client and server applications with the commands:

For UNIX:  
  mkdir -p build/classes
  
  export CLASSPATH=$CLASSPATH:$CXF_HOME/lib/cxf-manifest.jar:./build/classes
  javac -d build/classes src/demo/restful/client/*.java
  javac -d build/classes src/demo/restful/server/*.java

For Windows:
  mkdir build\classes
    Must use back slashes.

  set classpath=%classpath%;%CXF_HOME%\lib\cxf-manifest.jar;.\build\classes
  javac -d build\classes src\demo\restful\client\*.java
  javac -d build\classes src\demo\restful\server\*.java


Finally, copy resource files into the build/classes directory with the commands:

For UNIX:    
  cp ./src/demo/restful/client/*.xml ./build/classes/demo/restful/client
  cp ./src/demo/restful/server/*.xml ./build/classes/demo/restful/server

For Windows:
  copy src\demo\restful\client\*.xml build\classes\demo\restful\client
  copy src\demo\restful\server\*.xml build\classes\demo\restful\server


Running the demo using java
---------------------------

From the base directory of this sample (i.e., where this README file is
located) run the commands, entered on a single command line:

For UNIX (must use forward slashes):
    java -Djava.util.logging.config.file=$CXF_HOME/etc/logging.properties
         demo.restful.server.Server &

    java -Djava.util.logging.config.file=$CXF_HOME/etc/logging.properties
         demo.restful.client.Client

The server process starts in the background.  After running the client,
use the kill command to terminate the server process.

For Windows (may use either forward or back slashes):
  start 
    java -Djava.util.logging.config.file=%CXF_HOME%\etc\logging.properties
         demo.restful.server.Server

    java -Djava.util.logging.config.file=%CXF_HOME%\etc\logging.properties
       demo.restful.client.Client

A new command windows opens for the server process.  After running the
client, terminate the server process by issuing Ctrl-C in its command window.

To remove the code generated from the WSDL file and the .class
files, either delete the build directory and its contents or run:

  ant clean
