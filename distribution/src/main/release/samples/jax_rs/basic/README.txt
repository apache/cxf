JAX-RS Basic Demo 
=================

The demo shows a basic REST based Web Services using JAX-RS (JSR-311). The REST server provides the following services: 

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


Please review the README in the samples directory before
continuing.



Building and running the demo using maven
---------------------------------------

From the base directory of this sample (i.e., where this README file is
located), the maven pom.xml file can be used to build and run the demo. 


Using either UNIX or Windows:

  mvn install
  mvn -Pserver  (from one command line window)
  mvn -Pclient  (from a second command line window)
    

To remove the target dir, run mvn clean".



