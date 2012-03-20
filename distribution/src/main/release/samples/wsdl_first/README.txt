WSDL First Demo
===============

This demo shows how to build and call a webservice using a given WSDL (also called Contract First).
As writing a WSDL by hand is not so easy the following Howto may also be a useful read:
http://cxf.apache.org/docs/defining-contract-first-webservices-with-wsdl-generation-from-java.html

This demo mainly addresses SOAP over HTTP in Document / Literal or Document / Literal wrapped style. 
For other transports or styles the configuration may look different.

The Demo consist of three parts:

- Creating the server and client code stubs from the WSDL
- Service implementation (using JAX-WS or using Spring)
- Client implementation (using JAX-WS or using Spring)

Code generation
---------------
When using maven the code generation is done using the maven cxf-codegen-plugin
(see http://cxf.apache.org/docs/maven-cxf-codegen-plugin-wsdl-to-java.html).

The code generation is tuned using a binding.xml file. In this case the file configures that 
normal java Date is used for xsd:date and xsd:DateTime. If this is not present then XMLGregorianCalendar
will be used.

One other common use of the binding file is to also generate asynchronous stubs. The line
jaxws:enableAsyncMapping has to be uncommented to use this.

More info about the binding file can be found here:
http://jax-ws.java.net/jax-ws-20-fcs/docs/customizations.html

Server implementation
---------------------

The service is implemented in the class CustomerServiceImpl. The class simply implements the previously
generated service interface. The method getCustomersByName demonstrates how a query function could look like.
The idea is to search and return all customers with the given name. If the searched name is none then the method
returns an exception to indicate that no matching customer was found. (In a real implementation probably a list with
zero objects would be used. This is mainly to show how custom exceptions can be used).
For any other name the method will return a list of two Customer objects. The number of  objects can be increased to
test how fast CXF works for larger data.

Now that the service is implemented it needs to be made available.  This samples provides two options for deploying the 
web service provider: standalone server (using embedded Jetty) or as a WAR file in Tomcat (Version 6.x or 7.x).


Client implementation
---------------------

The main client code lives in the class CustomerServiceTester. This class needs a proxy to the service and then 
demonstrates some calls and their expected outcome using junit assertions.

The first call is a request getCustomersByName for all customers with name "Smith". The result is then checked.
Then the same method is called with the invalid name "None". In this case a NoSuchCustomerException is expected.
The third call shows that the one way method updateCustomer will return instantly even if the service needs some
time to process the request.

The classes CustomerServiceClient and CustomerServiceSpringClient show how to get a service proxy using JAX-WS 
or Spring and how to wire it to your business class (in this case CustomerServiceTester).

Prerequisite
------------
Please review the README in the samples main directory before continuing.

Building and running the demo using Maven
-----------------------------------------

From the base directory of this sample (i.e., where this README file is
located), the pom.xml file is used to build and run the demo. 

Using either UNIX or Windows:

  mvn clean install   (builds the demo and creates a WAR file for optional Tomcat deployment)
  mvn -Pserver  (from one command line window -- only if using a standalone service, i.e., embedded Jetty)
  mvn -Pclient  (from a second command line window)

If you're using Tomcat for the web service provider:
----------------------------------------------------
1.) Update the soap:address value in the resources/CustomerService.wsdl value, switching the
soap:address value to the servlet-specific one (presently commented-out).  Make sure the version
number there is the same as the version of CXF being used.

2.) You can manually copy the generated WAR file to the Tomcat webapps folder, or, if you
have Maven and Tomcat set up to use the Tomcat Maven Plugin (http://mojo.codehaus.org/tomcat-maven-plugin/)
you can use the mvn tomcat:redeploy command instead.  Important: if you're using this 
command, and are using Tomcat 6 instead of Tomcat 7, update the tomcat-maven-plugin configuration 
in the pom.xml, switching to the the Tomcat 6-specific "url" element.

To remove the code generated from the WSDL file and the .class files, run "mvn clean".

There is no special maven profile for the spring client and server but you can easily set it up yourself.

Using Eclipse to run and test the demo
--------------------------------------

run the following in the demo base directory

mvn eclipse:eclipse

Then use Import / Existing projects into workspace and browse to the wsdl_first directory. Import the wsdl_first project.

The demo can now be started using "Run as Java Application" on the CustomerServiceServer.java 
and the CustomerServiceClient. For the spring demo run the classes CustomerServiceSpringClient.java 
or CustomerServiceSpringServer.java 
