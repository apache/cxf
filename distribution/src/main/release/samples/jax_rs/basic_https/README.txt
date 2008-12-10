JAX-RS Basic Demo With HTTPS communications
===========================================

This demo takes the JAX-RS basic demo a step further 
by doing the communication using HTTPS.

The JAX-RS server is configured with a HTTPS listener. The listener 
requires client authentication so the client must provide suitable 
credentials. The listener configuration is taken from the 
"CherryServer.xml" file located under demo directory.  

The client is configured to provide its certificate "CN=Wibble" and
chain stored in the Java KeyStore "certs/wibble.jks" to the server. The
server authenticates the client's certificate using its trust store
"certs/truststore.jks", which holds the Certificate Authorities'
certificates.

Likewise the client authenticates the server's certificate "CN=Cherry"
and chain against the same trust store.  Note also the usage of the
cipherSuitesFilter configuration in the configuration files,
where each party imposes different ciphersuites contraints, so that the
ciphersuite eventually negotiated during the TLS handshake is acceptable
to both sides. This may be viewed by adding a -Djavax.net.debug=all 
argument to the JVM.

But please note that it is not adviseable to store sensitive data such
as passwords stored in a clear text configuration file, unless the
file is sufficiently protected by OS level permissions. The KeyStores
may be configured programatically so using user interaction may be
employed to keep passwords from being stored in configuration files.
The approach taken here is for demonstration reasons only. 

Please review the README in the samples directory before
continuing.


Prerequisites
-------------

If your environment already includes cxf-manifest.jar on the
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

  ant server  (from one command line window)
  ant client  (from a second command line window)
    

To remove the code generated from the WSDL file and the .class
files, run "ant clean".


Building the demo using wsdl2java and javac
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
  javac -d build\classes src\demo\jaxrs\server\*.java


Finally, copy resource files into the build/classes directory with the commands:

For UNIX:    
  cp ./src/demo/jaxrs/client/*.xml ./build/classes/demo/jaxrs/client
  cp ./src/demo/jaxrs/server/*.xml ./build/classes/demo/jaxrs/server

For Windows:
  copy src\demo\jaxrs\client\*.xml build\classes\demo\jaxrs\client
  copy src\demo\jaxrs\server\*.xml build\classes\demo\jaxrs\server


Running the demo using java
---------------------------

From the samples/jax_rs/basic_https directory run the following commands. They 
are entered on a single command line.

For UNIX (must use forward slashes):
    java -Djava.util.logging.config.file=$CXF_HOME/etc/logging.properties
         demo.jaxrs.server.Server &

    java -Djava.util.logging.config.file=$CXF_HOME/etc/logging.properties
         demo.jaxrs.client.Client

The server process starts in the background.  After running the client,
use the kill command to terminate the server process.

For Windows (may use either forward or back slashes):
  start 
    java -Djava.util.logging.config.file=%CXF_HOME%\etc\logging.properties
       demo.jaxrs.server.Server

    java -Djava.util.logging.config.file=%CXF_HOME%\etc\logging.properties
       demo.jaxrs.client.Client

A new command windows opens for the server process.  After running the
client, terminate the server process by issuing Ctrl-C in its command window.

To remove the code generated from the WSDL file and the .class
files, either delete the build directory and its contents or run:

  ant clean


Certificates
------------

If the certificates are expired for some reason, a shell script in 
bin/gencerts.sh will generate the set of certificates needed for
this sample. Just do the following:

        cd certs
        sh ../bin/gencerts.sh
