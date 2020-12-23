JAX-RS Brave/OpenZipkin Tracing Demo in OSGi container
=================

The demo shows a basic usage of Brave/OpenZipkin distributed tracer with SOAP
based Web Services.

A SOAP catalog service is provided on URL http://localhost:8181/cxf/catalog. The WSDL 
contract is available at http://localhost:8181/cxf/catalog?wsdl.

There are three SOAP actions:
  - addBook
  - deleteBook
  - getBook

Building and running the demo using Maven
---------------------------------------

From the base directory of this sample (i.e., where this README file is
located), the Maven pom.xml file can be used to build and run the demo. 

Using either UNIX or Windows:

  mvn install
  
Run OpenZipkin server (or point OSGI-INF/blueprint/context.xml to the existing one): 

  docker run -d -p 9411:9411 openzipkin/zipkin
  
Starting Karaf (refer to http://karaf.apache.org/manual/latest-4.0.x/quick-start.html)

  bin/karaf


          __ __                  ____      
         / //_/____ __________ _/ __/      
        / ,<  / __ `/ ___/ __ `/ /_        
       / /| |/ /_/ / /  / /_/ / __/        
      /_/ |_|\__,_/_/   \__,_/_/         
  
    Apache Karaf (4.0.7)
  
  Hit '<tab>' for a list of available commands
  and '[cmd] --help' for help on a specific command.
  Hit '<ctrl-d>' or type 'system:shutdown' or 'logout' to shutdown Karaf.


In order to install CXF's features, you need to add the CXF's features repo using

  feature:repo-add cxf 3.n.m

 where 3.n.m corresponds to a valid CXF version number (e.g., 3.2.0).

Install CXF's cxf-tracing-brave feature that installs all the required bundles
for this demo bundle.

  feature:install cxf-jaxws
  feature:install cxf-tracing-brave
  feature:install aries-blueprint

Install this demo bundle (using the appropriate bundle version number)
  
  install -s mvn:org.apache.servicemix.bundles/org.apache.servicemix.bundles.okio/1.15.0_1
  install -s mvn:org.apache.servicemix.bundles/org.apache.servicemix.bundles.okhttp/3.11.0_1
  install -s mvn:io.zipkin.reporter2/zipkin-sender-okhttp3/2.16.2
  install -s mvn:org.apache.cxf.samples/jax_ws_tracing_brave_osgi/3.n.m

You can verify if the CXF JAX-RS OpenZipkin Brave Blueprint Demo is installed and started.

  karaf@root()> list
  START LEVEL 100 , List Threshold: 50
   ID | State  | Lvl | Version | Name                              
  -----------------------------------------------------------------
  117 | Active |  80 | 3.3.0.SNAPSHOT | JAX-WS Demo Using Distributed Tracing with OpenZipkin Brave and OSGi
  karaf@root()>

Now, you will be able to access this CXF JAXWS demo service on your Karaf instance at

  http://localhost:8181/cxf/catalog?wsdl
  
Run the client test code which adds a book via:

  mvn -Pclient
  
In the Karaf logs (data/log/karaf.log) you can see the Tracing calls.
