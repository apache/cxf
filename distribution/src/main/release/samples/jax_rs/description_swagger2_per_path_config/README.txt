JAX-RS Swagger2Feature Demo
=================

The demo shows a basic usage of Swagger 2.0 API documentation with REST based Web Services using 
JAX-RS 2.0 (JSR-339) and path-based configuration. Swagger UI is available at: http://localhost:9000/

There are two versions of the API deployed under /v1/ and /v2/ base paths. The demo project demonstrates
the usage of path-based configuration for properly publishing Swagger 2.0 API documentation.

Building and running the demo using Maven
---------------------------------------

From the base directory of this sample (i.e., where this README file is
located), the Maven pom.xml file can be used to build and run the demo. 


Using either UNIX or Windows:

  mvn install
  mvn -Pserver  (from one command line window)  
    

After the service is started, the Swagger API documents in JSON and YAML
are available at

  http://localhost:9000/v1/swagger.json
  http://localhost:9000/v1/swagger.yaml

  http://localhost:9000/v2/swagger.json
  http://localhost:9000/v2/swagger.yaml

To view the Swagger document using Swagger-UI, use your Browser to 
open the Swagger-UI page at

  http://localhost:9000/v1?url=swagger.json
  http://localhost:9000/v2?url=swagger.json

or

  http://localhost:9000/v1?url=swagger.yaml
  http://localhost:9000/v2?url=swagger.yaml


To remove the target dir, run mvn clean".



