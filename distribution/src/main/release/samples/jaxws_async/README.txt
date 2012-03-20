JAX-WS Asynchronous Demo using Document/Literal Style
=====================================================

This demo illustrates the use of the JAX-WS asynchronous 
invocation model. Please refer to the JAX-WS 2.0 specification
(http://jcp.org/aboutJava/communityProcess/pfd/jsr224/index.html)
for background.

The asynchronous model allows the client thread to continue after 
making a two-way invocation without being blocked while awaiting a 
response from the server. Once the response is available, it is
delivered to the client application asynchronously using one
of two alternative approaches:

- Callback: the client application implements the 
javax.xml.ws.AsyncHandler interface to accept notification
of the response availability

- Polling: the client application periodically polls a
javax.xml.ws.Response instance to check if the response
is available

This demo illustrates both approaches.

Additional methods are generated on the Service Endpoint
Interface (SEI) to provide this asynchrony, named by 
convention with the suffix "Async".

As many applications will not require this functionality,
the asynchronous variants of the SEI methods are omitted
by default to avoid polluting the SEI with unnecessary 
baggage. In order to enable generation of these methods,
a bindings file (wsdl/async_bindings.xml) is passed
to the wsdl2java generator. 

Please review the README in the samples directory before
continuing.

Building and running the demo using Maven
---------------------------------------

From the base directory of this sample (i.e., where this README file is
located), the pom.xml file is used to build and run the demo. 

Using either UNIX or Windows:

  mvn install   (builds the demo)
  mvn -Pserver  (from one command line window)
  mvn -Pclient  (from a second command line window)


To remove the code generated from the WSDL file and the .class
files, run "mvn clean".


