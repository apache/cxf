JAX-RS OpenTelemetry Demo in OSGi container
=================

The demo shows a basic usage of OpenTelemetry API tracing capabilities 
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
  
Starting Karaf (refer to http://karaf.apache.org/manual/latest-4.4.x/quick-start.html)

  bin/karaf


          __ __                  ____      
         / //_/____ __________ _/ __/      
        / ,<  / __ `/ ___/ __ `/ /_        
       / /| |/ /_/ / /  / /_/ / __/        
      /_/ |_|\__,_/_/   \__,_/_/         
  
    Apache Karaf (4.4.3)
  
  Hit '<tab>' for a list of available commands
  and '[cmd] --help' for help on a specific command.
  Hit '<ctrl-d>' or type 'system:shutdown' or 'logout' to shutdown Karaf.


In order to install CXF's features, you need to add the CXF's features repo using

  feature:repo-add cxf 3.n.m

where 3.n.m corresponds to a valid CXF version number (e.g., 3.6.2).

Install CXF's cxf-tracing-opentelemetry feature that installs all the required bundles
for this demo bundle.

  feature:install cxf-jaxrs
  feature:install cxf-jsr-json
  feature:install cxf-tracing-opentelemetry
  feature:install aries-blueprint

Install the exporter compatible with OpenTelemetry API, as in this example 
we are using logging exporter:

  install -s wrap:mvn:io.opentelemetry/opentelemetry-sdk-common/1.29.0
  install -s wrap:mvn:io.opentelemetry/opentelemetry-sdk-metrics/1.29.0
  install -s wrap:mvn:io.opentelemetry/opentelemetry-sdk-trace/1.29.0
  install -s wrap:mvn:io.opentelemetry/opentelemetry-sdk-logs/1.29.0
  install -s wrap:mvn:io.opentelemetry/opentelemetry-sdk/1.29.0
  install -s wrap:mvn:io.opentelemetry/opentelemetry-exporter-common/1.29.0
  install -s wrap:mvn:io.opentelemetry/opentelemetry-exporter-logging/1.29.0

Install this demo bundle (using the appropriate bundle version number)
  
  install -s mvn:org.apache.cxf.samples/jax_rs_tracing_opentelemetry_osgi/3.n.m

You can verify if the CXF JAX-RS OpenTelemetry Blueprint Demo is installed and started.

  karaf@root()> list
  START LEVEL 100 , List Threshold: 50
   ID | State  | Lvl | Version   | Name
  ----+--------+-----+-----------+---------------------------------------------------------------------
  129 | Active |  80 | 0                  | wrap_mvn_io.opentelemetry_opentelemetry-sdk-common_1.29.0
  131 | Active |  80 | 0                  | wrap_mvn_io.opentelemetry_opentelemetry-sdk-metrics_1.29.0
  132 | Active |  80 | 0                  | wrap_mvn_io.opentelemetry_opentelemetry-sdk-trace_1.29.0
  133 | Active |  80 | 0                  | wrap_mvn_io.opentelemetry_opentelemetry-sdk-logs_1.29.0
  134 | Active |  80 | 0                  | wrap_mvn_io.opentelemetry_opentelemetry-sdk_1.29.0
  135 | Active |  80 | 0                  | wrap_mvn_io.opentelemetry_opentelemetry-exporter-common_1.29.0
  143 | Active |  80 | 0                  | wrap_mvn_io.opentelemetry_opentelemetry-exporter-logging_1.29.0
  149 | Active |  80 | 3.6.2.SNAPSHOT     | JAX-RS Demo Using Distributed Tracing with OpenTelemetry API and OSGi  
  karaf@root()>

Now, you will be able to access this CXF JAXRS demo service on your Karaf instance at

  http://localhost:8181/cxf/catalog
  
The following sample traces should be available in Karaf logs:

  20:49:19.850 INFO [BatchSpanProcessor_WorkerThread-1] 'Looking for books' : 9b12a38929dccef988a01e0855afcf58 9784703dce7ad842 INTERNAL [tracer: tracer:] {}
  20:49:19.851 INFO [BatchSpanProcessor_WorkerThread-1] 'GET /cxf/catalog' : 9b12a38929dccef988a01e0855afcf58 21dd42bf438a164a SERVER [tracer: tracer:] AttributesMap{data={http.status_code=200, http.url=http://localhost:8181/cxf/catalog, http.method=GET}, capacity=128, totalAddedValues=3}
