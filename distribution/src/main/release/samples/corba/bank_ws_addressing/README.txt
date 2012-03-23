CORBA Bank WS-Addressing Demo
=============================

This demo illustrates the use of the JAX-WS APIs to run a simple
"Bank" application using CORBA/IIOP instead of SOAP/XML.  It
also contains standard CORBA client/server applications using 
pure CORBA code so you can can see the JAX-WS client hit a pure
CORBA server and a pure CORBA client hit the JAX-WS server.

The Account objects returned from this sample are references to 
Accounts on the server.  

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


Use Case 1 - Pure CORBA Server, CXF/JAX-WS Client
-------------------------------------------------
Start the pure CORBA server by running
  mvn -Pcorba.server

Run the JAX-WS client by running:
  mvn -Pcxf.client


Use Case 2 - CXF/JAX-WS Server, Pure CORBA Client
-------------------------------------------------
Start the CXF/JAX-WS server by running
  mvn -Pcxf.server

Run the pure CORBA client by running:
  mvn -Pcorba.client


Use Case 3 - CXF/JAX-WS Server, CXF/JAX-WS Client
-------------------------------------------------
Start the CXF/JAX-WS server by running
  mvn -Pcxf.server

Run the CXF/JAX-WS client by running:
  mvn -Pcxf.client



Cleanup
=======

To remove the code generated from the WSDL file and the .class
files, just run "mvn clean".

