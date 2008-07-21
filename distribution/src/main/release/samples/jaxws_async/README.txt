JAX-WS Asynchronous Demo using Document/Literal Style
=====================================================

This demo illustrates the use of the JAX-WS asynchronous 
invocation model. Please refer to the JAX-WS 2.0 specification
(http://jcp.org/aboutJava/communityProcess/pfd/jsr224/index.html)
for background.

The asynchronous model allows the client thread to continue after 
making a two-way invocation without being blocked while awaiting a 
response from the server. Once the response is available, it is
delivered to the client application asynchronously using one
of two alternative approaches:

- Callback: the client application implements the 
javax.xml.ws.AsyncHandler interface to accept notification
of the response availability

- Polling: the client application periodically polls a
javax.xml.ws.Response instance to check if the response
is available

This demo illustrates both approaches.

Additional methods are generated on the Service Endpoint
Interface (SEI) to provide this asynchrony, named by 
convention with the suffix "Async".

As many applications will not require this functionality,
the asynchronous variants of the SEI methods are omitted
by default to avoid polluting the SEI with unnecessary 
baggage. In order to enable generation of these methods,
a bindings file (wsdl/async_bindings.xml) is passed
to the wsdl2java generator. 

Please review the README in the samples directory before
continuing.



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
located), run the following wsdl2java command to generate classes 
required in the async case.

For UNIX:
  mkdir -p build/classes
  wsdl2java -d build/classes -b ./wsdl/async_binding.xml -compile ./wsdl/hello_world_async.wsdl

For Windows:
  mkdir build\classes
    Must use back slashes.
  wsdl2java -d build\classes -b .\wsdl\async_binding.xml -compile .\wsdl\hello_world_async.wsdl
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
         demo.hw.client.Client ./wsdl/hello_world_async.wsdl

The server process starts in the background.  After running the client,
use the kill command to terminate the server process.

For Windows (may use either forward or back slashes):
  start 
    java -Djava.util.logging.config.file=%CXF_HOME%\etc\logging.properties
         demo.hw.server.Server

    java -Djava.util.logging.config.file=%CXF_HOME%\etc\logging.properties
       demo.hw.client.Client .\wsdl\hello_world_async.wsdl

A new command windows opens for the server process.  After running the
client, terminate the server process by issuing Ctrl-C in its command window.

To remove the code generated from the WSDL file and the .class
files, either delete the build directory and its contents or run:

  ant clean
