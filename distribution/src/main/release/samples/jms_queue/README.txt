JMS Transport Demo using Document-Literal Style
===============================================

This sample demonstrates use of the Document-Literal style binding over 
JMS Transport using the queue mechanism. This sample uses a 
start-from-WSDL approach, see the similar java_first_jms sample
for a Java-first sample.

Please review the README in the samples directory before
continuing.

This demo uses ActiveMQ as the JMS implementation for 
illustration purposes only.



Building and running the demo using Maven
---------------------------------------
  
From the base directory of this sample (i.e., where this README file is
located), using either UNIX or Windows:

    mvn install (this will build the demo)

    In separate command windows/shells:
    mvn -Pjms.broker
    mvn -Pserver
    mvn -Pclient

To remove the code generated from the WSDL file and the .class
files, run "mvn clean".


