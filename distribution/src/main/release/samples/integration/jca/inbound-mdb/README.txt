CXF INBOUND RESOURCE ADAPTER MESSAGE DRIVEN BEAN SAMPLE
=======================================================

Introduction to the inbound-mdb* Samples
----------------------------------------

This is one of three new inbound resource adapter samples (inbound-mdb,
inbound-mdb-dispatch, and inbound-mdb-dispatch-wsdl).
Please read all of them before you decide which is best suit for your 
scenario.  This series of inbound adapter samples leverages the JCA
Specification Version 1.5 and Message Driven Bean in EJB 2.1 to activate
CXF service endpoint facade inside the application server.  For more inform-
ation about the JCA  message inflow model, please refer to chapter 12
(Message Inflow) of the JCA Specification 1.5.

The JCA Specification 1.5 defines a framework that allows a resource adapter 
to be notified when a Message Driven Bean (MDB) is started.  The resource 
adapter can activate some services (in our case, CXF service endpoint 
facade) to enable inbound communication from an Enterprise Information System 
(EIS).  In order for the resource adapter to get notified, the deployment 
descriptors of the MDB (ejb-jar.xml) and the resource adapter (ra.xml) must 
define the same interface for messaging-type and messagelistener-type, 
respectively. 

Once the service endpoint facade is activated, the service facade can 
receive client requests and then invoke the MDB's message listener interface,
which is defined in the "messaging-type" of the deployement descriptor 
(ejb-jar.xml).

When the MDB is stopped, the resource adapter will be notified.  So, the 
CXF service endpoint facade will stop as well.

Notice that the resource adapter in this sample does not use the 
activation mechanism used by the old "inbound" sample.  There is no "ant
activate" task and the ejb_servants.properties file is ignored by the new
inbound resource adapter.  It is not recommended to use the old and new 
activation mechanism together in the same application server.  It is 
important to note that the old activation mechanism does not support IBM 
WebSphere 6.1 server.  

Detailed steps to deploy and run the sample in WebSphere 6.1 are provided
in this README file.  These samples can be run in other JCA 1.5 and EJB 3.0 
compliant application servers as well.  The ejb-jar.xml needs to be slightly  
modified in order to run in EJB 2.1 compliant application server.

Notice that each inbound-mdb* sample has its own ra.xml and ejb-jar.xml in 
its etc directory.  The ra.xml only contains inbound-resourceadapter 
(no outbound-resourceadapter) for clarity.  It is perfectly fine to have
both inbound and outbound resource adapter defined in ra.xml.

The following activation config properties are supported.  They can be put 
in the ejb-jar.xml, activation spec, and ra.xml.  Values specified in the
activation spec can override the ones in ra.xml.  And, the ejb-jar.xml takes 
precedence over the activation spec.  These are all String values.  Location
values are relative to the ejb jar.


The following properties apply when org.apache.cxf.jca.inbound.MDBActivationSpec
is specified.

wsdlLocation:          WSDL location
schemaLocations:       Comma separated schema locations 
serviceInterfaceClass: Service endpoint interface classname
busConfigLocation:     CXF bus config location
address:               Transport address
endpointName:          PortType QName in WSDL
serviceName:           Service Name QName in WSDL
displayName:           Name used for logging and as key in a map of endpoints.

When org.apache.cxf.jca.inbound.DispatchMDBActivationSpec is specified,
the following also applies.

targetBeanJndiName:    JNDI name of the target session bean

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

< End of Introduction to the inbound-mdb* Samples >


Simple MDB Invocation
---------------------

This sample shows a simple MDB invocation by the CXF service 
endpoint facade.  The service implementation is contained in the MDB.
This is the most straightward approach and does not involve any dispatching.
It should be faster then the other inbound-mdb* samples.  Another
advantage is that since this approach only requires a MDB (no Session Bean),
it does not require any EJB Home, Local or Remote interfaces.  


