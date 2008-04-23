CORBA Bank Demo
===============

This demo illustrates the user of JAX-WS API's for creating a service
that uses the CORBA/IIOP protocol for communication.  It also 
shows throwing exceptions accross that connection.



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


Use Case - CXF/JAX-WS Server, CXF/JAX-WS Client
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


