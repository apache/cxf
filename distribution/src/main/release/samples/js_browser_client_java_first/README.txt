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

Once the server is running, browse to:

  http://HOSTNAME:9000/Beverages.html

(Substitute your hostname for HOSTNAME.)

On the web page you see, click on the 'invoke' button to invoke the
very simple sayHi service, which takes no input and returns a single
string.
