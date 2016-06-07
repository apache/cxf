JAX-RS Swagger2Feature Spring Demo
=================

The demo shows a basic usage of Swagger 2.0 API documentation with REST based Web Services using 
JAX-RS 2.0 (JSR-339).

Building and running the demo using Maven
---------------------------------------

From the base directory of this sample (i.e., where this README file is
located), the Maven pom.xml file can be used to build and run the demo. 


Using either UNIX or Windows:

  mvn install
  mvn -Pserver  (from one command line window)  
    

After the service is started, the Swagger API documents in JSON and YAML
are available at

  http://localhost:9000/swaggerSample/swagger.json
  http://localhost:9000/swaggerSample/swagger.yaml

To remove the target dir, run mvn clean".


If you do not have your swagger-ui on your local system, you can download 
a copy from its download site.

At the console, type

  wget -N https://github.com/swagger-api/swagger-ui/archive/master.zip
  unzip master.zip

This will extract the content of the swagger-ui zip file. Using your Browser, open
the index.html file at swagger-ui-master/dist/. Finally, type in the above swagger 
document URL in the input field and click on "Explore" to view the document.



