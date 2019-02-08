JAX-RS WebSocket Demo 
=================

This is a websocket transport version of JAX-RS Basic Demo.

A RESTful customer service is provided on URL ws://localhost:9000/demo
Users access this URI to operate on customer.

This sample includes two convenient clients: a plain javascript browser client
and a node.js client based on atmosphere.


Connecting to the server
---------------------------------------

Open a websocket to ws://localhost:9000/demo and send requests over the websocket.

A GET request to path /demo/customerservice/customers/123

------------------------------------------------------------------------
GET /demo/customerservice/customers/123
------------------------------------------------------------------------

returns a customer instance whose id is 123. The XML document returned:

------------------------------------------------------------------------
<Customer>
  <id>123</id>
  <name>John</name>
</Customer>
------------------------------------------------------------------------

A GET request to path /demo/customerservice/orders/223/products/323

------------------------------------------------------------------------
GET /demo/customerservice/orders/223/products/323
------------------------------------------------------------------------

returns product 323 that belongs to order 223. The XML document returned:

------------------------------------------------------------------------
<Product>
  <description>product 323</description> 
  <id>323</id> 
</Product>
------------------------------------------------------------------------

A POST request to path /customerservice/customers

------------------------------------------------------------------------
POST /demo/customerservice/customers
Content-Type: text/xml; charset="utf-8"
------------------------------------------------------------------------

with the data:

------------------------------------------------------------------------
<Customer>
  <name>Jack</name>
</Customer>
------------------------------------------------------------------------

adds a customer whose name is Jack 


A PUT request to path /demo/customerservice/customers

------------------------------------------------------------------------
PUT /customerservice/customers
Content-Type: text/xml; charset="utf-8"
------------------------------------------------------------------------

with the data:

------------------------------------------------------------------------
<Customer>
  <id>123</id>
  <name>John</name>
</Customer>
------------------------------------------------------------------------

updates the customer instance whose id is 123


A GET request to path /demo/monitor with id monitor-12345

------------------------------------------------------------------------
GET /demo/customerservice/monitor
requestId: monitor-12345
------------------------------------------------------------------------

returns a continuous event stream on the customer
activities. Try invoking some customer related operations.

A GET request to path /demo/unmonitor with id monitor-12345

------------------------------------------------------------------------
GET /demo/customerservice/unmonitor/monitor-12345
------------------------------------------------------------------------

unregisters the event stream and returns its status.


The client code demonstrates how to send GET/POST/PUT/DELETE requests over
a websocket.


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

Using Javascript client in Browser
--------
Using a web browser that natively supports WebSocket (Safari, Chrome, Firefox):
After starting the server (see above), open the index.html page located at

  samples/jax_rs/websocket/src/main/resources/index.html

Click on the "Connect" button to establish the websocket connection.
Fill in the Request and click on the "Send" button. The sent and 
received data are displayed in the Log area.

Try out the above sample requests. When using POST or PUT with content,
make sure to have one empty line between the request header and
the content. For example, the above POST example should use the Request
value:

------------------------------------------------------------------------
POST /demo/customerservice/customers
Content-Type: text/xml; charset="utf-8"

<Customer>
  <name>Jack</name>
</Customer>
------------------------------------------------------------------------


Using Node.js client 
--------

Go to samples/jax_rs/websocket/src/test/resources and at the console

Assuming node (>=v4) and npm are installed, execute the following shell commands.

% npm install atmosphere.js
% node client.js

This client program supports websocket and sse and allows
you to choose your preferred protocol.


