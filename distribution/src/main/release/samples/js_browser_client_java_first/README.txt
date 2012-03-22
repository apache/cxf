Generated JavaScript using jax-ws APIs and jsr-181
==================================================

This sample shows the generation of JavaScript client code from a
JAX-WS server.

Building and running the demo using Maven
-----------------------------------------

From the base directory of this sample (i.e., where this README file is
located)

Using either UNIX or Windows:

  mvn install
  mvn -Pserver

Running the client in a browser
-------------------------------

Once the server is running, you can view its WSDL at:
   http://localhost:9000/beverages?wsdl

Also, browse to:

  http://localhost:9000/Beverages.html

On the web page you see, choose a beverage ingredient category
and select the invoke button to see a list of beverages 
containing that ingredients of that category.

