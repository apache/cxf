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
1) Start Karaf and at its console, type(*Note)
   feature:repo-add cxf 3.0.2
   feature:install war
   feature:install cxf-management-web
   install -s mvn:org.apache.cxf.samples/logbrowser/3.0.2/war

2) Open browser and go to:
    http://localhost:8181/cxf-samples-logbrowser/log/browser/LogBrowser.html
3) Add new endpoint with URL:
    http://localhost:8181/cxf-samples-logbrowser/log/logs
4) To generate custom log entry open new browser's window and go to:
    http://localhost:8181/cxf-samples-logbrowser/customer-service.html

*Note:
- CXF version 3.0.2 is used in this example, but any verions that include this
sample code may be used.
- As this web.xml registers two CXFServlets (one for the browsing the log and
the other for providing the test service), OSGi system property
org.apache.cxf.osgi.http.transport.disable must be set to false
(in etc/system.properties) to allow these two servlets to be started.
However, this will disable the auto-registration of normal CXF endpoints.
To avoid this issue, use samples/logbrowser-blueprint which is compatible
with the normal CXF endpoints.
