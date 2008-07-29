Colocated Demo using Document/Literal Style
=============================================

Please review the README in the samples directory before
continuing.


Prerequisite
------------

If your environment already includes cxf.jar on the
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
The client target automatically builds the demo.

Using either UNIX or Windows:

  ant client


To remove the code generated from the WSDL file and the .class
files, run "ant clean".


Building and running the demo using maven
---------------------------------------
From the base directory of this sample (i.e., where this README file is
located), the maven pom.xml file can be used to build and run the demo. 

Using either UNIX or Windows:

  mvn install (builds the demo)
  mvn -Pclient (runs demo)


To remove the code generated from the WSDL file and the .class
files, run "mvn clean".


Building the demo using wsdl2java and javac
-------------------------------------------

From the base directory of this sample (i.e., where this README file is
located) first create the target directory build/classes and then 
generate code from the WSDL file.

For UNIX:
  mkdir -p build/classes

  wsdl2java -d build/classes -compile ./wsdl/hello_world.wsdl

For Windows:
  mkdir build\classes
    Must use back slashes.

  wsdl2java -d build\classes -compile .\wsdl\hello_world.wsdl
    May use either forward or back slashes.

Now compile the provided client and server applications with the commands:

For UNIX:

  export CLASSPATH=$CLASSPATH:$CXF_HOME/lib/cxf.jar:./build/classes
  javac -d build/classes src/demo/colocated/server/*.java
  javac -d build/classes src/demo/colocated/client/*.java

For Windows:
  set classpath=%classpath%;%CXF_HOME%\lib\cxf.jar;.\build\classes
  javac -d build\classes src\demo\colocated\server\*.java
  javac -d build\classes src\demo\colocated\client\*.java


Running the demo using java
---------------------------

From the base directory of this sample (i.e., where this README file is
located) run the commands, entered on a single command line:

For UNIX (must use forward slashes):
    java -Djava.util.logging.config.file=$CXF_HOME/etc/logging.properties
         -Dcxf.config.file=./coloc.xml
         demo.colocated.client.Client ./wsdl/hello_world.wsdl

For Windows (may use either forward or back slashes):
  start
    java -Djava.util.logging.config.file=%CXF_HOME%\etc\logging.properties
         -Dcxf.config.file=.\coloc.xml
         demo.colocated.client.Client .\wsdl\hello_world.wsdl

To remove the code generated from the WSDL file and the .class
files, either delete the build directory and its contents or run:

  ant clean
