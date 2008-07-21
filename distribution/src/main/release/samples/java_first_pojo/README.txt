Java First POJO DEMO
====================

This demo illustrates how to develop a service use the "code first", pojo based.
This demo uses the JAXB Data binding by default, but you can use Aegis Data binding by 
uncomment below four lines:

(Client.java)
//import org.apache.cxf.aegis.databinding.AegisDatabinding;
//factory.getServiceFactory().setDataBinding(new AegisDatabinding());

(Server.java)
//import org.apache.cxf.aegis.databinding.AegisDatabinding;
//svrFactory.getServiceFactory().setDataBinding(new AegisDatabinding());


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

  ant server (from one command line window)
  ant client (from a second command line window)
    

To remove the code generated from the WSDL file and the .class
files, run "ant clean"

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


Building the demo using javac
------------------------------

From the base directory of this sample (i.e., where this README file is
located), first create the target directory build/classes:

For UNIX:
  mkdir -p build/classes

For Windows:
  mkdir build\classes
    Must use back slashes.

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
         demo.hw.client.Client

The server process starts in the background.  After running the client,
use the kill command to terminate the server process.

For Windows (may use either forward or back slashes):
  start 
    java -Djava.util.logging.config.file=%CXF_HOME%\etc\logging.properties
         demo.hw.server.Server

    java -Djava.util.logging.config.file=%CXF_HOME%\etc\logging.properties
       demo.hw.client.Client

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
         demo.hw.client.Client http://localhost:#/helloworld/services/hello_world

  For Windows:

    java -Djava.util.logging.config.file=%CXF_HOME%\etc\logging.properties
       demo.hw.client.Client http://localhost:#/helloworld/services/hello_world

Where # is the TCP/IP port used by the servlet container,
e.g., 8080.

