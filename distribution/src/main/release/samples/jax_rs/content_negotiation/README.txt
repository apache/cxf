JAX-RS Content Negotiation Demo
===============================

The demo shows how to do content negotiation so that the same resource can 
be served using multiple representations. 

A RESTful customer service is provided on URL http://localhost:9000/customers. 
Users access this URI to operate on customer.

A HTTP GET request to URL http://localhost:9000/customerservice/customers/123
with Accept header set to "application/xml" returns a customer instance in XML
format. The XML document returned:

<Customer>
  <id>123</id>
  <name>John</name>
</Customer>

A HTTP GET request to URL http://localhost:9000/customerservice/customers/123
with Accept header set to "application/json" returns a customer instance in JSON
format. The JSON document returned:

{"Customer":{"id":"123","name":"John"}}

A HTTP GET request to URL http://localhost:9000/customerservice/customers/123
without setting Accept header explicitly returns a customer instance in JSON 
format. This is because the Accept header will be absent from the request when using 
HTTP Client, in which case the CXF will treat the Accept content type as "*/*". 
The JSON document returned:

{"Customer":{"id":"123","name":"John"}}

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


