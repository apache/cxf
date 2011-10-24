WS-Notification Demo
====================

This demo shows how to use the WS-Notification service and API's 
provided by Apache CXF.


Please review the README in the samples directory before continuing.


Building and running the demo using Maven
-----------------------------------------

From the base directory of this sample (i.e., where this README file is
located), the maven pom.xml file can be used to build and run the demo. 

Using either UNIX or Windows:

  mvn install (builds the demo)
  mvn -Pwsn-server  (from one command line window)
  Mvn -Pclient  (from a second command line window)

On startup, the client will create a Consumer and subscribe that consumer
to a specific Topic on the broker.  It will then use the NotificationBroker
API's to notify the Consumers of an event.



