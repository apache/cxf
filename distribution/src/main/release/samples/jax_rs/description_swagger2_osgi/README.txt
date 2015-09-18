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


Install this demo feature using the local features.xml file that is located in
samples/jax_rs/description_swagger2_osgi/target/test-classes/features/features.xml, you
can directly use this file or copy it to somewhere. Assuming you have put this features.xml file at
/Users/me/work/cxf/samples/features.xml

You can add this local feature url by typing

  karaf@root()> feature:repo-add file:///Users/me/work/cxf/samples/features.xml
  Adding feature url file:///Users/me/work/cxf/samples/features.xml

Now you can see the features defined in this features file.

  karaf@root()> feature:list | grep demo
  demo-swagger-core             | 1.0.0            |           | demo-cxf-swagger-sample-1.0.0 | 
  demo-swagger-jaxrs            | 1.0.0            |           | demo-cxf-swagger-sample-1.0.0 | 
  demo-cxf-swagger-jaxrs-sample | 1.0.0            |           | demo-cxf-swagger-sample-1.0.0 | 
  karaf@root()> 

Install the demo sample feature that transitively install other features and bundles that are
required to run this demo sample.

  feature:install demo-cxf-swagger-jaxrs-sample

You can verify if the CXF JAX-RS Swagger2 Blueprint Demo is installed and started.

  karaf@root()> list 
  START LEVEL 100 , List Threshold: 50
   ID | State  | Lvl | Version          | Name                                       
  -----------------------------------------------------------------------------------
  107 | Active |  80 | 1.1.0.Final      | Bean Validation API                        
  108 | Active |  80 | 3.4.0            | Apache Commons Lang                        
  109 | Active |  80 | 2.4.6            | Jackson-core                               
  110 | Active |  80 | 2.4.6            | Jackson-annotations                        
  111 | Active |  80 | 2.4.6            | jackson-databind                           
  112 | Active |  80 | 2.4.6            | Jackson-dataformat-YAML                    
  113 | Active |  80 | 1.5.4.SNAPSHOT   | swagger-annotations                        
  114 | Active |  80 | 1.5.4.SNAPSHOT   | swagger-models                             
  115 | Active |  80 | 1.5.4.SNAPSHOT   | swagger-core                               
  116 | Active |  80 | 18.0.0           | Guava: Google Core Libraries for Java      
  117 | Active |  80 | 3.19.0.GA        | Javassist                                  
  118 | Active |  80 | 0.9.9.2          | Apache ServiceMix :: Bundles :: reflections
  119 | Active |  80 | 2.4.6            | Jackson-JAXRS-base                         
  120 | Active |  80 | 2.4.6            | Jackson-JAXRS-JSON                         
  121 | Active |  80 | 1.5.4.SNAPSHOT   | swagger-jaxrs                              
  122 | Active |  80 | 3.0.7            | CXF JAX-RS Swagger2 Blueprint Demo    
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
