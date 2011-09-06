MTOM Demo for SWA & XOP
=======================

This demo illustrates the use of a SOAP message 
with an attachment and XML-binary Optimized Packaging.

Please review the README in the samples directory before
continuing.


Building and running the demo using Maven
---------------------------------------
From the base directory of this sample (i.e., where this README file is
located), the maven pom.xml file can be used to build and run the demo. 

Using either UNIX or Windows:

  mvn install
  mvn -Perver  (from one command line window)
  mvn -Pclient  (from a second command line window)
    
To remove the code generated from the WSDL file and the .class
files, run mvn clean".


Building the demo using wsdl2java and javac
-------------------------------------------
If your environment already includes cxf-manifest.jar on the
CLASSPATH, and the JDK directories on the PATH
it is not necessary to set the environment as described in
the samples directory's README.  If your environment is not
properly configured, or if you are planning on using wsdl2java,
javac, and java to build and run the demos, you must set the
environment.

From the base directory of this sample (i.e., where this README file is
located), first create the target directory build/classes and then 
generate code from the WSDL file.

For UNIX:
  mkdir -p build/classes

  wsdl2java -d build/classes -compile ./wsdl/mtom_xop.wsdl

For Windows:
  mkdir build\classes
    Must use back slashes.

  wsdl2java -d build\classes -compile .\wsdl\mtom_xop.wsdl
    May use either forward or back slashes.

Now compile the provided client and server applications with the commands:

For UNIX:  
  
  export CLASSPATH=$CLASSPATH:$CXF_HOME/lib/cxf-manifest.jar:./build/classes
  javac -d build/classes src/demo/mtom/client/*.java
  javac -d build/classes src/demo/mtom/server/*.java
  cp src/demo/mtom/client/me.bmp build/classes/demo/mtom/client/me.bmp

For Windows:
  set classpath=%classpath%;%CXF_HOME%\lib\cxf-manifest.jar;.\build\classes
  javac -d build\classes src\demo\mtom\client\*.java
  javac -d build\classes src\demo\mtom\server\*.java
  copy src\demo\mtom\client\me.bmp build\classes\demo\mtom\client\me.bmp


Running the demo using java
---------------------------

From the base directory of this sample (i.e., where this README file is
located) run the commands, entered on a single command line:

For UNIX (must use forward slashes):
    java -Djava.util.logging.config.file=$CXF_HOME/etc/logging.properties
         demo.mtom.server.Server &

    java -Djava.util.logging.config.file=$CXF_HOME/etc/logging.properties
         demo.mtom.client.Client ./wsdl/mtom_xop.wsdl

The server process starts in the background.  After running the client,
use the kill command to terminate the server process.

For Windows (may use either forward or back slashes):
  start 
    java -Djava.util.logging.config.file=%CXF_HOME%\etc\logging.properties
         demo.mtom.server.Server

    java -Djava.util.logging.config.file=%CXF_HOME%\etc\logging.properties
       demo.mtom.client.Client .\wsdl\mtom_xop.wsdl

A new command windows opens for the server process.  After running the
client, terminate the server process by issuing Ctrl-C in its command window.


Building and running the demo in a servlet container
----------------------------------------------------
Please refer to samples directory README for building demo in a servlet container.

Using java, run the client application with the command:

  For UNIX:
    
    java -Djava.util.logging.config.file=$CXF_HOME/etc/logging.properties
         demo.mtom.client.Client http://localhost:#/mtom/services/mtom?wsdl

  For Windows:

    java -Djava.util.logging.config.file=%CXF_HOME%\etc\logging.properties
       demo.mtom.client.Client http://localhost:#/mtom/services/mtom?wsdl

Where # is the TCP/IP port used by the servlet container,
e.g., 8080.
