JAX-RS SSE OSGi Blueprint Demo 
=================

This is an OSGi Blueprint version of JAX-RS SSE Demo.

Building and running the demo using maven
---------------------------------------

From the base directory of this sample (i.e., where this README file is
located), the Maven pom.xml file can be used to build and run the demo. 


Using either UNIX or Windows:

  mvn install

This will produce a jar file in the target folder.

Starting Karaf (refer to http://karaf.apache.org/manual/latest/quick-start.html)

  bin/karaf


          __ __                  ____      
         / //_/____ __________ _/ __/      
        / ,<  / __ `/ ___/ __ `/ /_        
       / /| |/ /_/ / /  / /_/ / __/        
      /_/ |_|\__,_/_/   \__,_/_/         
  
    Apache Karaf (4.1.5)
  
  Hit '<tab>' for a list of available commands
  and '[cmd] --help' for help on a specific command.
  Hit '<ctrl-d>' or type 'system:shutdown' or 'logout' to shutdown Karaf.


In order to install CXF's features, you need to add the CXF's features repo using

  feature:repo-add cxf 3.n.m

 where 3.n.m corresponds to a valid CXF version number

Install CXF's cxf-sse and cxf-jackson features that installs all the required bundles
for this demo.

  feature:install cxf-sse cxf-jackson
  feature:install aries-blueprint

Install this demo bundle (using the appropriate bundle version number)

  install -s mvn:org.apache.cxf.samples/jax_rs_sse_osgi/3.n.m

And verify the bundles are installed.

karaf@root()> list -t 0 | grep CXF

START LEVEL 100 , List Threshold: 50
 ID | State   | Lvl | Version        | Name
----+---------+-----+----------------+---------------------------------------------------------
  63 | Active   |  40 | 3.2.5.SNAPSHOT   | Apache CXF Core
 64 | Active   |  40 | 3.2.5.SNAPSHOT   | Apache CXF Runtime JAX-RS Frontend
 65 | Active   |  40 | 3.2.5.SNAPSHOT   | Apache CXF Runtime Management
 66 | Active   |  40 | 3.2.5.SNAPSHOT   | Apache CXF JAX-RS Client
 67 | Active   |  40 | 3.2.5.SNAPSHOT   | Apache CXF JAX-RS Extensions: Providers
 68 | Active   |  40 | 3.2.5.SNAPSHOT   | Apache CXF JAX-RS Extensions: Search
 69 | Active   |  40 | 3.2.5.SNAPSHOT   | Apache CXF JAX-RS Extensions: JSON Basic
 71 | Active   |  40 | 3.2.5.SNAPSHOT   | Apache CXF JAX-RS Service Description
 72 | Active   |  40 | 3.2.5.SNAPSHOT   | Apache CXF JAX-RS Server-Side Events Support
 73 | Active   |  40 | 3.2.5.SNAPSHOT   | Apache CXF Runtime HTTP Transport
 74 | Active   |  40 | 3.2.5.SNAPSHOT   | Apache CXF Karaf Commands


karaf@root()> list
START LEVEL 100 , List Threshold: 50
 ID | State   | Lvl | Version        | Name
----+---------+-----+----------------+----------------------------------------------------------------------------------------------------------------------------------------------------------

 29 | Active  |  80 | 4.1.5          | Apache Karaf :: OSGi Services :: Event
 84 | Active  |  80 | 2.4.17         | atmosphere-runtime
110 | Active  |  80 | 3.2.5.SNAPSHOT | JAX-RS SSE Blueprint Demo
115 | Active  |  80 | 2.9.1          | Jackson-JAXRS-base
116 | Active  |  80 | 2.9.1          | Jackson-JAXRS-JSON 

Visit http://localhost:8181/cxf/ to see if this RESTful service is registered.

Using `curl` to test the service
--------

curl http://localhost:8181/cxf/stats/sse -H "Accept: text/event-stream"

Using the web browser of your choice to test the service
--------

http://localhost:8181/cxf/stats/static/index.html