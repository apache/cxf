INBOUND CONNECTION CXF J2EE DEMO
================================

 
This demo shows how to expose an Enterprise Java Bean over
SOAP/HTTP using CXF. This demo is based on JBoss4.0.5GA. 

Notice that a new CXF inbound resource adapter has been
introduced.  Please read "Introduction to the inbound-mdb* 
Samples" section in the inbound-mdb/README.txt.  It contains
important information.



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
    . Deploy the ejb application to the application server
    . Activate the EJB Web Services facade
    . Access the EJB using a Web Services client


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

Deploy the CXF J2EE Connector
===============================

    The CXF J2EE Connector must be deployed to the application
    server before running the demo.  A single resource adapter
    deployment will be shared by all of the demos, so this step need
    only be completed once.  


    How to deploy the CXF J2EE Connector is dependent on your 
    application server. Please consult your vendor documentation
    on connector deployment. Here are basic instructions to deploy
    the connector in JBoss application 
    servers.

  (Unix)    % ant deploy.cxf.rar
  (Windows) > ant deploy.cxf.rar

Or copy the connector RAR from its location in the CXF installation to
the JBoss deployment directory.

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
CXF environment is correctly set. 

The demo may be built from the directory 
inbound.

Issue the command:

  (Unix)    % ant
  (Windows) > ant



Deploying the demo EJB application
==================================

How to deploy an EJB application archive is dependent on your
application server. Please consult your vender documentation on
application deployment. Here are basic instructions to deploy the
demo application for JBoss application servers.

  (Unix)    % ant deploy.ejb.jar
  (Windows) > ant deploy.ejb.jar

Or copy the EJB archive ./j2ee-archives/greeterejb.jar 
to the JBoss deployment directory.
  
  (Unix)    % cp ./j2ee-archives/greeterejb.jar \ 
              $JBOSS_HOME/server/default/deploy
  (Windows) > copy .\j2ee-archives\greeterejb.jar 
              %JBOSS_HOME%\server\default\deploy

Activate the EJB Web Services facade
====================================

Exposing EJBs as Web Services in the CXF J2EE Connector is
controlled by a properties files called ejb_servants.properties.  The
file is located in $CXF_HOME/etc, by
default. The ant build script for this demo will automatically update
this file, so please ensure that you have write permissions for this
file:

    ant activate

The location of this file is configurable via the
EJBServantPropertiesURL property.

Please see the documentation for further information on the contents
of the properties files and how it is used. 

NOTE: The CXF J2EE Connector will check this file every 30 seconds
so it will be necessary to wait this length of time before running the
client. 


Running the Demo
================


Once the resource adapter and the EJB application have been deployed,
the client can be run with the ant build script: 

    ant client 

This will launch an CXF Java Client which contacts the web service
enabled EJB. 
