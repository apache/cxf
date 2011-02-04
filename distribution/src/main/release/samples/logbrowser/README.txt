Prerequisite
------------

If your environment already includes cxf-manifest.jar on the
CLASSPATH, and the JDK and ant bin directories on the PATH
it is not necessary to set the environment as described in
the samples directory README.  If your environment is not
properly configured, or if you are planning on using wsdl2java,
javac, and java to build and run the demos, you must set the
environment.

Building and running the demo using Maven
---------------------------------------

From the base directory of this sample (i.e., where this README file is
located), the pom.xml file is used to build and run the demo. 

Using either UNIX or Windows:

1)  Build and run by typing in terminal
    mvn install
2) Open browser and go to:
    http://localhost:9002/log/browser/LogBrowser.html
3) Add new endpoint with URL:
    http://localhost:9002/log/logs
4) To generate custom log entry open new browser's window and go to:
    http://localhost:9002/customer-service.html

To remove the .class files, run "mvn clean".
