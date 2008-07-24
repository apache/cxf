RESTful HTTP Binding Demo 
=========================

This demo shows how to create RESTful services using CXF's HTTP binding.
The server in the demo creates 3 different endpoints: a RESTful XML
endpoint, a RESTful JSON endpoint, and a SOAP endpoint.


Prerequisites
-------------

If your environment already includes cxf-manifest.jar on the
CLASSPATH, and the JDK and ant bin directories on the PATH
it is not necessary to set the environment as described in
the samples directory README.  If your environment is not
properly configured, or if you are planning on using wsdl2java,
javac, and java to build and run the demos, you must set the
environment.


Building and running the demo using Ant
---------------------------------------

From the base directory of this sample (i.e., where this README file is
located), the Ant build.xml file can be used to build and run the demo. 
The server and client targets automatically build the demo.

The demo has a class called demo.restful.server.Server which starts up various
endpoints. To start this server run the command:

Using either UNIX or Windows:
  ant server  (from one command line window)
  
The demo also includes a Client class which accesses data using 
HTTP. To run this client, do:

Using either UNIX or Windows:
  ant client  (from a second command line window)

To remove the code generated from the WSDL file and the .class
files, run "ant clean".


Building and running the demo using Maven
---------------------------------------

From the base directory of this sample (i.e., where this README file is
located), the maven pom.xml file can be used to build and run the demo. 

Build the demo:
   mvn install

The demo has a class called demo.restful.server.Server which starts up various
endpoints. To start this server run the command:

Using either UNIX or Windows:
  mvn -Pserver  (from one command line window)
  
The demo also includes a Client class which accesses data using 
HTTP. To run this client, do:

Using either UNIX or Windows:
  mvn -Pclient  (from a second command line window)

To remove the code generated from the WSDL file and the .class
files, run "mvn clean".

---------------------------------------


Once it is running try going to the following URLs:

http://localhost:8080/xml/customers
http://localhost:8080/json/customers
http://localhost:8080/xml/customers/123
http://localhost:8080/json/customers/123

These will serve out XML and JSON representation of the resources.

There is an HTML page that is served by CXF so you can try using the
JSON service via Javascript. Just go to:

http://localhost:8080/test.html

Included are some example JSON and XML files so you can add or update
customers using wget. Try the following commands and look at the results:

wget --post-file add.json http://localhost:8080/json/customers
wget --post-file add.xml http://localhost:8080/xml/customers
wget --post-file update.xml http://localhost:8080/xml/customers/123

And if you are interested in SOAP you can try the SOAP endpoint:

http://localhost:8080/soap?wsdl

    
