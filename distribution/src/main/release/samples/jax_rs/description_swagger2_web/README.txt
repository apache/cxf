JAX-RS Swagger2Feature Demo using WebApp with Spring
=================

The demo shows a basic usage of Swagger API documentation with multiple REST based Web Services using 
JAX-RS 2.0 (JSR-339). In this demo, the Swagger2Feature is configured using Spring.

Building and running the demo using Maven
---------------------------------------

From the base directory of this sample (i.e., where this README file is
located), the Maven pom.xml file can be used to build and run the demo. 


Using either UNIX or Windows:

  mvn install
  mvn package tomcat7:run


Two JAX-RS endpoints are availbale after the service has started. 
Swagger API documents in JSON and YAML are available at

  http://localhost:9000/app/swaggerSample/swagger.json
  http://localhost:9000/app/swaggerSample/swagger.yaml

and

  http://localhost:9000/app/swaggerSample2/swagger.json
  http://localhost:9000/app/swaggerSample2/swagger.yaml

To view the Swagger document using Swagger-UI, use your Browser to 
open the Swagger-UI page at

  http://localhost:9000/app/swaggerSample/api-docs?url=/app/swaggerSample/swagger.json
  or
  http://localhost:9000/app/swaggerSample2/api-docs?url=/app/swaggerSample2/swagger.json

or go to the CXF services page:

  http://localhost:9000/app/services

and follow Swagger links.

To remove the target dir, run mvn clean".