The downside is that the service endpoint interface must be exposed as the
messagelistener-type element in the deployment descriptors of the resource 
adapter.  So, you will have to edit the ra.xml to add a new
MDB that is exposed as a Web Service by the CXF resource adapter.

As you can see in etc/ra.xml file, the activation spec specified is 
org.apache.cxf.jca.inbound.MDBActivationSpec as it is not doing any 
dispatching.

This sample does not contain a WSDL.  CXF will use the service endpoint 
interface (Greeter) to build a service model as it is defined in the
serviceInterfaceClass property in the etc/ejb-jar.xml.


Building and running the sample using Ant
-----------------------------------------
From the base directory of this sample (i.e., where this README file is
located), the Ant build.xml file can be used to build and run the sample. 


Build CXF J2C RAR file
----------------------
  (Unix)    % ant generate.rar
  (Windows) > ant generate.rar

The result RAR file is located in build/lib/cxf.rar

Launch the application server
-----------------------------

The sample requires an application server (IBM WebSphere 6.1).  Make sure 
you have a running instance of an application server. 

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

4. On the Install RAR File page, select the "Local path" radio button if this 
   sample is located in the same machine that you are running the web browser.
   Otherwise, select the "Server path" radio button.  Specify or browse to 
   the path build/lib/cxf.rar file of this sample.  Then, click the "Next" 
   button.

5. On the next page, click the "OK" button to install the Resource Adapter.

6. On the next page, click the "CXF JCA Connector" link to edit the Resource
   Adatper.

7. On the "Configuration" page, click the "J2C activation specification"
   link.  

8. On the next page, click the "New" button to create a new Activation
   Specification.

9. On the next page, enter "MyActivationSpec" in the Name textbox and click
   the "OK" button.
   
   Notice that the JNDI name is optional.  If it is omitted, a JNDI name
   is created for you as eis/<ActivationSpecName> where <ActivationSpecName>
   is "MyActivationSpec" above.

10. Finally, click the "Save" link to commit the configuration.

   Notice that you can specify activation config values in the new 
   activation spec you just created.  The activation spec will be associated
   to your MDB later.  The MDB's deployment descriptor can define activation
   config values to override the values specified in the associated activation
   spec.


Building Message Driven Bean Jar
--------------------------------

Issue the command:

  (Unix)    % ant
  (Windows) > ant


Deploying the Message Drive Bean
--------------------------------

Please consult your vendor documentation on EJB deployment.  Here are 
basic instructions to deploy the MDB in WebSphere 6.1.

1. Logon to WebSphere Integrated Solution Console.  The default address is:

   http://<hostname>:9060/ibm/console/login.do

2. Navigate to tree menu: Applications -> Install new Applications

3. On the "Preparing for the application installation" page, select the 
   "Local path" radio button if this sample is located in the same machine 
   that you are running the web browser.  Otherwise, select the "Server path" 
   radio button.  Specify or browse to the path j2ee-archives/greeterejb.jar 
   file in this sample.  Then, click the "Next" button.

4. Click the "Next" button on the "Step 1: Select installation options" page.

5. Click the "Next" button on the "Step 2: Map modules to servers" page.

6. On the "Step 3: Bind listeners for message-driven beans" page, click the
   "Activation Specification" radio button on the far right column.  Specify 
   the Target Resource JNDI Name as below and the client the "Next" button.
   
   eis/MyActivationSpec

7. On the "Step 4: Summary" page, click the "Finish" button.

8. Finally, click the "Save" link to commit the configuration.

9. Navigate to tree menu: Applications -> Enterprise Applications

10. Select the box next to "greeterejb_jar" and click the "Start" button at 
    the top to start the MDB.


Running the Demo
----------------

Once the resource adapter and the EJB application have been deployed,
the client can be run with the ant build script: 

    ant client 

This will launch an CXF Java Client which contacts the web service
endpoint facade.  The facade then invokes the Greeter interface that is
implemented by the MDB.  The output from the MDB is returned to the client.

The following output is observed from the client.

client:
     [java]  server return: Hi  CXF from an EJB
