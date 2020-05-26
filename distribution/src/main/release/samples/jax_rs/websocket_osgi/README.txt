JAX-RS WebSocket OSGi Blueprint Demo 
=================

This is an OSGi Blueprint version of JAX-RS WebSocket Demo.

Building and running the demo using maven
---------------------------------------

From the base directory of this sample (i.e., where this README file is
located), the maven pom.xml file can be used to build and run the demo. 


Using either UNIX or Windows:

  mvn install

This will produce a war file in the target folder.

Starting Karaf (refer to http://karaf.apache.org/manual/latest-3.0.x/quick-start.html.
You can also use Karaf 4.0.x for this demo.)

  bin/karaf


          __ __                  ____      
         / //_/____ __________ _/ __/      
        / ,<  / __ `/ ___/ __ `/ /_        
       / /| |/ /_/ / /  / /_/ / __/        
      /_/ |_|\__,_/_/   \__,_/_/         
  
    Apache Karaf (4.2.8)
  
  Hit '<tab>' for a list of available commands
  and '[cmd] --help' for help on a specific command.
  Hit '<ctrl-d>' or type 'system:shutdown' or 'logout' to shutdown Karaf.


In order to install CXF's features, you need to add the CXF's features repo using

  feature:repo-add cxf 3.n.m

 where 3.n.m corresponds to a valid CXF version number

Install CXF's cxf-jaxrs and cxf-transports-websocket-server features that installs all the required bundles
for this demo bundle:

  feature:install aries-blueprint
  feature:install cxf-jaxrs
  install -s mvn:org.eclipse.jetty.websocket/websocket-api/9.4.22.v20191022
  install -s mvn:org.eclipse.jetty.websocket/websocket-common/9.4.22.v20191022
  install -s mvn:org.eclipse.jetty.websocket/websocket-servlet/9.4.22.v20191022
  install -s mvn:org.eclipse.jetty.websocket/websocket-server/9.4.22.v20191022
  feature:install cxf-transports-websocket-server

Install this demo bundle (using the appropriate bundle version number):

  install -s mvn:org.apache.cxf.samples/jax_rs_websocket_osgi/3.n.m

And verify the bundles are installed, e.g.:

karaf@root()> feature:repo-add cxf 3.3.6
Adding feature url mvn:org.apache.cxf.karaf/apache-cxf/3.3.6/xml/features
karaf@root()>  feature:install aries-blueprint
karaf@root()> feature:install cxf-jaxrs
karaf@root()> install -s mvn:org.eclipse.jetty.websocket/websocket-api/9.4.22.v20191022
Bundle ID: 126
karaf@root()> install -s mvn:org.eclipse.jetty.websocket/websocket-common/9.4.22.v20191022
Bundle ID: 127
karaf@root()> install -s mvn:org.eclipse.jetty.websocket/websocket-servlet/9.4.22.v20191022
Bundle ID: 128
karaf@root()> install -s mvn:org.eclipse.jetty.websocket/websocket-server/9.4.22.v20191022
Bundle ID: 129
karaf@root()> feature:install cxf-transports-websocket-server
karaf@root()> install -s mvn:org.apache.cxf.samples/jax_rs_websocket_osgi/3.3.6
Bundle ID: 132
karaf@root()> list
START LEVEL 100 , List Threshold: 50
 ID | State  | Lvl | Version          | Name
----+--------+-----+------------------+--------------------------------------------------------------------------------
 22 | Active |  80 | 4.2.8            | Apache Karaf :: OSGi Services :: Event
 95 | Active |  80 | 4.14.0           | Apache XBean OSGI Bundle Utilities
 96 | Active |  80 | 4.14.0           | Apache XBean :: Classpath Resource Finder
126 | Active |  80 | 9.4.22.v20191022 | Jetty :: Websocket :: API
127 | Active |  80 | 9.4.22.v20191022 | Jetty :: Websocket :: Common
128 | Active |  80 | 9.4.22.v20191022 | Jetty :: Websocket :: Servlet Interface
129 | Active |  80 | 9.4.22.v20191022 | Jetty :: Websocket :: Server
131 | Active |  80 | 2.5.2            | atmosphere-runtime
132 | Active |  80 | 3.3.6            | JAX-RS WebSocket Blueprint Demo
karaf@root()>


Visit http://localhost:8181/cxf/ to see if this RESTful service is registered.

Using Node.js client to test the service
--------

Go to samples/jax_rs/websocket_osgi/src/test/resources and at the console

Assuming node (>=v4) and npm are installed, execute the following shell commands.

% npm install atmosphere.js
% node client.js

This client program supports websocket and sse and allows
you to choose your preferred protocol.


