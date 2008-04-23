MTOSI alarm_retrieval Demo (SOAP OVER HTTP)
===========================================
===========================================

This code example demonstrates a simple cxf based client/server
Web service implementing the MTOSI alarm retrieval service.


Prerequisite
------------

If your environment already includes cxf-manifest.jar on the
CLASSPATH, and the JDK and ant bin directories on the PATH
it is not necessary to run the environment script described in
the samples directory README.  If your environment is not
properly configured, or if you are planning on using wsdl2java,
javac, and java to build and run the demos, you must set the
environment by running the script.


Building and running the demo using ant
---------------------------------------

From the samples/mtosi_1.1/alarm_retrieval directory, the ant
build script can be used to build and run the demo.

Using either UNIX or Windows:

  ant build
  ant server
  ant client


To remove the code generated from the WSDL file and the .class
files, run:

  ant clean


What Happens When the Demo Runs
===============================

The server publishes it's endpoint which opens an HTTP connection on the
TCP/IP port specified in the FaultServiceSOAP_HTTP.wsdl.

The client application accesses this WSDL file, from which it obtains
connection information.  Using this information, the client makes 
asynchronous SOAP over HTTP requests against the Web service hosted by
the server process.

In the "alarm_retrieval" demo, the client invokes the getActiveAlarmsCount
and the getActiveAlarms operations of the AlarmRetrieval interface.

For the getActiveAlarms operation, the HeaderT part which is sent in the
soap message header is used to communicate such things as the number of
alarm responses expected/desired.  So, by setting the RequestedBatchSize
in the HeaderT part, the client can indicate that it wishes the server to
limit the number of alarm responses sent in the response.

The client extracts data from the Alarm information resulting from the call
to getActiveAlarms, and displays it in a formatted way:

  Alarm #0:
  - Notification ID: 0001239
  - Object type: OT_EQUIPMENT
  - OS time: 20060101123000.0Z
  - NE time: 20060101122900.0Z
  - Layer/Rate: LR_DS0_64K
  - Probable cause type: AIS
  - Perceived severity: PS_MAJOR
  - Service affecting: SA_UNKNOWN
  - Root Cause Alarm Indication: NO
  (...)

See the following source for further details on the creation and population
of the types conveyed by the messages:

   - src/demo/client/Client.java (client side)
   - src/demo/server/AlarmRetrievalImpl.java (server side)


