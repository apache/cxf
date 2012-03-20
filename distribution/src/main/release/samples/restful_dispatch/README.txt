RESTful Hello World Demo 
========================

The demo shows REST based Web Services using the JAX-WS Provider/Dispatch. 
The REST server provides the following services: 

A RESTful customer service is provided on URL http://localhost:9000/customerservice/customer. 
Users access this URI to query or update customer info.

A HTTP GET request to URL http://localhost:9000/customerservice/customer returns 
a list of customer hyperlinks. This allows client navigates through the 
application states. The XML document returned:

<Customers>
  <Customer href="http://localhost/customerservice/customer?id=1234">
      <id>1234</id>
  </Customer>
  <Customer href="http://localhost/customerservice/customer?id=1235"> 
      <id>1235</id>
  </Customer>
  <Customer href="http://localhost/customerservice/customer?id=1236"> 
      <id>1236</id>
  </Customer>
</Customers>

A HTTP GET request to URL http://localhost:9000/customerservice/customer?id=1234 
returns a customer instance whose id is 1234. The XML document returned:

<Customer>
  <id>1234</id>
  <name>John</name>
  <phoneNumber>123456</phoneNumber>
</Customer>

A HTTP POST request to URL http://localhost:9000/customerservice/customer 
with the data:
<Customer>
  <id>1234</id>
  <name>John</name>
  <phoneNumber>123456</phoneNumber>
</Customer>

updates customer 1234 with the data provided. 

The XML document returned:
<Customer>
  <name>John</name>
  <id>123456</id>
</Customer>

The client code demonstrates how to send HTTP POST with XML data using 
JAX-WS Dispatch and how to send HTTP GET using URL.openStream(). The 
server code demonstrates how to build a RESTful endpoints through 
JAX-WS Provider interface.


Please review the README in the samples directory before continuing.


Building and running the demo using Maven
-----------------------------------------
From the base directory of this sample (i.e., where this README file is
located), the Maven pom.xml file can be used to build and run the demo. 


Using either UNIX or Windows:

  mvn install
  mvn -Pserver  (from one command line window)
  mvn -Pclient  (from a second command line window)
    

To remove the target dir, run "mvn clean".

