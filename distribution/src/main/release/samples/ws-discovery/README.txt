WS-Discovery Demo
====================

This demo shows how to use the WS-Discovery service and API's 
provided by Apache CXF.


Please review the README in the samples directory before continuing.


Building and running the demo using Maven
-----------------------------------------

From the base directory of this sample (i.e., where this README file is
located), the maven pom.xml file can be used to build and run the demo. 

Using either UNIX or Windows:

  mvn install (builds the demo)
  mvn -Pservice  (from one or more command line windows)
  mvn -Pclient  (from a second command line window)

The "service" profiles will startup a simple "Greeter" service on a random
port.  Because it is a random port, there is no way for the clients to
know where that service is deployed.  The service publishes itself 
automatically using WS-Discovery and will respond to WS-Discovery queries.

The "client" profile will launch a client application that will use the CXF
WS-Discovery API's to probe the network for all the "'Greeter" services 
that are available.  It will then iterate through all of them and call
the greetMe method.   

You can run multiple instances of the service in separate command line
windows.   The client will detect them all via WS-Discovery and make calls
to each of them.


