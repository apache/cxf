JAX-RS OpenTracing/Jaeger Demo in OSGi container
=================

The demo shows a basic usage of OpenTracing API + Jaeger distributed tracer 
with REST based Web Services using JAX-RS 2.0 (JSR-339), deployed inside OSGi
container. The server provides the following services at base URL 
http://localhost:8181/cxf/catalog: 

  GET http://localhost:8181/cxf/catalog 
  POST http://localhost:8181/cxf/catalog 
  GET http://localhost:8181/cxf/catalog/<id>
  DELETE http://localhost:8181/cxf/catalog/<id>


Building and running the demo using Maven
---------------------------------------

From the base directory of this sample (i.e., where this README file is
located), the Maven pom.xml file can be used to build and run the demo. 

Using either UNIX or Windows:

  mvn install
  
Starting Karaf (refer to http://karaf.apache.org/manual/latest-4.1.x/quick-start.html)

  bin/karaf


          __ __                  ____      
         / //_/____ __________ _/ __/      
        / ,<  / __ `/ ___/ __ `/ /_        
       / /| |/ /_/ / /  / /_/ / __/        
      /_/ |_|\__,_/_/   \__,_/_/         
  
    Apache Karaf (4.1.2)
  
  Hit '<tab>' for a list of available commands
  and '[cmd] --help' for help on a specific command.
  Hit '<ctrl-d>' or type 'system:shutdown' or 'logout' to shutdown Karaf.


In order to install CXF's features, you need to add the CXF's features repo using

  feature:repo-add cxf 3.n.m

where 3.n.m corresponds to a valid CXF version number (e.g., 3.2.1).

Install CXF's cxf-tracing-opentracing feature that installs all the required bundles
for this demo bundle.

  feature:install cxf-jaxrs
  feature:install cxf-jsr-json
  feature:install cxf-tracing-opentracing
  feature:install aries-blueprint
  
Install the distributed tracer compatible with OpenTracing API, as in this example 
we are using Uber Jaeger:

  install -s wrap:mvn:com.squareup.okio/okio/1.13.0
  install -s wrap:mvn:com.squareup.okhttp3/okhttp/3.9.0
  install -s wrap:mvn:org.apache.thrift/libthrift/0.12.0
  install -s wrap:mvn:io.jaegertracing/jaeger-core/1.0.0
  install -s wrap:mvn:io.jaegertracing/jaeger-thrift/1.0.0
  
Install this demo bundle (using the appropriate bundle version number)
  
  install -s mvn:org.apache.cxf.samples/jax_rs_tracing_opentracing_osgi/3.n.m

You can verify if the CXF JAX-RS OpenTracing Blueprint Demo is installed and started.

  karaf@root()> list
  START LEVEL 100 , List Threshold: 50
   ID | State  | Lvl | Version   | Name
  ----+--------+-----+-----------+---------------------------------------------------------------------
   28 | Active |  80 | 4.1.2     | Apache Karaf :: OSGi Services :: Event
  112 | Active |  80 | 0         | wrap_mvn_com.squareup.okio_okio_1.13.0
  113 | Active |  80 | 0         | wrap_mvn_com.squareup.okhttp3_okhttp_3.8.1
  114 | Active |  80 | 0.9.2     | Apache Thrift
  115 | Active |  80 | 0         | wrap_mvn_com.uber.jaeger_jaeger-core_0.20.6
  116 | Active |  80 | 0         | wrap_mvn_com.uber.jaeger_jaeger-thrift_0.20.6
  117 | Active |  80 | 3.n.m     | JAX-RS Demo Using Distributed Tracing with OpenTracing API and OSGi
  
  karaf@root()>

To collect the traces, please run Jaeger distributed tracer components, the
simplest way would be using Docker:

  docker run --rm -it --network=host -p 16686:16686 -p 14268:14268 jaegertracing/all-in-one

Now, you will be able to access this CXF JAXRS demo service on your Karaf instance at

  http://localhost:8181/cxf/catalog
  
The following sample traces should be available in Jaeger UI (available
be default at http://localhost:16686/search):

  +- cxf-server GET /cxf/catalog
     +- cxf-server Looking for books