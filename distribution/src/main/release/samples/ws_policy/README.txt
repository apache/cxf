WS-Policy Demo
==============

This demo shows how the CXF WS-Policy framework in Apache CXF uses
WSDL 1.1 Policy attachments to enable the use of WS-Addressing.

While most of the demo is very similar to the ws_addressing demo, there
are two major differences:
1.  Policy element containing an Addressing assertion is attached to 
the port element in the demo's wsdl.
2. The configuration files for the client and server specify that the 
CXF policy engine should be enabled. 
In addition, the configuration file for the server tells the policy
engine where to look for external policy attachments. This is necessary
since neither the implementor not the interface specify wsdlLocation
in their WebService annotation (the client could also use external policy
attachments instead of WSDL 1.1 attachments).
Note that, apart from the configuration of the decoupled http endpoint 
on which the client receives responses from the server, nothing else
needs to be configured. In particular, there is no need to specify the
WS-Addressing interceptors. The internals of what is involved in
ensuring that addressing headers are added to the messages (namely:
the addition of two addressing interceptors to the inbound and outbound
fault and message processing chains) are left to the policy framework,
or rather: its built-in support for the Addressing assertion.

Please review the README in the samples directory before continuing.


Prerequisite
------------

If your environment already includes cxf-manifest.jar on the CLASSPATH,
and the JDK and ant bin directories on the PATH, it is not necessary to
run the environment script described in the samples directory README.
If your environment is not properly configured, or if you are planning
on using wsdl2java, javac, and java to build and run the demos, you must
set the environment by running the script.


Building and running the demo using Maven
-----------------------------------------

From the samples/ws_policy directory, the Maven pom.xml can be used to
build and run the demo.  

Using either UNIX or Windows:

  mvn install (builds the demo)
  mvn -Pserver
  mvn -Pclient

Although not explicitly instructed to use these interceptors, 
both client and server will use the MAPAggregator and MAPCodec
interceptors to aggregate and encode the WS-Addressing MAPs.

To remove the code generated from the WSDL file and the .class
files, run "mvn clean".


Building the demo using wsdl2java and javac
------------------------------------------

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
  javac -d build/classes src/demo/ws_policy/common/*.java
  javac -d build/classes src/demo/ws_policy/client/*.java
  javac -d build/classes src/demo/ws_policy/server/*.java

For Windows:
  set classpath=%classpath%;%CXF_HOME%\lib\cxf-manifest.jar;.\build\classes;.
  javac -d build\classes src\demo\ws_policy\common\*.java
  javac -d build\classes src\demo\ws_policy\client\*.java
  javac -d build\classes src\demo\ws_policy\server\*.java


Running the demo using java
---------------------------

From the base directory of this sample (i.e., where this README file is
located) run the commands, entered on a single command line:

For UNIX (must use forward slashes):
    java -Djava.util.logging.config.file=./logging.properties
         -Dcxf.config.file=server.xml
         demo.ws_policy.server.Server &

    java -Djava.util.logging.config.file=./logging.properties
         -Dcxf.config.file=client.xml
         demo.ws_policy.client.Client

The server process starts in the background.

For Windows (may use either forward or back slashes):
  start 
    java -Djava.util.logging.config.file=.\logging.properties
         -Dcxf.config.file=server.xml
         demo.ws_policy.server.Server

    java -Djava.util.logging.config.file=.\logging.properties
         -Dcxf.config.file=client.xml
         demo.ws_policy.client.Client

The server process starts in a new command window.

After running the client, terminate the server process.

To remove the code generated from the WSDL file and the .class
files, either delete the build directory and its contents or run:

  ant clean

