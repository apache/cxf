JAX-RS GraalVM native-image Demo 
=================

This demo shows how JAX-RS services and clients could use GraalVM's native-image 
capabilities and be packaged as native executables. The REST server provides the 
following services: 

A RESTful customer service is provided on URL http://localhost:9000/customers. 
Users access this URI to operate on customer.

A HTTP GET request to URL http://localhost:9000/customerservice/customers/123
returns a customer instance whose id is 123. The XML document returned:

<Customer>
  <id>123</id>
  <name>John</name>
</Customer>

A HTTP GET request to URL http://localhost:9000/customerservice/orders/223/products/323
returns product 323 that belongs to order 223. The XML document returned:

<Product>
  <description>product 323</description> 
  <id>323</id> 
</Product>

A HTTP POST request to URL http://localhost:9000/customerservice/customers
with the data:

<Customer>
  <name>Jack</name>
</Customer>

adds a customer whose name is Jack 


A HTTP PUT request to URL http://localhost:9000/customerservice/customers
with the data:

<Customer>
  <id>123</id>
  <name>John</name>
</Customer>

updates the customer instance whose id is 123


The client code demonstrates how to send HTTP GET/POST/PUT/DELETE request. The 
server code demonstrates how to build a RESTful endpoint through 
JAX-RS (JSR-311) APIs.

Pre-requisites
---------------------------------------

GraalVM 22.3.0/JDK11+ or later distribution should be installed and pre-configured as 
default JVM runtime (using JAVA_HOME), see please instructions at [1].

The GraalVM's native-image tool should be installed, see please 
instructions at [2].

Building and running the demo using Maven
---------------------------------------

From the base directory of this sample (i.e., where this README file is
located), the maven pom.xml file can be used to build and run the demo. 

Using either UNIX or Windows, build server:  
  
  mvn clean package -Pserver  (from one command line window)
  
This goal will produce 'target/jaxrs-demo-server' executable (platform-dependent) 
which could be run right away: 

  On Windows: bin\jaxrs-demo-server.exe
  On Linux: ./bin/jaxrs-demo-server

Than build client:
  
  mvn clean package -Pclient  (from a second command line window)

This goal will produce 'target/jaxrs-demo-client' executable (platform-dependent) 
which could be run right away: 

  On Windows: bin\jaxrs-demo-client.exe
  On Linux: ./bin/jaxrs-demo-client

The command should produce the following output (assuming the server is up and running):

  Sent HTTP GET request to query customer info
  <?xml version="1.0" encoding="UTF-8" standalone="yes"?><Customer><id>123</id><name>Mary</name></Customer>


  Sent HTTP GET request to query sub resource product info
  <?xml version="1.0" encoding="UTF-8" standalone="yes"?><Product><description>product 323</description><id>323</id></Product>


  Sent HTTP PUT request to update customer info
  Response status code: 200
  Response body: 



  Sent HTTP POST request to add customer
  Response status code: 200
  Response body: 
  <?xml version="1.0" encoding="UTF-8" standalone="yes"?><Customer><id>128</id><name>Jack</name></Customer>

To remove the target dir, run mvn clean".

References
---------------------------------------
[1] https://www.graalvm.org/downloads/
[2] https://www.graalvm.org/reference-manual/native-image/ 


