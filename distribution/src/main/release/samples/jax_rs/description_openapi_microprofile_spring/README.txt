JAX-RS Swagger2Feature Spring Demo
=================

The demo shows a basic usage of OpenAPI v3.0 API documentation with REST based Web Services using 
JAX-RS 2.0 (JSR-339) and Microprofile implementation.

Building and running the demo using Maven
---------------------------------------

From the base directory of this sample (i.e., where this README file is
located), the Maven pom.xml file can be used to build and run the demo. 


Using either UNIX or Windows:

  mvn install
  mvn -Pserver  (from one command line window)  
    

After the service is started, the Swagger API documents in JSON and YAML
are available at

  http://localhost:9000/openapi.json
  http://localhost:9000/openapi.yaml

To remove the target dir, run mvn clean".

To navigate to the hosted Swagger UI, please type in the browser: 

  http://localhost:9000/api-docs/?url=/openapi.json


