Hello World Demo using JavaScript and E4X Implementations
=========================================================

The demo demonstrates the use of the JavaScript and E4X dynamic
languages to implement JAX-WS Providers.

The client side makes two Dispatch-based invocations. The first uses
SOAPMessage data in MESSAGE mode, and the second uses DOMSource in
PAYLOAD mode. The first service is implemented using E4X, the second
using JavaScript.

The two messages are constructed by reading in the XML files found in
the demo/hwDispatch/client directory.

Please review the README in the samples directory before
continuing.

Running the demo using maven
----------------------------

From the base directory of this sample (i.e., where this README file is
located) run the commands, entered on a single command line:

Using either UNIX or Windows:

  mvn -Pserver  (from one command line window)
  mvn -Pclient  (from a second command line window)


