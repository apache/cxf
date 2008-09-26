CXF INBOUND RESOURCE ADAPTER MESSAGE DRIVEN BEAN DISPATCHING SAMPLE (WSDL)
==========================================================================

Please read the "Introduction to the inbound-mdb* Samples" section
in the inbound-mdb/README.txt.  It contains important information.


MDB Invoking Session Bean (with WSDL)
-------------------------------------

This sample is similar to the "inbound-mdb-dispatch" sample except that it 
is bundled with WSDL and CXF bus config in the EJB jar.  CXF will create
a service bean based on the provided  WSDL.  The CXF bus config file in 
etc/cxf.xml enabled logging feature.   Also, this sample does not implement a 
remote interface for the GreeterBean.


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
shared by the inbound-mdb-dispatch sample.  If you have deployed
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

3. On the "Preparing for the application installation" page, select the "Local 
   path" radio button if this sample is located in the same machine that you 
   are running the web browse.  Otherwise, select the "Server path" radio 
   button.  Specify or browse to the path j2ee-archives/greeterwithwsdlejb.jar
   file in this sample.  Then, click the "Next" button.

4. Click the "Next" button on the "Step 1: Select installation options" page.

5. Click the "Next" button on the "Step 2: Map modules to servers" page.

6. On the "Step 3: Bind listeners for message-driven beans" page, click the
   "Activation Specification" radio button on the far right column.  The
   specify the Target Resource JNDI Name as below and the client the "Next"
   button.
   
   eis/MyActivationSpec

7. On the "Step 4: Provide JNDI names for beans" page, enter 
   "ejb/GreeterWithWsdlBean" then click the "Next" button.

8. On the "Step 5: Map EJB references to beans" page, enter value below and
   then click the "Next" button.

   ejb/GreeterWithWsdlBean

9. On the "Step 6: Summary" page, click the "Finish" button.

10. Click the "Save" link to commit the configuration.

11. Navigate to tree menu: Applications -> Enterprise Applications

12. Select the box next to "greeterwithwsdlejb_jar" and click the "Start" 
    button at the top to start the MDB.  


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

Notice that service contract (WSDL) is the same as the one in wsdl_first 
sample.  Therefore, you can also run the same client in the wsdl_first.

The following output is observed from the client.

client:
     [java] file:/C:/apache-cxf-2.2-SNAPSHOT/samples/integration/jca/inbound-mdb
-dispatch-wsdl/wsdl/hello_world.wsdl
     [java] Invoking sayHi...
     [java] Server responded with: Bonjour

     [java] Invoking greetMe...
     [java] Server responded with: Hello WTAM

     [java] Invoking greetMe with invalid length string, expecting exception...

     [java] Invoking greetMeOneWay...
     [java] No response from server as method is OneWay

     [java] Invoking pingMe, expecting exception...
     [java] Expected exception: PingMeFault has occurred: PingMeFault raised by
server
     [java] FaultDetail major:2
     [java] FaultDetail minor:1

Also, if trace log is enabled in the application server, you are able to see 
SOAP messages exchanged between the server and client.  



