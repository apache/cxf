Building and running the logbrowser demo using Maven
----------------------------------------------------

From the base directory of this sample (i.e., where this README file is
located), the pom.xml file is used to build and run the demo. 

Using either UNIX or Windows:

0) Build the project by typing:
    mvn clean install
1) Start the server:
    mvn -Pserver 
2) Open browser and go to:
    http://localhost:9002/log/browser/LogBrowser.html
3) Add new endpoint with URL:
    http://localhost:9002/log/logs
4) To generate custom log entry open new browser's window and go to:
    http://localhost:9002/customer-service.html

To remove the .class files, run "mvn clean".


This sample project generates a war file which can be used in a servlet container
or even in an OSGi container.

Assuming you have a freshly intalled Apache Karaf-3.x,
1) Start Karaf and at its console, type (note m.n corresponding the version numbers)
   feature:repo-add cxf 3.m.n
   feature:install war
   feature:install cxf-management-web
   install -s mvn:org.apache.cxf.samples/logbrowser/3.1.0-SNAPSHOT/war

2) Open browser and go to:
    http://localhost:8181/cxf-samples-logbrowser/log/browser/LogBrowser.html
3) Add new endpoint with URL:
    http://localhost:8181/cxf-samples-logbrowser/log/logs
4) To generate custom log entry open new browser's window and go to:
    http://localhost:8181/cxf-samples-logbrowser/customer-service.html
