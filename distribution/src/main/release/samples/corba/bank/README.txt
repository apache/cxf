CORBA Bank Demo
===============

This demo illustrates the user of JAX-WS API's for creating a service
that uses the CORBA/IIOP protocol for communication.  It also 
shows throwing exceptions across that connection.

Please review the README in the samples main directory before continuing.


Building and running the demo using Maven
-----------------------------------------
From the base directory of this sample (i.e., where this README 
file is located), just run "mvn clean install" to build the demo.

Prior to running the sample you'll need to start the orbd naming
service. If using the Sun ORB built into the JDK, from a command 
prompt, run:

  orbd -ORBInitialPort 1050 -serverPollingTime 200

Other ORBs may have different requirements.



Use Case - CXF/JAX-WS Server, CXF/JAX-WS Client
-------------------------------------------------
Start the CXF/JAX-WS server by running
  mvn -Pserver

Run the CXF/JAX-WS client by running:
  mvn -Pclient


Cleanup
=======

To remove the code generated from the WSDL file and the .class
files, just run "mvn clean".

