JavaScript Client Demo using Document/Literal Style
=============================================

This demo illustrates the use of the JavaScript client generator. This
demo deploys the a service based on the wsdl_first demo, and then
provides a browser-compatible client that communicates with it. Please
read the README.txt for the wsdl_first sample for more information on
the service.

The cxf.xml for this sample configures the embedded Jetty web server
to deliver static HTML content from the 'staticContent' directory.

Please review the README in the samples directory before continuing.

Please see the wiki user documentation for complete information on
JavaScript client feature.

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
located), the Ant build.xml file can be used to build and run the
server for the demo. 

Using either UNIX or Windows:

  ant server  (from one command line window)
    

To remove the code generated from the WSDL file and the .class
files, run "ant clean".


Building and running the demo using maven
---------------------------------------

From the base directory of this sample (i.e., where this README file is
located)

Using either UNIX or Windows:

  mvn install
  mvn -Pserver
  mvn -Pclient


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

Now compile the server applications with the commands:

For UNIX:  
  
  export CLASSPATH=$CLASSPATH:$CXF_HOME/lib/cxf-manifest.jar:./build/classes
  javac -d build/classes src/demo/hw/server/*.java

For Windows:
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
use the kill command to terminate the server process. Or wait five
minutes, and the server will exit.

For Windows (may use either forward or back slashes):
  start 
    java -Djava.util.logging.config.file=%CXF_HOME%\etc\logging.properties
         demo.hw.server.Server

On Windows, a new command windows opens for the server process.  After running the
client, terminate the server process by issuing Ctrl-C in its command window.

To remove the code generated from the WSDL file and the .class
files, either delete the build directory and its contents or run:

  ant clean


Running the client in a browser
-------------------------------

Once the server is running, browse to:

  http://HOSTNAME:9000/HelloWorld.html

(Substitute your hostname for HOSTNAME.)

On the web page you see, click on the 'invoke' button to invoke the
very simple sayHi service, which takes no input and returns a single
string.

