JAX-WS Dispatch/Provider Demo 
=============================

The demo demonstrates the use of JAX-WS Dispatch and Provider interface.
The client side Dispatch instance invokes upon an endpoint using a JAX-WS 
Provider implementor. There are three different invocations from the client. 
The first uses the SOAPMessage data in MESSAGE mode. The second uses the
DOMSource data in MESSAGE mode. The third uses the DOMSource in PAYLOAD mode.
The three different messages are constructed by reading in the XML files found
in the src/demo/hwDispatch/client directory.

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



