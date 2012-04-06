JAX-WS Asynchronous Demo using Document/Literal Style
=====================================================

This demo illustrates the use of the JAX-WS asynchronous 
invocation model. Please refer to the JAX-WS 2.0 specification
(http://jcp.org/aboutJava/communityProcess/pfd/jsr224/index.html)
for background.

This demo also illustrates the use of CXF specific server side 
asynchronous handling.   (The JAX-WS specification only addresses
asynchronous requests on the client side.)   This demo shows how to
enable CXF to call asynchronous methods to allow processing
on separate threads.


Client Side Asynchronous models
===============================
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

Server Side Asynchronous models
===============================
CXF provides two methods of handling requests asynchronously 
on the server side.   
-  Continuations: CXF provides an API that a developer can use 
to create a Continuation, suspend the request, resume it later, 
etc...  For more details, see:
   http://sberyozkin.blogspot.com/2008/12/continuations-in-cxf.html

- @UseAsyncMethod annotation: You can annotate the Impls 
synchronous method with the @UseAsyncMethod annotation (which uses
continuations internally).  If possible, CXF will instead 
call the async method (as generated for the client
above) with an AsyncHandler object that you can call back on when
the response is ready.

This sample uses the second method (much simpler).  When using the 
-Pserver profile to run the server, it will use the embedded Jetty
server which supports the continuations that are needed and you will
see logs mentioning it is responding asynchronously.  When 
deploying a war, if you deploy to a Servlet 3 container (such as 
Tomcat 7), you will also see those logs.  If you deploy to a
Servlet 2.5 container, continuations are not available and the
synchronous methods will be called instead.


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

If you're using Tomcat for the web service provider:
----------------------------------------------------
You can manually copy the generated WAR file to the Tomcat webapps folder, or, if you
have Maven and Tomcat set up to use the Tomcat Maven Plugin (http://mojo.codehaus.org/tomcat-maven-plugin/)
you can use the mvn tomcat:redeploy command instead. 

To run the client against the Tomcat deployed war, run:

  mvn -Pclient -Dwsdl.location='http://localhost:9000/jaxws_async-CXF_VERSION/services/SoapContext/SoapPort?wsdl'

replacing the CXF_VERSION text in the URL with the version of CXF you are using.


To remove the code generated from the WSDL file and the .class
files, run "mvn clean".


