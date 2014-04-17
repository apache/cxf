WSDL First Demo
===============

This demo shows how to build and call a webservice using a given WSDL (also
called Contract First). As writing a WSDL by hand is not so easy the following
How-To may be a useful read:
http://cxf.apache.org/docs/defining-contract-first-webservices-with-wsdl-generation-from-java.html

This demo mainly addresses SOAP over HTTP in Document/Literal or
Document/Literal wrapped style. For other transports or styles the
configuration may look different.

The Demo consist of three parts:

- Creating the server and client code stubs from the WSDL
- Service implementation (using JAX-WS or using Spring)
- Client implementation (using JAX-WS or using Spring)

Code generation
---------------
When using Maven the code generation is done using the Maven cxf-codegen-plugin
(http://cxf.apache.org/docs/maven-cxf-codegen-plugin-wsdl-to-java.html).

The code generation is tuned using a binding.xml file. In this case the file
configures normal Java Dates to be used for xsd:date and xsd:DateTime. If this
is not present then XMLGregorianCalendar will be used.

One other common use of the binding file is to also generate asynchronous
stubs. The line jaxws:enableAsyncMapping has to be uncommented to use this.

More info about the binding file can be found here:
http://jax-ws.java.net/jax-ws-20-fcs/docs/customizations.html

Server implementation
---------------------

The service is implemented in the class CustomerServiceImpl. The class simply
implements the previously generated service interface. The method
getCustomersByName demonstrates what a query function could look like. The
idea is to search and return all customers with a given name. If the searched
name is none then the method returns an exception to indicate that no matching
customer was found. (In a real implementation a list with zero objects would
probably be used. This is mainly to show how custom exceptions can be
created). For any other name the method will return a list of two Customer
objects. The number of objects can be increased to test how fast CXF works for
larger data.

Now that the service is implemented it needs to be made available. This sample
provides two options for deploying the web service provider: standalone server
(using embedded Jetty) or as a WAR file in Tomcat (Version 6.x or 7.x).


Client implementation
---------------------

The main client code lives in the class CustomerServiceTester. This class
needs a proxy to the service and then demonstrates some calls and their
expected outcome using junit assertions.

The first call is a request getCustomersByName for all customers with name
"Smith". The result is then checked. Then the same method is called with the
invalid name "None". In this case a NoSuchCustomerException is expected. The
third call shows that the one way method updateCustomer will return instantly
even if the service needs some time to process the request.

The classes CustomerServiceClient and CustomerServiceSpringClient show how to
get a service proxy using JAX-WS or Spring and how to wire it to your business
class (in this case CustomerServiceTester).

Prerequisite
------------
Please review the README in the samples main directory before continuing.

Building and running the demo using Maven
-----------------------------------------
From the base directory of this sample (i.e., where this README file is
located), the pom.xml file is used to build and run the demo. 

Using either UNIX or Windows:

  mvn clean install   (builds the demo and creates a WAR file for optional Tomcat deployment)
  mvn -Pserver  (from one command line window -- only if using embedded Jetty)
  mvn -Pclient  (from a second command line window)

There is no special Maven profile for the Spring client and server but you can
easily set it up yourself.

If you want to run the client and server with netty transport, you can use below command to  
start up the server and client:
  mvn -Pserver-netty (from one command line window -- only if using embedded Netty transport)
  mvn -Pclient-netty (from one command line window -- only if using embedded Netty transport)

If you're using Tomcat (embedded or standalone) for the web service provider:
----------------------------------------------------
1.) Update the soap:address value in the resources/CustomerService.wsdl value,
switching the soap:address value to the servlet-specific one (presently
commented-out).

2.) For standalone Tomcat: You can manually copy the generated WAR file to the Tomcat 
webapps folder, or, if you have Maven and Tomcat set up to use the Tomcat Maven Plugin 
(http://tomcat.apache.org/maven-plugin-2.1/tomcat7-maven-plugin/index.html) 
you can use the mvn tomcat7:redeploy command instead. 

For embedded Tomcat: Just run mvn tomcat7:run-war from the project base folder.

3.) Next activate the client via mvn -Pclient as explained in the previous section.


To remove the code generated from the WSDL file and the .class files, run
"mvn clean".

Using Eclipse to run and test the demo
--------------------------------------
Run the following in the demo base directory

mvn eclipse:eclipse

Then use Import / Existing projects into workspace and browse to the
wsdl_first directory. Import the wsdl_first project.

The demo can now be started using "Run as Java Application" on the
CustomerServiceServer.java and the CustomerServiceClient. For the spring demo
run the classes CustomerServiceSpringClient.java or
CustomerServiceSpringServer.java 

