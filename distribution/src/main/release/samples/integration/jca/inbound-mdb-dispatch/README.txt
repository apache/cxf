CXF INBOUND RESOURCE ADAPTER MESSAGE DRIVEN BEAN DISPATCHING SAMPLE
===================================================================

Please read the "Introduction to the inbound-mdb* Samples" section
in the inbound-mdb/README.txt.  It contains important information.


MDB Invoking Session Bean
-------------------------

This sample shows the MDB invoked by the service endpoint facade, which
dispatches the request to a targeted Stateless Session Bean.  The 
Stateless Session Bean is where the service implementation resides.  It is 
important to note that the targeted EJB must be a Stateless Session Bean.

The MDB is a generic implementation.  In fact, the default implementation
is supplied by CXF JCA integration.  The user does not have to write it.
See in etc/ejb-jar.xml, the ejb-class for the MDB is 
org.apache.cxf.jca.inbound.DispatchMDBMessageListenerImpl.  When this
implementation class is specified, the messaging-type should be set to
org.apache.cxf.jca.inbound.DispatchMDBMessageListener.  Also, notice that
the messagelistener-type in ra.xml matches the MDB's messaging-type.

As you can see in etc/ra.xml file, the activation spec specified is 
org.apache.cxf.jca.inbound.DispatchMDBActivationSpec so that the target
Session Bean's JNDI name can be specified.

Like the inbound-mdb sample, this sample does not have a WSDL.  CXF 
will use the service endpoint interface (Greeter) to build a service model 
as it is defined in the serviceInterfaceClass property in the etc/ejb-jar.xml.

In ejb-jar.xml, you will notice that there is a ejb-local-ref, which is 
required for the MDB to lookup a Local EJB object (reference) of the target
Session Bean.  To use the dispatching function provided by 
org.apache.cxf.jca.inbound.DispatchMDBMessageListenerImpl, a ejb-local-ref
must be defined in the ejb-jar.xml.

The advantage of the approach is that users don't need to update the ra.xml
in order to expose a new EJB as Web Service since the service endpoint 
interface is not exposed to the resource adapter's deployement descriptor.

We also define a remote interface for the Session Bean (GreeterBean).  It is
completely optional.  The remote interface is not accessed by the CXF JCA
connector.  Defining a remote interface would allow a EJB client to invoke
the Session Bean from outside the application server.


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

The sample requires an application server (WebSphere 6.1).  Make sure 
you have a running instance of an application server. 

Deploy the CXF JCA Connector
-----------------------------

The CXF JCA Connector must be deployed to the application server before 
running the sample.  The JCA Connector used in this sample can only be 
shared by the inbound-mdb-dispatch-wsdl sample.  If you have deployed
a CXF JCA Connector from that sample, you can skip the following steps
and goto "Building Message Driven Bean Jar".  Otherwise, please make sure 
there is no other CXF JCA Connector deployed into the application
server.

Please consult your vendor documentation on Resource Adapter deployment. 
Here are basic instructions to deploy the connector in WebSphere 6.1.

1. Logon to WebSphere Integrated Solution Console.  The default address is:

   http://<hostname>:9060/ibm/console/login.do

2. Navigate to tree menu: Resources -> Resource adapters -> Resource adapters

3. On the Resource adapters page, click the "Install RAR" button.

4. On the Install RAR File page, select the "Local path" radio button if this 
   sample is located in the same machine that you are running the web browser.
   Otherwise, select the "Server path" radio button.  Specify or browse to the 
   path build/lib/cxf.rar file in this sample.  Then, click the "Next" button.

5. On the next page, click the "OK" button to install the Resource Adapter.

6. On the next page, click the "CXF JCA Connector" link to edit the Resource
   Adatper.

7. On the "Configuration" page, click the "J2C activation specification"
   link.  

8. On the next page, click the "New" button to create a new Activation
   Specifications.

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

3. On the "Preparing for the application installation" page, the select "Local 
   path" radio button if this sample is located in the same machine that you 
   are running the web browswer.  Otherwise, select the "Server path" radio 
   button.  Specify or browse to the path 
   j2ee-archives/dispatchedgreeterejb.jar file in this sample.  Then, click 
   the "Next" button.

4. Click the "Next" button on the "Step 1: Select installation options" page.

5. Click the "Next" button on the "Step 2: Map modules to servers" page.

6. On the "Step 3: Bind listeners for message-driven beans" page, click the
   "Activation Specification" radio button on the far right column.  The
   specify the Target Resource JNDI Name as below and the client the "Next"
   button.
   
   eis/MyActivationSpec

7. On the "Step 4: Provide JNDI names for beans" page, enter 
   "ejb/DispatchedGreeterBean" then click the "Next" button.

8. On the "Step 5: Map EJB references to beans" page, enter value below and
   then click the "Next" button.

   ejb/DispatchedGreeterBean

9. On the "Step 6: Summary" page, click the "Finish" button.

10. Click the "Save" link to commit the configuration.

11. Navigate to tree menu: Applications -> Enterprise Applications

12. Select the box next to "dispatchedgreeterejb_jar" and click the "Start" 
    button at the top to start the MDB.  Make sure the "greeterejb_jar" is
    not started since dispatchedgreeterejb_jar's facade endpoint  will try 
    to bind to the same transport address.


Running the Demo
----------------

Once the resource adapter and the EJB application have been deployed,
the client can be run with the ant build script: 

    ant client 

This will launch an CXF Java Client which contacts the web service
endpoint facade.  The facade then invokes the MDB's message listener
which is org.apache.cxf.jca.inbound.DispatchMDBMessageListenerImpl.
The DispatchMDBMessageListenerImpl performs a JNDI lookup to obtain a
EJBLocalObject reference to the target Stateless Session Bean that 
implements the Greater interface.  The output from the Session Bean 
(GreeterBean) is returned to the client.

The following output is observed from the client.

client:
     [java]  server return: Hi  CXF from an EJB
