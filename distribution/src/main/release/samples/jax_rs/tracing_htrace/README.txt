JAX-RS HTrace Demo 
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

Running Apache HBase using Docker
---------------------------------------
Official repository: https://registry.hub.docker.com/u/nerdammer/hbase/
docker run -p 2181:2181 -p 60010:60010 -p 60000:60000 -p 60020:60020 -p 60030:60030 -h hbase nerdammer/hbase

Preparing test dataset
---------------------------------------
create 'catalog', {NAME => 'c', VERSIONS => 5}
put 'catalog', '7e51155e-70fd-4b2a-b3ae-0b6352945ecf', 'c:title', 'Apache CXF Web Service Development'
put 'catalog', 'f948c2ca-9687-4d56-8388-75bfde31fae9', 'c:title', 'HBase: The Definitive Guide'
put 'catalog', 'bd80053a-9542-47fa-9a36-25af5521f2fb', 'c:title', 'HBase in Action'

Building and running the demo using Maven
---------------------------------------

From the base directory of this sample (i.e., where this README file is
located), the Maven pom.xml file can be used to build and run the demo. 


Using either UNIX or Windows:

  mvn install
  mvn -Pserver  (from one command line window)
  mvn -Pclient  (from a second command line window)
    

To remove the target dir, run mvn clean".



