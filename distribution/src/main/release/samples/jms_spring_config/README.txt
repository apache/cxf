JMS Spring Config Demo
======================

This is basically the wsdl first demo adapted to the JMS transport. It uses the JMS spec configuration as well as
the ConnectionFactoryFeature to show how to add the JMS transport to any existing service without changing the wsdl
 (leaving the wsdl:service section disregarded.)  When using the war variant the service is deployed on Tomcat
 but only uses it as a runtime the http part of tomcat is not used.

The Demo consist of three parts:

- Creating the server and client code stubs from the WSDL
- Service implementation (using Spring)
- Client implementation (using Spring)

Server implementation
---------------------

The service is implemented in the class CustomerServiceImpl. The class simply implements the previously
generated service interface. The method getCustomersByName demonstrates how a query function could look like.
The idea is to search and return all customers with the given name. If the searched name is none then the method
returns an exception to indicate that no matching customer was found. (In a real implementation probably a list with
zero objects would be used. This is mainly to show how custom exceptions can be used).
For any other name the method will return a list of two Customer objects. The number of  objects can be increased to
test how fast CXF works for larger data.

Now that the service is implemented it needs to be made available. In this example a standalone server is deployed
via a WAR archive using the Spring config as demonstrated in the class CustomerServiceSpringServer.

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
---------------------------------------

From the base directory of this sample (i.e., where this README file is
located), the pom.xml file is used to build and run the demo. 

Using either UNIX or Windows:

  mvn clean install   (builds the demo)
  mvn -Pjms.broker (from one command line window)
  mvn -Pserver  (from as second command line window)
  mvn -Pclient  (from a third command line window)

To remove the code generated from the WSDL file and the .class
files, run "mvn clean".


Using eclipse to run and test the demo
--------------------------------------

run the following in the demo base directory

mvn eclipse:eclipse

Then use Import / Existing projects into workspace and browse to the wsdl_first directory. Import the wsdl_first project.

The demo can now be started using "Run as Java Application" on the classes CustomerServiceSpringClient.java 
or CustomerServiceSpringServer.java 
