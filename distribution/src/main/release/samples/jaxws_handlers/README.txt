JAX-WS Handler Demo
===================

This demo shows how JAX-WS handlers are used.  The server uses a
SOAP protocol handler which logs incoming and outgoing messages
to the console.  

The server code registers a handler using the @HandlerChain annotation
within the service implementation class. For this demo, LoggingHandler
is SOAPHandler that logs the entire SOAP message content to stdout.

The client includes a logical handler that checks the parameters on
outbound requests and short-circuits the invocation in certain
circumstances. This handler is specified programatically.

Please review the README in the samples directory before continuing.


Prerequisite
------------

If your environment already includes cxf-manifest.jar on the CLASSPATH,
and the JDK and ant bin directories on the PATH, it is not necessary to
set the environment as described in the samples directory's README.
If your environment is not properly configured, or if you are planning
on using wsdl2java, javac, and java to build and run the demos, you must
set the environment.


Building and running the demo using Ant
---------------------------------------
From the base directory of this sample (i.e., where this README file is
located), the Ant build.xml file can be used to build and run the demo. 
The server and client targets automatically build the demo.

Using either UNIX or Windows:

  ant server  (from one command line window)
  ant client  (from a second command line window)

When using these ant targets, the server process uses the LoggingHandler
and the client process uses the SmallNumberHandler. Notice that both
the client and server consoles display short informative messages. The 
client handler examines the operation parameters and, based on the 
parameter values, may decide not to forward the request to the server. The server 
handler displays the entire content of each message in its console. 
The @HandlerChain annotation in the implementation class indicates that 
the file demo_handler.xml includes the information needed to identify the
handler class.

  @HandlerChain(file = "../common/demo_handlers.xml", name = "DemoHandlerChain")

The client side SmallNumberHandler is specified programatically:

    SmallNumberHandler sh = new SmallNumberHandler();
    List<Handler> newHandlerChain = new ArrayList<Handler>();
    newHandlerChain.add(sh);
    ((BindingProvider)port).getBinding().setHandlerChain(newHandlerChain);


After running the client, terminate the server process.

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
located) first create the target directory build/classes and then 
generate code from the WSDL file.


For UNIX:
  mkdir -p build/classes

  wsdl2java -d build/classes -compile ./wsdl/addNumbers.wsdl

For Windows:
  mkdir build\classes
    Must use back slashes.

  wsdl2java -d build\classes -compile .\wsdl\addNumbers.wsdl
    May use either forward or back slashes.

Now compile the provided client and server applications with the commands:

For UNIX:  
  
  export CLASSPATH=$CLASSPATH:$CXF_HOME/lib/cxf-manifest.jar:./build/classes
  javac -d build/classes src/demo/handlers/common/*.java
  javac -d build/classes src/demo/handlers/client/*.java
  javac -d build/classes src/demo/handlers/server/*.java

For Windows:
  set classpath=%classpath%;%CXF_HOME%\lib\cxf-manifest.jar;.\build\classes
  javac -d build\classes src\demo\handlers\common\*.java
  javac -d build\classes src\demo\handlers\client\*.java
  javac -d build\classes src\demo\handlers\server\*.java

Finally, copy the demo_handlers.xml file from the src/demo/handlers/common
directory into the build/classes/demo/handlers/common directory.

For UNIX:
  cp ./src/demo/handlers/common/demo_handlers.xml ./build/classes/demo/handlers/common

For Windows:
  copy src\demo\handlers\common\demo_handlers.xml build\classes\demo\handlers\common


Running the demo using java
---------------------------

From the base directory of this sample (i.e., where this README file is
located) run the commands, entered on a single command line:

For UNIX (must use forward slashes):
    java -Djava.util.logging.config.file=$CXF_HOME%/etc/logging.properties
         demo.handlers.server.Server &

    java -Djava.util.logging.config.file=$CXF_HOME%/etc/logging.properties
         demo.handlers.client.Client ./wsdl/addNumbers.wsdl

The server process starts in the background.

For Windows (may use either forward or back slashes):
  start 
    java -Djava.util.logging.config.file=%CXF_HOME%\etc\logging.properties
         demo.handlers.server.Server

    java -Djava.util.logging.config.file=%CXF_HOME%\etc\logging.properties
         demo.handlers.client.Client .\wsdl\addNumbers.wsdl

The server process starts in a new command window.

After running the client, terminate the server process.

To remove the code generated from the WSDL file and the .class
files, either delete the build directory and its contents or run:

  ant clean
