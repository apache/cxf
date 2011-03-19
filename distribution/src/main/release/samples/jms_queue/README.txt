JMS Transport Demo using Document-Literal Style
===============================================

This sample demonstrates use of the Document-Literal style 
binding over JMS Transport using the queue mechanism.

Please review the README in the samples directory before
continuing.

This demo uses ActiveMQ as the JMS implementation for 
illustration purposes only.




Building and running the demo using maven
---------------------------------------
  
From the base directory of this sample (i.e., where this README file is
located), the Ant build.xml file can be used to build and run the demo. 
  
Using either UNIX or Windows:

    mvn install (this will build the demo)
    In separate command windows/shells:
    mvn -Pjms.broker
    mvn -Pserver
    mvn -Pclient

To remove the code generated from the WSDL file and the .class
files, run "mvn clean".


