JAX-RS SSE Demo 
=================

This is a SSE client version of JAX-RS Basic Demo.

A SSE endpoint service is provided on URL http://localhost:8686/rest/stats/sse, f.e.:

  http://localhost:8686/rest/stats/sse

Under the hood, Undertow application container is being used. The client is a simple 
SSE EventSource consumer which prints each event received in the console. 

Building and running the demo using maven
---------------------------------------

From the base directory of this sample (i.e., where this README file is
located), the maven pom.xml file can be used to build and run the demo. 

Using either UNIX or Windows:

  mvn install
  mvn -Pserver
  mvn -Pclient

To remove the target dir, run "mvn clean".  

Connecting to the SSE stream
---------------------------------------

Open a web browser at: http://localhost:8686/static/index.html 
