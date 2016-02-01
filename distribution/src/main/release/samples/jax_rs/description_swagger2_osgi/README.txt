JAX-RS Swagger2Feature Demo for OSGi using Blueprint
=================

The demo shows a basic usage of Swagger 2.0 API documentation with REST based Web Services
using JAX-RS 2.0 (JSR-339). In this demo, the Swagger2Feature is configured using Blueprint.

Building and running the demo
---------------------------------------
(Note this demo currently uses the snapshot version of some componens)

From the base directory of this sample (i.e., where this README file is
located), the Maven pom.xml file can be used to build and run the demo. 


Using either UNIX or Windows:

  mvn install


Starting Karaf (refer to http://karaf.apache.org/manual/latest-3.0.x/quick-start.html)

  bin/karaf


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

 where 3.n.m corresponds to a valid CXF version number (e.g., 3.1.4).

Install CXF's cxf-rs-description-swagger2 feature that installs all the required bundles
for this demo bundle.

  feature:install cxf-rs-description-swagger2

Install this demo bundle (using the appropriate bundle version number)

  install -s mvn:org.apache.cxf.samples/jax_rs_description_swagger2_osgi/3.n.m

You can verify if the CXF JAX-RS Swagger2 Blueprint Demo is installed and started.

  karaf@root()> list
  START LEVEL 100 , List Threshold: 50
   ID | State  | Lvl | Version | Name                              
  -----------------------------------------------------------------
  122 | Active |  80 | 3.1.4   | CXF JAX-RS Swagger2 Blueprint Demo
  karaf@root()>

Now, you will be able to access this CXF JAXRS demo service on your Karaf instance at

  http://localhost:8181/cxf/swaggerSample

And its Swagger API documents in either json or yaml are available at

  http://localhost:8181/cxf/swaggerSample/swagger.json
  http://localhost:8181/cxf/swaggerSample/swagger.yaml


If you do not have your swagger-ui on your local system, you can download 
a copy from its download site.

At the console, type

  wget -N https://github.com/swagger-api/swagger-ui/archive/master.zip
  unzip master.zip

This will extract the content of the swagger-ui zip file. Using your Browser, open
the index.html file at swagger-ui-master/dist/. Finally, type in the above swagger 
document URL in the input field and click on "Explore" to view the document.
