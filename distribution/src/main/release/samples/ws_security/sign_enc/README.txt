WS-Security Demo  (Signature and Encryption)
=================

This demo shows how WS-Security support in Apache CXF may be enabled.

WS-Security can be configured to the Client and Server endpoints by adding
WSS4JInterceptors. Both Server and Client can be configured for outgoing and
incoming interceptors. Various Actions like, Timestamp, UsernameToken,
Signature, Encryption, etc., can be applied to the interceptors by passing
appropriate configuration properties.

CXF 3.0.0 supports both a DOM-based (in-memory) and StAX-based (streaming)
approach to WS-Security. This demo shows how to use both approaches.

This demo also shows how a CryptoCoverageChecker can be used to make sure that
the correct Elements were signed and/or encrypted.

The logging feature is used to log the inbound and outbound SOAP messages and
display these to the console.

In all other respects this demo is based on the basic hello_world sample.

Please review the README in the samples directory before continuing.


Prerequisite
------------

If your environment already includes cxf-manifest.jar on the CLASSPATH,
and the JDK and ant bin directories on the PATH, it is not necessary to
run the environment script described in the samples directory README.
If your environment is not properly configured, or if you are planning
on using wsdl2java, javac, and java to build and run the demos, you must
set the environment by running the script.


*** Requirements ***

The samples in this directory use STRONG encryption. If you are using a version
of Java prior to 1.8.0_161 then you may need to install the Java Cryptography
Extension (JCE) Unlimited Strength Jurisdiction Policy Files [1] for the
examples to work. Note that from the 1.8.0_161 release, Java has the unlimited
strength policies installed by default. If you get errors about invalid key
lengths with an older JDK version, then the Unlimited Strength files are not
installed.

[1] http://www.oracle.com/technetwork/java/javase/downloads/index.html


Building and running the demo using Maven
---------------------------------------

From the base directory of this sample (i.e., where this README file is
located), the maven pom.xml file can be used to build and run the demo.

Using either UNIX or Windows:

  mvn install (builds the demo)

To use the DOM-based WS-Security functionality:

  mvn -Pserver  (from one command line window)
  mvn -Pclient  (from a second command line window)

To use the StAX-based WS-Security functionality:

  mvn -Pstax-server  (from one command line window)
  mvn -Pstax-client  (from a second command line window)

You can also run the DOM client against the StAX server, and vice versa.

On startup, the client makes a sequence of 4 two-way invocations.

To remove the code generated from the WSDL file and the .class
files, run "mvn clean".

Building the demo using wsdl2java and javac
-------------------------------------------

From the base directory of this sample (i.e., where this README file is
located) first create the target directory build/classes and then
generate code from the WSDL file.


For UNIX:
  mkdir -p build/classes

  wsdl2java -d build/classes -compile ./wsdl/hello_world_wssec.wsdl

For Windows:
  mkdir build\classes
    Must use back slashes.

  wsdl2java -d build\classes -compile .\wsdl\hello_world_wssec.wsdl
    May use either forward or back slashes.

Now compile the provided client and server applications with the commands:

For UNIX:

  export CLASSPATH=$CLASSPATH:$CXF_HOME/lib/cxf-manifest.jar:./build/classes
  javac -d build/classes src/demo/wssec/common/*.java
  javac -d build/classes src/demo/wssec/client/*.java
  javac -d build/classes src/demo/wssec/server/*.java

For Windows:
  set classpath=%classpath%;%CXF_HOME%\lib\cxf-manifest.jar;.\build\classes
  javac -d build\classes src\demo\wssec\common\*.java
  javac -d build\classes src\demo\wssec\client\*.java
  javac -d build\classes src\demo\wssec\server\*.java


Running the demo using java
---------------------------

From the base directory of this sample (i.e., where this README file is
located) run the commands, entered on a single command line:

For UNIX (must use forward slashes):
    java -Djava.util.logging.config.file=./logging.properties
         demo.wssec.server.Server &

    java -Djava.util.logging.config.file=./logging.properties
         demo.wssec.client.Client ./wsdl/hello_world_wssec.wsdl

The server process starts in the background.

For Windows (may use either forward or back slashes):
  start
    java -Djava.util.logging.config.file=.\logging.properties
         demo.wssec.server.Server

    java -Djava.util.logging.config.file=.\logging.properties
         demo.wssec.client.Client .\wsdl\hello_world_wssec.wsdl

The server process starts in a new command window.

After running the client, terminate the server process.

To remove the code generated from the WSDL file and the .class
files, delete the build directory and its contents.

