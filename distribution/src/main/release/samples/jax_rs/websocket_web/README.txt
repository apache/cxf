JAX-RS WebSocket WebApp Demo 
=================

This is an WebApp version of JAX-RS WebSocket Demo.

Building and running the demo using maven
---------------------------------------

From the base directory of this sample (i.e., where this README file is
located), the maven pom.xml file can be used to build and run the demo. 


Using either UNIX or Windows:

  mvn install

This will produce a war file in the target folder.

To run the war file using jetty9

  mvn jetty:run-war  (from one command line window)

To run the war file using tomcat7

  mvn tomcat7:run-war  (from one command line window)

To remove the target dir, run mvn clean".


You can use the same clients included in JAX-RS WebSockt Demo.
Refer to samples/jax_rs/websocket/README.txt for more information.

