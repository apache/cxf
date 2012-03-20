Building and running the logbrowser demo using Maven
----------------------------------------------------

From the base directory of this sample (i.e., where this README file is
located), the pom.xml file is used to build and run the demo. 

Using either UNIX or Windows:

1) Build the project by typing:
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
