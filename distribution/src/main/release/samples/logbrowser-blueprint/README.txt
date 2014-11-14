Building and running the logbrowser-blueprint demo on Karaf
----------------------------------------------------

From the base directory of this sample (i.e., where this README file is
located), the pom.xml file is used to build and run the demo. 

Using either UNIX or Windows:

0) Build the project by typing:
    mvn clean install

This sample project generates a bundle file which can be used in an OSGi container.

Assuming you have a freshly intalled Apache Karaf-3.x,
1) Start Karaf and at its console, type(*Note)
   feature:repo-add cxf 3.0.2
   feature:install cxf-management-web
   install -s mvn:org.apache.cxf.samples/logbrowser-blueprint/3.0.2

2) Open browser and go to:
    http://localhost:8181/cxf/samples/logbrowser/browser/LogBrowser.html

3) Add a new endpoint with URL:
    http://localhost:8181/cxf/samples/logbrowser/logs

4) To generate custom log entry, run some CXF scenarios that write some logs.

Additional features
5) Open the Log-Browser(PlainView) page by opening file test/resources/index_plain.html in the browser.

6) Click on the Connect button to open a WebSocket connection to the log browser service. 
Once the connection is open, click on the Subscribe button to subscribe to the logging feed.
When there are new log entries, they will be pushed to the socket and displayed on the Log text area.

*Note:
- CXF version 3.0.2 is used in this example, but any verions that include this
sample code may be used.
- The default logging setting is set to org.apache.cxf:INFO. This can be changed
in the configuration file org.apache.cxf.samples.logbrowser.cfg file in etc/. 
