HELLO WORLD (SOAP OVER HTTP) CXF J2EE OUTBOUND DEMO
===================================================


This demo shows how to connect with an Apache CXF Web service using a
Servlet deployed in an application server. This demo is based on JBoss4.0.5GA.


Running the Demo
================

There are a number of steps required to successfully run this demo
application:

    . Set Jboss environment
    . Set CXF environment
    . Build CXF J2EE Connector rar file
    . Launch the application server
    . Deploy the CXF J2EE Connector 
    . Build the demo
    . Deploy the web application to the application server
    . Launch the CXF Server
    . Accessing the web application 


Set Jboss environment
=====================
 (Unix) % export JBOSS_HOME=<jboss-home>
 (Windows) > set JBOSS_HOME=<jboss-home>


Set CXF environment
=====================
 (Unix) % export CXF_HOME=<cxf-home>
 (Windows) > set CXF_HOME=<cxf-home>


Build CXF J2EE Connector rar file
=================================
  (Unix)    % ant generate.rar
  (Windows) > ant generate.rar

This target will update jboss endorsed directory first,
it will copy activation-1.1.jar,jaxb-api-2.0.jar,
jaxb-impl-2.0.3.jar,jaxb-xjc-2.0.3.jar,stax-api-1.0.1.jar,
jsr181-api-1.0-MR1.jar,saaj-api-1.3.jar saaj-impl-1.3.jar
 files to $JBOSS_HOME/lib/endorsed directory.


Launch the application server
=============================

    The demo requires an application server.  Make sure you have a
    running instance of an application server. 
   

Deploy the Apache CXF J2EE Connector
===============================

    The Apache CXF J2EE Connector must be deployed to the application
    server before running the demo.  A single resource adapter
    deployment will be shared by all of the demos, so this step need
    only be completed once.  

    If you embed CXF in software product which have license, please copy
    the license file into $CXF_HOME/etc/ and save name with licenses.txt
    
    How to deploy the Apache CXF J2EE Connector is dependent on your 
    application server. Please consult your vendor documentation
    on connector deployment. Here are basic instructions to deploy
    the connector in JBoss application servers.

  (Unix)    % ant deploy.cxf.rar
  (Windows) > ant deploy.cxf.rar

Or copy the connector RAR from its location in the Apache CXF installation to
the JBoss deployment directory.

  Copy connector file to deployment directory.
    (Unix)    % cd $CXF_HOME/lib/
              % cp cxf.rar \ 
                 $JBOSS_HOME/server/default/deploy

    (Windows) > cd %CXF_HOME%\lib\
              > copy cxf.rar 
                 %JBOSS_HOME%\server\default\deploy

  Copy the cxf_j2ee_1_5-ds.xml file to the JBoss deployment directory.
    (Unix)    % cp ./etc/cxfj2ee_1_5-ds.xml $JBOSS_HOME/server/default/deploy

    (Windows) > copy .\etc\cxfj2ee_1_5-ds.xml 
                   %JBOSS_HOME%\server\default\deploy


Building the Demo
=================

Building the demo requires that there is a JDK available and that the
Apache CXF environment is correctly set. 

Before building this demo, build common dir first.
  (Unix)    % cd common
            % ant
  (Windows) > cd common
            > ant

The demo needs be built from the directory outbound.

Issue the command:

  (Unix)    % ant
  (Windows) > ant


Launch the Apache CXF Service
========================

Run the Apache CXF service provided by the hello_world
demo.

To launch the service:

1.  Move into the sample/wsdl_first/ directory.
2.  launch server
    Issue the command: 
  (Unix)    % ant server
  (Windows) > ant server


See wsdl_first/README.txt file for full details.


Deploying the demo WAR archive
==============================

How to deploy a WAR archive is dependent on your 
application server. Please consult your vendor documentation
on application deployment. Here are basic instructions to deploy
the demo application for JBoss application servers.

Run the command in outbound directory.

  (Unix)    % ant deploy.war
  (Windows) > ant deploy.war

Or copy the WAR archive ./build/lib/helloworld.war 
to the JBoss deployment directory.
  
  (Unix)    % cp ./build/lib/helloworld.war \ 
              $JBOSS_HOME/server/default/deploy
  (Windows) > copy .\build\lib\helloworld.war 
              %JBOSS_HOME%\server\default\deploy


Accessing the web application 
=============================

Using a web browser access the URI below corresponding to your
application server. (These URI assume that the application
server is running in the same machine as the web browser)

JBoss
-----
http://localhost:8080/helloworld/*.do


The web application provides a simple Web front-end to the Hello World
Application. 

command-line
------------
You can also run a client in command-line.

  (Unix)    % ant client
  (Windows) > ant client
