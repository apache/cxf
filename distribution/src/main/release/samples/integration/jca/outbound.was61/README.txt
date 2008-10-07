CXF OUTBOUND RESOURCE ADAPTER IBM WEBSPHERE 6.1 SAMPLE
======================================================

This sample demonstrates the new CXF outbound resource adapter.  Notice that
it does not use the ra.xml file located in <CXF_HOME>/etc directory as does 
the old outbound sample.  Instead, it uses the ra.xml file in the sample's 
etc directory.  You may notice in ra.xml file that the new outbound connect-
ion classes have a new package name: org.apache.cxf.jca.outbound.  The APIs 
for new outbound resource adapter use that package name.  

The src/demo/servlet/HelloWorldServlet.java source file illustrates how to 
invoke the CXF outbound connector APIs.

The RAR deployment descriptor used in this demo is only configured to enable
outbound connection for clarity.  It is perfectly fine to configure inbound 
and outbound connector in the same RAR file.

The README.txt (this file) provides detailed instructions to run the sample 
in IBM WebSphere 6.1.  However, the cxf.rar and helloworld.war files built 
by this sample can be deployed to other application servers as well.  Please
consult application server vendor's documentation for instructions to deploy 
in other application server environment.

In this sample, you will first build a resource adapter RAR file.  Next, you 
will create a web application and compile a WAR file.  Upon receiving a 
request, the web application will make outgoing calls to a CXF web service, 
gather results, and send responses to the client.

The web application is a Servlet that accepts a HTTP request.  It then looks 
up a CXFConnectoryFactory from the application server's JNDI registry.  The 
CXFConnectoryFactory is the entry point to gain access to CXF web service.   
The application calls the getConnection() method and gets back a 
CXFConnection.  With a CXFConnection, the application can get the web service
client by calling the getService() method.  The application must close the 
CXFConnection but the it can continue to use the web service client after the 
connection has been closed.

The sole parameter to CXFConnectoryFactory.getConnection() method is 
CXFConnectionSpec.   The following fields of CXFConnectionSpec are required:

serviceName  - the QName of the service
endpointName - the QName of the endpoint (port name)
wsdlURL      - the URL of the WSDL
serviceClass - the service interface class

The following fields of CXFConnectionSpec are optional:

busConfigURL - the URL of the CXF bus configuration.  If this is provided, it
               overrides the busConfigURL defined (if any) in the 
               CXFConnectionFactory custom property. (see the next paragraph)
address      - the transport address

The CXF bus configuration can be configured in CXF resource adapter factory's
custom property (busConfigURL).  More than one factory can be created for each
resource adapter.  Each factory has a JNDI name and can have different custom
property values.  The application can choose to (JNDI) lookup the factory (by
different JNDI name) it needs and picks up the factory's custom property 
values.  If busConfigURL in the CXFConnectionSpec is specified during the
getConnection() call, the value in the CXFConnectionSpec takes precedence over
the property value of the connection factory custom property.  If no
busConfigURL value is set in neither CXFConnectionSpec nor connection factory, 
default bus will be created.


Prerequisite
------------

For IBM WebSphere 6.1, you need to copy <CXF_HOME>/lib/wsdl4j-1.6.2.jar to 
<WAS_HOME>/AppServer/java/jre/lib/endorsed.  If the "endorsed" directory does
not exist, create a new one.  You need to restart the WebSphere 6.1 server.

If your environment already includes cxf-manifest.jar on the
CLASSPATH, and the JDK and ant bin directories on the PATH
it is not necessary to set the environment as described in
the samples directory README.  If your environment is not
properly configured, or if you are planning on using wsdl2java,
javac, and java to build and run the samples, you must set the
environment.


Building and running the sample using Ant
-----------------------------------------
From the base directory of this sample (i.e., where this README file is
located), the Ant build.xml file can be used to build and run the sample.
 

Build the common Servlet base jar
---------------------------------
  (Unix)    % cd ../common
            % ant
            % cd ../outbound.was

  (Windows) > cd ..\common
            > ant
            > cd ..\outbound.was

Build CXF J2C RAR file 
----------------------
  (Unix)    % ant generate.rar
  (Windows) > ant generate.rar

The result RAR file is located in build/classes/lib/cxf.rar


Launch the application server
-----------------------------

The sample requires an application server (IBM WebSphere 6.1).  Make sure 
you have a running instance of an application server. 


Enable Tracing CXF messages (Optional)
--------------------------------------

To see the full effect of this sample, you will need to enable trace log in
your application server.

Please consult your vendor documentation on setting tracing and log level.  
Here are basic instructions to deploy the MDB in WebSphere 6.1.

1. Logon to WebSphere Integrated Solution Console.  The default address is:

   http://<hostname>:9060/ibm/console/login.do

