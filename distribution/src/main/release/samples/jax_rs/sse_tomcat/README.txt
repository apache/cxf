JAX-RS SSE Demo 
=================

This is a SSE version of JAX-RS Basic Demo.

A SSE endpoint service is provided on URL http://localhost:8686/rest/api/stats/sse/{id}
where {id} is any integer value, f.e.:

  http://localhost:8686/rest/api/stats/sse/1

This sample includes a simple web UI using Highcharts JavaScript library to show off
randomly generated statistics about particular server, pushed to the client using
SSE JAX-RS endpoint. The UI is available at

  http://localhost:8686/static/index.html 

Under the hood, embedded Tomcat 8 container is being used.

Building and running the demo using maven
---------------------------------------

From the base directory of this sample (i.e., where this README file is
located), the maven pom.xml file can be used to build and run the demo. 

Using either UNIX or Windows:

  mvn install
  mvn -Pserver

To remove the target dir, run mvn clean".  

Connecting to the SSE stream
---------------------------------------

Open a web browser at: http://localhost:8686/static/index.html 
