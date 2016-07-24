JAX-RS Swagger2Feature Demo
=================

The demo shows a basic usage of Swagger 2.0 API documentation with REST based Web Services using 
JAX-RS 2.0 (JSR-339). Swagger UI is available at: http://localhost:9000/

Building and running the demo using Maven
---------------------------------------

From the base directory of this sample (i.e., where this README file is
located), the Maven pom.xml file can be used to build and run the demo. 


Using either UNIX or Windows:

  mvn install
  mvn -Pserver  (from one command line window)  
    

After the service is started, the Swagger API documents in JSON and YAML
are available at

  http://localhost:9000/swagger.json
  http://localhost:9000/swagger.yaml


To view the Swagger document using Swagger-UI, use your Browser to 
open the Swagger-UI page at

  http://localhost:9000/api-docs?url=/swagger.json

or

  http://localhost:9000/api-docs?url=/swagger.yaml

or access it from the CXF Services page:

  http://localhost:9000/services
  and follow a Swagger link.
  



