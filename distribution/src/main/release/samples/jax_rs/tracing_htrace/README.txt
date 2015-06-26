JAX-RS Search Demo 
=================

The demo shows a basic usage of HTrace distributed tracer with REST based 
Web Services using  JAX-RS 2.0 (JSR-339). The REST server provides the 
following services: 

A RESTful catalog service is provided on URL http://localhost:9000/catalog 

A HTTP GET request to URL http://localhost:9000/catalog generates following 
traces:

A HTTP POST request to URL http://localhost:9000/catalog generates following 
traces:

A HTTP GET request to URL http://localhost:9000/catalog/<id> generates following 
traces:

A HTTP DELETE request to URL http://localhost:9000/catalog/<id> generates following 
traces:

Building and running the demo using Maven
---------------------------------------

From the base directory of this sample (i.e., where this README file is
located), the Maven pom.xml file can be used to build and run the demo. 


Using either UNIX or Windows:

  mvn install
  mvn -Pserver  (from one command line window)
  mvn -Pclient  (from a second command line window)
    

To remove the target dir, run mvn clean".



