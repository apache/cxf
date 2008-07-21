JMS Transport Demo using Document-Literal Style
===============================================

This sample demonstrates use of the Document-Literal style 
binding over JMS Transport using the queue mechanism.

Please review the README in the samples directory before
continuing.

This demo uses ActiveMQ as the JMS implementation for 
illustration purposes only. Other JMS implementations 
such as Apache Qpid could also be used in the same way.
For details on how to use this demo on non-ActiveMQ 
implementations please check "Using other JMS implementaions"
below.


Prerequisite
------------

If your environment already includes cxf-manifest.jar on the
CLASSPATH, and the JDK and ant bin directories on the PATH
it is not necessary to set the environment as described in
the samples directory README.  If your environment is not
properly configured, or if you are planning on using wsdl2java,
javac, and java to build and run the demos, you must set the
environment.


Building and running the demo using ant
---------------------------------------

This demo requires ActiveMQ 5.0.0. Before you run this
demo, please make sure you had installed the ActiveMQ 5.0.0 and
set ACTIVEMQ_HOME and ACTIVEMQ_VERSION environment variables.
ActiveMQ 5.0.0 the version variable should be

For Unix:
export ACTIVEMQ_HOME=/installdir/apache-activemq-5.0.0

For Windows:
set ACTIVEMQ_VERSION=installdir\apache-activemq-5.0.0

ActiveMQ 5.0.0 the version variable should be
For Unix:
export ACTIVEMQ_VERSION=5.0.0

For Windows:
set ACTIVEMQ_VERSION=5.0.0

From the base directory of this sample (i.e., where this README file is
located), the Ant build.xml file can be used to build and run the demo. 
The server and client targets automatically build the demo.

Using either UNIX or Windows:

  ant jmsbroker.start 
  ant server 
  ant client
    

To remove the code generated from the WSDL file and the .class
files, run "ant clean".


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


Building the demo using wsdl2java and javac
-------------------------------------------

From the base directory of this sample (i.e., where this README file is
located) first create the target directory build/classes and then 
generate code from the WSDL file.

For UNIX:
  mkdir -p build/classes

  wsdl2java -d build/classes -compile ./wsdl/jms_greeter.wsdl

For Windows:
  mkdir build\classes
    Must use back slashes.

  wsdl2java -d build\classes -compile .\wsdl\jms_greeter.wsdl
    May use either forward or back slashes.

Now compile the provided client and server applications with the commands:

For UNIX:  
  
  export CLASSPATH=$CLASSPATH:$CXF_HOME/lib/cxf-manifest.jar:./build/classes:
$ACTIVEMQ_HOME/apache-activemq-$ACTIVEMQ_VERSION.jar
  javac -d build/classes src/demo/jms_greeter/client/*.java
  javac -d build/classes src/demo/jms_greeter/server/*.java

For Windows:
  set classpath=%classpath%;%CXF_HOME%\lib\cxf-manifest.jar;.\build\classes;
%ACTIVEMQ_HOME%\apache-activemq-%ACTIVEMQ_VERSION%.jar
  javac -d build\classes src\demo\jms_greeter\client\*.java
  javac -d build\classes src\demo\jms_greeter\server\*.java


Running the demo using java
---------------------------

From the base directory of this sample (i.e., where this README file is
located) run the commands, entered on a single command line:

For UNIX (must use forward slashes):
    java -Djava.util.logging.config.file=$CXF_HOME/etc/logging.properties
         demo.jms_greeter.server.Server &

    java -Djava.util.logging.config.file=$CXF_HOME/etc/logging.properties
         demo.jms_greeter.client.Client ./wsdl/jms_greeter.wsdl

The server process starts in the background.  After running the client,
use the kill command to terminate the server process.

For Windows (may use either forward or back slashes):
  start 
    java -Djava.util.logging.config.file=%CXF_HOME%\etc\logging.properties
         demo.jms_greeter.server.Server

    java -Djava.util.logging.config.file=%CXF_HOME%\etc\logging.properties
         demo.jms_greeter.client.Client .\wsdl\jms_greeter.wsdl

A new command windows opens for the server process.  After running the
client, terminate the server process by issuing Ctrl-C in its command window.


Now you can stop ActiveMQ JMS Broker by issuing Ctrl-C in its command window.

To remove the code generated from the WSDL file and the .class
files, either delete the build directory and its contents or run:

  ant clean


Using other JMS implementations:
-------------------------------


Using it with Apache Qpid:  
-------------------------

Before running the run, you will have to set QPID_HOME in your environment, 
and also have to start the Qpid broker. 

The following changes are needed to the demo to make it work for Apache Qpid.

1. Changes to wsdl/jms_greeter.wsdl
- Replace the jms:address element under wsdl:port with:                
      <jms:address
          destinationStyle="queue"
          jndiConnectionFactoryName="local"
          jndiDestinationName="dq">
          <jms:JMSNamingProperty name="java.naming.factory.initial" 
               value="org.apache.qpid.jndi.PropertiesFileInitialContextFactory"/>
          <jms:JMSNamingProperty name="java.naming.provider.url" 
               value="tcp://localhost:5672"/>
          <jms:JMSNamingProperty name="connectionfactory.local" 
               value="amqp://guest:guest@clientid/test?brokerlist='tcp://localhost:5672'"/>
          <jms:JMSNamingProperty name="queue.dq" value="queue://queuetest"/>
      </jms:address>

2. Changes to build.xml.
- Replace 
    <condition property="activemq.home" value="${env.ACTIVEMQ_HOME}">
        <isset property="env.ACTIVEMQ_HOME"/>
    </condition>
    <fail message="this sample need to use activemq, please setup  ACTIVEMQ_HOME in your environment"
        unless="activemq.home"/>
    <condition property="activemq.version" value="${env.ACTIVEMQ_VERSION}">
	<isset property="env.ACTIVEMQ_VERSION"/>
    </condition>
    <fail message="this sample need to use activemq, please setup ACTIVEMQ_VERSION in your envrionment"
        unless="activemq.version"/>
    <property name="thirdparty.classpath" location="${activemq.home}/incubator-activemq-${activemq.version}.jar"/>
 
  with

   <condition property="qpid.home" value="${env.QPID_HOME}">
        <isset property="env.QPID_HOME"/>
    </condition>

    <fail message="this sample need to use qpid, please setup QPID_HOME in your environment"
        unless="qpid.home"/>

    <property name="thirdparty.classpath" location="${qpid.home}/lib/qpid-incubating.jar"/>    

- Remove the target jmsbroker.start from build.xml. This is only a helper function to start the 
  Active MQ broker, and is not required if the broker can be started through the scripts.

3. Remove the file, EmbeddedBroker.java from under the directory src/demo/jms_greeter/broker. This is
   only a helper function and therefore need not be used, as long as the broker is started
   through the scripts.
