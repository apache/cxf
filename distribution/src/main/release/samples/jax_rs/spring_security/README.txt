JAX-RS Spring Security Demo 
===========================

The demo shows how to use Spring Security to secure a JAXRS-based RESTful service.
 
Two approaches toward securing a service are shown :
- using Spring Security @Secured annotations
- using AspectJ pointcut expressions

Additionally, the JAXRS annotations inheritance is demonstrated, from interface and abstract class 
definitions.


Prerequisites
-------------
Please read the SpringSecurity documentation available at http://static.springframework.org/spring-security/site/reference/html/springsecurity.html.

If you'd like to run a demo with Ant :
 - Download a Spring Framework distribution from http://www.springsource.org/download and unzip/untar it,
   as Fuse does not ship spring-aop.jar. Note the spring version used by Fuse and download a corresponding
   framework distribution.    
 - Download a Spring Security distribution from http://www.springsource.org/download and unzip/untar it.
   2.0.4 version was used to develop this demo but newer versions will also work.
 - Download an AspectJ distribution from http://www.eclipse.org/aspectj/downloads.php

Additionally, If your environment already includes cxf-manifest.jar on the
CLASSPATH, and the JDK and ant bin directories on the PATH
it is not necessary to set the environment as described in
the samples directory README.  If your environment is not
properly configured, or if you are planning on using wsdl2java,
javac, and java to build and run the demos, you must set the
environment.


Building and running the demo using Ant
---------------------------------------

From the base directory of this sample (i.e., where this README file is
located), the Ant build.xml file can be used to build and run the demo. 
The server and client targets automatically build the demo.

Using either UNIX or Windows:

  ant server -Dspring.home=%SPRING_HOME% -Dspring.security.home=%SPRING_SECURITY_HOME% -Daspectj.home=%ASPECTJ_HOME%
     (from one command line window)

  ant client  (from a second command line window)
    

To remove the .class files, either delete the build directory and its contents or run:

ant clean


Building and running the demo using maven
---------------------------------------

From the base directory of this sample (i.e., where this README file is
located), the maven pom.xml file can be used to build and run the demo. 


Using either UNIX or Windows:

  mvn install
  mvn -Pserver  (from one command line window)
  mvn -Pclient  (from a second command line window)
    

To remove the target dir, run "mvn clean".


Building the demo using javac
-------------------------------------------

From the base directory of this sample (i.e., where this README file is
located), first create the target directory build/classes and then 
compile the provided client and server applications with the commands:

For UNIX:  
  mkdir -p build/classes
  
  export CLASSPATH=$CLASSPATH:$CXF_HOME/lib/cxf-manifest.jar:./build/classes
  javac -d build/classes src/demo/jaxrs/client/*.java
  javac -d build/classes src/demo/jaxrs/server/*.java

For Windows:
  mkdir build\classes
    Must use back slashes.

  set classpath=%classpath%;%CXF_HOME%\lib\cxf-manifest.jar;.\build\classes
  javac -d build\classes src\demo\jaxrs\client\*.java
  javac -d build\classes src\demo\jaxrs\service\*.java
  javac -d build\classes src\demo\jaxrs\servlet\*.java

Finally, copy resource files into the build/classes directory with the commands:

For UNIX:    
  cp ./src/demo/jaxrs/client/*.xml ./build/classes/demo/jaxrs/client

For Windows:
  copy src\demo\jaxrs\client\*.xml build\classes\demo\jaxrs\client


Running the demo using java
---------------------------

From the samples/jax-rs/spring_security directory run the following commands. They 
are entered on a single command line.

For UNIX (must use forward slashes):
    java -Djava.util.logging.config.file=$CXF_HOME/etc/logging.properties
         demo.jaxrs.servlet.Server &

    java -Djava.util.logging.config.file=$CXF_HOME/etc/logging.properties
         demo.jaxrs.client.Client

The server process starts in the background.  After running the client,
use the kill command to terminate the server process.

For Windows (may use either forward or back slashes):
  start 
    java -Djava.util.logging.config.file=%CXF_HOME%\etc\logging.properties
       demo.jaxrs.servlet.Server

    java -Djava.util.logging.config.file=%CXF_HOME%\etc\logging.properties
       demo.jaxrs.client.Client

A new command windows opens for the server process.  After running the
client, terminate the server process by issuing Ctrl-C in its command window.

To remove the .class files, either delete the build directory and its contents or run:

  ant clean


What happens when a demo is run
-------------------------------

The demo web application located in a webapp folder is configured for two users, fred and bob, be able to
access various methods of a customer service bean. 

Fred is in both ROLE_CUSTOMER and ROLE_ADMIN roles, while Bob is in the ROLE_CUSTOMER role only.
After the server starts, the client is run and it's shown that Fred can access all the methods
while Bob can access only those which ROLE_CUSTOMER users are permitted to. 

By default, the demo is configured to use AspectJ pointcut expressions to apply ACL rules to a service bean.
Please see webapp/WEB-INF/beans.xml as well as src/demo/jaxrs/service.

demo.jaxrs.service.CustomerServiceImpl bean implements CustomerService interface. AspectJ expressions are applied to interface methods. Note, neither CustomerService interface nor its CustomerServiceImpl implementation have security-specific annotations. CustomerService interface does have JAXRS annotations which are inherited by the service bean.

To see the @Secured annotations in action, please uncomment 

<bean id="customerservice" class="demo.jaxrs.service.CustomerServiceSecuredImpl"/>

and comment the one used by default :

<bean id="customerservice" class="demo.jaxrs.service.CustomerServiceImpl"/>

Note this time @Secured annotations are coming from a CustomerServiceSecured interface, while
JAXRS annotations are inherited from AbstractCustomerServiceSecured class. Also the secure annotations have to be explictly enabled in the configuration :

<security:global-method-security secured-annotations="enabled"/>

Basic authentication is used to provide user credentials to a service. 
No HTTPS protocol is used in this demo for simplicity only.
