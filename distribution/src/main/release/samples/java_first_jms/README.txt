Java-First JMS Sample
=====================

This sample demonstrates use of SOAP over the JMS protocol.  It is
similar to the jms_queue sample except that it is Java-first instead
of WSDL-first.

Both JAX-WS and CXF-specific methods of creating the client and service
endpoints are demonstrated (see the ClientJMS and ServerJMS classes
for details.)  Setting the <useJaxws/> property in the pom.xml
to "-jaxws" will cause the former to be used, to anything else the
latter.

Please review the README in the samples directory before
continuing.

Building and running the demo using Maven
-----------------------------------------
  
From the base directory of this sample (i.e., where this README file is
located), using either UNIX or Windows, run "mvn clean install" to 
build the sample.
  
To run the sample, using either UNIX or Windows:

    mvn -Pserver (in one terminal window)
    mvn -Pclient (in another)

When finished, to delete the code generated during the build process
run "mvn clean".

