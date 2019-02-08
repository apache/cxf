JAX-RS OpenTracing/Jaeger/Apache Camel Tracing Demo
=================

The demo shows a basic usage of OpenTracing API + Jaeger distributed tracer
across multiple distributed REST-based components. It consists of HTTP route
defined using Apache Camel, which forwards the request to JAX-RS 2.0 (JSR-339)
service, backed by Apache CXF. The demo also contains a JAX-RS 2.0 (JSR-339)
client which initiates the connection to the Apache Camel HTTP route. The
following REST(ful) endpoints are available:

  - GET to http://0.0.0.0:8084/catalog
  - GET to http://localhost:8080/services/books

To collect the traces, please run Jaeger distributed tracer components, the
simplest way would be using Docker:

  docker run --rm -it --network=host -p 16686:16686 -p 14268:14268 jaegertracing/all-in-one

The following sample traces should be available in Jaeger UI (available
be default at http://localhost:16686/search):

  +- cxf-client GET http://localhost:8084/catalog
     +- camel-server GET
        +- camel-server GET
           +- cxf-service GET /services/books

Building and running the demo using Maven
---------------------------------------

From the base directory of this sample (i.e., where this README file is
located), the Maven pom.xml file can be used to build and run the demo.

Using either UNIX or Windows:

  mvn install
  mvn -Pcamel-server  (from one command line window)
  mvn -Pcxf-server  (from one command line window)
  mvn -Pclient  (from a third command line window)

To remove the target dir, run mvn clean".
