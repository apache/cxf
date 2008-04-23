Hello World CORBA Demo
======================

This demo illustrates the use of the JAX-WS APIs to run a simple
"hello world" application using CORBA/IIOP instead of SOAP/XML.  It
also contains standard CORBA client/server applications using 
pure CORBA code so you can can see the JAX-WS client hit a pure
CORBA server and a pure CORBA client hit the JAX-WS server.


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

First, if using the Sun ORB built into the JDK, you need to start the orbd
nameing service.   From a command prompt, run:

  orbd -ORBInitialPort 1050 -serverPollingTime 200

Other ORB's may have different requirements.


Use Case 1 - Pure CORBA Server, CXF/JAX-WS Client
-------------------------------------------------
Start the pure CORBA server by running
  ant corba.server

Run the JAX-WS client by running:
  ant cxf.client


Use Case 2 - CXF/JAX-WS Server, Pure CORBA Client
-------------------------------------------------
Start the CXF/JAX-WS server by running
  ant cxf.server

Run the pure CORBA client by running:
  ant corba.client


Use Case 3 - CXF/JAX-WS Server, CXF/JAX-WS Client
-------------------------------------------------
Start the CXF/JAX-WS server by running
  ant cxf.server

Run the CXF/JAX-WS client by running:
  ant cxf.client



Cleanup
=======

To remove the code generated from the WSDL file and the .class
files, either delete the build directory and its contents or run:

  ant clean

