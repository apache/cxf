JAX-RS Swagger2Feature Demo
=================

The demo shows a basic usage of OpenAPI v3.0 documentation with REST based Web Services using 
JAX-RS 2.0 (JSR-339). Swagger UI is available at: http://localhost:9000/

Building and running the demo using Maven
---------------------------------------

From the base directory of this sample (i.e., where this README file is
located), the Maven pom.xml file can be used to build and run the demo. 


Using either UNIX or Windows:

  mvn install
  mvn -Pserver  (from one command line window)  
    

After the service is started, the OpenAPI documents in JSON and YAML
are available at

  http://localhost:9000/openapi.json
  http://localhost:9000/openapi.yaml


To view the OpenAPI document using Swagger-UI, use your Browser to 
open the Swagger-UI page at

  http://localhost:9000/api-docs/?url=/openapi.json

or

  http://localhost:9000/api-docs/?url=/openapi.yaml

or access it from the CXF Services page:

  http://localhost:9000/services
  and follow a Swagger link.
  



