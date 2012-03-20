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


