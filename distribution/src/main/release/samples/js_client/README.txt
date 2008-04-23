Hello World Client Demo using JavaScript 
========================================

The client demo demonstrates the use of (non-browser)
JavaScript to call a CXF server.

The client side makes call using JAX-WS APIs. It uses the Mozilla Rhino library 
to read the JavaScript file and run it.

This demo only implements the client-side logic, and relies on the server
provided by the wsdl_first demo.


Building and running the demo server using ant
----------------------------------------------

From the samples/wsdl_first directory, the Ant build.xml file
can be used to build and run the demo.

Using either UNIX or Windows:

  ant build
  ant server  (in the background or another window)

To remove the code generated from the WSDL file and the .class
files, run:

  ant clean


Running the JavaScript client
-----------------------------

In another command line window, run the ant "client" target from 
the build.xml file located in the same directory as this README.

Using either UNIX or Windows:
  ant client

When running the client, you can terminate the server process by issuing 
Ctrl-C in its command window.

The client will show this output:
invoke sayHi().   return Bonjour
invoke greetMe(String).   return Hello Jeff

The same time, the server will give this output:
     [java] Executing operation sayHi

     [java] Executing operation greetMe
     [java] Message received: Jeff
