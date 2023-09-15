JAX-RS Micrometer Observation Demo
=================

The demo shows a basic usage of Micrometer Observation APIs (distributed tracing)
with REST based Web Services using JAX-RS 2.0 (JSR-339). The REST server provides the 
following services at URL http://localhost:9000/catalog: 

 - GET to http://localhost:9000/catalog 
 - POST to http://localhost:9000/catalog 
 - GET to http://localhost:9000/catalog/<id> 
 - DELETE to URL http://localhost:9000/catalog/<id> 
 - GET to URL http://localhost:9000/catalog/search?q=<query>

The last endpoint calls public Google Books API in order to search the books by 
query criteria. It demonstrates the integration with native OpenTelemetry instrumentation.

Building and running the demo using Maven
---------------------------------------

From the base directory of this sample (i.e., where this README file is
located), the Maven pom.xml file can be used to build and run the demo. 


Using either UNIX or Windows:

  mvn install
  mvn -Pserver  (from one command line window)
  mvn -Pclient  (from a second command line window)
    

To remove the target dir, run mvn clean".



