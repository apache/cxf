Apache CXF JAX-WS Failover Demo for OSGi using Blueprint
=================

The demo shows a basic usage of the clustering failover feature for JAX-WS 
services using OSGi environment.

Building and running the demo
---------------------------------------
The demo consists of two parts:
 - sample JAX-WS server
 - sample JAX-WS client which uses failover feature

From the base directory of this sample (i.e., where this README file is
located), build both projects using Maven using either UNIX or Windows:

  cd failover_server 
  mvn install

  cd failover_jaxws_osgi
  mvn install

Starting Karaf (refer to http://karaf.apache.org/manual/latest/quick-start.html)

  $ bin/karaf                                                                                         
        __ __                  ____                                                                 
        / //_/____ __________ _/ __/                                                                 
       / ,<  / __ `/ ___/ __ `/ /_                                                                   
      / /| |/ /_/ / /  / /_/ / __/                                                                   
     /_/ |_|\__,_/_/   \__,_/_/                                                                      
                                                                                                     
    Apache Karaf (4.0.3)                                                                              
                                                                                                     
  Hit '<tab>' for a list of available commands                                                        
  and '[cmd] --help' for help on a specific command.                                                  
  Hit '<ctrl-d>' or type 'system:shutdown' or 'logout' to shutdown Karaf.                             
                                                                                                     
  karaf@root()>                                                                                       

In order to install CXF's features, you need to add the CXF's features repo using

  feature:repo-add cxf 3.n.m

 where 3.n.m corresponds to a valid CXF version number (e.g., 3.1.5).

Install CXF's cxf-jaxws and cxf-features-clustering features:

  feature:install aries-blueprint cxf-jaxws cxf-features-clustering

Install the required bundles:

  install -s wrap:mvn:org.codeartisans/org.json/20130213
  install -s mvn:joda-time/joda-time/2.8.1
  install -s mvn:org.qi4j.core/org.qi4j.core.functional/2.1
  install -s mvn:org.qi4j.core/org.qi4j.core.api/2.1
  install -s mvn:org.qi4j.core/org.qi4j.core.io/2.1
  install -s mvn:org.qi4j.core/org.qi4j.core.spi/2.1
  install -s mvn:org.qi4j.core/org.qi4j.core.bootstrap/2.1
  install -s mvn:org.qi4j.library/org.qi4j.library.jmx/2.1
  install -s mvn:org.qi4j.library/org.qi4j.library.circuitbreaker/2.1
  
Install this demo bundles:
  install -s mvn:org.apache.cxf.samples/failover_server/3.n.m
  install -s mvn:org.apache.cxf.samples/failover_jaxws_osgi/3.n.m

You can verify if the Apache CXF Blueprint Demo Server and JAX-WS
Failover client are installed and started.

  karaf@root()> list
  
  START LEVEL 100 , List Threshold: 50
   ID | State  | Lvl | Version | Name                              
  -----------------------------------------------------------------
   52 | Active |  80 | 3.2.0.SNAPSHOT | Apache CXF Blueprint Demo Server
   53 | Active |  80 | 3.2.0.SNAPSHOT | Apache CXF JAX-WS Failover Blueprint Demo
  
  karaf@root()>

Now, you will be able to access this CXF JAX-WS demo service on your Karaf instance 
at:

  http://localhost:8181/cxf/sample

The JAX-WS client has been configured to use circuit breaker failover feature and 
sequential strategy with two addresses:

  http://localhost:8181/cxf/sample
  http://localhost:8282/cxf/sample  
