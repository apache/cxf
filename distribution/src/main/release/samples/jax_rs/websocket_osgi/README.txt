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

$ bin/karaf 
        __ __                  ____      
       / //_/____ __________ _/ __/      
      / ,<  / __ `/ ___/ __ `/ /_        
     / /| |/ /_/ / /  / /_/ / __/        
    /_/ |_|\__,_/_/   \__,_/_/         

  Apache Karaf (3.0.4)

Hit '<tab>' for a list of available commands
and '[cmd] --help' for help on a specific command.
Hit '<ctrl-d>' or type 'system:shutdown' or 'logout' to shutdown Karaf.



In order to install CXF's features, you need to add the CXF's features repo using

  feature:repo-add cxf 3.n.m

 where 3.n.m corresponds to a valid CXF version number

Install CXF's cxf-jaxrs and cxf-transports-websocket-server features that installs all the required bundles
for this demo bundle.

  feature:install cxf-jaxrs cxf-transports-websocket-server

Install this demo bundle (using the appropriate bundle version number)

  install -s mvn:org.apache.cxf.samples/jax_rs_websocket_osgi/3.n.m

And verify the bundles are installed.


karaf@root()> feature:repo-add cxf 3.1.7-SNAPSHOT
Adding feature url mvn:org.apache.cxf.karaf/apache-cxf/3.1.7-SNAPSHOT/xml/features
karaf@root()> feature:install cxf-jaxrs cxf-transports-websocket-server
karaf@root()> install -s mvn:org.apache.cxf.samples/jax_rs_websocket_osgi/3.1.7-SNAPSHOT
Bundle ID: 109
karaf@root()> list -t 0 | grep CXF
 80 | Active   |  40 | 3.1.7.SNAPSHOT   | Apache CXF Core                                                    
 81 | Active   |  40 | 3.1.7.SNAPSHOT   | Apache CXF Runtime Management                                      
100 | Active   |  40 | 3.1.7.SNAPSHOT   | Apache CXF Runtime HTTP Transport                                  
102 | Active   |  40 | 3.1.7.SNAPSHOT   | Apache CXF JAX-RS Extensions: Providers                            
103 | Active   |  40 | 3.1.7.SNAPSHOT   | Apache CXF JAX-RS Extensions: Search                               
104 | Active   |  40 | 3.1.7.SNAPSHOT   | Apache CXF JAX-RS Service Description                              
105 | Active   |  40 | 3.1.7.SNAPSHOT   | Apache CXF Runtime JAX-RS Frontend                                 
106 | Active   |  40 | 3.1.7.SNAPSHOT   | Apache CXF JAX-RS Client                                           
108 | Active   |  40 | 3.1.7.SNAPSHOT   | Apache CXF Runtime WebSocket Transport                             
karaf@root()> list
START LEVEL 100 , List Threshold: 50
 ID | State  | Lvl | Version        | Name                           
---------------------------------------------------------------------
107 | Active |  80 | 2.3.7          | atmosphere-runtime             
109 | Active |  80 | 3.1.7.SNAPSHOT | JAX-RS WebSocket Blueprint Demo
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


