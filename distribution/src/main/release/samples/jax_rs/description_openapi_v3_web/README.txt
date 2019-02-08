JAX-RS OpenApiFeature Demo using WebApp 
=================

The demo shows a basic usage of OpenAPI v3.0 documentation with REST based Web Services using 
JAX-RS 2.0 (JSR-339). In this demo, the OpenApiFeature is configured through web.xml file. The
sample also demonstrates usage of the openapi-configuration.json configuration file to publish
API metadata and servers / base path to use. 

Building and running the demo using Maven
---------------------------------------

From the base directory of this sample (i.e., where this README file is
located), the Maven pom.xml file can be used to build and run the demo. 

Using either UNIX or Windows:

  mvn install
  mvn jetty:run-war

The sample JAX-RS endpoint becomes available after the service has started. 
OpenAPI v3.0 documents in JSON and YAML are available at

  http://localhost:9000/app/openapi.json
  http://localhost:9000/app/openapi.yaml

To view the OpenAPI document using Swagger-UI, use your Browser to 
open the Swagger-UI page at

  http://localhost:9000/app/api-docs?url=/app/openapi.json

To remove the target dir, run mvn clean".



