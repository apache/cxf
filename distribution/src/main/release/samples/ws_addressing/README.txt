WS-Addressing Demo
==================

This demo shows how WS-Addressing support in Apache CXF may be enabled.  

The client and server both apply the addressing feature to the bus.
This ensures installation of the WS-Addressing interceptors,
comprising a logical interceptor (MAPAggregator)
responsible for aggregating the WS-A MessageAddressingProperties for
the current message, and a protocol interceptor (MAPCodec) responsible for
encoding/decoding these properties as SOAP Headers. 

A demo-specific logging.properties file is used to snoop the log messages
relating to WS-A Headers and display these to the console in concise form.

Normally the WS-Addressing MessageAddressProperties are generated and
propagated implicitly, without any intervention from the
application. In certain circumstances however, the application may wish
to participate in MAP assembly, for example to associate a sequence of
requests via the RelatesTo header. This demo illustrates both implicit
and explicit MAP propagation.

This demo also illustrates usage of the decoupled HTTP transport, whereby
a separate server->client HTTP connection is used to deliver the responses.
Note the normal HTTP mode (where the response is delivered on the back-
channel of the original client->server HTTP connection) may of course also
be used  with WS-Addressing; in this case the <wsa:ReplyTo> header is set to
a well-known anonymous URI, "http://www.w3.org/2005/08/addressing/anonymous".

In all other respects this demo is based on the basic hello_world sample,
illustrating that WS-Addressing usage is independent of the application.
One notable addition to the familiar hello_world WSDL is the usage
of the <wsaw:UsingAddressing> extension element to indicate the
WS-Addressing support is enabled for the service endpoint.

Please review the README in the samples directory before continuing.


Prerequisite
------------

If your environment already includes cxf-manifest.jar on the CLASSPATH,
and the JDK and ant bin directories on the PATH, it is not necessary to
run the environment script described in the samples directory README.
If your environment is not properly configured, or if you are planning
on using wsdl2java, javac, and java to build and run the demos, you must
set the environment by running the script.


Building and running the demo using Ant
---------------------------------------
From the base directory of this sample (i.e., where this README file is
located), the Ant build.xml file can be used to build and run the demo. 
The server and client targets automatically build the demo.

Using either UNIX or Windows:

  ant server  (from one command line window)
  ant client  (from a second command line window)

Both client and server will use the MAPAggregator and MAPCodec
handlers to aggregate and encode the WS-Addressing MAPs.

To remove the code generated from the WSDL file and the .class
files, run "ant clean".


Building and running the demo using maven
---------------------------------------
From the base directory of this sample (i.e., where this README file is
located), the maven pom.xml file can be used to build and run the demo. 

Using either UNIX or Windows:

  mvn install (builds the demo)
  mvn -Pserver  (from one command line window)
  Mvn -Pclient  (from a second command line window)

Both client and server will use the MAPAggregator and MAPCodec
handlers to aggregate and encode the WS-Addressing MAPs.

To remove the code generated from the WSDL file and the .class
files, run "mvn clean".


Building the demo using wsdl2java and javac
-------------------------------------------

From the base directory of this sample (i.e., where this README file is
located) first create the target directory build/classes and then 
generate code from the WSDL file.


For UNIX:
  mkdir -p build/classes

  wsdl2java -d build/classes -compile ./wsdl/hello_world_addr.wsdl

For Windows:
  mkdir build\classes
    Must use back slashes.

  wsdl2java -d build\classes -compile .\wsdl\hello_world_addr.wsdl
    May use either forward or back slashes.

Now compile the provided client and server applications with the commands:

For UNIX:  
  
  export CLASSPATH=$CLASSPATH:$CXF_HOME/lib/cxf-manifest.jar:./build/classes:.
  javac -d build/classes src/demo/ws_addressing/common/*.java
  javac -d build/classes src/demo/ws_addressing/client/*.java
  javac -d build/classes src/demo/ws_addressing/server/*.java

For Windows:
  set classpath=%classpath%;%CXF_HOME%\lib\cxf-manifest.jar;.\build\classes;.
  javac -d build\classes src\demo\ws_addressing\common\*.java
  javac -d build\classes src\demo\ws_addressing\client\*.java
  javac -d build\classes src\demo\ws_addressing\server\*.java


Running the demo using java
---------------------------

From the base directory of this sample (i.e., where this README file is
located) run the commands, entered on a single command line:

For UNIX (must use forward slashes):
    java -Djava.util.logging.config.file=./logging.properties
         -Dcxf.config.file=server.xml
         demo.ws_addressing.server.Server &

    java -Djava.util.logging.config.file=./logging.properties
         -Dcxf.config.file=client.xml
         demo.ws_addressing.client.Client ./wsdl/hello_world_addr.wsdl

The server process starts in the background.

For Windows (may use either forward or back slashes):
  start 
    java -Djava.util.logging.config.file=.\logging.properties
         -Dcxf.config.file=server.xml
         demo.ws_addressing.server.Server

    java -Djava.util.logging.config.file=.\logging.properties
         -Dcxf.config.file=client.xml
         demo.ws_addressing.client.Client .\wsdl\hello_world_addr.wsdl

The server process starts in a new command window.

After running the client, terminate the server process.

To remove the code generated from the WSDL file and the .class
files, either delete the build directory and its contents or run:

  ant clean

