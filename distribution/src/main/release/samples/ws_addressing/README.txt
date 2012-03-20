WS-Addressing Demo
==================

This demo shows how WS-Addressing support in Apache CXF may be enabled.  

The client and server both apply the addressing feature to the bus.
This ensures installation of the WS-Addressing interceptors,
comprising a logical interceptor (MAPAggregator)
responsible for aggregating the WS-A MessageAddressingProperties for
the current message, and a protocol interceptor (MAPCodec) responsible for
encoding/decoding these properties as SOAP Headers. 

A demo-specific logging.properties file is used to snoop the log messages
relating to WS-A Headers and display these to the console in concise form.

Normally the WS-Addressing MessageAddressProperties are generated and
propagated implicitly, without any intervention from the
application. In certain circumstances however, the application may wish
to participate in MAP assembly, for example to associate a sequence of
requests via the RelatesTo header. This demo illustrates both implicit
and explicit MAP propagation.

This demo also illustrates usage of the decoupled HTTP transport, whereby
a separate server->client HTTP connection is used to deliver the responses.
Note the normal HTTP mode (where the response is delivered on the back-
channel of the original client->server HTTP connection) may of course also
be used  with WS-Addressing; in this case the <wsa:ReplyTo> header is set to
a well-known anonymous URI, "http://www.w3.org/2005/08/addressing/anonymous".

In all other respects this demo is based on the basic hello_world sample,
illustrating that WS-Addressing usage is independent of the application.
One notable addition to the familiar hello_world WSDL is the usage
of the <wsaw:UsingAddressing> extension element to indicate the
WS-Addressing support is enabled for the service endpoint.

Please review the README in the samples directory before continuing.


Building and running the demo using Maven
-----------------------------------------
From the base directory of this sample (i.e., where this README file is
located), the Maven pom.xml file can be used to build and run the demo. 

Using either UNIX or Windows:

  mvn install (builds the demo)
  mvn -Pserver  (from one command line window)
  mvn -Pclient  (from a second command line window)

Both client and server will use the MAPAggregator and MAPCodec
handlers to aggregate and encode the WS-Addressing MAPs.

To remove the code generated from the WSDL file and the .class
files, run "mvn clean".