2. Navigate to tree menu: Servers -> Application Servers 

3. On the Application servers page, click of the target server (e.g. server1).
   We will refer the target server as "server1" thereafter.  Please subsitute
   for a suitable server name in your environment.

4. On the next page (Application servers -> server1 in the Configuration tab)
   and under Troubleshooting, click on Logging and Tracing.

5. On the Logging and Tracking page, click on Change Log Detail Levels.

6. On the next page, click on the Runtime tab.

7. On the next page, check the "Save runtime changes to configuration as well"
   box.  Enter the following in the textbox and then click OK.

   *=info: org.apache.cxf.*=finest

The trace log is located in:

   <WebSphere install>/AppServer/profiles/AppSrv01/logs/server/trace.log.

Please remember to remove the "org.apache.cxf.*=finest" entry in a production
environment.

Deploy the CXF JCA Connector
-----------------------------

The CXF JCA Connector must be deployed to the application server before 
running the sample.  The JCA Connector used in this sample has a unique
deployment descriptor (ra.xml) to demonstrate the direct invocation of
service implementation resides in the Message Driven Bean.  Therefore, the 
JCA Connector in this sample cannot be used by other JCA samples.  Please
make sure there is no other CXF JCA Connector deployed into the application
server.

Please consult your vendor documentation on Resource Adapter deployment. 
Here are basic instructions to deploy the CXF JCA connector in WebSphere 6.1.

1. Logon to WebSphere Integrated Solution Console.  The default address is:

   http://<hostname>:9060/ibm/console/login.do

2. Navigate to tree menu: Resources -> Resource adapters -> Resource adapters

3. On the Resource adapters page, click the "Install RAR" button.

4. On the Install RAR File page, select the "Local path" radio button if the
   browser that you are running is on the same machine as the WebSphere server.
   Otherwise, select the "Server path" radio button.  Specify or browse to the
   path build/lib/cxf.rar file of this sample.  Then, click the "Next" button.

5. On the next page, click the "OK" button to install the Resource Adapter.

6. On the next page, click the "CXF JCA Connector" link to edit the Resource
   Adatper.

7. On the "Configuration" page, click the "J2C connection factories"
   link.  

8. On the next page, click the "New" button to create a new connection
   factory.

9. On the next page, enter "CXFConnectionFactory" in the Name textbox and click
   the "OK" button.
   
   Notice that the JNDI name is optional.  If it is omitted, a JNDI name
   is created for you as eis/<ConnectionFactory> where <ConnectionFactory>
   is "CXFConnectionFactory" above.  Basically, you can name the factory
   anything as long as your application knows the JNDI name to look it up.

10. Finally, click the "Save" link to commit the configuration.


Building the Web Application
----------------------------

Issue the command:

  (Unix)    % ant
  (Windows) > ant

The output WAR file can be found in build/lib/helloworld.war.


Deploying the Web Applicatin
----------------------------

Please consult your vendor documentation on WAR file deployment.  Here are 
basic instructions to deploy the MDB in WebSphere 6.1.

1. Logon to WebSphere Integrated Solution Console.  The default address is:

   http://<hostname>:9060/ibm/console/login.do

2. Navigate to tree menu: Applications -> Install new Applications

3. On the "Preparing for the application installation" page, select the "Local 
   path" radio button if the browser that you are running is on the same 
   machine as the WebSphere server.  Otherwise, select the "Server path" radio
   button.  Specify or browse to the path helloworld.war file in this sample.

   Enter "helloworld" in the Context root textbox.  Then, click the "Next" 
   button.

4. Click the "Next" button on the "Step 1: Select installation options" page.

5. Click the "Next" button on the "Step 2: Map modules to servers" page.

6. On the "Step 3. Map resource references to resources" page, Enter the follow
   in the "Target Resource JNDI Name" and then click Next.

   eis/CXFConnectionFactory

7. On the "Step 4. Map virtual hosts for Web modules" page, click Next.

8. On the "Step 5. Summary" page, click Finish.

9. Finally, click the "Save" link to commit the configuration.

10. Navigate to tree menu: Applications -> Enterprise Applications

11. Select the box next to "helloworld_war" and click the "Start" button at 
    the top to start the application.


Running the Demo
----------------

Start the server in samples/wsdl_first by running:

    Change directory to ../../../wsdl_first
    ant server

Notice that the server will timeout and stop itself after a few minutes.

Next, run the client:

    ant client

NOTE: the URL in demo.client.Client.jar needs to be adjusted for other app
      server

This will launch the client submitted a HTTP request to the Servlet hosted by
the application server.  The Servlet obtained a CXFConnection.  And, from
the connection, it obtained a web service client.  The Servlet invoked the
The web server.  It gathered the result and replied to the client.

The following output is observed from the client.

client:
     [java]  server return: Hi  CXF from an EJB

