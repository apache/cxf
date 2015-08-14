JAX-RS Swagger2Feature Demo for OSGi using Blueprint
=================

The demo shows a basic usage of Swagger 2.0 API documentation with REST based Web Services
using JAX-RS 2.0 (JSR-339). In this demo, the Swagger2Feature is configured using Blueprint.

Building and running the demo
---------------------------------------

From the base directory of this sample (i.e., where this README file is
located), the Maven pom.xml file can be used to build and run the demo. 


Using either UNIX or Windows:

  mvn install

//TO-DO add karaf's intruction to run the demo


After installing this bundle on your Karaf instance, you will be able to find this service at

  http://localhost:8181/cxf/swaggerSample

And its Swagger API documents in json or yaml are available at

  http://localhost:8181/cxf/swaggerSample/swagger.json
  http://localhost:8181/cxf/swaggerSample/swagger.yaml



