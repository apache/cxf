Generated JavaScript using jax-ws APIs and jsr-181
=============================================

This sample shows the generation of JavaScript client code from a
JAX-WS server.

Prerequisite
------------

If your environment already includes cxf-manifest.jar on the
CLASSPATH, and the JDK and ant bin directories on the PATH
it is not necessary to set the environment as described in
the samples directory README.  If your environment is not
properly configured, or if you are planning on using wsdl2java,
javac, and java to build and run the demos, you must set the
environment.


Building and running the demo using ant
---------------------------------------

From the base directory of this sample (i.e., where this README file is
located), the Ant build.xml file can be used to build and run the demo.

Using either UNIX or Windows:

  ant server  (from one command line window)


Building and running the demo using maven
---------------------------------------

From the base directory of this sample (i.e., where this README file is
located)

Using either UNIX or Windows:

  mvn install
  mvn -Pserver


Building the demo using javac
-----------------------------

From the base directory of this sample (i.e., where this README file is
located), first create the target directory build/classes and then compile 
the provided client and server code.

For UNIX:  
  
  mkdir -p build/classes
  export CLASSPATH=$CLASSPATH:$CXF_HOME/lib/cxf-manifest.jar:./build/classes
  javac -d build/classes src/demo/hw/server/*.java

For Windows:
  mkdir build\classes
  set classpath=%classpath%;%CXF_HOME%\lib\cxf-manifest.jar;.\build\classes
  javac -d build\classes src\demo\hw\server\*.java


Running the demo using java
---------------------------

From the base directory of this sample (i.e., where this README file is
located) run the commands, entered on a single command line:

For UNIX (must use forward slashes):
    java -Djava.util.logging.config.file=$CXF_HOME/etc/logging.properties
         demo.hw.server.Server &

The server process starts in the background.  After running the client,
use the kill command to terminate the server process.

For Windows (may use either forward or back slashes):
  start 
    java -Djava.util.logging.config.file=%CXF_HOME%\etc\logging.properties
         demo.hw.server.Server

A new command windows opens for the server process.  After running the
client, terminate the server process by issuing Ctrl-C in its command window.

Running the client in a browser
-------------------------------

Once the server is running, browse to:

  http://HOSTNAME:9000/Beverages.html

(Substitute your hostname for HOSTNAME.)

On the web page you see, click on the 'invoke' button to invoke the
very simple sayHi service, which takes no input and returns a single
string.
